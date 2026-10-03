package com.android.inputmethod.latin.settings;

import androidx.appcompat.widget.SwitchCompat;

import com.android.inputmethod.latin.AudioAndHapticFeedbackManager;
import com.android.inputmethod.latin.R;
import com.android.inputmethod.latin.RichInputMethodManager;
import com.android.inputmethod.latin.settings.hig.HigList;

final class PreferencesSettingsScreen extends SettingsScreen {

    private SwitchCompat mNumberRowPassword;

    PreferencesSettingsScreen(final SettingsActivity activity) {
        super(activity);
    }

    @Override
    CharSequence getTitle() {
        return str(R.string.settings_screen_preferences);
    }

    @Override
    void onResume() {
        if (mRes.getBoolean(R.bool.config_enable_show_voice_key_option)) {
            RichInputMethodManager.getInstance().refreshSubtypeCaches();
        }
    }

    @Override
    void build(final HigList hig) {
        hig.header(str(R.string.hig_section_typing));
        final HigList.Group typing = hig.group();
        switchPref(typing, 0, Settings.PREF_AUTO_CAP, R.string.auto_cap,
                R.string.auto_cap_summary, true);
        switchPref(typing, 0, Settings.PREF_KEY_USE_DOUBLE_SPACE_PERIOD,
                R.string.use_double_space_period, R.string.use_double_space_period_summary, true);
        if (mRes.getBoolean(R.bool.config_enable_show_voice_key_option)) {
            switchPref(typing, 0, Settings.PREF_VOICE_INPUT_KEY, R.string.voice_input, 0, true);
        }

        hig.header(str(R.string.hig_section_layout));
        final HigList.Group layout = hig.group();
        switchPref(layout, 0, Settings.PREF_SHOW_NUMBER_ROW, R.string.number_row,
                R.string.number_row_summary, false);
        mNumberRowPassword = switchPref(layout, 0, Settings.PREF_SHOW_NUMBER_ROW_PASSWORD,
                R.string.number_row_password, R.string.number_row_password_summary, false);
        switchPref(layout, 0, Settings.PREF_SHOW_PUNCTUATION_KEYS,
                R.string.glass_settings_punctuation_keys,
                R.string.glass_settings_punctuation_keys_summary, false);
        switchPref(layout, 0, Settings.PREF_SHOW_LONGPRESS_HINTS, R.string.show_longpress_hints,
                R.string.show_longpress_hints_summary, true);
        updateNumberRowPassword();

        hig.header(str(R.string.hig_section_toolbar));
        final HigList.Group toolbar = hig.group();
        switchPref(toolbar, 0, Settings.PREF_SHOW_TOOLBAR, R.string.glass_settings_toolbar,
                R.string.glass_settings_toolbar_summary, true);
        switchPref(toolbar, 0, Settings.PREF_EMOJI_SUGGESTIONS,
                R.string.glass_settings_emoji_suggestions,
                R.string.glass_settings_emoji_suggestions_summary, true);
        switchPref(toolbar, 0, Settings.PREF_CLIPBOARD_HISTORY,
                R.string.glass_settings_clipboard_history,
                R.string.glass_settings_clipboard_history_summary, true);
        switchPref(toolbar, 0, Settings.PREF_CLIPBOARD_SUGGESTION,
                R.string.glass_settings_clipboard_suggestion, 0, true);

        hig.header(str(R.string.hig_section_key_feedback));
        final HigList.Group feedback = hig.group();
        if (AudioAndHapticFeedbackManager.getInstance().hasVibrator()) {
            switchPref(feedback, 0, Settings.PREF_VIBRATE_ON, R.string.vibrate_on_keypress, 0,
                    mRes.getBoolean(R.bool.config_default_vibration_enabled));
        }
        switchPref(feedback, 0, Settings.PREF_SOUND_ON, R.string.sound_on_keypress, 0,
                mRes.getBoolean(R.bool.config_default_sound_enabled));
        if (Settings.readFromBuildConfigIfToShowKeyPreviewPopupOption(mRes)) {
            switchPref(feedback, 0, Settings.PREF_POPUP_ON, R.string.popup_on_keypress, 0,
                    mRes.getBoolean(R.bool.config_default_key_preview_popup));
        }
    }

    @Override
    void onSharedPreferenceChanged(final String key) {
        if (Settings.PREF_SHOW_NUMBER_ROW.equals(key)) {
            updateNumberRowPassword();
        }
    }

    private void updateNumberRowPassword() {
        if (mNumberRowPassword != null) {
            HigList.setRowEnabled(mNumberRowPassword, !Settings.readShowNumberRow(mPrefs));
        }
    }
}
