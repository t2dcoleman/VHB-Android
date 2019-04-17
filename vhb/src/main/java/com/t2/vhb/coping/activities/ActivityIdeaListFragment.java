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

package com.t2.vhb.coping.activities;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.t2.vhb.R;
import com.t2.vhb.db.VhbContract.ActivityIdea;
import com.t2.vhb.widget.FavoriteCheckBox;

public class ActivityIdeaListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

    private static final int LOADER_IDEAS = 1;

    private IdeaAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getLoaderManager().initLoader(LOADER_IDEAS, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return new CursorLoader(getActivity(), ActivityIdea.CONTENT_URI, null, null, null, ActivityIdea.COL_FAVORITE
                + " DESC, "
                + ActivityIdea.COL_NAME + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
        mAdapter.swapCursor(data);
        getListView().requestFocus();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new IdeaAdapter(getActivity(), R.layout.activity_ideas_row);
        getListView().setCacheColorHint(Color.TRANSPARENT);
        setListAdapter(mAdapter);
    }

    private static final class IdeaAdapter extends SimpleCursorAdapter implements View.OnClickListener {

        public IdeaAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId, null, new String[] {}, new int[] {}, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = LayoutInflater.from(mContext).inflate(R.layout.activity_ideas_row, null);
            }

            final Cursor idea = (Cursor) getItem(position);
            ((TextView) v.findViewById(R.id.lbl_name)).setText(idea.getString(idea
                    .getColumnIndex(ActivityIdea.COL_NAME)));
            FavoriteCheckBox cb = (FavoriteCheckBox) v.findViewById(R.id.chk_favorite);
            cb.setChecked(idea.getInt(idea.getColumnIndex(ActivityIdea.COL_FAVORITE)) > 0);
            cb.setTag(position);
            cb.setOnClickListener(this);

            return v;
        }

        @Override
        public void onClick(View v) {
            CheckBox cb = (CheckBox) v;
            ContentValues vals = new ContentValues();
            vals.put(ActivityIdea.COL_FAVORITE, cb.isChecked());
            mContext.getContentResolver().update(
                    ContentUris.withAppendedId(ActivityIdea.CONTENT_URI, getItemId((Integer) v.getTag())),
                    vals, null, null);

        }

    }
}
