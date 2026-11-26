package com.liquidglass.lab

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.lab.backgrounds.LabBackgroundCanvas
import com.liquidglass.lab.controls.BackgroundPickerSheet
import com.liquidglass.lab.controls.CustomizationSheet
import com.liquidglass.lab.controls.PresetManagerSheet
import com.liquidglass.lab.model.LabState
import com.liquidglass.lab.model.LabViewMode
import com.liquidglass.lab.perf.FramePerfSnapshot
import com.liquidglass.lab.perf.FrameRateMonitor
import com.liquidglass.lab.showcase.DraggablePlaygroundScreen
import com.liquidglass.lab.showcase.EngineCatalogSheet
import com.liquidglass.lab.showcase.FullShowcaseScreen
import com.liquidglass.lab.showcase.MorphBenchScreen
import com.liquidglass.lab.showcase.SideBySideComparisonScreen
import com.liquidglass.lab.ui.theme.LiquidGlassLabTheme
import com.liquidglass.sdk.core.GlassEngineRegistry
import com.liquidglass.sdk.core.GlassVariant
import com.liquidglass.sdk.core.liquidGlassBackdropSource
import com.liquidglass.sdk.core.rememberGlassBackdropState
import kotlinx.coroutines.delay
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val frameRateMonitor = FrameRateMonitor(targetHz = 120)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestHighRefreshRate()

        setContent {
            LiquidGlassLabTheme(darkTheme = true, dynamicColor = false) {
                val appContext = LocalContext.current.applicationContext
                val labState = remember { LabState(appContext) }

                GravityLightingEffect(state = labState)

                LiquidGlassLabRoot(
                    state = labState,
                    perfSnapshot = frameRateMonitor.snapshot
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        frameRateMonitor.start(window)
    }

    override fun onPause() {
        frameRateMonitor.stop()
        super.onPause()
    }

    private fun requestHighRefreshRate() {
        runCatching {
            val lp = window.attributes
            lp.preferredRefreshRate = 120f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val modes = display?.supportedModes.orEmpty()
                val highest = modes.maxByOrNull { it.refreshRate }
                if (highest != null) {
                    lp.preferredDisplayModeId = highest.modeId
                }
            }
            window.attributes = lp
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@Composable
private fun GravityLightingEffect(state: LabState) {
    val context = LocalContext.current
    val enabled = state.tiltLightingEnabled
    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return@DisposableEffect onDispose {}
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?: return@DisposableEffect onDispose {}

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.values.size >= 2) {
                    state.updateLightFromGravity(event.values[0], event.values[1])
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
}

@Composable
private fun LiquidGlassLabRoot(
    state: LabState,
    perfSnapshot: FramePerfSnapshot
) {
    val backdrop = rememberGlassBackdropState()
    var showEngineSheet by remember { mutableStateOf(false) }
    var showCustomizeSheet by remember { mutableStateOf(false) }
    var showPresetsSheet by remember { mutableStateOf(false) }
    var showBackgroundSheet by remember { mutableStateOf(false) }

    val statusBarTopDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().value
    val navBarBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().value

    val topContentInsetDp = if (state.hudCollapsed) statusBarTopDp + 8f else statusBarTopDp + 108f
    val bottomContentInsetDp = if (state.hudCollapsed) navBarBottomDp + 8f else navBarBottomDp + 72f

    // Auto-clear transient status banners
    LaunchedEffect(state.statusBanner) {
        if (state.statusBanner != null) {
            delay(2400L)
            state.statusBanner = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Live Optical Viewport
        if (state.viewMode == LabViewMode.SIDE_BY_SIDE) {
            SideBySideComparisonScreen(
                state = state,
                topPaddingDp = topContentInsetDp,
                bottomPaddingDp = bottomContentInsetDp,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Single shared full-screen backdrop captured at GPU speed for all glass elements
            LabBackgroundCanvas(
                mode = state.backgroundMode,
                photoIndex = state.photoIndex,
                solidColorIndex = state.solidColorIndex,
                checkerScaleIndex = state.checkerScaleIndex,
                textWallDark = state.textWallDark,
                animationSpeed = state.parameters.animationSpeed,
                backdrop = backdrop,
                modifier = Modifier
                    .fillMaxSize()
                    .liquidGlassBackdropSource(backdrop)
            )

            when (state.viewMode) {
                LabViewMode.FULL_SHOWCASE -> FullShowcaseScreen(
                    state = state,
                    backdrop = backdrop,
                    topPaddingDp = topContentInsetDp,
                    bottomPaddingDp = bottomContentInsetDp
                )
                LabViewMode.DRAG_PLAYGROUND -> DraggablePlaygroundScreen(
                    state = state,
                    backdrop = backdrop,
                    topPaddingDp = topContentInsetDp,
                    bottomPaddingDp = bottomContentInsetDp
                )
                LabViewMode.MORPH_BENCH -> MorphBenchScreen(
                    state = state,
                    backdrop = backdrop,
                    bottomPaddingDp = bottomContentInsetDp
                )
                LabViewMode.SIDE_BY_SIDE -> Unit
            }
        }

        // 2. Clean Material 3 Laboratory Top HUD (instant engine switching + FPS/ms + view modes)
        AnimatedVisibility(
            visible = !state.hudCollapsed,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopLaboratoryHud(
                state = state,
                perfSnapshot = perfSnapshot,
                topInsetDp = statusBarTopDp,
                onOpenEngineCatalog = { showEngineSheet = true }
            )
        }

        // Floating restore HUD button when HUD is collapsed for pure full-screen inspection
        if (state.hudCollapsed) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = statusBarTopDp.dp + 8.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.showPerfOverlay) {
                    PerfBadge(perfSnapshot = perfSnapshot, onClick = { state.showPerfOverlay = false })
                }
                SmallFloatingActionButton(
                    onClick = { state.hudCollapsed = false },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
                ) {
                    Icon(Icons.Rounded.Visibility, contentDescription = "Show Lab Controls")
                }
            }
        }

        // 3. Transient status toast banner
        AnimatedVisibility(
            visible = state.statusBanner != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomContentInsetDp.dp + 12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.92f)
            ) {
                Text(
                    text = state.statusBanner.orEmpty(),
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // 4. Material 3 Bottom Laboratory Control Dock
        AnimatedVisibility(
            visible = !state.hudCollapsed,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BottomLaboratoryDock(
                state = state,
                bottomInsetDp = navBarBottomDp,
                onOpenBackgrounds = { showBackgroundSheet = true },
                onOpenCustomize = { showCustomizeSheet = true },
                onOpenPresets = { showPresetsSheet = true }
            )
        }

        // 5. Laboratory Modal Sheets
        if (showEngineSheet) {
            EngineCatalogSheet(
                state = state,
                onDismiss = { showEngineSheet = false }
            )
        }
        if (showCustomizeSheet) {
            CustomizationSheet(
                state = state,
                onDismiss = { showCustomizeSheet = false }
            )
        }
        if (showPresetsSheet) {
            PresetManagerSheet(
                state = state,
                onDismiss = { showPresetsSheet = false }
            )
        }
        if (showBackgroundSheet) {
            BackgroundPickerSheet(
                state = state,
                onDismiss = { showBackgroundSheet = false }
            )
        }
    }
}

@Composable
private fun TopLaboratoryHud(
    state: LabState,
    perfSnapshot: FramePerfSnapshot,
    topInsetDp: Float,
    onOpenEngineCatalog: () -> Unit
) {
    val allEngines = remember { GlassEngineRegistry.all() }
    val activeIdx = allEngines.indexOfFirst { it.id == state.selectedEngineId }.coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topInsetDp.dp + 6.dp, start = 10.dp, end = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val prev = (activeIdx - 1 + allEngines.size) % allEngines.size
                        state.selectEngine(allEngines[prev].id)
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous engine")
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenEngineCatalog)
                        .padding(horizontal = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${activeIdx + 1}/${allEngines.size} • ${state.activeEngine.shortName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Tap for engine audit • ${state.parameters.variant.name} • ${state.backgroundMode.label}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = {
                        val next = (activeIdx + 1) % allEngines.size
                        state.selectEngine(allEngines[next].id)
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Next engine")
                }

                if (state.showPerfOverlay) {
                    PerfBadge(
                        perfSnapshot = perfSnapshot,
                        onClick = { state.showPerfOverlay = false }
                    )
                } else {
                    IconButton(
                        onClick = { state.showPerfOverlay = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Rounded.Speed, contentDescription = "Show FPS monitor", modifier = Modifier.size(18.dp))
                    }
                }

                IconButton(
                    onClick = { state.hudCollapsed = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Rounded.VisibilityOff,
                        contentDescription = "Hide HUD for full-screen preview",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // View mode switcher pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LabViewMode.entries.forEach { mode ->
                val selected = state.viewMode == mode
                FilterChip(
                    selected = selected,
                    onClick = { state.viewMode = mode },
                    label = {
                        Text(
                            text = mode.label,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.height(30.dp)
                )
            }
        }
    }
}

@Composable
private fun PerfBadge(
    perfSnapshot: FramePerfSnapshot,
    onClick: () -> Unit
) {
    val fps = perfSnapshot.fps
    val badgeColor = when {
        fps >= 90 -> Color(0xFF1B5E20)
        fps >= 55 -> Color(0xFF0D47A1)
        fps > 0 -> Color(0xFFE65100)
        else -> Color(0xFF263238)
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = badgeColor.copy(alpha = 0.88f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = if (fps > 0) "$fps FPS" else "-- FPS",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = Color.White
            )
            Text(
                text = String.format(Locale.US, "%.1f ms", perfSnapshot.avgFrameMs),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun BottomLaboratoryDock(
    state: LabState,
    bottomInsetDp: Float,
    onOpenBackgrounds: () -> Unit,
    onOpenCustomize: () -> Unit,
    onOpenPresets: () -> Unit
) {
    val isClear = state.parameters.variant == GlassVariant.CLEAR
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = bottomInsetDp.dp + 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockAction(
                icon = Icons.Rounded.Palette,
                label = state.backgroundMode.label,
                onClick = onOpenBackgrounds
            )
            DockAction(
                icon = Icons.Rounded.Layers,
                label = if (isClear) "Clear" else "Regular",
                highlighted = isClear,
                onClick = {
                    state.setVariant(if (isClear) GlassVariant.REGULAR else GlassVariant.CLEAR)
                }
            )
            DockAction(
                icon = Icons.Rounded.Tune,
                label = "Customize",
                highlighted = true,
                onClick = onOpenCustomize
            )
            DockAction(
                icon = Icons.Rounded.BookmarkBorder,
                label = "Presets",
                onClick = onOpenPresets
            )
            DockAction(
                icon = Icons.Rounded.RestartAlt,
                label = "Reset",
                onClick = { state.resetToEngineOriginal() }
            )
        }
    }
}

@Composable
private fun DockAction(
    icon: ImageVector,
    label: String,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    val tint = if (highlighted) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
            color = tint,
            maxLines = 1
        )
    }
}
