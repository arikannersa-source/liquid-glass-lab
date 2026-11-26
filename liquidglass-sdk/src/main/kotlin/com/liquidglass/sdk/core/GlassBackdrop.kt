package com.liquidglass.sdk.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo

/**
 * Holds a live hardware-accelerated [GraphicsLayer] capture of the background scene
 * that floating Liquid Glass surfaces refract in real time.
 *
 * Uses [neverEqualPolicy] on [coordinates] plus direct draw-node invalidation listeners
 * so every pixel of glass movement, scrolling, or background animation updates the
 * refracted backdrop immediately at 120 FPS with zero CPU bitmap readbacks.
 */
@Stable
class GlassBackdropState internal constructor(
    val graphicsLayer: GraphicsLayer,
    fallbackColor: Color
) {
    var coordinates: LayoutCoordinates? by mutableStateOf(null, neverEqualPolicy())
        internal set

    var fallbackColor: Color by mutableStateOf(fallbackColor)

    /**
     * Draw-phase invalidation token updated by scrolling or animated backgrounds so
     * floating glass surfaces redraw in exact lockstep with dynamic content behind them.
     */
    var contentVersion: Long by mutableLongStateOf(0L)
        private set

    private val invalidationListeners = LinkedHashSet<() -> Unit>()

    internal fun addInvalidationListener(listener: () -> Unit) {
        invalidationListeners.add(listener)
    }

    internal fun removeInvalidationListener(listener: () -> Unit) {
        invalidationListeners.remove(listener)
    }

    internal fun dispatchBackdropUpdated() {
        if (invalidationListeners.isEmpty()) return
        // Iterate over snapshot copy in case a node detaches during traversal
        for (listener in invalidationListeners.toList()) {
            listener.invoke()
        }
    }

    fun notifyContentChanged() {
        contentVersion++
        dispatchBackdropUpdated()
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
 */
fun Modifier.liquidGlassBackdropSource(
    state: GlassBackdropState
): Modifier = this.then(GlassBackdropSourceElement(state))

private class GlassBackdropSourceElement(
    val state: GlassBackdropState
) : ModifierNodeElement<GlassBackdropSourceNode>() {

    override fun create(): GlassBackdropSourceNode = GlassBackdropSourceNode(state)

    override fun update(node: GlassBackdropSourceNode) {
        if (node.state !== state) {
            node.state.coordinates = null
            node.state = state
        }
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "liquidGlassBackdropSource"
        properties["state"] = state
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is GlassBackdropSourceElement && other.state === state)

    override fun hashCode(): Int = state.hashCode()
}

private class GlassBackdropSourceNode(
    var state: GlassBackdropState
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {

    override fun ContentDrawScope.draw() {
        state.graphicsLayer.record {
            this@draw.drawContent()
        }
        drawLayer(state.graphicsLayer)
        // Notify all sibling/overlay LiquidGlassNodes whenever the background scene re-records!
        state.dispatchBackdropUpdated()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            state.coordinates = coordinates
            state.dispatchBackdropUpdated()
        }
    }

    override fun onDetach() {
        if (state.coordinates != null) {
            state.coordinates = null
        }
    }
}
