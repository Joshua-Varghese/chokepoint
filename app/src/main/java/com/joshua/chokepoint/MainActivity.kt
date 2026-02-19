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
        Toast.makeText(this, "Payment Failed: $response", Toast.LENGTH_LONG).show()
    }
}
