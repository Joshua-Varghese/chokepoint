package com.joshua.chokepoint.data.mqtt

import android.content.Context
import android.util.Log
import com.joshua.chokepoint.BuildConfig
import com.joshua.chokepoint.data.model.SensorData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import info.mqtt.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.json.JSONObject

import com.joshua.chokepoint.data.firestore.FirestoreRepository

class MqttRepository(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository
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

    private fun parsePayload(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            
            // Extract Firmware Data
            val deviceId = json.optString("device_id", "unknown")
            val gasRaw = json.optInt("gas_raw", 0)
            val airQuality = json.optString("air_quality", "Unknown")
            val timestamp = json.optLong("timestamp", System.currentTimeMillis())

            // Legacy support
            val co2 = json.optDouble("co2", 0.0)
            val nh3 = json.optDouble("nh3", 0.0)
            val smoke = json.optDouble("smoke", 0.0)
            
            val data = SensorData(
                co2 = co2, nh3 = nh3, smoke = smoke,
                deviceId = deviceId, gasRaw = gasRaw, airQuality = airQuality, timestamp = timestamp
            )
            _sensorData.value = data
            
            // Save to Firestore for history
            firestoreRepository.saveSensorData(data)
            
        } catch (e: Exception) {
            Log.e("MQTT", "JSON Parsing error", e)
        }
    }

    fun disconnect() {
        try {
            mqttClient?.disconnect()
        } catch (e: MqttException) {
            Log.e("MQTT", "Disconnect error", e)
        }
    }
}
