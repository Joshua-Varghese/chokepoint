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
import com.joshua.chokepoint.ui.theme.TextDark
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import java.util.concurrent.TimeUnit

enum class TimeFilter(val label: String, val durationMillis: Long) {
    MINS_5("5 Mins", TimeUnit.MINUTES.toMillis(5)),
    MINS_10("10 Mins", TimeUnit.MINUTES.toMillis(10)),
    MINS_30("30 Mins", TimeUnit.MINUTES.toMillis(30)),
    HOUR_1("1 Hour", TimeUnit.HOURS.toMillis(1)),
    HOURS_24("24 Hours", TimeUnit.HOURS.toMillis(24)),
    ALL("All Time", Long.MAX_VALUE)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    deviceId: String,
    repository: FirestoreRepository,
    onBackClick: () -> Unit
) {
    val readings = remember { mutableStateListOf<SensorData>() }
    var selectedFilter by remember { mutableStateOf(TimeFilter.HOUR_1) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(deviceId) {
        isLoading = true
        if(deviceId.isNotEmpty()) {
            val history = repository.getHistoricalReadings(deviceId, 2000)
            readings.clear()
            readings.addAll(history)
        }
        isLoading = false
    }

    val currentTime = System.currentTimeMillis()
    val filteredReadings = remember(readings, selectedFilter) {
        if (selectedFilter == TimeFilter.ALL) {
            readings
        } else {
            readings.filter { currentTime - it.timestamp <= selectedFilter.durationMillis }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (deviceId == "home") "Global Reports" else "Sensor History", color = MaterialTheme.colorScheme.onBackground) },
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
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (deviceId == "home") {
                ReportsView(readings)
            } else {
                // Device specific analytics
                ScrollableFilterRow(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it }
                )
                
                if (filteredReadings.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No data for selected time range.", color = MaterialTheme.colorScheme.onBackground)
                    }
                } else {
                    Text(
                        "CO₂ Trend (Last ${filteredReadings.size} readings)",
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
                        SensorChart(data = filteredReadings, modifier = Modifier.fillMaxSize().padding(16.dp))
                    }
                    
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredReadings) { item ->
                            HistoryCard(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScrollableFilterRow(
    selectedFilter: TimeFilter,
    onFilterSelected: (TimeFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TimeFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun ReportsView(allReadings: List<SensorData>) {
    val currentTime = System.currentTimeMillis()
    val dayMillis = TimeUnit.DAYS.toMillis(1)
    val weekMillis = TimeUnit.DAYS.toMillis(7)
    val monthMillis = TimeUnit.DAYS.toMillis(30)

    val dailyReadings = allReadings.filter { currentTime - it.timestamp <= dayMillis }
    val weeklyReadings = allReadings.filter { currentTime - it.timestamp <= weekMillis }
    val monthlyReadings = allReadings.filter { currentTime - it.timestamp <= monthMillis }

    fun calculateAverages(readings: List<SensorData>): Pair<Int, Float> {
        if (readings.isEmpty()) return Pair(0, 0f)
        val avgCo2 = readings.map { it.co2 }.average().toInt()
        val avgSmoke = readings.map { it.smoke }.average().toFloat()
        return Pair(avgCo2, avgSmoke)
    }

    val (dailyCo2, dailySmoke) = calculateAverages(dailyReadings)
    val (weeklyCo2, weeklySmoke) = calculateAverages(weeklyReadings)
    val (monthlyCo2, monthlySmoke) = calculateAverages(monthlyReadings)
    val (allTimeCo2, allTimeSmoke) = calculateAverages(allReadings)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()) // Allow scrolling if screen is small
    ) {
        Text("Historical Averages", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))
        
        ReportCard(title = "Daily Average (Last 24h)", co2 = dailyCo2, smoke = dailySmoke, count = dailyReadings.size)
        Spacer(modifier = Modifier.height(12.dp))
        
        ReportCard(title = "Weekly Average (Last 7 Days)", co2 = weeklyCo2, smoke = weeklySmoke, count = weeklyReadings.size)
        Spacer(modifier = Modifier.height(12.dp))
        
        ReportCard(title = "Monthly Average (Last 30 Days)", co2 = monthlyCo2, smoke = monthlySmoke, count = monthlyReadings.size)
        Spacer(modifier = Modifier.height(12.dp))
        
        ReportCard(title = "All-Time Average", co2 = allTimeCo2, smoke = allTimeSmoke, count = allReadings.size)
    }
}

@Composable
fun ReportCard(title: String, co2: Int, smoke: Float, count: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardGrey),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("CO₂: $co2 ppm", color = TextDark)
                Text("Smoke: ${String.format("%.1f", smoke)}", color = TextDark)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Based on $count readings", style = MaterialTheme.typography.bodySmall, color = TextDark.copy(alpha=0.7f))
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
                    text = java.text.SimpleDateFormat("MMM dd, HH:mm:ss", java.util.Locale.getDefault()).format(data.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDark.copy(alpha = 0.7f)
                )
                Text(
                    text = "CO2: ${data.co2.toInt()} ppm",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Smoke: ${String.format("%.1f", data.smoke)}", color = TextDark)
            }
        }
    }
}
