package com.example.netisolate.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.netisolate.data.AppInfo
import com.example.netisolate.data.AppRepository
import com.example.netisolate.service.NetGuardService
import com.example.netisolate.ui.theme.CyberGreen
import com.example.netisolate.ui.theme.CyberMediumGray
import com.example.netisolate.ui.theme.CyberRed
import kotlinx.coroutines.launch

@Composable
fun QuarantineScreen(
    appRepository: AppRepository,
    isServiceRunning: Boolean,
    refreshKey: Int,
    onListUpdated: () -> Unit,
    onServiceToggle: (Boolean) -> Unit
) {
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    val scope = rememberCoroutineScope()

    // Load only quarantined apps
    LaunchedEffect(refreshKey) {
        val allApps = appRepository.getInstalledApps()
        apps = allApps.filter { it.isQuarantined }
    }
    
    // Refresh triggered from parent technically re-renders this

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        
        // Master Switch Card
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, if(isServiceRunning) CyberGreen else CyberRed, CutCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp), // Increased vertical padding for better spacing
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isServiceRunning) "STATUS: ISOLATED" else "STATUS: CONNECTED",
                    color = if (isServiceRunning) CyberGreen else CyberRed,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Switch(
                    checked = isServiceRunning,
                    onCheckedChange = onServiceToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberGreen,
                        checkedTrackColor = CyberGreen.copy(alpha = 0.5f),
                        uncheckedThumbColor = CyberRed,
                        uncheckedTrackColor = CyberRed.copy(alpha = 0.5f),
                        uncheckedBorderColor = CyberRed,
                        checkedBorderColor = CyberGreen
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (isServiceRunning) "SWITCH_ACTIVATED" else "SWITCH_INACTIVE",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "TARGETS [${apps.size}]",
            style = MaterialTheme.typography.titleMedium,
            color = CyberGreen,
            modifier = Modifier.padding(vertical = 8.dp)

        )
        
        DefaultDivider()

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(apps) { app ->
                 Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App Icon or Placeholder
                    if (app.icon != null) {
                        Image(
                            bitmap = app.icon.toBitmap().asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(CyberMediumGray)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = app.name,
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    
                    TextButton(
                        onClick = {
                            scope.launch {
                                appRepository.removeFromQuarantine(app.packageName)
                                // Update local list
                                apps = apps.filter { it.packageName != app.packageName }
                                onListUpdated()
                            }
                        }
                    ) {
                        Text("[RESTORE]", color = CyberGreen)
                    }
                 }
                 DefaultDivider()
            }
        }
    }
}

@Composable
fun DefaultDivider() {
    Divider(color = Color.DarkGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
}
