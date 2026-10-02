package com.android.inputmethod.latin.emoji;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.android.inputmethod.latin.Dictionary;
import com.android.inputmethod.latin.SuggestedWords;
import com.android.inputmethod.latin.SuggestedWords.SuggestedWordInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class EmojiSuggestions {
    public static final int MAX_EMOJI_PER_WORD = 3;
    private static final int STRIP_INDEX = 3;
    private static final int MIN_WORD_LENGTH = 2;

    private static volatile boolean sEnabled = true;
    private static volatile SharedPreferences sPrefs;

    private EmojiSuggestions() {}

    public static void init(final Context context) {
        EmojiKeywords.init(context);
        EmojiSkinTones.init(context);
        sPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void setEnabled(final boolean enabled) {
        sEnabled = enabled;
    }

    public static List<String> forWord(final CharSequence word, final int max) {
        if (!sEnabled || word == null || word.length() < MIN_WORD_LENGTH) {
            return Collections.emptyList();
        }
        final List<String> emoji = EmojiKeywords.exactMatches(word, max);
        final SharedPreferences prefs = sPrefs;
        if (prefs == null || emoji.isEmpty()) return emoji;
        final ArrayList<String> toned = new ArrayList<>(emoji.size());
        for (final String e : emoji) {
            toned.add(EmojiSkinTones.applyPreferred(prefs, e));
        }
        return toned;
    }

    public static boolean addTo(final ArrayList<SuggestedWordInfo> suggestions,
            final String typedWord, final Locale locale, final boolean willAutoCorrect) {
        if (suggestions.isEmpty() || TextUtils.isEmpty(typedWord) || locale == null
                || !"en".equals(locale.getLanguage())) {
            return willAutoCorrect;
        }
        final List<String> emoji = forWord(typedWord, MAX_EMOJI_PER_WORD);
        if (emoji.isEmpty()) return willAutoCorrect;
        boolean autoCorrect = willAutoCorrect;
        for (int i = 0; i < emoji.size(); i++) {
            final int index = i == 0 ? Math.min(STRIP_INDEX, suggestions.size())
                    : suggestions.size();
            if (index == SuggestedWords.INDEX_OF_AUTO_CORRECTION) {
                autoCorrect = false;
            }
            suggestions.add(index, new SuggestedWordInfo(emoji.get(i), "",
                    0, SuggestedWordInfo.KIND_EMOJI, Dictionary.DICTIONARY_HARDCODED,
                    SuggestedWordInfo.NOT_AN_INDEX,
                    SuggestedWordInfo.NOT_A_CONFIDENCE));
        }
        return autoCorrect;
    }

    public static boolean isEmoji(final CharSequence text) {
        if (TextUtils.isEmpty(text)) return false;
        final String s = text.toString();
        final int cp = s.codePointAt(0);
        if (cp >= 0x1F000) return true;
        if (Character.getType(cp) == Character.OTHER_SYMBOL) return true;
        final int next = Character.charCount(cp);
        return s.length() > next && (s.charAt(next) == '️' || s.charAt(next) == '⃣');
    }
}
