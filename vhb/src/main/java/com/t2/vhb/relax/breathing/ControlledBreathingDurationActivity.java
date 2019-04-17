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

import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.TextView;

import com.t2.controlledbreathing.DurationType;
import com.t2.vhb.ActionBarActivity;
import com.t2.vhb.R;

import java.text.DecimalFormat;

public class ControlledBreathingDurationActivity extends ActionBarActivity implements OnTouchListener, OnKeyListener,
        OnClickListener {

    public static final String KEY_TYPE = "type";

    private final DecimalFormat mFormatter = new DecimalFormat("#0.0' s'");
    private final DecimalFormat mAccessibilityFormatter = new DecimalFormat("#0.0' seconds'");

    private long mResult;
    private long mStartTime;

    private long mPreviousDuration;

    private DurationType mDurationType;

    private Handler mHandler;
    private Runnable mTimer;

    private Button mDone, mRevert, mHold, mDisable;
	private TextView mCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.controlled_breathing_duration);

        if (savedInstanceState != null) {
            mDurationType = (DurationType) savedInstanceState.getSerializable(KEY_TYPE);
        } else {
            mDurationType = (DurationType) getIntent().getExtras().getSerializable(KEY_TYPE);
        }

        if (mDurationType == null) {
            finish();
            return;
        }

        mPreviousDuration = PreferenceManager.getDefaultSharedPreferences(this).getLong(
                getString(mDurationType.getPrefKeyId()), mDurationType.getDefaultDuration());

        mHandler = new Handler();

        mDone = (Button) findViewById(R.id.btn_done);
        mRevert = (Button) findViewById(R.id.btn_revert);
        mHold = (Button) findViewById(R.id.btn_hold);
        mDisable = (Button) findViewById(R.id.btn_disable);

	    TextView mTitle = ((TextView) findViewById(R.id.lbl_title));
	    TextView mInstructions = ((TextView) findViewById(R.id.lbl_instructions));
        mCount = ((TextView) findViewById(R.id.lbl_count));

        setTitle("Controlled Breathing");
        mTitle.setText(getString(mDurationType.getNameId()) + " Duration");
        mInstructions.setText(mDurationType.getInstructionsId());

        mHold.setOnKeyListener(this);
        mHold.setOnTouchListener(this);

        mRevert.setOnClickListener(this);
        mDisable.setOnClickListener(this);
        mDone.setOnClickListener(this);

        resetCounter();
    }

    private void updateButtonVisibility() {
        mHold.setVisibility(mResult == 0 ? View.VISIBLE : View.GONE);
        mRevert.setVisibility(mResult == 0 ? View.GONE : View.VISIBLE);
        mDone.setVisibility(mResult == 0 ? View.GONE : View.VISIBLE);
        mDisable.setVisibility(mDurationType.isShowDisable() && mPreviousDuration > 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_TYPE, mDurationType);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            if (mResult == 0) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && mTimer == null) {
                    startCounter();
                    return true;
                } else if (event.getAction() == KeyEvent.ACTION_UP && mTimer != null) {
                    stopCounter();
                    return true;
                }
            }
        }
        return false;
    }

    private void startCounter() {
        mStartTime = System.currentTimeMillis();
        mTimer = new Runnable() {
            @Override
            public void run() {
                updateCounter();
                mHandler.postDelayed(this, 100);
            }
        };
        mHandler.removeCallbacks(mTimer);
        mHandler.postDelayed(mTimer, 100);
    }

    private void setCountText(float seconds) {
        mCount.setText(mFormatter.format(seconds));
        mCount.setContentDescription(mAccessibilityFormatter.format(seconds));
    }

    private void updateCounter() {
        final long start = mStartTime;
        final long millis = System.currentTimeMillis() - start;
        final float seconds = (millis / 1000.0f);

        setCountText(seconds);
    }

    private void stopCounter() {
        final long start = mStartTime;
        mResult = Math.max(System.currentTimeMillis() - start, mDurationType.getMinDuration());
        setCountText(mResult / 1000.0f);
        mHandler.removeCallbacks(mTimer);
        mTimer = null;

        updateButtonVisibility();
        mDone.setContentDescription("Accept " + mCount.getContentDescription().toString() + " duration");
        mDone.requestFocus();
        mDone.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
    }

    private void resetCounter() {
        mResult = 0;
        setCountText(mPreviousDuration / 1000.0f);
        updateButtonVisibility();
        mHold.requestFocus();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_revert) {
            resetCounter();
        } else if (v.getId() == R.id.btn_disable) {
            PreferenceManager.getDefaultSharedPreferences(this).edit().remove(getString(mDurationType.getPrefKeyId()))
                    .apply();
            finish();
        } else if (v.getId() == R.id.btn_done) {
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .putLong(getString(mDurationType.getPrefKeyId()), mResult).apply();
            finish();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.btn_hold) {
            if (mResult == 0) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startCounter();
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        stopCounter();
                        break;
                }
            }
        }
        return false;
    }
}
