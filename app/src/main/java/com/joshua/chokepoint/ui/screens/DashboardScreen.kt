package com.joshua.chokepoint.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable // New import
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.getValue // Add this
import androidx.compose.runtime.setValue // Add this
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class) // For Pager
@Composable
fun DashboardScreen(
    deviceReadings: Map<String, com.joshua.chokepoint.data.model.SensorData>, // Changed param
    isConnected: Boolean,
    savedDevices: List<com.joshua.chokepoint.data.firestore.FirestoreRepository.Device>,
    onLogoutClick: () -> Unit,
    onHistoryClick: (String) -> Unit,
    onMarketplaceClick: () -> Unit,
    onDevicesClick: () -> Unit,
    onAddDeviceClick: () -> Unit,
    onRecalibrateClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    // Force refresh every 10 seconds to update relative time
    var ticker by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0L) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(10_000)
            ticker = System.currentTimeMillis()
        }
    }
    
    // Pager State
    // If no devices, we simulate 1 page for the "Add Device" placeholder
    val pageCount = if (savedDevices.isEmpty()) 1 else savedDevices.size + 1
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { pageCount })

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
                    onClick = { onSettingsClick() } 
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddDeviceClick() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, "Add Device")
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
                        modifier = Modifier
                             .size(28.dp)
                             .clickable { onProfileClick() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Check if we have devices
            if (savedDevices.isEmpty()) {
                // Placeholder Card
                DashboardCard(
                    data = com.joshua.chokepoint.data.model.SensorData(deviceId="No Devices", airQuality="Setup Required"),
                    deviceName = "No Devices",
                    isOffline = false,
                    ticker = ticker
                )
            } else {
                // Horizontal Pager for Devices
                Column {
                    androidx.compose.foundation.pager.HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth().height(260.dp)
                    ) { page ->
                        if (page == 0) {
                            RankingCard(deviceReadings, savedDevices)
                        } else {
                            val device = savedDevices[page - 1]
                            val data = deviceReadings[device.id] ?: com.joshua.chokepoint.data.model.SensorData(deviceId = device.id, airQuality = "Waiting...")
                            
                            // Offline Logic
                            val currentTime = if (ticker > 0) ticker else System.currentTimeMillis()
                            val timeDiff = currentTime - data.timestamp
                            val isOffline = timeDiff > 30 * 1000 // 30 seconds threshold
                            
                            DashboardCard(
                                data = data,
                                deviceName = device.name,
                                isOffline = isOffline,
                                ticker = ticker
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Pager Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(pageCount) { iteration ->
                            val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha=0.2f)
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(8.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Detailed Readings (For CURRENTLY SELECTED Page)
            // If empty, show zeros
            val currentData = if (savedDevices.isNotEmpty()) {
                if (pagerState.currentPage == 0) {
                    val validReadings = savedDevices.mapNotNull { deviceReadings[it.id] }.filter { it.gasRaw > 0 }
                    val avgRaw = if (validReadings.isEmpty()) 0 else validReadings.map { it.gasRaw }.average().toInt()
                    val avgCo2 = if (validReadings.isEmpty()) 0.0 else validReadings.map { it.co2 }.average()
                    val avgSmoke = if (validReadings.isEmpty()) 0.0 else validReadings.map { it.smoke }.average()
                    val avgQuality = when {
                        validReadings.isEmpty() -> "Waiting..."
                        avgSmoke > 0.5 -> "Hazardous"
                        avgCo2 > 1000 -> "Poor"
                        avgCo2 > 400 -> "Moderate"
                        else -> "Excellent"
                    }
                    com.joshua.chokepoint.data.model.SensorData(airQuality = avgQuality, gasRaw = avgRaw, co2 = avgCo2)
                } else {
                    val currentDevice = savedDevices[pagerState.currentPage - 1]
                    deviceReadings[currentDevice.id] ?: com.joshua.chokepoint.data.model.SensorData()
                }
            } else {
                com.joshua.chokepoint.data.model.SensorData()
            }

            // Detailed Readings Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detailed Readings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (pagerState.currentPage == 0) {
                    TextButton(onClick = { onHistoryClick("home") }) {
                        Text("Reports", color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    val currentDevice = savedDevices[pagerState.currentPage - 1]
                    TextButton(onClick = { onHistoryClick(currentDevice.id) }) {
                        Text("Analytics", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grid of Cards
            Row(modifier = Modifier.fillMaxWidth()) {
                DetailCard(
                    title = "Gas Level",
                    value = "${currentData.gasRaw}",
                    unit = "",
                    modifier = Modifier
                        .weight(1f)
                        .height(160.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                DetailCard(
                    title = "Quality",
                    value = currentData.airQuality.take(9), // Limit length
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
                    value = "${(currentData.gasRaw / 10)}", 
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
fun DashboardCard(
    data: com.joshua.chokepoint.data.model.SensorData,
    deviceName: String,
    isOffline: Boolean,
    ticker: Long
) {
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
                
                Text(
                    text = data.airQuality.ifEmpty { "Unknown" },
                    style = MaterialTheme.typography.displayMedium,
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
                            color = if(data.airQuality == "Hazardous") Color.Red else Color.Green
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Raw: ${data.gasRaw}",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // Offline Logic
            // ... (Logic passed in via params to keep this pure if possible, or recalculated)
            // We passed isOffline
            
            val statusText = if (isOffline) {
                // We need timestamp to calculate "min ago" again if we want dynamic?
                // Or just show "Offline"
                "Offline" 
            } else {
                "Live"
            }
            val statusColor = if (isOffline) Color.Gray else MaterialTheme.colorScheme.onPrimary

            Text(
                text = "$deviceName • $statusText",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                color = statusColor,
                style = MaterialTheme.typography.bodyMedium
            )
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

@Composable
fun RankingCard(
    deviceReadings: Map<String, com.joshua.chokepoint.data.model.SensorData>,
    savedDevices: List<com.joshua.chokepoint.data.firestore.FirestoreRepository.Device>,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Text(
                text = "LOCATION RANKING",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                letterSpacing = 2.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            val rankedDevices = savedDevices.map { device ->
                val data = deviceReadings[device.id] ?: com.joshua.chokepoint.data.model.SensorData(deviceId=device.id)
                Pair(device, data)
            }.sortedByDescending { it.second.co2 }

            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                rankedDevices.forEach { (device, data) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(device.name, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(data.airQuality.ifEmpty { "Waiting..." }, color = MaterialTheme.colorScheme.onPrimary.copy(alpha=0.9f), modifier = Modifier.padding(end = 12.dp))
                            Surface(
                                modifier = Modifier.size(10.dp),
                                shape = CircleShape,
                                color = if(data.airQuality == "Hazardous" || data.airQuality == "Poor") Color.Red else Color.Green
                            ) {}
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                }
                if (rankedDevices.isEmpty()) {
                    Text(
                        text = "No devices to rank.",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 32.dp)
                    )
                }
            }
        }
    }
}
