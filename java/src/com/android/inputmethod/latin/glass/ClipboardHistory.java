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
    private static final long MAX_IMAGE_BYTES = 20L * 1024 * 1024;
    private static final java.util.concurrent.Executor IO_EXECUTOR =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private static final android.os.Handler MAIN =
            new android.os.Handler(android.os.Looper.getMainLooper());

    public static final class Item {
        public final String mText;
        public final String mImage;
        public final String mMimeType;
        public final long mTimestamp;
        public boolean mPinned;

        Item(final String text, final long timestamp, final boolean pinned) {
            this(text, null, null, timestamp, pinned);
        }

        Item(final String text, final String image, final String mimeType, final long timestamp,
                final boolean pinned) {
            mText = text;
            mImage = image;
            mMimeType = mimeType;
            mTimestamp = timestamp;
            mPinned = pinned;
        }

        public boolean isImage() {
            return mImage != null;
        }

        boolean sameContent(final Item other) {
            return isImage() ? mImage.equals(other.mImage) : !other.isImage()
                    && mText.equals(other.mText);
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
        final android.net.Uri uri = clip.getItemAt(0).getUri();
        final String imageMime = imageMimeType(description, uri);
        if (uri != null && imageMime != null) {
            final long now = System.currentTimeMillis();
            final String ext = imageMime.endsWith("png") ? "png"
                    : imageMime.endsWith("gif") ? "gif"
                    : imageMime.endsWith("webp") ? "webp" : "jpg";
            final String name = "img_" + now + "." + ext;
            IO_EXECUTOR.execute(() -> {
                final File dir = getClipsDir();
                if (dir == null) return;
                try (java.io.InputStream in =
                        mContext.getContentResolver().openInputStream(uri);
                        FileOutputStream out = new FileOutputStream(new File(dir, name))) {
                    if (in == null) return;
                    final byte[] buffer = new byte[64 * 1024];
                    long total = 0;
                    int n;
                    while ((n = in.read(buffer)) > 0) {
                        out.write(buffer, 0, n);
                        total += n;
                        if (total > MAX_IMAGE_BYTES) throw new IOException("image too large");
                    }
                } catch (IOException | SecurityException e) {
                    Log.w(TAG, "Cannot keep copied image", e);
                    new File(dir, name).delete();
                    return;
                }
                MAIN.post(() -> addItem(new Item("", name, imageMime, now, false)));
            });
            return;
        }
        final CharSequence text = clip.getItemAt(0).coerceToText(mContext);
        if (TextUtils.isEmpty(text) || TextUtils.getTrimmedLength(text) == 0) return;
        add(text.toString());
    }

    private static String imageMimeType(final ClipDescription description,
            final android.net.Uri uri) {
        if (description == null || uri == null) return null;
        for (int i = 0; i < description.getMimeTypeCount(); i++) {
            final String mime = description.getMimeType(i);
            if (mime != null && mime.startsWith("image/")) return mime;
        }
        return null;
    }

    private static boolean isSensitive(final ClipDescription description) {
        if (description == null) return false;
        final PersistableBundle extras = description.getExtras();
        return extras != null && extras.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false);
    }

    private void add(String text) {
        if (text.length() > MAX_TEXT_LENGTH) {
            text = text.substring(0, MAX_TEXT_LENGTH);
        }
        addItem(new Item(text, System.currentTimeMillis(), false));
    }

    private void addItem(Item item) {
        ensureLoaded();
        boolean pinned = false;
        for (final Iterator<Item> it = mItems.iterator(); it.hasNext();) {
            final Item existing = it.next();
            if (existing.sameContent(item)) {
                pinned = existing.mPinned;
                it.remove();
            }
        }
        item.mPinned = pinned;
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
        if (mSuggestion.isImage()
                || System.currentTimeMillis() - mSuggestion.mTimestamp > SUGGESTION_WINDOW_MS) {
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
        deleteImage(item);
        if (mSuggestion == item) mSuggestion = null;
        save();
        notifyChanged();
    }

    public void clearUnpinned() {
        for (final Item item : mItems) if (!item.mPinned) deleteImage(item);
        mItems.removeIf(item -> !item.mPinned);
        mSuggestion = null;
        save();
        notifyChanged();
    }

    private boolean expire() {
        final long now = System.currentTimeMillis();
        boolean removed = false;
        for (final Iterator<Item> it = mItems.iterator(); it.hasNext();) {
            final Item item = it.next();
            if (!item.mPinned && now - item.mTimestamp > EXPIRY_MS) {
                deleteImage(item);
                it.remove();
                removed = true;
            }
        }
        return removed;
    }

    private void trim() {
        expire();
        int unpinned = 0;
        for (final Iterator<Item> it = mItems.iterator(); it.hasNext();) {
            final Item item = it.next();
            if (item.mPinned) continue;
            if (++unpinned > MAX_UNPINNED) {
                deleteImage(item);
                it.remove();
            }
        }
    }

    private void notifyChanged() {
        for (final Listener listener : mListeners) {
            listener.onClipboardHistoryChanged();
        }
    }

    static File getCeFilesDir(final Context context) {
        final UserManager um = context.getSystemService(UserManager.class);
        if (um == null || !um.isUserUnlocked()) return null;
        final String dataDir = context.getDataDir().getPath();
        final String ceDir = dataDir.replaceFirst("/user_de/", "/user/");
        if (ceDir.equals(dataDir) && context.isDeviceProtectedStorage()) return null;
        final File dir = new File(ceDir, "files");
        if (!dir.isDirectory() && !dir.mkdirs()) return null;
        return dir;
    }

    static File getClipsDir(final Context context) {
        final File files = getCeFilesDir(context);
        if (files == null) return null;
        final File dir = new File(files, "clips");
        if (!dir.isDirectory() && !dir.mkdirs()) return null;
        return dir;
    }

    private File getClipsDir() {
        return getClipsDir(mContext);
    }

    public File getImageFile(final Item item) {
        if (!item.isImage()) return null;
        final File dir = getClipsDir();
        return dir == null ? null : new File(dir, item.mImage);
    }

    private void deleteImage(final Item item) {
        final File file = getImageFile(item);
        if (file != null) {
            IO_EXECUTOR.execute(file::delete);
        }
    }

    private File getFile() {
        final File dir = getCeFilesDir(mContext);
        return dir == null ? null : new File(dir, FILE_NAME);
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
                loaded.add(new Item(o.getString("text"),
                        o.has("image") ? o.getString("image") : null,
                        o.has("mime") ? o.getString("mime") : null,
                        o.getLong("time"), o.optBoolean("pinned")));
            }
            for (final Item item : loaded) {
                boolean duplicate = false;
                for (final Item existing : mItems) {
                    if (existing.sameContent(item)) {
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
                final JSONObject o = new JSONObject()
                        .put("text", item.mText)
                        .put("time", item.mTimestamp)
                        .put("pinned", item.mPinned);
                if (item.isImage()) {
                    o.put("image", item.mImage).put("mime", item.mMimeType);
                }
                array.put(o);
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
