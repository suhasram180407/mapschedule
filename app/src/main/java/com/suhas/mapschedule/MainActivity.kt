package com.suhas.mapschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.suhas.mapschedule.model.AppMapping
import com.suhas.mapschedule.ui.AddMappingScreen
import com.suhas.mapschedule.ui.EditMappingScreen
import com.suhas.mapschedule.ui.MainScreen
import com.suhas.mapschedule.viewmodel.MapScheduleViewModel

sealed interface Screen {
    data object Main : Screen
    data object Add : Screen
    data class Edit(val mapping: AppMapping) : Screen
}

class MainActivity : ComponentActivity() {

    private val viewModel: MapScheduleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val darkColors = darkColorScheme(
                primary = Color(0xFF00E5FF),
                background = Color(0xFF121212),
                surface = Color(0xFF1E1E1E),
                onPrimary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )

            MaterialTheme(colorScheme = darkColors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212)
                ) {
                    MapScheduleApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.suhas.mapschedule.widget.MapScheduleWidgetUpdater.updateAllWidgets(this)
        viewModel.loadMasterEnabled()
        viewModel.loadMappings()
        viewModel.loadInstalledApps()
    }
}

@Composable
fun MapScheduleApp(viewModel: MapScheduleViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }

    when (val screen = currentScreen) {
        is Screen.Main -> {
            MainScreen(
                mappings = uiState.mappings,
                isMasterEnabled = uiState.isMasterEnabled,
                onToggleMasterEnabled = { enabled -> viewModel.setMasterEnabled(enabled) },
                onAddNewMapping = { currentScreen = Screen.Add },
                onEditMapping = { mapping -> currentScreen = Screen.Edit(mapping) },
                onDeleteMapping = { id -> viewModel.deleteMapping(id) }
            )
        }

        is Screen.Add -> {
            AddMappingScreen(
                installedApps = uiState.installedApps,
                mappedPackageNames = uiState.mappedPackageNames,
                isLoadingApps = uiState.isLoadingApps,
                userMessage = uiState.userMessage,
                onClearUserMessage = { viewModel.clearUserMessage() },
                onSaveMapping = { drawingStrokes, selectedApp ->
                    viewModel.saveMapping(
                        id = null,
                        drawingStrokes = drawingStrokes,
                        selectedApp = selectedApp,
                        onSuccess = { currentScreen = Screen.Main }
                    )
                },
                onNavigateBack = { currentScreen = Screen.Main }
            )
        }

        is Screen.Edit -> {
            EditMappingScreen(
                mapping = screen.mapping,
                installedApps = uiState.installedApps,
                mappedPackageNames = viewModel.getMappedPackages(screen.mapping.id),
                isLoadingApps = uiState.isLoadingApps,
                userMessage = uiState.userMessage,
                onClearUserMessage = { viewModel.clearUserMessage() },
                onSaveMapping = { id, drawingStrokes, selectedApp ->
                    viewModel.saveMapping(
                        id = id,
                        drawingStrokes = drawingStrokes,
                        selectedApp = selectedApp,
                        onSuccess = { currentScreen = Screen.Main }
                    )
                },
                onNavigateBack = { currentScreen = Screen.Main }
            )
        }
    }
}
