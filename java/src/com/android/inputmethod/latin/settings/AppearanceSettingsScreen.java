package com.android.inputmethod.latin.settings;

import com.android.inputmethod.latin.R;
import com.android.inputmethod.latin.common.Constants;
import com.android.inputmethod.latin.define.ProductionFlags;
import com.android.inputmethod.latin.settings.hig.HigList;

final class AppearanceSettingsScreen extends SettingsScreen {

    AppearanceSettingsScreen(final SettingsActivity activity) {
        super(activity);
    }

    @Override
    CharSequence getTitle() {
        return str(R.string.settings_screen_appearance);
    }

    @Override
    void build(final HigList hig) {
        hig.group().row(R.drawable.ic_f7_paintbrush, str(R.string.settings_screen_theme),
                ThemeSettingsScreen.selectedThemeName(mActivity), true,
                v -> openScreen(SettingsActivity.SCREEN_THEME));

        hig.header(str(R.string.hig_section_layouts));
        final HigList.Group layouts = hig.group();
        layouts.row(0, str(R.string.custom_input_styles_title), null, true,
                v -> openLegacy(CustomInputStyleSettingsFragment.class,
                        R.string.custom_input_styles_title, null));
        if (ProductionFlags.IS_SPLIT_KEYBOARD_SUPPORTED
                && !Constants.isPhone(Settings.readScreenMetrics(mRes))) {
            switchPref(layouts, 0, Settings.PREF_ENABLE_SPLIT_KEYBOARD,
                    R.string.enable_split_keyboard, 0, false);
        }
    }
}
