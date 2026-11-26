package com.liquidglass.sdk.core

import com.liquidglass.sdk.shaders.FlutterSeaGlassEngine
import com.liquidglass.sdk.shaders.FrostedBaselineEngine
import com.liquidglass.sdk.shaders.HazeGlassEngine
import com.liquidglass.sdk.shaders.KyantBackdropEngine
import com.liquidglass.sdk.shaders.MochiG2Engine
import com.liquidglass.sdk.shaders.Qwea0GravityLensEngine
import com.liquidglass.sdk.shaders.ShayannIos27Engine
import com.liquidglass.sdk.shaders.SohumSnellEngine
import com.liquidglass.sdk.shaders.VitroIos26Engine

/**
 * Central pluggable registry of Liquid Glass rendering engines and built-in material presets.
 *
 * New rendering engines can be registered at runtime via [register] without rebuilding the UI.
 */
object GlassEngineRegistry {
    private val enginesMap: LinkedHashMap<String, LiquidGlassEngine> = linkedMapOf()

    init {
        register(ShayannIos27Engine())
        register(KyantBackdropEngine())
        register(VitroIos26Engine())
        register(HazeGlassEngine())
        register(SohumSnellEngine())
        register(FlutterSeaGlassEngine())
        register(MochiG2Engine())
        register(Qwea0GravityLensEngine())
        register(FrostedBaselineEngine())
    }

    /** Registers or replaces a [LiquidGlassEngine] implementation by its [LiquidGlassEngine.engineKey]. */
    fun register(engine: LiquidGlassEngine) {
        enginesMap[engine.engineKey] = engine
    }

    /** Returns all registered engines in canonical evaluation order. */
    fun all(): List<LiquidGlassEngine> = enginesMap.values.toList()

    /** Resolves an engine by [GlassEngineId]. */
    fun get(id: GlassEngineId): LiquidGlassEngine =
        enginesMap[id.key] ?: error("Engine ${id.key} is not registered")

    /** Resolves an engine by string key, falling back to [GlassEngineId.SHAYANN_IOS27]. */
    fun getByKey(key: String): LiquidGlassEngine =
        enginesMap[key] ?: get(GlassEngineId.SHAYANN_IOS27)

    /** Built-in calibrated presets for instant evaluation and JSON export. */
    fun builtInPresets(): List<GlassPreset> = listOf(
        GlassPreset(
            name = "iOS 27 Measured — Regular",
            description = "shayann07 lossless iPhone iOS 27 dual-kernel calibration (default Regular material).",
            engineKey = GlassEngineId.SHAYANN_IOS27.key,
            parameters = get(GlassEngineId.SHAYANN_IOS27).defaultParameters(GlassVariant.REGULAR, GlassAppearance.ADAPTIVE),
            builtIn = true
        ),
        GlassPreset(
            name = "iOS 27 Measured — Clear",
            description = "shayann07 high-clarity refractive glass for photos & rich media.",
            engineKey = GlassEngineId.SHAYANN_IOS27.key,
            parameters = get(GlassEngineId.SHAYANN_IOS27).defaultParameters(GlassVariant.CLEAR, GlassAppearance.ADAPTIVE),
            builtIn = true
        ),
        GlassPreset(
            name = "Kyant0 — 7-Band Spectral",
            description = "Kyant0 AndroidLiquidGlass original circleMap + 7-band spectral dispersion.",
            engineKey = GlassEngineId.KYANT_BACKDROP.key,
            parameters = get(GlassEngineId.KYANT_BACKDROP).defaultParameters(GlassVariant.REGULAR, GlassAppearance.ADAPTIVE),
            builtIn = true
        ),
        GlassPreset(
            name = "Vitro — iPhone iOS 26",
            description = "itshedimisawi Vitro side-by-side iPhone iOS 26 calibrated curves & vibrant rim.",
            engineKey = GlassEngineId.VITRO_IOS26.key,
            parameters = get(GlassEngineId.VITRO_IOS26).defaultParameters(GlassVariant.REGULAR, GlassAppearance.ADAPTIVE),
            builtIn = true
        ),
        GlassPreset(
            name = "Haze 2.1 — Spiralens (xenitV1)",
            description = "Chris Banes Haze Glass with xenitV1 Spiralens squircle Android preset.",
            engineKey = GlassEngineId.HAZE_GLASS.key,
            parameters = get(GlassEngineId.HAZE_GLASS).defaultParameters(GlassVariant.REGULAR, GlassAppearance.ADAPTIVE),
            builtIn = true
        ),
        GlassPreset(
            name = "Sohum — macOS 26 Control Center",
            description = "sohumsuthar BK7 Snell slab + measured 0.58L+34 luminance transfer & 1.72x chroma.",
            engineKey = GlassEngineId.SOHUM_SNELL.key,
            parameters = get(GlassEngineId.SOHUM_SNELL).defaultParameters(GlassVariant.REGULAR, GlassAppearance.ADAPTIVE),
            builtIn = true
        ),
        GlassPreset(
            name = "SeaGlass — iOS 27 Glint",
            description = "whynotmake-it iPhone 17 Pro iOS 27 HDR glint recoloring & tangential dark contour.",
            engineKey = GlassEngineId.FLUTTER_SEAGLASS.key,
            parameters = get(GlassEngineId.FLUTTER_SEAGLASS).defaultParameters(GlassVariant.REGULAR, GlassAppearance.ADAPTIVE),
            builtIn = true
        ),
        GlassPreset(
            name = "Mochi — G2 Supercircle",
            description = "feice11 Apple G2 continuous supercircle + dual bevel + erfp ring shadow.",
            engineKey = GlassEngineId.MOCHI_G2.key,
            parameters = get(GlassEngineId.MOCHI_G2).defaultParameters(GlassVariant.REGULAR, GlassAppearance.ADAPTIVE),
            builtIn = true
        ),
        GlassPreset(
            name = "QWEA0 — Gravity Lens",
            description = "QWEA0 inverse-power gravitational bevel + 3-tap rimSoft + absorbing tinted glass.",
            engineKey = GlassEngineId.QWEA0_GRAVITY.key,
            parameters = get(GlassEngineId.QWEA0_GRAVITY).defaultParameters(GlassVariant.REGULAR, GlassAppearance.ADAPTIVE),
            builtIn = true
        ),
        GlassPreset(
            name = "Frosted Glassmorphism (Baseline)",
            description = "Flat blurred rectangle with uniform 1dp stroke — non-refractive comparison baseline.",
            engineKey = GlassEngineId.FROSTED_BASELINE.key,
            parameters = get(GlassEngineId.FROSTED_BASELINE).defaultParameters(GlassVariant.REGULAR, GlassAppearance.ADAPTIVE),
            builtIn = true
        )
    )
}
