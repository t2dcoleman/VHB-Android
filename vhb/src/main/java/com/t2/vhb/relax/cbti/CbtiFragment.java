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

package com.t2.vhb.relax.cbti;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.t2.vhb.R;
import com.t2.vhb.relax.cbti.CbtiActivity.CbtiType;

public abstract class CbtiFragment extends Fragment implements OnCompletionListener, OnSeekCompleteListener {

    private static final String TAG = "com.t2.vhb.relax.cbti.CbtiFragment";

    private static final String KEY_AUDIO_POSITION = "audio_position";
    private static final String KEY_PLAYING = "playing";

    protected abstract int[] getCaptionIds();

    protected abstract int[] getCaptionStarts();

    protected abstract int getMediaId();

    protected abstract int getPlaceholderImageId();

    protected abstract int getWelcomeId();

    public abstract CbtiType getType();

    private int mAudioPosition;
    private boolean mPlaying;
    private int mLastCaptionIndex;
    private TextView mCaptionView;
    private Button mPlayView;
    private Button mPauseView;
    private ImageView mPlaceholderView;

    private Handler mHandler;

    private MediaPlayer mMediaPlayer;

    private final Runnable mSequencer = new Runnable() {
        @Override
        public void run() {
            if (mMediaPlayer.isPlaying()) {
                final int position = mMediaPlayer.getCurrentPosition();
                final int captionIndex = getCurrentCaptionIndex(position);
                if (mLastCaptionIndex != captionIndex) {
                    mCaptionView.setText(getCaptionIds()[captionIndex]);
                    mLastCaptionIndex = captionIndex;
                }
            }
            mHandler.postDelayed(mSequencer, 500);
        }
    };


    private void onPlaybackStarted() {

        mPlayView.setEnabled(false);
        mPauseView.setEnabled(true);
    }

    private void onPlaybackStopped() {

        mPlayView.setEnabled(true);
        mPauseView.setEnabled(false);
    }

    private void onPlaybackComplete() {

        mPlayView.setEnabled(true);
        mPauseView.setEnabled(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.cbti, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCaptionView = (TextView) view.findViewById(R.id.caption);
        mPlayView = (Button) view.findViewById(R.id.play);
        mPauseView = (Button) view.findViewById(R.id.pause);
        mPlaceholderView = (ImageView) view.findViewById(R.id.placeholder);
        mPlaceholderView.setImageResource(getPlaceholderImageId());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mAudioPosition = savedInstanceState.getInt(KEY_AUDIO_POSITION);
            mPlaying = savedInstanceState.getBoolean(KEY_PLAYING);
        }

        mPlayView.setOnClickListener(v -> {
            mPlaying = true;
            play();
        });

        mPauseView.setOnClickListener(v -> pausePlayback());

        mPauseView.setEnabled(mPlaying);
        mPlayView.setEnabled(!mPlaying);

        mLastCaptionIndex = getCurrentCaptionIndex(mAudioPosition);
        mCaptionView.setText(mLastCaptionIndex >= 0 ? getCaptionIds()[mLastCaptionIndex] : getWelcomeId());
    }

    private void pausePlayback() {
        mPlaying = false;
        mMediaPlayer.pause();
        mAudioPosition = mMediaPlayer.getCurrentPosition();
        mHandler.removeCallbacks(mSequencer);
        onPlaybackStopped();
        mPlaceholderView.setContentDescription("Paused.");
        mPlaceholderView.sendAccessibilityEvent(AccessibilityEventCompat.TYPE_ANNOUNCEMENT);
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        if (mPlaying) {
            play();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Uri uri = Uri.parse("android.resource://" + getActivity().getPackageName() + "/" + getMediaId());
        mHandler = new Handler();
        mMediaPlayer = MediaPlayer.create(getActivity(), uri);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnSeekCompleteListener(this);
        if (mAudioPosition > 0) {
            mMediaPlayer.seekTo(mAudioPosition);
        }
    }

    @Override
    public void onPause() {
        if (mPlaying) {
            if (mMediaPlayer != null) {
                mAudioPosition = mMediaPlayer.getCurrentPosition();
                mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
            mHandler.removeCallbacks(mSequencer);
        }
        super.onPause();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mAudioPosition = 0;
        mLastCaptionIndex = -1;
        mCaptionView.setText(getWelcomeId());
        onPlaybackComplete();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mMediaPlayer != null) {
            mAudioPosition = mMediaPlayer.getCurrentPosition();
        }

        outState.putInt(KEY_AUDIO_POSITION, mAudioPosition);
        outState.putBoolean(KEY_PLAYING, mPlaying);
    }

    private void play() {
        mPlaceholderView.setContentDescription("Guided imagery icon.");
        mMediaPlayer.start();
        mSequencer.run();
        onPlaybackStarted();
    }

    private int getCurrentCaptionIndex(int millis) {
        if (millis <= 0) {
            return -1;
        }

        final int seconds = millis / 1000;
        if (getCaptionStarts()[0] > seconds) {
            return 0;
        }

        int captionStart;
        int nextCaptionStart;
        for (int i = 0; i < getCaptionStarts().length - 1; i++) {
            captionStart = getCaptionStarts()[i];
            nextCaptionStart = getCaptionStarts()[i + 1];
            if (captionStart <= seconds && nextCaptionStart > seconds) {
                return i;
            }
        }

        return getCaptionStarts().length - 1;
    }
}
