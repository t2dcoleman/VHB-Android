/*
 *
 * Created by Wes Turney on 12/17/13.
 *
 * PmrLib
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
 * Government Agency Original Software Designation: PmrLib001
 * Government Agency Original Software Title: PmrLib
 * User Registration Requested. Please send email
 * with your contact information to: robert.a.kayl.civ@mail.mil
 * Government Agency Point of Contact for Original Software: robert.a.kayl.civ@mail.mil
 *
 */

package com.t2.pmr;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;

import com.t2.vhb.R;

import java.io.IOException;

import timber.log.Timber;

public class PmrFragment extends Fragment implements OnClickListener, OnSharedPreferenceChangeListener, OnKeyListener {

    private static final String TAG = "com.t2.pmr.PmrFragment";

    private static final Caption[] captions = Caption.values();

    private boolean mCaptionsEnabled;
    private MediaPlayer mAudioPlayer;
    private CaptionPlayer mCaptionPlayer;
    private long mStartTime;
    private long mCurrentTime;
    private int mAudioOffset;
    private int mCaptionIndex;
    private boolean mStarted, mPaused;
    private boolean mPausing;
    private boolean mFinished;

    private AlphaAnimation mShowAnimation, mHideAnimation;

    private View mOverlay;

    private PmrSessionListener mListener;

    public interface PmrSessionListener {
        void OnSessionStart();

        void OnSessionComplete();

        void OnSessionPause();

        void OnSessionResume();
    }

    public void setPmrSessionListener(PmrSessionListener listener) {
        mListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mStarted = savedInstanceState.getBoolean("started");
            mFinished = savedInstanceState.getBoolean("finished");
            if (mStarted) {
                mStartTime = savedInstanceState.getLong("start_time");
                mCurrentTime = savedInstanceState.getLong("current_time");
                mCaptionIndex = savedInstanceState.getInt("caption_index");
                mAudioOffset = savedInstanceState.getInt("audio_offset");
                mPaused = savedInstanceState.getBoolean("paused");
            }
        }

        mCaptionsEnabled = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(
                getString(R.string.pref_pmr_captions), true);

        mShowAnimation = new AlphaAnimation(0, 1);
        mShowAnimation.setInterpolator(new LinearInterpolator());
        mShowAnimation.setDuration(500);
        mShowAnimation.setFillAfter(true);
        mShowAnimation.setFillBefore(true);
        mShowAnimation.setFillEnabled(true);

        mHideAnimation = new AlphaAnimation(1, 0);
        mHideAnimation.setInterpolator(new LinearInterpolator());
        mHideAnimation.setDuration(500);
        mHideAnimation.setFillAfter(true);
        mHideAnimation.setFillBefore(true);
        mHideAnimation.setFillEnabled(true);

        mCaptionPlayer = new CaptionPlayer();

        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mOverlay = view.findViewById(R.id.lay_overlay);
        view.findViewById(R.id.lbl_start).setVisibility(mStarted ? View.GONE : View.VISIBLE);
        mOverlay.setVisibility(mPaused ? View.VISIBLE : View.GONE);
        getCaptionView().setVisibility(View.GONE);
        View wrapper = view.findViewById(R.id.lay_pmr_wrapper);
        wrapper.setOnKeyListener(this);
        wrapper.setOnClickListener(this);
        wrapper.requestFocus();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_pmr_captions))) {
            mCaptionsEnabled = sharedPreferences.getBoolean(key, true);
            if (mCaptionsEnabled) {
                mCaptionPlayer.start();
            } else {
                mCaptionPlayer.stop();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mStarted && !mPaused) {
            start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
    }

    private void startOrPause() {
        if (mFinished) {
            return;
        }

        if (mStarted) {
            if (mPaused) {
                mPaused = false;
                start();
                if (mListener != null) {
                    mListener.OnSessionResume();
                }
            } else {
                pause(true);
                mPaused = true;
                if (mListener != null) {
                    mListener.OnSessionPause();
                }
            }
        } else {
            mStarted = true;
            Animation anim = new AlphaAnimation(1.0f, 0.0f);
            anim.setFillAfter(true);
            anim.setDuration(2000);
            getView().findViewById(R.id.lbl_start).setVisibility(View.GONE);
            getView().findViewById(R.id.lbl_start).startAnimation(anim);
            start();
            if (mListener != null) {
                mListener.OnSessionStart();
            }
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
                && event.getAction() == KeyEvent.ACTION_UP) {
            startOrPause();
            return true;
        }
        return false;

    }

    @Override
    public void onClick(View v) {
        startOrPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.pmr_view, null);
    }

    @Override
    public void onPause() {
        pause(false);
        super.onPause();
    }

    public boolean isPlaying() {
        return mStarted && !mPaused && !mPausing && !mFinished;
    }

    private void pause(boolean clicked) {
        if (mPausing) {
            return;
        }
        mPausing = true;

        if (mPaused && clicked) {
            mPausing = false;
            return;
        }

        if (mCaptionPlayer != null) {
            mCaptionPlayer.stop();
        }
        getPmrView().cancelAllRunnables();

        if (mAudioPlayer != null) {
            try {
                if (mAudioPlayer.isPlaying()) {
                    mAudioOffset = mAudioPlayer.getCurrentPosition();
                    Timber.e("Audio Offset Recorded: %s", mAudioOffset);
                    mCurrentTime = System.currentTimeMillis();
                    mAudioPlayer.stop();
                }
            } catch (IllegalStateException ignored) {

            } finally {
                mAudioPlayer.release();
                mAudioPlayer = null;
            }

        }

        if (clicked) {
            mStartTime = mCurrentTime - mAudioOffset;
            getPmrView().pause();
            showOverlay();
            mOverlay.setContentDescription("Paused. Tap to resume.");
            mOverlay.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            mOverlay.sendAccessibilityEvent(AccessibilityEventCompat.TYPE_ANNOUNCEMENT);
        }

        mPausing = false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("finished", mFinished);
        if (mStarted) {
            pause(false);
            outState.putBoolean("started", mStarted);
            outState.putBoolean("paused", mPaused);
            outState.putLong("start_time", mStartTime);
            outState.putLong("current_time", System.currentTimeMillis());
            outState.putInt("caption_index", mCaptionIndex);
            outState.putInt("audio_offset", mAudioOffset);
        }
    }

    private void start() {
        if (mAudioPlayer != null) {
            try {
                mAudioPlayer.stop();
            } catch (IllegalStateException ignored) {
            } finally {
                mAudioPlayer.release();
                mAudioPlayer = null;
            }

        }

        mAudioPlayer = new MediaPlayer();

        mAudioPlayer.setOnInfoListener((mp, what, extra) -> {
            Timber.e("Media player info occurred. What: " + what + ", Extra: " + extra);
            return false;
        });

        mAudioPlayer.setOnBufferingUpdateListener((mp, percent) -> Timber.e("Media player buffering update " + percent));

        mAudioPlayer.setOnCompletionListener(mp -> {
            Timber.e("Play Complete");
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        });
        mAudioPlayer.setOnErrorListener((mp, what, extra) -> {
            Timber.e("Media player error occurred. What: " + what + ", Extra: " + extra);
            return true;
        });

        AssetFileDescriptor fd = getResources().openRawResourceFd(R.raw.pmr);

        if (fd != null) {
            try {
                mAudioPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
                fd.close();
            } catch (Exception e) {
                Timber.e(e);
            }
            mAudioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mAudioPlayer.setVolume(1, 1);
            mAudioPlayer.setOnPreparedListener(mp -> Timber.e("Prepared"));
            try {
                mAudioPlayer.prepare();
                if (mAudioOffset > 0) {
                    mAudioPlayer.setOnSeekCompleteListener(mp -> {
                        Timber.e("Seek Complete");

                        if (mCaptionsEnabled) {
                            mCaptionPlayer.start();
                        }

                        // This is done because we lose some time in the
                        // recreation
                        // shift
                        mStartTime = mCurrentTime - mAudioPlayer.getCurrentPosition();
                        getPmrView().start(mAudioPlayer.getCurrentPosition());
                        hideOverlay();
                        mAudioPlayer.setOnSeekCompleteListener(null);
                        mAudioPlayer.start();
                    });
                    mAudioPlayer.seekTo((mAudioOffset));
                } else {
                    mStartTime = System.currentTimeMillis();
                    if (mCaptionsEnabled) {
                        mCaptionPlayer.start();
                    }
                    mAudioPlayer.start();
                    getPmrView().start(0);
                    hideOverlay();
                }
            } catch (IllegalStateException | IOException e) {
                Timber.e(e);
            }
        }

    }

    private void hideOverlay() {
        if (mOverlay != null && mOverlay.getVisibility() == View.VISIBLE) {
            Animation anim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
            anim.setDuration(300);
            anim.setFillAfter(true);
            mOverlay.setVisibility(View.GONE);
            mOverlay.startAnimation(anim);
        }
    }

    private void showOverlay() {
        if (mOverlay != null && mOverlay.getVisibility() == View.GONE) {
            Animation anim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
            anim.setDuration(300);
            anim.setFillAfter(true);
            mOverlay.setVisibility(View.VISIBLE);
            mOverlay.startAnimation(anim);
        }
    }

    private CaptionView getCaptionView() {
        if (getView() == null) {
            return null;
        }
        return (CaptionView) getView().findViewById(R.id.lbl_caption);
    }

    private PmrView getPmrView() {
        if (getView() == null) {
            return null;
        }
        return (PmrView) getView().findViewById(R.id.lay_pmr);
    }

    private final class CaptionPlayer implements Runnable {
        private Handler mHandler;

        @Override
        public synchronized void run() {
            if (getCaptionView() == null) {
                return;
            }

            if (mAudioPlayer == null) {
                stop();
                return;
            }

            Caption c = captions[mCaptionIndex];
            long delta = mAudioPlayer.getCurrentPosition();
            while (delta > c.getEndOffset() && mCaptionIndex < captions.length) {
                mCaptionIndex++;
                c = captions[mCaptionIndex];
            }

            boolean visible = getCaptionView().getVisibility() == View.VISIBLE;
            boolean betweenCaptions = delta < c.getStartOffset();
            boolean finished = mCaptionIndex > captions.length - 1;

            Timber.d("Delta: " + delta + ", Start: " + c.getStartOffset() + ", End: " + c.getEndOffset() + ", Vis: "
                    + visible + ", Between: " + betweenCaptions);

            if (finished) {
                stop();
                return;
            }

            if (!betweenCaptions && !visible) {
                Timber.d("Showing");
                getCaptionView().setText(c.getText());
                showCaption();
            } else if (betweenCaptions && visible) {
                Timber.d("Hiding");
                hideCaption();
            }

            mHandler.postDelayed(this, 500);
        }

        private void showCaption() {
            if (getCaptionView().getVisibility() == View.GONE) {
                getCaptionView().startAnimation(mShowAnimation);
            }
            getCaptionView().setVisibility(View.VISIBLE);
        }

        private void hideCaption() {
            if (getCaptionView().getVisibility() == View.VISIBLE) {
                getCaptionView().startAnimation(mHideAnimation);
            }
            getCaptionView().setVisibility(View.GONE);
        }

        public void start() {
            mHandler = getPmrView().getHandler();

            if (mCaptionIndex >= captions.length) {
                return;
            }

            run();
        }

        public void stop() {
            hideCaption();
            if (mHandler != null) {
                mHandler.removeCallbacks(this);
                mHandler = null;
            }

        }
    }

}
