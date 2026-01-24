package com.example.netisolate.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.netisolate.data.AppInfo
import com.example.netisolate.data.AppRepository
import com.example.netisolate.ui.theme.CyberGreen
import com.example.netisolate.ui.theme.CyberGreenDim
import com.example.netisolate.ui.theme.CyberMediumGray
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistryScreen(
    appRepository: AppRepository,
    refreshKey: Int,
    onServiceUpdate: () -> Unit // Only notify service, don't force reload
) {
    var searchQuery by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Load apps
    LaunchedEffect(refreshKey) {
        isLoading = true
        apps = appRepository.getInstalledApps()
        isLoading = false
    }

    val filteredApps = apps.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
    }
    
    val selectedApps = filteredApps.filter { it.isQuarantined }
    val unselectedApps = filteredApps.filter { !it.isQuarantined }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("SEARCH", color = CyberGreenDim) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberGreen,
                unfocusedBorderColor = CyberGreenDim,
                cursorColor = CyberGreen,
                focusedTextColor = CyberGreen,
                unfocusedTextColor = CyberGreen
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyberGreen)
            }
        } else {
            LazyColumn {
                // SECTION: SELECTED
                if (selectedApps.isNotEmpty()) {
                    item {
                        Text(
                            text = "SELECTED [${selectedApps.size}]",
                            style = MaterialTheme.typography.titleSmall,
                            color = CyberGreen,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Divider(color = CyberGreen, thickness = 1.dp)
                    }
                    items(selectedApps, key = { it.packageName }) { app ->
                        AppItemRow(
                            app = app,
                            onSelect = { /* Already selected, do nothing or show message */ }
                        )
                        Divider(color = CyberMediumGray, thickness = 0.5.dp)
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                // SECTION: UNSELECTED
                if (unselectedApps.isNotEmpty()) {
                    item {
                        Text(
                            text = "UNSELECTED [${unselectedApps.size}]",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Divider(color = Color.Gray, thickness = 1.dp)
                    }
                    items(unselectedApps, key = { it.packageName }) { app ->
                        AppItemRow(
                            app = app,
                            onSelect = {
                                scope.launch {
                                    appRepository.addToQuarantine(app.packageName)
                                    // Optimistic local update
                                    apps = apps.map {
                                        if (it.packageName == app.packageName) it.copy(isQuarantined = true) else it
                                    }
                                    onServiceUpdate()
                                }
                            }
                        )
                        Divider(color = CyberMediumGray, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun AppItemRow(app: AppInfo, onSelect: () -> Unit) {
    val isSelected = app.isQuarantined
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable(enabled = !isSelected) { onSelect() },
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

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) CyberGreen else Color.LightGray
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        if (!isSelected) {
            Checkbox(
                checked = false,
                onCheckedChange = { onSelect() },
                colors = CheckboxDefaults.colors(
                    checkedColor = CyberGreen,
                    uncheckedColor = CyberGreenDim,
                    checkmarkColor = Color.Black
                )
            )
        }
    }
}
