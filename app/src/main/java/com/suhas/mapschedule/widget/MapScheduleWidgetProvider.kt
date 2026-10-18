package com.suhas.mapschedule.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.suhas.mapschedule.MainActivity
import com.suhas.mapschedule.data.InstalledAppsProvider
import com.suhas.mapschedule.repository.MappingRepository

/**
 * AppWidgetProvider handling widget lifecycle events and single/triple tap interactions.
 */
class MapScheduleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            MapScheduleWidgetUpdater.updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val repository = MappingRepository.getInstance(context)
        for (appWidgetId in appWidgetIds) {
            repository.removeWidgetMapping(appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        android.util.Log.i("MapSchedule", "Widget options changed for id=$appWidgetId")
        MapScheduleWidgetUpdater.updateWidget(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == MapScheduleWidgetUpdater.ACTION_WIDGET_TAP) {
            val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val packageName = intent.getStringExtra(MapScheduleWidgetUpdater.EXTRA_PACKAGE_NAME)
            handleWidgetTap(context.applicationContext, widgetId, packageName)
        }
    }

    private fun handleWidgetTap(context: Context, widgetId: Int, packageName: String?) {
        android.util.Log.i("MapSchedule", "Widget click: widgetId=$widgetId, pkg=$packageName")
        val now = System.currentTimeMillis()

        synchronized(tapLock) {
            // Remove taps older than multi-tap window (1000ms)
            val timestamps = tapHistory.getOrPut(widgetId) { mutableListOf() }
            timestamps.removeAll { now - it > MULTI_TAP_WINDOW_MS }
            timestamps.add(now)

            // Cancel any pending single-tap launch
            pendingLaunchRunnables[widgetId]?.let { mainHandler.removeCallbacks(it) }

            if (timestamps.size >= 3) {
                // TRIPLE TAP -> Open Map Schedule Main UI
                // Works even when Map Schedule is OFF (Requirement 20, 23, 24)
                android.util.Log.i("MapSchedule", "Triple tap detected on widget $widgetId -> Opening Main UI")
                timestamps.clear()
                pendingLaunchRunnables.remove(widgetId)
                openMapSchedule(context)
            } else {
                // Schedule launch after disambiguation window (400ms)
                val runnable = Runnable {
                    synchronized(tapLock) {
                        timestamps.clear()
                        pendingLaunchRunnables.remove(widgetId)
                    }
                    val repository = MappingRepository.getInstance(context)
                    // Check Master ON/OFF control
                    if (!repository.isMasterEnabled()) {
                        android.util.Log.i("MapSchedule", "Launcher disabled: Master switch is OFF")
                        Toast.makeText(context, "Map Schedule is OFF (triple-tap to open settings)", Toast.LENGTH_SHORT).show()
                        return@Runnable
                    }

                    // Single tap: Directly launch the mapped application
                    val targetPackage = packageName
                        ?: repository.getWidgetMapping(widgetId)?.packageName
                        ?: repository.getAllMappings().firstOrNull()?.packageName

                    if (!targetPackage.isNullOrBlank()) {
                        android.util.Log.i("MapSchedule", "Single tap confirmed for widget $widgetId -> Launching $targetPackage")
                        launchMappedApp(context, targetPackage)
                    } else {
                        android.util.Log.w("MapSchedule", "Single tap: No mapping configured for widget $widgetId")
                        Toast.makeText(context, "No application mapped (triple-tap to configure)", Toast.LENGTH_SHORT).show()
                    }
                }
                pendingLaunchRunnables[widgetId] = runnable
                mainHandler.postDelayed(runnable, SINGLE_TAP_DISAMBIGUATION_MS)
            }
        }
    }


    private fun launchMappedApp(context: Context, packageName: String?) {
        if (packageName.isNullOrBlank()) {
            android.util.Log.w("MapSchedule", "Cannot launch: package is blank")
            Toast.makeText(context, "No application mapped to this widget.", Toast.LENGTH_SHORT).show()
            openMapSchedule(context)
            return
        }

        val appsProvider = InstalledAppsProvider(context)
        if (!appsProvider.isAppInstalled(packageName)) {
            android.util.Log.w("MapSchedule", "Cannot launch: $packageName is no longer installed")
            Toast.makeText(context, "Mapped application is no longer installed.", Toast.LENGTH_LONG).show()
            openMapSchedule(context)
            return
        }

        val launchIntent = appsProvider.getLaunchIntent(packageName)
        if (launchIntent != null) {
            try {
                android.util.Log.i("MapSchedule", "Starting activity for package: $packageName")
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val options = android.app.ActivityOptions.makeBasic().apply {
                        pendingIntentBackgroundActivityStartMode = android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    }
                    context.startActivity(launchIntent, options.toBundle())
                } else {
                    context.startActivity(launchIntent)
                }
            } catch (e: Exception) {
                android.util.Log.e("MapSchedule", "Failed to launch $packageName", e)
                Toast.makeText(context, "Unable to launch application.", Toast.LENGTH_SHORT).show()
            }
        } else {
            android.util.Log.w("MapSchedule", "Cannot launch: launchIntent is null for $packageName")
            Toast.makeText(context, "Mapped application is no longer installed.", Toast.LENGTH_LONG).show()
            openMapSchedule(context)
        }
    }

    private fun openMapSchedule(context: Context) {
        android.util.Log.i("MapSchedule", "Opening MainActivity (Map Schedule Main UI)")
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = android.app.ActivityOptions.makeBasic().apply {
                    pendingIntentBackgroundActivityStartMode = android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                }
                context.startActivity(intent, options.toBundle())
            } else {
                context.startActivity(intent)
            }
        } catch (_: Exception) {
            // Fallback
        }
    }

    companion object {
        private const val MULTI_TAP_WINDOW_MS = 1000L
        private const val SINGLE_TAP_DISAMBIGUATION_MS = 400L

        private val tapLock = Any()
        private val tapHistory = mutableMapOf<Int, MutableList<Long>>()
        private val pendingLaunchRunnables = mutableMapOf<Int, Runnable>()
        private val mainHandler = Handler(Looper.getMainLooper())
    }
}
