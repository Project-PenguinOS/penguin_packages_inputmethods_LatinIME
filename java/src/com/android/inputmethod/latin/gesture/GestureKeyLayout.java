package com.android.inputmethod.latin.gesture;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;

public final class GestureKeyLayout {
    private static final int DIRECT_LOOKUP_SIZE = 0x800;

    private final int[] mCodes;
    final float[] mCenterX;
    final float[] mCenterY;
    final float[] mLeft;
    final float[] mTop;
    final float[] mRight;
    final float[] mBottom;
    final int mKeyCount;
    final float mKeyWidth;
    private final String mSignature;

    private final int[] mDirectLookup = new int[DIRECT_LOOKUP_SIZE];
    private final HashMap<Integer, Integer> mLookup = new HashMap<>();

    public GestureKeyLayout(final int[] codes, final int[] x, final int[] y, final int[] width,
            final int[] height, final int count, final int keyWidth) {
        mKeyWidth = Math.max(1, keyWidth);
        final int[] kept = new int[count];
        int keyCount = 0;
        final StringBuilder signature = new StringBuilder();
        Arrays.fill(mDirectLookup, -1);
        for (int i = 0; i < count; ++i) {
            final int code = codes[i];
            if (code <= 0 || !Character.isLetter(code)) continue;
            final int lower = Character.toLowerCase(code);
            if (findKey(lower) >= 0) continue;
            putKey(lower, keyCount);
            kept[keyCount++] = i;
            signature.append(lower).append(':').append(x[i]).append(',').append(y[i])
                    .append(',').append(width[i]).append(',').append(height[i]).append(';');
        }
        mKeyCount = keyCount;
        mCodes = new int[keyCount];
        mCenterX = new float[keyCount];
        mCenterY = new float[keyCount];
        mLeft = new float[keyCount];
        mTop = new float[keyCount];
        mRight = new float[keyCount];
        mBottom = new float[keyCount];
        final float inv = 1.0f / mKeyWidth;
        for (int k = 0; k < keyCount; ++k) {
            final int i = kept[k];
            mCodes[k] = Character.toLowerCase(codes[i]);
            mLeft[k] = x[i] * inv;
            mTop[k] = y[i] * inv;
            mRight[k] = (x[i] + width[i]) * inv;
            mBottom[k] = (y[i] + height[i]) * inv;
            mCenterX[k] = (mLeft[k] + mRight[k]) * 0.5f;
            mCenterY[k] = (mTop[k] + mBottom[k]) * 0.5f;
        }
        signature.append(keyWidth);
        mSignature = signature.toString();
    }

    private void putKey(final int code, final int index) {
        if (code < DIRECT_LOOKUP_SIZE) {
            mDirectLookup[code] = index;
        } else {
            mLookup.put(code, index);
        }
    }

    private int findKey(final int code) {
        if (code >= 0 && code < DIRECT_LOOKUP_SIZE) {
            return mDirectLookup[code];
        }
        final Integer index = mLookup.get(code);
        return index == null ? -1 : index;
    }

    public int getKeyCount() {
        return mKeyCount;
    }

    public int getCode(final int keyIndex) {
        return mCodes[keyIndex];
    }

    public String getSignature() {
        return mSignature;
    }

    public int getKeyIndexForCodePoint(final int codePoint) {
        final int lower = Character.toLowerCase(codePoint);
        int index = findKey(lower);
        if (index >= 0) return index;
        if (lower < 0x80) return -1;
        final String decomposed = Normalizer.normalize(
                new String(Character.toChars(lower)), Normalizer.Form.NFD);
        if (decomposed.isEmpty()) return -1;
        final int base = decomposed.codePointAt(0);
        if (base == lower) return -1;
        index = findKey(Character.toLowerCase(base));
        return index;
    }
}
