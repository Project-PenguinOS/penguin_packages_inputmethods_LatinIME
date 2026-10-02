package com.android.inputmethod.latin.settings;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceCategory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.android.inputmethod.latin.R;

final class IosGroupedAdapter implements ListAdapter {
    private final ListAdapter mInner;

    private IosGroupedAdapter(final ListAdapter inner) {
        mInner = inner;
    }

    static void install(final ListView list) {
        final ListAdapter adapter = list.getAdapter();
        if (adapter == null || adapter instanceof IosGroupedAdapter) return;
        list.setAdapter(new IosGroupedAdapter(adapter));
        list.setDivider(null);
        list.setSelector(android.R.color.transparent);
        list.setClipToPadding(false);
        final int pad = Math.round(8 * list.getResources().getDisplayMetrics().density);
        list.setPadding(0, pad, 0, pad * 4);
    }

    private boolean isRow(final int position) {
        if (position < 0 || position >= mInner.getCount()) return false;
        return !(mInner.getItem(position) instanceof PreferenceCategory);
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final View view = mInner.getView(position, convertView, parent);
        if (isRow(position)) {
            final boolean first = !isRow(position - 1);
            final boolean last = !isRow(position + 1);
            final View icon = view.findViewById(android.R.id.icon);
            final boolean hasIcon = icon != null && icon.getVisibility() == View.VISIBLE
                    && icon instanceof android.widget.ImageView
                    && ((android.widget.ImageView) icon).getDrawable() != null;
            final Drawable bg = view.getBackground();
            final CellDrawable cell;
            if (bg instanceof CellDrawable) {
                cell = (CellDrawable) bg;
            } else {
                cell = new CellDrawable(view.getContext(), first, last);
                view.setBackground(cell);
            }
            cell.setPosition(first, last);
            cell.setSeparatorInsetDp(hasIcon ? 75 : 32);
        }
        return view;
    }

    @Override public boolean areAllItemsEnabled() { return mInner.areAllItemsEnabled(); }
    @Override public boolean isEnabled(final int position) { return mInner.isEnabled(position); }
    @Override public void registerDataSetObserver(final DataSetObserver o) {
        mInner.registerDataSetObserver(o);
    }
    @Override public void unregisterDataSetObserver(final DataSetObserver o) {
        mInner.unregisterDataSetObserver(o);
    }
    @Override public int getCount() { return mInner.getCount(); }
    @Override public Object getItem(final int position) { return mInner.getItem(position); }
    @Override public long getItemId(final int position) { return mInner.getItemId(position); }
    @Override public boolean hasStableIds() { return mInner.hasStableIds(); }
    @Override public int getItemViewType(final int position) {
        return mInner.getItemViewType(position);
    }
    @Override public int getViewTypeCount() { return mInner.getViewTypeCount(); }
    @Override public boolean isEmpty() { return mInner.isEmpty(); }

    private static final class CellDrawable extends Drawable {
        private final Paint mFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mSeparator = new Paint();
        private final Path mPath = new Path();
        private final RectF mRect = new RectF();
        private final int mColor;
        private final int mPressedColor;
        private final float mInset;
        private final float mRadius;
        private float mSeparatorInset;
        private final float mDensity;
        private boolean mFirst;
        private boolean mLast;
        private boolean mPressed;

        CellDrawable(final Context context, final boolean first, final boolean last) {
            final float density = context.getResources().getDisplayMetrics().density;
            mColor = context.getColor(R.color.ios_cell);
            mPressedColor = context.getColor(R.color.ios_cell_pressed);
            mSeparator.setColor(context.getColor(R.color.ios_separator));
            mSeparator.setStrokeWidth(Math.max(1f, density * 0.33f));
            mInset = 16 * density;
            mRadius = 10 * density;
            mSeparatorInset = 32 * density;
            mDensity = density;
            mFirst = first;
            mLast = last;
        }

        void setSeparatorInsetDp(final float dp) {
            mSeparatorInset = dp * mDensity;
        }

        void setPosition(final boolean first, final boolean last) {
            if (mFirst == first && mLast == last) return;
            mFirst = first;
            mLast = last;
            invalidateSelf();
        }

        @Override
        public boolean isStateful() {
            return true;
        }

        @Override
        protected boolean onStateChange(final int[] state) {
            boolean pressed = false;
            for (final int s : state) {
                if (s == android.R.attr.state_pressed) pressed = true;
            }
            if (pressed == mPressed) return false;
            mPressed = pressed;
            invalidateSelf();
            return true;
        }

        @Override
        public void draw(final Canvas canvas) {
            final Rect b = getBounds();
            mRect.set(b.left + mInset, b.top, b.right - mInset, b.bottom);
            final float top = mFirst ? mRadius : 0;
            final float bottom = mLast ? mRadius : 0;
            mPath.reset();
            mPath.addRoundRect(mRect, new float[] { top, top, top, top,
                    bottom, bottom, bottom, bottom }, Path.Direction.CW);
            mFill.setColor(mPressed ? mPressedColor : mColor);
            canvas.drawPath(mPath, mFill);
            if (!mLast) {
                final float y = b.bottom - mSeparator.getStrokeWidth() / 2;
                canvas.drawLine(b.left + mSeparatorInset, y, b.right - mInset, y, mSeparator);
            }
        }

        @Override public void setAlpha(final int alpha) { mFill.setAlpha(alpha); }
        @Override public void setColorFilter(final ColorFilter filter) { mFill.setColorFilter(filter); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
