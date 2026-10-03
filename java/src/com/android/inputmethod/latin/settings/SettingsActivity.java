package com.android.inputmethod.latin.settings;

import android.app.backup.BackupManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.android.inputmethod.latin.AudioAndHapticFeedbackManager;
import com.android.inputmethod.latin.RichInputMethodManager;
import com.android.inputmethod.latin.permissions.PermissionsManager;
import com.android.inputmethod.latin.settings.hig.HigGlassActivity;
import com.android.inputmethod.latin.settings.hig.HigNavigationBar;
import com.android.inputmethod.latin.utils.FragmentUtils;
import com.android.inputmethod.latin.utils.StatsUtils;

public final class SettingsActivity extends HigGlassActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback,
        SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String EXTRA_SHOW_HOME_AS_UP = "show_home_as_up";
    public static final String EXTRA_ENTRY_KEY = "entry";
    public static final String EXTRA_ENTRY_VALUE_LONG_PRESS_COMMA = "long_press_comma";
    public static final String EXTRA_ENTRY_VALUE_APP_ICON = "app_icon";
    public static final String EXTRA_ENTRY_VALUE_NOTICE_DIALOG = "important_notice";
    public static final String EXTRA_ENTRY_VALUE_SYSTEM_SETTINGS = "system_settings";

    public static final String EXTRA_SCREEN = "screen";
    public static final String SCREEN_MAIN = "main";
    public static final String SCREEN_PREFERENCES = "preferences";
    public static final String SCREEN_APPEARANCE = "appearance";
    public static final String SCREEN_THEME = "theme";
    public static final String SCREEN_GESTURE = "gesture";
    public static final String SCREEN_CORRECTION = "correction";
    public static final String SCREEN_ADVANCED = "advanced";
    public static final String SCREEN_CREDITS = "credits";

    private static final String LEGACY_FRAGMENT_PACKAGE =
            "com.android.inputmethod.latin.settings.";

    private SharedPreferences mPrefs;
    private SettingsScreen mScreen;
    private boolean mResumed;

    @Override
    protected void onCreate(final Bundle savedState) {
        super.onCreate(savedState);
        final Intent intent = getIntent();
        final String legacyFragment = intent.getStringExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT);
        String screen = intent.getStringExtra(EXTRA_SCREEN);
        if (screen == null) {
            screen = screenForLegacyFragment(legacyFragment);
        }
        if (screen == null && legacyFragment != null
                && FragmentUtils.isValidFragment(legacyFragment)) {
            startActivity(new Intent(intent).setClass(this, LegacySettingsActivity.class));
            finish();
            return;
        }
        if (screen == null) {
            screen = SCREEN_MAIN;
        }

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        RichInputMethodManager.init(this);
        AudioAndHapticFeedbackManager.init(this);
        mScreen = createScreen(screen);
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        final CharSequence title = mScreen.getTitle();
        setTitle(title);
        final HigNavigationBar navBar = getNavBar();
        navBar.setTitle(title);
        final boolean isRoot = !intent.hasExtra(EXTRA_SCREEN);
        if (SCREEN_MAIN.equals(screen)) {
            setLargeTitle(title);
        }
        if (isRoot) {
            navBar.setBackVisible(intent.getBooleanExtra(EXTRA_SHOW_HOME_AS_UP, true));
            StatsUtils.onSettingsActivity(intent.hasExtra(EXTRA_ENTRY_KEY)
                    ? intent.getStringExtra(EXTRA_ENTRY_KEY)
                    : EXTRA_ENTRY_VALUE_SYSTEM_SETTINGS);
        }
    }

    private static String screenForLegacyFragment(final String fragment) {
        if (fragment == null || !fragment.startsWith(LEGACY_FRAGMENT_PACKAGE)) {
            return null;
        }
        switch (fragment.substring(LEGACY_FRAGMENT_PACKAGE.length())) {
        case "SettingsFragment":
            return SCREEN_MAIN;
        case "PreferencesSettingsFragment":
            return SCREEN_PREFERENCES;
        case "AppearanceSettingsFragment":
            return SCREEN_APPEARANCE;
        case "ThemeSettingsFragment":
            return SCREEN_THEME;
        case "GestureSettingsFragment":
            return SCREEN_GESTURE;
        case "CorrectionSettingsFragment":
            return SCREEN_CORRECTION;
        case "AdvancedSettingsFragment":
            return SCREEN_ADVANCED;
        default:
            return null;
        }
    }

    private SettingsScreen createScreen(final String screen) {
        switch (screen) {
        case SCREEN_PREFERENCES:
            return new PreferencesSettingsScreen(this);
        case SCREEN_APPEARANCE:
            return new AppearanceSettingsScreen(this);
        case SCREEN_THEME:
            return new ThemeSettingsScreen(this);
        case SCREEN_GESTURE:
            return new GestureSettingsScreen(this);
        case SCREEN_CORRECTION:
            return new CorrectionSettingsScreen(this);
        case SCREEN_ADVANCED:
            return new AdvancedSettingsScreen(this);
        case SCREEN_CREDITS:
            return new CreditsScreen(this);
        default:
            return new MainSettingsScreen(this);
        }
    }

    SharedPreferences getPrefs() {
        return mPrefs;
    }

    void openScreen(final String screen, final CharSequence backLabel) {
        startActivity(new Intent(this, SettingsActivity.class)
                .putExtra(EXTRA_SCREEN, screen)
                .putExtra(HigNavigationBar.EXTRA_BACK_LABEL, backLabel));
    }

    void rebuild() {
        if (mScreen != null && !isFinishing()) {
            mScreen.build(newList());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mResumed = true;
        mScreen.onResume();
        rebuild();
    }

    @Override
    protected void onPause() {
        mResumed = false;
        mScreen.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mPrefs != null) {
            mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        }
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
        new BackupManager(this).dataChanged();
        if (mResumed && key != null) {
            mScreen.onSharedPreferenceChanged(key);
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
            @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionsManager.get(this).onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }
}
