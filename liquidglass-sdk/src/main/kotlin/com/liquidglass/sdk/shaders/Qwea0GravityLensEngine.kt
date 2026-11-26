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
 * Engine 8: QWEA0 / Liquid-Glass-Android — Inverse-Power Gravity Lens & Absorbing Medium (MIT)
 *
 * Faithful AGSL implementation of `QWEA0/Liquid-Glass-Android` (`GlassLensRenderer.kt`):
 * - **Gravitational-lens inverse-power bevel profile**:
 *   `slope = (pow(1 + 4*t, -p) - pow(5, -p)) / (1 - pow(5, -p))`
 *   concentrating sharp refraction tightly along the outer pixels with a soft interior magnification tail
 * - Switchable inward (`-1`, default) vs outward (`+1`) refraction direction
 * - **3-tap normal-aligned `rimSoft` smear** preventing hard boundary step artifacts
 * - **Nonlinear highlight-protected vibrancy curve** (boosts desaturated colors without blowing out highlights)
 * - **Absorbing + scattering colored glass medium** (`absorbed + scattered`) preserving backdrop contrast
 * - **Symmetric dual-lobe `pow(facing, 4.5)` hairline + inward glow tail** (`pow(..., 1.5)`)
 */
class Qwea0GravityLensEngine : LiquidGlassEngine {
    override val id = GlassEngineId.QWEA0_GRAVITY
    override val displayName = "QWEA0 — Gravity Lens & Absorbing Glass"
    override val shortName = "QWEA0 Lens"
    override val author = "QWEA0 / pandadog"
    override val sourceRepoUrl = "https://github.com/QWEA0/Liquid-Glass-Android"
    override val license = "MIT"
    override val calibrationBasis = "Native Android AGSL single-pass lens with gravitational-lens inverse-power rim profile, 3-tap rimSoft smear, and dual-lobe iOS 26 Control Center rim angle fit"
    override val opticalSummary = "Inverse-power gravitational bevel profile, switchable inward/outward direction, 3-tap rimSoft smear, highlight-safe vibrancy, absorbing+scattering colored tint, pow(4.5) dual-lobe rim."
    override val portingNotes = "Native Android AGSL preserved directly from GlassLensRenderer.kt; extended from 2-shape smin to 6-shape merge group and integrated into Compose GraphicsLayer pipeline."
    override val capabilities = GlassCapabilities.FullLiquidGlass.copy(supportsDirectionToggle = true)
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
            glassThicknessDp = if (isClear) 24f else 22f,
            edgeCurvature = 1.0f, // Maps to inverse-power exponent p = 2.0
            lensDistortion = 0.10f,
            blurRadiusDp = if (isClear) 1.5f else 10.0f,
            transparency = if (isClear) 0.88f else 0.62f,
            tintColorArgb = 0xFFFFFFFFL,
            tintOpacity = if (isClear) 0.05f else 0.16f,
            chromaticAberration = if (isClear) 0.40f else 0.28f,
            specularIntensity = 1.0f,
            lightAngleDegrees = 225f,
            edgeIllumination = 1.0f,
            shadowIntensity = 0.80f,
            cornerRadiusDp = 28f,
            touchDeformation = 1.0f,
            animationSpeed = 1.0f,
            morphSmoothingDp = 28f,
            outwardSamplingOverride = false // QWEA0 defaults to inward (-1)
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun bindUniforms(shader: RuntimeShader, ctx: GlassDrawContext) {
        CommonAgsl.bindCommonUniforms(shader, ctx)
        val p = ctx.parameters.sanitized()
        val isClear = p.variant == GlassVariant.CLEAR
        val bevelPx = (p.glassThicknessDp * ctx.density).coerceAtLeast(3f)
        val refractPx = bevelPx * 1.15f * p.refractionStrength
        val falloff = (p.edgeCurvature * 2.0f).coerceIn(0f, 5.0f)
        val outward = p.outwardSamplingOverride ?: false

        shader.setFloatUniform("uBevel", bevelPx)
        shader.setFloatUniform("uRefractPx", refractPx)
        shader.setFloatUniform("uFalloff", falloff)
        shader.setFloatUniform("uRefractDir", if (outward) 1f else -1f)
        shader.setFloatUniform("uRimSoft", 1.25f * ctx.density)
        shader.setFloatUniform("uLensZoom", p.lensDistortion)
        shader.setFloatUniform("uDispersion", p.chromaticAberration * 0.24f)
        shader.setFloatUniform("uLight", ctx.lightDirX, ctx.lightDirY)
        shader.setFloatUniform("uSpecStrength", p.specularIntensity)
        shader.setFloatUniform("uEdgeIllum", p.edgeIllumination)
        shader.setFloatUniform("uTransparency", p.transparency)
        shader.setFloatUniform("uGlassTint", p.tintRed(), p.tintGreen(), p.tintBlue(), p.tintOpacity)
        shader.setFloatUniform("uSatFactor", if (isClear) 1.15f else 1.45f)
        shader.setFloatUniform("uIsClear", if (isClear) 1f else 0f)
        shader.setFloatUniform("uDarkMode", p.darkModeUniform())
    }

    companion object {
        val SHADER_SOURCE: String = """
${CommonAgsl.COMMON_UNIFORMS}
uniform float uBevel;
uniform float uRefractPx;
uniform float uFalloff;
uniform float uRefractDir;
uniform float uRimSoft;
uniform float uLensZoom;
uniform float uDispersion;
uniform float2 uLight;
uniform float uSpecStrength;
uniform float uEdgeIllum;
uniform float uTransparency;
uniform float4 uGlassTint;
uniform float uSatFactor;
uniform float uIsClear;
uniform float uDarkMode;

${CommonAgsl.COMMON_GEOMETRY}

half4 main(float2 coord) {
    float2 local = coord - float2(uPad);
    float2 halfSize = uSize * 0.5;

    float d = sceneSdRound(local, halfSize);
    float cov = clamp(0.5 - d / 1.5, 0.0, 1.0);
    if (cov <= 0.004) {
        float dSh = sceneSdRound(local - float2(0.0, 5.0 * uDensity), halfSize);
        return dropShadowOutside(dSh, uShadow, uDensity);
    }

    float2 n = sceneNormalRound(local, halfSize, 1.0);
    float t = clamp(-d / max(uBevel, 1.0), 0.0, 1.0);
    float edge = 1.0 - t;
    float slope;
    if (uFalloff > 0.01) {
        // Exact QWEA0 gravitational-lens inverse-power attenuation
        float gB = pow(5.0, -uFalloff);
        slope = (pow(1.0 + 4.0 * t, -uFalloff) - gB) / max(1.0 - gB, 1e-4);
    } else {
        slope = edge * edge;
    }

    float press = clamp(uPress, 0.0, 2.0);
    float refr = uRefractPx * (1.0 + 0.55 * press);
    float2 offset = n * (uRefractDir * slope * refr) - (local - halfSize) * (uLensZoom * 0.12);

    // Local liquid touch bulge
    if (press > 0.001) {
        float2 tp = local - uTouch;
        float tr = length(tp);
        float sigma = max(max(halfSize.x, halfSize.y), 1.0);
        float bump = press * exp(-(tr * tr) / (sigma * sigma * 0.30));
        if (tr > 1.0) {
            offset -= (tp / tr) * (bump * refr * 0.45);
        }
    }

    float2 cR = coord + offset * (1.0 - uDispersion * slope);
    float2 cG = coord + offset;
    float2 cB = coord + offset * (1.0 + uDispersion * slope);

    // 3-tap normal-aligned rimSoft smear (exact QWEA0 GlassLensRenderer.kt)
    float3 col;
    if (uRimSoft > 0.01 && slope > 0.005) {
        float2 sm = n * (uRimSoft * slope);
        col = float3(
            (sampleBackdropRgb(cR).r + sampleBackdropRgb(cR - sm).r + sampleBackdropRgb(cR + sm).r) / 3.0,
            (sampleBackdropRgb(cG).g + sampleBackdropRgb(cG - sm).g + sampleBackdropRgb(cG + sm).g) / 3.0,
            (sampleBackdropRgb(cB).b + sampleBackdropRgb(cB - sm).b + sampleBackdropRgb(cB + sm).b) / 3.0
        );
    } else {
        col = float3(
            sampleBackdropRgb(cR).r,
            sampleBackdropRgb(cG).g,
            sampleBackdropRgb(cB).b
        );
    }

    // Nonlinear highlight-protected vibrancy curve (exact QWEA0 GlassRuntimeEffects / GlassLensRenderer)
    float lum = luma709(col);
    if (uSatFactor <= 1.0) {
        col = mix(float3(lum), col, uSatFactor);
    } else {
        float satNow = max(col.r, max(col.g, col.b)) - min(col.r, min(col.g, col.b));
        float room = 1.0 - smoothstep(0.2, 0.85, satNow);
        float hl = 1.0 - smoothstep(0.75, 0.98, lum);
        float amount = 1.0 + (uSatFactor - 1.0) * mix(0.3, 1.0, room * hl);
        col = clamp(mix(float3(lum), col, amount), float3(0.0), float3(1.0));
    }

    // Adaptive luminance tint (Regular) vs clear dimming veil
    float lumT = luma709(col);
    float darkFactor = (uDarkMode < -0.5) ? smoothstep(0.35, 0.75, lumT) : (1.0 - clamp(uDarkMode, 0.0, 1.0));
    float adaptiveAlpha = (0.14 + 0.08 * darkFactor) * (1.0 - uTransparency * 0.45) * (1.0 - uIsClear * 0.80);
    col = mix(col, float3(1.0 - darkFactor), clamp(adaptiveAlpha, 0.0, 0.65));

    // Absorbing + scattering colored glass medium (exact QWEA0 model)
    if (uGlassTint.a > 0.002) {
        float lumTint = luma709(col);
        float3 absorbed = col * mix(float3(1.0), uGlassTint.rgb, 0.85);
        float3 scattered = uGlassTint.rgb * (0.38 * (1.0 - lumTint));
        col = mix(col, clamp(absorbed + scattered, float3(0.0), float3(1.0)), uGlassTint.a * mix(1.0, 0.35, uIsClear));
    }

    // Symmetric dual-lobe pow(facing, 4.5) hairline + inward glow tail
    float facing = dot(n, -uLight);
    float lobeF = pow(max(facing, 1e-5), 4.5);
    float lobeB = pow(max(-facing, 1e-5), 4.5);

    float bandW = clamp(uBevel * 0.30, 2.0, 8.0 * uDensity);
    float glowIn = clamp((-d - 1.0 * uDensity) / max(2.0 * uDensity, 1.0), 0.0, 1.0);
    float glow = glowIn * pow(clamp(1.0 - (-d - 3.0 * uDensity) / max(bandW, 1.0), 0.0, 1.0), 1.5) * cov;
    float hair = clamp(1.0 - abs(d + 1.0 * uDensity) / max(2.0 * uDensity, 1.0), 0.0, 1.0) * cov;
    float spec = (hair * 0.72 * (lobeF + lobeB) * uSpecStrength + glow * 0.14 * lobeF * uEdgeIllum)
        * (1.0 - 0.25 * clamp(press, 0.0, 1.0));

    float innerSh = smoothstep(5.0 * uDensity, 0.0, -d) * (1.0 - hair) * 0.045 * uShadow;
    col = clamp(col + float3(spec + press * 0.04) - float3(innerSh), float3(0.0), float3(1.0));
    return half4(half3(col * cov), half(cov));
}
""".trimIndent()
    }
}
