package com.suhas.mapschedule.repository

import android.content.Context
import com.suhas.mapschedule.data.MappingStorage
import com.suhas.mapschedule.drawing.DrawingRenderer
import com.suhas.mapschedule.model.AppMapping

/**
 * Single source of truth repository for AppMappings and WidgetConfigurations.
 */
class MappingRepository private constructor(private val context: Context) {

    private val storage = MappingStorage(context.applicationContext)

    @Volatile
    private var cachedMappings: MutableList<AppMapping>? = null

    @Volatile
    private var cachedWidgetConfigs: MutableMap<Int, String>? = null

    private val lock = Any()

    fun getAllMappings(): List<AppMapping> = synchronized(lock) {
        if (cachedMappings == null) {
            cachedMappings = storage.loadMappings().toMutableList()
        }
        cachedMappings!!.toList()
    }

    fun refreshMappings(): List<AppMapping> = synchronized(lock) {
        cachedMappings = storage.loadMappings().toMutableList()
        cachedMappings!!.toList()
    }

    fun getMappingById(id: String): AppMapping? = synchronized(lock) {
        getAllMappings().find { it.id == id }
    }

    fun findMappingByPackage(packageName: String): AppMapping? = synchronized(lock) {
        getAllMappings().find { it.packageName == packageName }
    }

    fun findMappingByDrawingHash(drawingHash: String): AppMapping? = synchronized(lock) {
        if (drawingHash.isBlank()) return null
        getAllMappings().find { it.drawingHash == drawingHash }
    }

    fun isPackageMapped(packageName: String, excludeMappingId: String? = null): Boolean = synchronized(lock) {
        getAllMappings().any { it.id != excludeMappingId && it.packageName == packageName }
    }

    fun isDrawingMapped(drawingHash: String, excludeMappingId: String? = null): Boolean = synchronized(lock) {
        if (drawingHash.isBlank()) return false
        getAllMappings().any { it.id != excludeMappingId && it.drawingHash == drawingHash }
    }

    fun getMappedPackages(excludeMappingId: String? = null): Set<String> = synchronized(lock) {
        getAllMappings()
            .filter { it.id != excludeMappingId }
            .map { it.packageName }
            .toSet()
    }

    fun isMasterEnabled(): Boolean = synchronized(lock) {
        storage.isMasterEnabled()
    }

    fun setMasterEnabled(enabled: Boolean) = synchronized(lock) {
        storage.setMasterEnabled(enabled)
    }

    fun saveMapping(mapping: AppMapping): AppMapping = synchronized(lock) {
        val list = getAllMappings().toMutableList()
        val index = list.indexOfFirst { it.id == mapping.id }

        val hash = if (mapping.drawingHash.isNotBlank()) {
            mapping.drawingHash
        } else {
            com.suhas.mapschedule.drawing.DrawingFingerprint.computeHash(mapping.drawingStrokes)
        }

        // Render preview from drawingStrokes (for home screen widget RemoteViews)
        val previewPath = DrawingRenderer.savePreview(
            context = context,
            mappingId = mapping.id,
            strokes = mapping.drawingStrokes
        )

        val updatedMapping = AppMapping(
            id = mapping.id,
            drawingStrokes = mapping.drawingStrokes,
            drawingHash = hash,
            drawingPreview = previewPath ?: mapping.drawingPreview,
            appName = mapping.appName,
            packageName = mapping.packageName,
            createdAt = mapping.createdAt,
            updatedAt = System.currentTimeMillis()
        )

        if (index >= 0) {
            list[index] = updatedMapping
        } else {
            list.add(0, updatedMapping)
        }

        cachedMappings = list
        storage.saveMappings(list)
        android.util.Log.i(
            "MapSchedule",
            "Mapping saved: id=${updatedMapping.id}, hash=$hash, pkg=${updatedMapping.packageName}"
        )
        updatedMapping
    }

    fun deleteMapping(id: String): Boolean = synchronized(lock) {
        val list = getAllMappings().toMutableList()
        val toRemove = list.find { it.id == id } ?: return false

        // Remove preview file
        DrawingRenderer.deletePreview(toRemove.previewPath)

        list.remove(toRemove)
        cachedMappings = list
        storage.saveMappings(list)

        // Clean up any widget configurations pointing to this deleted mapping
        val configs = getAllWidgetConfigurations().toMutableMap()
        val affectedWidgets = configs.filter { it.value == id }.keys
        for (wId in affectedWidgets) {
            configs.remove(wId)
        }
        cachedWidgetConfigs = configs
        storage.saveWidgetConfigurations(configs)

        true
    }

    fun getAllWidgetConfigurations(): Map<Int, String> = synchronized(lock) {
        if (cachedWidgetConfigs == null) {
            cachedWidgetConfigs = storage.loadWidgetConfigurations().toMutableMap()
        }
        cachedWidgetConfigs!!.toMap()
    }

    fun getWidgetMappingId(widgetId: Int): String? = synchronized(lock) {
        getAllWidgetConfigurations()[widgetId]
    }

    fun getWidgetMapping(widgetId: Int): AppMapping? = synchronized(lock) {
        val mappingId = getWidgetMappingId(widgetId) ?: return null
        getMappingById(mappingId)
    }

    fun setWidgetMapping(widgetId: Int, mappingId: String) = synchronized(lock) {
        val configs = getAllWidgetConfigurations().toMutableMap()
        configs[widgetId] = mappingId
        cachedWidgetConfigs = configs
        storage.saveWidgetConfigurations(configs)
    }

    fun removeWidgetMapping(widgetId: Int) = synchronized(lock) {
        val configs = getAllWidgetConfigurations().toMutableMap()
        if (configs.remove(widgetId) != null) {
            cachedWidgetConfigs = configs
            storage.saveWidgetConfigurations(configs)
        }
    }

    companion object {
        @Volatile
        private var instance: MappingRepository? = null

        fun getInstance(context: Context): MappingRepository {
            return instance ?: synchronized(this) {
                instance ?: MappingRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
