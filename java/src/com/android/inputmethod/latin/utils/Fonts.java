package com.android.inputmethod.latin.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.util.Log;

public final class Fonts {
    private static final String TAG = Fonts.class.getSimpleName();
    private static final String FONT_PATH = "fonts/Inter.ttf";

    private static Typeface sRegular = Typeface.DEFAULT;
    private static Typeface sMedium = Typeface.DEFAULT;
    private static Typeface sSemibold = Typeface.DEFAULT_BOLD;
    private static boolean sInitialized;

    private Fonts() {}

    public static synchronized void init(final Context context) {
        if (sInitialized) return;
        sInitialized = true;
        final AssetManager assets = context.getApplicationContext().getAssets();
        sRegular = build(assets, 400, Typeface.DEFAULT);
        sMedium = build(assets, 500, sRegular);
        sSemibold = build(assets, 600, Typeface.DEFAULT_BOLD);
    }

    private static Typeface build(final AssetManager assets, final int weight,
            final Typeface fallback) {
        try {
            final Typeface tf = new Typeface.Builder(assets, FONT_PATH)
                    .setFontVariationSettings("'wght' " + weight + ", 'opsz' 20")
                    .setWeight(weight)
                    .build();
            return tf != null ? tf : fallback;
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot load " + FONT_PATH, e);
            return fallback;
        }
    }

    public static Typeface regular() {
        return sRegular;
    }

    public static Typeface medium() {
        return sMedium;
    }

    public static Typeface semibold() {
        return sSemibold;
    }

    public static Typeface fromStyle(final int style) {
        return (style & Typeface.BOLD) != 0 ? sSemibold : sRegular;
    }
}
