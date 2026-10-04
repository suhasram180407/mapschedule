package com.suhas.mapschedule.drawing

import com.suhas.mapschedule.model.DrawingPoint
import com.suhas.mapschedule.model.DrawingStroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawingSerializationTest {

    @Test
    fun testEmptyDrawingSerialization() {
        val json = DrawingSerializer.serialize(emptyList())
        assertEquals("[]", json)
        val deserialized = DrawingSerializer.deserialize(json)
        assertTrue(deserialized.isEmpty())
    }

    @Test
    fun testSingleStrokeSerializationAndDeserialization() {
        val stroke = DrawingStroke(
            points = listOf(
                DrawingPoint(10f, 20f),
                DrawingPoint(15.5f, 25.5f),
                DrawingPoint(30f, 50f)
            ),
            strokeWidth = 14f,
            timestamp = 1700000000000L
        )

        val json = DrawingSerializer.serialize(listOf(stroke))
        val deserialized = DrawingSerializer.deserialize(json)

        assertEquals(1, deserialized.size)
        val restored = deserialized[0]
        assertEquals(14f, restored.strokeWidth, 0.01f)
        assertEquals(1700000000000L, restored.timestamp)
        assertEquals(3, restored.points.size)
        assertEquals(10f, restored.points[0].x, 0.01f)
        assertEquals(20f, restored.points[0].y, 0.01f)
        assertEquals(15.5f, restored.points[1].x, 0.01f)
        assertEquals(25.5f, restored.points[1].y, 0.01f)
        assertEquals(30f, restored.points[2].x, 0.01f)
        assertEquals(50f, restored.points[2].y, 0.01f)
    }

    @Test
    fun testMultipleStrokesSerialization() {
        val strokes = listOf(
            DrawingStroke(
                points = listOf(DrawingPoint(0f, 0f), DrawingPoint(10f, 10f)),
                strokeWidth = 12f
            ),
            DrawingStroke(
                points = listOf(DrawingPoint(50f, 50f), DrawingPoint(60f, 60f), DrawingPoint(70f, 80f)),
                strokeWidth = 16f
            )
        )

        val json = DrawingSerializer.serialize(strokes)
        val deserialized = DrawingSerializer.deserialize(json)

        assertEquals(2, deserialized.size)
        assertEquals(2, deserialized[0].points.size)
        assertEquals(3, deserialized[1].points.size)
        assertEquals(12f, deserialized[0].strokeWidth, 0.01f)
        assertEquals(16f, deserialized[1].strokeWidth, 0.01f)
    }

    @Test
    fun testCorruptStringDeserializationSafe() {
        val deserialized = DrawingSerializer.deserialize("{invalid json:;;;}")
        assertTrue(deserialized.isEmpty())

        val emptyResult = DrawingSerializer.deserialize("")
        assertTrue(emptyResult.isEmpty())
    }
}
