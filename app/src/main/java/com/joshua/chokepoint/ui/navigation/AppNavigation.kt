package com.joshua.chokepoint.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.joshua.chokepoint.data.firestore.FirestoreRepository
import com.joshua.chokepoint.data.mqtt.MqttRepository
import com.joshua.chokepoint.data.repository.CartRepositoryImpl
import com.joshua.chokepoint.data.repository.MarketplaceRepositoryImpl
import com.joshua.chokepoint.data.discovery.DiscoveryRepository
import com.joshua.chokepoint.data.repository.SettingsRepository
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import com.joshua.chokepoint.ui.screens.*
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val startDestination = if (auth.currentUser != null) "dashboard" else "login"
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    // Repositories
    // Using remember to keep instances across recompositions
    val firestoreRepository = remember { FirestoreRepository() } 
    val mqttRepository = remember { MqttRepository.getInstance(context) }
    val marketplaceRepository = remember { MarketplaceRepositoryImpl() }
    val cartRepository = remember { CartRepositoryImpl() }
    val discoveryRepository = remember { DiscoveryRepository(context) }
    val settingsRepository = remember { SettingsRepository(context) }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("login") {
            LoginScreen(
                isLoading = false, // Should be managed by a ViewModel ideally
                onLoginClick = { email, password ->
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener { authResult ->
                            val user = authResult.user
                            if (user != null) {
                                firestoreRepository.syncUserProfile(
                                    uid = user.uid,
                                    email = user.email ?: email,
                                    name = user.displayName ?: email.substringBefore("@"),
                                    onSuccess = {
                                        navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
                                    },
                                    onFailure = {
                                        navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
                                    }
                                )
                            }
                        }
                        .addOnFailureListener { /* Handle error */ }
                },
                onGoogleSignInClick = {
                    coroutineScope.launch {
                        try {
                            val activity = context.findActivity()
                            if (activity == null) {
                                Log.e("Auth", "Activity context not found for Google Sign-In")
                                return@launch
                            }
                            
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId("164679848850-ct4nn3gnb5hu61doi8oivd5lj9mpq66h.apps.googleusercontent.com")
                                .setAutoSelectEnabled(true)
                                .build()
                                
                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()

                            val result = credentialManager.getCredential(activity, request)
                            val credential = result.credential
                            
                            if (credential is CustomCredential &&
                                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                val idToken = googleIdTokenCredential.idToken
                                val firebaseAuthWithGoogle = GoogleAuthProvider.getCredential(idToken, null)
                                
                                auth.signInWithCredential(firebaseAuthWithGoogle)
                                    .addOnSuccessListener { authResult ->
                                        val user = authResult.user
                                        if (user != null) {
                                            firestoreRepository.syncUserProfile(
                                                uid = user.uid,
                                                email = user.email ?: "",
                                                name = user.displayName ?: "Google User",
                                                onSuccess = {
                                                    navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
                                                },
                                                onFailure = {
                                                    navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
                                                }
                                            )
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("Auth", "Firebase Google Sign-In failed", e)
                                    }
                            }
                        } catch (e: Exception) {
                            Log.e("Auth", "Google Sign-In failed", e)
                        }
                    }
                },
                onForgotPasswordClick = { navController.navigate("forgot_password") },
                onBackClick = { /* No back from login usually, or exit app */ },
                onSignUpClick = { navController.navigate("signup") }
            )
        }

        composable("signup") {
            SignUpScreen(
                isLoading = false,
                onSignUpClick = { email, password, name ->
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { authResult ->
                             firestoreRepository.createUserProfile(
                                 uid = authResult.user!!.uid,
                                 email = email,
                                 name = name,
                                 onSuccess = {
                                     navController.navigate("dashboard") { popUpTo("signup") { inclusive = true } }
                                 },
                                 onFailure = { /* Handle profile creation error */ }
                             )
                        }
                        .addOnFailureListener { /* Handle signup error */ }
                },
                onBackClick = { navController.navigate("login") }
            )
        }

        composable("forgot_password") {
            ForgotPasswordScreen(
                isLoading = false,
                onBackClick = { navController.popBackStack() },
                onSendResetEmailClick = { email ->
                    auth.sendPasswordResetEmail(email)
                        .addOnSuccessListener { navController.popBackStack() }
                }
            )
        }

        composable("dashboard") {
            val viewModel: DashboardViewModel = viewModel {
                DashboardViewModel(mqttRepository, firestoreRepository)
            }
            
            // Connect MQTT on dashboard load
            LaunchedEffect(Unit) {
                viewModel.connect()
            }

            val deviceReadings by viewModel.deviceReadings.collectAsState()
            val isConnected by viewModel.isConnected.collectAsState()
            val savedDevices by viewModel.savedDevices.collectAsState()

            DashboardScreen(
                deviceReadings = deviceReadings,
                isConnected = isConnected,
                savedDevices = savedDevices,
                onLogoutClick = {
                    auth.signOut()
                    viewModel.disconnect()
                    navController.navigate("login") { popUpTo("dashboard") { inclusive = true } }
                },
                onHistoryClick = { deviceId -> navController.navigate("analytics/$deviceId") },
                onMarketplaceClick = { navController.navigate("marketplace") },
                onDevicesClick = { navController.navigate("devices") },
                onAddDeviceClick = { navController.navigate("provisioning") },
                onRecalibrateClick = { deviceId -> viewModel.recalibrateSensor(deviceId) },
                onSettingsClick = { navController.navigate("settings") },
                onProfileClick = { navController.navigate("profile") }
            )
        }

        composable("marketplace") {
            val viewModel: MarketplaceViewModel = viewModel {
                MarketplaceViewModel(marketplaceRepository)
            }
            MarketplaceScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onProductClick = { productId -> navController.navigate("product/$productId") }
            )
        }

        composable(
            route = "product/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
            
            ProductDetailScreen(
                productId = productId,
                repository = marketplaceRepository,
                cartRepository = cartRepository,
                onBackClick = { navController.popBackStack() },
                onCartClick = { navController.navigate("cart") }
            )
        }

        composable(
            route = "analytics/{deviceId}",
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable
            AnalyticsScreen(
                deviceId = deviceId,
                repository = firestoreRepository,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("cart") {
            CartScreen(
                repository = cartRepository,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("devices") {
            DevicesScreen(
                onBackClick = { navController.popBackStack() },
                onAddDeviceClick = { navController.navigate("provisioning") }
            )
        }

        composable("profile") {
            val viewModel: ProfileViewModel = viewModel {
                ProfileViewModel(firestoreRepository)
            }
            ProfileScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onSignOutClick = {
                    auth.signOut()
                    navController.navigate("login") { popUpTo("dashboard") { inclusive = true } }
                }
            )
        }

        composable("settings") {
            val viewModel: SettingsViewModel = viewModel {
                SettingsViewModel(settingsRepository)
            }
            SettingsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("provisioning") {
             ProvisioningScreen(
                 onBackClick = { navController.popBackStack() },
                 onProvisionComplete = { navController.popBackStack() }
             )
        }
    }
}
