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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;

import com.t2.controlledbreathing.ControlledBreathingBarView.OnControlledBreathingEventListener.ControlledBreathingEvent;
import com.t2.vhb.R;

import java.util.EnumMap;

public class ControlledBreathingBarView extends View implements OnSharedPreferenceChangeListener {

    public interface OnControlledBreathingEventListener {
        enum ControlledBreathingEvent {
            PAUSED,
            UNPAUSED,
            INHALE_START,
            INHALE_END,
            INHALE_HALF,
            EXHALE_START,
            EXHALE_HALF,
            EXHALE_END,
            HOLD_START,
            HOLD_END,
            REST_START,
            REST_END,
            INHALE_RESUME,
            EXHALE_RESUME,
            HOLD_RESUME,
            REST_RESUME
        }

        void OnControlledBreathingEvent(ControlledBreathingEvent event, long duration);
    }

    private static final class BarState extends BaseSavedState {

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<BarState> CREATOR =
                new Parcelable.Creator<BarState>() {
                    @Override
                    public BarState createFromParcel(Parcel source) {
                        return new BarState(source);
                    }

                    @Override
                    public BarState[] newArray(int size) {
                        return new BarState[size];
                    }
                };

        private float mInterpTime;
        private boolean mInhaling;
        private boolean mExhaling;
        private boolean mHolding;
        private boolean mResting;
        private boolean mStarted;
        private boolean mCanceled;
        private boolean mPaused;

        public BarState() {
            super(Parcel.obtain());
        }

        public BarState(Parcel in) {
            super(in);
            mInterpTime = in.readFloat();
            mInhaling = in.readInt() == 1;
            mHolding = in.readInt() == 1;
            mResting = in.readInt() == 1;
            mStarted = in.readInt() == 1;
            mCanceled = in.readInt() == 1;
            mPaused = in.readInt() == 1;
            mExhaling = in.readInt() == 1;
            in.recycle();
        }

        public BarState(Parcelable parcelable) {
            super(parcelable);
        }

    }

    private class ExhaleAnimation extends Animation implements AnimationListener {

        private boolean mMidpointFired;

        @Override
        public void initialize(int width, int height, int parentWidth, int parentHeight) {
            super.initialize(width, height, parentWidth, parentHeight);
            setAnimationListener(this);
            setInterpolator(new AccelerateDecelerateInterpolator());
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            reset();

            if (mState.mCanceled || mState.mPaused) {
                return;
            }

            if (mRestoredInterp >= 0) {
                mRestoredInterp = -1;
            }

            mState.mExhaling = false;
            fireEvent(ControlledBreathingEvent.EXHALE_END, 0);
            if (mRestAnimation.getDuration() > 0) {
                startAnimation(mRestAnimation);
            } else {
                startAnimation(mInhaleAnimation);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
            if (mState.mPaused) {
                return;
            }

            mState.mInhaling = false;
            mState.mExhaling = true;
            mState.mHolding = false;
            mState.mResting = false;

            if (mRestoredInterp >= 0) {
                fireEvent(ControlledBreathingEvent.EXHALE_RESUME, getDuration());
            } else {
                fireEvent(ControlledBreathingEvent.EXHALE_START, getDuration());
            }
        }

        @Override
        public void reset() {
            super.reset();
            mMidpointFired = false;
        }

        @Override
        public boolean willChangeBounds() {
            return true;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            if (mState.mPaused) {
                return;
            }

            if (mRestoredInterp >= 0) {
                mState.mInterpTime = mRestoredInterp + (interpolatedTime * (1.0f - mRestoredInterp));
            } else {
                mState.mInterpTime = interpolatedTime;
            }

            if (!mMidpointFired && mState.mInterpTime >= 0.5f) {
                fireEvent(ControlledBreathingEvent.EXHALE_HALF, (long) (getDuration() / 2.0f));
                mMidpointFired = true;
            }

            calculateBarHeight();
        }

    }

    private class HoldAnimation extends Animation implements AnimationListener {

        @Override
        public void initialize(int width, int height, int parentWidth, int parentHeight) {
            super.initialize(width, height, parentWidth, parentHeight);
            setAnimationListener(this);
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            reset();

            if (mState.mCanceled || mState.mPaused) {
                return;
            }

            if (mRestoredInterp >= 0) {
                mRestoredInterp = -1;
            }

            mState.mHolding = false;
            fireEvent(ControlledBreathingEvent.HOLD_END, 0);
            startAnimation(mExhaleAnimation);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
            if (mState.mPaused) {
                return;
            }

            mState.mInhaling = false;
            mState.mExhaling = false;
            mState.mHolding = true;
            mState.mResting = false;

            if (mRestoredInterp >= 0) {
                fireEvent(ControlledBreathingEvent.HOLD_RESUME, getDuration());
            } else {
                fireEvent(ControlledBreathingEvent.HOLD_START, getDuration());
            }

        }

        @Override
        public void reset() {
            super.reset();
        }

        @Override
        public boolean willChangeBounds() {
            return true;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            if (mState.mPaused) {
                return;
            }

            if (mRestoredInterp >= 0) {
                mState.mInterpTime = mRestoredInterp + (interpolatedTime * (1.0f - mRestoredInterp));
            } else {
                mState.mInterpTime = interpolatedTime;
            }

            calculateBarHeight();
        }
    }

    private class InhaleAnimation extends Animation implements AnimationListener {

        private boolean mMidpointFired;

        @Override
        public void initialize(int width, int height, int parentWidth, int parentHeight) {
            super.initialize(width, height, parentWidth, parentHeight);
            setAnimationListener(this);
            setInterpolator(new AccelerateDecelerateInterpolator());

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            reset();

            if (mState.mCanceled || mState.mPaused) {
                return;
            }

            if (mRestoredInterp >= 0) {
                mRestoredInterp = -1;
            }

            mState.mInhaling = false;

            fireEvent(ControlledBreathingEvent.INHALE_END, 0);
            if (mHoldAnimation.getDuration() > 0) {
                startAnimation(mHoldAnimation);
            } else {
                startAnimation(mExhaleAnimation);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
            if (mState.mPaused) {
                return;
            }

            mState.mInhaling = true;
            mState.mExhaling = false;
            mState.mHolding = false;
            mState.mResting = false;

            if (mRestoredInterp >= 0) {
                fireEvent(ControlledBreathingEvent.INHALE_RESUME, getDuration());
            } else {
                fireEvent(ControlledBreathingEvent.INHALE_START, getDuration());
            }
        }

        @Override
        public void reset() {
            super.reset();
            mMidpointFired = false;
        }

        @Override
        public boolean willChangeBounds() {
            return true;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            if (mState.mPaused) {
                return;
            }

            if (mRestoredInterp >= 0) {
                mState.mInterpTime = mRestoredInterp + (interpolatedTime * (1.0f - mRestoredInterp));
            } else {
                mState.mInterpTime = interpolatedTime;
            }

            if (!mMidpointFired && mState.mInterpTime >= 0.5f) {
                fireEvent(ControlledBreathingEvent.INHALE_HALF, (long) (getDuration() / 2.0f));
                mMidpointFired = true;
            }

            calculateBarHeight();

        }
    }

    private class RestAnimation extends Animation implements AnimationListener {

        @Override
        public void initialize(int width, int height, int parentWidth, int parentHeight) {
            super.initialize(width, height, parentWidth, parentHeight);
            setAnimationListener(this);
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            reset();

            if (mState.mCanceled || mState.mPaused) {
                return;
            }

            if (mRestoredInterp >= 0) {
                mRestoredInterp = -1;
            }

            mState.mResting = false;
            fireEvent(ControlledBreathingEvent.REST_END, 0);
            startAnimation(mInhaleAnimation);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
            if (mState.mPaused) {
                return;
            }

            mState.mInhaling = false;
            mState.mExhaling = false;
            mState.mHolding = false;
            mState.mResting = true;

            if (mRestoredInterp >= 0) {
                fireEvent(ControlledBreathingEvent.REST_RESUME, getDuration());
            } else {
                fireEvent(ControlledBreathingEvent.REST_START, getDuration());
            }

        }

        @Override
        public void reset() {
            super.reset();
        }

        @Override
        public boolean willChangeBounds() {
            return true;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            if (mState.mPaused) {
                return;
            }

            if (mRestoredInterp >= 0) {
                mState.mInterpTime = mRestoredInterp + (interpolatedTime * (1.0f - mRestoredInterp));
            } else {
                mState.mInterpTime = interpolatedTime;
            }

            calculateBarHeight();
        }
    }

    private static final int BORDER_HEIGHT = 12;

    private static final int SEGMENT_HEIGHT = 1;
    // Represents the maximum possible height of the bar. This is set when
    // the view is first laid out.
    private int mMaxBarHeight;
    // Represents the current height of the breath bar
    private int mBarHeight;
    private int mPrevBarHeight;

    // Represents the amount of space required between each one-second segment
    // line to fill the max bar height
    private float mInhaleSegmentHeight;

    private float mExhaleSegmentHeight;

    private InhaleAnimation mInhaleAnimation;
    private ExhaleAnimation mExhaleAnimation;

    private HoldAnimation mHoldAnimation;

    private RestAnimation mRestAnimation;

    // Attribute assigned via xml. This determines if the bar fills horizontally
    // or vertically.
    private boolean mLandscape;

    // Paint used to draw the bar itself
    private Paint mFillPaint;

    // Paint used to draw segment lines while inhaling
    private Paint mInhaleLinePaint;

    private Paint mInhaleInnerLinePaint;

    // Paint used to draw segment lines while exhaling
    private Paint mExhaleLinePaint;

    // Represents the current state of the bar. Used to restore state.
    private BarState mState;

    private final RectF mBarRect = new RectF();

    // The previous animation interpolation time. Used to restore the state of
    // the breathing bar in the event the activity gets consumed.
    private float mRestoredInterp = -1;

    private OnControlledBreathingEventListener mListener;

    private RectF mSegmentRect;

    private int mHeight;

    private int mWidth;

    private EnumMap<DurationType, Animation> mDurationAnimations;

    public ControlledBreathingBarView(Context context) {
        super(context);
        init();
    }

    public ControlledBreathingBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ControlledBreathingBarView);
        mLandscape = a.getBoolean(R.styleable.ControlledBreathingBarView_landscape, false);
        a.recycle();
        init();
    }

    public ControlledBreathingBarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ControlledBreathingBarView, defStyle, 0);
        mLandscape = a.getBoolean(R.styleable.ControlledBreathingBarView_landscape, false);
        a.recycle();
        init();
    }

    public boolean isPaused() {
        return mState.mPaused;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof BarState) {
            super.onRestoreInstanceState(((BarState) state).getSuperState());
            mState = ((BarState) state);

            if (mState.mStarted) {
                restoreAnimation();
            }

        } else {
            super.onRestoreInstanceState(state);
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable state = super.onSaveInstanceState();
        BarState saveState = new BarState(state);
        saveState.mInterpTime = mState.mInterpTime;
        saveState.mInhaling = mState.mInhaling;
        saveState.mHolding = mState.mHolding;
        saveState.mResting = mState.mResting;
        saveState.mStarted = mState.mStarted;
        saveState.mCanceled = mState.mCanceled;
        saveState.mPaused = mState.mPaused;
        saveState.mExhaling = mState.mExhaling;
        return saveState;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Animation anim;
        String typeKey;
        for (DurationType type : mDurationAnimations.keySet()) {
            typeKey = getContext().getString(type.getPrefKeyId());
            if (key.equals(typeKey)) {
                anim = mDurationAnimations.get(type);
                if (anim != null) {
                    anim.setDuration(sharedPreferences.getLong(typeKey, type.getDefaultDuration()));
                }
                if (type == DurationType.EXHALE) {
                    float secs = mExhaleAnimation.getDuration() / 1000.0f;
                    mExhaleSegmentHeight = mMaxBarHeight / secs;
                } else if (type == DurationType.INHALE) {
                    float secs = mInhaleAnimation.getDuration() / 1000.0f;
                    mInhaleSegmentHeight = mMaxBarHeight / secs;
                }
            }
        }
    }

    public void pause() {
        mState.mPaused = true;
        clearAnimation();
        fireEvent(ControlledBreathingEvent.PAUSED, 0);
    }

    public void resume() {
        mState.mPaused = false;
        restoreAnimation();
        fireEvent(ControlledBreathingEvent.UNPAUSED, 0);
    }

    public void setOnControlledBreathingEventListener(OnControlledBreathingEventListener listener) {
        mListener = listener;
    }

    public void start() {
        mState.mStarted = true;
        mState.mCanceled = false;
        startAnimation(mExhaleAnimation);
    }

    public void stop() {
        mState.mStarted = false;
        mState.mCanceled = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        super.onDraw(canvas);
        canvas.restore();

        // Rotate the canvas if we are drawing a horizontal bar
        canvas.save();
        if (mLandscape) {
            canvas.translate(mMaxBarHeight + BORDER_HEIGHT, 0);
            canvas.rotate(90.0f);
        }

        canvas.save();
        canvas.translate(6, 6);

        if (isInEditMode()) {
            drawExampleBar(canvas);
            canvas.drawText(mMaxBarHeight + "", 5, 50, mExhaleLinePaint);
            canvas.drawText(mHeight + "", 5, 100, mExhaleLinePaint);
            canvas.drawText(mWidth + "", 5, 150, mExhaleLinePaint);
            return;
        }

        evaluateSegmentLineAlpha();

        canvas.save();
        canvas.translate(0, mMaxBarHeight - mBarHeight);
        mBarRect.set(0, 0, mWidth, mBarHeight);
        canvas.drawRect(mBarRect, mFillPaint);
        canvas.restore();

        drawSegmentLines(canvas);

        canvas.restore();
        canvas.restore();

    }

    public boolean isInhaling() {
        return mState.mInhaling;
    }

    public boolean isExhaling() {
        return mState.mExhaling;
    }

    public boolean isResting() {
        return mState.mResting;
    }

    public boolean isHolding() {
        return mState.mHolding;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (mMaxBarHeight == 0) {
            mMaxBarHeight = (mLandscape ? getMeasuredWidth() : getMeasuredHeight()) - BORDER_HEIGHT;
            mBarHeight = mMaxBarHeight;
            mHeight = (mLandscape ? getMeasuredWidth() : getMeasuredHeight());
            mWidth = (mLandscape ? getMeasuredHeight() : getMeasuredWidth()) - BORDER_HEIGHT;
            mSegmentRect = new RectF(5, -SEGMENT_HEIGHT, mWidth - 5, SEGMENT_HEIGHT);
        }

        if (!isInEditMode()) {
            float secs = mInhaleAnimation.getDuration() / 1000.0f;
            mInhaleSegmentHeight = mMaxBarHeight / secs;
            secs = mExhaleAnimation.getDuration() / 1000.0f;
            mExhaleSegmentHeight = mMaxBarHeight / secs;
            calculateBarHeight();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void calculateBarHeight() {
        if (mState.mResting) {
            mBarHeight = 5;
        } else if (mState.mHolding) {
            mBarHeight = mMaxBarHeight;
        } else if (mState.mInhaling) {
            mBarHeight = (int) (mMaxBarHeight * mState.mInterpTime) + (int) (5 * (1 - mState.mInterpTime));
        } else if (mState.mExhaling) {
            mBarHeight = (int) (mMaxBarHeight * (1 - mState.mInterpTime)) + (int) (5 * mState.mInterpTime);
        }

        if (mPrevBarHeight != mBarHeight) {
            postInvalidate();
        }
    }

    private void drawExampleBar(Canvas canvas) {
        canvas.save();
        canvas.translate(0, mMaxBarHeight - mBarHeight);
        canvas.drawRect(new RectF(0, 0, mWidth, mBarHeight), mFillPaint);
        canvas.restore();
        final float segmentHeight = mMaxBarHeight / 7.2f;
        canvas.save();
        canvas.clipRect(0, mMaxBarHeight - mBarHeight, mWidth, mMaxBarHeight, Region.Op.REPLACE);
        canvas.translate(0, mMaxBarHeight);
        for (int i = 1; i < Math.ceil(7.2); i++) {
            canvas.translate(0, -segmentHeight);
            canvas.drawRoundRect(mSegmentRect, 5f, 5f, mExhaleLinePaint);
        }

        canvas.restore();
    }

    private void drawSegmentLines(Canvas canvas) {
        canvas.save();
        canvas.clipRect(0, mMaxBarHeight - mBarHeight, mWidth, mMaxBarHeight);
        canvas.translate(0, mMaxBarHeight);
        for (int i = 1; i < Math.ceil(mExhaleAnimation.getDuration() / 1000.0f); i++) {
            canvas.translate(0, -mExhaleSegmentHeight);
            canvas.drawRoundRect(mSegmentRect, 2f, 2f, mExhaleLinePaint);
        }
        canvas.restore();

        canvas.save();
        canvas.clipRect(0, 0, mWidth, mMaxBarHeight - mBarHeight);
        canvas.translate(0, mMaxBarHeight);
        for (int i = 1; i < Math.ceil(mInhaleAnimation.getDuration() / 1000.0f); i++) {
            canvas.translate(0, -mInhaleSegmentHeight);
            canvas.drawRoundRect(mSegmentRect, 2f, 2f, mInhaleLinePaint);
        }
        canvas.restore();
    }

    private void evaluateSegmentLineAlpha() {
        float ratio = (mState.mInhaling ? mMaxBarHeight - mBarHeight : mBarHeight) / (0.1f * mMaxBarHeight);
        if (ratio > 1) {
            ratio = 0;
        } else {
            ratio = Math.abs(1 - ratio) * 255;
        }

        if (mState.mInhaling) {
            mExhaleLinePaint.setAlpha((int) ratio);
            mInhaleLinePaint.setAlpha(255);
            mInhaleInnerLinePaint.setAlpha(255);
        } else {
            mInhaleLinePaint.setAlpha((int) ratio);
            mInhaleInnerLinePaint.setAlpha((int) ratio);
            mExhaleLinePaint.setAlpha(255);
        }
    }

    private void fireEvent(ControlledBreathingEvent event, long duration) {
        if (mListener != null) {
            mListener.OnControlledBreathingEvent(event, duration);
        }
    }

    private void init() {
        mFillPaint = new Paint();
        mFillPaint.setColor(0xAA000000);

        mInhaleLinePaint = new Paint();
        mInhaleLinePaint.setColor(0xAAF8F8F8);
        mInhaleLinePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mInhaleInnerLinePaint = new Paint();
        mInhaleInnerLinePaint.setColor(0xAAF8F8F8);
        mInhaleInnerLinePaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        mExhaleLinePaint = new Paint();
        mExhaleLinePaint.setColor(0xAAF8F8F8);
        mExhaleLinePaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        mState = new BarState();

        if (isInEditMode()) {
            return;
        }

        mInhaleAnimation = new InhaleAnimation();
        mExhaleAnimation = new ExhaleAnimation();
        mHoldAnimation = new HoldAnimation();
        mRestAnimation = new RestAnimation();

        mDurationAnimations = new EnumMap<>(DurationType.class);
        mDurationAnimations.put(DurationType.EXHALE, mExhaleAnimation);
        mDurationAnimations.put(DurationType.INHALE, mInhaleAnimation);
        mDurationAnimations.put(DurationType.REST, mRestAnimation);
        mDurationAnimations.put(DurationType.HOLD, mHoldAnimation);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        Animation anim;
        for (DurationType type : mDurationAnimations.keySet()) {
            anim = mDurationAnimations.get(type);
            if (anim != null) {
                anim.setDuration(prefs.getLong(getContext().getString(type.getPrefKeyId()), type.getDefaultDuration()));
            }
        }

        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    private void restoreAnimation() {
        mRestoredInterp = mState.mInterpTime;
        Animation mRestoredAnimation;
        if (mState.mResting) {
            mRestoredAnimation = new RestAnimation();
            mRestoredAnimation.setDuration(mRestAnimation.getDuration());
        } else if (mState.mHolding) {
            mRestoredAnimation = new HoldAnimation();
            mRestoredAnimation.setDuration(mHoldAnimation.getDuration());
        } else if (mState.mInhaling) {
            mRestoredAnimation = new InhaleAnimation();
            mRestoredAnimation.setDuration(mInhaleAnimation.getDuration());
        } else {
            mRestoredAnimation = new ExhaleAnimation();
            mRestoredAnimation.setDuration(mExhaleAnimation.getDuration());
        }
        mRestoredAnimation.setInterpolator(new DecelerateInterpolator());
        mRestoredAnimation.scaleCurrentDuration(1 - mState.mInterpTime);
        startAnimation(mRestoredAnimation);
    }

}
