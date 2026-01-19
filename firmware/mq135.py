import machine
import time
import random

class MQ135:
    def __init__(self, pin_adc):
        self.adc = machine.ADC(machine.Pin(pin_adc))
        self.adc.atten(machine.ADC.ATTN_11DB) # Full range: 3.3v

    def get_readings(self):
        # Simulate real gas variation based on raw ADC
        raw = self.adc.read()
        # Fake formulas for demo
        co2 = 400 + (raw / 10) + random.uniform(-10, 10)
        nh3 = 5 + (raw / 500)
        smoke = (raw / 100)
        return {"co2": co2, "nh3": nh3, "smoke": smoke}
