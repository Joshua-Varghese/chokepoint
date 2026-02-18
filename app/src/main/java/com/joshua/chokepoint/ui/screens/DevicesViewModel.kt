package com.joshua.chokepoint.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshua.chokepoint.data.firestore.FirestoreRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.joshua.chokepoint.data.mqtt.MqttRepository

class DevicesViewModel(
    private val repository: FirestoreRepository,
    private val mqttRepository: MqttRepository // Add this
) : ViewModel() {

    val devices: StateFlow<List<FirestoreRepository.Device>> = repository.observeDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addDevice(name: String) {
        viewModelScope.launch {
            repository.addDevice(name)
        }
    }

    fun renameDevice(deviceId: String, newName: String) {
        viewModelScope.launch {
            repository.updateDeviceName(deviceId, newName)
        }
    }

    fun removeDevice(deviceId: String) {
        viewModelScope.launch {
            repository.removeDevice(deviceId)
        }
    }

    fun clearDeviceHistory(deviceId: String) {
        viewModelScope.launch {
            repository.clearDeviceHistory(
                deviceId = deviceId,
                onSuccess = { /* Handle success if needed, e.g. show toast via effect */ },
                onError = { /* Handle error */ }
            )
        }
    }

    fun factoryResetDevice(deviceId: String) {
        viewModelScope.launch {
            mqttRepository.publishCommand(deviceId, "{\"cmd\": \"reset_wifi\"}")
            // Maybe remove from list locally or show toast?
            // The device will restart and stop sending data.
        }
    }
}
