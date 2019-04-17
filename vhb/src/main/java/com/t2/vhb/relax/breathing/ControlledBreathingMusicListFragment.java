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

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.MediaColumns;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView;
import android.widget.Toast;

import com.t2.vhb.R;
import com.t2.vhb.db.VhbContract.Media;
import com.t2.vhb.util.MediaUtils;
import com.t2.vhb.util.OnFragmentDataLoadedListener;

/**
 * @author wes
 */
public class ControlledBreathingMusicListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

    private static final int LOADER_MUSIC = 1;

    private CursorAdapter mAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        MediaUtils.purgeMediaReferences(getActivity().getContentResolver(), Media.BreathingMusic.MEDIA_TYPE);

        getLoaderManager().initLoader(LOADER_MUSIC, null, this);

        getListView().setCacheColorHint(Color.TRANSPARENT);
        mAdapter = new CursorAdapter(getActivity(), null, 0) {

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                TextView titleView = (TextView) view.findViewById(R.id.lbl_title);
                TextView artistView = (TextView) view.findViewById(R.id.lbl_artist);

                long sysId = cursor.getLong(cursor.getColumnIndex(Media.COL_EXTERNAL_ID));

                Cursor data = context.getContentResolver().query(Audio.Media.EXTERNAL_CONTENT_URI, new String[] {
                        MediaColumns.TITLE, AudioColumns.ARTIST
                }, BaseColumns._ID + " = " + sysId, null, null);

                if (data.moveToFirst()) {
                    String title = data.getString(0);
                    if (title.trim().length() > 0) {
                        titleView.setText(title);
                    } else {
                        titleView.setText(R.string.music_untitled);
                    }

                    String artist = data.getString(1);
                    if (!artist.equals("<unknown>")) {
                        artistView.setText(artist);
                    } else {
                        artistView.setText(R.string.music_unknown_artist);
                    }
                }

                data.close();
            }

            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                return LayoutInflater.from(context).inflate(R.layout.music_list_row, parent, false);
            }
        };

        setListAdapter(mAdapter);

        registerForContextMenu(getListView());
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnu_remove:
                AdapterView.AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
                Cursor cursor = (Cursor) mAdapter.getItem(info.position);

                getActivity().getContentResolver().delete(
                        Media.BreathingMusic.CONTENT_URI.buildUpon()
                                .appendEncodedPath(cursor.getInt(cursor.getColumnIndex(BaseColumns._ID)) + "").build(), null,
                        null);
                Toast.makeText(getActivity(), R.string.music_remove_success, Toast.LENGTH_SHORT).show();
                return true;
        }
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(R.menu.ctx_music, menu);

        AdapterView.AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Cursor selectedCursor = (Cursor) mAdapter.getItem(info.position);
        long id = selectedCursor.getLong(selectedCursor.getColumnIndex(Media.COL_EXTERNAL_ID));
        Cursor song = getActivity().getContentResolver().query(Audio.Media.EXTERNAL_CONTENT_URI, new String[] {
            MediaColumns.TITLE
        }, BaseColumns._ID + " = " + id, null, null);
        song.moveToFirst();
        menu.setHeaderTitle(song.getString(0));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), Media.BreathingMusic.CONTENT_URI, null, Media.COL_INACTIVE + " = 0",
                null, null);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        if (getActivity() instanceof OnFragmentDataLoadedListener) {
            int count = data != null ? data.getCount() : 0;
            ((OnFragmentDataLoadedListener) getActivity()).onFragmentDataLoaded(count);
        }
    }
}
