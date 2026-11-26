package com.liquidglass.sdk

import com.liquidglass.sdk.core.GlassOpticsMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

class GlassOpticsMathTest {

    @Test
    fun snellClosedFormMatchesTrigonometricAcrossAllSlopes() {
        val slopes = floatArrayOf(0f, 0.1f, 0.5f, 1.0f, 2.5f, 6.0f, 20.0f)
        for (s in slopes) {
            val closed = GlassOpticsMath.snellShiftClosedForm(s, GlassOpticsMath.BK7_N_D)
            val trig = GlassOpticsMath.snellShiftTrigonometric(s, GlassOpticsMath.BK7_N_D)
            assertEquals("Mismatch at slope=$s", trig, closed, 1e-4f)
        }
    }

    @Test
    fun fresnelUnpolarizedNormalAndGrazingLimits() {
        val normalR = GlassOpticsMath.fresnelUnpolarized(0f, GlassOpticsMath.BK7_N_D)
        // Normal incidence glass air->BK7: ((1.5168-1)/(1.5168+1))^2 ≈ 0.0422
        assertEquals(0.0422f, normalR, 0.002f)

        val grazingR = GlassOpticsMath.fresnelUnpolarized((PI / 2.0 - 0.001).toFloat(), GlassOpticsMath.BK7_N_D)
        assertTrue("Grazing reflectance should approach 1.0, got $grazingR", grazingR > 0.95f)
    }

    @Test
    fun sohumMacOs26TransferCurveMatchesMeasuredNumbers() {
        // Measured macOS 26 Control Center: L_glass = 0.58 * L + 34
        val blackLift = GlassOpticsMath.sohumTransferLuminance(9f)
        assertEquals(39.4f, blackLift, 2.5f) // L=9 lifts ~4x to ~37-40
        val satRestore = GlassOpticsMath.sohumChromaCompensation()
        assertEquals(1.72f, satRestore, 0.02f)
    }

    @Test
    fun shayannIos27DarkToneAndDualKernelBehavior() {
        assertEquals(31.72f, GlassOpticsMath.shayannDarkTone255(0f), 0.1f)
        assertEquals(185.11f, GlassOpticsMath.shayannDarkTone255(1f), 0.1f)
        // One-sided detail: positive c amplifies positive detail more than negative
        val posDetail = GlassOpticsMath.shayannDualKernel(0.5f, 0.8f, 0.27f, 0.22f)
        val negDetail = GlassOpticsMath.shayannDualKernel(0.5f, 0.2f, 0.27f, 0.22f)
        assertTrue(posDetail - 0.5f > 0.5f - negDetail)
    }

    @Test
    fun sminPolyCreatesSmoothBridgeBetweenAdjacentShapes() {
        // Two circles of radius 30 spaced 70 apart -> gap of 10px at midpoint (d1 = 5, d2 = 5)
        val d1 = 5f
        val d2 = 5f
        val hardUnion = minOf(d1, d2)
        val smoothUnion = GlassOpticsMath.sminPoly(d1, d2, 30f)
        assertEquals(5f, hardUnion, 1e-4f)
        assertTrue("Expected negative interior bridge distance, got $smoothUnion", smoothUnion < 0f)
    }

    @Test
    fun oklabRoundTripPreservesLinearRgb() {
        val samples = listOf(
            floatArrayOf(0.8f, 0.2f, 0.15f),
            floatArrayOf(0.1f, 0.7f, 0.4f),
            floatArrayOf(0.2f, 0.3f, 0.9f),
            floatArrayOf(0.5f, 0.5f, 0.5f)
        )
        for (rgb in samples) {
            val lab = GlassOpticsMath.linearToOklab(rgb[0], rgb[1], rgb[2])
            val back = GlassOpticsMath.oklabToLinear(lab[0], lab[1], lab[2])
            assertTrue(abs(rgb[0] - back[0]) < 1e-3f)
            assertTrue(abs(rgb[1] - back[1]) < 1e-3f)
            assertTrue(abs(rgb[2] - back[2]) < 1e-3f)
        }
    }

    @Test
    fun gravityLensProfileMonotonicallyDecaysFromEdgeToInterior() {
        val atEdge = GlassOpticsMath.qwea0GravitySlope(0f, 2f)
        val atMid = GlassOpticsMath.qwea0GravitySlope(0.25f, 2f)
        val atEnd = GlassOpticsMath.qwea0GravitySlope(1f, 2f)
        assertEquals(1f, atEdge, 1e-4f)
        assertEquals(0f, atEnd, 1e-4f)
        assertTrue("Inverse-power profile concentrates bend near rim (mid=$atMid)", atMid in 0.18f..0.28f)
    }
}
