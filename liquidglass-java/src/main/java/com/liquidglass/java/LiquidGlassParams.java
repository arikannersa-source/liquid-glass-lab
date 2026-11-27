package com.liquidglass.java;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Pure-Java optical configuration and JSON preset model for Liquid Glass.
 *
 * Uses only {@code java.*} and {@code org.json.*} (built directly into Android's {@code android.jar}),
 * so it compiles in plain {@code javac} / {@code build.sh} projects with zero Gradle, Kotlin, or Compose dependencies.
 */
public final class LiquidGlassParams {

    public enum Variant {
        REGULAR,
        CLEAR
    }

    public enum Appearance {
        ADAPTIVE,
        LIGHT,
        DARK
    }

    /**
     * Secondary SDF shape (in local pixel coordinates relative to the primary element's top-left)
     * merged in a single shader pass via smooth minimum ({@code smin}) — ideal for morphing droplets
     * or multi-cell one-piece game shapes (such as 4-cell Tetris tetrominoes).
     */
    public static final class SubShape {
        public final float centerX;
        public final float centerY;
        public final float halfWidth;
        public final float halfHeight;
        public final float cornerRadius;

        public SubShape(float centerX, float centerY, float halfWidth, float halfHeight, float cornerRadius) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.halfWidth = halfWidth;
            this.halfHeight = halfHeight;
            this.cornerRadius = cornerRadius;
        }
    }

    public Variant variant = Variant.REGULAR;
    public Appearance appearance = Appearance.ADAPTIVE;
    public float refractionStrength = 1.15f;
    public float glassThicknessDp = 24f;
    public float edgeCurvature = 1.0f;
    public float lensDistortion = 0.14f;
    public float blurRadiusDp = 5.5f;
    public float transparency = 0.78f;
    public long tintColorArgb = 0xFFF5F7FAL;
    public float tintOpacity = 0.06f;
    public float chromaticAberration = 0.24f;
    public float specularIntensity = 1.08f;
    public float lightAngleDegrees = 270f;
    public float edgeIllumination = 1.05f;
    public float shadowIntensity = 0.90f;
    public float cornerRadiusDp = 28f;
    public float touchDeformation = 1.0f;
    public float animationSpeed = 1.0f;
    public float morphSmoothingDp = 28f;
    public int surfaceProfile = 1;
    public Boolean outwardSamplingOverride = Boolean.TRUE;

    public LiquidGlassParams() {
    }

    public LiquidGlassParams copy() {
        LiquidGlassParams c = new LiquidGlassParams();
        c.variant = this.variant;
        c.appearance = this.appearance;
        c.refractionStrength = this.refractionStrength;
        c.glassThicknessDp = this.glassThicknessDp;
        c.edgeCurvature = this.edgeCurvature;
        c.lensDistortion = this.lensDistortion;
        c.blurRadiusDp = this.blurRadiusDp;
        c.transparency = this.transparency;
        c.tintColorArgb = this.tintColorArgb;
        c.tintOpacity = this.tintOpacity;
        c.chromaticAberration = this.chromaticAberration;
        c.specularIntensity = this.specularIntensity;
        c.lightAngleDegrees = this.lightAngleDegrees;
        c.edgeIllumination = this.edgeIllumination;
        c.shadowIntensity = this.shadowIntensity;
        c.cornerRadiusDp = this.cornerRadiusDp;
        c.touchDeformation = this.touchDeformation;
        c.animationSpeed = this.animationSpeed;
        c.morphSmoothingDp = this.morphSmoothingDp;
        c.surfaceProfile = this.surfaceProfile;
        c.outwardSamplingOverride = this.outwardSamplingOverride;
        return c;
    }

    /** Engine #1 (Shayann iOS 27) calibrated Regular preset — best for cards, HUD panels, bars, dialogs. */
    public static LiquidGlassParams ios27Regular() {
        return forEngine(LiquidGlassEngineId.SHAYANN_IOS27, Variant.REGULAR, Appearance.ADAPTIVE);
    }

    /** Engine #1 (Shayann iOS 27) calibrated Clear preset — best for floating pills, game pieces, media overlays. */
    public static LiquidGlassParams ios27Clear() {
        return forEngine(LiquidGlassEngineId.SHAYANN_IOS27, Variant.CLEAR, Appearance.ADAPTIVE);
    }

    /** Convenience helper for tinted clear liquid glass (e.g., colored Liquid Glass Tetris tetrominoes). */
    public LiquidGlassParams withTint(int argbColor, float opacity) {
        LiquidGlassParams c = copy();
        c.tintColorArgb = argbColor & 0xFFFFFFFFL;
        c.tintOpacity = clamp(opacity, 0f, 0.95f);
        return c;
    }

    public static LiquidGlassParams forEngine(LiquidGlassEngineId engineId, Variant variant, Appearance appearance) {
        LiquidGlassParams p = new LiquidGlassParams();
        boolean isClear = (variant == Variant.CLEAR);
        p.variant = variant;
        p.appearance = appearance;
        p.refractionStrength = isClear ? 1.22f : 1.15f;
        p.glassThicknessDp = 24f;
        p.edgeCurvature = 1.0f;
        p.lensDistortion = isClear ? 0.18f : 0.14f;
        p.blurRadiusDp = isClear ? 0.8f : 5.5f;
        p.transparency = isClear ? 0.92f : 0.78f;
        p.tintColorArgb = (appearance == Appearance.DARK) ? 0xFF101218L : 0xFFF5F7FAL;
        p.tintOpacity = isClear ? 0.03f : (appearance == Appearance.ADAPTIVE ? 0.06f : 0.16f);
        p.chromaticAberration = isClear ? 0.30f : 0.24f;
        p.specularIntensity = isClear ? 1.15f : 1.08f;
        p.lightAngleDegrees = (engineId == LiquidGlassEngineId.SHAYANN_IOS27
                || engineId == LiquidGlassEngineId.FLUTTER_SEAGLASS) ? 270f : 225f;
        p.edgeIllumination = 1.05f;
        p.shadowIntensity = isClear ? 0.75f : 0.90f;
        p.cornerRadiusDp = 28f;
        p.touchDeformation = 1.0f;
        p.animationSpeed = 1.0f;
        p.morphSmoothingDp = 28f;
        p.surfaceProfile = 1;
        p.outwardSamplingOverride = Boolean.TRUE;

        if (engineId == LiquidGlassEngineId.FROSTED_BASELINE) {
            p.refractionStrength = 0f;
            p.glassThicknessDp = 0f;
            p.lensDistortion = 0f;
            p.blurRadiusDp = isClear ? 4f : 18f;
            p.transparency = isClear ? 0.82f : 0.55f;
            p.tintOpacity = isClear ? 0.12f : 0.34f;
            p.chromaticAberration = 0f;
            p.specularIntensity = 0f;
            p.morphSmoothingDp = 0f;
        }
        return p.sanitized();
    }

    public LiquidGlassParams sanitized() {
        LiquidGlassParams c = copy();
        c.refractionStrength = clamp(c.refractionStrength, 0f, 2.5f);
        c.glassThicknessDp = clamp(c.glassThicknessDp, 0f, 64f);
        c.edgeCurvature = clamp(c.edgeCurvature, 0f, 2.5f);
        c.lensDistortion = clamp(c.lensDistortion, -0.5f, 1.0f);
        c.blurRadiusDp = clamp(c.blurRadiusDp, 0f, 48f);
        c.transparency = clamp(c.transparency, 0f, 1f);
        c.tintOpacity = clamp(c.tintOpacity, 0f, 0.95f);
        c.chromaticAberration = clamp(c.chromaticAberration, 0f, 2.5f);
        c.specularIntensity = clamp(c.specularIntensity, 0f, 2.5f);
        c.lightAngleDegrees = ((c.lightAngleDegrees % 360f) + 360f) % 360f;
        c.edgeIllumination = clamp(c.edgeIllumination, 0f, 2.5f);
        c.shadowIntensity = clamp(c.shadowIntensity, 0f, 2.5f);
        c.cornerRadiusDp = clamp(c.cornerRadiusDp, 0f, 64f);
        c.touchDeformation = clamp(c.touchDeformation, 0f, 2.5f);
        c.animationSpeed = clamp(c.animationSpeed, 0.25f, 2.5f);
        c.morphSmoothingDp = clamp(c.morphSmoothingDp, 0f, 64f);
        if (c.surfaceProfile < 0) c.surfaceProfile = 0;
        if (c.surfaceProfile > 3) c.surfaceProfile = 3;
        return c;
    }

    public float tintRed() {
        return ((tintColorArgb >> 16) & 0xFFL) / 255f;
    }

    public float tintGreen() {
        return ((tintColorArgb >> 8) & 0xFFL) / 255f;
    }

    public float tintBlue() {
        return (tintColorArgb & 0xFFL) / 255f;
    }

    public float darkModeUniform() {
        if (appearance == Appearance.LIGHT) return 0f;
        if (appearance == Appearance.DARK) return 1f;
        return -1f;
    }

    /** Parses a JSON preset exported from Liquid Glass Lab (returns engine + parameters). */
    public static ParsedPreset parsePresetJson(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        String name = root.optString("name", "Imported");
        LiquidGlassEngineId engine = LiquidGlassEngineId.fromKey(root.optString("engine", "shayann_ios27"));
        JSONObject pObj = root.optJSONObject("parameters");
        if (pObj == null) pObj = root;

        LiquidGlassParams p = new LiquidGlassParams();
        try {
            p.variant = Variant.valueOf(pObj.optString("variant", "REGULAR").toUpperCase());
        } catch (IllegalArgumentException ignored) {
            p.variant = Variant.REGULAR;
        }
        try {
            p.appearance = Appearance.valueOf(pObj.optString("appearance", "ADAPTIVE").toUpperCase());
        } catch (IllegalArgumentException ignored) {
            p.appearance = Appearance.ADAPTIVE;
        }
        p.refractionStrength = (float) pObj.optDouble("refractionStrength", 1.15);
        p.glassThicknessDp = (float) pObj.optDouble("glassThicknessDp", 24.0);
        p.edgeCurvature = (float) pObj.optDouble("edgeCurvature", 1.0);
        p.lensDistortion = (float) pObj.optDouble("lensDistortion", 0.14);
        p.blurRadiusDp = (float) pObj.optDouble("blurRadiusDp", 5.5);
        p.transparency = (float) pObj.optDouble("transparency", 0.78);
        Object rawTint = pObj.opt("tintColorArgb");
        if (rawTint instanceof Number) {
            p.tintColorArgb = ((Number) rawTint).longValue() & 0xFFFFFFFFL;
        } else if (rawTint instanceof String) {
            p.tintColorArgb = parseHexArgb((String) rawTint);
        }
        p.tintOpacity = (float) pObj.optDouble("tintOpacity", 0.06);
        p.chromaticAberration = (float) pObj.optDouble("chromaticAberration", 0.24);
        p.specularIntensity = (float) pObj.optDouble("specularIntensity", 1.08);
        p.lightAngleDegrees = (float) pObj.optDouble("lightAngleDegrees", 270.0);
        p.edgeIllumination = (float) pObj.optDouble("edgeIllumination", 1.05);
        p.shadowIntensity = (float) pObj.optDouble("shadowIntensity", 0.90);
        p.cornerRadiusDp = (float) pObj.optDouble("cornerRadiusDp", 28.0);
        p.touchDeformation = (float) pObj.optDouble("touchDeformation", 1.0);
        p.animationSpeed = (float) pObj.optDouble("animationSpeed", 1.0);
        p.morphSmoothingDp = (float) pObj.optDouble("morphSmoothingDp", 28.0);
        p.surfaceProfile = pObj.optInt("surfaceProfile", 1);
        if (pObj.has("outwardSamplingOverride") && !pObj.isNull("outwardSamplingOverride")) {
            p.outwardSamplingOverride = pObj.optBoolean("outwardSamplingOverride");
        }
        return new ParsedPreset(name, engine, p.sanitized());
    }

    public static final class ParsedPreset {
        public final String name;
        public final LiquidGlassEngineId engineId;
        public final LiquidGlassParams params;

        public ParsedPreset(String name, LiquidGlassEngineId engineId, LiquidGlassParams params) {
            this.name = name;
            this.engineId = engineId;
            this.params = params;
        }
    }

    private static long parseHexArgb(String raw) {
        String s = raw.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);
        try {
            if (s.length() == 6) return (0xFF000000L | Long.parseLong(s, 16)) & 0xFFFFFFFFL;
            if (s.length() == 8) return Long.parseLong(s, 16) & 0xFFFFFFFFL;
        } catch (NumberFormatException ignored) {
        }
        return 0xFFFFFFFFL;
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
