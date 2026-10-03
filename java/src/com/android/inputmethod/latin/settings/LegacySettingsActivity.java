package com.android.inputmethod.latin.settings;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;

import com.android.inputmethod.latin.permissions.PermissionsManager;
import com.android.inputmethod.latin.utils.FragmentUtils;

public final class LegacySettingsActivity extends PreferenceActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    static Intent newIntent(final Context context, final String fragment,
            @StringRes final int title, @Nullable final Bundle args) {
        return new Intent(context, LegacySettingsActivity.class)
                .putExtra(EXTRA_SHOW_FRAGMENT, fragment)
                .putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, args)
                .putExtra(EXTRA_SHOW_FRAGMENT_TITLE, title);
    }

    @Override
    protected void onCreate(final Bundle savedState) {
        super.onCreate(savedState);
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
        final int title = getIntent().getIntExtra(EXTRA_SHOW_FRAGMENT_TITLE, 0);
        if (title != 0) {
            setTitle(title);
        }
    }

    @Override
    public Intent getIntent() {
        final Intent intent = super.getIntent();
        intent.putExtra(EXTRA_NO_HEADERS, true);
        return intent;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean isValidFragment(final String fragmentName) {
        return FragmentUtils.isValidFragment(fragmentName);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final String[] permissions,
            final int[] grantResults) {
        PermissionsManager.get(this).onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }
}
