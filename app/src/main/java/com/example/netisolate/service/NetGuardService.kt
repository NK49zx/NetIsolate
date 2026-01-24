package com.example.netisolate.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.netisolate.BuildConfig
import com.example.netisolate.MainActivity
import com.example.netisolate.R
import com.example.netisolate.data.AppRepository
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

import android.content.pm.ServiceInfo

class NetGuardService : VpnService() {

    companion object {
        const val ACTION_START = "com.example.netisolate.service.START"
        const val ACTION_STOP = "com.example.netisolate.service.STOP"
        const val ACTION_UPDATE = "com.example.netisolate.service.UPDATE"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "NetGuardChannel"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var appRepository: AppRepository

    override fun onCreate() {
        super.onCreate()
        appRepository = AppRepository(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID, 
                        createNotification(), 
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(NOTIFICATION_ID, createNotification())
                }
                appRepository.setVpnActive(true)
                startVPN()
            }
            ACTION_STOP -> {
                appRepository.setVpnActive(false)
                stopVPN()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_UPDATE -> {
                // Re-establish VPN with new list
                startVPN()
            }
        }
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val channelId = CHANNEL_ID
        val channelName = "NetGuard Service"
        
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, channelId)
            .setContentTitle("NetGuard Active")
            .setContentText("Restricting internet access for quarantined apps/s.")
            .setSmallIcon(R.mipmap.ic_launcher) // Use default icon
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun startVPN() {
        serviceScope.launch {
            val quarantinedApps = appRepository.getQuarantinedApps()
            
            // If no apps are quarantined, we technically don't need the VPN, 
            // but to keep the "Active" state visible, we can keep it creating an empty interface 
            // or we could stop it. The prompt implies a toggle. 
            // If the list is empty, we effectively block nothing.
            
            val builder = Builder()
            
            // Sanction-list strategy: Only allow quarantined apps to route to VPN.
            // Explicitly allowed apps are routed to this interface and dropped (sinkholed).
            // All other apps bypass the VPN and connect normally.
            
            var hasApps = false
            for (packageName in quarantinedApps) {
                try {
                    builder.addAllowedApplication(packageName)
                    hasApps = true
                    if (BuildConfig.DEBUG) {
                        Log.d("NetGuardService", "Blocking app: $packageName")
                    }
                } catch (e: Exception) {
                    Log.e("NetGuardService", "Failed to add app: $packageName", e)
                }
            }
            
            if (!hasApps) {
                // If list is empty, add a dummy package to force "allow-list" mode.
                // This ensures "allow-list" is active but empty, so no real apps are blocked.
                 builder.addAllowedApplication("com.example.nonexistent.dummy")
            }

            builder.setSession("NetGuard")
            builder.setMtu(1500)
            // Local private IP for the interface
            builder.addAddress("10.0.0.2", 32)
            builder.addRoute("0.0.0.0", 0)

            try {
                // Close old interface
                vpnInterface?.close()
                
                // Establish VPN. Traffic from allowed apps is restricted.
                vpnInterface = builder.establish()
                if (BuildConfig.DEBUG) {
                    Log.i("NetGuardService", "VPN Established. Blocked apps count: ${quarantinedApps.size}")
                }
                
                // No read/write loop needed; packets are dropped by default.
                
            } catch (e: Exception) {
                Log.e("NetGuardService", "Error establishing VPN", e)
            }
        }
    }

    private fun stopVPN() {
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: IOException) {
            Log.e("NetGuardService", "Error closing VPN", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVPN()
    }
}
