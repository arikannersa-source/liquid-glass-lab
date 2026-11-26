package com.liquidglass.sdk.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Pure Kotlin optical reference math matching the AGSL shaders and `docs/OPTICAL_SPECIFICATION.md`.
 *
 * Framework-independent: used for unit-testing mathematical invariants and documenting exact
 * formulas for future cross-platform ports (iOS/Metal, WebGL/WGSL, Flutter/Impeller).
 */
object GlassOpticsMath {

    /** BK7 crown glass refractive indices (Fraunhofer d, C, F lines). */
    const val BK7_N_D = 1.5168f
    const val BK7_N_C = 1.5143f
    const val BK7_N_F = 1.5224f

    /** Superellipse bevel height: f(x) = (1 - (1 - x)^p)^(1/p), x in [0, 1]. */
    fun superellipseHeight(x: Float, power: Float = 4f): Float {
        val u = (1f - x.coerceIn(0f, 1f)).coerceIn(0f, 1f)
        return (1f - u.pow(power)).coerceAtLeast(0f).pow(1f / power)
    }

    /** Derivative df/dx of the superellipse bevel height profile. */
    fun superellipseSlope(x: Float, power: Float = 4f): Float {
        val u = (1f - x.coerceIn(0.002f, 0.998f))
        val up = u.pow(power)
        return u.pow(power - 1f) / max(1f - up, 1e-4f).pow(1f - 1f / power)
    }

    /**
     * Closed-form exact Snell deviation tan(theta_i - theta_t) without transcendental calls
     * (from shayann07/liquidglass GlassShaderSource.kt).
     */
    fun snellShiftClosedForm(slope: Float, ior: Float = BK7_N_D): Float {
        val inv = 1f / sqrt(1f + slope * slope)
        val si = slope * inv
        val ci = inv
        val st = si / max(ior, 1.0001f)
        val ct = sqrt(max(1f - st * st, 0f))
        return (si * ct - ci * st) / max(ci * ct + si * st, 1e-4f)
    }

    /** Direct trigonometric Snell deviation for reference verification against [snellShiftClosedForm]. */
    fun snellShiftTrigonometric(slope: Float, ior: Float = BK7_N_D): Float {
        val thetaI = atan(slope)
        val thetaT = asin((sin(thetaI) / max(ior, 1.0001f)).coerceIn(-1f, 1f))
        return tan(thetaI - thetaT)
    }

    /** Exact unpolarized Fresnel power reflectance R(theta_i) for air -> medium of index [ior]. */
    fun fresnelUnpolarized(thetaI: Float, ior: Float = BK7_N_D): Float {
        val ci = cos(thetaI)
        val st = sin(thetaI) / max(ior, 1.0001f)
        if (st >= 1f) return 1f
        val ct = sqrt(max(1f - st * st, 0f))
        val rs = (ci - ior * ct) / (ci + ior * ct)
        val rp = (ior * ci - ct) / (ior * ci + ct)
        return 0.5f * (rs * rs + rp * rp)
    }

    /** Schlick Fresnel reflectance approximation. */
    fun fresnelSchlick(cosTheta: Float, ior: Float = BK7_N_D): Float {
        val r0Root = (ior - 1f) / (ior + 1f)
        val r0 = r0Root * r0Root
        return r0 + (1f - r0) * (1f - cosTheta.coerceIn(0f, 1f)).pow(5f)
    }

    /** sohumsuthar macOS 26 Control Center calibrated displacement profile (in units of bezel width B). */
    fun sohumDisplacement(x: Float, ior: Float = BK7_N_D, baseT: Float = 0.35f, fresnelFade: Boolean = true): Float {
        val slope = superellipseSlope(x, 4f)
        val thetaS = atan(slope)
        val thetaR = asin((sin(thetaS) / ior).coerceIn(-1f, 1f))
        val t = baseT + superellipseHeight(x, 4f)
        var d = t * tan(thetaS - thetaR)
        if (fresnelFade) {
            d *= (1f - fresnelUnpolarized(thetaS, ior))
        }
        return d
    }

    /** sohumsuthar macOS 26 Control Center calibrated luminance transfer line: L_glass = (1-a)*b*L + 255*a. */
    fun sohumTransferLuminance(backdropLuma255: Float, alpha: Float = 0.134f, beta: Float = 0.67f): Float {
        return (1f - alpha) * beta * backdropLuma255 + 255f * alpha
    }

    /** sohumsuthar analytical chroma compensation factor S = 1 / ((1 - alpha) * beta). */
    fun sohumChromaCompensation(alpha: Float = 0.134f, beta: Float = 0.67f): Float {
        return 1f / ((1f - alpha) * beta)
    }

    /** Kyant0 circular arc displacement map: 1 - sqrt(1 - x^2). */
    fun kyantCircleMap(x: Float): Float {
        val c = x.coerceIn(0f, 1f)
        return 1f - sqrt(max(0f, 1f - c * c))
    }

    /** QWEA0 inverse-power gravitational lens profile. */
    fun qwea0GravitySlope(t: Float, falloffPower: Float = 2f): Float {
        val clamped = t.coerceIn(0f, 1f)
        if (falloffPower <= 0.001f) {
            val e = 1f - clamped
            return e * e
        }
        val gb = 5f.pow(-falloffPower)
        return ((1f + 4f * clamped).pow(-falloffPower) - gb) / (1f - gb)
    }

    /** shayann07 iOS 27 dual-kernel measured material equation: lin = B + k*(D - B) + c*|D - B|. */
    fun shayannDualKernel(wide: Float, detail: Float, fineShare: Float, detailLift: Float): Float {
        val diff = detail - wide
        return wide + fineShare * diff + detailLift * abs(diff)
    }

    /** shayann07 iOS 27 measured dark tone curve (levels 0..255): 31.72 + 254.40*x - 101.01*x^2. */
    fun shayannDarkTone255(x01: Float): Float {
        val x = x01.coerceIn(0f, 1f)
        return (31.72f + 254.40f * x - 101.01f * x * x).coerceIn(0f, 255f)
    }

    /** itshedimisawi Vitro iPhone-calibrated dark glass bend curve. */
    fun vitroDarkBend(luma: Float): Float {
        val l = luma.coerceIn(0f, 1f)
        return 0.19f * (1f - l).pow(16f) - 1.0f * max(l - 0.33f, 0f).pow(1.5f)
    }

    /** Exact rounded rectangle SDF (negative inside, zero on outline, positive outside). */
    fun sdRoundedRect(px: Float, py: Float, halfW: Float, halfH: Float, radius: Float): Float {
        val r = min(radius, min(halfW, halfH))
        val qx = abs(px) - halfW + r
        val qy = abs(py) - halfH + r
        val outside = hypot(max(qx, 0f), max(qy, 0f))
        val inside = min(max(qx, qy), 0f)
        return outside + inside - r
    }

    /** Lame superellipse corner SDF (power=2 circle, power=4 Apple squircle). */
    fun sdSquircleRect(px: Float, py: Float, halfW: Float, halfH: Float, radius: Float, power: Float = 4f): Float {
        val r = min(radius, min(halfW, halfH))
        val qx = abs(px) - halfW + r
        val qy = abs(py) - halfH + r
        val mx = max(qx, 0f)
        val my = max(qy, 0f)
        val corner = if (power <= 2.001f) hypot(mx, my) else (mx.pow(power) + my.pow(power)).pow(1f / power)
        return min(max(qx, qy), 0f) + corner - r
    }

    /** Polynomial smooth minimum (smin) for liquid merging/morphing. */
    fun sminPoly(a: Float, b: Float, k: Float): Float {
        if (k <= 1e-4f) return min(a, b)
        val h = (0.5f + 0.5f * (b - a) / k).coerceIn(0f, 1f)
        return b * (1f - h) + a * h - k * h * (1f - h)
    }

    /** Linear RGB to Oklab conversion (matching GlassOklabSource.kt). */
    fun linearToOklab(r: Float, g: Float, b: Float): FloatArray {
        val l = 0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b
        val m = 0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b
        val s = 0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b
        val lC = max(l, 0f).pow(1f / 3f)
        val mC = max(m, 0f).pow(1f / 3f)
        val sC = max(s, 0f).pow(1f / 3f)
        return floatArrayOf(
            0.2104542553f * lC + 0.7936177850f * mC - 0.0040720468f * sC,
            1.9779984951f * lC - 2.4285922050f * mC + 0.4505937099f * sC,
            0.0259040371f * lC + 0.7827717662f * mC - 0.8086757660f * sC
        )
    }

    /** Oklab to Linear RGB conversion. */
    fun oklabToLinear(L: Float, a: Float, b: Float): FloatArray {
        val lC = L + 0.3963377774f * a + 0.2158037573f * b
        val mC = L - 0.1055613458f * a - 0.0638541728f * b
        val sC = L - 0.0894841775f * a - 1.2914855480f * b
        val l = lC * lC * lC
        val m = mC * mC * mC
        val s = sC * sC * sC
        return floatArrayOf(
            4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s,
            -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s,
            -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s
        )
    }

    /** ITU-R BT.709 perceptual relative luminance. */
    fun luma709(r: Float, g: Float, b: Float): Float = 0.2126f * r + 0.7152f * g + 0.0722f * b
}
