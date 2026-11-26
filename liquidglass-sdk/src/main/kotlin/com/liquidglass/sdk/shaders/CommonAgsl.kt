package com.liquidglass.sdk.shaders

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import com.liquidglass.sdk.core.GlassDrawContext

/**
 * Shared AGSL (SkSL dialect) primitives and uniform-binding helpers shared across engines.
 *
 * All snippets obey strict Android 13–17 AGSL and Skia SkSL rules:
 * - No `dFdx`/`dFdy`/`fwidth`
 * - No `int` uniforms (all flags/counts passed as `float`)
 * - Constant loop bounds (`MAX_EXTRA_SHAPES = 5`)
 * - Safe `pow()` bases and `inversesqrt`/division guards
 * - Premultiplied RGBA output
 */
internal object CommonAgsl {
    const val MAX_EXTRA_SHAPES = 5

    /** Common uniforms declared by every engine for geometry, morphing, touch, and shadow. */
    const val COMMON_UNIFORMS = """
uniform shader content;
uniform float2 uSize;
uniform float uPad;
uniform float4 uBackdrop;
uniform float4 uRadii;
uniform float uDensity;
uniform float uExtraCount;
uniform float4 uExtraShapes[5];
uniform float uExtraRadii[5];
uniform float uMergeK;
uniform float2 uTouch;
uniform float uPress;
uniform float uShadow;
"""

    /** Rounded-rect SDF, Lame superellipse SDF, smooth union (smin), safe backdrop sampling, drop shadow. */
    const val COMMON_GEOMETRY = """
float radiusAt(float2 p, float4 r) {
    float2 rr = (p.x >= 0.0) ? r.yz : r.xw;
    return (p.y >= 0.0) ? rr.y : rr.x;
}

float sdRoundBox(float2 p, float2 halfSize, float radius) {
    float r = min(radius, min(halfSize.x, halfSize.y));
    float2 q = abs(p) - halfSize + float2(r);
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}

float sdRoundBox4(float2 p, float2 halfSize, float4 radii) {
    float r = min(radiusAt(p, radii), min(halfSize.x, halfSize.y));
    float2 q = abs(p) - halfSize + float2(r);
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}

float lnNorm(float2 v, float n) {
    if (n <= 2.01) return length(v);
    float2 av = max(abs(v), float2(1e-6));
    return pow(pow(av.x, n) + pow(av.y, n), 1.0 / n);
}

float sdSquircleBox4(float2 p, float2 halfSize, float4 radii, float power) {
    float r = min(radiusAt(p, radii), min(halfSize.x, halfSize.y));
    float2 q = abs(p) - halfSize + float2(r);
    float2 pos = max(q, float2(0.0));
    float outside = (pos.x > 0.0 || pos.y > 0.0) ? lnNorm(pos, power) : 0.0;
    return min(max(q.x, q.y), 0.0) + outside - r;
}

float sminPoly(float a, float b, float k) {
    if (k <= 0.5) return min(a, b);
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

float sceneSdRound(float2 local, float2 halfSize) {
    float d = sdRoundBox4(local - halfSize, halfSize, uRadii);
    for (int i = 0; i < 5; i++) {
        if (float(i) >= uExtraCount) break;
        float4 s = uExtraShapes[i];
        float d2 = sdRoundBox(local - s.xy, s.zw, uExtraRadii[i]);
        d = sminPoly(d, d2, uMergeK);
    }
    return d;
}

float sceneSdSquircle(float2 local, float2 halfSize, float power) {
    float d = sdSquircleBox4(local - halfSize, halfSize, uRadii, power);
    for (int i = 0; i < 5; i++) {
        if (float(i) >= uExtraCount) break;
        float4 s = uExtraShapes[i];
        float d2 = sdSquircleBox4(local - s.xy, s.zw, float4(uExtraRadii[i]), power);
        d = sminPoly(d, d2, uMergeK);
    }
    return d;
}

float2 sceneNormalRound(float2 local, float2 halfSize, float eps) {
    float2 ex = float2(eps, 0.0);
    float2 ey = float2(0.0, eps);
    float2 g = float2(
        sceneSdRound(local + ex, halfSize) - sceneSdRound(local - ex, halfSize),
        sceneSdRound(local + ey, halfSize) - sceneSdRound(local - ey, halfSize)
    );
    float len = length(g);
    return (len > 1e-5) ? (g / len) : float2(0.0, -1.0);
}

float2 sceneNormalSquircle(float2 local, float2 halfSize, float power, float eps) {
    float2 ex = float2(eps, 0.0);
    float2 ey = float2(0.0, eps);
    float2 g = float2(
        sceneSdSquircle(local + ex, halfSize, power) - sceneSdSquircle(local - ex, halfSize, power),
        sceneSdSquircle(local + ey, halfSize, power) - sceneSdSquircle(local - ey, halfSize, power)
    );
    float len = length(g);
    return (len > 1e-5) ? (g / len) : float2(0.0, -1.0);
}

float3 sampleBackdropRgb(float2 coord) {
    float2 q = clamp(coord, uBackdrop.xy, uBackdrop.zw);
    half4 c = content.eval(q);
    return (c.a > half(0.004)) ? float3(c.rgb / c.a) : float3(0.08, 0.08, 0.10);
}

float luma709(float3 c) {
    return dot(c, float3(0.2126, 0.7152, 0.0722));
}

half4 dropShadowOutside(float dShadow, float shadowStrength, float density) {
    if (shadowStrength <= 0.005) return half4(0.0);
    float spread = max(14.0 * density, 4.0);
    float alpha = (1.0 - smoothstep(-2.0 * density, spread, dShadow)) * 0.22 * clamp(shadowStrength, 0.0, 2.0);
    return half4(0.0, 0.0, 0.0, half(clamp(alpha, 0.0, 0.65)));
}
"""

    /** Oklab perceptual highlight helper (adapted from shayann07/liquidglass GlassOklabSource.kt). */
    const val OKLAB_HIGHLIGHT = """
float3 toLinearApprox(float3 c) { return c * c; }
float3 fromLinearApprox(float3 c) { return sqrt(max(c, float3(0.0))); }

float3 linearToOklab(float3 c) {
    float l = 0.4122214708 * c.r + 0.5363325363 * c.g + 0.0514459929 * c.b;
    float m = 0.2119034982 * c.r + 0.6806995451 * c.g + 0.1073969566 * c.b;
    float s = 0.0883024619 * c.r + 0.2817188376 * c.g + 0.6299787005 * c.b;
    float l_ = pow(max(l, 1e-6), 0.3333333333);
    float m_ = pow(max(m, 1e-6), 0.3333333333);
    float s_ = pow(max(s, 1e-6), 0.3333333333);
    return float3(
        0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_,
        1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_,
        0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_);
}

float3 oklabToLinear(float3 lab) {
    float l_ = lab.x + 0.3963377774 * lab.y + 0.2158037573 * lab.z;
    float m_ = lab.x - 0.1055613458 * lab.y - 0.0638541728 * lab.z;
    float s_ = lab.x - 0.0894841775 * lab.y - 1.2914855480 * lab.z;
    float l = l_ * l_ * l_;
    float m = m_ * m_ * m_;
    float s = s_ * s_ * s_;
    return float3(
         4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
        -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
        -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s);
}

float3 applyOklabHighlight(float3 col, float amount, float chroma) {
    if (amount <= 0.0001) return col;
    float3 additive = col + float3(amount);
    if (chroma <= 0.0001) return additive;
    float3 lab = linearToOklab(toLinearApprox(clamp(col, float3(0.0), float3(1.0))));
    lab.x = lab.x + amount * 0.90;
    lab.yz = lab.yz * (1.0 + amount * 1.20);
    float3 lifted = fromLinearApprox(clamp(oklabToLinear(lab), float3(0.0), float3(1.0)));
    return mix(additive, lifted, clamp(chroma, 0.0, 1.0));
}
"""

    /** Binds all common uniforms shared across every engine. */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun bindCommonUniforms(shader: RuntimeShader, ctx: GlassDrawContext) {
        val p = ctx.parameters.sanitized()
        shader.setFloatUniform("uSize", ctx.widthPx, ctx.heightPx)
        shader.setFloatUniform("uPad", ctx.padPx)
        shader.setFloatUniform(
            "uBackdrop",
            ctx.backdropBoundsLeft,
            ctx.backdropBoundsTop,
            ctx.backdropBoundsRight,
            ctx.backdropBoundsBottom
        )
        val r = ctx.cornerRadiiPx
        shader.setFloatUniform(
            "uRadii",
            r.getOrElse(0) { p.cornerRadiusDp * ctx.density },
            r.getOrElse(1) { p.cornerRadiusDp * ctx.density },
            r.getOrElse(2) { p.cornerRadiusDp * ctx.density },
            r.getOrElse(3) { p.cornerRadiusDp * ctx.density }
        )
        shader.setFloatUniform("uDensity", ctx.density)

        val extraCount = ctx.shapes.size.coerceAtMost(MAX_EXTRA_SHAPES)
        shader.setFloatUniform("uExtraCount", extraCount.toFloat())
        val packedShapes = FloatArray(MAX_EXTRA_SHAPES * 4)
        val packedRadii = FloatArray(MAX_EXTRA_SHAPES)
        for (i in 0 until extraCount) {
            val s = ctx.shapes[i]
            packedShapes[i * 4 + 0] = s.centerX
            packedShapes[i * 4 + 1] = s.centerY
            packedShapes[i * 4 + 2] = s.halfWidth
            packedShapes[i * 4 + 3] = s.halfHeight
            packedRadii[i] = s.cornerRadius
        }
        shader.setFloatUniform("uExtraShapes", packedShapes)
        shader.setFloatUniform("uExtraRadii", packedRadii)
        shader.setFloatUniform("uMergeK", p.morphSmoothingDp * ctx.density)
        shader.setFloatUniform("uTouch", ctx.touchX, ctx.touchY)
        shader.setFloatUniform("uPress", (ctx.pressAmount * p.touchDeformation).coerceIn(0f, 2.5f))
        shader.setFloatUniform("uShadow", p.shadowIntensity)
    }
}
