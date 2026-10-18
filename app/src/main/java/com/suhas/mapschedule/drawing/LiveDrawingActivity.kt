package com.suhas.mapschedule.drawing

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suhas.mapschedule.MainActivity
import com.suhas.mapschedule.data.InstalledAppsProvider
import com.suhas.mapschedule.model.AppMapping
import com.suhas.mapschedule.model.DrawingStroke
import com.suhas.mapschedule.repository.MappingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Option A: In-Place Zero-Animation Interactive Drawing Canvas.
 *
 * Characteristics:
 * - Activates directly at the exact pixel position of the home-screen widget using sourceBounds.
 * - Zero window open/close animation (instant 0ms visual transformation of the widget pad).
 * - Multi-stroke freehand drawing support.
 * - 750ms inactivity timeout triggers deterministic point-cloud matching.
 * - Triple-tap immediately opens Map Schedule Main UI.
 * - Tapping outside the widget box cleanly dismisses without distraction.
 * - Leaves other home screen pages completely untouched.
 */
class LiveDrawingActivity : ComponentActivity() {

    private lateinit var repository: MappingRepository
    private lateinit var appsProvider: InstalledAppsProvider

    private val currentMappings = mutableStateOf<List<AppMapping>>(emptyList())
    private val isMasterOn = mutableStateOf(true)
    private val currentSourceBounds = mutableStateOf<Rect?>(null)
    private val isFromWidget = mutableStateOf(false)
    private val widgetMinWidthDp = mutableStateOf(0)
    private val widgetMinHeightDp = mutableStateOf(0)
    private val widgetMaxWidthDp = mutableStateOf(0)
    private val widgetMaxHeightDp = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        disableActivityTransitions()

        repository = MappingRepository.getInstance(applicationContext)
        appsProvider = InstalledAppsProvider(applicationContext)

        refreshStateFromIntent(intent)

        if (!repository.isMasterEnabled()) {
            handleDisabledWidgetTap()
            return
        }

        setContent {
            val isPortrait = resources.configuration.orientation != android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val targetWidthDp = if (isPortrait) {
                if (widgetMinWidthDp.value > 0) widgetMinWidthDp.value else widgetMaxWidthDp.value
            } else {
                if (widgetMaxWidthDp.value > 0) widgetMaxWidthDp.value else widgetMinWidthDp.value
            }
            val targetHeightDp = if (isPortrait) {
                if (widgetMinHeightDp.value > 0) widgetMinHeightDp.value else widgetMaxHeightDp.value
            } else {
                if (widgetMinHeightDp.value > 0) widgetMinHeightDp.value else widgetMaxHeightDp.value
            }

            LiveDrawingScreen(
                sourceBounds = currentSourceBounds.value,
                targetWidthDp = targetWidthDp,
                targetHeightDp = targetHeightDp,
                fromWidgetTap = isFromWidget.value,
                isMasterEnabled = isMasterOn.value,
                mappings = currentMappings.value,
                onOpenMainUi = {
                    openMainUi()
                },
                onLaunchApp = { mapping ->
                    launchMappedApp(mapping)
                },
                onClose = {
                    finishAndRemoveWithZeroAnimation()
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        disableActivityTransitions()
        // Always refresh mappings from disk so any newly created/edited mapping is immediately recognized
        currentMappings.value = repository.refreshMappings()
        isMasterOn.value = repository.isMasterEnabled()

        if (!repository.isMasterEnabled()) {
            finishAndRemoveWithZeroAnimation()
            return
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        disableActivityTransitions()
        refreshStateFromIntent(intent)

        if (!repository.isMasterEnabled()) {
            handleDisabledWidgetTap()
            return
        }
    }

    private fun handleDisabledWidgetTap() {
        val now = System.currentTimeMillis()
        synchronized(disabledTapLock) {
            disabledTapTimestamps.removeAll { now - it > 1000L }
            disabledTapTimestamps.add(now)

            if (disabledTapTimestamps.size >= 3) {
                disabledTapTimestamps.clear()
                android.util.Log.i("MapSchedule", "Triple tap detected while OFF -> Opening Main UI")
                openMainUi()
            } else {
                android.util.Log.i("MapSchedule", "Widget tapped while OFF (${disabledTapTimestamps.size}/3 taps) -> Showing toast & shutting down")
                Toast.makeText(this, "Map Schedule is OFF (triple-tap to open settings)", Toast.LENGTH_SHORT).show()
                finishAndRemoveWithZeroAnimation()
            }
        }
    }

    private fun refreshStateFromIntent(intent: Intent?) {
        currentSourceBounds.value = intent?.sourceBounds
        isFromWidget.value = intent?.getBooleanExtra(EXTRA_FROM_WIDGET_TAP, false) ?: false

        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val sysOptions = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            try {
                AppWidgetManager.getInstance(applicationContext).getAppWidgetOptions(appWidgetId)
            } catch (_: Exception) {
                null
            }
        } else null

        val minW = sysOptions?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)?.takeIf { it > 0 }
            ?: (intent?.getIntExtra(EXTRA_WIDGET_MIN_WIDTH_DP, 0) ?: 0)
        val maxW = sysOptions?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)?.takeIf { it > 0 }
            ?: (intent?.getIntExtra(EXTRA_WIDGET_MAX_WIDTH_DP, 0) ?: 0)
        val minH = sysOptions?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)?.takeIf { it > 0 }
            ?: (intent?.getIntExtra(EXTRA_WIDGET_MIN_HEIGHT_DP, 0) ?: 0)
        val maxH = sysOptions?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)?.takeIf { it > 0 }
            ?: (intent?.getIntExtra(EXTRA_WIDGET_MAX_HEIGHT_DP, 0) ?: 0)

        widgetMinWidthDp.value = minW
        widgetMaxWidthDp.value = maxW
        widgetMinHeightDp.value = minH
        widgetMaxHeightDp.value = maxH
        currentMappings.value = repository.refreshMappings()
        isMasterOn.value = repository.isMasterEnabled()

        sysOptions?.keySet()?.forEach { key ->
            android.util.Log.i("MapSchedule", "sysOptions key: $key = ${sysOptions.get(key)}")
        }

        android.util.Log.i(
            "MapSchedule",
            "LiveDrawingActivity: sourceBounds=${currentSourceBounds.value}, minW=$minW, maxW=$maxW, minH=$minH, maxH=$maxH"
        )
    }

    private fun disableActivityTransitions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    override fun finish() {
        super.finish()
        disableActivityTransitions()
    }

    private fun finishAndRemoveWithZeroAnimation() {
        finishAndRemoveTask()
        disableActivityTransitions()
    }

    private fun launchMappedApp(mapping: AppMapping) {
        if (!repository.isMasterEnabled()) {
            Toast.makeText(this, "Map Schedule is OFF (triple-tap to open settings)", Toast.LENGTH_SHORT).show()
            finishAndRemoveWithZeroAnimation()
            return
        }

        val launchIntent = appsProvider.getLaunchIntent(mapping.packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            )
            try {
                startActivity(launchIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to launch ${mapping.appName}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Mapped application is no longer installed.", Toast.LENGTH_LONG).show()
        }

        // Always remove task immediately so the drawing pad disappears completely
        finishAndRemoveWithZeroAnimation()
    }

    private fun openMainUi() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finishAndRemoveWithZeroAnimation()
    }

    companion object {
        const val EXTRA_FROM_WIDGET_TAP = "extra_from_widget_tap"
        const val EXTRA_WIDGET_MIN_WIDTH_DP = "extra_widget_min_width_dp"
        const val EXTRA_WIDGET_MAX_WIDTH_DP = "extra_widget_max_width_dp"
        const val EXTRA_WIDGET_MIN_HEIGHT_DP = "extra_widget_min_height_dp"
        const val EXTRA_WIDGET_MAX_HEIGHT_DP = "extra_widget_max_height_dp"

        private val disabledTapLock = Any()
        private val disabledTapTimestamps = mutableListOf<Long>()
    }
}

@Composable
fun LiveDrawingScreen(
    sourceBounds: Rect?,
    targetWidthDp: Int,
    targetHeightDp: Int,
    fromWidgetTap: Boolean,
    isMasterEnabled: Boolean,
    mappings: List<AppMapping>,
    onOpenMainUi: () -> Unit,
    onLaunchApp: (AppMapping) -> Unit,
    onClose: () -> Unit
) {
    var drawnStrokes by remember { mutableStateOf<List<DrawingStroke>>(emptyList()) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var inactivityJob by remember { mutableStateOf<Job?>(null) }

    // When mappings change or screen is re-entered, ensure clean canvas
    LaunchedEffect(mappings) {
        drawnStrokes = emptyList()
        feedbackMessage = null
    }

    // Triple tap tracking: include initial widget tap if launched via widget click
    val tapTimestamps = remember {
        val list = mutableListOf<Long>()
        if (fromWidgetTap) {
            list.add(System.currentTimeMillis())
        }
        list
    }

    fun registerTap() {
        val now = System.currentTimeMillis()
        tapTimestamps.removeAll { now - it > 1000L }
        tapTimestamps.add(now)
        if (tapTimestamps.size >= 3) {
            tapTimestamps.clear()
            onOpenMainUi()
        }
    }

    fun executeMatch() {
        if (!isMasterEnabled) {
            feedbackMessage = "Map Schedule is OFF"
            scope.launch {
                delay(1000L)
                feedbackMessage = null
                drawnStrokes = emptyList()
            }
            return
        }

        if (drawnStrokes.isEmpty()) return

        if (mappings.isEmpty()) {
            feedbackMessage = "No matching design"
            scope.launch {
                delay(800L)
                feedbackMessage = null
                drawnStrokes = emptyList()
            }
            return
        }

        when (val result = GestureMatcher.match(drawnStrokes, mappings)) {
            is GestureMatcher.MatchResult.Matched -> {
                val matched = result.mapping
                // IMMEDIATELY clear strokes so the drawing disappears right as the app opens
                drawnStrokes = emptyList()
                feedbackMessage = null
                inactivityJob?.cancel()
                onLaunchApp(matched)
            }
            is GestureMatcher.MatchResult.Ambiguous,
            is GestureMatcher.MatchResult.NoMatch -> {
                feedbackMessage = "No matching design"
                scope.launch {
                    delay(800L)
                    feedbackMessage = null
                    // Clear the drawing so user can immediately draw the new one
                    drawnStrokes = emptyList()
                }
            }
            is GestureMatcher.MatchResult.Empty -> {
                drawnStrokes = emptyList()
            }
        }
    }

    // Auto-match after 750ms of inactivity after drawing
    LaunchedEffect(drawnStrokes) {
        if (drawnStrokes.isNotEmpty()) {
            inactivityJob?.cancel()
            inactivityJob = scope.launch {
                delay(750L) // Inactivity timeout allows multi-stroke drawings
                executeMatch()
            }
        }
    }

    val density = LocalDensity.current
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onClose() // Tapping anywhere outside the widget box dismisses cleanly
                }
        ) {
            val screenWidthPx = constraints.maxWidth
            val screenHeightPx = constraints.maxHeight
            val densityScale = density.density

            val targetWidthPx = if (targetWidthDp > 0) with(density) { targetWidthDp.dp.roundToPx() } else 0
            val targetHeightPx = if (targetHeightDp > 0) with(density) { targetHeightDp.dp.roundToPx() } else 0

            val overlayBounds = WidgetOverlayBoundsCalculator.calculateBounds(
                sourceBounds = sourceBounds,
                targetWidthPx = targetWidthPx,
                targetHeightPx = targetHeightPx,
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx,
                densityScale = densityScale,
                targetWidthDp = targetWidthDp,
                targetHeightDp = targetHeightDp
            )

            val leftDp = with(density) { overlayBounds.leftPx.toDp() }
            val topDp = with(density) { overlayBounds.topPx.toDp() }
            val widthDp = with(density) { overlayBounds.widthPx.toDp() }
            val heightDp = with(density) { overlayBounds.heightPx.toDp() }

            val boxModifier = Modifier
                .offset { IntOffset(overlayBounds.leftPx, overlayBounds.topPx) }
                .size(width = widthDp, height = heightDp)

            Box(
                modifier = boxModifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .border(1.dp, Color(0xFF222222), RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Absorb clicks inside the box
                    }
            ) {
                // Interactive drawing canvas inside the in-place black widget box
                DrawingCanvas(
                    strokes = drawnStrokes,
                    onStrokesChanged = { updated ->
                        feedbackMessage = null
                        drawnStrokes = updated
                    },
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = Color.Black,
                    showBorder = false,
                    cornerRadius = 16.dp,
                    onTap = { registerTap() }
                )

                // Feedback message inside the widget box
                feedbackMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = Color(0x99FFFFFF),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33333333))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
