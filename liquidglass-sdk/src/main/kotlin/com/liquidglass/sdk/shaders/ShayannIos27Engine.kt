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
 * Engine 1: shayann07 / Measured iOS 27 Liquid Glass (Apache-2.0)
 *
 * Faithful AGSL implementation of `shayann07/liquidglass`:
 * - Displacement-first, blur-second architecture (keeps refracted rim legible & crisp)
 * - Dual-kernel iOS 27 measured material: `lin = B_wide + k*(D_fine - B_wide) + c*|D_fine - B_wide|`
 * - Measured concave dark tone curve (`31.72 + 254.40x - 101.01x^2`) + Display P3->sRGB chroma gain
 * - Lame superellipse squircle outline (`n = 4`) + exact closed-form Snell bevel (`snellShift`)
 * - Outward sampling with padded backdrop + optional inward iOS 27 rest-fold profile
 * - Oklab perceptual highlights + Schlick Fresnel + iOS 27 outer dark contour + fingertip magnifier
 */
class ShayannIos27Engine : LiquidGlassEngine {
    override val id = GlassEngineId.SHAYANN_IOS27
    override val displayName = "shayann07 — Measured iOS 27"
    override val shortName = "iOS 27 Measured"
    override val author = "shayann07 (liquidglass)"
    override val sourceRepoUrl = "https://github.com/shayann07/liquidglass"
    override val license = "Apache-2.0"
    override val calibrationBasis = "Lossless iOS 27 calibration captures on iPhone (89 synthetic stripe/step stills + 125-point RGB color cube, 1.3–1.4 / 255 RMS)"
    override val opticalSummary = "Displacement-first Snell superellipse lens, dual-kernel wide+fine tone separation, P3->sRGB chroma boost, Oklab highlights, outer dark contour, fingertip swell."
    override val portingNotes = "Native Android AGSL preserved closely from GlassShaderSource.kt; unified multi-shape smin array (up to 6 shapes) and direct laboratory uniform exposure for all 16 controls."
    override val capabilities = GlassCapabilities.FullLiquidGlass.copy(supportsDirectionToggle = true)
    override val usesPreBlur = false
    override val usesWideBlurStrip = true

    override val agslSource: String = SHADER_SOURCE

    override fun defaultParameters(
        variant: GlassVariant,
        appearance: GlassAppearance
    ): GlassParameters {
        val isClear = variant == GlassVariant.CLEAR
        return GlassParameters(
            variant = variant,
            appearance = appearance,
            refractionStrength = if (isClear) 1.15f else 1.0f,
            glassThicknessDp = if (isClear) 24f else 26f,
            edgeCurvature = 1.0f, // Maps to bevelPower = 4.0 (squircle bevel)
            lensDistortion = if (isClear) 0.16f else 0.10f,
            blurRadiusDp = if (isClear) 1.0f else 15.5f, // iOS 27 measured wide kernel 15.5 pt
            transparency = if (isClear) 0.90f else 0.56f,
            tintColorArgb = if (appearance == GlassAppearance.DARK) 0xFF000000L else 0xFFF1F1F1L,
            tintOpacity = if (isClear) 0.08f else 0.44f,
            chromaticAberration = if (isClear) 0.22f else 0.14f,
            specularIntensity = if (isClear) 1.15f else 0.95f,
            lightAngleDegrees = 270f, // Overhead iOS 27 measured light axis
            edgeIllumination = if (isClear) 1.0f else 0.95f,
            shadowIntensity = if (isClear) 0.70f else 0.90f,
            cornerRadiusDp = 28f,
            touchDeformation = 1.0f,
            animationSpeed = 1.0f,
            morphSmoothingDp = 28f,
            outwardSamplingOverride = true
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun bindUniforms(shader: RuntimeShader, ctx: GlassDrawContext) {
        CommonAgsl.bindCommonUniforms(shader, ctx)
        val p = ctx.parameters.sanitized()
        val isClear = p.variant == GlassVariant.CLEAR
        val bandPx = (p.glassThicknessDp * ctx.density).coerceAtLeast(4f)
        val depthPx = bandPx * 0.58f * p.refractionStrength
        val bevelPower = (2.0f + p.edgeCurvature * 2.0f).coerceIn(1.8f, 6.0f)
        val cornerPower = (2.0f + p.edgeCurvature * 2.0f).coerceIn(2.0f, 5.0f)

        shader.setFloatUniform("uWideStripY", ctx.wideStripY)
        shader.setFloatUniform("uWideScale", ctx.wideScale)
        shader.setFloatUniform("uRefractBand", bandPx)
        shader.setFloatUniform("uRefractDepth", depthPx)
        shader.setFloatUniform("uIor", 1.50f)
        shader.setFloatUniform("uBevelPower", bevelPower)
        shader.setFloatUniform("uCornerPower", cornerPower)
        shader.setFloatUniform("uLensZoom", p.lensDistortion)
        shader.setFloatUniform("uBlurPx", (p.blurRadiusDp * ctx.density).coerceAtLeast(0f))
        shader.setFloatUniform("uAberration", p.chromaticAberration * 0.18f)
        shader.setFloatUniform("uTransparency", p.transparency)
        shader.setFloatUniform("uTint", p.tintRed(), p.tintGreen(), p.tintBlue(), p.tintOpacity)
        shader.setFloatUniform("uLight", ctx.lightDirX, ctx.lightDirY)
        shader.setFloatUniform("uSpecular", p.specularIntensity)
        shader.setFloatUniform("uEdgeIllum", p.edgeIllumination)
        shader.setFloatUniform("uIsClear", if (isClear) 1f else 0f)
        shader.setFloatUniform("uDarkMode", p.darkModeUniform())
        val outward = p.outwardSamplingOverride ?: true
        shader.setFloatUniform("uOutward", if (outward) 1f else -1f)
    }

    companion object {
        val SHADER_SOURCE: String = """
${CommonAgsl.COMMON_UNIFORMS}
uniform float uWideStripY;
uniform float uWideScale;
uniform float uRefractBand;
uniform float uRefractDepth;
uniform float uIor;
uniform float uBevelPower;
uniform float uCornerPower;
uniform float uLensZoom;
uniform float uBlurPx;
uniform float uAberration;
uniform float uTransparency;
uniform float4 uTint;
uniform float2 uLight;
uniform float uSpecular;
uniform float uEdgeIllum;
uniform float uIsClear;
uniform float uDarkMode;
uniform float uOutward;

${CommonAgsl.COMMON_GEOMETRY}
${CommonAgsl.OKLAB_HIGHLIGHT}

float bevelSlope(float e, float p) {
    float u = 1.0 - clamp(e, 0.002, 0.998);
    float up = pow(u, p);
    return pow(u, p - 1.0) / pow(max(1.0 - up, 1e-4), 1.0 - 1.0 / p);
}

float snellShift(float slope, float ior) {
    float inv = inversesqrt(1.0 + slope * slope);
    float si = slope * inv;
    float ci = inv;
    float st = si / max(ior, 1.0001);
    float ct = sqrt(max(1.0 - st * st, 0.0));
    return (si * ct - ci * st) / max(ci * ct + si * st, 1e-4);
}

float3 sampleWideRgb(float2 coord) {
    if (uWideStripY < 1.0) return sampleBackdropRgb(coord);
    float2 q = clamp(coord, uBackdrop.xy, uBackdrop.zw);
    float2 c = float2(q.x * uWideScale, uWideStripY + q.y * uWideScale);
    half4 s = content.eval(c);
    return (s.a > half(0.004)) ? float3(s.rgb / s.a) : sampleBackdropRgb(coord);
}

// 4-tap iOS 27 measured detail kernel (1/3 dp = 1 px at 3x)
float3 detailCross(float2 p, float r) {
    if (r < 0.25) return sampleBackdropRgb(p);
    return (sampleBackdropRgb(p + float2(r, 0.0))
          + sampleBackdropRgb(p - float2(r, 0.0))
          + sampleBackdropRgb(p + float2(0.0, r))
          + sampleBackdropRgb(p - float2(0.0, r))) * 0.25;
}

float2 tapRot(float2 coord) {
    float3 q = fract(float3(coord.x, coord.y, coord.x) * 0.1031);
    q += dot(q, q.yzx + 33.33);
    float a = fract((q.x + q.y) * q.z) * 6.2831853;
    return float2(cos(a), sin(a));
}

float2 rot2(float2 v, float2 r) {
    return float2(v.x * r.x - v.y * r.y, v.x * r.y + v.y * r.x);
}

float3 scatterFine(float2 p, float rad, float2 rot) {
    if (rad < 0.5) return sampleBackdropRgb(p);
    float r = min(rad, 3.0 * uDensity);
    float3 sum = sampleBackdropRgb(p) * 2.0;
    sum += sampleBackdropRgb(p + r * rot2(float2( 1.0,  0.0), rot));
    sum += sampleBackdropRgb(p + r * rot2(float2( 0.0,  1.0), rot));
    sum += sampleBackdropRgb(p + r * rot2(float2(-1.0,  0.0), rot));
    sum += sampleBackdropRgb(p + r * rot2(float2( 0.0, -1.0), rot));
    return sum * (1.0 / 6.0);
}

half4 main(float2 coord) {
    float2 local = coord - float2(uPad);
    float2 halfSize = uSize * 0.5;
    float2 p = local - halfSize;

    float d = sceneSdSquircle(local, halfSize, uCornerPower);
    float coverage = clamp(0.5 - d, 0.0, 1.0);
    float outerBand = clamp(1.0 - abs(d - 0.35 * uDensity) / max(0.85 * uDensity, 1.0), 0.0, 1.0);

    if (coverage <= 0.0) {
        float dSh = sceneSdSquircle(local - float2(0.0, 5.0 * uDensity), halfSize, uCornerPower);
        half4 sh = dropShadowOutside(dSh, uShadow, uDensity);
        if (outerBand > 0.001 && uEdgeIllum > 0.01) {
            float2 on = sceneNormalSquircle(local, halfSize, uCornerPower, 1.0);
            float side = pow(max(1.0 - abs(dot(on, uLight)), 0.0), 0.75);
            float oc = clamp(outerBand * 0.16 * uEdgeIllum * mix(0.25, 1.0, side), 0.0, 0.55);
            sh.a = max(sh.a, half(oc));
        }
        return sh;
    }

    float eps = clamp(uRefractBand * 0.25, 1.0, 12.0);
    float2 g = float2(
        sceneSdSquircle(local + float2(eps, 0.0), halfSize, uCornerPower) - sceneSdSquircle(local - float2(eps, 0.0), halfSize, uCornerPower),
        sceneSdSquircle(local + float2(0.0, eps), halfSize, uCornerPower) - sceneSdSquircle(local - float2(0.0, eps), halfSize, uCornerPower)
    );
    float gLen = length(g);
    float2 n = (gLen > 1e-5) ? (g / gLen) : float2(0.0, -1.0);
    float axisFade = smoothstep(0.15, 0.80, gLen / (2.0 * eps));

    float depth = max(-d, 0.0);
    float inradius = min(halfSize.x, halfSize.y);
    float aspect = max(halfSize.x, halfSize.y) / max(inradius, 1.0);
    float domeWeight = (uExtraCount > 0.5) ? 0.0 :
        smoothstep(1.5, 3.0, aspect) * smoothstep(0.75, 1.0, uRefractBand / max(inradius, 1.0));

    float opticalDepth = depth;
    float2 nRefract = n;
    if (domeWeight > 0.001) {
        float2 q = p / max(halfSize, float2(1.0));
        float2 q2 = q * q;
        float domeR = sqrt(clamp(q2.x + q2.y - q2.x * q2.y, 0.0, 1.0));
        opticalDepth = mix(depth, (1.0 - domeR) * uRefractBand, domeWeight);
        float2 dGrad = float2(q.x * (1.0 - q2.y), q.y * (1.0 - q2.x));
        float dLen = length(dGrad);
        if (dLen > 1e-4) nRefract = normalize(mix(n, dGrad / dLen, domeWeight));
        axisFade = mix(axisFade, 1.0, domeWeight);
    }

    float e = clamp(opticalDepth / max(uRefractBand, 1.0), 0.0, 1.0);
    float pw = max(uBevelPower, 1.5);
    float slope = bevelSlope(e, pw);
    float slopeLight = (domeWeight > 0.001) ? bevelSlope(clamp(depth / max(uRefractBand, 1.0), 0.0, 1.0), pw) : slope;

    float bend = snellShift(slope, uIor) / max(snellShift(bevelSlope(0.0, pw), uIor), 1e-4);
    bend *= smoothstep(1.0, 0.72, e) * axisFade * smoothstep(0.0, 1.5, depth);
    bend = clamp(bend, 0.0, 1.0);

    // Interior gentle lens magnification + fingertip Gaussian swell
    float2 qNorm = p / max(halfSize, float2(1.0));
    float r2 = clamp(dot(qNorm, qNorm), 0.0, 1.0);
    float shoulder = 1.0 - r2;
    float2 base = coord - p * (clamp(uLensZoom, -0.4, 0.6) * shoulder * shoulder);

    float2 touchDelta = local - uTouch;
    float touchSigma = max(min(uSize.x, uSize.y), 1.0);
    float touchFall = exp(-dot(touchDelta, touchDelta) / (touchSigma * touchSigma)) * clamp(uPress, 0.0, 2.0);
    base -= touchDelta * (touchFall * 0.17);

    float2 push = nRefract * (uOutward * bend * uRefractDepth * (1.0 + 0.25 * touchFall));
    float split = uAberration * bend;
    float2 pushR = push * (1.0 - split);
    float2 pushB = push * (1.0 + split);

    // Displacement-first crisp rim sample with subtle normal-aligned anti-aliasing smear
    float2 sm = nRefract * (0.75 * uDensity * bend);
    float3 sharp = float3(
        (sampleBackdropRgb(base + pushR).r + sampleBackdropRgb(base + pushR - sm).r + sampleBackdropRgb(base + pushR + sm).r) / 3.0,
        (sampleBackdropRgb(base + push).g  + sampleBackdropRgb(base + push - sm).g  + sampleBackdropRgb(base + push + sm).g)  / 3.0,
        (sampleBackdropRgb(base + pushB).b + sampleBackdropRgb(base + pushB - sm).b + sampleBackdropRgb(base + pushB + sm).b) / 3.0
    );

    // Dual-kernel iOS 27 material: B_wide (tone) + k*(D_fine - B_wide) + c*|D_fine - B_wide|
    float2 rot = tapRot(coord);
    float fineRad = mix(0.35 * uDensity, min(uBlurPx * 0.12, 2.0 * uDensity), e);
    float3 detailCopy = (fineRad < 0.75 * uDensity) ? detailCross(base + push, fineRad) : scatterFine(base + push, fineRad, rot);
    float3 wideCopy = (uBlurPx > 1.0) ? sampleWideRgb(base + push * 0.35) : detailCopy;
    float wideBlend = smoothstep(0.5, 10.0 * uDensity, uBlurPx);

    float wideLuma = luma709(wideCopy);
    float darkFactor = (uDarkMode < -0.5) ? smoothstep(0.42, 0.58, 1.0 - wideLuma) : clamp(uDarkMode, 0.0, 1.0);

    float fineShare = mix(0.275, 0.85, clamp(uTransparency, 0.0, 1.0));
    if (uIsClear > 0.5) fineShare = mix(0.75, 0.96, uTransparency);
    float detailLift = mix(0.22, -0.22, darkFactor) * (1.0 - uIsClear * 0.85) * (1.0 - uTransparency * 0.4);

    float3 dual = wideCopy + (detailCopy - wideCopy) * fineShare + abs(detailCopy - wideCopy) * detailLift;
    float3 interior = mix(detailCopy, dual, wideBlend);

    // Preserve crisp refracted rim & chromatic fringe near the outline
    float rimSharp = (1.0 - e) * (1.0 - e);
    float3 bg = mix(interior, sharp, rimSharp * mix(0.65, 0.95, uIsClear));

    // iOS 27 measured tone + P3->sRGB chroma boost (about luma y)
    float y = luma709(bg);
    float3 chromaDiff = bg - float3(y);
    float3 g0 = mix(float3(0.988, 0.952, 0.979), float3(1.317, 1.256, 1.314), darkFactor);
    float3 g1 = mix(float3(-0.076, 0.028, -0.045), float3(-0.427, -0.351, -0.429), darkFactor);

    float tintStr = clamp(uTint.a * (1.0 - uTransparency * 0.55) * mix(1.0, 0.28, uIsClear), 0.0, 0.92);
    float3 col = mix(bg, uTint.rgb, tintStr);

    // Concave dark tone bow: uToneCurve * x * (1 - x) + fixed 31.7/255 dark lift
    float bow = mix(0.0, 0.396, darkFactor) * (1.0 - uIsClear * 0.85) * (1.0 - uTransparency * 0.45);
    col += bow * bg * (float3(1.0) - bg);
    float chromaWeight = (1.0 - uIsClear * 0.65) * 0.85;
    float3 chromaGain = (g0 + g1 * y) - float3(1.0 - tintStr);
    col += chromaWeight * (chromaDiff * chromaGain);
    col += float3(mix(0.022, 0.124, darkFactor) * (1.0 - uIsClear * 0.80) * (1.0 - uTransparency * 0.4));

    // Rim lighting: Schlick Fresnel + directional chamfer/bead specular + counter lobe + Oklab highlight
    float bevelW = max(2.2 * uDensity, uRefractBand * 0.14);
    float bevelBand = smoothstep(bevelW, -0.2 * bevelW, depth);
    float innerShadow = max(smoothstep(bevelW * 2.4, 0.0, depth) - bevelBand, 0.0);
    col -= float3(innerShadow * 0.07 * uShadow);

    float cosI = inversesqrt(1.0 + slopeLight * slopeLight);
    float f0 = (uIor - 1.0) / (uIor + 1.0);
    f0 = f0 * f0;
    float fresnel = f0 + (1.0 - f0) * pow(max(1.0 - cosI, 0.0), 5.0);

    float facing = dot(n, -uLight);
    float faceUp = max(facing, 0.0);
    float faceDown = max(-facing, 0.0);
    float lobeUp = 0.47 * pow(max(faceUp, 1e-5), 24.0) + 0.53 * pow(max(faceUp, 1e-5), 2.2);
    float lobeDown = 0.47 * pow(max(faceDown, 1e-5), 24.0) + 0.53 * pow(max(faceDown, 1e-5), 2.2);
    float edgeLine = smoothstep(2.0 * uDensity, 0.0, depth) * 0.55 * (lobeUp + 0.72 * lobeDown);
    float glowTail = 0.055 * exp(-max(depth - 0.67 * bevelW, 0.0) / max(bevelW * 1.4, 1.0)) * (faceUp + 0.6 * faceDown);

    float highlight = ((lobeUp + 0.65 * lobeDown) * bevelBand * 0.34 * uSpecular
                     + edgeLine * 0.45 * uSpecular
                     + fresnel * 0.30 * bevelBand * uEdgeIllum
                     + glowTail * uEdgeIllum);
    col = applyOklabHighlight(col, highlight, 0.78);

    // Subtle dark inner contour at sides where light is tangential (iOS 27 refinement)
    float rimDark = clamp(1.0 - abs(depth - 1.2 * uDensity) / max(1.4 * uDensity, 1.0), 0.0, 1.0);
    float sideMask = 1.0 - abs(facing);
    col -= float3(rimDark * sideMask * 0.14 * uEdgeIllum);

    // Fingertip illumination from within
    col += float3(touchFall * 0.075);

    col = clamp(col, float3(0.0), float3(1.0));
    half4 result = half4(half3(col * coverage), half(coverage));
    if (outerBand > 0.001 && uEdgeIllum > 0.01) {
        float side = pow(max(1.0 - abs(dot(n, uLight)), 0.0), 0.75);
        result.a = clamp(result.a + half(outerBand * 0.16 * uEdgeIllum * mix(0.25, 1.0, side) * (1.0 - coverage)), half(0.0), half(1.0));
    }
    return result;
}
""".trimIndent()
    }
}
