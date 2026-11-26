---
name: android-liquid-glass-sdk
description: Integrate calibrated Apple-grade Liquid Glass optical refraction, chromatic dispersion, specular glints, elastic touch deformation, and liquid morphing on Android (API 33–37) using the reusable :liquidglass-sdk library and JSON presets instead of generating approximations or flat glassmorphism.
---

# Android Liquid Glass SDK Skill

Use this skill whenever building an Android / Jetpack Compose interface requiring **Liquid Glass**. Never generate a blurred translucent box with a static border (`Modifier.blur` + `BorderStroke`), and never write ad-hoc AGSL shaders from scratch when `:liquidglass-sdk` is available.

## Core Architecture Rules

1. **Minimum & Target SDK**: Requires `minSdk >= 33` (Android 13 AGSL `RuntimeShader` + `RenderEffect`), verified through `compileSdk = 37` / `targetSdk = 37` (Android 17, 120Hz LTPO OLED).
2. **Sibling Backdrop Topology**:
   - Create `val backdrop = rememberGlassBackdropState()`.
   - Attach `.liquidGlassBackdropSource(backdrop)` **only** to the background composable subtree.
   - Render all Liquid Glass components as **siblings above** that background subtree inside a `Box`.
   - Call `backdrop.notifyContentChanged()` whenever a `LazyListState`, `ScrollState`, or custom `Canvas` animation advances.
3. **In-Window Menus & Dialogs**:
   - Never use `Popup` or `Dialog` windows for Liquid Glass overlays on Android—separate OS windows cannot read the host window's GPU `GraphicsLayer`. Use `GlassMenuOverlay` and `GlassDialogOverlay` inside the root `Box`.
4. **Regular vs. Clear Material Selection**:
   - **Regular (`GlassVariant.REGULAR`)**: Default for navigation bars, search bars, cards, menus, dialogs, and text-bearing controls. Compresses background luminance range and boosts perceptual chroma for effortless legibility.
   - **Clear (`GlassVariant.CLEAR`)**: Reserved for controls floating over rich media/photography where optical transparency is prioritized over diffuse frosting.

## Standard Integration Snippet

```kotlin
import com.liquidglass.sdk.components.*
import com.liquidglass.sdk.core.*
import com.liquidglass.sdk.modifier.liquidGlass

@Composable
fun LiquidGlassScaffold(presetJson: String) {
    val backdrop = rememberGlassBackdropState()
    val preset = remember(presetJson) { GlassPreset.fromJson(presetJson).getOrThrow() }
    val engineId = GlassEngineId.fromKey(preset.engineKey) ?: GlassEngineId.SHAYANN_IOS27
    val params = preset.parameters

    Box(Modifier.fillMaxSize()) {
        // 1. Captured background
        AppWallpaperOrFeed(
            modifier = Modifier
                .fillMaxSize()
                .liquidGlassBackdropSource(backdrop)
        )

        // 2. Refractive UI layer
        GlassSearchBar(
            query = "",
            onQueryChange = {},
            backdrop = backdrop,
            engineId = engineId,
            parameters = params,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp, start = 16.dp, end = 16.dp)
        )

        GlassBottomNavigationBar(
            items = listOf(GlassNavItem("Home"), GlassNavItem("Explore"), GlassNavItem("Profile")),
            selectedIndex = 0,
            onSelect = {},
            backdrop = backdrop,
            engineId = engineId,
            parameters = params,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        )
    }
}
```

## Available Engines (`GlassEngineId` — Ranked `#1` to `#9` by iOS Fidelity)
1. `SHAYANN_IOS27` (`shayann_ios27`): **#1 Closest to iOS 27** — lossless-capture calibrated outward refraction + Oklab transfer + dual-strip sharp rim.
2. `FLUTTER_SEAGLASS` (`flutter_seaglass`): **#2** — Reverse-engineered `CASDFElementLayer` / iOS 27 HDR glint recoloring + tangential rim filter + dark meniscus contour + bevel inner shadow.
3. `VITRO_IOS26` (`vitro_ios26`): **#3** — iPhone iOS 26 side-by-side calibrated `pale`/`deep` curves + vibrant rim recoloring.
4. `HAZE_GLASS` (`haze_glass`): **#4** — Chris Banes Haze 2.1 dome-blended distance + 4 surface profiles + fold envelope (`Spiralens` preset included).
5. `SOHUM_SNELL` (`sohum_snell`): **#5** — Exact BK7 (`n = 1.52`) 3D Snell slab + Fresnel transmittance + macOS 26 `0.58L + 34` transfer.
6. `MOCHI_G2` (`mochi_g2`): **#6** — Apple G2 continuous supercircle + dual outer/inner refraction + `erfp` ring shadow.
7. `KYANT_BACKDROP` (`kyant_backdrop`): **#7** — 7-band rainbow spectral dispersion + `circleMap` + `1.5x` corner smoothing.
8. `QWEA0_GRAVITY` (`qwea0_gravity`): **#8** — Inverse-power gravitational lens + inward/outward toggle + absorbing tint.
9. `FROSTED_BASELINE` (`frosted_baseline`): **#9** — Non-refractive blur + border baseline (for comparison only — never select for production Liquid Glass).

## Reference Files in Repository
- Compiled AAR: `releases/liquidglass-sdk-1.1.0.aar`
- Standalone AGSL Shaders: `shaders/*.agsl`
- Portable JSON Presets: `presets/*.json`
- Mathematical Spec (for non-Android ports): `docs/OPTICAL_SPECIFICATION.md`
