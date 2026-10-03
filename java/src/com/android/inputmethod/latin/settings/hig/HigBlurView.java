package com.android.inputmethod.latin.settings.hig;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RecordingCanvas;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import com.android.inputmethod.latin.R;

public final class HigBlurView extends FrameLayout {

    private final RenderNode mBlurNode = new RenderNode("HigBlurView");
    private final int[] mLocation = new int[2];
    private final int[] mTargetLocation = new int[2];
    @Nullable private HigBlurTarget mTarget;
    @ColorInt private int mOverlayColor;
    @ColorInt private int mFallbackColor;
    private float mRadius;

    public HigBlurView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        mOverlayColor = context.getColor(R.color.glass_bar);
        mFallbackColor = (context.getColor(R.color.hig_grouped_background) & 0x00FFFFFF)
                | 0xF2000000;
    }

    public HigBlurView setupWith(HigBlurTarget target, float radiusDp) {
        mTarget = target;
        target.setOnContentDrawnListener(this::invalidate);
        mRadius = radiusDp * getResources().getDisplayMetrics().density;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && mRadius > 0) {
            mBlurNode.setRenderEffect(
                    RenderEffect.createBlurEffect(mRadius, mRadius, Shader.TileMode.CLAMP));
        }
        invalidate();
        return this;
    }

    @Override
    public void draw(Canvas canvas) {
        RenderNode content = mTarget != null ? mTarget.getNode() : null;
        if (content != null && canvas.isHardwareAccelerated()
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getLocationInWindow(mLocation);
            mTarget.getLocationInWindow(mTargetLocation);
            mBlurNode.setPosition(0, 0, getWidth(), getHeight());
            RecordingCanvas recording = mBlurNode.beginRecording();
            try {
                recording.translate(mTargetLocation[0] - mLocation[0],
                        mTargetLocation[1] - mLocation[1]);
                recording.drawRenderNode(content);
            } finally {
                mBlurNode.endRecording();
            }
            canvas.save();
            canvas.clipRect(0, 0, getWidth(), getHeight());
            canvas.drawRenderNode(mBlurNode);
            canvas.restore();
            canvas.drawColor(mOverlayColor);
        } else {
            canvas.drawColor(mFallbackColor);
        }
        super.draw(canvas);
    }
}
