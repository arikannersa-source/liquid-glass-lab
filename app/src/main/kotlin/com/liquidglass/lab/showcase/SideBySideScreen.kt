package com.liquidglass.lab.showcase

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.lab.backgrounds.LabBackgroundCanvas
import com.liquidglass.lab.model.LabState
import com.liquidglass.sdk.components.GlassButton
import com.liquidglass.sdk.components.GlassSurface
import com.liquidglass.sdk.components.GlassSwitch
import com.liquidglass.sdk.components.glassForegroundColor
import com.liquidglass.sdk.core.GlassEngineId
import com.liquidglass.sdk.core.GlassEngineRegistry
import com.liquidglass.sdk.core.GlassParameters
import com.liquidglass.sdk.core.GlassVariant
import com.liquidglass.sdk.core.liquidGlassBackdropSource
import com.liquidglass.sdk.core.rememberGlassBackdropState
import kotlin.math.roundToInt

/**
 * Synchronized Side-by-Side Comparison view (Top/Bottom OR Left/Right split).
 *
 * Both halves render IDENTICAL backgrounds, components, dimensions, and lighting.
 * Dragging the glass lens or pill on either half moves its twin on the opposite half in lockstep.
 */
@Composable
fun SideBySideComparisonScreen(
    state: LabState,
    topPaddingDp: Float,
    bottomPaddingDp: Float,
    modifier: Modifier = Modifier
) {
    var syncedDrag by remember { mutableStateOf(Offset.Zero) }
    var switchState by remember { mutableStateOf(true) }

    val paramsA = state.parameters
    val paramsB = if (state.compareUseNativeDefaults) {
        GlassEngineRegistry.get(state.compareEngineId)
            .defaultParameters(paramsA.variant, paramsA.appearance)
            .copy(
                lightAngleDegrees = paramsA.lightAngleDegrees,
                cornerRadiusDp = paramsA.cornerRadiusDp
            )
    } else {
        paramsA
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = topPaddingDp.dp + 4.dp, bottom = bottomPaddingDp.dp)
    ) {
        // Compact Material 3 Comparison Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EngineDropdownChip(
                    prefix = "A",
                    selected = state.selectedEngineId,
                    onSelect = { state.selectEngine(it) },
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val oldA = state.selectedEngineId
                        state.selectEngine(state.compareEngineId)
                        state.compareEngineId = oldA
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Rounded.SwapHoriz, contentDescription = "Swap engines A and B")
                }
                EngineDropdownChip(
                    prefix = "B",
                    selected = state.compareEngineId,
                    onSelect = { state.compareEngineId = it },
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { state.compareVerticalSplit = !state.compareVerticalSplit },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        if (state.compareVerticalSplit) Icons.Rounded.ViewAgenda else Icons.Rounded.ViewColumn,
                        contentDescription = "Toggle horizontal/vertical split"
                    )
                }
            }
        }

        // Native Defaults vs Locked Identical Params toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = state.compareUseNativeDefaults,
                onClick = { state.compareUseNativeDefaults = true },
                label = { Text("Native Defaults", fontSize = 11.sp) }
            )
            FilterChip(
                selected = !state.compareUseNativeDefaults,
                onClick = { state.compareUseNativeDefaults = false },
                label = { Text("Locked Identical Sliders", fontSize = 11.sp) }
            )
        }

        // Split viewport
        if (state.compareVerticalSplit) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                ComparisonPane(
                    label = "A · ${state.activeEngine.shortName}",
                    engineId = state.selectedEngineId,
                    parameters = paramsA,
                    state = state,
                    syncedDrag = syncedDrag,
                    onDrag = { syncedDrag += it },
                    switchChecked = switchState,
                    onSwitchToggle = { switchState = it },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.65f)))
                ComparisonPane(
                    label = "B · ${state.compareEngine.shortName}",
                    engineId = state.compareEngineId,
                    parameters = paramsB,
                    state = state,
                    syncedDrag = syncedDrag,
                    onDrag = { syncedDrag += it },
                    switchChecked = switchState,
                    onSwitchToggle = { switchState = it },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        } else {
            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                ComparisonPane(
                    label = "A · ${state.activeEngine.shortName}",
                    engineId = state.selectedEngineId,
                    parameters = paramsA,
                    state = state,
                    syncedDrag = syncedDrag,
                    onDrag = { syncedDrag += it },
                    switchChecked = switchState,
                    onSwitchToggle = { switchState = it },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
                Box(modifier = Modifier.height(2.dp).fillMaxWidth().background(Color.White.copy(alpha = 0.65f)))
                ComparisonPane(
                    label = "B · ${state.compareEngine.shortName}",
                    engineId = state.compareEngineId,
                    parameters = paramsB,
                    state = state,
                    syncedDrag = syncedDrag,
                    onDrag = { syncedDrag += it },
                    switchChecked = switchState,
                    onSwitchToggle = { switchState = it },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ComparisonPane(
    label: String,
    engineId: GlassEngineId,
    parameters: GlassParameters,
    state: LabState,
    syncedDrag: Offset,
    onDrag: (Offset) -> Unit,
    switchChecked: Boolean,
    onSwitchToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val paneBackdrop = rememberGlassBackdropState()
    val isDark = state.backdropIsDark
    val fg = glassForegroundColor(parameters, isDark)

    Box(modifier = modifier.clipToBounds()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .liquidGlassBackdropSource(paneBackdrop)
        ) {
            LabBackgroundCanvas(
                mode = state.backgroundMode,
                photoIndex = state.photoIndex,
                solidColorIndex = state.solidColorIndex,
                checkerScaleIndex = state.checkerScaleIndex,
                textWallDark = state.textWallDark,
                animationSpeed = parameters.animationSpeed,
                backdrop = paneBackdrop
            )
        }

        // Identical synced draggable glass assembly
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset(syncedDrag.x.roundToInt(), syncedDrag.y.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, delta ->
                        change.consume()
                        onDrag(delta)
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Identical Glass Card / Lens Pill
            GlassSurface(
                backdrop = paneBackdrop,
                engineId = engineId,
                parameters = parameters,
                cornerRadiusDp = parameters.cornerRadiusDp.dp,
                interactive = true,
                modifier = Modifier.size(width = 210.dp, height = 84.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = GlassEngineRegistry.get(engineId).shortName,
                        color = fg,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${parameters.variant.label} · Synced Drag",
                        color = fg.copy(alpha = 0.78f),
                        fontSize = 11.sp
                    )
                }
            }

            // Identical Clear Circular Lens + Switch + Button row
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassSurface(
                    backdrop = paneBackdrop,
                    engineId = engineId,
                    parameters = parameters.copy(variant = GlassVariant.CLEAR, blurRadiusDp = 1f),
                    cornerRadiusDp = 28.dp,
                    modifier = Modifier.size(56.dp)
                ) {
                    Text("Lens", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                GlassSwitch(
                    checked = switchChecked,
                    onCheckedChange = onSwitchToggle,
                    backdrop = paneBackdrop,
                    engineId = engineId,
                    parameters = parameters
                )
                GlassButton(
                    text = "Tap",
                    backdrop = paneBackdrop,
                    engineId = engineId,
                    parameters = parameters,
                    backdropIsDark = isDark,
                    onClick = {}
                )
            }
        }

        // Corner engine label pill
        Surface(
            color = Color.Black.copy(alpha = 0.68f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun EngineDropdownChip(
    prefix: String,
    selected: GlassEngineId,
    onSelect: (GlassEngineId) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val engine = GlassEngineRegistry.get(selected)
    Box(modifier = modifier) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Text(
                text = "$prefix: ${engine.shortName}",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            GlassEngineRegistry.all().forEach { candidate ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(candidate.displayName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(candidate.license, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    onClick = {
                        onSelect(candidate.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
