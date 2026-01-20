package com.joshua.chokepoint.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.joshua.chokepoint.ui.theme.MintBackground
import com.joshua.chokepoint.ui.theme.TextLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvisioningScreen(
    onBackClick: () -> Unit,
    onProvisionComplete: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Device", color = TextLight, fontWeight = FontWeight.Bold) },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(Icons.Default.Wifi, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)

            Text(
                "Connect to Device WiFi",
                style = MaterialTheme.typography.headlineMedium,
                color = TextLight, 
                fontWeight = FontWeight.Bold
            )

            Text(
                "1. Open WiFi Settings.\n2. Connect to 'Chokepoint-XXXX'.\n3. Tap below to configure.",
                color = TextLight,
                style = MaterialTheme.typography.bodyLarge
            )

            Button(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Open WiFi Settings")
            }

            Button(
                onClick = {
                     val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://192.168.4.1"))
                     context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Configure Device (Open Browser)")
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onProvisionComplete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Text("I'm Done")
            }
        }
    }
}
