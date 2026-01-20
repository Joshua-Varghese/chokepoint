# Chokepoint Firmware Configuration

try:
    from secrets import WIFI_SSID, WIFI_PASS, MQTT_USER, MQTT_PASS
except ImportError:
    print("Error: secrets.py not found. Please create it.")
    WIFI_SSID = ""
    WIFI_PASS = ""
    MQTT_USER = ""
    MQTT_PASS = ""

# MQTT Configuration
MQTT_SERVER = "puffin.rmq2.cloudamqp.com" 
MQTT_PORT = 1883
MQTT_KEEPALIVE = 60

# Device Settings
import machine
import ubinascii
_id = ubinascii.hexlify(machine.unique_id()).decode('utf-8')
DEVICE_ID = f"esp32_{_id}" # E.g., esp32_240ac4d876a1
DATA_TOPIC = f"chokepoint/devices/{DEVICE_ID}/data"
COMMAND_TOPIC = f"chokepoint/devices/{DEVICE_ID}/commands"

# Sensor Settings
SENSOR_PIN = 34  # ADC Pin for MQ-135
READ_INTERVAL = 30 # Seconds between readings (User Requested)
