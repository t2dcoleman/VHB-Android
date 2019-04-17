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

package com.t2.vhb.coping.cards;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.t2.copingcards.CopingContract.CopingCard;
import com.t2.copingcards.CopingContract.CopingCard.CopingSkill;
import com.t2.copingcards.CopingContract.CopingCard.Symptom;
import com.t2.vhb.R;

public class CopingViewFragment extends Fragment implements LoaderCallbacks<Cursor> {

    private static final int LOADER_COPING_SKILLS = 2;
    private static final int LOADER_SYMPTOMS = 3;
    private static final int LOADER_CARD = 1;

    private long mId;
    private View mCardContainer;
    private TextView mProblemArea;
    private LinearLayout mSymptomsContainer;
    private LinearLayout mCopingSkillsContainer;

    public static CopingViewFragment newInstance(long cardId) {
        final CopingViewFragment frg = new CopingViewFragment();

        final Bundle args = new Bundle();
        args.putLong("id", cardId);
        frg.setArguments(args);

        return frg;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_coping_detail, null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);

        mId = getArguments().getLong("id");

        initViews();

        mCardContainer.setVisibility(View.GONE);

        getLoaderManager().initLoader(LOADER_CARD, null, this);
        getLoaderManager().initLoader(LOADER_COPING_SKILLS, null, this);
        getLoaderManager().initLoader(LOADER_SYMPTOMS, null, this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(com.t2.vhb.R.menu.mnu_coping_card_details, menu);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
        if (id == LOADER_CARD) {
            return new CursorLoader(getActivity(), CopingCard.getContentUri(mId), null, null, null, null);
        } else if (id == LOADER_COPING_SKILLS) {
            return new CursorLoader(getActivity(), CopingSkill.getContentUri(mId), null, null, null, null);
        } else if (id == LOADER_SYMPTOMS) {
            return new CursorLoader(getActivity(), Symptom.getContentUri(mId), null, null, null, null);
        } else {
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() == LOADER_CARD) {
            initCard(cursor);
        } else if (loader.getId() == LOADER_COPING_SKILLS) {
            initCopingSkills(cursor);
        } else if (loader.getId() == LOADER_SYMPTOMS) {
            initSymptoms(cursor);
        }

        if (mCardContainer.getVisibility() == View.GONE) {
            mCardContainer.setVisibility(View.VISIBLE);
            Animation anim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
            anim.setStartOffset(100);
            anim.setDuration(300);
            mCardContainer.startAnimation(anim);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        int itemId = item.getItemId();
        if (itemId == com.t2.vhb.R.id.action_delete) {
            DialogFragment frg = CopingDeleteDialog.createInstance(mId);
            frg.show(getFragmentManager(), "delete");
            return true;
        } else if (itemId == com.t2.vhb.R.id.action_edit) {
            intent = new Intent(getActivity(), CopingEditActivity.class);
            intent.putExtra("id", mId);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initCard(Cursor card) {
        if (card.moveToFirst()) {
            mProblemArea.setText(card.getString(card.getColumnIndex(CopingCard.COL_PROBLEM_AREA)));
        } else {
            mProblemArea.setText("");
        }
    }

    private void initSymptoms(Cursor symptoms) {
        final LayoutInflater inf = LayoutInflater.from(getActivity());
        final LinearLayout.LayoutParams params = new LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        View container;
        TextView detail;
        mSymptomsContainer.removeAllViews();
        if (symptoms.moveToFirst()) {
            do {
                container = inf.inflate(R.layout.symptom_row, null);
                detail = (TextView) container.findViewById(R.id.detail);
                detail.setText(symptoms.getString(symptoms.getColumnIndex(Symptom.COL_SYMPTOM_TEXT)));
                mSymptomsContainer.addView(container, params);
            } while (symptoms.moveToNext());
        }
    }

    private void initCopingSkills(Cursor skills) {
        final LayoutInflater inf = LayoutInflater.from(getActivity());
        final LinearLayout.LayoutParams params = new LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        View container;
        TextView detail;
        mCopingSkillsContainer.removeAllViews();
        if (skills.moveToFirst()) {
            do {
                container = inf.inflate(R.layout.coping_skill_row, null);
                detail = (TextView) container.findViewById(R.id.detail);
                detail.setText(skills.getString(skills.getColumnIndex(CopingSkill.COL_SKILL_TEXT)));
                mCopingSkillsContainer.addView(container, params);
            } while (skills.moveToNext());
        }
    }

    private void initViews() {
        mProblemArea = (TextView) getView().findViewById(
                R.id.problem_area);
        mSymptomsContainer = (LinearLayout) getView().findViewById(
                R.id.symptoms_container);
        mCopingSkillsContainer = (LinearLayout) getView().findViewById(
                R.id.coping_skills_container);
        mCardContainer = getView().findViewById(R.id.coping_card);
    }

}
