package com.suhas.mapschedule.drawing

import android.graphics.Rect
import kotlin.math.roundToInt

/**
 * Pixel bounds for positioning the live sketch overlay directly on top of the widget.
 */
data class OverlayBounds(
    val leftPx: Int,
    val topPx: Int,
    val widthPx: Int,
    val heightPx: Int
)

/**
 * Calculates exact in-place screen coordinates and dimensions for the drawing overlay canvas,
 * adapting seamlessly whether the user has shrunk the widget (small) or enlarged it (big),
 * placed on any side (left, right, center), and across different launcher behaviors (Vivo, Pixel, Samsung, etc.).
 */
object WidgetOverlayBoundsCalculator {

    fun calculateBounds(
        sourceBounds: Rect?,
        targetWidthPx: Int,
        targetHeightPx: Int,
        screenWidthPx: Int,
        screenHeightPx: Int,
        densityScale: Float,
        targetWidthDp: Int = 0,
        targetHeightDp: Int = 0
    ): OverlayBounds {
        if (sourceBounds == null) {
            return calculateBounds(
                rawLeft = null,
                rawTop = null,
                rawRight = null,
                rawBottom = null,
                targetWidthPx = targetWidthPx,
                targetHeightPx = targetHeightPx,
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx,
                densityScale = densityScale,
                targetWidthDp = targetWidthDp,
                targetHeightDp = targetHeightDp
            )
        }
        return calculateBounds(
            rawLeft = sourceBounds.left,
            rawTop = sourceBounds.top,
            rawRight = sourceBounds.right,
            rawBottom = sourceBounds.bottom,
            targetWidthPx = targetWidthPx,
            targetHeightPx = targetHeightPx,
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx,
            densityScale = densityScale,
            targetWidthDp = targetWidthDp,
            targetHeightDp = targetHeightDp
        )
    }

    fun calculateBounds(
        rawLeft: Int?,
        rawTop: Int?,
        rawRight: Int?,
        rawBottom: Int?,
        targetWidthPx: Int,
        targetHeightPx: Int,
        screenWidthPx: Int,
        screenHeightPx: Int,
        densityScale: Float,
        targetWidthDp: Int = 0,
        targetHeightDp: Int = 0
    ): OverlayBounds {
        val resolvedWidthDp = if (targetWidthDp > 0) targetWidthDp else if (targetWidthPx > 0) (targetWidthPx / densityScale).toInt() else 0
        val resolvedHeightDp = if (targetHeightDp > 0) targetHeightDp else if (targetHeightPx > 0) (targetHeightPx / densityScale).toInt() else 0

        // Determine column span (1 to 5 columns):
        val numCols = when {
            resolvedWidthDp in 1..90 -> 1
            resolvedWidthDp in 91..160 -> 2
            resolvedWidthDp in 161..230 -> 3
            resolvedWidthDp in 231..310 -> 4
            resolvedWidthDp > 310 -> 5
            rawLeft != null && rawRight != null && (rawRight - rawLeft) > 30 -> {
                val rawW = rawRight - rawLeft
                when {
                    rawW < 200 -> 1
                    rawW < 400 -> 2
                    rawW < 600 -> 3
                    rawW < 800 -> 4
                    else -> 5
                }
            }
            else -> 4
        }

        // Determine row span (1 to 4 rows):
        val numRows = when {
            resolvedHeightDp in 1..120 -> 1
            resolvedHeightDp in 121..220 -> 2
            resolvedHeightDp > 220 -> 3
            rawTop != null && rawBottom != null && (rawBottom - rawTop) > 30 -> {
                val rawH = rawBottom - rawTop
                if (rawH < 350) 1 else 2
            }
            else -> 2
        }

        val width = if (rawRight != null && rawLeft != null) rawRight - rawLeft else 0
        val height = if (rawBottom != null && rawTop != null) rawBottom - rawTop else 0

        if (rawLeft == null || rawTop == null || rawRight == null || rawBottom == null || width <= 30 || height <= 30) {
            val fallbackW = if (targetWidthPx > 40) targetWidthPx else when (numCols) {
                1 -> 124
                2 -> 322
                3 -> 520
                4 -> 718
                else -> (screenWidthPx - 162).coerceAtLeast(100)
            }
            val fallbackH = if (targetHeightPx > 40) targetHeightPx else if (numRows == 1) 176 else 463
            val left = ((screenWidthPx - fallbackW) / 2).coerceAtLeast(0)
            val top = ((screenHeightPx - fallbackH) / 2).coerceAtLeast(0)
            return OverlayBounds(left, top, fallbackW, fallbackH)
        }

        val rawWidth = rawRight - rawLeft
        val rawHeight = rawBottom - rawTop

        // 1. Precise Left and Top:
        // RemoteViews sourceBounds provides the origin of the widget host view.
        // A 2px left offset and 3px top offset aligns 1:1 with the visual shape's 1dp stroke and rounded corner.
        val leftPx = (rawLeft + 2).coerceAtLeast(0)
        val topPx = (rawTop + 3).coerceAtLeast(0)

        // 2. Precise Height:
        // On launchers like Vivo Launcher3, rawBottom is inflated by cell label area (~34dp = 94px).
        // If rawHeight is greater than targetHeightPx + 35, launcher has appended label padding to rawHeight.
        val labelPaddingPx = (38 * densityScale).toInt()
        val isHeightInflated = (targetHeightPx > 40 && rawHeight > targetHeightPx + 35) ||
                                (targetHeightPx <= 40 && rawHeight > labelPaddingPx + 150)
        val heightPx = if (isHeightInflated) {
            when (numRows) {
                1 -> 176
                2 -> 463
                else -> rawHeight - labelPaddingPx
            }
        } else {
            rawHeight
        }

        // 3. Precise Width:
        // On launchers like Vivo, rawRight represents unclipped workspace cell bounds (563 for 2-col, 857 for 3-col, 1172 for 5-col).
        // On standard launchers (e.g. Pixel Launcher), rawRight is already the exact widget boundary.
        val expectedWidthForCols = when (numCols) {
            1 -> 124
            2 -> 322
            3 -> 520
            4 -> 718
            else -> (screenWidthPx - 2 * 81).coerceAtLeast(100)
        }

        val isWidthInflated = rawRight >= screenWidthPx || (resolvedWidthDp > 0 && rawWidth > (resolvedWidthDp * densityScale * 1.25f)) || (resolvedWidthDp > 0 && rawWidth > expectedWidthForCols + 60)
        val widthPx = if (isWidthInflated) {
            if (numCols >= 5 || (rawRight >= screenWidthPx && leftPx <= 100 && resolvedWidthDp >= 320)) {
                // Full width widget spanning screen horizontally
                (screenWidthPx - 2 * leftPx).coerceAtLeast(100)
            } else {
                // Shrunk widget: exactly matches the column span size
                // wherever on the screen it is placed (left, right, or center)
                expectedWidthForCols.coerceIn(60, screenWidthPx - leftPx)
            }
        } else {
            // Standard launcher (e.g. Pixel Launcher) where rawRight provides the exact boundary
            rawWidth
        }

        // Clamp safely to screen boundaries
        val clampedLeft = leftPx.coerceIn(0, (screenWidthPx - 60).coerceAtLeast(0))
        val clampedTop = topPx.coerceIn(0, (screenHeightPx - 60).coerceAtLeast(0))
        val clampedWidth = widthPx.coerceIn(60, screenWidthPx - clampedLeft)
        val clampedHeight = heightPx.coerceIn(60, screenHeightPx - clampedTop)

        return OverlayBounds(
            leftPx = clampedLeft,
            topPx = clampedTop,
            widthPx = clampedWidth,
            heightPx = clampedHeight
        )
    }
}
