package com.liquidglass.sdk.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.sdk.core.GlassBackdropState
import com.liquidglass.sdk.core.GlassEngineId
import com.liquidglass.sdk.core.GlassParameters

/**
 * Interactive Liquid Glass capsule button with elastic gel-press animation and fingertip glow.
 */
@Composable
fun GlassButton(
    text: String,
    backdrop: GlassBackdropState,
    engineId: GlassEngineId,
    parameters: GlassParameters,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdropIsDark: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    leadingIcon: (@Composable (Color) -> Unit)? = null
) {
    val fg = glassForegroundColor(parameters, backdropIsDark)
    GlassSurface(
        backdrop = backdrop,
        engineId = engineId,
        parameters = parameters,
        modifier = modifier,
        cornerRadiusDp = parameters.cornerRadiusDp.dp,
        interactive = true,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (leadingIcon != null) leadingIcon(fg)
            BasicText(
                text = text,
                style = TextStyle(
                    color = fg,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

/**
 * Circular / squircle Liquid Glass Floating Action Button (FAB).
 */
@Composable
fun GlassFab(
    backdrop: GlassBackdropState,
    engineId: GlassEngineId,
    parameters: GlassParameters,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 60.dp,
    backdropIsDark: Boolean = true,
    content: @Composable BoxScope.(Color) -> Unit
) {
    val fg = glassForegroundColor(parameters, backdropIsDark)
    GlassSurface(
        backdrop = backdrop,
        engineId = engineId,
        parameters = parameters,
        modifier = modifier.size(sizeDp),
        cornerRadiusDp = sizeDp / 2f,
        interactive = true,
        onClick = onClick,
        contentAlignment = Alignment.Center
    ) {
        content(fg)
    }
}
