package com.android.inputmethod.keyboard.emoji;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.inputmethod.latin.R;

final class SkinTonePicker {
    interface Listener {
        void onSkinToneChosen(String emoji);
    }

    private static final int CELL_WIDTH_DP = 44;
    private static final int CELL_HEIGHT_DP = 48;
    private static final int PADDING_DP = 4;
    private static final int GAP_DP = 6;

    private final FrameLayout mContainer;
    private View mScrim;
    private View mRow;

    SkinTonePicker(final FrameLayout container) {
        mContainer = container;
    }

    boolean isShowing() {
        return mRow != null;
    }

    void dismiss() {
        if (mScrim != null) mContainer.removeView(mScrim);
        if (mRow != null) mContainer.removeView(mRow);
        mScrim = null;
        mRow = null;
    }

    void show(final Rect keyRect, final String[] variants, final int selected,
            final Listener listener) {
        dismiss();
        final Context context = mContainer.getContext();
        final float density = context.getResources().getDisplayMetrics().density;
        final int cellW = Math.round(CELL_WIDTH_DP * density);
        final int cellH = Math.round(CELL_HEIGHT_DP * density);
        final int padding = Math.round(PADDING_DP * density);
        final int gap = Math.round(GAP_DP * density);

        final View scrim = new View(context);
        scrim.setClickable(true);
        scrim.setOnClickListener(v -> dismiss());
        mContainer.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(padding, padding, padding, padding);
        final GradientDrawable background = new GradientDrawable();
        background.setColor(context.getColor(R.color.glass_key_preview));
        background.setCornerRadius(12 * density);
        row.setBackground(background);
        row.setElevation(8 * density);
        row.setContentDescription(context.getString(R.string.emoji_skin_tones));
        for (int i = 0; i < variants.length; i++) {
            final String emoji = variants[i];
            final TextView cell = new TextView(context);
            cell.setText(emoji);
            cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
            cell.setGravity(Gravity.CENTER);
            cell.setIncludeFontPadding(false);
            cell.setBackground(cellBackground(context, density, i == selected));
            cell.setContentDescription(emoji);
            cell.setHapticFeedbackEnabled(false);
            cell.setSoundEffectsEnabled(false);
            cell.setOnClickListener(v -> {
                dismiss();
                listener.onSkinToneChosen(emoji);
            });
            row.addView(cell, new LinearLayout.LayoutParams(cellW, cellH));
        }

        final int rowW = cellW * variants.length + 2 * padding;
        final int rowH = cellH + 2 * padding;
        final int containerW = mContainer.getWidth();
        int x = keyRect.centerX() - rowW / 2;
        x = Math.max(gap, Math.min(x, containerW - rowW - gap));
        int y = keyRect.top - rowH - gap;
        if (y < 0) {
            y = keyRect.bottom + gap;
        }
        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(rowW, rowH);
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.leftMargin = Math.max(0, x);
        lp.topMargin = Math.max(0, y);
        mContainer.addView(row, lp);
        mScrim = scrim;
        mRow = row;
    }

    private static StateListDrawable cellBackground(final Context context, final float density,
            final boolean selected) {
        final StateListDrawable states = new StateListDrawable();
        final GradientDrawable chip = new GradientDrawable();
        chip.setColor(context.getColor(R.color.glass_chip_pressed));
        chip.setCornerRadius(10 * density);
        states.addState(new int[] { android.R.attr.state_pressed }, chip);
        if (selected) {
            states.addState(new int[0], chip);
        }
        return states;
    }
}
