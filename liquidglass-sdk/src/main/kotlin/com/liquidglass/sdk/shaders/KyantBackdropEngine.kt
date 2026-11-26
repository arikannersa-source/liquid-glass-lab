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
 * Engine 2: Kyant0 / AndroidLiquidGlass (Apache-2.0)
 *
 * Faithful AGSL implementation of `Kyant0/AndroidLiquidGlass`:
 * - Pre-blur + vibrancy + rounded-rect SDF lens
 * - Exact `circleMap(x) = 1.0 - sqrt(1.0 - x * x)` inward refraction
 * - Inflated corner normal radius `min(radius * 1.5, min(halfSize.x, halfSize.y))`
 * - Radial 3D dome `depthEffect * normalize(centeredCoord)`
 * - Exact 7-band spectral dispersion (Red/Orange/Yellow/Green/Cyan/Blue/Purple)
 *   weighted by `/3.5, /7.0, /3.0` and corner quadrant `(centeredCoord.x * centeredCoord.y) / (halfSize.x * halfSize.y)`
 * - Dual `DefaultHighlight` + `AmbientHighlight` directional rim illumination
 */
class KyantBackdropEngine : LiquidGlassEngine {
    override val id = GlassEngineId.KYANT_BACKDROP
    override val displayName = "Kyant0 — AndroidLiquidGlass"
    override val shortName = "Kyant0"
    override val author = "Kyant0"
    override val sourceRepoUrl = "https://github.com/Kyant0/AndroidLiquidGlass"
    override val license = "Apache-2.0"
    override val calibrationBasis = "Visual engineering for Jetpack Compose Multiplatform (4,800+ GitHub stars; 7-band spectral dispersion + inflated-radius rounded-rect SDF)"
    override val opticalSummary = "Pre-blurred backdrop + circleMap inward refraction, 1.5x inflated corner normal smoothing, radial depthEffect, 7-band spectral dispersion, dual Default+Ambient highlights."
    override val portingNotes = "Fused Kyant0's RoundedRectRefractionWithDispersion + DefaultHighlight + AmbientHighlight + vibrancy into one AGSL pass over pre-blurred RenderEffect; extended SDF with multi-shape smin morphing."
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
            refractionStrength = if (isClear) 1.20f else 1.10f,
            glassThicknessDp = if (isClear) 24f else 22f,
            edgeCurvature = 1.0f,
            lensDistortion = 0.22f, // Kyant0's depthEffect radial dome
            blurRadiusDp = if (isClear) 1.0f else 4.5f, // Low pre-blur so rim refraction stays visible
            transparency = if (isClear) 0.92f else 0.78f,
            tintColorArgb = if (appearance == GlassAppearance.DARK) 0xFF161820L else 0xFFFFFFFFL,
            tintOpacity = when {
                isClear -> 0.03f
                appearance == GlassAppearance.ADAPTIVE -> 0.05f
                else -> 0.14f
            },
            chromaticAberration = if (isClear) 0.45f else 0.35f,
            specularIntensity = 1.05f,
            lightAngleDegrees = 225f, // Top-left 45-degree specular axis (Kyant0 default)
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
        val refrHeightPx = (p.glassThicknessDp * ctx.density).coerceAtLeast(2f)
        // Kyant0 negates refractionAmount at host so outward gradient samples inward
        val refrAmountPx = -refrHeightPx * 1.15f * p.refractionStrength

        shader.setFloatUniform("uRefractionHeight", refrHeightPx)
        shader.setFloatUniform("uRefractionAmount", refrAmountPx)
        shader.setFloatUniform("uDepthEffect", p.lensDistortion)
        shader.setFloatUniform("uCurvature", p.edgeCurvature)
        shader.setFloatUniform("uChromaticAberration", p.chromaticAberration)
        shader.setFloatUniform("uTransparency", p.transparency)
        shader.setFloatUniform("uTint", p.tintRed(), p.tintGreen(), p.tintBlue(), p.tintOpacity)
        shader.setFloatUniform("uLightAngle", ctx.lightRad)
        shader.setFloatUniform("uSpecular", p.specularIntensity)
        shader.setFloatUniform("uEdgeIllum", p.edgeIllumination)
        shader.setFloatUniform("uIsClear", if (isClear) 1f else 0f)
        shader.setFloatUniform("uDarkMode", p.darkModeUniform())
    }

    companion object {
        val SHADER_SOURCE: String = """
${CommonAgsl.COMMON_UNIFORMS}
uniform float uRefractionHeight;
uniform float uRefractionAmount;
uniform float uDepthEffect;
uniform float uCurvature;
uniform float uChromaticAberration;
uniform float uTransparency;
uniform float4 uTint;
uniform float uLightAngle;
uniform float uSpecular;
uniform float uEdgeIllum;
uniform float uIsClear;
uniform float uDarkMode;

${CommonAgsl.COMMON_GEOMETRY}

float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {
        float2 pos = max(cornerCoord, float2(0.0));
        float l = length(pos);
        return (l > 1e-5) ? (sign(coord) * (pos / l)) : float2(0.0, -1.0);
    } else {
        float gradX = step(cornerCoord.y, cornerCoord.x);
        return sign(coord) * float2(gradX, 1.0 - gradX);
    }
}

float circleMap(float x) {
    float c = clamp(x, 0.0, 1.0);
    float shaped = pow(max(c, 1e-5), clamp(uCurvature, 0.35, 2.2));
    return 1.0 - sqrt(max(1.0 - shaped * shaped, 0.0));
}

half4 main(float2 coord) {
    float2 local = coord - float2(uPad);
    float2 halfSize = uSize * 0.5;
    float2 centered = local - halfSize;

    float sd = sceneSdRound(local, halfSize);
    float coverage = clamp(0.5 - sd, 0.0, 1.0);
    if (coverage <= 0.0) {
        float dSh = sceneSdRound(local - float2(0.0, 5.0 * uDensity), halfSize);
        return dropShadowOutside(dSh, uShadow, uDensity);
    }

    float radius = min(radiusAt(centered, uRadii), min(halfSize.x, halfSize.y));
    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));

    // Exact Kyant0 inflated-radius gradient when single shape, smooth SDF gradient when merged
    float2 baseGrad;
    if (uExtraCount < 0.5) {
        baseGrad = gradSdRoundedRect(centered, halfSize, gradRadius);
    } else {
        baseGrad = sceneNormalRound(local, halfSize, 1.5);
    }

    float cLen = length(centered);
    float2 radialDir = (cLen > 1e-4) ? (centered / cLen) : float2(0.0);
    float2 combinedGrad = baseGrad + uDepthEffect * radialDir;
    float cgLen = length(combinedGrad);
    float2 grad = (cgLen > 1e-5) ? (combinedGrad / cgLen) : baseGrad;

    // Touch bulge
    float2 touchDelta = local - uTouch;
    float touchSigma = max(min(uSize.x, uSize.y), 1.0);
    float touchFall = exp(-dot(touchDelta, touchDelta) / (touchSigma * touchSigma)) * clamp(uPress, 0.0, 2.0);
    float2 baseCoord = coord - touchDelta * (touchFall * 0.14);

    float depth = max(-sd, 0.0);
    float3 rgb;
    if (depth >= uRefractionHeight) {
        rgb = sampleBackdropRgb(baseCoord);
    } else {
        float d = circleMap(1.0 - depth / max(uRefractionHeight, 1.0)) * uRefractionAmount * (1.0 + 0.3 * touchFall);
        float2 refractedCoord = baseCoord + d * grad;

        if (uChromaticAberration > 0.005) {
            // Exact Kyant0 7-band spectral dispersion
            float quadFactor = (centered.x * centered.y) / max(halfSize.x * halfSize.y, 1.0);
            float dispIntensity = uChromaticAberration * mix(quadFactor, sign(quadFactor + 1e-4) * 0.45, 0.25);
            float2 dispCoord = d * grad * dispIntensity;

            float3 acc = float3(0.0);
            float3 red    = sampleBackdropRgb(refractedCoord + dispCoord);
            float3 orange = sampleBackdropRgb(refractedCoord + dispCoord * (2.0 / 3.0));
            float3 yellow = sampleBackdropRgb(refractedCoord + dispCoord * (1.0 / 3.0));
            float3 green  = sampleBackdropRgb(refractedCoord);
            float3 cyan   = sampleBackdropRgb(refractedCoord - dispCoord * (1.0 / 3.0));
            float3 blue   = sampleBackdropRgb(refractedCoord - dispCoord * (2.0 / 3.0));
            float3 purple = sampleBackdropRgb(refractedCoord - dispCoord);

            acc.r = red.r / 3.5 + orange.r / 3.5 + yellow.r / 3.5 + purple.r / 7.0;
            acc.g = orange.g / 7.0 + yellow.g / 3.5 + green.g / 3.5 + cyan.g / 3.5;
            acc.b = cyan.b / 3.0 + blue.b / 3.0 + purple.b / 3.0;
            rgb = acc;
        } else {
            rgb = sampleBackdropRgb(refractedCoord);
        }
    }

    // Kyant0 vibrancy saturation boost (1.35x regular, 1.15x clear)
    float l = luma709(rgb);
    float vib = mix(1.38, 1.12, uIsClear);
    rgb = clamp(mix(float3(l), rgb, vib), float3(0.0), float3(1.0));

    float darkFactor = (uDarkMode < -0.5) ? smoothstep(0.45, 0.65, 1.0 - l) : clamp(uDarkMode, 0.0, 1.0);
    float3 surfaceTone = mix(float3(0.96, 0.96, 0.98), float3(0.10, 0.10, 0.13), darkFactor);
    float surfaceAlpha = (1.0 - uTransparency) * mix(0.42, 0.10, uIsClear);
    rgb = mix(rgb, surfaceTone, clamp(surfaceAlpha, 0.0, 0.72));
    rgb = mix(rgb, uTint.rgb, clamp(uTint.a * mix(1.0, 0.35, uIsClear), 0.0, 0.88));

    // Directional specular bevel + opposite-rim ambient highlight (Kyant0 SdfShader.kt formula)
    float rimDepth = clamp(1.0 - depth / max(uRefractionHeight, 1.0), 0.0, 1.0);
    float rimStroke = smoothstep(3.0 * uDensity, 0.0, depth);
    float2 lightVec = float2(cos(uLightAngle), sin(uLightAngle));
    float dDot = dot(baseGrad, lightVec);
    float keyBevel = clamp(dDot, 0.0, 1.0);
    float defaultHi = pow(keyBevel, 2.0) * max(rimStroke, rimDepth * 0.75) * 0.65 * uSpecular;

    float oppBevel = clamp(-dDot, 0.0, 1.0);
    float ambientHi = oppBevel * smoothstep(1.0, 0.0, abs(rimDepth - 0.25) * 4.0) * 0.30 * uEdgeIllum;

    rgb += float3(defaultHi + ambientHi);

    // Inner shadow subtle thickness cue
    float innerSh = smoothstep(5.5 * uDensity, 0.0, depth) * (1.0 - rimStroke);
    rgb -= float3(innerSh * 0.05 * uShadow);
    rgb += float3(touchFall * 0.06);

    rgb = clamp(rgb, float3(0.0), float3(1.0));
    return half4(half3(rgb * coverage), half(coverage));
}
""".trimIndent()
    }
}
