# Phase 1 Deep Research & Engine Audit

This document records the empirical findings from inspecting actual shader source code, calibration methodology, and licenses across GitHub, shader repositories, reverse-engineering writeups (`CASDFElementLayer` / `CABackdropLayer`), and technical studies of Apple's iOS 26 / iOS 27 / macOS 26 Liquid Glass.

> **No Fabricated Parity Claims**: Because this build environment is a headless Linux container without physical iPhone 17 Pro or Pixel 11 Pro hardware attached, all shader execution tests (`SkiaShaderExecutionTest`) verify SkSL/AGSL compilation and quantitative pixel-level optical properties (edge refraction displacement, RGB chromatic dispersion over achromatic checkerboards, directional rim lighting, Regular vs Clear transfer curves, `smin` liquid merging bridges, and dual-strip wide blur). Final perceptual selection is intentionally deferred to **Phase 6** on your physical **Pixel 11 Pro (Android 17, 120Hz LTPO OLED)**.

---

## Canonical `#1`–`#9` Ranking by Fidelity to Apple iOS 26/27 Liquid Glass

| Rank | Engine | Key | Why It Ranks Here Against Genuine iOS Liquid Glass |
| :- | :--- | :--- | :--- |
| **#1** | **Shayann iOS 27** | `shayann_ios27` | Measured directly from lossless iOS 27 captures: **outward** convex rim sampling (`uPad`), sharp unblurred rim + dual-strip wide interior blur, Apple G2 superellipse + medial-axis dome, `n=1.50` Snell meniscus, Oklab perceptual highlights & chroma boost. |
| **#2** | **Flutter SeaGlass** | `flutter_seaglass` | Reverse-engineered from `CASDFElementLayer` / iPhone 17 Pro iOS 27: **HDR specular glint recoloring**, peripheral dark meniscus contour, directional raised-rim bevel inner shadow, 3-tap tangential rim filter, G2 superellipse. |
| **#3** | **Vitro iOS 26** | `vitro_ios26` | Calibrated side-by-side against a physical iPhone running iOS 26: nonlinear `pale`/`deep` shadow-lift & highlight-hold transfer curves and vibrant chroma-amplifying `1.25dp` hairline rim recoloring. |
| **#4** | **Haze 2.1 (`Spiralens`)** | `haze_glass` | Dual-strip sharp-rim detail pass + wide interior blur, dome-blended optical distance for wide bars, 4 bevel profiles (`Squircle` default), UV fold-prevention envelope (`0.90 * dist`), linear-light chroma preservation. |
| **#5** | **Sohum BK7 Snell** | `sohum_snell` | Exact 3D **BK7 crown-glass (`n=1.52`) Snell's Law** meniscus slab + unpolarized Fresnel `(Rs+Rp)/2` + macOS 26 Control Center measured `0.58L + 34` transfer curve. |
| **#6** | **Mochi G2** | `mochi_g2` | Apple G2 continuous supercircle (`lnNorm`), dual-strip sharp-rim/blurred-interior sampling, analytical `erfp` Gaussian ring shadow, opposing outer/inner dual-zone bevel. |
| **#7** | **Kyant0 Backdrop** | `kyant_backdrop` | Visually tuned rather than Apple-measured: pre-blurs before refraction and samples inward via `circleMap`, with dramatic 7-band rainbow corner dispersion (`R/O/Y/G/C/B/P`) and `1.5x` corner smoothing. |
| **#8** | **QWEA0 Gravity Lens** | `qwea0_gravity` | Heuristic inverse-power `(1+4t)^-p` falloff with pre-blur and `G1` circular corners; includes absorbing+scattering tint and inward/outward toggle. |
| **#9** | **Frosted Baseline** | `frosted_baseline` | Control baseline: flat Gaussian blur + uniform translucent fill + `1dp` border (zero optical refraction, zero magnification, zero dispersion, zero `smin` morphing). |

---

## 1. Investigation of Initial Candidates & Additional Discoveries

### A. Initial Candidates Specified in Request

| Repository | License | Actual Implementation Inspected | Empirical Calibration & Findings | Integrated Engine |
| :--- | :--- | :--- | :--- | :--- |
| **`shayann07/liquidglass`** | Apache-2.0 | Android AGSL (`GlassShaderSource.kt`, `GlassOklabSource.kt`, `GlassNormalSource.kt`) + `docs/research/measured-model.md` | **Measured against lossless iOS 27 captures**. Identified two critical errors in nearly all other Android clones: (1) Apple's bevel samples **outward** past the element silhouette into a padded region (`uPad`), bending background objects around the rim rather than pulling interior pixels outward; (2) Apple keeps the rim **unblurred/sharp** while blurring the interior, using Oklab perceptual space (`L = 0.25 + 0.36 L_in` light / `0.21 + 0.45 L_in` dark, `1.65x` chroma). | `ShayannIos27Engine` (`shayann_ios27`) |
| **`Kyant0/AndroidLiquidGlass`** | Apache-2.0 | Compose Multiplatform + AGSL (`Shaders.kt`, `LayerBackdrop.kt`, `SdfShader.kt`, `InteractiveHighlight.kt`) | Most widely starred Android library (4,800+ stars), but **visually tuned rather than measured against Apple captures**. Pre-blurs the entire backdrop via `RenderEffect.createBlurEffect` *before* refraction (which softens crisp rim detail) and samples **inward** (`-refractionAmount * grad`). Excels at **7-band spectral dispersion** (`Red/Orange/Yellow/Green/Cyan/Blue/Purple`), `1.5x` inflated corner normal smoothing, and damped spring touch elasticity. | `KyantBackdropEngine` (`kyant_backdrop`) |
| **`sohumsuthar/liquid-glass`** | MIT | Web React + SVG `feDisplacementMap` (`lib/glass-optics.mjs`, `PHYSICS.md`) | **Calibrated against macOS 26 Control Center** pixel luminance measurements (`L_glass = 0.58 * L + 34` with `1.72x` saturation boost) combined with exact 3D **BK7 crown-glass (`n = 1.52`, Abbe `V_d = 64.17`) Snell's Law** through a curved meniscus slab (`d = h_local * tan(theta_1 - theta_2)`) and unpolarized Fresnel transmittance `(Rs + Rp)/2`. Ported from CPU SVG lut generation to real-time per-pixel GPU AGSL. | `SohumSnellEngine` (`sohum_snell`) |
| **`xenitV1/liquid-glass-skill`** | MIT | Agent skill (`SKILL.md`, `references/materials.md`, `references/platforms.md`, `assets/refraction.js`) | Not a standalone Android shader engine; documents Apple HIG Regular vs Clear material rules and recommends **`chrisbanes/haze` (`haze-glass`)** on Android with a `Spiralens` squircle surface profile. Led directly to inspecting `chrisbanes/haze` 2.1 (`haze-glass`) and exposing `xenitV1`'s Spiralens preset in the Lab. | `HazeGlassEngine` (`haze_glass`) + `Haze 2.1 — Spiralens (xenitV1)` preset |

### B. Superior & Complementary Implementations Discovered Beyond Initial List

| Repository | License | Actual Implementation Inspected | Why It Merited Integration | Integrated Engine |
| :--- | :--- | :--- | :--- | :--- |
| **`itshedimisawi/vitro`** | Apache-2.0 | Native Android AGSL (`GlassShaders.kt`, `Optics.kt`, `GlassShade.kt`) | **Calibrated side-by-side against a physical iPhone running iOS 26**. Features extraordinarily accurate nonlinear light (`pale`) and dark (`deep`) luminance transfer curves (`DarkLift = 0.19*(1-L)^16`, `DarkShade = (L-0.33)^1.5`) and **vibrant chroma-amplifying rim recoloring** that brightens rims without washing saturated colors out to chalky white. | `VitroIos26Engine` (`vitro_ios26`) |
| **`chrisbanes/haze` (`haze-glass`)** | Apache-2.0 | Compose + AGSL (`GlassShaders.kt`, `2026-08-31-ios-27-liquid-glass.md`) | Production-hardened architecture introducing **dome-blended optical distance** (fixes flat-center look on wide pills/cards), **4 selectable bevel profiles** (`Circular`, `Squircle`, `Sinusoidal`, `Concave`), a **fold-prevention envelope** (`maxDisp = dist * 0.90`) preventing UV self-intersection creases, gamut-preserving linear-light chroma boost, and a sharp-rim detail pass. | `HazeGlassEngine` (`haze_glass`) |
| **`whynotmake-it/flutter_liquid_glass`** (`flutter_sea_glass`) | MIT | Flutter Impeller GLSL (`liquid_glass_final_render_core.glsl`, `MODEL_DESIGN.md`) | Explicitly **reverse-engineered from macOS 26 `CASDFElementLayer` / `CABackdropLayer` behavior and iPhone 17 Pro iOS 27 captures**. Captures three subtle Apple optical cues missing elsewhere: (1) **tangential 3-tap box-filtered rim sampling**, (2) **peripheral dark refraction contour** inside the meniscus, and (3) **HDR specular glint recoloring** (`luma -> specular`, boosting saturation of bright refracted highlights) plus a **directional raised-rim bevel inner shadow**. | `FlutterSeaGlassEngine` (`flutter_seaglass`) |
| **`feice11/mochi-compose`** | Apache-2.0 | Android AGSL (`LiquidShaders.kt`) | Implements **Apple's G2 continuous-curvature supercircle (`lnNorm`)** corner geometry, **opposing dual-zone refraction** (outer inward compression + inner subtle expansion), analytical **`erfp` error-function Gaussian ring drop shadow**, and key/fill rim bleed. | `MochiG2Engine` (`mochi_g2`) |
| **`QWEA0/Liquid-Glass-Android`** | MIT | Android AGSL (`GlassLensRenderer.kt`, `GlassRuntimeEffects.kt`) | Implements **inverse-power gravitational lens decay** `(1 + 4t)^-p`, live **inward/outward direction toggle**, 3-tap tangential `rimSoft` anti-aliasing, Beer-Lambert-like **absorbing + scattering tinted glass**, and `pow(4.5)` dual-lobe specular highlights. | `Qwea0GravityLensEngine` (`qwea0_gravity`) |

### C. Additional Projects Audited & Excluded (With Reasons)

- **`Mahmud0808/GlassLayer`**: Licensed under **GPL-3.0** (viral copyleft). Excluded so `:liquidglass-sdk` remains 100% Apache-2.0 / MIT compatible for proprietary or commercial Android apps.
- **`DnV1eX/LiquidGlassKit`** & **`DevBehindYou/LiquidLens`**: Missing standard OSI open-source licenses (`NOASSERTION` / custom). Excluded from code borrowing.
- **`PrismalAGSL`** (MIT) & **`Abdullajon1881/LiquidGlass`** (Apache-2.0): Direct forks/ports of `Kyant0/AndroidLiquidGlass`'s `circleMap` + 7-band dispersion shader; already represented faithfully by `KyantBackdropEngine`.
- **`skydoves/Cloudy`** (Apache-2.0): Primarily a blur library with a basic inward rounded-rect lens (`LiquidGlassShaderSource.kt`); superseded optically by `Shayann`, `Haze 2.1`, and `Vitro`.
- **`iyinchao/liquid-glass-studio`** (MIT, WebGL2/WGSL): Browser playground whose Oklab/LCH perceptual highlight concept is already incorporated in `ShayannIos27Engine`.

---

## 2. Five Critical Optical Differentiators Uncovered

1. **Outward vs. Inward Rim Sampling**:
   - In real convex glass (and Apple's iOS 26/27 `CABackdropLayer`), rays near the curved perimeter bend **outward** past the widget's footprint, pulling surrounding background content into the bevel.
   - `Kyant0`, `Vitro`, and most Android demos sample **inward** because standard Android `RenderEffect` clips to view bounds unless padded.
   - `:liquidglass-sdk` solves this cleanly by recording a padded region (`uPad`) inside an offscreen compositing wrapper (`LiquidGlassModifier.kt`) so `ShayannIos27Engine` (and `Qwea0GravityLensEngine` when toggled to Outward) genuinely refract content outside the widget silhouette without HWUI viewport-scissor clipping.
2. **Sharp Rim + Blurred Interior vs. Pre-Blurred Backdrop**:
   - Pre-blurring the entire capture via `RenderEffect.createBlurEffect` before refraction (`Kyant0`, `Vitro`, `QWEA0`) blurs away the crisp edge detail that Apple's bevel bends sharply.
   - `ShayannIos27Engine`, `HazeGlassEngine`, and `MochiG2Engine` use `:liquidglass-sdk`'s **dual-strip GPU architecture**: the primary region (`0..recH`) remains sharp for rim refraction, while a quarter-resolution hardware-blurred strip at `y >= uWideStripY` supplies wide-radius (`0–48dp`) interior frosting in a single `RuntimeShader` pass with **zero CPU readback**.
3. **Perceptual Transfer Curves (Avoiding "Muddy Gray" Glass)**:
   - Naive `mix(color, white, alpha)` desaturates colors into milky gray.
   - `Shayann` solves this in **Oklab** (`1.65x` `(a,b)` chroma boost + compressed `L`), `Vitro` uses asymmetric `pale`/`deep` curves with vibrant rim division (`(1 - paleLit)/paleLit`), `Sohum` uses macOS-measured `0.58*L + 34` with `1.72x` saturation, and `SeaGlass` uses HDR glint saturation boosting.
4. **Corner Continuity (G1 Circular Arc vs. G2 Apple Squircle)**:
   - Standard circular rounded rectangles (`sdRoundBox`) have a curvature discontinuity where the straight edge meets the arc, causing a visible "kink" in specular rim highlights and normal vectors.
   - `Shayann`, `Haze` (Profile 1), `SeaGlass`, and `Mochi G2` use Lamé superellipse (`p = 4`, Apple G2 continuous curvature) normals so rim specular glides smoothly around corners.
5. **Liquid Merging (`smin` Smooth Union)**:
   - Apple's Liquid Glass morphs adjacent controls (e.g., FAB + action pills, tab indicators) as a single continuous refractive meniscus using SDF polynomial smooth-minimum (`sminPoly`), which bends normals smoothly across the liquid bridge rather than overlapping two flat alpha layers.

---

## 3. Documented Porting Changes & Known Limitations

- **Unified Multi-Shape & Touch Uniform Header (`CommonAgsl.kt`)**: All 8 refractive engines were extended with a shared 5-neighbor `uExtraShapes` / `sminPoly` SDF union hook and `uTouch`/`uPress` elastic deformation uniforms so every engine can be evaluated fairly on the **Morphing** and **Showcase** benches under identical geometry and touch physics. When `uExtraCount == 0` and `uPress == 0`, each engine reduces to its exact single-shape upstream optical equations.
- **`SohumSnellEngine` (Web SVG -> Android AGSL)**: Upstream `sohumsuthar/liquid-glass` precomputed a 1D radial displacement LUT in JavaScript and applied an 8-bit `feDisplacementMap` (`+-127px` quantization limit). Porting to floating-point AGSL removes 8-bit quantization artifacts and adds full directional rim lighting while preserving exact `n = 1.52` BK7 Snell + Fresnel equations.
- **`FlutterSeaGlassEngine` (Flutter Impeller GLSL -> Android AGSL)**: Translated GLSL `#include <flutter/runtime_effect.glsl>`, `vec2/vec3/vec4`, and texture samplers to SkSL/AGSL `float2/float3/float4` and `content.eval(coord)`. Upstream's multi-pass SDF texture was replaced with analytical per-pixel G2 superellipse SDF + smooth union (`sceneSdSquircle`).
- **In-Window Overlays (`GlassMenuOverlay`, `GlassDialogOverlay`)**: Standard Android `PopupWindow` / `Dialog` create separate OS Window surfaces that cannot sample the host Activity's GPU `GraphicsLayer`. `:liquidglass-sdk` renders menus and dialogs as top-level in-window overlays so they refract the live background at 120 FPS.
- **Minimum Android Version**: AGSL `RuntimeShader` + `RenderEffect.createRuntimeShaderEffect` require **Android 13+ (API 33+)**. The Laboratory app and SDK compile against and target **Android 17 (API 37)** for your Pixel 11 Pro.
