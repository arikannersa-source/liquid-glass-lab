package com.liquidglass.lab

import com.liquidglass.lab.model.BackgroundMode
import com.liquidglass.lab.model.LabCheckerSizesDp
import com.liquidglass.lab.model.LabPhotoTitles
import com.liquidglass.lab.model.LabSolidSwatches
import com.liquidglass.lab.model.LabViewMode
import com.liquidglass.sdk.core.GlassEngineId
import com.liquidglass.sdk.core.GlassEngineRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LabModelUnitTest {

    @Test
    fun allEightRequiredBackgroundModesAreAvailable() {
        assertEquals(8, BackgroundMode.entries.size)
        assertEquals(3, LabPhotoTitles.size)
        assertTrue(LabSolidSwatches.size >= 6)
        assertEquals(listOf(16f, 32f, 56f), LabCheckerSizesDp)
    }

    @Test
    fun allFourLaboratoryViewModesAreAvailable() {
        assertEquals(
            listOf(
                LabViewMode.FULL_SHOWCASE,
                LabViewMode.DRAG_PLAYGROUND,
                LabViewMode.MORPH_BENCH,
                LabViewMode.SIDE_BY_SIDE
            ),
            LabViewMode.entries
        )
    }

    @Test
    fun nineEnginesIncludeEightRefractiveEnginesPlusFrostedBaseline() {
        val all = GlassEngineRegistry.all()
        assertEquals(9, all.size)
        val refractive = all.filter { it.id != GlassEngineId.FROSTED_BASELINE }
        assertEquals(8, refractive.size)
        refractive.forEach { engine ->
            assertTrue("${engine.engineKey} must support refractionStrength", engine.capabilities.refractionStrength)
            assertTrue("${engine.engineKey} must support chromaticAberration", engine.capabilities.chromaticAberration)
            assertTrue("${engine.engineKey} must support morphingBehavior", engine.capabilities.morphingBehavior)
        }
        val frosted = GlassEngineRegistry.get(GlassEngineId.FROSTED_BASELINE)
        assertTrue(!frosted.capabilities.refractionStrength)
        assertTrue(!frosted.capabilities.chromaticAberration)
    }
}
