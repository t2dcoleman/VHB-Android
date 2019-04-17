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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.t2.copingcards.CopingContract.CopingCard;
import com.t2.copingcards.CopingContract.CopingCard.CopingSkill;
import com.t2.copingcards.CopingContract.CopingCard.Symptom;
import com.t2.vhb.ActionBarActivity;
import com.t2.vhb.R;

import java.util.ArrayList;
import java.util.List;

public class CopingEditActivity extends ActionBarActivity implements OnClickListener {

    private static final String KEY_PROBLEM_AREA = "problem_area";
    private static final String KEY_COPING_SKILLS = "coping_skills";
    private static final String KEY_SYMPTOMS = "symptoms";

    private static final String PREF_TUTORIAL_COMPLETE = "tutorial_complete";

    private Long mId;
    private int mTutorialStep;
    private boolean mHelpRequested;

    private LayoutInflater mInf;
	private TextView mStepOne;
	private EditText mProblemArea;
    private LinearLayout mCopingSkillsContainer, mSymptomsContainer;
    private SharedPreferences mPrefs;

    private TextView[] mSteps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.activity_coping_edit);
        setIcon(com.t2.vhb.R.drawable.icon_coping_cards);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mInf = LayoutInflater.from(this);

        initViews();

        final Bundle extras = savedInstanceState == null ? getIntent().getExtras() : savedInstanceState;
        if (extras != null) {
            mId = extras.containsKey("id") ? extras.getLong("id") : null;
            if (extras.containsKey(KEY_PROBLEM_AREA)) {
                mProblemArea.setText(extras.getString(KEY_PROBLEM_AREA));
                initSymptoms(extras.getStringArrayList(KEY_SYMPTOMS));
                initCopingSkills(extras.getStringArrayList(KEY_COPING_SKILLS));
            } else if (isEdit()) {
                initCard();
            }
        } else {
            createCopingSkillRow("");
            createSymptomRow("");
        }

        setTitle("Coping Cards");
        getSupportActionBar().setSubtitle(isEdit() ? "Edit Card" : "New Card");
        refreshViews();
    }

    private void initSymptoms(List<String> symptoms) {
        mSymptomsContainer.removeAllViews();
        if (symptoms.isEmpty()) {
            createSymptomRow("");
        } else {
            for (String symptom : symptoms) {
                createSymptomRow(symptom);
            }
        }
    }

    private void initCopingSkills(List<String> responses) {
        mCopingSkillsContainer.removeAllViews();
        if (responses.isEmpty()) {
            createCopingSkillRow("");
        } else {
            for (String response : responses) {
                createCopingSkillRow(response);
            }
        }
    }

    private boolean isNewUser() {
        return !mPrefs.contains(PREF_TUTORIAL_COMPLETE);
    }

    private void createCopingSkillRow(String response) {
        createCopingSkillRow(response, false);
    }

    private void createCopingSkillRow(String response, boolean changeFocus) {
        final View row = mInf.inflate(R.layout.coping_skill_edit_row, null);
        final EditText et = (EditText) row.findViewById(R.id.coping_skill);
        final ImageButton btn = (ImageButton) row.findViewById(R.id.delete);
        final int btnId = mCopingSkillsContainer.getChildCount() + 2000;
        btn.setOnClickListener(v -> {
            ((ViewGroup) v.getParent().getParent()).removeView((View) v.getParent());
            if (mCopingSkillsContainer.getChildCount() == 0) {
                createCopingSkillRow("", true);
            }
        });
        et.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        et.setOnEditorActionListener((v, actionId, event) -> {
            final int childIndex = mCopingSkillsContainer.indexOfChild((View) v.getParent());
            if (childIndex < mCopingSkillsContainer.getChildCount() - 1) {
                mCopingSkillsContainer.getChildAt(childIndex + 1).findViewById(R.id.coping_skill).requestFocus();
            } else {
                final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mStepOne.getWindowToken(), 0);
            }
            return true;
        });

        btn.setId(btnId);
        et.setNextFocusRightId(btnId);
        et.setSaveEnabled(false);
        et.setText(response);
        mCopingSkillsContainer.addView(row);

        if (changeFocus) {
            et.requestFocusFromTouch();
        }
    }

    private void createSymptomRow(String symptom) {
        createSymptomRow(symptom, false);
    }

    private void createSymptomRow(String symptom, boolean changeFocus) {
        final View row = mInf.inflate(R.layout.symptom_edit_row, null);
        final EditText et = (EditText) row.findViewById(R.id.symptom);
        final ImageButton btn = (ImageButton) row.findViewById(R.id.delete);
        final int btnId = mSymptomsContainer.getChildCount() + 3000;
        btn.setOnClickListener(v -> {
            ((ViewGroup) v.getParent().getParent()).removeView((View) v.getParent());
            if (mSymptomsContainer.getChildCount() == 0) {
                createSymptomRow("", true);
            }
        });
        et.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        et.setOnEditorActionListener((v, actionId, event) -> {
            final int childIndex = mSymptomsContainer.indexOfChild((View) v.getParent());
            if (childIndex < mSymptomsContainer.getChildCount() - 1) {
                mSymptomsContainer.getChildAt(childIndex + 1).findViewById(R.id.symptom).requestFocus();
            } else {
                mCopingSkillsContainer.getChildAt(0).findViewById(R.id.coping_skill).requestFocus();
            }
            return true;
        });
        btn.setId(btnId);
        et.setNextFocusRightId(btnId);
        et.setSaveEnabled(false);
        et.setText(symptom);
        mSymptomsContainer.addView(row);

        if (changeFocus) {
            et.requestFocusFromTouch();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.save:
                saveCard();
                setResult(RESULT_OK);
                finish();
                break;
            case R.id.discard:
                setResult(RESULT_CANCELED);
                finish();
                break;
            case R.id.add_coping_skill:
                createCopingSkillRow("", true);
                break;
            case R.id.add_symptom:
                createSymptomRow("", true);
                break;
            case R.id.step_one:
            case R.id.step_two:
            case R.id.step_three:
                nextTutorialStep();
                break;
        }
    }

    private void initCard() {
        final List<String> vals = new ArrayList<>();

        // Load Card
        Cursor c = getContentResolver().query(CopingCard.getContentUri(mId), null, null, null, null);
        if (c.moveToFirst()) {
            mProblemArea.setText(c.getString(c.getColumnIndex(CopingCard.COL_PROBLEM_AREA)));
        }
        c.close();

        // Load Coping Skills
        c = getContentResolver().query(CopingSkill.getContentUri(mId), null, null, null, null);
        vals.clear();
        if (c.moveToFirst()) {
            do {
                vals.add(c.getString(c.getColumnIndex(CopingSkill.COL_SKILL_TEXT)));
            } while (c.moveToNext());
        }
        c.close();
        initCopingSkills(vals);

        // Load Symptoms
        c = getContentResolver().query(Symptom.getContentUri(mId), null, null, null, null);
        vals.clear();
        if (c.moveToFirst()) {
            do {
                vals.add(c.getString(c.getColumnIndex(Symptom.COL_SYMPTOM_TEXT)));
            } while (c.moveToNext());
        }
        c.close();
        initSymptoms(vals);
    }

    private void initViews() {
        mStepOne = (TextView) findViewById(R.id.step_one);
	    TextView mStepTwo = (TextView) findViewById(R.id.step_two);
	    TextView mStepThree = (TextView) findViewById(R.id.step_three);

	    Button mSave = (Button) findViewById(R.id.save);
	    Button mDiscard = (Button) findViewById(R.id.discard);

	    Button mAddSymptom = (Button) findViewById(R.id.add_symptom);
	    Button mAddCopingSkill = (Button) findViewById(R.id.add_coping_skill);

        mSteps = new TextView[] {
                mStepOne, mStepTwo, mStepThree
        };

        mProblemArea = (EditText) findViewById(R.id.problem_area);
        mSymptomsContainer = (LinearLayout) findViewById(R.id.symptoms);
        mCopingSkillsContainer = (LinearLayout) findViewById(R.id.coping_skills);

        mAddSymptom.setOnClickListener(this);
        mAddCopingSkill.setOnClickListener(this);
        mStepOne.setOnClickListener(this);
        mStepTwo.setOnClickListener(this);
        mStepThree.setOnClickListener(this);
        mStepOne.bringToFront();
        mStepTwo.bringToFront();
        mStepThree.bringToFront();
        mSave.setOnClickListener(this);
        mDiscard.setOnClickListener(this);
    }


    private void saveCard() {
        final ContentValues vals = new ContentValues();
        final ContentResolver cr = getContentResolver();


        // Save/Update Card
        vals.put(CopingCard.COL_PROBLEM_AREA, mProblemArea.getText().toString().trim());
        if (isEdit()) {
            cr.update(CopingCard.getContentUri(mId), vals, null, null);
        } else {
            mId = ContentUris.parseId(cr.insert(CopingCard.CONTENT_URI, vals));
        }

        // Clear & Rewrite Coping Skills
        cr.delete(CopingSkill.getContentUri(mId), null, null);
        List<ContentValues> rows = new ArrayList<>();
        ContentValues row;
        for (int i = 0; i < mCopingSkillsContainer.getChildCount(); i++) {
            String value = ((EditText) mCopingSkillsContainer.getChildAt(i).findViewById(R.id.coping_skill)).getText()
                    .toString().trim();
            if (TextUtils.isEmpty(value)) {
                continue;
            }

            row = new ContentValues();
            row.put(CopingSkill.COL_SKILL_TEXT, value);
            rows.add(row);
        }
        cr.bulkInsert(CopingSkill.getContentUri(mId), rows.toArray(new ContentValues[] {}));

        // Clear & Rewrite Symptoms
        cr.delete(Symptom.getContentUri(mId), null, null);
        rows.clear();
        for (int i = 0; i < mSymptomsContainer.getChildCount(); i++) {
            String value = ((EditText) mSymptomsContainer.getChildAt(i).findViewById(R.id.symptom)).getText()
                    .toString().trim();
            if (TextUtils.isEmpty(value)) {
                continue;
            }

            row = new ContentValues();
            row.put(Symptom.COL_SYMPTOM_TEXT, value);
            rows.add(row);
        }
        cr.bulkInsert(Symptom.getContentUri(mId), rows.toArray(new ContentValues[] {}));

        Toast.makeText(this, "Card Saved", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case com.t2.vhb.R.id.action_help:
                mHelpRequested = true;
                mTutorialStep = 0;
                refreshViews();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void nextTutorialStep() {
        mTutorialStep++;
        if (mTutorialStep == 3) {
            mPrefs.edit().putBoolean(PREF_TUTORIAL_COMPLETE, true).apply();
            mHelpRequested = false;
        }

        refreshViews();
    }

    private void refreshViews() {
        final boolean helpVisible = isHelpVisible();
        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        final ViewGroup content = (ViewGroup) findViewById(R.id.content);

        imm.hideSoftInputFromWindow(mStepOne.getWindowToken(), 0);
        for (int i = 0; i < mSteps.length; i++) {
            mSteps[i].setVisibility(helpVisible && i == mTutorialStep ? View.VISIBLE : View.GONE);
            mSteps[i].bringToFront();
            if (helpVisible && i == mTutorialStep) {
                toggleImportantForAccessibility(content, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
                ViewCompat.setImportantForAccessibility(mSteps[i], ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
                mSteps[i].requestFocus();
                mSteps[i].sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                mSteps[i].sendAccessibilityEvent(AccessibilityEventCompat.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
            }
        }

        if (!helpVisible) {
            toggleImportantForAccessibility(content, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
    }

    private void toggleImportantForAccessibility(ViewGroup views, int status) {
        for (int j = 0; j < views.getChildCount(); j++) {
            View v = views.getChildAt(j);
            if (v instanceof ViewGroup) {
                toggleImportantForAccessibility((ViewGroup) v, status);
            } else {
                ViewCompat.setImportantForAccessibility(v, status);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (isEdit()) {
            outState.putLong("id", mId);
        }

        outState.putString(KEY_PROBLEM_AREA, mProblemArea.getText().toString());

        final ArrayList<String> skills = new ArrayList<>();
        for (int i = 0; i < mCopingSkillsContainer.getChildCount(); i++) {
            skills.add(((EditText) mCopingSkillsContainer.getChildAt(i).findViewById(R.id.coping_skill)).getText()
                    .toString());
        }
        outState.putStringArrayList(KEY_COPING_SKILLS, skills);

        final ArrayList<String> symptoms = new ArrayList<>();
        for (int i = 0; i < mSymptomsContainer.getChildCount(); i++) {
            symptoms.add(((EditText) mSymptomsContainer.getChildAt(i).findViewById(R.id.symptom)).getText().toString());
        }
        outState.putStringArrayList(KEY_SYMPTOMS, symptoms);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(com.t2.vhb.R.menu.mnu_coping_card_edit, menu);
        return true;
    }

    private boolean isEdit() {
        return mId != null;
    }

    private boolean isHelpVisible() {
        return isNewUser() || mHelpRequested;
    }

}
