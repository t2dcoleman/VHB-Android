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
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import com.t2.vhb.R;

/**
 * This class adds a stroke to the generic TextView allowing the text to stand
 * out better against the background (ie. in the AllApps button).
 */
public class StrokedTextView extends android.support.v7.widget.AppCompatTextView {
    private final Canvas mCanvas = new Canvas();
    private final Paint mPaint = new Paint();
    private Bitmap mCache;
    private boolean mUpdateCachedBitmap;
    private int mStrokeColor;
    private float mStrokeWidth;
    private int mTextColor;

    public StrokedTextView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public StrokedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public StrokedTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StrokedTextView, defStyle, 0);
        mStrokeColor = a.getColor(R.styleable.StrokedTextView_strokeColor, 0xFF000000);
        mStrokeWidth = a.getFloat(R.styleable.StrokedTextView_strokeWidth, 0.0f);
        mTextColor = a.getColor(R.styleable.StrokedTextView_strokeTextColor, 0xFFFFFFFF);
        a.recycle();
        mUpdateCachedBitmap = true;

        // Setup the text paint
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int before, int after) {
        super.onTextChanged(text, start, before, after);
        mUpdateCachedBitmap = true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            mUpdateCachedBitmap = true;
            mCache = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        } else {
            mCache = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mCache != null) {
            if (mUpdateCachedBitmap) {
                final int w = getMeasuredWidth();
                final int h = getMeasuredHeight();
                final String text = getText().toString();
                final Rect textBounds = new Rect();
                final Paint textPaint = getPaint();
                final int textWidth = (int) textPaint.measureText(text);
                textPaint.getTextBounds("x", 0, 1, textBounds);

                // Clear the old cached image
                mCanvas.setBitmap(mCache);
                mCanvas.drawColor(0, PorterDuff.Mode.CLEAR);

                // Draw the drawable
                final int drawableLeft = getPaddingLeft();
                final int drawableTop = getPaddingTop();
                final Drawable[] drawables = getCompoundDrawables();
                for (Drawable drawable : drawables) {
                    if (drawable != null) {
                        drawable.setBounds(drawableLeft, drawableTop,
                                drawableLeft + drawable.getIntrinsicWidth(),
                                drawableTop + drawable.getIntrinsicHeight());
                        drawable.draw(mCanvas);
                    }
                }

                final int left = w - getPaddingRight() - textWidth;
                final int bottom = (h + textBounds.height()) / 2;

                // Draw the outline of the text
                mPaint.setStrokeWidth(mStrokeWidth);
                mPaint.setColor(mStrokeColor);
                mPaint.setTextSize(getTextSize());
                mPaint.setTypeface(textPaint.getTypeface());
                mCanvas.drawText(text, left, bottom, mPaint);

                // Draw the text itself
                mPaint.setStrokeWidth(0);
                mPaint.setColor(mTextColor);
                mCanvas.drawText(text, left, bottom, mPaint);

                mUpdateCachedBitmap = false;
            }
            canvas.drawBitmap(mCache, 0, 0, mPaint);
        } else {
            super.onDraw(canvas);
        }
    }
}
