package com.android.inputmethod.latin.gesture;

import java.util.Arrays;

public final class GestureLexicon {
    public static final int MAX_WORD_LENGTH = 32;
    public static final int MAX_PROBABILITY = 255;

    private final char[] mChars;
    private final int[] mOffsets;
    private final short[] mProbabilities;
    private final int mSize;

    private GestureLexicon(final char[] chars, final int[] offsets,
            final short[] probabilities, final int size) {
        mChars = chars;
        mOffsets = offsets;
        mProbabilities = probabilities;
        mSize = size;
    }

    public int size() {
        return mSize;
    }

    public String getWord(final int index) {
        return new String(mChars, mOffsets[index], mOffsets[index + 1] - mOffsets[index]);
    }

    public int getProbability(final int index) {
        return mProbabilities[index];
    }

    int getStart(final int index) {
        return mOffsets[index];
    }

    int getEnd(final int index) {
        return mOffsets[index + 1];
    }

    char[] getChars() {
        return mChars;
    }

    public static boolean isGesturableWord(final String word) {
        final int length = word.length();
        if (length < 2 || length > MAX_WORD_LENGTH) {
            return false;
        }
        int letters = 0;
        for (int i = 0; i < length; ) {
            final int codePoint = word.codePointAt(i);
            if (Character.isLetter(codePoint)) {
                ++letters;
            } else if (!isSkippedCharacter(codePoint)) {
                return false;
            }
            i += Character.charCount(codePoint);
        }
        return letters >= 2;
    }

    public static boolean isSkippedCharacter(final int codePoint) {
        return codePoint == '\'' || codePoint == '-' || codePoint == '’'
                || Character.getType(codePoint) == Character.NON_SPACING_MARK;
    }

    public static final class Builder {
        private final StringBuilder mChars = new StringBuilder();
        private int[] mOffsets = new int[1024];
        private short[] mProbabilities = new short[1024];
        private int mSize;

        public boolean add(final String word, final int probability) {
            if (probability < 0 || !isGesturableWord(word)) {
                return false;
            }
            if (mSize + 1 >= mOffsets.length) {
                mOffsets = Arrays.copyOf(mOffsets, mOffsets.length * 2);
                mProbabilities = Arrays.copyOf(mProbabilities, mProbabilities.length * 2);
            }
            mOffsets[mSize] = mChars.length();
            mProbabilities[mSize] = (short) Math.min(probability, MAX_PROBABILITY);
            mChars.append(word);
            ++mSize;
            return true;
        }

        public int size() {
            return mSize;
        }

        public GestureLexicon build(final int maxWords) {
            final int[] offsets = Arrays.copyOf(mOffsets, mSize + 1);
            offsets[mSize] = mChars.length();
            final char[] allChars = new char[mChars.length()];
            mChars.getChars(0, allChars.length, allChars, 0);
            if (mSize <= maxWords) {
                return new GestureLexicon(allChars, offsets,
                        Arrays.copyOf(mProbabilities, mSize), mSize);
            }
            final int[] histogram = new int[MAX_PROBABILITY + 1];
            for (int i = 0; i < mSize; ++i) {
                ++histogram[mProbabilities[i]];
            }
            int threshold = MAX_PROBABILITY;
            int kept = 0;
            while (threshold > 0 && kept + histogram[threshold] <= maxWords) {
                kept += histogram[threshold];
                --threshold;
            }
            int roomAtThreshold = maxWords - kept;
            final StringBuilder chars = new StringBuilder();
            final int[] newOffsets = new int[maxWords + 1];
            final short[] newProbabilities = new short[maxWords];
            int size = 0;
            for (int i = 0; i < mSize && size < maxWords; ++i) {
                final int probability = mProbabilities[i];
                if (probability < threshold) continue;
                if (probability == threshold) {
                    if (roomAtThreshold <= 0) continue;
                    --roomAtThreshold;
                }
                newOffsets[size] = chars.length();
                newProbabilities[size] = (short) probability;
                chars.append(allChars, offsets[i], offsets[i + 1] - offsets[i]);
                ++size;
            }
            newOffsets[size] = chars.length();
            final char[] keptChars = new char[chars.length()];
            chars.getChars(0, keptChars.length, keptChars, 0);
            return new GestureLexicon(keptChars, Arrays.copyOf(newOffsets, size + 1),
                    Arrays.copyOf(newProbabilities, size), size);
        }
    }
}
