package com.suhas.mapschedule.data

import android.content.Context
import com.suhas.mapschedule.drawing.DrawingSerializer
import com.suhas.mapschedule.model.AppMapping
import com.suhas.mapschedule.model.WidgetConfiguration
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Local persistent file-based storage for AppMappings and WidgetConfigurations.
 * Provides atomic file writing to prevent corruption on sudden termination.
 */
class MappingStorage(private val context: Context) {

    private val mappingsFile: File
        get() = File(context.filesDir, "mappings.json")

    private val widgetConfigFile: File
        get() = File(context.filesDir, "widget_configs.json")

    private val settingsFile: File
        get() = File(context.filesDir, "settings.json")

    private val lock = Any()

    /**
     * Checks if the master launcher switch is ON. Defaults to true.
     */
    fun isMasterEnabled(): Boolean = synchronized(lock) {
        if (!settingsFile.exists()) return true
        try {
            val jsonStr = settingsFile.readText()
            if (jsonStr.isBlank()) return true
            val obj = JSONObject(jsonStr)
            obj.optBoolean("master_enabled", true)
        } catch (_: Exception) {
            true
        }
    }

    /**
     * Persists the master launcher switch state atomically.
     */
    fun setMasterEnabled(enabled: Boolean) = synchronized(lock) {
        val obj = JSONObject().apply {
            put("master_enabled", enabled)
        }
        atomicWrite(settingsFile, obj.toString(2))
    }

    /**
     * Loads all saved [AppMapping] records from disk.
     */
    fun loadMappings(): List<AppMapping> = synchronized(lock) {
        if (!mappingsFile.exists()) return emptyList()
        try {
            val jsonStr = mappingsFile.readText()
            if (jsonStr.isBlank()) return emptyList()

            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<AppMapping>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val strokesJson = if (obj.has("drawingStrokes")) {
                    obj.getString("drawingStrokes")
                } else if (obj.has("widgetDesignStrokes")) {
                    obj.getString("widgetDesignStrokes")
                } else if (obj.has("mappingCharacterStrokes")) {
                    obj.getString("mappingCharacterStrokes")
                } else {
                    obj.optString("strokes", "[]")
                }
                val strokes = DrawingSerializer.deserialize(strokesJson)

                val storedHash = if (obj.has("drawingHash")) {
                    obj.getString("drawingHash")
                } else if (obj.has("mappingCharacterHash")) {
                    obj.getString("mappingCharacterHash")
                } else {
                    ""
                }
                val hash = if (storedHash.isNotBlank()) {
                    storedHash
                } else {
                    com.suhas.mapschedule.drawing.DrawingFingerprint.computeHash(strokes)
                }

                val preview = if (obj.has("drawingPreview")) {
                    obj.optString("drawingPreview", "").ifEmpty { null }
                } else if (obj.has("widgetDesignPreview")) {
                    obj.optString("widgetDesignPreview", "").ifEmpty { null }
                } else {
                    obj.optString("previewPath", "").ifEmpty { null }
                }

                list.add(
                    AppMapping(
                        id = obj.getString("id"),
                        drawingStrokes = strokes,
                        drawingHash = hash,
                        drawingPreview = preview,
                        appName = obj.getString("appName"),
                        packageName = obj.getString("packageName"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Atomically saves the full list of [AppMapping] records to disk.
     */
    fun saveMappings(mappings: List<AppMapping>) = synchronized(lock) {
        val jsonArray = JSONArray()
        for (m in mappings) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("drawingStrokes", DrawingSerializer.serialize(m.drawingStrokes))
                put("drawingHash", m.drawingHash)
                put("drawingPreview", m.drawingPreview ?: "")
                // Compatibility fields for older versions
                put("widgetDesignStrokes", DrawingSerializer.serialize(m.drawingStrokes))
                put("widgetDesignPreview", m.drawingPreview ?: "")
                put("mappingCharacterStrokes", DrawingSerializer.serialize(m.drawingStrokes))
                put("mappingCharacterHash", m.drawingHash)
                put("strokes", DrawingSerializer.serialize(m.drawingStrokes))
                put("previewPath", m.drawingPreview ?: "")
                put("appName", m.appName)
                put("packageName", m.packageName)
                put("createdAt", m.createdAt)
                put("updatedAt", m.updatedAt)
            }
            jsonArray.put(obj)
        }
        atomicWrite(mappingsFile, jsonArray.toString(2))
    }

    /**
     * Loads all widget configuration mappings (widgetId -> mappingId).
     */
    fun loadWidgetConfigurations(): Map<Int, String> = synchronized(lock) {
        if (!widgetConfigFile.exists()) return emptyMap()
        try {
            val jsonStr = widgetConfigFile.readText()
            if (jsonStr.isBlank()) return emptyMap()

            val jsonArray = JSONArray(jsonStr)
            val map = mutableMapOf<Int, String>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val widgetId = obj.getInt("widgetId")
                val mappingId = obj.getString("mappingId")
                map[widgetId] = mappingId
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /**
     * Atomically saves widget configurations to disk.
     */
    fun saveWidgetConfigurations(configs: Map<Int, String>) = synchronized(lock) {
        val jsonArray = JSONArray()
        for ((widgetId, mappingId) in configs) {
            val obj = JSONObject().apply {
                put("widgetId", widgetId)
                put("mappingId", mappingId)
            }
            jsonArray.put(obj)
        }
        atomicWrite(widgetConfigFile, jsonArray.toString(2))
    }

    private fun atomicWrite(targetFile: File, content: String) {
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
        try {
            FileOutputStream(tempFile).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
                fos.flush()
            }
            if (tempFile.exists()) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
            }
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            throw e
        }
    }
}
