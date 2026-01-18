import network
import time
import ujson
import socket
import machine

CONFIG_FILE = 'wifi.json'
AP_SSID = "Chokepoint-Setup"
AP_PASS = "" # Open

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
    
    network.WLAN(network.STA_IF).active(False)
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
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    s.bind(('', 80))
    s.listen(1)
    
    print("Waiting for credentials on port 80...")
    
    while True:
        try:
            conn, addr = s.accept()
            print('Got connection from', addr)
            request = conn.recv(1024)
            request_str = str(request)
            
            # Simple Parser for /configure?ssid=...&pass=...
            if "ssid=" in request_str:
                try:
                    # Extract SSID
                    ssid_start = request_str.find("ssid=") + 5
                    ssid_end = request_str.find("&", ssid_start)
                    if ssid_end == -1: ssid_end = request_str.find(" ", ssid_start)
                    ssid = request_str[ssid_start:ssid_end]
                    
                    # Extract Pass
                    pass_start = request_str.find("pass=") + 5
                    pass_end = request_str.find(" ", pass_start)
                    if pass_end == -1: pass_end = len(request_str)
                    http_idx = request_str.find(" HTTP", pass_start)
                    if http_idx != -1 and http_idx < pass_end: pass_end = http_idx
                    
                    password = request_str[pass_start:pass_end]
                    
                    # Cleanup
                    ssid = ssid.replace("%20", " ").replace("+", " ").replace("'", "").strip()
                    password = password.replace("%20", " ").replace("+", " ").replace("'", "").strip()
                    
                    print(f"Parsed Creds: {ssid} / {password}")
                    save_config(ssid, password)
                    
                    # Get ID
                    import config
                    device_id = getattr(config, 'DEVICE_ID', 'esp32_unknown')
                    
                    # JSON Response
                    response_body = ujson.dumps({
                        "status": "success",
                        "device_id": device_id,
                        "message": "Config Saved"
                    })
                    header = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n"
                    conn.send(header.encode() + response_body.encode())
                    conn.close()
                    
                    time.sleep(2)
                    machine.reset()
                    
                except Exception as e:
                    print(f"Parse Error: {e}")
                    conn.send(b"HTTP/1.1 400 Bad Request\r\n\r\nError")
                    conn.close()
            else:
                 # Landing Page with Device ID for Manual Claiming
                 import config
                 did = getattr(config, 'DEVICE_ID', 'Loading...')
                 
                 resp = f"""HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n
                 <html>
                 <head><title>Chokepoint Setup</title></head>
                 <body style='font-family:sans-serif; padding:20px'>
                    <h1>Device Setup</h1>
                    <div style='background:#eee; padding:10px; border-radius:5px'>
                        <h3>Device ID:</h3>
                        <h2 style='color:#007BFF'>{did}</h2>
                        <p>If App setup fails, enter this ID manually in the App.</p>
                    </div>
                 </body>
                 </html>"""
                 conn.send(resp.encode())
                 conn.close()
                 
        except Exception as e:
            print(f"Server Error: {e}")
            if 'conn' in locals() and conn: conn.close()

def connect():
    conf = load_config()
    if not conf:
        start_ap_mode()
        return False
        
    wlan = network.WLAN(network.STA_IF)
    wlan.active(True)
    
    if not wlan.isconnected():
        print(f"Connecting to {conf['ssid']}...")
        wlan.connect(conf['ssid'], conf['pass'])
        
        max_wait = 20
        while max_wait > 0:
            if wlan.status() == network.STAT_GOT_IP:
                break
            max_wait -= 1
            time.sleep(1)
            
    if wlan.status() == network.STAT_GOT_IP:
        print('Connected:', wlan.ifconfig())
        return True
    else:
        print("WiFi Failed. Starting AP.")
        start_ap_mode()
        return False
