package com.joshua.chokepoint.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.joshua.chokepoint.data.firestore.FirestoreRepository
import com.joshua.chokepoint.data.model.SensorData
import com.joshua.chokepoint.ui.theme.CardGrey
import com.joshua.chokepoint.ui.theme.MintBackground
import com.joshua.chokepoint.ui.theme.TextLight
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    deviceId: String,
    repository: FirestoreRepository,
    onBackClick: () -> Unit
) {
    val readings = remember { mutableStateListOf<SensorData>() }
    
    // Simple state flow observation
    LaunchedEffect(deviceId) {
        if(deviceId.isNotEmpty()) {
            repository.observeRecentReadings(deviceId, 50).collect { list ->
                readings.clear()
                readings.addAll(list)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sensor History", color = TextLight) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MintBackground)
            )
        },
        containerColor = MintBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (readings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No history yet. Wait for sensor data...", color = TextLight)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(readings) { item ->
                        HistoryCard(item)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(data: SensorData) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardGrey),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(data.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLight.copy(alpha = 0.7f)
                )
                Text(
                    text = "CO2: ${data.co2.toInt()} ppm",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextLight
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("NH3: ${String.format("%.2f", data.nh3)}", color = TextLight)
                Text("Smoke: ${String.format("%.1f", data.smoke)}", color = TextLight)
            }
        }
    }
}
