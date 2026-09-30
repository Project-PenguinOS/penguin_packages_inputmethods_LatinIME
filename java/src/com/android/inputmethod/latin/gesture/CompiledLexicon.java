package com.android.inputmethod.latin.gesture;

import java.util.Arrays;
import java.util.HashMap;

public final class CompiledLexicon {
    final GestureLexicon mLexicon;
    final GestureKeyLayout mLayout;
    final byte[] mKeys;
    final int[] mKeyStarts;
    final float[] mPathLengths;
    final int[][] mBuckets;

    public CompiledLexicon(final GestureLexicon lexicon, final GestureKeyLayout layout) {
        mLexicon = lexicon;
        mLayout = layout;
        final int size = lexicon.size();
        final int keyCount = layout.getKeyCount();
        final char[] chars = lexicon.getChars();
        byte[] keys = new byte[Math.max(16, chars.length)];
        final int[] keyStarts = new int[size + 1];
        final float[] pathLengths = new float[size];
        final int[] bucketSizes = new int[Math.max(1, keyCount * keyCount)];
        final int[] bucketOfWord = new int[size];
        final HashMap<Integer, Integer> nonAsciiCache = new HashMap<>();
        int keyPos = 0;
        for (int w = 0; w < size; ++w) {
            keyStarts[w] = keyPos;
            final int start = lexicon.getStart(w);
            final int end = lexicon.getEnd(w);
            if (keyPos + (end - start) > keys.length) {
                keys = Arrays.copyOf(keys, Math.max(keys.length * 2, keyPos + (end - start)));
            }
            int wordKeyCount = 0;
            int previousKey = -1;
            boolean valid = keyCount > 0 && keyCount <= 256;
            for (int i = start; i < end && valid; ) {
                final int codePoint = Character.codePointAt(chars, i, end);
                i += Character.charCount(codePoint);
                if (GestureLexicon.isSkippedCharacter(codePoint)) continue;
                final int key;
                if (codePoint < 0x80) {
                    key = layout.getKeyIndexForCodePoint(codePoint);
                } else {
                    final Integer cached = nonAsciiCache.get(codePoint);
                    if (cached != null) {
                        key = cached;
                    } else {
                        key = layout.getKeyIndexForCodePoint(codePoint);
                        nonAsciiCache.put(codePoint, key);
                    }
                }
                if (key < 0) {
                    valid = false;
                } else if (key != previousKey) {
                    keys[keyPos + wordKeyCount++] = (byte) key;
                    previousKey = key;
                }
            }
            if (!valid || wordKeyCount < 2) {
                pathLengths[w] = -1.0f;
                bucketOfWord[w] = -1;
                continue;
            }
            float length = 0.0f;
            for (int k = 1; k < wordKeyCount; ++k) {
                final int a = keys[keyPos + k - 1] & 0xFF;
                final int b = keys[keyPos + k] & 0xFF;
                length += GestureMath.distance(layout.mCenterX[a], layout.mCenterY[a],
                        layout.mCenterX[b], layout.mCenterY[b]);
            }
            pathLengths[w] = length;
            final int first = keys[keyPos] & 0xFF;
            final int last = keys[keyPos + wordKeyCount - 1] & 0xFF;
            final int bucket = first * keyCount + last;
            bucketOfWord[w] = bucket;
            ++bucketSizes[bucket];
            keyPos += wordKeyCount;
        }
        keyStarts[size] = keyPos;
        mKeys = Arrays.copyOf(keys, keyPos);
        mKeyStarts = keyStarts;
        mPathLengths = pathLengths;
        mBuckets = new int[bucketSizes.length][];
        final int[] fill = new int[bucketSizes.length];
        for (int b = 0; b < bucketSizes.length; ++b) {
            if (bucketSizes[b] > 0) mBuckets[b] = new int[bucketSizes[b]];
        }
        final int[][] wordsByProbability = new int[GestureLexicon.MAX_PROBABILITY + 1][];
        final int[] probabilityCounts = new int[GestureLexicon.MAX_PROBABILITY + 1];
        for (int w = 0; w < size; ++w) {
            if (bucketOfWord[w] >= 0) ++probabilityCounts[lexicon.getProbability(w)];
        }
        for (int p = 0; p <= GestureLexicon.MAX_PROBABILITY; ++p) {
            wordsByProbability[p] = new int[probabilityCounts[p]];
            probabilityCounts[p] = 0;
        }
        for (int w = 0; w < size; ++w) {
            if (bucketOfWord[w] < 0) continue;
            final int p = lexicon.getProbability(w);
            wordsByProbability[p][probabilityCounts[p]++] = w;
        }
        for (int p = GestureLexicon.MAX_PROBABILITY; p >= 0; --p) {
            for (final int w : wordsByProbability[p]) {
                final int bucket = bucketOfWord[w];
                mBuckets[bucket][fill[bucket]++] = w;
            }
        }
    }

    public GestureLexicon getLexicon() {
        return mLexicon;
    }

    public GestureKeyLayout getLayout() {
        return mLayout;
    }
}
