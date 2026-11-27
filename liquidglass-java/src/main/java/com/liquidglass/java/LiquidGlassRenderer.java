package com.liquidglass.java;

import android.graphics.Canvas;
import android.graphics.RecordingCanvas;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.RuntimeShader;
import android.graphics.Shader;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure-Java hardware-accelerated Liquid Glass renderer powered by Android 13–17
 * {@link RuntimeShader}, {@link RenderEffect}, and {@link RenderNode}.
 *
 * <p>Executes the <b>exact</b> Engine #1 ({@code shayann_ios27.agsl}) and #2–#9 shaders from
 * {@code liquid-glass-lab} — including outward padded rim refraction ({@code uPad}),
 * dual-strip quarter-resolution wide interior blur + razor-sharp rim, Oklab perceptual
 * highlights, Snell meniscus optics, and {@code smin} multi-shape merging — with zero
 * Kotlin, Compose, or Gradle dependencies.</p>
 */
public final class LiquidGlassRenderer {

    private final RenderNode glassNode = new RenderNode("LiquidGlassNode");
    private final RenderNode wideBlurNode = new RenderNode("LiquidGlassWideBlur");
    private final RenderNode offscreenHostNode = new RenderNode("LiquidGlassHost");
    private final Map<LiquidGlassEngineId, RuntimeShader> shaderCache = new HashMap<>();

    private float density = 3.0f;

    public LiquidGlassRenderer() {
        offscreenHostNode.setUseCompositingLayer(true, null);
    }

    public LiquidGlassRenderer(float displayDensity) {
        this();
        this.density = Math.max(displayDensity, 0.5f);
    }

    public void setDensity(float displayDensity) {
        this.density = Math.max(displayDensity, 0.5f);
    }

    public float getDensity() {
        return density;
    }

    /**
     * Convenience overload rendering Engine #1 ({@link LiquidGlassEngineId#SHAYANN_IOS27})
     * onto {@code targetCanvas} at {@code (leftInBackdrop, topInBackdrop)} with dimensions {@code width x height}.
     */
    public void draw(
            Canvas targetCanvas,
            LiquidGlassBackdrop backdrop,
            float leftInBackdrop,
            float topInBackdrop,
            float width,
            float height,
            float cornerRadiusPx,
            LiquidGlassParams params
    ) {
        draw(
                targetCanvas,
                backdrop,
                LiquidGlassEngineId.SHAYANN_IOS27,
                leftInBackdrop,
                topInBackdrop,
                width,
                height,
                cornerRadiusPx,
                params,
                Collections.emptyList(),
                width * 0.5f,
                height * 0.5f,
                0f,
                0f
        );
    }

    /**
     * Full hardware Liquid Glass draw call supporting any {@link LiquidGlassEngineId},
     * multi-block {@link LiquidGlassParams.SubShape} merging (e.g. one-piece Tetris tetrominoes
     * or morphing droplets), and interactive fingertip press deformation.
     *
     * <p>Note: {@code targetCanvas} should already be translated to {@code (leftInBackdrop, topInBackdrop)}
     * if drawing inside a child {@code View.onDraw(Canvas)}, OR if drawing in backdrop/board coordinates
     * use {@link #drawAtPosition}.</p>
     */
    public void drawAtPosition(
            Canvas targetCanvas,
            LiquidGlassBackdrop backdrop,
            LiquidGlassEngineId engineId,
            float xOnCanvasAndBackdrop,
            float yOnCanvasAndBackdrop,
            float width,
            float height,
            float cornerRadiusPx,
            LiquidGlassParams params,
            List<LiquidGlassParams.SubShape> extraShapes,
            float extraPadPx
    ) {
        if (targetCanvas == null) return;
        targetCanvas.save();
        targetCanvas.translate(xOnCanvasAndBackdrop, yOnCanvasAndBackdrop);
        draw(
                targetCanvas,
                backdrop,
                engineId,
                xOnCanvasAndBackdrop,
                yOnCanvasAndBackdrop,
                width,
                height,
                cornerRadiusPx,
                params,
                extraShapes,
                width * 0.5f,
                height * 0.5f,
                0f,
                extraPadPx
        );
        targetCanvas.restore();
    }

    /**
     * Core hardware draw routine. Draws at local origin {@code (0, 0)} on {@code targetCanvas},
     * sampling {@code backdrop} at offset {@code (leftInBackdrop, topInBackdrop)}.
     */
    public void draw(
            Canvas targetCanvas,
            LiquidGlassBackdrop backdrop,
            LiquidGlassEngineId engineId,
            float leftInBackdrop,
            float topInBackdrop,
            float width,
            float height,
            float cornerRadiusPx,
            LiquidGlassParams params,
            List<LiquidGlassParams.SubShape> extraShapes,
            float touchX,
            float touchY,
            float pressAmount,
            float extraPadPx
    ) {
        if (targetCanvas == null || backdrop == null || width < 2f || height < 2f) {
            return;
        }
        if (!targetCanvas.isHardwareAccelerated()) {
            throw new IllegalStateException(
                    "LiquidGlassRenderer requires a hardware-accelerated Canvas (Android 13+ Window/RenderNode). " +
                    "Never substitute software Bitmap/Paint imitations — enable android:hardwareAccelerated=\"true\"."
            );
        }
        if (!backdrop.getRenderNode().hasDisplayList()) {
            return;
        }

        LiquidGlassEngineId activeEngine = (engineId != null) ? engineId : LiquidGlassEngineId.SHAYANN_IOS27;
        LiquidGlassParams safeParams = (params != null ? params : LiquidGlassParams.ios27Regular()).sanitized();

        float maxR = Math.min(width, height) * 0.5f;
        float rPx = Math.max(0f, Math.min(cornerRadiusPx >= 0f ? cornerRadiusPx : safeParams.cornerRadiusDp * density, maxR));
        float blurPx = safeParams.blurRadiusDp * density;
        float thickPx = safeParams.glassThicknessDp * density;

        float autoPadPx = clamp(Math.max(24f * density, Math.max(blurPx * 0.65f, thickPx * 0.85f)), 20f, 84f * density);
        float padPx = Math.max(autoPadPx, extraPadPx);

        int recW = Math.max(4, (int) Math.ceil(width + 2f * padPx));
        int recH = Math.max(4, (int) Math.ceil(height + 2f * padPx));

        boolean usesWideStrip = activeEngine.usesWideBlurStrip();
        float wideScale = 0.25f;
        int wideW = usesWideStrip ? Math.max(8, (int) Math.ceil(recW * wideScale)) : 0;
        int wideH = usesWideStrip ? Math.max(8, (int) Math.ceil(recH * wideScale)) : 0;
        float wideStripY = usesWideStrip ? (recH + 4f) : 0f;
        int totalRecH = usesWideStrip ? (recH + wideH + 8) : recH;

        RuntimeShader shader = shaderCache.get(activeEngine);
        if (shader == null) {
            shader = new RuntimeShader(LiquidGlassShaders.getShaderSource(activeEngine));
            shaderCache.put(activeEngine, shader);
        }

        int bgW = Math.max(backdrop.getWidthPx(), 1);
        int bgH = Math.max(backdrop.getHeightPx(), 1);
        float loX = clamp(padPx - leftInBackdrop, 1f, recW - 2f);
        float loY = clamp(padPx - topInBackdrop, 1f, recH - 2f);
        float hiX = clamp(padPx - leftInBackdrop + bgW, loX + 1f, recW - 1f);
        float hiY = clamp(padPx - topInBackdrop + bgH, loY + 1f, recH - 1f);

        LiquidGlassShaders.bindUniforms(
                shader,
                activeEngine,
                width,
                height,
                padPx,
                density,
                rPx,
                safeParams,
                extraShapes != null ? extraShapes : Collections.emptyList(),
                touchX,
                touchY,
                pressAmount,
                wideStripY,
                wideScale,
                loX,
                loY,
                hiX,
                hiY
        );

        int fallbackArgb = backdrop.getFallbackColorArgb();

        // 1. Optional quarter-resolution wide-blur strip for crisp-rim + blurred-interior engines (#1, #4, #6)
        if (usesWideStrip) {
            float wideSigma = clamp(blurPx * wideScale, 0.6f, 36f);
            wideBlurNode.setPosition(0, 0, wideW, wideH);
            wideBlurNode.setRenderEffect(
                    blurPx > 0.5f
                            ? RenderEffect.createBlurEffect(wideSigma, wideSigma, Shader.TileMode.CLAMP)
                            : null
            );
            RecordingCanvas wbCanvas = wideBlurNode.beginRecording(wideW, wideH);
            wbCanvas.drawColor(fallbackArgb);
            wbCanvas.scale(wideScale, wideScale);
            wbCanvas.translate(padPx - leftInBackdrop, padPx - topInBackdrop);
            wbCanvas.drawRenderNode(backdrop.getRenderNode());
            wideBlurNode.endRecording();
        }

        // 2. Build runtime shader RenderEffect (chaining pre-blur only for engines that require it)
        RenderEffect shaderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content");
        RenderEffect combinedEffect;
        if (activeEngine.usesPreBlur() && blurPx > 0.5f) {
            RenderEffect preBlur = RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP);
            combinedEffect = RenderEffect.createChainEffect(shaderEffect, preBlur);
        } else {
            combinedEffect = shaderEffect;
        }

        glassNode.setPosition(0, 0, recW, totalRecH);
        glassNode.setRenderEffect(combinedEffect);
        RecordingCanvas gCanvas = glassNode.beginRecording(recW, totalRecH);
        gCanvas.drawColor(fallbackArgb);
        gCanvas.save();
        gCanvas.translate(padPx - leftInBackdrop, padPx - topInBackdrop);
        gCanvas.drawRenderNode(backdrop.getRenderNode());
        gCanvas.restore();
        if (usesWideStrip) {
            gCanvas.save();
            gCanvas.translate(0f, wideStripY);
            gCanvas.drawRenderNode(wideBlurNode);
            gCanvas.restore();
        }
        glassNode.endRecording();

        // 3. Wrap inside offscreen host RenderNode so HWUI viewport scissor never clips uPad or uWideStripY
        offscreenHostNode.setPosition(0, 0, recW, totalRecH);
        RecordingCanvas hostCanvas = offscreenHostNode.beginRecording(recW, totalRecH);
        hostCanvas.drawRenderNode(glassNode);
        offscreenHostNode.endRecording();

        targetCanvas.save();
        targetCanvas.translate(-padPx, -padPx);
        targetCanvas.clipRect(0f, 0f, recW, recH);
        targetCanvas.drawRenderNode(offscreenHostNode);
        targetCanvas.restore();
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
