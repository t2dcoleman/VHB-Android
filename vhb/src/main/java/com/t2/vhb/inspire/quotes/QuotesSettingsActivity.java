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

package com.t2.vhb.inspire.quotes;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.support.v4.preference.PreferenceFragment;
import android.text.TextUtils;
import android.view.WindowManager;

import com.t2.vhb.ActionBarActivity;
import com.t2.vhb.R;
import com.t2.vhb.util.TimePreference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import timber.log.Timber;

/**
 * @author wes
 */
public class QuotesSettingsActivity extends ActionBarActivity {

    private static final String TAG = "com.t2.vhb.inspire.quotes.ViewQuotesPreferenceActivity";

    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.settings);

        setTitle("Settings");
        getSupportFragmentManager().beginTransaction().replace(R.id.settings, new QuotesSettingsFragment()).commit();
    }

    public static class QuotesSettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prf_quotes);

            initSummaries();
        }

        private void initSummaries() {
            ListPreference listPref = (ListPreference) findPreference(getString(R.string.pref_quotes_delay));
            listPref.setOnPreferenceChangeListener((preference, newValue) -> {
                final ListPreference listPref1 = (ListPreference) preference;
                final String newEntry = (String) listPref1.getEntries()[listPref1.findIndexOfValue((String) newValue)];
                listPref1.setSummary(newEntry);

                getListView().requestFocus();
                getListView().setSelection(0);

                Map<String, String> data = new HashMap<>();
                data.put("Duration", newEntry.replace(" Seconds", ""));

                return true;
            });
            listPref.setSummary(listPref.getEntry());

            TimePreference timePref = (TimePreference) findPreference(getString(R.string.pref_quotes_reminder_time));
            timePref.setOnPreferenceChangeListener((preference, newValue) -> {
                final SharedPreferences prefs = preference.getSharedPreferences();
                final AlarmManager am = (AlarmManager) Objects.requireNonNull(getActivity()).getSystemService(Context.ALARM_SERVICE);
                if(am == null) return false;

                final Intent intent = new Intent(getActivity(), QuoteNotificationReceiver.class);
                final PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

                am.cancel(pendingIntent);

                final String value = (String) newValue;
                final boolean valueSet = !TextUtils.isEmpty(value);

                if (valueSet) {
                    final String[] values = value.split(":");
                    final int hour = Integer.parseInt(values[0]);
                    final int minute = Integer.parseInt(values[1]);

                    Calendar now = Calendar.getInstance();
                    Calendar alarm = Calendar.getInstance();
                    alarm.set(Calendar.MILLISECOND, 0);
                    alarm.set(Calendar.SECOND, 0);
                    alarm.set(Calendar.MINUTE, minute);
                    alarm.set(Calendar.HOUR_OF_DAY, hour);

                    if (now.after(alarm)) {
                        alarm.add(Calendar.DATE, 1);
                    }

                    Timber.d("Alarm Set: %s", alarm.getTime());

                    final Map<String, String> data = new HashMap<>();
                    data.put("State", "On");
                    data.clear();
                    data.put("Time", TIME_FORMAT.format(alarm.getTime()));

                    // TODO: Set to 1 day interval
                    am.setRepeating(AlarmManager.RTC, alarm.getTimeInMillis(), AlarmManager.INTERVAL_DAY,
                            pendingIntent);
                } else {
                    final Map<String, String> data = new HashMap<>();
                    data.put("State", "Off");
                    Timber.d("Alarm Disabled");
                }
                return true;
            });
            timePref.setSummary(timePref.getTimeDescription());
        }
    }
}
