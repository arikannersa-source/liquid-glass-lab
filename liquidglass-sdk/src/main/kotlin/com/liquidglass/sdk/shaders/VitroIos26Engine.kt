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
 * Engine 3: itshedimisawi / Vitro — iPhone iOS 26 Calibrated (Apache-2.0)
 *
 * Faithful AGSL implementation of `itshedimisawi/vitro`:
 * - Calibrated side-by-side against a real iPhone running iOS 26 (`Optics.kt` constants:
 *   `LightVeil=1.32, LightFade=1.34, LightWash=0.58, DarkGain=0.08, DarkVivid=0.18,
 *    DarkLift=0.19*(1-l)^16, DarkShade=1.0*max(l-0.33,0)^1.5, LightRim=0.91,
 *    DarkRimGain=0.35, DarkRimLift=0.12, LightRimVivid=0.20, DarkRimVivid=0.58, RimFloor=0.11`)
 * - Cubic bevel inward refraction (`-dir * depth * slope^3`) + interior magnification zoom
 * - 3-channel RGB edge dispersion
 * - Multi-shape smooth union (`smin`)
 * - Vibrant rim recoloring (boosts underlying color vividness & brightness instead of painting milky white)
 */
class VitroIos26Engine : LiquidGlassEngine {
    override val id = GlassEngineId.VITRO_IOS26
    override val displayName = "Vitro — iPhone iOS 26 Calibrated"
    override val shortName = "Vitro iOS 26"
    override val author = "itshedimisawi (vitro)"
    override val sourceRepoUrl = "https://github.com/itshedimisawi/vitro"
    override val license = "Apache-2.0"
    override val calibrationBasis = "Every default constant tuned side-by-side against screen captures of a physical iPhone running iOS 26 (light/dark veils, dark bend curve, vibrant rim recoloring)"
    override val opticalSummary = "Cubic bevel inward refraction + zoom, iPhone-calibrated light wash and dark knee/lift curves, vibrant chroma-amplifying rim highlights, multi-shape smin."
    override val portingNotes = "Native Android AGSL preserved directly from GlassShaders.kt & Optics.kt; adapted to shared live GraphicsLayer backdrop + laboratory interactive uniform controls."
    override val capabilities = GlassCapabilities.FullLiquidGlass
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
            refractionStrength = 1.0f,
            glassThicknessDp = if (isClear) 22f else 20f,
            edgeCurvature = 1.0f, // Cubic slope^3
            lensDistortion = if (isClear) 0.14f else 0.08f, // Magnification zoom
            blurRadiusDp = if (isClear) 1.5f else 8.0f,
            transparency = if (isClear) 0.88f else 0.58f,
            tintColorArgb = 0xFFFFFFFFL,
            tintOpacity = if (isClear) 0.04f else 0.10f,
            chromaticAberration = if (isClear) 0.35f else 0.25f,
            specularIntensity = 1.0f,
            lightAngleDegrees = 225f, // Top-left light
            edgeIllumination = 1.0f,
            shadowIntensity = 0.85f,
            cornerRadiusDp = 28f,
            touchDeformation = 1.0f,
            animationSpeed = 1.0f,
            morphSmoothingDp = 28f
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun bindUniforms(shader: RuntimeShader, ctx: GlassDrawContext) {
        CommonAgsl.bindCommonUniforms(shader, ctx)
        val p = ctx.parameters.sanitized()
        val isClear = p.variant == GlassVariant.CLEAR
        val bevelPx = (p.glassThicknessDp * ctx.density).coerceAtLeast(2f)
        val depthPx = bevelPx * 0.95f * p.refractionStrength
        val zoom = (1.0f - p.lensDistortion * 0.18f).coerceIn(0.65f, 1.25f)
        val veil = ((1.0f - p.transparency) * 0.65f * if (isClear) 0.18f else 1.0f).coerceIn(0f, 1f)

        shader.setFloatUniform("uBevel", bevelPx)
        shader.setFloatUniform("uDepth", depthPx)
        shader.setFloatUniform("uCurvature", p.edgeCurvature)
        shader.setFloatUniform("uDispersion", p.chromaticAberration * 0.16f)
        shader.setFloatUniform("uZoom", zoom)
        shader.setFloatUniform("uSaturation", if (isClear) 1.15f else 1.32f)
        shader.setFloatUniform("uBrightness", veil)
        shader.setFloatUniform("uLight", ctx.lightDirX, ctx.lightDirY)
        shader.setFloatUniform("uHighlight", p.specularIntensity * p.edgeIllumination)
        shader.setFloatUniform("uTint", p.tintRed(), p.tintGreen(), p.tintBlue(), p.tintOpacity * 0.85f)
        shader.setFloatUniform("uDimming", if (isClear) 0.08f else 0.0f)
        shader.setFloatUniform("uDarkMode", p.darkModeUniform())
    }

    companion object {
        // Exact iPhone-calibrated constants from itshedimisawi/vitro Optics.kt
        val SHADER_SOURCE: String = """
${CommonAgsl.COMMON_UNIFORMS}
uniform float uBevel;
uniform float uDepth;
uniform float uCurvature;
uniform float uDispersion;
uniform float uZoom;
uniform float uSaturation;
uniform float uBrightness;
uniform float2 uLight;
uniform float uHighlight;
uniform float4 uTint;
uniform float uDimming;
uniform float uDarkMode;

${CommonAgsl.COMMON_GEOMETRY}

half4 main(float2 coord) {
    float2 local = coord - float2(uPad);
    float2 halfSize = uSize * 0.5;
    float2 pivot = halfSize + float2(uPad);

    float sd = sceneSdRound(local, halfSize);
    float cover = clamp(0.5 - sd, 0.0, 1.0);
    if (cover <= 0.0) {
        float dSh = sceneSdRound(local - float2(0.0, 5.0 * uDensity), halfSize);
        return dropShadowOutside(dSh, uShadow, uDensity);
    }

    float2 dir = sceneNormalRound(local, halfSize, 1.25);
    float inset = max(-sd, 0.0);
    float slope = 1.0 - clamp(inset / max(uBevel, 1.0), 0.0, 1.0);
    float expPow = clamp(1.5 + 1.5 * uCurvature, 1.2, 5.0);
    float2 shift = -dir * (uDepth * pow(max(slope, 1e-5), expPow));

    // Touch spotlight + gentle liquid swell
    float2 touchDelta = local - uTouch;
    float spot = exp(-dot(touchDelta, touchDelta) / max(halfSize.y * halfSize.y * 6.0, 1.0));
    float touchAmt = clamp(uPress, 0.0, 2.0);
    shift -= touchDelta * (touchAmt * 0.12 * spot);

    float2 p = coord - pivot;
    float fringe = 2.0 * uDispersion;
    float3 color;
    if (fringe < 0.001 || slope < 0.001) {
        color = sampleBackdropRgb(pivot + (p + shift) * uZoom);
    } else {
        color = float3(
            sampleBackdropRgb(pivot + (p + shift * (1.0 - fringe)) * uZoom).r,
            sampleBackdropRgb(pivot + (p + shift) * uZoom).g,
            sampleBackdropRgb(pivot + (p + shift * (1.0 + fringe)) * uZoom).b
        );
    }

    // Saturation
    float3 weights = float3(0.2126, 0.7152, 0.0722);
    color = max(mix(float3(dot(color, weights)), color, uSaturation), float3(0.0));

    float facing = dot(dir, -uLight);
    float rim = 1.0 - smoothstep(0.75 * uDensity, 1.45 * uDensity, inset);
    float edge = rim * (0.11 + (1.0 - 0.11) * abs(facing)) * uHighlight;

    float veil = uBrightness;
    float luma = min(dot(color, weights), 1.0);
    float dark = (uDarkMode < -0.5) ? smoothstep(0.38, 0.62, 1.0 - luma) : clamp(uDarkMode, 0.0, 1.0);

    // Exact Vitro iPhone-calibrated pale (light) and deep (dark) glass transfer curves
    float3 pale = mix(color, float3(luma), 0.58 * veil) + float3(veil * (1.32 - 1.34 * luma));
    float lift = 0.19 * pow(max(1.0 - luma, 1e-5), 16.0);
    float shade = 1.0 * pow(max(luma - 0.33, 1e-5), 1.5);
    float3 vivid = max(mix(float3(luma), color, 1.0 + 0.18 * veil), float3(0.0));
    float3 deep = max(vivid * (1.0 + 0.08 * veil) + float3(veil * (lift - shade)), float3(0.0));

    // Exact Vitro vibrant rim recoloring (brightens & intensifies chroma instead of washing out)
    float3 paleSeen = min(pale, float3(1.0));
    float paleLit = clamp(dot(paleSeen, weights), 0.02, 1.0);
    float3 paleVivid = max(mix(float3(paleLit), paleSeen, 1.0 + 0.20 * edge), float3(0.0));
    pale += paleVivid * (1.0 + 0.91 * edge * (1.0 - paleLit) / paleLit) - paleSeen;

    float3 deepSeen = min(deep, float3(1.0));
    float deepLit = clamp(dot(deepSeen, weights), 0.02, 1.0);
    float3 deepVivid = max(mix(float3(deepLit), deepSeen, 1.0 + 0.58 * edge), float3(0.0));
    deep += max(deepVivid * (1.0 + edge * (0.35 + 0.12 / deepLit)) - deepSeen, float3(0.0));

    color = mix(pale, deep, dark);
    color = mix(color, uTint.rgb, clamp(uTint.a, 0.0, 0.90));
    color *= (1.0 - uDimming);

    // Subtle inner shadow and touch glow
    float innerSh = smoothstep(5.0 * uDensity, 0.0, inset) * (1.0 - rim);
    color -= float3(innerSh * 0.045 * uShadow);
    color = clamp(color + float3(touchAmt * 0.05 * (0.7 + 0.3 * spot)), float3(0.0), float3(1.0));

    return half4(half3(color * cover), half(cover));
}
""".trimIndent()
    }
}
