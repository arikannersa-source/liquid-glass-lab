package com.liquidglass.sdk.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.sdk.core.GlassBackdropState
import com.liquidglass.sdk.core.GlassEngineId
import com.liquidglass.sdk.core.GlassParameters
import com.liquidglass.sdk.core.GlassSubShape
import com.liquidglass.sdk.modifier.GlassMotionPhysics
import com.liquidglass.sdk.modifier.liquidGlass
import kotlin.math.roundToInt

/**
 * Interactive liquid merging & morphing glass cluster using real-time SDF smooth union (`smin`).
 *
 * The central capsule renders in one unified AGSL shader pass together with a freely draggable
 * glass droplet and two elastic morphing satellite orbs. Drag the satellite droplet toward or
 * away from the hub (or tap the hub to expand/collapse) to observe live liquid bridge formation,
 * refractive edge bending across the merging neck, and continuous specular highlights.
 */
@Composable
fun GlassMorphCluster(
    backdrop: GlassBackdropState,
    engineId: GlassEngineId,
    parameters: GlassParameters,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onToggleExpanded: () -> Unit = {},
    dropletOffsetDp: Offset = Offset(78f, 0f),
    onDropletDragDp: ((Offset) -> Unit)? = null,
    backdropIsDark: Boolean = true
) {
    val density = LocalDensity.current
    val fg = glassForegroundColor(parameters, backdropIsDark)

    var localDropletOffset by remember { mutableStateOf(dropletOffsetDp) }
    val activeDropletDp = if (onDropletDragDp != null) dropletOffsetDp else localDropletOffset

    val expandAnim by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = GlassMotionPhysics.elasticFloatSpring(parameters.animationSpeed, dampingRatio = 0.58f),
        label = "morphExpand"
    )

    val hubW = 88.dp + (44.dp * expandAnim)
    val hubH = 74.dp

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val hubWPx = with(density) { hubW.toPx() }
        val hubHPx = with(density) { hubH.toPx() }
        val hubCenterLocal = Offset(hubWPx * 0.5f, hubHPx * 0.5f)

        val satDistPx = with(density) { (26.dp + 68.dp * expandAnim).toPx() }
        val satRPx = with(density) { (22.dp + 6.dp * expandAnim).toPx() }
        val dropRPx = with(density) { 32.dp.toPx() }

        // Satellite positions expressed in the central hub's local pixel coordinates!
        val dragCenterLocal = Offset(
            hubCenterLocal.x + activeDropletDp.x * density.density,
            hubCenterLocal.y + activeDropletDp.y * density.density
        )

        val subShapes = listOf(
            // Freely draggable droplet
            GlassSubShape(
                centerX = dragCenterLocal.x,
                centerY = dragCenterLocal.y,
                halfWidth = dropRPx,
                halfHeight = dropRPx,
                cornerRadius = dropRPx
            ),
            // Left morphing orb that emerges smoothly from the hub on tap
            GlassSubShape(
                centerX = hubCenterLocal.x - satDistPx,
                centerY = hubCenterLocal.y,
                halfWidth = satRPx,
                halfHeight = satRPx,
                cornerRadius = satRPx
            ),
            // Top morphing orb that emerges smoothly from the hub on tap
            GlassSubShape(
                centerX = hubCenterLocal.x,
                centerY = hubCenterLocal.y - satDistPx * 0.82f,
                halfWidth = satRPx * 0.92f,
                halfHeight = satRPx * 0.92f,
                cornerRadius = satRPx * 0.92f
            )
        )

        // Single unified AGSL pass sized to the central hub with generous padding covering all satellites
        Box(
            modifier = Modifier
                .size(width = hubW, height = hubH)
                .liquidGlass(
                    backdrop = backdrop,
                    engineId = engineId,
                    parameters = parameters,
                    cornerRadiusOverrideDp = hubH / 2f,
                    touchOffsetPx = dragCenterLocal,
                    pressAmount = expandAnim * 0.45f,
                    extraShapes = subShapes,
                    extraPadDp = 156.dp
                )
                .pointerInput(onToggleExpanded) {
                    detectTapGestures { onToggleExpanded() }
                },
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = if (expanded) "Split" else "Tap Morph",
                style = TextStyle(color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            )
        }

        // Draggable satellite handle positioned directly over the draggable glass droplet
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (activeDropletDp.x * density.density).roundToInt(),
                        (activeDropletDp.y * density.density).roundToInt()
                    )
                }
                .size(64.dp)
                .pointerInput(activeDropletDp) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaDp = Offset(dragAmount.x / density.density, dragAmount.y / density.density)
                        val updated = Offset(
                            (activeDropletDp.x + deltaDp.x).coerceIn(-115f, 115f),
                            (activeDropletDp.y + deltaDp.y).coerceIn(-85f, 85f)
                        )
                        if (onDropletDragDp != null) onDropletDragDp(updated) else localDropletOffset = updated
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = "Drag",
                style = TextStyle(color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            )
        }
    }
}
