package com.liquidglass.lab.showcase

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.lab.model.LabState
import com.liquidglass.sdk.core.GlassEngineRegistry

/**
 * Material 3 Engine Selection & Provenance Audit Sheet.
 * Discloses each engine's upstream repository, license, calibration basis, and porting changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngineCatalogSheet(
    state: LabState,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Rendering Engines (9 Integrated)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Keep custom slider values identical on switch",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = state.useIdenticalParamsOnSwitch,
                    onCheckedChange = { state.useIdenticalParamsOnSwitch = it }
                )
            }
            Spacer(Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                items(GlassEngineRegistry.all(), key = { it.engineKey }) { engine ->
                    val selected = engine.id == state.selectedEngineId
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        border = if (selected) {
                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                state.selectEngine(engine.id)
                                onDismiss()
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = engine.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = engine.license.substringBefore(" ("),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Calibration: ${engine.calibrationBasis}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                lineHeight = 15.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = engine.opticalSummary,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Port notes: ${engine.portingNotes}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
