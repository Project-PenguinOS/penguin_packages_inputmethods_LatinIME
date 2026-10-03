package com.android.inputmethod.latin.settings.hig;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.materialswitch.MaterialSwitch;

import com.android.inputmethod.latin.R;

public final class HigList {

    private static final Object GROUP_TAG = new Object();

    private final Context mContext;
    private final ViewGroup mContainer;
    private boolean mLastWasHeader;

    public HigList(ViewGroup container) {
        mContext = container.getContext();
        mContainer = container;
    }

    public TextView header(CharSequence text) {
        TextView view = text(text, 13, R.color.label_secondary);
        view.setAllCaps(true);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.setMargins(dp(16), dp(mContainer.getChildCount() == 0 ? 16 : 24), dp(16), dp(7));
        mContainer.addView(view, lp);
        mLastWasHeader = true;
        return view;
    }

    public TextView footer(CharSequence text) {
        TextView view = text(text, 13, R.color.label_secondary);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.setMargins(dp(16), dp(7), dp(16), 0);
        mContainer.addView(view, lp);
        mLastWasHeader = false;
        return view;
    }

    public Group group() {
        LinearLayout group = new LinearLayout(mContext);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setTag(GROUP_TAG);
        LinearLayout.LayoutParams lp = matchWrap();

        lp.setMargins(0, mLastWasHeader ? 0 : dp(mContainer.getChildCount() == 0 ? 16 : 24),
                0, 0);
        mContainer.addView(group, lp);
        mLastWasHeader = false;
        return new Group(group);
    }

    public final class Group {
        private final LinearLayout mGroup;

        Group(LinearLayout group) {
            mGroup = group;
        }

        public Row row(@DrawableRes int icon, CharSequence title,
                @Nullable CharSequence value, boolean chevron,
                @Nullable View.OnClickListener onClick) {
            LinearLayout row = newRow(onClick);
            if (icon != 0) {
                row.addView(glyph(icon), glyphParams());
            }
            TextView titleView = text(title, 17, R.color.label_primary);
            titleView.setSingleLine(true);
            titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.addView(titleView, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView valueView = text(value, 17, R.color.label_secondary);
            valueView.setVisibility(value == null ? View.GONE : View.VISIBLE);
            LinearLayout.LayoutParams vlp = wrapWrap();
            vlp.setMarginStart(dp(8));
            row.addView(valueView, vlp);
            if (chevron) {
                row.addView(chevron(), chevronParams());
            }
            add(row, icon != 0 ? 58 : 16);
            return new Row(row, titleView, valueView);
        }

        public SwitchCompat switchRow(CharSequence title, boolean checked,
                SwitchCompat.OnCheckedChangeListener listener) {
            return switchRow(0, title, checked, listener);
        }

        public SwitchCompat switchRow(@DrawableRes int icon, CharSequence title, boolean checked,
                SwitchCompat.OnCheckedChangeListener listener) {
            return switchRow(icon, title, null, checked, listener);
        }

        public SwitchCompat switchRow(@DrawableRes int icon, CharSequence title,
                @Nullable CharSequence subtitle, boolean checked,
                SwitchCompat.OnCheckedChangeListener listener) {
            LinearLayout row = newRow(null);
            if (icon != 0) {
                row.addView(glyph(icon), glyphParams());
            }
            row.addView(titleBlock(title, subtitle), new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            SwitchCompat toggle = new SwitchCompat(new ContextThemeWrapper(mContext,
                    androidx.appcompat.R.style.Theme_AppCompat_DayNight_NoActionBar));
            toggle.setThumbDrawable(mContext.getDrawable(R.drawable.ios_switch_thumb));
            toggle.setTrackDrawable(mContext.getDrawable(R.drawable.ios_switch_track));
            toggle.setThumbTintList(null);
            toggle.setTrackTintList(null);
            toggle.setShowText(false);
            toggle.setSwitchMinWidth(dp(51));
            toggle.setBackground(null);
            toggle.setChecked(checked);
            toggle.setOnCheckedChangeListener(listener);
            toggle.setContentDescription(title);
            LinearLayout.LayoutParams lp = wrapWrap();
            lp.setMarginStart(dp(8));
            row.addView(toggle, lp);
            row.setOnClickListener(v -> toggle.toggle());
            add(row, icon != 0 ? 58 : 16);
            return toggle;
        }

        public Row subtitleRow(@DrawableRes int icon, CharSequence title,
                @Nullable CharSequence subtitle, boolean chevron,
                @Nullable View.OnClickListener onClick) {
            LinearLayout row = newRow(onClick);
            if (icon != 0) {
                row.addView(glyph(icon), glyphParams());
            }
            LinearLayout block = titleBlock(title, subtitle);
            row.addView(block, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            if (chevron) {
                row.addView(chevron(), chevronParams());
            }
            add(row, icon != 0 ? 58 : 16);
            return new Row(row, (TextView) block.getChildAt(0), (TextView) block.getChildAt(1));
        }

        public CheckRow checkRow(@DrawableRes int icon, CharSequence title, boolean checked,
                View.OnClickListener onClick) {
            LinearLayout row = newRow(onClick);
            if (icon != 0) {
                row.addView(glyph(icon), glyphParams());
            }
            TextView titleView = text(title, 17, R.color.label_primary);
            titleView.setSingleLine(true);
            titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.addView(titleView, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            ImageView check = new ImageView(mContext);
            check.setImageResource(R.drawable.ic_hig_checkmark);
            check.setImageTintList(ColorStateList.valueOf(mContext.getColor(R.color.hig_accent)));
            check.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(22), dp(22));
            clp.setMarginStart(dp(8));
            row.addView(check, clp);
            add(row, icon != 0 ? 58 : 16);
            CheckRow checkRow = new CheckRow(row, titleView, check);
            checkRow.setChecked(checked);
            return checkRow;
        }

        public SeekBar sliderRow(boolean indent, @Nullable CharSequence minLabel,
                @Nullable CharSequence maxLabel, int min, int max, int value,
                SeekBar.OnSeekBarChangeListener listener) {
            LinearLayout row = newRow(null);
            row.setPaddingRelative(dp(indent ? 58 : 16), 0, dp(16), 0);
            row.setBackground(null);
            if (minLabel != null) {
                LinearLayout.LayoutParams lp = wrapWrap();
                lp.setMarginEnd(dp(4));
                row.addView(text(minLabel, 13, R.color.label_secondary), lp);
            }
            SeekBar slider = new SeekBar(mContext);
            slider.setProgressDrawable(mContext.getDrawable(R.drawable.hig_slider_track));
            slider.setThumb(mContext.getDrawable(R.drawable.hig_slider_thumb));
            slider.setSplitTrack(false);
            slider.setBackground(null);
            slider.setPadding(dp(20), 0, dp(20), 0);
            slider.setMin(min);
            slider.setMax(max);
            slider.setProgress(value);
            slider.setOnSeekBarChangeListener(listener);
            row.addView(slider, new LinearLayout.LayoutParams(0, dp(44), 1f));
            if (maxLabel != null) {
                LinearLayout.LayoutParams lp = wrapWrap();
                lp.setMarginStart(dp(4));
                row.addView(text(maxLabel, 13, R.color.label_secondary), lp);
            }

            mGroup.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return slider;
        }

        public void customRow(View view) {
            add(view, 16);
        }

        public EditText fieldRow(CharSequence placeholder, int inputType) {
            LinearLayout row = newRow(null);
            EditText field = new EditText(mContext);
            field.setBackground(null);
            field.setHint(placeholder);

            field.setSingleLine((inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
            field.setInputType(inputType);

            field.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            field.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            field.setTextColor(mContext.getColor(R.color.label_primary));
            field.setHintTextColor(mContext.getColor(R.color.hig_tertiary_label));
            field.setPadding(0, dp(11), 0, dp(11));
            row.addView(field, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            add(row, 16);
            return field;
        }

        public TextView buttonRow(CharSequence title, boolean destructive,
                View.OnClickListener onClick) {
            LinearLayout row = newRow(onClick);
            TextView titleView = text(title, 17,
                    destructive ? R.color.system_red : R.color.hig_accent);
            row.addView(titleView, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            add(row, 16);
            return titleView;
        }

        private LinearLayout newRow(@Nullable View.OnClickListener onClick) {
            LinearLayout row = new LinearLayout(mContext);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(dp(44));
            row.setPaddingRelative(dp(16), 0, dp(16), 0);
            row.setBackgroundResource(R.drawable.hig_cell_background);
            if (onClick != null) {
                row.setOnClickListener(onClick);
            }
            return row;
        }

        private void add(View row, int separatorInsetDp) {
            if (mGroup.getChildCount() > 0) {
                View separator = new View(mContext);
                separator.setBackgroundColor(mContext.getColor(R.color.separator));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1) / 3));
                lp.setMarginStart(dp(separatorInsetDp));
                mGroup.addView(separator, lp);
            }
            mGroup.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    public static final class Row {
        public final View view;
        public final TextView title;
        public final TextView value;

        Row(View view, TextView title, TextView value) {
            this.view = view;
            this.title = title;
            this.value = value;
        }

        public void setValue(@Nullable CharSequence text) {
            value.setText(text);
            value.setVisibility(text == null ? View.GONE : View.VISIBLE);
        }
    }

    public static final class CheckRow {
        public final View view;
        public final TextView title;
        private final ImageView mCheck;

        CheckRow(View view, TextView title, ImageView check) {
            this.view = view;
            this.title = title;
            mCheck = check;
        }

        public void setChecked(boolean checked) {
            mCheck.setVisibility(checked ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void view(View view) {
        mContainer.addView(view, matchWrap());
        mLastWasHeader = false;
    }

    public static void setRowEnabled(View viewInRow, boolean enabled) {
        View row = viewInRow;
        while (row.getParent() instanceof View
                && ((View) row.getParent()).getTag() != GROUP_TAG) {
            row = (View) row.getParent();
        }
        row.setAlpha(enabled ? 1f : 0.4f);
        setEnabledRecursive(row, enabled);
    }

    private static void setEnabledRecursive(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setEnabledRecursive(group.getChildAt(i), enabled);
            }
        }
    }

    private LinearLayout titleBlock(CharSequence title, @Nullable CharSequence subtitle) {
        LinearLayout block = new LinearLayout(mContext);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(0, dp(subtitle == null ? 0 : 9), 0, dp(subtitle == null ? 0 : 9));
        block.addView(text(title, 17, R.color.label_primary), matchWrap());
        TextView sub = text(subtitle, 13, R.color.label_secondary);
        sub.setVisibility(subtitle == null ? View.GONE : View.VISIBLE);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(2);
        block.addView(sub, lp);
        return block;
    }

    private ImageView glyph(@DrawableRes int icon) {
        ImageView glyph = new ImageView(mContext);
        glyph.setImageResource(icon);
        glyph.setImageTintList(ColorStateList.valueOf(mContext.getColor(R.color.label_primary)));
        glyph.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return glyph;
    }

    private LinearLayout.LayoutParams glyphParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(26), dp(26));
        lp.setMarginEnd(dp(16));
        return lp;
    }

    private ImageView chevron() {
        ImageView view = new ImageView(mContext);
        view.setImageResource(R.drawable.ic_settings_chevron);
        view.setImageTintList(ColorStateList.valueOf(mContext.getColor(R.color.hig_tertiary_label)));
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return view;
    }

    private LinearLayout.LayoutParams chevronParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(20), dp(20));
        lp.setMarginStart(dp(4));
        lp.setMarginEnd(-dp(4));
        return lp;
    }

    private TextView text(@Nullable CharSequence text, int sp, @ColorRes int color) {
        TextView view = new TextView(mContext);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTextColor(mContext.getColor(color));
        view.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static ColorStateList checkedList(int checked, int unchecked) {
        return new ColorStateList(
                new int[][] {{android.R.attr.state_checked}, {}},
                new int[] {checked, unchecked});
    }

    private int dp(int value) {
        return Math.round(value * mContext.getResources().getDisplayMetrics().density);
    }
}
