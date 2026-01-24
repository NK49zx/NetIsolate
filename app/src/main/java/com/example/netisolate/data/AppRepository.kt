package com.example.netisolate.data

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("quarantine_prefs", Context.MODE_PRIVATE)

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val apps = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        val quarantinedSet = getQuarantinedApps()

        apps.mapNotNull { packageInfo ->
            val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null
            
            // Filter core system apps. Retain user-facing apps (with launch intent).
            val launchIntent = packageManager.getLaunchIntentForPackage(packageInfo.packageName)
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            
            if (launchIntent != null || !isSystem) {
                AppInfo(
                    name = packageManager.getApplicationLabel(appInfo).toString(),
                    packageName = packageInfo.packageName,
                    icon = packageManager.getApplicationIcon(appInfo),
                    isSystem = isSystem,
                    isQuarantined = quarantinedSet.contains(packageInfo.packageName),
                    uid = appInfo.uid
                )
            } else {
                null
            }
        }.sortedBy { it.name }
    }

    fun getQuarantinedApps(): Set<String> {
        return prefs.getStringSet("quarantined_apps", emptySet()) ?: emptySet()
    }

    fun addToQuarantine(packageName: String) {
        val current = HashSet(getQuarantinedApps())
        current.add(packageName)
        prefs.edit(commit = true) { putStringSet("quarantined_apps", current) }
    }

    fun removeFromQuarantine(packageName: String) {
        val current = HashSet(getQuarantinedApps())
        current.remove(packageName)
        prefs.edit(commit = true) { putStringSet("quarantined_apps", current) }
    }
    
    fun setVpnActive(isActive: Boolean) {
        prefs.edit { putBoolean("vpn_active", isActive) }
    }

    fun isVpnActive(): Boolean {
        return prefs.getBoolean("vpn_active", false)
    }
    
    // Function "isQuarantined" removed as it was unused.
}
