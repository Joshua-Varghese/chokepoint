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
import java.text.SimpleDateFormat
import java.util.Locale

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    deviceId: String,
    repository: FirestoreRepository,
    onBackClick: () -> Unit
) {
    val readings = remember { mutableStateListOf<SensorData>() }
    
    LaunchedEffect(deviceId) {
        if(deviceId.isNotEmpty()) {
            val history = repository.getHistoricalReadings(deviceId, 300)
            readings.clear()
            readings.addAll(history)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sensor History", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (readings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No history yet. Wait for sensor data...", color = MaterialTheme.colorScheme.onBackground)
                }
            } else {
                Text(
                    "CO₂ Trend (Last ${readings.size} readings)",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardGrey),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(200.dp)
                ) {
                    SensorChart(data = readings, modifier = Modifier.fillMaxSize().padding(16.dp))
                }
                
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
fun SensorChart(data: List<SensorData>, modifier: Modifier = Modifier) {
    if (data.size < 2) return
    val sorted = data.sortedBy { it.timestamp }
    val maxVal = sorted.maxOfOrNull { it.co2 }?.coerceAtLeast(100.0) ?: 100.0
    val minVal = sorted.minOfOrNull { it.co2 }?.coerceAtMost(0.0) ?: 0.0
    val range = (maxVal - minVal).coerceAtLeast(1.0)
    
    val minTime = sorted.first().timestamp
    val maxTime = sorted.last().timestamp
    val timeRange = (maxTime - minTime).coerceAtLeast(1L).toFloat()

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        val path = Path()
        sorted.forEachIndexed { index, dp ->
            val x = if (timeRange > 0f) width * ((dp.timestamp - minTime).toFloat() / timeRange) else 0f
            val y = height - (((dp.co2 - minVal) / range).toFloat() * height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        
        drawPath(
            path = path,
            color = androidx.compose.ui.graphics.Color(0xFF34D399),
            style = Stroke(width = 3.dp.toPx())
        )
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
                    text = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(data.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "CO2: ${data.co2.toInt()} ppm",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Smoke: ${String.format("%.1f", data.smoke)}", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
