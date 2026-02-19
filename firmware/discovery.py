import socket
import ubinascii
import machine
import select

class Discovery:
    def __init__(self, port=6666):
        self.port = port
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.sock.bind(('0.0.0.0', self.port))
        self.sock.setblocking(False)
        self.device_id = ubinascii.hexlify(machine.unique_id()).decode()

    def check(self):
        try:
            # Check for readable data without blocking
            r, _, _ = select.select([self.sock], [], [], 0)
            if r:
                data, addr = self.sock.recvfrom(1024)
                msg = data.decode().strip()
                
                # Protocol: DISCOVER:{TARGET_ID}
                if msg.startswith("DISCOVER:"):
                    target_id = msg.split(":")[1]
                    if target_id == self.device_id:
                        print(f"Discovery matched for {self.device_id} from {addr}")
                        response = f"HERE:{self.device_id}"
                        self.sock.sendto(response.encode(), addr)
                    else:
                        # Silent drop (Stealth Mode)
                        pass
        except Exception as e:
            print("Discovery Error:", e)
