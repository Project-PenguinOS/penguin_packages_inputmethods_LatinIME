package com.android.inputmethod.latin.glass;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.android.inputmethod.latin.R;
import com.android.inputmethod.latin.settings.Settings;

public final class GlassPanels {
    public static final int PANEL_NONE = 0;
    public static final int PANEL_CLIPBOARD = 1;
    public static final int PANEL_TEXT_EDIT = 2;

    public static final float ONE_HANDED_WIDTH_RATIO = 0.82f;

    public interface OneHandedListener {
        void onOneHandedModeChanged(int mode);
    }

    private final PanelHost mHost;
    private final ClipboardHistory mClipboardHistory;
    private final OneHandedListener mOneHandedListener;
    private FrameLayout mArea;
    private View mKeyboardView;
    private View mPanel;
    private int mPanelId = PANEL_NONE;
    private LinearLayout mOneHandedControls;
    private int mOneHandedMode = Settings.ONE_HANDED_OFF;

    public GlassPanels(final PanelHost host, final ClipboardHistory clipboardHistory,
            final OneHandedListener oneHandedListener) {
        mHost = host;
        mClipboardHistory = clipboardHistory;
        mOneHandedListener = oneHandedListener;
    }

    public void attach(final View inputView) {
        mArea = inputView.findViewById(R.id.keyboard_area);
        mKeyboardView = inputView.findViewById(R.id.keyboard_view);
        mPanel = null;
        mPanelId = PANEL_NONE;
        mOneHandedControls = null;
        applyOneHandedMode(mOneHandedMode);
    }

    public int getShowingPanel() {
        return mPanelId;
    }

    public void togglePanel(final int panelId) {
        if (mPanelId == panelId) {
            hidePanel();
        } else {
            showPanel(panelId);
        }
    }

    public void showPanel(final int panelId) {
        if (mArea == null || mKeyboardView == null) return;
        hidePanel();
        final Context context = mArea.getContext();
        final View panel;
        switch (panelId) {
        case PANEL_CLIPBOARD:
            panel = new ClipboardPanelView(context, mHost, mClipboardHistory);
            break;
        case PANEL_TEXT_EDIT:
            panel = new TextEditPanelView(context, mHost);
            break;
        default:
            return;
        }
        final int height = Math.max(mKeyboardView.getHeight(),
                context.getResources().getDimensionPixelSize(R.dimen.glass_keyboard_height));
        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, height);
        mArea.addView(panel, lp);
        mKeyboardView.setVisibility(View.INVISIBLE);
        if (mOneHandedControls != null) mOneHandedControls.setVisibility(View.GONE);
        mPanel = panel;
        mPanelId = panelId;
    }

    public void hidePanel() {
        if (mPanel != null && mArea != null) {
            mArea.removeView(mPanel);
        }
        mPanel = null;
        mPanelId = PANEL_NONE;
        if (mKeyboardView != null) mKeyboardView.setVisibility(View.VISIBLE);
        if (mOneHandedControls != null) mOneHandedControls.setVisibility(View.VISIBLE);
    }

    public void applyOneHandedMode(final int mode) {
        mOneHandedMode = mode;
        if (mArea == null || mKeyboardView == null) return;
        final FrameLayout.LayoutParams kbLp =
                (FrameLayout.LayoutParams) mKeyboardView.getLayoutParams();
        final boolean on = mode != Settings.ONE_HANDED_OFF;
        final boolean left = mode == Settings.ONE_HANDED_LEFT;
        kbLp.gravity = !on ? Gravity.CENTER_HORIZONTAL
                : (left ? Gravity.START : Gravity.END);
        mKeyboardView.setLayoutParams(kbLp);

        if (!on) {
            if (mOneHandedControls != null) {
                mArea.removeView(mOneHandedControls);
                mOneHandedControls = null;
            }
            return;
        }
        if (mOneHandedControls == null) {
            mOneHandedControls = buildOneHandedControls(mArea.getContext());
            final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            mArea.addView(mOneHandedControls, lp);
        }
        final int areaWidth = mArea.getResources().getDisplayMetrics().widthPixels;
        final FrameLayout.LayoutParams lp =
                (FrameLayout.LayoutParams) mOneHandedControls.getLayoutParams();
        lp.width = Math.round(areaWidth * (1f - ONE_HANDED_WIDTH_RATIO));
        lp.gravity = left ? Gravity.END : Gravity.START;
        mOneHandedControls.setLayoutParams(lp);
        final ImageButton swap = (ImageButton) mOneHandedControls.getChildAt(1);
        swap.setImageResource(left ? R.drawable.ic_f7_chevron_right : R.drawable.ic_f7_chevron_left);
    }

    private LinearLayout buildOneHandedControls(final Context context) {
        final LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER);
        final ImageButton expand = GlassUi.iconKey(context,
                R.drawable.ic_f7_arrow_up_left_arrow_down_right,
                context.getString(R.string.glass_one_handed_exit));
        expand.setBackgroundResource(R.drawable.glass_icon_button_background);
        expand.setOnClickListener(v -> {
            mHost.performFeedback(v);
            mOneHandedListener.onOneHandedModeChanged(Settings.ONE_HANDED_OFF);
        });
        final ImageButton swap = GlassUi.iconKey(context, R.drawable.ic_f7_chevron_left,
                context.getString(R.string.glass_one_handed_switch_side));
        swap.setBackgroundResource(R.drawable.glass_icon_button_background);
        swap.setOnClickListener(v -> {
            mHost.performFeedback(v);
            mOneHandedListener.onOneHandedModeChanged(
                    mOneHandedMode == Settings.ONE_HANDED_LEFT
                            ? Settings.ONE_HANDED_RIGHT : Settings.ONE_HANDED_LEFT);
        });
        final int size = GlassUi.dp(context, 48);
        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMargins(0, GlassUi.dp(context, 10), 0, GlassUi.dp(context, 10));
        column.addView(expand, lp);
        column.addView(swap, new LinearLayout.LayoutParams(lp));
        return column;
    }
}
