import network
import time
import ubinascii
import machine
import json
import socket

class WifiManager:
    def __init__(self, config_file="wifi.json"):
        self.sta_if = network.WLAN(network.STA_IF)
        self.ap_if = network.WLAN(network.AP_IF)
        self.config_file = config_file

    def get_device_id(self):
        return ubinascii.hexlify(machine.unique_id()).decode()

    def load_config(self):
        try:
            with open(self.config_file, 'r') as f:
                data = json.load(f)
                return data.get("ssid"), data.get("password")
        except:
            return None, None

    def save_config(self, ssid, password):
        try:
            with open(self.config_file, 'w') as f:
                json.dump({"ssid": ssid, "password": password}, f)
            return True
        except:
            return False

    def start_ap(self):
        self.ap_if.active(True)
        ssid = 'Chokepoint-' + self.get_device_id()
        self.ap_if.config(essid=ssid, authmode=0)
        print('AP Active: ', self.ap_if.ifconfig())
        print('Connect to WiFi:', ssid)
        print('Then browse to: http://192.168.4.1')

    def connect(self, ssid, password):
        self.sta_if.active(True)
        self.sta_if.connect(ssid, password)
        print('Connecting to', ssid, '...')
        for _ in range(20):
            if self.sta_if.isconnected():
                print('Connected!', self.sta_if.ifconfig())
                return True
            time.sleep(1)
        return False

    def run_provisioning_server(self):
        self.start_ap()
        
        # Simple HTTP Server for Provisioning
        addr = socket.getaddrinfo('0.0.0.0', 80)[0][-1]
        s = socket.socket()
        s.bind(addr)
        s.listen(1)
        print('Provisioning Server Listening on', addr)

        while True:
            cl, addr = s.accept()
            print('Client connected from', addr)
            try:
                cl_file = cl.makefile('rwb', 0)
                while True:
                    line = cl_file.readline()
                    if not line or line == b'\r\n':
                        break
                    # Parse Body if POST
                    if line.startswith(b'POST'):
                        # Read body (simplified content-length parsing omitted for brevity in demo)
                        # We expect simple raw JSON or form data
                        pass
                
                # Capture GET request line
                request_line = line.decode()
                addr = addr[0]
                
                # Consume remaining headers
                while True:
                    h = cl_file.readline()
                    if not h or h == b'\r\n':
                        break

                print("Request:", request_line)
                
                # Parsing Logic
                if 'GET /save?' in request_line:
                    try:
                        path = request_line.split(' ')[1]
                        query = path.split('?')[1]
                        # Safe parameter parsing
                        params = {}
                        for pair in query.split('&'):
                            if '=' in pair:
                                k, v = pair.split('=', 1)
                                params[k] = v.replace('+', ' ').replace('%20', ' ')
                        
                        ssid = params.get('ssid', '').strip()
                        pwd = params.get('password', '').strip()
                        
                        if ssid:
                            self.save_config(ssid, pwd)
                            # Success Response
                            cl.send('HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n')
                            cl.send('<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width, initial-scale=1"></head>')
                            cl.send('<body style="font-family:sans-serif; text-align:center; padding:20px;">')
                            cl.send('<h1>Saved!</h1><p>Restarting...</p>')
                            cl.send('</body></html>')
                            cl.send('</body></html>')
                            print("Config Saved. Rebooting in 3 seconds...")
                            time.sleep(3)
                            cl.close()
                            machine.reset()
                        else:
                            print("Ignored empty SSID")
                    except Exception as e:
                        print("Parse Error", e)

                # Serve Form (Mobile Friendly)
                response = """HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; background: #f4f4f5; padding: 20px; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }
                        .card { background: white; padding: 2rem; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); width: 100%; max-width: 320px; }
                        h1 { font-size: 1.5rem; margin-bottom: 1.5rem; text-align: center; color: #18181b; }
                        input { width: 100%; padding: 12px; margin-bottom: 1rem; border: 1px solid #e4e4e7; border-radius: 8px; box-sizing: border-box; font-size: 16px; }
                        button { width: 100%; padding: 12px; background: #000; color: white; border: none; border-radius: 8px; font-weight: bold; cursor: pointer; font-size: 16px; }
                        label { display: block; margin-bottom: 0.5rem; font-size: 0.875rem; color: #52525b; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h1>Setup WiFi</h1>
                        <form action="/save" method="get">
                            <label>Network Name</label>
                            <input name="ssid" placeholder="MyWiFi" required>
                            <label>Password</label>
                            <input name="password" type="password" placeholder="********">
                            <button type="submit">Connect</button>
                        </form>
                    </div>
                </body>
                </html>
                """
                cl.send(response)
                
            except Exception as e:
                print("Server Error", e)
            finally:
                cl.close()

