package com.android.inputmethod.latin.settings.hig;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.inputmethod.latin.R;

public final class HigNavigationBar extends FrameLayout {

  public static final String EXTRA_BACK_LABEL = "hig_back_label";

  private TextView title;
  private TextView backLabel;
  private View back;
  private ImageView backChevron;
  private LinearLayout trailing;
  @Nullable private Integer tint;

  public HigNavigationBar(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    title = findViewById(R.id.hig_nav_title);
    backLabel = findViewById(R.id.hig_nav_back_label);
    back = findViewById(R.id.hig_nav_back);
    backChevron = findViewById(R.id.hig_nav_back_chevron);
    trailing = findViewById(R.id.hig_nav_trailing);
  }

  public void setTint(@ColorInt int color) {
    tint = color;
    backLabel.setTextColor(color);
    backChevron.setImageTintList(ColorStateList.valueOf(color));
    for (int i = 0; i < trailing.getChildCount(); i++) {
      tintItem(trailing.getChildAt(i), color);
    }
  }

  public void setLeadingTextButton(CharSequence text, OnClickListener listener) {
    backChevron.setVisibility(GONE);
    backLabel.setText(text);
    back.setContentDescription(text);
    back.setPaddingRelative(dp(16), 0, dp(8), 0);
    back.setOnClickListener(listener);
  }

  public TextView addTextAction(CharSequence text, boolean bold, OnClickListener listener) {
    TextView item = new TextView(getContext());
    item.setText(text);
    item.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
    item.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
    item.setGravity(Gravity.CENTER);
    item.setMinWidth(dp(44));
    item.setPadding(dp(8), 0, dp(8), 0);
    addTrailing(item, listener);
    return item;
  }

  public ImageView addIconAction(@DrawableRes int icon, CharSequence description,
      OnClickListener listener) {
    ImageView item = new ImageView(getContext());
    item.setImageResource(icon);
    item.setContentDescription(description);
    item.setScaleType(ImageView.ScaleType.CENTER);
    item.setMinimumWidth(dp(44));
    addTrailing(item, listener);
    return item;
  }

  private void addTrailing(View item, OnClickListener listener) {
    TypedValue background = new TypedValue();
    getContext().getTheme().resolveAttribute(
        android.R.attr.selectableItemBackgroundBorderless, background, true);
    item.setBackgroundResource(background.resourceId);
    item.setOnClickListener(listener);
    trailing.addView(item, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
    int color = tint != null ? tint : backLabel.getCurrentTextColor();
    tintItem(item, color);
  }

  private static void tintItem(View item, @ColorInt int color) {
    ColorStateList colors = new ColorStateList(
        new int[][] {{-android.R.attr.state_enabled}, {}},
        new int[] {(color & 0x00FFFFFF) | 0x4D000000, color});
    if (item instanceof TextView) {
      ((TextView) item).setTextColor(colors);
    } else if (item instanceof ImageView) {
      ((ImageView) item).setImageTintList(colors);
    }
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  public void setTitle(@Nullable CharSequence text) {
    title.setText(text);
  }

  public void setBackLabel(@Nullable CharSequence text) {
    CharSequence label =
        text == null || text.length() == 0 ? getContext().getText(R.string.hig_navigation_back) : text;
    backLabel.setText(label);
    back.setContentDescription(label);
  }

  public void setOnBackClickListener(OnClickListener listener) {
    back.setOnClickListener(listener);
  }

  public void setBackVisible(boolean visible) {
    back.setVisibility(visible ? VISIBLE : INVISIBLE);
  }

  public TextView getTitleView() {
    return title;
  }

  @Nullable
  public static HigNavigationBar installInActionBar(AppCompatActivity activity) {
    ActionBar actionBar = activity.getSupportActionBar();
    if (actionBar == null) {
      return null;
    }
    HigNavigationBar bar = inflate(activity, null);
    actionBar.setDisplayShowTitleEnabled(false);
    actionBar.setDisplayHomeAsUpEnabled(false);
    actionBar.setDisplayShowHomeEnabled(false);
    actionBar.setDisplayShowCustomEnabled(true);
    actionBar.setCustomView(
        bar,
        new ActionBar.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    if (bar.getParent() instanceof Toolbar) {
      ((Toolbar) bar.getParent()).setContentInsetsAbsolute(0, 0);
    }
    bar.bind(activity);
    return bar;
  }

  public static HigNavigationBar installInToolbar(Activity activity, Toolbar toolbar) {
    toolbar.setNavigationIcon(null);
    toolbar.setTitle(null);
    toolbar.setContentInsetsAbsolute(0, 0);
    HigNavigationBar bar = inflate(activity, toolbar);
    toolbar.addView(
        bar,
        new Toolbar.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    bar.bind(activity);
    return bar;
  }

  private static HigNavigationBar inflate(Activity activity, @Nullable ViewGroup parent) {
    return (HigNavigationBar)
        LayoutInflater.from(activity).inflate(R.layout.hig_navigation_bar, parent, false);
  }

  public void bindTo(Activity activity) {
    bind(activity);
  }

  private void bind(Activity activity) {
    setTitle(activity.getTitle());
    setBackLabel(activity.getIntent().getCharSequenceExtra(EXTRA_BACK_LABEL));
    setOnBackClickListener(v -> activity.onBackPressed());
  }
}
