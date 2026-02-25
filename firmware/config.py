# config.py
WIFI_SSID = ""  # Set by Provisioning
WIFI_PASS = ""
MQTT_BROKER = "puffin.rmq2.cloudamqp.com"
MQTT_PORT = 1883
MQTT_USER = "lztdkevt:lztdkevt"
MQTT_PASS = "vG7j8gUsE9yG5Li7Mb8qaAcpExZLgdUS"
MQTT_TOPIC_DATA = "sensors/data"
MQTT_TOPIC_CMD = "cmd/"
DEVICE_ID_FILE = "device_id.txt"
GITHUB_PAT = ""   # Leave empty. Set this inside secrets.py
GITHUB_OWNER = "Joshua-Varghese"     # Your GitHub Username
GITHUB_REPO = "chokepoint"  

# Load local secrets to prevent committing PATs to GitHub
try:
    import secrets
    GITHUB_PAT = getattr(secrets, 'GITHUB_PAT', GITHUB_PAT)
    WIFI_SSID = getattr(secrets, 'WIFI_SSID', WIFI_SSID)
    WIFI_PASS = getattr(secrets, 'WIFI_PASS', WIFI_PASS)
except ImportError:
    pass