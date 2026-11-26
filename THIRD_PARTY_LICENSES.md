# Third-Party Licenses & Attribution (`NOTICE`)

`Liquid Glass Lab` and `:liquidglass-sdk` integrate and adapt optical equations, shader formulas, and empirical calibration models from permissively licensed (**Apache-2.0** and **MIT**) open-source projects.

Projects with copyleft (`GPL-3.0`, e.g., `Mahmud0808/GlassLayer`) or missing/unverified licenses (e.g., `DnV1eX/LiquidGlassKit`, `DevBehindYou/LiquidLens`) were **excluded** from code integration during Phase 1 license auditing so `:liquidglass-sdk` remains 100% commercially and privately reusable.

---

## 1. `shayann07/liquidglass` (`ShayannIos27Engine`)
- **Repository**: https://github.com/shayann07/liquidglass
- **Author**: Shayan (`shayann07`)
- **License**: Apache License 2.0
- **Attributed Concepts**: Outward padded rim sampling, Snell meniscus normal refraction (`n = 1.50`), Oklab perceptual luminance/chroma transfer (`L = 0.25 + 0.36 * L_in` light / `0.21 + 0.45 * L_in` dark, `1.65x` chroma), dual-strip sharp-rim + blurred-interior blending.

## 2. `Kyant0/AndroidLiquidGlass` (`KyantBackdropEngine`)
- **Repository**: https://github.com/Kyant0/AndroidLiquidGlass
- **Author**: Kyant (`Kyant0`)
- **License**: Apache License 2.0
- **Attributed Concepts**: `circleMap` quarter-circle profile, `1.5x` inflated corner normal smoothing, radial `depthEffect` dome vector, 7-band spectral chromatic dispersion (`Red/Orange/Yellow/Green/Cyan/Blue/Purple`), directional bevel + opposite-rim ambient highlights, elastic spring press physics.

## 3. `itshedimisawi/vitro` (`VitroIos26Engine`)
- **Repository**: https://github.com/itshedimisawi/vitro
- **Author**: Hedi Misawi (`itshedimisawi`)
- **License**: Apache License 2.0
- **Attributed Concepts**: iPhone iOS 26 side-by-side calibrated `pale` (light) and `deep` (dark) luminance-dependent transfer curves (`LightWash`, `LightVeil`, `DarkLift`, `DarkShade`) and vibrant chroma-preserving rim recoloring.

## 4. `chrisbanes/haze` — `haze-glass` (`HazeGlassEngine`)
- **Repository**: https://github.com/chrisbanes/haze
- **Author**: Chris Banes & Contributors
- **License**: Apache License 2.0
- **Attributed Concepts**: Dome-blended optical distance (`effectiveDist`), 4 bevel surface profiles (`Circular`, `Squircle`, `Sinusoidal`, `Concave`), fold-prevention envelope (`maxDisp = dist * 0.90`), linear-light gamut-preserving saturation, sharp-rim detail pass.

## 5. `xenitV1/liquid-glass-skill` (Spiralens Preset & Material Rules)
- **Repository**: https://github.com/xenitV1/liquid-glass-skill
- **Author**: `xenitV1`
- **License**: MIT License
- **Attributed Concepts**: HIG Regular vs Clear material rules and Spiralens squircle Android preset parameters (`surfaceProfile = Squircle`).

## 6. `sohumsuthar/liquid-glass` (`SohumSnellEngine`)
- **Repository**: https://github.com/sohumsuthar/liquid-glass
- **Author**: Sohum Suthar (`sohumsuthar`)
- **License**: MIT License
- **Attributed Concepts**: BK7 crown-glass (`n = 1.52`, Abbe `V_d = 64.17`) exact 3D Snell refraction through a curved meniscus slab (`d = h_local * tan(theta_1 - theta_2)`), unpolarized Fresnel reflectance/transmittance `(Rs + Rp)/2`, and macOS 26 Control Center measured transfer curve (`L_glass = 0.58 * L + 34`, `1.72x` saturation).

## 7. `whynotmake-it/flutter_liquid_glass` / `liquid_glass_renderer` (`FlutterSeaGlassEngine`)
- **Repository**: https://github.com/whynotmake-it/flutter_liquid_glass
- **Authors**: Tim Lehmann / `whynotmake.it`
- **License**: MIT License
- **Attributed Concepts**: Ported from Flutter Impeller GLSL (`liquid_glass_final_render_core.glsl`) to Android AGSL: box-filtered tangential rim sampling, peripheral dark refraction contour, iPhone 17 Pro iOS 27 HDR glint recoloring, and directional raised-rim bevel inner shadow.

## 8. `feice11/mochi-compose` (`MochiG2Engine`)
- **Repository**: https://github.com/feice11/mochi-compose
- **Author**: `feice11`
- **License**: Apache License 2.0
- **Attributed Concepts**: Apple G2 continuous-curvature supercircle SDF (`lnNorm`), dual outer + inner opposing refraction zones, analytical `erfp` Gaussian ring shadow, directional key + fill rim strokes, and interior rim bleed.

## 9. `QWEA0/Liquid-Glass-Android` (`Qwea0GravityLensEngine`)
- **Repository**: https://github.com/QWEA0/Liquid-Glass-Android
- **Author**: `QWEA0`
- **License**: MIT License
- **Attributed Concepts**: Inverse-power gravitational lens decay `(1 + 4t)^-p`, toggleable inward/outward rim sampling, 3-tap tangential `rimSoft` smoothing, absorbing + scattering tinted glass model, and dual-lobe `pow(4.5)` specular rim.

## 10. Additional Studied References (Non-code / Comparative Study Only)
- **`iyinchao/liquid-glass-studio`** (MIT License — https://github.com/iyinchao/liquid-glass-studio): WebGL2/WGSL reference studied for Oklab/LCH perceptual highlight space.
- **`skydoves/Cloudy`** (Apache License 2.0 — https://github.com/skydoves/Cloudy): Evaluated during Phase 1 (`sky.kt` / `LiquidGlassShaderSource.kt`).

---

## MIT License Text (Applies to MIT-Attributed Components Above)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
