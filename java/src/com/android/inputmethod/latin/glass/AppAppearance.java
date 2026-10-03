package com.android.inputmethod.latin.glass;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.LruCache;

public final class AppAppearance {
    private static final LruCache<String, Boolean> sCache = new LruCache<>(64);

    private AppAppearance() {}

    public static void invalidate() {
        sCache.evictAll();
    }

    public static Boolean isLight(final Context context, final String packageName) {
        if (packageName == null || packageName.equals(context.getPackageName())) return null;
        final Boolean cached = sCache.get(packageName);
        if (cached != null) return cached;
        final Boolean light = resolve(context, packageName);
        if (light != null) sCache.put(packageName, light);
        return light;
    }

    private static Boolean resolve(final Context context, final String packageName) {
        try {
            final Context app = context.createPackageContext(packageName, 0);
            final int themeRes = app.getApplicationInfo().theme;
            if (themeRes == 0) return null;
            final Resources.Theme theme = app.getResources().newTheme();
            theme.applyStyle(themeRes, true);
            final TypedArray a = theme.obtainStyledAttributes(new int[] {
                    android.R.attr.isLightTheme, android.R.attr.colorBackground });
            try {
                if (a.hasValue(0)) return a.getBoolean(0, true);
                if (a.hasValue(1)) {
                    return Color.luminance(a.getColor(1, Color.WHITE)) > 0.5f;
                }
                return null;
            } finally {
                a.recycle();
            }
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            return null;
        }
    }
}
