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

package com.t2.vhb.relax.breathing;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.support.v4.preference.PreferenceFragment;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;

import com.t2.controlledbreathing.BackgroundCategory;
import com.t2.controlledbreathing.DurationType;
import com.t2.controlledbreathing.MusicCategory;
import com.t2.vhb.ActionBarActivity;
import com.t2.vhb.R;

import java.text.DecimalFormat;

public class ControlledBreathingSettingsActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.settings);

        setTitle("Settings");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, new ControlledBreathingSettingsFragment()).commit();
    }

    public static class ControlledBreathingSettingsFragment extends PreferenceFragment implements
            OnPreferenceClickListener, OnSharedPreferenceChangeListener {
        private final DecimalFormat mFormatter = new DecimalFormat("#0.0' s'");
        private final DecimalFormat mAccessibilityFormatter = new DecimalFormat("#0.0' seconds'");

        private String formatDuration(long duration) {
            final AccessibilityManager am = (AccessibilityManager) getActivity()
                    .getSystemService(ACCESSIBILITY_SERVICE);
            return am.isEnabled() ? mAccessibilityFormatter.format(duration / 1000.0f) : mFormatter
                    .format(duration / 1000.0f);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prf_breathing);

            SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();

            ListPreference bkgPref = (ListPreference) findPreference(getString(R.string.pref_breathing_background));
            BackgroundCategory[] bkgCats = BackgroundCategory.values();
            String[] bkgNames = new String[bkgCats.length];
            String[] bkgValues = new String[bkgCats.length];
            int index = 0;
            for (BackgroundCategory cat : bkgCats) {
                bkgNames[index] = cat.toString();
                bkgValues[index] = cat.name();
                index++;
            }
            bkgPref.setEntries(bkgNames);
            bkgPref.setEntryValues(bkgValues);
            bkgPref.setDefaultValue(BackgroundCategory.RAINFORESTS.name());
            bkgPref.setValue(prefs.getString(bkgPref.getKey(), BackgroundCategory.RAINFORESTS.name()));

            ListPreference musicPref = (ListPreference) findPreference(getString(R.string.pref_breathing_music));
            MusicCategory[] musCats = MusicCategory.values();
            String[] musNames = new String[musCats.length];
            String[] musValues = new String[musCats.length];
            index = 0;
            for (MusicCategory cat : musCats) {
                musNames[index] = cat.toString();
                musValues[index] = cat.name();
                index++;
            }
            musicPref.setEntries(musNames);
            musicPref.setEntryValues(musValues);
            musicPref.setDefaultValue(MusicCategory.RANDOM.name());
            musicPref.setValue(prefs.getString(musicPref.getKey(), MusicCategory.RANDOM.name()));

            boolean myMusicEnabled = MusicCategory.PERSONAL_MUSIC.name().equals(
                    prefs.getString(musicPref.getKey(), MusicCategory.RANDOM.name()));
            Preference myMusicPref = findPreference(getString(R.string.pref_breathing_custom_music));
            myMusicPref.setEnabled(myMusicEnabled);
            myMusicPref.setOnPreferenceClickListener(this);

            prefs.registerOnSharedPreferenceChangeListener(this);

            initSummaries();
        }

        @Override
        public void onDestroy() {
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onDestroy();
        }

        private void initSummaries() {
            Preference pref;

            for (DurationType type : DurationType.values()) {
                pref = findPreference(getString(type.getPrefKeyId()));
                pref.setOnPreferenceClickListener(this);

                if (type.isShowDisable() && !pref.getSharedPreferences().contains(pref.getKey())) {
                    pref.setSummary("Disabled");
                } else {
                    pref.setSummary(formatDuration(pref.getSharedPreferences().getLong(pref.getKey(),
                            type.getDefaultDuration())));
                }

            }

            pref = findPreference(getString(R.string.pref_session_duration));
            final int duration = pref.getSharedPreferences().getInt(pref.getKey(), 0);
            if (duration == 0) {
                pref.setSummary("Unlimited");
            } else if (duration == 1) {
                pref.setSummary(duration + " Minute");
            } else {
                pref.setSummary(duration + " Minutes");
            }

            pref = findPreference(getString(R.string.pref_breathing_background));
            BackgroundCategory bCat = BackgroundCategory.valueOf(pref.getSharedPreferences().getString(pref.getKey(),
                    BackgroundCategory.RAINFORESTS.name()));
            String summary = bCat.toString();
            if (bCat == BackgroundCategory.BEACHES) {
                summary += ": Courtesy of NOAA";
            }
            pref.setSummary(summary);

            pref = findPreference(getString(R.string.pref_breathing_music));
            MusicCategory mCat = MusicCategory.valueOf(pref.getSharedPreferences().getString(pref.getKey(),
                    MusicCategory.RANDOM.name()));
            pref.setSummary(mCat.toString());
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            for (DurationType type : DurationType.values()) {
                if (getString(type.getPrefKeyId()).equals(preference.getKey())) {
                    Intent intent = new Intent(getActivity(), ControlledBreathingDurationActivity.class);
                    intent.putExtra(ControlledBreathingDurationActivity.KEY_TYPE, type);
                    startActivity(intent);
                    return true;
                }
            }

            if (preference.getKey().equals(getString(R.string.pref_breathing_custom_music))) {
                startActivity(new Intent(getActivity(), ControlledBreathingMusicActivity.class));
                return true;
            }
            return false;
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            Preference pref = findPreference(key);
            for (DurationType type : DurationType.values()) {
                if (!getString(type.getPrefKeyId()).equals(key)) {
                    continue;
                }

                if (type.isShowDisable() && !prefs.contains(pref.getKey())) {
                    pref.setSummary("Disabled");
                } else {
                    pref.setSummary(formatDuration(prefs.getLong(pref.getKey(), type.getDefaultDuration())));
                }

            }

            if (key.equals(getString(R.string.pref_session_duration))) {
                final int duration = prefs.getInt(key, 0);
                if (duration == 0) {
                    pref.setSummary("Unlimited");
                } else if (duration == 1) {
                    pref.setSummary(duration + " Minute");
                } else {
                    pref.setSummary(duration + " Minutes");
                }
            } else if (key.equals(getString(R.string.pref_breathing_background))) {
                BackgroundCategory category = BackgroundCategory.valueOf(prefs.getString(key,
                        BackgroundCategory.RAINFORESTS.name()));
                String summary = category.toString();
                if (category == BackgroundCategory.BEACHES) {
                    summary += ": Courtesy of NOAA";
                }
                pref.setSummary(summary);
            } else if (key.equals(getString(R.string.pref_breathing_music))) {
                MusicCategory category = MusicCategory.valueOf(prefs.getString(key, MusicCategory.RANDOM.name()));
                pref.setSummary(category.toString());
                boolean myMusicEnabled = MusicCategory.PERSONAL_MUSIC.name().equals(
                        prefs.getString(key, MusicCategory.RANDOM.name()));
                Preference myMusicPref = findPreference(getString(R.string.pref_breathing_custom_music));
                myMusicPref.setEnabled(myMusicEnabled);
            }
        }

    }

}
