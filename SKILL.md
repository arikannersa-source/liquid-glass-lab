---
name: android-liquid-glass-sdk
description: Integrate genuine Apple-calibrated Liquid Glass Engine #1 (shayann_ios27.agsl) on Android (API 33–37) in BOTH Kotlin/Jetpack Compose (:liquidglass-sdk) AND Plain-Java/Views/Canvas/build.sh projects (liquidglass-java) with zero imitations.
---

# Android Liquid Glass SDK Skill

Use this skill whenever building or styling any Android app or game with **Liquid Glass**.

## Zero-Tolerance Anti-Imitation Policy
1. **NEVER** write a hand-rolled fake `Drawable`, `Canvas` shadow/bevel/highlight imitation (such as a custom `LiquidGlassDrawable.java`), or `Modifier.blur()` + white border, and never claim an imitation is Engine `#1`.
2. **NEVER** write a dummy `Backdrop` stand-in class—always capture the real background into a hardware `RenderNode` (`LiquidGlassBackdrop` in Java) or `GraphicsLayer` (`GlassBackdropState` in Compose).
3. Choose the matching genuine SDK distribution from `https://github.com/arikannersa-source/liquid-glass-lab`:
   - **Plain Java / Android `View` / Custom `Canvas` / `build.sh` (`javac` + `d8`)**: Use `com.liquidglass.java.*` (`liquidglass-java/src/main/java/com/liquidglass/java/*.java` or `releases/liquidglass-java-1.2.0.jar`). Zero Kotlin, zero Compose, zero AndroidX, zero Gradle required.
   - **Kotlin + Jetpack Compose**: Use `:liquidglass-sdk` (`liquidglass-sdk/` module or `releases/liquidglass-sdk-1.1.0.aar`).

---

## Track 1: Plain Java / Views / Custom `Canvas` / `build.sh` (`com.liquidglass.java.*`)

### Drop-in Setup (`build.sh` or Gradle)
- Copy the 5 files from `liquidglass-java/src/main/java/com/liquidglass/java/` into your source folder **OR** add `releases/liquidglass-java-1.2.0.jar` to `javac -cp` and `d8`.
- Requires `minSdk >= 33` and hardware-accelerated window (`android:hardwareAccelerated="true"`).

### Custom `Canvas` / Game Loop Pattern (e.g., Tetris Board, One-Piece Tetrominoes, HUD)
```java
private final LiquidGlassBackdrop backdrop = new LiquidGlassBackdrop();
private final LiquidGlassRenderer renderer = new LiquidGlassRenderer(density);
private final LiquidGlassParams hudParams = LiquidGlassParams.ios27Regular();
private final LiquidGlassParams pieceParams = LiquidGlassParams.ios27Clear().withTint(0xFF0A84FF, 0.28f);

@Override
protected void onDraw(Canvas canvas) {
    // 1. Record background/grid into hardware RenderNode and draw to screen
    RecordingCanvas bg = backdrop.beginRecording(getWidth(), getHeight());
    drawBoardBackground(bg);
    backdrop.endRecordingAndDraw(canvas);

    // 2. Render Engine #1 Liquid Glass panel or multi-cell SubShape piece
    renderer.drawAtPosition(
        canvas, backdrop, LiquidGlassEngineId.SHAYANN_IOS27,
        x, y, width, height, cornerRadiusPx,
        pieceParams, extraSubShapes, extraPadPx
    );
}
```

### Android `View` Hierarchy Pattern
Wrap background views in `new LiquidGlassBackdrop.BackdropFrameLayout(context, backdrop)` and place `new LiquidGlassView(context, backdrop)` as a sibling above it inside a `FrameLayout`.

---

## Track 2: Kotlin + Jetpack Compose (`:liquidglass-sdk`)

```kotlin
val backdrop = rememberGlassBackdropState()
val params = GlassEngineRegistry.get(GlassEngineId.SHAYANN_IOS27).defaultParameters()

Box(Modifier.fillMaxSize()) {
    AppBackground(
        modifier = Modifier.fillMaxSize().liquidGlassBackdropSource(backdrop)
    )
    GlassBottomNavigationBar(
        items = navItems,
        selectedIndex = tab,
        onSelect = { tab = it },
        backdrop = backdrop,
        engineId = GlassEngineId.SHAYANN_IOS27,
        parameters = params,
        modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp)
    )
}
```
