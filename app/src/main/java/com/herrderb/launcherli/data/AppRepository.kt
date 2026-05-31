package com.herrderb.launcherli.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.UserManager

data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable? = null
)

class AppRepository(private val context: Context) {

    fun getInstalledApps(): List<AppInfo> {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val profile = userManager.userProfiles.first()

        return launcherApps.getActivityList(null, profile)
            .map { info ->
                AppInfo(
                    label = info.label.toString(),
                    packageName = info.applicationInfo.packageName,
                    activityName = info.componentName.className,
                    icon = info.getBadgedIcon(0)
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun launchApp(appInfo: AppInfo) {
        val intent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
