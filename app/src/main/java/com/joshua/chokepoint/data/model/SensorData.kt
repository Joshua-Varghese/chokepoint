package com.joshua.chokepoint.data.model

data class SensorData(
    val co2: Double = 0.0,
    val nh3: Double = 0.0,
    val smoke: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)
