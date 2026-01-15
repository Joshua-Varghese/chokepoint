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
                            
                            SensorData(
                                co2 = co2, 
                                nh3 = nh3, 
                                smoke = smoke, 
                                timestamp = timestamp
                            )
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

    // --- Device Management ---
    private val devicesCollection = db.collection("devices")

    data class Device(
        val id: String = "", 
        val name: String = "", 
        val isActive: Boolean = true,
        val role: String = "viewer", // admin or viewer
        val shareCode: String = ""
    )

    fun observeDevices(): Flow<List<Device>> = callbackFlow {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val userDevicesCollection = db.collection("users").document(userId).collection("devices")
        
        val listener = userDevicesCollection.addSnapshotListener { snapshot, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val devices = snapshot.documents.mapNotNull { doc ->
                    try {
                        Device(
                            id = doc.getString("id") ?: doc.id,
                            name = doc.getString("name") ?: "Unknown Device",
                            isActive = true,
                            role = doc.getString("role") ?: "viewer",
                            shareCode = doc.getString("shareCode") ?: ""
                        )
                    } catch (e: Exception) { null }
                }
                trySend(devices)
            }
        }
        awaitClose { listener.remove() }
    }

    fun addDevice(name: String) {
        // Deprecated: Use claimDevice instead
    }

    fun claimDevice(deviceId: String, name: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            onError(Exception("User not logged in"))
            return
        }

        val batch = db.batch()
        
        // Generate a random 6-char Share Code (Uppercase Alphanumeric)
        val shareCode = (1..6).map { (('A'..'Z') + ('0'..'9')).random() }.joinToString("")

        // 1. Global Registry (Create if not exists)
        val globalDeviceRef = devicesCollection.document(deviceId)
        val globalData = hashMapOf(
            "name" to name, 
            "adminId" to userId,
            "shareCode" to shareCode, // New: Save Share Code
            "registeredAt" to com.google.firebase.Timestamp.now()
        )
        // Use Merge so we don't wipe existing data, but we DO update the shareCode if we are claiming
        batch.set(globalDeviceRef, globalData, com.google.firebase.firestore.SetOptions.merge())

        // 2. User Registry (Private to user)
        val userDeviceRef = db.collection("users").document(userId)
            .collection("devices").document(deviceId)
            
        val userDeviceData = hashMapOf(
            "id" to deviceId,
            "name" to name, 
            "role" to "admin",
            "shareCode" to shareCode, // Save here too for easy display
            "addedAt" to com.google.firebase.Timestamp.now()
        )
        batch.set(userDeviceRef, userDeviceData)

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }

    fun linkDevice(shareCode: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            onError(Exception("User not logged in"))
            return
        }
        
        // 1. Find device by Share Code
        devicesCollection.whereEqualTo("shareCode", shareCode).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    onError(Exception("Invalid Code"))
                    return@addOnSuccessListener
                }
                
                val doc = snapshot.documents.first()
                val deviceId = doc.id
                val deviceName = doc.getString("name") ?: "Shared Device"
                
                // 2. Add to User's private list as Viewer
                val userDeviceRef = db.collection("users").document(userId)
                    .collection("devices").document(deviceId)
                    
                val userDeviceData = hashMapOf(
                    "id" to deviceId,
                    "name" to deviceName, 
                    "role" to "viewer",
                    "addedAt" to com.google.firebase.Timestamp.now()
                )
                
                userDeviceRef.set(userDeviceData)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onError(e) }
            }
            .addOnFailureListener { e -> onError(e) }
    }

    fun updateDeviceName(deviceId: String, newName: String) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        // Update User's private nickname
        db.collection("users").document(userId)
            .collection("devices").document(deviceId)
            .update("name", newName)
            
        // Optionally update Global name if you are Admin? 
        // For now, let's keep private names private.
    }

    fun removeDevice(deviceId: String) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        db.collection("users").document(userId)
            .collection("devices").document(deviceId)
            .delete()
    }
}
