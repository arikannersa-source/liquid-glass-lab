package com.liquidglass.lab.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.lab.model.BackgroundMode
import com.liquidglass.lab.model.LabCheckerSizesDp
import com.liquidglass.lab.model.LabPhotoTitles
import com.liquidglass.lab.model.LabSolidSwatches
import com.liquidglass.lab.model.LabState

/**
 * Selector sheet for all 8 laboratory calibration backgrounds + sub-options
 * (photo selection, checkerboard tile scale, editorial text polarity, and solid color swatches).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BackgroundPickerSheet(
    state: LabState,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = Color.Transparent,
        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Calibration Backgrounds (8 Modes)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Switch backgrounds instantaneously while keeping engine, dimensions, and lighting identical.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                BackgroundMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.backgroundMode == mode,
                        onClick = { state.backgroundMode = mode },
                        label = { Text(mode.label, fontSize = 12.sp) }
                    )
                }
            }

            when (state.backgroundMode) {
                BackgroundMode.PHOTO -> {
                    HorizontalDivider()
                    Text("High-Resolution Photograph", style = MaterialTheme.typography.labelLarge)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        LabPhotoTitles.forEachIndexed { idx, title ->
                            SegmentedButton(
                                selected = state.photoIndex == idx,
                                onClick = { state.photoIndex = idx },
                                shape = SegmentedButtonDefaults.itemShape(idx, LabPhotoTitles.size)
                            ) {
                                Text(title, fontSize = 11.sp)
                            }
                        }
                    }
                }

                BackgroundMode.CHECKERBOARD -> {
                    HorizontalDivider()
                    Text("Checkerboard Tile Size (Dispersion & Snell Stress Test)", style = MaterialTheme.typography.labelLarge)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        LabCheckerSizesDp.forEachIndexed { idx, sizeDp ->
                            SegmentedButton(
                                selected = state.checkerScaleIndex == idx,
                                onClick = { state.checkerScaleIndex = idx },
                                shape = SegmentedButtonDefaults.itemShape(idx, LabCheckerSizesDp.size)
                            ) {
                                Text("${sizeDp.toInt()} dp", fontSize = 12.sp)
                            }
                        }
                    }
                }

                BackgroundMode.TEXT_WALL -> {
                    HorizontalDivider()
                    Text("Typography Contrast Polarity", style = MaterialTheme.typography.labelLarge)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf("Light Paper" to false, "Dark OLED" to true).forEachIndexed { idx, (label, isDark) ->
                            SegmentedButton(
                                selected = state.textWallDark == isDark,
                                onClick = { state.textWallDark = isDark },
                                shape = SegmentedButtonDefaults.itemShape(idx, 2)
                            ) {
                                Text(label, fontSize = 12.sp)
                            }
                        }
                    }
                }

                BackgroundMode.SOLID_COLOR -> {
                    HorizontalDivider()
                    Text("Calibrated Solid Swatches", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LabSolidSwatches.forEachIndexed { idx, swatch ->
                            FilterChip(
                                selected = state.solidColorIndex == idx,
                                onClick = { state.solidColorIndex = idx },
                                label = { Text(swatch.name, fontSize = 11.sp) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(swatch.color)
                                            .border(1.dp, Color.Gray.copy(alpha = 0.6f), CircleShape)
                                    )
                                }
                            )
                        }
                    }
                }

                else -> {
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}
