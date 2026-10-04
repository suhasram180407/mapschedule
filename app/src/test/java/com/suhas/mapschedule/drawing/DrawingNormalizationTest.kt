package com.suhas.mapschedule.drawing

import com.suhas.mapschedule.model.DrawingPoint
import com.suhas.mapschedule.model.DrawingStroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawingNormalizationTest {

    @Test
    fun testEmptyStrokesBoundingBoxIsNull() {
        val bounds = DrawingNormalization.computeBoundingBox(emptyList())
        assertNull(bounds)

        val normalized = DrawingNormalization.normalize(emptyList(), 100f, 100f)
        assertTrue(normalized.isEmpty())
    }

    @Test
    fun testBoundingBoxComputation() {
        val strokes = listOf(
            DrawingStroke(
                points = listOf(
                    DrawingPoint(10f, 20f),
                    DrawingPoint(50f, 80f),
                    DrawingPoint(5f, 40f)
                )
            )
        )

        val bounds = DrawingNormalization.computeBoundingBox(strokes)
        assertNotNull(bounds)
        assertEquals(5f, bounds!!.minX, 0.001f)
        assertEquals(20f, bounds.minY, 0.001f)
        assertEquals(50f, bounds.maxX, 0.001f)
        assertEquals(80f, bounds.maxY, 0.001f)
        assertEquals(45f, bounds.width, 0.001f)
        assertEquals(60f, bounds.height, 0.001f)
    }

    @Test
    fun testNormalizationKeepsAspectRatioAndCenters() {
        // A tall shape: width 20, height 100
        val tallStrokes = listOf(
            DrawingStroke(
                points = listOf(
                    DrawingPoint(0f, 0f),
                    DrawingPoint(20f, 100f)
                )
            )
        )

        val targetSize = 200f
        val paddingFraction = 0.10f
        // Available dimension: 200 * (1 - 0.20) = 160
        val normalized = DrawingNormalization.normalize(
            strokes = tallStrokes,
            targetWidth = targetSize,
            targetHeight = targetSize,
            paddingFraction = paddingFraction
        )

        assertEquals(1, normalized.size)
        val normBounds = DrawingNormalization.computeBoundingBox(normalized)
        assertNotNull(normBounds)

        // Height should scale to exactly availHeight (160)
        assertEquals(160f, normBounds!!.height, 0.01f)
        // Width should scale proportionally: 20 * (160 / 100) = 32
        assertEquals(32f, normBounds.width, 0.01f)

        // Should be centered horizontally: (200 - 32) / 2 = 84
        assertEquals(84f, normBounds.minX, 0.01f)
        assertEquals(116f, normBounds.maxX, 0.01f)

        // Centered vertically: (200 - 160) / 2 = 20
        assertEquals(20f, normBounds.minY, 0.01f)
        assertEquals(180f, normBounds.maxY, 0.01f)
    }

    @Test
    fun testWideShapeNormalization() {
        // A wide shape: width 100, height 20
        val wideStrokes = listOf(
            DrawingStroke(
                points = listOf(
                    DrawingPoint(0f, 0f),
                    DrawingPoint(100f, 20f)
                )
            )
        )

        val targetSize = 200f
        val paddingFraction = 0.10f
        val normalized = DrawingNormalization.normalize(
            strokes = wideStrokes,
            targetWidth = targetSize,
            targetHeight = targetSize,
            paddingFraction = paddingFraction
        )

        val normBounds = DrawingNormalization.computeBoundingBox(normalized)
        assertNotNull(normBounds)

        // Width scales to 160, Height scales to 20 * (160/100) = 32
        assertEquals(160f, normBounds!!.width, 0.01f)
        assertEquals(32f, normBounds.height, 0.01f)

        // Centered vertically: (200 - 32) / 2 = 84
        assertEquals(84f, normBounds.minY, 0.01f)
        assertEquals(116f, normBounds.maxY, 0.01f)
    }
}
