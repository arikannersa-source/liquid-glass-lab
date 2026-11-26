# Liquid Glass Lab & Reusable Android SDK (`:liquidglass-sdk`)

A native Android 17 (API 37) optical evaluation laboratory and standalone Jetpack Compose / AGSL library built to **evaluate, compare, customize, and permanently standardize** the most visually accurate recreation of Apple's Liquid Glass on Android (targeted for **Google Pixel 11 Pro**, 120Hz LTPO OLED).

---

## Installable Release Artifacts (`releases/`)

- **Installable Laboratory APK**: [`releases/LiquidGlassLab-v1.1.0-release.apk`](releases/LiquidGlassLab-v1.1.0-release.apk)  
  *(R8-optimized, resource-shrunk, signed for immediate sideloading or `adb install -r releases/LiquidGlassLab-v1.1.0-release.apk` on Pixel 11 Pro)*
- **Standalone Reusable SDK AAR**: [`releases/liquidglass-sdk-1.1.0.aar`](releases/liquidglass-sdk-1.1.0.aar)  
  *(Zero Material 3 or app-UI dependencies; ready to drop into any Android app)*

---

## Integrated Rendering Engines (Ranked `#1`–`#9` by Apple iOS Liquid Glass Fidelity)

Every engine is independently selectable in real time over **identical backgrounds, UI components, dimensions, and lighting**, tuned out-of-the-box to its closest achievable match to Apple's iOS 26/27 Liquid Glass:

| Rank | Engine Key | Upstream Source & License | Calibration & Optical Model |
| :- | :--- | :--- | :--- |
| **#1** | `shayann_ios27` | [`shayann07/liquidglass`](https://github.com/shayann07/liquidglass) (Apache-2.0) | **iOS 27 lossless-capture calibrated**: outward padded rim refraction (`n=1.50`), Apple G2 superellipse, sharp rim + dual-strip wide interior blur, Oklab perceptual transfer (`1.65x` chroma). |
| **#2** | `flutter_seaglass` | [`whynotmake-it/flutter_liquid_glass`](https://github.com/whynotmake-it/flutter_liquid_glass) (MIT) | **Reverse-engineered macOS 26 / iPhone 17 Pro iOS 27**: tangential box-filtered rim, peripheral dark meniscus contour, HDR glint recoloring, directional raised-rim inner shadow. |
| **#3** | `vitro_ios26` | [`itshedimisawi/vitro`](https://github.com/itshedimisawi/vitro) (Apache-2.0) | **iPhone iOS 26 side-by-side calibrated**: nonlinear `pale`/`deep` luminance transfer curves & vibrant chroma-amplifying rim recoloring. |
| **#4** | `haze_glass` | [`chrisbanes/haze`](https://github.com/chrisbanes/haze) 2.1 + [`xenitV1`](https://github.com/xenitV1/liquid-glass-skill) (Apache-2.0 / MIT) | **Dome-blended optical distance**, 4 selectable bevel profiles (`Circular`, `Squircle`, `Sinusoidal`, `Concave`), UV fold-prevention envelope, linear chroma boost, crisp rim detail pass. |
| **#5** | `sohum_snell` | [`sohumsuthar/liquid-glass`](https://github.com/sohumsuthar/liquid-glass) (MIT) | **macOS 26 Control Center calibrated**: exact 3D **BK7 (`n=1.52`) Snell's Law** meniscus slab + unpolarized Fresnel `(Rs+Rp)/2` + `0.58L + 34` transfer. |
| **#6** | `mochi_g2` | [`feice11/mochi-compose`](https://github.com/feice11/mochi-compose) (Apache-2.0) | **Apple G2 supercircle (`lnNorm`)**, opposing dual-zone outer/inner refraction, analytical `erfp` Gaussian ring shadow, key/fill rim bleed. |
| **#7** | `kyant_backdrop` | [`Kyant0/AndroidLiquidGlass`](https://github.com/Kyant0/AndroidLiquidGlass) (Apache-2.0) | **7-band spectral chromatic dispersion** (`R/O/Y/G/C/B/P`), `circleMap` quarter-circle lens, `1.5x` inflated corner smoothing, radial `depthEffect`. |
| **#8** | `qwea0_gravity` | [`QWEA0/Liquid-Glass-Android`](https://github.com/QWEA0/Liquid-Glass-Android) (MIT) | **Inverse-power gravitational lens** `(1+4t)^-p`, live inward/outward sampling toggle, 3-tap `rimSoft`, absorbing + scattering tint. |
| **#9** | `frosted_baseline` | Android `RenderEffect` Baseline (Apache-2.0) | **Conventional Frosted Glassmorphism** (uniform Gaussian blur + flat translucent fill + uniform `1dp` border, zero optical lensing) for direct A/B comparison. |

---

## Laboratory Capabilities

### 13 Interactive Liquid Glass Components
1. Buttons (`GlassButton`)
2. Floating Action Buttons (`GlassFab`)
3. Bottom Navigation Bars (`GlassBottomNavigationBar`)
4. Segmented Controls (`GlassSegmentedControl`)
5. Sliders with refractive liquid thumb (`GlassSlider`)
6. Switches with refractive pill thumb (`GlassSwitch`)
7. Search Bars (`GlassSearchBar`)
8. Cards (`GlassCard`)
9. In-window Refractive Menus (`GlassMenuOverlay`)
10. In-window Refractive Dialogs (`GlassDialogOverlay`)
11. Floating App Icons (`GlassFloatingIcon`)
12. Free-form Draggable Glass Lenses (`DraggablePlaygroundScreen`)
13. Merging & Morphing `smin` Liquid Droplets (`GlassMorphCluster` / `MorphBenchScreen`)

### 8 Calibration Backgrounds
- High-resolution 9:16 photographs (*Big Sur Sunset*, *Tokyo Neon Rain*, *Botanical Macro*)
- Multicolored mesh gradients
- Black-and-white checkerboards (`16dp`, `32dp`, `56dp` — ruthless chromatic dispersion & Snell test)
- Sub-pixel fine optical calibration grid
- High-contrast editorial typography (*Light Paper* & *Dark OLED*)
- 7 calibrated solid color swatches
- 120Hz animated flowing chromatic ribbons & orbs
- Live scrollable feed content

### 4 Laboratory Viewports & Live 120Hz Telemetry
- **Showcase**: Full interactive suite of all 13 components
- **Drag Lens**: 4 draggable optical shapes over any background
- **Morphing**: Dedicated `smin` liquid bridge & splitting droplet bench
- **Compare A/B**: Synchronized Top/Bottom or Left/Right split-screen comparison with lockstep dragging
- **Live FPS & Frame Time Badge**: Hardware `Choreographer` + `Window.OnFrameMetricsAvailableListener` reporting live FPS and GPU/CPU frame render duration (`ms`)

### 16 Live Customization Controls + JSON Presets
Live sliders (with un-dimmed upper viewport and honest per-engine capability disclosure) for Refraction Strength, Glass Thickness, Edge Curvature, Lens Distortion, Background Blur, Transparency, Tint (+ swatches), Chromatic Aberration, Specular Highlights, Lighting Direction (+ **Pixel Gravity Sensor** tilt-lighting mode), Edge Illumination, Shadows, Corner Radius, Touch Deformation, Animation Speed, and Morphing Behavior—plus **Regular & Clear** variants and full **Save / Name / Import / Export `.json` presets**.

---

## Repository Structure

```text
liquid-glass-lab/
├── liquidglass-sdk/          # Standalone reusable Android library module (zero M3/app deps)
├── app/                      # Liquid Glass Lab Material 3 Android 17 evaluation app
├── shaders/                  # Standalone verified .agsl shader source files (all 9 engines)
├── presets/                  # Exportable calibrated .json material presets
├── releases/                 # Compiled installable APK + standalone SDK AAR
├── examples/                 # Minimal Jetpack Compose SDK integration example
├── docs/
│   ├── RESEARCH_COMPARISON.md    # Phase 1 empirical code study, calibration & porting audit
│   ├── OPTICAL_SPECIFICATION.md  # Framework-independent math & physics specification
│   └── INTEGRATION.md            # Step-by-step AAR & Gradle integration guide
├── AGENTS.md                 # Mandatory rules for future AI coding agents
├── SKILL.md                  # Reusable AI agent skill definition
├── LICENSE                   # Apache License 2.0
└── THIRD_PARTY_LICENSES.md   # Upstream attributions & permissive license texts
```

---

## Building & Running Tests

```bash
# Run all unit tests + real Skia AGSL compilation & pixel-level optical verification
./gradlew test

# Assemble standalone AAR and signed release APK (compileSdk 37, targetSdk 37)
./gradlew :liquidglass-sdk:assembleRelease :app:assembleRelease
```
