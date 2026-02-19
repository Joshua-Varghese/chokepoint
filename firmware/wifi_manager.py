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

    def reset_config(self):
        import os
        try:
            os.remove(self.config_file)
            print("Config deleted.")
            return True
        except Exception as e:
            print("Error deleting config:", e)
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
            try:
                cl, addr = s.accept()
                cl.settimeout(3.0) # Avoid hanging
                print('Client connected from', addr)
                
                cl_file = cl.makefile('rwb', 0)
                
                # Read Request Line
                try:
                    raw_req = cl_file.readline()
                except Exception as e:
                    print("Read Error:", e)
                    cl.close()
                    continue

                if not raw_req:
                    print("Empty Request")
                    cl.close()
                    continue
                
                print("Raw Request:", raw_req)
                request_line = raw_req.decode().strip()
                
                # Consume Headers (prevent blocking)
                while True:
                    try:
                        line = cl_file.readline()
                        if not line or line == b'\r\n' or line == b'\n':
                            break
                    except:
                        break

                print("Parsed Request:", request_line)
                
                # Parsing Logic
                if 'GET /save?' in request_line:
                    try:
                        path = request_line.split(' ')[1]
                        query = path.split('?')[1]
                        params = {}
                        for pair in query.split('&'):
                            if '=' in pair:
                                k, v = pair.split('=', 1)
                                params[k] = v.replace('+', ' ').replace('%20', ' ')
                        
                        ssid = params.get('ssid', '').strip()
                        pwd = params.get('password', '').strip()
                        
                        if ssid:
                            self.save_config(ssid, pwd)
                            # Success Response with Device ID
                            device_id = self.get_device_id()
                            success_html = """HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n
                            <!DOCTYPE html><html>
                            <head><meta name="viewport" content="width=device-width, initial-scale=1">
                            <style>body{font-family:sans-serif;background:#222;color:#fff;text-align:center;padding:20px;}
                            .id{background:#333;padding:10px;font-family:monospace;font-size:1.2em;border-radius:4px;margin:10px 0;user-select:all;}
                            </style></head>
                            <body>
                                <h1>Saved!</h1>
                                <p>Device ID:</p>
                                <div class="id">""" + device_id + """</div>
                                <p>Copy this ID to claim the device in the app.</p>
                                <p>Restarting...</p>
                            </body></html>"""
                            cl.send(success_html.encode())
                            time.sleep(1)
                            cl.close()
                            time.sleep(2)
                            machine.reset()
                        else:
                            print("Ignored empty SSID")
                    except Exception as e:
                        print("Parse Error", e)

                # Serve Form
                response = """HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nConnection: close\r\n\r\n
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Chokepoint Setup</title>
                    <style>
                        body { font-family: sans-serif; background: #222; color: #fff; padding: 20px; text-align: center; }
                        input { padding: 10px; margin: 10px 0; width: 100%; box-sizing: border-box; }
                        button { padding: 10px; width: 100%; background: #0f0; color: #000; border: none; font-weight: bold; }
                    </style>
                </head>
                <body>
                    <h2>Setup WiFi</h2>
                    <form action="/save" method="get">
                        <input name="ssid" placeholder="WiFi Name (SSID)" required>
                        <input name="password" type="password" placeholder="Password">
                        <button type="submit">save</button>
                    </form>
                </body>
                </html>
                """
                cl.send(response.encode())
                
            except Exception as e:
                print("Server Error", e)
            finally:
                try: cl.close()
                except: pass


