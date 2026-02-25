package com.joshua.chokepoint.data.repository

import android.app.Activity
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.joshua.chokepoint.BuildConfig
import com.joshua.chokepoint.data.model.Product
import com.razorpay.Checkout
import org.json.JSONObject
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CheckoutHelper(private val activity: Activity) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        Checkout.preload(activity.applicationContext)
    }

    fun startPayment(
        product: Product,
        variantName: String?,
        totalPrice: Double,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            onError("User not logged in")
            return
        }

        // 1. Create Order in Firestore (Pending)
        val orderId = "ord_${System.currentTimeMillis()}"
        val orderData = hashMapOf(
            "userId" to user.uid,
            "userEmail" to (user.email ?: ""),
            "items" to listOf(
                mapOf(
                    "productId" to product.id,
                    "name" to product.name,
                    "variant" to (variantName ?: "Standard"),
                    "price" to totalPrice,
                    "quantity" to 1,
                    "image" to product.imageUrl
                )
            ),
            "amount" to totalPrice,
            "status" to "pending",
            "createdAt" to Date(),
            "platform" to "android"
        )

        db.collection("orders").document(orderId).set(orderData)
            .addOnSuccessListener {
                // 2. Launch Razorpay
                launchRazorpay(orderId, totalPrice, user.email ?: "", onSuccess, onError)
            }
            .addOnFailureListener { e ->
                onError("Failed to create order: ${e.message}")
            }
    }

    private fun launchRazorpay(
        orderId: String,
        amount: Double,
        email: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val checkout = Checkout()
        checkout.setKeyID(BuildConfig.RAZORPAY_KEY_ID)

        try {
            val options = JSONObject()
            options.put("name", "Chokepoint")
            options.put("description", "Order #$orderId")
            options.put("image", "https://chokepoint.io/logo.png") // Replace with actual logo if avail
            options.put("currency", "INR")
            options.put("amount", (amount * 100).toInt()) // Amount in paise
            val prefill = JSONObject()
            prefill.put("email", email)
            // prefill.put("contact", "9876543210") // Optional: Pass phone if available
            options.put("prefill", prefill)
            
            val retryObj = JSONObject()
            retryObj.put("enabled", true)
            retryObj.put("max_count", 4)
            options.put("retry", retryObj)

            checkout.open(activity, options)
            
            // Note: The actual callback comes via Activity.onPaymentSuccess/Error
            // We need a way to link that back here, or handle it in MainActivity
            // For now, we'll assume MainActivity handles the callback and updates Firestore
            
        } catch (e: Exception) {
            onError("Error starting checkout: ${e.message}")
        }
    }

    fun updateOrderStatus(orderId: String, paymentId: String?, status: String) {
        val updateData = hashMapOf<String, Any>(
            "status" to status
        )
        if (paymentId != null) {
            updateData["paymentId"] = paymentId
        }
        
        db.collection("orders").document(orderId).update(updateData)
            .addOnSuccessListener { 
                Log.d("CheckoutHelper", "Order $orderId updated to $status") 
            }
            .addOnFailureListener { e ->
                Log.e("CheckoutHelper", "Failed to update order $orderId", e)
            }
    }
}
