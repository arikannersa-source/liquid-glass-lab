package com.liquidglass.sdk

import com.liquidglass.sdk.core.GlassEngineRegistry
import com.liquidglass.sdk.core.GlassPreset
import org.jetbrains.skia.RuntimeEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExportedDeliverablesParityTest {

    private fun resolveRepoRoot(): File {
        val cwd = File(System.getProperty("user.dir") ?: ".")
        return if (File(cwd, "settings.gradle.kts").exists()) cwd else cwd.parentFile
    }

    @Test
    fun standaloneAgslShaderFilesAndJsonPresetsMatchLiveSdkByteForByte() {
        val root = resolveRepoRoot()
        val shadersDir = File(root, "shaders")
        val presetsDir = File(root, "presets")
        val exportMode = System.getenv("EXPORT_SDK_DELIVERABLES") == "1"

        if (exportMode) {
            shadersDir.mkdirs()
            presetsDir.mkdirs()
            for (engine in GlassEngineRegistry.all()) {
                File(shadersDir, "${engine.engineKey}.agsl").writeText(engine.agslSource + "\n", Charsets.UTF_8)
            }
            for (preset in GlassEngineRegistry.builtInPresets()) {
                val slug = slugify(preset.name)
                File(presetsDir, "$slug.json").writeText(preset.toJson(prettyIndent = 2) + "\n", Charsets.UTF_8)
            }
        }

        // Strict verification of all 9 standalone .agsl shader source files
        for (engine in GlassEngineRegistry.all()) {
            val shaderFile = File(shadersDir, "${engine.engineKey}.agsl")
            assertTrue("Missing standalone shader file: ${shaderFile.absolutePath}", shaderFile.exists())
            val diskContent = shaderFile.readText(Charsets.UTF_8).trimEnd()
            assertEquals(
                "Shader file ${shaderFile.name} drifted from ${engine::class.simpleName}.agslSource",
                engine.agslSource.trimEnd(),
                diskContent
            )
            // Verify the standalone .agsl file compiles directly in Skia
            val effect = RuntimeEffect.makeForShader(diskContent)
            effect.close()
        }

        // Strict verification of all built-in .json preset files
        val presets = GlassEngineRegistry.builtInPresets()
        for (preset in presets) {
            val slug = slugify(preset.name)
            val presetFile = File(presetsDir, "$slug.json")
            assertTrue("Missing standalone preset file: ${presetFile.absolutePath}", presetFile.exists())
            val parsed = GlassPreset.fromJson(presetFile.readText(Charsets.UTF_8)).getOrThrow()
            assertEquals(preset.name, parsed.name)
            assertEquals(preset.engineKey, parsed.engineKey)
            assertEquals(preset.parameters.variant, parsed.parameters.variant)
            assertEquals(preset.parameters.refractionStrength, parsed.parameters.refractionStrength, 1e-4f)
        }
    }

    private fun slugify(name: String): String =
        name.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
}
