package com.liquidglass.java;

/**
 * Canonical Liquid Glass rendering engine identifiers ranked #1 to #9 by empirical
 * optical fidelity to Apple's iOS 26/27 Liquid Glass.
 *
 * Pure Java 17 — zero Kotlin, zero AndroidX, zero Jetpack Compose dependencies.
 */
public enum LiquidGlassEngineId {
    SHAYANN_IOS27("shayann_ios27", true, false),
    FLUTTER_SEAGLASS("flutter_seaglass", false, true),
    VITRO_IOS26("vitro_ios26", false, true),
    HAZE_GLASS("haze_glass", true, false),
    SOHUM_SNELL("sohum_snell", false, true),
    MOCHI_G2("mochi_g2", true, false),
    KYANT_BACKDROP("kyant_backdrop", false, true),
    QWEA0_GRAVITY("qwea0_gravity", false, true),
    FROSTED_BASELINE("frosted_baseline", false, true);

    private final String key;
    private final boolean usesWideBlurStrip;
    private final boolean usesPreBlur;

    LiquidGlassEngineId(String key, boolean usesWideBlurStrip, boolean usesPreBlur) {
        this.key = key;
        this.usesWideBlurStrip = usesWideBlurStrip;
        this.usesPreBlur = usesPreBlur;
    }

    public String getKey() {
        return key;
    }

    public boolean usesWideBlurStrip() {
        return usesWideBlurStrip;
    }

    public boolean usesPreBlur() {
        return usesPreBlur;
    }

    public static LiquidGlassEngineId fromKey(String key) {
        if (key == null) return SHAYANN_IOS27;
        for (LiquidGlassEngineId id : values()) {
            if (id.key.equalsIgnoreCase(key.trim())) {
                return id;
            }
        }
        return SHAYANN_IOS27;
    }
}
