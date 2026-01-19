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
                
                # Parse GET request manually for demo
                # Request: GET /save?ssid=MyWiFi&password=123 HTTP/1.1
                if b'/save?' in line:
                    try:
                        path = line.split(b' ')[1]
                        query = path.split(b'?')[1].decode()
                        params = dict(qs.split('=') for qs in query.split('&'))
                        ssid = params.get('ssid', '').replace('+', ' ')
                        pwd = params.get('password', '').replace('+', ' ')
                        
                        if ssid:
                            self.save_config(ssid, pwd)
                            cl.send('HTTP/1.1 200 OK\r\n\r\nSaved. Restarting...')
                            cl.close()
                            time.sleep(1)
                            machine.reset()
                    except Exception as e:
                        print("Parse/Save Error", e)

                # Send Form
                response = """HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n
                <!DOCTYPE html><html><body>
                <h1>Setup WiFi</h1>
                <form action="/save" method="get">
                    SSID: <input name="ssid"><br>
                    Pass: <input name="password"><br>
                    <input type="submit" value="Save">
                </form>
                </body></html>
                """
                cl.send(response)
                
            except Exception as e:
                print("Server Error", e)
            finally:
                cl.close()

