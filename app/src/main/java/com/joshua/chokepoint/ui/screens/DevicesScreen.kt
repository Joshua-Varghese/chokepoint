package com.joshua.chokepoint.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
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
    viewModel: DevicesViewModel = androidx.lifecycle.viewmodel.compose.viewModel { 
        DevicesViewModel(com.joshua.chokepoint.data.firestore.FirestoreRepository()) 
    }
) {
    val devices by viewModel.devices.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddOptionsDialog by remember { mutableStateOf(false) }
    var showSpectateDialog by remember { mutableStateOf(false) }
    var selectedDevice by remember { mutableStateOf<com.joshua.chokepoint.data.firestore.FirestoreRepository.Device?>(null) }
    var newName by remember { mutableStateOf("") }
    var spectateCode by remember { mutableStateOf("") }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val repo = remember { com.joshua.chokepoint.data.firestore.FirestoreRepository() }

    // Rename Dialog
    if (showEditDialog && selectedDevice != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Rename Device") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Device Name") }
                )
            },
            confirmButton = {
                TextButton(
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
            text = { Text("Do you want to setup a new device or spectate an existing one?") },
            confirmButton = {
                Button(
                   onClick = {
                       showAddOptionsDialog = false
                       onAddDeviceClick() // Go to Provisioning
                   } 
                ) { Text("Setup New Device") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddOptionsDialog = false
                        showSpectateDialog = true // Open Spectate Input
                    }
                ) { Text("Spectate Existing") }
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
                            onDelete = { viewModel.removeDevice(device.id) },
                            onEdit = {
                                selectedDevice = device
                                newName = device.name
                                showEditDialog = true
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
}

@Composable
fun DeviceListItem(
    device: com.joshua.chokepoint.data.firestore.FirestoreRepository.Device, 
    onDelete: () -> Unit, 
    onEdit: () -> Unit,
    onCopyCode: () -> Unit
) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    
                    Column {
                        Text(
                            text = device.name, 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
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
                     }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error.copy(alpha=0.7f))
                    }
                }
            }
        }
    }
}
