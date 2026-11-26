package com.liquidglass.sdk.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.sdk.core.GlassAppearance
import com.liquidglass.sdk.core.GlassBackdropState
import com.liquidglass.sdk.core.GlassEngineId
import com.liquidglass.sdk.core.GlassParameters
import com.liquidglass.sdk.core.GlassSubShape
import com.liquidglass.sdk.core.GlassVariant
import com.liquidglass.sdk.modifier.GlassMotionPhysics
import com.liquidglass.sdk.modifier.liquidGlass

/** Computes readable foreground content color for glass surfaces based on appearance & variant. */
fun glassForegroundColor(parameters: GlassParameters, backdropIsDark: Boolean = true): Color {
    return when {
        parameters.variant == GlassVariant.CLEAR -> Color.White
        parameters.appearance == GlassAppearance.LIGHT -> Color(0xFF14161C)
        parameters.appearance == GlassAppearance.DARK -> Color(0xFFF6F7FB)
        else -> if (backdropIsDark) Color(0xFFF6F7FB) else Color(0xFF14161C)
    }
}

/**
 * Foundational reusable Liquid Glass surface composable with optional interactive elastic
 * gel-press physics and live touch point tracking.
 */
@Composable
fun GlassSurface(
    backdrop: GlassBackdropState,
    engineId: GlassEngineId,
    parameters: GlassParameters,
    modifier: Modifier = Modifier,
    cornerRadiusDp: Dp? = null,
    interactive: Boolean = false,
    extraShapes: List<GlassSubShape> = emptyList(),
    onClick: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    var touchOffset by remember { mutableStateOf<Offset?>(null) }

    val pressAnim by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = GlassMotionPhysics.elasticFloatSpring(parameters.animationSpeed),
        label = "glassPress"
    )
    val (scaleX, scaleY) = GlassMotionPhysics.computeGelScale(
        pressProgress = pressAnim,
        touchDeformation = parameters.touchDeformation
    )

    val touchModifier = if (interactive || onClick != null) {
        Modifier.pointerInput(onClick) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                pressed = true
                touchOffset = down.position
                val up = waitForUpOrCancellation()
                pressed = false
                if (up != null) {
                    onClick?.invoke()
                }
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                this.scaleX = scaleX
                this.scaleY = scaleY
            }
            .liquidGlass(
                backdrop = backdrop,
                engineId = engineId,
                parameters = parameters,
                cornerRadiusOverrideDp = cornerRadiusDp,
                touchOffsetPx = touchOffset,
                pressAmount = pressAnim,
                extraShapes = extraShapes
            )
            .then(touchModifier),
        contentAlignment = contentAlignment,
        content = content
    )
}

/**
 * Reusable Liquid Glass Card container.
 */
@Composable
fun GlassCard(
    backdrop: GlassBackdropState,
    engineId: GlassEngineId,
    parameters: GlassParameters,
    modifier: Modifier = Modifier,
    cornerRadiusDp: Dp? = null,
    interactive: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassSurface(
        backdrop = backdrop,
        engineId = engineId,
        parameters = parameters,
        modifier = modifier,
        cornerRadiusDp = cornerRadiusDp,
        interactive = interactive,
        onClick = onClick,
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/**
 * Floating squircle glass icon (iOS 26/27 style) with elastic press magnification & rim light.
 */
@Composable
fun GlassFloatingIcon(
    backdrop: GlassBackdropState,
    engineId: GlassEngineId,
    parameters: GlassParameters,
    label: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 64.dp,
    backdropIsDark: Boolean = true,
    onClick: () -> Unit = {},
    iconContent: @Composable BoxScope.(Color) -> Unit
) {
    val fg = glassForegroundColor(parameters, backdropIsDark)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        GlassSurface(
            backdrop = backdrop,
            engineId = engineId,
            parameters = parameters,
            modifier = Modifier.size(sizeDp),
            cornerRadiusDp = (sizeDp * 0.28f).coerceAtMost(parameters.cornerRadiusDp.dp),
            interactive = true,
            onClick = onClick,
            contentAlignment = Alignment.Center
        ) {
            iconContent(fg)
        }
        BasicText(
            text = label,
            style = TextStyle(
                color = if (backdropIsDark) Color.White else Color(0xFF14161C),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

/**
 * In-window Liquid Glass Menu overlay so the menu refracts the live app backdrop directly
 * (Android's separate Dialog/Popup windows cannot sample the parent window's GraphicsLayer).
 */
@Composable
fun GlassMenuOverlay(
    visible: Boolean,
    backdrop: GlassBackdropState,
    engineId: GlassEngineId,
    parameters: GlassParameters,
    items: List<Pair<String, () -> Unit>>,
    onDismiss: () -> Unit,
    backdropIsDark: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    val fg = glassForegroundColor(parameters, backdropIsDark)
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            backdrop = backdrop,
            engineId = engineId,
            parameters = parameters,
            modifier = Modifier.widthIn(min = 210.dp, max = 270.dp),
            cornerRadiusDp = 24.dp,
            interactive = false
        ) {
            items.forEachIndexed { idx, (title, action) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            action()
                            onDismiss()
                        }
                        .padding(vertical = 11.dp, horizontal = 8.dp)
                ) {
                    BasicText(
                        text = title,
                        style = TextStyle(
                            color = fg,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                if (idx < items.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(fg.copy(alpha = 0.14f))
                    )
                }
            }
        }
    }
}

/**
 * In-window Liquid Glass Dialog overlay with optional dimming scrim and glass action buttons.
 */
@Composable
fun GlassDialogOverlay(
    visible: Boolean,
    title: String,
    body: String,
    backdrop: GlassBackdropState,
    engineId: GlassEngineId,
    parameters: GlassParameters,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "Apply",
    dismissLabel: String = "Cancel",
    backdropIsDark: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    val fg = glassForegroundColor(parameters, backdropIsDark)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.22f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            backdrop = backdrop,
            engineId = engineId,
            parameters = parameters,
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            cornerRadiusDp = 30.dp,
            interactive = false
        ) {
            BasicText(
                text = title,
                style = TextStyle(
                    color = fg,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(Modifier.height(8.dp))
            BasicText(
                text = body,
                style = TextStyle(
                    color = fg.copy(alpha = 0.82f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                GlassButton(
                    text = dismissLabel,
                    backdrop = backdrop,
                    engineId = engineId,
                    parameters = parameters,
                    backdropIsDark = backdropIsDark,
                    onClick = onDismiss
                )
                GlassButton(
                    text = confirmLabel,
                    backdrop = backdrop,
                    engineId = engineId,
                    parameters = parameters.copy(
                        tintColorArgb = 0xFF0A84FFL,
                        tintOpacity = parameters.tintOpacity.coerceAtLeast(0.32f)
                    ),
                    backdropIsDark = true,
                    onClick = {
                        onConfirm()
                        onDismiss()
                    }
                )
            }
        }
    }
}
