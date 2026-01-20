# Chokepoint Firmware Configuration

# Wi-Fi Credentials
WIFI_SSID = "Amal Jyothi New"
WIFI_PASS = "amaljyothi"

# MQTT Configuration
MQTT_SERVER = "puffin.rmq2.cloudamqp.com" # Public broker for testing, change to your own
MQTT_PORT = 1883
MQTT_USER = "lztdkevt:lztdkevt"
MQTT_PASS = "vG7j8gUsE9yG5Li7Mb8qaAcpExZLgdUS"
MQTT_KEEPALIVE = 60

# Device Settings
DEVICE_ID = "esp32_mq135_001"
DATA_TOPIC = f"chokepoint/devices/{DEVICE_ID}/data"
COMMAND_TOPIC = f"chokepoint/devices/{DEVICE_ID}/commands"

# Sensor Settings
SENSOR_PIN = 34  # ADC Pin for MQ-135
READ_INTERVAL = 5 # Seconds between readings
