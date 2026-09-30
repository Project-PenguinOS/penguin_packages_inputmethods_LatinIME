/*
 * Copyright (C) 2020 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.inputmethod.latin.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.view.View;
import android.widget.ListView;

import com.android.inputmethod.latin.R;

/**
 * A base abstract class for a {@link PreferenceFragment} that removes divider lines
 */
public abstract class CustomPreferenceFragment extends PreferenceFragment {
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // remove dividers
        View rootView = getView();
        ListView list = (ListView) rootView.findViewById(android.R.id.list);
        list.setDivider(null);
        applyIosLayouts(getPreferenceScreen());
        list.post(() -> IosGroupedAdapter.install(list));
    }

    static void applyIosLayouts(final PreferenceGroup group) {
        if (group == null) return;
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            final Preference preference = group.getPreference(i);
            if (preference instanceof PreferenceCategory) {
                preference.setLayoutResource(R.layout.ios_preference_category);
                applyIosLayouts((PreferenceGroup) preference);
            } else {
                preference.setLayoutResource(R.layout.ios_preference);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        final View rootView = getView();
        if (rootView == null) return;
        final ListView list = (ListView) rootView.findViewById(android.R.id.list);
        if (list != null) {
            list.post(() -> IosGroupedAdapter.install(list));
        }
    }
}
