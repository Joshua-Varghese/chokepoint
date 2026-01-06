package com.joshua.chokepoint.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshua.chokepoint.data.model.SensorData
import com.joshua.chokepoint.data.mqtt.MqttRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(private val repository: MqttRepository) : ViewModel() {

    val isConnected: StateFlow<Boolean> = repository.isConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val sensorData: StateFlow<SensorData> = repository.sensorData
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SensorData())

    fun connect() {
        repository.connect()
    }
    
    fun disconnect() {
        repository.disconnect()
    }
}
