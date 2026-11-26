# `:liquidglass-sdk` Integration Guide

`:liquidglass-sdk` is completely decoupled from `Liquid Glass Lab` (`:app`) and has **zero Material 3 or application-layer dependencies**—only Jetpack Compose `runtime`, `ui`, `ui-graphics`, and `foundation` (`minSdk = 33`, `compileSdk = 37`).

---

## Option A: Drop-in Compiled AAR (`releases/liquidglass-sdk-1.1.0.aar`)

Copy `releases/liquidglass-sdk-1.1.0.aar` into your Android project's `app/libs/` folder and add:

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(files("libs/liquidglass-sdk-1.1.0.aar"))
    implementation("androidx.compose.ui:ui:1.12.1")
    implementation("androidx.compose.ui:ui-graphics:1.12.1")
    implementation("androidx.compose.foundation:foundation:1.12.1")
}
```

## Option B: Gradle Source Module (`:liquidglass-sdk`)

Copy the `liquidglass-sdk/` directory into your project and include it in `settings.gradle.kts`:

```kotlin
include(":liquidglass-sdk")
```
```kotlin
dependencies {
    implementation(project(":liquidglass-sdk"))
}
```

---

## 3-Step Jetpack Compose Usage

### Step 1: Capture the Live Backdrop
Wrap your screen's background content (photos, `LazyColumn`, maps, video/Canvas, gradients) with `liquidGlassBackdropSource(backdrop)`. Glass elements placed *sibling-above* this modifier refract it in real time at 120 FPS with zero bitmap readback:

```kotlin
val backdrop = rememberGlassBackdropState()

Box(Modifier.fillMaxSize()) {
    // 1. Background layer (never recursively captures the glass above it!)
    MyScrollingContent(
        modifier = Modifier
            .fillMaxSize()
            .liquidGlassBackdropSource(backdrop)
    )

    // 2. Load an exported JSON preset (or use engine defaults)
    val preset = remember {
        GlassPreset.fromJson(presetJsonString).getOrNull()
    }
    val engineId = GlassEngineId.fromKey(preset?.engineKey) ?: GlassEngineId.SHAYANN_IOS27
    val params = preset?.parameters
        ?: GlassEngineRegistry.get(engineId).defaultParameters()

    // 3. Drop in reusable Liquid Glass components or Modifier.liquidGlass(...)
    GlassBottomNavigationBar(
        items = listOf(
            GlassNavItem("Home") { Icon(Icons.Rounded.Home, null) },
            GlassNavItem("Search") { Icon(Icons.Rounded.Search, null) },
            GlassNavItem("Library") { Icon(Icons.Rounded.AutoAwesome, null) }
        ),
        selectedIndex = selectedTab,
        onSelect = { selectedTab = it },
        backdrop = backdrop,
        engineId = engineId,
        parameters = params,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    )
}
```

### Step 2: Apply `.liquidGlass(...)` to Any Custom Composable
```kotlin
Box(
    modifier = Modifier
        .size(width = 220.dp, height = 64.dp)
        .liquidGlass(
            backdrop = backdrop,
            engineId = GlassEngineId.SHAYANN_IOS27,
            parameters = params,
            cornerRadiusOverrideDp = 32.dp
        ),
    contentAlignment = Alignment.Center
) {
    Text("Custom Refractive Pill", color = Color.White)
}
```

### Step 3: Notify Backdrop on Custom Scroll / Frame Animations
Static state changes automatically invalidate `GraphicsLayer`, and animated Canvas draws automatically update when calling `backdrop.notifyContentChanged()`:
```kotlin
LaunchedEffect(lazyListState) {
    snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
        .collect { backdrop.notifyContentChanged() }
}
```

---

## Extracting a Single Permanent Engine After Evaluation

Once you select your preferred engine on your Pixel 11 Pro in Phase 6:
1. Keep only your chosen engine implementation in `com.liquidglass.sdk.shaders.<ChosenEngine>.kt` (plus `CommonAgsl.kt`).
2. Set it as the sole entry in `GlassEngineRegistry` and ship your exported `.json` preset from `presets/` as the default `GlassParameters`.
3. Every reusable component (`GlassButton`, `GlassFab`, `GlassBottomNavigationBar`, `GlassSegmentedControl`, `GlassSlider`, `GlassSwitch`, `GlassSearchBar`, `GlassCard`, `GlassMenuOverlay`, `GlassDialogOverlay`, `GlassFloatingIcon`, `GlassMorphCluster`) continues to work with zero API changes.
