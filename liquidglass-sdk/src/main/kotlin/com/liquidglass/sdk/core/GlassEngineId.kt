package com.liquidglass.sdk.core

/**
 * Built-in rendering engines integrated into the Liquid Glass SDK & Laboratory.
 *
 * The architecture also supports custom third-party engines via [LiquidGlassEngine.engineKey],
 * so new implementations can be plugged into [GlassEngineRegistry] without modifying UI code.
 */
enum class GlassEngineId(val key: String) {
    SHAYANN_IOS27("shayann_ios27"),
    KYANT_BACKDROP("kyant_backdrop"),
    VITRO_IOS26("vitro_ios26"),
    HAZE_GLASS("haze_glass"),
    SOHUM_SNELL("sohum_snell"),
    FLUTTER_SEAGLASS("flutter_seaglass"),
    MOCHI_G2("mochi_g2"),
    QWEA0_GRAVITY("qwea0_gravity"),
    FROSTED_BASELINE("frosted_baseline");

    companion object {
        fun fromKey(key: String): GlassEngineId? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Apple Liquid Glass material variant (WWDC 2025 / iOS 26 & iOS 27 HIG).
 * - [REGULAR]: Default functional-layer glass with adaptive luminosity & balanced legibility.
 * - [CLEAR]: Highly translucent optical glass for rich photo/media backdrops (optional dimming veil).
 */
enum class GlassVariant(val label: String) {
    REGULAR("Regular"),
    CLEAR("Clear")
}

/**
 * Theme/luminosity appearance mode.
 * - [LIGHT]: Light-mode glass material parameters.
 * - [DARK]: Dark-mode glass material parameters.
 * - [ADAPTIVE]: Continuously adapts tint and contrast to backdrop luminance.
 */
enum class GlassAppearance(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    ADAPTIVE("Adaptive")
}
