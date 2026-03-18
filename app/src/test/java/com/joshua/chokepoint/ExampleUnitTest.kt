package com.joshua.chokepoint

import com.joshua.chokepoint.data.model.SensorData
import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun testNotificationFatigueAlgorithm() {
        val lastBaseline = 300
        val currentReading = SensorData(gasRaw = 310, airQuality = "Good") // Minor fluctuation
        
        // Simulating the rate of change check in the Cloud functions
        val difference = Math.abs(currentReading.gasRaw - lastBaseline)
        val isSignificant = difference > 50 
        
        assertFalse("Minor fluctuation should not trigger an alert", isSignificant)
    }

    @Test
    fun testSignificantAirQualityDrop() {
        val lastBaseline = 300
        val currentReading = SensorData(gasRaw = 450, airQuality = "Warning") // Major drop
        
        val difference = Math.abs(currentReading.gasRaw - lastBaseline)
        val isSignificant = difference > 50 
        
        assertTrue("Major fluctuation should trigger an alert", isSignificant)
    }
}