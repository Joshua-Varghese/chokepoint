package com.joshua.chokepoint.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshua.chokepoint.data.repository.SettingsRepository
import com.joshua.chokepoint.data.repository.NotificationSensitivity
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val notificationsEnabled: StateFlow<Boolean> = settingsRepository.notificationsEnabled
    val sensitivity: StateFlow<NotificationSensitivity> = settingsRepository.sensitivity

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
        }
    }

    fun setSensitivity(level: NotificationSensitivity) {
        viewModelScope.launch {
            settingsRepository.setSensitivity(level)
        }
    }
}
