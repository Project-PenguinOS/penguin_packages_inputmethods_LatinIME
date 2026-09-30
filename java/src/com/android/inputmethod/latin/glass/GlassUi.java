package com.android.inputmethod.latin.glass;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.inputmethod.latin.R;
import com.android.inputmethod.latin.utils.Fonts;

final class GlassUi {
    private GlassUi() {}

    static int dp(final Context context, final float dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable rounded(final int color, final float radiusPx) {
        final GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusPx);
        return d;
    }

    static Drawable keyBackground(final Context context) {
        return context.getDrawable(R.drawable.btn_keyboard_key_glass_functional);
    }

    static Drawable cardBackground(final Context context) {
        final float radius = dp(context, 12);
        final GradientDrawable content = rounded(context.getColor(R.color.glass_panel_item), radius);
        final GradientDrawable mask = rounded(0xFFFFFFFF, radius);
        return new RippleDrawable(ColorStateList.valueOf(context.getColor(R.color.glass_chip_pressed)),
                content, mask);
    }

    static TextView label(final Context context, final CharSequence text, final float sp,
            final boolean semibold) {
        final TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        tv.setTextColor(context.getColor(R.color.glass_key_text));
        tv.setTypeface(semibold ? Fonts.semibold() : Fonts.regular());
        tv.setIncludeFontPadding(false);
        return tv;
    }

    static TextView keyButton(final Context context, final CharSequence text, final float sp) {
        final TextView tv = label(context, text, sp, false);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(keyBackground(context));
        tv.setSingleLine(true);
        tv.setHapticFeedbackEnabled(false);
        tv.setSoundEffectsEnabled(false);
        return tv;
    }

    static ImageButton iconKey(final Context context, final int iconRes,
            final CharSequence description) {
        final ImageButton b = new ImageButton(context);
        b.setImageResource(iconRes);
        b.setImageTintList(ColorStateList.valueOf(context.getColor(R.color.glass_icon)));
        b.setScaleType(ImageView.ScaleType.CENTER);
        b.setBackground(keyBackground(context));
        b.setContentDescription(description);
        b.setPadding(0, 0, 0, 0);
        b.setHapticFeedbackEnabled(false);
        b.setSoundEffectsEnabled(false);
        return b;
    }

    static void setVisible(final View v, final boolean visible) {
        v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
