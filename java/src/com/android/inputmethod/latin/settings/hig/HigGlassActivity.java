package com.android.inputmethod.latin.settings.hig;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.inputmethod.latin.R;

public abstract class HigGlassActivity extends AppCompatActivity {

    public static final float BLUR_RADIUS = 10f;

    private HigNavigationBar mNavBar;
    private LinearLayout mList;
    private ScrollView mScroll;
    private TextView mLargeTitle;
    private View mHairline;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_hig_glass);
        mList = findViewById(R.id.list);
        mScroll = findViewById(R.id.scroll);
        mLargeTitle = findViewById(R.id.large_title);
        mNavBar = findViewById(R.id.nav_bar);
        mNavBar.bindTo(this);
        setupGlass();
        setupInsets();
    }

    private void setupGlass() {
        HigBlurTarget target = findViewById(R.id.blur_target);
        HigBlurView glass = findViewById(R.id.nav_glass);
        glass.setupWith(target, BLUR_RADIUS);

        mHairline = findViewById(R.id.nav_hairline);
        mScroll.setOnScrollChangeListener((v, x, y, oldX, oldY) -> {
            updateScrollEffects(y);
            glass.invalidate();
        });
        mList.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> glass.invalidate());
    }

    private void updateScrollEffects(int y) {
        float density = getResources().getDisplayMetrics().density;
        mHairline.setAlpha(clamp(y / density / 8f));
        if (mLargeTitle.getVisibility() == View.VISIBLE) {
            float height = Math.max(1, mLargeTitle.getHeight());
            mNavBar.getTitleView().setAlpha(clamp((y - height / 2f) / (height / 2f)));
        }
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private void setupInsets() {
        View spacer = findViewById(R.id.status_spacer);
        int barHeight = getResources().getDimensionPixelSize(R.dimen.hig_navigation_bar_height);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content),
                (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
                    ViewGroup.LayoutParams lp = spacer.getLayoutParams();
                    lp.height = bars.top;
                    spacer.setLayoutParams(lp);
                    mScroll.setPadding(bars.left, bars.top + barHeight, bars.right, bars.bottom);
                    return insets;
                });
    }

    protected HigNavigationBar getNavBar() {
        return mNavBar;
    }

    protected void setLargeTitle(@Nullable CharSequence title) {
        mLargeTitle.setText(title);
        mLargeTitle.setVisibility(title == null ? View.GONE : View.VISIBLE);
        mNavBar.getTitleView().setAlpha(title == null ? 1f : 0f);
        mLargeTitle.post(() -> updateScrollEffects(mScroll.getScrollY()));
    }

    protected HigList newList() {
        mList.removeAllViews();
        return new HigList(mList);
    }

    protected ViewGroup getListContainer() {
        return mList;
    }
}
