import math

# Default calibration if no R0 is stored
DEFAULT_R0 = 76.63

# MQ-135 Gas Curves (CO2)
# The curve is modeled as: ppm = a * (Rs/R0) ^ b
# For CO2: a = 110.47, b = -2.862 (Approximated from datasheet)
CO2_A = 110.47
CO2_B = -2.862

def get_ppm(raw_val, r0=DEFAULT_R0):
    if raw_val <= 0: return 400.0 # Baseline CO2
    
    # 1. Convert Raw (0-4095) to Voltage (assuming 3.3V reference)
    v_out = raw_val * (3.3 / 4095)
    
    # 2. Calculate Sensor Resistance (Rs)
    # Circuit: Vcc (5V) -> Sensor -> RL (1k) -> GND
    # Rs = ((Vcc/Vout) - 1) * RL
    if v_out >= 5.0 or v_out <= 0: return 400.0
    
    rs = ((5.0 / v_out) - 1.0) * 1.0 # RL is usually 1.0k
    
    # 3. Calculate Ratio (Rs/R0)
    ratio = rs / r0
    
    # 4. Calculate PPM
    ppm = CO2_A * math.pow(ratio, CO2_B)
    
    # Clamp to reasonable values
    return max(400.0, min(ppm, 10000.0))
