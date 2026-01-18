import network
import time
import ujson
import socket
import machine

CONFIG_FILE = 'wifi.json'
AP_SSID = "Chokepoint-Setup"
AP_PASS = "" # Open network

def load_config():
    try:
        with open(CONFIG_FILE, 'r') as f:
            return ujson.load(f)
    except:
        return None

def save_config(ssid, password):
    with open(CONFIG_FILE, 'w') as f:
        ujson.dump({"ssid": ssid, "pass": password}, f)

def start_ap_mode():
    print(f"Starting AP Mode: {AP_SSID}")
    
    # Clean up STA interface
    sta = network.WLAN(network.STA_IF)
    sta.active(False)
    time.sleep(0.5)

    ap = network.WLAN(network.AP_IF)
    ap.active(True)
    time.sleep(0.5)
    
    try:
        ap.config(essid=AP_SSID, password=AP_PASS)
    except Exception as e:
        print(f"SoftAP Config Error: {e}")
    
    print(f"AP Active. IP: {ap.ifconfig()[0]}")
    
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1) # Allow port reuse
    s.bind(('', 80))
    s.listen(1)
    
    print("Waiting for credentials on port 80...")
    
    while True:
        conn = None
        try:
            conn, addr = s.accept()
            print('Got connection from', addr)
            request = conn.recv(1024)
            request_str = str(request)
            print("RAW Request:", request_str) # Debug print
            
            if "ssid=" in request_str and "pass=" in request_str:
                
                # Robust extraction
                ssid_start = request_str.find("ssid=") + 5
                ssid_end = request_str.find("&", ssid_start)
                if ssid_end == -1: 
                    # Fallback if & not found, look for HTTP space
                    ssid_end = request_str.find(" ", ssid_start)
                
                pass_start = request_str.find("pass=") + 5
                pass_end = request_str.find(" ", pass_start)
                
                if pass_end == -1:
                    password = request_str[pass_start:]
                else:
                    password = request_str[pass_start:pass_end]
                
                ssid = request_str[ssid_start:ssid_end]
                
                # Decode URL encoding
                ssid = ssid.replace("%20", " ").replace("+", " ")
                password = password.replace("%20", " ").replace("+", " ")
                
                # Remove quotes from bytes->str conversion if present
                ssid = ssid.replace("'", "")
                password = password.replace("'", "")
                
                ssid = ssid.strip()
                password = password.strip()
                
                print(f"Parsed Creds: SSID='{ssid}', PASS='{password}'")
                save_config(ssid, password)
                
                # Import config to get Device ID (lazy import to ensure it exists)
                import config
                device_id = config.DEVICE_ID
                
                # Return JSON response with Device ID for App to claim
                response_body = ujson.dumps({
                    "status": "success",
                    "device_id": device_id,
                    "message": "Config Saved. Rebooting..."
                })
                
                response = f"HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n{response_body}"
                conn.send(response.encode())
                conn.close()
                time.sleep(2)
                machine.reset()
            
            response = "HTTP/1.1 200 OK\n\nChokepoint Setup: Send GET /configure?ssid=...&pass=..."
            conn.send(response.encode())
            if conn: conn.close()
            
        except Exception as e:
            print("Web Server Error:", e)
        conn, addr = s.accept()
        print('Got a connection from %s' % str(addr))
        request = conn.recv(1024)
        request = str(request)
        
        # Simple parsing for GET /?ssid=MYSSID&pass=MYPASS or POST JSON
        # For simplicity, we'll verify if we received a config payload
        # In a robust app, use a proper URL encoded parser
        
        # Mocking the parsing for the demo to accept simple JSON body if possible
        # or simple GET query params
        
        try:
            # Check for JSON payload in body
            if "ssid" in request and "pass" in request:
                # Very naive parsing for demo purposes (assuming simple POST or parameters)
                # In real life, use a regex or split
                
                # Let's assume the Android app sends a Clean JSON string or query params
                # For this demo, we'll wait for a specific URL format: /configure?ssid=...&pass=...
                
                # We will extract vaguely
                ssid_start = request.find("ssid=") + 5
                ssid_end = request.find("&", ssid_start)
                if ssid_end == -1: ssid_end = request.find(" ", ssid_start)
                
                pass_start = request.find("pass=") + 5
                pass_end = request.find(" ", pass_start)
                
                # FIX: If no space found (end of string), take the rest of the string
                if pass_end == -1:
                    password = request[pass_start:]
                else:
                    password = request[pass_start:pass_end]
                
                # Restore SSID extraction (MISSING IN PREVIOUS EDIT)
                ssid = request[ssid_start:ssid_end]
                
                ssid = ssid.replace("%20", " ")
                password = password.replace("%20", " ")
                
                # Strip potential HTTP garbage if parsing failed slightly
                ssid = ssid.strip()
                password = password.strip()
                
                print(f"Received Config: '{ssid}' / '{password}'")
                save_config(ssid, password)
                
                response = "HTTP/1.1 200 OK\n\nConfig Saved. Rebooting..."
                conn.send(response.encode())
                conn.close()
                time.sleep(2)
                machine.reset()
                
        except Exception as e:
            print("Error parsing", e)
            
        response = "HTTP/1.1 200 OK\n\nNexus Setup: Send POST or GET /configure?ssid=XX&pass=XX"
        conn.send(response.encode())
        conn.close()


def connect():
    conf = load_config()
    
    if not conf:
        print("No Config Found. Starting AP.")
        start_ap_mode()
        return False
    
    print(f"Connecting to {conf['ssid']}...")
    
    # Clean up AP interface
    ap = network.WLAN(network.AP_IF)
    ap.active(False)
    time.sleep(0.5)
        
    wlan = network.WLAN(network.STA_IF)
    wlan.active(True)
    time.sleep(1.0) # Critical wait for ESP32 radio init
    
    try:
        wlan.connect(conf['ssid'], conf['pass'])
    except Exception as e:
         print(f"Connect Failure: {e}")
         start_ap_mode()
         return False
    
    max_wait = 40 # Increased to 40s
    while max_wait > 0:
        status = wlan.status()
        print(f'Waiting... Status: {status}')
        
        if status == network.STAT_GOT_IP:
            break
        
        if status == network.STAT_WRONG_PASSWORD:
            print("WRONG PASSWORD DETECTED!")
            break
        elif status == network.STAT_NO_AP_FOUND:
            print("AP NOT FOUND! (Check 2.4GHz vs 5GHz)")
            # Keep waiting just in case it appears
            
        max_wait -= 1
        time.sleep(1)
        
    final_status = wlan.status()
    if final_status != network.STAT_GOT_IP:
        print(f"Connection Failed. Final Status: {final_status}")
        start_ap_mode()
        return False
    else:
        print('Connected! IP:', wlan.ifconfig()[0])
        return True
