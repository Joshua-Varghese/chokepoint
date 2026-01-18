# main.py - Main Loop (DUMMY DATA MODE)
import time
import ujson
import machine
import urandom # For dummy data
from umqtt.simple import MQTTClient
import config

# --- Dummy Data Generator ---
def read_dummy_sensor():
    # Simulate a gas sensor value between 100 and 4095
    # Vary it slightly to look realistic
    base = 500
    noise = urandom.getrandbits(10) # 0-1023
    raw_value = base + noise
    
    # Logic for status
    status = "Good"
    if raw_value > 2500:
        status = "Hazardous"
    elif raw_value > 1500:
        status = "Poor"
    elif raw_value > 800:
        status = "Moderate"
        
    return raw_value, status

def connect_mqtt():
    global client
    print(f"Connecting to MQTT Broker: {config.MQTT_SERVER}")
    try:
        client = MQTTClient(config.DEVICE_ID, config.MQTT_SERVER, 
                           user=config.MQTT_USER, password=config.MQTT_PASS, 
                           keepalive=config.MQTT_KEEPALIVE)
        client.connect()
        print("Connected to MQTT")
        return True
    except Exception as e:
        print(f"MQTT Connection Failed: {e}")
        return False

def restart():
    print("Soft Resetting...")
    machine.reset()

# --- Main Logic ---
print("Starting Sensor Loop (SIMULATION MODE)...")

client = None
while True:
    try:
        if not client:
            if not connect_mqtt():
                time.sleep(5)
                continue

        # 1. Generate Dummy Data
        raw, quality = read_dummy_sensor()
        print(f"Simulating: Raw={raw} | Quality={quality}")
        
        # 2. Prepare Payload
        payload = ujson.dumps({
            "device_id": config.DEVICE_ID,
            "gas_raw": raw,
            "air_quality": quality,
            "timestamp": time.time()
        })
        
        # 3. Publish
        client.publish(config.DATA_TOPIC, payload)
        print(f"Published to {config.DATA_TOPIC}")
        
    except OSError as e:
        print(f"MQTT/Network Error: {e}")
        client = None # Force reconnect
        time.sleep(2)
        
    time.sleep(config.READ_INTERVAL)
