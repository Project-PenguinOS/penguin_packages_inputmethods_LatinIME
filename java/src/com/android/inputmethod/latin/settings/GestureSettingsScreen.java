package com.android.inputmethod.latin.settings;

import androidx.appcompat.widget.SwitchCompat;

import com.android.inputmethod.latin.R;
import com.android.inputmethod.latin.settings.hig.HigList;

final class GestureSettingsScreen extends SettingsScreen {

    private SwitchCompat mFloatingPreview;
    private SwitchCompat mPreviewTrail;

    GestureSettingsScreen(final SettingsActivity activity) {
        super(activity);
    }

    @Override
    CharSequence getTitle() {
        return str(R.string.settings_screen_gesture);
    }

    @Override
    void build(final HigList hig) {
        switchPref(hig.group(), R.drawable.ic_f7_hand_draw, Settings.PREF_GESTURE_INPUT,
                R.string.gesture_input, R.string.gesture_input_summary, true);
        final HigList.Group options = hig.group();
        mFloatingPreview = switchPref(options, 0, Settings.PREF_GESTURE_FLOATING_PREVIEW_TEXT,
                R.string.gesture_floating_preview_text,
                R.string.gesture_floating_preview_text_summary, true);
        mPreviewTrail = switchPref(options, 0, Settings.PREF_GESTURE_PREVIEW_TRAIL,
                R.string.gesture_preview_trail, 0, true);
        updateDependents();
    }

    @Override
    void onSharedPreferenceChanged(final String key) {
        if (Settings.PREF_GESTURE_INPUT.equals(key)) {
            updateDependents();
        }
    }

    private void updateDependents() {
        final boolean enabled = mPrefs.getBoolean(Settings.PREF_GESTURE_INPUT, true);
        HigList.setRowEnabled(mFloatingPreview, enabled);
        HigList.setRowEnabled(mPreviewTrail, enabled);
    }
}
