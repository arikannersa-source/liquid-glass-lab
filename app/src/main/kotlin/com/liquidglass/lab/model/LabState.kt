package com.liquidglass.lab.model

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.liquidglass.sdk.core.GlassAppearance
import com.liquidglass.sdk.core.GlassEngineId
import com.liquidglass.sdk.core.GlassEngineRegistry
import com.liquidglass.sdk.core.GlassParameters
import com.liquidglass.sdk.core.GlassPreset
import com.liquidglass.sdk.core.GlassVariant
import com.liquidglass.sdk.core.LiquidGlassEngine
import org.json.JSONArray

/**
 * Complete reactive state container for Liquid Glass Lab.
 *
 * Switching engines preserves identical background, components, dimensions, and lighting.
 * Supports instant reset-to-original for every developer's upstream defaults and persistent
 * JSON preset storage/import/export.
 */
@Stable
class LabState(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("liquid_glass_lab_presets", Context.MODE_PRIVATE)

    var selectedEngineId by mutableStateOf(GlassEngineId.SHAYANN_IOS27)
    var compareEngineId by mutableStateOf(GlassEngineId.FROSTED_BASELINE)
    var compareVerticalSplit by mutableStateOf(false)
    var compareUseNativeDefaults by mutableStateOf(true)

    var parameters by mutableStateOf(
        GlassEngineRegistry.get(GlassEngineId.SHAYANN_IOS27)
            .defaultParameters(GlassVariant.REGULAR, GlassAppearance.ADAPTIVE)
    )

    var viewMode by mutableStateOf(LabViewMode.FULL_SHOWCASE)
    var backgroundMode by mutableStateOf(BackgroundMode.PHOTO)
    var photoIndex by mutableIntStateOf(0)
    var solidColorIndex by mutableIntStateOf(0)
    var checkerScaleIndex by mutableIntStateOf(1)
    var textWallDark by mutableStateOf(false)

    var hudCollapsed by mutableStateOf(false)
    var showPerfOverlay by mutableStateOf(true)
    var tiltLightingEnabled by mutableStateOf(false)
    var useIdenticalParamsOnSwitch by mutableStateOf(false)

    var userPresets by mutableStateOf(loadUserPresets())
        private set

    var statusBanner by mutableStateOf<String?>(null)

    val activeEngine: LiquidGlassEngine
        get() = GlassEngineRegistry.get(selectedEngineId)

    val compareEngine: LiquidGlassEngine
        get() = GlassEngineRegistry.get(compareEngineId)

    val backdropIsDark: Boolean
        get() = when (backgroundMode) {
            BackgroundMode.SOLID_COLOR -> LabSolidSwatches[solidColorIndex % LabSolidSwatches.size].isDark
            BackgroundMode.TEXT_WALL -> textWallDark
            else -> backgroundMode.isDarkDefault
        }

    fun selectEngine(id: GlassEngineId) {
        if (selectedEngineId == id) return
        selectedEngineId = id
        if (!useIdenticalParamsOnSwitch) {
            // Load the developer's intended upstream defaults while preserving variant, appearance, and lighting angle!
            val devDefaults = GlassEngineRegistry.get(id)
                .defaultParameters(parameters.variant, parameters.appearance)
            parameters = devDefaults.copy(
                lightAngleDegrees = parameters.lightAngleDegrees,
                cornerRadiusDp = parameters.cornerRadiusDp,
                animationSpeed = parameters.animationSpeed,
                morphSmoothingDp = parameters.morphSmoothingDp
            )
        }
    }

    fun setVariant(variant: GlassVariant) {
        val devDefaults = activeEngine.defaultParameters(variant, parameters.appearance)
        parameters = devDefaults.copy(
            lightAngleDegrees = parameters.lightAngleDegrees,
            cornerRadiusDp = parameters.cornerRadiusDp,
            animationSpeed = parameters.animationSpeed,
            morphSmoothingDp = parameters.morphSmoothingDp
        )
    }

    fun setAppearance(appearance: GlassAppearance) {
        val devDefaults = activeEngine.defaultParameters(parameters.variant, appearance)
        parameters = parameters.copy(
            appearance = appearance,
            tintColorArgb = devDefaults.tintColorArgb
        )
    }

    /** Restores exact developer-intended upstream optical defaults for the active engine. */
    fun resetToEngineOriginal() {
        parameters = activeEngine.defaultParameters(parameters.variant, parameters.appearance)
        statusBanner = "Reset to ${activeEngine.shortName} developer defaults"
    }

    fun updateLightFromGravity(gx: Float, gy: Float) {
        if (!tiltLightingEnabled) return
        val angleDeg = Math.toDegrees(kotlin.math.atan2(gy.toDouble(), -gx.toDouble())).toFloat()
        val normalized = ((angleDeg % 360f) + 360f) % 360f
        parameters = parameters.copy(lightAngleDegrees = normalized)
    }

    fun saveCustomPreset(name: String, description: String = ""): GlassPreset {
        val preset = GlassPreset(
            name = name.trim().ifBlank { "Custom ${userPresets.size + 1}" },
            description = description.trim().ifBlank { "Saved on ${activeEngine.shortName}" },
            engineKey = selectedEngineId.key,
            parameters = parameters.sanitized(),
            builtIn = false
        )
        val updated = listOf(preset) + userPresets.filterNot { it.name == preset.name }
        userPresets = updated
        persistUserPresets(updated)
        statusBanner = "Saved preset \"${preset.name}\""
        return preset
    }

    fun deleteUserPreset(name: String) {
        val updated = userPresets.filterNot { it.name == name }
        userPresets = updated
        persistUserPresets(updated)
        statusBanner = "Deleted preset \"$name\""
    }

    fun applyPreset(preset: GlassPreset) {
        val engineId = GlassEngineId.fromKey(preset.engineKey) ?: selectedEngineId
        selectedEngineId = engineId
        parameters = preset.parameters.sanitized()
        statusBanner = "Applied \"${preset.name}\""
    }

    fun importPresetJson(json: String): Result<GlassPreset> {
        return GlassPreset.fromJson(json).onSuccess { imported ->
            val updated = listOf(imported) + userPresets.filterNot { it.name == imported.name }
            userPresets = updated
            persistUserPresets(updated)
            applyPreset(imported)
            statusBanner = "Imported \"${imported.name}\""
        }.onFailure {
            statusBanner = "Import failed: ${it.message ?: "Invalid JSON"}"
        }
    }

    fun exportCurrentAsPreset(name: String = "${activeEngine.shortName} Custom"): GlassPreset =
        GlassPreset(
            name = name,
            description = "Exported from Liquid Glass Lab (${activeEngine.displayName})",
            engineKey = selectedEngineId.key,
            parameters = parameters.sanitized(),
            builtIn = false
        )

    private fun loadUserPresets(): List<GlassPreset> {
        val raw = prefs.getString(KEY_USER_PRESETS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    GlassPreset.fromJson(arr.getString(i)).getOrNull()?.let { add(it) }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun persistUserPresets(list: List<GlassPreset>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson(prettyIndent = 0)) }
        prefs.edit().putString(KEY_USER_PRESETS, arr.toString()).apply()
    }

    companion object {
        private const val KEY_USER_PRESETS = "user_presets_v1"
    }
}
