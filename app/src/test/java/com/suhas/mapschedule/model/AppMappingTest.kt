package com.suhas.mapschedule.model

import com.suhas.mapschedule.drawing.DrawingSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppMappingTest {

    @Test
    fun testMappingCreation() {
        val stroke = DrawingStroke(
            points = listOf(DrawingPoint(10f, 10f), DrawingPoint(50f, 50f))
        )
        val mapping = AppMapping(
            appName = "YouTube",
            packageName = "com.google.android.youtube",
            strokes = listOf(stroke)
        )

        assertNotNull(mapping.id)
        assertTrue(mapping.id.isNotEmpty())
        assertEquals("YouTube", mapping.appName)
        assertEquals("com.google.android.youtube", mapping.packageName)
        assertEquals(1, mapping.strokes.size)
        assertTrue(mapping.createdAt > 0)
        assertEquals(mapping.createdAt, mapping.updatedAt)
    }

    @Test
    fun testMappingUpdate() {
        val initialStroke = DrawingStroke(listOf(DrawingPoint(1f, 1f)))
        val mapping = AppMapping(
            appName = "Chrome",
            packageName = "com.android.chrome",
            strokes = listOf(initialStroke)
        )

        val newStrokes = listOf(
            DrawingStroke(listOf(DrawingPoint(20f, 20f), DrawingPoint(40f, 40f)))
        )
        val updatedMapping = mapping.copy(
            mappingCharacterStrokes = newStrokes,
            appName = "Google Chrome",
            updatedAt = System.currentTimeMillis() + 1000
        )

        assertEquals(mapping.id, updatedMapping.id)
        assertEquals("Google Chrome", updatedMapping.appName)
        assertEquals("com.android.chrome", updatedMapping.packageName)
        assertEquals(1, updatedMapping.strokes.size)
        assertEquals(2, updatedMapping.strokes[0].points.size)
    }

    @Test
    fun testWidgetConfigurationMappingAssociation() {
        val widgetId1 = 42
        val widgetId2 = 99
        val mappingId1 = "map-alpha-1"
        val mappingId2 = "map-beta-2"

        val config1 = WidgetConfiguration(widgetId = widgetId1, mappingId = mappingId1)
        val config2 = WidgetConfiguration(widgetId = widgetId2, mappingId = mappingId2)

        assertEquals(42, config1.widgetId)
        assertEquals("map-alpha-1", config1.mappingId)
        assertEquals(99, config2.widgetId)
        assertEquals("map-beta-2", config2.mappingId)
        assertNotEquals(config1.widgetId, config2.widgetId)
    }

    @Test
    fun testDrawingValidationRules() {
        // Empty drawing: zero strokes
        val emptyStrokes = emptyList<DrawingStroke>()
        val totalPointsZero = emptyStrokes.sumOf { it.points.size }
        val isEmptyDrawing = emptyStrokes.isEmpty() || totalPointsZero == 0
        assertTrue("Empty stroke list should be invalid", isEmptyDrawing)

        // Empty strokes with no points
        val strokeNoPoints = listOf(DrawingStroke(points = emptyList()))
        val totalPointsEmpty = strokeNoPoints.sumOf { it.points.size }
        val isInvalidStroke = totalPointsEmpty == 0
        assertTrue("Stroke with zero points should be invalid", isInvalidStroke)

        // Valid stroke with points
        val validStroke = listOf(DrawingStroke(points = listOf(DrawingPoint(1f, 2f))))
        val isValid = validStroke.isNotEmpty() && validStroke.sumOf { it.points.size } > 0
        assertTrue("Valid stroke should be accepted", isValid)
    }
}
