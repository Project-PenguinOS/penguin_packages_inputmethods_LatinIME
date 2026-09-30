package com.android.inputmethod.latin.gesture;

public final class GestureMath {
    private GestureMath() {
    }

    public static float pathLength(final float[] xs, final float[] ys, final int count) {
        float length = 0.0f;
        for (int i = 1; i < count; ++i) {
            length += distance(xs[i - 1], ys[i - 1], xs[i], ys[i]);
        }
        return length;
    }

    public static float resample(final float[] xs, final float[] ys, final int count,
            final int n, final float[] outX, final float[] outY) {
        final float total = pathLength(xs, ys, count);
        if (count == 1 || total <= 0.0f) {
            for (int i = 0; i < n; ++i) {
                outX[i] = xs[0];
                outY[i] = ys[0];
            }
            return 0.0f;
        }
        final float step = total / (n - 1);
        outX[0] = xs[0];
        outY[0] = ys[0];
        int out = 1;
        float segmentStart = 0.0f;
        float nextTarget = step;
        for (int i = 1; i < count && out < n - 1; ++i) {
            final float x0 = xs[i - 1];
            final float y0 = ys[i - 1];
            final float segmentLength = distance(x0, y0, xs[i], ys[i]);
            if (segmentLength <= 0.0f) {
                continue;
            }
            final float segmentEnd = segmentStart + segmentLength;
            while (nextTarget <= segmentEnd && out < n - 1) {
                final float t = (nextTarget - segmentStart) / segmentLength;
                outX[out] = x0 + t * (xs[i] - x0);
                outY[out] = y0 + t * (ys[i] - y0);
                ++out;
                nextTarget += step;
            }
            segmentStart = segmentEnd;
        }
        while (out < n) {
            outX[out] = xs[count - 1];
            outY[out] = ys[count - 1];
            ++out;
        }
        return total;
    }

    public static float meanDistance(final float[] ax, final float[] ay, final float[] bx,
            final float[] by, final int n) {
        float sum = 0.0f;
        for (int i = 0; i < n; ++i) {
            sum += distance(ax[i], ay[i], bx[i], by[i]);
        }
        return sum / n;
    }

    public static void normalizeShape(final float[] xs, final float[] ys, final int n,
            final float minScale, final float[] outX, final float[] outY) {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float cx = 0.0f;
        float cy = 0.0f;
        for (int i = 0; i < n; ++i) {
            final float x = xs[i];
            final float y = ys[i];
            cx += x;
            cy += y;
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }
        cx /= n;
        cy /= n;
        final float scale = Math.max(minScale, Math.max(maxX - minX, maxY - minY));
        final float inv = 1.0f / scale;
        for (int i = 0; i < n; ++i) {
            outX[i] = (xs[i] - cx) * inv;
            outY[i] = (ys[i] - cy) * inv;
        }
    }

    public static float squaredDistanceToRect(final float x, final float y, final float left,
            final float top, final float right, final float bottom) {
        final float dx = x < left ? left - x : (x > right ? x - right : 0.0f);
        final float dy = y < top ? top - y : (y > bottom ? y - bottom : 0.0f);
        return dx * dx + dy * dy;
    }

    public static float distance(final float x0, final float y0, final float x1,
            final float y1) {
        final float dx = x1 - x0;
        final float dy = y1 - y0;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
