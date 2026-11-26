package com.liquidglass.sdk.core

import androidx.compose.runtime.Immutable
import org.json.JSONObject

/**
 * Portable, exportable JSON material preset capturing engine selection + all optical parameters.
 *
 * Uses Android's built-in `org.json` to keep the reusable SDK AAR free of third-party
 * serialization dependencies.
 */
@Immutable
data class GlassPreset(
    val name: String,
    val description: String = "",
    val engineKey: String = GlassEngineId.SHAYANN_IOS27.key,
    val parameters: GlassParameters = GlassParameters(),
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val builtIn: Boolean = false
) {
    fun toJson(prettyIndent: Int = 2): String {
        val root = JSONObject()
        root.put("schemaVersion", schemaVersion)
        root.put("name", name)
        root.put("description", description)
        root.put("engine", engineKey)

        val p = parameters.sanitized()
        val params = JSONObject().apply {
            put("variant", p.variant.name)
            put("appearance", p.appearance.name)
            put("refractionStrength", p.refractionStrength.toDouble())
            put("glassThicknessDp", p.glassThicknessDp.toDouble())
            put("edgeCurvature", p.edgeCurvature.toDouble())
            put("lensDistortion", p.lensDistortion.toDouble())
            put("blurRadiusDp", p.blurRadiusDp.toDouble())
            put("transparency", p.transparency.toDouble())
            put("tintColorArgb", String.format("#%08X", p.tintColorArgb and 0xFFFFFFFFL))
            put("tintOpacity", p.tintOpacity.toDouble())
            put("chromaticAberration", p.chromaticAberration.toDouble())
            put("specularIntensity", p.specularIntensity.toDouble())
            put("lightAngleDegrees", p.lightAngleDegrees.toDouble())
            put("edgeIllumination", p.edgeIllumination.toDouble())
            put("shadowIntensity", p.shadowIntensity.toDouble())
            put("cornerRadiusDp", p.cornerRadiusDp.toDouble())
            put("touchDeformation", p.touchDeformation.toDouble())
            put("animationSpeed", p.animationSpeed.toDouble())
            put("morphSmoothingDp", p.morphSmoothingDp.toDouble())
            put("surfaceProfile", p.surfaceProfile)
            if (p.outwardSamplingOverride != null) {
                put("outwardSamplingOverride", p.outwardSamplingOverride)
            }
        }
        root.put("parameters", params)
        return if (prettyIndent > 0) root.toString(prettyIndent) else root.toString()
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1

        fun fromJson(json: String): Result<GlassPreset> = runCatching {
            val root = JSONObject(json)
            val name = root.optString("name", "Imported Preset").ifBlank { "Imported Preset" }
            val description = root.optString("description", "")
            val engineKey = root.optString("engine", GlassEngineId.SHAYANN_IOS27.key)
            val schemaVersion = root.optInt("schemaVersion", CURRENT_SCHEMA_VERSION)
            val pObj = root.optJSONObject("parameters") ?: JSONObject()

            val variant = runCatching {
                GlassVariant.valueOf(pObj.optString("variant", GlassVariant.REGULAR.name).uppercase())
            }.getOrDefault(GlassVariant.REGULAR)

            val appearance = runCatching {
                GlassAppearance.valueOf(pObj.optString("appearance", GlassAppearance.ADAPTIVE.name).uppercase())
            }.getOrDefault(GlassAppearance.ADAPTIVE)

            val tintRaw: Any? = pObj.opt("tintColorArgb")
            val tintArgb = when (tintRaw) {
                null -> 0xFFFFFFFFL
                is Number -> tintRaw.toLong() and 0xFFFFFFFFL
                is String -> parseHexArgb(tintRaw)
                else -> 0xFFFFFFFFL
            }

            val outwardOverride = if (pObj.has("outwardSamplingOverride") && !pObj.isNull("outwardSamplingOverride")) {
                pObj.optBoolean("outwardSamplingOverride")
            } else {
                null
            }

            val params = GlassParameters(
                variant = variant,
                appearance = appearance,
                refractionStrength = pObj.optDouble("refractionStrength", 1.0).toFloat(),
                glassThicknessDp = pObj.optDouble("glassThicknessDp", 24.0).toFloat(),
                edgeCurvature = pObj.optDouble("edgeCurvature", 1.0).toFloat(),
                lensDistortion = pObj.optDouble("lensDistortion", 0.25).toFloat(),
                blurRadiusDp = pObj.optDouble("blurRadiusDp", 12.0).toFloat(),
                transparency = pObj.optDouble("transparency", 0.65).toFloat(),
                tintColorArgb = tintArgb,
                tintOpacity = pObj.optDouble("tintOpacity", 0.18).toFloat(),
                chromaticAberration = pObj.optDouble("chromaticAberration", 0.35).toFloat(),
                specularIntensity = pObj.optDouble("specularIntensity", 1.0).toFloat(),
                lightAngleDegrees = pObj.optDouble("lightAngleDegrees", 225.0).toFloat(),
                edgeIllumination = pObj.optDouble("edgeIllumination", 1.0).toFloat(),
                shadowIntensity = pObj.optDouble("shadowIntensity", 0.85).toFloat(),
                cornerRadiusDp = pObj.optDouble("cornerRadiusDp", 26.0).toFloat(),
                touchDeformation = pObj.optDouble("touchDeformation", 1.0).toFloat(),
                animationSpeed = pObj.optDouble("animationSpeed", 1.0).toFloat(),
                morphSmoothingDp = pObj.optDouble("morphSmoothingDp", 28.0).toFloat(),
                surfaceProfile = pObj.optInt("surfaceProfile", 1),
                outwardSamplingOverride = outwardOverride
            ).sanitized()

            GlassPreset(
                name = name,
                description = description,
                engineKey = engineKey,
                parameters = params,
                schemaVersion = schemaVersion,
                builtIn = false
            )
        }

        private fun parseHexArgb(raw: String): Long {
            val cleaned = raw.trim().removePrefix("#").removePrefix("0x").removePrefix("0X")
            return when (cleaned.length) {
                6 -> (0xFF000000L or cleaned.toLong(16)) and 0xFFFFFFFFL
                8 -> cleaned.toLong(16) and 0xFFFFFFFFL
                else -> 0xFFFFFFFFL
            }
        }
    }
}
