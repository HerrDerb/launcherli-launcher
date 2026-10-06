package com.herrderb.launcherli.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * One launchable activity. A package can expose several (e.g. Phone and Contacts),
 * and work-profile apps repeat a package under another user, so identity is [key].
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    /** UserManager serial of the owning profile; 0 is the main user. */
    val userSerial: Long = 0
) {
    val key: String
        get() = if (userSerial == 0L) "$packageName/$activityName" else "$packageName/$activityName@$userSerial"
}

/**
 * Resolves a stored app key. Entries saved before keys existed hold only a
 * package name; those fall back to the package's first activity.
 */
fun List<AppInfo>.findByKey(key: String): AppInfo? =
    find { it.key == key } ?: if ('/' !in key) find { it.packageName == key && it.userSerial == 0L } else null

class AppRepository(private val context: Context) {

    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager

    fun getInstalledApps(): List<AppInfo> {
        // Called on every package change, so cached icons can't go stale.
        iconCache.evictAll()
        return userManager.userProfiles.flatMap { user ->
            val serial = serialOf(user)
            launcherApps.getActivityList(null, user).map { info ->
                val label = info.label.toString()
                AppInfo(
                    label = if (serial == 0L) label else context.packageManager.getUserBadgedLabel(label, user).toString(),
                    packageName = info.applicationInfo.packageName,
                    activityName = info.componentName.className,
                    userSerial = serial
                )
            }
        }.sortedBy { it.label.lowercase() }
    }

    /**
     * Emits whenever installed apps change (install, uninstall, update, enable,
     * work profile paused/resumed). Event-driven, so nothing runs while idle.
     */
    fun appChanges(): Flow<Unit> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: UserHandle) { trySend(Unit) }
            override fun onPackageRemoved(packageName: String, user: UserHandle) { trySend(Unit) }
            override fun onPackageChanged(packageName: String, user: UserHandle) { trySend(Unit) }
            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) { trySend(Unit) }
            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) { trySend(Unit) }
        }
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        awaitClose { launcherApps.unregisterCallback(callback) }
    }

    /** Icon already decoded at [sizePx], or null. Cheap; safe on the main thread. */
    fun cachedIcon(app: AppInfo, sizePx: Int): ImageBitmap? = iconCache.get("${app.key}#$sizePx")

    /**
     * Decodes [app]'s icon at exactly [sizePx] (no upscaling blur) and caches it, so
     * scrolling a row back into view costs no binder call or decode. Call off-main.
     */
    fun loadIcon(app: AppInfo, sizePx: Int): ImageBitmap? {
        cachedIcon(app, sizePx)?.let { return it }
        val bitmap = loadIconDrawable(app)?.toBitmap(sizePx, sizePx)?.asImageBitmap() ?: return null
        iconCache.put("${app.key}#$sizePx", bitmap)
        return bitmap
    }

    private fun loadIconDrawable(app: AppInfo): Drawable? = try {
        val user = userOf(app) ?: return null
        launcherApps.getActivityList(app.packageName, user)
            .firstOrNull { it.componentName.className == app.activityName }
            ?.getBadgedIcon(0)
    } catch (_: Exception) {
        null
    }

    /** Starts exactly this activity in its own profile, not just the package's default. */
    fun launchApp(app: AppInfo) {
        try {
            val user = userOf(app) ?: return
            launcherApps.startMainActivity(ComponentName(app.packageName, app.activityName), user, null, null)
        } catch (_: Exception) {
            // Uninstalled, disabled, or work profile paused in the meantime.
        }
    }

    fun openAppInfo(app: AppInfo) {
        try {
            val user = userOf(app) ?: return
            launcherApps.startAppDetailsActivity(ComponentName(app.packageName, app.activityName), user, null, null)
        } catch (_: Exception) {
        }
    }

    private fun serialOf(user: UserHandle): Long =
        if (user == android.os.Process.myUserHandle()) 0L else userManager.getSerialNumberForUser(user)

    private fun userOf(app: AppInfo): UserHandle? =
        if (app.userSerial == 0L) android.os.Process.myUserHandle()
        else userManager.getUserForSerialNumber(app.userSerial)

    companion object {
        // Shared across instances; sized in bytes (about 200 icons at 96 px).
        private val iconCache = object : LruCache<String, ImageBitmap>(8 * 1024 * 1024) {
            override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
        }
    }
}
