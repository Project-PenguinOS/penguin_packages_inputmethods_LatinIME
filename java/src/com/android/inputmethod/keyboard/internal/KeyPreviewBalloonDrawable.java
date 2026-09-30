package com.android.inputmethod.keyboard.internal;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public final class KeyPreviewBalloonDrawable extends Drawable {
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mPath = new Path();
    private final float mHeadRadius;
    private final float mFootRadius;
    private final float mShadowRadius;

    private int mStemLeft;
    private int mStemWidth;
    private int mHeadHeight;
    private int mNeckHeight;

    public KeyPreviewBalloonDrawable(final int color, final int shadowColor, final float headRadius,
            final float footRadius, final float shadowRadius) {
        mPaint.setColor(color);
        mPaint.setStyle(Paint.Style.FILL);
        mShadowRadius = shadowRadius;
        if (shadowRadius > 0 && (shadowColor >>> 24) != 0) {
            mPaint.setShadowLayer(shadowRadius, 0, shadowRadius / 3f, shadowColor);
        }
        mHeadRadius = headRadius;
        mFootRadius = footRadius;
    }

    public void setGeometry(final int stemLeft, final int stemWidth, final int headHeight,
            final int neckHeight) {
        mStemLeft = stemLeft;
        mStemWidth = stemWidth;
        mHeadHeight = headHeight;
        mNeckHeight = neckHeight;
        buildPath(getBounds());
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(final Rect bounds) {
        buildPath(bounds);
    }

    private void buildPath(final Rect b) {
        mPath.reset();
        if (b.isEmpty() || mStemWidth <= 0) return;
        final float inset = mShadowRadius * 0.5f;
        final float left = b.left + inset;
        final float right = b.right - inset;
        final float top = b.top + inset;
        final float bottom = b.bottom;
        final float headBottom = top + mHeadHeight;
        final float neckBottom = headBottom + mNeckHeight;
        final float stemL = Math.max(left, b.left + mStemLeft);
        final float stemR = Math.min(right, b.left + mStemLeft + mStemWidth);
        final float r = Math.min(mHeadRadius, (right - left) / 2f);
        final float fr = Math.min(mFootRadius, (stemR - stemL) / 2f);

        mPath.moveTo(left + r, top);
        mPath.lineTo(right - r, top);
        mPath.quadTo(right, top, right, top + r);
        mPath.lineTo(right, headBottom - r * 0.5f);
        mPath.cubicTo(right, headBottom + mNeckHeight * 0.35f,
                stemR, headBottom + mNeckHeight * 0.25f,
                stemR, neckBottom);
        mPath.lineTo(stemR, bottom - fr);
        mPath.quadTo(stemR, bottom, stemR - fr, bottom);
        mPath.lineTo(stemL + fr, bottom);
        mPath.quadTo(stemL, bottom, stemL, bottom - fr);
        mPath.lineTo(stemL, neckBottom);
        mPath.cubicTo(stemL, headBottom + mNeckHeight * 0.25f,
                left, headBottom + mNeckHeight * 0.35f,
                left, headBottom - r * 0.5f);
        mPath.lineTo(left, top + r);
        mPath.quadTo(left, top, left + r, top);
        mPath.close();
    }

    @Override
    public void draw(final Canvas canvas) {
        canvas.drawPath(mPath, mPaint);
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
