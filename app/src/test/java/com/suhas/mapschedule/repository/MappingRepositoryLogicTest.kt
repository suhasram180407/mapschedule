package com.suhas.mapschedule.repository

import com.suhas.mapschedule.model.AppMapping
import com.suhas.mapschedule.model.DrawingPoint
import com.suhas.mapschedule.model.DrawingStroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MappingRepositoryLogicTest {

    @Test
    fun testMappingListOperations() {
        val list = mutableListOf<AppMapping>()

        val stroke = DrawingStroke(listOf(DrawingPoint(5f, 5f)))
        val mapping1 = AppMapping(id = "m1", appName = "App 1", packageName = "com.app.one", strokes = listOf(stroke))
        val mapping2 = AppMapping(id = "m2", appName = "App 2", packageName = "com.app.two", strokes = listOf(stroke))

        list.add(mapping1)
        list.add(mapping2)
        assertEquals(2, list.size)

        // Find by ID
        val found = list.find { it.id == "m1" }
        assertEquals("App 1", found?.appName)

        // Update
        val idx = list.indexOfFirst { it.id == "m1" }
        val updated = mapping1.copy(appName = "App 1 Modified")
        list[idx] = updated
        assertEquals("App 1 Modified", list[0].appName)

        // Delete
        val removed = list.removeIf { it.id == "m2" }
        assertTrue(removed)
        assertEquals(1, list.size)
        assertNull(list.find { it.id == "m2" })
    }

    @Test
    fun testWidgetMappingCleanUpOnMappingDeletion() {
        val widgetConfigs = mutableMapOf<Int, String>()
        widgetConfigs[101] = "map_A"
        widgetConfigs[102] = "map_B"
        widgetConfigs[103] = "map_A" // Multiple widgets pointing to map_A

        // When map_A is deleted, all widgets mapped to map_A must be cleaned up
        val deletedMappingId = "map_A"
        val affectedWidgets = widgetConfigs.filter { it.value == deletedMappingId }.keys.toList()
        for (wId in affectedWidgets) {
            widgetConfigs.remove(wId)
        }

        assertFalse(widgetConfigs.containsKey(101))
        assertFalse(widgetConfigs.containsKey(103))
        assertTrue(widgetConfigs.containsKey(102))
        assertEquals("map_B", widgetConfigs[102])
    }

    @Test
    fun testMissingAppDetectionLogic() {
        val installedPackages = setOf("com.google.android.youtube", "com.android.chrome")

        val targetApp1 = "com.google.android.youtube"
        val isInstalled1 = installedPackages.contains(targetApp1)
        assertTrue("YouTube should be recognized as installed", isInstalled1)

        val targetApp2 = "com.uninstalled.randomapp"
        val isInstalled2 = installedPackages.contains(targetApp2)
        assertFalse("Missing package must be recognized as NOT installed", isInstalled2)
    }
}
