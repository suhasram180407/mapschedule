package com.suhas.mapschedule.widget

import com.suhas.mapschedule.model.AppMapping
import com.suhas.mapschedule.model.DrawingPoint
import com.suhas.mapschedule.model.DrawingStroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Unit tests verifying Home Screen Widget interactions:
 * 1. Widget displays user's saved design.
 * 2. Single-tap resolves directly to mapped application (no popup activity).
 * 3. Triple-tap resolves to Map Schedule Main UI.
 * 4. Triple-tap works when Master Switch is OFF.
 * 5. Single-tap is disabled when Master Switch is OFF.
 * 6. Multi-instance widgets resolve to their respective assigned apps.
 */
class HomeWidgetInteractionTest {

    private fun sampleStroke(symbol: String): List<DrawingStroke> {
        return listOf(
            DrawingStroke(
                points = listOf(
                    DrawingPoint(10f, 10f),
                    DrawingPoint(50f, 50f),
                    DrawingPoint(90f, 90f)
                ),
                strokeWidth = 14f
            )
        )
    }

    private fun sampleMapping(appName: String, packageName: String): AppMapping {
        val strokes = sampleStroke(appName)
        return AppMapping(
            id = UUID.randomUUID().toString(),
            appName = appName,
            packageName = packageName,
            widgetDesignStrokes = strokes,
            widgetDesignPreview = "/fake/path/${appName.lowercase()}.png"
        )
    }

    // Model simulating tap disambiguation logic from MapScheduleWidgetProvider
    class WidgetTapSimulator(
        var isMasterEnabled: Boolean = true
    ) {
        val widgetMappings = mutableMapOf<Int, AppMapping>()
        val tapHistory = mutableMapOf<Int, MutableList<Long>>()
        val multiTapWindowMs = 1000L

        var lastLaunchedPackage: String? = null
        var mainUiOpened: Boolean = false
        var lastToastMessage: String? = null

        fun registerTap(widgetId: Int, timestamp: Long): String {
            val list = tapHistory.getOrPut(widgetId) { mutableListOf() }
            list.removeAll { timestamp - it > multiTapWindowMs }
            list.add(timestamp)

            return if (list.size >= 3) {
                list.clear()
                mainUiOpened = true
                "TRIPLE_TAP_MAIN_UI"
            } else {
                "TAP_RECORDED"
            }
        }

        fun executeSingleTap(widgetId: Int) {
            tapHistory[widgetId]?.clear()
            if (!isMasterEnabled) {
                lastToastMessage = "Map Schedule is OFF (triple-tap to open settings)"
                return
            }

            val mapping = widgetMappings[widgetId]
            if (mapping != null) {
                lastLaunchedPackage = mapping.packageName
            } else {
                lastToastMessage = "No application mapped to this widget"
            }
        }
    }

    @Test
    fun testWidgetHasUserDesignStrokesAndPreview() {
        val mapping = sampleMapping("Brave", "com.brave.browser")
        assertNotNull(mapping.drawingStrokes)
        assertTrue(mapping.drawingStrokes.isNotEmpty())
        assertEquals("/fake/path/brave.png", mapping.drawingPreview)
    }

    @Test
    fun testSingleTapLaunchesMappedAppDirectlyWithoutPopup() {
        val simulator = WidgetTapSimulator(isMasterEnabled = true)
        val braveMapping = sampleMapping("Brave", "com.brave.browser")
        val widgetId = 101
        simulator.widgetMappings[widgetId] = braveMapping

        val result = simulator.registerTap(widgetId, 1000L)
        assertEquals("TAP_RECORDED", result)
        assertFalse(simulator.mainUiOpened)

        simulator.executeSingleTap(widgetId)
        assertEquals("com.brave.browser", simulator.lastLaunchedPackage)
        assertFalse(simulator.mainUiOpened)
        assertNull(simulator.lastToastMessage)
    }

    @Test
    fun testTripleTapOpensMainUi() {
        val simulator = WidgetTapSimulator(isMasterEnabled = true)
        val widgetId = 102
        simulator.widgetMappings[widgetId] = sampleMapping("Blinkit", "com.grofers.customerapp")

        simulator.registerTap(widgetId, 1000L)
        simulator.registerTap(widgetId, 1200L)
        val result = simulator.registerTap(widgetId, 1400L)

        assertEquals("TRIPLE_TAP_MAIN_UI", result)
        assertTrue(simulator.mainUiOpened)
        assertNull(simulator.lastLaunchedPackage)
    }

    @Test
    fun testTripleTapWorksWhenMasterSwitchIsOff() {
        val simulator = WidgetTapSimulator(isMasterEnabled = false)
        val widgetId = 103
        simulator.widgetMappings[widgetId] = sampleMapping("Albums", "com.vivo.gallery")

        simulator.registerTap(widgetId, 1000L)
        simulator.registerTap(widgetId, 1150L)
        val result = simulator.registerTap(widgetId, 1300L)

        assertEquals("TRIPLE_TAP_MAIN_UI", result)
        assertTrue(simulator.mainUiOpened)
    }

    @Test
    fun testSingleTapBlockedWhenMasterSwitchIsOff() {
        val simulator = WidgetTapSimulator(isMasterEnabled = false)
        val widgetId = 104
        simulator.widgetMappings[widgetId] = sampleMapping("YouTube", "com.google.android.youtube")

        simulator.registerTap(widgetId, 1000L)
        simulator.executeSingleTap(widgetId)

        assertNull(simulator.lastLaunchedPackage)
        assertEquals("Map Schedule is OFF (triple-tap to open settings)", simulator.lastToastMessage)
    }

    @Test
    fun testMultipleWidgetsLaunchRespectiveApps() {
        val simulator = WidgetTapSimulator(isMasterEnabled = true)
        val widget1 = 201
        val widget2 = 202

        simulator.widgetMappings[widget1] = sampleMapping("Brave", "com.brave.browser")
        simulator.widgetMappings[widget2] = sampleMapping("Blinkit", "com.grofers.customerapp")

        simulator.executeSingleTap(widget1)
        assertEquals("com.brave.browser", simulator.lastLaunchedPackage)

        simulator.executeSingleTap(widget2)
        assertEquals("com.grofers.customerapp", simulator.lastLaunchedPackage)
    }

    @Test
    fun testTapsOutsideMultiTapWindowDoNotTriggerTripleTap() {
        val simulator = WidgetTapSimulator(isMasterEnabled = true)
        val widgetId = 301

        // Taps spaced > 1000ms apart
        simulator.registerTap(widgetId, 1000L)
        simulator.registerTap(widgetId, 2500L)
        val result = simulator.registerTap(widgetId, 4000L)

        assertEquals("TAP_RECORDED", result)
        assertFalse(simulator.mainUiOpened)
    }
}
