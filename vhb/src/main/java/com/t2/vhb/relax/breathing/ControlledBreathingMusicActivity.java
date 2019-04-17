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

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Audio;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.CursorAdapter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.t2.vhb.ActionBarActivity;
import com.t2.vhb.R;
import com.t2.vhb.db.VhbContract.Media;
import com.t2.vhb.util.MediaUtils;
import com.t2.vhb.util.OnFragmentDataLoadedListener;

/**
 * @author wes
 */
public class ControlledBreathingMusicActivity extends ActionBarActivity implements OnFragmentDataLoadedListener {

    private static final int REQUEST_SELECT_MUSIC = 1;

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.mnu_music, menu);
        return true;
    }

    /*
     * (non-Javadoc)
     * @see
     * com.t2.vhb.util.OnFragmentDataLoadedListener#onFragmentDataLoaded(int)
     */
    @Override
    public void onFragmentDataLoaded(int newCount) {
        findViewById(R.id.lay_empty).setVisibility(newCount > 0 ? View.GONE : View.VISIBLE);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnu_add:
                Intent selectIntent = new Intent(Intent.ACTION_PICK, Audio.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(selectIntent, REQUEST_SELECT_MUSIC);
                break;
        }

        return super.onOptionsItemSelected(item);

    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int,
     * android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SELECT_MUSIC:
                if (resultCode == RESULT_OK) {
                    Uri musicUri = data.getData();
                    MediaUtils.saveMediaReference(this, musicUri, Media.BreathingMusic.MEDIA_TYPE);
                    Toast.makeText(this, R.string.music_add_success, Toast.LENGTH_SHORT).show();
                }
                MediaUtils.purgeMediaReferences(getContentResolver(), Media.BreathingMusic.MEDIA_TYPE);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /*
     * (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.controlled_breathing_music_view);
        setTitle("My Music");

        ListFragment frag = (ListFragment) getSupportFragmentManager().findFragmentById(R.id.frg_music);
        frag.getListView().setOnItemClickListener((arg0, view, position, arg3) -> {
            CursorAdapter adapter = (CursorAdapter) arg0.getAdapter();
            Cursor cursor = (Cursor) adapter.getItem(position);
            Intent intent = new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(
                    Audio.Media.EXTERNAL_CONTENT_URI, cursor.getLong(cursor.getColumnIndex(Media.COL_EXTERNAL_ID))));
            startActivity(intent);
        });

        ((TextView) findViewById(R.id.lbl_empty)).setText(R.string.music_empty);
        ((TextView) findViewById(R.id.lbl_empty_sub)).setText(R.string.music_press_menu);
    }

}
