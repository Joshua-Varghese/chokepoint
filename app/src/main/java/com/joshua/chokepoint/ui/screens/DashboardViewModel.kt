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

    // Exposed Map of SensorData for all devices
    val deviceReadings: StateFlow<Map<String, SensorData>> = repository.deviceReadings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

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
