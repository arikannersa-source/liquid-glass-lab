package com.liquidglass.sdk.core

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Complete optical and interactive parameter set for Liquid Glass evaluation and reuse.
 *
 * Covers all 16 laboratory customization axes plus Apple Regular/Clear variants and
 * adaptive light/dark material appearance.
 */
@Immutable
data class GlassParameters(
    val variant: GlassVariant = GlassVariant.REGULAR,
    val appearance: GlassAppearance = GlassAppearance.ADAPTIVE,
    /** Overall optical displacement multiplier (0.0 = off, 1.0 = calibrated default, 2.0 = extreme). */
    val refractionStrength: Float = 1.0f,
    /** Bevel / refraction band thickness in dp (2..60 dp). */
    val glassThicknessDp: Float = 24f,
    /** Bevel profile curvature / exponent (0.2..2.0; 1.0 = engine's native profile). */
    val edgeCurvature: Float = 1.0f,
    /** Interior lens magnification / dome depth distortion (-0.5..1.0). */
    val lensDistortion: Float = 0.25f,
    /** Backdrop blur radius in dp (0..48 dp). */
    val blurRadiusDp: Float = 12f,
    /** Material transparency (0.0 = dense frosted, 1.0 = ultra-clear). */
    val transparency: Float = 0.65f,
    /** Tint color as ARGB 32-bit unsigned value (e.g. 0xFFFFFFFF for neutral white). */
    val tintColorArgb: Long = 0xFFFFFFFFL,
    /** Tint blend / absorption opacity (0.0..0.95). */
    val tintOpacity: Float = 0.18f,
    /** Wavelength chromatic dispersion strength (0.0..2.0). */
    val chromaticAberration: Float = 0.35f,
    /** Directional specular highlight intensity (0.0..2.0). */
    val specularIntensity: Float = 1.0f,
    /** Light source direction in degrees (0 = right, 90 = bottom, 135 = bottom-left, 225 = top-left, 270 = top). */
    val lightAngleDegrees: Float = 225f,
    /** Fresnel rim glow & outer contour illumination balance (0.0..2.0). */
    val edgeIllumination: Float = 1.0f,
    /** Drop shadow & inner bevel shadow strength (0.0..2.0). */
    val shadowIntensity: Float = 0.85f,
    /** Corner radius in dp (0..64 dp; clamped to half the shortest side per shape). */
    val cornerRadiusDp: Float = 26f,
    /** Elastic touch press bulge, squash-stretch, and fingertip glow (0.0..2.0). */
    val touchDeformation: Float = 1.0f,
    /** Spring & animation speed multiplier (0.25..2.5). */
    val animationSpeed: Float = 1.0f,
    /** Liquid smooth-minimum (smin) morphing fusion radius in dp (0..60 dp). */
    val morphSmoothingDp: Float = 28f,
    /** Surface profile selector for multi-profile engines like Haze (0=Circle, 1=Squircle, 2=Concave, 3=Lip). */
    val surfaceProfile: Int = 1,
    /** Optional override for outward (+1) vs inward (-1) sampling where supported (null = engine default). */
    val outwardSamplingOverride: Boolean? = null
) {
    val tintColor: Color get() = Color(tintColorArgb.toULong() shl 32)

    fun tintRed(): Float = ((tintColorArgb shr 16) and 0xFFL) / 255f
    fun tintGreen(): Float = ((tintColorArgb shr 8) and 0xFFL) / 255f
    fun tintBlue(): Float = (tintColorArgb and 0xFFL) / 255f

    /** Effective dark-mode factor: 0.0 = light, 1.0 = dark, -1.0 = auto-adaptive from backdrop luma. */
    fun darkModeUniform(): Float = when (appearance) {
        GlassAppearance.LIGHT -> 0f
        GlassAppearance.DARK -> 1f
        GlassAppearance.ADAPTIVE -> -1f
    }

    /** Clamped copy ensuring safe numerical ranges for all GPU shaders. */
    fun sanitized(): GlassParameters = copy(
        refractionStrength = refractionStrength.coerceIn(0f, 2.5f),
        glassThicknessDp = glassThicknessDp.coerceIn(0f, 64f),
        edgeCurvature = edgeCurvature.coerceIn(0f, 2.5f),
        lensDistortion = lensDistortion.coerceIn(-0.5f, 1.0f),
        blurRadiusDp = blurRadiusDp.coerceIn(0f, 48f),
        transparency = transparency.coerceIn(0f, 1f),
        tintOpacity = tintOpacity.coerceIn(0f, 0.95f),
        chromaticAberration = chromaticAberration.coerceIn(0f, 2.5f),
        specularIntensity = specularIntensity.coerceIn(0f, 2.5f),
        lightAngleDegrees = ((lightAngleDegrees % 360f) + 360f) % 360f,
        edgeIllumination = edgeIllumination.coerceIn(0f, 2.5f),
        shadowIntensity = shadowIntensity.coerceIn(0f, 2.5f),
        cornerRadiusDp = cornerRadiusDp.coerceIn(0f, 64f),
        touchDeformation = touchDeformation.coerceIn(0f, 2.5f),
        animationSpeed = animationSpeed.coerceIn(0.25f, 2.5f),
        morphSmoothingDp = morphSmoothingDp.coerceIn(0f, 64f),
        surfaceProfile = surfaceProfile.coerceIn(0, 3)
    )
}
