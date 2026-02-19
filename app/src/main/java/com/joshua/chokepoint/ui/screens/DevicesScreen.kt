package com.joshua.chokepoint.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange // Add this
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel // Add this
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    onBackClick: () -> Unit,
    onAddDeviceClick: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: DevicesViewModel = androidx.lifecycle.viewmodel.compose.viewModel { 
        DevicesViewModel(
            com.joshua.chokepoint.data.firestore.FirestoreRepository(),
            com.joshua.chokepoint.data.mqtt.MqttRepository.getInstance(context),
            com.joshua.chokepoint.data.discovery.DiscoveryRepository(context)
        ) 
    }
    val devices by viewModel.devices.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddOptionsDialog by remember { mutableStateOf(false) }
    var showSpectateDialog by remember { mutableStateOf(false) }
    var selectedDevice by remember { mutableStateOf<com.joshua.chokepoint.data.firestore.FirestoreRepository.Device?>(null) }
    var newName by remember { mutableStateOf("") }
    var spectateCode by remember { mutableStateOf("") }
    
    val verificationState by viewModel.verificationState.collectAsState()
    
    // Claim Device State (Moved up)
    var showClaimDialog by remember { mutableStateOf(false) }

    // Reset verification when dialog opens/closes
    LaunchedEffect(showClaimDialog) {
        if (!showClaimDialog) viewModel.resetVerification()
    }
    var claimDeviceId by remember { mutableStateOf("") }
    var claimDeviceName by remember { mutableStateOf("") }
    
    // Nuclear Delete Confirmation State
    var deviceToDelete by remember { mutableStateOf<com.joshua.chokepoint.data.firestore.FirestoreRepository.Device?>(null) }
    
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val repo = remember { com.joshua.chokepoint.data.firestore.FirestoreRepository() }

    // Rename Dialog
    if (showEditDialog && selectedDevice != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Device Settings") }, // Changed title
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Device Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Dangerous Actions Section
                    Text("Danger Zone", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = {
                            selectedDevice?.let { device ->
                                viewModel.deleteDeviceFully(device.id) // Updated method name
                                android.widget.Toast.makeText(context, "Reset Command Sent", android.widget.Toast.LENGTH_SHORT).show()
                                showEditDialog = false
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Remote Factory Reset")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedDevice?.let { device ->
                            if (newName.isNotBlank()) {
                                viewModel.renameDevice(device.id, newName)
                            }
                        }
                        showEditDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }
    
    // Add Options Dialog
    if (showAddOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showAddOptionsDialog = false },
            title = { Text("Add Device") },
            text = { Text("Choose how you want to add a device:") },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showAddOptionsDialog = false
                            onAddDeviceClick() // Go to Provisioning
                        } 
                    ) { Text("Setup New Device (WiFi)") }
                    
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showAddOptionsDialog = false
                            showClaimDialog = true // Open Claim Dialog
                        } 
                    ) { Text("Claim Existing Device (ID)") }
                    
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showAddOptionsDialog = false
                            showSpectateDialog = true // Open Spectate Dialog
                        }
                    ) { Text("Spectate via Code") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddOptionsDialog = false }) { Text("Cancel") }
            }
        )
    }



    if (showClaimDialog) {
        AlertDialog(
            onDismissRequest = { showClaimDialog = false },
            title = { Text("Claim Device") },
            text = {
                Column {
                    Text("Enter the Device ID found on the sticker or logs.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = claimDeviceId,
                        onValueChange = { claimDeviceId = it },
                        label = { Text("Device ID (e.g. fce8...)") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = claimDeviceName,
                        onValueChange = { claimDeviceName = it },
                        label = { Text("Nickname") }
                    )

                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Verification Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.verifyDevice(claimDeviceId) },
                            modifier = Modifier.weight(1f),
                            enabled = claimDeviceId.isNotBlank() && verificationState != DevicesViewModel.VerificationState.Verifying
                        ) {
                            if (verificationState == DevicesViewModel.VerificationState.Verifying) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verifying...")
                            } else {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verify ID")
                            }
                        }
                    }
                    
                    if (verificationState == DevicesViewModel.VerificationState.Success) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = Color(0xFF4CAF50))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Device Online & Verified", color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (verificationState == DevicesViewModel.VerificationState.Failed) {
                        Spacer(modifier = Modifier.height(8.dp))
                         Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cancel, contentDescription = "Failed", tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Device Not Found", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repo.claimDevice(
                            deviceId = claimDeviceId.trim(),
                            name = claimDeviceName.ifBlank { "My Device" },
                            onSuccess = {
                                showClaimDialog = false
                                android.widget.Toast.makeText(context, "Device Claimed Successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onError = { e ->
                                android.widget.Toast.makeText(context, "Error: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ) { Text("Claim") }
            },
            dismissButton = {
                TextButton(onClick = { showClaimDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Spectate Dialog
    if (showSpectateDialog) {
        AlertDialog(
            onDismissRequest = { showSpectateDialog = false },
            title = { Text("Spectate Device") },
            text = {
                Column {
                    Text("Enter the 6-character Share Code from the device owner.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = spectateCode,
                        onValueChange = { spectateCode = it.uppercase() },
                        label = { Text("Share Code (e.g. A1B2C3)") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (spectateCode.length == 6) {
                            repo.linkDevice(
                                shareCode = spectateCode,
                                onSuccess = {
                                    showSpectateDialog = false
                                    android.widget.Toast.makeText(context, "Device Linked!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                onError = {
                                    android.widget.Toast.makeText(context, "Invalid Code", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                ) { Text("Link") }
            },
            dismissButton = {
                TextButton(onClick = { showSpectateDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Devices") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddOptionsDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Device")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (devices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No devices found. Add one!", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(devices) { device ->
                        DeviceListItem(
                            device = device,
                            onDelete = { 
                                deviceToDelete = device
                            },
                            onEdit = {
                                selectedDevice = device
                                newName = device.name
                                showEditDialog = true
                            },
                            onClearHistory = {
                                viewModel.clearDeviceHistory(device.id)
                                android.widget.Toast.makeText(context, "History Cleared", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onCopyCode = {
                                if (device.shareCode.isNotEmpty()) {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(device.shareCode))
                                     android.widget.Toast.makeText(context, "Code Copied: ${device.shareCode}",  android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Nuclear Delete Confirmation
    if (deviceToDelete != null) {
        AlertDialog(
            onDismissRequest = { deviceToDelete = null },
            title = { Text("Delete Device?") },
            text = { Text("This will Factory Reset the device (wipe WiFi) and remove it from your account. This cannot be undone.", color = MaterialTheme.colorScheme.error) },
            confirmButton = {
                Button(
                    onClick = {
                        deviceToDelete?.let { device ->
                            viewModel.deleteDeviceFully(device.id)
                            android.widget.Toast.makeText(context, "Device Reset & Removed", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        deviceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete & Reset") }
            },
            dismissButton = {
                TextButton(onClick = { deviceToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun DeviceListItem(
    device: com.joshua.chokepoint.data.firestore.FirestoreRepository.Device, 
    onDelete: () -> Unit, 
    onEdit: () -> Unit,
    onClearHistory: () -> Unit, // New callback
    onCopyCode: () -> Unit
) {
    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear History?") },
            text = { Text("This will permanently delete all sensor data for this device.") },
            confirmButton = {
                TextButton(onClick = { 
                    onClearHistory()
                    showClearConfirm = false 
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon based on Role
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = if (device.role == "admin") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondary.copy(alpha=0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                             Icon(
                                 Icons.Default.Sensors, 
                                 contentDescription = null, 
                                 tint = if (device.role == "admin") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                             )
                        }
                    }
                   
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = device.name, 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = com.joshua.chokepoint.ui.theme.GoodGreen,
                                modifier = Modifier.size(6.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if(device.role == "admin") "Admin • Share Code: ${device.shareCode.ifEmpty { "..." }}" else "Spectator", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Actions Menu
                Row {
                    if (device.role == "admin") {
                        IconButton(onClick = onCopyCode) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        // Clear History Button - Changed Icon to DateRange
                        IconButton(onClick = { showClearConfirm = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Clear History", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    // Delete Device Button (Nuclear)
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove Device", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
