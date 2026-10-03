package com.android.inputmethod.latin.glass;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
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
            final Boolean day = isLightTheme(app, themeRes, Configuration.UI_MODE_NIGHT_NO);
            final Boolean night = isLightTheme(app, themeRes, Configuration.UI_MODE_NIGHT_YES);
            if (Boolean.FALSE.equals(day) && Boolean.FALSE.equals(night)) return false;
            return null;
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            return null;
        }
    }

    private static Boolean isLightTheme(final Context app, final int themeRes, final int night) {
        final Configuration config = new Configuration(app.getResources().getConfiguration());
        config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
        final Resources.Theme theme =
                app.createConfigurationContext(config).getResources().newTheme();
        theme.applyStyle(themeRes, true);
        final TypedArray a = theme.obtainStyledAttributes(new int[] {
                android.R.attr.isLightTheme, android.R.attr.colorBackground });
        try {
            if (a.hasValue(0)) return a.getBoolean(0, true);
            if (a.hasValue(1)) return Color.luminance(a.getColor(1, Color.WHITE)) > 0.5f;
            return null;
        } finally {
            a.recycle();
        }
    }
}
