package com.suhas.mapschedule.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.suhas.mapschedule.data.AppInfo
import com.suhas.mapschedule.data.InstalledAppsProvider
import com.suhas.mapschedule.model.AppMapping
import com.suhas.mapschedule.model.DrawingStroke
import com.suhas.mapschedule.repository.MappingRepository
import com.suhas.mapschedule.widget.MapScheduleWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class UiState(
    val mappings: List<AppMapping> = emptyList(),
    val installedApps: List<AppInfo> = emptyList(),
    val mappedPackageNames: Set<String> = emptySet(),
    val isLoadingApps: Boolean = false,
    val isMasterEnabled: Boolean = true,
    val userMessage: String? = null
)

class MapScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MappingRepository.getInstance(application)
    private val appsProvider = InstalledAppsProvider(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadMasterEnabled()
        loadMappings()
        loadInstalledApps()
    }

    fun loadMasterEnabled() {
        val enabled = repository.isMasterEnabled()
        _uiState.value = _uiState.value.copy(isMasterEnabled = enabled)
    }

    fun setMasterEnabled(enabled: Boolean) {
        repository.setMasterEnabled(enabled)
        _uiState.value = _uiState.value.copy(isMasterEnabled = enabled)
        // Refresh widgets if needed
        MapScheduleWidgetUpdater.updateAllWidgets(getApplication())
    }

    fun loadMappings() {
        val list = repository.getAllMappings()
        val mapped = repository.getMappedPackages()
        _uiState.value = _uiState.value.copy(mappings = list, mappedPackageNames = mapped)
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingApps = true)
            val apps = withContext(Dispatchers.IO) {
                appsProvider.getLaunchableApps()
            }
            _uiState.value = _uiState.value.copy(
                installedApps = apps,
                isLoadingApps = false
            )
        }
    }

    fun getMappedPackages(excludeMappingId: String? = null): Set<String> {
        return repository.getMappedPackages(excludeMappingId)
    }

    /**
     * Validates and saves or updates an AppMapping.
     * Enforces strict 1-to-1 uniqueness:
     * - One application can only be mapped once
     * - One custom drawing can only be mapped once
     * Returns true on success, false on validation failure.
     */
    fun saveMapping(
        id: String? = null,
        drawingStrokes: List<DrawingStroke>,
        selectedApp: AppInfo?,
        onSuccess: () -> Unit
    ): Boolean {
        // 1. Check drawing is not empty
        val points = drawingStrokes.sumOf { it.points.size }
        if (drawingStrokes.isEmpty() || points == 0) {
            _uiState.value = _uiState.value.copy(userMessage = "Please draw a widget design.")
            return false
        }

        // 2. Check application is selected
        if (selectedApp == null) {
            _uiState.value = _uiState.value.copy(userMessage = "Please select an application.")
            return false
        }

        // 3. Generate canonical drawing representation & hash
        val hash = com.suhas.mapschedule.drawing.DrawingFingerprint.computeHash(drawingStrokes)

        // 4. Check duplicate drawing
        if (repository.isDrawingMapped(hash, excludeMappingId = id)) {
            android.util.Log.w(
                "MapSchedule",
                "Mapping duplicate rejection: reason=duplicate drawing, hash=$hash"
            )
            _uiState.value = _uiState.value.copy(
                userMessage = "This design is already mapped."
            )
            return false
        }

        // 5. Check duplicate application package
        if (repository.isPackageMapped(selectedApp.packageName, excludeMappingId = id)) {
            android.util.Log.w(
                "MapSchedule",
                "Mapping duplicate rejection: reason=duplicate package, pkg=${selectedApp.packageName}"
            )
            _uiState.value = _uiState.value.copy(
                userMessage = "This application is already mapped."
            )
            return false
        }

        // 6. Valid -> proceed to save
        viewModelScope.launch {
            val mappingId = id ?: UUID.randomUUID().toString()
            val existing = if (id != null) repository.getMappingById(id) else null
            val mapping = AppMapping(
                id = mappingId,
                drawingStrokes = drawingStrokes,
                drawingHash = hash,
                appName = selectedApp.appName,
                packageName = selectedApp.packageName,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            withContext(Dispatchers.IO) {
                repository.saveMapping(mapping)
                // Notify widgets to update with new widget design
                MapScheduleWidgetUpdater.updateAllWidgets(getApplication())
            }

            loadMappings()
            onSuccess()
        }
        return true
    }

    // Backward-compatibility overload for legacy 2-drawing callers
    fun saveMapping(
        id: String? = null,
        mappingCharacterStrokes: List<DrawingStroke>,
        widgetDesignStrokes: List<DrawingStroke>,
        selectedApp: AppInfo?,
        onSuccess: () -> Unit
    ): Boolean = saveMapping(
        id = id,
        drawingStrokes = if (widgetDesignStrokes.isNotEmpty()) widgetDesignStrokes else mappingCharacterStrokes,
        selectedApp = selectedApp,
        onSuccess = onSuccess
    )

    fun deleteMapping(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteMapping(id)
                MapScheduleWidgetUpdater.updateAllWidgets(getApplication())
            }
            loadMappings()
        }
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
