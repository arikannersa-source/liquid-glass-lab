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
 * Engine 7: feice11 / Mochi Compose — Apple G2 Continuous Supercircle Liquid Glass (Apache-2.0)
 *
 * Faithful AGSL implementation of `feice11/mochi-compose` (`LiquidShaders.kt`):
 * - Exact **Apple G2 continuous supercircle SDF** (`1.5286649465560913` corner ratio +
 *   degree-4 rational polynomial `((-0.926054*t + 3.15601)*t - 3.64122)*t + 1.26803)*t + 0.268531`)
 * - Smooth-minimum neck gradient normalization (`d /= clamp(length(g) * 2.0, 0.35, 1.0)`)
 * - Dual **inner + outer bevel refraction** (`amt - sqrt((2 - t) * t) * amt`)
 * - Luma-compressed face color matrix (`uLumaComp`) + **rim edge color bleed** (`uBleed`)
 * - **Stroke-mode key/fill dual specular rim highlight** (`vv / (1 + amount*(1 - vv))`)
 * - **Gaussian `erfp` inner ring shadow** shifted 8dp downward inside the glass
 */
class MochiG2Engine : LiquidGlassEngine {
    override val id = GlassEngineId.MOCHI_G2
    override val displayName = "Mochi — Apple G2 Supercircle"
    override val shortName = "Mochi G2"
    override val author = "feice11 (mochi-compose)"
    override val sourceRepoUrl = "https://github.com/feice11/mochi-compose"
    override val license = "Apache-2.0"
    override val calibrationBasis = "Native Jetpack Compose iOS-motion & G2 continuous supercircle implementation with dual inner/outer refraction and Gaussian erfp ring shadow"
    override val opticalSummary = "Exact Apple G2 continuous supercircle SDF (1.52866x curvature), dual inner+outer refraction, luma-compressed face matrix, edge color bleed, erfp downward ring shadow."
    override val portingNotes = "Consolidated Mochi's LIQUID_FACE_AGSL + LIQUID_FINISH_AGSL into one AGSL pass over pre-blurred RenderEffect + wide strip; preserves exact supercircle polynomial, bevel(), erfp(), and key/fill rim math."
    override val capabilities = GlassCapabilities.FullLiquidGlass
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
            refractionStrength = if (isClear) 1.20f else 1.12f,
            glassThicknessDp = if (isClear) 22f else 24f,
            edgeCurvature = 1.0f,
            lensDistortion = 0.16f,
            blurRadiusDp = if (isClear) 1.0f else 6.0f,
            transparency = if (isClear) 0.92f else 0.78f,
            tintColorArgb = if (appearance == GlassAppearance.DARK) 0xFF161820L else 0xFFFAFAFDL,
            tintOpacity = when {
                isClear -> 0.03f
                appearance == GlassAppearance.ADAPTIVE -> 0.05f
                else -> 0.14f
            },
            chromaticAberration = 0.25f,
            specularIntensity = 1.05f,
            lightAngleDegrees = 225f,
            edgeIllumination = 1.05f,
            shadowIntensity = 0.95f,
            cornerRadiusDp = 28f,
            touchDeformation = 1.0f,
            animationSpeed = 1.0f,
            morphSmoothingDp = 30f
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun bindUniforms(shader: RuntimeShader, ctx: GlassDrawContext) {
        CommonAgsl.bindCommonUniforms(shader, ctx)
        val p = ctx.parameters.sanitized()
        val isClear = p.variant == GlassVariant.CLEAR
        val innerH = (p.glassThicknessDp * ctx.density).coerceAtLeast(3f)
        val innerAmt = innerH * 0.85f * p.refractionStrength
        val outerAmt = innerH * 0.35f * p.refractionStrength

        shader.setFloatUniform("uWideStripY", ctx.wideStripY)
        shader.setFloatUniform("uWideScale", ctx.wideScale)
        shader.setFloatUniform("uBlurPx", (p.blurRadiusDp * ctx.density).coerceAtLeast(0f))
        shader.setFloatUniform("uInnerAmt", innerAmt)
        shader.setFloatUniform("uInnerInvH", 1.0f / innerH)
        shader.setFloatUniform("uOuterAmt", outerAmt)
        shader.setFloatUniform("uCurvature", p.edgeCurvature)
        shader.setFloatUniform("uLensZoom", p.lensDistortion)
        shader.setFloatUniform("uAberration", p.chromaticAberration * 0.15f)
        shader.setFloatUniform("uTransparency", p.transparency)
        shader.setFloatUniform("uTint", p.tintRed(), p.tintGreen(), p.tintBlue(), p.tintOpacity)
        shader.setFloatUniform("uLight", ctx.lightDirX, ctx.lightDirY)
        shader.setFloatUniform("uSpecular", p.specularIntensity)
        shader.setFloatUniform("uEdgeIllum", p.edgeIllumination)
        shader.setFloatUniform("uIsClear", if (isClear) 1f else 0f)
        shader.setFloatUniform("uDarkMode", p.darkModeUniform())
    }

    companion object {
        val SHADER_SOURCE: String = """
${CommonAgsl.COMMON_UNIFORMS}
uniform float uWideStripY;
uniform float uWideScale;
uniform float uBlurPx;
uniform float uInnerAmt;
uniform float uInnerInvH;
uniform float uOuterAmt;
uniform float uCurvature;
uniform float uLensZoom;
uniform float uAberration;
uniform float uTransparency;
uniform float4 uTint;
uniform float2 uLight;
uniform float uSpecular;
uniform float uEdgeIllum;
uniform float uIsClear;
uniform float uDarkMode;

${CommonAgsl.COMMON_GEOMETRY}

// Exact Apple G2 continuous supercircle SDF from feice11/mochi-compose LiquidShaders.kt
float3 mochiSupercircle(float2 p, float2 b, float r) {
    r = clamp(abs(r), 0.5, min(b.x, b.y));
    float R = min(r * 1.5286649465560913, min(b.x, b.y));
    float2 q = p - b + float2(R);
    float2 u = abs(max(float2(0.0), q / max(R, 1e-4)));
    float L = length(u);
    float umax = max(u.x, u.y);
    float umin = min(u.x, u.y);
    float t = (umax <= 1e-5) ? 0.0 : clamp(umin / umax, 0.0, 1.0);
    float poly = (((-0.9260540008544922 * t + 3.1560099124908447) * t - 3.6412200927734375) * t + 1.268030047416687) * t + 0.2685309946537018;
    float denom = max(1.0 - t * t * clamp(L, 0.0, 1.0) * poly, 0.05);
    float f = (L + 1.0 - 1.0 / denom) - 1.0;
    float2 m = max(float2(0.0), q);
    float mm = dot(m, m);
    float2 n = (mm > 1e-6) ? (m * inversesqrt(mm)) : ((q.x > q.y) ? float2(1.0, 0.0) : float2(0.0, 1.0));
    return float3(min(max(q.x, q.y), 0.0) + R * f, n);
}

float mochiGroupD(float2 local, float2 halfSize) {
    float2 p0 = local - halfSize;
    float r0 = radiusAt(p0, uRadii);
    float d = mochiSupercircle(abs(p0), halfSize, r0).x;
    for (int i = 0; i < 5; i++) {
        if (float(i) >= uExtraCount) break;
        float4 s = uExtraShapes[i];
        float e = mochiSupercircle(abs(local - s.xy), s.zw, uExtraRadii[i]).x;
        d = sminPoly(d, e, uMergeK);
    }
    return d;
}

// Mochi bevel profile: amt - sqrt((2 - t) * t) * amt
float mochiBevel(float amt, float invH, float d) {
    float t = clamp(invH * (-d), 0.0, 1.0);
    float shaped = pow(max(t, 1e-5), clamp(1.0 / max(uCurvature, 0.35), 0.4, 2.2));
    return amt - clamp(sqrt(max((2.0 - shaped) * shaped, 0.0)), 0.0, 1.0) * amt;
}

// Mochi Gaussian erfp approximation for inner ring shadow
float erfp(float x) {
    float s = clamp(x * 0.25 + 0.5, 0.0, 1.0) * 4.0 - 2.0;
    float s2 = s * s;
    return (((0.0029544830322265625 * s2 - 0.034454345703125) * s2 + 0.168212890625) * s2 - 0.560546875) * s + 0.5;
}

float3 sampleMochiMip(float2 coord, float blurAmt) {
    float3 sharp = sampleBackdropRgb(coord);
    if (uWideStripY < 1.0 || blurAmt < 0.5) return sharp;
    float2 q = clamp(coord, uBackdrop.xy, uBackdrop.zw);
    float2 c = float2(q.x * uWideScale, uWideStripY + q.y * uWideScale);
    half4 s = content.eval(c);
    float3 wide = (s.a > half(0.004)) ? float3(s.rgb / s.a) : sharp;
    float w = smoothstep(0.5, 12.0 * uDensity, blurAmt);
    return mix(sharp, wide, w);
}

half4 main(float2 coord) {
    float2 local = coord - float2(uPad);
    float2 halfSize = uSize * 0.5;

    float d = mochiGroupD(local, halfSize);
    float2 g = float2(
        mochiGroupD(local + float2(0.75, 0.0), halfSize) - mochiGroupD(local - float2(0.75, 0.0), halfSize),
        mochiGroupD(local + float2(0.0, 0.75), halfSize) - mochiGroupD(local - float2(0.0, 0.75), halfSize)
    );
    float gLen = length(g);
    float2 n = (gLen > 1e-5) ? (g / gLen) : float2(0.0, -1.0);
    // Neck distance compensation from Mochi LiquidShaders.kt
    if (uExtraCount > 0.5) {
        d /= clamp(gLen / 1.5, 0.35, 1.0);
    }

    float cov = clamp(0.5 - d, 0.0, 1.0);
    if (cov <= 0.0) {
        float dSh = mochiGroupD(local - float2(0.0, 5.5 * uDensity), halfSize);
        return dropShadowOutside(dSh, uShadow, uDensity);
    }

    float depth = max(-d, 0.0);
    float dispInner = mochiBevel(uInnerAmt, uInnerInvH, d);
    float dispOuter = mochiBevel(uOuterAmt, uInnerInvH * 2.2, d);

    // Touch radial bulge + press glow
    float2 touchDelta = local - uTouch;
    float touchSigma = max(min(uSize.x, uSize.y) * 0.75, 1.0);
    float touchFall = exp(-dot(touchDelta, touchDelta) / (touchSigma * touchSigma)) * clamp(uPress, 0.0, 2.0);

    float2 baseDisp = -n * (dispInner * (1.0 + 0.25 * touchFall))
        - (local - halfSize) * (uLensZoom * 0.12)
        - touchDelta * (touchFall * 0.14);

    // Distance-dependent blur (crisp near rim, softer interior)
    float rimT = clamp(depth * uInnerInvH, 0.0, 1.0);
    float localBlur = uBlurPx * mix(0.22, 1.0, rimT);

    float3 c;
    if (uAberration > 0.005 && dispInner > 0.1) {
        float2 ca = n * (dispInner * uAberration);
        c = float3(
            sampleMochiMip(coord + baseDisp - ca, localBlur).r,
            sampleMochiMip(coord + baseDisp, localBlur).g,
            sampleMochiMip(coord + baseDisp + ca, localBlur).b
        );
    } else {
        c = sampleMochiMip(coord + baseDisp, localBlur);
    }

    // Outer edge counter-refraction blend (Mochi d > -1.0pt outer refraction stage)
    float outerBlend = (1.0 - smoothstep(0.0, 3.0 * uDensity, depth)) * 0.35;
    if (outerBlend > 0.01) {
        float3 cOuter = sampleBackdropRgb(coord + n * dispOuter);
        c = mix(c, cOuter, outerBlend);
    }

    // Luma compression + face color transfer
    float L = luma709(c);
    float darkFactor = (uDarkMode < -0.5) ? smoothstep(0.42, 0.62, 1.0 - L) : clamp(uDarkMode, 0.0, 1.0);
    float lumaComp = mix(0.22, 0.05, uIsClear);
    float kComp = clamp(1.0 - L * lumaComp, 0.0, 1.0);
    c = mix(float3(kComp * L), kComp * c, (1.0 - kComp) * 0.30 + mix(1.22, 1.08, uIsClear));

    float3 faceBias = mix(float3(0.16, 0.16, 0.18), float3(-0.10, -0.10, -0.09), darkFactor);
    c = clamp(c + faceBias * (1.0 - uTransparency * 0.55) * (1.0 - uIsClear * 0.8), float3(0.0), float3(1.0));

    // Rim color bleed (Mochi uBleed stage)
    float bleedW = (1.0 - smoothstep(0.0, 12.0 * uDensity, depth)) * 0.24 * (1.0 - uIsClear * 0.65);
    if (bleedW > 0.005) {
        float3 bCol = sampleMochiMip(coord - n * (4.0 * uDensity), uBlurPx * 1.4);
        c = mix(c, clamp(bCol * 1.12, float3(0.0), float3(1.0)), bleedW);
    }

    c = mix(c, uTint.rgb, clamp(uTint.a * mix(1.0, 0.30, uIsClear), 0.0, 0.88));

    // Mochi Gaussian erfp inner ring shadow shifted 8dp downward inside glass
    if (uShadow > 0.01 && depth < 22.0 * uDensity) {
        float shiftD = mochiGroupD(local - float2(0.0, 6.0 * uDensity), halfSize);
        float invR = 1.0 / max(5.0 * uDensity, 1.0);
        float a1 = invR * shiftD;
        float a2 = a1 + 4.0 * uDensity * invR;
        float ring = clamp(erfp(a1 * 0.70710678) - erfp(a2 * 0.70710678), 0.0, 1.0) * 0.16 * uShadow;
        c *= (1.0 - ring);
    }

    // Stroke-mode key/fill dual specular rim highlight (Mochi LIQUID_FINISH_AGSL)
    float rimStroke = smoothstep(2.4 * uDensity, 0.0, depth);
    float dd = dot(n, -uLight);
    float kfSpread = 0.22;
    float invS = 1.0 / max(1.0 - kfSpread, 1e-4);
    float2 vv = clamp((float2(dd, -dd) - float2(kfSpread)) * invS, float2(0.0), float2(1.0)) * rimStroke;
    float2 rKF = vv / max(float2(1.0) + 1.8 * (float2(1.0) - vv), float2(1e-4));
    float hl = (rKF.x * 0.68 + rKF.y * 0.44) * uSpecular;
    float fresnelRim = smoothstep(3.5 * uDensity, 0.0, depth) * 0.18 * uEdgeIllum;

    c = clamp(c + float3(hl + fresnelRim + touchFall * 0.09), float3(0.0), float3(1.0));
    return half4(half3(c * cov), half(cov));
}
""".trimIndent()
    }
}
