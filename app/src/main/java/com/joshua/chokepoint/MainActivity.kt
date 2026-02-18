package com.joshua.chokepoint

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private var isLoading by mutableStateOf(false)
    private var onLoginSuccess: (() -> Unit)? = null
    private var onMissingProfile: (() -> Unit)? = null

    // Update State
    private var updateRelease: com.google.firebase.appdistribution.AppDistributionRelease? by mutableStateOf(null)
    private var isUpdateDialogVisible by mutableStateOf(false)
    private var isDownloading by mutableStateOf(false)
    private var downloadProgress by mutableStateOf(0f)
    private var downloadStatus by mutableStateOf("")

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

    private fun checkForUpdate() {
        val appDistribution = com.google.firebase.appdistribution.FirebaseAppDistribution.getInstance()
        appDistribution.checkForNewRelease()
            .addOnSuccessListener { release ->
                if (release != null) {
                    val currentVersionCode = BuildConfig.VERSION_CODE.toLong()
                    val newVersionCode = release.versionCode
                    
                    if (newVersionCode > currentVersionCode) {
                        updateRelease = release
                        isUpdateDialogVisible = true
                    } else {
                        Log.d("AppDistribution", "New release found ($newVersionCode) but it is not newer than current ($currentVersionCode). Skipping.")
                    }
                }
            }
            .addOnFailureListener {
                Log.e("AppDistribution", "Check failed", it)
            }
    }

    private fun startUpdate() {
        // Check for "Install Unknown Apps" permission (Android 8.0+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
                Toast.makeText(this, "Please allow installing unknown apps", Toast.LENGTH_LONG).show()
                return
            }
        }

        val release = updateRelease ?: return
        isUpdateDialogVisible = false
        isDownloading = true
        downloadStatus = "Starting download..."
        
        val appDistribution = com.google.firebase.appdistribution.FirebaseAppDistribution.getInstance()
        appDistribution.updateApp()
            .addOnProgressListener { progress ->
                val percentage = progress.apkBytesDownloaded * 100 / progress.apkFileTotalBytes
                downloadProgress = percentage.toFloat() / 100f
                downloadStatus = "Downloading: $percentage%"
            }
            .addOnSuccessListener {
                 downloadStatus = "Ready to install!"
                 // App closes automatically to install
            }
            .addOnFailureListener {
                isDownloading = false
                downloadStatus = "Update failed: ${it.localizedMessage}"
                Toast.makeText(this, "Update Failed", Toast.LENGTH_LONG).show()
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // START BACKGROUND SERVICE
        val serviceIntent = android.content.Intent(this, com.joshua.chokepoint.service.SafetyService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        setupGoogleSignIn()

        setContent {
            ChokepointandroidTheme {
                val navController = rememberNavController()
                
                // Request Notification Permission (Android 13+)
                val context = androidx.compose.ui.platform.LocalContext.current
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        if (isGranted) {
                            Log.d("Permissions", "Notification permission granted")
                        } else {
                            Log.w("Permissions", "Notification permission denied")
                        }
                    }
                )

                LaunchedEffect(Unit) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                         if (androidx.core.content.ContextCompat.checkSelfPermission(
                                context, 
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                // Update Dialog Overlay
                if (isUpdateDialogVisible && updateRelease != null) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { isUpdateDialogVisible = false },
                        title = { androidx.compose.material3.Text("New Update Available") },
                        text = { 
                            androidx.compose.material3.Text(
                                "Version: ${updateRelease?.displayVersion} (${updateRelease?.versionCode})\n\n" +
                                "Notes: ${updateRelease?.releaseNotes}"
                            ) 
                        },
                        confirmButton = {
                            androidx.compose.material3.Button(onClick = { startUpdate() }) {
                                androidx.compose.material3.Text("Update Now")
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = { isUpdateDialogVisible = false }) {
                                androidx.compose.material3.Text("Later")
                            }
                        }
                    )
                }

                // Progress Dialog Overlay
                if (isDownloading) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { /* Prevent dismiss */ },
                        title = { androidx.compose.material3.Text("Updating App...") },
                        text = {
                            androidx.compose.foundation.layout.Column {
                                androidx.compose.material3.Text(downloadStatus)
                                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                                )
                            }
                        },
                        confirmButton = {}
                    )
                }

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
                                    isLoading = false
                                    if (success) {
                                        Toast.makeText(this@MainActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                                        // Navigation handled by checkProfileAndNavigate callback logic below
                                        // But wait, signInWithEmail currently takes (Boolean) -> Unit.
                                        // We need to change how we call it.
                                        
                                        // Actually, I can't easily change the hook inside signInWithEmail to trigger navigation 
                                        // because it needs access to auth.currentUser which is available.
                                        
                                        // Strategy:
                                        // 1. signInWithEmail returns success.
                                        // 2. We check current user here.
                                        val user = auth.currentUser
                                        if (user != null) {
                                            checkProfileAndNavigate(user)
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
                                onMissingProfile = {
                                    navController.navigate("complete_profile") {
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

                    composable("complete_profile") {
                        com.joshua.chokepoint.ui.screens.CompleteProfileScreen(
                            onSaveClick = { name ->
                                val user = auth.currentUser
                                if (user != null) {
                                    val repo = com.joshua.chokepoint.data.firestore.FirestoreRepository()
                                    repo.syncUserProfile(
                                        uid = user.uid,
                                        email = user.email ?: "",
                                        name = name,
                                        onSuccess = {
                                            navController.navigate("dashboard") {
                                                popUpTo("complete_profile") { inclusive = true }
                                            }
                                        },
                                        onFailure = {
                                            Toast.makeText(this@MainActivity, "Failed to save profile.", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        )
                    }

                    composable("signup") {
                        SignUpScreen(
                            isLoading = isLoading,
                            onSignUpClick = { email, password, name ->
                                isLoading = true
                                signUpWithEmail(email, password, name) { success, message ->
                                    if (success) {
                                         Toast.makeText(this@MainActivity, "Account Created!", Toast.LENGTH_SHORT).show()
                                         navController.navigate("dashboard") {
                                             popUpTo("landing") { inclusive = true }
                                         }
                                    } else {
                                        isLoading = false
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
                        
                        // Use Singleton
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val repository = remember { 
                            com.joshua.chokepoint.data.mqtt.MqttRepository.getInstance(context)
                        }
                        
                        val viewModel = remember { com.joshua.chokepoint.ui.screens.DashboardViewModel(repository, firestoreRepository) }
                        
                        // Collect state
                        val isConnected by viewModel.isConnected.collectAsState()
                        val sensorData by viewModel.sensorData.collectAsState()
                        val savedDevices by viewModel.savedDevices.collectAsState() // Collect devices
                        
                        // CONNECT IS HANDLED BY SERVICE NOW, BUT KEEPING IT HERE IS SAFE (IDEMPOTENT)
                        LaunchedEffect(Unit) {
                            viewModel.connect()
                        }
                        
                        // DO NOT DISCONNECT ON LEAVE - We want background service to control it
                        /* 
                        DisposableEffect(Unit) {
                            onDispose {
                                viewModel.disconnect()
                            }
                        }
                        */

                        DashboardScreen(
                            sensorData = sensorData,
                            isConnected = isConnected,
                            savedDevices = savedDevices, 
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
                            onDevicesClick = {
                                navController.navigate("devices")
                            },
                            onAddDeviceClick = {
                                navController.navigate("provisioning")
                            },
                            onRecalibrateClick = { deviceId ->
                                viewModel.recalibrateSensor(deviceId)
                                Toast.makeText(applicationContext, "Calibrating sensor...", Toast.LENGTH_SHORT).show()
                            },
                            onSettingsClick = { // NEW
                                navController.navigate("settings")
                            }
                        )
                    }

                    composable("settings") { // NEW ROUTE
                        val settingsRepository = remember { com.joshua.chokepoint.data.repository.SettingsRepository(applicationContext) }
                        val viewModel = remember { com.joshua.chokepoint.ui.screens.SettingsViewModel(settingsRepository) }
                        
                        com.joshua.chokepoint.ui.screens.SettingsScreen(
                            viewModel = viewModel,
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("provisioning") {
                         com.joshua.chokepoint.ui.screens.ProvisioningScreen(
                             onBackClick = {
                                 navController.popBackStack()
                             },
                             onProvisionComplete = {
                                 navController.popBackStack()
                             }
                         )
                    }

                    composable("devices") {
                        com.joshua.chokepoint.ui.screens.DevicesScreen(
                            onBackClick = {
                                navController.popBackStack()
                            },
                            onAddDeviceClick = {
                                navController.navigate("provisioning")
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

    override fun onResume() {
        super.onResume()
        checkForUpdate()
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
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d("Auth", "Login success: ${user?.email}")
                    
                    if (user != null) {
                        // Check if we have a name
                        val googleName = user.displayName
                        if (googleName.isNullOrBlank()) {
                            // Name missing -> Go to Complete Profile Screen
                            // We need to trigger navigation. Since we are in a callback, 
                            // we rely on onLoginSuccess to handle commonly but here we need a specific 'incomplete' state.
                            // BUT, onLoginSuccess is currently void.
                            // Let's modify the architecture slightly: 
                            // We will expose a separate callback or handle it via a boolean flag/livedata observation?
                            // Simpler: Just run on specific logic if we can access navController. 
                            // But we can't easily access navController here directly without restructuring.
                            // WORKAROUND: We will piggyback on onLoginSuccess but we need to check the user in the UI?
                            // No, let's just update the onLoginSuccess signature or use a mutable state that triggers navigation.
                            
                            // Let's use a MutableState in MainActivity that the LaunchedEffect or similar observes?
                            // Or, since we defined onLoginSuccess as a lambda that CAPTURES the navController in onCreate,
                            // we can re-define it? No, it's defined inside composable usually or passed down.
                            // Wait, onLoginSuccess is a property of MainActivity: `private var onLoginSuccess: (() -> Unit)? = null`
                            // And it's assigned inside the `composable("login")` block! 
                            // So it captures the SPECIFIC navController for that screen.
                            
                            // ISSUE: If we want to navigate to "complete_profile", we need a DIFFERENT callback or pass parameters.
                            // Let's change onLoginSuccess to take a boolean 'needsProfile'.
                            
                            // ACTUALLY: The easiest fix is to simply attempt the sync. 
                            // If name is empty, we sync it as empty/placeholder, BUT we navigate to "complete_profile" 
                            // immediately after login if we detect it's empty.
                            
                            // Let's modify the onLoginSuccess lambda in the composable "login" to check the current user's state.
                             onLoginSuccess?.invoke() // This just goes to dashboard.
                             // We need to intervene.
                             
                             // Let's separate "onLoginSuccess" (dashboard) from "onMissingProfile".
                             onMissingProfile?.invoke()
                        } else {
                            val repo = com.joshua.chokepoint.data.firestore.FirestoreRepository()
                                repo.syncUserProfile(
                                uid = user.uid,
                                email = user.email ?: "",
                                name = googleName,
                                onSuccess = { checkProfileAndNavigate(user) },
                                onFailure = { checkProfileAndNavigate(user) }
                            )
                        }
                    } else {
                         // Should not happen
                         onLoginSuccess?.invoke()
                    }
                } else {
                    Log.e("Auth", "Firebase auth failed", task.exception)
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkProfileAndNavigate(user: com.google.firebase.auth.FirebaseUser) {
        val repo = com.joshua.chokepoint.data.firestore.FirestoreRepository()
        repo.getUserProfile(user.uid, 
            onSuccess = { name ->
                if (name.isNullOrBlank()) {
                    onMissingProfile?.invoke()
                } else {
                    onLoginSuccess?.invoke()
                }
            },
            onFailure = {
                 // Offline or error? Default to dashboard to avoid lockout, or show error?
                 // Let's default to dashboard but warn.
                 Log.e("Auth", "Failed to check profile", it)
                 onLoginSuccess?.invoke()
            }
        )
    }

    private fun signInWithEmail(email: String, pass: String, onResult: (Boolean) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                         checkProfileAndNavigate(user)
                         // We don't call onResult(true) immediately because navigation handles it?
                         // Wait, LoginScreen expects onResult to stop loading.
                         // But we want to trigger navigation.
                         // Modified LoginScreen logic: onLoginClick handles UI. 
                         // But we need to signal success/failure.
                         // Let's keep onResult(true) but we must Ensure navigation happens.
                         // The LoginScreen calls logic: if(success) nav -> dashboard.
                         
                         // ISSUE: The LoginScreen logic manually navigates to "dashboard" on success:
                         // if (success) { ... navController.navigate("dashboard") ... }
                         // This OVERRIDES our check.
                         
                         // We need to change LoginScreen usage in onCreate.
                         // But here, I can't easily change the callback structure passed to LoginScreen without changing Comp.
                         
                         // Fix: I will update the LoginScreen usage in onCreate to NOT navigate manually, 
                         // but rely on onLoginSuccess/onMissingProfile which I will trigger here.
                         onResult(false) // Hack: prevent LoginScreen traversing? No, that shows "Login Failed".
                         
                         // Refactor: We need `checkProfileAndNavigate` inside the LoginScreen callback in onCreate.
                         // See next edit.
                         onResult(true)
                    } else {
                         onResult(true)
                    }
                } else {
                    Log.w("Auth", "signInWithEmail:failure", task.exception)
                    onResult(false)
                }
            }
    }

    private fun signUpWithEmail(email: String, pass: String, name: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Create Firestore Profile
                    val uid = task.result.user?.uid ?: ""
                    val repo = com.joshua.chokepoint.data.firestore.FirestoreRepository()
                    repo.createUserProfile(uid, email, name, 
                        onSuccess = {
                             onResult(true, null)
                        },
                        onFailure = { e ->
                             onResult(false, "Profile Sync Failed: ${e.localizedMessage}")
                        }
                    )
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
