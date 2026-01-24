package com.example.netisolate

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.netisolate.data.AppRepository
import com.example.netisolate.service.NetGuardService
import com.example.netisolate.ui.QuarantineScreen
import com.example.netisolate.ui.RegistryScreen
import com.example.netisolate.ui.theme.CyberGreen
import com.example.netisolate.ui.theme.CyberRed
import com.example.netisolate.ui.theme.NetIsolateTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var appRepository: AppRepository
    private var isVpnServiceRunning by mutableStateOf(false)

    // Launcher for VPN permission
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startNetGuardService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appRepository = AppRepository(this)
        
        // Restore state
        isVpnServiceRunning = appRepository.isVpnActive()

        setContent {
            NetIsolateTheme {
                MainScreen()
            }
        }
    }

    private fun startNetGuardService() {
        val intent = Intent(this, NetGuardService::class.java).apply {
            action = NetGuardService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        isVpnServiceRunning = true
    }
    
    private fun stopNetGuardService() {
        val intent = Intent(this, NetGuardService::class.java).apply {
            action = NetGuardService.ACTION_STOP
        }
        startService(intent)
        isVpnServiceRunning = false
    }

    private fun checkAndStartVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            // Permission not granted, ask for it
            vpnPermissionLauncher.launch(intent)
        } else {
            // Permission already granted
            startNetGuardService()
        }
    }

    @Composable
    fun MainScreen() {
        // Pager state for swipe navigation between Registry and Quarantine
        val pagerState = rememberPagerState(pageCount = { 2 })
        val scope = rememberCoroutineScope()
        
        // State refresh trigger for page change
        var refreshTrigger by remember { mutableIntStateOf(0) }
        
        // Refresh data on tab switch
        LaunchedEffect(pagerState.currentPage) {
            refreshTrigger++
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    containerColor = Color.Black,
                    contentColor = CyberGreen
                ) {
                    NavigationBarItem(
                        selected = pagerState.currentPage == 0,
                        onClick = { 
                            scope.launch { 
                                pagerState.animateScrollToPage(0) 
                            }
                        },
                        icon = { Text("REGISTRY", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = CyberGreen,
                            indicatorColor = CyberGreen,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 1,
                        onClick = { 
                            scope.launch { 
                                pagerState.animateScrollToPage(1) 
                            }
                        },
                        icon = { Text("QUARANTINE", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = CyberRed,
                            indicatorColor = CyberRed.copy(alpha = 0.8f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize().background(Color.Black)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    if (page == 0) {
                        RegistryScreen(
                             appRepository = appRepository,
                             refreshKey = refreshTrigger,
                             onServiceUpdate = { 
                                 // Notify service of changes without full UI reload
                                 if (isVpnServiceRunning) {
                                     val updateIntent = Intent(this@MainActivity, NetGuardService::class.java).apply {
                                         action = NetGuardService.ACTION_UPDATE
                                     }
                                     startService(updateIntent)
                                 }
                             }
                        )
                    } else {
                        QuarantineScreen(
                            appRepository = appRepository,
                            isServiceRunning = isVpnServiceRunning,
                            refreshKey = refreshTrigger,
                            onListUpdated = {
                                if (isVpnServiceRunning) {
                                     val updateIntent = Intent(this@MainActivity, NetGuardService::class.java).apply {
                                         action = NetGuardService.ACTION_UPDATE
                                     }
                                     startService(updateIntent)
                                 }
                                refreshTrigger++
                            },
                            onServiceToggle = { shouldBeOn ->
                                if (shouldBeOn) {
                                    checkAndStartVpn()
                                } else {
                                    stopNetGuardService()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}