# boot.py - Runs on boot-up
import wifi_manager

# This will:
# 1. Try to load wifi.json
# 2. If found, try to connect -> Returns True
# 3. If not found or fail, start AP Mode (192.168.4.1) -> Returns False

# Note: We let main.py handle the rest, or block here if strict connectivity needed.
# For now, wifi_manager.connect() handles the LED blinking and AP fallback internally.
wifi_manager.connect()
