package com.android.inputmethod.keyboard.internal;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

public final class PreviewOverlay {
    private static final String TAG = PreviewOverlay.class.getSimpleName();

    private static PreviewOverlay sCurrent;

    private final FrameLayout mRoot;
    private final PopupWindow mPopup;
    private final int[] mLocation = new int[2];
    private final Rect mTmpRect = new Rect();
    private Runnable mOnOutsideTouch;
    private View mAnchor;
    private int mExpectedLeft;
    private int mExpectedTop;

    public PreviewOverlay(final Context context) {
        mRoot = new FrameLayout(context) {
            @Override
            public boolean onTouchEvent(final MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN && mOnOutsideTouch != null) {
                    mOnOutsideTouch.run();
                }
                return true;
            }
        };
        mRoot.setClipChildren(false);
        mPopup = new PopupWindow(mRoot, 0, 0, false);
        mPopup.setBackgroundDrawable(null);
        mPopup.setClippingEnabled(false);
        mPopup.setIsLaidOutInScreen(true);
        mPopup.setTouchable(false);
        mPopup.setAnimationStyle(0);
        mPopup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        mPopup.setWindowLayoutType(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG);
        sCurrent = this;
    }

    public ViewGroup getRoot() {
        return mRoot;
    }

    public void show(final View anchor) {
        if (anchor.getWindowToken() == null) {
            return;
        }
        mAnchor = anchor;
        final View windowRoot = anchor.getRootView();
        windowRoot.getLocationOnScreen(mLocation);
        final int windowBottom = mLocation[1] + windowRoot.getHeight();
        windowRoot.getWindowVisibleDisplayFrame(mTmpRect);
        final WindowManager wm = anchor.getContext().getSystemService(WindowManager.class);
        final Rect screen = wm.getCurrentWindowMetrics().getBounds();
        final int width = windowRoot.getWidth() > 0 ? windowRoot.getWidth() : screen.width();
        final int height = Math.max(windowBottom, screen.height());
        mExpectedLeft = mLocation[0];
        mExpectedTop = windowBottom - height;
        try {
            if (mPopup.isShowing()) {
                mPopup.update(mExpectedLeft, mExpectedTop, width, height);
            } else {
                mPopup.setWidth(width);
                mPopup.setHeight(height);
                mPopup.showAtLocation(anchor, Gravity.NO_GRAVITY, mExpectedLeft, mExpectedTop);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot show preview overlay", e);
        }
    }

    public void dismiss() {
        if (mPopup.isShowing()) {
            try {
                mPopup.dismiss();
            } catch (RuntimeException e) {
                Log.w(TAG, "Cannot dismiss preview overlay", e);
            }
        }
        mAnchor = null;
    }

    public boolean isShowing() {
        return mPopup.isShowing();
    }

    public void setTouchable(final boolean touchable, final Runnable onOutsideTouch) {
        mOnOutsideTouch = touchable ? onOutsideTouch : null;
        if (mPopup.isTouchable() == touchable) {
            return;
        }
        mPopup.setTouchable(touchable);
        if (mPopup.isShowing()) {
            mPopup.update();
        }
    }

    public void getLocationInOverlay(final View view, final int[] outLocation) {
        view.getLocationOnScreen(outLocation);
        if (mRoot.isAttachedToWindow() && mRoot.getHeight() > 0) {
            mRoot.getLocationOnScreen(mLocation);
        } else {
            mLocation[0] = mExpectedLeft;
            mLocation[1] = mExpectedTop;
        }
        outLocation[0] -= mLocation[0];
        outLocation[1] -= mLocation[1];
    }

    public static void getLocationInCurrentOverlay(final View view, final int[] outLocation) {
        final PreviewOverlay overlay = sCurrent;
        if (overlay != null && overlay.isShowing()) {
            overlay.getLocationInOverlay(view, outLocation);
        } else {
            view.getLocationInWindow(outLocation);
        }
    }
}
