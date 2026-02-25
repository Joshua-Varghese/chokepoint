# boot.py -- run on boot-up
import builtins
import time
import machine
import config
from wifi_manager import WifiManager
import urequests
import gc

gc.collect()

def bootloader_ota_check():
    print("--- CHOKEPOINT BOOTLOADER ---")
    wm = WifiManager()
    ssid, password = wm.load_config()
    
    if not ssid:
        print("No WiFi Config found. Booting direct to main.py for Provisioning...")
        return
        
    print(f"Connecting to {ssid} for OTA Check...")
    if not wm.connect(ssid, password):
        print("WiFi Failed. Booting main.py...")
        return
        
    print("WiFi Connected. Checking GitHub for OTA updates...")
    if not config.GITHUB_PAT or not config.GITHUB_OWNER or not config.GITHUB_REPO:
        print("GitHub Configuration missing in config.py. Skipping OTA.")
        return

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
            b64_content = data.get("content", "")
            
            local_sha = ""
            try:
                with open("version_sha.txt", "r") as f:
                    local_sha = f.read().strip()
            except:
                pass
            
            if remote_sha and remote_sha != local_sha:
                print("\n[BOOTLOADER] New firmware commit detected:", remote_sha)
                
                if b64_content:
                    print("[BOOTLOADER] Decoding 'main.py' directly from API response...")
                    import ubinascii
                    # GitHub formats Base64 with newlines, we must remove them for the ESP32 decoder
                    b64_content = b64_content.replace("\n", "")
                    
                    try:
                        raw_bytes = ubinascii.a2b_base64(b64_content)
                        with open("main.py", "wb") as f:
                            f.write(raw_bytes)
                            
                        with open("version_sha.txt", "w") as f:
                            f.write(remote_sha)
                            
                        print("[BOOTLOADER] Update successfully applied. Rebooting cleanly into new main.py!")
                        r.close()
                        time.sleep(1)
                        machine.reset()
                    except Exception as e:
                        print("[BOOTLOADER] Failed to decode or write firmware:", e)
                else:
                    print("[BOOTLOADER] Firmware update failed: No content array in JSON.")
            else:
                print("[BOOTLOADER] Firmware is up to date.")
            
            r.close()
        else:
            print("[BOOTLOADER] GitHub API Check Failed. Status:", r.status_code)
            r.close()
    except Exception as e:
        print("[BOOTLOADER] Update check failed:", e)

# Run the OTA Check before handing off to main.py
try:
    bootloader_ota_check()
except Exception as e:
    print("Bootloader error:", e)
    
print("--- HANDING OVER TO MAIN.PY ---")
