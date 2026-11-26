package com.liquidglass.lab.showcase

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.lab.model.LabState
import com.liquidglass.sdk.components.GlassButton
import com.liquidglass.sdk.components.GlassMorphCluster
import com.liquidglass.sdk.components.GlassSurface
import com.liquidglass.sdk.components.glassForegroundColor
import com.liquidglass.sdk.core.GlassBackdropState
import com.liquidglass.sdk.core.GlassVariant
import kotlin.math.roundToInt

/**
 * Free-form Draggable Glass Optical Test Bench.
 * Drag 4 distinct glass geometries (Lens Circle, Capsule Pill, Squircle Card, Long Bar)
 * anywhere over high-contrast grids, photos, checkerboards, or text to observe live edge refraction.
 */
@Composable
fun DraggablePlaygroundScreen(
    state: LabState,
    backdrop: GlassBackdropState,
    topPaddingDp: Float,
    bottomPaddingDp: Float,
    modifier: Modifier = Modifier
) {
    val engineId = state.selectedEngineId
    val params = state.parameters
    val isDark = state.backdropIsDark
    val fg = glassForegroundColor(params, isDark)

    var circlePos by remember { mutableStateOf(Offset(-90f, -150f)) }
    var pillPos by remember { mutableStateOf(Offset(80f, -60f)) }
    var cardPos by remember { mutableStateOf(Offset(0f, 110f)) }
    var barPos by remember { mutableStateOf(Offset(0f, 290f)) }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Draggable Clear Circular Magnifying Lens
        GlassSurface(
            backdrop = backdrop,
            engineId = engineId,
            parameters = params.copy(variant = GlassVariant.CLEAR, blurRadiusDp = 1f),
            cornerRadiusDp = 60.dp,
            interactive = true,
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset(circlePos.x.roundToInt(), circlePos.y.roundToInt()) }
                .size(120.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        circlePos += drag
                    }
                }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.CenterFocusStrong, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Text("Clear Lens", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // 2. Draggable Capsule Pill (Active Variant)
        GlassSurface(
            backdrop = backdrop,
            engineId = engineId,
            parameters = params,
            cornerRadiusDp = 32.dp,
            interactive = true,
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset(pillPos.x.roundToInt(), pillPos.y.roundToInt()) }
                .size(width = 180.dp, height = 64.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        pillPos += drag
                    }
                }
        ) {
            Text(
                text = "Drag Capsule",
                color = fg,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // 3. Draggable Rounded Squircle Card
        GlassSurface(
            backdrop = backdrop,
            engineId = engineId,
            parameters = params,
            cornerRadiusDp = params.cornerRadiusDp.dp,
            interactive = true,
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset(cardPos.x.roundToInt(), cardPos.y.roundToInt()) }
                .size(width = 230.dp, height = 132.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        cardPos += drag
                    }
                }
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(state.activeEngine.shortName, color = fg, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Drag across high-contrast lines to inspect bevel compression, rim sheen & dispersion.",
                    color = fg.copy(alpha = 0.78f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        // 4. Draggable Elongated Bar (tests medial-axis dome stability!)
        GlassSurface(
            backdrop = backdrop,
            engineId = engineId,
            parameters = params,
            cornerRadiusDp = 28.dp,
            interactive = true,
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset(barPos.x.roundToInt(), barPos.y.roundToInt()) }
                .size(width = 290.dp, height = 56.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        barPos += drag
                    }
                }
        ) {
            Text("Elongated Pill — Medial Axis Test", color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        // Reset positions button
        FilledTonalIconButton(
            onClick = {
                circlePos = Offset(-90f, -150f)
                pillPos = Offset(80f, -60f)
                cardPos = Offset(0f, 110f)
                barPos = Offset(0f, 290f)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = bottomPaddingDp.dp + 18.dp)
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = "Reset draggable positions")
        }
    }
}

/**
 * Dedicated full-screen Liquid Morphing & Merging Bench (`smin` smooth union).
 */
@Composable
fun MorphBenchScreen(
    state: LabState,
    backdrop: GlassBackdropState,
    bottomPaddingDp: Float,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var dropletOffsetDp by remember { mutableStateOf(Offset(76f, 0f)) }

    Box(modifier = modifier.fillMaxSize()) {
        GlassMorphCluster(
            backdrop = backdrop,
            engineId = state.selectedEngineId,
            parameters = state.parameters,
            expanded = expanded,
            onToggleExpanded = { expanded = !expanded },
            dropletOffsetDp = dropletOffsetDp,
            onDropletDragDp = { dropletOffsetDp = it },
            backdropIsDark = state.backdropIsDark,
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = bottomPaddingDp.dp + 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            GlassButton(
                text = if (expanded) "Merge Orbs" else "Split Orbs",
                backdrop = backdrop,
                engineId = state.selectedEngineId,
                parameters = state.parameters,
                backdropIsDark = state.backdropIsDark,
                onClick = { expanded = !expanded }
            )
            GlassButton(
                text = "Center Droplet",
                backdrop = backdrop,
                engineId = state.selectedEngineId,
                parameters = state.parameters,
                backdropIsDark = state.backdropIsDark,
                onClick = { dropletOffsetDp = Offset(58f, 0f) }
            )
        }
    }
}
