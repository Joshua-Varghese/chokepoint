package com.joshua.chokepoint.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.joshua.chokepoint.MainActivity
import com.joshua.chokepoint.R
import com.joshua.chokepoint.data.mqtt.MqttRepository

class SafetyService : Service() {

    private val CHANNEL_ID = "SAFETY_MONITOR_CHANNEL"

    override fun onCreate() {
        super.onCreate()
        Log.d("SafetyService", "Service Created")
        startForegroundService()
        
        // Ensure MQTT is connected
        MqttRepository.getInstance(this).connect()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SafetyService", "Service Started")
        // If the system kills the service, restart it with the last intent
        return START_STICKY
    }

    private fun startForegroundService() {
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Chokepoint Monitoring Active")
            .setContentText("Listening for air quality alerts...")
            .setSmallIcon(android.R.drawable.ic_menu_compass) // Use a generic icon for now
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Persistent
            .build()

        // ID must not be 0
        startForeground(999, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Background Monitoring Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
