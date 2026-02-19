package com.joshua.chokepoint.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Sensitivity Enum
enum class NotificationSensitivity {
    ALL,        // All alerts (Smoke, Gas, CO2)
    CRITICAL    // Critical Only (Focus on CO2 as requested)
}

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _sensitivity = MutableStateFlow(
        NotificationSensitivity.valueOf(
            prefs.getString(KEY_SENSITIVITY, NotificationSensitivity.ALL.name) ?: NotificationSensitivity.ALL.name
        )
    )
    val sensitivity: StateFlow<NotificationSensitivity> = _sensitivity.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            KEY_NOTIFICATIONS_ENABLED -> {
                _notificationsEnabled.value = sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
            }
            KEY_SENSITIVITY -> {
                val senseName = sharedPreferences.getString(KEY_SENSITIVITY, NotificationSensitivity.ALL.name)
                _sensitivity.value = NotificationSensitivity.valueOf(senseName ?: NotificationSensitivity.ALL.name)
            }
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun setSensitivity(sensitivity: NotificationSensitivity) {
        prefs.edit().putString(KEY_SENSITIVITY, sensitivity.name).apply()
    }

    companion object {
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_SENSITIVITY = "notification_sensitivity"
    }
}
