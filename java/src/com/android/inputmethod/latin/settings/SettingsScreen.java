package com.android.inputmethod.latin.settings;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.SeekBar;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.SwitchCompat;

import com.android.inputmethod.latin.R;
import com.android.inputmethod.latin.settings.hig.HigAlert;
import com.android.inputmethod.latin.settings.hig.HigList;

abstract class SettingsScreen {

    interface IntValue {
        int read();
        void write(int value);
        String text(int value);
        void feedback(int value);
    }

    final SettingsActivity mActivity;
    final SharedPreferences mPrefs;
    final Resources mRes;

    SettingsScreen(final SettingsActivity activity) {
        mActivity = activity;
        mPrefs = activity.getPrefs();
        mRes = activity.getResources();
    }

    abstract CharSequence getTitle();

    abstract void build(HigList hig);

    void onResume() {}

    void onPause() {}

    void onSharedPreferenceChanged(final String key) {}

    final void rebuild() {
        mActivity.rebuild();
    }

    final String str(@StringRes final int res) {
        return mActivity.getString(res);
    }

    final SwitchCompat switchPref(final HigList.Group group, @DrawableRes final int icon,
            final String key, @StringRes final int title, @StringRes final int summary,
            final boolean defaultValue) {
        return group.switchRow(icon, str(title), summary == 0 ? null : str(summary),
                mPrefs.getBoolean(key, defaultValue),
                (button, checked) -> mPrefs.edit().putBoolean(key, checked).apply());
    }

    final void openScreen(final String screen) {
        mActivity.openScreen(screen, getTitle());
    }

    final void openLegacy(final Class<?> fragment, @StringRes final int title,
            @Nullable final Bundle args) {
        mActivity.startActivity(LegacySettingsActivity.newIntent(mActivity, fragment.getName(),
                title, args));
    }

    static final class Slider {
        final HigList.Row mRow;
        final SeekBar mBar;

        Slider(final HigList.Row row, final SeekBar bar) {
            mRow = row;
            mBar = bar;
        }

        void setEnabled(final boolean enabled) {
            HigList.setRowEnabled(mRow.view, enabled);
            HigList.setRowEnabled(mBar, enabled);
        }
    }

    final Slider sliderPref(final HigList.Group group, @StringRes final int title,
            final int min, final int max, final int step, final String key,
            final IntValue value) {
        final int current = value.read();
        final HigList.Row row = group.row(0, str(title), value.text(current), false,
                v -> HigAlert.actionSheet(mActivity)
                        .title(str(title))
                        .action(str(R.string.hig_reset_to_default), HigAlert.Style.DEFAULT,
                                () -> {
                                    mPrefs.edit().remove(key).apply();
                                    rebuild();
                                })
                        .action(str(R.string.hig_cancel), HigAlert.Style.CANCEL, null)
                        .show());
        final SeekBar bar = group.sliderRow(false, null, null, min, max, clip(current, min, max, step),
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(final SeekBar bar, final int progress,
                            final boolean fromUser) {
                        if (fromUser) {
                            row.setValue(value.text(clip(progress, min, max, step)));
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(final SeekBar bar) {}

                    @Override
                    public void onStopTrackingTouch(final SeekBar bar) {
                        final int v = clip(bar.getProgress(), min, max, step);
                        value.write(v);
                        value.feedback(v);
                    }
                });
        return new Slider(row, bar);
    }

    static int clip(final int value, final int min, final int max, final int step) {
        final int clipped = Math.min(max, Math.max(min, value));
        return step <= 1 ? clipped : clipped - (clipped % step);
    }
}
