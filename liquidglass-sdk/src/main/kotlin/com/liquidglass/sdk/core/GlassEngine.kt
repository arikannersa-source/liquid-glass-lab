package com.liquidglass.sdk.core

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Immutable

/**
 * Single secondary shape participating in a liquid-glass smooth-minimum (`smin`) merge group.
 * Coordinates are in local panel pixels relative to the primary shape's top-left corner.
 */
@Immutable
data class GlassSubShape(
    val centerX: Float,
    val centerY: Float,
    val halfWidth: Float,
    val halfHeight: Float,
    val cornerRadius: Float
)

/**
 * Per-frame geometric, optical, and interaction state passed to a [LiquidGlassEngine]
 * when configuring its AGSL [RuntimeShader] and [RenderEffect].
 */
data class GlassDrawContext(
    val widthPx: Float,
    val heightPx: Float,
    val padPx: Float,
    val density: Float,
    val cornerRadiiPx: FloatArray, // TL, TR, BR, BL
    val parameters: GlassParameters,
    val touchX: Float = widthPx * 0.5f,
    val touchY: Float = heightPx * 0.5f,
    val pressAmount: Float = 0f,
    val shapes: List<GlassSubShape> = emptyList(),
    val wideStripY: Float = 0f,
    val wideScale: Float = 0.25f,
    val backdropBoundsLeft: Float = 0f,
    val backdropBoundsTop: Float = 0f,
    val backdropBoundsRight: Float = widthPx + 2f * padPx,
    val backdropBoundsBottom: Float = heightPx + 2f * padPx,
    val fallbackR: Float = 0.1f,
    val fallbackG: Float = 0.1f,
    val fallbackB: Float = 0.12f
) {
    val lightRad: Float get() = Math.toRadians(parameters.lightAngleDegrees.toDouble()).toFloat()
    val lightDirX: Float get() = kotlin.math.cos(lightRad)
    val lightDirY: Float get() = kotlin.math.sin(lightRad)
}

/**
 * Metadata and runtime contract for a pluggable Liquid Glass rendering engine.
 *
 * Every engine is completely self-contained and decoupled from the Laboratory UI so any
 * engine can be extracted directly as a standalone library or replaced later.
 */
interface LiquidGlassEngine {
    val id: GlassEngineId
    val engineKey: String get() = id.key
    val displayName: String
    val shortName: String
    val author: String
    val sourceRepoUrl: String
    val license: String
    val calibrationBasis: String
    val opticalSummary: String
    val portingNotes: String
    val capabilities: GlassCapabilities

    /** Raw AGSL shader source string (matches `shaders/<engineKey>.agsl`). */
    val agslSource: String

    /**
     * When true, the host chains a hardware Gaussian [RenderEffect.createBlurEffect] BEFORE
     * the AGSL shader runs. When false, the AGSL shader receives the sharp backdrop directly
     * (displacement-first architecture).
     */
    val usesPreBlur: Boolean

    /**
     * When true, the host also records a quarter-resolution wide-blurred copy of the backdrop
     * into a strip below the sharp backdrop inside the same GraphicsLayer (zero Bitmap readback).
     */
    val usesWideBlurStrip: Boolean get() = false

    /**
     * Returns the developer's intended default parameters for this engine so "Reset to Original"
     * always restores authentic upstream optics.
     */
    fun defaultParameters(
        variant: GlassVariant = GlassVariant.REGULAR,
        appearance: GlassAppearance = GlassAppearance.ADAPTIVE
    ): GlassParameters

    /**
     * Binds all uniforms required by [agslSource]. Must only reference uniforms declared
     * in [agslSource].
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun bindUniforms(shader: RuntimeShader, ctx: GlassDrawContext)

    /**
     * Builds the Android [RenderEffect] for the given [RuntimeShader] and [GlassDrawContext].
     * Default implementation chains pre-blur when [usesPreBlur] is true and `blurRadiusDp > 0.25`.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun buildRenderEffect(shader: RuntimeShader, ctx: GlassDrawContext): RenderEffect {
        bindUniforms(shader, ctx)
        val shaderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content")
        val blurPx = (ctx.parameters.blurRadiusDp * ctx.density).coerceIn(0f, 120f)
        return if (usesPreBlur && blurPx > 0.5f) {
            val blurEffect = RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP)
            RenderEffect.createChainEffect(shaderEffect, blurEffect)
        } else {
            shaderEffect
        }
    }
}
