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

package com.t2.vhb.distractions.photopuzzle;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v4.preference.PreferenceFragment;
import android.view.Display;
import android.view.WindowManager;

import com.michaelnovakjr.numberpicker.NumberPickerPreference;
import com.t2.vhb.ActionBarActivity;
import com.t2.vhb.R;

public class PhotoPuzzleSettingsActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.settings);

        setTitle("Settings");
        getSupportFragmentManager().beginTransaction().replace(R.id.settings, new PhotoPuzzleSettingsFragment())
                .commit();

    }

    public static class PhotoPuzzleSettingsFragment extends PreferenceFragment {

        private NumberPickerPreference mRowsPref, mColsPref;

        private int mMaxRows, mMaxCols;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prf_photo_puzzle);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            float density = getResources().getDisplayMetrics().density;
            float dpHeight = display.getHeight() / density;
            float dpWidth = display.getWidth() / density;

            mMaxRows = Math.max((int) (dpHeight / 44), PhotoPuzzleView.MIN_ROWS);
            mMaxCols = Math.max((int) (dpWidth / 44), PhotoPuzzleView.MIN_COLUMNS);

            mRowsPref = (NumberPickerPreference) findPreference(getString(R.string.pref_photo_puzzle_rows));
            mRowsPref.setRange(PhotoPuzzleView.MIN_ROWS, mMaxRows);
            mColsPref = (NumberPickerPreference) findPreference(getString(R.string.pref_photo_puzzle_columns));
            mColsPref.setRange(PhotoPuzzleView.MIN_COLUMNS, mMaxCols);

            int currentRows = getPreferenceScreen().getSharedPreferences().getInt(
                    getString(R.string.pref_photo_puzzle_rows), PhotoPuzzleView.MIN_ROWS);
            int currentCols = getPreferenceScreen().getSharedPreferences().getInt(
                    getString(R.string.pref_photo_puzzle_columns), PhotoPuzzleView.MIN_COLUMNS);

            if (currentRows > mMaxRows) {
                getPreferenceScreen().getSharedPreferences().edit()
                        .putInt(getString(R.string.pref_photo_puzzle_rows), mMaxRows).apply();
            }

            if (currentRows < PhotoPuzzleView.MIN_ROWS) {
                getPreferenceScreen().getSharedPreferences().edit()
                        .putInt(getString(R.string.pref_photo_puzzle_rows), PhotoPuzzleView.MIN_ROWS).apply();
            }

            if (currentCols > mMaxCols) {
                getPreferenceScreen().getSharedPreferences().edit()
                        .putInt(getString(R.string.pref_photo_puzzle_columns), mMaxCols).apply();
            }

            if (currentCols < PhotoPuzzleView.MIN_COLUMNS) {
                getPreferenceScreen().getSharedPreferences().edit()
                        .putInt(getString(R.string.pref_photo_puzzle_columns), PhotoPuzzleView.MIN_COLUMNS).apply();
            }

            initSummaries();
        }

        private void initSummaries() {
            Preference pref = findPreference(getString(R.string.pref_photo_puzzle_columns));
            pref.setSummary("" + pref.getSharedPreferences().getInt(pref.getKey(), PhotoPuzzleView.MIN_COLUMNS));
            pref.setOnPreferenceChangeListener((preference, newValue) -> {
                if (!(newValue instanceof Integer) || (Integer) newValue < PhotoPuzzleView.MIN_COLUMNS
                        || (Integer) newValue > mMaxCols) {
                    return false;
                }

                preference.setSummary("" + newValue);
                return true;
            });

            pref = findPreference(getString(R.string.pref_photo_puzzle_rows));
            pref.setSummary("" + pref.getSharedPreferences().getInt(pref.getKey(), PhotoPuzzleView.MIN_ROWS));
            pref.setOnPreferenceChangeListener((preference, newValue) -> {
                if (!(newValue instanceof Integer) || (Integer) newValue < PhotoPuzzleView.MIN_ROWS
                        || (Integer) newValue > mMaxRows) {
                    return false;
                }

                preference.setSummary("" + newValue);
                return true;
            });
        }

    }

}
