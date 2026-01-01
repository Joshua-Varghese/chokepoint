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

class MqttRepository(private val context: Context) {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _sensorData = MutableStateFlow(SensorData())
    val sensorData: StateFlow<SensorData> = _sensorData.asStateFlow()

    private var mqttClient: MqttAndroidClient? = null

    fun connect() {
        if (mqttClient != null && mqttClient!!.isConnected) return

        val serverUri = BuildConfig.MQTT_BROKER_URL
        if (serverUri.isEmpty() || serverUri == "null") {
            Log.e("MQTT", "Broker URL is missing in BuildConfig")
            return
        }

        try {
            val clientId = MqttClient.generateClientId()
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
            mqttClient?.subscribe("sensors/data", 0, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTT", "Subscribed to sensors/data")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MQTT", "Failed to subscribe", exception)
                }
            })
        } catch (e: MqttException) {
            Log.e("MQTT", "Exception during subscribe", e)
        }
    }

    private fun parsePayload(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            val co2 = json.optDouble("co2", 0.0)
            val nh3 = json.optDouble("nh3", 0.0)
            val smoke = json.optDouble("smoke", 0.0)
            
            _sensorData.value = SensorData(co2, nh3, smoke)
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
