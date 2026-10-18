package com.suhas.mapschedule.drawing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetOverlayBoundsCalculatorTest {

    private val densityScale = 2.475f
    private val screenWidth = 1080
    private val screenHeight = 2400

    @Test
    fun testVivoFullWidthWidgetOverlay() {
        val targetWidthPx = (344 * densityScale).toInt()
        val targetHeightPx = (180 * densityScale).toInt()

        val bounds = WidgetOverlayBoundsCalculator.calculateBounds(
            rawLeft = 79,
            rawTop = 505,
            rawRight = 1172,
            rawBottom = 1062,
            targetWidthPx = targetWidthPx,
            targetHeightPx = targetHeightPx,
            screenWidthPx = screenWidth,
            screenHeightPx = screenHeight,
            densityScale = densityScale,
            targetWidthDp = 344,
            targetHeightDp = 180
        )

        assertEquals(81, bounds.leftPx)
        assertEquals(508, bounds.topPx)
        assertEquals(screenWidth - 2 * 81, bounds.widthPx)
        assertEquals(463, bounds.heightPx)
    }

    @Test
    fun testVivoShrunk3x1WidgetOverlay() {
        val targetWidthPx = (200 * densityScale).toInt()
        val targetHeightPx = (82 * densityScale).toInt()

        val bounds = WidgetOverlayBoundsCalculator.calculateBounds(
            rawLeft = 79,
            rawTop = 505,
            rawRight = 857,
            rawBottom = 775,
            targetWidthPx = targetWidthPx,
            targetHeightPx = targetHeightPx,
            screenWidthPx = screenWidth,
            screenHeightPx = screenHeight,
            densityScale = densityScale,
            targetWidthDp = 200,
            targetHeightDp = 82
        )

        assertEquals(81, bounds.leftPx)
        assertEquals(508, bounds.topPx)
        assertEquals(520, bounds.widthPx)
        assertEquals(176, bounds.heightPx)
    }

    @Test
    fun testVivoShrunk2x1WidgetOverlay() {
        val targetWidthPx = (128 * densityScale).toInt()
        val targetHeightPx = (82 * densityScale).toInt()

        val bounds = WidgetOverlayBoundsCalculator.calculateBounds(
            rawLeft = 79,
            rawTop = 505,
            rawRight = 563,
            rawBottom = 775,
            targetWidthPx = targetWidthPx,
            targetHeightPx = targetHeightPx,
            screenWidthPx = screenWidth,
            screenHeightPx = screenHeight,
            densityScale = densityScale,
            targetWidthDp = 128,
            targetHeightDp = 82
        )

        assertEquals(81, bounds.leftPx)
        assertEquals(508, bounds.topPx)
        assertEquals(322, bounds.widthPx)
        assertEquals(176, bounds.heightPx)
    }

    @Test
    fun testVivoShrunk3x2WidgetOverlay() {
        val targetWidthPx = (200 * densityScale).toInt()
        val targetHeightPx = (180 * densityScale).toInt()

        val bounds = WidgetOverlayBoundsCalculator.calculateBounds(
            rawLeft = 79,
            rawTop = 505,
            rawRight = 702,
            rawBottom = 1062,
            targetWidthPx = targetWidthPx,
            targetHeightPx = targetHeightPx,
            screenWidthPx = screenWidth,
            screenHeightPx = screenHeight,
            densityScale = densityScale,
            targetWidthDp = 200,
            targetHeightDp = 180
        )

        assertEquals(81, bounds.leftPx)
        assertEquals(508, bounds.topPx)
        assertEquals(520, bounds.widthPx)
        assertEquals(463, bounds.heightPx)
    }

    @Test
    fun testStandardLauncherBounds() {
        // Standard Pixel Launcher where bounds match widget exactly without inflation
        val targetWidthPx = (180 * densityScale).toInt()
        val targetHeightPx = (120 * densityScale).toInt()

        val bounds = WidgetOverlayBoundsCalculator.calculateBounds(
            rawLeft = 48,
            rawTop = 500,
            rawRight = 490,
            rawBottom = 750,
            targetWidthPx = targetWidthPx,
            targetHeightPx = targetHeightPx,
            screenWidthPx = screenWidth,
            screenHeightPx = screenHeight,
            densityScale = densityScale,
            targetWidthDp = 180,
            targetHeightDp = 120
        )

        assertEquals(50, bounds.leftPx)
        assertEquals(503, bounds.topPx)
        assertEquals(490 - 48, bounds.widthPx)
        assertEquals(750 - 500, bounds.heightPx)
    }

    @Test
    fun testNullSourceBoundsFallback() {
        val targetWidthPx = (360 * densityScale).toInt()
        val targetHeightPx = (190 * densityScale).toInt()

        val bounds = WidgetOverlayBoundsCalculator.calculateBounds(
            rawLeft = null,
            rawTop = null,
            rawRight = null,
            rawBottom = null,
            targetWidthPx = targetWidthPx,
            targetHeightPx = targetHeightPx,
            screenWidthPx = screenWidth,
            screenHeightPx = screenHeight,
            densityScale = densityScale
        )

        assertEquals(targetWidthPx, bounds.widthPx)
        assertEquals(targetHeightPx, bounds.heightPx)
        assertTrue(bounds.leftPx >= 0)
        assertTrue(bounds.topPx >= 0)
    }

    @Test
    fun testVivoShrunk2x1WidgetOverlayOnRightSide() {
        val targetWidthPx = (128 * densityScale).toInt()
        val targetHeightPx = (82 * densityScale).toInt()

        val bounds = WidgetOverlayBoundsCalculator.calculateBounds(
            rawLeft = 673,
            rawTop = 505,
            rawRight = 1157,
            rawBottom = 775,
            targetWidthPx = targetWidthPx,
            targetHeightPx = targetHeightPx,
            screenWidthPx = screenWidth,
            screenHeightPx = screenHeight,
            densityScale = densityScale,
            targetWidthDp = 128,
            targetHeightDp = 82
        )

        assertEquals(675, bounds.leftPx)
        assertEquals(508, bounds.topPx)
        assertEquals(322, bounds.widthPx)
        assertEquals(176, bounds.heightPx)
    }

    @Test
    fun testVivoShrunk2x1WidgetOverlayInMiddle() {
        val targetWidthPx = (128 * densityScale).toInt()
        val targetHeightPx = (82 * densityScale).toInt()

        val bounds = WidgetOverlayBoundsCalculator.calculateBounds(
            rawLeft = 277,
            rawTop = 505,
            rawRight = 761,
            rawBottom = 775,
            targetWidthPx = targetWidthPx,
            targetHeightPx = targetHeightPx,
            screenWidthPx = screenWidth,
            screenHeightPx = screenHeight,
            densityScale = densityScale,
            targetWidthDp = 128,
            targetHeightDp = 82
        )

        assertEquals(279, bounds.leftPx)
        assertEquals(508, bounds.topPx)
        assertEquals(322, bounds.widthPx)
        assertEquals(176, bounds.heightPx)
    }

    @Test
    fun testVivoShrunk4x1WidgetOverlay() {
        val targetWidthPx = (272 * densityScale).toInt()
        val targetHeightPx = (82 * densityScale).toInt()

        val bounds = WidgetOverlayBoundsCalculator.calculateBounds(
            rawLeft = 79,
            rawTop = 505,
            rawRight = 1000,
            rawBottom = 775,
            targetWidthPx = targetWidthPx,
            targetHeightPx = targetHeightPx,
            screenWidthPx = screenWidth,
            screenHeightPx = screenHeight,
            densityScale = densityScale,
            targetWidthDp = 272,
            targetHeightDp = 82
        )

        assertEquals(81, bounds.leftPx)
        assertEquals(508, bounds.topPx)
        assertEquals(718, bounds.widthPx)
        assertEquals(176, bounds.heightPx)
    }

    @Test
    fun testVivoShrunk1x1WidgetOverlay() {
        val targetWidthPx = (56 * densityScale).toInt()
        val targetHeightPx = (82 * densityScale).toInt()

        val bounds = WidgetOverlayBoundsCalculator.calculateBounds(
            rawLeft = 79,
            rawTop = 505,
            rawRight = 300,
            rawBottom = 775,
            targetWidthPx = targetWidthPx,
            targetHeightPx = targetHeightPx,
            screenWidthPx = screenWidth,
            screenHeightPx = screenHeight,
            densityScale = densityScale,
            targetWidthDp = 56,
            targetHeightDp = 82
        )

        assertEquals(81, bounds.leftPx)
        assertEquals(508, bounds.topPx)
        assertEquals(124, bounds.widthPx)
        assertEquals(176, bounds.heightPx)
    }
}
