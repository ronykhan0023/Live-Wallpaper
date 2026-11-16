package com.example.util

import android.app.WallpaperManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import com.example.service.RonyLiveWallpaperService
import java.util.Locale

object WallpaperHelper {

    private const val DEVELOPER_WHATSAPP_URL = "https://wa.me/8801637360712"
    const val DONATION_NUMBER = "01637360712"
    const val BKASH_PACKAGE = "com.bKash.customerapp"
    const val NAGAD_PACKAGE = "com.konasl.nagad"

    fun isRonyWallpaperActive(context: Context): Boolean {
        return runCatching {
            val wm = WallpaperManager.getInstance(context)
            val info = wm.wallpaperInfo
            info?.component == ComponentName(context, RonyLiveWallpaperService::class.java)
        }.getOrDefault(false)
    }

    fun launchLiveWallpaperPicker(context: Context): Boolean {
        val component = ComponentName(context, RonyLiveWallpaperService::class.java)
        val directIntent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val launchedDirect = runCatching {
            context.startActivity(directIntent)
            true
        }.getOrDefault(false)
        if (launchedDirect) return true

        val chooserIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(chooserIntent)
            true
        }.getOrDefault(false)
    }

    fun openDeveloperWhatsApp(context: Context): Boolean {
        val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse(DEVELOPER_WHATSAPP_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(waIntent)
            true
        }.getOrDefault(false)
    }

    fun copyDonationNumber(context: Context): Boolean {
        return runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return false
            clipboard.setPrimaryClip(ClipData.newPlainText("Donation Number", DONATION_NUMBER))
            true
        }.getOrDefault(false)
    }

    fun openDonationApp(context: Context, packageName: String): Boolean {
        copyDonationNumber(context)
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } ?: return false
        return runCatching {
            context.startActivity(launchIntent)
            true
        }.getOrDefault(false)
    }

    /**
     * Probes known OEM Auto-Start / Background Startup manager screens (Vivo, Xiaomi,
     * Oppo/Realme/OnePlus, Honor/Huawei, Asus) and falls back safely to App Info settings.
     */
    fun openAutoStartSettings(context: Context): Boolean {
        val oemCandidates = listOf(
            ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
            ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
            ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"),
            ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity"),
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
        )

        val pm = context.packageManager
        for (component in oemCandidates) {
            val intent = Intent().apply {
                this.component = component
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val resolved = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolved != null) {
                val started = runCatching {
                    context.startActivity(intent)
                    true
                }.getOrDefault(false)
                if (started) return true
            }
        }
        return openAppInfoSettings(context)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return runCatching {
            pm.isIgnoringBatteryOptimizations(context.packageName)
        }.getOrDefault(false)
    }

    fun openBatteryOptimizationSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false) || openAppInfoSettings(context)
    }

    fun openAppInfoSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 MB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            else -> String.format(Locale.US, "%.0f KB", kb.coerceAtLeast(1.0))
        }
    }

    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0L) return "0:00"
        val totalSeconds = durationMs / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }
}
