package com.joshua.chokepoint.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshua.chokepoint.data.model.SensorData
import com.joshua.chokepoint.data.mqtt.MqttRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    private val repository: MqttRepository,
    private val firestoreRepository: com.joshua.chokepoint.data.firestore.FirestoreRepository // Inject this
) : ViewModel() {

    val isConnected: StateFlow<Boolean> = repository.isConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Device Management
    val savedDevices: StateFlow<List<com.joshua.chokepoint.data.firestore.FirestoreRepository.Device>> = firestoreRepository.observeDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently Selected Device Logic (Default to first)
    // In a real app, this would be a selection stored in DataStore or UI state
    // For now, we derive it.
    private val allReadings = repository.deviceReadings

    // Exposed SensorData determined by what is selected or available
    val sensorData: StateFlow<SensorData> = kotlinx.coroutines.flow.combine(allReadings, savedDevices) { readings, devices ->
        if (devices.isEmpty()) {
            return@combine SensorData(airQuality = "No Devices", deviceId = "No Devices Connected")
        }
        // TODO: Add selection support. For now, pick first device.
        val targetId = devices.first().id
        readings[targetId] ?: SensorData(deviceId = targetId, airQuality = "Waiting for data...")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SensorData())

    fun connect() {
        repository.connect()
    }
    
    fun disconnect() {
        repository.disconnect()
    }

    fun recalibrateSensor(deviceId: String) {
        if (deviceId.isEmpty() || deviceId == "No Devices Connected") return
        repository.publishCommand(deviceId, "CAL")
    }
}
