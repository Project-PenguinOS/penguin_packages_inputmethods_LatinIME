package com.android.inputmethod.latin.glass;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Window;
import android.view.WindowManager;

import com.android.inputmethod.latin.R;

import java.util.function.Consumer;

public final class GlassWindow {
    private final Context mContext;
    private final Window mWindow;
    private final WindowManager mWindowManager;
    private final Consumer<Boolean> mBlurListener = this::onCrossWindowBlurChanged;
    private boolean mEnabled;
    private boolean mBlurSupported = true;
    private boolean mListening;

    public GlassWindow(final Context context, final Window window) {
        mContext = context;
        mWindow = window;
        mWindowManager = context.getSystemService(WindowManager.class);
    }

    public void setEnabled(final boolean enabled) {
        mEnabled = enabled;
        if (enabled && !mListening && mWindowManager != null) {
            mWindowManager.addCrossWindowBlurEnabledListener(mContext.getMainExecutor(),
                    mBlurListener);
            mListening = true;
        } else if (!enabled && mListening) {
            mWindowManager.removeCrossWindowBlurEnabledListener(mBlurListener);
            mListening = false;
        }
        apply();
    }

    public void release() {
        if (mListening) {
            mWindowManager.removeCrossWindowBlurEnabledListener(mBlurListener);
            mListening = false;
        }
    }

    private void onCrossWindowBlurChanged(final boolean supported) {
        mBlurSupported = supported;
        apply();
    }

    private void apply() {
        if (!mEnabled) {
            mWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            mWindow.setBackgroundBlurRadius(0);
            return;
        }
        final boolean blur = mBlurSupported && mWindowManager != null
                && mWindowManager.isCrossWindowBlurEnabled();
        final GradientDrawable sheet = new GradientDrawable();
        sheet.setShape(GradientDrawable.RECTANGLE);
        sheet.setCornerRadius(
                mContext.getResources().getDimension(R.dimen.glass_sheet_corner_radius));
        sheet.setColor(mContext.getColor(
                blur ? R.color.glass_sheet_tint : R.color.glass_sheet_tint_opaque));
        sheet.setStroke(mContext.getResources().getDimensionPixelSize(
                R.dimen.glass_sheet_rim_width), mContext.getColor(R.color.glass_sheet_rim));
        mWindow.setBackgroundDrawable(sheet);
        mWindow.setBackgroundBlurRadius(blur ? mContext.getResources().getDimensionPixelSize(
                R.dimen.glass_sheet_blur_radius) : 0);
    }
}
