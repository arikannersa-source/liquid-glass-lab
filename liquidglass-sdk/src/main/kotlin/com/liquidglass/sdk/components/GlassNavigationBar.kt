package com.liquidglass.sdk.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.liquidglass.sdk.core.GlassVariant
import com.liquidglass.sdk.modifier.GlassMotionPhysics
import com.liquidglass.sdk.modifier.liquidGlass
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Item model for [GlassBottomNavigationBar].
 */
data class GlassNavItem(
    val label: String,
    val icon: @Composable (Color, Boolean) -> Unit
)

/**
 * Apple iOS 26/27 style Floating Liquid Glass Bottom Navigation Bar.
 *
 * Features a live refractive glass capsule bar plus an interactive selection lens
 * that you can tap OR drag/throw across tabs: while dragged, the lens lifts into
 * a clear, magnifying refractive droplet with elastic spring settling!
 */
@Composable
fun GlassBottomNavigationBar(
    items: List<GlassNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: GlassBackdropState,
    engineId: GlassEngineId,
    parameters: GlassParameters,
    modifier: Modifier = Modifier,
    backdropIsDark: Boolean = true
) {
    if (items.isEmpty()) return
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val fg = glassForegroundColor(parameters, backdropIsDark)

    val animFraction = remember { Animatable(selectedIndex.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    var dragVelocity by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(selectedIndex, isDragging) {
        if (!isDragging) {
            animFraction.animateTo(
                targetValue = selectedIndex.toFloat(),
                animationSpec = GlassMotionPhysics.elasticFloatSpring(parameters.animationSpeed, dampingRatio = 0.66f)
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .liquidGlass(
                backdrop = backdrop,
                engineId = engineId,
                parameters = parameters,
                cornerRadiusOverrideDp = 34.dp,
                pressAmount = if (isDragging) 0.45f else 0f
            )
            .padding(horizontal = 6.dp, vertical = 5.dp)
    ) {
        val totalW = with(density) { maxWidth.toPx() }
        val slotW = totalW / items.size.coerceAtLeast(1)
        val slotDp = maxWidth / items.size.coerceAtLeast(1)

        // Elevated clear-lens indicator that magnifies and refracts while sliding or held!
        val lensParams = remember(parameters, isDragging) {
            if (isDragging) {
                parameters.copy(
                    variant = GlassVariant.CLEAR,
                    refractionStrength = (parameters.refractionStrength * 1.30f).coerceAtMost(2.2f),
                    lensDistortion = (parameters.lensDistortion + 0.18f).coerceAtMost(0.85f),
                    blurRadiusDp = (parameters.blurRadiusDp * 0.20f).coerceAtMost(2.5f),
                    transparency = 0.92f,
                    tintOpacity = 0.10f
                )
            } else {
                parameters.copy(
                    tintColorArgb = if (backdropIsDark) 0xFFFFFFFFL else 0xFF0A84FFL,
                    tintOpacity = (parameters.tintOpacity + 0.14f).coerceAtMost(0.45f)
                )
            }
        }

        val currentX = (animFraction.value * slotW).coerceIn(0f, (totalW - slotW).coerceAtLeast(0f))
        val lensLiftScale = if (isDragging) 1f + 0.11f * parameters.touchDeformation.coerceIn(0f, 1.5f) else 1f

        Box(
            modifier = Modifier
                .offset { IntOffset(currentX.roundToInt(), 0) }
                .width(slotDp)
                .fillMaxHeight()
                .graphicsLayer {
                    scaleX = lensLiftScale
                    scaleY = lensLiftScale
                }
                .liquidGlass(
                    backdrop = backdrop,
                    engineId = engineId,
                    parameters = lensParams,
                    cornerRadiusOverrideDp = 29.dp,
                    pressAmount = if (isDragging) 1.0f else 0.1f
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(items.size, slotW) {
                    detectTapGestures { offset ->
                        val idx = (offset.x / slotW).toInt().coerceIn(0, items.lastIndex)
                        onSelect(idx)
                    }
                }
                .pointerInput(items.size, slotW) {
                    detectHorizontalDragGestures(
                        onDragStart = { start ->
                            isDragging = true
                            dragVelocity = 0f
                            val idx = ((start.x - slotW * 0.5f) / slotW).coerceIn(0f, items.lastIndex.toFloat())
                            scope.launch { animFraction.snapTo(idx) }
                        },
                        onDragEnd = {
                            isDragging = false
                            val projected = animFraction.value + (dragVelocity / slotW) * 0.10f
                            val targetIdx = projected.roundToInt().coerceIn(0, items.lastIndex)
                            onSelect(targetIdx)
                            scope.launch {
                                animFraction.animateTo(
                                    targetValue = targetIdx.toFloat(),
                                    animationSpec = GlassMotionPhysics.elasticFloatSpring(parameters.animationSpeed)
                                )
                            }
                        },
                        onDragCancel = { isDragging = false },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragVelocity = dragAmount * 60f
                            val next = (animFraction.value + dragAmount / slotW).coerceIn(0f, items.lastIndex.toFloat())
                            scope.launch { animFraction.snapTo(next) }
                        }
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                val activeColor = if (isSelected) Color(0xFF0A84FF) else fg.copy(alpha = 0.78f)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    item.icon(activeColor, isSelected)
                    BasicText(
                        text = item.label,
                        style = TextStyle(
                            color = activeColor,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}
