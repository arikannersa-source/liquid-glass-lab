package com.liquidglass.sdk.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.sdk.core.GlassBackdropState
import com.liquidglass.sdk.core.GlassEngineId
import com.liquidglass.sdk.core.GlassParameters
import com.liquidglass.sdk.core.GlassVariant
import com.liquidglass.sdk.modifier.GlassMotionPhysics
import com.liquidglass.sdk.modifier.liquidGlass
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Sliding elastic Liquid Glass Segmented Control.
 */
@Composable
fun GlassSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: GlassBackdropState,
    engineId: GlassEngineId,
    parameters: GlassParameters,
    modifier: Modifier = Modifier,
    backdropIsDark: Boolean = true
) {
    if (options.isEmpty()) return
    val density = LocalDensity.current
    val fg = glassForegroundColor(parameters, backdropIsDark)
    val animIdx by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = GlassMotionPhysics.elasticFloatSpring(parameters.animationSpeed, dampingRatio = 0.68f),
        label = "segThumb"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .liquidGlass(
                backdrop = backdrop,
                engineId = engineId,
                parameters = parameters,
                cornerRadiusOverrideDp = 22.dp
            )
            .padding(4.dp)
    ) {
        val totalW = with(density) { maxWidth.toPx() }
        val slotW = totalW / options.size
        val slotDp = maxWidth / options.size

        Box(
            modifier = Modifier
                .offset { IntOffset((animIdx * slotW).roundToInt(), 0) }
                .width(slotDp)
                .fillMaxHeight()
                .liquidGlass(
                    backdrop = backdrop,
                    engineId = engineId,
                    parameters = parameters.copy(
                        variant = GlassVariant.CLEAR,
                        tintColorArgb = 0xFFFFFFFFL,
                        tintOpacity = 0.24f,
                        refractionStrength = (parameters.refractionStrength * 1.15f).coerceAtMost(2f)
                    ),
                    cornerRadiusOverrideDp = 18.dp
                )
        )

        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            options.forEachIndexed { i, title ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(i) },
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = title,
                        style = TextStyle(
                            color = if (i == selectedIndex) fg else fg.copy(alpha = 0.68f),
                            fontSize = 13.sp,
                            fontWeight = if (i == selectedIndex) FontWeight.SemiBold else FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

/**
 * Interactive Liquid Glass Slider whose refractive pill thumb bends and magnifies the colored track behind it.
 */
@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    backdrop: GlassBackdropState,
    engineId: GlassEngineId,
    parameters: GlassParameters,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF0A84FF)
) {
    val density = LocalDensity.current
    var dragging by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(
        targetValue = if (dragging) 1f else 0f,
        animationSpec = GlassMotionPhysics.elasticFloatSpring(parameters.animationSpeed),
        label = "sliderPress"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val totalW = with(density) { maxWidth.toPx() }
        val thumbW = with(density) { 48.dp.toPx() }
        val travel = (totalW - thumbW).coerceAtLeast(1f)
        val frac = value.coerceIn(0f, 1f)

        // Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.24f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(frac)
                    .fillMaxHeight()
                    .background(activeColor)
            )
        }

        // Interactive refractive clear-glass pill thumb (magnifies active track when dragged!)
        val thumbScaleX = 1f + 0.14f * pressAnim * parameters.touchDeformation.coerceIn(0f, 1.5f)
        val thumbScaleY = 1f + 0.08f * pressAnim * parameters.touchDeformation.coerceIn(0f, 1.5f)
        val thumbParams = parameters.copy(
            variant = if (dragging) GlassVariant.CLEAR else parameters.variant,
            refractionStrength = (parameters.refractionStrength * (1f + 0.35f * pressAnim)).coerceAtMost(2.2f),
            blurRadiusDp = if (dragging) 1.0f else (parameters.blurRadiusDp * 0.35f).coerceAtMost(4f),
            lensDistortion = (parameters.lensDistortion + 0.22f * pressAnim).coerceAtMost(0.85f)
        )

        Box(
            modifier = Modifier
                .offset { IntOffset((frac * travel).roundToInt(), 0) }
                .size(width = 48.dp, height = 30.dp)
                .graphicsLayer {
                    scaleX = thumbScaleX
                    scaleY = thumbScaleY
                }
                .liquidGlass(
                    backdrop = backdrop,
                    engineId = engineId,
                    parameters = thumbParams,
                    cornerRadiusOverrideDp = 15.dp,
                    pressAmount = pressAnim
                )
        )

        // Gesture layer across full track
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(travel) {
                    detectTapGestures { pos ->
                        onValueChange(((pos.x - thumbW * 0.5f) / travel).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(travel) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val v = ((change.position.x - thumbW * 0.5f) / travel).coerceIn(0f, 1f)
                            onValueChange(v)
                        }
                    )
                }
        )
    }
}

/**
 * Apple iOS 26/27 style Liquid Glass Toggle Switch with elastic refractive thumb.
 */
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    backdrop: GlassBackdropState,
    engineId: GlassEngineId,
    parameters: GlassParameters,
    modifier: Modifier = Modifier,
    checkedColor: Color = Color(0xFF34C759)
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thumbProgress = remember { Animatable(if (checked) 1f else 0f) }
    var pressing by remember { mutableStateOf(false) }

    LaunchedEffect(checked) {
        thumbProgress.animateTo(
            targetValue = if (checked) 1f else 0f,
            animationSpec = GlassMotionPhysics.elasticFloatSpring(parameters.animationSpeed, dampingRatio = 0.62f)
        )
    }

    val stretchAnim by animateFloatAsState(
        targetValue = if (pressing) 1f else 0f,
        animationSpec = GlassMotionPhysics.elasticFloatSpring(parameters.animationSpeed),
        label = "switchStretch"
    )

    val trackW = 66.dp
    val trackH = 36.dp
    val travelPx = with(density) { (trackW - 36.dp - 6.dp).toPx() }

    Box(
        modifier = modifier
            .size(width = trackW, height = trackH)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (thumbProgress.value > 0.5f) checkedColor.copy(alpha = 0.85f)
                else Color.White.copy(alpha = 0.24f)
            )
            .pointerInput(checked) {
                detectTapGestures(
                    onPress = {
                        pressing = true
                        tryAwaitRelease()
                        pressing = false
                    },
                    onTap = { onCheckedChange(!checked) }
                )
            }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val sx = 1f + 0.18f * stretchAnim * parameters.touchDeformation.coerceIn(0f, 1.5f)
        val sy = 1f + 0.08f * stretchAnim * parameters.touchDeformation.coerceIn(0f, 1.5f)

        Box(
            modifier = Modifier
                .offset { IntOffset((thumbProgress.value * travelPx).roundToInt(), 0) }
                .size(width = 36.dp, height = 30.dp)
                .graphicsLayer {
                    scaleX = sx
                    scaleY = sy
                }
                .liquidGlass(
                    backdrop = backdrop,
                    engineId = engineId,
                    parameters = parameters.copy(
                        variant = if (pressing) GlassVariant.CLEAR else parameters.variant,
                        blurRadiusDp = if (pressing) 1f else (parameters.blurRadiusDp * 0.4f).coerceAtMost(5f),
                        refractionStrength = (parameters.refractionStrength * (1f + 0.35f * stretchAnim)).coerceAtMost(2.2f)
                    ),
                    cornerRadiusOverrideDp = 15.dp,
                    pressAmount = stretchAnim
                )
        )
    }
}

/**
 * Floating Liquid Glass Search Bar capsule with live text input.
 */
@Composable
fun GlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    backdrop: GlassBackdropState,
    engineId: GlassEngineId,
    parameters: GlassParameters,
    modifier: Modifier = Modifier,
    placeholder: String = "Search Liquid Glass...",
    backdropIsDark: Boolean = true,
    leadingIcon: @Composable (Color) -> Unit = {},
    clearIcon: @Composable (Color) -> Unit = {}
) {
    val fg = glassForegroundColor(parameters, backdropIsDark)
    GlassSurface(
        backdrop = backdrop,
        engineId = engineId,
        parameters = parameters,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        cornerRadiusDp = 25.dp,
        interactive = true,
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            leadingIcon(fg.copy(alpha = 0.8f))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    BasicText(
                        text = placeholder,
                        style = TextStyle(
                            color = fg.copy(alpha = 0.55f),
                            fontSize = 15.sp
                        )
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = fg,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(Color(0xFF0A84FF)),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable { onQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    clearIcon(fg.copy(alpha = 0.8f))
                }
            }
        }
    }
}
