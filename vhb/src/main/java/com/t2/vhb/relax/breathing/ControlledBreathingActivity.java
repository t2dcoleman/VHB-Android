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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.MediaColumns;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.t2.controlledbreathing.ControlledBreathingFragment;
import com.t2.controlledbreathing.ControlledBreathingFragment.BackgroundProvider;
import com.t2.controlledbreathing.ControlledBreathingFragment.MusicProvider;
import com.t2.vhb.ActionBarActivity;
import com.t2.vhb.R;
import com.t2.vhb.db.VhbContract.Media;
import com.t2.vhb.db.VhbContract.Media.BreathingMusic;
import com.t2.vhb.util.MediaUtils;

import java.security.SecureRandom;
import java.util.Random;

public class ControlledBreathingActivity extends ActionBarActivity implements BackgroundProvider, MusicProvider {

    private static final String TAG = "ControlledBreathingActivity";
    private static final int REQUEST_SETTINGS = 1;
    private static final int DIALOG_INFO = 1;

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_INFO:
                return new AlertDialog.Builder(this)
                        .setTitle("Controlled Breathing")
                        .setIcon(R.drawable.ic_dialog_info)
                        .setMessage(
                                "Controlled breathing is a technique that you can use to counteract stress."
                                        + "\n\nTo make this tool more effective, please take a moment to personalize your settings.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            startActivityForResult(new Intent(ControlledBreathingActivity.this,
                                    ControlledBreathingSettingsActivity.class), REQUEST_SETTINGS);
                            PreferenceManager.getDefaultSharedPreferences(ControlledBreathingActivity.this).edit()
                                    .putBoolean(getString(R.string.pref_breathing_setup), true).apply();
                        }).setNegativeButton("No Thanks", (dialog, which) -> {
                            dismissDialog(DIALOG_INFO);
                            PreferenceManager.getDefaultSharedPreferences(ControlledBreathingActivity.this).edit()
                                    .putBoolean(getString(R.string.pref_breathing_setup), true).apply();
                        }).create();
        }
        return super.onCreateDialog(id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.controlled_breathing);
        setTitle("Controlled Breathing");

        if (!PreferenceManager.getDefaultSharedPreferences(ControlledBreathingActivity.this).getBoolean(
                getString(R.string.pref_breathing_setup), false)) {
            showDialog(DIALOG_INFO);
        }
    }

    @Override
    public Uri getMusic() {
        Cursor cursor = getContentResolver().query(BreathingMusic.CONTENT_URI, null, null, null, null);
        Random rand = new SecureRandom();
        int count = cursor.getCount();
        if (count <= 0) {
            return null;
        }

        int index = rand.nextInt(count);
        cursor.moveToPosition(index);
        Uri song = ContentUris.withAppendedId(Audio.Media.EXTERNAL_CONTENT_URI,
                Long.parseLong(cursor.getString(cursor.getColumnIndex(Media.COL_EXTERNAL_ID))));
        cursor.close();
        return song;
    }

    @Override
    public Bitmap getBackground() {
        try (
                final Cursor cursor = getContentResolver().query(Media.Photos.CONTENT_URI, new String[]{
                        Media.COL_EXTERNAL_ID, Media.COL_ROTATION
                }, null, null, null)) {
            if (cursor == null || cursor.getCount() == 0) {
                return null;
            }

            final Random rand = new SecureRandom();
            final int index = rand.nextInt(cursor.getCount());

            cursor.moveToPosition(index);
            final long id = cursor.getLong(0);
            final int rotation = cursor.getInt(1);
            cursor.close();

            final Cursor media = getContentResolver().query(
                    ContentUris.withAppendedId(Images.Media.EXTERNAL_CONTENT_URI, id), new String[]{
                            MediaColumns.DATA
                    }, null, null, null);
            String path = null;
            if (media!= null && media.moveToFirst()) {
                path = media.getString(0);
            }
            if (media != null) {
                media.close();
            }

            if (path == null) {
                return null;
            }

            Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            int maxDim = display.getHeight();
            if (display.getWidth() > maxDim) {
                maxDim = display.getWidth();
            }

            Bitmap image = MediaUtils.sampleImage(getContentResolver(), path, Integer.MAX_VALUE, maxDim, rotation,
                    false, true);

            float ratio = (float) maxDim / (float) image.getHeight();
            image = Bitmap.createScaledBitmap(image, (int) (image.getWidth() * ratio),
                    (int) (image.getHeight() * ratio), false);
            return image;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mnu_breathing, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnu_restart:
                Intent intent = new Intent(this, ControlledBreathingActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                break;
            case R.id.mnu_settings:
                startActivityForResult(new Intent(this, ControlledBreathingSettingsActivity.class), REQUEST_SETTINGS);
                ControlledBreathingFragment frg = (ControlledBreathingFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.frg_controlled_breathing);
                if (frg.isPlaying()) {
                    frg.pauseSession();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SETTINGS:
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

}
