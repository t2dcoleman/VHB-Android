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

package com.t2.vhb.distractions.sudoku;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;
import android.view.WindowManager;

import com.t2.sudoku.SudokuGridFragment;
import com.t2.sudoku.SudokuGridFragment.SudokuListener;
import com.t2.sudoku.db.SudokuContract.Sudoku;
import com.t2.vhb.ActionBarActivity;
import com.t2.vhb.R;

import java.util.HashMap;
import java.util.Map;

public class SudokuActivity extends ActionBarActivity implements SudokuListener {

    private static final int DIALOG_INSTRUCTIONS = 2;

    private static final String TAG = "SudokuActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.sudoku);
        setTitle("Sudoku");

        int puzzleId = PreferenceManager.getDefaultSharedPreferences(this).getInt(
                getString(R.string.pref_sudoku_puzzle), -1);
        if (getSupportFragmentManager().findFragmentByTag(TAG) == null) {
            if (puzzleId > 0) {
                Cursor cursor = getContentResolver().query(ContentUris.withAppendedId(Sudoku.CONTENT_URI, puzzleId),
                        new String[] {
                            Sudoku.COL_TITLE
                        }, null, null, null);
                if (cursor.moveToFirst()) {
                    getSupportActionBar().setSubtitle(cursor.getString(0));
                }
                cursor.close();
                Fragment fragment = new SudokuGridFragment();
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.add(R.id.lay_sudoku, fragment, TAG);
                ft.commit();
                ((SudokuGridFragment) fragment).setSudokuListener(this);
            } else {
                startActivity(new Intent(this, SudokuChangePuzzleActivity.class));
                finish();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnu_instructions:
                showDialog(DIALOG_INSTRUCTIONS);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_INSTRUCTIONS:
                return new AlertDialog.Builder(this).setTitle("Sudoku Rules").setIcon(R.drawable.ic_dialog_info)
                        .setView(getLayoutInflater().inflate(R.layout.sudoku_instructions, null)).create();
        }
        return super.onCreateDialog(id);

    }

    @Override
    public void onPuzzleComplete(String puzzleName) {
        final Map<String, String> paramMap = new HashMap<>();
        paramMap.put("Puzzle", puzzleName);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}
