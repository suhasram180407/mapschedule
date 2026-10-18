package com.suhas.mapschedule.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.suhas.mapschedule.R
import com.suhas.mapschedule.drawing.DrawingRenderer
import com.suhas.mapschedule.drawing.LiveDrawingActivity
import com.suhas.mapschedule.repository.MappingRepository

/**
 * Handles updating AppWidget RemoteViews with the clean black drawing pad
 * and setting up instant zero-animation tap PendingIntent to LiveDrawingActivity.
 */
object MapScheduleWidgetUpdater {

    const val ACTION_WIDGET_TAP = "com.suhas.mapschedule.WIDGET_TAP"
    const val EXTRA_MAPPING_ID = "extra_mapping_id"
    const val EXTRA_PACKAGE_NAME = "extra_package_name"

    fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        optionsBundle: android.os.Bundle? = null
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_map_schedule)

        val options = optionsBundle ?: appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidthDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
        val maxWidthDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) ?: 0
        val minHeightDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 0
        val maxHeightDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) ?: 0

        android.util.Log.i(
            "MapSchedule",
            "Widget update: widgetId=$appWidgetId, minW=$minWidthDp, maxW=$maxWidthDp, minH=$minHeightDp, maxH=$maxHeightDp"
        )

        // The widget is the clean, solid black interactive drawing pad
        views.setContentDescription(R.id.widget_root, "Map Schedule Drawing Pad")

        // Setup instant 0ms tap intent to LiveDrawingActivity with FLAG_ACTIVITY_NO_ANIMATION and FLAG_MUTABLE
        // (FLAG_MUTABLE allows the Android Launcher to attach intent.sourceBounds for exact in-place pixel alignment)
        val tapIntent = Intent(context, LiveDrawingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(LiveDrawingActivity.EXTRA_FROM_WIDGET_TAP, true)
            putExtra(LiveDrawingActivity.EXTRA_WIDGET_MIN_WIDTH_DP, minWidthDp)
            putExtra(LiveDrawingActivity.EXTRA_WIDGET_MAX_WIDTH_DP, maxWidthDp)
            putExtra(LiveDrawingActivity.EXTRA_WIDGET_MIN_HEIGHT_DP, minHeightDp)
            putExtra(LiveDrawingActivity.EXTRA_WIDGET_MAX_HEIGHT_DP, maxHeightDp)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, MapScheduleWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }
}
