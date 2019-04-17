/*
 *
 * Created by Wes Turney on 12/17/13.
 *
 * SudokuLib
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
 * Government Agency Original Software Designation: SudokuLib001
 * Government Agency Original Software Title: SudokuLib
 * User Registration Requested. Please send email
 * with your contact information to: robert.a.kayl.civ@mail.mil
 * Government Agency Point of Contact for Original Software: robert.a.kayl.civ@mail.mil
 *
 */

package com.t2.sudoku;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.t2.sudoku.db.SudokuContract.Sudoku;
import com.t2.sudoku.db.SudokuContract.Sudoku.Difficulty;
import com.t2.vhb.R;

public class SudokuPuzzlesFragment extends ListFragment implements LoaderCallbacks<Cursor>, OnItemClickListener {

    private static final int LOADER_PUZZLES = 1;

    private SudokuPuzzleAdapter mAdapter;
    private Difficulty mDifficulty;

    public static SudokuPuzzlesFragment createInstance(Difficulty difficulty) {
        SudokuPuzzlesFragment frg = new SudokuPuzzlesFragment();
        Bundle args = new Bundle();
        args.putSerializable("difficulty", difficulty);
        frg.setArguments(args);
        return frg;
    }

    public interface OnSudokuPuzzleSelectedListener {
        void onSudokuPuzzleSelected(int id);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getListView().setCacheColorHint(Color.TRANSPARENT);

        mDifficulty = (Difficulty) getArguments().getSerializable("difficulty");

        getLoaderManager().initLoader(LOADER_PUZZLES, null, this);
        mAdapter = new SudokuPuzzleAdapter(getActivity(), null);
        setListAdapter(mAdapter);
        getListView().setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        Cursor cursor = (Cursor) mAdapter.getItem(arg2);
        int id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID));
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                .putInt(getString(R.string.pref_sudoku_puzzle), id).apply();

        if (getActivity() instanceof OnSudokuPuzzleSelectedListener) {
            ((OnSudokuPuzzleSelectedListener) getActivity()).onSudokuPuzzleSelected(id);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), Sudoku.CONTENT_URI.buildUpon().appendPath(mDifficulty.name()).build(),
                new String[] {
                        Sudoku.COL_TITLE, Sudoku.COL_COMPLETE, BaseColumns._ID
                }, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    private final class SudokuPuzzleAdapter extends SimpleCursorAdapter {

        public SudokuPuzzleAdapter(Context context, Cursor c) {
            super(context, R.layout.sudoku_row, c, new String[] {
                    Sudoku.COL_TITLE, Sudoku.COL_COMPLETE
            }, new int[] {
                    R.id.lbl_name, R.id.img_complete
            }, 0);
        }

        @Override
        public View getView(int position, View reuseView, ViewGroup parent) {
            View v = super.getView(position, reuseView, parent);

            TextView tv = (TextView) v.findViewById(R.id.lbl_name);
            tv.setText(String.format("%s #%d", mDifficulty.getName(), position + 1));
            View complete = v.findViewById(R.id.img_complete);
            v.setContentDescription(String.format("%s Number %d: %s", mDifficulty.getAccessibilityName(), position + 1,
                    complete.getVisibility() == View.VISIBLE ? "Complete." : "Incomplete"));

            return v;
        }

        @Override
        public void setViewImage(ImageView v, String value) {
            v.setVisibility(Integer.parseInt(value) == 1 ? View.VISIBLE : View.INVISIBLE);
        }

    }

}
