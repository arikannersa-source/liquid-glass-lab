package com.liquidglass.lab.controls

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.lab.model.LabState
import com.liquidglass.sdk.core.GlassEngineRegistry
import com.liquidglass.sdk.core.GlassPreset

/**
 * Material Preset Manager supporting:
 * - Saving & naming custom material presets locally
 * - Exporting presets as `.json` files via Storage Access Framework (`CreateDocument`)
 * - Importing `.json` files via Storage Access Framework (`OpenDocument`) or raw JSON paste
 * - Sharing / copying portable JSON for immediate SDK reuse (`GlassPreset.fromJson`)
 * - Applying any built-in upstream-calibrated preset
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetManagerSheet(
    state: LabState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var presetName by remember { mutableStateOf("${state.activeEngine.shortName} Custom") }
    var presetNotes by remember { mutableStateOf("") }
    var showPasteImportDialog by remember { mutableStateOf(false) }
    var pasteBuffer by remember { mutableStateOf("") }
    var showRawJsonPreview by remember { mutableStateOf(false) }
    var pendingExportJson by remember { mutableStateOf("") }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && pendingExportJson.isNotBlank()) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(pendingExportJson.toByteArray(Charsets.UTF_8))
                }
                state.statusBanner = "Exported JSON preset to file"
            }.onFailure {
                state.statusBanner = "Export failed: ${it.localizedMessage}"
            }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.readText()
                    ?: error("Unable to read selected file")
                state.importPresetJson(text)
            }.onFailure {
                state.statusBanner = "File read error: ${it.localizedMessage}"
            }
        }
    }

    if (showPasteImportDialog) {
        AlertDialog(
            onDismissRequest = { showPasteImportDialog = false },
            title = { Text("Import Preset JSON") },
            text = {
                OutlinedTextField(
                    value = pasteBuffer,
                    onValueChange = { pasteBuffer = it },
                    label = { Text("Paste Liquid Glass JSON preset") },
                    minLines = 6,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        state.importPresetJson(pasteBuffer)
                        showPasteImportDialog = false
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = Color.Transparent,
        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.97f)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.68f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "Material Presets & JSON SDK Export",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Save, name, import, or export portable JSON presets compatible with GlassPreset.fromJson() in :liquidglass-sdk.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Save & Export current configuration
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = presetName,
                            onValueChange = { presetName = it },
                            label = { Text("Preset Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = presetNotes,
                            onValueChange = { presetNotes = it },
                            label = { Text("Optional Optical Notes") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { state.saveCustomPreset(presetName, presetNotes) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Save", fontSize = 12.sp)
                            }
                            FilledTonalButton(
                                onClick = {
                                    val preset = state.exportCurrentAsPreset(presetName)
                                    pendingExportJson = preset.toJson(prettyIndent = 2)
                                    val safeFile = preset.name.lowercase()
                                        .replace(Regex("[^a-z0-9]+"), "_")
                                        .trim('_')
                                        .ifBlank { "liquid_glass_preset" }
                                    exportFileLauncher.launch("$safeFile.json")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Export .json", fontSize = 12.sp)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val json = state.exportCurrentAsPreset(presetName).toJson(prettyIndent = 2)
                                    copyToClipboard(context, presetName, json)
                                    state.statusBanner = "Copied preset JSON to clipboard"
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("Copy JSON", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    val json = state.exportCurrentAsPreset(presetName).toJson(prettyIndent = 2)
                                    shareJsonText(context, presetName, json)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("Share", fontSize = 11.sp)
                            }
                        }
                        TextButton(onClick = { showRawJsonPreview = !showRawJsonPreview }) {
                            Text(if (showRawJsonPreview) "Hide Live JSON Schema" else "Preview Live JSON Schema")
                        }
                        if (showRawJsonPreview) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0C0F16)
                            ) {
                                Text(
                                    text = state.exportCurrentAsPreset(presetName).toJson(prettyIndent = 2),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color(0xFF9CDCFE),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Import buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { importFileLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Import .json File", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { showPasteImportDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Paste JSON", fontSize = 12.sp)
                    }
                }
            }

            // User-saved presets
            if (state.userPresets.isNotEmpty()) {
                item {
                    HorizontalDivider()
                    Text(
                        text = "Saved Custom Presets (${state.userPresets.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(state.userPresets, key = { "user_${it.name}" }) { preset ->
                    PresetCard(
                        preset = preset,
                        onApply = { state.applyPreset(preset) },
                        onExport = {
                            pendingExportJson = preset.toJson(prettyIndent = 2)
                            val safe = preset.name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                            exportFileLauncher.launch("${safe.ifBlank { "preset" }}.json")
                        },
                        onDelete = { state.deleteUserPreset(preset.name) }
                    )
                }
            }

            // Built-in calibrated presets
            item {
                HorizontalDivider()
                Text(
                    text = "Built-in Calibrated Presets",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(GlassEngineRegistry.builtInPresets(), key = { "builtin_${it.name}" }) { preset ->
                PresetCard(
                    preset = preset,
                    onApply = { state.applyPreset(preset) },
                    onExport = {
                        pendingExportJson = preset.toJson(prettyIndent = 2)
                        val safe = preset.name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                        exportFileLauncher.launch("$safe.json")
                    },
                    onDelete = null
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun PresetCard(
    preset: GlassPreset,
    onApply: () -> Unit,
    onExport: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onApply)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                if (preset.description.isNotBlank()) {
                    Text(
                        text = preset.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
                Text(
                    text = "Engine: ${preset.engineKey} • ${preset.parameters.variant.name}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onExport) {
                Icon(Icons.Rounded.FileDownload, contentDescription = "Export JSON")
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete preset")
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, json: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText(label, json))
}

private fun shareJsonText(context: Context, title: String, json: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_SUBJECT, "$title.json")
        putExtra(Intent.EXTRA_TEXT, json)
    }
    context.startActivity(Intent.createChooser(intent, "Share Liquid Glass Preset"))
}
