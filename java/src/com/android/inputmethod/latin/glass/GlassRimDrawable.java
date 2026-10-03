package com.android.inputmethod.latin.glass;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

final class GlassRimDrawable extends Drawable {
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mRect = new RectF();
    private final float mRadius;
    private final int mColor;

    GlassRimDrawable(final int color, final float strokeWidth, final float radius) {
        mColor = color;
        mRadius = radius;
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(strokeWidth);
    }

    @Override
    protected void onBoundsChange(final Rect bounds) {
        final float half = mPaint.getStrokeWidth() / 2f;
        mRect.set(bounds.left + half, bounds.top + half, bounds.right - half,
                bounds.bottom - half);
        final float fadeEnd = bounds.top + mRadius * 2.2f;
        mPaint.setShader(new LinearGradient(0, bounds.top, 0, fadeEnd,
                new int[] { mColor, mColor, Color.TRANSPARENT },
                new float[] { 0f, 0.35f, 1f }, Shader.TileMode.CLAMP));
    }

    @Override
    public void draw(final Canvas canvas) {
        canvas.drawRoundRect(mRect, mRadius, mRadius, mPaint);
    }

    @Override
    public void setAlpha(final int alpha) {
        mPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(final ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
