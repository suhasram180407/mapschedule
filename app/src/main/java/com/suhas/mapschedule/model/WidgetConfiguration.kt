package com.suhas.mapschedule.model

/**
 * Associates an Android AppWidget instance ID with a specific AppMapping ID.
 *
 * @param widgetId The appWidgetId assigned by AppWidgetManager
 * @param mappingId The ID of the AppMapping displayed on this widget instance
 */
data class WidgetConfiguration(
    val widgetId: Int,
    val mappingId: String
)
