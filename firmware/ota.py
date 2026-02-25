import network
import urequests
import os
import machine
import time

class OTAUpdater:
    def __init__(self, main_dir='/'):
        self.main_dir = main_dir

    def download_and_install(self, url, filename="main.py", headers=None):
        print("OTA: Downloading from", url)
        try:
            response = urequests.get(url, headers=headers)
            if response.status_code == 200:
                print("OTA: Download success. Writing to", filename)
                with open(filename, 'w') as f:
                    f.write(response.text)
                print("OTA: Update Complete.")
                response.close()
                return True
            else:
                print("OTA: Failed. Status Code:", response.status_code)
                response.close()
                return False
        except Exception as e:
            print("OTA: Error", e)
            return False

    def simple_update(self, url, headers=None):
        # Update main.py by default
        return self.download_and_install(url, "main.py", headers=headers)
