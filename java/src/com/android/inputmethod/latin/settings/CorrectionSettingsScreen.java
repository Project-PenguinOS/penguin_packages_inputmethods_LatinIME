package com.android.inputmethod.latin.settings;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.android.inputmethod.dictionarypack.DictionarySettingsActivity;
import com.android.inputmethod.latin.R;
import com.android.inputmethod.latin.permissions.PermissionsManager;
import com.android.inputmethod.latin.permissions.PermissionsUtil;
import com.android.inputmethod.latin.settings.hig.HigList;
import com.android.inputmethod.latin.spellcheck.SpellCheckerSettingsActivity;
import com.android.inputmethod.latin.userdictionary.UserDictionaryList;
import com.android.inputmethod.latin.userdictionary.UserDictionarySettings;

import java.util.TreeSet;

final class CorrectionSettingsScreen extends SettingsScreen
        implements PermissionsManager.PermissionsResultCallback {

    CorrectionSettingsScreen(final SettingsActivity activity) {
        super(activity);
        turnOffUseContactsIfNoPermission();
    }

    @Override
    CharSequence getTitle() {
        return str(R.string.settings_screen_correction);
    }

    @Override
    void build(final HigList hig) {
        final PackageManager pm = mActivity.getPackageManager();
        hig.header(str(R.string.hig_section_dictionaries));
        final HigList.Group dictionaries = hig.group();

        final Intent personalDictionary = new Intent("android.settings.USER_DICTIONARY_SETTINGS");
        if (pm.resolveActivity(personalDictionary, PackageManager.MATCH_DEFAULT_ONLY) != null) {
            dictionaries.row(0, str(R.string.edit_personal_dictionary), null, true,
                    v -> mActivity.startActivity(personalDictionary));
        } else {
            final TreeSet<String> locales =
                    UserDictionaryList.getUserDictionaryLocalesSet(mActivity);
            if (locales != null) {
                dictionaries.row(0, str(R.string.edit_personal_dictionary), null, true, v -> {
                    if (locales.size() <= 1) {
                        final Bundle args = new Bundle();
                        if (locales.size() == 1) {
                            args.putString("locale", locales.first());
                        }
                        openLegacy(UserDictionarySettings.class,
                                R.string.edit_personal_dictionary, args);
                    } else {
                        openLegacy(UserDictionaryList.class,
                                R.string.edit_personal_dictionary, null);
                    }
                });
            }
        }

        final Intent addOns = new Intent(Intent.ACTION_MAIN)
                .setClassName(mActivity.getPackageName(),
                        DictionarySettingsActivity.class.getName())
                .putExtra("clientId", str(R.string.dictionary_pack_client_id));
        if (!pm.queryIntentActivities(addOns, 0).isEmpty()) {
            dictionaries.row(0, str(R.string.configure_dictionaries_title), null, true,
                    v -> mActivity.startActivity(addOns));
        }
        dictionaries.row(0, str(R.string.android_spell_checker_settings), null, true,
                v -> mActivity.startActivity(
                        new Intent(mActivity, SpellCheckerSettingsActivity.class)));

        hig.header(str(R.string.hig_section_suggestions));
        final HigList.Group suggestions = hig.group();
        switchPref(suggestions, 0, Settings.PREF_SHOW_SUGGESTIONS, R.string.prefs_show_suggestions,
                R.string.prefs_show_suggestions_summary, true);
        switchPref(suggestions, 0, Settings.PREF_AUTO_CORRECTION, R.string.auto_correction,
                R.string.auto_correction_summary, true);
        switchPref(suggestions, 0, Settings.PREF_BIGRAM_PREDICTIONS, R.string.bigram_prediction,
                R.string.bigram_prediction_summary, true);
        switchPref(suggestions, 0, Settings.PREF_BLOCK_POTENTIALLY_OFFENSIVE,
                R.string.prefs_block_potentially_offensive_title,
                R.string.prefs_block_potentially_offensive_summary,
                mRes.getBoolean(R.bool.config_block_potentially_offensive));

        hig.header(str(R.string.hig_section_personalization));
        final HigList.Group personalization = hig.group();
        switchPref(personalization, 0, Settings.PREF_KEY_USE_PERSONALIZED_DICTS,
                R.string.use_personalized_dicts, R.string.use_personalized_dicts_summary, true);
        switchPref(personalization, 0, Settings.PREF_KEY_USE_CONTACTS_DICT,
                R.string.use_contacts_dict, R.string.use_contacts_dict_summary, true);
    }

    @Override
    void onSharedPreferenceChanged(final String key) {
        if (!Settings.PREF_KEY_USE_CONTACTS_DICT.equals(key)
                || !mPrefs.getBoolean(key, false)
                || PermissionsUtil.checkAllPermissionsGranted(
                        mActivity, Manifest.permission.READ_CONTACTS)) {
            return;
        }
        PermissionsManager.get(mActivity).requestPermissions(this, mActivity,
                Manifest.permission.READ_CONTACTS);
    }

    @Override
    public void onRequestPermissionsResult(final boolean allGranted) {
        if (turnOffUseContactsIfNoPermission()) {
            rebuild();
        }
    }

    private boolean turnOffUseContactsIfNoPermission() {
        if (PermissionsUtil.checkAllPermissionsGranted(
                mActivity, Manifest.permission.READ_CONTACTS)
                || !mPrefs.getBoolean(Settings.PREF_KEY_USE_CONTACTS_DICT, true)) {
            return false;
        }
        mPrefs.edit().putBoolean(Settings.PREF_KEY_USE_CONTACTS_DICT, false).apply();
        return true;
    }
}
