/*
 *
 * Created by Wes Turney on 12/17/13.
 *
 * ControlledBreathingLib
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
 * Government Agency Original Software Designation: ControlledBreathingLib001
 * Government Agency Original Software Title: ControlledBreathingLib
 * User Registration Requested. Please send email
 * with your contact information to: robert.a.kayl.civ@mail.mil
 * Government Agency Point of Contact for Original Software: robert.a.kayl.civ@mail.mil
 *
 */

package com.t2.controlledbreathing;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.t2.controlledbreathing.ControlledBreathingBackgroundView.OnBackgroundChangedListener;
import com.t2.controlledbreathing.ControlledBreathingBarView.OnControlledBreathingEventListener;
import com.t2.vhb.R;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;

import timber.log.Timber;


public class ControlledBreathingFragment extends Fragment implements OnControlledBreathingEventListener,
        OnClickListener, OnBackgroundChangedListener, OnPreparedListener, OnSeekCompleteListener, OnCompletionListener {

    public interface BackgroundProvider {
        Bitmap getBackground();
    }

    public interface MusicProvider {
        Uri getMusic();
    }

    private static class BackgroundTaskLoader extends AsyncTask<BackgroundCategory, Void, Bitmap> {

        private WeakReference<ControlledBreathingFragment> controlledBreathingFragmentWeakReference;

        BackgroundTaskLoader(ControlledBreathingFragment context) {
            controlledBreathingFragmentWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Bitmap doInBackground(BackgroundCategory... params) {
            ControlledBreathingFragment fragment = controlledBreathingFragmentWeakReference.get();
            if(fragment == null) return null;

            BackgroundCategory category = params[0];

            if (category == BackgroundCategory.NONE) {
                return null;
            }

            Random rand = new SecureRandom();
            if (category == BackgroundCategory.PERSONAL_IMAGES) {
                if (fragment.getActivity() instanceof BackgroundProvider) {
                    return ((BackgroundProvider) fragment.getActivity()).getBackground();
                }
                return null;
            } else {
                int[] ids = category.getResources();
                int index = rand.nextInt(ids.length);
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inPreferredConfig = Bitmap.Config.RGB_565;

                Display display = ((WindowManager) fragment.getActivity().getSystemService(Context.WINDOW_SERVICE))
                        .getDefaultDisplay();
                Point defSize = new Point();
                display.getSize(defSize);
                
                int maxDim = defSize.y;
                if (defSize.x > maxDim) {
                    maxDim = defSize.x;
                }
                Bitmap image = sampleImage(fragment.getResources(), ids[index], Integer.MAX_VALUE, maxDim, false, true);
                float ratio = (float) maxDim / (float) image.getHeight();
                image = Bitmap.createScaledBitmap(image, (int) (image.getWidth() * ratio),
                        (int) (image.getHeight() * ratio), false);
                return image;
            }

        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);

            ControlledBreathingFragment fragment = controlledBreathingFragmentWeakReference.get();
            if(fragment == null) return;

            if (fragment.mBackgroundView != null && result != null) {
                fragment.mBackgroundView.queueBackground(result);
            }
        }

        private Bitmap sampleImage(Resources res, int resId, int targetWidth, int targetHeight,
                                   boolean includeWidth, boolean includeHeight) {

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(res, resId, options);

            int widthTmp = options.outWidth;
            int heightTmp = options.outHeight;
            int scale = 1;
            while (true) {
                if ((!includeWidth || widthTmp <= targetWidth) && (!includeHeight || heightTmp <= targetHeight)) {
                    break;
                }

                widthTmp /= 2;
                heightTmp /= 2;
                scale *= 2;
            }

            options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;

            Bitmap sampled = null;
            //InputStream in = null;
            while (sampled == null && scale <= 128) {
                try {
                    if (scale > 1) {
                        options.inSampleSize = scale;
                    }
                    sampled = BitmapFactory.decodeResource(res, resId, options);
                    break;
                } catch (OutOfMemoryError e) {
                    scale *= 2;
                }
            }

            return sampled;
        }

    }

    private final DecimalFormat mFormatter = new DecimalFormat("#0.0' s'");
    private Animation mInhaleMessageAnimation;

    private Animation mExhaleMessageAnimation;
    private Animation mHoldMessageAnimation;
    private Animation mRestMessageAnimation;
    private boolean mStarted;
    private boolean mStarting;
    private boolean mFinished;
    private boolean mPaused;
    private boolean mFinishStarted;

    private CountDownTimer mStartTimer;
    private MediaPlayer mPlayer;
    private MediaPlayer mMusicPlayer;

    private int mSoundPhase;
    private Handler mSessionHandler;

    private Runnable mSessionTimer;
    // private long mSessionStartTime;
    private long mSessionDuration;

    private Toast mDurationToast;
    private Uri mMusicUri;
    private int mMusicPosition;
    private ControlledBreathingBarView mBarView;
    private ControlledBreathingBackgroundView mBackgroundView;

    private TextView mHoldView, mRestView, mInhaleView, mExhaleView, mMessageView;
    private ImageButton mIncreaseDurationButton, mDecreaseDurationButton;
    private ViewGroup mContentView, mOverlayView;

    private SharedPreferences mPrefs;
    private boolean mMusicStarting;
    private boolean mPromptStarting;

    private static final int[] INHALE_AUDIO = {
            R.raw.breathing_inhale_1, R.raw.breathing_inhale_2, R.raw.breathing_inhale_3, R.raw.breathing_inhale_4
    };

    private static final int[] EXHALE_AUDIO = {
            R.raw.breathing_exhale_1, R.raw.breathing_exhale_2, R.raw.breathing_exhale_3, R.raw.breathing_exhale_4
    };

    private static final int[] MISC_AUDIO = {
            R.raw.breathing_misc_relax_1, R.raw.breathing_misc_relax_2, R.raw.breathing_misc_relax_3,
            R.raw.breathing_misc_relax_4, R.raw.breathing_misc_focus, R.raw.breathing_misc_naturally,
            R.raw.breathing_misc_rythmic, R.raw.breathing_misc_smooth
    };

    public boolean isPlaying() {
        return mStarted && !mStarting && !mPaused;
    }

    @SuppressLint("ShowToast")
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        mDurationToast = Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT);

        mBackgroundView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @SuppressWarnings("deprecation")
			@SuppressLint("NewApi")
            @Override
            public void onGlobalLayout() {
                mBackgroundView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                final BackgroundCategory category = BackgroundCategory.valueOf(PreferenceManager
                        .getDefaultSharedPreferences(getActivity()).getString(
                                getString(R.string.pref_breathing_background), BackgroundCategory.RAINFORESTS.name()));

                new BackgroundTaskLoader(ControlledBreathingFragment.this).execute(category);
            }
        });

        initDurations(savedInstanceState);

        mContentView.setOnClickListener(this);
        mIncreaseDurationButton.setOnClickListener(this);
        mDecreaseDurationButton.setOnClickListener(this);
        mBarView.setOnControlledBreathingEventListener(this);
        mBackgroundView.setOnBackgroundChangedListener(this);

        updateDurationDescriptions();

        if (mStarted) {
            mMessageView.setVisibility(View.INVISIBLE);
            mInhaleView.setVisibility(View.INVISIBLE);
            mHoldView.setVisibility(View.INVISIBLE);
            mRestView.setVisibility(View.INVISIBLE);
            mExhaleView.setVisibility(View.INVISIBLE);
            mExhaleView.setText("Exhale");
        }

        if (mFinished) {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getView().findViewById(R.id.lay_breathing).setVisibility(View.INVISIBLE);
            mBackgroundView.setVisibility(View.INVISIBLE);
            getView().findViewById(R.id.lbl_complete).setVisibility(View.VISIBLE);
        }

        if (mPaused) {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mOverlayView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackgroundChanged() {
        BackgroundCategory category = BackgroundCategory.valueOf(PreferenceManager.getDefaultSharedPreferences(
                getActivity()).getString(getString(R.string.pref_breathing_background),
                BackgroundCategory.RAINFORESTS.name()));

        new BackgroundTaskLoader(this).execute(category);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.lay_breathing_text) {
            if (mFinished) {
                getActivity().finish();
                return;
            }

            if (!mStarting && !mStarted) {
                startSession();
            } else if (!mStarting && mStarted && !mBarView.isPaused()) {
                pauseSession();
            } else if (mBarView.isPaused()) {
                resumeSession();
            }
        } else if (v.getId() == R.id.btn_add_time || v.getId() == R.id.btn_remove_time) {
            final boolean addTime = v.getId() == R.id.btn_add_time;
            final DurationType type = mBarView.isInhaling() || mBarView.isResting() ? DurationType.INHALE
                    : DurationType.EXHALE;
            final String key = getString(type.getPrefKeyId());
            final long minDuration = 1200;
            final long newDuration = getDuration(type) + (addTime ? 200 : -200);

            if (newDuration < minDuration) {
                return;
            }

            mPrefs.edit().putLong(key, newDuration).apply();
            showDurationToast(String.format("%s duration - %s", getString(type.getNameId()),
                    mFormatter.format(newDuration / 1000.0)));
            setTextFadeDuration();
        }

        if (!mStarted) {
            mBarView.invalidate();
        }

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mMusicUri = null;
        mp.reset();
        startMusic();
    }

    private long mLastTimerAnnounce = 0;

    private void updateAccessibilityState() {
        if (mBarView.isInhaling()) {
            mContentView.setContentDescription("Inhale.");
        } else if (mBarView.isExhaling()) {
            mContentView.setContentDescription("Exhale.");
        } else if (mBarView.isResting()) {
            mContentView.setContentDescription("Rest.");
        } else if (mBarView.isHolding()) {
            mContentView.setContentDescription("Hold.");
        }

        if (mSessionDuration > 0 && System.currentTimeMillis() - mLastTimerAnnounce > 30000
                && (mBarView.isInhaling() || mBarView.isExhaling())) {
            long timeLeft = mSessionDuration / 1000;
            long hours = 0;
            long minutes = 0;
            long seconds = 0;
            if (timeLeft >= 3600) {
                hours = timeLeft / 3600;
                timeLeft -= hours * 3600;
            }
            if (timeLeft >= 60) {
                minutes = timeLeft / 60;
                timeLeft -= minutes * 60;
            }
            seconds = timeLeft;

            mContentView.setContentDescription(String.format("%s %d minutes %d seconds remain.",
                    mContentView.getContentDescription(), minutes, seconds));

            mLastTimerAnnounce = System.currentTimeMillis();
        }
        mContentView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        mContentView.sendAccessibilityEvent(AccessibilityEventCompat.TYPE_ANNOUNCEMENT);
    }

    /**
     * ControlledBreathingBarView sends events at various stages in its journey
     * up and down. These events are used to sync up audio and visual elements
     * of the activity with the state of the bar.
     */
    @Override
    public void OnControlledBreathingEvent(ControlledBreathingEvent event, long duration) {
        Timber.d("Inhaling: %s, Exhaling: %s, Resting: %s, Holding: %s", mBarView.isInhaling(),
                mBarView.isExhaling(), mBarView.isResting(), mBarView.isHolding());

        Random rand = new SecureRandom();
        long fadeOutDuration = duration - 400;
        switch (event) {
            case EXHALE_START:
                updateDurationDescriptions();
                mExhaleView.setText("Exhale");
                mExhaleView.setVisibility(View.VISIBLE);
                if (fadeOutDuration > 0) {
                    List<Animation> animations = ((AnimationSet) mExhaleMessageAnimation).getAnimations();
                    animations.get(0).setDuration(400);
                    animations.get(1).setDuration(fadeOutDuration);
                    mExhaleView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                    mExhaleView.startAnimation(mExhaleMessageAnimation);
                }
                startPrompt(EXHALE_AUDIO[rand.nextInt(EXHALE_AUDIO.length)]);
                updateAccessibilityState();
                break;
            case EXHALE_RESUME:
                updateDurationDescriptions();
                mExhaleView.setVisibility(View.VISIBLE);
                if (fadeOutDuration > 0) {
                    List<Animation> animations = ((AnimationSet) mExhaleMessageAnimation).getAnimations();
                    animations.get(0).setDuration(0);
                    animations.get(1).setDuration(duration);
                    mExhaleView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                    mExhaleView.startAnimation(mExhaleMessageAnimation);
                }
                break;
            case INHALE_START:
                updateDurationDescriptions();
                mInhaleView.setVisibility(View.VISIBLE);
                if (fadeOutDuration > 0) {
                    List<Animation> animations = ((AnimationSet) mInhaleMessageAnimation).getAnimations();
                    animations.get(0).setDuration(400);
                    animations.get(1).setDuration(fadeOutDuration);
                    mInhaleView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                    mInhaleView.startAnimation(mInhaleMessageAnimation);
                }
                startPrompt(INHALE_AUDIO[rand.nextInt(INHALE_AUDIO.length)]);
                updateAccessibilityState();
                break;
            case INHALE_RESUME:
                updateDurationDescriptions();
                mInhaleView.setVisibility(View.VISIBLE);
                if (fadeOutDuration > 0) {
                    List<Animation> animations = ((AnimationSet) mInhaleMessageAnimation).getAnimations();
                    animations.get(0).setDuration(0);
                    animations.get(1).setDuration(duration);
                    mInhaleView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                    mInhaleView.startAnimation(mInhaleMessageAnimation);
                }
                break;
            case EXHALE_HALF:
            case INHALE_HALF:
                startPrompt(MISC_AUDIO[rand.nextInt(MISC_AUDIO.length)]);
                break;
            case EXHALE_END:
                mExhaleView.setVisibility(View.INVISIBLE);
                mExhaleMessageAnimation.reset();
                break;
            case INHALE_END:
                mInhaleView.setVisibility(View.INVISIBLE);
                mInhaleMessageAnimation.reset();
                break;
            case HOLD_END:
                mHoldMessageAnimation.reset();
                mHoldView.setVisibility(View.INVISIBLE);
                break;
            case HOLD_START:
                mHoldView.setVisibility(View.VISIBLE);
                if (fadeOutDuration > 0) {
                    List<Animation> animations = ((AnimationSet) mHoldMessageAnimation).getAnimations();
                    animations.get(0).setDuration(400);
                    animations.get(1).setDuration(fadeOutDuration);
                    mHoldView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                    mHoldView.startAnimation(mHoldMessageAnimation);
                }
                updateAccessibilityState();
                break;
            case HOLD_RESUME:
                mHoldView.setVisibility(View.VISIBLE);
                if (fadeOutDuration > 0) {
                    List<Animation> animations = ((AnimationSet) mHoldMessageAnimation).getAnimations();
                    animations.get(0).setDuration(0);
                    animations.get(1).setDuration(duration);
                    mHoldView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                    mHoldView.startAnimation(mHoldMessageAnimation);
                }
                break;

            case REST_END:
                mRestMessageAnimation.reset();
                mRestView.setVisibility(View.INVISIBLE);
                break;
            case REST_START:
                mRestView.setVisibility(View.VISIBLE);
                if (fadeOutDuration > 0) {
                    List<Animation> animations = ((AnimationSet) mRestMessageAnimation).getAnimations();
                    animations.get(0).setDuration(400);
                    animations.get(1).setDuration(fadeOutDuration);
                    mRestView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                    mRestView.startAnimation(mRestMessageAnimation);
                }
                updateAccessibilityState();
                break;
            case REST_RESUME:
                mRestView.setVisibility(View.VISIBLE);
                if (fadeOutDuration > 0) {
                    List<Animation> animations = ((AnimationSet) mRestMessageAnimation).getAnimations();
                    animations.get(0).setDuration(0);
                    animations.get(1).setDuration(duration);
                    mRestView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                    mRestView.startAnimation(mRestMessageAnimation);
                }
                break;
            case PAUSED:
                mBackgroundView.pause();
                mInhaleView.clearAnimation();
                mExhaleView.clearAnimation();
                mRestView.clearAnimation();
                mHoldView.clearAnimation();
                Animation anim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
                anim.setDuration(300);
                anim.setFillAfter(true);
                mOverlayView.setVisibility(View.VISIBLE);
                mOverlayView.startAnimation(anim);
                break;
            case UNPAUSED:
                anim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
                anim.setDuration(300);
                anim.setFillAfter(true);
                mOverlayView.setVisibility(View.GONE);
                mOverlayView.startAnimation(anim);
                mBackgroundView.resume();
                break;

        }
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) {
            mStarted = state.getBoolean("started");
            String uriString = state.getString("music_file");
            if (uriString != null) {
                mMusicUri = Uri.parse(Uri.decode(uriString));
                mMusicPosition = state.getInt("music_position");
            }
            mSessionDuration = state.getLong("session");
            mFinished = state.getBoolean("finished");
            mPaused = state.getBoolean("paused");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.controlled_breathing_view, container);
    }

    @Override
    public void onPause() {
        super.onPause();

        pauseAudio();

        if (mSessionHandler != null) {
            mSessionHandler.removeCallbacks(mSessionTimer);
        }

        if (mStarting) {
            mStartTimer.cancel();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (mp == mMusicPlayer && mMusicPosition > 0) {
            mp.setOnSeekCompleteListener(this);
            mp.seekTo(mMusicPosition);
        } else {
            mp.start();
            mMusicStarting = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mStarted) {
            restart();
        } else {
            Timber.d("Inhaling: %s, Exhaling: %s, Resting: %s, Holding: %s", mBarView.isInhaling(),
                    mBarView.isExhaling(), mBarView.isResting(), mBarView.isHolding());
            mInhaleView.setVisibility(mBarView.isInhaling() ? View.VISIBLE : View.INVISIBLE);
            mHoldView.setVisibility(mBarView.isHolding() ? View.VISIBLE : View.INVISIBLE);
            mRestView.setVisibility(mBarView.isResting() ? View.VISIBLE : View.INVISIBLE);
            mExhaleView.setVisibility(mBarView.isExhaling() ? View.VISIBLE : View.INVISIBLE);

            if (mSessionDuration > 0) {
                mMessageView.setVisibility(View.VISIBLE);
                mMessageView.setText(DateUtils.formatElapsedTime(mSessionDuration / 1000));
                mSessionHandler.postDelayed(mSessionTimer, 100);
            }

            if (!mPaused) {
                startMusic();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("started", mStarted);

        if (mMusicUri != null) {
            outState.putString("music_file", mMusicUri.toString());
            if (mMusicPlayer != null) {
                try {
                    if (mMusicPlayer.isPlaying()) {
                        mMusicPosition = mMusicPlayer.getCurrentPosition();
                    }
                } catch (IllegalStateException ignored) {
                }
            }
        }

        outState.putInt("music_position", mMusicPosition);
        outState.putLong("session", mSessionDuration);
        outState.putBoolean("finished", mFinished);
        outState.putBoolean("paused", mPaused);
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        mMusicPosition = 0;
        mp.start();
        mMusicStarting = false;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBarView = (ControlledBreathingBarView) view.findViewById(R.id.bar);
        mHoldView = (TextView) view.findViewById(R.id.lbl_hold);
        mRestView = (TextView) view.findViewById(R.id.lbl_rest);
        mInhaleView = (TextView) view.findViewById(R.id.lbl_inhale);
        mMessageView = (TextView) view.findViewById(R.id.lbl_message);
        mExhaleView = (TextView) view.findViewById(R.id.lbl_exhale);
        mBackgroundView = (ControlledBreathingBackgroundView) view.findViewById(R.id.img_background);
        mContentView = (ViewGroup) view.findViewById(R.id.lay_breathing_text);
        mOverlayView = (ViewGroup) view.findViewById(R.id.lay_overlay);
        mIncreaseDurationButton = (ImageButton) view.findViewById(R.id.btn_add_time);
        mDecreaseDurationButton = (ImageButton) view.findViewById(R.id.btn_remove_time);
    }

    public void pauseSession() {
        mPaused = true;

        if (mSessionHandler != null) {
            mSessionHandler.removeCallbacks(mSessionTimer);
        }

        mBarView.pause();
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mContentView.setContentDescription("Session paused. Tap to continue.");
        mContentView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        mContentView.sendAccessibilityEvent(AccessibilityEventCompat.TYPE_ANNOUNCEMENT);

        onSessionPause();
    }

    private void restart() {
        mSessionDuration = mPrefs.getInt(getString(R.string.pref_session_duration), 0) * 60000;

        initSession();

        mStartTimer = new CountDownTimer(6000, 200) {

            private boolean mDeepBreathShown, mThreeShown, mTwoShown, mOneShown;

            @Override
            public void onFinish() {
                mDeepBreathShown = false;
                mThreeShown = false;
                mTwoShown = false;
                mOneShown = false;

                mExhaleView.setVisibility(View.INVISIBLE);
                mExhaleView.setText("");
                mMessageView.setText("");
                mMessageView.clearAnimation();
                mBarView.start();
                mExhaleView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                ((ControlledBreathingBackgroundView) getView().findViewById(R.id.img_background)).start();
                if (mSessionDuration > 0) {
                    mMessageView.setText(DateUtils.formatElapsedTime(mSessionDuration / 1000));
                    Animation fadeIn = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
                    fadeIn.setFillAfter(true);
                    fadeIn.setDuration(500);
                    mMessageView.startAnimation(fadeIn);
                    mSessionHandler.postDelayed(mSessionTimer, 1000);
                }
                mStarting = false;

                mStarted = true;
                mExhaleView.startAnimation(((AnimationSet) mExhaleMessageAnimation).getAnimations().get(1));
                onSessionStart();
            }

            @Override
            public void onTick(long millisUntilFinished) {
                StrokedTextView view = (StrokedTextView) mExhaleView;
                if (view == null) {
                    return;
                }

                if (millisUntilFinished <= 1000 && !mOneShown) {
                    mOneShown = true;
                    view.setText("1");
                    mContentView.setContentDescription("1");
                    mContentView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                    mContentView.sendAccessibilityEvent(AccessibilityEventCompat.TYPE_ANNOUNCEMENT);
                } else if (millisUntilFinished <= 2200 && !mTwoShown) {
                    mTwoShown = true;
                    view.setText("2");
                    mContentView.setContentDescription("2");
                    mContentView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                    mContentView.sendAccessibilityEvent(AccessibilityEventCompat.TYPE_ANNOUNCEMENT);
                } else if (millisUntilFinished <= 3400 && !mThreeShown) {
                    mThreeShown = true;
                    view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 60);
                    mContentView.setContentDescription("3");
                    view.setText("3");
                    mContentView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                    mContentView.sendAccessibilityEvent(AccessibilityEventCompat.TYPE_ANNOUNCEMENT);
                } else if (millisUntilFinished <= 4800 && !mDeepBreathShown) {
                    mDeepBreathShown = true;
                    view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 35);
                    mContentView.setContentDescription("Deep Breath");
                    view.setText("Deep breath!");
                    mContentView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                    mContentView.sendAccessibilityEvent(AccessibilityEventCompat.TYPE_ANNOUNCEMENT);
                }
            }
        };

        mStarted = false;
        mStarting = false;
        mExhaleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 60);
        mExhaleView.setText("Ready?");
        mMessageView.setText("Tap to start.");
        mContentView.setContentDescription("Ready? Tap to start.");
        mContentView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        mContentView.sendAccessibilityEvent(AccessibilityEventCompat.TYPE_ANNOUNCEMENT);
        mMessageView.clearAnimation();
    }

    private void resumeSession() {
        mPaused = false;
        mBarView.resume();
        if (mSessionDuration > 0) {
            mSessionHandler.postDelayed(mSessionTimer, 100);
        }

        mContentView.setContentDescription("Session resumed.");
        mContentView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        mContentView.sendAccessibilityEvent(AccessibilityEventCompat.TYPE_ANNOUNCEMENT);

        onSessionResume();
    }

    protected void onSessionFinish() {
        mContentView.setContentDescription("Session complete. Well done.");
        mContentView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        mContentView.sendAccessibilityEvent(AccessibilityEventCompat.TYPE_ANNOUNCEMENT);
    }

    protected void onSessionPause() {
        pauseAudio();
    }

    protected void onSessionResume() {
        startMusic();
    }

    protected void onSessionStart() {
        startMusic();
    }

    protected void onSessionStarting() {

    }

    private void finishSession() {
        mSessionDuration = 0;
        mBarView.stop();
        Animation fadeIn = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
        fadeIn.setFillAfter(true);
        fadeIn.setStartOffset(0);
        fadeIn.setDuration(2000);
        fadeIn.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                getView().findViewById(R.id.lbl_complete).setVisibility(View.VISIBLE);
                getView().findViewById(R.id.lbl_complete).sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }
        });
        getView().findViewById(R.id.lbl_complete).startAnimation(fadeIn);
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mStarted = false;
        mFinished = true;
        onSessionFinish();
    }

    private long getDuration(DurationType type) {
        return mPrefs.getLong(getString(type.getPrefKeyId()), type.getDefaultDuration());
    }

    private void initDurations(Bundle state) {
        if (state == null) {
            mSessionDuration = mPrefs.getInt(getString(R.string.pref_session_duration), 0) * 60000;
        }

        mInhaleMessageAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.inhale);
        mExhaleMessageAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.exhale);
        mHoldMessageAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.hold);
        mRestMessageAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.rest);

        setTextFadeDuration();

        Random rand = new SecureRandom();
        mSoundPhase = rand.nextInt(6);

        initSession();
    }

    private void initSession() {
        if (mSessionDuration > 0) {
            mSessionHandler = new Handler();
            mSessionTimer = () -> {
                if (mBarView.isPaused()) {
                    return;
                }

                mSessionDuration = mSessionDuration - 1000;
                if (!mFinishStarted && mSessionDuration <= 4000) {
                    mFinishStarted = true;
                    Animation fadeOut = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
                    fadeOut.setFillAfter(true);
                    fadeOut.setDuration(mSessionDuration);
                    getView().findViewById(R.id.lay_breathing).startAnimation(fadeOut);
                    mBackgroundView.stop();
                    mBackgroundView.startAnimation(fadeOut);
                }

                if (mSessionDuration <= 0) {
                    finishSession();
                    return;
                }

                ((TextView) getView().findViewById(R.id.lbl_message)).setText(DateUtils
                        .formatElapsedTime(mSessionDuration / 1000));
                mSessionHandler.postDelayed(mSessionTimer, 1000);
            };
        }
    }

    private boolean isMusicEnabled() {
        return !MusicCategory.NONE.name().equals(
                mPrefs.getString(getString(R.string.pref_breathing_music), MusicCategory.NONE.name()));
    }

    private boolean isPromptEnabled() {
        return mPrefs.getBoolean(getString(R.string.pref_breathing_prompts), true);
    }

    private void pauseAudio() {
        if (isMusicEnabled() && mMusicPlayer != null) {
            try {
                if (mMusicPlayer.isPlaying()) {
                    mMusicPosition = mMusicPlayer.getCurrentPosition();
                }
            } catch (IllegalStateException ignored) {
            }
        }

        stopMusic();
        stopPrompt();
    }

    private void setTextFadeDuration() {
        ((AnimationSet) mInhaleMessageAnimation).getAnimations().get(1)
                .setDuration(getDuration(DurationType.INHALE) - 400);
        ((AnimationSet) mExhaleMessageAnimation).getAnimations().get(1)
                .setDuration(getDuration(DurationType.EXHALE) - 400);
        ((AnimationSet) mHoldMessageAnimation).getAnimations().get(1)
                .setDuration(Math.max(0, getDuration(DurationType.HOLD) - 400));
        ((AnimationSet) mRestMessageAnimation).getAnimations().get(1)
                .setDuration(Math.max(0, getDuration(DurationType.REST) - 400));
    }

    private void showDurationToast(String text) {
        mDurationToast.setText(text);
        mDurationToast.show();
    }

    private void startMusic() {
        if (!isMusicEnabled() || mMusicStarting) {
            return;
        }

        if (mMusicPlayer != null) {
            try {
                if (mMusicPlayer.isPlaying()) {
                    return;
                }
            } catch (IllegalStateException ignored) {
            }
        }

        mMusicStarting = true;

        if (mMusicUri == null) {
            MusicCategory category = MusicCategory.valueOf(PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .getString(getActivity().getString(R.string.pref_breathing_music), MusicCategory.RANDOM.name()));
            Resources res = getResources();
            switch (category) {
                case AMBIENT_EVENINGS:
                case EVO_SOLUTION:
                case OCEAN_MIST:
                case WANING_MOMENTS:
                case WATERMARK:
                    long rId = category.getResourceId();
                    mMusicUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                            + res.getResourcePackageName((int) rId) + "/" + rId);
                    break;
                case RANDOM:
                    rId = MusicCategory.getRandomResource();
                    mMusicUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                            + res.getResourcePackageName((int) rId) + "/" + rId);
                    break;
                case PERSONAL_MUSIC:
                    if (!(getActivity() instanceof MusicProvider)) {
                        mMusicUri = null;
                        break;
                    }
                    mMusicUri = ((MusicProvider) getActivity()).getMusic();
                    break;
                case NONE:
                    mMusicUri = null;
                    break;
            }

        }

        if (mMusicUri == null) {
            mMusicStarting = false;
            return;
        }

        try {
            mMusicPlayer = new MediaPlayer();
            mMusicPlayer.setOnErrorListener((mp, what, extra) -> {
                mp.reset();
                mMusicStarting = false;
                return true;
            });
            mMusicPlayer.setOnCompletionListener(this);
            mMusicPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMusicPlayer.setVolume(0.7f, 0.7f);
            mMusicPlayer.setDataSource(getActivity(), mMusicUri);
            mMusicPlayer.setOnPreparedListener(this);
            mMusicPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startPrompt(int id) {
        if (!isPromptEnabled() || mPromptStarting) {
            return;
        }

        mPromptStarting = true;

        mSoundPhase = (mSoundPhase + 1) % 5;
        if (mSoundPhase != 0) {
            mPromptStarting = false;
            return;
        }

        if (mPlayer != null) {
            try {
                if (mPlayer.isPlaying()) {
                    mPromptStarting = false;
                    return;
                }
            } catch (IllegalStateException ignored) {
            }

            mPlayer.release();
        }

        try {
            mPlayer = new MediaPlayer();
            mPlayer.setOnErrorListener((mp, what, extra) -> {
                mp.reset();
                return true;
            });
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            AssetFileDescriptor afd = getResources().openRawResourceFd(id);
            mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mPlayer.setOnPreparedListener(this);
            mPlayer.prepareAsync();
        } catch (IOException e) {
            Timber.e(e);
        }
        mPromptStarting = false;
    }

    private void startSession() {
        onSessionStarting();
        mStarting = true;
        mExhaleView.setText("Alright.");
        mContentView.setContentDescription("Alright.");
        mContentView.sendAccessibilityEvent(AccessibilityEventCompat.TYPE_ANNOUNCEMENT);
        mContentView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        Animation anim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
        anim.setDuration(4000);
        anim.setFillAfter(true);
        mMessageView.startAnimation(anim);
        mStartTimer.start();
    }

    private void stopMusic() {
        // mMusicPosition = 0;
        // mMusicUri = null;
        if (mMusicPlayer != null) {
            try {
                mMusicPlayer.stop();
                mMusicPlayer.reset();
            } catch (IllegalStateException ignored) {
            }
            mMusicPlayer.release();
            mMusicPlayer = null;
        }
    }

    private void stopPrompt() {
        if (mPlayer != null) {
            try {
                mPlayer.stop();
                mPlayer.reset();
            } catch (IllegalStateException ignored) {
            }
            mPlayer.release();
            mPlayer = null;
        }
    }

    private void updateDurationDescriptions() {
        getView().findViewById(R.id.btn_add_time).setContentDescription(
                mBarView.isInhaling() ? "Increase inhale duration" : "Increase exhale duration");
        getView().findViewById(R.id.btn_remove_time).setContentDescription(
                mBarView.isInhaling() ? "Decrease inhale duration" : "Decrease exhale duration");
    }
}
