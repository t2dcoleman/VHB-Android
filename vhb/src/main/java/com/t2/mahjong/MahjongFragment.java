/*
 *
 * Created by Wes Turney on 12/17/13.
 *
 * MahjongLib
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
 * Government Agency Original Software Designation: MahjongLib001
 * Government Agency Original Software Title: MahjongLib
 * User Registration Requested. Please send email
 * with your contact information to: robert.a.kayl.civ@mail.mil
 * Government Agency Point of Contact for Original Software: robert.a.kayl.civ@mail.mil
 *
 */

package com.t2.mahjong;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import com.t2.mahjong.MahjongViewGroup.MahjongListener;
import com.t2.mahjong.db.MahjongContract;
import com.t2.vhb.R;

public class MahjongFragment extends Fragment implements MahjongListener {

    private int mPuzzleId;
    private boolean mCompleted;

    private MahjongListener mMahjongListener;

    public void setMahjongListener(MahjongListener mahjongListener) {
        mMahjongListener = mahjongListener;
    }

    private MahjongViewGroup getPuzzle() {
        return (MahjongViewGroup) getView().findViewById(R.id.lay_mahjong_tiles);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mPuzzleId = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt(
                getString(R.string.pref_mahjong_puzzle), -1);
        getPuzzle().setMahjonggListener(this);
        loadPuzzle();
    }

    private void loadPuzzle() {
        final Cursor data = getActivity().getContentResolver().query(
                ContentUris.withAppendedId(MahjongContract.Mahjong.CONTENT_URI, mPuzzleId), null, null, null, null);
        data.moveToFirst();
        final MahjongCursorLayout layout = new MahjongCursorLayout(data);
        getPuzzle().setGame(new Mahjong(layout));
        data.close();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.mnu_mahjong, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.mahjong_view, null);
        MahjongBackground background;
        try {
            background = MahjongBackground.valueOf(PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .getString(getString(R.string.pref_mahjong_background), MahjongBackground.REDWOOD.name()));
        } catch (Exception e) {
            background = MahjongBackground.REDWOOD;
        }
        v.findViewById(R.id.lay_mahjong_board).setBackgroundResource(background.getResourceId());
        return v;
    }

    @Override
    public void onPuzzleComplete() {
        final AccessibilityManager accessibilityManager = (AccessibilityManager) getActivity().getSystemService(
                Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager.isEnabled()) {
            getPuzzle().trySendAccessibilityNotification("Puzzle complete.");
            final Handler handler = new Handler();
            handler.postDelayed(this::completePuzzle, 1500);
        } else {
            Toast.makeText(getActivity(), "Puzzle Complete", Toast.LENGTH_SHORT).show();
            completePuzzle();
        }
    }

    private void completePuzzle() {
        if (mMahjongListener != null) {
            mMahjongListener.onPuzzleComplete();
        }
        mCompleted = true;
        ContentValues cv = new ContentValues();
        cv.put(MahjongContract.Mahjong.COL_COMPLETE, true);
        cv.put(MahjongContract.Mahjong.COL_CURRENT, (String) null);
        getActivity().getContentResolver().update(
                ContentUris.withAppendedId(MahjongContract.Mahjong.CONTENT_URI, mPuzzleId), cv, null, null);
    }

    @Override
    public void onPuzzleIncompletable() {
        if (mMahjongListener != null) {
            mMahjongListener.onPuzzleIncompletable();
        }
        getPuzzle().trySendAccessibilityNotification("No solutions left, resetting puzzle.");
        Toast.makeText(getActivity(), "No Solutions Left!", Toast.LENGTH_SHORT).show();
        resetPuzzle();
    }

    private void resetPuzzle() {
        ContentValues cv = new ContentValues();
        cv.put(MahjongContract.Mahjong.COL_CURRENT, (String) null);
        getActivity().getContentResolver().update(
                ContentUris.withAppendedId(MahjongContract.Mahjong.CONTENT_URI, mPuzzleId), cv, null, null);
        loadPuzzle();
        getPuzzle().trySendAccessibilityNotification("Puzzle reset.");
    }

    @Override
    public void onDestroyView() {
        getPuzzle().clearCache();
        super.onDestroyView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.mnu_hint) {
            getPuzzle().showHint();
            return true;
        } else if (item.getItemId() == R.id.mnu_change_puzzle) {
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                    .remove(getString(R.string.pref_mahjong_puzzle)).apply();
            Intent intent = new Intent(getActivity(), getActivity().getClass());
            startActivity(intent);
            getActivity().finish();
            return true;
        } else if (item.getItemId() == R.id.mnu_reset_puzzle) {
            resetPuzzle();
            return true;
        } else if (item.getItemId() == R.id.mnu_highlight) {
            getPuzzle().setShowFreeTiles(!getPuzzle().isShowFreeTiles());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getPuzzle() != null && !mCompleted) {
            ContentValues cv = new ContentValues();
            cv.put(MahjongContract.Mahjong.COL_CURRENT, getPuzzle().getCurrentState());
            getActivity().getContentResolver().update(
                    ContentUris.withAppendedId(MahjongContract.Mahjong.CONTENT_URI, mPuzzleId), cv, null, null);
        }
    }
}
