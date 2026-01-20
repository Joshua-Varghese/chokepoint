package com.joshua.chokepoint.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DashboardScreen(
    sensorData: com.joshua.chokepoint.data.model.SensorData,
    isConnected: Boolean,
    savedDevices: List<com.joshua.chokepoint.data.firestore.FirestoreRepository.Device>, // Add this
    onLogoutClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onMarketplaceClick: () -> Unit,
    onDevicesClick: () -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = true,
                    onClick = { }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Sensors, contentDescription = "Devices") },
                    label = { Text("Devices") },
                    selected = false,
                    onClick = { onDevicesClick() }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.ShoppingBag, contentDescription = "Market") },
                    label = { Text("Market") },
                    selected = false,
                    onClick = { onMarketplaceClick() }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = { onLogoutClick() } 
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Row {
                   if (isConnected) Text("🟢", modifier = Modifier.padding(end=8.dp)) else Text("🔴", modifier = Modifier.padding(end=8.dp))
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "Notifications",
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Profile",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Main AQI Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "AIR QUALITY INDEX",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Show actual Air Quality from Sensor
                        val quality = sensorData.airQuality.ifEmpty { "Unknown" }
                        
                        Text(
                            text = quality,
                            style = MaterialTheme.typography.displayMedium, // Smaller font for text
                            fontSize = 40.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(8.dp),
                                    shape = CircleShape,
                                    color = if(quality == "Hazardous") Color.Red else Color.Green
                                ) {}
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Raw: ${sensorData.gasRaw}",
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    // Try to find a friendly name for this deviceId
                    val friendlyName = savedDevices.find { it.id == sensorData.deviceId }?.name 
                        ?: savedDevices.firstOrNull()?.name // Fallback: Show first device name for demo
                        ?: sensorData.deviceId.ifEmpty { "Unknown Device" }

                    Text(
                        text = "$friendlyName • Live",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Detailed Readings Header
            Text(
                text = "Detailed Readings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Grid of Cards
            Row(modifier = Modifier.fillMaxWidth()) {
                DetailCard(
                    title = "Gas Level",
                    value = "${sensorData.gasRaw}",
                    unit = "",
                    modifier = Modifier
                        .weight(1f)
                        .height(160.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                DetailCard(
                    title = "Quality",
                    value = sensorData.airQuality.take(4), // Shorten if needed
                    unit = "",
                    modifier = Modifier
                        .weight(1f)
                        .height(160.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                DetailCard(
                    title = "CO2 (Est)",
                    value = "${(sensorData.gasRaw / 10)}", // Dummy estimation
                    unit = "ppm",
                    modifier = Modifier
                        .weight(1f)
                        .height(160.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                 DetailCard(
                    title = "Temp",
                    value = "0",
                    unit = "°C",
                    isPlaceholder = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(160.dp)
                )
            }
        }
    }
}

@Composable
fun DetailCard(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = com.joshua.chokepoint.ui.theme.CardGrey),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
               modifier = Modifier.fillMaxWidth(),
               horizontalArrangement = Arrangement.SpaceBetween 
            ) {
                 // Icon Placeholder
                 Icon(Icons.Default.Sensors, contentDescription = null, tint = Color.Gray)
                 
                 Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = com.joshua.chokepoint.ui.theme.GoodGreen) {}
            }
            
            Column {
                Text(
                    text = if(isPlaceholder) "$value$unit" else "$value $unit", 
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black // Fix: Ensure visibility on light card
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray // Fix: Better contrast
                )
            }
        }
    }
}
