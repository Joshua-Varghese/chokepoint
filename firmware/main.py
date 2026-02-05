import time
import machine
import ubinascii
import json
import config
from wifi_manager import WifiManager
from umqtt.simple import MQTTClient
try:
    import mq135
except ImportError:
    mq135 = None

# --- Global State ---
device_id = ubinascii.hexlify(machine.unique_id()).decode()
wm = WifiManager()
mqtt = None

# --- Sensor Mock if mq135 missing ---
if mq135:
    mq = mq135.MQ135(34) # Pin 34
else:
    class MockSensor:
        def get_readings(self):
            return {"co2": 400.0, "nh3": 0.05, "smoke": 10.0}
    mq = MockSensor()

def mqtt_callback(topic, msg):
    print("MSG:", topic, msg)
    try:
        cmd = json.loads(msg)
        if cmd.get('cmd') == 'reset':
            machine.reset()
        elif cmd.get('cmd') == 'ping':
            mqtt.publish(f"chokepoint/devices/{device_id}/status", "pong")
    except:
        pass

def main():
    global mqtt
    print("Booting Chokepoint Firmware...")
    print("Device ID:", device_id)
    
    # 1. Try to load and connect WiFi
    ssid, password = wm.load_config()
    connected = False
    
    if ssid:
        print(f"Found Config for {ssid}")
        connected = wm.connect(ssid, password)
    
    # 2. If Failed -> Provisioning Mode
    if not connected:
        print("WiFi Connection Failed or Config Missing.")
        # Only start AP if we really can't connect.
        # Check if we should retry or go straight to AP. 
        # For now, go to AP.
        wm.run_provisioning_server()
        return # run_provisioning_server loops forever/resets

    # 3. MQTT Connection
    print("WiFi Connected. Connecting to RabbitMQ...")
    try:
        mqtt = MQTTClient(
            client_id=device_id,
            server=config.MQTT_BROKER,
            port=config.MQTT_PORT,
            user=config.MQTT_USER,
            password=config.MQTT_PASS,
            keepalive=60
        )
        mqtt.set_callback(mqtt_callback)
        mqtt.connect()
        print("MQTT Connected!")
        
        # Subscribe
        cmd_topic = f"chokepoint/devices/{device_id}/cmd"
        mqtt.subscribe(cmd_topic)
        
        # Main Loop
        while True:
            try:
                mqtt.check_msg()
                
                # Read Sensor
                data = mq.get_readings() # Returns dict
                data['device_id'] = device_id
                data['timestamp'] = int(time.time())
                
                # Publish
                payload = json.dumps(data)
                topic = f"chokepoint/devices/{device_id}/data"
                mqtt.publish(topic, payload)
                print("Pub:", payload)
                
                time.sleep(2)
                
            except OSError as e:
                print("MQTT Error:", e)
                # Try reconnect
                try: 
                    mqtt.connect() 
                    mqtt.subscribe(cmd_topic)
                except: 
                    time.sleep(5)
                    
    except Exception as e:
        print("Fatal Error:", e)
        time.sleep(10)
        machine.reset()

if __name__ == "__main__":
    main()