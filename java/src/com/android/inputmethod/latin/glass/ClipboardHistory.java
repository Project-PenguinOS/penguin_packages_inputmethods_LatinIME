package com.android.inputmethod.latin.glass;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.PersistableBundle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ClipboardHistory implements ClipboardManager.OnPrimaryClipChangedListener {
    private static final String TAG = ClipboardHistory.class.getSimpleName();
    private static final String FILE_NAME = "clipboard_history.json";
    private static final long EXPIRY_MS = 60 * 60 * 1000L;
    private static final long SUGGESTION_WINDOW_MS = 90 * 1000L;
    private static final int MAX_UNPINNED = 30;
    private static final int MAX_TEXT_LENGTH = 10_000;

    public static final class Item {
        public final String mText;
        public final long mTimestamp;
        public boolean mPinned;

        Item(final String text, final long timestamp, final boolean pinned) {
            mText = text;
            mTimestamp = timestamp;
            mPinned = pinned;
        }
    }

    public interface Listener {
        void onClipboardHistoryChanged();
    }

    private final Context mContext;
    private final ClipboardManager mClipboardManager;
    private final ArrayList<Item> mItems = new ArrayList<>();
    private final List<Listener> mListeners = new CopyOnWriteArrayList<>();
    private boolean mEnabled;
    private boolean mLoaded;
    private Item mSuggestion;

    public ClipboardHistory(final Context context) {
        mContext = context.getApplicationContext();
        mClipboardManager = mContext.getSystemService(ClipboardManager.class);
    }

    public void setEnabled(final boolean enabled) {
        if (mEnabled == enabled) return;
        mEnabled = enabled;
        if (mClipboardManager == null) return;
        if (enabled) {
            mClipboardManager.addPrimaryClipChangedListener(this);
        } else {
            mClipboardManager.removePrimaryClipChangedListener(this);
            mSuggestion = null;
        }
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void addListener(final Listener listener) {
        mListeners.add(listener);
    }

    public void removeListener(final Listener listener) {
        mListeners.remove(listener);
    }

    @Override
    public void onPrimaryClipChanged() {
        if (!mEnabled) return;
        final ClipData clip;
        try {
            clip = mClipboardManager.getPrimaryClip();
        } catch (SecurityException e) {
            return;
        }
        if (clip == null || clip.getItemCount() == 0) return;
        final ClipDescription description = clip.getDescription();
        if (isSensitive(description)) return;
        final CharSequence text = clip.getItemAt(0).coerceToText(mContext);
        if (TextUtils.isEmpty(text) || TextUtils.getTrimmedLength(text) == 0) return;
        add(text.toString());
    }

    private static boolean isSensitive(final ClipDescription description) {
        if (description == null) return false;
        final PersistableBundle extras = description.getExtras();
        return extras != null && extras.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false);
    }

    private void add(String text) {
        ensureLoaded();
        if (text.length() > MAX_TEXT_LENGTH) {
            text = text.substring(0, MAX_TEXT_LENGTH);
        }
        boolean pinned = false;
        for (final Iterator<Item> it = mItems.iterator(); it.hasNext();) {
            final Item item = it.next();
            if (item.mText.equals(text)) {
                pinned = item.mPinned;
                it.remove();
            }
        }
        final Item item = new Item(text, System.currentTimeMillis(), pinned);
        mItems.add(0, item);
        mSuggestion = item;
        trim();
        save();
        notifyChanged();
    }

    public List<Item> getItems() {
        ensureLoaded();
        if (expire()) {
            save();
        }
        final ArrayList<Item> result = new ArrayList<>(mItems.size());
        for (final Item item : mItems) if (item.mPinned) result.add(item);
        for (final Item item : mItems) if (!item.mPinned) result.add(item);
        return result;
    }

    public String getRecentSuggestion() {
        if (!mEnabled || mSuggestion == null) return null;
        if (System.currentTimeMillis() - mSuggestion.mTimestamp > SUGGESTION_WINDOW_MS) {
            mSuggestion = null;
            return null;
        }
        return mSuggestion.mText;
    }

    public void consumeSuggestion() {
        mSuggestion = null;
    }

    public void setPinned(final Item item, final boolean pinned) {
        item.mPinned = pinned;
        save();
        notifyChanged();
    }

    public void remove(final Item item) {
        mItems.remove(item);
        if (mSuggestion == item) mSuggestion = null;
        save();
        notifyChanged();
    }

    public void clearUnpinned() {
        mItems.removeIf(item -> !item.mPinned);
        mSuggestion = null;
        save();
        notifyChanged();
    }

    private boolean expire() {
        final long now = System.currentTimeMillis();
        return mItems.removeIf(item -> !item.mPinned && now - item.mTimestamp > EXPIRY_MS);
    }

    private void trim() {
        expire();
        int unpinned = 0;
        for (final Iterator<Item> it = mItems.iterator(); it.hasNext();) {
            final Item item = it.next();
            if (item.mPinned) continue;
            if (++unpinned > MAX_UNPINNED) it.remove();
        }
    }

    private void notifyChanged() {
        for (final Listener listener : mListeners) {
            listener.onClipboardHistoryChanged();
        }
    }

    private File getFile() {
        final UserManager um = mContext.getSystemService(UserManager.class);
        if (um == null || !um.isUserUnlocked()) return null;
        final String dataDir = mContext.getDataDir().getPath();
        final String ceDir = dataDir.replaceFirst("/user_de/", "/user/");
        if (ceDir.equals(dataDir) && mContext.isDeviceProtectedStorage()) return null;
        final File dir = new File(ceDir, "files");
        if (!dir.isDirectory() && !dir.mkdirs()) return null;
        return new File(dir, FILE_NAME);
    }

    private void ensureLoaded() {
        if (mLoaded) return;
        final File file = getFile();
        if (file == null) return;
        mLoaded = true;
        if (!file.exists()) return;
        try (FileInputStream in = new FileInputStream(file)) {
            final byte[] data = in.readAllBytes();
            final JSONArray array = new JSONArray(new String(data, StandardCharsets.UTF_8));
            final ArrayList<Item> loaded = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                final JSONObject o = array.getJSONObject(i);
                loaded.add(new Item(o.getString("text"), o.getLong("time"),
                        o.optBoolean("pinned")));
            }
            for (final Item item : loaded) {
                boolean duplicate = false;
                for (final Item existing : mItems) {
                    if (existing.mText.equals(item.mText)) {
                        existing.mPinned |= item.mPinned;
                        duplicate = true;
                    }
                }
                if (!duplicate) mItems.add(item);
            }
            expire();
        } catch (IOException | JSONException e) {
            Log.w(TAG, "Cannot read clipboard history", e);
        }
    }

    private void save() {
        final File file = getFile();
        if (file == null) return;
        final JSONArray array = new JSONArray();
        try {
            for (final Item item : mItems) {
                array.put(new JSONObject()
                        .put("text", item.mText)
                        .put("time", item.mTimestamp)
                        .put("pinned", item.mPinned));
            }
        } catch (JSONException e) {
            return;
        }
        final File tmp = new File(file.getPath() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(array.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.w(TAG, "Cannot write clipboard history", e);
            return;
        }
        if (!tmp.renameTo(file)) {
            tmp.delete();
        }
    }
}
