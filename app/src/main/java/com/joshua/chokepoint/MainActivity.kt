package com.joshua.chokepoint

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseApp
import com.joshua.chokepoint.ui.screens.LoginScreen
import com.joshua.chokepoint.ui.theme.ChokepointandroidTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.razorpay.PaymentResultListener
import com.razorpay.Checkout
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity(), PaymentResultListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)

        setContent {
            ChokepointandroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    com.joshua.chokepoint.ui.navigation.AppNavigation(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        // PermissionGuard(this).checkAndRequestPermissions() // Removed as class is missing
        checkForNewRelease()
    }

    private fun checkForNewRelease() {
        val firebaseAppDistribution =
            com.google.firebase.appdistribution.FirebaseAppDistribution.getInstance()
        firebaseAppDistribution.updateIfNewReleaseAvailable()
            .addOnProgressListener { updateProgress ->
                // customized progress UI
            }
            .addOnFailureListener { e ->
                // handle error
            }
    }

    override fun onPaymentSuccess(razorpayPaymentID: String?) {
        Toast.makeText(this, "Payment Successful: $razorpayPaymentID", Toast.LENGTH_LONG).show()
        // Here you would optimally communicate back to the ViewModel/CheckoutHelper
        // Since we don't have a direct reference to the active helper instance easily,
        // we'll rely on the user seeing the toast and the backend/webhook verifying the payment
        // or broadcasting the success.
        // For a tighter integration, you'd use a shared ViewModel or EventBus.
    }

    override fun onPaymentError(code: Int, response: String?) {
        try {
            if (code == Checkout.PAYMENT_CANCELED || code == Checkout.TLS_ERROR) {
                 Toast.makeText(this, "Payment Cancelled", Toast.LENGTH_SHORT).show()
                 return
            }
            
            // Try to parse detailed error
            val errorMsg = try {
                 val json = org.json.JSONObject(response ?: "")
                 val errorObj = json.optJSONObject("error")
                 errorObj?.optString("description") ?: response
            } catch (e: Exception) {
                 response
            }
            
            Toast.makeText(this, "Payment Failed", Toast.LENGTH_LONG).show() // Masking details as requested
            // Log the actual error for debugging
             android.util.Log.e("RazorpayError", "Code: $code, Response: $response")
        } catch (e: Exception) {
            Toast.makeText(this, "Payment Failed", Toast.LENGTH_SHORT).show()
        }
    }
}
