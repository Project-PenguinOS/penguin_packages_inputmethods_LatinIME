package com.android.inputmethod.latin.glass;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

public final class FloatingKeyboard {
    public static final String PREF_FLOATING = "pref_floating_keyboard";
    private static final String PREF_X = "pref_floating_keyboard_x";
    private static final String PREF_Y = "pref_floating_keyboard_y";

    public static final float WIDTH_RATIO = 0.84f;

    private final Context mContext;
    private final Window mWindow;
    private final SharedPreferences mPrefs;
    private final Runnable mOnMoved;
    private boolean mFloating;

    public FloatingKeyboard(final Context context, final Window window, final Runnable onMoved) {
        mContext = context;
        mWindow = window;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mOnMoved = onMoved;
    }

    public static boolean isEnabled(final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_FLOATING, false);
    }

    public static void setEnabled(final Context context, final boolean enabled) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(PREF_FLOATING, enabled).apply();
    }

    public boolean isFloating() {
        return mFloating;
    }

    private Rect screen() {
        return mContext.getSystemService(WindowManager.class).getCurrentWindowMetrics()
                .getBounds();
    }

    public void apply(final boolean floating) {
        mFloating = floating;
        final WindowManager.LayoutParams lp = mWindow.getAttributes();
        if (!floating) {
            if (lp.gravity == Gravity.BOTTOM && lp.x == 0 && lp.y == 0
                    && lp.width == ViewGroup.LayoutParams.MATCH_PARENT) {
                return;
            }
            lp.gravity = Gravity.BOTTOM;
            lp.x = 0;
            lp.y = 0;
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            mWindow.setAttributes(lp);
            return;
        }
        final Rect screen = screen();
        final int width = Math.round(screen.width() * WIDTH_RATIO);
        lp.gravity = Gravity.BOTTOM | Gravity.START;
        lp.width = width;
        final int defaultX = (screen.width() - width) / 2;
        final int defaultY = Math.round(screen.height() * 0.08f);
        lp.x = clampX(mPrefs.getInt(PREF_X, defaultX), width, screen);
        lp.y = clampY(mPrefs.getInt(PREF_Y, defaultY), screen);
        mWindow.setAttributes(lp);
    }

    private static int clampX(final int x, final int width, final Rect screen) {
        return Math.max(0, Math.min(screen.width() - width, x));
    }

    private int clampY(final int y, final Rect screen) {
        final View decor = mWindow.getDecorView();
        final int height = decor.getHeight() > 0 ? decor.getHeight() : screen.height() / 3;
        return Math.max(0, Math.min(screen.height() - height, y));
    }

    @SuppressLint("ClickableViewAccessibility")
    public void attachHandle(final View handle) {
        final float[] start = new float[2];
        final int[] origin = new int[2];
        handle.setOnTouchListener((v, event) -> {
            if (!mFloating) return false;
            final WindowManager.LayoutParams lp = mWindow.getAttributes();
            switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                start[0] = event.getRawX();
                start[1] = event.getRawY();
                origin[0] = lp.x;
                origin[1] = lp.y;
                v.setPressed(true);
                return true;
            case MotionEvent.ACTION_MOVE: {
                final Rect screen = screen();
                lp.x = clampX(origin[0] + Math.round(event.getRawX() - start[0]), lp.width,
                        screen);
                lp.y = clampY(origin[1] - Math.round(event.getRawY() - start[1]), screen);
                mWindow.setAttributes(lp);
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                v.setPressed(false);
                mPrefs.edit().putInt(PREF_X, lp.x).putInt(PREF_Y, lp.y).apply();
                mOnMoved.run();
                return true;
            default:
                return true;
            }
        });
    }
}
