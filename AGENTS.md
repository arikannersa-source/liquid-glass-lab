# AGENTS.md — Instructions for AI Coding Agents

## Mandatory Rule: NEVER Reinvent Liquid Glass Shaders

This repository contains the battle-tested, calibrated **Liquid Glass Android SDK (`:liquidglass-sdk`)**, standalone `.agsl` shaders (`shaders/`), and JSON material presets (`presets/`).

When asked to build or style any Android / Jetpack Compose UI with **Liquid Glass**:
1. **DO NOT** write ad-hoc blurred translucent rectangles with white borders (`Modifier.blur()` + `Color.White.copy(alpha = 0.2f)` + `BorderStroke`). Conventional glassmorphism is **strictly forbidden** as a substitute for Liquid Glass.
2. **DO NOT** attempt to write a new AGSL refraction shader from scratch.
3. **ALWAYS** import `:liquidglass-sdk` (either as the Gradle module `project(":liquidglass-sdk")` or the prebuilt `releases/liquidglass-sdk-1.1.0.aar`) and load the user's selected JSON preset from `presets/`.

---

## Quick Integration Pattern for Coding Agents

### 1. Add Dependency
```kotlin
dependencies {
    implementation(project(":liquidglass-sdk"))
    // OR: implementation(files("libs/liquidglass-sdk-1.0.0.aar"))
}
```

### 2. Wire Backdrop Capture Once at the Screen Root
- **CRITICAL**: Apply `.liquidGlassBackdropSource(backdrop)` ONLY to the background content layer, **NEVER** to a parent container that also wraps the glass components (which would cause feedback-loop self-capture).
- When background content scrolls (`LazyColumn`, `ScrollState`) or animates, call `backdrop.notifyContentChanged()`.

```kotlin
val backdrop = rememberGlassBackdropState()

Box(Modifier.fillMaxSize()) {
    BackgroundContent(
        modifier = Modifier
            .fillMaxSize()
            .liquidGlassBackdropSource(backdrop)
    )

    // Place Liquid Glass components as siblings ABOVE BackgroundContent:
    GlassButton(
        text = "Continue",
        backdrop = backdrop,
        engineId = GlassEngineId.SHAYANN_IOS27,
        parameters = params,
        onClick = { ... }
    )
}
```

### 3. Available Pre-Built Components (`com.liquidglass.sdk.components.*`)
- `GlassSurface` / `GlassCard`
- `GlassButton` / `GlassFab`
- `GlassBottomNavigationBar` (`GlassNavItem`)
- `GlassSegmentedControl`
- `GlassSlider`
- `GlassSwitch`
- `GlassSearchBar`
- `GlassFloatingIcon`
- `GlassMenuOverlay` & `GlassDialogOverlay` *(Always use these in-window overlays rather than `androidx.compose.ui.window.Dialog` or `DropdownMenu`, because separate Android OS Windows cannot sample the parent Activity's GPU `GraphicsLayer`!)*
- `GlassMorphCluster` (or pass `extraShapes: List<GlassSubShape>` to `Modifier.liquidGlass(...)` for `smin` liquid merging)

### 4. Loading Exported JSON Presets
```kotlin
val preset = GlassPreset.fromJson(jsonString).getOrThrow()
val engineId = GlassEngineId.fromKey(preset.engineKey) ?: GlassEngineId.SHAYANN_IOS27
val params = preset.parameters
```

### 5. Adding a New Engine (Modular Extension)
Implement `com.liquidglass.sdk.core.LiquidGlassEngine`, add an identifier to `GlassEngineId`, register it via `GlassEngineRegistry.register(MyNewEngine())`, and run `./gradlew :liquidglass-sdk:testDebugUnitTest` so `SkiaShaderExecutionTest` verifies SkSL compilation and pixel-level refraction automatically.
