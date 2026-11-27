# AGENTS.md — Mandatory Instructions for AI Coding Agents

## ZERO-TOLERANCE ANTI-IMITATION RULE

This repository ships **TWO complete, genuine hardware-AGSL SDK distributions** of Engine `#1` (`SHAYANN_IOS27` / `shayann_ios27.agsl`) and Engines `#2`–`#9`:

1. **Kotlin + Jetpack Compose + Gradle Projects** → Use **`:liquidglass-sdk`** (`liquidglass-sdk/` module or `releases/liquidglass-sdk-1.1.0.aar`).
2. **Plain Java + Android `View` / `Canvas` / `Drawable` / Custom `build.sh` (`javac` + `d8`) Projects** → Use **`liquidglass-java`** (`liquidglass-java/src/main/java/com/liquidglass/java/*.java` or `releases/liquidglass-java-1.2.0.jar`). It has **ZERO Kotlin, ZERO Jetpack Compose, ZERO AndroidX, and ZERO Gradle dependencies**—compiling directly against `android.jar` (API 33+).

### Strictly Forbidden Agent Behaviors
- **NEVER** quietly substitute a hand-written fake `Drawable`, `Paint` gradient/shadow/highlight bevel, or `Modifier.blur()` + border imitation (e.g., `LiquidGlassDrawable.java`) and claim it uses Engine `#1`.
- **NEVER** write a fake stand-in `Backdrop` class that does not record the real background into a hardware `RenderNode` / `GraphicsLayer`.
- **NEVER** claim incompatibility because a project uses plain Java, Android `View`s, custom `Canvas` game loops, or `build.sh` without Gradle/Compose—use `com.liquidglass.java.*` (`releases/liquidglass-java-1.2.0.jar` or copy the 5 `.java` files in `liquidglass-java/src/main/java/com/liquidglass/java/`).
- If a target project has `minSdk < 33` or software-only `Bitmap` rendering where Android's `RuntimeShader` + `RenderNode` cannot execute, **stop and tell the user explicitly** before writing any fallback.

---

## Track A: Plain Java / Android `View` / Custom `Canvas` / `build.sh` (`com.liquidglass.java.*`)

### 1. Add to `build.sh` (`javac` + `d8`) or Gradle
- **Option 1 (Copy 5 `.java` files — zero build script changes if compiling `src/**/*.java`)**:
  Copy `liquidglass-java/src/main/java/com/liquidglass/java/*.java` directly into your project's Java source tree.
- **Option 2 (Precompiled JAR)**:
  Download `releases/liquidglass-java-1.2.0.jar`, pass `-cp "$ANDROID_JAR:libs/liquidglass-java-1.2.0.jar"` to `javac`, and include `libs/liquidglass-java-1.2.0.jar` in your `d8` command.
- Ensure `<application android:hardwareAccelerated="true">` and `minSdk >= 33` (`targetSdk 37`).

### 2. Usage in Custom `View.onDraw(Canvas)` / Game Loops (e.g. Tetris Board, Pieces & HUD)
See [`examples/PlainJavaGameAndViewIntegration.java`](examples/PlainJavaGameAndViewIntegration.java) for a complete compilable example:
```java
// Fields:
private final LiquidGlassBackdrop backdrop = new LiquidGlassBackdrop();
private final LiquidGlassRenderer glassRenderer = new LiquidGlassRenderer(density);
private final LiquidGlassParams hudParams = LiquidGlassParams.ios27Regular();
private final LiquidGlassParams clearPieceParams = LiquidGlassParams.ios27Clear();

@Override
protected void onDraw(Canvas canvas) {
    // 1. Record the live background / grid into hardware RenderNode AND draw it to screen:
    RecordingCanvas bg = backdrop.beginRecording(getWidth(), getHeight());
    drawBackgroundAndGrid(bg);
    backdrop.endRecordingAndDraw(canvas);

    // 2. Render Engine #1 Liquid Glass HUD panels, buttons, or multi-cell one-piece game shapes:
    glassRenderer.drawAtPosition(
        canvas,
        backdrop,
        LiquidGlassEngineId.SHAYANN_IOS27,
        panelX, panelY, panelW, panelH,
        cornerRadiusPx,
        hudParams,
        extraSubShapesOrNull, // Up to 5 SubShapes merged in one unified SDF pass (e.g. 4-cell tetrominoes!)
        extraPadPx
    );
}
```

### 3. Usage in Classic Android `View` / `FrameLayout` Hierarchies
1. Wrap background views **only** inside `LiquidGlassBackdrop.BackdropFrameLayout` (`new LiquidGlassBackdrop.BackdropFrameLayout(context, backdrop)`).
2. Add `new LiquidGlassView(context, backdrop)` as a **sibling above** `BackdropFrameLayout` inside your root `FrameLayout`.

---

## Track B: Kotlin + Jetpack Compose (`:liquidglass-sdk`)

### 1. Add Dependency
```kotlin
dependencies {
    implementation(project(":liquidglass-sdk"))
    // OR: implementation(files("libs/liquidglass-sdk-1.1.0.aar"))
}
```

### 2. Wire Backdrop Capture Once at the Screen Root
Apply `.liquidGlassBackdropSource(backdrop)` **only** to the background composable subtree, and place Liquid Glass components (`GlassButton`, `GlassBottomNavigationBar`, `GlassSearchBar`, `GlassCard`, `GlassSlider`, `GlassSwitch`, `GlassSegmentedControl`, `GlassMenuOverlay`, `GlassDialogOverlay`, or `Modifier.liquidGlass(...)`) as **siblings above** it:
```kotlin
val backdrop = rememberGlassBackdropState()

Box(Modifier.fillMaxSize()) {
    BackgroundContent(
        modifier = Modifier
            .fillMaxSize()
            .liquidGlassBackdropSource(backdrop)
    )

    GlassButton(
        text = "Continue",
        backdrop = backdrop,
        engineId = GlassEngineId.SHAYANN_IOS27,
        parameters = GlassEngineRegistry.get(GlassEngineId.SHAYANN_IOS27).defaultParameters(),
        onClick = { ... }
    )
}
```
