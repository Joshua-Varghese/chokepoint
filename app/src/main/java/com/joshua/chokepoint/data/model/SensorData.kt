package com.joshua.chokepoint.data.model

data class SensorData(
    // Legacy support
    val co2: Double = 0.0,
    val nh3: Double = 0.0,
    val smoke: Double = 0.0,
    
    // Firmware Data
    val deviceId: String = "",
    val gasRaw: Int = 0,
    val airQuality: String = "Unknown",
    val timestamp: Long = System.currentTimeMillis()
)
