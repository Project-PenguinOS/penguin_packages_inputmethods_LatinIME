package com.android.inputmethod.latin.settings;

import android.content.Context;
import android.content.res.Resources;

import com.android.inputmethod.keyboard.KeyboardTheme;
import com.android.inputmethod.latin.R;
import com.android.inputmethod.latin.settings.hig.HigList;

final class ThemeSettingsScreen extends SettingsScreen {

    ThemeSettingsScreen(final SettingsActivity activity) {
        super(activity);
    }

    static String selectedThemeName(final Context context) {
        final Resources res = context.getResources();
        final int selected = KeyboardTheme.getSelectedKeyboardThemeId(context);
        final String[] names = res.getStringArray(R.array.keyboard_theme_names);
        final int[] ids = res.getIntArray(R.array.keyboard_theme_ids);
        for (int i = 0; i < names.length; i++) {
            if (ids[i] == selected) {
                return names[i];
            }
        }
        return null;
    }

    @Override
    CharSequence getTitle() {
        return str(R.string.settings_screen_theme);
    }

    @Override
    void build(final HigList hig) {
        final String[] names = mRes.getStringArray(R.array.keyboard_theme_names);
        final int[] ids = mRes.getIntArray(R.array.keyboard_theme_ids);
        final int selected = KeyboardTheme.getSelectedKeyboardThemeId(mActivity);
        final HigList.Group group = hig.group();
        for (int i = 0; i < names.length; i++) {
            final int id = ids[i];
            group.checkRow(0, names[i], id == selected, v -> {
                if (id != KeyboardTheme.getSelectedKeyboardThemeId(mActivity)) {
                    KeyboardTheme.saveKeyboardThemeId(id, mPrefs);
                    rebuild();
                }
            });
        }
    }
}
