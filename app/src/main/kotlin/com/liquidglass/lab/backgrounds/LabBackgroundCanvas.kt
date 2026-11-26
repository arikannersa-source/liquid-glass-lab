package com.liquidglass.lab.backgrounds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.lab.R
import com.liquidglass.lab.model.BackgroundMode
import com.liquidglass.lab.model.LabCheckerSizesDp
import com.liquidglass.lab.model.LabSolidSwatches
import com.liquidglass.sdk.core.GlassBackdropState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders any of the 8 laboratory calibration backgrounds inside the live [GlassBackdropState]
 * capture subtree. Notifies [backdrop] on animation frames and scroll offsets so floating
 * glass elements refract moving content in exact 120Hz lockstep.
 */
@Composable
fun LabBackgroundCanvas(
    mode: BackgroundMode,
    photoIndex: Int,
    solidColorIndex: Int,
    checkerScaleIndex: Int,
    textWallDark: Boolean,
    animationSpeed: Float,
    backdrop: GlassBackdropState,
    modifier: Modifier = Modifier
) {
    // Notify backdrop whenever static background mode or sub-selection changes
    LaunchedEffect(mode, photoIndex, solidColorIndex, checkerScaleIndex, textWallDark) {
        backdrop.notifyContentChanged()
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (mode) {
            BackgroundMode.PHOTO -> PhotoBackground(photoIndex)
            BackgroundMode.GRADIENT -> MulticolorGradientBackground()
            BackgroundMode.CHECKERBOARD -> CheckerboardBackground(LabCheckerSizesDp[checkerScaleIndex % LabCheckerSizesDp.size])
            BackgroundMode.FINE_GRID -> FineCalibrationGridBackground()
            BackgroundMode.TEXT_WALL -> EditorialTextBackground(dark = textWallDark)
            BackgroundMode.SOLID_COLOR -> {
                val swatch = LabSolidSwatches[solidColorIndex % LabSolidSwatches.size]
                Box(modifier = Modifier.fillMaxSize().background(swatch.color))
            }
            BackgroundMode.ANIMATED -> AnimatedOpticalBackground(animationSpeed, backdrop)
            BackgroundMode.SCROLLING_FEED -> ScrollingMediaFeedBackground(backdrop)
        }
    }
}

@Composable
private fun PhotoBackground(photoIndex: Int) {
    val resId = when (photoIndex % 3) {
        0 -> R.drawable.bg_photo_coastal_sunset
        1 -> R.drawable.bg_photo_neon_architecture
        else -> R.drawable.bg_photo_botanical_macro
    }
    Image(
        painter = painterResource(id = resId),
        contentDescription = "High-resolution test photograph",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    )
}

@Composable
private fun MulticolorGradientBackground() {
    Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0D18))) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF0F172A),
                    Color(0xFF1D4ED8),
                    Color(0xFF9333EA),
                    Color(0xFFEC4899),
                    Color(0xFFF59E0B),
                    Color(0xFF10B981)
                ),
                start = Offset.Zero,
                end = Offset(w, h)
            )
        )
        // High-saturation chromatic orbs to stress-test chromatic dispersion and vibrancy
        val orbs = listOf(
            Triple(Offset(w * 0.22f, h * 0.20f), w * 0.42f, Color(0xFF00F0FF)),
            Triple(Offset(w * 0.80f, h * 0.34f), w * 0.46f, Color(0xFFFF2D55)),
            Triple(Offset(w * 0.30f, h * 0.66f), w * 0.48f, Color(0xFFFFD60A)),
            Triple(Offset(w * 0.78f, h * 0.82f), w * 0.44f, Color(0xFF30D158))
        )
        for ((center, radius, color) in orbs) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.85f), color.copy(alpha = 0f)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
        // Crisp diagonal chromatic calibration ribbon across center
        for (i in 0 until 7) {
            val stripeColors = listOf(
                Color(0xFFFF3B30), Color(0xFFFF9500), Color(0xFFFFCC00),
                Color(0xFF34C759), Color(0xFF00C7BE), Color(0xFF0A84FF), Color(0xFFBF5AF2)
            )
            val y = h * 0.44f + i * 16.dp.toPx()
            drawRect(
                color = stripeColors[i],
                topLeft = Offset(0f, y),
                size = Size(w, 12.dp.toPx())
            )
        }
    }
}

@Composable
private fun CheckerboardBackground(tileDp: Float) {
    Canvas(modifier = Modifier.fillMaxSize().background(Color.White)) {
        val tile = tileDp.dp.toPx().coerceAtLeast(8f)
        val cols = (size.width / tile).toInt() + 2
        val rows = (size.height / tile).toInt() + 2
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val isDark = (r + c) % 2 == 0
                drawRect(
                    color = if (isDark) Color(0xFF101216) else Color(0xFFFAFAFC),
                    topLeft = Offset(c * tile, r * tile),
                    size = Size(tile, tile)
                )
            }
        }
    }
}

@Composable
private fun FineCalibrationGridBackground() {
    Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFF0C121E))) {
        val minor = 12.dp.toPx()
        val major = minor * 4f
        val minorColor = Color(0xFF233452)
        val majorColor = Color(0xFF38BDF8).copy(alpha = 0.65f)
        val accentColor = Color(0xFFF43F5E).copy(alpha = 0.85f)

        var x = 0f
        var idx = 0
        while (x <= size.width) {
            val isMajor = idx % 4 == 0
            drawLine(
                color = if (isMajor) majorColor else minorColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = if (isMajor) 1.6f else 0.9f
            )
            x += minor
            idx++
        }

        var y = 0f
        idx = 0
        while (y <= size.height) {
            val isMajor = idx % 4 == 0
            drawLine(
                color = if (isMajor) majorColor else minorColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = if (isMajor) 1.6f else 0.9f
            )
            y += minor
            idx++
        }

        // Precision optical alignment rings + diagonal crosshairs
        val center = Offset(size.width * 0.5f, size.height * 0.5f)
        listOf(60f, 120f, 180f, 240f).forEach { rDp ->
            drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = 0.55f),
                radius = rDp.dp.toPx(),
                center = center,
                style = Stroke(width = 1.6f)
            )
        }
        drawLine(accentColor, Offset(0f, center.y), Offset(size.width, center.y), strokeWidth = 2.2f)
        drawLine(accentColor, Offset(center.x, 0f), Offset(center.x, size.height), strokeWidth = 2.2f)
        drawLine(Color(0xFFFACC15).copy(alpha = 0.55f), Offset.Zero, Offset(size.width, size.height), strokeWidth = 1.5f)
        drawLine(Color(0xFFFACC15).copy(alpha = 0.55f), Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = 1.5f)
    }
}

@Composable
private fun EditorialTextBackground(dark: Boolean) {
    val bg = if (dark) Color(0xFF0E1015) else Color(0xFFF6F7FB)
    val ink = if (dark) Color(0xFFF2F4FA) else Color(0xFF10131A)
    val sub = if (dark) Color(0xFF94A3B8) else Color(0xFF475569)
    val accent = Color(0xFF0A84FF)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "OPTICAL LEGIBILITY & RIM LENSING",
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.8.sp
        )
        Text(
            text = "Convex Edge Refraction curves letterforms cleanly along the glass outline.",
            color = ink,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 32.sp
        )
        repeat(8) { idx ->
            Text(
                text = "Line ${idx + 1}: Hold a glass lens over crisp serif and sans-serif text. Real Liquid Glass compresses and curves strokes inside the outer bevel band while keeping the center readable under Regular glass and razor-sharp under Clear glass. 0123456789 ABCDEFGHIJKLMNOPQRSTUVWXYZ",
                color = if (idx % 2 == 0) ink else sub,
                fontSize = if (idx % 3 == 0) 16.sp else 13.sp,
                fontFamily = if (idx % 2 == 0) FontFamily.SansSerif else FontFamily.Monospace,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun AnimatedOpticalBackground(
    animationSpeed: Float,
    backdrop: GlassBackdropState
) {
    var phase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(animationSpeed) {
        var lastNs = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNs != 0L) {
                    val dt = ((now - lastNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    phase += dt * animationSpeed
                    backdrop.notifyContentChanged()
                }
                lastNs = now
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFF090D16))) {
        val w = size.width
        val h = size.height

        // Animated deep gradient backdrop
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF0A0F1F), Color(0xFF1A103D), Color(0xFF0A2433)),
                start = Offset(w * (0.2f + 0.3f * sin(phase * 0.4f)), 0f),
                end = Offset(w * (0.8f - 0.3f * cos(phase * 0.3f)), h)
            )
        )

        // Moving high-contrast diagonal stripes to make live refraction immediately unmistakable
        val stripeSpacing = 56.dp.toPx()
        val scrollOff = (phase * 70f * density) % stripeSpacing
        var x = -h + scrollOff
        while (x < w + h) {
            drawLine(
                color = Color.White.copy(alpha = 0.14f),
                start = Offset(x, 0f),
                end = Offset(x + h * 0.5f, h),
                strokeWidth = 10.dp.toPx()
            )
            x += stripeSpacing
        }

        // Orbiting saturated spheres
        val colors = listOf(
            Color(0xFFFF375F), Color(0xFF0A84FF), Color(0xFF30D158),
            Color(0xFFFFD60A), Color(0xFFBF5AF2), Color(0xFF64D2FF)
        )
        colors.forEachIndexed { idx, color ->
            val angle = phase * (0.65f + idx * 0.12f) + idx * (PI.toFloat() / 3f)
            val cx = w * 0.5f + cos(angle) * w * 0.34f
            val cy = h * (0.18f + 0.13f * idx) + sin(angle * 1.3f) * h * 0.09f
            val r = (46f + (idx % 3) * 18f).dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color, color.copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r * 1.6f
                ),
                radius = r * 1.6f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = color,
                radius = r * 0.48f,
                center = Offset(cx, cy)
            )
        }

        // Sine wave luminous ribbon
        val path = Path()
        val steps = 40
        for (i in 0..steps) {
            val fx = i / steps.toFloat()
            val px = fx * w
            val py = h * 0.52f + sin(fx * 6.283f * 1.5f + phase * 2.2f) * 64.dp.toPx()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(
            path = path,
            color = Color(0xFF38BDF8),
            style = Stroke(width = 8.dp.toPx())
        )
    }
}

@Composable
private fun ScrollingMediaFeedBackground(backdrop: GlassBackdropState) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { backdrop.notifyContentChanged() }
    }

    val cards = remember {
        listOf(
            Triple("Big Sur Golden Hour", "Coastal refraction & warm sky luminance test", R.drawable.bg_photo_coastal_sunset),
            Triple("Shinjuku Neon Rain", "High-contrast dark architectural grid & specular bokeh", R.drawable.bg_photo_neon_architecture),
            Triple("Tropical Dew Macro", "Vivid emerald, coral orange & cobalt chromatic test", R.drawable.bg_photo_botanical_macro),
            Triple("Refractive Typography", "Scroll rapidly beneath the floating bottom bar and top search bar to inspect edge bending and adaptive contrast in motion.", R.drawable.bg_photo_coastal_sunset),
            Triple("Night Specular Optics", "Observe how each engine lifts deep shadows while holding bright neon highlights in check.", R.drawable.bg_photo_neon_architecture),
            Triple("Chromatic Dispersion", "Drag the floating glass pills across the vibrant petal edges to compare RGB prism separation.", R.drawable.bg_photo_botanical_macro)
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14)),
        contentPadding = PaddingValues(top = 110.dp, bottom = 140.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(cards) { index, (title, subtitle, imgRes) ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF171C26))
            ) {
                Image(
                    painter = painterResource(id = imgRes),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${index + 1}. $title",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            color = Color(0xFFB4BED2),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when (index % 3) {
                                    0 -> Color(0xFFFF6B35)
                                    1 -> Color(0xFF00D2FF)
                                    else -> Color(0xFF34C759)
                                }
                            )
                    )
                }
            }
        }
    }
}
