from machine import ADC, Pin
import time

# Configuration
analog_pin = ADC(Pin(34))
analog_pin.atten(ADC.ATTN_11DB) # For 3.3V range
analog_pin.width(ADC.WIDTH_12BIT)

# Voltage Divider & Sensor Constants
RLOAD = 1.0  # The load resistor on the MQ-135 board is usually 1k Ohm
FRESH_AIR_FACTOR = 3.6  # Clean air factor for MQ-135

def get_r0():
    print("Finding R0 in fresh air... please wait.")
    total_r0 = 0
    samples = 50
    
    for _ in range(samples):
        raw = analog_pin.read()
        # Convert raw to voltage (compensating for 11dB attenuation)
        v_out = raw * (3.3 / 4095) 
        
        # Calculate Sensor Resistance (Rs)
        # Formula: Rs = ((Vcc/Vout) - 1) * RL
        if v_out > 0:
            rs = ((5.0 / v_out) - 1) * RLOAD
            total_r0 += rs / FRESH_AIR_FACTOR
        
        time.sleep(0.5)
    
    final_r0 = total_r0 / samples
    print("Calibration Complete!")
    print("Your R0 value is:", final_r0)
    return final_r0

# Run calibration
R0 = get_r0()