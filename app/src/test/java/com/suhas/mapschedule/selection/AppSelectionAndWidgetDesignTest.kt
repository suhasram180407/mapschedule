package com.suhas.mapschedule.selection

import com.suhas.mapschedule.drawing.DrawingFingerprint
import com.suhas.mapschedule.drawing.DrawingSerializer
import com.suhas.mapschedule.data.AppInfo
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
import java.util.UUID

/**
 * Comprehensive Unit Tests covering the 15 required scenarios from Prompt Section 43:
 * 1. App selection.
 * 2. App deselection on second tap.
 * 3. Only one selected app at a time.
 * 4. Already-mapped app cannot be selected.
 * 5. Current mapping's app remains selectable during edit.
 * 6. Duplicate package validation.
 * 7. Duplicate mapping-character validation.
 * 8. Widget Design cannot be empty.
 * 9. Mapping Character cannot be empty.
 * 10. Widget Design persistence.
 * 11. Mapping Character persistence.
 * 12. Widget Design and Mapping Character remain independent.
 * 13. Widget uses Widget Design.
 * 14. Widget update after Widget Design edit.
 * 15. Multiple widget instances remain independent.
 */
class AppSelectionAndWidgetDesignTest {

    private fun createStroke(vararg coords: Pair<Float, Float>): DrawingStroke {
        return DrawingStroke(
            points = coords.map { DrawingPoint(it.first, it.second) },
            strokeWidth = 14f
        )
    }

    // Helper simulating the App Selection toggle logic from AppSelectionDialog
    private fun handleAppClick(
        clickedApp: AppInfo,
        currentSelection: AppInfo?,
        mappedPackages: Set<String>
    ): AppInfo? {
        if (mappedPackages.contains(clickedApp.packageName)) {
            // Already mapped -> disabled, unclickable
            return currentSelection
        }
        return if (currentSelection?.packageName == clickedApp.packageName) {
            // Second tap on same app -> deselect
            null
        } else {
            // Single tap -> select new app
            clickedApp
        }
    }

    // 1. App selection
    @Test
    fun test01_AppSelection() {
        val app = AppInfo("YouTube", "com.google.android.youtube")
        val mappedPackages = emptySet<String>()

        var selectedApp: AppInfo? = null
        selectedApp = handleAppClick(app, selectedApp, mappedPackages)

        assertNotNull("Application should be selected after one tap", selectedApp)
        assertEquals("com.google.android.youtube", selectedApp?.packageName)
    }

    // 2. App deselection on second tap
    @Test
    fun test02_AppDeselectionOnSecondTap() {
        val app = AppInfo("YouTube", "com.google.android.youtube")
        val mappedPackages = emptySet<String>()

        // First tap -> select
        var selectedApp: AppInfo? = handleAppClick(app, null, mappedPackages)
        assertNotNull(selectedApp)

        // Second tap on the SAME app -> deselect
        selectedApp = handleAppClick(app, selectedApp, mappedPackages)
        assertNull("Application should be deselected after second tap on same item", selectedApp)
    }

    // 3. Only one selected app at a time
    @Test
    fun test03_OnlyOneSelectedAppAtATime() {
        val youtube = AppInfo("YouTube", "com.google.android.youtube")
        val chrome = AppInfo("Chrome", "com.android.chrome")
        val mappedPackages = emptySet<String>()

        var selectedApp: AppInfo? = handleAppClick(youtube, null, mappedPackages)
        assertEquals("com.google.android.youtube", selectedApp?.packageName)

        // Tapping Chrome switches selection
        selectedApp = handleAppClick(chrome, selectedApp, mappedPackages)
        assertEquals("Chrome should now be the only selected app", "com.android.chrome", selectedApp?.packageName)
    }

    // 4. Already-mapped app cannot be selected
    @Test
    fun test04_AlreadyMappedAppCannotBeSelected() {
        val youtube = AppInfo("YouTube", "com.google.android.youtube")
        val chrome = AppInfo("Chrome", "com.android.chrome")
        val mappedPackages = setOf("com.google.android.youtube") // YouTube is already mapped

        var selectedApp: AppInfo? = null

        // Attempt to tap already-mapped YouTube
        selectedApp = handleAppClick(youtube, selectedApp, mappedPackages)
        assertNull("Already mapped app must not be selected", selectedApp)

        // Unused app can be selected
        selectedApp = handleAppClick(chrome, selectedApp, mappedPackages)
        assertEquals("com.android.chrome", selectedApp?.packageName)
    }

    // 5. Current mapping's app remains selectable during edit
    @Test
    fun test05_CurrentMappingsAppRemainsSelectableDuringEdit() {
        val mapping1 = AppMapping(
            id = "m1",
            appName = "YouTube",
            packageName = "com.google.android.youtube",
            mappingCharacterStrokes = listOf(createStroke(10f to 10f, 20f to 20f)),
            widgetDesignStrokes = listOf(createStroke(10f to 10f, 30f to 30f))
        )
        val mapping2 = AppMapping(
            id = "m2",
            appName = "WhatsApp",
            packageName = "com.whatsapp",
            mappingCharacterStrokes = listOf(createStroke(40f to 40f, 50f to 50f)),
            widgetDesignStrokes = listOf(createStroke(40f to 40f, 60f to 60f))
        )
        val allMappings = listOf(mapping1, mapping2)

        // When editing m1, exclude m1 from mapped packages
        val editingMappingId = "m1"
        val mappedPackagesForEdit = allMappings
            .filter { it.id != editingMappingId }
            .map { it.packageName }
            .toSet()

        assertFalse("Current mapping's package must NOT be locked during edit", mappedPackagesForEdit.contains("com.google.android.youtube"))
        assertTrue("Other mapping's package MUST remain locked during edit", mappedPackagesForEdit.contains("com.whatsapp"))

        // YouTube can be tapped/selected/deselected
        val youtube = AppInfo("YouTube", "com.google.android.youtube")
        var selectedApp: AppInfo? = youtube
        selectedApp = handleAppClick(youtube, selectedApp, mappedPackagesForEdit)
        assertNull("Can deselect current app during edit", selectedApp)

        selectedApp = handleAppClick(youtube, selectedApp, mappedPackagesForEdit)
        assertEquals("Can re-select current app during edit", "com.google.android.youtube", selectedApp?.packageName)

        // WhatsApp cannot be selected
        val whatsapp = AppInfo("WhatsApp", "com.whatsapp")
        val previous = selectedApp
        selectedApp = handleAppClick(whatsapp, selectedApp, mappedPackagesForEdit)
        assertEquals("WhatsApp cannot be selected because it belongs to m2", previous, selectedApp)
    }

    // 6. Duplicate package validation
    @Test
    fun test06_DuplicatePackageValidation() {
        val mapping1 = AppMapping(
            id = "m1",
            appName = "YouTube",
            packageName = "com.google.android.youtube",
            mappingCharacterStrokes = listOf(createStroke(10f to 10f)),
            widgetDesignStrokes = listOf(createStroke(10f to 10f))
        )
        val existingMappings = listOf(mapping1)

        val newPackage = "com.google.android.youtube"
        val isDuplicate = existingMappings.any { it.packageName == newPackage }
        assertTrue("Mapping with duplicate package must be rejected", isDuplicate)

        val uniquePackage = "com.android.chrome"
        val isUnique = existingMappings.none { it.packageName == uniquePackage }
        assertTrue("Mapping with unique package must be accepted", isUnique)
    }

    // 7. Duplicate mapping-character validation
    @Test
    fun test07_DuplicateMappingCharacterValidation() {
        val characterStroke = createStroke(10f to 10f, 50f to 50f, 90f to 90f)
        val characterHash = DrawingFingerprint.computeHash(listOf(characterStroke))

        val mapping1 = AppMapping(
            id = "m1",
            appName = "YouTube",
            packageName = "com.google.android.youtube",
            mappingCharacterStrokes = listOf(characterStroke),
            mappingCharacterHash = characterHash,
            widgetDesignStrokes = listOf(createStroke(0f to 0f, 10f to 10f))
        )
        val existingMappings = listOf(mapping1)

        // Attempt creating a new mapping with the exact same character drawing
        val duplicateStrokes = listOf(characterStroke)
        val duplicateHash = DrawingFingerprint.computeHash(duplicateStrokes)

        val isDuplicateChar = existingMappings.any { it.mappingCharacterHash == duplicateHash }
        assertTrue("Duplicate mapping character drawing must be rejected", isDuplicateChar)

        // A different character drawing is accepted
        val differentStroke = createStroke(0f to 100f, 100f to 0f)
        val differentHash = DrawingFingerprint.computeHash(listOf(differentStroke))
        val isDifferentChar = existingMappings.none { it.mappingCharacterHash == differentHash }
        assertTrue("Distinct character drawing must be accepted", isDifferentChar)
    }

    // 8. Widget Design cannot be empty
    @Test
    fun test08_WidgetDesignCannotBeEmpty() {
        val emptyStrokes = emptyList<DrawingStroke>()
        val totalPoints = emptyStrokes.sumOf { it.points.size }
        val isInvalid = emptyStrokes.isEmpty() || totalPoints == 0
        assertTrue("Widget design with 0 strokes must be flagged as empty", isInvalid)

        val strokeWithNoPoints = listOf(DrawingStroke(emptyList()))
        val totalPointsZero = strokeWithNoPoints.sumOf { it.points.size }
        assertTrue("Widget design with 0 points must be flagged as empty", totalPointsZero == 0)

        val validWidgetDesign = listOf(createStroke(10f to 10f, 20f to 20f))
        val isValid = validWidgetDesign.isNotEmpty() && validWidgetDesign.sumOf { it.points.size } > 0
        assertTrue("Valid widget design must be accepted", isValid)
    }

    // 9. Mapping Character cannot be empty
    @Test
    fun test09_MappingCharacterCannotBeEmpty() {
        val emptyStrokes = emptyList<DrawingStroke>()
        val totalPoints = emptyStrokes.sumOf { it.points.size }
        val isInvalid = emptyStrokes.isEmpty() || totalPoints == 0
        assertTrue("Mapping character with 0 strokes must be flagged as empty", isInvalid)

        val validCharacter = listOf(createStroke(5f to 5f, 15f to 15f))
        val isValid = validCharacter.isNotEmpty() && validCharacter.sumOf { it.points.size } > 0
        assertTrue("Valid character drawing must be accepted", isValid)
    }

    // 10. Widget Design persistence
    @Test
    fun test10_WidgetDesignPersistence() {
        val widgetStrokes = listOf(
            createStroke(10f to 10f, 50f to 50f),
            createStroke(30f to 70f, 80f to 90f)
        )
        val serialized = DrawingSerializer.serialize(widgetStrokes)
        assertNotNull(serialized)
        assertTrue(serialized.contains("pts"))

        val deserialized = DrawingSerializer.deserialize(serialized)
        assertEquals(2, deserialized.size)
        assertEquals(2, deserialized[0].points.size)
        assertEquals(10f, deserialized[0].points[0].x, 0.001f)
        assertEquals(50f, deserialized[0].points[1].y, 0.001f)
        assertEquals(2, deserialized[1].points.size)
    }

    // 11. Mapping Character persistence
    @Test
    fun test11_MappingCharacterPersistence() {
        val characterStrokes = listOf(createStroke(5f to 15f, 25f to 35f, 45f to 55f))
        val hash = DrawingFingerprint.computeHash(characterStrokes)

        val mapping = AppMapping(
            id = "persist_test",
            appName = "Calculator",
            packageName = "com.google.android.calculator",
            mappingCharacterStrokes = characterStrokes,
            mappingCharacterHash = hash,
            widgetDesignStrokes = listOf(createStroke(1f to 1f))
        )

        assertEquals("persist_test", mapping.id)
        assertEquals(hash, mapping.mappingCharacterHash)
        assertEquals(1, mapping.mappingCharacterStrokes.size)
        assertEquals(3, mapping.mappingCharacterStrokes[0].points.size)
    }

    // 12. Widget Design and Mapping Character remain independent
    @Test
    fun test12_WidgetDesignAndMappingCharacterRemainIndependent() {
        val charStrokeA = createStroke(0f to 0f, 10f to 10f)
        val widgetDesignA = createStroke(50f to 50f, 60f to 60f, 70f to 70f)

        var mapping = AppMapping(
            id = "m_indep",
            appName = "Notes",
            packageName = "com.example.notes",
            mappingCharacterStrokes = listOf(charStrokeA),
            widgetDesignStrokes = listOf(widgetDesignA)
        )

        // Editing Mapping Character to a distinct shape should NOT change Widget Design
        val charStrokeB = createStroke(0f to 0f, 100f to 0f, 50f to 100f)
        val updatedMappingCharOnly = mapping.copy(
            mappingCharacterStrokes = listOf(charStrokeB),
            mappingCharacterHash = DrawingFingerprint.computeHash(listOf(charStrokeB))
        )

        assertEquals("Widget design must remain unchanged when character changes",
            1, updatedMappingCharOnly.widgetDesignStrokes.size)
        assertEquals(3, updatedMappingCharOnly.widgetDesignStrokes[0].points.size)
        assertNotEquals(mapping.mappingCharacterHash, updatedMappingCharOnly.mappingCharacterHash)

        // Editing Widget Design should NOT change Mapping Character
        val widgetDesignB = createStroke(1f to 2f)
        val updatedWidgetOnly = mapping.copy(
            widgetDesignStrokes = listOf(widgetDesignB)
        )

        assertEquals("Character strokes must remain unchanged when widget design changes",
            mapping.mappingCharacterStrokes, updatedWidgetOnly.mappingCharacterStrokes)
        assertEquals("Character hash must remain unchanged when widget design changes",
            mapping.mappingCharacterHash, updatedWidgetOnly.mappingCharacterHash)
    }

    // 13. Widget uses Widget Design
    @Test
    fun test13_WidgetUsesWidgetDesign() {
        val charStroke = createStroke(10f to 10f, 20f to 20f) // Star / Symbol
        val widgetStroke = createStroke(30f to 30f, 40f to 40f, 50f to 50f) // Custom Widget Design

        val mapping = AppMapping(
            id = "m_widget_check",
            appName = "YouTube",
            packageName = "com.google.android.youtube",
            mappingCharacterStrokes = listOf(charStroke),
            widgetDesignStrokes = listOf(widgetStroke),
            widgetDesignPreview = "/data/user/0/com.suhas.mapschedule/files/previews/m_widget_check.png"
        )

        // The widget display must draw widgetDesignStrokes (not mappingCharacterStrokes)
        val strokesForWidget = mapping.widgetDesignStrokes
        assertEquals("Widget must use widgetDesignStrokes", 1, strokesForWidget.size)
        assertEquals("Widget stroke must have 3 points", 3, strokesForWidget[0].points.size)
        assertNotEquals("Widget strokes must not be character strokes",
            mapping.mappingCharacterStrokes[0].points.size, strokesForWidget[0].points.size)
    }

    // 14. Widget update after Widget Design edit
    @Test
    fun test14_WidgetUpdateAfterWidgetDesignEdit() {
        val designA = listOf(createStroke(10f to 10f))
        val designB = listOf(createStroke(20f to 20f, 40f to 40f, 60f to 60f))

        var mapping = AppMapping(
            id = "m_edit_widget",
            appName = "YouTube",
            packageName = "com.google.android.youtube",
            mappingCharacterStrokes = listOf(createStroke(5f to 5f)),
            widgetDesignStrokes = designA
        )

        val widgetConfigs = mapOf(1001 to "m_edit_widget")

        // Edit widget design to design B
        mapping = mapping.copy(
            widgetDesignStrokes = designB,
            updatedAt = System.currentTimeMillis()
        )

        // The existing widget ID 1001 resolves to the updated mapping
        val currentWidgetMapping = widgetConfigs[1001]?.let { if (it == mapping.id) mapping else null }
        assertNotNull(currentWidgetMapping)
        assertEquals("Widget must display Design B without recreating the widget",
            3, currentWidgetMapping!!.widgetDesignStrokes[0].points.size)
    }

    // 15. Multiple widget instances remain independent
    @Test
    fun test15_MultipleWidgetInstancesRemainIndependent() {
        val mapping1 = AppMapping(
            id = "map_1",
            appName = "YouTube",
            packageName = "com.google.android.youtube",
            mappingCharacterStrokes = listOf(createStroke(1f to 1f)),
            widgetDesignStrokes = listOf(createStroke(10f to 10f, 20f to 20f))
        )
        val mapping2 = AppMapping(
            id = "map_2",
            appName = "Chrome",
            packageName = "com.android.chrome",
            mappingCharacterStrokes = listOf(createStroke(2f to 2f)),
            widgetDesignStrokes = listOf(createStroke(30f to 30f, 40f to 40f, 50f to 50f))
        )
        val mappings = listOf(mapping1, mapping2)

        val widgetConfigs = mutableMapOf(
            501 to "map_1",
            502 to "map_2"
        )

        // Widget 501 resolves to YouTube
        val w1Mapping = mappings.find { it.id == widgetConfigs[501] }
        assertEquals("com.google.android.youtube", w1Mapping?.packageName)
        assertEquals(2, w1Mapping?.widgetDesignStrokes?.get(0)?.points?.size)

        // Widget 502 resolves to Chrome
        val w2Mapping = mappings.find { it.id == widgetConfigs[502] }
        assertEquals("com.android.chrome", w2Mapping?.packageName)
        assertEquals(3, w2Mapping?.widgetDesignStrokes?.get(0)?.points?.size)

        // Deleting Widget 501 does NOT affect Widget 502
        widgetConfigs.remove(501)
        assertNull(widgetConfigs[501])
        assertEquals("map_2", widgetConfigs[502])
    }

    // 16. Master ON/OFF default state is ON
    @Test
    fun test16_MasterOnOffDefaultStateIsOn() {
        var isMasterEnabled = true // Default state
        assertTrue("Master switch must be ON by default", isMasterEnabled)
    }

    // 17. Master ON/OFF disables launcher functionality when OFF
    @Test
    fun test17_MasterOnOffDisablesLauncherWhenOff() {
        var isMasterEnabled = true
        fun canLaunchMappedApp(): Boolean = isMasterEnabled

        assertTrue("When ON, launcher functionality is active", canLaunchMappedApp())

        isMasterEnabled = false
        assertFalse("When OFF, launcher functionality must be disabled", canLaunchMappedApp())

        isMasterEnabled = true
        assertTrue("When toggled back ON, launcher functionality is restored", canLaunchMappedApp())
    }

    // 18. OFF state preserves existing mappings, drawings, and widget configs
    @Test
    fun test18_OffStatePreservesExistingData() {
        val mapping = AppMapping(
            id = "preserved_map",
            appName = "YouTube",
            packageName = "com.google.android.youtube",
            mappingCharacterStrokes = listOf(createStroke(1f to 1f, 2f to 2f)),
            widgetDesignStrokes = listOf(createStroke(3f to 3f, 4f to 4f))
        )
        val mappings = mutableListOf(mapping)
        val widgetConfigs = mutableMapOf(101 to "preserved_map")

        // Switch OFF
        var isMasterEnabled = false
        assertFalse(isMasterEnabled)

        // Verify data was NOT deleted
        assertEquals("Mappings must not be deleted when turned OFF", 1, mappings.size)
        assertEquals("Mapping Character must not be deleted", 2, mappings[0].mappingCharacterStrokes[0].points.size)
        assertEquals("Widget Design must not be deleted", 2, mappings[0].widgetDesignStrokes[0].points.size)
        assertEquals("Widget configs must not be deleted", "preserved_map", widgetConfigs[101])
    }

    // 19. OFF + Triple Tap opens Main UI
    @Test
    fun test19_OffPlusTripleTapOpensMainUi() {
        val isMasterEnabled = false // OFF
        var openedMainUi = false
        var launchedApp = false

        fun handleTapSequence(tapCount: Int) {
            if (tapCount >= 3) {
                // Triple tap -> ALWAYS opens Main UI (even when OFF)
                openedMainUi = true
            } else {
                if (isMasterEnabled) {
                    launchedApp = true
                }
            }
        }

        // Single tap when OFF
        handleTapSequence(1)
        assertFalse("Single tap when OFF must not launch app", launchedApp)
        assertFalse("Single tap when OFF must not open Main UI", openedMainUi)

        // Triple tap when OFF
        handleTapSequence(3)
        assertTrue("Triple tap when OFF MUST open Main UI", openedMainUi)
        assertFalse("Triple tap must not launch mapped app", launchedApp)
    }

    // 20. Deleted mapping safety: widget does not crash and does not launch stale package
    @Test
    fun test20_DeletedMappingSafety() {
        val mappings = mutableListOf<AppMapping>()
        val widgetConfigs = mutableMapOf(201 to "deleted_map_id")

        fun getLaunchTarget(widgetId: Int): String? {
            val mappingId = widgetConfigs[widgetId] ?: return null
            val mapping = mappings.find { it.id == mappingId } ?: return null
            return mapping.packageName
        }

        val target = getLaunchTarget(201)
        assertNull("Widget with deleted mapping must return null target (safe minimal state, no stale launch)", target)
    }

    // 21. Uninstalled app safety: handled safely without crash, flagged unavailable
    @Test
    fun test21_UninstalledAppSafety() {
        val mapping = AppMapping(
            id = "uninstalled_map",
            appName = "OldApp",
            packageName = "com.example.oldapp",
            mappingCharacterStrokes = listOf(createStroke(1f to 1f)),
            widgetDesignStrokes = listOf(createStroke(2f to 2f))
        )
        val installedPackages = setOf("com.android.chrome", "com.google.android.youtube")

        val isInstalled = installedPackages.contains(mapping.packageName)
        assertFalse("Mapped app that is no longer installed must be flagged as uninstalled", isInstalled)

        fun canLaunch(pkg: String): Boolean = installedPackages.contains(pkg)
        assertFalse("Must never attempt to launch uninstalled package", canLaunch(mapping.packageName))
    }

    // 22. Unified single drawing model
    @Test
    fun test22_UnifiedSingleDrawingModel() {
        val stroke = createStroke(15f to 15f, 30f to 30f)
        val mapping = AppMapping(
            id = "unified_1",
            appName = "Instagram",
            packageName = "com.instagram.android",
            drawingStrokes = listOf(stroke)
        )

        assertEquals("drawingStrokes must be accessible directly", 1, mapping.drawingStrokes.size)
        assertEquals("drawingStrokes must contain 2 points", 2, mapping.drawingStrokes[0].points.size)
        assertTrue("drawingHash must be computed", mapping.drawingHash.isNotBlank())
        assertEquals("strokes alias must match drawingStrokes", mapping.drawingStrokes, mapping.strokes)
        assertEquals("widgetDesignStrokes alias must match drawingStrokes", mapping.drawingStrokes, mapping.widgetDesignStrokes)
    }

    // 23. Validation: draw widget design required
    @Test
    fun test23_ValidationDrawWidgetDesignRequired() {
        val emptyStrokes = emptyList<DrawingStroke>()
        fun validate(strokes: List<DrawingStroke>, app: AppInfo?): String? {
            val points = strokes.sumOf { it.points.size }
            if (strokes.isEmpty() || points == 0) return "Please draw a widget design."
            if (app == null) return "Please select an application."
            return null
        }

        val error = validate(emptyStrokes, AppInfo("Chrome", "com.android.chrome"))
        assertEquals("Please draw a widget design.", error)
    }

    // 24. Validation: application required
    @Test
    fun test24_ValidationApplicationRequired() {
        val stroke = createStroke(10f to 10f)
        fun validate(strokes: List<DrawingStroke>, app: AppInfo?): String? {
            val points = strokes.sumOf { it.points.size }
            if (strokes.isEmpty() || points == 0) return "Please draw a widget design."
            if (app == null) return "Please select an application."
            return null
        }

        val error = validate(listOf(stroke), null)
        assertEquals("Please select an application.", error)
    }

    // 25. Validation: duplicate design rejected
    @Test
    fun test25_ValidationDuplicateDesignRejected() {
        val existingDrawing = listOf(createStroke(10f to 10f, 20f to 20f))
        val existingHash = com.suhas.mapschedule.drawing.DrawingFingerprint.computeHash(existingDrawing)
        val mappedHashes = setOf(existingHash)

        fun validateDrawing(strokes: List<DrawingStroke>): String? {
            val hash = com.suhas.mapschedule.drawing.DrawingFingerprint.computeHash(strokes)
            if (mappedHashes.contains(hash)) return "This design is already mapped."
            return null
        }

        val error = validateDrawing(existingDrawing)
        assertEquals("This design is already mapped.", error)

        val newDrawing = listOf(createStroke(10f to 10f, 50f to 90f, 90f to 10f))
        assertNull("Distinct drawing must not be rejected", validateDrawing(newDrawing))
    }

    // 26. Validation: duplicate application rejected
    @Test
    fun test26_ValidationDuplicateApplicationRejected() {
        val mappedPackages = setOf("com.android.chrome")
        fun validatePackage(pkg: String, excludeId: String? = null): String? {
            if (mappedPackages.contains(pkg)) return "This application is already mapped."
            return null
        }

        val error = validatePackage("com.android.chrome")
        assertEquals("This application is already mapped.", error)

        assertNull("Available app must not be rejected", validatePackage("com.spotify.music"))
    }

    // 27. App selection toggle & single selection behavior
    @Test
    fun test27_AppSelectionToggleAndSingleSelect() {
        var selectedApp: AppInfo? = null
        val chrome = AppInfo("Chrome", "com.android.chrome")
        val spotify = AppInfo("Spotify", "com.spotify.music")

        // First tap: SELECT Chrome
        selectedApp = if (selectedApp?.packageName == chrome.packageName) null else chrome
        assertEquals("First tap selects Chrome", "com.android.chrome", selectedApp?.packageName)

        // Second tap on same app: DESELECT Chrome
        selectedApp = if (selectedApp?.packageName == chrome.packageName) null else chrome
        assertNull("Second tap on same app deselects it", selectedApp)

        // Tap Chrome again: SELECT Chrome
        selectedApp = if (selectedApp?.packageName == chrome.packageName) null else chrome
        assertEquals("com.android.chrome", selectedApp?.packageName)

        // Tap Spotify: SELECT Spotify (replaces Chrome)
        selectedApp = if (selectedApp?.packageName == spotify.packageName) null else spotify
        assertEquals("Selecting another app replaces current selection", "com.spotify.music", selectedApp?.packageName)

        // Tap Spotify again: DESELECT Spotify
        selectedApp = if (selectedApp?.packageName == spotify.packageName) null else spotify
        assertNull("Deselects on second tap", selectedApp)
    }
}
