import network
import urequests
import os
import machine
import time

class OTAUpdater:
    def __init__(self, main_dir='/'):
        self.main_dir = main_dir

    def download_and_install(self, url, filename="main.py"):
        print("OTA: Downloading from", url)
        try:
            response = urequests.get(url)
            if response.status_code == 200:
                print("OTA: Download success. Writing to", filename)
                with open(filename, 'w') as f:
                    f.write(response.text)
                print("OTA: Update Complete. Restarting...")
                time.sleep(1)
                machine.reset()
            else:
                print("OTA: Failed. Status Code:", response.status_code)
                response.close()
        except Exception as e:
            print("OTA: Error", e)

    def simple_update(self, url):
        # Update main.py by default
        self.download_and_install(url, "main.py")
