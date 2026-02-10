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
        // For backwards compatibility during transition if needed
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
        
        // 1. Check Global Role
        devicesCollection.document(deviceId).get().addOnSuccessListener { doc ->
            val adminId = doc.getString("adminId")
            
            if (adminId == userId) {
                // User IS Admin -> Full Nuke
                Log.d("Firestore", "User is Admin. Unclaiming and Wiping device $deviceId")
                
                // A. Unclaim (Remove adminId, shareCode, reset name)
                devicesCollection.document(deviceId).update(
                    mapOf(
                        "adminId" to null,
                        "shareCode" to null,
                        "name" to "Unclaimed Device"
                    )
                )
                
                // B. Wipe Data
                clearDeviceHistory(deviceId, {}, {})
            }
            
            // 2. Remove from Private List (Always do this)
            db.collection("users").document(userId)
                .collection("devices").document(deviceId)
                .delete()
        }
    }

    fun clearDeviceHistory(deviceId: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        // This requires deleting all documents in the subcollection.
        // Firestore does not support deleting a whole collection client-side efficiently without listing.
        // We will list and delete in batches.
        val readingsRef = db.collection("devices").document(deviceId).collection("readings")
        
        readingsRef.get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onError(e) }
            }
            .addOnFailureListener { e -> onError(e) }
    }

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

    fun createUserProfile(uid: String, email: String, name: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val user = hashMapOf(
            "email" to email,
            "name" to name,
            "id" to uid,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("users").document(uid)
            .set(user)
            .addOnSuccessListener { 
                Log.d("Firestore", "User profile created for $uid")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error creating user profile", e)
                onFailure(e)
            }
    }
    fun syncUserProfile(uid: String, email: String, name: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userRef = db.collection("users").document(uid)
        
        userRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // Update name if missing or empty
                    val existingName = snapshot.getString("name")
                    if (existingName.isNullOrBlank() && name.isNotBlank()) {
                         userRef.update("name", name)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { onFailure(it) }
                    } else {
                        // Already exists and has name (or we have no better name), just proceed
                        onSuccess()
                    }
                } else {
                    // Create new profile
                    val newUser = hashMapOf(
                        "email" to email,
                        "name" to name.ifBlank { "User ${uid.take(5)}" },
                        "id" to uid,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )
                    userRef.set(newUser)
                        .addOnSuccessListener { 
                            Log.d("Firestore", "User profile synced for $uid")
                            onSuccess() 
                        }
                        .addOnFailureListener { 
                            Log.e("Firestore", "Error syncing user profile", it)
                            onFailure(it) 
                        }
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun getUserProfile(uid: String, onSuccess: (String?) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    onSuccess(document.getString("name"))
                } else {
                    onSuccess(null)
                }
            }
            .addOnFailureListener { onFailure(it) }
    }
}
