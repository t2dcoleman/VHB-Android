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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;

import timber.log.Timber;

public class ControlledBreathingBackgroundView extends View {

    // Represents the current left offset of the background image
    private float mLeftOffset;
    // Represents the current top offset of the background image
    private float mTopOffset;

    private Paint mForegroundPaint;
    private Paint mBackgroundPaint;

    // Represents the alpha level of the background image. The foreground image
    // is the inverse of this value
    private int mAlpha;

    // Represents the difference in dimensions between the foreground image and
    // the background image
    private float mWidthDiff;

    private BackgroundState mState;

    // The previous animation interpolation time. Used to restore the state of
    // the background in the event the activity gets consumed.
    private float mRestoredInterp;

    private OnBackgroundChangedListener mOnBackgroundChangedListener;

    public void setOnBackgroundChangedListener(OnBackgroundChangedListener onBackgroundChangedListener) {
        mOnBackgroundChangedListener = onBackgroundChangedListener;
    }

    public ControlledBreathingBackgroundView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public ControlledBreathingBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ControlledBreathingBackgroundView(Context context) {
        super(context);
        init();
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof BackgroundState) {
            super.onRestoreInstanceState(((BackgroundState) state).getSuperState());
            mState = ((BackgroundState) state);
            if (mState.mStarted && !mState.mPaused) {
                restoreAnimation();
            }

            if (mState.mCurrentBackground == null
                    || mState.mNextBackground == null
                    || mState.mCurrentBackground.isRecycled()
                    || mState.mNextBackground.isRecycled()) {
                Timber.d("New Image Requested");
                fireOnBackgroundChanged();
            }

        } else {
            super.onRestoreInstanceState(state);
        }
    }

    private void restoreAnimation() {
        // No need to restore state if the instance happened to get
        // destroyed at the beginning or end of the animation
        if (mState.mInterpTime < .0001 || mState.mInterpTime > .9999) {
            start();
            Timber.d("New animation started");
        } else {
            BackgroundScrollAnimation anim = new BackgroundScrollAnimation();
            anim.setInterpolator(new DecelerateInterpolator());
            anim.scaleCurrentDuration(1 - mState.mInterpTime);
            mRestoredInterp = mState.mInterpTime;
            evaluateDiffs();
            startAnimation(anim);
            Timber.d("old animation contiued");
        }
    }

    public void pause() {
        mState.mPaused = true;
        clearAnimation();
    }

    public void resume() {
        mState.mPaused = false;
        restoreAnimation();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable state = super.onSaveInstanceState();
        BackgroundState saveState = new BackgroundState(state);
        saveState.mInterpTime = mState.mInterpTime;
        saveState.mCurrentBackground = mState.mCurrentBackground;
        saveState.mNextBackground = mState.mNextBackground;
        saveState.mStarted = mState.mStarted;
        saveState.mPaused = mState.mPaused;
        return saveState;
    }

    private static final class BackgroundState extends BaseSavedState {

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<BackgroundState> CREATOR =
                new Parcelable.Creator<BackgroundState>() {
                    @Override
                    public BackgroundState createFromParcel(Parcel source) {
                        return new BackgroundState(source);
                    }

                    @Override
                    public BackgroundState[] newArray(int size) {
                        return new BackgroundState[size];
                    }
                };

        private Bitmap mCurrentBackground;
        private Bitmap mNextBackground;
        private boolean mStarted;
        private boolean mPaused;
        private float mInterpTime;

        public BackgroundState(Parcel in) {
            super(in);
            mInterpTime = in.readFloat();
            mCurrentBackground = in.readParcelable(Bitmap.class.getClassLoader());
            mNextBackground = in.readParcelable(Bitmap.class.getClassLoader());
            mStarted = in.readInt() == 1;
            mPaused = in.readInt() == 1;
        }

        public BackgroundState() {
            super(Parcel.obtain());
        }

        public BackgroundState(Parcelable parcelable) {
            super(parcelable);
        }

    }

    private void init() {
        mForegroundPaint = new Paint();
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setAlpha(0);
        mState = new BackgroundState();
    }

    /**
     * Called when a bitmap is finished async loading
     */
    public synchronized void queueBackground(Bitmap background) {
        if (background != null) {
            if (mState.mCurrentBackground == null || mState.mCurrentBackground.isRecycled()) {
                mState.mCurrentBackground = background;
                evaluateDiffs();
            } else {
                if (mState.mNextBackground == null
                        || mState.mNextBackground.isRecycled()) {
                    mState.mNextBackground = background;
                }
            }
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mState.mCurrentBackground != null) {
            evaluatePosition();
            evaluateDiffs();
            invalidate();
        }
    }

    /**
     * Determines the differences between the view dimensions and the dimensions
     * of the actual image. If the image is smaller than the view it is centered
     * and does not scroll.
     */
    private void evaluateDiffs() {
        if (mState.mCurrentBackground == null || mState.mCurrentBackground == null
                || mState.mCurrentBackground.isRecycled()) {
            return;
        }

        mWidthDiff = mState.mCurrentBackground.getWidth() - getMeasuredWidth();
        float mHeightDiff = mState.mCurrentBackground.getHeight() - getMeasuredHeight();
        if (mWidthDiff < 0) {
            mLeftOffset = mWidthDiff / 2.0f;
        }
        mTopOffset = mHeightDiff / 2.0f;
    }

    public void stop() {
        mState.mStarted = false;
        clearAnimation();
    }

    /**
     * Begin animating the backgrounds
     */
    public void start() {
        if (mState.mCurrentBackground == null) {
            return;
        }

        if (mState.mNextBackground == null) {
            fireOnBackgroundChanged();
        }
        mState.mStarted = true;
        evaluateDiffs();
        startAnimation(new BackgroundScrollAnimation());
    }

    private void fireOnBackgroundChanged() {
        if (mOnBackgroundChangedListener != null) {
            mOnBackgroundChangedListener.onBackgroundChanged();
        }
    }

    /**
     * Right now the images cross-fade. Fading only the foreground or background
     * is better on the processor but looks silly if either image doesn't fill
     * the entire screen
     */
    @Override
    protected void onDraw(Canvas canvas) {
        // Draw the foreground image
        if (mState.mCurrentBackground != null && mState.mCurrentBackground != null
                && !mState.mCurrentBackground.isRecycled()) {
            mForegroundPaint.setAlpha(255 - mAlpha);
            canvas.drawBitmap(mState.mCurrentBackground, -mLeftOffset, -mTopOffset, mForegroundPaint);
        }

        // Draw the background image if we are currently fading between them
        if (mState.mNextBackground != null && mState.mNextBackground != null
                && !mState.mNextBackground.isRecycled()
                && mAlpha > 0) {
            float widthDiff = mState.mNextBackground.getWidth() - getMeasuredWidth();
            float topDiff = mState.mNextBackground.getHeight() - getMeasuredHeight();
            float leftOffset = 0;
            float topOffset = topDiff / 2.0f;
            if (widthDiff < 0) {
                leftOffset = widthDiff / 2.0f;
            }
            canvas.drawBitmap(mState.mNextBackground, -leftOffset, -topOffset, mBackgroundPaint);
        }
    }

    /**
     * Evaluate the offsets to display the foreground image at for the current
     * animation state.
     */
    private void evaluatePosition() {
        mAlpha = (int) (((mState.mInterpTime - 0.9) * 10) * 255.0f);
        if (mAlpha < 0) {
            mAlpha = 0;
        }
        mBackgroundPaint.setAlpha(mAlpha);

        if (mWidthDiff > 0) {
            mLeftOffset = (mWidthDiff * mState.mInterpTime);
        }
    }

    private class BackgroundScrollAnimation extends Animation implements AnimationListener {

        public BackgroundScrollAnimation() {
            setDuration(45000);
            setAnimationListener(this);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            if (!mState.mStarted || mState.mPaused) {
                return;
            }

            if (mRestoredInterp >= 0) {
                mState.mInterpTime = mRestoredInterp + (interpolatedTime * (1.0f - mRestoredInterp));
            } else {
                mState.mInterpTime = interpolatedTime;
            }

            evaluatePosition();
            postInvalidate();
        }

        /**
         * Swap the foreground and background now that fading between the two is
         * complete. Recycle the old foreground image and request a new one.
         * Rinse and repeat
         */
        @Override
        public void onAnimationEnd(Animation animation) {
            if (!mState.mStarted || mState.mPaused) {
                return;
            }

            synchronized (mState) {
                Bitmap temp = mState.mCurrentBackground;
                mState.mCurrentBackground = mState.mNextBackground;
                mState.mNextBackground = null;
                if(temp != null) temp.recycle();
            }

            mLeftOffset = 0;
            mTopOffset = 0;
            mAlpha = 0;
            mBackgroundPaint.setAlpha(0);
            mForegroundPaint.setAlpha(255);
            evaluateDiffs();
            reset();
            fireOnBackgroundChanged();

            if (mRestoredInterp >= 0) {
                startAnimation(new BackgroundScrollAnimation());
                mRestoredInterp = -1.0f;
            } else {
                startAnimation(this);
            }

        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }

    }

    /**
     * Notifies the listener that the foreground and background have swapped.
     * This is currently used to fire off an async load for a new background
     * image
     */
    public interface OnBackgroundChangedListener {
        void onBackgroundChanged();
    }

}
