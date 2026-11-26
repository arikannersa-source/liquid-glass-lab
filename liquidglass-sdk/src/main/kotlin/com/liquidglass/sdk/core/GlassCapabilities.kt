package com.liquidglass.sdk.core

import androidx.compose.runtime.Immutable

/**
 * Declares which optical customization parameters a rendering engine genuinely implements.
 *
 * The laboratory UI uses this mask to disclose unsupported features honestly rather than
 * presenting non-functional sliders.
 */
@Immutable
data class GlassCapabilities(
    val refractionStrength: Boolean = true,
    val glassThickness: Boolean = true,
    val edgeCurvature: Boolean = true,
    val lensDistortion: Boolean = true,
    val backgroundBlur: Boolean = true,
    val transparency: Boolean = true,
    val tint: Boolean = true,
    val chromaticAberration: Boolean = true,
    val specularHighlights: Boolean = true,
    val lightingDirection: Boolean = true,
    val edgeIllumination: Boolean = true,
    val shadows: Boolean = true,
    val cornerRadius: Boolean = true,
    val touchDeformation: Boolean = true,
    val animationSpeed: Boolean = true,
    val morphingBehavior: Boolean = true,
    val supportsClearVariant: Boolean = true,
    val supportsHazeSurfaceProfiles: Boolean = false,
    val supportsDirectionToggle: Boolean = false
) {
    companion object {
        val FullLiquidGlass = GlassCapabilities()

        val FrostedOnly = GlassCapabilities(
            refractionStrength = false,
            glassThickness = false,
            edgeCurvature = false,
            lensDistortion = false,
            backgroundBlur = true,
            transparency = true,
            tint = true,
            chromaticAberration = false,
            specularHighlights = false,
            lightingDirection = false,
            edgeIllumination = true,
            shadows = true,
            cornerRadius = true,
            touchDeformation = true,
            animationSpeed = true,
            morphingBehavior = false,
            supportsClearVariant = true
        )
    }
}
