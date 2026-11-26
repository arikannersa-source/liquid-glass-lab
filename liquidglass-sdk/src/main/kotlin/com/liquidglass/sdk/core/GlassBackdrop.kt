package com.liquidglass.sdk.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Holds a live hardware-accelerated [GraphicsLayer] capture of the background scene
 * that floating Liquid Glass surfaces refract in real time.
 *
 * Zero CPU bitmap readbacks: everything stays inside Android's HWUI RenderNode graph.
 */
@Stable
class GlassBackdropState internal constructor(
    val graphicsLayer: GraphicsLayer,
    fallbackColor: Color
) {
    var coordinates: LayoutCoordinates? by mutableStateOf(null)
        internal set

    var fallbackColor: Color by mutableStateOf(fallbackColor)

    /**
     * Draw-phase invalidation token updated by scrolling or animated backgrounds so
     * floating glass surfaces redraw in exact lockstep with dynamic content behind them.
     */
    var contentVersion: Long by mutableLongStateOf(0L)
        private set

    fun notifyContentChanged() {
        contentVersion++
    }
}

@Composable
fun rememberGlassBackdropState(
    fallbackColor: Color = Color(0xFF101218)
): GlassBackdropState {
    val layer = rememberGraphicsLayer()
    val state = remember(layer) { GlassBackdropState(layer, fallbackColor) }
    state.fallbackColor = fallbackColor
    return state
}

/**
 * Marks a background composable subtree as the live refractive backdrop source for
 * sibling/overlay Liquid Glass elements sharing [state].
 *
 * Important Apple/Android rule: ensure the background subtree paints an opaque base
 * so refracted samples never read transparent black at screen boundaries.
 */
fun Modifier.liquidGlassBackdropSource(
    state: GlassBackdropState
): Modifier = this
    .onGloballyPositioned { state.coordinates = it }
    .drawWithContent {
        state.graphicsLayer.record {
            this@drawWithContent.drawContent()
        }
        drawLayer(state.graphicsLayer)
    }
