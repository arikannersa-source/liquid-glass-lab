package com.example.liquidglassdemo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.liquidglass.sdk.components.GlassBottomNavigationBar
import com.liquidglass.sdk.components.GlassButton
import com.liquidglass.sdk.components.GlassCard
import com.liquidglass.sdk.components.GlassNavItem
import com.liquidglass.sdk.components.GlassSearchBar
import com.liquidglass.sdk.components.GlassSegmentedControl
import com.liquidglass.sdk.components.GlassSlider
import com.liquidglass.sdk.components.GlassSwitch
import com.liquidglass.sdk.core.GlassAppearance
import com.liquidglass.sdk.core.GlassEngineId
import com.liquidglass.sdk.core.GlassEngineRegistry
import com.liquidglass.sdk.core.GlassPreset
import com.liquidglass.sdk.core.GlassVariant
import com.liquidglass.sdk.core.liquidGlassBackdropSource
import com.liquidglass.sdk.core.rememberGlassBackdropState

/**
 * Minimal self-contained Jetpack Compose example showing how any Android app imports
 * `:liquidglass-sdk` without writing a single line of shader code.
 */
@Composable
fun MinimalLiquidGlassScreen(exportedPresetJson: String? = null) {
    val backdrop = rememberGlassBackdropState()

    // Load exported JSON preset if provided, otherwise use engine defaults
    val preset = remember(exportedPresetJson) {
        exportedPresetJson?.let { GlassPreset.fromJson(it).getOrNull() }
    }
    val engineId = GlassEngineId.fromKey(preset?.engineKey) ?: GlassEngineId.SHAYANN_IOS27
    val params = preset?.parameters
        ?: GlassEngineRegistry.get(engineId).defaultParameters(
            variant = GlassVariant.REGULAR,
            appearance = GlassAppearance.ADAPTIVE
        )

    var query by remember { mutableStateOf("") }
    var segment by remember { mutableIntStateOf(0) }
    var volume by remember { mutableFloatStateOf(0.7f) }
    var enabled by remember { mutableStateOf(true) }
    var tab by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Live background layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .liquidGlassBackdropSource(backdrop)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0B132B), Color(0xFF1C2541), Color(0xFF3A506B), Color(0xFF5BC0BE))
                    )
                )
        )

        // 2. Reusable Liquid Glass UI components
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 56.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassSearchBar(
                query = query,
                onQueryChange = { query = it },
                backdrop = backdrop,
                engineId = engineId,
                parameters = params
            )

            GlassSegmentedControl(
                options = listOf("Spatial", "Studio", "Live"),
                selectedIndex = segment,
                onSelect = { segment = it },
                backdrop = backdrop,
                engineId = engineId,
                parameters = params
            )

            GlassCard(
                backdrop = backdrop,
                engineId = engineId,
                parameters = params,
                modifier = Modifier.fillMaxWidth()
            ) {
                GlassSlider(
                    value = volume,
                    onValueChange = { volume = it },
                    backdrop = backdrop,
                    engineId = engineId,
                    parameters = params
                )
                GlassSwitch(
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                    backdrop = backdrop,
                    engineId = engineId,
                    parameters = params
                )
                GlassButton(
                    text = "Primary Liquid Action",
                    backdrop = backdrop,
                    engineId = engineId,
                    parameters = params,
                    onClick = {}
                )
            }
        }

        GlassBottomNavigationBar(
            items = listOf(
                GlassNavItem("Discover"),
                GlassNavItem("Radio"),
                GlassNavItem("Library")
            ),
            selectedIndex = tab,
            onSelect = { tab = it },
            backdrop = backdrop,
            engineId = engineId,
            parameters = params,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 28.dp)
        )
    }
}
