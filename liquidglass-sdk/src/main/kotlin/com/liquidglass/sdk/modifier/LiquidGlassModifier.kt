package com.liquidglass.sdk.modifier

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
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
 * Uses an offscreen compositing wrapper layer so Android HWUI's viewport scissor never
 * clips padded outward sampling or wide-kernel blur strips — even for edge-hugging top
 * search bars or floating bottom navigation bars.
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
): Modifier = composed {
    val glassLayer = rememberGraphicsLayer()
    val wideBlurLayer = rememberGraphicsLayer()
    val offscreenHostLayer = rememberGraphicsLayer().apply {
        compositingStrategy = CompositingStrategy.Offscreen
    }
    var ownCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    // Cache compiled RuntimeShader per engine key so switching engines is instantaneous.
    val shaderCache = remember { HashMap<String, RuntimeShader>() }

    this
        .onGloballyPositioned { ownCoords = it }
        .drawBehind {
            // Read contentVersion in draw phase only so animated/scrolling backdrops trigger redraws without recomposition.
            @Suppress("UNUSED_VARIABLE")
            val version = backdrop.contentVersion

            val w = size.width
            val h = size.height
            if (w < 2f || h < 2f) return@drawBehind
            val myCoords = ownCoords ?: return@drawBehind
            val bgCoords = backdrop.coordinates ?: return@drawBehind
            if (!myCoords.isAttached || !bgCoords.isAttached) return@drawBehind

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

            // Compute generous sampling pad covering outward refraction, blur margin, and soft drop shadows.
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

            // Compute valid backdrop sample bounds inside recorded coordinates so refracted rays stay inside real pixels.
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

            // If engine uses a dual-kernel wide blur strip, record quarter-res hardware-blurred copy first.
            if (engine.usesWideBlurStrip) {
                val wideSigma = (blurPx * wideScale).coerceIn(0.6f, 36f)
                wideBlurLayer.renderEffect = if (blurPx > 0.5f) {
                    RenderEffect.createBlurEffect(wideSigma, wideSigma, Shader.TileMode.CLAMP)
                        .asComposeRenderEffect()
                } else {
                    null
                }
                wideBlurLayer.record(IntSize(wideW, wideH)) {
                    drawRect(backdrop.fallbackColor)
                    scale(scaleX = wideScale, scaleY = wideScale, pivot = Offset.Zero) {
                        translate(left = padPx - relOffset.x, top = padPx - relOffset.y) {
                            drawLayer(backdrop.graphicsLayer)
                        }
                    }
                }
            }

            // Record sharp backdrop (and optional wide blur strip below it) into glassLayer.
            glassLayer.renderEffect = engine.buildRenderEffect(shader, drawCtx).asComposeRenderEffect()
            glassLayer.record(IntSize(recW, totalRecH)) {
                drawRect(backdrop.fallbackColor)
                translate(left = padPx - relOffset.x, top = padPx - relOffset.y) {
                    drawLayer(backdrop.graphicsLayer)
                }
                if (engine.usesWideBlurStrip) {
                    translate(left = 0f, top = wideStripY) {
                        drawLayer(wideBlurLayer)
                    }
                }
            }

            // Draw through an offscreen host layer sized to the full recorded bounds so HWUI's
            // viewport scissor never truncates padded refraction samples or wide-blur strips.
            offscreenHostLayer.record(IntSize(recW, totalRecH)) {
                drawLayer(glassLayer)
            }

            translate(left = -padPx, top = -padPx) {
                clipRect(
                    left = 0f,
                    top = 0f,
                    right = recW.toFloat(),
                    bottom = recH.toFloat()
                ) {
                    drawLayer(offscreenHostLayer)
                }
            }
        }
}
