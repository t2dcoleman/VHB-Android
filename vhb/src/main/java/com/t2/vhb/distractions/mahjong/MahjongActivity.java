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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;
import android.view.WindowManager;

import com.t2.mahjong.MahjongBackground;
import com.t2.mahjong.MahjongFragment;
import com.t2.mahjong.MahjongViewGroup.MahjongListener;
import com.t2.mahjong.db.MahjongContract.Mahjong;
import com.t2.vhb.ActionBarActivity;
import com.t2.vhb.R;

import java.util.HashMap;
import java.util.Map;

public class MahjongActivity extends ActionBarActivity implements MahjongListener, OnSharedPreferenceChangeListener {

    private static final int DIALOG_INSTRUCTIONS = 2;

    private static final String TAG = "MahjongActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.main);
        setTitle("Mahjong");
        setIcon(R.drawable.icon_distract_mahjong);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int puzzleId = prefs.getInt(getString(R.string.pref_mahjong_puzzle), -1);
        if (getSupportFragmentManager().findFragmentByTag(TAG) == null) {
            if (puzzleId >= 0) {
                Cursor cursor = getContentResolver().query(ContentUris.withAppendedId(Mahjong.CONTENT_URI, puzzleId),
                        new String[] {
                            Mahjong.COL_TITLE
                        }, null, null, null);
                if (cursor.moveToFirst()) {
                    getSupportActionBar().setSubtitle(cursor.getString(0));
                }
                cursor.close();
                MahjongFragment fragment = new MahjongFragment();
                fragment.setMahjongListener(this);
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.add(R.id.lay_mahjong, fragment, TAG);
                ft.commit();
            } else {
                startActivity(new Intent(this, MahjongChangePuzzleActivity.class));
                finish();
            }
        }

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_mahjong_background))) {
            MahjongFragment fragment = (MahjongFragment) getSupportFragmentManager().findFragmentByTag(TAG);
            MahjongBackground background;
            try {
                background = MahjongBackground.valueOf(sharedPreferences.getString(key,
                        MahjongBackground.REDWOOD.name()));
            } catch (Exception e) {
                background = MahjongBackground.REDWOOD;
            }
            fragment.getView().findViewById(R.id.lay_mahjong_board).setBackgroundResource(background.getResourceId());
            fragment.getView().findViewById(R.id.lay_mahjong_board).invalidate();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_INSTRUCTIONS:
                return new AlertDialog.Builder(this).setTitle("Mahjong Rules").setIcon(R.drawable.ic_dialog_info)
                        .setView(getLayoutInflater().inflate(R.layout.mahjong_instructions, null)).create();
        }
        return super.onCreateDialog(id);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnu_instructions:
                showDialog(DIALOG_INSTRUCTIONS);
                return true;
            case R.id.mnu_settings:
                startActivity(new Intent(this, MahjongSettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPuzzleComplete() {
        final Map<String, String> paramMap = new HashMap<>();
        paramMap.put("Puzzle", getSupportActionBar().getSubtitle().toString());
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(getString(R.string.pref_mahjong_puzzle), -1)
                .apply();
        finish();
        startActivity(new Intent(this, MahjongActivity.class));
    }

    @Override
    public void onPuzzleIncompletable() {
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

}
