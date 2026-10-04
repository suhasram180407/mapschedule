package com.suhas.mapschedule.drawing

import com.suhas.mapschedule.model.DrawingPoint
import com.suhas.mapschedule.model.DrawingStroke
import kotlin.math.max
import kotlin.math.min

/**
 * Normalizes user drawing strokes by:
 * 1. Finding the bounding box of all strokes.
 * 2. Trimming excess empty space.
 * 3. Preserving the exact aspect ratio (no horizontal/vertical distortion).
 * 4. Scaling uniformly to fit target dimensions.
 * 5. Centering within the target frame with safe padding to prevent clipping.
 */
object DrawingNormalization {

    data class BoundingBox(
        val minX: Float,
        val minY: Float,
        val maxX: Float,
        val maxY: Float
    ) {
        val width: Float get() = max(maxX - minX, 1f)
        val height: Float get() = max(maxY - minY, 1f)
    }

    /**
     * Computes the bounding box of a list of strokes.
     * Returns null if there are no points.
     */
    fun computeBoundingBox(strokes: List<DrawingStroke>): BoundingBox? {
        val allPoints = strokes.flatMap { it.points }
        if (allPoints.isEmpty()) return null

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (pt in allPoints) {
            if (pt.x < minX) minX = pt.x
            if (pt.x > maxX) maxX = pt.x
            if (pt.y < minY) minY = pt.y
            if (pt.y > maxY) maxY = pt.y
        }

        return BoundingBox(minX, minY, maxX, maxY)
    }

    /**
     * Normalizes strokes to fit into [targetWidth] x [targetHeight] box,
     * centered and aspect-ratio preserved.
     */
    fun normalize(
        strokes: List<DrawingStroke>,
        targetWidth: Float,
        targetHeight: Float,
        paddingFraction: Float = 0.12f
    ): List<DrawingStroke> {
        val bounds = computeBoundingBox(strokes) ?: return emptyList()

        val availWidth = targetWidth * (1f - 2f * paddingFraction)
        val availHeight = targetHeight * (1f - 2f * paddingFraction)

        val scale = min(availWidth / bounds.width, availHeight / bounds.height)

        val scaledWidth = bounds.width * scale
        val scaledHeight = bounds.height * scale

        val offsetX = (targetWidth - scaledWidth) / 2f
        val offsetY = (targetHeight - scaledHeight) / 2f

        return strokes.map { stroke ->
            val normalizedPoints = stroke.points.map { pt ->
                DrawingPoint(
                    x = offsetX + (pt.x - bounds.minX) * scale,
                    y = offsetY + (pt.y - bounds.minY) * scale
                )
            }
            val normalizedWidth = max(stroke.strokeWidth * scale, 4f)
            stroke.copy(points = normalizedPoints, strokeWidth = normalizedWidth)
        }
    }
}
