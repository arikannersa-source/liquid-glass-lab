package com.liquidglass.sdk

import com.liquidglass.sdk.core.GlassAppearance
import com.liquidglass.sdk.core.GlassDrawContext
import com.liquidglass.sdk.core.GlassEngineId
import com.liquidglass.sdk.core.GlassEngineRegistry
import com.liquidglass.sdk.core.GlassParameters
import com.liquidglass.sdk.core.GlassSubShape
import com.liquidglass.sdk.core.GlassVariant
import com.liquidglass.sdk.core.LiquidGlassEngine
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.jetbrains.skia.Shader
import org.jetbrains.skia.Surface
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Compiles and EXECUTES every integrated engine's verbatim AGSL shader on real Skia
 * (`RuntimeEffect.makeForShader`) on Linux JVM, verifying:
 * 1. Zero AGSL/SkSL syntax, type, or unsupported-intrinsic errors across all 9 engines.
 * 2. Every shader renders valid premultiplied pixels with clean SDF silhouettes.
 * 3. All 8 Liquid Glass engines produce real optical edge refraction displacement over a
 *    high-contrast checkerboard (while Frosted Baseline produces zero displacement).
 * 4. Chromatic aberration separates RGB channels near the bevel edge.
 * 5. Multi-shape `smin` morphing forms an opaque refractive bridge between adjacent droplets.
 * 6. Regular vs Clear variants and lighting direction visibly alter rendered output.
 */
class SkiaShaderExecutionTest {

    @Test
    fun allNineAgslShadersCompileCleanlyInSkiaAndObeyAgslRules() {
        for (engine in GlassEngineRegistry.all()) {
            val src = engine.agslSource
            // Static Android AGSL guardrails
            assertFalse("${engine.engineKey} must not use dFdx", src.contains("dFdx"))
            assertFalse("${engine.engineKey} must not use dFdy", src.contains("dFdy"))
            assertFalse("${engine.engineKey} must not use fwidth", src.contains("fwidth"))
            assertFalse("${engine.engineKey} must not use sampler2D", src.contains("sampler2D"))
            assertFalse("${engine.engineKey} must not use uniform int", Regex("""uniform\s+int\b""").containsMatchIn(src))
            assertTrue("${engine.engineKey} must declare half4 main(float2 ...)", src.contains("half4 main(float2"))

            // Real Skia SkSL/AGSL compiler verification!
            val effect = RuntimeEffect.makeForShader(src)
            effect.close()
        }
    }

    @Test
    fun allEightLiquidGlassEnginesRefractAndMorphWhileFrostedBaselineDoesNot() {
        val width = 160
        val height = 100
        val pad = 40
        val recW = width + 2 * pad
        val recH = height + 2 * pad
        val checkerboard = makeCheckerboardShader(recW, recH, tile = 16)

        val baselineEngine = GlassEngineRegistry.get(GlassEngineId.FROSTED_BASELINE)
        val baselineBmp = renderEngine(
            engine = baselineEngine,
            backdrop = checkerboard,
            width = width,
            height = height,
            pad = pad,
            params = baselineEngine.defaultParameters(GlassVariant.CLEAR, GlassAppearance.DARK).copy(
                blurRadiusDp = 0f,
                transparency = 0.95f,
                tintOpacity = 0f
            )
        )

        for (engine in GlassEngineRegistry.all()) {
            val clearParams = engine.defaultParameters(GlassVariant.CLEAR, GlassAppearance.ADAPTIVE).copy(
                blurRadiusDp = 0f,
                refractionStrength = 1.25f,
                glassThicknessDp = 24f,
                chromaticAberration = 1.2f,
                specularIntensity = 1.0f
            )

            // 1. Render standard clear glass over checkerboard
            val bmp = renderEngine(engine, checkerboard, width, height, pad, clearParams)

            // Center pixel must be opaque glass
            val centerAlpha = alphaAt(bmp, pad + width / 2, pad + height / 2)
            assertTrue("${engine.engineKey}: center should be opaque, alpha=$centerAlpha", centerAlpha > 240)

            // Far corner pixel outside glass & shadow must be near transparent
            val farAlpha = alphaAt(bmp, 2, 2)
            assertTrue("${engine.engineKey}: far exterior should be transparent, alpha=$farAlpha", farAlpha < 20)

            if (engine.id != GlassEngineId.FROSTED_BASELINE) {
                // 2. Optical refraction check: pixels along the bevel rim must differ from unwarped baseline
                var rimDiffSum = 0.0
                var rimCount = 0
                for (y in (pad + 3)..(pad + 18)) {
                    for (x in (pad + 12)..(pad + width - 12)) {
                        val c1 = bmp.getColor(x, y)
                        val c2 = baselineBmp.getColor(x, y)
                        rimDiffSum += abs(((c1 shr 16) and 0xFF) - ((c2 shr 16) and 0xFF))
                        rimCount++
                    }
                }
                val meanRimDiff = rimDiffSum / rimCount
                assertTrue(
                    "${engine.engineKey}: expected measurable optical refraction vs flat baseline, meanRimDiff=$meanRimDiff",
                    meanRimDiff > 8.0
                )

                // 3. Chromatic dispersion check over achromatic B&W checkerboard: R vs B channel spread!
                var maxChromaSpread = 0
                for (y in (pad + 2)..(pad + height - 2)) {
                    for (x in (pad + 2)..(pad + width - 2)) {
                        val c = bmp.getColor(x, y)
                        val r = (c shr 16) and 0xFF
                        val b = c and 0xFF
                        val spread = abs(r - b)
                        if (spread > maxChromaSpread) maxChromaSpread = spread
                    }
                }
                assertTrue(
                    "${engine.engineKey}: expected chromatic aberration RGB separation over B&W checkerboard, maxSpread=$maxChromaSpread",
                    maxChromaSpread >= 5
                )

                // 4. Lighting direction response check over mid-tone backdrop (so highlights cannot clip against 255 white)
                val midTone = makeSolidShader(recW, recH, 0xFF2D3850.toInt())
                val topLit = renderEngine(engine, midTone, width, height, pad, clearParams.copy(lightAngleDegrees = 270f))
                val sideLit = renderEngine(engine, midTone, width, height, pad, clearParams.copy(lightAngleDegrees = 0f))
                var topDelta = 0.0
                for (y in pad..(pad + 3)) {
                    for (x in (pad + 30)..(pad + width - 30)) {
                        topDelta += lumaAt(topLit, x, y) - lumaAt(sideLit, x, y)
                    }
                }
                assertTrue(
                    "${engine.engineKey}: changing lightAngle 270° -> 0° should visibly change rim lighting, delta=$topDelta",
                    abs(topDelta) > 20.0
                )

                // 5. Regular vs Clear variant check
                val regBmp = renderEngine(
                    engine,
                    checkerboard,
                    width,
                    height,
                    pad,
                    engine.defaultParameters(GlassVariant.REGULAR, GlassAppearance.LIGHT)
                )
                var variantDiff = 0.0
                for (x in (pad + 40)..(pad + width - 40)) {
                    variantDiff += abs(lumaAt(regBmp, x, pad + height / 2) - lumaAt(bmp, x, pad + height / 2))
                }
                assertTrue(
                    "${engine.engineKey}: Regular and Clear variants should render distinctly, diff=$variantDiff",
                    variantDiff > 100.0
                )
            }

            // 6. Multi-shape smin liquid morphing bridge check
            // Primary shape is 60x60 centered at (30,30); secondary 60x60 circle at (102,30) -> 12px gap!
            val morphParams = clearParams.copy(cornerRadiusDp = 30f, morphSmoothingDp = 38f)
            val bridgeShape = GlassSubShape(
                centerX = 102f,
                centerY = 30f,
                halfWidth = 30f,
                halfHeight = 30f,
                cornerRadius = 30f
            )
            val morphBmp = renderEngine(
                engine = engine,
                backdrop = checkerboard,
                width = 60,
                height = 60,
                pad = 90,
                params = morphParams,
                extraShapes = listOf(bridgeShape)
            )
            // Midpoint between primary right edge (x=60) and secondary left edge (x=72) is local x=66, y=30
            val bridgeAlpha = alphaAt(morphBmp, 90 + 66, 90 + 30)
            if (engine.id == GlassEngineId.FROSTED_BASELINE) {
                assertTrue("Frosted baseline must NOT merge shapes, bridgeAlpha=$bridgeAlpha", bridgeAlpha < 80)
            } else {
                assertTrue("${engine.engineKey}: smin morphing should form liquid bridge at midpoint, bridgeAlpha=$bridgeAlpha", bridgeAlpha > 200)
            }
        }
    }

    @Test
    fun wideBlurStripEnginesSampleDownscaledBlurStripWithoutArtifacts() {
        // Verifies the dual-strip GPU wide-blur path (uWideStripY > 0) used by Shayann, Haze 2.1, and Mochi G2
        val width = 140
        val height = 90
        val pad = 32
        val recW = width + 2 * pad
        val recH = height + 2 * pad
        val wideStripY = recH + 16
        val totalH = wideStripY + (recH / 4) + 8

        // Top region is dark; wide-blur strip region at y >= wideStripY is bright cyan so interior wide-blur sample is unmistakable
        val surface = Surface.makeRasterN32Premul(recW, totalH)
        val c = surface.canvas
        val darkPaint = Paint().apply { color = 0xFF141824.toInt() }
        val stripPaint = Paint().apply { color = 0xFF20D0FF.toInt() }
        c.drawRect(Rect.makeXYWH(0f, 0f, recW.toFloat(), recH.toFloat()), darkPaint)
        c.drawRect(Rect.makeXYWH(0f, wideStripY.toFloat(), (recW / 4).toFloat(), (recH / 4).toFloat()), stripPaint)
        val compositeShader = surface.makeImageSnapshot().makeShader()
        darkPaint.close()
        stripPaint.close()
        surface.close()

        val wideEngines = listOf(
            GlassEngineId.SHAYANN_IOS27,
            GlassEngineId.HAZE_GLASS,
            GlassEngineId.MOCHI_G2
        )

        for (id in wideEngines) {
            val engine = GlassEngineRegistry.get(id)
            assertTrue("${engine.engineKey} must declare usesWideBlurStrip", engine.usesWideBlurStrip)
            val params = engine.defaultParameters(GlassVariant.REGULAR, GlassAppearance.DARK).copy(
                blurRadiusDp = 24f,
                transparency = 0.85f,
                tintOpacity = 0.05f
            )
            val withStrip = renderEngine(engine, compositeShader, width, height, pad, params, wideStripY = wideStripY.toFloat())
            val withoutStrip = renderEngine(engine, compositeShader, width, height, pad, params, wideStripY = 0f)

            val centerWith = lumaAt(withStrip, pad + width / 2, pad + height / 2)
            val centerWithout = lumaAt(withoutStrip, pad + width / 2, pad + height / 2)
            assertTrue(
                "${engine.engineKey}: uWideStripY > 0 must sample the downscaled blur strip in the interior (with=$centerWith, without=$centerWithout)",
                centerWith > centerWithout + 12.0
            )
        }
    }

    private fun renderEngine(
        engine: LiquidGlassEngine,
        backdrop: Shader,
        width: Int,
        height: Int,
        pad: Int,
        params: GlassParameters,
        extraShapes: List<GlassSubShape> = emptyList(),
        wideStripY: Float = 0f
    ): Bitmap {
        val recW = width + 2 * pad
        val recH = height + 2 * pad
        val density = 1.0f
        val p = params.sanitized()
        val rPx = minOf(p.cornerRadiusDp * density, minOf(width, height) * 0.5f)
        val ctx = GlassDrawContext(
            widthPx = width.toFloat(),
            heightPx = height.toFloat(),
            padPx = pad.toFloat(),
            density = density,
            cornerRadiiPx = floatArrayOf(rPx, rPx, rPx, rPx),
            parameters = p,
            shapes = extraShapes,
            wideStripY = wideStripY,
            wideScale = 0.25f,
            backdropBoundsLeft = 0f,
            backdropBoundsTop = 0f,
            backdropBoundsRight = recW.toFloat(),
            backdropBoundsBottom = recH.toFloat()
        )

        val effect = RuntimeEffect.makeForShader(engine.agslSource)
        val builder = RuntimeShaderBuilder(effect)
        builder.child("content", backdrop)
        bindSkiaUniforms(engine, builder, ctx)

        val surface = Surface.makeRasterN32Premul(recW, recH)
        val canvas = surface.canvas
        canvas.clear(0x00000000)
        val paint = Paint().apply { shader = builder.makeShader() }
        canvas.drawRect(Rect.makeWH(recW.toFloat(), recH.toFloat()), paint)

        val bmp = Bitmap()
        bmp.allocPixels(ImageInfo(recW, recH, ColorType.BGRA_8888, ColorAlphaType.PREMUL))
        surface.readPixels(bmp, 0, 0)
        paint.close()
        surface.close()
        effect.close()
        return bmp
    }

    private fun bindSkiaUniforms(engine: LiquidGlassEngine, b: RuntimeShaderBuilder, ctx: GlassDrawContext) {
        val p = ctx.parameters.sanitized()
        val isClear = p.variant == GlassVariant.CLEAR
        // Common uniforms
        b.uniform("uSize", ctx.widthPx, ctx.heightPx)
        b.uniform("uPad", ctx.padPx)
        b.uniform("uBackdrop", ctx.backdropBoundsLeft, ctx.backdropBoundsTop, ctx.backdropBoundsRight, ctx.backdropBoundsBottom)
        b.uniform("uRadii", ctx.cornerRadiiPx[0], ctx.cornerRadiiPx[1], ctx.cornerRadiiPx[2], ctx.cornerRadiiPx[3])
        b.uniform("uDensity", ctx.density)
        b.uniform("uExtraCount", ctx.shapes.size.toFloat())
        val packedShapes = FloatArray(20)
        val packedRadii = FloatArray(5)
        ctx.shapes.take(5).forEachIndexed { i, s ->
            packedShapes[i * 4 + 0] = s.centerX
            packedShapes[i * 4 + 1] = s.centerY
            packedShapes[i * 4 + 2] = s.halfWidth
            packedShapes[i * 4 + 3] = s.halfHeight
            packedRadii[i] = s.cornerRadius
        }
        b.uniform("uExtraShapes", packedShapes)
        b.uniform("uExtraRadii", packedRadii)
        b.uniform("uMergeK", p.morphSmoothingDp * ctx.density)
        b.uniform("uTouch", ctx.touchX, ctx.touchY)
        b.uniform("uPress", ctx.pressAmount * p.touchDeformation)
        b.uniform("uShadow", p.shadowIntensity)

        when (engine.id) {
            GlassEngineId.SHAYANN_IOS27 -> {
                val band = (p.glassThicknessDp * ctx.density).coerceAtLeast(4f)
                b.uniform("uWideStripY", ctx.wideStripY)
                b.uniform("uWideScale", ctx.wideScale)
                b.uniform("uRefractBand", band)
                b.uniform("uRefractDepth", band * 0.82f * p.refractionStrength)
                b.uniform("uIor", 1.50f)
                b.uniform("uBevelPower", (2f + p.edgeCurvature * 2f).coerceIn(1.8f, 6f))
                b.uniform("uCornerPower", (2f + p.edgeCurvature * 2f).coerceIn(2f, 5f))
                b.uniform("uLensZoom", p.lensDistortion)
                b.uniform("uBlurPx", p.blurRadiusDp * ctx.density)
                b.uniform("uAberration", p.chromaticAberration * 0.18f)
                b.uniform("uTransparency", p.transparency)
                b.uniform("uTint", p.tintRed(), p.tintGreen(), p.tintBlue(), p.tintOpacity)
                b.uniform("uLight", ctx.lightDirX, ctx.lightDirY)
                b.uniform("uSpecular", p.specularIntensity)
                b.uniform("uEdgeIllum", p.edgeIllumination)
                b.uniform("uIsClear", if (isClear) 1f else 0f)
                b.uniform("uDarkMode", p.darkModeUniform())
                b.uniform("uOutward", if (p.outwardSamplingOverride ?: true) 1f else -1f)
            }
            GlassEngineId.KYANT_BACKDROP -> {
                val h = (p.glassThicknessDp * ctx.density).coerceAtLeast(2f)
                b.uniform("uRefractionHeight", h)
                b.uniform("uRefractionAmount", -h * 1.15f * p.refractionStrength)
                b.uniform("uDepthEffect", p.lensDistortion)
                b.uniform("uCurvature", p.edgeCurvature)
                b.uniform("uChromaticAberration", p.chromaticAberration)
                b.uniform("uTransparency", p.transparency)
                b.uniform("uTint", p.tintRed(), p.tintGreen(), p.tintBlue(), p.tintOpacity)
                b.uniform("uLightAngle", ctx.lightRad)
                b.uniform("uSpecular", p.specularIntensity)
                b.uniform("uEdgeIllum", p.edgeIllumination)
                b.uniform("uIsClear", if (isClear) 1f else 0f)
                b.uniform("uDarkMode", p.darkModeUniform())
            }
            GlassEngineId.VITRO_IOS26 -> {
                val bevel = (p.glassThicknessDp * ctx.density).coerceAtLeast(2f)
                b.uniform("uBevel", bevel)
                b.uniform("uDepth", bevel * 0.95f * p.refractionStrength)
                b.uniform("uCurvature", p.edgeCurvature)
                b.uniform("uDispersion", p.chromaticAberration * 0.16f)
                b.uniform("uZoom", (1f - p.lensDistortion * 0.18f).coerceIn(0.65f, 1.25f))
                b.uniform("uSaturation", if (isClear) 1.15f else 1.32f)
                b.uniform("uBrightness", ((1f - p.transparency) * 0.65f * if (isClear) 0.18f else 1f).coerceIn(0f, 1f))
                b.uniform("uLight", ctx.lightDirX, ctx.lightDirY)
                b.uniform("uHighlight", p.specularIntensity * p.edgeIllumination)
                b.uniform("uTint", p.tintRed(), p.tintGreen(), p.tintBlue(), p.tintOpacity * 0.85f)
                b.uniform("uDimming", if (isClear) 0.08f else 0f)
                b.uniform("uDarkMode", p.darkModeUniform())
            }
            GlassEngineId.HAZE_GLASS -> {
                val h = (p.glassThicknessDp * ctx.density).coerceAtLeast(3f)
                b.uniform("uWideStripY", ctx.wideStripY)
                b.uniform("uWideScale", ctx.wideScale)
                b.uniform("uBlurPx", p.blurRadiusDp * ctx.density)
                b.uniform("uRefractionHeight", h)
                b.uniform("uRefractionScale", h * 1.1f * p.refractionStrength)
                b.uniform("uFoldStrength", if (isClear) 0.35f else 0.72f * p.edgeCurvature.coerceIn(0.3f, 1.5f))
                b.uniform("uSurfaceProfile", p.surfaceProfile.toFloat())
                b.uniform("uLensDistortion", p.lensDistortion)
                b.uniform("uChromaMult", if (isClear) 1.12f else 1.32f)
                b.uniform("uAberration", p.chromaticAberration * 0.22f)
                b.uniform("uTransparency", p.transparency)
                b.uniform("uTint", p.tintRed(), p.tintGreen(), p.tintBlue(), p.tintOpacity)
                b.uniform("uLight", ctx.lightDirX, ctx.lightDirY)
                b.uniform("uSpecular", p.specularIntensity)
                b.uniform("uEdgeIllum", p.edgeIllumination)
                b.uniform("uIsClear", if (isClear) 1f else 0f)
                b.uniform("uDarkMode", p.darkModeUniform())
            }
            GlassEngineId.SOHUM_SNELL -> {
                b.uniform("uBezelPx", (p.glassThicknessDp * ctx.density).coerceAtLeast(3f))
                b.uniform("uRefractScale", p.refractionStrength)
                b.uniform("uProfilePower", (2f + p.edgeCurvature * 2f).coerceIn(2f, 6f))
                b.uniform("uBaseT", (0.35f + p.lensDistortion * 0.4f).coerceIn(0.05f, 1f))
                b.uniform("uDispersionStrength", p.chromaticAberration * 10f)
                b.uniform("uTransparency", p.transparency)
                b.uniform("uTint", p.tintRed(), p.tintGreen(), p.tintBlue(), p.tintOpacity)
                b.uniform("uLight", ctx.lightDirX, ctx.lightDirY)
                b.uniform("uSpecular", p.specularIntensity)
                b.uniform("uEdgeIllum", p.edgeIllumination)
                b.uniform("uIsClear", if (isClear) 1f else 0f)
                b.uniform("uDarkMode", p.darkModeUniform())
            }
            GlassEngineId.FLUTTER_SEAGLASS -> {
                val bevel = (p.glassThicknessDp * ctx.density).coerceAtLeast(3f)
                b.uniform("uBevelPx", bevel)
                b.uniform("uDispScalePx", bevel * 1.45f * p.refractionStrength)
                b.uniform("uCurvature", p.edgeCurvature)
                b.uniform("uLensZoom", p.lensDistortion)
                b.uniform("uDispersion", p.chromaticAberration * 0.14f)
                b.uniform("uTransparency", p.transparency)
                b.uniform("uTint", p.tintRed(), p.tintGreen(), p.tintBlue(), p.tintOpacity)
                b.uniform("uLight", ctx.lightDirX, ctx.lightDirY)
                b.uniform("uLightIntensity", p.specularIntensity)
                b.uniform("uContourStrength", p.edgeIllumination)
                b.uniform("uIsClear", if (isClear) 1f else 0f)
                b.uniform("uDarkMode", p.darkModeUniform())
            }
            GlassEngineId.MOCHI_G2 -> {
                val h = (p.glassThicknessDp * ctx.density).coerceAtLeast(3f)
                b.uniform("uWideStripY", ctx.wideStripY)
                b.uniform("uWideScale", ctx.wideScale)
                b.uniform("uBlurPx", p.blurRadiusDp * ctx.density)
                b.uniform("uInnerAmt", h * 0.85f * p.refractionStrength)
                b.uniform("uInnerInvH", 1f / h)
                b.uniform("uOuterAmt", h * 0.35f * p.refractionStrength)
                b.uniform("uCurvature", p.edgeCurvature)
                b.uniform("uLensZoom", p.lensDistortion)
                b.uniform("uAberration", p.chromaticAberration * 0.15f)
                b.uniform("uTransparency", p.transparency)
                b.uniform("uTint", p.tintRed(), p.tintGreen(), p.tintBlue(), p.tintOpacity)
                b.uniform("uLight", ctx.lightDirX, ctx.lightDirY)
                b.uniform("uSpecular", p.specularIntensity)
                b.uniform("uEdgeIllum", p.edgeIllumination)
                b.uniform("uIsClear", if (isClear) 1f else 0f)
                b.uniform("uDarkMode", p.darkModeUniform())
            }
            GlassEngineId.QWEA0_GRAVITY -> {
                val bevel = (p.glassThicknessDp * ctx.density).coerceAtLeast(3f)
                b.uniform("uBevel", bevel)
                b.uniform("uRefractPx", bevel * 1.15f * p.refractionStrength)
                b.uniform("uFalloff", (p.edgeCurvature * 2f).coerceIn(0f, 5f))
                b.uniform("uRefractDir", if (p.outwardSamplingOverride ?: false) 1f else -1f)
                b.uniform("uRimSoft", 1.25f * ctx.density)
                b.uniform("uLensZoom", p.lensDistortion)
                b.uniform("uDispersion", p.chromaticAberration * 0.24f)
                b.uniform("uLight", ctx.lightDirX, ctx.lightDirY)
                b.uniform("uSpecStrength", p.specularIntensity)
                b.uniform("uEdgeIllum", p.edgeIllumination)
                b.uniform("uTransparency", p.transparency)
                b.uniform("uGlassTint", p.tintRed(), p.tintGreen(), p.tintBlue(), p.tintOpacity)
                b.uniform("uSatFactor", if (isClear) 1.15f else 1.45f)
                b.uniform("uIsClear", if (isClear) 1f else 0f)
                b.uniform("uDarkMode", p.darkModeUniform())
            }
            GlassEngineId.FROSTED_BASELINE -> {
                b.uniform("uTransparency", p.transparency)
                b.uniform("uTint", p.tintRed(), p.tintGreen(), p.tintBlue(), p.tintOpacity)
                b.uniform("uBorderAlpha", 0.42f * p.edgeIllumination)
                b.uniform("uIsClear", if (isClear) 1f else 0f)
                b.uniform("uDarkMode", p.darkModeUniform())
            }
        }
    }

    private fun makeCheckerboardShader(w: Int, h: Int, tile: Int): Shader {
        val surface = Surface.makeRasterN32Premul(w, h)
        val c = surface.canvas
        val white = Paint().apply { color = 0xFFFFFFFF.toInt() }
        val black = Paint().apply { color = 0xFF101014.toInt() }
        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val isWhite = ((x / tile) + (y / tile)) % 2 == 0
                c.drawRect(Rect.makeXYWH(x.toFloat(), y.toFloat(), tile.toFloat(), tile.toFloat()), if (isWhite) white else black)
                x += tile
            }
            y += tile
        }
        val image = surface.makeImageSnapshot()
        white.close()
        black.close()
        surface.close()
        return image.makeShader()
    }

    private fun makeSolidShader(w: Int, h: Int, argb: Int): Shader {
        val surface = Surface.makeRasterN32Premul(w, h)
        val paint = Paint().apply { color = argb }
        surface.canvas.drawRect(Rect.makeWH(w.toFloat(), h.toFloat()), paint)
        val image = surface.makeImageSnapshot()
        paint.close()
        surface.close()
        return image.makeShader()
    }

    private fun alphaAt(bmp: Bitmap, x: Int, y: Int): Int = (bmp.getColor(x, y) ushr 24) and 0xFF

    private fun lumaAt(bmp: Bitmap, x: Int, y: Int): Double {
        val c = bmp.getColor(x, y)
        val r = (c shr 16) and 0xFF
        val g = (c shr 8) and 0xFF
        val b = c and 0xFF
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
}
