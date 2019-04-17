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

package com.t2.vhb.widget;

import android.app.ListActivity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.t2.vhb.R;
import com.t2.vhb.widget.QuoteWidgetProvider.UpdateService;

import java.util.ArrayList;

public class QuoteWidgetPreferenceActivity extends ListActivity {

    private BackgroundAdapter mAdapter;

    private int mAppWidgetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setTitle("Select a Background");
        Toast.makeText(this, "Please select a background.", Toast.LENGTH_SHORT).show();

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        mAdapter = new BackgroundAdapter(this);
        setListAdapter(mAdapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        String rName = getResources().getResourceName(mAdapter.getItem(position));
        getSharedPreferences(getPackageName(), 0).edit().putString("widget_background", rName).apply();
        Intent resultValue = new Intent();
        this.startService(new Intent(this, UpdateService.class));
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    private static final class BackgroundAdapter extends ArrayAdapter<Integer> {
        public BackgroundAdapter(Context ctx) {
            super(ctx, R.layout.quote_widget_row, new ArrayList<>());
            add(R.drawable.widget_grd_blue);
            add(R.drawable.widget_grd_green);
            add(R.drawable.widget_grd_purple);
            add(R.drawable.widget_grey);
            add(R.drawable.widget_orange);
            add(R.drawable.widget_outgrd_blue);
            add(R.drawable.widget_outgrd_green);
            add(R.drawable.widget_outgrd_grey);
            add(R.drawable.widget_outgrd_purple);
            add(R.drawable.widget_red);
            add(R.drawable.widget_solid_blue);
            add(R.drawable.widget_solid_grey);
            add(R.drawable.widget_solid_red);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = LayoutInflater.from(getContext()).inflate(R.layout.quote_widget_row, null);
            }

            View bkg = v.findViewById(R.id.background);
            bkg.setBackgroundResource(getItem(position));
            return v;
        }
    }

}
