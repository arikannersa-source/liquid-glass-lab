package com.liquidglass.sdk

import com.liquidglass.sdk.core.GlassAppearance
import com.liquidglass.sdk.core.GlassEngineId
import com.liquidglass.sdk.core.GlassEngineRegistry
import com.liquidglass.sdk.core.GlassParameters
import com.liquidglass.sdk.core.GlassPreset
import com.liquidglass.sdk.core.GlassVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassPresetJsonTest {

    @Test
    fun allBuiltInPresetsRoundTripLosslesslyThroughJson() {
        val presets = GlassEngineRegistry.builtInPresets()
        assertTrue("Expected at least 10 built-in presets", presets.size >= 10)

        for (preset in presets) {
            val json = preset.toJson()
            val parsed = GlassPreset.fromJson(json).getOrThrow()
            assertEquals(preset.name, parsed.name)
            assertEquals(preset.engineKey, parsed.engineKey)
            assertEquals(preset.parameters.variant, parsed.parameters.variant)
            assertEquals(preset.parameters.appearance, parsed.parameters.appearance)
            assertEquals(preset.parameters.refractionStrength, parsed.parameters.refractionStrength, 1e-4f)
            assertEquals(preset.parameters.glassThicknessDp, parsed.parameters.glassThicknessDp, 1e-4f)
            assertEquals(preset.parameters.blurRadiusDp, parsed.parameters.blurRadiusDp, 1e-4f)
            assertEquals(preset.parameters.tintColorArgb, parsed.parameters.tintColorArgb)
            assertEquals(preset.parameters.chromaticAberration, parsed.parameters.chromaticAberration, 1e-4f)
            assertEquals(preset.parameters.specularIntensity, parsed.parameters.specularIntensity, 1e-4f)
            assertEquals(preset.parameters.lightAngleDegrees, parsed.parameters.lightAngleDegrees, 1e-4f)
            assertEquals(preset.parameters.morphSmoothingDp, parsed.parameters.morphSmoothingDp, 1e-4f)
        }
    }

    @Test
    fun customPresetSanitizesOutOfRangeValues() {
        val extreme = GlassPreset(
            name = "Custom Stress",
            engineKey = GlassEngineId.HAZE_GLASS.key,
            parameters = GlassParameters(
                variant = GlassVariant.CLEAR,
                appearance = GlassAppearance.DARK,
                refractionStrength = 99f,
                blurRadiusDp = -10f,
                transparency = 5f,
                tintColorArgb = 0xFF0A84FFL,
                lightAngleDegrees = 450f,
                surfaceProfile = 3,
                outwardSamplingOverride = false
            )
        )
        val parsed = GlassPreset.fromJson(extreme.toJson()).getOrThrow()
        assertEquals(2.5f, parsed.parameters.refractionStrength, 1e-4f)
        assertEquals(0f, parsed.parameters.blurRadiusDp, 1e-4f)
        assertEquals(1f, parsed.parameters.transparency, 1e-4f)
        assertEquals(90f, parsed.parameters.lightAngleDegrees, 1e-4f)
        assertEquals(3, parsed.parameters.surfaceProfile)
        assertEquals(false, parsed.parameters.outwardSamplingOverride)
        assertEquals(0xFF0A84FFL, parsed.parameters.tintColorArgb)
    }

    @Test
    fun invalidJsonReturnsFailureResultWithoutThrowing() {
        val result = GlassPreset.fromJson("{ not valid json !!!")
        assertTrue(result.isFailure)
    }
}
