package com.android.inputmethod.keyboard.emoji;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;

import com.android.inputmethod.latin.R;
import com.android.inputmethod.latin.common.Constants;
import com.android.inputmethod.latin.emoji.EmojiKeywords;
import com.android.inputmethod.latin.emoji.EmojiSkinTones;

import java.util.ArrayList;
import java.util.List;

public final class EmojiSearchController implements EmojiSearchView.Callback {
    public interface Host {
        void showLetterKeys();

        void showEmojiPalette();

        void commitEmoji(String emoji);

        void performFeedback(View view);
    }

    private static final int MAX_RESULTS = 60;
    private static final int MAX_QUERY_LENGTH = 40;

    private final Host mHost;
    private final StringBuilder mQuery = new StringBuilder();
    private boolean mActive;
    private EmojiSearchView mView;
    private EmojiPalettesView mPalette;
    private View mStrip;
    private int mStripHeight;
    private SharedPreferences mPrefs;

    public EmojiSearchController(final Host host) {
        mHost = host;
    }

    public void attach(final View inputView) {
        finish();
        mPalette = inputView.findViewById(R.id.emoji_palettes_view);
        mStrip = inputView.findViewById(R.id.suggestion_strip_view);
        final ViewGroup frame = inputView.findViewById(R.id.main_keyboard_frame);
        if (mPalette == null || mStrip == null || frame == null) {
            mView = null;
            return;
        }
        mPrefs = PreferenceManager.getDefaultSharedPreferences(inputView.getContext());
        mPalette.setOnSearchRequestedListener(this::start);
        mView = new EmojiSearchView(inputView.getContext(), this);
        mView.setVisibility(View.GONE);
        frame.addView(mView, frame.indexOfChild(mStrip), new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public boolean isActive() {
        return mActive;
    }

    public void start() {
        if (mActive || mView == null) return;
        mActive = true;
        mQuery.setLength(0);
        mView.setVisibility(View.VISIBLE);
        final ViewGroup.LayoutParams lp = mStrip.getLayoutParams();
        mStripHeight = lp.height;
        lp.height = 0;
        mStrip.setLayoutParams(lp);
        EmojiKeywords.load(this::refresh);
        refresh();
        mHost.showLetterKeys();
    }

    public void stop() {
        if (!mActive) return;
        finish();
        mHost.showEmojiPalette();
    }

    public void reset() {
        finish();
    }

    private void finish() {
        if (!mActive) return;
        mActive = false;
        mQuery.setLength(0);
        if (mView != null) {
            mView.setVisibility(View.GONE);
        }
        if (mStrip != null) {
            final ViewGroup.LayoutParams lp = mStrip.getLayoutParams();
            lp.height = mStripHeight;
            mStrip.setLayoutParams(lp);
        }
    }

    public boolean onKey(final int code) {
        if (!mActive) return false;
        switch (code) {
        case Constants.CODE_EMOJI:
            finish();
            return false;
        case Constants.CODE_DELETE:
            onDelete();
            return true;
        case Constants.CODE_ENTER:
            final List<String> results = results();
            if (!results.isEmpty()) {
                insert(results.get(0));
            }
            return true;
        default:
            if (code > 0) {
                onCodePoint(code);
                return true;
            }
            return false;
        }
    }

    public boolean onText(final String text) {
        if (!mActive) return false;
        if (text != null && mQuery.length() + text.length() <= MAX_QUERY_LENGTH) {
            mQuery.append(text);
            refresh();
        }
        return true;
    }

    public void onCodePoint(final int codePoint) {
        if (!mActive || mQuery.length() >= MAX_QUERY_LENGTH) return;
        if (codePoint == Constants.CODE_SPACE && mQuery.length() == 0) return;
        mQuery.appendCodePoint(Character.toLowerCase(codePoint));
        refresh();
    }

    public void onDelete() {
        if (!mActive || mQuery.length() == 0) return;
        final int last = Character.codePointBefore(mQuery, mQuery.length());
        mQuery.setLength(mQuery.length() - Character.charCount(last));
        refresh();
    }

    private List<String> results() {
        final List<String> found;
        if (mQuery.toString().trim().isEmpty()) {
            found = mPalette != null ? mPalette.getRecentEmoji(MAX_RESULTS) : new ArrayList<>();
            if (found.isEmpty()) {
                found.addAll(EmojiKeywords.popular(MAX_RESULTS));
            }
        } else {
            found = EmojiKeywords.search(mQuery, MAX_RESULTS);
        }
        if (mPrefs == null) return found;
        final ArrayList<String> toned = new ArrayList<>(found.size());
        for (final String e : found) {
            toned.add(EmojiSkinTones.applyPreferred(mPrefs, e));
        }
        return toned;
    }

    private void refresh() {
        if (!mActive || mView == null) return;
        mView.setQuery(mQuery);
        final boolean searching = !mQuery.toString().trim().isEmpty();
        mView.setResults(results(), searching && EmojiKeywords.isLoaded());
    }

    private void insert(final String emoji) {
        mHost.commitEmoji(emoji);
        if (mPalette != null) {
            mPalette.addRecentEmoji(emoji);
        }
    }

    @Override
    public void onCloseClicked(final View view) {
        mHost.performFeedback(view);
        stop();
    }

    @Override
    public void onClearClicked(final View view) {
        mHost.performFeedback(view);
        mQuery.setLength(0);
        refresh();
    }

    @Override
    public void onResultClicked(final View view, final String emoji) {
        mHost.performFeedback(view);
        insert(emoji);
    }
}
