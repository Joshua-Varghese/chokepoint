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
    fun observeRecentReadings(limit: Int = 20): Flow<List<SensorData>> = callbackFlow {
        val listener = collection
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
                            // Map manually or use toObject if SensorData matches exactly
                            val co2 = doc.getDouble("co2") ?: 0.0
                            val nh3 = doc.getDouble("nh3") ?: 0.0
                            val smoke = doc.getDouble("smoke") ?: 0.0
                            val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                            
                            SensorData(co2, nh3, smoke, timestamp)
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
        collection.add(data)
            .addOnSuccessListener {
                Log.d("Firestore", "DocumentSnapshot added with ID: ${it.id}")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error adding document", e)
            }
    }
}
