package com.liquidglass.java;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import java.util.Collections;

/**
 * Drop-in Android {@link FrameLayout} View rendering genuine Engine #1 ({@code shayann_ios27.agsl})
 * or any other {@link LiquidGlassEngineId} in plain-Java or XML View hierarchies.
 *
 * <p>Automatically tracks live window coordinates on every frame ({@link ViewTreeObserver.OnPreDrawListener})
 * so dragging, scrolling, or animating either the glass view or the background updates the
 * refracted image continuously at 120 FPS.</p>
 */
public class LiquidGlassView extends FrameLayout {

    private LiquidGlassBackdrop backdrop;
    private LiquidGlassEngineId engineId = LiquidGlassEngineId.SHAYANN_IOS27;
    private LiquidGlassParams params = LiquidGlassParams.ios27Regular();
    private float cornerRadiusPx = -1f; // -1 uses params.cornerRadiusDp * density
    private boolean interactiveTouch = false;

    private final LiquidGlassRenderer renderer;
    private final int[] myLoc = new int[2];
    private final int[] bgLoc = new int[2];
    private int lastRelX = Integer.MIN_VALUE;
    private int lastRelY = Integer.MIN_VALUE;

    private float touchX;
    private float touchY;
    private float pressAmount;

    private final Runnable backdropListener = this::invalidate;

    private final ViewTreeObserver.OnPreDrawListener preDrawListener = () -> {
        if (backdrop != null && backdrop.getHostView() != null && isShown()) {
            getLocationInWindow(myLoc);
            backdrop.getHostView().getLocationInWindow(bgLoc);
            int rx = myLoc[0] - bgLoc[0];
            int ry = myLoc[1] - bgLoc[1];
            if (rx != lastRelX || ry != lastRelY) {
                lastRelX = rx;
                lastRelY = ry;
                invalidate();
            }
        }
        return true;
    };

    public LiquidGlassView(Context context) {
        this(context, (AttributeSet) null);
    }

    public LiquidGlassView(Context context, LiquidGlassBackdrop backdrop) {
        this(context, (AttributeSet) null);
        setBackdrop(backdrop);
    }

    public LiquidGlassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);
        float density = context.getResources().getDisplayMetrics().density;
        this.renderer = new LiquidGlassRenderer(density);
    }

    public void setBackdrop(LiquidGlassBackdrop backdrop) {
        if (this.backdrop == backdrop) return;
        if (this.backdrop != null && isAttachedToWindow()) {
            this.backdrop.removeInvalidationListener(backdropListener);
        }
        this.backdrop = backdrop;
        if (this.backdrop != null && isAttachedToWindow()) {
            this.backdrop.addInvalidationListener(backdropListener);
        }
        invalidate();
    }

    public LiquidGlassBackdrop getBackdrop() {
        return backdrop;
    }

    public void setEngineId(LiquidGlassEngineId engineId) {
        this.engineId = (engineId != null) ? engineId : LiquidGlassEngineId.SHAYANN_IOS27;
        invalidate();
    }

    public LiquidGlassEngineId getEngineId() {
        return engineId;
    }

    public void setParams(LiquidGlassParams params) {
        this.params = (params != null) ? params : LiquidGlassParams.ios27Regular();
        invalidate();
    }

    public LiquidGlassParams getParams() {
        return params;
    }

    public void setCornerRadiusPx(float cornerRadiusPx) {
        this.cornerRadiusPx = cornerRadiusPx;
        invalidate();
    }

    public void setInteractiveTouch(boolean interactiveTouch) {
        this.interactiveTouch = interactiveTouch;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (backdrop != null) {
            backdrop.addInvalidationListener(backdropListener);
        }
        getViewTreeObserver().addOnPreDrawListener(preDrawListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
        if (backdrop != null) {
            backdrop.removeInvalidationListener(backdropListener);
        }
        super.onDetachedFromWindow();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (interactiveTouch) {
            int action = ev.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                touchX = ev.getX();
                touchY = ev.getY();
                pressAmount = 1.0f;
                invalidate();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                pressAmount = 0f;
                invalidate();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (backdrop != null && canvas.isHardwareAccelerated() && getWidth() > 2 && getHeight() > 2) {
            float relX = getLeft();
            float relY = getTop();
            View host = backdrop.getHostView();
            if (host != null) {
                getLocationInWindow(myLoc);
                host.getLocationInWindow(bgLoc);
                relX = myLoc[0] - bgLoc[0];
                relY = myLoc[1] - bgLoc[1];
                lastRelX = (int) relX;
                lastRelY = (int) relY;
            }
            float tx = (pressAmount > 0f) ? touchX : getWidth() * 0.5f;
            float ty = (pressAmount > 0f) ? touchY : getHeight() * 0.5f;
            renderer.draw(
                    canvas,
                    backdrop,
                    engineId,
                    relX,
                    relY,
                    getWidth(),
                    getHeight(),
                    cornerRadiusPx,
                    params,
                    Collections.emptyList(),
                    tx,
                    ty,
                    pressAmount,
                    0f
            );
        }
        super.dispatchDraw(canvas);
    }
}
