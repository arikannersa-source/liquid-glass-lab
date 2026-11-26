package com.liquidglass.sdk.modifier

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import com.liquidglass.sdk.core.GlassBackdropState
import com.liquidglass.sdk.core.GlassDrawContext
import com.liquidglass.sdk.core.GlassEngineId
import com.liquidglass.sdk.core.GlassEngineRegistry
import com.liquidglass.sdk.core.GlassParameters
import com.liquidglass.sdk.core.GlassSubShape
import com.liquidglass.sdk.core.LiquidGlassEngine
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Applies any registered [LiquidGlassEngine] directly to any Jetpack Compose element,
 * refracting the live [backdrop] behind it at up to 120 FPS with zero bitmap readback.
 *
 * Implemented as a dedicated [ModifierNodeElement] ([DrawModifierNode] +
 * [GlobalPositionAwareModifierNode] + [LayoutAwareModifierNode]) so every pixel of
 * dragging (`Modifier.offset`), scrolling (`verticalScroll` / `LazyColumn`), layout resize,
 * or animated background frame immediately invalidates and re-renders the refracted scene.
 */
fun Modifier.liquidGlass(
    backdrop: GlassBackdropState,
    engineId: GlassEngineId,
    parameters: GlassParameters,
    cornerRadiusOverrideDp: Dp? = null,
    touchOffsetPx: Offset? = null,
    pressAmount: Float = 0f,
    extraShapes: List<GlassSubShape> = emptyList(),
    extraPadDp: Dp = Dp.Unspecified
): Modifier = liquidGlass(
    backdrop = backdrop,
    engine = GlassEngineRegistry.get(engineId),
    parameters = parameters,
    cornerRadiusOverrideDp = cornerRadiusOverrideDp,
    touchOffsetPx = touchOffsetPx,
    pressAmount = pressAmount,
    extraShapes = extraShapes,
    extraPadDp = extraPadDp
)

/**
 * Engine-instance overload supporting custom third-party [LiquidGlassEngine] plugins.
 */
fun Modifier.liquidGlass(
    backdrop: GlassBackdropState,
    engine: LiquidGlassEngine,
    parameters: GlassParameters,
    cornerRadiusOverrideDp: Dp? = null,
    touchOffsetPx: Offset? = null,
    pressAmount: Float = 0f,
    extraShapes: List<GlassSubShape> = emptyList(),
    extraPadDp: Dp = Dp.Unspecified
): Modifier = this.then(
    LiquidGlassElement(
        backdrop = backdrop,
        engine = engine,
        parameters = parameters,
        cornerRadiusOverrideDp = cornerRadiusOverrideDp,
        touchOffsetPx = touchOffsetPx,
        pressAmount = pressAmount,
        extraShapes = extraShapes,
        extraPadDp = extraPadDp
    )
)

private data class LiquidGlassElement(
    val backdrop: GlassBackdropState,
    val engine: LiquidGlassEngine,
    val parameters: GlassParameters,
    val cornerRadiusOverrideDp: Dp?,
    val touchOffsetPx: Offset?,
    val pressAmount: Float,
    val extraShapes: List<GlassSubShape>,
    val extraPadDp: Dp
) : ModifierNodeElement<LiquidGlassNode>() {

    override fun create(): LiquidGlassNode = LiquidGlassNode(
        backdrop = backdrop,
        engine = engine,
        parameters = parameters,
        cornerRadiusOverrideDp = cornerRadiusOverrideDp,
        touchOffsetPx = touchOffsetPx,
        pressAmount = pressAmount,
        extraShapes = extraShapes,
        extraPadDp = extraPadDp
    )

    override fun update(node: LiquidGlassNode) {
        node.updateProps(
            newBackdrop = backdrop,
            newEngine = engine,
            newParameters = parameters,
            newCornerRadiusOverrideDp = cornerRadiusOverrideDp,
            newTouchOffsetPx = touchOffsetPx,
            newPressAmount = pressAmount,
            newExtraShapes = extraShapes,
            newExtraPadDp = extraPadDp
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "liquidGlass"
        properties["engine"] = engine.engineKey
        properties["parameters"] = parameters
    }
}

private class LiquidGlassNode(
    var backdrop: GlassBackdropState,
    var engine: LiquidGlassEngine,
    var parameters: GlassParameters,
    var cornerRadiusOverrideDp: Dp?,
    var touchOffsetPx: Offset?,
    var pressAmount: Float,
    var extraShapes: List<GlassSubShape>,
    var extraPadDp: Dp
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode, LayoutAwareModifierNode {

    private var glassLayer: GraphicsLayer? = null
    private var wideBlurLayer: GraphicsLayer? = null
    private var offscreenHostLayer: GraphicsLayer? = null

    // neverEqualPolicy ensures Compose never drops coordinate updates on identical NodeCoordinator instances!
    private var ownCoords: LayoutCoordinates? by mutableStateOf(null, neverEqualPolicy())
    private val shaderCache = HashMap<String, RuntimeShader>()

    private val backdropListener: () -> Unit = {
        if (isAttached) {
            invalidateDraw()
        }
    }

    override fun onAttach() {
        val ctx = requireGraphicsContext()
        glassLayer = ctx.createGraphicsLayer()
        wideBlurLayer = ctx.createGraphicsLayer()
        offscreenHostLayer = ctx.createGraphicsLayer().apply {
            compositingStrategy = CompositingStrategy.Offscreen
        }
        backdrop.addInvalidationListener(backdropListener)
    }

    override fun onDetach() {
        backdrop.removeInvalidationListener(backdropListener)
        val ctx = requireGraphicsContext()
        glassLayer?.let { ctx.releaseGraphicsLayer(it) }
        wideBlurLayer?.let { ctx.releaseGraphicsLayer(it) }
        offscreenHostLayer?.let { ctx.releaseGraphicsLayer(it) }
        glassLayer = null
        wideBlurLayer = null
        offscreenHostLayer = null
        ownCoords = null
        shaderCache.clear()
    }

    fun updateProps(
        newBackdrop: GlassBackdropState,
        newEngine: LiquidGlassEngine,
        newParameters: GlassParameters,
        newCornerRadiusOverrideDp: Dp?,
        newTouchOffsetPx: Offset?,
        newPressAmount: Float,
        newExtraShapes: List<GlassSubShape>,
        newExtraPadDp: Dp
    ) {
        if (backdrop !== newBackdrop) {
            if (isAttached) {
                backdrop.removeInvalidationListener(backdropListener)
                newBackdrop.addInvalidationListener(backdropListener)
            }
            backdrop = newBackdrop
        }
        engine = newEngine
        parameters = newParameters
        cornerRadiusOverrideDp = newCornerRadiusOverrideDp
        touchOffsetPx = newTouchOffsetPx
        pressAmount = newPressAmount
        extraShapes = newExtraShapes
        extraPadDp = newExtraPadDp
        if (isAttached) {
            invalidateDraw()
        }
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            ownCoords = coordinates
            invalidateDraw()
        }
    }

    override fun onRemeasured(size: IntSize) {
        if (isAttached) {
            invalidateDraw()
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            ownCoords = coordinates
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        // Read draw-phase snapshot states so any coordinate or content tick triggers immediate redraw
        @Suppress("UNUSED_VARIABLE")
        val version = backdrop.contentVersion
        val myCoords = ownCoords
        val bgCoords = backdrop.coordinates

        val gLayer = glassLayer
        val wbLayer = wideBlurLayer
        val hostLayer = offscreenHostLayer

        val w = size.width
        val h = size.height
        if (w < 2f || h < 2f ||
            gLayer == null || wbLayer == null || hostLayer == null ||
            myCoords == null || bgCoords == null ||
            !myCoords.isAttached || !bgCoords.isAttached
        ) {
            drawContent()
            return
        }

        val relOffset = try {
            bgCoords.localPositionOf(myCoords, Offset.Zero)
        } catch (_: Exception) {
            myCoords.positionInWindow() - bgCoords.positionInWindow()
        }

        val safeParams = parameters.sanitized()
        val radiusDp = cornerRadiusOverrideDp?.value ?: safeParams.cornerRadiusDp
        val radiusPx = min(radiusDp * density, min(w, h) * 0.5f)
        val blurPx = safeParams.blurRadiusDp * density
        val thickPx = safeParams.glassThicknessDp * density

        // Generous sampling pad covering outward refraction, blur margin, and soft drop shadows.
        val autoPadPx = max(24f * density, max(blurPx * 0.65f, thickPx * 0.85f)).coerceIn(20f, 84f * density)
        val padPx = if (extraPadDp != Dp.Unspecified) {
            max(autoPadPx, extraPadDp.toPx())
        } else {
            autoPadPx
        }

        val recW = ceil(w + 2f * padPx).toInt().coerceAtLeast(4)
        val recH = ceil(h + 2f * padPx).toInt().coerceAtLeast(4)

        val wideScale = 0.25f
        val wideW = if (engine.usesWideBlurStrip) ceil(recW * wideScale).toInt().coerceAtLeast(8) else 0
        val wideH = if (engine.usesWideBlurStrip) ceil(recH * wideScale).toInt().coerceAtLeast(8) else 0
        val wideStripY = if (engine.usesWideBlurStrip) (recH + 4).toFloat() else 0f
        val totalRecH = if (engine.usesWideBlurStrip) (recH + wideH + 8) else recH

        val shader = shaderCache.getOrPut(engine.engineKey) {
            RuntimeShader(engine.agslSource)
        }

        val bgSize = bgCoords.size
        val loX = (padPx - relOffset.x).coerceIn(1f, (recW - 2).toFloat())
        val loY = (padPx - relOffset.y).coerceIn(1f, (recH - 2).toFloat())
        val hiX = (padPx - relOffset.x + bgSize.width).coerceIn(loX + 1f, (recW - 1).toFloat())
        val hiY = (padPx - relOffset.y + bgSize.height).coerceIn(loY + 1f, (recH - 1).toFloat())

        val tx = touchOffsetPx?.x ?: (w * 0.5f)
        val ty = touchOffsetPx?.y ?: (h * 0.5f)

        val drawCtx = GlassDrawContext(
            widthPx = w,
            heightPx = h,
            padPx = padPx,
            density = density,
            cornerRadiiPx = floatArrayOf(radiusPx, radiusPx, radiusPx, radiusPx),
            parameters = safeParams,
            touchX = tx,
            touchY = ty,
            pressAmount = pressAmount,
            shapes = extraShapes,
            wideStripY = wideStripY,
            wideScale = wideScale,
            backdropBoundsLeft = loX,
            backdropBoundsTop = loY,
            backdropBoundsRight = hiX,
            backdropBoundsBottom = hiY,
            fallbackR = backdrop.fallbackColor.red,
            fallbackG = backdrop.fallbackColor.green,
            fallbackB = backdrop.fallbackColor.blue
        )

        if (engine.usesWideBlurStrip) {
            val wideSigma = (blurPx * wideScale).coerceIn(0.6f, 36f)
            wbLayer.renderEffect = if (blurPx > 0.5f) {
                RenderEffect.createBlurEffect(wideSigma, wideSigma, Shader.TileMode.CLAMP)
                    .asComposeRenderEffect()
            } else {
                null
            }
            wbLayer.record(IntSize(wideW, wideH)) {
                drawRect(backdrop.fallbackColor)
                scale(scaleX = wideScale, scaleY = wideScale, pivot = Offset.Zero) {
                    translate(left = padPx - relOffset.x, top = padPx - relOffset.y) {
                        drawLayer(backdrop.graphicsLayer)
                    }
                }
            }
        }

        gLayer.renderEffect = engine.buildRenderEffect(shader, drawCtx).asComposeRenderEffect()
        gLayer.record(IntSize(recW, totalRecH)) {
            drawRect(backdrop.fallbackColor)
            translate(left = padPx - relOffset.x, top = padPx - relOffset.y) {
                drawLayer(backdrop.graphicsLayer)
            }
            if (engine.usesWideBlurStrip) {
                translate(left = 0f, top = wideStripY) {
                    drawLayer(wbLayer)
                }
            }
        }

        hostLayer.record(IntSize(recW, totalRecH)) {
            drawLayer(gLayer)
        }

        translate(left = -padPx, top = -padPx) {
            clipRect(
                left = 0f,
                top = 0f,
                right = recW.toFloat(),
                bottom = recH.toFloat()
            ) {
                drawLayer(hostLayer)
            }
        }

        // Render foreground text/icons above the refracted liquid glass surface
        drawContent()
    }
}
