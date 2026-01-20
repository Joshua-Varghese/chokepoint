# scan.py - Run this to debug Wi-Fi visibility
import network
import time

def scan():
    sta = network.WLAN(network.STA_IF)
    sta.active(True)
    
    print("Scanning for networks...")
    networks = sta.scan()
    
    print(f"\nFound {len(networks)} networks:")
    print("-" * 40)
    print(f"{'SSID':<20} | {'RSSI':<5} | {'Channel'}")
    print("-" * 40)
    
    found_target = False
    target = "Amal Jyothi New"
    
    for net in networks:
        ssid = net[0].decode('utf-8')
        rssi = net[3]
        channel = net[2] # Some firmwares index differently, check docs if weird
        
        print(f"{ssid:<20} | {rssi:<5} | {channel}")
        
        if ssid == target:
            found_target = True
            
    print("-" * 40)
    
    if found_target:
        print(f"\n✅ SUCCESS: '{target}' was found!")
        print("If connection fails, check Password or DHCP.")
    else:
        print(f"\n❌ FAILURE: '{target}' was NOT found.")
        print("Possible reasons:")
        print("1. It is a 5GHz-only network (ESP32 only supports 2.4GHz).")
        print("2. Signal is too weak.")
        print("3. SSID is hidden.")

scan()
