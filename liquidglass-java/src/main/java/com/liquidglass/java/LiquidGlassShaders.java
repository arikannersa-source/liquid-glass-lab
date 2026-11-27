package com.liquidglass.java;

import android.graphics.RuntimeShader;
import java.util.List;

/**
 * Verbatim AGSL shader sources and uniform bindings for all 9 Liquid Glass engines.
 * Byte-for-byte identical to shaders/*.agsl and :liquidglass-sdk (verified by automated tests).
 */
public final class LiquidGlassShaders {

    private LiquidGlassShaders() {}

    public static final String SHAYANN_IOS27 =
        "\n" +
        "uniform shader content;\n" +
        "uniform float2 uSize;\n" +
        "uniform float uPad;\n" +
        "uniform float4 uBackdrop;\n" +
        "uniform float4 uRadii;\n" +
        "uniform float uDensity;\n" +
        "uniform float uExtraCount;\n" +
        "uniform float4 uExtraShapes[5];\n" +
        "uniform float uExtraRadii[5];\n" +
        "uniform float uMergeK;\n" +
        "uniform float2 uTouch;\n" +
        "uniform float uPress;\n" +
        "uniform float uShadow;\n" +
        "\n" +
        "uniform float uWideStripY;\n" +
        "uniform float uWideScale;\n" +
        "uniform float uRefractBand;\n" +
        "uniform float uRefractDepth;\n" +
        "uniform float uIor;\n" +
        "uniform float uBevelPower;\n" +
        "uniform float uCornerPower;\n" +
        "uniform float uLensZoom;\n" +
        "uniform float uBlurPx;\n" +
        "uniform float uAberration;\n" +
        "uniform float uTransparency;\n" +
        "uniform float4 uTint;\n" +
        "uniform float2 uLight;\n" +
        "uniform float uSpecular;\n" +
        "uniform float uEdgeIllum;\n" +
        "uniform float uIsClear;\n" +
        "uniform float uDarkMode;\n" +
        "uniform float uOutward;\n" +
        "\n" +
        "\n" +
        "float radiusAt(float2 p, float4 r) {\n" +
        "    float2 rr = (p.x >= 0.0) ? r.yz : r.xw;\n" +
        "    return (p.y >= 0.0) ? rr.y : rr.x;\n" +
        "}\n" +
        "\n" +
        "float sdRoundBox(float2 p, float2 halfSize, float radius) {\n" +
        "    float r = min(radius, min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;\n" +
        "}\n" +
        "\n" +
        "float sdRoundBox4(float2 p, float2 halfSize, float4 radii) {\n" +
        "    float r = min(radiusAt(p, radii), min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;\n" +
        "}\n" +
        "\n" +
        "float lnNorm(float2 v, float n) {\n" +
        "    if (n <= 2.01) return length(v);\n" +
        "    float2 av = max(abs(v), float2(1e-6));\n" +
        "    return pow(pow(av.x, n) + pow(av.y, n), 1.0 / n);\n" +
        "}\n" +
        "\n" +
        "float sdSquircleBox4(float2 p, float2 halfSize, float4 radii, float power) {\n" +
        "    float r = min(radiusAt(p, radii), min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    float2 pos = max(q, float2(0.0));\n" +
        "    float outside = (pos.x > 0.0 || pos.y > 0.0) ? lnNorm(pos, power) : 0.0;\n" +
        "    return min(max(q.x, q.y), 0.0) + outside - r;\n" +
        "}\n" +
        "\n" +
        "float sminPoly(float a, float b, float k) {\n" +
        "    if (k <= 0.5) return min(a, b);\n" +
        "    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);\n" +
        "    return mix(b, a, h) - k * h * (1.0 - h);\n" +
        "}\n" +
        "\n" +
        "float sceneSdRound(float2 local, float2 halfSize) {\n" +
        "    float d = sdRoundBox4(local - halfSize, halfSize, uRadii);\n" +
        "    for (int i = 0; i < 5; i++) {\n" +
        "        if (float(i) >= uExtraCount) break;\n" +
        "        float4 s = uExtraShapes[i];\n" +
        "        float d2 = sdRoundBox(local - s.xy, s.zw, uExtraRadii[i]);\n" +
        "        d = sminPoly(d, d2, uMergeK);\n" +
        "    }\n" +
        "    return d;\n" +
        "}\n" +
        "\n" +
        "float sceneSdSquircle(float2 local, float2 halfSize, float power) {\n" +
        "    float d = sdSquircleBox4(local - halfSize, halfSize, uRadii, power);\n" +
        "    for (int i = 0; i < 5; i++) {\n" +
        "        if (float(i) >= uExtraCount) break;\n" +
        "        float4 s = uExtraShapes[i];\n" +
        "        float d2 = sdSquircleBox4(local - s.xy, s.zw, float4(uExtraRadii[i]), power);\n" +
        "        d = sminPoly(d, d2, uMergeK);\n" +
        "    }\n" +
        "    return d;\n" +
        "}\n" +
        "\n" +
        "float2 sceneNormalRound(float2 local, float2 halfSize, float eps) {\n" +
        "    float2 ex = float2(eps, 0.0);\n" +
        "    float2 ey = float2(0.0, eps);\n" +
        "    float2 g = float2(\n" +
        "        sceneSdRound(local + ex, halfSize) - sceneSdRound(local - ex, halfSize),\n" +
        "        sceneSdRound(local + ey, halfSize) - sceneSdRound(local - ey, halfSize)\n" +
        "    );\n" +
        "    float len = length(g);\n" +
        "    return (len > 1e-5) ? (g / len) : float2(0.0, -1.0);\n" +
        "}\n" +
        "\n" +
        "float2 sceneNormalSquircle(float2 local, float2 halfSize, float power, float eps) {\n" +
        "    float2 ex = float2(eps, 0.0);\n" +
        "    float2 ey = float2(0.0, eps);\n" +
        "    float2 g = float2(\n" +
        "        sceneSdSquircle(local + ex, halfSize, power) - sceneSdSquircle(local - ex, halfSize, power),\n" +
        "        sceneSdSquircle(local + ey, halfSize, power) - sceneSdSquircle(local - ey, halfSize, power)\n" +
        "    );\n" +
        "    float len = length(g);\n" +
        "    return (len > 1e-5) ? (g / len) : float2(0.0, -1.0);\n" +
        "}\n" +
        "\n" +
        "float3 sampleBackdropRgb(float2 coord) {\n" +
        "    float2 q = clamp(coord, uBackdrop.xy, uBackdrop.zw);\n" +
        "    half4 c = content.eval(q);\n" +
        "    return (c.a > half(0.004)) ? float3(c.rgb / c.a) : float3(0.08, 0.08, 0.10);\n" +
        "}\n" +
        "\n" +
        "float luma709(float3 c) {\n" +
        "    return dot(c, float3(0.2126, 0.7152, 0.0722));\n" +
        "}\n" +
        "\n" +
        "half4 dropShadowOutside(float dShadow, float shadowStrength, float density) {\n" +
        "    if (shadowStrength <= 0.005) return half4(0.0);\n" +
        "    float spread = max(14.0 * density, 4.0);\n" +
        "    float alpha = (1.0 - smoothstep(-2.0 * density, spread, dShadow)) * 0.22 * clamp(shadowStrength, 0.0, 2.0);\n" +
        "    return half4(0.0, 0.0, 0.0, half(clamp(alpha, 0.0, 0.65)));\n" +
        "}\n" +
        "\n" +
        "\n" +
        "float3 toLinearApprox(float3 c) { return c * c; }\n" +
        "float3 fromLinearApprox(float3 c) { return sqrt(max(c, float3(0.0))); }\n" +
        "\n" +
        "float3 linearToOklab(float3 c) {\n" +
        "    float l = 0.4122214708 * c.r + 0.5363325363 * c.g + 0.0514459929 * c.b;\n" +
        "    float m = 0.2119034982 * c.r + 0.6806995451 * c.g + 0.1073969566 * c.b;\n" +
        "    float s = 0.0883024619 * c.r + 0.2817188376 * c.g + 0.6299787005 * c.b;\n" +
        "    float l_ = pow(max(l, 1e-6), 0.3333333333);\n" +
        "    float m_ = pow(max(m, 1e-6), 0.3333333333);\n" +
        "    float s_ = pow(max(s, 1e-6), 0.3333333333);\n" +
        "    return float3(\n" +
        "        0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_,\n" +
        "        1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_,\n" +
        "        0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_);\n" +
        "}\n" +
        "\n" +
        "float3 oklabToLinear(float3 lab) {\n" +
        "    float l_ = lab.x + 0.3963377774 * lab.y + 0.2158037573 * lab.z;\n" +
        "    float m_ = lab.x - 0.1055613458 * lab.y - 0.0638541728 * lab.z;\n" +
        "    float s_ = lab.x - 0.0894841775 * lab.y - 1.2914855480 * lab.z;\n" +
        "    float l = l_ * l_ * l_;\n" +
        "    float m = m_ * m_ * m_;\n" +
        "    float s = s_ * s_ * s_;\n" +
        "    return float3(\n" +
        "         4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,\n" +
        "        -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,\n" +
        "        -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s);\n" +
        "}\n" +
        "\n" +
        "float3 applyOklabHighlight(float3 col, float amount, float chroma) {\n" +
        "    if (amount <= 0.0001) return col;\n" +
        "    float3 additive = col + float3(amount);\n" +
        "    if (chroma <= 0.0001) return additive;\n" +
        "    float3 lab = linearToOklab(toLinearApprox(clamp(col, float3(0.0), float3(1.0))));\n" +
        "    lab.x = lab.x + amount * 0.90;\n" +
        "    lab.yz = lab.yz * (1.0 + amount * 1.20);\n" +
        "    float3 lifted = fromLinearApprox(clamp(oklabToLinear(lab), float3(0.0), float3(1.0)));\n" +
        "    return mix(additive, lifted, clamp(chroma, 0.0, 1.0));\n" +
        "}\n" +
        "\n" +
        "\n" +
        "float bevelSlope(float e, float p) {\n" +
        "    float u = 1.0 - clamp(e, 0.002, 0.998);\n" +
        "    float up = pow(u, p);\n" +
        "    return pow(u, p - 1.0) / pow(max(1.0 - up, 1e-4), 1.0 - 1.0 / p);\n" +
        "}\n" +
        "\n" +
        "float snellShift(float slope, float ior) {\n" +
        "    float inv = inversesqrt(1.0 + slope * slope);\n" +
        "    float si = slope * inv;\n" +
        "    float ci = inv;\n" +
        "    float st = si / max(ior, 1.0001);\n" +
        "    float ct = sqrt(max(1.0 - st * st, 0.0));\n" +
        "    return (si * ct - ci * st) / max(ci * ct + si * st, 1e-4);\n" +
        "}\n" +
        "\n" +
        "float3 sampleWideRgb(float2 coord) {\n" +
        "    if (uWideStripY < 1.0) return sampleBackdropRgb(coord);\n" +
        "    float2 q = clamp(coord, uBackdrop.xy, uBackdrop.zw);\n" +
        "    float2 c = float2(q.x * uWideScale, uWideStripY + q.y * uWideScale);\n" +
        "    half4 s = content.eval(c);\n" +
        "    return (s.a > half(0.004)) ? float3(s.rgb / s.a) : sampleBackdropRgb(coord);\n" +
        "}\n" +
        "\n" +
        "// 4-tap iOS 27 measured detail kernel (1/3 dp = 1 px at 3x)\n" +
        "float3 detailCross(float2 p, float r) {\n" +
        "    if (r < 0.25) return sampleBackdropRgb(p);\n" +
        "    return (sampleBackdropRgb(p + float2(r, 0.0))\n" +
        "          + sampleBackdropRgb(p - float2(r, 0.0))\n" +
        "          + sampleBackdropRgb(p + float2(0.0, r))\n" +
        "          + sampleBackdropRgb(p - float2(0.0, r))) * 0.25;\n" +
        "}\n" +
        "\n" +
        "float2 tapRot(float2 coord) {\n" +
        "    float3 q = fract(float3(coord.x, coord.y, coord.x) * 0.1031);\n" +
        "    q += dot(q, q.yzx + 33.33);\n" +
        "    float a = fract((q.x + q.y) * q.z) * 6.2831853;\n" +
        "    return float2(cos(a), sin(a));\n" +
        "}\n" +
        "\n" +
        "float2 rot2(float2 v, float2 r) {\n" +
        "    return float2(v.x * r.x - v.y * r.y, v.x * r.y + v.y * r.x);\n" +
        "}\n" +
        "\n" +
        "float3 scatterFine(float2 p, float rad, float2 rot) {\n" +
        "    if (rad < 0.5) return sampleBackdropRgb(p);\n" +
        "    float r = min(rad, 3.0 * uDensity);\n" +
        "    float3 sum = sampleBackdropRgb(p) * 2.0;\n" +
        "    sum += sampleBackdropRgb(p + r * rot2(float2( 1.0,  0.0), rot));\n" +
        "    sum += sampleBackdropRgb(p + r * rot2(float2( 0.0,  1.0), rot));\n" +
        "    sum += sampleBackdropRgb(p + r * rot2(float2(-1.0,  0.0), rot));\n" +
        "    sum += sampleBackdropRgb(p + r * rot2(float2( 0.0, -1.0), rot));\n" +
        "    return sum * (1.0 / 6.0);\n" +
        "}\n" +
        "\n" +
        "half4 main(float2 coord) {\n" +
        "    float2 local = coord - float2(uPad);\n" +
        "    float2 halfSize = uSize * 0.5;\n" +
        "    float2 p = local - halfSize;\n" +
        "\n" +
        "    float d = sceneSdSquircle(local, halfSize, uCornerPower);\n" +
        "    float coverage = clamp(0.5 - d, 0.0, 1.0);\n" +
        "    float outerBand = clamp(1.0 - abs(d - 0.35 * uDensity) / max(0.85 * uDensity, 1.0), 0.0, 1.0);\n" +
        "\n" +
        "    if (coverage <= 0.0) {\n" +
        "        float dSh = sceneSdSquircle(local - float2(0.0, 5.0 * uDensity), halfSize, uCornerPower);\n" +
        "        half4 sh = dropShadowOutside(dSh, uShadow, uDensity);\n" +
        "        if (outerBand > 0.001 && uEdgeIllum > 0.01) {\n" +
        "            float2 on = sceneNormalSquircle(local, halfSize, uCornerPower, 1.0);\n" +
        "            float side = pow(max(1.0 - abs(dot(on, uLight)), 0.0), 0.75);\n" +
        "            float oc = clamp(outerBand * 0.16 * uEdgeIllum * mix(0.25, 1.0, side), 0.0, 0.55);\n" +
        "            sh.a = max(sh.a, half(oc));\n" +
        "        }\n" +
        "        return sh;\n" +
        "    }\n" +
        "\n" +
        "    float eps = clamp(uRefractBand * 0.25, 1.0, 12.0);\n" +
        "    float2 g = float2(\n" +
        "        sceneSdSquircle(local + float2(eps, 0.0), halfSize, uCornerPower) - sceneSdSquircle(local - float2(eps, 0.0), halfSize, uCornerPower),\n" +
        "        sceneSdSquircle(local + float2(0.0, eps), halfSize, uCornerPower) - sceneSdSquircle(local - float2(0.0, eps), halfSize, uCornerPower)\n" +
        "    );\n" +
        "    float gLen = length(g);\n" +
        "    float2 n = (gLen > 1e-5) ? (g / gLen) : float2(0.0, -1.0);\n" +
        "    float axisFade = smoothstep(0.15, 0.80, gLen / (2.0 * eps));\n" +
        "\n" +
        "    float depth = max(-d, 0.0);\n" +
        "    float inradius = min(halfSize.x, halfSize.y);\n" +
        "    float aspect = max(halfSize.x, halfSize.y) / max(inradius, 1.0);\n" +
        "    float domeWeight = (uExtraCount > 0.5) ? 0.0 :\n" +
        "        smoothstep(1.5, 3.0, aspect) * smoothstep(0.75, 1.0, uRefractBand / max(inradius, 1.0));\n" +
        "\n" +
        "    float opticalDepth = depth;\n" +
        "    float2 nRefract = n;\n" +
        "    if (domeWeight > 0.001) {\n" +
        "        float2 q = p / max(halfSize, float2(1.0));\n" +
        "        float2 q2 = q * q;\n" +
        "        float domeR = sqrt(clamp(q2.x + q2.y - q2.x * q2.y, 0.0, 1.0));\n" +
        "        opticalDepth = mix(depth, (1.0 - domeR) * uRefractBand, domeWeight);\n" +
        "        float2 dGrad = float2(q.x * (1.0 - q2.y), q.y * (1.0 - q2.x));\n" +
        "        float dLen = length(dGrad);\n" +
        "        if (dLen > 1e-4) nRefract = normalize(mix(n, dGrad / dLen, domeWeight));\n" +
        "        axisFade = mix(axisFade, 1.0, domeWeight);\n" +
        "    }\n" +
        "\n" +
        "    float e = clamp(opticalDepth / max(uRefractBand, 1.0), 0.0, 1.0);\n" +
        "    float pw = max(uBevelPower, 1.5);\n" +
        "    float slope = bevelSlope(e, pw);\n" +
        "    float slopeLight = (domeWeight > 0.001) ? bevelSlope(clamp(depth / max(uRefractBand, 1.0), 0.0, 1.0), pw) : slope;\n" +
        "\n" +
        "    float bend = snellShift(slope, uIor) / max(snellShift(bevelSlope(0.0, pw), uIor), 1e-4);\n" +
        "    bend *= smoothstep(1.0, 0.72, e) * axisFade * smoothstep(0.0, 1.5, depth);\n" +
        "    bend = clamp(bend, 0.0, 1.0);\n" +
        "\n" +
        "    // Interior gentle lens magnification + fingertip Gaussian swell\n" +
        "    float2 qNorm = p / max(halfSize, float2(1.0));\n" +
        "    float r2 = clamp(dot(qNorm, qNorm), 0.0, 1.0);\n" +
        "    float shoulder = 1.0 - r2;\n" +
        "    float2 base = coord - p * (clamp(uLensZoom, -0.4, 0.6) * shoulder * shoulder);\n" +
        "\n" +
        "    float2 touchDelta = local - uTouch;\n" +
        "    float touchSigma = max(min(uSize.x, uSize.y), 1.0);\n" +
        "    float touchFall = exp(-dot(touchDelta, touchDelta) / (touchSigma * touchSigma)) * clamp(uPress, 0.0, 2.0);\n" +
        "    base -= touchDelta * (touchFall * 0.17);\n" +
        "\n" +
        "    float2 push = nRefract * (uOutward * bend * uRefractDepth * (1.0 + 0.25 * touchFall));\n" +
        "    float split = uAberration * bend;\n" +
        "    float2 pushR = push * (1.0 - split);\n" +
        "    float2 pushB = push * (1.0 + split);\n" +
        "\n" +
        "    // Displacement-first crisp rim sample with subtle normal-aligned anti-aliasing smear\n" +
        "    float2 sm = nRefract * (0.75 * uDensity * bend);\n" +
        "    float3 sharp = float3(\n" +
        "        (sampleBackdropRgb(base + pushR).r + sampleBackdropRgb(base + pushR - sm).r + sampleBackdropRgb(base + pushR + sm).r) / 3.0,\n" +
        "        (sampleBackdropRgb(base + push).g  + sampleBackdropRgb(base + push - sm).g  + sampleBackdropRgb(base + push + sm).g)  / 3.0,\n" +
        "        (sampleBackdropRgb(base + pushB).b + sampleBackdropRgb(base + pushB - sm).b + sampleBackdropRgb(base + pushB + sm).b) / 3.0\n" +
        "    );\n" +
        "\n" +
        "    // Dual-kernel iOS 27 material: B_wide (tone) + k*(D_fine - B_wide) + c*|D_fine - B_wide|\n" +
        "    float2 rot = tapRot(coord);\n" +
        "    float fineRad = mix(0.35 * uDensity, min(uBlurPx * 0.12, 2.0 * uDensity), e);\n" +
        "    float3 detailCopy = (fineRad < 0.75 * uDensity) ? detailCross(base + push, fineRad) : scatterFine(base + push, fineRad, rot);\n" +
        "    float3 wideCopy = (uBlurPx > 1.0) ? sampleWideRgb(base + push * 0.35) : detailCopy;\n" +
        "    float wideBlend = smoothstep(0.5, 10.0 * uDensity, uBlurPx);\n" +
        "\n" +
        "    float wideLuma = luma709(wideCopy);\n" +
        "    float darkFactor = (uDarkMode < -0.5) ? smoothstep(0.42, 0.58, 1.0 - wideLuma) : clamp(uDarkMode, 0.0, 1.0);\n" +
        "\n" +
        "    float fineShare = mix(0.275, 0.85, clamp(uTransparency, 0.0, 1.0));\n" +
        "    if (uIsClear > 0.5) fineShare = mix(0.75, 0.96, uTransparency);\n" +
        "    float detailLift = mix(0.22, -0.22, darkFactor) * (1.0 - uIsClear * 0.85) * (1.0 - uTransparency * 0.4);\n" +
        "\n" +
        "    float3 dual = wideCopy + (detailCopy - wideCopy) * fineShare + abs(detailCopy - wideCopy) * detailLift;\n" +
        "    float3 interior = mix(detailCopy, dual, wideBlend);\n" +
        "\n" +
        "    // Preserve crisp refracted rim & chromatic fringe near the outline\n" +
        "    float rimSharp = (1.0 - e) * (1.0 - e);\n" +
        "    float3 bg = mix(interior, sharp, rimSharp * mix(0.65, 0.95, uIsClear));\n" +
        "\n" +
        "    // iOS 27 measured tone + P3->sRGB chroma boost (about luma y)\n" +
        "    float y = luma709(bg);\n" +
        "    float3 chromaDiff = bg - float3(y);\n" +
        "    float3 g0 = mix(float3(0.988, 0.952, 0.979), float3(1.317, 1.256, 1.314), darkFactor);\n" +
        "    float3 g1 = mix(float3(-0.076, 0.028, -0.045), float3(-0.427, -0.351, -0.429), darkFactor);\n" +
        "\n" +
        "    float tintStr = clamp(uTint.a * (1.0 - uTransparency * 0.55) * mix(1.0, 0.28, uIsClear), 0.0, 0.92);\n" +
        "    float3 col = mix(bg, uTint.rgb, tintStr);\n" +
        "\n" +
        "    // Concave dark tone bow: uToneCurve * x * (1 - x) + fixed 31.7/255 dark lift\n" +
        "    float bow = mix(0.0, 0.396, darkFactor) * (1.0 - uIsClear * 0.85) * (1.0 - uTransparency * 0.45);\n" +
        "    col += bow * bg * (float3(1.0) - bg);\n" +
        "    float chromaWeight = (1.0 - uIsClear * 0.65) * 0.85;\n" +
        "    float3 chromaGain = (g0 + g1 * y) - float3(1.0 - tintStr);\n" +
        "    col += chromaWeight * (chromaDiff * chromaGain);\n" +
        "    col += float3(mix(0.022, 0.124, darkFactor) * (1.0 - uIsClear * 0.80) * (1.0 - uTransparency * 0.4));\n" +
        "\n" +
        "    // Rim lighting: Schlick Fresnel + directional chamfer/bead specular + counter lobe + Oklab highlight\n" +
        "    float bevelW = max(2.2 * uDensity, uRefractBand * 0.14);\n" +
        "    float bevelBand = smoothstep(bevelW, -0.2 * bevelW, depth);\n" +
        "    float innerShadow = max(smoothstep(bevelW * 2.4, 0.0, depth) - bevelBand, 0.0);\n" +
        "    col -= float3(innerShadow * 0.07 * uShadow);\n" +
        "\n" +
        "    float cosI = inversesqrt(1.0 + slopeLight * slopeLight);\n" +
        "    float f0 = (uIor - 1.0) / (uIor + 1.0);\n" +
        "    f0 = f0 * f0;\n" +
        "    float fresnel = f0 + (1.0 - f0) * pow(max(1.0 - cosI, 0.0), 5.0);\n" +
        "\n" +
        "    float facing = dot(n, -uLight);\n" +
        "    float faceUp = max(facing, 0.0);\n" +
        "    float faceDown = max(-facing, 0.0);\n" +
        "    float lobeUp = 0.47 * pow(max(faceUp, 1e-5), 24.0) + 0.53 * pow(max(faceUp, 1e-5), 2.2);\n" +
        "    float lobeDown = 0.47 * pow(max(faceDown, 1e-5), 24.0) + 0.53 * pow(max(faceDown, 1e-5), 2.2);\n" +
        "    float edgeLine = smoothstep(2.0 * uDensity, 0.0, depth) * 0.55 * (lobeUp + 0.72 * lobeDown);\n" +
        "    float glowTail = 0.055 * exp(-max(depth - 0.67 * bevelW, 0.0) / max(bevelW * 1.4, 1.0)) * (faceUp + 0.6 * faceDown);\n" +
        "\n" +
        "    float highlight = ((lobeUp + 0.65 * lobeDown) * bevelBand * 0.34 * uSpecular\n" +
        "                     + edgeLine * 0.45 * uSpecular\n" +
        "                     + fresnel * 0.30 * bevelBand * uEdgeIllum\n" +
        "                     + glowTail * uEdgeIllum);\n" +
        "    col = applyOklabHighlight(col, highlight, 0.78);\n" +
        "\n" +
        "    // Subtle dark inner contour at sides where light is tangential (iOS 27 refinement)\n" +
        "    float rimDark = clamp(1.0 - abs(depth - 1.2 * uDensity) / max(1.4 * uDensity, 1.0), 0.0, 1.0);\n" +
        "    float sideMask = 1.0 - abs(facing);\n" +
        "    col -= float3(rimDark * sideMask * 0.14 * uEdgeIllum);\n" +
        "\n" +
        "    // Fingertip illumination from within\n" +
        "    col += float3(touchFall * 0.075);\n" +
        "\n" +
        "    col = clamp(col, float3(0.0), float3(1.0));\n" +
        "    half4 result = half4(half3(col * coverage), half(coverage));\n" +
        "    if (outerBand > 0.001 && uEdgeIllum > 0.01) {\n" +
        "        float side = pow(max(1.0 - abs(dot(n, uLight)), 0.0), 0.75);\n" +
        "        result.a = clamp(result.a + half(outerBand * 0.16 * uEdgeIllum * mix(0.25, 1.0, side) * (1.0 - coverage)), half(0.0), half(1.0));\n" +
        "    }\n" +
        "    return result;\n" +
        "}";

    public static final String FLUTTER_SEAGLASS =
        "\n" +
        "uniform shader content;\n" +
        "uniform float2 uSize;\n" +
        "uniform float uPad;\n" +
        "uniform float4 uBackdrop;\n" +
        "uniform float4 uRadii;\n" +
        "uniform float uDensity;\n" +
        "uniform float uExtraCount;\n" +
        "uniform float4 uExtraShapes[5];\n" +
        "uniform float uExtraRadii[5];\n" +
        "uniform float uMergeK;\n" +
        "uniform float2 uTouch;\n" +
        "uniform float uPress;\n" +
        "uniform float uShadow;\n" +
        "\n" +
        "uniform float uBevelPx;\n" +
        "uniform float uDispScalePx;\n" +
        "uniform float uCurvature;\n" +
        "uniform float uLensZoom;\n" +
        "uniform float uDispersion;\n" +
        "uniform float uTransparency;\n" +
        "uniform float4 uTint;\n" +
        "uniform float2 uLight;\n" +
        "uniform float uLightIntensity;\n" +
        "uniform float uContourStrength;\n" +
        "uniform float uIsClear;\n" +
        "uniform float uDarkMode;\n" +
        "\n" +
        "\n" +
        "float radiusAt(float2 p, float4 r) {\n" +
        "    float2 rr = (p.x >= 0.0) ? r.yz : r.xw;\n" +
        "    return (p.y >= 0.0) ? rr.y : rr.x;\n" +
        "}\n" +
        "\n" +
        "float sdRoundBox(float2 p, float2 halfSize, float radius) {\n" +
        "    float r = min(radius, min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;\n" +
        "}\n" +
        "\n" +
        "float sdRoundBox4(float2 p, float2 halfSize, float4 radii) {\n" +
        "    float r = min(radiusAt(p, radii), min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;\n" +
        "}\n" +
        "\n" +
        "float lnNorm(float2 v, float n) {\n" +
        "    if (n <= 2.01) return length(v);\n" +
        "    float2 av = max(abs(v), float2(1e-6));\n" +
        "    return pow(pow(av.x, n) + pow(av.y, n), 1.0 / n);\n" +
        "}\n" +
        "\n" +
        "float sdSquircleBox4(float2 p, float2 halfSize, float4 radii, float power) {\n" +
        "    float r = min(radiusAt(p, radii), min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    float2 pos = max(q, float2(0.0));\n" +
        "    float outside = (pos.x > 0.0 || pos.y > 0.0) ? lnNorm(pos, power) : 0.0;\n" +
        "    return min(max(q.x, q.y), 0.0) + outside - r;\n" +
        "}\n" +
        "\n" +
        "float sminPoly(float a, float b, float k) {\n" +
        "    if (k <= 0.5) return min(a, b);\n" +
        "    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);\n" +
        "    return mix(b, a, h) - k * h * (1.0 - h);\n" +
        "}\n" +
        "\n" +
        "float sceneSdRound(float2 local, float2 halfSize) {\n" +
        "    float d = sdRoundBox4(local - halfSize, halfSize, uRadii);\n" +
        "    for (int i = 0; i < 5; i++) {\n" +
        "        if (float(i) >= uExtraCount) break;\n" +
        "        float4 s = uExtraShapes[i];\n" +
        "        float d2 = sdRoundBox(local - s.xy, s.zw, uExtraRadii[i]);\n" +
        "        d = sminPoly(d, d2, uMergeK);\n" +
        "    }\n" +
        "    return d;\n" +
        "}\n" +
        "\n" +
        "float sceneSdSquircle(float2 local, float2 halfSize, float power) {\n" +
        "    float d = sdSquircleBox4(local - halfSize, halfSize, uRadii, power);\n" +
        "    for (int i = 0; i < 5; i++) {\n" +
        "        if (float(i) >= uExtraCount) break;\n" +
        "        float4 s = uExtraShapes[i];\n" +
        "        float d2 = sdSquircleBox4(local - s.xy, s.zw, float4(uExtraRadii[i]), power);\n" +
        "        d = sminPoly(d, d2, uMergeK);\n" +
        "    }\n" +
        "    return d;\n" +
        "}\n" +
        "\n" +
        "float2 sceneNormalRound(float2 local, float2 halfSize, float eps) {\n" +
        "    float2 ex = float2(eps, 0.0);\n" +
        "    float2 ey = float2(0.0, eps);\n" +
        "    float2 g = float2(\n" +
        "        sceneSdRound(local + ex, halfSize) - sceneSdRound(local - ex, halfSize),\n" +
        "        sceneSdRound(local + ey, halfSize) - sceneSdRound(local - ey, halfSize)\n" +
        "    );\n" +
        "    float len = length(g);\n" +
        "    return (len > 1e-5) ? (g / len) : float2(0.0, -1.0);\n" +
        "}\n" +
        "\n" +
        "float2 sceneNormalSquircle(float2 local, float2 halfSize, float power, float eps) {\n" +
        "    float2 ex = float2(eps, 0.0);\n" +
        "    float2 ey = float2(0.0, eps);\n" +
        "    float2 g = float2(\n" +
        "        sceneSdSquircle(local + ex, halfSize, power) - sceneSdSquircle(local - ex, halfSize, power),\n" +
        "        sceneSdSquircle(local + ey, halfSize, power) - sceneSdSquircle(local - ey, halfSize, power)\n" +
        "    );\n" +
        "    float len = length(g);\n" +
        "    return (len > 1e-5) ? (g / len) : float2(0.0, -1.0);\n" +
        "}\n" +
        "\n" +
        "float3 sampleBackdropRgb(float2 coord) {\n" +
        "    float2 q = clamp(coord, uBackdrop.xy, uBackdrop.zw);\n" +
        "    half4 c = content.eval(q);\n" +
        "    return (c.a > half(0.004)) ? float3(c.rgb / c.a) : float3(0.08, 0.08, 0.10);\n" +
        "}\n" +
        "\n" +
        "float luma709(float3 c) {\n" +
        "    return dot(c, float3(0.2126, 0.7152, 0.0722));\n" +
        "}\n" +
        "\n" +
        "half4 dropShadowOutside(float dShadow, float shadowStrength, float density) {\n" +
        "    if (shadowStrength <= 0.005) return half4(0.0);\n" +
        "    float spread = max(14.0 * density, 4.0);\n" +
        "    float alpha = (1.0 - smoothstep(-2.0 * density, spread, dShadow)) * 0.22 * clamp(shadowStrength, 0.0, 2.0);\n" +
        "    return half4(0.0, 0.0, 0.0, half(clamp(alpha, 0.0, 0.65)));\n" +
        "}\n" +
        "\n" +
        "\n" +
        "// Exact iOS 27 glint constants from whynotmake-it/flutter_liquid_glass\n" +
        "const float kGlintLuminance = 1.60;\n" +
        "const float kGlintVibrancy  = 2.85;\n" +
        "const float kGlintPeak      = 0.285;\n" +
        "const float kGlintBleed     = 0.21;\n" +
        "const float kGlintBleedReach = 4.0;\n" +
        "\n" +
        "// Box-filtered subpixel contour ramp integral from liquid_glass_final_render_core.glsl\n" +
        "float contourIntegral(float t, float edgeW) {\n" +
        "    float u = clamp(t, 0.0, edgeW);\n" +
        "    return u - (u * u) / max(2.0 * edgeW, 1e-4);\n" +
        "}\n" +
        "\n" +
        "float2 contourCoverage(float signedInward, float edgeW) {\n" +
        "    if (edgeW <= 0.001) return float2(0.0);\n" +
        "    float outward = -signedInward;\n" +
        "    float lo = outward - 0.5;\n" +
        "    float hi = outward + 0.5;\n" +
        "    return float2(\n" +
        "        contourIntegral(max(hi, 0.0), edgeW) - contourIntegral(max(lo, 0.0), edgeW),\n" +
        "        contourIntegral(min(hi, 0.0), edgeW) - contourIntegral(min(lo, 0.0), edgeW)\n" +
        "    );\n" +
        "}\n" +
        "\n" +
        "float lightAxisTangency(float2 n, float2 lightDir) {\n" +
        "    return abs(dot(n, float2(-lightDir.y, lightDir.x)));\n" +
        "}\n" +
        "\n" +
        "half4 main(float2 coord) {\n" +
        "    float2 local = coord - float2(uPad);\n" +
        "    float2 halfSize = uSize * 0.5;\n" +
        "\n" +
        "    float sd = sceneSdSquircle(local, halfSize, 4.0);\n" +
        "    float signedInward = -sd;\n" +
        "    float coverage = clamp(signedInward + 0.5, 0.0, 1.0);\n" +
        "    float edgeW = max(1.15 * uDensity, 1.0);\n" +
        "    float2 contourCov = contourCoverage(signedInward, edgeW);\n" +
        "\n" +
        "    if (coverage <= 0.0) {\n" +
        "        float dSh = sceneSdSquircle(local - float2(0.0, 5.0 * uDensity), halfSize, 4.0);\n" +
        "        half4 sh = dropShadowOutside(dSh, uShadow, uDensity);\n" +
        "        if (contourCov.x > 0.001 && uContourStrength > 0.01) {\n" +
        "            float2 on = sceneNormalSquircle(local, halfSize, 4.0, 1.0);\n" +
        "            float tang = mix(0.30, 1.0, lightAxisTangency(on, uLight));\n" +
        "            float ca = clamp(contourCov.x * 0.34 * tang * uContourStrength, 0.0, 0.70);\n" +
        "            sh.a = max(sh.a, half(ca));\n" +
        "        }\n" +
        "        return sh;\n" +
        "    }\n" +
        "\n" +
        "    float2 n = sceneNormalSquircle(local, halfSize, 4.0, 1.25);\n" +
        "    float inward = max(signedInward, 0.0);\n" +
        "    float halfMinor = min(halfSize.x, halfSize.y);\n" +
        "\n" +
        "    // refractionFitsShape: shrink bevel on small controls for regular glass, full lens on clear\n" +
        "    float effBevel = mix(min(uBevelPx, halfMinor * 0.72), uBevelPx, uIsClear);\n" +
        "    float t = clamp(inward / max(effBevel, 1.0), 0.0, 1.0);\n" +
        "    float edge = 1.0 - t;\n" +
        "    float curved = pow(max(edge, 1e-5), clamp(1.35 + 1.15 * uCurvature, 0.8, 4.0));\n" +
        "\n" +
        "    // Touch deformation\n" +
        "    float2 touchDelta = local - uTouch;\n" +
        "    float touchSigma = max(halfMinor * 1.4, 1.0);\n" +
        "    float touchFall = exp(-dot(touchDelta, touchDelta) / (touchSigma * touchSigma)) * clamp(uPress, 0.0, 2.0);\n" +
        "\n" +
        "    float2 disp = -n * (curved * uDispScalePx * (1.0 + 0.30 * touchFall));\n" +
        "    disp -= (local - halfSize) * (uLensZoom * 0.14 * (1.0 - edge));\n" +
        "    disp -= touchDelta * (touchFall * 0.13);\n" +
        "\n" +
        "    float3 rgb;\n" +
        "    if (uDispersion > 0.004 && curved > 0.01) {\n" +
        "        float2 ca = disp * uDispersion;\n" +
        "        rgb = float3(\n" +
        "            sampleBackdropRgb(coord + disp - ca).r,\n" +
        "            sampleBackdropRgb(coord + disp).g,\n" +
        "            sampleBackdropRgb(coord + disp + ca).b\n" +
        "        );\n" +
        "    } else {\n" +
        "        rgb = sampleBackdropRgb(coord + disp);\n" +
        "    }\n" +
        "\n" +
        "    // Parametric iOS 27 face transfer (transmittance + vibrancy + luminance lift)\n" +
        "    float bgLuma = luma709(rgb);\n" +
        "    float darkFactor = (uDarkMode < -0.5) ? smoothstep(0.42, 0.60, 1.0 - bgLuma) : clamp(uDarkMode, 0.0, 1.0);\n" +
        "    float sat = mix(1.34, 1.12, uIsClear);\n" +
        "    rgb = clamp(mix(float3(bgLuma), rgb, sat), float3(0.0), float3(1.0));\n" +
        "\n" +
        "    float transmittance = mix(mix(0.62, 0.88, uTransparency), 0.94, uIsClear);\n" +
        "    float3 neutralEmission = mix(float3(0.97, 0.97, 0.99), float3(0.09, 0.09, 0.11), darkFactor);\n" +
        "    float3 face = mix(neutralEmission, rgb, transmittance);\n" +
        "    face += float3(mix(0.03, 0.09, darkFactor) * (1.0 - uIsClear * 0.85) * (1.0 - uTransparency * 0.4));\n" +
        "    face = mix(face, uTint.rgb, clamp(uTint.a * mix(1.0, 0.32, uIsClear), 0.0, 0.88));\n" +
        "    face = clamp(face, float3(0.0), float3(1.0));\n" +
        "\n" +
        "    // Directional raised-rim bevel inner shadow (whynotmake-it liquid_glass_final_render_core.glsl)\n" +
        "    float bevelShadowDepth = max(effBevel * 0.48, 3.0 * uDensity);\n" +
        "    float lightFacing = dot(n, -uLight);\n" +
        "    float shadowShift = max(3.0 * uDensity * lightFacing, 0.0);\n" +
        "    float penumbra = 2.0 * shadowShift;\n" +
        "    float leading = (penumbra > 0.001) ? smoothstep(0.0, penumbra, inward) : 1.0;\n" +
        "    float trailing = 1.0 - smoothstep(0.0, bevelShadowDepth, max(inward - shadowShift, 0.0));\n" +
        "    float sizeFactor = mix(1.0, 1.65, smoothstep(bevelShadowDepth * 2.5, bevelShadowDepth * 4.5, halfMinor));\n" +
        "    float bevelShadow = leading * trailing * max(0.25, 0.6 + 0.4 * lightFacing) * 0.16 * uShadow * sizeFactor;\n" +
        "    face *= (1.0 - clamp(bevelShadow, 0.0, 0.45));\n" +
        "\n" +
        "    // iOS 27 Glint Recoloring: mixes face toward bright HDR-headroom chromatic target\n" +
        "    float glintW = max(2.2 * uDensity, 1.5);\n" +
        "    float glintProfile = clamp(1.0 - inward / glintW, 0.0, 1.0)\n" +
        "        + kGlintBleed * clamp(1.0 - inward / (glintW * kGlintBleedReach), 0.0, 1.0);\n" +
        "    float axisAlign = 1.0 - lightAxisTangency(n, uLight);\n" +
        "    float returnW = (lightFacing >= 0.0) ? 1.0 : 0.78;\n" +
        "    float glint = clamp(uLightIntensity * kGlintPeak * axisAlign * returnW * glintProfile, 0.0, 1.0);\n" +
        "\n" +
        "    float faceLuma = luma709(face);\n" +
        "    float3 faceChroma = face - float3(faceLuma);\n" +
        "    float glintLuma = mix(kGlintLuminance, 1.15, uIsClear);\n" +
        "    float faceGain = mix(0.0, 0.55, uIsClear);\n" +
        "    float3 glintTarget = clamp(float3(glintLuma) + faceGain * face + kGlintVibrancy * faceChroma, float3(0.0), float3(2.2));\n" +
        "    face = mix(face, glintTarget, glint);\n" +
        "\n" +
        "    // Box-filtered inner dark contour share at tangential sides\n" +
        "    float outlineCov = contourCov.y / max(coverage, 0.001);\n" +
        "    float tang = mix(0.30, 1.0, lightAxisTangency(n, uLight));\n" +
        "    float darkContour = clamp(outlineCov * 0.34 * tang * uContourStrength, 0.0, 0.65);\n" +
        "    face = mix(face, float3(0.02, 0.02, 0.03), darkContour);\n" +
        "\n" +
        "    face = clamp(face + float3(touchFall * 0.06), float3(0.0), float3(1.0));\n" +
        "    half4 res = half4(half3(face * coverage), half(coverage));\n" +
        "    if (contourCov.x > 0.001 && uContourStrength > 0.01) {\n" +
        "        float ca = clamp(contourCov.x * 0.34 * tang * uContourStrength * (1.0 - coverage), 0.0, 0.70);\n" +
        "        res.a = clamp(res.a + half(ca), half(0.0), half(1.0));\n" +
        "    }\n" +
        "    return res;\n" +
        "}";

    public static final String VITRO_IOS26 =
        "\n" +
        "uniform shader content;\n" +
        "uniform float2 uSize;\n" +
        "uniform float uPad;\n" +
        "uniform float4 uBackdrop;\n" +
        "uniform float4 uRadii;\n" +
        "uniform float uDensity;\n" +
        "uniform float uExtraCount;\n" +
        "uniform float4 uExtraShapes[5];\n" +
        "uniform float uExtraRadii[5];\n" +
        "uniform float uMergeK;\n" +
        "uniform float2 uTouch;\n" +
        "uniform float uPress;\n" +
        "uniform float uShadow;\n" +
        "\n" +
        "uniform float uBevel;\n" +
        "uniform float uDepth;\n" +
        "uniform float uCurvature;\n" +
        "uniform float uDispersion;\n" +
        "uniform float uZoom;\n" +
        "uniform float uSaturation;\n" +
        "uniform float uBrightness;\n" +
        "uniform float2 uLight;\n" +
        "uniform float uHighlight;\n" +
        "uniform float4 uTint;\n" +
        "uniform float uDimming;\n" +
        "uniform float uDarkMode;\n" +
        "\n" +
        "\n" +
        "float radiusAt(float2 p, float4 r) {\n" +
        "    float2 rr = (p.x >= 0.0) ? r.yz : r.xw;\n" +
        "    return (p.y >= 0.0) ? rr.y : rr.x;\n" +
        "}\n" +
        "\n" +
        "float sdRoundBox(float2 p, float2 halfSize, float radius) {\n" +
        "    float r = min(radius, min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;\n" +
        "}\n" +
        "\n" +
        "float sdRoundBox4(float2 p, float2 halfSize, float4 radii) {\n" +
        "    float r = min(radiusAt(p, radii), min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;\n" +
        "}\n" +
        "\n" +
        "float lnNorm(float2 v, float n) {\n" +
        "    if (n <= 2.01) return length(v);\n" +
        "    float2 av = max(abs(v), float2(1e-6));\n" +
        "    return pow(pow(av.x, n) + pow(av.y, n), 1.0 / n);\n" +
        "}\n" +
        "\n" +
        "float sdSquircleBox4(float2 p, float2 halfSize, float4 radii, float power) {\n" +
        "    float r = min(radiusAt(p, radii), min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    float2 pos = max(q, float2(0.0));\n" +
        "    float outside = (pos.x > 0.0 || pos.y > 0.0) ? lnNorm(pos, power) : 0.0;\n" +
        "    return min(max(q.x, q.y), 0.0) + outside - r;\n" +
        "}\n" +
        "\n" +
        "float sminPoly(float a, float b, float k) {\n" +
        "    if (k <= 0.5) return min(a, b);\n" +
        "    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);\n" +
        "    return mix(b, a, h) - k * h * (1.0 - h);\n" +
        "}\n" +
        "\n" +
        "float sceneSdRound(float2 local, float2 halfSize) {\n" +
        "    float d = sdRoundBox4(local - halfSize, halfSize, uRadii);\n" +
        "    for (int i = 0; i < 5; i++) {\n" +
        "        if (float(i) >= uExtraCount) break;\n" +
        "        float4 s = uExtraShapes[i];\n" +
        "        float d2 = sdRoundBox(local - s.xy, s.zw, uExtraRadii[i]);\n" +
        "        d = sminPoly(d, d2, uMergeK);\n" +
        "    }\n" +
        "    return d;\n" +
        "}\n" +
        "\n" +
        "float sceneSdSquircle(float2 local, float2 halfSize, float power) {\n" +
        "    float d = sdSquircleBox4(local - halfSize, halfSize, uRadii, power);\n" +
        "    for (int i = 0; i < 5; i++) {\n" +
        "        if (float(i) >= uExtraCount) break;\n" +
        "        float4 s = uExtraShapes[i];\n" +
        "        float d2 = sdSquircleBox4(local - s.xy, s.zw, float4(uExtraRadii[i]), power);\n" +
        "        d = sminPoly(d, d2, uMergeK);\n" +
        "    }\n" +
        "    return d;\n" +
        "}\n" +
        "\n" +
        "float2 sceneNormalRound(float2 local, float2 halfSize, float eps) {\n" +
        "    float2 ex = float2(eps, 0.0);\n" +
        "    float2 ey = float2(0.0, eps);\n" +
        "    float2 g = float2(\n" +
        "        sceneSdRound(local + ex, halfSize) - sceneSdRound(local - ex, halfSize),\n" +
        "        sceneSdRound(local + ey, halfSize) - sceneSdRound(local - ey, halfSize)\n" +
        "    );\n" +
        "    float len = length(g);\n" +
        "    return (len > 1e-5) ? (g / len) : float2(0.0, -1.0);\n" +
        "}\n" +
        "\n" +
        "float2 sceneNormalSquircle(float2 local, float2 halfSize, float power, float eps) {\n" +
        "    float2 ex = float2(eps, 0.0);\n" +
        "    float2 ey = float2(0.0, eps);\n" +
        "    float2 g = float2(\n" +
        "        sceneSdSquircle(local + ex, halfSize, power) - sceneSdSquircle(local - ex, halfSize, power),\n" +
        "        sceneSdSquircle(local + ey, halfSize, power) - sceneSdSquircle(local - ey, halfSize, power)\n" +
        "    );\n" +
        "    float len = length(g);\n" +
        "    return (len > 1e-5) ? (g / len) : float2(0.0, -1.0);\n" +
        "}\n" +
        "\n" +
        "float3 sampleBackdropRgb(float2 coord) {\n" +
        "    float2 q = clamp(coord, uBackdrop.xy, uBackdrop.zw);\n" +
        "    half4 c = content.eval(q);\n" +
        "    return (c.a > half(0.004)) ? float3(c.rgb / c.a) : float3(0.08, 0.08, 0.10);\n" +
        "}\n" +
        "\n" +
        "float luma709(float3 c) {\n" +
        "    return dot(c, float3(0.2126, 0.7152, 0.0722));\n" +
        "}\n" +
        "\n" +
        "half4 dropShadowOutside(float dShadow, float shadowStrength, float density) {\n" +
        "    if (shadowStrength <= 0.005) return half4(0.0);\n" +
        "    float spread = max(14.0 * density, 4.0);\n" +
        "    float alpha = (1.0 - smoothstep(-2.0 * density, spread, dShadow)) * 0.22 * clamp(shadowStrength, 0.0, 2.0);\n" +
        "    return half4(0.0, 0.0, 0.0, half(clamp(alpha, 0.0, 0.65)));\n" +
        "}\n" +
        "\n" +
        "\n" +
        "half4 main(float2 coord) {\n" +
        "    float2 local = coord - float2(uPad);\n" +
        "    float2 halfSize = uSize * 0.5;\n" +
        "    float2 pivot = halfSize + float2(uPad);\n" +
        "\n" +
        "    float sd = sceneSdRound(local, halfSize);\n" +
        "    float cover = clamp(0.5 - sd, 0.0, 1.0);\n" +
        "    if (cover <= 0.0) {\n" +
        "        float dSh = sceneSdRound(local - float2(0.0, 5.0 * uDensity), halfSize);\n" +
        "        return dropShadowOutside(dSh, uShadow, uDensity);\n" +
        "    }\n" +
        "\n" +
        "    float2 dir = sceneNormalRound(local, halfSize, 1.25);\n" +
        "    float inset = max(-sd, 0.0);\n" +
        "    float slope = 1.0 - clamp(inset / max(uBevel, 1.0), 0.0, 1.0);\n" +
        "    float expPow = clamp(1.5 + 1.5 * uCurvature, 1.2, 5.0);\n" +
        "    float2 shift = -dir * (uDepth * pow(max(slope, 1e-5), expPow));\n" +
        "\n" +
        "    // Touch spotlight + gentle liquid swell\n" +
        "    float2 touchDelta = local - uTouch;\n" +
        "    float spot = exp(-dot(touchDelta, touchDelta) / max(halfSize.y * halfSize.y * 6.0, 1.0));\n" +
        "    float touchAmt = clamp(uPress, 0.0, 2.0);\n" +
        "    shift -= touchDelta * (touchAmt * 0.12 * spot);\n" +
        "\n" +
        "    float2 p = coord - pivot;\n" +
        "    float fringe = 2.0 * uDispersion;\n" +
        "    float3 color;\n" +
        "    if (fringe < 0.001 || slope < 0.001) {\n" +
        "        color = sampleBackdropRgb(pivot + (p + shift) * uZoom);\n" +
        "    } else {\n" +
        "        color = float3(\n" +
        "            sampleBackdropRgb(pivot + (p + shift * (1.0 - fringe)) * uZoom).r,\n" +
        "            sampleBackdropRgb(pivot + (p + shift) * uZoom).g,\n" +
        "            sampleBackdropRgb(pivot + (p + shift * (1.0 + fringe)) * uZoom).b\n" +
        "        );\n" +
        "    }\n" +
        "\n" +
        "    // Saturation\n" +
        "    float3 weights = float3(0.2126, 0.7152, 0.0722);\n" +
        "    color = max(mix(float3(dot(color, weights)), color, uSaturation), float3(0.0));\n" +
        "\n" +
        "    float facing = dot(dir, -uLight);\n" +
        "    float rim = 1.0 - smoothstep(0.75 * uDensity, 1.45 * uDensity, inset);\n" +
        "    float edge = rim * (0.11 + (1.0 - 0.11) * abs(facing)) * uHighlight;\n" +
        "\n" +
        "    float veil = uBrightness;\n" +
        "    float luma = min(dot(color, weights), 1.0);\n" +
        "    float dark = (uDarkMode < -0.5) ? smoothstep(0.38, 0.62, 1.0 - luma) : clamp(uDarkMode, 0.0, 1.0);\n" +
        "\n" +
        "    // Exact Vitro iPhone-calibrated pale (light) and deep (dark) glass transfer curves\n" +
        "    float3 pale = mix(color, float3(luma), 0.58 * veil) + float3(veil * (1.32 - 1.34 * luma));\n" +
        "    float lift = 0.19 * pow(max(1.0 - luma, 1e-5), 16.0);\n" +
        "    float shade = 1.0 * pow(max(luma - 0.33, 1e-5), 1.5);\n" +
        "    float3 vivid = max(mix(float3(luma), color, 1.0 + 0.18 * veil), float3(0.0));\n" +
        "    float3 deep = max(vivid * (1.0 + 0.08 * veil) + float3(veil * (lift - shade)), float3(0.0));\n" +
        "\n" +
        "    // Exact Vitro vibrant rim recoloring (brightens & intensifies chroma instead of washing out)\n" +
        "    float3 paleSeen = min(pale, float3(1.0));\n" +
        "    float paleLit = clamp(dot(paleSeen, weights), 0.02, 1.0);\n" +
        "    float3 paleVivid = max(mix(float3(paleLit), paleSeen, 1.0 + 0.20 * edge), float3(0.0));\n" +
        "    pale += paleVivid * (1.0 + 0.91 * edge * (1.0 - paleLit) / paleLit) - paleSeen;\n" +
        "\n" +
        "    float3 deepSeen = min(deep, float3(1.0));\n" +
        "    float deepLit = clamp(dot(deepSeen, weights), 0.02, 1.0);\n" +
        "    float3 deepVivid = max(mix(float3(deepLit), deepSeen, 1.0 + 0.58 * edge), float3(0.0));\n" +
        "    deep += max(deepVivid * (1.0 + edge * (0.35 + 0.12 / deepLit)) - deepSeen, float3(0.0));\n" +
        "\n" +
        "    color = mix(pale, deep, dark);\n" +
        "    color = mix(color, uTint.rgb, clamp(uTint.a, 0.0, 0.90));\n" +
        "    color *= (1.0 - uDimming);\n" +
        "\n" +
        "    // Subtle inner shadow and touch glow\n" +
        "    float innerSh = smoothstep(5.0 * uDensity, 0.0, inset) * (1.0 - rim);\n" +
        "    color -= float3(innerSh * 0.045 * uShadow);\n" +
        "    color = clamp(color + float3(touchAmt * 0.05 * (0.7 + 0.3 * spot)), float3(0.0), float3(1.0));\n" +
        "\n" +
        "    return half4(half3(color * cover), half(cover));\n" +
        "}";

    public static final String HAZE_GLASS =
        "\n" +
        "uniform shader content;\n" +
        "uniform float2 uSize;\n" +
        "uniform float uPad;\n" +
        "uniform float4 uBackdrop;\n" +
        "uniform float4 uRadii;\n" +
        "uniform float uDensity;\n" +
        "uniform float uExtraCount;\n" +
        "uniform float4 uExtraShapes[5];\n" +
        "uniform float uExtraRadii[5];\n" +
        "uniform float uMergeK;\n" +
        "uniform float2 uTouch;\n" +
        "uniform float uPress;\n" +
        "uniform float uShadow;\n" +
        "\n" +
        "uniform float uWideStripY;\n" +
        "uniform float uWideScale;\n" +
        "uniform float uBlurPx;\n" +
        "uniform float uRefractionHeight;\n" +
        "uniform float uRefractionScale;\n" +
        "uniform float uFoldStrength;\n" +
        "uniform float uSurfaceProfile;\n" +
        "uniform float uLensDistortion;\n" +
        "uniform float uChromaMult;\n" +
        "uniform float uAberration;\n" +
        "uniform float uTransparency;\n" +
        "uniform float4 uTint;\n" +
        "uniform float2 uLight;\n" +
        "uniform float uSpecular;\n" +
        "uniform float uEdgeIllum;\n" +
        "uniform float uIsClear;\n" +
        "uniform float uDarkMode;\n" +
        "\n" +
        "\n" +
        "float radiusAt(float2 p, float4 r) {\n" +
        "    float2 rr = (p.x >= 0.0) ? r.yz : r.xw;\n" +
        "    return (p.y >= 0.0) ? rr.y : rr.x;\n" +
        "}\n" +
        "\n" +
        "float sdRoundBox(float2 p, float2 halfSize, float radius) {\n" +
        "    float r = min(radius, min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;\n" +
        "}\n" +
        "\n" +
        "float sdRoundBox4(float2 p, float2 halfSize, float4 radii) {\n" +
        "    float r = min(radiusAt(p, radii), min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;\n" +
        "}\n" +
        "\n" +
        "float lnNorm(float2 v, float n) {\n" +
        "    if (n <= 2.01) return length(v);\n" +
        "    float2 av = max(abs(v), float2(1e-6));\n" +
        "    return pow(pow(av.x, n) + pow(av.y, n), 1.0 / n);\n" +
        "}\n" +
        "\n" +
        "float sdSquircleBox4(float2 p, float2 halfSize, float4 radii, float power) {\n" +
        "    float r = min(radiusAt(p, radii), min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    float2 pos = max(q, float2(0.0));\n" +
        "    float outside = (pos.x > 0.0 || pos.y > 0.0) ? lnNorm(pos, power) : 0.0;\n" +
        "    return min(max(q.x, q.y), 0.0) + outside - r;\n" +
        "}\n" +
        "\n" +
        "float sminPoly(float a, float b, float k) {\n" +
        "    if (k <= 0.5) return min(a, b);\n" +
        "    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);\n" +
        "    return mix(b, a, h) - k * h * (1.0 - h);\n" +
        "}\n" +
        "\n" +
        "float sceneSdRound(float2 local, float2 halfSize) {\n" +
        "    float d = sdRoundBox4(local - halfSize, halfSize, uRadii);\n" +
        "    for (int i = 0; i < 5; i++) {\n" +
        "        if (float(i) >= uExtraCount) break;\n" +
        "        float4 s = uExtraShapes[i];\n" +
        "        float d2 = sdRoundBox(local - s.xy, s.zw, uExtraRadii[i]);\n" +
        "        d = sminPoly(d, d2, uMergeK);\n" +
        "    }\n" +
        "    return d;\n" +
        "}\n" +
        "\n" +
        "float sceneSdSquircle(float2 local, float2 halfSize, float power) {\n" +
        "    float d = sdSquircleBox4(local - halfSize, halfSize, uRadii, power);\n" +
        "    for (int i = 0; i < 5; i++) {\n" +
        "        if (float(i) >= uExtraCount) break;\n" +
        "        float4 s = uExtraShapes[i];\n" +
        "        float d2 = sdSquircleBox4(local - s.xy, s.zw, float4(uExtraRadii[i]), power);\n" +
        "        d = sminPoly(d, d2, uMergeK);\n" +
        "    }\n" +
        "    return d;\n" +
        "}\n" +
        "\n" +
        "float2 sceneNormalRound(float2 local, float2 halfSize, float eps) {\n" +
        "    float2 ex = float2(eps, 0.0);\n" +
        "    float2 ey = float2(0.0, eps);\n" +
        "    float2 g = float2(\n" +
        "        sceneSdRound(local + ex, halfSize) - sceneSdRound(local - ex, halfSize),\n" +
        "        sceneSdRound(local + ey, halfSize) - sceneSdRound(local - ey, halfSize)\n" +
        "    );\n" +
        "    float len = length(g);\n" +
        "    return (len > 1e-5) ? (g / len) : float2(0.0, -1.0);\n" +
        "}\n" +
        "\n" +
        "float2 sceneNormalSquircle(float2 local, float2 halfSize, float power, float eps) {\n" +
        "    float2 ex = float2(eps, 0.0);\n" +
        "    float2 ey = float2(0.0, eps);\n" +
        "    float2 g = float2(\n" +
        "        sceneSdSquircle(local + ex, halfSize, power) - sceneSdSquircle(local - ex, halfSize, power),\n" +
        "        sceneSdSquircle(local + ey, halfSize, power) - sceneSdSquircle(local - ey, halfSize, power)\n" +
        "    );\n" +
        "    float len = length(g);\n" +
        "    return (len > 1e-5) ? (g / len) : float2(0.0, -1.0);\n" +
        "}\n" +
        "\n" +
        "float3 sampleBackdropRgb(float2 coord) {\n" +
        "    float2 q = clamp(coord, uBackdrop.xy, uBackdrop.zw);\n" +
        "    half4 c = content.eval(q);\n" +
        "    return (c.a > half(0.004)) ? float3(c.rgb / c.a) : float3(0.08, 0.08, 0.10);\n" +
        "}\n" +
        "\n" +
        "float luma709(float3 c) {\n" +
        "    return dot(c, float3(0.2126, 0.7152, 0.0722));\n" +
        "}\n" +
        "\n" +
        "half4 dropShadowOutside(float dShadow, float shadowStrength, float density) {\n" +
        "    if (shadowStrength <= 0.005) return half4(0.0);\n" +
        "    float spread = max(14.0 * density, 4.0);\n" +
        "    float alpha = (1.0 - smoothstep(-2.0 * density, spread, dShadow)) * 0.22 * clamp(shadowStrength, 0.0, 2.0);\n" +
        "    return half4(0.0, 0.0, 0.0, half(clamp(alpha, 0.0, 0.65)));\n" +
        "}\n" +
        "\n" +
        "\n" +
        "float smootherstep(float x) {\n" +
        "    float t = clamp(x, 0.0, 1.0);\n" +
        "    return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);\n" +
        "}\n" +
        "\n" +
        "float circleMap(float x) {\n" +
        "    return 1.0 - sqrt(max(0.0, 1.0 - x * x));\n" +
        "}\n" +
        "\n" +
        "float squircleMap(float t) {\n" +
        "    return 1.0 - smootherstep(t * sqrt(max(t, 0.0)));\n" +
        "}\n" +
        "\n" +
        "float evaluateProfile(float t) {\n" +
        "    float x = 1.0 - clamp(t, 0.0, 1.0);\n" +
        "    if (uSurfaceProfile > 2.5) {\n" +
        "        // Lip profile: convex outer rim + concave inner dip\n" +
        "        float convex = circleMap(x);\n" +
        "        float concave = -circleMap(x);\n" +
        "        return mix(convex, concave, smootherstep(clamp(t / 0.7, 0.0, 1.0)));\n" +
        "    } else if (uSurfaceProfile > 1.5) {\n" +
        "        return -circleMap(x);\n" +
        "    } else if (uSurfaceProfile > 0.5) {\n" +
        "        return squircleMap(t);\n" +
        "    }\n" +
        "    return circleMap(x);\n" +
        "}\n" +
        "\n" +
        "float3 sampleBlurredRgb(float2 coord) {\n" +
        "    if (uWideStripY < 1.0 || uBlurPx < 0.75) return sampleBackdropRgb(coord);\n" +
        "    float2 q = clamp(coord, uBackdrop.xy, uBackdrop.zw);\n" +
        "    float2 c = float2(q.x * uWideScale, uWideStripY + q.y * uWideScale);\n" +
        "    half4 s = content.eval(c);\n" +
        "    float3 wide = (s.a > half(0.004)) ? float3(s.rgb / s.a) : sampleBackdropRgb(coord);\n" +
        "    float blend = smoothstep(0.75, 10.0 * uDensity, uBlurPx);\n" +
        "    return mix(sampleBackdropRgb(coord), wide, blend);\n" +
        "}\n" +
        "\n" +
        "float3 toLinearSrgb(float3 c) {\n" +
        "    return pow(max(c, float3(1e-5)), float3(2.2));\n" +
        "}\n" +
        "\n" +
        "float3 fromLinearSrgb(float3 c) {\n" +
        "    return pow(max(c, float3(0.0)), float3(1.0 / 2.2));\n" +
        "}\n" +
        "\n" +
        "// Gamut-preserving linear-sRGB chroma multiplier from chrisbanes/haze GlassShaders.kt\n" +
        "float3 applyHazeColorGrading(float3 color, float chromaMult, float whitePoint) {\n" +
        "    if (abs(chromaMult - 1.0) > 0.001) {\n" +
        "        float3 lin = toLinearSrgb(clamp(color, float3(0.0), float3(1.0)));\n" +
        "        float lum = dot(lin, float3(0.2126, 0.7152, 0.0722));\n" +
        "        float scale = chromaMult;\n" +
        "        if (chromaMult > 1.0) {\n" +
        "            float3 delta = lin - float3(lum);\n" +
        "            if (delta.r > 1e-4) scale = min(scale, (1.0 - lum) / delta.r);\n" +
        "            if (delta.r < -1e-4) scale = min(scale, -lum / delta.r);\n" +
        "            if (delta.g > 1e-4) scale = min(scale, (1.0 - lum) / delta.g);\n" +
        "            if (delta.g < -1e-4) scale = min(scale, -lum / delta.g);\n" +
        "            if (delta.b > 1e-4) scale = min(scale, (1.0 - lum) / delta.b);\n" +
        "            if (delta.b < -1e-4) scale = min(scale, -lum / delta.b);\n" +
        "            scale = max(1.0, scale);\n" +
        "        }\n" +
        "        color = fromLinearSrgb(mix(float3(lum), lin, scale));\n" +
        "    }\n" +
        "    if (abs(whitePoint) > 0.001) {\n" +
        "        float3 target = (whitePoint > 0.0) ? float3(1.0) : float3(0.0);\n" +
        "        color = mix(color, target, abs(whitePoint));\n" +
        "    }\n" +
        "    return clamp(color, float3(0.0), float3(1.0));\n" +
        "}\n" +
        "\n" +
        "half4 main(float2 coord) {\n" +
        "    float2 local = coord - float2(uPad);\n" +
        "    float2 halfSize = uSize * 0.5;\n" +
        "    float2 centered = local - halfSize;\n" +
        "\n" +
        "    float sd = sceneSdSquircle(local, halfSize, 3.6);\n" +
        "    float coverage = clamp(0.5 - sd, 0.0, 1.0);\n" +
        "    if (coverage <= 0.0) {\n" +
        "        float dSh = sceneSdSquircle(local - float2(0.0, 5.0 * uDensity), halfSize, 3.6);\n" +
        "        return dropShadowOutside(dSh, uShadow, uDensity);\n" +
        "    }\n" +
        "\n" +
        "    float distToEdge = max(-sd, 0.0);\n" +
        "\n" +
        "    // Haze dome-blended elongated optical field\n" +
        "    float shortSide = max(min(uSize.x, uSize.y), 1.0);\n" +
        "    float aspect = max(uSize.x, uSize.y) / shortSide;\n" +
        "    float elongation = smootherstep(clamp((aspect - 1.5) / 1.5, 0.0, 1.0));\n" +
        "    float overlap = smootherstep(clamp((uRefractionHeight / (shortSide * 0.5) - 0.75) / 0.25, 0.0, 1.0));\n" +
        "    float fieldWeight = (uExtraCount > 0.5) ? 0.0 : (elongation * overlap);\n" +
        "\n" +
        "    float2 normCoord = centered / max(halfSize, float2(1.0));\n" +
        "    float2 sq = normCoord * normCoord;\n" +
        "    float domeRadius = sqrt(clamp(sq.x + sq.y - sq.x * sq.y, 0.0, 1.0));\n" +
        "    float domeDist = max((1.0 - domeRadius) * uRefractionHeight, 0.0);\n" +
        "    float opticalDist = mix(distToEdge, domeDist, fieldWeight);\n" +
        "\n" +
        "    float2 boundaryGrad = sceneNormalSquircle(local, halfSize, 3.6, 1.5);\n" +
        "    float2 domeGrad = float2(normCoord.x * (1.0 - sq.y), normCoord.y * (1.0 - sq.x));\n" +
        "    float2 optGrad = mix(boundaryGrad, domeGrad, fieldWeight);\n" +
        "    float gLen = length(optGrad);\n" +
        "    float2 dispGrad = (gLen > 1e-4) ? (optGrad / gLen) : boundaryGrad;\n" +
        "\n" +
        "    float t = clamp(opticalDist / max(uRefractionHeight, 1.0), 0.0, 1.0);\n" +
        "    float heightNorm;\n" +
        "    if (uIsClear > 0.5) {\n" +
        "        // Empirical Clear fit from Haze GlassShaders.kt: exp(-t/0.225)*(1-t^2)^2\n" +
        "        float cutoff = max(1.0 - t * t, 0.0);\n" +
        "        heightNorm = exp(-t / 0.225) * cutoff * cutoff;\n" +
        "    } else {\n" +
        "        heightNorm = evaluateProfile(t);\n" +
        "    }\n" +
        "\n" +
        "    // Haze refraction fold envelope\n" +
        "    float foldRise = smootherstep(clamp(t / 0.2, 0.0, 1.0));\n" +
        "    float foldFall = 1.0 - smootherstep(clamp((t - 0.25) / 0.35, 0.0, 1.0));\n" +
        "    float foldW = clamp(uFoldStrength * foldRise * foldFall, 0.0, 1.0);\n" +
        "    float effHeight = mix(heightNorm, abs(heightNorm) * 0.02, foldW);\n" +
        "\n" +
        "    // Touch localized refraction multiplier\n" +
        "    float2 touchDelta = local - uTouch;\n" +
        "    float touchR = max(shortSide * 0.6, 1.0);\n" +
        "    float touchW = exp(-dot(touchDelta, touchDelta) / (touchR * touchR)) * clamp(uPress, 0.0, 2.0);\n" +
        "    float2 disp = -dispGrad * (effHeight * uRefractionScale * (1.0 + 0.35 * touchW));\n" +
        "    disp -= centered * (uLensDistortion * 0.12 * (1.0 - domeRadius));\n" +
        "    disp -= touchDelta * (touchW * 0.14);\n" +
        "\n" +
        "    float2 refractCoord = coord + disp;\n" +
        "    float cornerW = clamp(abs(normCoord.x * normCoord.y) * 1.4 + 0.25, 0.0, 1.0);\n" +
        "    float2 chromaOff = disp * (uAberration * cornerW);\n" +
        "\n" +
        "    // Sample blurred body + sharp-rim detail pass (Haze two-detail fusion)\n" +
        "    float3 bodyRgb;\n" +
        "    if (length(chromaOff) > 0.05) {\n" +
        "        bodyRgb = float3(\n" +
        "            sampleBlurredRgb(refractCoord + chromaOff).r,\n" +
        "            sampleBlurredRgb(refractCoord).g,\n" +
        "            sampleBlurredRgb(refractCoord - chromaOff).b\n" +
        "        );\n" +
        "    } else {\n" +
        "        bodyRgb = sampleBlurredRgb(refractCoord);\n" +
        "    }\n" +
        "\n" +
        "    float3 sharpRimRgb = float3(\n" +
        "        sampleBackdropRgb(refractCoord + chromaOff).r,\n" +
        "        sampleBackdropRgb(refractCoord).g,\n" +
        "        sampleBackdropRgb(refractCoord - chromaOff).b\n" +
        "    );\n" +
        "    float rimDetailMask = (1.0 - smootherstep(t)) * mix(0.55, 0.95, uIsClear);\n" +
        "    float3 rgb = mix(bodyRgb, sharpRimRgb, rimDetailMask);\n" +
        "\n" +
        "    // Content-normal ambient Fresnel (Haze GlassShaders.kt computeContentNormal)\n" +
        "    float lC = luma709(rgb);\n" +
        "    float lR = luma709(sampleBackdropRgb(refractCoord + float2(2.0, 0.0)));\n" +
        "    float lD = luma709(sampleBackdropRgb(refractCoord + float2(0.0, 2.0)));\n" +
        "    float3 contentNormal = normalize(float3((lR - lC) * 1.5, (lD - lC) * 1.5, 1.0));\n" +
        "    float3 shapeNormal = normalize(float3(-boundaryGrad.x * (1.0 - t), -boundaryGrad.y * (1.0 - t), 1.0));\n" +
        "    float3 finalNormal = normalize(mix(shapeNormal, contentNormal, 0.22));\n" +
        "    float fresnel = pow(max(1.0 - max(finalNormal.z, 0.0), 0.0), 3.0);\n" +
        "\n" +
        "    float darkFactor = (uDarkMode < -0.5) ? smoothstep(0.40, 0.62, 1.0 - lC) : clamp(uDarkMode, 0.0, 1.0);\n" +
        "    float whitePt = mix(0.14, -0.14, darkFactor) * (1.0 - uTransparency * 0.6) * (1.0 - uIsClear * 0.85);\n" +
        "    rgb = applyHazeColorGrading(rgb, uChromaMult, whitePt);\n" +
        "\n" +
        "    float tintAlpha = clamp(uTint.a * (1.0 - uTransparency * 0.45) * mix(1.0, 0.30, uIsClear), 0.0, 0.85);\n" +
        "    rgb = mix(rgb, uTint.rgb, tintAlpha);\n" +
        "    rgb *= (1.0 + fresnel * 0.35 * uEdgeIllum);\n" +
        "\n" +
        "    // Directional rim highlight + iOS 27 darkened edge contour\n" +
        "    float rimBand = smoothstep(2.4 * uDensity, 0.0, distToEdge);\n" +
        "    float lightFacing = dot(boundaryGrad, -uLight);\n" +
        "    float spec = (pow(max(lightFacing, 0.0), 3.0) + 0.55 * pow(max(-lightFacing, 0.0), 3.5)) * rimBand * 0.52 * uSpecular;\n" +
        "    float darkEdge = smoothstep(1.8 * uDensity, 0.0, distToEdge) * (1.0 - abs(lightFacing)) * 0.16 * uEdgeIllum;\n" +
        "    float innerSh = smoothstep(5.5 * uDensity, 0.0, distToEdge) * (1.0 - rimBand) * 0.05 * uShadow;\n" +
        "\n" +
        "    rgb = rgb + float3(spec + touchW * 0.06) - float3(darkEdge + innerSh);\n" +
        "    rgb = clamp(rgb, float3(0.0), float3(1.0));\n" +
        "\n" +
        "    return half4(half3(rgb * coverage), half(coverage));\n" +
        "}";

    public static final String SOHUM_SNELL =
        "\n" +
        "uniform shader content;\n" +
        "uniform float2 uSize;\n" +
        "uniform float uPad;\n" +
        "uniform float4 uBackdrop;\n" +
        "uniform float4 uRadii;\n" +
        "uniform float uDensity;\n" +
        "uniform float uExtraCount;\n" +
        "uniform float4 uExtraShapes[5];\n" +
        "uniform float uExtraRadii[5];\n" +
        "uniform float uMergeK;\n" +
        "uniform float2 uTouch;\n" +
        "uniform float uPress;\n" +
        "uniform float uShadow;\n" +
        "\n" +
        "uniform float uBezelPx;\n" +
        "uniform float uRefractScale;\n" +
        "uniform float uProfilePower;\n" +
        "uniform float uBaseT;\n" +
        "uniform float uDispersionStrength;\n" +
        "uniform float uTransparency;\n" +
        "uniform float4 uTint;\n" +
        "uniform float2 uLight;\n" +
        "uniform float uSpecular;\n" +
        "uniform float uEdgeIllum;\n" +
        "uniform float uIsClear;\n" +
        "uniform float uDarkMode;\n" +
        "\n" +
        "\n" +
        "float radiusAt(float2 p, float4 r) {\n" +
        "    float2 rr = (p.x >= 0.0) ? r.yz : r.xw;\n" +
        "    return (p.y >= 0.0) ? rr.y : rr.x;\n" +
        "}\n" +
        "\n" +
        "float sdRoundBox(float2 p, float2 halfSize, float radius) {\n" +
        "    float r = min(radius, min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;\n" +
        "}\n" +
        "\n" +
        "float sdRoundBox4(float2 p, float2 halfSize, float4 radii) {\n" +
        "    float r = min(radiusAt(p, radii), min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;\n" +
        "}\n" +
        "\n" +
        "float lnNorm(float2 v, float n) {\n" +
        "    if (n <= 2.01) return length(v);\n" +
        "    float2 av = max(abs(v), float2(1e-6));\n" +
        "    return pow(pow(av.x, n) + pow(av.y, n), 1.0 / n);\n" +
        "}\n" +
        "\n" +
        "float sdSquircleBox4(float2 p, float2 halfSize, float4 radii, float power) {\n" +
        "    float r = min(radiusAt(p, radii), min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    float2 pos = max(q, float2(0.0));\n" +
        "    float outside = (pos.x > 0.0 || pos.y > 0.0) ? lnNorm(pos, power) : 0.0;\n" +
        "    return min(max(q.x, q.y), 0.0) + outside - r;\n" +
        "}\n" +
        "\n" +
        "float sminPoly(float a, float b, float k) {\n" +
        "    if (k <= 0.5) return min(a, b);\n" +
        "    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);\n" +
        "    return mix(b, a, h) - k * h * (1.0 - h);\n" +
        "}\n" +
        "\n" +
        "float sceneSdRound(float2 local, float2 halfSize) {\n" +
        "    float d = sdRoundBox4(local - halfSize, halfSize, uRadii);\n" +
        "    for (int i = 0; i < 5; i++) {\n" +
        "        if (float(i) >= uExtraCount) break;\n" +
        "        float4 s = uExtraShapes[i];\n" +
        "        float d2 = sdRoundBox(local - s.xy, s.zw, uExtraRadii[i]);\n" +
        "        d = sminPoly(d, d2, uMergeK);\n" +
        "    }\n" +
        "    return d;\n" +
        "}\n" +
        "\n" +
        "float sceneSdSquircle(float2 local, float2 halfSize, float power) {\n" +
        "    float d = sdSquircleBox4(local - halfSize, halfSize, uRadii, power);\n" +
        "    for (int i = 0; i < 5; i++) {\n" +
        "        if (float(i) >= uExtraCount) break;\n" +
        "        float4 s = uExtraShapes[i];\n" +
        "        float d2 = sdSquircleBox4(local - s.xy, s.zw, float4(uExtraRadii[i]), power);\n" +
        "        d = sminPoly(d, d2, uMergeK);\n" +
        "    }\n" +
        "    return d;\n" +
        "}\n" +
        "\n" +
        "float2 sceneNormalRound(float2 local, float2 halfSize, float eps) {\n" +
        "    float2 ex = float2(eps, 0.0);\n" +
        "    float2 ey = float2(0.0, eps);\n" +
        "    float2 g = float2(\n" +
        "        sceneSdRound(local + ex, halfSize) - sceneSdRound(local - ex, halfSize),\n" +
        "        sceneSdRound(local + ey, halfSize) - sceneSdRound(local - ey, halfSize)\n" +
        "    );\n" +
        "    float len = length(g);\n" +
        "    return (len > 1e-5) ? (g / len) : float2(0.0, -1.0);\n" +
        "}\n" +
        "\n" +
        "float2 sceneNormalSquircle(float2 local, float2 halfSize, float power, float eps) {\n" +
        "    float2 ex = float2(eps, 0.0);\n" +
        "    float2 ey = float2(0.0, eps);\n" +
        "    float2 g = float2(\n" +
        "        sceneSdSquircle(local + ex, halfSize, power) - sceneSdSquircle(local - ex, halfSize, power),\n" +
        "        sceneSdSquircle(local + ey, halfSize, power) - sceneSdSquircle(local - ey, halfSize, power)\n" +
        "    );\n" +
        "    float len = length(g);\n" +
        "    return (len > 1e-5) ? (g / len) : float2(0.0, -1.0);\n" +
        "}\n" +
        "\n" +
        "float3 sampleBackdropRgb(float2 coord) {\n" +
        "    float2 q = clamp(coord, uBackdrop.xy, uBackdrop.zw);\n" +
        "    half4 c = content.eval(q);\n" +
        "    return (c.a > half(0.004)) ? float3(c.rgb / c.a) : float3(0.08, 0.08, 0.10);\n" +
        "}\n" +
        "\n" +
        "float luma709(float3 c) {\n" +
        "    return dot(c, float3(0.2126, 0.7152, 0.0722));\n" +
        "}\n" +
        "\n" +
        "half4 dropShadowOutside(float dShadow, float shadowStrength, float density) {\n" +
        "    if (shadowStrength <= 0.005) return half4(0.0);\n" +
        "    float spread = max(14.0 * density, 4.0);\n" +
        "    float alpha = (1.0 - smoothstep(-2.0 * density, spread, dShadow)) * 0.22 * clamp(shadowStrength, 0.0, 2.0);\n" +
        "    return half4(0.0, 0.0, 0.0, half(clamp(alpha, 0.0, 0.65)));\n" +
        "}\n" +
        "\n" +
        "\n" +
        "// BK7 crown glass Fraunhofer indices (from sohumsuthar/liquid-glass glass-optics.mjs)\n" +
        "const float N_D = 1.5168;\n" +
        "const float N_C = 1.5143;\n" +
        "const float N_F = 1.5224;\n" +
        "\n" +
        "float squircleHeight(float x, float p) {\n" +
        "    float u = 1.0 - clamp(x, 0.0, 1.0);\n" +
        "    return pow(max(1.0 - pow(max(u, 1e-5), p), 1e-5), 1.0 / p);\n" +
        "}\n" +
        "\n" +
        "float squircleSlope(float x, float p) {\n" +
        "    float u = 1.0 - clamp(x, 0.004, 0.996);\n" +
        "    float up = pow(u, p);\n" +
        "    return pow(u, p - 1.0) / pow(max(1.0 - up, 1e-4), 1.0 - 1.0 / p);\n" +
        "}\n" +
        "\n" +
        "// Exact unpolarized Fresnel power reflectance R(theta_i)\n" +
        "float fresnelReflectance(float thetaI, float n) {\n" +
        "    float ci = cos(thetaI);\n" +
        "    float st = sin(thetaI) / n;\n" +
        "    if (st >= 0.999) return 1.0;\n" +
        "    float ct = sqrt(max(1.0 - st * st, 0.0));\n" +
        "    float rs = (ci - n * ct) / max(ci + n * ct, 1e-5);\n" +
        "    float rp = (n * ci - ct) / max(n * ci + ct, 1e-5);\n" +
        "    return 0.5 * (rs * rs + rp * rp);\n" +
        "}\n" +
        "\n" +
        "half4 main(float2 coord) {\n" +
        "    float2 local = coord - float2(uPad);\n" +
        "    float2 halfSize = uSize * 0.5;\n" +
        "\n" +
        "    float sd = sceneSdSquircle(local, halfSize, 4.0);\n" +
        "    float coverage = clamp(0.5 - sd, 0.0, 1.0);\n" +
        "    if (coverage <= 0.0) {\n" +
        "        float dSh = sceneSdSquircle(local - float2(0.0, 5.0 * uDensity), halfSize, 4.0);\n" +
        "        return dropShadowOutside(dSh, uShadow, uDensity);\n" +
        "    }\n" +
        "\n" +
        "    float dist = max(-sd, 0.0);\n" +
        "    float2 nrm = sceneNormalSquircle(local, halfSize, 4.0, 1.0);\n" +
        "\n" +
        "    float2 disp = float2(0.0);\n" +
        "    float fresnelRim = 0.04;\n" +
        "    if (dist < uBezelPx) {\n" +
        "        float x = clamp(dist / max(uBezelPx, 1.0), 0.004, 0.996);\n" +
        "        float slope = squircleSlope(x, uProfilePower);\n" +
        "        float thetaS = atan(slope);\n" +
        "        float sinR = clamp(sin(thetaS) / N_D, -0.999, 0.999);\n" +
        "        float thetaR = asin(sinR);\n" +
        "        float t = uBaseT + squircleHeight(x, uProfilePower);\n" +
        "        float refl = fresnelReflectance(thetaS, N_D);\n" +
        "        fresnelRim = refl;\n" +
        "        float d = t * tan(thetaS - thetaR) * (1.0 - refl);\n" +
        "        // Convex glass shifts background sample point inward along SDF normal\n" +
        "        disp = -nrm * (d * uBezelPx * uRefractScale);\n" +
        "    }\n" +
        "\n" +
        "    // Touch deformation lens bulge\n" +
        "    float2 touchDelta = local - uTouch;\n" +
        "    float touchSigma = max(min(uSize.x, uSize.y), 1.0);\n" +
        "    float touchFall = exp(-dot(touchDelta, touchDelta) / (touchSigma * touchSigma)) * clamp(uPress, 0.0, 2.0);\n" +
        "    disp -= touchDelta * (touchFall * 0.14);\n" +
        "\n" +
        "    // Thin-prism BK7 chromatic dispersion ratios\n" +
        "    float rRatio = 1.0 + (((N_C - 1.0) / (N_D - 1.0)) - 1.0) * uDispersionStrength;\n" +
        "    float bRatio = 1.0 + (((N_F - 1.0) / (N_D - 1.0)) - 1.0) * uDispersionStrength;\n" +
        "\n" +
        "    float3 rgb;\n" +
        "    if (uDispersionStrength > 0.05 && length(disp) > 0.1) {\n" +
        "        rgb = float3(\n" +
        "            sampleBackdropRgb(coord + disp * rRatio).r,\n" +
        "            sampleBackdropRgb(coord + disp).g,\n" +
        "            sampleBackdropRgb(coord + disp * bRatio).b\n" +
        "        );\n" +
        "    } else {\n" +
        "        rgb = sampleBackdropRgb(coord + disp);\n" +
        "    }\n" +
        "\n" +
        "    // Measured macOS 26 Control Center linear compressive transfer curve:\n" +
        "    // L_glass = (1 - alpha) * beta * L_backdrop + alpha, with analytical chroma restoration S = 1 / ((1 - alpha) * beta)\n" +
        "    float luma = luma709(rgb);\n" +
        "    float darkFactor = (uDarkMode < -0.5) ? smoothstep(0.42, 0.62, 1.0 - luma) : clamp(uDarkMode, 0.0, 1.0);\n" +
        "\n" +
        "    float alpha = clamp(uTint.a * (1.0 - uTransparency * 0.35) * mix(1.0, 0.25, uIsClear), 0.01, 0.65);\n" +
        "    float beta = mix(0.67, 0.94, uIsClear) * mix(0.82, 1.08, uTransparency);\n" +
        "    float slopeComp = clamp((1.0 - alpha) * beta, 0.25, 1.0);\n" +
        "    float satRestore = clamp(1.0 / slopeComp, 1.0, 1.95);\n" +
        "\n" +
        "    // Restore chroma first, then apply measured compressive transfer\n" +
        "    rgb = clamp(mix(float3(luma), rgb, satRestore), float3(0.0), float3(1.0));\n" +
        "    float3 scrimColor = mix(uTint.rgb, float3(0.18, 0.19, 0.22), darkFactor * 0.65);\n" +
        "    rgb = rgb * slopeComp + scrimColor * alpha;\n" +
        "\n" +
        "    // Fresnel-shaped directional specular rim ring\n" +
        "    float rimMask = 1.0 - smoothstep(0.0, max(uBezelPx * 0.35, 2.5 * uDensity), dist);\n" +
        "    float facing = dot(nrm, -uLight);\n" +
        "    float dirLobe = pow(max(facing, 0.0), 3.2) + 0.55 * pow(max(-facing, 0.0), 3.8);\n" +
        "    float rimSpec = (fresnelRim * 0.65 * uEdgeIllum + dirLobe * 0.55 * uSpecular) * rimMask;\n" +
        "    float innerEdge = smoothstep(2.0 * uDensity, 0.0, dist) * (0.28 * uEdgeIllum + 0.35 * dirLobe * uSpecular);\n" +
        "\n" +
        "    // Subtle inner depth shadow\n" +
        "    float innerSh = smoothstep(6.0 * uDensity, 0.0, dist) * (1.0 - smoothstep(2.0 * uDensity, 0.0, dist)) * 0.06 * uShadow;\n" +
        "\n" +
        "    rgb = clamp(rgb + float3(rimSpec + innerEdge + touchFall * 0.06) - float3(innerSh), float3(0.0), float3(1.0));\n" +
        "    return half4(half3(rgb * coverage), half(coverage));\n" +
        "}";

    public static final String MOCHI_G2 =
        "\n" +
        "uniform shader content;\n" +
        "uniform float2 uSize;\n" +
        "uniform float uPad;\n" +
        "uniform float4 uBackdrop;\n" +
        "uniform float4 uRadii;\n" +
        "uniform float uDensity;\n" +
        "uniform float uExtraCount;\n" +
        "uniform float4 uExtraShapes[5];\n" +
        "uniform float uExtraRadii[5];\n" +
        "uniform float uMergeK;\n" +
        "uniform float2 uTouch;\n" +
        "uniform float uPress;\n" +
        "uniform float uShadow;\n" +
        "\n" +
        "uniform float uWideStripY;\n" +
        "uniform float uWideScale;\n" +
        "uniform float uBlurPx;\n" +
        "uniform float uInnerAmt;\n" +
        "uniform float uInnerInvH;\n" +
        "uniform float uOuterAmt;\n" +
        "uniform float uCurvature;\n" +
        "uniform float uLensZoom;\n" +
        "uniform float uAberration;\n" +
        "uniform float uTransparency;\n" +
        "uniform float4 uTint;\n" +
        "uniform float2 uLight;\n" +
        "uniform float uSpecular;\n" +
        "uniform float uEdgeIllum;\n" +
        "uniform float uIsClear;\n" +
        "uniform float uDarkMode;\n" +
        "\n" +
        "\n" +
        "float radiusAt(float2 p, float4 r) {\n" +
        "    float2 rr = (p.x >= 0.0) ? r.yz : r.xw;\n" +
        "    return (p.y >= 0.0) ? rr.y : rr.x;\n" +
        "}\n" +
        "\n" +
        "float sdRoundBox(float2 p, float2 halfSize, float radius) {\n" +
        "    float r = min(radius, min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;\n" +
        "}\n" +
        "\n" +
        "float sdRoundBox4(float2 p, float2 halfSize, float4 radii) {\n" +
        "    float r = min(radiusAt(p, radii), min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;\n" +
        "}\n" +
        "\n" +
        "float lnNorm(float2 v, float n) {\n" +
        "    if (n <= 2.01) return length(v);\n" +
        "    float2 av = max(abs(v), float2(1e-6));\n" +
        "    return pow(pow(av.x, n) + pow(av.y, n), 1.0 / n);\n" +
        "}\n" +
        "\n" +
        "float sdSquircleBox4(float2 p, float2 halfSize, float4 radii, float power) {\n" +
        "    float r = min(radiusAt(p, radii), min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    float2 pos = max(q, float2(0.0));\n" +
        "    float outside = (pos.x > 0.0 || pos.y > 0.0) ? lnNorm(pos, power) : 0.0;\n" +
        "    return min(max(q.x, q.y), 0.0) + outside - r;\n" +
        "}\n" +
        "\n" +
        "float sminPoly(float a, float b, float k) {\n" +
        "    if (k <= 0.5) return min(a, b);\n" +
        "    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);\n" +
        "    return mix(b, a, h) - k * h * (1.0 - h);\n" +
        "}\n" +
        "\n" +
        "float sceneSdRound(float2 local, float2 halfSize) {\n" +
        "    float d = sdRoundBox4(local - halfSize, halfSize, uRadii);\n" +
        "    for (int i = 0; i < 5; i++) {\n" +
        "        if (float(i) >= uExtraCount) break;\n" +
        "        float4 s = uExtraShapes[i];\n" +
        "        float d2 = sdRoundBox(local - s.xy, s.zw, uExtraRadii[i]);\n" +
        "        d = sminPoly(d, d2, uMergeK);\n" +
        "    }\n" +
        "    return d;\n" +
        "}\n" +
        "\n" +
        "float sceneSdSquircle(float2 local, float2 halfSize, float power) {\n" +
        "    float d = sdSquircleBox4(local - halfSize, halfSize, uRadii, power);\n" +
        "    for (int i = 0; i < 5; i++) {\n" +
        "        if (float(i) >= uExtraCount) break;\n" +
        "        float4 s = uExtraShapes[i];\n" +
        "        float d2 = sdSquircleBox4(local - s.xy, s.zw, float4(uExtraRadii[i]), power);\n" +
        "        d = sminPoly(d, d2, uMergeK);\n" +
        "    }\n" +
        "    return d;\n" +
        "}\n" +
        "\n" +
        "float2 sceneNormalRound(float2 local, float2 halfSize, float eps) {\n" +
        "    float2 ex = float2(eps, 0.0);\n" +
        "    float2 ey = float2(0.0, eps);\n" +
        "    float2 g = float2(\n" +
        "        sceneSdRound(local + ex, halfSize) - sceneSdRound(local - ex, halfSize),\n" +
        "        sceneSdRound(local + ey, halfSize) - sceneSdRound(local - ey, halfSize)\n" +
        "    );\n" +
        "    float len = length(g);\n" +
        "    return (len > 1e-5) ? (g / len) : float2(0.0, -1.0);\n" +
        "}\n" +
        "\n" +
        "float2 sceneNormalSquircle(float2 local, float2 halfSize, float power, float eps) {\n" +
        "    float2 ex = float2(eps, 0.0);\n" +
        "    float2 ey = float2(0.0, eps);\n" +
        "    float2 g = float2(\n" +
        "        sceneSdSquircle(local + ex, halfSize, power) - sceneSdSquircle(local - ex, halfSize, power),\n" +
        "        sceneSdSquircle(local + ey, halfSize, power) - sceneSdSquircle(local - ey, halfSize, power)\n" +
        "    );\n" +
        "    float len = length(g);\n" +
        "    return (len > 1e-5) ? (g / len) : float2(0.0, -1.0);\n" +
        "}\n" +
        "\n" +
        "float3 sampleBackdropRgb(float2 coord) {\n" +
        "    float2 q = clamp(coord, uBackdrop.xy, uBackdrop.zw);\n" +
        "    half4 c = content.eval(q);\n" +
        "    return (c.a > half(0.004)) ? float3(c.rgb / c.a) : float3(0.08, 0.08, 0.10);\n" +
        "}\n" +
        "\n" +
        "float luma709(float3 c) {\n" +
        "    return dot(c, float3(0.2126, 0.7152, 0.0722));\n" +
        "}\n" +
        "\n" +
        "half4 dropShadowOutside(float dShadow, float shadowStrength, float density) {\n" +
        "    if (shadowStrength <= 0.005) return half4(0.0);\n" +
        "    float spread = max(14.0 * density, 4.0);\n" +
        "    float alpha = (1.0 - smoothstep(-2.0 * density, spread, dShadow)) * 0.22 * clamp(shadowStrength, 0.0, 2.0);\n" +
        "    return half4(0.0, 0.0, 0.0, half(clamp(alpha, 0.0, 0.65)));\n" +
        "}\n" +
        "\n" +
        "\n" +
        "// Exact Apple G2 continuous supercircle SDF from feice11/mochi-compose LiquidShaders.kt\n" +
        "float3 mochiSupercircle(float2 p, float2 b, float r) {\n" +
        "    r = clamp(abs(r), 0.5, min(b.x, b.y));\n" +
        "    float R = min(r * 1.5286649465560913, min(b.x, b.y));\n" +
        "    float2 q = p - b + float2(R);\n" +
        "    float2 u = abs(max(float2(0.0), q / max(R, 1e-4)));\n" +
        "    float L = length(u);\n" +
        "    float umax = max(u.x, u.y);\n" +
        "    float umin = min(u.x, u.y);\n" +
        "    float t = (umax <= 1e-5) ? 0.0 : clamp(umin / umax, 0.0, 1.0);\n" +
        "    float poly = (((-0.9260540008544922 * t + 3.1560099124908447) * t - 3.6412200927734375) * t + 1.268030047416687) * t + 0.2685309946537018;\n" +
        "    float denom = max(1.0 - t * t * clamp(L, 0.0, 1.0) * poly, 0.05);\n" +
        "    float f = (L + 1.0 - 1.0 / denom) - 1.0;\n" +
        "    float2 m = max(float2(0.0), q);\n" +
        "    float mm = dot(m, m);\n" +
        "    float2 n = (mm > 1e-6) ? (m * inversesqrt(mm)) : ((q.x > q.y) ? float2(1.0, 0.0) : float2(0.0, 1.0));\n" +
        "    return float3(min(max(q.x, q.y), 0.0) + R * f, n);\n" +
        "}\n" +
        "\n" +
        "float mochiGroupD(float2 local, float2 halfSize) {\n" +
        "    float2 p0 = local - halfSize;\n" +
        "    float r0 = radiusAt(p0, uRadii);\n" +
        "    float d = mochiSupercircle(abs(p0), halfSize, r0).x;\n" +
        "    for (int i = 0; i < 5; i++) {\n" +
        "        if (float(i) >= uExtraCount) break;\n" +
        "        float4 s = uExtraShapes[i];\n" +
        "        float e = mochiSupercircle(abs(local - s.xy), s.zw, uExtraRadii[i]).x;\n" +
        "        d = sminPoly(d, e, uMergeK);\n" +
        "    }\n" +
        "    return d;\n" +
        "}\n" +
        "\n" +
        "// Mochi bevel profile: amt - sqrt((2 - t) * t) * amt\n" +
        "float mochiBevel(float amt, float invH, float d) {\n" +
        "    float t = clamp(invH * (-d), 0.0, 1.0);\n" +
        "    float shaped = pow(max(t, 1e-5), clamp(1.0 / max(uCurvature, 0.35), 0.4, 2.2));\n" +
        "    return amt - clamp(sqrt(max((2.0 - shaped) * shaped, 0.0)), 0.0, 1.0) * amt;\n" +
        "}\n" +
        "\n" +
        "// Mochi Gaussian erfp approximation for inner ring shadow\n" +
        "float erfp(float x) {\n" +
        "    float s = clamp(x * 0.25 + 0.5, 0.0, 1.0) * 4.0 - 2.0;\n" +
        "    float s2 = s * s;\n" +
        "    return (((0.0029544830322265625 * s2 - 0.034454345703125) * s2 + 0.168212890625) * s2 - 0.560546875) * s + 0.5;\n" +
        "}\n" +
        "\n" +
        "float3 sampleMochiMip(float2 coord, float blurAmt) {\n" +
        "    float3 sharp = sampleBackdropRgb(coord);\n" +
        "    if (uWideStripY < 1.0 || blurAmt < 0.5) return sharp;\n" +
        "    float2 q = clamp(coord, uBackdrop.xy, uBackdrop.zw);\n" +
        "    float2 c = float2(q.x * uWideScale, uWideStripY + q.y * uWideScale);\n" +
        "    half4 s = content.eval(c);\n" +
        "    float3 wide = (s.a > half(0.004)) ? float3(s.rgb / s.a) : sharp;\n" +
        "    float w = smoothstep(0.5, 12.0 * uDensity, blurAmt);\n" +
        "    return mix(sharp, wide, w);\n" +
        "}\n" +
        "\n" +
        "half4 main(float2 coord) {\n" +
        "    float2 local = coord - float2(uPad);\n" +
        "    float2 halfSize = uSize * 0.5;\n" +
        "\n" +
        "    float d = mochiGroupD(local, halfSize);\n" +
        "    float2 g = float2(\n" +
        "        mochiGroupD(local + float2(0.75, 0.0), halfSize) - mochiGroupD(local - float2(0.75, 0.0), halfSize),\n" +
        "        mochiGroupD(local + float2(0.0, 0.75), halfSize) - mochiGroupD(local - float2(0.0, 0.75), halfSize)\n" +
        "    );\n" +
        "    float gLen = length(g);\n" +
        "    float2 n = (gLen > 1e-5) ? (g / gLen) : float2(0.0, -1.0);\n" +
        "    // Neck distance compensation from Mochi LiquidShaders.kt\n" +
        "    if (uExtraCount > 0.5) {\n" +
        "        d /= clamp(gLen / 1.5, 0.35, 1.0);\n" +
        "    }\n" +
        "\n" +
        "    float cov = clamp(0.5 - d, 0.0, 1.0);\n" +
        "    if (cov <= 0.0) {\n" +
        "        float dSh = mochiGroupD(local - float2(0.0, 5.5 * uDensity), halfSize);\n" +
        "        return dropShadowOutside(dSh, uShadow, uDensity);\n" +
        "    }\n" +
        "\n" +
        "    float depth = max(-d, 0.0);\n" +
        "    float dispInner = mochiBevel(uInnerAmt, uInnerInvH, d);\n" +
        "    float dispOuter = mochiBevel(uOuterAmt, uInnerInvH * 2.2, d);\n" +
        "\n" +
        "    // Touch radial bulge + press glow\n" +
        "    float2 touchDelta = local - uTouch;\n" +
        "    float touchSigma = max(min(uSize.x, uSize.y) * 0.75, 1.0);\n" +
        "    float touchFall = exp(-dot(touchDelta, touchDelta) / (touchSigma * touchSigma)) * clamp(uPress, 0.0, 2.0);\n" +
        "\n" +
        "    float2 baseDisp = -n * (dispInner * (1.0 + 0.25 * touchFall))\n" +
        "        - (local - halfSize) * (uLensZoom * 0.12)\n" +
        "        - touchDelta * (touchFall * 0.14);\n" +
        "\n" +
        "    // Distance-dependent blur (crisp near rim, softer interior)\n" +
        "    float rimT = clamp(depth * uInnerInvH, 0.0, 1.0);\n" +
        "    float localBlur = uBlurPx * mix(0.22, 1.0, rimT);\n" +
        "\n" +
        "    float3 c;\n" +
        "    if (uAberration > 0.005 && dispInner > 0.1) {\n" +
        "        float2 ca = n * (dispInner * uAberration);\n" +
        "        c = float3(\n" +
        "            sampleMochiMip(coord + baseDisp - ca, localBlur).r,\n" +
        "            sampleMochiMip(coord + baseDisp, localBlur).g,\n" +
        "            sampleMochiMip(coord + baseDisp + ca, localBlur).b\n" +
        "        );\n" +
        "    } else {\n" +
        "        c = sampleMochiMip(coord + baseDisp, localBlur);\n" +
        "    }\n" +
        "\n" +
        "    // Outer edge counter-refraction blend (Mochi d > -1.0pt outer refraction stage)\n" +
        "    float outerBlend = (1.0 - smoothstep(0.0, 3.0 * uDensity, depth)) * 0.35;\n" +
        "    if (outerBlend > 0.01) {\n" +
        "        float3 cOuter = sampleBackdropRgb(coord + n * dispOuter);\n" +
        "        c = mix(c, cOuter, outerBlend);\n" +
        "    }\n" +
        "\n" +
        "    // Luma compression + face color transfer\n" +
        "    float L = luma709(c);\n" +
        "    float darkFactor = (uDarkMode < -0.5) ? smoothstep(0.42, 0.62, 1.0 - L) : clamp(uDarkMode, 0.0, 1.0);\n" +
        "    float lumaComp = mix(0.22, 0.05, uIsClear);\n" +
        "    float kComp = clamp(1.0 - L * lumaComp, 0.0, 1.0);\n" +
        "    c = mix(float3(kComp * L), kComp * c, (1.0 - kComp) * 0.30 + mix(1.22, 1.08, uIsClear));\n" +
        "\n" +
        "    float3 faceBias = mix(float3(0.16, 0.16, 0.18), float3(-0.10, -0.10, -0.09), darkFactor);\n" +
        "    c = clamp(c + faceBias * (1.0 - uTransparency * 0.55) * (1.0 - uIsClear * 0.8), float3(0.0), float3(1.0));\n" +
        "\n" +
        "    // Rim color bleed (Mochi uBleed stage)\n" +
        "    float bleedW = (1.0 - smoothstep(0.0, 12.0 * uDensity, depth)) * 0.24 * (1.0 - uIsClear * 0.65);\n" +
        "    if (bleedW > 0.005) {\n" +
        "        float3 bCol = sampleMochiMip(coord - n * (4.0 * uDensity), uBlurPx * 1.4);\n" +
        "        c = mix(c, clamp(bCol * 1.12, float3(0.0), float3(1.0)), bleedW);\n" +
        "    }\n" +
        "\n" +
        "    c = mix(c, uTint.rgb, clamp(uTint.a * mix(1.0, 0.30, uIsClear), 0.0, 0.88));\n" +
        "\n" +
        "    // Mochi Gaussian erfp inner ring shadow shifted 8dp downward inside glass\n" +
        "    if (uShadow > 0.01 && depth < 22.0 * uDensity) {\n" +
        "        float shiftD = mochiGroupD(local - float2(0.0, 6.0 * uDensity), halfSize);\n" +
        "        float invR = 1.0 / max(5.0 * uDensity, 1.0);\n" +
        "        float a1 = invR * shiftD;\n" +
        "        float a2 = a1 + 4.0 * uDensity * invR;\n" +
        "        float ring = clamp(erfp(a1 * 0.70710678) - erfp(a2 * 0.70710678), 0.0, 1.0) * 0.16 * uShadow;\n" +
        "        c *= (1.0 - ring);\n" +
        "    }\n" +
        "\n" +
        "    // Stroke-mode key/fill dual specular rim highlight (Mochi LIQUID_FINISH_AGSL)\n" +
        "    float rimStroke = smoothstep(2.4 * uDensity, 0.0, depth);\n" +
        "    float dd = dot(n, -uLight);\n" +
        "    float kfSpread = 0.22;\n" +
        "    float invS = 1.0 / max(1.0 - kfSpread, 1e-4);\n" +
        "    float2 vv = clamp((float2(dd, -dd) - float2(kfSpread)) * invS, float2(0.0), float2(1.0)) * rimStroke;\n" +
        "    float2 rKF = vv / max(float2(1.0) + 1.8 * (float2(1.0) - vv), float2(1e-4));\n" +
        "    float hl = (rKF.x * 0.68 + rKF.y * 0.44) * uSpecular;\n" +
        "    float fresnelRim = smoothstep(3.5 * uDensity, 0.0, depth) * 0.18 * uEdgeIllum;\n" +
        "\n" +
        "    c = clamp(c + float3(hl + fresnelRim + touchFall * 0.09), float3(0.0), float3(1.0));\n" +
        "    return half4(half3(c * cov), half(cov));\n" +
        "}";

    public static final String KYANT_BACKDROP =
        "\n" +
        "uniform shader content;\n" +
        "uniform float2 uSize;\n" +
        "uniform float uPad;\n" +
        "uniform float4 uBackdrop;\n" +
        "uniform float4 uRadii;\n" +
        "uniform float uDensity;\n" +
        "uniform float uExtraCount;\n" +
        "uniform float4 uExtraShapes[5];\n" +
        "uniform float uExtraRadii[5];\n" +
        "uniform float uMergeK;\n" +
        "uniform float2 uTouch;\n" +
        "uniform float uPress;\n" +
        "uniform float uShadow;\n" +
        "\n" +
        "uniform float uRefractionHeight;\n" +
        "uniform float uRefractionAmount;\n" +
        "uniform float uDepthEffect;\n" +
        "uniform float uCurvature;\n" +
        "uniform float uChromaticAberration;\n" +
        "uniform float uTransparency;\n" +
        "uniform float4 uTint;\n" +
        "uniform float uLightAngle;\n" +
        "uniform float uSpecular;\n" +
        "uniform float uEdgeIllum;\n" +
        "uniform float uIsClear;\n" +
        "uniform float uDarkMode;\n" +
        "\n" +
        "\n" +
        "float radiusAt(float2 p, float4 r) {\n" +
        "    float2 rr = (p.x >= 0.0) ? r.yz : r.xw;\n" +
        "    return (p.y >= 0.0) ? rr.y : rr.x;\n" +
        "}\n" +
        "\n" +
        "float sdRoundBox(float2 p, float2 halfSize, float radius) {\n" +
        "    float r = min(radius, min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;\n" +
        "}\n" +
        "\n" +
        "float sdRoundBox4(float2 p, float2 halfSize, float4 radii) {\n" +
        "    float r = min(radiusAt(p, radii), min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;\n" +
        "}\n" +
        "\n" +
        "float lnNorm(float2 v, float n) {\n" +
        "    if (n <= 2.01) return length(v);\n" +
        "    float2 av = max(abs(v), float2(1e-6));\n" +
        "    return pow(pow(av.x, n) + pow(av.y, n), 1.0 / n);\n" +
        "}\n" +
        "\n" +
        "float sdSquircleBox4(float2 p, float2 halfSize, float4 radii, float power) {\n" +
        "    float r = min(radiusAt(p, radii), min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    float2 pos = max(q, float2(0.0));\n" +
        "    float outside = (pos.x > 0.0 || pos.y > 0.0) ? lnNorm(pos, power) : 0.0;\n" +
        "    return min(max(q.x, q.y), 0.0) + outside - r;\n" +
        "}\n" +
        "\n" +
        "float sminPoly(float a, float b, float k) {\n" +
        "    if (k <= 0.5) return min(a, b);\n" +
        "    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);\n" +
        "    return mix(b, a, h) - k * h * (1.0 - h);\n" +
        "}\n" +
        "\n" +
        "float sceneSdRound(float2 local, float2 halfSize) {\n" +
        "    float d = sdRoundBox4(local - halfSize, halfSize, uRadii);\n" +
        "    for (int i = 0; i < 5; i++) {\n" +
        "        if (float(i) >= uExtraCount) break;\n" +
        "        float4 s = uExtraShapes[i];\n" +
        "        float d2 = sdRoundBox(local - s.xy, s.zw, uExtraRadii[i]);\n" +
        "        d = sminPoly(d, d2, uMergeK);\n" +
        "    }\n" +
        "    return d;\n" +
        "}\n" +
        "\n" +
        "float sceneSdSquircle(float2 local, float2 halfSize, float power) {\n" +
        "    float d = sdSquircleBox4(local - halfSize, halfSize, uRadii, power);\n" +
        "    for (int i = 0; i < 5; i++) {\n" +
        "        if (float(i) >= uExtraCount) break;\n" +
        "        float4 s = uExtraShapes[i];\n" +
        "        float d2 = sdSquircleBox4(local - s.xy, s.zw, float4(uExtraRadii[i]), power);\n" +
        "        d = sminPoly(d, d2, uMergeK);\n" +
        "    }\n" +
        "    return d;\n" +
        "}\n" +
        "\n" +
        "float2 sceneNormalRound(float2 local, float2 halfSize, float eps) {\n" +
        "    float2 ex = float2(eps, 0.0);\n" +
        "    float2 ey = float2(0.0, eps);\n" +
        "    float2 g = float2(\n" +
        "        sceneSdRound(local + ex, halfSize) - sceneSdRound(local - ex, halfSize),\n" +
        "        sceneSdRound(local + ey, halfSize) - sceneSdRound(local - ey, halfSize)\n" +
        "    );\n" +
        "    float len = length(g);\n" +
        "    return (len > 1e-5) ? (g / len) : float2(0.0, -1.0);\n" +
        "}\n" +
        "\n" +
        "float2 sceneNormalSquircle(float2 local, float2 halfSize, float power, float eps) {\n" +
        "    float2 ex = float2(eps, 0.0);\n" +
        "    float2 ey = float2(0.0, eps);\n" +
        "    float2 g = float2(\n" +
        "        sceneSdSquircle(local + ex, halfSize, power) - sceneSdSquircle(local - ex, halfSize, power),\n" +
        "        sceneSdSquircle(local + ey, halfSize, power) - sceneSdSquircle(local - ey, halfSize, power)\n" +
        "    );\n" +
        "    float len = length(g);\n" +
        "    return (len > 1e-5) ? (g / len) : float2(0.0, -1.0);\n" +
        "}\n" +
        "\n" +
        "float3 sampleBackdropRgb(float2 coord) {\n" +
        "    float2 q = clamp(coord, uBackdrop.xy, uBackdrop.zw);\n" +
        "    half4 c = content.eval(q);\n" +
        "    return (c.a > half(0.004)) ? float3(c.rgb / c.a) : float3(0.08, 0.08, 0.10);\n" +
        "}\n" +
        "\n" +
        "float luma709(float3 c) {\n" +
        "    return dot(c, float3(0.2126, 0.7152, 0.0722));\n" +
        "}\n" +
        "\n" +
        "half4 dropShadowOutside(float dShadow, float shadowStrength, float density) {\n" +
        "    if (shadowStrength <= 0.005) return half4(0.0);\n" +
        "    float spread = max(14.0 * density, 4.0);\n" +
        "    float alpha = (1.0 - smoothstep(-2.0 * density, spread, dShadow)) * 0.22 * clamp(shadowStrength, 0.0, 2.0);\n" +
        "    return half4(0.0, 0.0, 0.0, half(clamp(alpha, 0.0, 0.65)));\n" +
        "}\n" +
        "\n" +
        "\n" +
        "float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {\n" +
        "    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));\n" +
        "    if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {\n" +
        "        float2 pos = max(cornerCoord, float2(0.0));\n" +
        "        float l = length(pos);\n" +
        "        return (l > 1e-5) ? (sign(coord) * (pos / l)) : float2(0.0, -1.0);\n" +
        "    } else {\n" +
        "        float gradX = step(cornerCoord.y, cornerCoord.x);\n" +
        "        return sign(coord) * float2(gradX, 1.0 - gradX);\n" +
        "    }\n" +
        "}\n" +
        "\n" +
        "float circleMap(float x) {\n" +
        "    float c = clamp(x, 0.0, 1.0);\n" +
        "    float shaped = pow(max(c, 1e-5), clamp(uCurvature, 0.35, 2.2));\n" +
        "    return 1.0 - sqrt(max(1.0 - shaped * shaped, 0.0));\n" +
        "}\n" +
        "\n" +
        "half4 main(float2 coord) {\n" +
        "    float2 local = coord - float2(uPad);\n" +
        "    float2 halfSize = uSize * 0.5;\n" +
        "    float2 centered = local - halfSize;\n" +
        "\n" +
        "    float sd = sceneSdRound(local, halfSize);\n" +
        "    float coverage = clamp(0.5 - sd, 0.0, 1.0);\n" +
        "    if (coverage <= 0.0) {\n" +
        "        float dSh = sceneSdRound(local - float2(0.0, 5.0 * uDensity), halfSize);\n" +
        "        return dropShadowOutside(dSh, uShadow, uDensity);\n" +
        "    }\n" +
        "\n" +
        "    float radius = min(radiusAt(centered, uRadii), min(halfSize.x, halfSize.y));\n" +
        "    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));\n" +
        "\n" +
        "    // Exact Kyant0 inflated-radius gradient when single shape, smooth SDF gradient when merged\n" +
        "    float2 baseGrad;\n" +
        "    if (uExtraCount < 0.5) {\n" +
        "        baseGrad = gradSdRoundedRect(centered, halfSize, gradRadius);\n" +
        "    } else {\n" +
        "        baseGrad = sceneNormalRound(local, halfSize, 1.5);\n" +
        "    }\n" +
        "\n" +
        "    float cLen = length(centered);\n" +
        "    float2 radialDir = (cLen > 1e-4) ? (centered / cLen) : float2(0.0);\n" +
        "    float2 combinedGrad = baseGrad + uDepthEffect * radialDir;\n" +
        "    float cgLen = length(combinedGrad);\n" +
        "    float2 grad = (cgLen > 1e-5) ? (combinedGrad / cgLen) : baseGrad;\n" +
        "\n" +
        "    // Touch bulge\n" +
        "    float2 touchDelta = local - uTouch;\n" +
        "    float touchSigma = max(min(uSize.x, uSize.y), 1.0);\n" +
        "    float touchFall = exp(-dot(touchDelta, touchDelta) / (touchSigma * touchSigma)) * clamp(uPress, 0.0, 2.0);\n" +
        "    float2 baseCoord = coord - touchDelta * (touchFall * 0.14);\n" +
        "\n" +
        "    float depth = max(-sd, 0.0);\n" +
        "    float3 rgb;\n" +
        "    if (depth >= uRefractionHeight) {\n" +
        "        rgb = sampleBackdropRgb(baseCoord);\n" +
        "    } else {\n" +
        "        float d = circleMap(1.0 - depth / max(uRefractionHeight, 1.0)) * uRefractionAmount * (1.0 + 0.3 * touchFall);\n" +
        "        float2 refractedCoord = baseCoord + d * grad;\n" +
        "\n" +
        "        if (uChromaticAberration > 0.005) {\n" +
        "            // Exact Kyant0 7-band spectral dispersion\n" +
        "            float quadFactor = (centered.x * centered.y) / max(halfSize.x * halfSize.y, 1.0);\n" +
        "            float dispIntensity = uChromaticAberration * mix(quadFactor, sign(quadFactor + 1e-4) * 0.45, 0.25);\n" +
        "            float2 dispCoord = d * grad * dispIntensity;\n" +
        "\n" +
        "            float3 acc = float3(0.0);\n" +
        "            float3 red    = sampleBackdropRgb(refractedCoord + dispCoord);\n" +
        "            float3 orange = sampleBackdropRgb(refractedCoord + dispCoord * (2.0 / 3.0));\n" +
        "            float3 yellow = sampleBackdropRgb(refractedCoord + dispCoord * (1.0 / 3.0));\n" +
        "            float3 green  = sampleBackdropRgb(refractedCoord);\n" +
        "            float3 cyan   = sampleBackdropRgb(refractedCoord - dispCoord * (1.0 / 3.0));\n" +
        "            float3 blue   = sampleBackdropRgb(refractedCoord - dispCoord * (2.0 / 3.0));\n" +
        "            float3 purple = sampleBackdropRgb(refractedCoord - dispCoord);\n" +
        "\n" +
        "            acc.r = red.r / 3.5 + orange.r / 3.5 + yellow.r / 3.5 + purple.r / 7.0;\n" +
        "            acc.g = orange.g / 7.0 + yellow.g / 3.5 + green.g / 3.5 + cyan.g / 3.5;\n" +
        "            acc.b = cyan.b / 3.0 + blue.b / 3.0 + purple.b / 3.0;\n" +
        "            rgb = acc;\n" +
        "        } else {\n" +
        "            rgb = sampleBackdropRgb(refractedCoord);\n" +
        "        }\n" +
        "    }\n" +
        "\n" +
        "    // Kyant0 vibrancy saturation boost (1.35x regular, 1.15x clear)\n" +
        "    float l = luma709(rgb);\n" +
        "    float vib = mix(1.38, 1.12, uIsClear);\n" +
        "    rgb = clamp(mix(float3(l), rgb, vib), float3(0.0), float3(1.0));\n" +
        "\n" +
        "    float darkFactor = (uDarkMode < -0.5) ? smoothstep(0.45, 0.65, 1.0 - l) : clamp(uDarkMode, 0.0, 1.0);\n" +
        "    float3 surfaceTone = mix(float3(0.96, 0.96, 0.98), float3(0.10, 0.10, 0.13), darkFactor);\n" +
        "    float surfaceAlpha = (1.0 - uTransparency) * mix(0.42, 0.10, uIsClear);\n" +
        "    rgb = mix(rgb, surfaceTone, clamp(surfaceAlpha, 0.0, 0.72));\n" +
        "    rgb = mix(rgb, uTint.rgb, clamp(uTint.a * mix(1.0, 0.35, uIsClear), 0.0, 0.88));\n" +
        "\n" +
        "    // Directional specular bevel + opposite-rim ambient highlight (Kyant0 SdfShader.kt formula)\n" +
        "    float rimDepth = clamp(1.0 - depth / max(uRefractionHeight, 1.0), 0.0, 1.0);\n" +
        "    float rimStroke = smoothstep(3.0 * uDensity, 0.0, depth);\n" +
        "    float2 lightVec = float2(cos(uLightAngle), sin(uLightAngle));\n" +
        "    float dDot = dot(baseGrad, lightVec);\n" +
        "    float keyBevel = clamp(dDot, 0.0, 1.0);\n" +
        "    float defaultHi = pow(keyBevel, 2.0) * max(rimStroke, rimDepth * 0.75) * 0.65 * uSpecular;\n" +
        "\n" +
        "    float oppBevel = clamp(-dDot, 0.0, 1.0);\n" +
        "    float ambientHi = oppBevel * smoothstep(1.0, 0.0, abs(rimDepth - 0.25) * 4.0) * 0.30 * uEdgeIllum;\n" +
        "\n" +
        "    rgb += float3(defaultHi + ambientHi);\n" +
        "\n" +
        "    // Inner shadow subtle thickness cue\n" +
        "    float innerSh = smoothstep(5.5 * uDensity, 0.0, depth) * (1.0 - rimStroke);\n" +
        "    rgb -= float3(innerSh * 0.05 * uShadow);\n" +
        "    rgb += float3(touchFall * 0.06);\n" +
        "\n" +
        "    rgb = clamp(rgb, float3(0.0), float3(1.0));\n" +
        "    return half4(half3(rgb * coverage), half(coverage));\n" +
        "}";

    public static final String QWEA0_GRAVITY =
        "\n" +
        "uniform shader content;\n" +
        "uniform float2 uSize;\n" +
        "uniform float uPad;\n" +
        "uniform float4 uBackdrop;\n" +
        "uniform float4 uRadii;\n" +
        "uniform float uDensity;\n" +
        "uniform float uExtraCount;\n" +
        "uniform float4 uExtraShapes[5];\n" +
        "uniform float uExtraRadii[5];\n" +
        "uniform float uMergeK;\n" +
        "uniform float2 uTouch;\n" +
        "uniform float uPress;\n" +
        "uniform float uShadow;\n" +
        "\n" +
        "uniform float uBevel;\n" +
        "uniform float uRefractPx;\n" +
        "uniform float uFalloff;\n" +
        "uniform float uRefractDir;\n" +
        "uniform float uRimSoft;\n" +
        "uniform float uLensZoom;\n" +
        "uniform float uDispersion;\n" +
        "uniform float2 uLight;\n" +
        "uniform float uSpecStrength;\n" +
        "uniform float uEdgeIllum;\n" +
        "uniform float uTransparency;\n" +
        "uniform float4 uGlassTint;\n" +
        "uniform float uSatFactor;\n" +
        "uniform float uIsClear;\n" +
        "uniform float uDarkMode;\n" +
        "\n" +
        "\n" +
        "float radiusAt(float2 p, float4 r) {\n" +
        "    float2 rr = (p.x >= 0.0) ? r.yz : r.xw;\n" +
        "    return (p.y >= 0.0) ? rr.y : rr.x;\n" +
        "}\n" +
        "\n" +
        "float sdRoundBox(float2 p, float2 halfSize, float radius) {\n" +
        "    float r = min(radius, min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;\n" +
        "}\n" +
        "\n" +
        "float sdRoundBox4(float2 p, float2 halfSize, float4 radii) {\n" +
        "    float r = min(radiusAt(p, radii), min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;\n" +
        "}\n" +
        "\n" +
        "float lnNorm(float2 v, float n) {\n" +
        "    if (n <= 2.01) return length(v);\n" +
        "    float2 av = max(abs(v), float2(1e-6));\n" +
        "    return pow(pow(av.x, n) + pow(av.y, n), 1.0 / n);\n" +
        "}\n" +
        "\n" +
        "float sdSquircleBox4(float2 p, float2 halfSize, float4 radii, float power) {\n" +
        "    float r = min(radiusAt(p, radii), min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    float2 pos = max(q, float2(0.0));\n" +
        "    float outside = (pos.x > 0.0 || pos.y > 0.0) ? lnNorm(pos, power) : 0.0;\n" +
        "    return min(max(q.x, q.y), 0.0) + outside - r;\n" +
        "}\n" +
        "\n" +
        "float sminPoly(float a, float b, float k) {\n" +
        "    if (k <= 0.5) return min(a, b);\n" +
        "    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);\n" +
        "    return mix(b, a, h) - k * h * (1.0 - h);\n" +
        "}\n" +
        "\n" +
        "float sceneSdRound(float2 local, float2 halfSize) {\n" +
        "    float d = sdRoundBox4(local - halfSize, halfSize, uRadii);\n" +
        "    for (int i = 0; i < 5; i++) {\n" +
        "        if (float(i) >= uExtraCount) break;\n" +
        "        float4 s = uExtraShapes[i];\n" +
        "        float d2 = sdRoundBox(local - s.xy, s.zw, uExtraRadii[i]);\n" +
        "        d = sminPoly(d, d2, uMergeK);\n" +
        "    }\n" +
        "    return d;\n" +
        "}\n" +
        "\n" +
        "float sceneSdSquircle(float2 local, float2 halfSize, float power) {\n" +
        "    float d = sdSquircleBox4(local - halfSize, halfSize, uRadii, power);\n" +
        "    for (int i = 0; i < 5; i++) {\n" +
        "        if (float(i) >= uExtraCount) break;\n" +
        "        float4 s = uExtraShapes[i];\n" +
        "        float d2 = sdSquircleBox4(local - s.xy, s.zw, float4(uExtraRadii[i]), power);\n" +
        "        d = sminPoly(d, d2, uMergeK);\n" +
        "    }\n" +
        "    return d;\n" +
        "}\n" +
        "\n" +
        "float2 sceneNormalRound(float2 local, float2 halfSize, float eps) {\n" +
        "    float2 ex = float2(eps, 0.0);\n" +
        "    float2 ey = float2(0.0, eps);\n" +
        "    float2 g = float2(\n" +
        "        sceneSdRound(local + ex, halfSize) - sceneSdRound(local - ex, halfSize),\n" +
        "        sceneSdRound(local + ey, halfSize) - sceneSdRound(local - ey, halfSize)\n" +
        "    );\n" +
        "    float len = length(g);\n" +
        "    return (len > 1e-5) ? (g / len) : float2(0.0, -1.0);\n" +
        "}\n" +
        "\n" +
        "float2 sceneNormalSquircle(float2 local, float2 halfSize, float power, float eps) {\n" +
        "    float2 ex = float2(eps, 0.0);\n" +
        "    float2 ey = float2(0.0, eps);\n" +
        "    float2 g = float2(\n" +
        "        sceneSdSquircle(local + ex, halfSize, power) - sceneSdSquircle(local - ex, halfSize, power),\n" +
        "        sceneSdSquircle(local + ey, halfSize, power) - sceneSdSquircle(local - ey, halfSize, power)\n" +
        "    );\n" +
        "    float len = length(g);\n" +
        "    return (len > 1e-5) ? (g / len) : float2(0.0, -1.0);\n" +
        "}\n" +
        "\n" +
        "float3 sampleBackdropRgb(float2 coord) {\n" +
        "    float2 q = clamp(coord, uBackdrop.xy, uBackdrop.zw);\n" +
        "    half4 c = content.eval(q);\n" +
        "    return (c.a > half(0.004)) ? float3(c.rgb / c.a) : float3(0.08, 0.08, 0.10);\n" +
        "}\n" +
        "\n" +
        "float luma709(float3 c) {\n" +
        "    return dot(c, float3(0.2126, 0.7152, 0.0722));\n" +
        "}\n" +
        "\n" +
        "half4 dropShadowOutside(float dShadow, float shadowStrength, float density) {\n" +
        "    if (shadowStrength <= 0.005) return half4(0.0);\n" +
        "    float spread = max(14.0 * density, 4.0);\n" +
        "    float alpha = (1.0 - smoothstep(-2.0 * density, spread, dShadow)) * 0.22 * clamp(shadowStrength, 0.0, 2.0);\n" +
        "    return half4(0.0, 0.0, 0.0, half(clamp(alpha, 0.0, 0.65)));\n" +
        "}\n" +
        "\n" +
        "\n" +
        "half4 main(float2 coord) {\n" +
        "    float2 local = coord - float2(uPad);\n" +
        "    float2 halfSize = uSize * 0.5;\n" +
        "\n" +
        "    float d = sceneSdRound(local, halfSize);\n" +
        "    float cov = clamp(0.5 - d / 1.5, 0.0, 1.0);\n" +
        "    if (cov <= 0.004) {\n" +
        "        float dSh = sceneSdRound(local - float2(0.0, 5.0 * uDensity), halfSize);\n" +
        "        return dropShadowOutside(dSh, uShadow, uDensity);\n" +
        "    }\n" +
        "\n" +
        "    float2 n = sceneNormalRound(local, halfSize, 1.0);\n" +
        "    float t = clamp(-d / max(uBevel, 1.0), 0.0, 1.0);\n" +
        "    float edge = 1.0 - t;\n" +
        "    float slope;\n" +
        "    if (uFalloff > 0.01) {\n" +
        "        // Exact QWEA0 gravitational-lens inverse-power attenuation\n" +
        "        float gB = pow(5.0, -uFalloff);\n" +
        "        slope = (pow(1.0 + 4.0 * t, -uFalloff) - gB) / max(1.0 - gB, 1e-4);\n" +
        "    } else {\n" +
        "        slope = edge * edge;\n" +
        "    }\n" +
        "\n" +
        "    float press = clamp(uPress, 0.0, 2.0);\n" +
        "    float refr = uRefractPx * (1.0 + 0.55 * press);\n" +
        "    float2 offset = n * (uRefractDir * slope * refr) - (local - halfSize) * (uLensZoom * 0.12);\n" +
        "\n" +
        "    // Local liquid touch bulge\n" +
        "    if (press > 0.001) {\n" +
        "        float2 tp = local - uTouch;\n" +
        "        float tr = length(tp);\n" +
        "        float sigma = max(max(halfSize.x, halfSize.y), 1.0);\n" +
        "        float bump = press * exp(-(tr * tr) / (sigma * sigma * 0.30));\n" +
        "        if (tr > 1.0) {\n" +
        "            offset -= (tp / tr) * (bump * refr * 0.45);\n" +
        "        }\n" +
        "    }\n" +
        "\n" +
        "    float2 cR = coord + offset * (1.0 - uDispersion * slope);\n" +
        "    float2 cG = coord + offset;\n" +
        "    float2 cB = coord + offset * (1.0 + uDispersion * slope);\n" +
        "\n" +
        "    // 3-tap normal-aligned rimSoft smear (exact QWEA0 GlassLensRenderer.kt)\n" +
        "    float3 col;\n" +
        "    if (uRimSoft > 0.01 && slope > 0.005) {\n" +
        "        float2 sm = n * (uRimSoft * slope);\n" +
        "        col = float3(\n" +
        "            (sampleBackdropRgb(cR).r + sampleBackdropRgb(cR - sm).r + sampleBackdropRgb(cR + sm).r) / 3.0,\n" +
        "            (sampleBackdropRgb(cG).g + sampleBackdropRgb(cG - sm).g + sampleBackdropRgb(cG + sm).g) / 3.0,\n" +
        "            (sampleBackdropRgb(cB).b + sampleBackdropRgb(cB - sm).b + sampleBackdropRgb(cB + sm).b) / 3.0\n" +
        "        );\n" +
        "    } else {\n" +
        "        col = float3(\n" +
        "            sampleBackdropRgb(cR).r,\n" +
        "            sampleBackdropRgb(cG).g,\n" +
        "            sampleBackdropRgb(cB).b\n" +
        "        );\n" +
        "    }\n" +
        "\n" +
        "    // Nonlinear highlight-protected vibrancy curve (exact QWEA0 GlassRuntimeEffects / GlassLensRenderer)\n" +
        "    float lum = luma709(col);\n" +
        "    if (uSatFactor <= 1.0) {\n" +
        "        col = mix(float3(lum), col, uSatFactor);\n" +
        "    } else {\n" +
        "        float satNow = max(col.r, max(col.g, col.b)) - min(col.r, min(col.g, col.b));\n" +
        "        float room = 1.0 - smoothstep(0.2, 0.85, satNow);\n" +
        "        float hl = 1.0 - smoothstep(0.75, 0.98, lum);\n" +
        "        float amount = 1.0 + (uSatFactor - 1.0) * mix(0.3, 1.0, room * hl);\n" +
        "        col = clamp(mix(float3(lum), col, amount), float3(0.0), float3(1.0));\n" +
        "    }\n" +
        "\n" +
        "    // Adaptive luminance tint (Regular) vs clear dimming veil\n" +
        "    float lumT = luma709(col);\n" +
        "    float darkFactor = (uDarkMode < -0.5) ? smoothstep(0.35, 0.75, lumT) : (1.0 - clamp(uDarkMode, 0.0, 1.0));\n" +
        "    float adaptiveAlpha = (0.14 + 0.08 * darkFactor) * (1.0 - uTransparency * 0.45) * (1.0 - uIsClear * 0.80);\n" +
        "    col = mix(col, float3(1.0 - darkFactor), clamp(adaptiveAlpha, 0.0, 0.65));\n" +
        "\n" +
        "    // Absorbing + scattering colored glass medium (exact QWEA0 model)\n" +
        "    if (uGlassTint.a > 0.002) {\n" +
        "        float lumTint = luma709(col);\n" +
        "        float3 absorbed = col * mix(float3(1.0), uGlassTint.rgb, 0.85);\n" +
        "        float3 scattered = uGlassTint.rgb * (0.38 * (1.0 - lumTint));\n" +
        "        col = mix(col, clamp(absorbed + scattered, float3(0.0), float3(1.0)), uGlassTint.a * mix(1.0, 0.35, uIsClear));\n" +
        "    }\n" +
        "\n" +
        "    // Symmetric dual-lobe pow(facing, 4.5) hairline + inward glow tail\n" +
        "    float facing = dot(n, -uLight);\n" +
        "    float lobeF = pow(max(facing, 1e-5), 4.5);\n" +
        "    float lobeB = pow(max(-facing, 1e-5), 4.5);\n" +
        "\n" +
        "    float bandW = clamp(uBevel * 0.30, 2.0, 8.0 * uDensity);\n" +
        "    float glowIn = clamp((-d - 1.0 * uDensity) / max(2.0 * uDensity, 1.0), 0.0, 1.0);\n" +
        "    float glow = glowIn * pow(clamp(1.0 - (-d - 3.0 * uDensity) / max(bandW, 1.0), 0.0, 1.0), 1.5) * cov;\n" +
        "    float hair = clamp(1.0 - abs(d + 1.0 * uDensity) / max(2.0 * uDensity, 1.0), 0.0, 1.0) * cov;\n" +
        "    float spec = (hair * 0.72 * (lobeF + lobeB) * uSpecStrength + glow * 0.14 * lobeF * uEdgeIllum)\n" +
        "        * (1.0 - 0.25 * clamp(press, 0.0, 1.0));\n" +
        "\n" +
        "    float innerSh = smoothstep(5.0 * uDensity, 0.0, -d) * (1.0 - hair) * 0.045 * uShadow;\n" +
        "    col = clamp(col + float3(spec + press * 0.04) - float3(innerSh), float3(0.0), float3(1.0));\n" +
        "    return half4(half3(col * cov), half(cov));\n" +
        "}";

    public static final String FROSTED_BASELINE =
        "\n" +
        "uniform shader content;\n" +
        "uniform float2 uSize;\n" +
        "uniform float uPad;\n" +
        "uniform float4 uBackdrop;\n" +
        "uniform float4 uRadii;\n" +
        "uniform float uDensity;\n" +
        "uniform float uExtraCount;\n" +
        "uniform float4 uExtraShapes[5];\n" +
        "uniform float uExtraRadii[5];\n" +
        "uniform float uMergeK;\n" +
        "uniform float2 uTouch;\n" +
        "uniform float uPress;\n" +
        "uniform float uShadow;\n" +
        "\n" +
        "uniform float uTransparency;\n" +
        "uniform float4 uTint;\n" +
        "uniform float uBorderAlpha;\n" +
        "uniform float uIsClear;\n" +
        "uniform float uDarkMode;\n" +
        "\n" +
        "\n" +
        "float radiusAt(float2 p, float4 r) {\n" +
        "    float2 rr = (p.x >= 0.0) ? r.yz : r.xw;\n" +
        "    return (p.y >= 0.0) ? rr.y : rr.x;\n" +
        "}\n" +
        "\n" +
        "float sdRoundBox(float2 p, float2 halfSize, float radius) {\n" +
        "    float r = min(radius, min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;\n" +
        "}\n" +
        "\n" +
        "float sdRoundBox4(float2 p, float2 halfSize, float4 radii) {\n" +
        "    float r = min(radiusAt(p, radii), min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;\n" +
        "}\n" +
        "\n" +
        "float lnNorm(float2 v, float n) {\n" +
        "    if (n <= 2.01) return length(v);\n" +
        "    float2 av = max(abs(v), float2(1e-6));\n" +
        "    return pow(pow(av.x, n) + pow(av.y, n), 1.0 / n);\n" +
        "}\n" +
        "\n" +
        "float sdSquircleBox4(float2 p, float2 halfSize, float4 radii, float power) {\n" +
        "    float r = min(radiusAt(p, radii), min(halfSize.x, halfSize.y));\n" +
        "    float2 q = abs(p) - halfSize + float2(r);\n" +
        "    float2 pos = max(q, float2(0.0));\n" +
        "    float outside = (pos.x > 0.0 || pos.y > 0.0) ? lnNorm(pos, power) : 0.0;\n" +
        "    return min(max(q.x, q.y), 0.0) + outside - r;\n" +
        "}\n" +
        "\n" +
        "float sminPoly(float a, float b, float k) {\n" +
        "    if (k <= 0.5) return min(a, b);\n" +
        "    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);\n" +
        "    return mix(b, a, h) - k * h * (1.0 - h);\n" +
        "}\n" +
        "\n" +
        "float sceneSdRound(float2 local, float2 halfSize) {\n" +
        "    float d = sdRoundBox4(local - halfSize, halfSize, uRadii);\n" +
        "    for (int i = 0; i < 5; i++) {\n" +
        "        if (float(i) >= uExtraCount) break;\n" +
        "        float4 s = uExtraShapes[i];\n" +
        "        float d2 = sdRoundBox(local - s.xy, s.zw, uExtraRadii[i]);\n" +
        "        d = sminPoly(d, d2, uMergeK);\n" +
        "    }\n" +
        "    return d;\n" +
        "}\n" +
        "\n" +
        "float sceneSdSquircle(float2 local, float2 halfSize, float power) {\n" +
        "    float d = sdSquircleBox4(local - halfSize, halfSize, uRadii, power);\n" +
        "    for (int i = 0; i < 5; i++) {\n" +
        "        if (float(i) >= uExtraCount) break;\n" +
        "        float4 s = uExtraShapes[i];\n" +
        "        float d2 = sdSquircleBox4(local - s.xy, s.zw, float4(uExtraRadii[i]), power);\n" +
        "        d = sminPoly(d, d2, uMergeK);\n" +
        "    }\n" +
        "    return d;\n" +
        "}\n" +
        "\n" +
        "float2 sceneNormalRound(float2 local, float2 halfSize, float eps) {\n" +
        "    float2 ex = float2(eps, 0.0);\n" +
        "    float2 ey = float2(0.0, eps);\n" +
        "    float2 g = float2(\n" +
        "        sceneSdRound(local + ex, halfSize) - sceneSdRound(local - ex, halfSize),\n" +
        "        sceneSdRound(local + ey, halfSize) - sceneSdRound(local - ey, halfSize)\n" +
        "    );\n" +
        "    float len = length(g);\n" +
        "    return (len > 1e-5) ? (g / len) : float2(0.0, -1.0);\n" +
        "}\n" +
        "\n" +
        "float2 sceneNormalSquircle(float2 local, float2 halfSize, float power, float eps) {\n" +
        "    float2 ex = float2(eps, 0.0);\n" +
        "    float2 ey = float2(0.0, eps);\n" +
        "    float2 g = float2(\n" +
        "        sceneSdSquircle(local + ex, halfSize, power) - sceneSdSquircle(local - ex, halfSize, power),\n" +
        "        sceneSdSquircle(local + ey, halfSize, power) - sceneSdSquircle(local - ey, halfSize, power)\n" +
        "    );\n" +
        "    float len = length(g);\n" +
        "    return (len > 1e-5) ? (g / len) : float2(0.0, -1.0);\n" +
        "}\n" +
        "\n" +
        "float3 sampleBackdropRgb(float2 coord) {\n" +
        "    float2 q = clamp(coord, uBackdrop.xy, uBackdrop.zw);\n" +
        "    half4 c = content.eval(q);\n" +
        "    return (c.a > half(0.004)) ? float3(c.rgb / c.a) : float3(0.08, 0.08, 0.10);\n" +
        "}\n" +
        "\n" +
        "float luma709(float3 c) {\n" +
        "    return dot(c, float3(0.2126, 0.7152, 0.0722));\n" +
        "}\n" +
        "\n" +
        "half4 dropShadowOutside(float dShadow, float shadowStrength, float density) {\n" +
        "    if (shadowStrength <= 0.005) return half4(0.0);\n" +
        "    float spread = max(14.0 * density, 4.0);\n" +
        "    float alpha = (1.0 - smoothstep(-2.0 * density, spread, dShadow)) * 0.22 * clamp(shadowStrength, 0.0, 2.0);\n" +
        "    return half4(0.0, 0.0, 0.0, half(clamp(alpha, 0.0, 0.65)));\n" +
        "}\n" +
        "\n" +
        "\n" +
        "// Plain non-merging union (no smin liquid bridge in conventional glassmorphism)\n" +
        "float plainSd(float2 local, float2 halfSize) {\n" +
        "    float d = sdRoundBox4(local - halfSize, halfSize, uRadii);\n" +
        "    for (int i = 0; i < 5; i++) {\n" +
        "        if (float(i) >= uExtraCount) break;\n" +
        "        float4 s = uExtraShapes[i];\n" +
        "        d = min(d, sdRoundBox(local - s.xy, s.zw, uExtraRadii[i]));\n" +
        "    }\n" +
        "    return d;\n" +
        "}\n" +
        "\n" +
        "half4 main(float2 coord) {\n" +
        "    float2 local = coord - float2(uPad);\n" +
        "    float2 halfSize = uSize * 0.5;\n" +
        "\n" +
        "    float sd = plainSd(local, halfSize);\n" +
        "    float coverage = clamp(0.5 - sd, 0.0, 1.0);\n" +
        "    if (coverage <= 0.0) {\n" +
        "        float dSh = plainSd(local - float2(0.0, 4.0 * uDensity), halfSize);\n" +
        "        return dropShadowOutside(dSh, uShadow, uDensity);\n" +
        "    }\n" +
        "\n" +
        "    // ZERO refraction: direct 1:1 unwarped sample of pre-blurred backdrop\n" +
        "    float3 rgb = sampleBackdropRgb(coord);\n" +
        "    float luma = luma709(rgb);\n" +
        "    float darkFactor = (uDarkMode < -0.5) ? smoothstep(0.45, 0.65, 1.0 - luma) : clamp(uDarkMode, 0.0, 1.0);\n" +
        "\n" +
        "    float3 veilColor = mix(float3(0.96, 0.96, 0.98), float3(0.11, 0.11, 0.14), darkFactor);\n" +
        "    float veilAlpha = clamp((1.0 - uTransparency) * mix(0.55, 0.18, uIsClear), 0.0, 0.85);\n" +
        "    rgb = mix(rgb, veilColor, veilAlpha);\n" +
        "    rgb = mix(rgb, uTint.rgb, clamp(uTint.a * mix(1.0, 0.4, uIsClear), 0.0, 0.90));\n" +
        "\n" +
        "    // Uniform 1dp flat white border (classic glassmorphism look)\n" +
        "    float depth = max(-sd, 0.0);\n" +
        "    float border = (1.0 - smoothstep(0.6 * uDensity, 1.3 * uDensity, depth)) * clamp(uBorderAlpha, 0.0, 1.0);\n" +
        "    rgb = mix(rgb, float3(1.0), border);\n" +
        "    rgb = clamp(rgb + float3(clamp(uPress, 0.0, 1.0) * 0.04), float3(0.0), float3(1.0));\n" +
        "\n" +
        "    return half4(half3(rgb * coverage), half(coverage));\n" +
        "}";

    public static String getShaderSource(LiquidGlassEngineId engineId) {
        if (engineId == null) return SHAYANN_IOS27;
        switch (engineId) {
            case SHAYANN_IOS27: return SHAYANN_IOS27;
            case FLUTTER_SEAGLASS: return FLUTTER_SEAGLASS;
            case VITRO_IOS26: return VITRO_IOS26;
            case HAZE_GLASS: return HAZE_GLASS;
            case SOHUM_SNELL: return SOHUM_SNELL;
            case MOCHI_G2: return MOCHI_G2;
            case KYANT_BACKDROP: return KYANT_BACKDROP;
            case QWEA0_GRAVITY: return QWEA0_GRAVITY;
            case FROSTED_BASELINE: return FROSTED_BASELINE;
            default: return SHAYANN_IOS27;
        }
    }

    public static void bindUniforms(
            RuntimeShader shader,
            LiquidGlassEngineId engineId,
            float widthPx,
            float heightPx,
            float padPx,
            float density,
            float cornerRadiusPx,
            LiquidGlassParams rawParams,
            List<LiquidGlassParams.SubShape> extraShapes,
            float touchX,
            float touchY,
            float pressAmount,
            float wideStripY,
            float wideScale,
            float backdropLeft,
            float backdropTop,
            float backdropRight,
            float backdropBottom
    ) {
        LiquidGlassParams p = rawParams.sanitized();
        boolean isClear = (p.variant == LiquidGlassParams.Variant.CLEAR);

        // Common geometry, touch & morphing uniforms
        shader.setFloatUniform("uSize", widthPx, heightPx);
        shader.setFloatUniform("uPad", padPx);
        shader.setFloatUniform("uBackdrop", backdropLeft, backdropTop, backdropRight, backdropBottom);
        shader.setFloatUniform("uRadii", cornerRadiusPx, cornerRadiusPx, cornerRadiusPx, cornerRadiusPx);
        shader.setFloatUniform("uDensity", density);

        int count = (extraShapes == null) ? 0 : Math.min(extraShapes.size(), 5);
        shader.setFloatUniform("uExtraCount", (float) count);
        float[] packedShapes = new float[20];
        float[] packedRadii = new float[5];
        for (int i = 0; i < count; i++) {
            LiquidGlassParams.SubShape s = extraShapes.get(i);
            packedShapes[i * 4] = s.centerX;
            packedShapes[i * 4 + 1] = s.centerY;
            packedShapes[i * 4 + 2] = s.halfWidth;
            packedShapes[i * 4 + 3] = s.halfHeight;
            packedRadii[i] = s.cornerRadius;
        }
        shader.setFloatUniform("uExtraShapes", packedShapes);
        shader.setFloatUniform("uExtraRadii", packedRadii);
        shader.setFloatUniform("uMergeK", p.morphSmoothingDp * density);
        shader.setFloatUniform("uTouch", touchX, touchY);
        shader.setFloatUniform("uPress", pressAmount * p.touchDeformation);
        shader.setFloatUniform("uShadow", p.shadowIntensity);

        float lightRad = (float) Math.toRadians(p.lightAngleDegrees);
        float lightDirX = (float) Math.cos(lightRad);
        float lightDirY = (float) Math.sin(lightRad);

        switch (engineId) {
            case SHAYANN_IOS27: {
                float band = Math.max(4f, p.glassThicknessDp * density);
                boolean outward = (p.outwardSamplingOverride == null) || p.outwardSamplingOverride;
                shader.setFloatUniform("uWideStripY", wideStripY);
                shader.setFloatUniform("uWideScale", wideScale);
                shader.setFloatUniform("uRefractBand", band);
                shader.setFloatUniform("uRefractDepth", band * 0.82f * p.refractionStrength);
                shader.setFloatUniform("uIor", 1.50f);
                shader.setFloatUniform("uBevelPower", clamp(2f + p.edgeCurvature * 2f, 1.8f, 6f));
                shader.setFloatUniform("uCornerPower", clamp(2f + p.edgeCurvature * 2f, 2f, 5f));
                shader.setFloatUniform("uLensZoom", p.lensDistortion);
                shader.setFloatUniform("uBlurPx", Math.max(0f, p.blurRadiusDp * density));
                shader.setFloatUniform("uAberration", p.chromaticAberration * 0.18f);
                shader.setFloatUniform("uTransparency", p.transparency);
                shader.setFloatUniform("uTint", p.tintRed(), p.tintGreen(), p.tintBlue(), p.tintOpacity);
                shader.setFloatUniform("uLight", lightDirX, lightDirY);
                shader.setFloatUniform("uSpecular", p.specularIntensity);
                shader.setFloatUniform("uEdgeIllum", p.edgeIllumination);
                shader.setFloatUniform("uIsClear", isClear ? 1f : 0f);
                shader.setFloatUniform("uDarkMode", p.darkModeUniform());
                shader.setFloatUniform("uOutward", outward ? 1f : -1f);
                break;
            }
            case FLUTTER_SEAGLASS: {
                float bevel = Math.max(3f, p.glassThicknessDp * density);
                shader.setFloatUniform("uBevelPx", bevel);
                shader.setFloatUniform("uDispScalePx", bevel * 1.45f * p.refractionStrength);
                shader.setFloatUniform("uCurvature", p.edgeCurvature);
                shader.setFloatUniform("uLensZoom", p.lensDistortion);
                shader.setFloatUniform("uDispersion", p.chromaticAberration * 0.14f);
                shader.setFloatUniform("uTransparency", p.transparency);
                shader.setFloatUniform("uTint", p.tintRed(), p.tintGreen(), p.tintBlue(), p.tintOpacity);
                shader.setFloatUniform("uLight", lightDirX, lightDirY);
                shader.setFloatUniform("uLightIntensity", p.specularIntensity);
                shader.setFloatUniform("uContourStrength", p.edgeIllumination);
                shader.setFloatUniform("uIsClear", isClear ? 1f : 0f);
                shader.setFloatUniform("uDarkMode", p.darkModeUniform());
                break;
            }
            case VITRO_IOS26: {
                float bevel = Math.max(2f, p.glassThicknessDp * density);
                shader.setFloatUniform("uBevel", bevel);
                shader.setFloatUniform("uDepth", bevel * 0.95f * p.refractionStrength);
                shader.setFloatUniform("uCurvature", p.edgeCurvature);
                shader.setFloatUniform("uDispersion", p.chromaticAberration * 0.16f);
                shader.setFloatUniform("uZoom", clamp(1f - p.lensDistortion * 0.18f, 0.65f, 1.25f));
                shader.setFloatUniform("uSaturation", isClear ? 1.15f : 1.32f);
                shader.setFloatUniform("uBrightness", clamp((1f - p.transparency) * 0.65f * (isClear ? 0.18f : 1f), 0f, 1f));
                shader.setFloatUniform("uLight", lightDirX, lightDirY);
                shader.setFloatUniform("uHighlight", p.specularIntensity * p.edgeIllumination);
                shader.setFloatUniform("uTint", p.tintRed(), p.tintGreen(), p.tintBlue(), p.tintOpacity * 0.85f);
                shader.setFloatUniform("uDimming", isClear ? 0.08f : 0f);
                shader.setFloatUniform("uDarkMode", p.darkModeUniform());
                break;
            }
            case HAZE_GLASS: {
                float h = Math.max(3f, p.glassThicknessDp * density);
                shader.setFloatUniform("uWideStripY", wideStripY);
                shader.setFloatUniform("uWideScale", wideScale);
                shader.setFloatUniform("uBlurPx", Math.max(0f, p.blurRadiusDp * density));
                shader.setFloatUniform("uRefractionHeight", h);
                shader.setFloatUniform("uRefractionScale", h * 1.1f * p.refractionStrength);
                shader.setFloatUniform("uFoldStrength", isClear ? 0.35f : 0.72f * clamp(p.edgeCurvature, 0.3f, 1.5f));
                shader.setFloatUniform("uSurfaceProfile", (float) p.surfaceProfile);
                shader.setFloatUniform("uLensDistortion", p.lensDistortion);
                shader.setFloatUniform("uChromaMult", isClear ? 1.12f : 1.32f);
                shader.setFloatUniform("uAberration", p.chromaticAberration * 0.22f);
                shader.setFloatUniform("uTransparency", p.transparency);
                shader.setFloatUniform("uTint", p.tintRed(), p.tintGreen(), p.tintBlue(), p.tintOpacity);
                shader.setFloatUniform("uLight", lightDirX, lightDirY);
                shader.setFloatUniform("uSpecular", p.specularIntensity);
                shader.setFloatUniform("uEdgeIllum", p.edgeIllumination);
                shader.setFloatUniform("uIsClear", isClear ? 1f : 0f);
                shader.setFloatUniform("uDarkMode", p.darkModeUniform());
                break;
            }
            case SOHUM_SNELL: {
                shader.setFloatUniform("uBezelPx", Math.max(3f, p.glassThicknessDp * density));
                shader.setFloatUniform("uRefractScale", p.refractionStrength);
                shader.setFloatUniform("uProfilePower", clamp(2f + p.edgeCurvature * 2f, 2f, 6f));
                shader.setFloatUniform("uBaseT", clamp(0.35f + p.lensDistortion * 0.4f, 0.05f, 1f));
                shader.setFloatUniform("uDispersionStrength", p.chromaticAberration * 10f);
                shader.setFloatUniform("uTransparency", p.transparency);
                shader.setFloatUniform("uTint", p.tintRed(), p.tintGreen(), p.tintBlue(), p.tintOpacity);
                shader.setFloatUniform("uLight", lightDirX, lightDirY);
                shader.setFloatUniform("uSpecular", p.specularIntensity);
                shader.setFloatUniform("uEdgeIllum", p.edgeIllumination);
                shader.setFloatUniform("uIsClear", isClear ? 1f : 0f);
                shader.setFloatUniform("uDarkMode", p.darkModeUniform());
                break;
            }
            case MOCHI_G2: {
                float h = Math.max(3f, p.glassThicknessDp * density);
                shader.setFloatUniform("uWideStripY", wideStripY);
                shader.setFloatUniform("uWideScale", wideScale);
                shader.setFloatUniform("uBlurPx", Math.max(0f, p.blurRadiusDp * density));
                shader.setFloatUniform("uInnerAmt", h * 0.85f * p.refractionStrength);
                shader.setFloatUniform("uInnerInvH", 1f / h);
                shader.setFloatUniform("uOuterAmt", h * 0.35f * p.refractionStrength);
                shader.setFloatUniform("uCurvature", p.edgeCurvature);
                shader.setFloatUniform("uLensZoom", p.lensDistortion);
                shader.setFloatUniform("uAberration", p.chromaticAberration * 0.15f);
                shader.setFloatUniform("uTransparency", p.transparency);
                shader.setFloatUniform("uTint", p.tintRed(), p.tintGreen(), p.tintBlue(), p.tintOpacity);
                shader.setFloatUniform("uLight", lightDirX, lightDirY);
                shader.setFloatUniform("uSpecular", p.specularIntensity);
                shader.setFloatUniform("uEdgeIllum", p.edgeIllumination);
                shader.setFloatUniform("uIsClear", isClear ? 1f : 0f);
                shader.setFloatUniform("uDarkMode", p.darkModeUniform());
                break;
            }
            case KYANT_BACKDROP: {
                float h = Math.max(2f, p.glassThicknessDp * density);
                shader.setFloatUniform("uRefractionHeight", h);
                shader.setFloatUniform("uRefractionAmount", -h * 1.15f * p.refractionStrength);
                shader.setFloatUniform("uDepthEffect", p.lensDistortion);
                shader.setFloatUniform("uCurvature", p.edgeCurvature);
                shader.setFloatUniform("uChromaticAberration", p.chromaticAberration);
                shader.setFloatUniform("uTransparency", p.transparency);
                shader.setFloatUniform("uTint", p.tintRed(), p.tintGreen(), p.tintBlue(), p.tintOpacity);
                shader.setFloatUniform("uLightAngle", lightRad);
                shader.setFloatUniform("uSpecular", p.specularIntensity);
                shader.setFloatUniform("uEdgeIllum", p.edgeIllumination);
                shader.setFloatUniform("uIsClear", isClear ? 1f : 0f);
                shader.setFloatUniform("uDarkMode", p.darkModeUniform());
                break;
            }
            case QWEA0_GRAVITY: {
                float bevel = Math.max(3f, p.glassThicknessDp * density);
                boolean outward = (p.outwardSamplingOverride != null) ? p.outwardSamplingOverride : true;
                shader.setFloatUniform("uBevel", bevel);
                shader.setFloatUniform("uRefractPx", bevel * 1.15f * p.refractionStrength);
                shader.setFloatUniform("uFalloff", clamp(p.edgeCurvature * 2f, 0f, 5f));
                shader.setFloatUniform("uRefractDir", outward ? 1f : -1f);
                shader.setFloatUniform("uRimSoft", 1.25f * density);
                shader.setFloatUniform("uLensZoom", p.lensDistortion);
                shader.setFloatUniform("uDispersion", p.chromaticAberration * 0.24f);
                shader.setFloatUniform("uLight", lightDirX, lightDirY);
                shader.setFloatUniform("uSpecStrength", p.specularIntensity);
                shader.setFloatUniform("uEdgeIllum", p.edgeIllumination);
                shader.setFloatUniform("uTransparency", p.transparency);
                shader.setFloatUniform("uGlassTint", p.tintRed(), p.tintGreen(), p.tintBlue(), p.tintOpacity);
                shader.setFloatUniform("uSatFactor", isClear ? 1.15f : 1.45f);
                shader.setFloatUniform("uIsClear", isClear ? 1f : 0f);
                shader.setFloatUniform("uDarkMode", p.darkModeUniform());
                break;
            }
            case FROSTED_BASELINE: {
                shader.setFloatUniform("uTransparency", p.transparency);
                shader.setFloatUniform("uTint", p.tintRed(), p.tintGreen(), p.tintBlue(), p.tintOpacity);
                shader.setFloatUniform("uBorderAlpha", 0.42f * p.edgeIllumination);
                shader.setFloatUniform("uIsClear", isClear ? 1f : 0f);
                shader.setFloatUniform("uDarkMode", p.darkModeUniform());
                break;
            }
        }
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
