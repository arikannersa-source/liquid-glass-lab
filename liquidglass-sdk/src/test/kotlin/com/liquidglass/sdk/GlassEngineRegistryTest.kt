package com.liquidglass.sdk

import com.liquidglass.sdk.core.GlassAppearance
import com.liquidglass.sdk.core.GlassEngineId
import com.liquidglass.sdk.core.GlassEngineRegistry
import com.liquidglass.sdk.core.GlassVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassEngineRegistryTest {

    @Test
    fun allNineEnginesAreRegisteredWithCompleteMetadata() {
        val engines = GlassEngineRegistry.all()
        assertEquals(9, engines.size)
        for (engine in engines) {
            assertTrue("Missing displayName on ${engine.engineKey}", engine.displayName.isNotBlank())
            assertTrue("Missing license on ${engine.engineKey}", engine.license.isNotBlank())
            assertTrue("Missing sourceRepoUrl on ${engine.engineKey}", engine.sourceRepoUrl.startsWith("https://"))
            assertTrue("Missing agslSource on ${engine.engineKey}", engine.agslSource.contains("half4 main(float2"))
        }
    }

    @Test
    fun frostedBaselineHonestyDisclosesUnsupportedOpticalControls() {
        val baseline = GlassEngineRegistry.get(GlassEngineId.FROSTED_BASELINE)
        assertFalse(baseline.capabilities.refractionStrength)
        assertFalse(baseline.capabilities.chromaticAberration)
        assertFalse(baseline.capabilities.specularHighlights)
        assertFalse(baseline.capabilities.morphingBehavior)
        assertTrue(baseline.capabilities.backgroundBlur)
        assertTrue(baseline.capabilities.transparency)
    }

    @Test
    fun regularAndClearVariantsProduceDistinctParametersOnEveryEngine() {
        for (engine in GlassEngineRegistry.all()) {
            val regular = engine.defaultParameters(GlassVariant.REGULAR, GlassAppearance.ADAPTIVE)
            val clear = engine.defaultParameters(GlassVariant.CLEAR, GlassAppearance.ADAPTIVE)
            assertEquals(GlassVariant.REGULAR, regular.variant)
            assertEquals(GlassVariant.CLEAR, clear.variant)
            assertTrue(
                "Clear variant should be more transparent than Regular on ${engine.engineKey}",
                clear.transparency > regular.transparency
            )
            assertTrue(
                "Clear variant should use less blur than Regular on ${engine.engineKey}",
                clear.blurRadiusDp < regular.blurRadiusDp
            )
            assertNotEquals(regular, clear)
        }
    }
}
