package com.joshua.chokepoint.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showEditDialog by remember { mutableStateOf(false) }
    var newNameInput by remember { mutableStateOf("") }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Name") },
            text = {
                TextField(
                    value = newNameInput,
                    onValueChange = { newNameInput = it },
                    label = { Text("Full Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateName(newNameInput)
                    showEditDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                userProfile?.let { user ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar
                        Surface(
                            shape = CircleShape,
                            modifier = Modifier.size(100.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = user.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Name
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { 
                                newNameInput = user.name
                                showEditDialog = true 
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Details Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Account Details", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Member Since: ${formatDate(user.createdAt)}")
                                Text("User ID: ${user.id}")
                            }
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Sign Out
                        Button(
                            onClick = onSignOutClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign Out")
                        }
                    }
                } ?: run {
                     Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                         Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                             Text("User Profile Not Found", style = MaterialTheme.typography.titleLarge)
                             Spacer(modifier = Modifier.height(8.dp))
                             
                             val errorMsg = viewModel.error.collectAsState().value
                             if (errorMsg != null) {
                                 Text(errorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                                 Spacer(modifier = Modifier.height(16.dp))
                             }
                             
                             Button(onClick = { viewModel.loadProfile() }) {
                                 Text("Retry")
                             }
                             Spacer(modifier = Modifier.height(8.dp))
                             TextButton(onClick = onSignOutClick) {
                                 Text("Sign Out / Login")
                             }
                         }
                     }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return if (timestamp > 0) {
        val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        sdf.format(Date(timestamp * 1000))
    } else {
        "Unknown"
    }
}
