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

package com.t2.vhb.home;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.preference.PreferenceFragment;
import android.view.WindowManager;

import com.t2.vhb.ActionBarActivity;
import com.t2.vhb.R;
import com.t2.vhb.UpdateLoginActivity;

import java.util.List;

public class HomeSettingsActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.settings);

        setTitle("Settings");
        getSupportFragmentManager().beginTransaction().replace(R.id.settings, new HomeSettingsFragment()).commit();
    }

    public static class HomeSettingsFragment extends PreferenceFragment implements OnPreferenceChangeListener,
            OnPreferenceClickListener, LoaderCallbacks<Uri> {

        public static final int DIALOG_STATISTICS = 1;

        private ProgressDialog mProgressDialog;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prf_global);


            findPreference(getString(R.string.pref_clear_preferences)).setOnPreferenceClickListener(this);

            findPreference(getString(R.string.pref_send_feedback)).setOnPreferenceClickListener(this);
            findPreference(getString(R.string.pref_rate_app)).setOnPreferenceClickListener(this);
            findPreference(getString(R.string.pref_update_security)).setOnPreferenceClickListener(this);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            return false;
        }

        @Override
        public Loader<Uri> onCreateLoader(int id, Bundle arg1) {

            return null;
        }

        @Override
        public void onLoadFinished(Loader<Uri> loader, Uri result) {}

        @Override
        public void onLoaderReset(Loader<Uri> arg0) {

        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            final String key = preference.getKey();
            if (key.equals(getString(R.string.pref_clear_preferences))) {
                final DialogFragment frg = new ClearPreferencesDialog();
                frg.setTargetFragment(this, 0);
                frg.show(getFragmentManager(), "clear_prefs");
                return true;
            } else if (key.equals(getString(R.string.pref_rate_app))) {
                final String pack = "com.t2.vhb";
                final String installer = "test";
                final Intent intent = new Intent(Intent.ACTION_VIEW);
                if (installer != null && installer.equals("com.amazon.venezia")) {
                    intent.setData(Uri.parse("amzn://apps/android?p=" + pack));
                } else {
                    intent.setData(Uri.parse("market://details?id=" + pack));
                }

                final List<ResolveInfo> handlers = getActivity().getPackageManager().queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
                if (handlers.isEmpty()) {
                    if (installer != null && installer.equals("com.amazon.venezia")) {
                        intent.setData(Uri.parse("http://www.amazon.com/gp/mas/dl/android?p=" + pack));
                    } else {
                        intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + pack));
                    }
                }

                startActivity(intent);
            } else if (key.equals(getString(R.string.pref_send_feedback))) {
                final Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setType("message/rfc822");
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {
                    "usarmy.ncr.medcom-usamrmc-dcoe.t2-central@mail.mil"
                });
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "VHB Feedback");
                startActivity(Intent.createChooser(emailIntent, "Send mail..."));
            } else if (key.equals(getString(R.string.pref_update_security))) {
                startActivity(new Intent(getActivity(), UpdateLoginActivity.class));
            }

            return false;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            Fragment dlg = getFragmentManager().findFragmentByTag("disenroll");
            if (dlg != null) {
                dlg.setTargetFragment(this, 0);
            }
        }

    }

}
