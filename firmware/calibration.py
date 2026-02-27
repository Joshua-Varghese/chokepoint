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
    samples = 100
    
    for _ in range(samples):
        raw = analog_pin.read()
        v_out = raw * (3.3 / 4095) 
        
        if v_out > 0.1: # Eliminate noise
            rs = ((5.0 / v_out) - 1.0) * RLOAD
            total_r0 += rs / FRESH_AIR_FACTOR
        
        time.sleep(0.1)
    
    final_r0 = total_r0 / samples
    print("Calibration Complete!")
    print("Your R0 value is:", final_r0)
    
    # Save to file for main.py to use
    try:
        with open("r0_value.txt", "w") as f:
            f.write(str(final_r0))
        print("R0 saved to r0_value.txt")
    except Exception as e:
        print("Failed to save R0:", e)
        
    return final_r0

if __name__ == "__main__":
    R0 = get_r0()
