package com.android.inputmethod.latin.settings;

import android.content.Context;
import android.media.AudioManager;

import com.android.inputmethod.latin.AudioAndHapticFeedbackManager;
import com.android.inputmethod.latin.R;
import com.android.inputmethod.latin.SystemBroadcastReceiver;
import com.android.inputmethod.latin.settings.hig.HigAlert;
import com.android.inputmethod.latin.settings.hig.HigList;

import java.util.Locale;

final class AdvancedSettingsScreen extends SettingsScreen {

    private HigList.Row mDismissDelay;
    private Slider mVibration;
    private Slider mVolume;

    AdvancedSettingsScreen(final SettingsActivity activity) {
        super(activity);
    }

    @Override
    CharSequence getTitle() {
        return str(R.string.settings_screen_advanced);
    }

    @Override
    void build(final HigList hig) {
        mDismissDelay = null;
        mVibration = null;
        mVolume = null;

        hig.header(str(R.string.hig_section_key_feedback));
        final HigList.Group feedback = hig.group();
        if (Settings.readFromBuildConfigIfToShowKeyPreviewPopupOption(mRes)) {
            addDismissDelay(feedback);
        }
        if (AudioAndHapticFeedbackManager.getInstance().hasVibrator()) {
            mVibration = sliderPref(feedback, R.string.prefs_keypress_vibration_duration_settings,
                    0, mRes.getInteger(R.integer.config_max_vibration_duration), 1,
                    Settings.PREF_VIBRATION_DURATION_SETTINGS, new IntValue() {
                        @Override
                        public int read() {
                            return Settings.readKeypressVibrationDuration(mPrefs, mRes);
                        }

                        @Override
                        public void write(final int value) {
                            mPrefs.edit().putInt(Settings.PREF_VIBRATION_DURATION_SETTINGS, value)
                                    .apply();
                        }

                        @Override
                        public String text(final int value) {
                            return value < 0 ? str(R.string.settings_system_default)
                                    : mRes.getString(R.string.abbreviation_unit_milliseconds,
                                            Integer.toString(value));
                        }

                        @Override
                        public void feedback(final int value) {
                            AudioAndHapticFeedbackManager.getInstance().vibrate(value);
                        }
                    });
        }
        final AudioManager am = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);
        mVolume = sliderPref(feedback, R.string.prefs_keypress_sound_volume_settings, 0, 100, 1,
                Settings.PREF_KEYPRESS_SOUND_VOLUME, new IntValue() {
                    @Override
                    public int read() {
                        return (int) (Settings.readKeypressSoundVolume(mPrefs, mRes) * 100f);
                    }

                    @Override
                    public void write(final int value) {
                        mPrefs.edit().putFloat(Settings.PREF_KEYPRESS_SOUND_VOLUME, value / 100f)
                                .apply();
                    }

                    @Override
                    public String text(final int value) {
                        return value < 0 ? str(R.string.settings_system_default)
                                : Integer.toString(value);
                    }

                    @Override
                    public void feedback(final int value) {
                        am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, value / 100f);
                    }
                });
        hig.footer(str(R.string.hig_slider_footer));

        hig.header(str(R.string.hig_section_timing));
        final HigList.Group keys = hig.group();
        sliderPref(keys, R.string.prefs_key_longpress_timeout_settings,
                mRes.getInteger(R.integer.config_min_longpress_timeout),
                mRes.getInteger(R.integer.config_max_longpress_timeout),
                mRes.getInteger(R.integer.config_longpress_timeout_step),
                Settings.PREF_KEY_LONGPRESS_TIMEOUT, new IntValue() {
                    @Override
                    public int read() {
                        return Settings.readKeyLongpressTimeout(mPrefs, mRes);
                    }

                    @Override
                    public void write(final int value) {
                        mPrefs.edit().putInt(Settings.PREF_KEY_LONGPRESS_TIMEOUT, value).apply();
                    }

                    @Override
                    public String text(final int value) {
                        return mRes.getString(R.string.abbreviation_unit_milliseconds,
                                Integer.toString(value));
                    }

                    @Override
                    public void feedback(final int value) {}
                });
        sliderPref(keys, R.string.prefs_keyboard_height_scale, 50, 120, 1,
                Settings.PREF_KEYBOARD_HEIGHT_SCALE, new IntValue() {
                    @Override
                    public int read() {
                        return Math.round(Settings.readKeyboardHeight(mPrefs,
                                SettingsValues.DEFAULT_SIZE_SCALE) * 100f);
                    }

                    @Override
                    public void write(final int value) {
                        mPrefs.edit().putFloat(Settings.PREF_KEYBOARD_HEIGHT_SCALE, value / 100f)
                                .apply();
                    }

                    @Override
                    public String text(final int value) {
                        return String.format(Locale.ROOT, "%d%%", value);
                    }

                    @Override
                    public void feedback(final int value) {}
                });

        hig.header(str(R.string.hig_section_trackpad));
        final HigList.Group trackpad = hig.group();
        switchPref(trackpad, 0, Settings.PREF_SPACE_TRACKPAD, R.string.space_trackpad,
                R.string.space_trackpad_summary, true);
        switchPref(trackpad, 0, Settings.PREF_BACKSPACE_TRACKPAD, R.string.backspace_trackpad,
                R.string.backspace_trackpad_summary, true);

        hig.header(str(R.string.hig_section_other));
        final HigList.Group other = hig.group();
        switchPref(other, 0, Settings.PREF_ENABLE_EMOJI_ALT_PHYSICAL_KEY,
                R.string.prefs_enable_emoji_alt_physical_key,
                R.string.prefs_enable_emoji_alt_physical_key_summary, true);
        other.switchRow(0, str(R.string.show_setup_wizard_icon),
                str(R.string.show_setup_wizard_icon_summary),
                Settings.readShowSetupWizardIcon(mPrefs, mActivity),
                (button, checked) -> mPrefs.edit()
                        .putBoolean(Settings.PREF_SHOW_SETUP_WIZARD_ICON, checked).apply());
        if (Settings.isInternal(mPrefs)) {
            other.row(0, str(R.string.hig_debug_settings), null, true,
                    v -> openLegacy(DebugSettingsFragment.class, R.string.hig_debug_settings,
                            null));
        }

        refreshEnablings();
    }

    private void addDismissDelay(final HigList.Group group) {
        final String defaultDelay =
                Integer.toString(mRes.getInteger(R.integer.config_key_preview_linger_timeout));
        final String[] values = { "0", defaultDelay };
        final String[] names = {
                str(R.string.key_preview_popup_dismiss_no_delay),
                str(R.string.key_preview_popup_dismiss_default_delay),
        };
        final String current =
                mPrefs.getString(Settings.PREF_KEY_PREVIEW_POPUP_DISMISS_DELAY, defaultDelay);
        String currentName = null;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) {
                currentName = names[i];
            }
        }
        mDismissDelay = group.row(0, str(R.string.key_preview_popup_dismiss_delay), currentName,
                true, v -> {
                    final HigAlert sheet = HigAlert.actionSheet(mActivity)
                            .title(str(R.string.key_preview_popup_dismiss_delay));
                    for (int i = 0; i < values.length; i++) {
                        final String value = values[i];
                        sheet.action(names[i], HigAlert.Style.DEFAULT, () -> {
                            mPrefs.edit().putString(
                                    Settings.PREF_KEY_PREVIEW_POPUP_DISMISS_DELAY, value).apply();
                            rebuild();
                        });
                    }
                    sheet.action(str(R.string.hig_cancel), HigAlert.Style.CANCEL, null).show();
                });
    }

    @Override
    void onSharedPreferenceChanged(final String key) {
        if (Settings.PREF_SHOW_SETUP_WIZARD_ICON.equals(key)) {
            SystemBroadcastReceiver.toggleAppIcon(mActivity);
        }
        refreshEnablings();
    }

    private void refreshEnablings() {
        if (mDismissDelay != null) {
            HigList.setRowEnabled(mDismissDelay.view,
                    Settings.readKeyPreviewPopupEnabled(mPrefs, mRes));
        }
        if (mVibration != null) {
            mVibration.setEnabled(Settings.readVibrationEnabled(mPrefs, mRes));
        }
        if (mVolume != null) {
            mVolume.setEnabled(Settings.readKeypressSoundEnabled(mPrefs, mRes));
        }
    }
}
