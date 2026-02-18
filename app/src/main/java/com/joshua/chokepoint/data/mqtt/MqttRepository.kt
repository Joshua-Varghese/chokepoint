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

    private val _sensorData = MutableStateFlow(SensorData())
    val sensorData: StateFlow<SensorData> = _sensorData.asStateFlow()

    private var mqttClient: MqttAndroidClient? = null

    fun connect() {
        if (mqttClient != null && mqttClient!!.isConnected) return

        try {
            val clientId = MqttClient.generateClientId()
            
            // Use Secure Config from local.properties
            val serverUri = BuildConfig.MQTT_BROKER_URL.ifEmpty { 
                "tcp://puffin.rmq2.cloudamqp.com:1883" // Fallback only if local.properties failed, but should alert user
            }
            
            mqttClient = MqttAndroidClient(context, serverUri, clientId)

            val options = MqttConnectOptions().apply {
                userName = BuildConfig.MQTT_USERNAME
                password = BuildConfig.MQTT_PASSWORD.toCharArray()
                isAutomaticReconnect = true
                isCleanSession = true
            }

            mqttClient?.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                     Log.d("MQTT", "Connection Complete. Reconnect: $reconnect")
                     _isConnected.value = true
                     if(reconnect) subscribeToTopics()
                }

                override fun connectionLost(cause: Throwable?) {
                    Log.w("MQTT", "Connection lost")
                    _isConnected.value = false
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    try {
                        val payload = String(message?.payload ?: ByteArray(0))
                        Log.d("MQTT", "Message received: $payload")
                        parsePayload(payload)
                    } catch (e: Exception) {
                        Log.e("MQTT", "Error parsing message", e)
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    // Not used for subscribing
                }
            })

            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTT", "Connected to broker")
                    _isConnected.value = true
                    subscribeToTopics()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MQTT", "Failed to connect: ${exception?.message}", exception)
                    exception?.printStackTrace()
                    _isConnected.value = false
                }
            })

        } catch (e: Exception) {
            Log.e("MQTT", "CRITICAL MQTT ERROR", e)
        }
    }

    private fun subscribeToTopics() {
        try {
            // Subscribe to all devices' data
            val topic = "chokepoint/devices/+/data"
            mqttClient?.subscribe(topic, 0, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTT", "Subscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MQTT", "Failed to subscribe to $topic", exception)
                }
            })
        } catch (e: MqttException) {
            Log.e("MQTT", "Exception during subscribe", e)
        }
    }

    private var claimedDeviceIds: Set<String> = emptySet()

    // Initialize in a coroutine scope (e.g. from ViewModel or by making MqttRepository a helper)
    // However, since MqttRepository is just a class here, we need to start observing somewhere.
    // For simplicity given the current architecture, we'll expose a function to start filtering
    // or rely on the fact that we can launch a coroutine if we had a scope.
    // To avoid architectural refractory, we'll use a listener approach or just check against cache.
    // BETTER APPROACH: We'll launch a coroutine in the constructor/init block using GlobalScope (careful) 
    // or pass a scope. Given this is a prototype, GlobalScope or a dedicated scope is acceptable.
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
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        // Check Settings First
        if (!settingsRepository.notificationsEnabled.value) return
        val sensitivity = settingsRepository.sensitivity.value

        val issues = mutableListOf<String>()
        var isSmoke = false
        var isAir = false

        // SMOKE CHECK (Only if ALL is selected, per user request to prioritize CO2 in Critical)
        // User asked: "Critical showing CO2 not smoke". 
        // So if Sensitivity == ALL, we show Smoke + CO2.
        // If Sensitivity == CRITICAL, we show CO2 only.
        if (sensitivity == com.joshua.chokepoint.data.repository.NotificationSensitivity.ALL) {
            if (data.smoke > 0.5) {
                issues.add("Smoke Detected")
                isSmoke = true
            }
        }

        // CO2/GAS CHECK (Always check if notifications are enabled)
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

            triggerNotification(notificationManager, 1, title, content)
        }
    }

    private fun triggerNotification(manager: android.app.NotificationManager, id: Int, title: String, content: String) {
        // Create an explicit intent for an Activity in your app
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
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Replace with app icon in real app
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(false) // ALERT EVERY TIME
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) // OPEN APP ON CLICK
        
        // Rate limit: Only notify every 5 seconds (for testing) to avoid spam
        val lastTime = lastNotificationTime[id] ?: 0L
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTime > 5000) {
             Log.d("MQTT", "Triggering Notification ID: $id - Title: $title")
             manager.notify(id, builder.build())
             lastNotificationTime[id] = currentTime
        } else {
             Log.d("MQTT", "Rate Limited Notification ID: $id")
        }
    }

    private val lastNotificationTime = mutableMapOf<Int, Long>()

    init {
        createNotificationChannel()
        // Start watching for allowed devices immediately
        scope.launch {
            firestoreRepository.observeDevices().collect { devices ->
                claimedDeviceIds = devices.map { it.id }.toSet()
                Log.d("MQTT", "Updated Allowed Devices: $claimedDeviceIds")
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
                // Fill in defaults for restored fields if not available in MQTT payload
                gasRaw = json.optInt("gas_raw", 0),
                airQuality = json.optString("air_quality", "Unknown")
            )
            _sensorData.value = data
            
            // CHECK THRESHOLDS
            checkThresholds(data)
            
            // Save to Firestore history (sub-collection)
            firestoreRepository.saveSensorData(data)
            
        } catch (e: Exception) {
            Log.e("MQTT", "JSON Parsing error", e)
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
