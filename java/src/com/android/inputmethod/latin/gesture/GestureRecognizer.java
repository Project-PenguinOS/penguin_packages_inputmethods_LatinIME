package com.android.inputmethod.latin.gesture;

public final class GestureRecognizer {
    public static final int SAMPLE_COUNT = 48;

    private static final float SIGMA_LOCATION = 0.55f;
    private static final float SIGMA_SHAPE = 0.12f;
    private static final float SIGMA_ALIGNMENT = 0.42f;
    private static final float SIGMA_ENDPOINTS = 0.6f;
    private static final float LANGUAGE_WEIGHT = 8.0f;

    private static final float START_KEY_RADIUS = 0.45f;
    private static final float END_KEY_RADIUS = 0.7f;
    private static final float MAX_ALIGNMENT_RMS = 1.1f;
    private static final float MAX_LOCATION_DISTANCE = 1.6f;
    private static final float MIN_STROKE_LENGTH = 0.3f;
    private static final float MAX_COST_MARGIN = 12.0f;
    private static final float MIN_SHAPE_SCALE = 1.0f;

    private static final float LOCATION_WEIGHT = 1.0f / (SIGMA_LOCATION * SIGMA_LOCATION);
    private static final float SHAPE_WEIGHT = 1.0f / (SIGMA_SHAPE * SIGMA_SHAPE);
    private static final float ALIGNMENT_WEIGHT = 1.0f / (SIGMA_ALIGNMENT * SIGMA_ALIGNMENT);
    private static final float ENDPOINT_WEIGHT = 1.0f / (SIGMA_ENDPOINTS * SIGMA_ENDPOINTS);

    public static final class Candidate {
        public CompiledLexicon mLexicon;
        public int mWordIndex;
        public float mCost;

        public String getWord() {
            return mLexicon.mLexicon.getWord(mWordIndex);
        }
    }

    private float[] mStrokeX = new float[256];
    private float[] mStrokeY = new float[256];
    private final float[] mSampleX = new float[SAMPLE_COUNT];
    private final float[] mSampleY = new float[SAMPLE_COUNT];
    private final float[] mShapeX = new float[SAMPLE_COUNT];
    private final float[] mShapeY = new float[SAMPLE_COUNT];
    private final float[] mIdealX = new float[SAMPLE_COUNT];
    private final float[] mIdealY = new float[SAMPLE_COUNT];
    private final float[] mIdealShapeX = new float[SAMPLE_COUNT];
    private final float[] mIdealShapeY = new float[SAMPLE_COUNT];
    private final float[] mPathX = new float[GestureLexicon.MAX_WORD_LENGTH];
    private final float[] mPathY = new float[GestureLexicon.MAX_WORD_LENGTH];
    private final float[] mDtwPrevious = new float[SAMPLE_COUNT];
    private final float[] mDtwCurrent = new float[SAMPLE_COUNT];
    private final boolean[] mStartKeys = new boolean[256];
    private final boolean[] mEndKeys = new boolean[256];
    private final float[] mKeyToStroke = new float[256];

    private Candidate[] mResults = new Candidate[0];
    private int mResultCount;
    private int mEvaluatedCount;

    public int getEvaluatedCount() {
        return mEvaluatedCount;
    }

    public Candidate[] recognize(final int[] xs, final int[] ys, final int count,
            final CompiledLexicon[] lexicons, final int maxResults) {
        mResultCount = 0;
        mEvaluatedCount = 0;
        if (mResults.length < maxResults) {
            final Candidate[] results = new Candidate[maxResults];
            System.arraycopy(mResults, 0, results, 0, mResults.length);
            for (int i = mResults.length; i < maxResults; ++i) results[i] = new Candidate();
            mResults = results;
        }
        if (count < 2 || lexicons.length == 0 || maxResults <= 0) {
            return new Candidate[0];
        }
        final GestureKeyLayout layout = lexicons[0].mLayout;
        final int keyCount = layout.getKeyCount();
        if (keyCount == 0 || keyCount > 256) {
            return new Candidate[0];
        }

        if (mStrokeX.length < count) {
            mStrokeX = new float[count];
            mStrokeY = new float[count];
        }
        final float inv = 1.0f / layout.mKeyWidth;
        for (int i = 0; i < count; ++i) {
            mStrokeX[i] = xs[i] * inv;
            mStrokeY[i] = ys[i] * inv;
        }
        final float strokeLength = GestureMath.resample(mStrokeX, mStrokeY, count,
                SAMPLE_COUNT, mSampleX, mSampleY);
        if (strokeLength < MIN_STROKE_LENGTH) {
            return new Candidate[0];
        }
        GestureMath.normalizeShape(mSampleX, mSampleY, SAMPLE_COUNT, MIN_SHAPE_SCALE,
                mShapeX, mShapeY);
        final float startX = mStrokeX[0];
        final float startY = mStrokeY[0];
        final float endX = mStrokeX[count - 1];
        final float endY = mStrokeY[count - 1];
        findKeysNear(layout, startX, startY, START_KEY_RADIUS, mStartKeys);
        findKeysNear(layout, endX, endY, END_KEY_RADIUS, mEndKeys);

        for (int k = 0; k < keyCount; ++k) {
            float best = Float.MAX_VALUE;
            final float x = layout.mCenterX[k];
            final float y = layout.mCenterY[k];
            for (int i = 0; i < SAMPLE_COUNT; ++i) {
                final float d = squaredDistance(x, y, mSampleX[i], mSampleY[i]);
                if (d < best) best = d;
            }
            mKeyToStroke[k] = best;
        }

        final float maxPathLength = (strokeLength + 1.2f) * 1.7f;
        final float minPathLength = strokeLength * 0.4f - 1.5f;
        final float maxAlignmentSum = MAX_ALIGNMENT_RMS * MAX_ALIGNMENT_RMS;

        for (final CompiledLexicon lexicon : lexicons) {
            if (lexicon.mLayout != layout) continue;
            final GestureLexicon words = lexicon.mLexicon;
            for (int first = 0; first < keyCount; ++first) {
                if (!mStartKeys[first]) continue;
                for (int last = 0; last < keyCount; ++last) {
                    if (!mEndKeys[last]) continue;
                    final int[] bucket = lexicon.mBuckets[first * keyCount + last];
                    if (bucket == null) continue;
                    for (final int w : bucket) {
                        final float pathLength = lexicon.mPathLengths[w];
                        if (pathLength > maxPathLength || pathLength < minPathLength) continue;
                        final int keyStart = lexicon.mKeyStarts[w];
                        final int wordKeyCount = lexicon.mKeyStarts[w + 1] - keyStart;
                        float alignmentBound = 0.0f;
                        for (int k = 0; k < wordKeyCount; ++k) {
                            alignmentBound += mKeyToStroke[lexicon.mKeys[keyStart + k] & 0xFF];
                        }
                        alignmentBound /= wordKeyCount;
                        if (alignmentBound > maxAlignmentSum) continue;
                        final int firstKey = lexicon.mKeys[keyStart] & 0xFF;
                        final int lastKey = lexicon.mKeys[keyStart + wordKeyCount - 1] & 0xFF;
                        final float languageCost = LANGUAGE_WEIGHT
                                * (GestureLexicon.MAX_PROBABILITY - words.getProbability(w))
                                / GestureLexicon.MAX_PROBABILITY;
                        final float endpointCost = ENDPOINT_WEIGHT * (
                                squaredDistance(startX, startY, layout.mCenterX[firstKey],
                                        layout.mCenterY[firstKey])
                                + squaredDistance(endX, endY, layout.mCenterX[lastKey],
                                        layout.mCenterY[lastKey]));
                        final float alignmentCostBound = ALIGNMENT_WEIGHT * alignmentBound;
                        float cost = languageCost + endpointCost;
                        if (isWorseThanAllResults(cost + alignmentCostBound, maxResults)) {
                            continue;
                        }
                        for (int k = 0; k < wordKeyCount; ++k) {
                            final int key = lexicon.mKeys[keyStart + k] & 0xFF;
                            mPathX[k] = layout.mCenterX[key];
                            mPathY[k] = layout.mCenterY[key];
                        }
                        GestureMath.resample(mPathX, mPathY, wordKeyCount, SAMPLE_COUNT,
                                mIdealX, mIdealY);
                        final float location = GestureMath.meanDistance(mSampleX, mSampleY,
                                mIdealX, mIdealY, SAMPLE_COUNT);
                        if (location > MAX_LOCATION_DISTANCE) continue;
                        cost += LOCATION_WEIGHT * location * location;
                        if (isWorseThanAllResults(cost + alignmentCostBound, maxResults)) {
                            continue;
                        }
                        GestureMath.normalizeShape(mIdealX, mIdealY, SAMPLE_COUNT,
                                MIN_SHAPE_SCALE, mIdealShapeX, mIdealShapeY);
                        final float shape = GestureMath.meanDistance(mShapeX, mShapeY,
                                mIdealShapeX, mIdealShapeY, SAMPLE_COUNT);
                        cost += SHAPE_WEIGHT * shape * shape;
                        if (isWorseThanAllResults(cost + alignmentCostBound, maxResults)) {
                            continue;
                        }
                        final float alignment = alignmentMeanSquare(wordKeyCount,
                                maxAlignmentSum);
                        if (alignment > maxAlignmentSum) continue;
                        cost += ALIGNMENT_WEIGHT * alignment;
                        ++mEvaluatedCount;
                        insertResult(lexicon, w, cost, maxResults);
                    }
                }
            }
        }
        int resultCount = 0;
        while (resultCount < mResultCount
                && mResults[resultCount].mCost <= mResults[0].mCost + MAX_COST_MARGIN) {
            ++resultCount;
        }
        final Candidate[] results = new Candidate[resultCount];
        for (int i = 0; i < resultCount; ++i) {
            final Candidate candidate = new Candidate();
            candidate.mLexicon = mResults[i].mLexicon;
            candidate.mWordIndex = mResults[i].mWordIndex;
            candidate.mCost = mResults[i].mCost;
            results[i] = candidate;
        }
        return results;
    }

    private static void findKeysNear(final GestureKeyLayout layout, final float x,
            final float y, final float radius, final boolean[] out) {
        final int keyCount = layout.getKeyCount();
        final float radiusSquared = radius * radius;
        int nearest = -1;
        float nearestDistance = Float.MAX_VALUE;
        boolean found = false;
        for (int k = 0; k < keyCount; ++k) {
            final float d = GestureMath.squaredDistanceToRect(x, y, layout.mLeft[k],
                    layout.mTop[k], layout.mRight[k], layout.mBottom[k]);
            out[k] = d <= radiusSquared;
            found |= out[k];
            if (d < nearestDistance) {
                nearestDistance = d;
                nearest = k;
            }
        }
        if (!found && nearest >= 0) {
            out[nearest] = true;
        }
    }

    private float alignmentMeanSquare(final int keyCount, final float maxMeanSquare) {
        final float limit = maxMeanSquare * keyCount;
        float[] previous = mDtwPrevious;
        float[] current = mDtwCurrent;
        float running = Float.MAX_VALUE;
        final float x0 = mPathX[0];
        final float y0 = mPathY[0];
        for (int i = 0; i < SAMPLE_COUNT; ++i) {
            final float d = squaredDistance(x0, y0, mSampleX[i], mSampleY[i]);
            if (d < running) running = d;
            previous[i] = running;
        }
        for (int k = 1; k < keyCount; ++k) {
            final float x = mPathX[k];
            final float y = mPathY[k];
            running = Float.MAX_VALUE;
            for (int i = 0; i < SAMPLE_COUNT; ++i) {
                final float d = squaredDistance(x, y, mSampleX[i], mSampleY[i]) + previous[i];
                if (d < running) running = d;
                current[i] = running;
            }
            if (running > limit) {
                return Float.MAX_VALUE;
            }
            final float[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[SAMPLE_COUNT - 1] / keyCount;
    }

    private boolean isWorseThanAllResults(final float cost, final int maxResults) {
        if (mResultCount == 0) return false;
        if (cost > mResults[0].mCost + MAX_COST_MARGIN) return true;
        return mResultCount >= maxResults && cost >= mResults[mResultCount - 1].mCost;
    }

    private void insertResult(final CompiledLexicon lexicon, final int wordIndex,
            final float cost, final int maxResults) {
        if (isWorseThanAllResults(cost, maxResults)) return;
        int position = Math.min(mResultCount, maxResults - 1);
        final Candidate slot = mResults[position];
        while (position > 0 && mResults[position - 1].mCost > cost) {
            mResults[position] = mResults[position - 1];
            --position;
        }
        slot.mLexicon = lexicon;
        slot.mWordIndex = wordIndex;
        slot.mCost = cost;
        mResults[position] = slot;
        if (mResultCount < maxResults) ++mResultCount;
    }

    private static float squaredDistance(final float x0, final float y0, final float x1,
            final float y1) {
        final float dx = x1 - x0;
        final float dy = y1 - y0;
        return dx * dx + dy * dy;
    }
}
