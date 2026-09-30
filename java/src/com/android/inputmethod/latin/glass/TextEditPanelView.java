package com.android.inputmethod.latin.glass;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.inputmethod.latin.R;

public final class TextEditPanelView extends LinearLayout {
    private static final long REPEAT_START_DELAY = 400;
    private static final long REPEAT_INTERVAL = 50;

    private final PanelHost mHost;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final TextView mSelect;
    private boolean mSelecting;

    public TextEditPanelView(final Context context, final PanelHost host) {
        super(context);
        mHost = host;
        setOrientation(HORIZONTAL);
        final int pad = GlassUi.dp(context, 3);
        setPadding(pad, pad, pad, GlassUi.dp(context, 4));

        final LinearLayout pad3 = column(context);
        pad3.addView(repeatKey(context, R.drawable.ic_f7_arrow_up, 0,
                KeyEvent.KEYCODE_DPAD_UP), rowParams());
        final LinearLayout middle = row(context);
        middle.addView(repeatKey(context, R.drawable.ic_f7_arrow_left, 0,
                KeyEvent.KEYCODE_DPAD_LEFT), cellParams(1f));
        mSelect = GlassUi.keyButton(context, context.getString(R.string.glass_edit_select), 15);
        mSelect.setOnClickListener(v -> {
            mHost.performFeedback(v);
            setSelecting(!mSelecting);
        });
        middle.addView(mSelect, cellParams(1.2f));
        middle.addView(repeatKey(context, R.drawable.ic_f7_arrow_right, 0,
                KeyEvent.KEYCODE_DPAD_RIGHT), cellParams(1f));
        pad3.addView(middle, groupParams());
        pad3.addView(repeatKey(context, R.drawable.ic_f7_arrow_down, 0,
                KeyEvent.KEYCODE_DPAD_DOWN), rowParams());
        final LinearLayout bottom = row(context);
        bottom.addView(repeatKey(context, R.drawable.ic_f7_arrow_left_to_line, R.string.glass_edit_home,
                KeyEvent.KEYCODE_MOVE_HOME), cellParams(1f));
        bottom.addView(repeatKey(context, R.drawable.ic_f7_arrow_right_to_line, R.string.glass_edit_end,
                KeyEvent.KEYCODE_MOVE_END), cellParams(1f));
        bottom.addView(actionKey(context, R.string.glass_edit_cut, android.R.id.cut),
                cellParams(1f));
        pad3.addView(bottom, groupParams());
        addView(pad3, new LayoutParams(0, LayoutParams.MATCH_PARENT, 2.2f));

        final LinearLayout actions = column(context);
        actions.addView(actionKey(context, R.string.glass_edit_select_all, android.R.id.selectAll),
                rowParams());
        actions.addView(actionKey(context, R.string.glass_edit_copy, android.R.id.copy),
                rowParams());
        actions.addView(actionKey(context, R.string.glass_edit_paste, android.R.id.paste),
                rowParams());
        final ImageButton delete = repeatKey(context, R.drawable.ic_f7_delete_left,
                R.string.spoken_description_delete, KeyEvent.KEYCODE_DEL);
        actions.addView(delete, rowParams());
        addView(actions, new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
    }

    private void setSelecting(final boolean selecting) {
        mSelecting = selecting;
        mSelect.setActivated(selecting);
        mSelect.setTextColor(getContext().getColor(
                selecting ? R.color.glass_accent : R.color.glass_key_text));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeCallbacksAndMessages(null);
        setSelecting(false);
    }

    private TextView actionKey(final Context context, final int labelRes, final int actionId) {
        final TextView key = GlassUi.keyButton(context, context.getString(labelRes), 15);
        key.setOnClickListener(v -> {
            mHost.performFeedback(v);
            mHost.performContextMenuAction(actionId);
            if (actionId != android.R.id.selectAll) {
                setSelecting(false);
            }
        });
        return key;
    }

    @SuppressLint("ClickableViewAccessibility")
    private ImageButton repeatKey(final Context context, final int iconRes,
            final int descriptionRes, final int keyCode) {
        final ImageButton key = GlassUi.iconKey(context, iconRes,
                descriptionRes != 0 ? context.getString(descriptionRes) : null);
        GlassUi.setIconSize(key, GlassUi.dp(context, 20));
        final Runnable repeat = new Runnable() {
            @Override
            public void run() {
                send(keyCode);
                mHandler.postDelayed(this, REPEAT_INTERVAL);
            }
        };
        key.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                v.setPressed(true);
                mHost.performFeedback(v);
                send(keyCode);
                mHandler.postDelayed(repeat, REPEAT_START_DELAY);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                v.setPressed(false);
                mHandler.removeCallbacks(repeat);
                return true;
            default:
                return true;
            }
        });
        key.setOnClickListener(v -> send(keyCode));
        return key;
    }

    private void send(final int keyCode) {
        final boolean shift = mSelecting && keyCode != KeyEvent.KEYCODE_DEL;
        mHost.sendKey(keyCode, shift);
    }

    private static LinearLayout column(final Context context) {
        final LinearLayout l = new LinearLayout(context);
        l.setOrientation(VERTICAL);
        return l;
    }

    private static LinearLayout row(final Context context) {
        final LinearLayout l = new LinearLayout(context);
        l.setOrientation(HORIZONTAL);
        return l;
    }

    private LayoutParams rowParams() {
        final LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        final int m = GlassUi.dp(getContext(), 3);
        lp.setMargins(m, m, m, m);
        return lp;
    }

    private LayoutParams groupParams() {
        final LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        final int m = GlassUi.dp(getContext(), 3);
        lp.setMargins(0, m, 0, m);
        return lp;
    }

    private LayoutParams cellParams(final float weight) {
        final LayoutParams lp = new LayoutParams(0, LayoutParams.MATCH_PARENT, weight);
        final int m = GlassUi.dp(getContext(), 3);
        lp.setMargins(m, 0, m, 0);
        return lp;
    }

}
