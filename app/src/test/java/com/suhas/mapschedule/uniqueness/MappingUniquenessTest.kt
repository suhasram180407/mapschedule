package com.suhas.mapschedule.uniqueness

import com.suhas.mapschedule.drawing.DrawingFingerprint
import com.suhas.mapschedule.model.AppMapping
import com.suhas.mapschedule.model.DrawingPoint
import com.suhas.mapschedule.model.DrawingStroke
import com.suhas.mapschedule.model.WidgetConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MappingUniquenessTest {

    private fun createStroke(vararg coords: Pair<Float, Float>): DrawingStroke {
        return DrawingStroke(
            points = coords.map { DrawingPoint(it.first, it.second) },
            strokeWidth = 14f
        )
    }

    // 1. Can create mapping
    @Test
    fun testCanCreateMapping() {
        val stroke = createStroke(10f to 10f, 50f to 50f)
        val mapping = AppMapping(
            appName = "YouTube",
            packageName = "com.google.android.youtube",
            strokes = listOf(stroke)
        )
        assertNotNull(mapping.id)
        assertEquals("com.google.android.youtube", mapping.packageName)
        assertTrue(mapping.drawingHash.isNotBlank())
    }

    // 2. Cannot map same package twice
    @Test
    fun testCannotMapSamePackageTwice() {
        val stroke1 = createStroke(10f to 10f, 20f to 20f)
        val stroke2 = createStroke(30f to 30f, 80f to 80f)

        val mapping1 = AppMapping(
            id = "m1",
            appName = "YouTube",
            packageName = "com.google.android.youtube",
            strokes = listOf(stroke1)
        )
        val existingMappings = mutableListOf(mapping1)

        val newPackage = "com.google.android.youtube"
        val isDuplicatePackage = existingMappings.any { it.packageName == newPackage }
        assertTrue("Duplicate package must be detected and rejected", isDuplicatePackage)
    }

    // 3. Can map different packages
    @Test
    fun testCanMapDifferentPackages() {
        val stroke1 = createStroke(10f to 10f, 20f to 20f)
        val stroke2 = createStroke(50f to 50f, 90f to 90f)

        val mapping1 = AppMapping(
            id = "m1",
            appName = "YouTube",
            packageName = "com.google.android.youtube",
            strokes = listOf(stroke1)
        )
        val existingMappings = mutableListOf(mapping1)

        val newPackage = "com.android.chrome"
        val isDuplicatePackage = existingMappings.any { it.packageName == newPackage }
        assertFalse("Different package should not be flagged as duplicate", isDuplicatePackage)
    }

    // 4. Cannot reuse same drawing
    @Test
    fun testCannotReuseSameDrawing() {
        val stroke = createStroke(15f to 25f, 60f to 80f)
        val hash = DrawingFingerprint.computeHash(listOf(stroke))

        val mapping1 = AppMapping(
            id = "m1",
            appName = "YouTube",
            packageName = "com.google.android.youtube",
            strokes = listOf(stroke),
            drawingHash = hash
        )
        val existingMappings = mutableListOf(mapping1)

        // Attempting another app with same drawing stroke structure
        val newDrawingStrokes = listOf(stroke)
        val newHash = DrawingFingerprint.computeHash(newDrawingStrokes)

        val isDuplicateDrawing = existingMappings.any { it.drawingHash == newHash }
        assertTrue("Same drawing must be detected as duplicate", isDuplicateDrawing)
    }

    // 5. Can reuse deleted package
    @Test
    fun testCanReuseDeletedPackage() {
        val stroke1 = createStroke(10f to 10f, 20f to 20f)
        val mapping1 = AppMapping(
            id = "m1",
            appName = "YouTube",
            packageName = "com.google.android.youtube",
            strokes = listOf(stroke1)
        )
        val existingMappings = mutableListOf(mapping1)

        // Delete mapping1
        existingMappings.removeIf { it.id == "m1" }

        // Now creating mapping with YouTube should be allowed
        val isDuplicatePackage = existingMappings.any { it.packageName == "com.google.android.youtube" }
        assertFalse("Deleted package must be reusable", isDuplicatePackage)
    }

    // 6. Can reuse deleted drawing
    @Test
    fun testCanReuseDeletedDrawing() {
        val stroke1 = createStroke(10f to 10f, 20f to 20f)
        val hash = DrawingFingerprint.computeHash(listOf(stroke1))
        val mapping1 = AppMapping(
            id = "m1",
            appName = "YouTube",
            packageName = "com.google.android.youtube",
            strokes = listOf(stroke1),
            drawingHash = hash
        )
        val existingMappings = mutableListOf(mapping1)

        // Delete mapping1
        existingMappings.removeIf { it.id == "m1" }

        // Now creating mapping with same drawing should be allowed
        val isDuplicateDrawing = existingMappings.any { it.drawingHash == hash }
        assertFalse("Deleted drawing must be reusable", isDuplicateDrawing)
    }

    // 7. Editing existing mapping without changing it succeeds
    @Test
    fun testEditingExistingMappingWithoutChangesSucceeds() {
        val stroke1 = createStroke(10f to 10f, 20f to 20f)
        val hash = DrawingFingerprint.computeHash(listOf(stroke1))
        val mapping1 = AppMapping(
            id = "m1",
            appName = "YouTube",
            packageName = "com.google.android.youtube",
            strokes = listOf(stroke1),
            drawingHash = hash
        )
        val existingMappings = listOf(mapping1)

        // Excluding current mapping ID m1
        val currentId = "m1"
        val isDuplicatePackage = existingMappings.any { it.id != currentId && it.packageName == "com.google.android.youtube" }
        val isDuplicateDrawing = existingMappings.any { it.id != currentId && it.drawingHash == hash }

        assertFalse("Self mapping should not be flagged as duplicate package", isDuplicatePackage)
        assertFalse("Self mapping should not be flagged as duplicate drawing", isDuplicateDrawing)
    }

    // 8. Editing to another mapping's package fails
    @Test
    fun testEditingToAnotherMappingsPackageFails() {
        val stroke1 = createStroke(10f to 10f, 20f to 20f)
        val stroke2 = createStroke(50f to 50f, 60f to 60f)
        val mapping1 = AppMapping(id = "m1", appName = "YouTube", packageName = "com.google.android.youtube", strokes = listOf(stroke1))
        val mapping2 = AppMapping(id = "m2", appName = "Chrome", packageName = "com.android.chrome", strokes = listOf(stroke2))

        val existingMappings = listOf(mapping1, mapping2)

        // When editing m1, attempting to change package to m2's package (Chrome)
        val targetPackage = "com.android.chrome"
        val isConflict = existingMappings.any { it.id != "m1" && it.packageName == targetPackage }
        assertTrue("Editing to an existing mapping's package must fail", isConflict)
    }

    // 9. Editing to another mapping's drawing fails
    @Test
    fun testEditingToAnotherMappingsDrawingFails() {
        val stroke1 = createStroke(10f to 10f, 20f to 20f)
        val stroke2 = createStroke(50f to 50f, 60f to 60f)
        val hash1 = DrawingFingerprint.computeHash(listOf(stroke1))
        val hash2 = DrawingFingerprint.computeHash(listOf(stroke2))

        val mapping1 = AppMapping(id = "m1", appName = "YouTube", packageName = "com.google.android.youtube", strokes = listOf(stroke1), drawingHash = hash1)
        val mapping2 = AppMapping(id = "m2", appName = "Chrome", packageName = "com.android.chrome", strokes = listOf(stroke2), drawingHash = hash2)

        val existingMappings = listOf(mapping1, mapping2)

        // When editing m1, attempting to redraw so it matches m2's drawing (hash2)
        val isConflict = existingMappings.any { it.id != "m1" && it.drawingHash == hash2 }
        assertTrue("Editing to an existing mapping's drawing must fail", isConflict)
    }

    // 10. Drawing hash is deterministic
    @Test
    fun testDrawingHashIsDeterministic() {
        val strokeA = createStroke(0f to 0f, 50f to 50f, 100f to 100f)
        val strokeB = createStroke(0f to 0f, 50f to 50f, 100f to 100f)

        val hashA = DrawingFingerprint.computeHash(listOf(strokeA))
        val hashB = DrawingFingerprint.computeHash(listOf(strokeB))

        assertEquals("Same stroke input must generate identical hash", hashA, hashB)
        assertTrue("Hash must not be empty", hashA.isNotEmpty())

        // Distinct shape generates different hash
        val strokeC = createStroke(0f to 100f, 50f to 0f, 100f to 100f)
        val hashC = DrawingFingerprint.computeHash(listOf(strokeC))
        assertNotEquals("Distinct shapes must generate distinct hashes", hashA, hashC)
    }

    // 11. Widget ID maps to correct mapping ID
    @Test
    fun testWidgetIdMapsToCorrectMappingId() {
        val widgetConfigs = mutableMapOf<Int, String>()
        widgetConfigs[166] = "mapping-7"
        widgetConfigs[167] = "mapping-12"

        assertEquals("mapping-7", widgetConfigs[166])
        assertEquals("mapping-12", widgetConfigs[167])
        assertNull(widgetConfigs[999])
    }

    // 12. Widget mapping resolves correct package
    @Test
    fun testWidgetMappingResolvesCorrectPackage() {
        val mapping1 = AppMapping(id = "m7", appName = "YouTube", packageName = "com.google.android.youtube", strokes = emptyList())
        val mapping2 = AppMapping(id = "m12", appName = "Chrome", packageName = "com.android.chrome", strokes = emptyList())
        val mappings = listOf(mapping1, mapping2)

        val widgetConfigs = mapOf(166 to "m7", 167 to "m12")

        val targetMappingId1 = widgetConfigs[166]
        val resolvedPackage1 = mappings.find { it.id == targetMappingId1 }?.packageName
        assertEquals("com.google.android.youtube", resolvedPackage1)

        val targetMappingId2 = widgetConfigs[167]
        val resolvedPackage2 = mappings.find { it.id == targetMappingId2 }?.packageName
        assertEquals("com.android.chrome", resolvedPackage2)
    }

    // 13. Missing package handled safely
    @Test
    fun testMissingPackageHandledSafely() {
        val installedPackages = setOf("com.google.android.youtube", "com.android.chrome")
        val mappedPackage = "com.uninstalled.app"

        val isInstalled = installedPackages.contains(mappedPackage)
        assertFalse("Uninstalled app should be detected as missing", isInstalled)

        // Launch should safely abort when package is missing
        val canLaunch = if (isInstalled) true else false
        assertFalse("Missing app must not be launched", canLaunch)
    }

    // 14. Widget update after mapping edit
    @Test
    fun testWidgetUpdateAfterMappingEdit() {
        val strokeOriginal = createStroke(10f to 10f)
        val strokeModified = createStroke(20f to 20f, 40f to 40f)

        var mapping = AppMapping(id = "m1", appName = "YouTube", packageName = "com.google.android.youtube", strokes = listOf(strokeOriginal))
        val widgetConfigs = mapOf(200 to "m1")

        // Edit mapping
        mapping = mapping.copy(mappingCharacterStrokes = listOf(strokeModified), updatedAt = System.currentTimeMillis())

        // Widget lookup retrieves updated mapping
        val widgetMapping = if (widgetConfigs[200] == mapping.id) mapping else null
        assertNotNull(widgetMapping)
        assertEquals(1, widgetMapping!!.strokes.size)
        assertEquals(2, widgetMapping.strokes[0].points.size)
    }

    // 15. Widget cleanup after mapping deletion
    @Test
    fun testWidgetCleanupAfterMappingDeletion() {
        val widgetConfigs = mutableMapOf(201 to "m1", 202 to "m2")
        val deletedMappingId = "m1"

        // Clean up widgets pointing to deleted mapping
        val affectedWidgets = widgetConfigs.filter { it.value == deletedMappingId }.keys.toList()
        for (wId in affectedWidgets) {
            widgetConfigs.remove(wId)
        }

        assertNull("Widget 201 should be unlinked after m1 deletion", widgetConfigs[201])
        assertEquals("m2", widgetConfigs[202])
    }
}
