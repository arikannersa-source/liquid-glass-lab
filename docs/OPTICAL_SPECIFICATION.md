# Framework-Independent Liquid Glass Optical Specification

**Version**: 1.0.0  
**Purpose**: Reference mathematical and physical model of Liquid Glass decoupled from Android/Compose so future ports (iOS/macOS Metal, Flutter Impeller, WebGPU/WGSL, Vulkan, OpenGL ES) reproduce identical optical behavior.

---

## 1. Coordinate System & Padded Capture Geometry

Let a UI element have logical pixel dimensions $W \times H$ (`uSize`) and half-extents $\mathbf{b} = (W/2, H/2)$.

1. **Outward Refraction Margin (`uPad`)**: Because a convex glass rim bends rays *outward* beyond the element's silhouette, the backdrop capture region must extend by $P$ pixels on all 4 sides:
   $$P = \lceil \max(\text{thicknessPx} \cdot \text{refractionStrength} + 16\cdot\text{density},\; 24\cdot\text{density}) \rceil$$
   Recording dimensions: $W_{\text{rec}} = W + 2P$, $H_{\text{rec}} = H + 2P$.
2. **Local Element Coordinates**: For fragment coordinate $\mathbf{x}_{\text{frag}} \in [0, W_{\text{rec}}] \times [0, H_{\text{rec}}]$:
   $$\mathbf{p}_{\text{local}} = \mathbf{x}_{\text{frag}} - (P, P), \qquad \mathbf{p} = \mathbf{p}_{\text{local}} - \mathbf{b}$$
3. **Screen-Space Light Vector**: Light angle $\theta_L \in [0^\circ, 360^\circ)$ (`lightAngleDegrees`, default $225^\circ$ = top-left $45^\circ$):
   $$\hat{\mathbf{L}} = (\cos\theta_L,\; \sin\theta_L)$$

---

## 2. Signed Distance Fields & Liquid Merging (`smin`)

### 2.1 Apple G2 Continuous Superellipse (`p = 4` Squircle)
Standard circular arcs ($L_2$ norm) exhibit a $G^1$ curvature jump where straight edges meet corner arcs, creating kinks in refractive normals. Continuous-curvature corners use the Lamé $L_n$ norm ($n \approx 4.0$):
$$\|\mathbf{v}\|_n = \left(|v_x|^n + |v_y|^n\right)^{1/n}$$
For corner radius $r = \min(r_{\text{quadrant}}(\mathbf{p}), \min(b_x, b_y))$ and $\mathbf{q} = |\mathbf{p}| - \mathbf{b} + (r, r)$:
$$d_{\text{squircle}}(\mathbf{p}) = \min(\max(q_x, q_y), 0) + \|\max(\mathbf{q}, \mathbf{0})\|_n - r$$

### 2.2 Polynomial Smooth Minimum (`smin`) for Merging Glass
When $M$ sibling shapes $\{d_i\}_{i=1}^M$ approach within smoothing radius $k > 0$ (`morphSmoothingDp * density`), their scalar distance fields combine BEFORE normal estimation:
$$h = \text{clamp}\!\left(\frac{1}{2} + \frac{d_2 - d_1}{2k},\; 0,\; 1\right), \qquad \text{smin}(d_1, d_2, k) = \text{mix}(d_2, d_1, h) - k\,h(1 - h)$$

### 2.3 Outward Boundary Normal
Evaluated via central finite differences ($\epsilon \approx 1.25\text{ px}$) on the combined scene SDF $d(\mathbf{p}_{\text{local}})$:
$$\hat{\mathbf{n}}(\mathbf{p}_{\text{local}}) = \text{normalize}\!\begin{pmatrix} d(\mathbf{p}_{\text{local}} + (\epsilon, 0)) - d(\mathbf{p}_{\text{local}} - (\epsilon, 0)) \\ d(\mathbf{p}_{\text{local}} + (0, \epsilon)) - d(\mathbf{p}_{\text{local}} - (0, \epsilon)) \end{pmatrix}$$
Outside the silhouette $d > 0$; on the boundary $d = 0$; inside the glass $d < 0$, with inward boundary distance $u = \max(-d, 0)$.

---

## 3. 3D Meniscus Surface Profiles & Snell's Law Refraction

Let $B > 0$ be the bevel width in pixels (`glassThicknessDp * density`) and $t = \text{clamp}(u / B, 0, 1)$ be normalized inward progress ($t = 0$ at outer rim, $t = 1$ at flat interior plateau).

### 3.1 Four Canonical Meniscus Profiles
1. **Circular / Quarter-Torus (`Kyant0`, `Sohum`, `Haze` Profile 0)**:
   $$z(t) = \sqrt{1 - (1 - t)^2}, \qquad \text{slope}(t) = \frac{1 - t}{\sqrt{\max(t(2 - t), \epsilon)}}$$
2. **Power / Squircle Meniscus (`Shayann`, `Vitro`, `Haze` Profile 1)**:
   $$\text{edgeFactor}(t) = (1 - t)^\gamma \quad (\gamma \in [1.8, 5.0])$$
3. **Sinusoidal S-Curve (`Haze` Profile 2)**:
   $$z(t) = \sin^2\!\left(\frac{\pi}{2} t\right)$$
4. **Inverse-Power Gravitational Lens (`QWEA0`)**:
   $$\text{decay}(t) = (1 + 4t)^{-p}$$

### 3.2 Exact 3D Vector Snell's Law (`n_air = 1.0 -> n_glass = 1.50..1.52`)
Given surface 3D unit normal $\hat{\mathbf{N}} = (\sin\theta_s \hat{\mathbf{n}}_x,\; \sin\theta_s \hat{\mathbf{n}}_y,\; \cos\theta_s)$ and vertical incident view ray $\hat{\mathbf{I}} = (0, 0, -1)$ with $\eta = n_{\text{air}} / n_{\text{glass}}$:
$$c_1 = -\hat{\mathbf{I}}\cdot\hat{\mathbf{N}} = \cos\theta_s, \qquad k = 1 - \eta^2(1 - c_1^2)$$
$$\hat{\mathbf{T}} = \eta\hat{\mathbf{I}} + (\eta c_1 - \sqrt{\max(k, 0)})\hat{\mathbf{N}}$$
For slab thickness $h(t)$, lateral UV displacement is:
$$\Delta\mathbf{x} = s_{\text{dir}} \cdot D_{\text{refract}} \cdot \frac{\hat{\mathbf{T}}_{xy}}{\max(-\hat{\mathbf{T}}_z, 0.15)}$$
where $s_{\text{dir}} = +1$ samples **outward** (convex rim magnification of exterior background, iOS 27 behavior) and $s_{\text{dir}} = -1$ samples **inward**.

### 3.3 Fold-Prevention Envelope
To prevent UV Jacobian inversion (double-image creasing on tight pills when displacement exceeds local radius), clamp inward displacement to:
$$\|\Delta\mathbf{x}_{\text{inward}}\| \le 0.90\,u$$

---

## 4. Chromatic Dispersion (Abbe Wavelength Separation)

Crown/borosilicate optical glass exhibits higher refractive index for shorter wavelengths ($n_B > n_G > n_R$):
- **3-Channel Cauchy Triad (`Shayann`, `Vitro`, `Haze`, `Sohum`)**:
  $$\Delta\mathbf{x}_R = \Delta\mathbf{x}\,(1 - \alpha_{\text{disp}}), \qquad \Delta\mathbf{x}_G = \Delta\mathbf{x}, \qquad \Delta\mathbf{x}_B = \Delta\mathbf{x}\,(1 + \alpha_{\text{disp}})$$
- **7-Band Spectral Integration (`Kyant0`)**: Samples 7 evenly spaced shifts $\{-1, -\tfrac{2}{3}, -\tfrac{1}{3}, 0, +\tfrac{1}{3}, +\tfrac{2}{3}, +1\}\cdot\delta\mathbf{x}$ weighted across Red, Orange, Yellow, Green, Cyan, Blue, and Violet lobes.

---

## 5. Dual-Zone Sharp Rim + Blurred Interior & Perceptual Color Space

1. **Dual-Zone Blur**: Real glass does not blur rays refracted sharply at the curved outer rim ($t \to 0$). Blend between crisp refracted RGB $\mathbf{C}_{\text{sharp}}$ at the rim and wide-kernel Gaussian blurred RGB $\mathbf{C}_{\text{blur}}$ in the interior:
   $$\mathbf{C}_{\text{refr}} = \text{mix}\!\left(\mathbf{C}_{\text{sharp}},\; \mathbf{C}_{\text{blur}},\; \text{smoothstep}(0.05, 0.55, t)\right)$$
2. **Oklab Perceptual Glass Transfer (`Regular` Variant, iOS 27 Calibrated)**:
   Convert linear sRGB $\to$ LMS cube-root $\to$ Oklab $(L, a, b)$:
   - **Light Appearance**: $L' = 0.25 + 0.36\,L$, $\;(a', b') = 1.65\,(a, b)$
   - **Dark Appearance**: $L' = 0.21 + 0.45\,L$, $\;(a', b') = 1.55\,(a, b)$
   - **Clear Variant**: Preserves full dynamic range ($L' \approx L$) with $1.15\times$ chroma boost and subtle dark-scrim contrast protection.

---

## 6. Fresnel Rim Illumination, Specular Glints & Touch Elasticity

1. **Schlick / Unpolarized Fresnel Rim**:
   $$R_0 = \left(\frac{1 - n}{1 + n}\right)^2 \approx 0.04, \qquad F(\cos\theta_s) = R_0 + (1 - R_0)(1 - \cos\theta_s)^5$$
2. **Directional Key + Opposite Bounce Rim**:
   $$I_{\text{key}} = \max(\hat{\mathbf{n}} \cdot (-\hat{\mathbf{L}}), 0)^p, \qquad I_{\text{fill}} = 0.35\,\max(\hat{\mathbf{n}} \cdot \hat{\mathbf{L}}, 0)^{p_{\text{fill}}}$$
   Confined to a $1.0\text{–}2.5\text{ dp}$ hairline stroke (`smoothstep(w_dp * density, 0, u)`), optionally recoloring bright refracted highlights via HDR chroma boost (`SeaGlass` / `Vitro`) instead of flat white addition.
3. **Underdamped Spring Touch Physics**:
   - **Scale swell**: $1.0 \to 1.0 + 0.035 \cdot \text{touchDeformation}$ with damping ratio $\zeta = 0.52$, stiffness $k = 380$.
   - **Local lens indentation**: Gaussian falloff $\exp(-\|\mathbf{p}_{\text{local}} - \mathbf{p}_{\text{touch}}\|^2 / \sigma^2)$ perturbing local refraction shift toward the finger contact point.
