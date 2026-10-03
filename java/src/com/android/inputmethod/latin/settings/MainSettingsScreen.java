package com.android.inputmethod.latin.settings;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import com.android.inputmethod.latin.R;
import com.android.inputmethod.latin.define.ProductionFlags;
import com.android.inputmethod.latin.settings.hig.HigList;
import com.android.inputmethod.latin.utils.ApplicationUtils;

import java.util.List;

final class MainSettingsScreen extends SettingsScreen {

    MainSettingsScreen(final SettingsActivity activity) {
        super(activity);
    }

    @Override
    CharSequence getTitle() {
        return str(ApplicationUtils.getActivityTitleResId(mActivity, SettingsActivity.class));
    }

    @Override
    void build(final HigList hig) {
        final InputMethodManager imm = mActivity.getSystemService(InputMethodManager.class);
        final InputMethodInfo imi = findOwnImi(mActivity, imm);
        if (imi != null && imi.getSubtypeCount() > 1) {
            final String title = str(R.string.select_language);
            hig.group().subtitleRow(R.drawable.ic_f7_globe, title,
                    enabledSubtypesLabel(imm, imi), true, v -> {
                        final Intent intent = new Intent(
                                android.provider.Settings.ACTION_INPUT_METHOD_SUBTYPE_SETTINGS)
                                .putExtra(android.provider.Settings.EXTRA_INPUT_METHOD_ID,
                                        imi.getId())
                                .putExtra(Intent.EXTRA_TITLE, title);
                        try {
                            mActivity.startActivity(intent);
                        } catch (final ActivityNotFoundException e) {
                        }
                    });
        }

        final HigList.Group screens = hig.group();
        screens.row(R.drawable.ic_f7_slider_horizontal_3, str(R.string.settings_screen_preferences),
                null, true, v -> openScreen(SettingsActivity.SCREEN_PREFERENCES));
        if (ProductionFlags.ENABLE_ACCOUNT_SIGN_IN) {
            screens.row(R.drawable.ic_f7_hand_raised_fill, str(R.string.settings_screen_accounts),
                    null, true, v -> openLegacy(AccountsSettingsFragment.class,
                            R.string.settings_screen_accounts, null));
        }
        screens.row(R.drawable.ic_f7_paintbrush, str(R.string.settings_screen_appearance),
                null, true, v -> openScreen(SettingsActivity.SCREEN_APPEARANCE));
        if (Settings.readFromBuildConfigIfGestureInputEnabled(mRes)) {
            screens.row(R.drawable.ic_f7_hand_draw, str(R.string.settings_screen_gesture),
                    null, true, v -> openScreen(SettingsActivity.SCREEN_GESTURE));
        }
        screens.row(R.drawable.ic_f7_textformat_abc_dottedunderline,
                str(R.string.settings_screen_correction), null, true,
                v -> openScreen(SettingsActivity.SCREEN_CORRECTION));
        screens.row(R.drawable.ic_f7_gear_alt, str(R.string.settings_screen_advanced),
                null, true, v -> openScreen(SettingsActivity.SCREEN_ADVANCED));

        hig.header(str(R.string.about_category_title));
        hig.group().row(0, str(R.string.credits_title), null, true,
                v -> openScreen(SettingsActivity.SCREEN_CREDITS));
    }

    private static InputMethodInfo findOwnImi(final Context context,
            final InputMethodManager imm) {
        if (imm == null) {
            return null;
        }
        for (final InputMethodInfo imi : imm.getInputMethodList()) {
            if (imi.getPackageName().equals(context.getPackageName())) {
                return imi;
            }
        }
        return null;
    }

    private String enabledSubtypesLabel(final InputMethodManager imm, final InputMethodInfo imi) {
        final List<InputMethodSubtype> subtypes = imm.getEnabledInputMethodSubtypeList(imi, true);
        final StringBuilder sb = new StringBuilder();
        for (final InputMethodSubtype subtype : subtypes) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(subtype.getDisplayName(mActivity, imi.getPackageName(),
                    imi.getServiceInfo().applicationInfo));
        }
        return TextUtils.isEmpty(sb) ? null : sb.toString();
    }
}
