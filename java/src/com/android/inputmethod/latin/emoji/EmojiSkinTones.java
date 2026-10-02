package com.android.inputmethod.latin.emoji;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class EmojiSkinTones {
    private static final String TAG = EmojiSkinTones.class.getSimpleName();
    private static final String ASSET = "emoji/skin_tones.tsv";

    public static final int TONE_DEFAULT = 0;
    public static final int TONE_COUNT = 6;

    public static final String PREF_DEFAULT_TONE = "pref_emoji_skin_tone_default";
    private static final String PREF_TONE_PREFIX = "pref_emoji_skin_tone_";

    private static volatile Map<String, String[]> sVariants;
    private static volatile Map<String, String> sBaseKeys;

    private EmojiSkinTones() {}

    public static void init(final Context context) {
        if (sVariants != null) return;
        synchronized (EmojiSkinTones.class) {
            if (sVariants != null) return;
            final HashMap<String, String[]> variants = new HashMap<>();
            final HashMap<String, String> baseKeys = new HashMap<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    context.getAssets().open(ASSET), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    final String[] parts = line.split("\t");
                    if (parts.length != TONE_COUNT) continue;
                    final String key = strip(parts[0]);
                    variants.put(key, parts);
                    for (final String p : parts) {
                        baseKeys.put(strip(p), key);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Can't read " + ASSET, e);
            }
            sBaseKeys = baseKeys;
            sVariants = variants;
        }
    }

    private static String strip(final String s) {
        return s.replace("️", "");
    }

    private static Map<String, String[]> variantsMap() {
        final Map<String, String[]> v = sVariants;
        return v != null ? v : Collections.emptyMap();
    }

    private static String baseKey(final String emoji) {
        final Map<String, String> keys = sBaseKeys;
        return emoji == null || keys == null ? null : keys.get(strip(emoji));
    }

    public static boolean hasSkinTones(final String emoji) {
        return baseKey(emoji) != null;
    }

    public static String[] variants(final String emoji) {
        final String key = baseKey(emoji);
        return key == null ? null : variantsMap().get(key);
    }

    public static String baseOf(final String emoji) {
        final String[] v = variants(emoji);
        return v == null ? null : v[0];
    }

    public static int toneOf(final String emoji) {
        final String[] v = variants(emoji);
        if (v == null) return -1;
        final String s = strip(emoji);
        for (int i = 0; i < v.length; i++) {
            if (strip(v[i]).equals(s)) return i;
        }
        return -1;
    }

    private static String prefKey(final String baseKey) {
        final StringBuilder sb = new StringBuilder(PREF_TONE_PREFIX);
        for (int i = 0; i < baseKey.length(); ) {
            final int cp = baseKey.codePointAt(i);
            if (i > 0) sb.append('_');
            sb.append(Integer.toHexString(cp));
            i += Character.charCount(cp);
        }
        return sb.toString();
    }

    public static int getDefaultTone(final SharedPreferences prefs) {
        final int tone = prefs.getInt(PREF_DEFAULT_TONE, TONE_DEFAULT);
        return tone >= 0 && tone < TONE_COUNT ? tone : TONE_DEFAULT;
    }

    public static void setDefaultTone(final SharedPreferences prefs, final int tone) {
        prefs.edit().putInt(PREF_DEFAULT_TONE, tone).apply();
    }

    public static int getPreferredTone(final SharedPreferences prefs, final String emoji) {
        final String key = baseKey(emoji);
        if (key == null) return TONE_DEFAULT;
        final int tone = prefs.getInt(prefKey(key), -1);
        return tone >= 0 && tone < TONE_COUNT ? tone : getDefaultTone(prefs);
    }

    public static void setPreferredTone(final SharedPreferences prefs, final String emoji,
            final int tone) {
        final String key = baseKey(emoji);
        if (key == null) return;
        prefs.edit().putInt(prefKey(key), tone).apply();
    }

    public static String applyPreferred(final SharedPreferences prefs, final String emoji) {
        final String[] v = variants(emoji);
        if (v == null) return emoji;
        return v[getPreferredTone(prefs, emoji)];
    }
}
