package com.android.inputmethod.latin.settings.hig;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RecordingCanvas;
import android.graphics.RenderNode;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

public final class HigBlurTarget extends FrameLayout {

    private final RenderNode mNode = new RenderNode("HigBlurTarget");
    private boolean mRecorded;
    @Nullable private Runnable mOnContentDrawn;

    public HigBlurTarget(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void draw(Canvas canvas) {
        if (!canvas.isHardwareAccelerated()) {
            mRecorded = false;
            super.draw(canvas);
            return;
        }
        mNode.setPosition(0, 0, getWidth(), getHeight());
        RecordingCanvas recording = mNode.beginRecording();
        try {
            super.draw(recording);
        } finally {
            mNode.endRecording();
        }
        mRecorded = true;
        canvas.drawRenderNode(mNode);
        if (mOnContentDrawn != null) {
            mOnContentDrawn.run();
        }
    }

    void setOnContentDrawnListener(@Nullable Runnable listener) {
        mOnContentDrawn = listener;
    }

    @Nullable
    RenderNode getNode() {
        return mRecorded ? mNode : null;
    }
}
