package com.android.inputmethod.latin.glass;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.android.inputmethod.latin.R;

import java.util.ArrayList;
import java.util.List;

public final class ClipboardPanelView extends LinearLayout
        implements ClipboardHistory.Listener {
    private static final int MENU_PIN = 1;
    private static final int MENU_DELETE = 2;

    private final PanelHost mHost;
    private final ClipboardHistory mHistory;
    private final RecyclerView mList;
    private final TextView mEmpty;
    private final TextView mClear;
    private final LinearLayout mDisabled;
    private final Adapter mAdapter = new Adapter();

    public ClipboardPanelView(final Context context, final PanelHost host,
            final ClipboardHistory history) {
        super(context);
        mHost = host;
        mHistory = history;
        setOrientation(VERTICAL);
        final int side = GlassUi.dp(context, 6);
        setPadding(side, GlassUi.dp(context, 2), side, GlassUi.dp(context, 4));

        final FrameLayout header = new FrameLayout(context);
        final TextView abc = GlassUi.keyButton(context, "ABC", 16);
        abc.setContentDescription(context.getString(R.string.glass_back_to_keyboard));
        abc.setOnClickListener(v -> {
            mHost.performFeedback(v);
            mHost.closePanel();
        });
        header.addView(abc, new FrameLayout.LayoutParams(GlassUi.dp(context, 64),
                GlassUi.dp(context, 36), Gravity.START | Gravity.CENTER_VERTICAL));
        final TextView title = GlassUi.label(context,
                context.getString(R.string.glass_clipboard_title), 17, true);
        header.addView(title, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        mClear = GlassUi.keyButton(context, context.getString(R.string.glass_clipboard_clear), 16);
        mClear.setOnClickListener(v -> {
            mHost.performFeedback(v);
            mHistory.clearUnpinned();
        });
        header.addView(mClear, new FrameLayout.LayoutParams(GlassUi.dp(context, 72),
                GlassUi.dp(context, 36), Gravity.END | Gravity.CENTER_VERTICAL));
        addView(header, new LayoutParams(LayoutParams.MATCH_PARENT, GlassUi.dp(context, 46)));

        final FrameLayout body = new FrameLayout(context);
        mList = new RecyclerView(context);
        mList.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        mList.setAdapter(mAdapter);
        mList.setClipToPadding(false);
        mList.setOverScrollMode(OVER_SCROLL_NEVER);
        mList.setVerticalFadingEdgeEnabled(true);
        mList.setFadingEdgeLength(GlassUi.dp(context, 16));
        body.addView(mList, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        mEmpty = GlassUi.label(context, context.getString(R.string.glass_clipboard_empty), 15,
                false);
        mEmpty.setTextColor(context.getColor(R.color.glass_suggestion_secondary));
        mEmpty.setGravity(Gravity.CENTER);
        body.addView(mEmpty, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        mDisabled = new LinearLayout(context);
        mDisabled.setOrientation(VERTICAL);
        mDisabled.setGravity(Gravity.CENTER);
        final TextView off = GlassUi.label(context,
                context.getString(R.string.glass_clipboard_disabled), 15, false);
        off.setTextColor(context.getColor(R.color.glass_suggestion_secondary));
        mDisabled.addView(off);
        final TextView turnOn = GlassUi.keyButton(context,
                context.getString(R.string.glass_clipboard_enable), 16);
        turnOn.setTextColor(context.getColor(R.color.glass_accent));
        turnOn.setOnClickListener(v -> {
            mHost.performFeedback(v);
            mHost.enableClipboardHistory();
            refresh();
        });
        final LayoutParams lp = new LayoutParams(GlassUi.dp(context, 120), GlassUi.dp(context, 40));
        lp.topMargin = GlassUi.dp(context, 12);
        mDisabled.addView(turnOn, lp);
        body.addView(mDisabled, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        addView(body, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mHistory.addListener(this);
        refresh();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHistory.removeListener(this);
    }

    @Override
    public void onClipboardHistoryChanged() {
        post(this::refresh);
    }

    private void refresh() {
        final boolean enabled = mHistory.isEnabled();
        final List<ClipboardHistory.Item> items = enabled
                ? mHistory.getItems() : new ArrayList<>();
        mAdapter.setItems(items);
        GlassUi.setVisible(mDisabled, !enabled);
        GlassUi.setVisible(mEmpty, enabled && items.isEmpty());
        GlassUi.setVisible(mList, enabled && !items.isEmpty());
        boolean hasUnpinned = false;
        for (final ClipboardHistory.Item item : items) hasUnpinned |= !item.mPinned;
        mClear.setVisibility(hasUnpinned ? VISIBLE : INVISIBLE);
    }

    private void showItemMenu(final View anchor, final ClipboardHistory.Item item) {
        final Context context = getContext();
        final PopupMenu menu = new PopupMenu(context, anchor);
        menu.getMenu().add(Menu.NONE, MENU_PIN, 0, context.getString(
                item.mPinned ? R.string.glass_clipboard_unpin : R.string.glass_clipboard_pin));
        menu.getMenu().add(Menu.NONE, MENU_DELETE, 1,
                context.getString(R.string.glass_clipboard_delete));
        menu.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == MENU_PIN) {
                mHistory.setPinned(item, !item.mPinned);
            } else {
                mHistory.remove(item);
            }
            return true;
        });
        menu.show();
    }

    private final class Adapter extends RecyclerView.Adapter<Holder> {
        private final ArrayList<ClipboardHistory.Item> mItems = new ArrayList<>();

        void setItems(final List<ClipboardHistory.Item> items) {
            mItems.clear();
            mItems.addAll(items);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            return new Holder(parent.getContext());
        }

        @Override
        public void onBindViewHolder(@NonNull final Holder holder, final int position) {
            holder.bind(mItems.get(position));
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }

    private final class Holder extends RecyclerView.ViewHolder {
        private final TextView mText;
        private final ImageView mPin;
        private ClipboardHistory.Item mItem;

        Holder(final Context context) {
            super(new FrameLayout(context));
            final FrameLayout card = (FrameLayout) itemView;
            final int m = GlassUi.dp(context, 4);
            final RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(m, m, m, m);
            card.setLayoutParams(lp);
            card.setBackground(GlassUi.cardBackground(context));
            card.setMinimumHeight(GlassUi.dp(context, 48));
            mText = GlassUi.label(context, null, 14, false);
            mText.setMaxLines(4);
            mText.setEllipsize(TextUtils.TruncateAt.END);
            mText.setLineSpacing(GlassUi.dp(context, 2), 1f);
            final int p = GlassUi.dp(context, 12);
            mText.setPadding(p, p, p + GlassUi.dp(context, 10), p);
            card.addView(mText, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            mPin = new ImageView(context);
            mPin.setImageResource(R.drawable.ic_f7_pin_fill);
            mPin.setImageTintList(ColorStateList.valueOf(context.getColor(R.color.glass_accent)));
            final int s = GlassUi.dp(context, 14);
            final FrameLayout.LayoutParams pinLp = new FrameLayout.LayoutParams(s, s,
                    Gravity.TOP | Gravity.END);
            pinLp.setMargins(0, GlassUi.dp(context, 7), GlassUi.dp(context, 7), 0);
            card.addView(mPin, pinLp);
            card.setOnClickListener(v -> {
                if (mItem == null) return;
                mHost.performFeedback(v);
                mHost.commitText(mItem.mText);
            });
            card.setOnLongClickListener(v -> {
                if (mItem == null) return false;
                mHost.performFeedback(v);
                showItemMenu(v, mItem);
                return true;
            });
        }

        void bind(final ClipboardHistory.Item item) {
            mItem = item;
            mText.setText(item.mText);
            mPin.setVisibility(item.mPinned ? VISIBLE : GONE);
            itemView.setContentDescription(item.mText);
        }
    }
}
