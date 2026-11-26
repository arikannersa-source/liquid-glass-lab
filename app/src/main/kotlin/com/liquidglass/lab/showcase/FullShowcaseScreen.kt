package com.liquidglass.lab.showcase

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CompassCalibration
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.lab.model.BackgroundMode
import com.liquidglass.lab.model.LabState
import com.liquidglass.sdk.components.GlassBottomNavigationBar
import com.liquidglass.sdk.components.GlassButton
import com.liquidglass.sdk.components.GlassCard
import com.liquidglass.sdk.components.GlassDialogOverlay
import com.liquidglass.sdk.components.GlassFab
import com.liquidglass.sdk.components.GlassFloatingIcon
import com.liquidglass.sdk.components.GlassMenuOverlay
import com.liquidglass.sdk.components.GlassMorphCluster
import com.liquidglass.sdk.components.GlassNavItem
import com.liquidglass.sdk.components.GlassSearchBar
import com.liquidglass.sdk.components.GlassSegmentedControl
import com.liquidglass.sdk.components.GlassSlider
import com.liquidglass.sdk.components.GlassSurface
import com.liquidglass.sdk.components.GlassSwitch
import com.liquidglass.sdk.components.glassForegroundColor
import com.liquidglass.sdk.core.GlassBackdropState
import com.liquidglass.sdk.core.GlassVariant
import kotlin.math.roundToInt

/**
 * Complete Interactive Showcase demonstrating ALL 13 required Liquid Glass components
 * under the currently active rendering engine:
 * 1. Buttons, 2. FAB, 3. Bottom Navigation Bar, 4. Segmented Controls, 5. Sliders,
 * 6. Switches, 7. Search Bars, 8. Cards, 9. Menus, 10. Dialogs, 11. Floating Icons,
 * 12. Draggable Glass Shapes, 13. Merging & Morphing Glass Elements.
 */
@Composable
fun FullShowcaseScreen(
    state: LabState,
    backdrop: GlassBackdropState,
    topPaddingDp: Float,
    bottomPaddingDp: Float,
    modifier: Modifier = Modifier
) {
    val engineId = state.selectedEngineId
    val params = state.parameters
    val isDark = state.backdropIsDark
    val fg = glassForegroundColor(params, isDark)

    var searchQuery by remember { mutableStateOf("") }
    var selectedSegment by remember { mutableIntStateOf(0) }
    var sliderVal by remember { mutableFloatStateOf(0.62f) }
    var switchChecked by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var menuVisible by remember { mutableStateOf(false) }
    var dialogVisible by remember { mutableStateOf(false) }
    var morphExpanded by remember { mutableStateOf(false) }
    var dragLensOffset by remember { mutableStateOf(Offset.Zero) }

    val navItems = remember {
        listOf(
            GlassNavItem("Home") { color, _ -> Icon(Icons.Rounded.Home, null, tint = color, modifier = Modifier.size(22.dp)) },
            GlassNavItem("Explore") { color, _ -> Icon(Icons.Rounded.Explore, null, tint = color, modifier = Modifier.size(22.dp)) },
            GlassNavItem("Saved") { color, _ -> Icon(Icons.Rounded.Favorite, null, tint = color, modifier = Modifier.size(22.dp)) },
            GlassNavItem("Profile") { color, _ -> Icon(Icons.Rounded.Person, null, tint = color, modifier = Modifier.size(22.dp)) }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (state.backgroundMode == BackgroundMode.SCROLLING_FEED) {
            // In Scrolling Feed mode, leave middle canvas touch-transparent so the user can scroll
            // the live photo/editorial feed directly behind the floating glass bars and draggable lens!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topPaddingDp.dp + 8.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    backdrop = backdrop,
                    engineId = engineId,
                    parameters = params,
                    backdropIsDark = isDark,
                    leadingIcon = { c -> Icon(Icons.Rounded.Search, null, tint = c, modifier = Modifier.size(20.dp)) },
                    clearIcon = { c -> Icon(Icons.Rounded.Close, null, tint = c, modifier = Modifier.size(16.dp)) }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassButton(
                        text = "Menu",
                        backdrop = backdrop,
                        engineId = engineId,
                        parameters = params,
                        backdropIsDark = isDark,
                        leadingIcon = { c -> Icon(Icons.Rounded.Menu, null, tint = c, modifier = Modifier.size(18.dp)) },
                        onClick = { menuVisible = true }
                    )
                    GlassButton(
                        text = "Dialog",
                        backdrop = backdrop,
                        engineId = engineId,
                        parameters = params,
                        backdropIsDark = isDark,
                        onClick = { dialogVisible = true }
                    )
                    Spacer(Modifier.weight(1f))
                    GlassSwitch(
                        checked = switchChecked,
                        onCheckedChange = { switchChecked = it },
                        backdrop = backdrop,
                        engineId = engineId,
                        parameters = params
                    )
                }
            }
        } else {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        top = topPaddingDp.dp + 8.dp,
                        bottom = bottomPaddingDp.dp + 92.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Search Bar
                GlassSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    backdrop = backdrop,
                    engineId = engineId,
                    parameters = params,
                    backdropIsDark = isDark,
                    leadingIcon = { c -> Icon(Icons.Rounded.Search, null, tint = c, modifier = Modifier.size(20.dp)) },
                    clearIcon = { c -> Icon(Icons.Rounded.Close, null, tint = c, modifier = Modifier.size(16.dp)) }
                )

                // 2. Segmented Control
                GlassSegmentedControl(
                    options = listOf("Optical", "Controls", "Morph"),
                    selectedIndex = selectedSegment,
                    onSelect = { selectedSegment = it },
                    backdrop = backdrop,
                    engineId = engineId,
                    parameters = params,
                    backdropIsDark = isDark
                )

                // 3. Buttons & FAB row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassButton(
                        text = "Regular",
                        backdrop = backdrop,
                        engineId = engineId,
                        parameters = params,
                        backdropIsDark = isDark,
                        leadingIcon = { c -> Icon(Icons.Rounded.AutoAwesome, null, tint = c, modifier = Modifier.size(18.dp)) },
                        onClick = { state.setVariant(GlassVariant.REGULAR) }
                    )
                    GlassButton(
                        text = "Clear",
                        backdrop = backdrop,
                        engineId = engineId,
                        parameters = params.copy(
                            tintColorArgb = 0xFF0A84FFL,
                            tintOpacity = params.tintOpacity.coerceAtLeast(0.26f)
                        ),
                        backdropIsDark = true,
                        onClick = { state.setVariant(GlassVariant.CLEAR) }
                    )
                    Spacer(Modifier.weight(1f))
                    GlassFab(
                        backdrop = backdrop,
                        engineId = engineId,
                        parameters = params,
                        backdropIsDark = isDark,
                        onClick = { menuVisible = true }
                    ) { c ->
                        Icon(Icons.Rounded.Add, contentDescription = "Glass FAB", tint = c, modifier = Modifier.size(26.dp))
                    }
                }

                // 4. Floating Icons row (squircle iOS style)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GlassFloatingIcon(backdrop, engineId, params, "Camera", backdropIsDark = isDark) { c ->
                        Icon(Icons.Rounded.CameraAlt, null, tint = c, modifier = Modifier.size(28.dp))
                    }
                    GlassFloatingIcon(backdrop, engineId, params, "Photos", backdropIsDark = isDark) { c ->
                        Icon(Icons.Rounded.PhotoLibrary, null, tint = c, modifier = Modifier.size(28.dp))
                    }
                    GlassFloatingIcon(backdrop, engineId, params, "Music", backdropIsDark = isDark) { c ->
                        Icon(Icons.Rounded.MusicNote, null, tint = c, modifier = Modifier.size(28.dp))
                    }
                    GlassFloatingIcon(backdrop, engineId, params, "Optics", backdropIsDark = isDark) { c ->
                        Icon(Icons.Rounded.CompassCalibration, null, tint = c, modifier = Modifier.size(28.dp))
                    }
                }

                // 5. Card containing Sliders, Switches, Menu & Dialog triggers
                GlassCard(
                    backdrop = backdrop,
                    engineId = engineId,
                    parameters = params,
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadiusDp = params.cornerRadiusDp.dp,
                    interactive = false
                ) {
                    Text(
                        text = state.activeEngine.displayName,
                        color = fg,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = state.activeEngine.opticalSummary,
                        color = fg.copy(alpha = 0.78f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(Modifier.height(14.dp))

                    // Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Refractive Slider", color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("${(sliderVal * 100).roundToInt()}%", color = fg.copy(alpha = 0.75f), fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    GlassSlider(
                        value = sliderVal,
                        onValueChange = { sliderVal = it },
                        backdrop = backdrop,
                        engineId = engineId,
                        parameters = params
                    )

                    Spacer(Modifier.height(12.dp))

                    // Switch + Menu + Dialog actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlassSwitch(
                                checked = switchChecked,
                                onCheckedChange = { switchChecked = it },
                                backdrop = backdrop,
                                engineId = engineId,
                                parameters = params
                            )
                            Text(
                                text = if (switchChecked) "Adaptive On" else "Off",
                                color = fg,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassButton(
                                text = "Menu",
                                backdrop = backdrop,
                                engineId = engineId,
                                parameters = params,
                                backdropIsDark = isDark,
                                onClick = { menuVisible = true }
                            )
                            GlassButton(
                                text = "Dialog",
                                backdrop = backdrop,
                                engineId = engineId,
                                parameters = params,
                                backdropIsDark = isDark,
                                onClick = { dialogVisible = true }
                            )
                        }
                    }
                }

                // 6. Merging & Morphing Liquid Glass Cluster right inside the showcase
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlassMorphCluster(
                        backdrop = backdrop,
                        engineId = engineId,
                        parameters = params,
                        expanded = morphExpanded,
                        onToggleExpanded = { morphExpanded = !morphExpanded },
                        backdropIsDark = isDark,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // 7. Freely Draggable Optical Inspection Lens floating over everything
        GlassSurface(
            backdrop = backdrop,
            engineId = engineId,
            parameters = params.copy(
                variant = GlassVariant.CLEAR,
                blurRadiusDp = (params.blurRadiusDp * 0.25f).coerceAtMost(3f)
            ),
            cornerRadiusDp = 42.dp,
            interactive = true,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
                .offset { IntOffset(dragLensOffset.x.roundToInt(), dragLensOffset.y.roundToInt()) }
                .size(84.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        dragLensOffset += dragAmount
                    }
                }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Settings, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Text("Drag", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // 8. Floating Bottom Navigation Bar with draggable magnifying selection lens
        GlassBottomNavigationBar(
            items = navItems,
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
            backdrop = backdrop,
            engineId = engineId,
            parameters = params,
            backdropIsDark = isDark,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = bottomPaddingDp.dp + 10.dp)
        )

        // 9. In-window Glass Menu Overlay (refracts live backdrop!)
        GlassMenuOverlay(
            visible = menuVisible,
            backdrop = backdrop,
            engineId = engineId,
            parameters = params,
            backdropIsDark = isDark,
            items = listOf(
                "Switch to Regular Glass" to { state.setVariant(GlassVariant.REGULAR) },
                "Switch to Clear Glass" to { state.setVariant(GlassVariant.CLEAR) },
                "Reset Engine to Original" to { state.resetToEngineOriginal() },
                "Open Glass Dialog" to { dialogVisible = true }
            ),
            onDismiss = { menuVisible = false }
        )

        // 10. In-window Glass Dialog Overlay (refracts live backdrop!)
        GlassDialogOverlay(
            visible = dialogVisible,
            title = "Liquid Glass Material Check",
            body = "Rendered live by ${state.activeEngine.displayName} (${state.activeEngine.license}). Notice how edges refract the live background beneath this dialog rather than dimming behind a flat window.",
            backdrop = backdrop,
            engineId = engineId,
            parameters = params,
            backdropIsDark = isDark,
            confirmLabel = "Keep Engine",
            dismissLabel = "Close",
            onConfirm = {},
            onDismiss = { dialogVisible = false }
        )
    }
}
