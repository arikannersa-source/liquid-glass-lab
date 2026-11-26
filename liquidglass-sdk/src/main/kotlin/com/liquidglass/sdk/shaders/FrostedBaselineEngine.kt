package com.liquidglass.sdk.shaders

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import com.liquidglass.sdk.core.GlassAppearance
import com.liquidglass.sdk.core.GlassCapabilities
import com.liquidglass.sdk.core.GlassDrawContext
import com.liquidglass.sdk.core.GlassEngineId
import com.liquidglass.sdk.core.GlassParameters
import com.liquidglass.sdk.core.GlassVariant
import com.liquidglass.sdk.core.LiquidGlassEngine

/**
 * Engine 9: Conventional Frosted Glass Baseline (Glassmorphism Reference)
 *
 * Intentionally implements standard 2020-era frosted glassmorphism:
 * - Uniform Gaussian blur (`RenderEffect.createBlurEffect`)
 * - Flat translucent tint veil
 * - Crisp uniform 1dp bright border stroke
 * - Drop shadow
 * - NO optical refraction, NO lens distortion, NO chromatic dispersion, NO curved specular highlights, NO liquid morphing!
 *
 * Included strictly as the Phase 2 scientific baseline so evaluators can immediately verify
 * the optical gulf between flat glassmorphism and true refractive Liquid Glass.
 */
class FrostedBaselineEngine : LiquidGlassEngine {
    override val id = GlassEngineId.FROSTED_BASELINE
    override val displayName = "Baseline — Frosted Glassmorphism"
    override val shortName = "Frosted Baseline"
    override val author = "Liquid Glass Lab (Control Baseline)"
    override val sourceRepoUrl = "https://developer.android.com/reference/android/graphics/RenderEffect"
    override val license = "Apache-2.0"
    override val calibrationBasis = "Conventional frosted-glass baseline (Gaussian blur + flat translucent veil + uniform 1dp border; zero optical lensing)"
    override val opticalSummary = "Uniform Gaussian blur + flat translucent tint + static 1dp border. Zero edge refraction, zero dispersion, zero curved highlights — control reference only."
    override val portingNotes = "Native Android RenderEffect.createBlurEffect chained into a clean rounded-rect tint/border AGSL compositor."
    override val capabilities = GlassCapabilities.FrostedOnly
    override val usesPreBlur = true

    override val agslSource: String = SHADER_SOURCE

    override fun defaultParameters(
        variant: GlassVariant,
        appearance: GlassAppearance
    ): GlassParameters {
        val isClear = variant == GlassVariant.CLEAR
        return GlassParameters(
            variant = variant,
            appearance = appearance,
            refractionStrength = 0f,
            glassThicknessDp = 0f,
            edgeCurvature = 1f,
            lensDistortion = 0f,
            blurRadiusDp = if (isClear) 4f else 18f,
            transparency = if (isClear) 0.82f else 0.55f,
            tintColorArgb = if (appearance == GlassAppearance.DARK) 0xFF1C1C20L else 0xFFFFFFFFL,
            tintOpacity = if (isClear) 0.12f else 0.34f,
            chromaticAberration = 0f,
            specularIntensity = 0f,
            lightAngleDegrees = 225f,
            edgeIllumination = 0.85f,
            shadowIntensity = 0.75f,
            cornerRadiusDp = 28f,
            touchDeformation = 0.5f,
            animationSpeed = 1.0f,
            morphSmoothingDp = 0f
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun bindUniforms(shader: RuntimeShader, ctx: GlassDrawContext) {
        CommonAgsl.bindCommonUniforms(shader, ctx)
        val p = ctx.parameters.sanitized()
        val isClear = p.variant == GlassVariant.CLEAR

        shader.setFloatUniform("uTransparency", p.transparency)
        shader.setFloatUniform("uTint", p.tintRed(), p.tintGreen(), p.tintBlue(), p.tintOpacity)
        shader.setFloatUniform("uBorderAlpha", 0.42f * p.edgeIllumination)
        shader.setFloatUniform("uIsClear", if (isClear) 1f else 0f)
        shader.setFloatUniform("uDarkMode", p.darkModeUniform())
    }

    companion object {
        val SHADER_SOURCE: String = """
${CommonAgsl.COMMON_UNIFORMS}
uniform float uTransparency;
uniform float4 uTint;
uniform float uBorderAlpha;
uniform float uIsClear;
uniform float uDarkMode;

${CommonAgsl.COMMON_GEOMETRY}

// Plain non-merging union (no smin liquid bridge in conventional glassmorphism)
float plainSd(float2 local, float2 halfSize) {
    float d = sdRoundBox4(local - halfSize, halfSize, uRadii);
    for (int i = 0; i < 5; i++) {
        if (float(i) >= uExtraCount) break;
        float4 s = uExtraShapes[i];
        d = min(d, sdRoundBox(local - s.xy, s.zw, uExtraRadii[i]));
    }
    return d;
}

half4 main(float2 coord) {
    float2 local = coord - float2(uPad);
    float2 halfSize = uSize * 0.5;

    float sd = plainSd(local, halfSize);
    float coverage = clamp(0.5 - sd, 0.0, 1.0);
    if (coverage <= 0.0) {
        float dSh = plainSd(local - float2(0.0, 4.0 * uDensity), halfSize);
        return dropShadowOutside(dSh, uShadow, uDensity);
    }

    // ZERO refraction: direct 1:1 unwarped sample of pre-blurred backdrop
    float3 rgb = sampleBackdropRgb(coord);
    float luma = luma709(rgb);
    float darkFactor = (uDarkMode < -0.5) ? smoothstep(0.45, 0.65, 1.0 - luma) : clamp(uDarkMode, 0.0, 1.0);

    float3 veilColor = mix(float3(0.96, 0.96, 0.98), float3(0.11, 0.11, 0.14), darkFactor);
    float veilAlpha = clamp((1.0 - uTransparency) * mix(0.55, 0.18, uIsClear), 0.0, 0.85);
    rgb = mix(rgb, veilColor, veilAlpha);
    rgb = mix(rgb, uTint.rgb, clamp(uTint.a * mix(1.0, 0.4, uIsClear), 0.0, 0.90));

    // Uniform 1dp flat white border (classic glassmorphism look)
    float depth = max(-sd, 0.0);
    float border = (1.0 - smoothstep(0.6 * uDensity, 1.3 * uDensity, depth)) * clamp(uBorderAlpha, 0.0, 1.0);
    rgb = mix(rgb, float3(1.0), border);
    rgb = clamp(rgb + float3(clamp(uPress, 0.0, 1.0) * 0.04), float3(0.0), float3(1.0));

    return half4(half3(rgb * coverage), half(coverage));
}
""".trimIndent()
    }
}
