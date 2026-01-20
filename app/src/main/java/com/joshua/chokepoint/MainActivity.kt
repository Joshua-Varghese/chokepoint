package com.joshua.chokepoint

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.joshua.chokepoint.ui.screens.DashboardScreen
import com.joshua.chokepoint.ui.screens.ForgotPasswordScreen
import com.joshua.chokepoint.ui.screens.LandingScreen
import com.joshua.chokepoint.ui.screens.LoginScreen
import com.joshua.chokepoint.ui.screens.SignUpScreen
import com.joshua.chokepoint.ui.theme.ChokepointandroidTheme

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private var isLoading by mutableStateOf(false)
    private var onLoginSuccess: (() -> Unit)? = null

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d("GoogleSignIn", "Sign-in Intent Result OK. Processing data...")
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    Log.d("GoogleSignIn", "Google Account retrieved: ${account.email}. ID Token length: ${account.idToken?.length}")
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    isLoading = false
                    Log.e("GoogleSignIn", "Google sign-in failed. Status Code: ${e.statusCode}", e)
                    Toast.makeText(this, "Google Sign-In Error: ${e.statusCode}", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    isLoading = false
                    Log.e("GoogleSignIn", "Unknown sign-in error", e)
                    Toast.makeText(this, "Sign-In Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            } else {
                isLoading = false
                Log.w("GoogleSignIn", "Sign-in cancelled or failed. ResultCode: ${result.resultCode}")
                Toast.makeText(this, "Sign-In Cancelled", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupGoogleSignIn()

        setContent {
            ChokepointandroidTheme {
                val navController = rememberNavController()

                // Determine start destination
                val currentUser = auth.currentUser
                val startDest = if (currentUser != null) "dashboard" else "landing"

                NavHost(navController = navController, startDestination = startDest) {

                    composable("landing") {
                        LandingScreen(
                            onGetStartedClick = {
                                navController.navigate("login")
                            }
                        )
                    }

                    composable("login") {
                        LoginScreen(
                            isLoading = isLoading,
                            onLoginClick = { email, password ->
                                isLoading = true
                                signInWithEmail(email, password) { success ->
                                    isLoading = false
                                    if (success) {
                                        Toast.makeText(this@MainActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                                        navController.navigate("dashboard") {
                                            popUpTo("landing") { inclusive = true }
                                        }
                                    } else {
                                        Toast.makeText(this@MainActivity, "Login Failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onGoogleSignInClick = {
                                isLoading = true
                                onLoginSuccess = {
                                    Toast.makeText(this@MainActivity, "Google Login Successful!", Toast.LENGTH_SHORT).show()
                                    navController.navigate("dashboard") {
                                        popUpTo("landing") { inclusive = true }
                                    }
                                }
                                signInWithGoogle()
                            },
                            onForgotPasswordClick = {
                                navController.navigate("forgot_password")
                            },
                            onBackClick = {
                                navController.popBackStack()
                            },
                            onSignUpClick = {
                                navController.navigate("signup")
                            }
                        )
                    }

                    composable("signup") {
                        SignUpScreen(
                            isLoading = isLoading,
                            onSignUpClick = { email, password ->
                                isLoading = true
                                signUpWithEmail(email, password) { success, message ->
                                    isLoading = false
                                    if (success) {
                                        Toast.makeText(this@MainActivity, "Account Created!", Toast.LENGTH_SHORT).show()
                                        navController.navigate("dashboard") {
                                            popUpTo("landing") { inclusive = true }
                                        }
                                    } else {
                                        Toast.makeText(this@MainActivity, "Sign Up Failed: $message", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("forgot_password") {
                        ForgotPasswordScreen(
                            isLoading = isLoading,
                            onSendResetEmailClick = { email ->
                                isLoading = true
                                sendPasswordResetEmail(email) {
                                    isLoading = false
                                    Toast.makeText(this@MainActivity, "Reset link sent if email exists.", Toast.LENGTH_LONG).show()
                                    navController.popBackStack()
                                }
                            },
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("dashboard") {
                        val firestoreRepository = remember { com.joshua.chokepoint.data.firestore.FirestoreRepository() }
                        val repository = remember { com.joshua.chokepoint.data.mqtt.MqttRepository(applicationContext, firestoreRepository) }
                        val viewModel = remember { com.joshua.chokepoint.ui.screens.DashboardViewModel(repository) }
                        
                        // Collect state
                        val isConnected by viewModel.isConnected.collectAsState()
                        val sensorData by viewModel.sensorData.collectAsState()
                        
                        // Connect on launch
                        LaunchedEffect(Unit) {
                            viewModel.connect()
                        }
                        
                        // Disconnect on leave
                        DisposableEffect(Unit) {
                            onDispose {
                                viewModel.disconnect()
                            }
                        }

                        DashboardScreen(
                            sensorData = sensorData,
                            isConnected = isConnected,
                            onLogoutClick = {
                                viewModel.disconnect()
                                auth.signOut()
                                googleSignInClient.signOut()
                                navController.navigate("landing") {
                                    popUpTo("dashboard") { inclusive = true }
                                }
                            },
                            onHistoryClick = {
                                val id = sensorData.deviceId.ifEmpty { "unknown" }
                                navController.navigate("analytics/$id")
                            },
                            onMarketplaceClick = {
                                navController.navigate("marketplace")
                            },
                            onAddDeviceClick = {
                                navController.navigate("provisioning")
                            }
                        )
                    }

                    composable("provisioning") {
                         com.joshua.chokepoint.ui.screens.ProvisioningScreen(
                             onBackClick = {
                                 navController.popBackStack()
                             },
                             onProvisionComplete = {
                                 // Optionally navigate somewhere or just pop back
                                 navController.popBackStack()
                             }
                         )
                    }

                    composable("analytics/{deviceId}") { backStackEntry ->
                        val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
                        val firestoreRepository = remember { com.joshua.chokepoint.data.firestore.FirestoreRepository() }
                        com.joshua.chokepoint.ui.screens.AnalyticsScreen(
                            deviceId = deviceId,
                            repository = firestoreRepository,
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("marketplace") {
                        val marketplaceRepository = remember { com.joshua.chokepoint.data.repository.MarketplaceRepositoryImpl() }
                        val viewModel = remember { com.joshua.chokepoint.ui.screens.MarketplaceViewModel(marketplaceRepository) }
                        
                        com.joshua.chokepoint.ui.screens.MarketplaceScreen(
                            viewModel = viewModel,
                            onBackClick = {
                                navController.popBackStack()
                            },
                            onProductClick = { productId ->
                                navController.navigate("product_detail/$productId")
                            },
                            onCartClick = {
                                navController.navigate("cart")
                            }
                        )
                    }

                    composable("cart") {
                        val cartRepository = remember { com.joshua.chokepoint.data.repository.CartRepositoryImpl() }
                        com.joshua.chokepoint.ui.screens.CartScreen(
                            repository = cartRepository,
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("product_detail/{productId}") { backStackEntry ->
                        val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
                        val marketplaceRepository = remember { com.joshua.chokepoint.data.repository.MarketplaceRepositoryImpl() }
                        val cartRepository = remember { com.joshua.chokepoint.data.repository.CartRepositoryImpl() }
                        
                        com.joshua.chokepoint.ui.screens.ProductDetailScreen(
                            productId = productId,
                            repository = marketplaceRepository,
                            cartRepository = cartRepository,
                            onBackClick = {
                                navController.popBackStack()
                            },
                            onCartClick = {
                                navController.navigate("cart")
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            Log.d("Auth", "Already logged in: ${user.email}")
        }
    }

    private fun setupGoogleSignIn() {
        auth = FirebaseAuth.getInstance()

        if (BuildConfig.DEFAULT_WEB_CLIENT_ID.isEmpty() || BuildConfig.DEFAULT_WEB_CLIENT_ID == "null") {
            Log.e("Auth", "Web Client ID is not set! Check local.properties.")
            Toast.makeText(this, "Config Error: Web Client ID missing", Toast.LENGTH_LONG).show()
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.DEFAULT_WEB_CLIENT_ID)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signInWithGoogle() {
        val intent = googleSignInClient.signInIntent
        signInLauncher.launch(intent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                isLoading = false
                if (task.isSuccessful) {
                    Log.d("Auth", "Login success: ${auth.currentUser?.email}")
                    onLoginSuccess?.invoke()
                } else {
                    Log.e("Auth", "Firebase auth failed", task.exception)
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signInWithEmail(email: String, pass: String, onResult: (Boolean) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    onResult(true)
                } else {
                    Log.w("Auth", "signInWithEmail:failure", task.exception)
                    onResult(false)
                }
            }
    }

    private fun signUpWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    Log.w("Auth", "createUserWithEmail:failure", task.exception)
                    onResult(false, task.exception?.localizedMessage)
                }
            }
    }

    private fun sendPasswordResetEmail(email: String, onComplete: () -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("Auth", "Email sent.")
                }
                onComplete()
            }
    }
}
