package com.liquidglass.lab.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.lab.model.LabState
import com.liquidglass.sdk.core.GlassAppearance
import com.liquidglass.sdk.core.GlassVariant
import java.util.Locale

private val TintSwatches = listOf(
    "Pure Ice" to 0xFFFFFFFFL,
    "Sky Cyan" to 0xFF64D2FFL,
    "iOS Blue" to 0xFF0A84FFL,
    "Mint" to 0xFF30D158L,
    "Amber" to 0xFFFFD60AL,
    "Coral" to 0xFFFF453AL,
    "Violet" to 0xFFBF5AF2L,
    "Obsidian" to 0xFF1C1C22L
)

private val HazeProfileLabels = listOf("Circular", "Squircle", "Sinusoidal", "Concave")

/**
 * Live Material 3 Customization Panel exposing all 16 optical, geometric, lighting,
 * and motion controls.
 *
 * Uses `scrimColor = Color.Transparent` and caps height at 56% of the display so the
 * upper viewport remains completely un-dimmed for real-time optical inspection while
 * dragging any slider. Unsupported controls for a given engine are honestly badged and disabled.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomizationSheet(
    state: LabState,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val engine = state.activeEngine
    val cap = engine.capabilities
    val p = state.parameters

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = Color.Transparent,
        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.56f)
                .padding(horizontal = 16.dp)
        ) {
            // Header + Reset to Original
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Optical & Material Controls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Engine: ${engine.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                FilledTonalButton(onClick = { state.resetToEngineOriginal() }) {
                    Icon(
                        Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text("Reset Original", fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Variant: Regular vs Clear Liquid Glass
                SectionLabel("Material Variant & Adaptive Contrast")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                        GlassVariant.entries.forEachIndexed { idx, variant ->
                            val enabled = variant == GlassVariant.REGULAR || cap.supportsClearVariant
                            SegmentedButton(
                                selected = p.variant == variant,
                                onClick = { state.setVariant(variant) },
                                enabled = enabled,
                                shape = SegmentedButtonDefaults.itemShape(idx, GlassVariant.entries.size)
                            ) {
                                Text(
                                    text = if (variant == GlassVariant.REGULAR) "Regular" else "Clear",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    GlassAppearance.entries.forEachIndexed { idx, app ->
                        SegmentedButton(
                            selected = p.appearance == app,
                            onClick = { state.setAppearance(app) },
                            shape = SegmentedButtonDefaults.itemShape(idx, GlassAppearance.entries.size)
                        ) {
                            Text(app.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 12.sp)
                        }
                    }
                }

                // Engine-specific controls (Haze surface profile, Outward/Inward sampling)
                if (cap.supportsHazeSurfaceProfiles) {
                    SectionLabel("Haze 2.1 Bevel Surface Profile")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        HazeProfileLabels.forEachIndexed { idx, label ->
                            SegmentedButton(
                                selected = p.surfaceProfile == idx,
                                onClick = { state.parameters = p.copy(surfaceProfile = idx) },
                                shape = SegmentedButtonDefaults.itemShape(idx, HazeProfileLabels.size)
                            ) {
                                Text(label, fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (cap.supportsDirectionToggle) {
                    val defaultOutward = engine.id.key == "shayann_ios27"
                    val isOutward = p.outwardSamplingOverride ?: defaultOutward
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Outward Rim Sampling (Convex Edge Bend)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isOutward) "Samples outside bounds into uPad (Apple iOS 27 behavior)" else "Samples inward inside silhouette",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isOutward,
                                onCheckedChange = { state.parameters = p.copy(outwardSamplingOverride = it) }
                            )
                        }
                    }
                }

                HorizontalDivider()
                SectionLabel("1–4. Optical Refraction & Lens Geometry")

                ParamSlider(
                    title = "1. Refraction Strength",
                    valueText = String.format(Locale.US, "%.2fx", p.refractionStrength),
                    value = p.refractionStrength,
                    range = 0f..2.5f,
                    supported = cap.refractionStrength,
                    engineName = engine.shortName,
                    onValueChange = { state.parameters = p.copy(refractionStrength = it) }
                )

                ParamSlider(
                    title = "2. Glass Thickness (Bevel Band)",
                    valueText = String.format(Locale.US, "%.1f dp", p.glassThicknessDp),
                    value = p.glassThicknessDp,
                    range = 0f..64f,
                    supported = cap.glassThickness,
                    engineName = engine.shortName,
                    onValueChange = { state.parameters = p.copy(glassThicknessDp = it) }
                )

                ParamSlider(
                    title = "3. Edge Curvature (Meniscus Slope)",
                    valueText = String.format(Locale.US, "%.2f", p.edgeCurvature),
                    value = p.edgeCurvature,
                    range = 0.2f..2.5f,
                    supported = cap.edgeCurvature,
                    engineName = engine.shortName,
                    onValueChange = { state.parameters = p.copy(edgeCurvature = it) }
                )

                ParamSlider(
                    title = "4. Lens Distortion (Interior Magnification)",
                    valueText = String.format(Locale.US, "%+.2f", p.lensDistortion),
                    value = p.lensDistortion,
                    range = -0.5f..1.0f,
                    supported = cap.lensDistortion,
                    engineName = engine.shortName,
                    onValueChange = { state.parameters = p.copy(lensDistortion = it) }
                )

                HorizontalDivider()
                SectionLabel("5–8. Blur, Transmittance, Tint & Dispersion")

                ParamSlider(
                    title = "5. Background Blur Radius",
                    valueText = String.format(Locale.US, "%.1f dp", p.blurRadiusDp),
                    value = p.blurRadiusDp,
                    range = 0f..48f,
                    supported = cap.backgroundBlur,
                    engineName = engine.shortName,
                    onValueChange = { state.parameters = p.copy(blurRadiusDp = it) }
                )

                ParamSlider(
                    title = "6. Transparency (Body Transmittance)",
                    valueText = String.format(Locale.US, "%.0f%%", p.transparency * 100f),
                    value = p.transparency,
                    range = 0f..1f,
                    supported = cap.transparency,
                    engineName = engine.shortName,
                    onValueChange = { state.parameters = p.copy(transparency = it) }
                )

                ParamSlider(
                    title = "7. Tint Strength",
                    valueText = String.format(Locale.US, "%.0f%%", p.tintOpacity * 100f),
                    value = p.tintOpacity,
                    range = 0f..0.95f,
                    supported = cap.tint,
                    engineName = engine.shortName,
                    onValueChange = { state.parameters = p.copy(tintOpacity = it) }
                )

                if (cap.tint) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TintSwatches.forEach { (label, argb) ->
                            val selected = (p.tintColorArgb and 0xFFFFFFL) == (argb and 0xFFFFFFL)
                            FilterChip(
                                selected = selected,
                                onClick = { state.parameters = p.copy(tintColorArgb = argb) },
                                label = { Text(label, fontSize = 11.sp) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(Color(argb.toInt()))
                                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    )
                                }
                            )
                        }
                    }
                }

                ParamSlider(
                    title = "8. Chromatic Aberration (RGB Dispersion)",
                    valueText = String.format(Locale.US, "%.2f", p.chromaticAberration),
                    value = p.chromaticAberration,
                    range = 0f..2.5f,
                    supported = cap.chromaticAberration,
                    engineName = engine.shortName,
                    onValueChange = { state.parameters = p.copy(chromaticAberration = it) }
                )

                HorizontalDivider()
                SectionLabel("9–12. Specular Rim, Lighting & Shadows")

                ParamSlider(
                    title = "9. Specular Highlights",
                    valueText = String.format(Locale.US, "%.2fx", p.specularIntensity),
                    value = p.specularIntensity,
                    range = 0f..2.5f,
                    supported = cap.specularHighlights,
                    engineName = engine.shortName,
                    onValueChange = { state.parameters = p.copy(specularIntensity = it) }
                )

                ParamSlider(
                    title = "10. Lighting Direction",
                    valueText = String.format(Locale.US, "%.0f°", p.lightAngleDegrees),
                    value = p.lightAngleDegrees,
                    range = 0f..360f,
                    supported = cap.lightingDirection,
                    engineName = engine.shortName,
                    onValueChange = {
                        state.tiltLightingEnabled = false
                        state.parameters = p.copy(lightAngleDegrees = it)
                    }
                )

                if (cap.lightingDirection) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Drive light angle from Pixel gravity sensor",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = state.tiltLightingEnabled,
                            onCheckedChange = { state.tiltLightingEnabled = it }
                        )
                    }
                }

                ParamSlider(
                    title = "11. Edge Illumination (Fresnel Rim)",
                    valueText = String.format(Locale.US, "%.2fx", p.edgeIllumination),
                    value = p.edgeIllumination,
                    range = 0f..2.5f,
                    supported = cap.edgeIllumination,
                    engineName = engine.shortName,
                    onValueChange = { state.parameters = p.copy(edgeIllumination = it) }
                )

                ParamSlider(
                    title = "12. Shadows (Elevation & Inner Bevel)",
                    valueText = String.format(Locale.US, "%.2fx", p.shadowIntensity),
                    value = p.shadowIntensity,
                    range = 0f..2.5f,
                    supported = cap.shadows,
                    engineName = engine.shortName,
                    onValueChange = { state.parameters = p.copy(shadowIntensity = it) }
                )

                HorizontalDivider()
                SectionLabel("13–16. Shape, Touch Elasticity & Liquid Morphing")

                ParamSlider(
                    title = "13. Corner Radius",
                    valueText = String.format(Locale.US, "%.0f dp", p.cornerRadiusDp),
                    value = p.cornerRadiusDp,
                    range = 0f..56f,
                    supported = cap.cornerRadius,
                    engineName = engine.shortName,
                    onValueChange = { state.parameters = p.copy(cornerRadiusDp = it) }
                )

                ParamSlider(
                    title = "14. Touch Deformation & Press Swell",
                    valueText = String.format(Locale.US, "%.2fx", p.touchDeformation),
                    value = p.touchDeformation,
                    range = 0f..2.5f,
                    supported = cap.touchDeformation,
                    engineName = engine.shortName,
                    onValueChange = { state.parameters = p.copy(touchDeformation = it) }
                )

                ParamSlider(
                    title = "15. Animation Speed",
                    valueText = String.format(Locale.US, "%.2fx", p.animationSpeed),
                    value = p.animationSpeed,
                    range = 0.25f..2.5f,
                    supported = cap.animationSpeed,
                    engineName = engine.shortName,
                    onValueChange = { state.parameters = p.copy(animationSpeed = it) }
                )

                ParamSlider(
                    title = "16. Morphing Behavior (smin Bridge Radius)",
                    valueText = String.format(Locale.US, "%.1f dp", p.morphSmoothingDp),
                    value = p.morphSmoothingDp,
                    range = 0f..64f,
                    supported = cap.morphingBehavior,
                    engineName = engine.shortName,
                    onValueChange = { state.parameters = p.copy(morphSmoothingDp = it) }
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ParamSlider(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    supported: Boolean,
    engineName: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (supported) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                }
            )
            if (supported) {
                Text(
                    text = valueText,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = "Unsupported by $engineName",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            enabled = supported,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
