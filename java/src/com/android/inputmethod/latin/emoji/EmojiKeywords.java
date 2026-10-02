package com.android.inputmethod.latin.emoji;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class EmojiKeywords {
    private static final String TAG = EmojiKeywords.class.getSimpleName();
    private static final String ASSET = "emoji/en.tsv";

    private static final Object sLock = new Object();
    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());
    private static final ArrayList<Runnable> sPendingCallbacks = new ArrayList<>();
    private static Context sAppContext;
    private static boolean sLoading;
    private static volatile Index sIndex;

    private EmojiKeywords() {}

    public static void init(final Context context) {
        synchronized (sLock) {
            if (sAppContext == null) {
                sAppContext = context.getApplicationContext();
            }
        }
    }

    public static boolean isLoaded() {
        return sIndex != null;
    }

    public static void load(final Runnable onLoaded) {
        synchronized (sLock) {
            if (sIndex != null) {
                if (onLoaded != null) sMainHandler.post(onLoaded);
                return;
            }
            if (onLoaded != null) sPendingCallbacks.add(onLoaded);
            if (sLoading || sAppContext == null) return;
            sLoading = true;
        }
        final Context context = sAppContext;
        final Thread loader = new Thread(() -> {
            Index index = null;
            try {
                index = Index.read(context);
            } catch (IOException e) {
                Log.e(TAG, "Can't read " + ASSET, e);
            }
            final ArrayList<Runnable> callbacks;
            synchronized (sLock) {
                sIndex = index != null ? index : Index.EMPTY;
                sLoading = false;
                callbacks = new ArrayList<>(sPendingCallbacks);
                sPendingCallbacks.clear();
            }
            for (final Runnable r : callbacks) sMainHandler.post(r);
        }, "EmojiKeywords");
        loader.setPriority(Thread.MIN_PRIORITY);
        loader.start();
    }

    private static Index index() {
        final Index index = sIndex;
        if (index == null) {
            load(null);
        }
        return index;
    }

    public static List<String> search(final CharSequence query, final int max) {
        final Index index = index();
        if (index == null || max <= 0) return Collections.emptyList();
        return index.search(normalize(query), max);
    }

    public static List<String> exactMatches(final CharSequence word, final int max) {
        final Index index = index();
        if (index == null || max <= 0) return Collections.emptyList();
        return index.exact(normalize(word), max);
    }

    public static List<String> popular(final int max) {
        final Index index = index();
        if (index == null) return Collections.emptyList();
        final int n = Math.min(max, index.mEmoji.length);
        return Arrays.asList(index.mEmoji).subList(0, n);
    }

    static String normalize(final CharSequence s) {
        if (s == null) return "";
        return s.toString().trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    static ArrayList<String> words(final String s) {
        final ArrayList<String> out = new ArrayList<>();
        int start = -1;
        for (int i = 0; i < s.length(); ) {
            final int cp = s.codePointAt(i);
            final boolean wordChar = Character.isLetterOrDigit(cp);
            if (wordChar && start < 0) {
                start = i;
            } else if (!wordChar && start >= 0) {
                out.add(s.substring(start, i));
                start = -1;
            }
            i += Character.charCount(cp);
        }
        if (start >= 0) out.add(s.substring(start));
        return out;
    }

    private static final class Index {
        static final Index EMPTY = new Index(new String[0], new String[0][],
                new String[0], new int[0][], new HashMap<>());

        final String[] mEmoji;
        final String[][] mTerms;
        final String[] mWords;
        final int[][] mWordEmoji;
        final HashMap<String, int[]> mExact;

        Index(final String[] emoji, final String[][] terms, final String[] words,
                final int[][] wordEmoji, final HashMap<String, int[]> exact) {
            mEmoji = emoji;
            mTerms = terms;
            mWords = words;
            mWordEmoji = wordEmoji;
            mExact = exact;
        }

        static Index read(final Context context) throws IOException {
            final ArrayList<String> emoji = new ArrayList<>();
            final ArrayList<String[]> terms = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    context.getAssets().open(ASSET), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    final int tab = line.indexOf('\t');
                    if (tab <= 0) continue;
                    emoji.add(line.substring(0, tab));
                    terms.add(TextUtils.split(line.substring(tab + 1), "\\|"));
                }
            }
            final TreeMap<String, ArrayList<Integer>> words = new TreeMap<>();
            final HashMap<String, ArrayList<Integer>> exact = new HashMap<>();
            for (int id = 0; id < terms.size(); id++) {
                for (final String term : terms.get(id)) {
                    addId(exact, term, id);
                    for (final String w : words(term)) {
                        addId(words, w, id);
                    }
                }
            }
            final String[] wordArray = words.keySet().toArray(new String[0]);
            final int[][] wordEmoji = new int[wordArray.length][];
            int i = 0;
            for (final ArrayList<Integer> ids : words.values()) {
                wordEmoji[i++] = toArray(ids);
            }
            final HashMap<String, int[]> exactArrays = new HashMap<>(exact.size() * 2);
            for (final Map.Entry<String, ArrayList<Integer>> e : exact.entrySet()) {
                exactArrays.put(e.getKey(), toArray(e.getValue()));
            }
            return new Index(emoji.toArray(new String[0]), terms.toArray(new String[0][]),
                    wordArray, wordEmoji, exactArrays);
        }

        private static void addId(final Map<String, ArrayList<Integer>> map, final String key,
                final int id) {
            ArrayList<Integer> ids = map.get(key);
            if (ids == null) {
                ids = new ArrayList<>(2);
                map.put(key, ids);
            }
            if (ids.isEmpty() || ids.get(ids.size() - 1) != id) {
                ids.add(id);
            }
        }

        private static int[] toArray(final ArrayList<Integer> list) {
            final int[] a = new int[list.size()];
            for (int i = 0; i < a.length; i++) a[i] = list.get(i);
            return a;
        }

        List<String> exact(final String q, final int max) {
            final int[] ids = mExact.get(q);
            if (ids == null) return Collections.emptyList();
            final ArrayList<String> named = new ArrayList<>();
            final ArrayList<String> others = new ArrayList<>();
            for (final int id : ids) {
                (mTerms[id].length > 0 && mTerms[id][0].equals(q) ? named : others)
                        .add(mEmoji[id]);
            }
            named.addAll(others);
            return named.size() > max ? new ArrayList<>(named.subList(0, max)) : named;
        }

        List<String> search(final String q, final int max) {
            if (q.isEmpty()) return Collections.emptyList();
            final ArrayList<String> tokens = words(q);
            if (tokens.isEmpty()) {
                return exact(q, max);
            }
            final int n = mEmoji.length;
            final int[] matched = new int[n];
            final int[] stamp = new int[n];
            for (int t = 0; t < tokens.size(); t++) {
                final String token = tokens.get(t);
                int w = lowerBound(token);
                for (; w < mWords.length && mWords[w].startsWith(token); w++) {
                    for (final int id : mWordEmoji[w]) {
                        if (stamp[id] != t + 1 && matched[id] == t) {
                            stamp[id] = t + 1;
                            matched[id]++;
                        }
                    }
                }
            }
            final int need = tokens.size();
            final ArrayList<int[]> hits = new ArrayList<>();
            for (int id = 0; id < n; id++) {
                if (matched[id] != need) continue;
                int score = 1;
                final String[] terms = mTerms[id];
                for (int k = 0; k < terms.length; k++) {
                    if (terms[k].equals(q)) {
                        score = Math.max(score, k == 0 ? 4 : 3);
                    } else if (terms[k].startsWith(q)) {
                        score = Math.max(score, 2);
                    }
                }
                hits.add(new int[] { score, id });
            }
            Collections.sort(hits, (a, b) -> a[0] != b[0] ? b[0] - a[0] : a[1] - b[1]);
            final ArrayList<String> out = new ArrayList<>(Math.min(max, hits.size()));
            for (int i = 0; i < hits.size() && i < max; i++) {
                out.add(mEmoji[hits.get(i)[1]]);
            }
            return out;
        }

        private int lowerBound(final String key) {
            int lo = 0;
            int hi = mWords.length;
            while (lo < hi) {
                final int mid = (lo + hi) >>> 1;
                if (mWords[mid].compareTo(key) < 0) {
                    lo = mid + 1;
                } else {
                    hi = mid;
                }
            }
            return lo;
        }
    }
}
