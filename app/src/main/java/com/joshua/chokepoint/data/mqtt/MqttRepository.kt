package com.joshua.chokepoint.data.mqtt

import android.content.Context
import android.util.Log
import com.joshua.chokepoint.BuildConfig
import com.joshua.chokepoint.data.model.SensorData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import info.mqtt.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.json.JSONObject

import com.joshua.chokepoint.data.firestore.FirestoreRepository

class MqttRepository(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository,
    private val settingsRepository: com.joshua.chokepoint.data.repository.SettingsRepository
) {


    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // Map of DeviceID -> SensorData
    private val _deviceReadings = MutableStateFlow<Map<String, SensorData>>(emptyMap())
    val deviceReadings: StateFlow<Map<String, SensorData>> = _deviceReadings.asStateFlow()

    private var mqttClient: MqttAndroidClient? = null
    private var subscribedTopics = mutableSetOf<String>()

    fun connect() {
        if (mqttClient != null && mqttClient!!.isConnected) return

        try {
            val clientId = MqttClient.generateClientId()
            
            val serverUri = BuildConfig.MQTT_BROKER_URL.ifEmpty { 
                "tcp://puffin.rmq2.cloudamqp.com:1883"
            }
            
            mqttClient = MqttAndroidClient(context, serverUri, clientId)

            val options = MqttConnectOptions().apply {
                userName = BuildConfig.MQTT_USERNAME
                password = BuildConfig.MQTT_PASSWORD.toCharArray()
                isAutomaticReconnect = true
                isCleanSession = false // Keep session to receive missed messages? Maybe true for cleaner start.
            }

            mqttClient?.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                     Log.d("MQTT", "Connection Complete. Reconnect: $reconnect")
                     _isConnected.value = true
                     // Resubscribe to all needed topics
                     resubscribeAll()
                }

                override fun connectionLost(cause: Throwable?) {
                    Log.w("MQTT", "Connection lost")
                    _isConnected.value = false
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    try {
                        val payload = String(message?.payload ?: ByteArray(0))
                        // Log.d("MQTT", "Message received: $payload")
                        parsePayload(payload)
                    } catch (e: Exception) {
                        Log.e("MQTT", "Error parsing message", e)
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) { }
            })

            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTT", "Connected to broker")
                    _isConnected.value = true
                    resubscribeAll()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MQTT", "Failed to connect", exception)
                    _isConnected.value = false
                }
            })

        } catch (e: Exception) {
            Log.e("MQTT", "CRITICAL MQTT ERROR", e)
        }
    }

    // Called when connected or when device list changes
    private fun resubscribeAll() {
        if (mqttClient == null || !mqttClient!!.isConnected) return

        subscribedTopics.forEach { topic ->
             subscribeToTopic(topic)
        }
    }

    private fun subscribeToTopic(topic: String) {
        if (mqttClient?.isConnected != true) {
            Log.w("MQTT", "Cannot subscribe to $topic, client not connected yet")
            return
        }
        try {
            mqttClient?.subscribe(topic, 0, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTT", "Subscribed to $topic")
                }
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MQTT", "Failed to subscribe to $topic", exception)
                }
            })
        } catch (e: Exception) {
            Log.e("MQTT", "Exception subscribing", e)
        }
    }
    
    private fun unsubscribeFromTopic(topic: String) {
        if (mqttClient?.isConnected != true) return
        try {
             mqttClient?.unsubscribe(topic)
             Log.d("MQTT", "Unsubscribed from $topic")
        } catch (e: Exception) {
            Log.e("MQTT", "Exception unsubscribing", e)
        }
    }

    // Dynamic Subscription Management
    private fun updateSubscriptions(devices: List<FirestoreRepository.Device>) {
        val newTopics = devices.map { "chokepoint/devices/${it.id}/data" }.toSet()
        
        // Find topics to remove
        val toRemove = subscribedTopics - newTopics
        toRemove.forEach { unsubscribeFromTopic(it) }
        
        // Find topics to add
        val toAdd = newTopics - subscribedTopics
        toAdd.forEach { subscribeToTopic(it) }

        subscribedTopics.clear()
        subscribedTopics.addAll(newTopics)
        Log.d("MQTT", "Updated subscriptions. Active: $subscribedTopics")

        // Fetch Last Known Data for these devices
        scope.launch {
            devices.forEach { device ->
                // Check if we already have data (don't overwrite live data)
                if (!_deviceReadings.value.containsKey(device.id)) {
                    val lastData = firestoreRepository.getLastReading(device.id)
                    if (lastData != null) {
                        // Apply Fallback Parsing logic here too just in case old data is raw
                        // But Firestore data should be clean if saved nicely.
                        // Actually, safety check: ensure deviceId matches
                        val cleanData = lastData.copy(deviceId = device.id) 
                        
                        // Update Map safely
                        val currentMap = _deviceReadings.value
                        if (!currentMap.containsKey(device.id)) {
                             _deviceReadings.value = currentMap + (device.id to cleanData)
                             Log.d("MQTT", "Restored last known data for ${device.id}")
                        }
                    }
                }
            }
        }
    }

    private var claimedDeviceIds: Set<String> = emptySet()
    
    // Lifecycle Scope (Global for Singleton)
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Safety Alerts"
            val descriptionText = "Critical safety warnings for air quality"
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel("SAFETY_ALERTS", name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager: android.app.NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkThresholds(data: SensorData) {
        // ... (Same Logic) ...
        // Only verify if this device is allowed? Subscription handles that now.
        // But double check against claimedDeviceIds just in case.
        if (data.deviceId !in claimedDeviceIds) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        // Check Settings First
        if (!settingsRepository.notificationsEnabled.value) return
        val sensitivity = settingsRepository.sensitivity.value

        val issues = mutableListOf<String>()
        var isSmoke = false
        var isAir = false

        if (sensitivity == com.joshua.chokepoint.data.repository.NotificationSensitivity.ALL) {
            if (data.smoke > 0.5) {
                issues.add("Smoke Detected")
                isSmoke = true
            }
        }

        if (data.gasRaw > 2500 || data.co2 > 1500) {
             issues.add("High CO2/Gas")
             isAir = true
        }

        if (issues.isNotEmpty()) {
            val title = if (isSmoke && isAir) "🔥 CRITICAL: Smoke & Gas!" 
                        else if (isSmoke) "🔥 Smoke Detected!"
                        else "⚠️ Dangerous Air Quality"
            
            val content = if (isSmoke) "Evacuate immediately. Smoke usage detected on ${data.deviceId}."
                          else "Air quality is unsafe (${data.gasRaw}). Check ventilation."

            triggerNotification(notificationManager, data.deviceId.hashCode(), title, content)
        }
    }

    private fun triggerNotification(manager: android.app.NotificationManager, id: Int, title: String, content: String) {
        val intent = android.content.Intent(context, com.joshua.chokepoint.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: android.app.PendingIntent = android.app.PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = androidx.core.app.NotificationCompat.Builder(context, "SAFETY_ALERTS")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(false)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        
        val lastTime = lastNotificationTime[id] ?: 0L
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTime > 5000) {
             Log.d("MQTT", "Triggering Notification ID: $id - Title: $title")
             manager.notify(id, builder.build())
             lastNotificationTime[id] = currentTime
        }
    }

    private val lastNotificationTime = mutableMapOf<Int, Long>()

    init {
        createNotificationChannel()
        scope.launch {
            firestoreRepository.observeDevices().collect { devices ->
                claimedDeviceIds = devices.map { it.id }.toSet()
                updateSubscriptions(devices)
            }
        }
    }

    private fun parsePayload(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            val deviceId = json.optString("device_id", "unknown_device")

            val co2 = json.optDouble("co2", 0.0)
            val nh3 = json.optDouble("nh3", 0.0)
            val smoke = json.optDouble("smoke", 0.0)
            
            val data = SensorData(
                co2 = co2,
                nh3 = nh3,
                smoke = smoke,
                deviceId = deviceId,
                timestamp = System.currentTimeMillis(),
                gasRaw = json.optInt("gas_raw", co2.toInt()), 
                airQuality = json.optString("air_quality", calculateAirQuality(co2, smoke))
            )
            
            // Update Map
            _deviceReadings.value = _deviceReadings.value + (deviceId to data)
            
            checkThresholds(data)
            firestoreRepository.saveSensorData(data)
            
        } catch (e: Exception) {
            Log.e("MQTT", "JSON Parsing error", e)
        }
    }

    private fun calculateAirQuality(co2: Double, smoke: Double): String {
        return when {
            smoke > 0.5 -> "Hazardous" // Smoke detected
            co2 > 1000 -> "Poor"
            co2 > 400 -> "Moderate"
            else -> "Good"
        }
    }



    fun publishCommand(deviceId: String, command: String) {
        if (mqttClient == null || !mqttClient!!.isConnected) return

        val topic = "chokepoint/devices/$deviceId/cmd"
        val message = MqttMessage(command.toByteArray())
        message.qos = 1
        message.isRetained = false

        try {
            mqttClient?.publish(topic, message)
            Log.d("MQTT", "Published command $command to $topic")
        } catch (e: MqttException) {
            Log.e("MQTT", "Error publishing command", e)
        }
    }

    fun disconnect() {
        try {
            mqttClient?.disconnect()
        } catch (e: MqttException) {
            Log.e("MQTT", "Disconnect error", e)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: MqttRepository? = null

        fun getInstance(context: Context): MqttRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val firestore = FirestoreRepository()
                    val settings = com.joshua.chokepoint.data.repository.SettingsRepository(context.applicationContext)
                    MqttRepository(context.applicationContext, firestore, settings).also { INSTANCE = it }
                }
            }
        }
    }
}
