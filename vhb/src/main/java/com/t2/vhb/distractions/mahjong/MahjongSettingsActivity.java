/*
 *
 * Created by Wes Turney on 12/17/13.
 *
 * VirtualHopeBox
 *
 * Copyright  2009-2014 United States Government as represented by
 * the Chief Information Officer of the National Center for Telehealth
 * and Technology. All Rights Reserved.
 *
 * Copyright  2009-2014 Contributors. All Rights Reserved.
 *
 * THIS OPEN SOURCE AGREEMENT ("AGREEMENT") DEFINES THE RIGHTS OF USE,
 * REPRODUCTION, DISTRIBUTION, MODIFICATION AND REDISTRIBUTION OF CERTAIN
 * COMPUTER SOFTWARE ORIGINALLY RELEASED BY THE UNITED STATES GOVERNMENT
 * AS REPRESENTED BY THE GOVERNMENT AGENCY LISTED BELOW ("GOVERNMENT AGENCY").
 * THE UNITED STATES GOVERNMENT, AS REPRESENTED BY GOVERNMENT AGENCY, IS AN
 * INTENDED THIRD-PARTY BENEFICIARY OF ALL SUBSEQUENT DISTRIBUTIONS OR
 * REDISTRIBUTIONS OF THE SUBJECT SOFTWARE. ANYONE WHO USES, REPRODUCES,
 * DISTRIBUTES, MODIFIES OR REDISTRIBUTES THE SUBJECT SOFTWARE, AS DEFINED
 * HEREIN, OR ANY PART THEREOF, IS, BY THAT ACTION, ACCEPTING IN FULL THE
 * RESPONSIBILITIES AND OBLIGATIONS CONTAINED IN THIS AGREEMENT.
 *
 * Government Agency: The National Center for Telehealth and Technology
 * Government Agency Original Software Designation: VirtualHopeBox001
 * Government Agency Original Software Title: VirtualHopeBox
 * User Registration Requested. Please send email
 * with your contact information to: robert.a.kayl.civ@mail.mil
 * Government Agency Point of Contact for Original Software: robert.a.kayl.civ@mail.mil
 *
 */

package com.t2.vhb.distractions.mahjong;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v4.preference.PreferenceFragment;
import android.view.WindowManager;

import com.t2.mahjong.MahjongBackground;
import com.t2.vhb.ActionBarActivity;
import com.t2.vhb.R;

public class MahjongSettingsActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.settings);

        setTitle("Settings");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, new MahjongSettingsFragment())
                .commit();
    }

    public static class MahjongSettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle paramBundle) {
            super.onCreate(paramBundle);
            addPreferencesFromResource(R.xml.prf_mahjong);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            ListPreference bkgPref = (ListPreference) findPreference(getString(R.string.pref_mahjong_background));
            MahjongBackground[] bkgs = MahjongBackground.values();
            String[] bkgNames = new String[bkgs.length];
            String[] bkgValues = new String[bkgs.length];
            int index = 0;
            for (MahjongBackground bkg : bkgs) {
                bkgNames[index] = bkg.toString();
                bkgValues[index] = bkg.name();
                index++;
            }
            bkgPref.setEntries(bkgNames);
            bkgPref.setEntryValues(bkgValues);
            bkgPref.setDefaultValue(MahjongBackground.REDWOOD.name());
            bkgPref.setValue(prefs.getString(bkgPref.getKey(), MahjongBackground.REDWOOD.name()));

            initSummaries();
        }

        private void initSummaries() {
            Preference pref = findPreference(getString(R.string.pref_mahjong_background));
            pref.setOnPreferenceChangeListener((preference, newValue) -> {
                MahjongBackground category = MahjongBackground.valueOf((String) newValue);
                preference.setSummary(category.toString());
                return true;
            });

            MahjongBackground bCat = MahjongBackground.valueOf(pref.getSharedPreferences().getString(pref.getKey(),
                    MahjongBackground.REDWOOD.name()));
            pref.setSummary(bCat.toString());
        }
    }
}
