package com.android.inputmethod.latin.settings;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.android.inputmethod.latin.R;
import com.android.inputmethod.latin.settings.hig.HigList;

final class CreditsScreen extends SettingsScreen {

    private static final String[][] TEAM = {
        {"jendermine", "jendermine"},
        {"redducc", "redducc"},
        {"@Venkat3620", "Venkat3620"},
        {"@pranayadmn", "pranayadmn"},
        {"candy paneer", null},
        {"@Primakre", "Primakre"},
        {"@Whoraj", "Whoraj"},
    };

    CreditsScreen(final SettingsActivity activity) {
        super(activity);
    }

    @Override
    CharSequence getTitle() {
        return str(R.string.credits_title);
    }

    @Override
    void build(final HigList hig) {
        final View header = LayoutInflater.from(mActivity).inflate(R.layout.credits_header,
                null, false);
        ((TextView) header.findViewById(android.R.id.title)).setText(R.string.credits_app_name);
        ((TextView) header.findViewById(android.R.id.summary)).setText(versionText(mActivity));
        hig.view(header);

        hig.header(str(R.string.credits_team_category));
        final HigList.Group team = hig.group();
        for (final String[] member : TEAM) {
            final String name = member[0];
            final String telegram = member[1];
            final CharSequence subtitle =
                    name.equals("candy paneer") ? str(R.string.credits_kitsune) : null;
            final HigList.Row row = team.subtitleRow(0, name, subtitle, telegram != null,
                    telegram == null ? null : v -> openTelegram(telegram));
            if (telegram != null) {
                row.view.setContentDescription(
                        mActivity.getString(R.string.credits_open_telegram, name));
            }
        }
    }

    private void openTelegram(final String username) {
        try {
            mActivity.startActivity(
                    new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/" + username)));
        } catch (final ActivityNotFoundException e) {
        }
    }

    private static CharSequence versionText(final Context context) {
        try {
            final PackageInfo info =
                    context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return context.getString(
                    R.string.credits_version, info.versionName, info.getLongVersionCode());
        } catch (final PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
