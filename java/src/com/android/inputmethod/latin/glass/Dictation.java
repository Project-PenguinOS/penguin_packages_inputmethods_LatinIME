package com.android.inputmethod.latin.glass;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputConnection;

import com.android.inputmethod.latin.permissions.PermissionsManager;
import com.android.inputmethod.latin.permissions.PermissionsUtil;

import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Supplier;

public final class Dictation implements RecognitionListener {
    private static final String TAG = Dictation.class.getSimpleName();

    public interface Callback {
        void onDictationStateChanged(boolean listening);

        void onDictationLevel(float level);

        void onDictationUnavailable();
    }

    private final Context mContext;
    private final Supplier<InputConnection> mInputConnection;
    private final Supplier<Locale> mLocale;
    private final Callback mCallback;
    private SpeechRecognizer mRecognizer;
    private boolean mListening;
    private boolean mHasComposing;

    public Dictation(final Context context, final Supplier<InputConnection> inputConnection,
            final Supplier<Locale> locale, final Callback callback) {
        mContext = context;
        mInputConnection = inputConnection;
        mLocale = locale;
        mCallback = callback;
    }

    public boolean isListening() {
        return mListening;
    }

    public void toggle() {
        if (mListening) {
            stop();
        } else {
            start();
        }
    }

    public void start() {
        if (!SpeechRecognizer.isRecognitionAvailable(mContext)) {
            mCallback.onDictationUnavailable();
            return;
        }
        if (!PermissionsUtil.checkAllPermissionsGranted(mContext,
                Manifest.permission.RECORD_AUDIO)) {
            PermissionsManager.get(mContext).requestPermissions(granted -> {
                if (granted) start();
            }, null, Manifest.permission.RECORD_AUDIO);
            return;
        }
        if (mRecognizer == null) {
            try {
                mRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext);
                mRecognizer.setRecognitionListener(this);
            } catch (RuntimeException e) {
                Log.w(TAG, "Cannot create speech recognizer", e);
                mCallback.onDictationUnavailable();
                return;
            }
        }
        final InputConnection ic = mInputConnection.get();
        if (ic != null) ic.finishComposingText();
        final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, mLocale.get().toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, mContext.getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                2000);
        try {
            mRecognizer.startListening(intent);
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot start dictation", e);
            mCallback.onDictationUnavailable();
            return;
        }
        setListening(true);
    }

    public void stop() {
        if (mRecognizer != null && mListening) {
            mRecognizer.stopListening();
        }
    }

    public void cancel() {
        if (mRecognizer != null) {
            mRecognizer.cancel();
        }
        finishComposing();
        mPhraseStart = null;
        setListening(false);
    }

    public void destroy() {
        cancel();
        if (mRecognizer != null) {
            mRecognizer.destroy();
            mRecognizer = null;
        }
    }

    private void setListening(final boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
        mCallback.onDictationStateChanged(listening);
    }

    private void finishComposing() {
        if (!mHasComposing) return;
        final InputConnection ic = mInputConnection.get();
        if (ic != null) ic.finishComposingText();
        mHasComposing = false;
    }

    private String fitToContext(String text) {
        text = text.trim();
        if (text.isEmpty()) return text;
        final InputConnection ic = mInputConnection.get();
        final CharSequence before = ic == null ? null : ic.getTextBeforeCursor(3, 0);
        final String prev = before == null ? "" : before.toString();
        final String trimmed = prev.trim();
        final boolean sentenceStart = trimmed.isEmpty() || trimmed.endsWith(".")
                || trimmed.endsWith("!") || trimmed.endsWith("?") || prev.endsWith("\n");
        if (sentenceStart) {
            text = text.substring(0, 1).toUpperCase(mLocale.get()) + text.substring(1);
        }
        if (!prev.isEmpty() && !Character.isWhitespace(prev.charAt(prev.length() - 1))) {
            text = " " + text;
        }
        return text;
    }

    private static String best(final Bundle results) {
        final ArrayList<String> list =
                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    @Override
    public void onPartialResults(final Bundle partialResults) {
        final String text = best(partialResults);
        if (TextUtils.isEmpty(text)) return;
        final InputConnection ic = mInputConnection.get();
        if (ic == null) return;
        ic.setComposingText(fitToContextKeepingPhrase(text), 1);
        mHasComposing = true;
    }

    private String mPhraseStart;

    private String fitToContextKeepingPhrase(final String text) {
        if (mPhraseStart == null) {
            mPhraseStart = fitToContext(text);
            return mPhraseStart;
        }
        final String trimmed = text.trim();
        if (trimmed.isEmpty()) return trimmed;
        final boolean leadingSpace = mPhraseStart.startsWith(" ");
        final String head = leadingSpace ? mPhraseStart.substring(1) : mPhraseStart;
        final boolean capital = !head.isEmpty() && Character.isUpperCase(head.charAt(0));
        String result = capital
                ? trimmed.substring(0, 1).toUpperCase(mLocale.get()) + trimmed.substring(1)
                : trimmed;
        return leadingSpace ? " " + result : result;
    }

    @Override
    public void onResults(final Bundle results) {
        final String text = best(results);
        final InputConnection ic = mInputConnection.get();
        if (ic != null && !TextUtils.isEmpty(text)) {
            final String fitted = fitToContextKeepingPhrase(text);
            if (mHasComposing) {
                ic.setComposingText(fitted, 1);
                ic.finishComposingText();
            } else {
                ic.commitText(fitted, 1);
            }
        } else {
            finishComposing();
        }
        mHasComposing = false;
        mPhraseStart = null;
        setListening(false);
    }

    @Override
    public void onError(final int error) {
        Log.d(TAG, "Dictation error " + error);
        finishComposing();
        mPhraseStart = null;
        setListening(false);
        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY
                || error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE
                || error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED) {
            mCallback.onDictationUnavailable();
        }
    }

    @Override
    public void onRmsChanged(final float rmsdB) {
        mCallback.onDictationLevel(Math.max(0f, Math.min(1f, (rmsdB + 2f) / 12f)));
    }

    @Override public void onReadyForSpeech(final Bundle params) {}
    @Override public void onBeginningOfSpeech() {}
    @Override public void onBufferReceived(final byte[] buffer) {}
    @Override public void onEndOfSpeech() {}
    @Override public void onEvent(final int eventType, final Bundle params) {}
}
