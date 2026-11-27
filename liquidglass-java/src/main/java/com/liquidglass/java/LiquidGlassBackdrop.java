package com.liquidglass.java;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RecordingCanvas;
import android.graphics.RenderNode;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure-Java hardware {@link RenderNode} backdrop capture for Android 13–17 (API 33+).
 *
 * <p>Zero Kotlin, zero Jetpack Compose, zero AndroidX dependencies. Works in any plain-Java
 * {@code build.sh} ({@code javac} + {@code d8}) or Gradle project, whether UI is built with
 * classic {@link View}s, XML layouts, or custom {@link View#onDraw(Canvas)} game loops.</p>
 *
 * <h3>Usage Pattern A — Custom {@code View.onDraw(Canvas)} / Game Board (e.g. Tetris):</h3>
 * <pre>{@code
 * RecordingCanvas bgCanvas = backdrop.beginRecording(getWidth(), getHeight());
 * drawBoardBackgroundAndGrid(bgCanvas);
 * backdrop.endRecordingAndDraw(canvas);
 *
 * // Now draw Liquid Glass HUD cards or one-piece tetrominoes over `canvas`:
 * glassRenderer.draw(canvas, backdrop, pieceX, pieceY, cellW, cellH, cornerRadiusPx, params);
 * }</pre>
 *
 * <h3>Usage Pattern B — Android {@code View} Hierarchy:</h3>
 * Wrap your background views in {@link LiquidGlassBackdrop.BackdropFrameLayout} and place
 * {@link LiquidGlassView}s as siblings above it inside a root {@link FrameLayout}.
 */
public final class LiquidGlassBackdrop {

    private final RenderNode renderNode = new RenderNode("LiquidGlassBackdrop");
    private View hostView;
    private int fallbackColorArgb = 0xFF101218;
    private int widthPx;
    private int heightPx;
    private final List<Runnable> invalidationListeners = new ArrayList<>();

    public LiquidGlassBackdrop() {
    }

    public RenderNode getRenderNode() {
        return renderNode;
    }

    public int getWidthPx() {
        return widthPx;
    }

    public int getHeightPx() {
        return heightPx;
    }

    public int getFallbackColorArgb() {
        return fallbackColorArgb;
    }

    public void setFallbackColorArgb(int argb) {
        this.fallbackColorArgb = argb;
    }

    public View getHostView() {
        return hostView;
    }

    public void setHostView(View hostView) {
        this.hostView = hostView;
    }

    public void addInvalidationListener(Runnable listener) {
        if (listener != null && !invalidationListeners.contains(listener)) {
            invalidationListeners.add(listener);
        }
    }

    public void removeInvalidationListener(Runnable listener) {
        invalidationListeners.remove(listener);
    }

    public void notifyContentChanged() {
        List<Runnable> copy = new ArrayList<>(invalidationListeners);
        for (Runnable r : copy) {
            r.run();
        }
    }

    /**
     * Begins recording background drawing commands into the hardware {@link RenderNode}.
     * Always pair with {@link #endRecording()} or {@link #endRecordingAndDraw(Canvas)}.
     */
    public RecordingCanvas beginRecording(int width, int height) {
        int w = Math.max(width, 1);
        int h = Math.max(height, 1);
        this.widthPx = w;
        this.heightPx = h;
        renderNode.setPosition(0, 0, w, h);
        RecordingCanvas rc = renderNode.beginRecording(w, h);
        rc.drawColor(fallbackColorArgb);
        return rc;
    }

    /** Finishes recording without drawing to another canvas. */
    public void endRecording() {
        renderNode.endRecording();
        notifyContentChanged();
    }

    /** Finishes recording and immediately draws the recorded backdrop onto {@code targetCanvas}. */
    public void endRecordingAndDraw(Canvas targetCanvas) {
        renderNode.endRecording();
        if (targetCanvas != null && targetCanvas.isHardwareAccelerated()) {
            targetCanvas.drawRenderNode(renderNode);
        }
        notifyContentChanged();
    }

    /** Captures an existing background {@link View} into this hardware {@link RenderNode}. */
    public void recordFromView(View backgroundView) {
        if (backgroundView == null) return;
        this.hostView = backgroundView;
        int w = backgroundView.getWidth();
        int h = backgroundView.getHeight();
        if (w <= 0 || h <= 0) return;
        RecordingCanvas rc = beginRecording(w, h);
        backgroundView.draw(rc);
        endRecording();
    }

    /**
     * Drop-in {@link FrameLayout} equivalent of Compose's {@code .liquidGlassBackdropSource(backdrop)}.
     * Place background views inside this container, and place {@link LiquidGlassView}s as siblings
     * above this container (never inside it, to avoid recursive self-capture).
     */
    public static class BackdropFrameLayout extends FrameLayout {
        private LiquidGlassBackdrop backdrop;

        public BackdropFrameLayout(Context context) {
            super(context);
            init(new LiquidGlassBackdrop());
        }

        public BackdropFrameLayout(Context context, LiquidGlassBackdrop backdrop) {
            super(context);
            init(backdrop != null ? backdrop : new LiquidGlassBackdrop());
        }

        public BackdropFrameLayout(Context context, AttributeSet attrs) {
            super(context, attrs);
            init(new LiquidGlassBackdrop());
        }

        private void init(LiquidGlassBackdrop b) {
            this.backdrop = b;
            this.backdrop.setHostView(this);
        }

        public LiquidGlassBackdrop getBackdrop() {
            return backdrop;
        }

        public void setBackdrop(LiquidGlassBackdrop backdrop) {
            this.backdrop = backdrop;
            if (backdrop != null) {
                backdrop.setHostView(this);
            }
            invalidate();
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            if (backdrop != null && canvas.isHardwareAccelerated() && getWidth() > 0 && getHeight() > 0) {
                RecordingCanvas rc = backdrop.beginRecording(getWidth(), getHeight());
                super.dispatchDraw(rc);
                backdrop.endRecordingAndDraw(canvas);
            } else {
                super.dispatchDraw(canvas);
            }
        }
    }
}
