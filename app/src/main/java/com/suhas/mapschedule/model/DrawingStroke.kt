package com.suhas.mapschedule.model

/**
 * Represents a single 2D point in normalized or canvas space.
 */
data class DrawingPoint(
    val x: Float,
    val y: Float
)

/**
 * Represents a single stroke drawn by the user, consisting of ordered points
 * and the stroke width.
 */
data class DrawingStroke(
    val points: List<DrawingPoint>,
    val strokeWidth: Float = 12f,
    val timestamp: Long = System.currentTimeMillis()
)
