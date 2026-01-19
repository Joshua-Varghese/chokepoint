package com.joshua.chokepoint.data.firestore

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.joshua.chokepoint.data.model.SensorData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("sensor_readings")

    /**
     * Observes the last [limit] readings in real-time.
     */
    /**
     * Observes the last [limit] readings for a specific device.
     */
    fun observeRecentReadings(deviceId: String, limit: Int = 20): Flow<List<SensorData>> = callbackFlow {
        if (deviceId.isEmpty()) {
            close(IllegalArgumentException("DeviceId cannot be empty"))
            return@callbackFlow
        }

        val listener = db.collection("devices").document(deviceId).collection("readings")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("Firestore", "Listen failed.", e)
                    close(e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val readings = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(SensorData::class.java)
                        } catch (e: Exception) {
                            Log.e("Firestore", "Error parsing doc ${doc.id}", e)
                            null
                        }
                    }
                    trySend(readings)
                }
            }

        awaitClose { listener.remove() }
    }

    fun saveSensorData(data: SensorData) {
        if (data.deviceId.isEmpty()) {
            Log.w("Firestore", "Skipping save: No Device ID")
            return
        }

        db.collection("devices")
            .document(data.deviceId)
            .collection("readings")
            .add(data)
            .addOnSuccessListener {
                Log.d("Firestore", "Reading saved to devices/${data.deviceId}/readings/${it.id}")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error adding document", e)
            }
    }
}
