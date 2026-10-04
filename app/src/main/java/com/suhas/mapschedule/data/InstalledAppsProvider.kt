package com.suhas.mapschedule.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.os.UserManager

/**
 * Model representing an installed launchable application.
 */
data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable? = null
)

/**
 * Queries Android PackageManager and LauncherApps dynamically for launchable applications.
 */
class InstalledAppsProvider(private val context: Context) {

    /**
     * Retrieves all installed launchable apps, sorted alphabetically by name.
     * Restricts discovery strictly to real user-facing launcher apps that appear in the app drawer:
     * 1. LauncherApps system service across all user profiles (official launcher standard)
     * 2. PackageManager CATEGORY_LAUNCHER query with MATCH_ALL (fallback)
     *
     * Hidden internal system components, firmware daemons, and non-launcher packages
     * are strictly excluded so only real apps on the user's phone are shown.
     */
    fun getLaunchableApps(): List<AppInfo> {
        val pm = context.packageManager
        val ownPackage = context.packageName
        val appMap = mutableMapOf<String, AppInfo>()

        // 1. LauncherApps service: this is what Android home-screen launchers use to populate the app drawer.
        // It handles standard profiles, work profiles, cloned/dual apps, and OEM user-facing apps.
        try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
            val profiles = try {
                userManager?.userProfiles ?: listOf(Process.myUserHandle())
            } catch (_: Exception) {
                listOf(Process.myUserHandle())
            }

            if (launcherApps != null) {
                for (profile in profiles) {
                    try {
                        val activityList = launcherApps.getActivityList(null, profile)
                        for (activity in activityList) {
                            val pkg = activity.applicationInfo.packageName
                            if (pkg == ownPackage) continue
                            if (!activity.applicationInfo.enabled) continue

                            val label = try {
                                activity.label?.toString()?.trim()
                            } catch (_: Exception) {
                                null
                            }
                            val appName = if (!label.isNullOrBlank()) {
                                label
                            } else {
                                try {
                                    activity.applicationInfo.loadLabel(pm).toString().trim().ifBlank { pkg }
                                } catch (_: Exception) {
                                    pkg
                                }
                            }

                            val icon = try {
                                activity.getBadgedIcon(0)
                            } catch (_: Exception) {
                                try {
                                    activity.applicationInfo.loadIcon(pm)
                                } catch (_: Exception) {
                                    null
                                }
                            }
                            appMap[pkg] = AppInfo(appName = appName, packageName = pkg, icon = icon)
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("MapSchedule", "Error querying LauncherApps for profile $profile", e)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("MapSchedule", "LauncherApps discovery fallback triggered", e)
        }

        // 2. PackageManager CATEGORY_LAUNCHER query (standard launcher intent fallback)
        try {
            val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolves = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
            } else {
                pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            }
            for (resolve in resolves) {
                val pkg = resolve.activityInfo.packageName
                if (pkg == ownPackage || appMap.containsKey(pkg)) continue
                if (!resolve.activityInfo.isEnabled) continue
                if (!resolve.activityInfo.applicationInfo.enabled) continue

                val appName = try {
                    resolve.loadLabel(pm).toString().trim().ifBlank { pkg }
                } catch (_: Exception) {
                    pkg
                }
                val icon = try {
                    resolve.loadIcon(pm)
                } catch (_: Exception) {
                    null
                }
                appMap[pkg] = AppInfo(appName = appName, packageName = pkg, icon = icon)
            }
        } catch (e: Exception) {
            android.util.Log.w("MapSchedule", "CATEGORY_LAUNCHER query error", e)
        }

        return appMap.values.sortedBy { it.appName.lowercase() }
    }

    /**
     * Checks if a package is currently installed and has a launch intent.
     */
    fun isAppInstalled(packageName: String): Boolean {
        return context.packageManager.getLaunchIntentForPackage(packageName) != null
    }

    /**
     * Returns the launch intent for the given package name, if available.
     */
    fun getLaunchIntent(packageName: String): Intent? {
        val pm = context.packageManager
        val direct = pm.getLaunchIntentForPackage(packageName)
        if (direct != null) {
            return direct.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
        }
        return null
    }
}
