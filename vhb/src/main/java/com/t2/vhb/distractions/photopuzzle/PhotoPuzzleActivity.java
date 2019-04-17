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

package com.t2.vhb.distractions.photopuzzle;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import com.t2.vhb.ActionBarActivity;
import com.t2.vhb.R;
import com.t2.vhb.db.VhbContract.Media;
import com.t2.vhb.db.VhbContract.Media.Photos;
import com.t2.vhb.distractions.photopuzzle.PhotoPuzzleView.PhotoPuzzleListener;
import com.t2.vhb.media.MediaViewActivity;
import com.t2.vhb.util.MediaUtils;

import java.security.SecureRandom;
import java.util.Random;

/**
 * @author wes
 */
public class PhotoPuzzleActivity extends ActionBarActivity implements PhotoPuzzleListener,
        OnSharedPreferenceChangeListener {

    private static final String TAG = "PhotoPuzzleActivity";

    private boolean mComplete;
    private long mPhotoId;
	private boolean mFirstCreation;
    private View mPreviousFocus;

    @Override
    public void onBackPressed() {
        if (getSolutionView().getVisibility() == View.VISIBLE) {
            hideSolution();
            return;
        }
        super.onBackPressed();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mnu_photo_puzzle, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.mnu_hint:
                if (getSolutionView().getVisibility() == View.VISIBLE) {
                    break;
                }
                showSolution();
                break;
            case R.id.mnu_settings:
                startActivity(new Intent(this, PhotoPuzzleSettingsActivity.class));
                break;
            case R.id.mnu_change_photo:
                mComplete = true;
                getSolutionView().clearAnimation();
                getSolutionView().setVisibility(View.GONE);
                createPuzzle();
                break;
            default:
                return false;
        }

        return true;
    }

    @Override
    public void onPuzzleComplete(int rows, int cols, int totalMoves) {
        mComplete = true;
        if (totalMoves > 0) {
            Toast.makeText(this, "Puzzle completed in " + totalMoves + " turns!", Toast.LENGTH_SHORT).show();
        }

        createPuzzle();
    }

    @Override
    public void onPuzzleCreated() {
        if (!mComplete && !mFirstCreation) {
            return;
        }
        getSolutionView().setImageBitmap(getPuzzleView().getSolutionImage());
        getPuzzleView().clearAnimation();
        AnimationSet set = new AnimationSet(true);
        TranslateAnimation trans = new TranslateAnimation(0, 0, -50.0f, 0);
        trans.setDuration(500);
        AlphaAnimation alpha = new AlphaAnimation(0.0f, 1.0f);
        alpha.setDuration(500);
        set.addAnimation(trans);
        set.addAnimation(alpha);
        set.setFillAfter(true);
        getPuzzleView().setLayoutAnimation(new LayoutAnimationController(set));
        getPuzzleView().startLayoutAnimation();
        getPuzzleView().setVisibility(View.VISIBLE);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_photo_puzzle_columns))
                || key.equals(getString(R.string.pref_photo_puzzle_rows))) {
	        boolean mChanged = true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.photo_puzzle);

        if (savedInstanceState != null) {
            mComplete = savedInstanceState.getBoolean("complete");
            mPhotoId = savedInstanceState.getLong("photo_id");
        } else {
            getPuzzleView().setVisibility(View.INVISIBLE);
            mFirstCreation = true;
            initPuzzle();
        }

        getPuzzleView().setPhotoPuzzleListener(this);

        getSolutionView().setOnKeyListener((v, keyCode, event) -> {
            hideSolution();
            return true;
        });

        getSolutionView().setOnClickListener(v -> hideSolution());

        if (mComplete) {
            initPuzzle();
        }

        setTitle("Photo Puzzle");
        setIcon(R.drawable.icon_distract_picture);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        Toast.makeText(this, "Tap any two tiles to swap them. Restore the photo to its original state.",
                Toast.LENGTH_LONG).show();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {

        }
        return super.onCreateDialog(id);
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("complete", mComplete);
        outState.putLong("photo_id", mPhotoId);
    }

    private void createPuzzle() {
        AnimationSet set = new AnimationSet(true);
        TranslateAnimation trans = new TranslateAnimation(0, 0, 0, 50f);
        trans.setDuration(500);
        AlphaAnimation alpha = new AlphaAnimation(1.0f, 0.0f);
        alpha.setDuration(500);
        set.addAnimation(trans);
        set.addAnimation(alpha);
        set.setFillAfter(true);

        LayoutAnimationController anim = new LayoutAnimationController(set);

        getPuzzleView().setLayoutAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationEnd(Animation animation) {
                if (mComplete) {
                    mComplete = false;
                    getPuzzleView().setVisibility(View.INVISIBLE);
                    initPuzzle();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }
        });
        getPuzzleView().setLayoutAnimation(anim);
        getPuzzleView().startLayoutAnimation();
        getPuzzleView().invalidate();
    }

    private PhotoPuzzleView getPuzzleView() {
        return (PhotoPuzzleView) findViewById(R.id.cvw_photo_grid);
    }

    private ImageView getSolutionView() {
        return (ImageView) findViewById(R.id.img_solution);
    }

    private void hideSolution() {
        Animation anim = new AlphaAnimation(0.8f, 0.0f);
        anim.setDuration(200);
        anim.setFillAfter(true);
        getSolutionView().startAnimation(anim);
        getSolutionView().setVisibility(View.GONE);
        getSolutionView().setClickable(false);
        if (mPreviousFocus != null) {
            mPreviousFocus.requestFocus();
            mPreviousFocus = null;
        }
    }

    private void initPuzzle() {
        MediaUtils.purgeMediaReferences(getContentResolver(), Photos.MEDIA_TYPE);
        mComplete = false;

        Cursor cursor = getContentResolver().query(Photos.CONTENT_URI, new String[] {
                Media.COL_EXTERNAL_ID, Media.COL_ROTATION
        }, null, null, null);
        if (!cursor.moveToFirst()) {
            cursor.close();
            Toast.makeText(this, "Please add at least one photo before playing.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MediaViewActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        Random rand = new SecureRandom();

        int maxOffset = cursor.getCount();
        long id;
        int rotation;
        do {
            int index = rand.nextInt(maxOffset);
            cursor.moveToFirst();
            cursor.move(index);
            id = cursor.getLong(0);
            rotation = cursor.getInt(1);
        } while (id == mPhotoId && maxOffset > 1);
        cursor.close();

        mPhotoId = id;

        getPuzzleView().reset();
        getPuzzleView().setImage(id, rotation);
    }

    private void showSolution() {
        Animation anim = new AlphaAnimation(0.0f, 0.8f);
        anim.setDuration(200);
        anim.setFillAfter(true);
        getSolutionView().startAnimation(anim);
        getSolutionView().setVisibility(View.VISIBLE);
        getSolutionView().setClickable(true);
        mPreviousFocus = getCurrentFocus();
        getSolutionView().requestFocus();
    }

}
