package com.liquidglass.lab.model

import androidx.compose.ui.graphics.Color

/** All 8 required laboratory backgrounds for optical stress-testing. */
enum class BackgroundMode(val label: String, val isDarkDefault: Boolean) {
    PHOTO("Photos", true),
    GRADIENT("Gradient", true),
    CHECKERBOARD("Checker", false),
    FINE_GRID("Fine Grid", true),
    TEXT_WALL("Text", false),
    SOLID_COLOR("Solid", true),
    ANIMATED("Animated", true),
    SCROLLING_FEED("Scrolling", true)
}

enum class LabViewMode(val label: String) {
    FULL_SHOWCASE("Showcase"),
    DRAG_PLAYGROUND("Drag Lens"),
    MORPH_BENCH("Morphing"),
    SIDE_BY_SIDE("Compare A/B")
}

data class SolidSwatch(val name: String, val color: Color, val isDark: Boolean)

val LabSolidSwatches = listOf(
    SolidSwatch("Pure Black", Color(0xFF000000), true),
    SolidSwatch("Pure White", Color(0xFFFFFFFF), false),
    SolidSwatch("50% Gray", Color(0xFF808080), true),
    SolidSwatch("Apple Blue", Color(0xFF0A84FF), true),
    SolidSwatch("Coral Red", Color(0xFFFF3B30), true),
    SolidSwatch("Emerald", Color(0xFF34C759), true),
    SolidSwatch("Warm Amber", Color(0xFFFF9F0A), false)
)

val LabPhotoTitles = listOf(
    "Big Sur Sunset",
    "Tokyo Neon Rain",
    "Botanical Macro"
)

val LabCheckerSizesDp = listOf(16f, 32f, 56f)
