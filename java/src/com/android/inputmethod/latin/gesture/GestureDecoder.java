package com.android.inputmethod.latin.gesture;

import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import com.android.inputmethod.keyboard.Key;
import com.android.inputmethod.keyboard.Keyboard;
import com.android.inputmethod.latin.Dictionary;
import com.android.inputmethod.latin.DictionaryFacilitator;
import com.android.inputmethod.latin.SuggestedWords.SuggestedWordInfo;
import com.android.inputmethod.latin.common.InputPointers;
import com.android.inputmethod.latin.define.DebugFlags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class GestureDecoder {
    private static final String TAG = GestureDecoder.class.getSimpleName();
    private static final boolean DEBUG = DebugFlags.DEBUG_ENABLED;

    private static final int MAX_MAIN_LEXICON_WORDS = 100000;
    private static final int MAX_EXTRA_LEXICON_WORDS = 20000;
    private static final int MIN_EXTRA_WORD_PROBABILITY = 150;
    private static final long EXTRA_LEXICON_REFRESH_INTERVAL_MS = 60 * 1000;
    private static final long MAX_WAIT_FOR_LEXICON_MS = 2000;
    private static final int MAX_COMPILED_LEXICONS = 6;
    private static final int MAX_OFFENSIVE_CACHE_SIZE = 2000;

    private static final String[] EXTRA_DICTIONARY_TYPES = {
            Dictionary.TYPE_USER, Dictionary.TYPE_CONTACTS };

    private static volatile int sCorrectionX;
    private static volatile int sCorrectionY;

    public static void setInputCoordinateCorrection(final int correctionX,
            final int correctionY) {
        sCorrectionX = correctionX;
        sCorrectionY = correctionY;
    }

    private static final class LexiconEntry {
        final Dictionary mDictionary;
        final GestureLexicon mLexicon;
        final long mCreationTime;

        LexiconEntry(final Dictionary dictionary, final GestureLexicon lexicon) {
            mDictionary = dictionary;
            mLexicon = lexicon;
            mCreationTime = SystemClock.uptimeMillis();
        }
    }

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor(runnable -> {
        final Thread thread = new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            runnable.run();
        }, "GestureLexiconLoader");
        thread.setDaemon(true);
        return thread;
    });

    private final Object mLock = new Object();
    @Nullable private LexiconEntry mMainEntry;
    private final LexiconEntry[] mExtraEntries = new LexiconEntry[EXTRA_DICTIONARY_TYPES.length];
    private final LinkedHashMap<String, CompiledLexicon> mCompiled =
            new LinkedHashMap<>(16, 0.75f, true);
    @Nullable private Future<?> mPendingPreparation;
    private final HashMap<String, Boolean> mOffensiveCache = new HashMap<>();

    private final GestureRecognizer mRecognizer = new GestureRecognizer();
    private GestureKeyLayout mLastLayout;
    private Keyboard mLastKeyboard;
    private int[] mXs = new int[256];
    private int[] mYs = new int[256];

    public void prepare(@Nonnull final DictionaryFacilitator facilitator,
            @Nullable final Keyboard keyboard) {
        prepare(facilitator, keyboard == null ? null : createLayout(keyboard));
    }

    private void prepare(@Nonnull final DictionaryFacilitator facilitator,
            @Nullable final GestureKeyLayout layout) {
        synchronized (mLock) {
            if (mPendingPreparation != null && !mPendingPreparation.isDone()) {
                return;
            }
            if (!needsPreparationLocked(facilitator, layout)) {
                return;
            }
            mPendingPreparation = mExecutor.submit(() -> prepareInBackground(facilitator, layout));
        }
    }

    private boolean needsPreparationLocked(final DictionaryFacilitator facilitator,
            @Nullable final GestureKeyLayout layout) {
        final Dictionary mainDictionary =
                facilitator.getDictionaryForGesture(Dictionary.TYPE_MAIN);
        if (mainDictionary != null && mainDictionary.isInitialized()
                && (mMainEntry == null || mMainEntry.mDictionary != mainDictionary)) {
            return true;
        }
        final long now = SystemClock.uptimeMillis();
        for (int i = 0; i < EXTRA_DICTIONARY_TYPES.length; ++i) {
            final Dictionary dictionary =
                    facilitator.getDictionaryForGesture(EXTRA_DICTIONARY_TYPES[i]);
            final LexiconEntry entry = mExtraEntries[i];
            if (dictionary == null) {
                if (entry != null) return true;
                continue;
            }
            if (entry == null || entry.mDictionary != dictionary
                    || now - entry.mCreationTime > EXTRA_LEXICON_REFRESH_INTERVAL_MS) {
                return true;
            }
        }
        if (layout != null) {
            for (final LexiconEntry entry : getEntriesLocked()) {
                if (!mCompiled.containsKey(getCompiledKey(entry, layout))) return true;
            }
        }
        return false;
    }

    private void prepareInBackground(final DictionaryFacilitator facilitator,
            @Nullable final GestureKeyLayout layout) {
        final long startTime = SystemClock.uptimeMillis();
        final Dictionary mainDictionary =
                facilitator.getDictionaryForGesture(Dictionary.TYPE_MAIN);
        final LexiconEntry currentMain;
        synchronized (mLock) {
            currentMain = mMainEntry;
        }
        if (mainDictionary != null && mainDictionary.isInitialized()
                && (currentMain == null || currentMain.mDictionary != mainDictionary)) {
            final GestureLexicon lexicon = readLexicon(mainDictionary, MAX_MAIN_LEXICON_WORDS,
                    0);
            if (lexicon != null) {
                synchronized (mLock) {
                    mMainEntry = new LexiconEntry(mainDictionary, lexicon);
                    mOffensiveCache.clear();
                }
                if (DEBUG) {
                    Log.d(TAG, "Main lexicon: " + lexicon.size() + " words in "
                            + (SystemClock.uptimeMillis() - startTime) + "ms");
                }
            }
        }
        final long now = SystemClock.uptimeMillis();
        for (int i = 0; i < EXTRA_DICTIONARY_TYPES.length; ++i) {
            final Dictionary dictionary =
                    facilitator.getDictionaryForGesture(EXTRA_DICTIONARY_TYPES[i]);
            final LexiconEntry entry;
            synchronized (mLock) {
                entry = mExtraEntries[i];
            }
            if (dictionary == null) {
                synchronized (mLock) {
                    mExtraEntries[i] = null;
                }
                continue;
            }
            if (entry != null && entry.mDictionary == dictionary
                    && now - entry.mCreationTime <= EXTRA_LEXICON_REFRESH_INTERVAL_MS) {
                continue;
            }
            final GestureLexicon lexicon = readLexicon(dictionary, MAX_EXTRA_LEXICON_WORDS,
                    MIN_EXTRA_WORD_PROBABILITY);
            synchronized (mLock) {
                if (lexicon != null) {
                    mExtraEntries[i] = new LexiconEntry(dictionary, lexicon);
                } else if (entry != null && entry.mDictionary != dictionary) {
                    mExtraEntries[i] = null;
                }
            }
        }
        if (layout != null) {
            final List<LexiconEntry> entries;
            synchronized (mLock) {
                entries = getEntriesLocked();
            }
            for (final LexiconEntry entry : entries) {
                getOrCompile(entry, layout);
            }
        }
        if (DEBUG) {
            Log.d(TAG, "Preparation took " + (SystemClock.uptimeMillis() - startTime) + "ms");
        }
    }

    @Nullable
    private static GestureLexicon readLexicon(final Dictionary dictionary, final int maxWords,
            final int minProbability) {
        final GestureLexicon.Builder builder = new GestureLexicon.Builder();
        final boolean complete = dictionary.iterateWordsForGesture((word, probability) -> {
            builder.add(word, Math.max(minProbability, probability));
            return true;
        });
        if (!complete && builder.size() == 0) {
            return null;
        }
        return builder.build(maxWords);
    }

    private List<LexiconEntry> getEntriesLocked() {
        final ArrayList<LexiconEntry> entries = new ArrayList<>();
        if (mMainEntry != null) entries.add(mMainEntry);
        for (final LexiconEntry entry : mExtraEntries) {
            if (entry != null && entry.mLexicon.size() > 0) entries.add(entry);
        }
        return entries;
    }

    private static String getCompiledKey(final LexiconEntry entry,
            final GestureKeyLayout layout) {
        return System.identityHashCode(entry.mLexicon) + "@" + layout.getSignature();
    }

    private CompiledLexicon getOrCompile(final LexiconEntry entry,
            final GestureKeyLayout layout) {
        final String key = getCompiledKey(entry, layout);
        synchronized (mLock) {
            final CompiledLexicon compiled = mCompiled.get(key);
            if (compiled != null && compiled.mLexicon == entry.mLexicon) return compiled;
        }
        final long startTime = SystemClock.uptimeMillis();
        final CompiledLexicon compiled = new CompiledLexicon(entry.mLexicon, layout);
        if (DEBUG) {
            Log.d(TAG, "Compiled " + entry.mLexicon.size() + " words in "
                    + (SystemClock.uptimeMillis() - startTime) + "ms");
        }
        synchronized (mLock) {
            mCompiled.put(key, compiled);
            while (mCompiled.size() > MAX_COMPILED_LEXICONS) {
                final String eldest = mCompiled.keySet().iterator().next();
                mCompiled.remove(eldest);
            }
        }
        return compiled;
    }

    @Nullable
    private CompiledLexicon getCompiledOrNull(final LexiconEntry entry,
            final GestureKeyLayout layout) {
        synchronized (mLock) {
            final CompiledLexicon compiled = mCompiled.get(getCompiledKey(entry, layout));
            return compiled != null && compiled.mLexicon == entry.mLexicon ? compiled : null;
        }
    }

    private GestureKeyLayout createLayout(final Keyboard keyboard) {
        final List<Key> keys = keyboard.getSortedKeys();
        final int count = keys.size();
        final int[] codes = new int[count];
        final int[] x = new int[count];
        final int[] y = new int[count];
        final int[] width = new int[count];
        final int[] height = new int[count];
        for (int i = 0; i < count; ++i) {
            final Key key = keys.get(i);
            codes[i] = key.getCode();
            x[i] = key.getX();
            y[i] = key.getY();
            width[i] = key.getWidth();
            height[i] = key.getHeight();
        }
        return new GestureKeyLayout(codes, x, y, width, height, count,
                keyboard.mMostCommonKeyWidth);
    }

    private GestureKeyLayout getLayout(final Keyboard keyboard) {
        if (keyboard != mLastKeyboard || mLastLayout == null) {
            mLastLayout = createLayout(keyboard);
            mLastKeyboard = keyboard;
        }
        return mLastLayout;
    }

    @Nonnull
    public synchronized ArrayList<SuggestedWordInfo> getSuggestions(
            @Nonnull final InputPointers inputPointers, @Nonnull final Keyboard keyboard,
            @Nonnull final DictionaryFacilitator facilitator, final boolean blockOffensiveWords,
            final boolean isTailBatchInput, final int maxResults) {
        final ArrayList<SuggestedWordInfo> suggestions = new ArrayList<>();
        final long startTime = SystemClock.uptimeMillis();
        final GestureKeyLayout layout = getLayout(keyboard);
        if (layout.getKeyCount() == 0) {
            return suggestions;
        }
        prepare(facilitator, layout);
        List<LexiconEntry> entries;
        synchronized (mLock) {
            entries = getEntriesLocked();
        }
        CompiledLexicon[] lexicons = getCompiledLexicons(entries, layout);
        final long deadline = startTime + MAX_WAIT_FOR_LEXICON_MS;
        while (isTailBatchInput && (entries.isEmpty() || lexicons.length < entries.size())) {
            prepare(facilitator, layout);
            final Future<?> pending;
            synchronized (mLock) {
                pending = mPendingPreparation;
            }
            final long remaining = deadline - SystemClock.uptimeMillis();
            if (pending == null || pending.isDone() || remaining <= 0) {
                break;
            }
            try {
                pending.get(remaining, TimeUnit.MILLISECONDS);
            } catch (final InterruptedException | ExecutionException | TimeoutException e) {
                Log.w(TAG, "Gesture lexicon not ready", e);
                break;
            }
            synchronized (mLock) {
                entries = getEntriesLocked();
            }
            lexicons = getCompiledLexicons(entries, layout);
        }
        if (isTailBatchInput) {
            synchronized (mLock) {
                entries = getEntriesLocked();
            }
            lexicons = getCompiledLexicons(entries, layout);
        }
        if (lexicons.length == 0) {
            return suggestions;
        }

        final int size = inputPointers.getPointerSize();
        final int[] xs = inputPointers.getXCoordinates();
        final int[] ys = inputPointers.getYCoordinates();
        final int[] pointerIds = inputPointers.getPointerIds();
        if (size <= 0 || xs.length < size || ys.length < size) {
            return suggestions;
        }
        if (mXs.length < size) {
            mXs = new int[size];
            mYs = new int[size];
        }
        final int correctionX = sCorrectionX;
        final int correctionY = sCorrectionY;
        final int pointerId = pointerIds.length > 0 ? pointerIds[0] : 0;
        int count = 0;
        for (int i = 0; i < size; ++i) {
            if (i < pointerIds.length && pointerIds[i] != pointerId) continue;
            mXs[count] = xs[i] + correctionX;
            mYs[count] = ys[i] + correctionY;
            ++count;
        }

        final GestureRecognizer.Candidate[] candidates = mRecognizer.recognize(mXs, mYs, count,
                lexicons, maxResults + 4);
        final Dictionary mainDictionary = entries.isEmpty() ? null : entries.get(0).mDictionary;
        for (final GestureRecognizer.Candidate candidate : candidates) {
            if (suggestions.size() >= maxResults) break;
            final Dictionary sourceDictionary = findSourceDictionary(entries, candidate.mLexicon);
            if (sourceDictionary == null) continue;
            final String word = candidate.getWord();
            if (blockOffensiveWords && sourceDictionary == mainDictionary
                    && isPossiblyOffensive(sourceDictionary, word)) {
                continue;
            }
            final int score = Math.max(1, (int) (1000000 - candidate.mCost * 1000));
            suggestions.add(new SuggestedWordInfo(word, "", score,
                    SuggestedWordInfo.KIND_CORRECTION, sourceDictionary,
                    SuggestedWordInfo.NOT_AN_INDEX,
                    SuggestedWordInfo.NOT_A_CONFIDENCE));
        }
        if (DEBUG) {
            Log.d(TAG, "Decoded " + count + " points in "
                    + (SystemClock.uptimeMillis() - startTime) + "ms, "
                    + mRecognizer.getEvaluatedCount() + " candidates scored: "
                    + (suggestions.isEmpty() ? "-" : suggestions.get(0).mWord));
        }
        return suggestions;
    }

    private CompiledLexicon[] getCompiledLexicons(final List<LexiconEntry> entries,
            final GestureKeyLayout layout) {
        final ArrayList<CompiledLexicon> lexicons = new ArrayList<>(entries.size());
        for (final LexiconEntry entry : entries) {
            final CompiledLexicon compiled = getCompiledOrNull(entry, layout);
            if (compiled != null) lexicons.add(compiled);
        }
        return lexicons.toArray(new CompiledLexicon[lexicons.size()]);
    }

    @Nullable
    private static Dictionary findSourceDictionary(final List<LexiconEntry> entries,
            final CompiledLexicon lexicon) {
        for (final LexiconEntry entry : entries) {
            if (entry.mLexicon == lexicon.mLexicon) return entry.mDictionary;
        }
        return null;
    }

    private boolean isPossiblyOffensive(final Dictionary dictionary, final String word) {
        synchronized (mLock) {
            final Boolean cached = mOffensiveCache.get(word);
            if (cached != null) return cached;
        }
        final boolean offensive = dictionary.isPossiblyOffensiveWord(word);
        synchronized (mLock) {
            if (mOffensiveCache.size() >= MAX_OFFENSIVE_CACHE_SIZE) mOffensiveCache.clear();
            mOffensiveCache.put(word, offensive);
        }
        return offensive;
    }
}
