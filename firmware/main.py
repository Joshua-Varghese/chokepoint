import time
import machine
import ubinascii
import json
import config
from wifi_manager import WifiManager
from umqtt.simple import MQTTClient
import urequests
import ota
try:
    import mq135
except ImportError:
    mq135 = None

from discovery import Discovery
import file_mgr
print("Testing")
# --- Global State ---
device_id = ubinascii.hexlify(machine.unique_id()).decode()
wm = WifiManager()
mqtt = None
discovery_service = None
fm = file_mgr.FileManager()

# ---- OTA UPGRADE CHECK ----
CURRENT_VERSION = "1.0.0"

def check_for_updates():
    if not config.GITHUB_PAT or not config.GITHUB_OWNER or not config.GITHUB_REPO:
        print("GitHub Configuration missing in config.py. Skipping OTA.")
        return

    print("Checking GitHub for OTA updates...")
    try:
        url = f"https://api.github.com/repos/{config.GITHUB_OWNER}/{config.GITHUB_REPO}/contents/firmware/main.py"
        headers = {
            "User-Agent": "ESP32-Chokepoint",
            "Authorization": f"Bearer {config.GITHUB_PAT}",
            "Accept": "application/vnd.github.v3+json"
        }
        r = urequests.get(url, headers=headers)
        if r.status_code == 200:
            data = r.json()
            remote_sha = data.get("sha", "")
            download_url = data.get("download_url", "")
            
            local_sha = ""
            try:
                with open("version_sha.txt", "r") as f:
                    local_sha = f.read().strip()
            except:
                pass
            
            if remote_sha and remote_sha != local_sha:
                print("New firmware commit detected:", remote_sha)
                
                # Fetch private raw file using PAT directly from the API endpoint
                headers_raw = {
                    "User-Agent": "ESP32-Chokepoint",
                    "Authorization": f"Bearer {config.GITHUB_PAT}",
                    "Accept": "application/vnd.github.v3.raw"
                }
                
                updater = ota.OTAUpdater()
                success = updater.simple_update(url, headers=headers_raw)
                
                if success:
                    with open("version_sha.txt", "w") as f:
                        f.write(remote_sha)
                    print("Update successfully applied. Rebooting!")
                    import time
                    time.sleep(1)
                    machine.reset()
                else:
                    print("Firmware update download failed.")
            else:
                print("Firmware is up to date.")
        else:
            print("GitHub API Check Failed. Status:", r.status_code)
            print("Response:", r.text)
        r.close()
    except Exception as e:
        print("Update check failed:", e)

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
        elif cmd.get('cmd') == 'reset_wifi':
            print("Received Factory Reset Command!")
            wm.reset_config()
            time.sleep(1)
            machine.reset()
        elif cmd.get('cmd') == 'ping':
            mqtt.publish(f"chokepoint/devices/{device_id}/status", "pong")
        elif cmd.get('cmd') == 'ls':
            files = fm.list_files()
            mqtt.publish(f"chokepoint/devices/{device_id}/res", json.dumps({"cmd": "ls", "files": files}))
        elif cmd.get('cmd') == 'read':
            content = fm.read_file(cmd.get('path', ''))
            mqtt.publish(f"chokepoint/devices/{device_id}/res", json.dumps({"cmd": "read", "path": cmd.get('path', ''), "content": content}))
        elif cmd.get('cmd') == 'write':
            status = fm.write_file(cmd.get('path', ''), cmd.get('content', ''))
            mqtt.publish(f"chokepoint/devices/{device_id}/res", json.dumps({"cmd": "write", "path": cmd.get('path', ''), "status": status}))
        elif cmd.get('cmd') == 'rm':
            status = fm.delete_file(cmd.get('path', ''))
            mqtt.publish(f"chokepoint/devices/{device_id}/res", json.dumps({"cmd": "rm", "path": cmd.get('path', ''), "status": status}))
        elif cmd.get('cmd') == 'restart':
            machine.reset()
    except:
        pass

def main():
    global mqtt, discovery_service
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
        wm.run_provisioning_server()
        return 



    # 3. Initialize Discovery
    try:
        discovery_service = Discovery()
        print("Discovery Service Started on UDP 6666")
    except Exception as e:
        print("Discovery Init Failed:", e)

    # 4. MQTT Connection
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
        last_sensor_read = 0
        while True:
            try:
                # Fast checks
                mqtt.check_msg()
                if discovery_service:
                    discovery_service.check()
                
                # Periodic Sensor Read (every 2s)
                now = time.time()
                if now - last_sensor_read >= 2:
                    last_sensor_read = now
                    
                    # Read Sensor
                    data = mq.get_readings()
                    data['device_id'] = device_id
                    data['timestamp'] = int(now)
                    
                    # Publish
                    payload = json.dumps(data)
                    topic = f"chokepoint/devices/{device_id}/data"
                    mqtt.publish(topic, payload)
                    print("Pub:", payload)
                
                time.sleep(0.1) # Responsive loop
                
            except OSError as e:
                print("MQTT Error:", e)
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