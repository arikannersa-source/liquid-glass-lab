package com.example.liquidglassjava;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RecordingCanvas;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.liquidglass.java.LiquidGlassBackdrop;
import com.liquidglass.java.LiquidGlassEngineId;
import com.liquidglass.java.LiquidGlassParams;
import com.liquidglass.java.LiquidGlassRenderer;
import com.liquidglass.java.LiquidGlassView;

import java.util.Arrays;
import java.util.List;

/**
 * Plain-Java (zero Kotlin, zero Compose, zero Gradle required) examples showing how to use
 * genuine Engine #1 ({@link LiquidGlassEngineId#SHAYANN_IOS27}) in:
 * 1. Custom {@link View#onDraw(Canvas)} game boards (e.g., Liquid Glass Tetris HUD + one-piece tetrominoes)
 * 2. Classic Android {@link View} / {@link FrameLayout} hierarchies.
 */
public final class PlainJavaGameAndViewIntegration {

    /**
     * Example 1: Custom Canvas Game View (e.g., Liquid Glass Tetris Board + One-Piece Tetromino).
     */
    public static class LiquidGlassBoardView extends View {
        private final LiquidGlassBackdrop backdrop = new LiquidGlassBackdrop();
        private final LiquidGlassRenderer glassRenderer;
        private final LiquidGlassParams hudParams = LiquidGlassParams.ios27Regular();
        private final LiquidGlassParams pieceBaseParams;
        private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float density;

        public LiquidGlassBoardView(Context context) {
            super(context);
            this.density = context.getResources().getDisplayMetrics().density;
            this.glassRenderer = new LiquidGlassRenderer(density);

            // Configure Clear iOS 27 glass with crisp joined cells for one-piece tetrominoes
            this.pieceBaseParams = LiquidGlassParams.ios27Clear();
            this.pieceBaseParams.glassThicknessDp = 10f;
            this.pieceBaseParams.morphSmoothingDp = 3f; // Subtle liquid bridge across adjacent cells

            gridPaint.setColor(0x33FFFFFF);
            gridPaint.setStrokeWidth(density);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0 || !canvas.isHardwareAccelerated()) return;

            // STEP 1: Record the background & board grid into LiquidGlassBackdrop's hardware RenderNode
            RecordingCanvas bg = backdrop.beginRecording(w, h);
            bg.drawColor(0xFF0E1320);
            float cell = w / 10f;
            for (int col = 0; col <= 10; col++) {
                bg.drawLine(col * cell, 0f, col * cell, h, gridPaint);
            }
            for (int row = 0; row <= 20; row++) {
                bg.drawLine(0f, row * cell, w, row * cell, gridPaint);
            }
            backdrop.endRecordingAndDraw(canvas);

            // STEP 2: Render a one-piece T-Tetromino (4 cells: [1,0], [0,1], [1,1], [2,1]) as ONE unified Engine #1 glass shape!
            float originX = 3 * cell;
            float originY = 5 * cell;
            float half = cell * 0.5f;
            float cornerR = 6f * density;

            // Primary box is center cell [1,1] at (originX + cell, originY + cell) of size (cell x cell).
            // SubShapes are expressed relative to the primary box's top-left corner:
            List<LiquidGlassParams.SubShape> tPieceSiblings = Arrays.asList(
                    new LiquidGlassParams.SubShape(half, -half, half, half, cornerR),     // Top cell [1,0]
                    new LiquidGlassParams.SubShape(-half, half, half, half, cornerR),     // Left cell [0,1]
                    new LiquidGlassParams.SubShape(cell + half, half, half, half, cornerR) // Right cell [2,1]
            );
            LiquidGlassParams purpleTPiece = pieceBaseParams.withTint(0xFFBF5AF2, 0.30f);

            glassRenderer.drawAtPosition(
                    canvas,
                    backdrop,
                    LiquidGlassEngineId.SHAYANN_IOS27,
                    originX + cell,
                    originY + cell,
                    cell,
                    cell,
                    cornerR,
                    purpleTPiece,
                    tPieceSiblings,
                    cell * 1.5f
            );

            // STEP 3: Render a floating Liquid Glass Score / Next-Piece HUD panel
            glassRenderer.drawAtPosition(
                    canvas,
                    backdrop,
                    LiquidGlassEngineId.SHAYANN_IOS27,
                    16f * density,
                    16f * density,
                    160f * density,
                    72f * density,
                    22f * density,
                    hudParams,
                    null,
                    0f
            );
        }
    }

    /**
     * Example 2: Classic Android View Hierarchy (FrameLayout + LiquidGlassView).
     */
    public static FrameLayout createViewHierarchyExample(Context context, View backgroundContentView) {
        FrameLayout root = new FrameLayout(context);
        LiquidGlassBackdrop backdrop = new LiquidGlassBackdrop();

        // 1. Wrap background content ONLY inside BackdropFrameLayout
        LiquidGlassBackdrop.BackdropFrameLayout bgContainer =
                new LiquidGlassBackdrop.BackdropFrameLayout(context, backdrop);
        bgContainer.addView(backgroundContentView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        root.addView(bgContainer);

        // 2. Add genuine Engine #1 LiquidGlassView as a sibling above bgContainer
        LiquidGlassView glassPill = new LiquidGlassView(context, backdrop);
        glassPill.setEngineId(LiquidGlassEngineId.SHAYANN_IOS27);
        glassPill.setParams(LiquidGlassParams.ios27Regular());
        glassPill.setInteractiveTouch(true);

        TextView label = new TextView(context);
        label.setText("Liquid Glass Engine #1");
        label.setTextColor(0xFFFFFFFF);
        label.setGravity(Gravity.CENTER);
        glassPill.addView(label);

        int density = (int) context.getResources().getDisplayMetrics().density;
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(220 * density, 56 * density);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.bottomMargin = 32 * density;
        root.addView(glassPill, lp);

        return root;
    }
}
