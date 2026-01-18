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
                text = "Please connect this phone to 'Chokepoint-Setup' Wi-Fi network before proceeding.",
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

            // Step 1: Send Credentials to ESP32
            Button(
                onClick = {
                    if (ssid.isBlank()) {
                        Toast.makeText(context, "Enter SSID", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSending = true
                    statusMessage = "Sending credentials..."
                    
                    scope.launch {
                        val responseJson = sendCredentials(ssid, password)
                        
                        if (responseJson != null) {
                            try {
                                val json = org.json.JSONObject(responseJson)
                                val deviceId = json.optString("device_id")
                                
                                if (deviceId.isNotEmpty()) {
                                    isSending = false
                                    pendingDeviceId = deviceId // Store it to trigger Step 2
                                    statusMessage = "Device Found! Please Restore Internet."
                                    
                                } else {
                                    isSending = false
                                    statusMessage = "Provisioned (Legacy). No ID."
                                    onPairSuccess() 
                                }
                            } catch (e: Exception) {
                                isSending = false
                                statusMessage = "Error parsing device response."
                                e.printStackTrace()
                            }
                        } else {
                            isSending = false
                            statusMessage = "Failed. Are you connected to Chokepoint-Setup?"
                        }
                    }
                },
                enabled = !isSending && pendingDeviceId.isEmpty(), // Disable if already found
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isSending) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Configure Device")
                }
            }
            
            // Step 2: Claim Device (Only visible after Step 1 success)
            if (pendingDeviceId.isNotEmpty()) {
                 Spacer(modifier = Modifier.height(24.dp))
                Card(
                     colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                     modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Step 2: Connect to Internet", style = MaterialTheme.typography.titleMedium)
                        Text("Your phone is likely still on the Device Wi-Fi (No Internet).", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("1. Disconnect from 'Chokepoint-Setup'.\n2. Connect to Mobile Data or Home Wi-Fi.\n3. Tap below to save the device.", style = MaterialTheme.typography.bodySmall)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                val repo = com.joshua.chokepoint.data.firestore.FirestoreRepository()
                                statusMessage = "Claiming ${pendingDeviceId}..."
                                
                                repo.claimDevice(
                                    deviceId = pendingDeviceId,
                                    name = "New Sensor",
                                    onSuccess = {
                                        statusMessage = "Success! Device Claimed."
                                        Toast.makeText(context, "Device Claimed! You are the Admin.", Toast.LENGTH_LONG).show()
                                        onPairSuccess()
                                    },
                                    onError = { e ->
                                        statusMessage = "Claim Failed: ${e.message}. Check Internet."
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                             Text("Finish Claiming")
                        }
                    }
                }
            } else if (statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = statusMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// Simple suspend function to hit the ESP32 Web Server
// Returns the response body (JSON) if success, or null if failed
suspend fun sendCredentials(ssid: String, pass: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("http://192.168.4.1/configure?ssid=$ssid&pass=$pass")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            
            val responseCode = conn.responseCode
            if (responseCode == 200) {
                // Read response
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                return@withContext response
            } else {
                conn.disconnect()
                return@withContext null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}
