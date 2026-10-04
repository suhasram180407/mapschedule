package com.suhas.mapschedule.drawing

import com.suhas.mapschedule.model.AppMapping
import com.suhas.mapschedule.model.DrawingPoint
import com.suhas.mapschedule.model.DrawingStroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureMatcherTest {

    // Helper: create a letter 'A' gesture (3 strokes: left slant, right slant, crossbar)
    private fun createLetterA(): List<DrawingStroke> {
        val leftSlant = DrawingStroke(
            listOf(
                DrawingPoint(50f, 10f),
                DrawingPoint(35f, 40f),
                DrawingPoint(20f, 70f),
                DrawingPoint(10f, 90f)
            )
        )
        val rightSlant = DrawingStroke(
            listOf(
                DrawingPoint(50f, 10f),
                DrawingPoint(65f, 40f),
                DrawingPoint(80f, 70f),
                DrawingPoint(90f, 90f)
            )
        )
        val crossbar = DrawingStroke(
            listOf(
                DrawingPoint(30f, 55f),
                DrawingPoint(50f, 55f),
                DrawingPoint(70f, 55f)
            )
        )
        return listOf(leftSlant, rightSlant, crossbar)
    }

    // Helper: create a letter 'C' gesture (1 stroke arc on left side)
    private fun createLetterC(): List<DrawingStroke> {
        val points = mutableListOf<DrawingPoint>()
        // Arc from (80, 20) -> (30, 20) -> (15, 50) -> (30, 80) -> (80, 80)
        points.add(DrawingPoint(80f, 20f))
        points.add(DrawingPoint(60f, 15f))
        points.add(DrawingPoint(40f, 20f))
        points.add(DrawingPoint(20f, 35f))
        points.add(DrawingPoint(15f, 50f))
        points.add(DrawingPoint(20f, 65f))
        points.add(DrawingPoint(40f, 80f))
        points.add(DrawingPoint(60f, 85f))
        points.add(DrawingPoint(80f, 80f))
        return listOf(DrawingStroke(points))
    }

    // Helper: create a 5-point star '★' gesture (multi-stroke or continuous 5 segments)
    private fun createStar(): List<DrawingStroke> {
        val points = listOf(
            DrawingPoint(50f, 10f),
            DrawingPoint(65f, 90f),
            DrawingPoint(10f, 40f),
            DrawingPoint(90f, 40f),
            DrawingPoint(35f, 90f),
            DrawingPoint(50f, 10f)
        )
        return listOf(DrawingStroke(points))
    }

    // Helper: create a vertical line '|'
    private fun createVerticalLine(): List<DrawingStroke> {
        return listOf(
            DrawingStroke(
                listOf(
                    DrawingPoint(50f, 10f),
                    DrawingPoint(50f, 50f),
                    DrawingPoint(50f, 90f)
                )
            )
        )
    }

    // Helper: create a horizontal line '-'
    private fun createHorizontalLine(): List<DrawingStroke> {
        return listOf(
            DrawingStroke(
                listOf(
                    DrawingPoint(10f, 50f),
                    DrawingPoint(50f, 50f),
                    DrawingPoint(90f, 50f)
                )
            )
        )
    }

    @Test
    fun testExactMatchScoresHigh() {
        val letterA = createLetterA()
        val mapping = AppMapping(
            appName = "Chrome",
            packageName = "com.android.chrome",
            drawingStrokes = letterA
        )

        val result = GestureMatcher.match(letterA, listOf(mapping))
        assertTrue("Expected Matched result", result is GestureMatcher.MatchResult.Matched)
        val matched = result as GestureMatcher.MatchResult.Matched
        assertEquals("com.android.chrome", matched.mapping.packageName)
        assertTrue("Score should be >= 0.90 for exact match, was ${matched.score}", matched.score >= 0.90f)
    }

    @Test
    fun testLetterAMatchesSlightlyDifferentA() {
        val refA = createLetterA()
        // Candidate A with slight coordinate shifts and different sample count
        val candidateA = listOf(
            DrawingStroke(
                listOf(
                    DrawingPoint(52f, 12f),
                    DrawingPoint(36f, 38f),
                    DrawingPoint(22f, 68f),
                    DrawingPoint(12f, 88f)
                )
            ),
            DrawingStroke(
                listOf(
                    DrawingPoint(52f, 12f),
                    DrawingPoint(67f, 42f),
                    DrawingPoint(82f, 72f),
                    DrawingPoint(88f, 88f)
                )
            ),
            DrawingStroke(
                listOf(
                    DrawingPoint(32f, 53f),
                    DrawingPoint(48f, 54f),
                    DrawingPoint(68f, 53f)
                )
            )
        )

        val mapping = AppMapping(
            appName = "Chrome",
            packageName = "com.android.chrome",
            drawingStrokes = refA
        )

        val result = GestureMatcher.match(candidateA, listOf(mapping))
        assertTrue("Candidate A should match reference A", result is GestureMatcher.MatchResult.Matched)
        val matched = result as GestureMatcher.MatchResult.Matched
        assertEquals("Chrome", matched.mapping.appName)
        assertTrue("Score should be >= 0.70 for hand-drawn variation, was ${matched.score}", matched.score >= 0.70f)
    }

    @Test
    fun testLetterCRejectsLetterA() {
        val refA = createLetterA()
        val candidateC = createLetterC()

        val mappingA = AppMapping(
            appName = "Chrome",
            packageName = "com.android.chrome",
            drawingStrokes = refA
        )

        val result = GestureMatcher.match(candidateC, listOf(mappingA))
        assertTrue("Letter C must NOT match reference A", result is GestureMatcher.MatchResult.NoMatch)
    }

    @Test
    fun testMultipleMappingsSelectsBestMatch() {
        val refA = createLetterA()
        val refC = createLetterC()
        val refStar = createStar()

        val mappingA = AppMapping(appName = "Chrome", packageName = "com.android.chrome", drawingStrokes = refA)
        val mappingC = AppMapping(appName = "Spotify", packageName = "com.spotify.music", drawingStrokes = refC)
        val mappingStar = AppMapping(appName = "Instagram", packageName = "com.instagram.android", drawingStrokes = refStar)

        val mappings = listOf(mappingA, mappingC, mappingStar)

        // Draw C
        val candidateC = createLetterC()
        val resultC = GestureMatcher.match(candidateC, mappings)
        assertTrue(resultC is GestureMatcher.MatchResult.Matched)
        assertEquals("com.spotify.music", (resultC as GestureMatcher.MatchResult.Matched).mapping.packageName)

        // Draw Star
        val candidateStar = createStar()
        val resultStar = GestureMatcher.match(candidateStar, mappings)
        assertTrue(resultStar is GestureMatcher.MatchResult.Matched)
        assertEquals("com.instagram.android", (resultStar as GestureMatcher.MatchResult.Matched).mapping.packageName)

        // Draw A
        val candidateA = createLetterA()
        val resultA = GestureMatcher.match(candidateA, mappings)
        assertTrue(resultA is GestureMatcher.MatchResult.Matched)
        assertEquals("com.android.chrome", (resultA as GestureMatcher.MatchResult.Matched).mapping.packageName)
    }

    @Test
    fun testVerticalAndHorizontalLinesDoNotMatchDueToAspectRatio() {
        val vert = createVerticalLine()
        val horiz = createHorizontalLine()

        val mappingVert = AppMapping(appName = "VertApp", packageName = "com.vert", drawingStrokes = vert)
        val result = GestureMatcher.match(horiz, listOf(mappingVert))
        assertTrue("Horizontal line must NOT match vertical line", result is GestureMatcher.MatchResult.NoMatch)
    }

    @Test
    fun testUnknownScribbleReturnsNoMatch() {
        val refA = createLetterA()
        val mappingA = AppMapping(appName = "Chrome", packageName = "com.android.chrome", drawingStrokes = refA)

        // Scribble in corner
        val scribble = listOf(
            DrawingStroke(
                listOf(
                    DrawingPoint(90f, 90f),
                    DrawingPoint(95f, 92f),
                    DrawingPoint(91f, 94f),
                    DrawingPoint(96f, 95f)
                )
            )
        )

        val result = GestureMatcher.match(scribble, listOf(mappingA))
        assertTrue("Unknown scribble should return NoMatch", result is GestureMatcher.MatchResult.NoMatch)
    }

    @Test
    fun testEmptyCandidateReturnsEmpty() {
        val refA = createLetterA()
        val mappingA = AppMapping(appName = "Chrome", packageName = "com.android.chrome", drawingStrokes = refA)

        val result = GestureMatcher.match(emptyList(), listOf(mappingA))
        assertEquals(GestureMatcher.MatchResult.Empty, result)
    }

    @Test
    fun testEmptyMappingsReturnsNoMatch() {
        val letterA = createLetterA()
        val result = GestureMatcher.match(letterA, emptyList())
        assertTrue(result is GestureMatcher.MatchResult.NoMatch)
    }
}
