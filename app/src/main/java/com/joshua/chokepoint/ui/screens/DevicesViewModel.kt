package com.joshua.chokepoint.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshua.chokepoint.data.firestore.FirestoreRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow // Add this
import kotlinx.coroutines.launch

import com.joshua.chokepoint.data.mqtt.MqttRepository

import com.joshua.chokepoint.data.discovery.DiscoveryRepository
        
class DevicesViewModel(
    private val repository: FirestoreRepository,
    private val mqttRepository: MqttRepository,
    private val discoveryRepository: DiscoveryRepository
) : ViewModel() {

    val devices: StateFlow<List<FirestoreRepository.Device>> = repository.observeDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _verificationState = kotlinx.coroutines.flow.MutableStateFlow<VerificationState>(VerificationState.Idle)
    val verificationState = _verificationState.asStateFlow()

    enum class VerificationState {
        Idle, Verifying, Success, Failed
    }

    fun resetVerification() {
        _verificationState.value = VerificationState.Idle
    }

    fun verifyDevice(deviceId: String) {
        if (deviceId.isBlank()) return
        
        viewModelScope.launch {
            _verificationState.value = VerificationState.Verifying
            
            // Clean ID just in case
            val cleanId = deviceId.trim()
            
            discoveryRepository.verifyDevice(cleanId).collect { isFound ->
                if (isFound) {
                    _verificationState.value = VerificationState.Success
                } else {
                    _verificationState.value = VerificationState.Failed
                }
            }
        }
    }

    // ... existing methods ...

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

    fun deleteDeviceFully(deviceId: String) {
        viewModelScope.launch {
            // 1. Try to factory reset the device remotely
            try {
                mqttRepository.publishCommand(deviceId, "{\"cmd\": \"reset_wifi\"}")
            } catch (e: Exception) {
                // Ignore errors if device is already offline
            }
            
            // 2. Remove from Firestore (Nuclear option)
            repository.removeDevice(deviceId)
        }
    }
}
