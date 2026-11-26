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
 * Engine 4: Chris Banes / Haze Glass 2.1 (+ xenitV1 Spiralens Android Preset) (Apache-2.0 / MIT preset)
 *
 * Faithful AGSL implementation of `chrisbanes/haze` (`haze-glass`) optics:
 * - Dome-blended elongated optical field (`domeDistance` prevents medial-axis collapse on pills)
 * - 4 selectable surface profiles (0=Circle, 1=Squircle `1-smootherstep(t*sqrt(t))`, 2=Concave, 3=Lip)
 * - Refraction fold envelope (`refractionFoldEnvelope`) + Clear empirical exponential decay `exp(-t/0.225)*(1-t^2)^2`
 * - Content-normal lighting blend (underlying backdrop luminance gradients perturb surface lighting!)
 * - Gamut-preserving linear-sRGB chroma multiplier
 * - Sharp-rim detail pass (reads crisp unblurred capture at the rim while interior reads blurred strip!)
 * - iOS 27 darkened edge contour + dual specular rim
 */
class HazeGlassEngine : LiquidGlassEngine {
    override val id = GlassEngineId.HAZE_GLASS
    override val displayName = "Haze Glass 2.1 (+ xenitV1 Preset)"
    override val shortName = "Haze Glass"
    override val author = "Chris Banes (haze) + xenitV1"
    override val sourceRepoUrl = "https://github.com/chrisbanes/haze"
    override val license = "Apache-2.0 (+ MIT preset from xenitV1/liquid-glass-skill)"
    override val calibrationBasis = "Production Compose ADRs + WWDC26 iOS 27 dark-edge/diffusion research + xenitV1 Spiralens shipped Android Compose recipe"
    override val opticalSummary = "Dome-blended elongated field, 4 surface profiles (Circle/Squircle/Concave/Lip), fold envelope, content-normal ambient lighting, gamut-safe linear chroma, sharp-rim detail blend."
    override val portingNotes = "Unified Haze's fused optical pass + sharp-rim detail pass + directional rim highlight into one AGSL shader sampling both crisp backdrop and blurred wide strip."
    override val capabilities = GlassCapabilities.FullLiquidGlass.copy(supportsHazeSurfaceProfiles = true)
    override val usesPreBlur = false
    override val usesWideBlurStrip = true

    override val agslSource: String = SHADER_SOURCE

    override fun defaultParameters(
        variant: GlassVariant,
        appearance: GlassAppearance
    ): GlassParameters {
        val isClear = variant == GlassVariant.CLEAR
        // Spiralens squircle profile tuned for Apple iOS Liquid Glass optical clarity
        return GlassParameters(
            variant = variant,
            appearance = appearance,
            refractionStrength = if (isClear) 1.22f else 1.15f,
            glassThicknessDp = if (isClear) 22f else 24f,
            edgeCurvature = 1.0f,
            lensDistortion = 0.18f,
            blurRadiusDp = if (isClear) 1.0f else 6.5f,
            transparency = if (isClear) 0.92f else 0.78f,
            tintColorArgb = if (appearance == GlassAppearance.DARK) 0xFF161820L else 0xFFFCFCFEL,
            tintOpacity = when {
                isClear -> 0.03f
                appearance == GlassAppearance.ADAPTIVE -> 0.06f
                else -> 0.14f
            },
            chromaticAberration = 0.28f,
            specularIntensity = 1.05f,
            lightAngleDegrees = 225f,
            edgeIllumination = 1.05f,
            shadowIntensity = 0.85f,
            cornerRadiusDp = 28f,
            touchDeformation = 1.0f,
            animationSpeed = 1.0f,
            morphSmoothingDp = 28f,
            surfaceProfile = 1 // Squircle default (xenitV1 recommendation)
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun bindUniforms(shader: RuntimeShader, ctx: GlassDrawContext) {
        CommonAgsl.bindCommonUniforms(shader, ctx)
        val p = ctx.parameters.sanitized()
        val isClear = p.variant == GlassVariant.CLEAR
        val refrHeightPx = (p.glassThicknessDp * ctx.density).coerceAtLeast(3f)
        val refrScalePx = refrHeightPx * 1.1f * p.refractionStrength

        shader.setFloatUniform("uWideStripY", ctx.wideStripY)
        shader.setFloatUniform("uWideScale", ctx.wideScale)
        shader.setFloatUniform("uBlurPx", (p.blurRadiusDp * ctx.density).coerceAtLeast(0f))
        shader.setFloatUniform("uRefractionHeight", refrHeightPx)
        shader.setFloatUniform("uRefractionScale", refrScalePx)
        shader.setFloatUniform("uFoldStrength", if (isClear) 0.35f else 0.72f * p.edgeCurvature.coerceIn(0.3f, 1.5f))
        shader.setFloatUniform("uSurfaceProfile", p.surfaceProfile.toFloat())
        shader.setFloatUniform("uLensDistortion", p.lensDistortion)
        shader.setFloatUniform("uChromaMult", if (isClear) 1.12f else 1.32f)
        shader.setFloatUniform("uAberration", p.chromaticAberration * 0.22f)
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
uniform float uRefractionHeight;
uniform float uRefractionScale;
uniform float uFoldStrength;
uniform float uSurfaceProfile;
uniform float uLensDistortion;
uniform float uChromaMult;
uniform float uAberration;
uniform float uTransparency;
uniform float4 uTint;
uniform float2 uLight;
uniform float uSpecular;
uniform float uEdgeIllum;
uniform float uIsClear;
uniform float uDarkMode;

${CommonAgsl.COMMON_GEOMETRY}

float smootherstep(float x) {
    float t = clamp(x, 0.0, 1.0);
    return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
}

float circleMap(float x) {
    return 1.0 - sqrt(max(0.0, 1.0 - x * x));
}

float squircleMap(float t) {
    return 1.0 - smootherstep(t * sqrt(max(t, 0.0)));
}

float evaluateProfile(float t) {
    float x = 1.0 - clamp(t, 0.0, 1.0);
    if (uSurfaceProfile > 2.5) {
        // Lip profile: convex outer rim + concave inner dip
        float convex = circleMap(x);
        float concave = -circleMap(x);
        return mix(convex, concave, smootherstep(clamp(t / 0.7, 0.0, 1.0)));
    } else if (uSurfaceProfile > 1.5) {
        return -circleMap(x);
    } else if (uSurfaceProfile > 0.5) {
        return squircleMap(t);
    }
    return circleMap(x);
}

float3 sampleBlurredRgb(float2 coord) {
    if (uWideStripY < 1.0 || uBlurPx < 0.75) return sampleBackdropRgb(coord);
    float2 q = clamp(coord, uBackdrop.xy, uBackdrop.zw);
    float2 c = float2(q.x * uWideScale, uWideStripY + q.y * uWideScale);
    half4 s = content.eval(c);
    float3 wide = (s.a > half(0.004)) ? float3(s.rgb / s.a) : sampleBackdropRgb(coord);
    float blend = smoothstep(0.75, 10.0 * uDensity, uBlurPx);
    return mix(sampleBackdropRgb(coord), wide, blend);
}

float3 toLinearSrgb(float3 c) {
    return pow(max(c, float3(1e-5)), float3(2.2));
}

float3 fromLinearSrgb(float3 c) {
    return pow(max(c, float3(0.0)), float3(1.0 / 2.2));
}

// Gamut-preserving linear-sRGB chroma multiplier from chrisbanes/haze GlassShaders.kt
float3 applyHazeColorGrading(float3 color, float chromaMult, float whitePoint) {
    if (abs(chromaMult - 1.0) > 0.001) {
        float3 lin = toLinearSrgb(clamp(color, float3(0.0), float3(1.0)));
        float lum = dot(lin, float3(0.2126, 0.7152, 0.0722));
        float scale = chromaMult;
        if (chromaMult > 1.0) {
            float3 delta = lin - float3(lum);
            if (delta.r > 1e-4) scale = min(scale, (1.0 - lum) / delta.r);
            if (delta.r < -1e-4) scale = min(scale, -lum / delta.r);
            if (delta.g > 1e-4) scale = min(scale, (1.0 - lum) / delta.g);
            if (delta.g < -1e-4) scale = min(scale, -lum / delta.g);
            if (delta.b > 1e-4) scale = min(scale, (1.0 - lum) / delta.b);
            if (delta.b < -1e-4) scale = min(scale, -lum / delta.b);
            scale = max(1.0, scale);
        }
        color = fromLinearSrgb(mix(float3(lum), lin, scale));
    }
    if (abs(whitePoint) > 0.001) {
        float3 target = (whitePoint > 0.0) ? float3(1.0) : float3(0.0);
        color = mix(color, target, abs(whitePoint));
    }
    return clamp(color, float3(0.0), float3(1.0));
}

half4 main(float2 coord) {
    float2 local = coord - float2(uPad);
    float2 halfSize = uSize * 0.5;
    float2 centered = local - halfSize;

    float sd = sceneSdSquircle(local, halfSize, 3.6);
    float coverage = clamp(0.5 - sd, 0.0, 1.0);
    if (coverage <= 0.0) {
        float dSh = sceneSdSquircle(local - float2(0.0, 5.0 * uDensity), halfSize, 3.6);
        return dropShadowOutside(dSh, uShadow, uDensity);
    }

    float distToEdge = max(-sd, 0.0);

    // Haze dome-blended elongated optical field
    float shortSide = max(min(uSize.x, uSize.y), 1.0);
    float aspect = max(uSize.x, uSize.y) / shortSide;
    float elongation = smootherstep(clamp((aspect - 1.5) / 1.5, 0.0, 1.0));
    float overlap = smootherstep(clamp((uRefractionHeight / (shortSide * 0.5) - 0.75) / 0.25, 0.0, 1.0));
    float fieldWeight = (uExtraCount > 0.5) ? 0.0 : (elongation * overlap);

    float2 normCoord = centered / max(halfSize, float2(1.0));
    float2 sq = normCoord * normCoord;
    float domeRadius = sqrt(clamp(sq.x + sq.y - sq.x * sq.y, 0.0, 1.0));
    float domeDist = max((1.0 - domeRadius) * uRefractionHeight, 0.0);
    float opticalDist = mix(distToEdge, domeDist, fieldWeight);

    float2 boundaryGrad = sceneNormalSquircle(local, halfSize, 3.6, 1.5);
    float2 domeGrad = float2(normCoord.x * (1.0 - sq.y), normCoord.y * (1.0 - sq.x));
    float2 optGrad = mix(boundaryGrad, domeGrad, fieldWeight);
    float gLen = length(optGrad);
    float2 dispGrad = (gLen > 1e-4) ? (optGrad / gLen) : boundaryGrad;

    float t = clamp(opticalDist / max(uRefractionHeight, 1.0), 0.0, 1.0);
    float heightNorm;
    if (uIsClear > 0.5) {
        // Empirical Clear fit from Haze GlassShaders.kt: exp(-t/0.225)*(1-t^2)^2
        float cutoff = max(1.0 - t * t, 0.0);
        heightNorm = exp(-t / 0.225) * cutoff * cutoff;
    } else {
        heightNorm = evaluateProfile(t);
    }

    // Haze refraction fold envelope
    float foldRise = smootherstep(clamp(t / 0.2, 0.0, 1.0));
    float foldFall = 1.0 - smootherstep(clamp((t - 0.25) / 0.35, 0.0, 1.0));
    float foldW = clamp(uFoldStrength * foldRise * foldFall, 0.0, 1.0);
    float effHeight = mix(heightNorm, abs(heightNorm) * 0.02, foldW);

    // Touch localized refraction multiplier
    float2 touchDelta = local - uTouch;
    float touchR = max(shortSide * 0.6, 1.0);
    float touchW = exp(-dot(touchDelta, touchDelta) / (touchR * touchR)) * clamp(uPress, 0.0, 2.0);
    float2 disp = -dispGrad * (effHeight * uRefractionScale * (1.0 + 0.35 * touchW));
    disp -= centered * (uLensDistortion * 0.12 * (1.0 - domeRadius));
    disp -= touchDelta * (touchW * 0.14);

    float2 refractCoord = coord + disp;
    float cornerW = clamp(abs(normCoord.x * normCoord.y) * 1.4 + 0.25, 0.0, 1.0);
    float2 chromaOff = disp * (uAberration * cornerW);

    // Sample blurred body + sharp-rim detail pass (Haze two-detail fusion)
    float3 bodyRgb;
    if (length(chromaOff) > 0.05) {
        bodyRgb = float3(
            sampleBlurredRgb(refractCoord + chromaOff).r,
            sampleBlurredRgb(refractCoord).g,
            sampleBlurredRgb(refractCoord - chromaOff).b
        );
    } else {
        bodyRgb = sampleBlurredRgb(refractCoord);
    }

    float3 sharpRimRgb = float3(
        sampleBackdropRgb(refractCoord + chromaOff).r,
        sampleBackdropRgb(refractCoord).g,
        sampleBackdropRgb(refractCoord - chromaOff).b
    );
    float rimDetailMask = (1.0 - smootherstep(t)) * mix(0.55, 0.95, uIsClear);
    float3 rgb = mix(bodyRgb, sharpRimRgb, rimDetailMask);

    // Content-normal ambient Fresnel (Haze GlassShaders.kt computeContentNormal)
    float lC = luma709(rgb);
    float lR = luma709(sampleBackdropRgb(refractCoord + float2(2.0, 0.0)));
    float lD = luma709(sampleBackdropRgb(refractCoord + float2(0.0, 2.0)));
    float3 contentNormal = normalize(float3((lR - lC) * 1.5, (lD - lC) * 1.5, 1.0));
    float3 shapeNormal = normalize(float3(-boundaryGrad.x * (1.0 - t), -boundaryGrad.y * (1.0 - t), 1.0));
    float3 finalNormal = normalize(mix(shapeNormal, contentNormal, 0.22));
    float fresnel = pow(max(1.0 - max(finalNormal.z, 0.0), 0.0), 3.0);

    float darkFactor = (uDarkMode < -0.5) ? smoothstep(0.40, 0.62, 1.0 - lC) : clamp(uDarkMode, 0.0, 1.0);
    float whitePt = mix(0.14, -0.14, darkFactor) * (1.0 - uTransparency * 0.6) * (1.0 - uIsClear * 0.85);
    rgb = applyHazeColorGrading(rgb, uChromaMult, whitePt);

    float tintAlpha = clamp(uTint.a * (1.0 - uTransparency * 0.45) * mix(1.0, 0.30, uIsClear), 0.0, 0.85);
    rgb = mix(rgb, uTint.rgb, tintAlpha);
    rgb *= (1.0 + fresnel * 0.35 * uEdgeIllum);

    // Directional rim highlight + iOS 27 darkened edge contour
    float rimBand = smoothstep(2.4 * uDensity, 0.0, distToEdge);
    float lightFacing = dot(boundaryGrad, -uLight);
    float spec = (pow(max(lightFacing, 0.0), 3.0) + 0.55 * pow(max(-lightFacing, 0.0), 3.5)) * rimBand * 0.52 * uSpecular;
    float darkEdge = smoothstep(1.8 * uDensity, 0.0, distToEdge) * (1.0 - abs(lightFacing)) * 0.16 * uEdgeIllum;
    float innerSh = smoothstep(5.5 * uDensity, 0.0, distToEdge) * (1.0 - rimBand) * 0.05 * uShadow;

    rgb = rgb + float3(spec + touchW * 0.06) - float3(darkEdge + innerSh);
    rgb = clamp(rgb, float3(0.0), float3(1.0));

    return half4(half3(rgb * coverage), half(coverage));
}
""".trimIndent()
    }
}
