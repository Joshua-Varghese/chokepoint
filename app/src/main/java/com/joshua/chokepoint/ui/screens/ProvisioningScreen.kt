package com.joshua.chokepoint.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvisioningScreen(
    onBackClick: () -> Unit,
    onPairSuccess: () -> Unit
) {
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var pendingDeviceId by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup New Device") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Connect to Device Wi-Fi",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "1. Connect phone to 'Chokepoint-Setup' Wi-Fi.\n2. Enter Home Wi-Fi details below.\n3. Tap 'Configure'.\n\nAlternatively, manually enter Device ID if known.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            OutlinedTextField(
                value = ssid,
                onValueChange = { ssid = it },
                label = { Text("Your Home Wi-Fi Name (SSID)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Wi-Fi Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Step 1: Send Credentials
            Button(
                onClick = {
                    if (ssid.isBlank()) {
                        Toast.makeText(context, "Enter SSID", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSending = true
                    statusMessage = "Sending..."
                    
                    scope.launch {
                        val responseJson = sendCredentials(ssid, password)
                        if (responseJson != null) {
                            try {
                                val json = org.json.JSONObject(responseJson)
                                val deviceId = json.optString("device_id")
                                if (deviceId.isNotEmpty()) {
                                    isSending = false
                                    pendingDeviceId = deviceId
                                    statusMessage = "Device Configured! Reconnect Internet."
                                } else {
                                    isSending = false
                                    statusMessage = "Success but no ID returned?"
                                }
                            } catch (e: Exception) {
                                isSending = false
                                statusMessage = "Error parsing: ${responseJson.take(20)}"
                            }
                        } else {
                            isSending = false
                            statusMessage = "Failed. Connected to 'Chokepoint-Setup'?"
                        }
                    }
                },
                enabled = !isSending && pendingDeviceId.isEmpty(),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isSending) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Configure Device")
                }
            }
            
            // Step 2 or Manual Fallback
            if (pendingDeviceId.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Step 2: Connect to Internet", style = MaterialTheme.typography.titleMedium)
                        Text("Disconnect from Device Wi-Fi. Reconnect to Internet.", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val repo = com.joshua.chokepoint.data.firestore.FirestoreRepository()
                                repo.claimDevice(pendingDeviceId, "New Sensor", 
                                    { Toast.makeText(context, "Success!", Toast.LENGTH_LONG).show(); onPairSuccess() },
                                    { Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_SHORT).show() }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Finish Claiming") }
                    }
                }
            }
            
            // Manual Entry Fallback
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = pendingDeviceId,
                onValueChange = { pendingDeviceId = it },
                label = { Text("Manual Device ID (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Helpful Button for 'Old People' / Non-techies
            TextButton(
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("http://192.168.4.1"))
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    try { context.startActivity(intent) } catch(_: Exception) {}
                },
                modifier = Modifier.align(Alignment.End) 
            ) {
                Text("Don't know ID? Open Setup Page")
            }

            if (pendingDeviceId.isNotEmpty() && !statusMessage.contains("Configured")) {
                 Button(
                     onClick = {
                          val repo = com.joshua.chokepoint.data.firestore.FirestoreRepository()
                          repo.claimDevice(pendingDeviceId, "Manual Sensor", 
                              { Toast.makeText(context, "Success!", Toast.LENGTH_LONG).show(); onPairSuccess() },
                              { Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_SHORT).show() }
                          )
                     },
                     modifier = Modifier.fillMaxWidth()
                 ) { Text("Claim Manually") }
            }

            if (statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = statusMessage, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

suspend fun sendCredentials(ssid: String, pass: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("http://192.168.4.1/configure?ssid=$ssid&pass=$pass")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        } catch (e: Exception) { null }
    }
}

