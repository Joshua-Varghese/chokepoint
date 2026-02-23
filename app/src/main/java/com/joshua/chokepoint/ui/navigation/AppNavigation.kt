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
import com.joshua.chokepoint.ui.screens.*

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val startDestination = if (auth.currentUser != null) "dashboard" else "login"
    val context = LocalContext.current

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
                        .addOnSuccessListener { navController.navigate("dashboard") { popUpTo("login") { inclusive = true } } }
                        .addOnFailureListener { /* Handle error */ }
                },
                onGoogleSignInClick = { /* Handle Google Sign In */ },
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
