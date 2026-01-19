from wifi_manager import WifiManager
from mq135 import MQ135
from file_mgr import FileManager
import config
import machine
import time
import json
from umqtt.simple import MQTTClient

# --- Setup ---
wm = WifiManager()
device_id = wm.get_device_id()
mq = MQ135(34)
fm = FileManager()

mqtt = None



def mqtt_callback(topic, msg):
    print("MQTT MSG:", topic, msg)
    try:
        payload = json.loads(msg)
        if "cmd" in payload:
            cmd = payload["cmd"]
            res_topic = f"devices/{device_id}/res"
            
            if cmd == "ls":
                files = fm.list_files()
                mqtt.publish(res_topic, json.dumps({"cmd": "ls", "files": files}))
                
            elif cmd == "read":
                path = payload.get("path")
                content = fm.read_file(path)
                # Chunking large files might be needed later, for now simple send
                mqtt.publish(res_topic, json.dumps({"cmd": "read", "path": path, "content": content}))
                
            elif cmd == "write":
                path = payload.get("path")
                content = payload.get("content")
                status = fm.write_file(path, content)
                mqtt.publish(res_topic, json.dumps({"cmd": "write", "path": path, "status": status}))
                print(f"File {path} written. Status: {status}")
                # Auto-restart if main.py is updated? 
                if path == "main.py":
                    print("Main updated. Rebooting...")
                    time.sleep(1)
                    machine.reset()

            elif cmd == "rm":
                path = payload.get("path")
                status = fm.delete_file(path)
                mqtt.publish(res_topic, json.dumps({"cmd": "rm", "path": path, "status": status}))

            elif cmd == "restart":
                machine.reset()
                
    except Exception as e:
        print("MQTT Callback Error:", e)

def main():
    global mqtt
    print("Booting Chokepoint Firmware...")
    print("Device ID:", device_id)
    
    # 1. Try to load config
    ssid, password = wm.load_config()
    
    connected = False
    if ssid:
        print(f"Found Config for {ssid}. Connecting...")
        connected = wm.connect(ssid, password)
    
    # 2. If no config or connection failed -> AP Mode
    if not connected:
        print("WiFi Connection Failed or No Config. Starting Provisioning Mode.")
        wm.run_provisioning_server()
        # run_provisioning_server loops forever until reboot
        return

    # 3. If connected, proceed to MQTT
    if connected:
        print("WiFi Connected.")
        
        try:
            mqtt = MQTTClient(
                client_id=device_id,
                server=config.MQTT_BROKER,
                port=config.MQTT_PORT,
                user=config.MQTT_USER,
                password=config.MQTT_PASS
            )
            mqtt.set_callback(mqtt_callback)
            mqtt.connect()
            print("MQTT Connected.")
            
            # Subscribe to CMD topic
            cmd_topic = f"devices/{device_id}/cmd"
            mqtt.subscribe(cmd_topic)
            print("Listening on:", cmd_topic)
            
            while True:  # Main Loop
                try:
                    mqtt.check_msg()
                    
                    # Periodic Sensor Data
                    data = mq.get_readings()
                    data['device_id'] = device_id
                    mqtt.publish(config.MQTT_TOPIC_DATA, json.dumps(data))
                    
                    time.sleep(2) # Faster loop for responsiveness
                except Exception as e:
                    print("Loop Error:", e)
                    time.sleep(5)
                    try: mqtt.connect()
                    except: pass
                    
        except Exception as e:
            print("MQTT Connection Failed:", e)
            time.sleep(10)
            machine.reset()
    else:
        machine.reset()

if __name__ == "__main__":
    main()
