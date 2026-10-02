package com.android.inputmethod.keyboard.emoji;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.inputmethod.latin.R;
import com.android.inputmethod.latin.utils.Fonts;

import java.util.List;

final class EmojiSearchView extends LinearLayout {
    interface Callback {
        void onCloseClicked(View view);
        void onClearClicked(View view);
        void onResultClicked(View view, String emoji);
    }

    private final Callback mCallback;
    private final TextView mQueryView;
    private final View mCaret;
    private final ImageButton mClearButton;
    private final HorizontalScrollView mResultsScroller;
    private final LinearLayout mResults;
    private final TextView mEmptyView;
    private ObjectAnimator mCaretBlink;

    EmojiSearchView(final Context context, final Callback callback) {
        super(context);
        mCallback = callback;
        setOrientation(VERTICAL);
        final Resources res = context.getResources();
        final int rowHeight = res.getDimensionPixelSize(R.dimen.emoji_search_row_height);
        final int fieldHeight = res.getDimensionPixelSize(R.dimen.emoji_search_field_height);
        final ColorStateList secondary =
                ColorStateList.valueOf(context.getColor(R.color.glass_suggestion_secondary));

        final LinearLayout fieldRow = new LinearLayout(context);
        fieldRow.setOrientation(HORIZONTAL);
        fieldRow.setGravity(Gravity.CENTER_VERTICAL);
        fieldRow.setPadding(0, 0, dp(8), 0);
        final ImageButton close = iconButton(context, R.drawable.ic_f7_xmark,
                ColorStateList.valueOf(context.getColor(R.color.glass_icon)),
                R.string.emoji_search_close);
        close.setOnClickListener(mCallback::onCloseClicked);
        fieldRow.addView(close, new LayoutParams(rowHeight, rowHeight));

        final LinearLayout field = new LinearLayout(context);
        field.setOrientation(HORIZONTAL);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setPadding(dp(8), 0, 0, 0);
        field.setBackgroundResource(R.drawable.emoji_search_field_background);
        final ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.ic_f7_search);
        icon.setImageTintList(secondary);
        field.addView(icon, new LayoutParams(dp(18), dp(18)));
        mQueryView = new TextView(context);
        mQueryView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                res.getDimension(R.dimen.emoji_search_text_size));
        mQueryView.setTypeface(Fonts.regular());
        mQueryView.setSingleLine(true);
        mQueryView.setEllipsize(TextUtils.TruncateAt.START);
        mQueryView.setIncludeFontPadding(false);
        mQueryView.setMaxWidth(res.getDisplayMetrics().widthPixels * 3 / 5);
        final LayoutParams queryLp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        queryLp.leftMargin = dp(6);
        field.addView(mQueryView, queryLp);
        mCaret = new View(context);
        mCaret.setBackgroundColor(context.getColor(R.color.glass_accent));
        final LayoutParams caretLp = new LayoutParams(dp(2), dp(20));
        caretLp.leftMargin = dp(1);
        field.addView(mCaret, caretLp);
        field.addView(new View(context), new LayoutParams(0, 1, 1f));
        mClearButton = iconButton(context, R.drawable.ic_f7_xmark_circle_fill, secondary,
                R.string.emoji_search_clear);
        mClearButton.setOnClickListener(mCallback::onClearClicked);
        field.addView(mClearButton, new LayoutParams(fieldHeight, fieldHeight));
        fieldRow.addView(field, new LayoutParams(0, fieldHeight, 1f));
        addView(fieldRow, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowHeight));

        final FrameLayout resultsFrame = new FrameLayout(context);
        mResultsScroller = new HorizontalScrollView(context);
        mResultsScroller.setHorizontalScrollBarEnabled(false);
        mResultsScroller.setOverScrollMode(OVER_SCROLL_NEVER);
        mResults = new LinearLayout(context);
        mResults.setOrientation(HORIZONTAL);
        mResults.setPadding(dp(4), 0, dp(4), 0);
        mResultsScroller.addView(mResults, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        resultsFrame.addView(mResultsScroller, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mEmptyView = new TextView(context);
        mEmptyView.setText(R.string.emoji_search_no_results);
        mEmptyView.setTextColor(context.getColor(R.color.glass_suggestion_secondary));
        mEmptyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        mEmptyView.setTypeface(Fonts.regular());
        mEmptyView.setGravity(Gravity.CENTER);
        mEmptyView.setVisibility(GONE);
        resultsFrame.addView(mEmptyView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addView(resultsFrame, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowHeight));
        setQuery("");
    }

    private int dp(final float dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private static ImageButton iconButton(final Context context, final int iconRes,
            final ColorStateList tint, final int descriptionRes) {
        final ImageButton b = new ImageButton(context);
        b.setImageResource(iconRes);
        b.setImageTintList(tint);
        b.setScaleType(ImageView.ScaleType.CENTER);
        b.setBackgroundResource(R.drawable.glass_icon_button_background);
        b.setContentDescription(context.getString(descriptionRes));
        b.setPadding(0, 0, 0, 0);
        b.setHapticFeedbackEnabled(false);
        b.setSoundEffectsEnabled(false);
        return b;
    }

    void setQuery(final CharSequence query) {
        final boolean empty = TextUtils.isEmpty(query);
        mQueryView.setText(empty ? getContext().getString(R.string.emoji_search_hint) : query);
        mQueryView.setTextColor(getContext().getColor(empty
                ? R.color.glass_suggestion_secondary : R.color.glass_key_text));
        final ViewGroup field = (ViewGroup) mQueryView.getParent();
        field.removeView(mCaret);
        field.addView(mCaret, field.indexOfChild(mQueryView) + (empty ? 0 : 1));
        mClearButton.setVisibility(empty ? INVISIBLE : VISIBLE);
    }

    void setResults(final List<String> emoji, final boolean showEmptyMessage) {
        mResults.removeAllViews();
        final Resources res = getResources();
        final int width = res.getDimensionPixelSize(R.dimen.emoji_search_result_width);
        final float textSize = res.getDimension(R.dimen.emoji_search_result_text_size);
        for (final String e : emoji) {
            final TextView cell = new TextView(getContext());
            cell.setText(e);
            cell.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            cell.setGravity(Gravity.CENTER);
            cell.setIncludeFontPadding(false);
            cell.setBackground(cellBackground());
            cell.setContentDescription(e);
            cell.setHapticFeedbackEnabled(false);
            cell.setSoundEffectsEnabled(false);
            cell.setOnClickListener(v -> mCallback.onResultClicked(v, e));
            mResults.addView(cell, new LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        mResultsScroller.scrollTo(0, 0);
        mEmptyView.setVisibility(emoji.isEmpty() && showEmptyMessage ? VISIBLE : GONE);
    }

    private StateListDrawable cellBackground() {
        final GradientDrawable chip = new GradientDrawable();
        chip.setColor(getContext().getColor(R.color.glass_chip_pressed));
        chip.setCornerRadius(dp(10));
        final StateListDrawable states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_pressed }, chip);
        return states;
    }

    @Override
    protected void onVisibilityChanged(final View changedView, final int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        updateCaretBlink();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateCaretBlink();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopCaretBlink();
        super.onDetachedFromWindow();
    }

    private void updateCaretBlink() {
        if (isShown()) {
            if (mCaretBlink == null) {
                mCaretBlink = ObjectAnimator.ofFloat(mCaret, View.ALPHA, 1f, 1f, 0f, 0f);
                mCaretBlink.setDuration(1000);
                mCaretBlink.setRepeatCount(ValueAnimator.INFINITE);
                mCaretBlink.start();
            }
        } else {
            stopCaretBlink();
        }
    }

    private void stopCaretBlink() {
        if (mCaretBlink != null) {
            mCaretBlink.cancel();
            mCaretBlink = null;
        }
        mCaret.setAlpha(1f);
    }
}
