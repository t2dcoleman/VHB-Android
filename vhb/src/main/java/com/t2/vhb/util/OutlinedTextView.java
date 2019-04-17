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

package com.t2.vhb.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint.Style;
import android.util.AttributeSet;

import com.t2.vhb.R;

public class OutlinedTextView extends android.support.v7.widget.AppCompatTextView {
    private final ColorStateList mOutlineColor;
    private int mOutlineSize = 6;

    public OutlinedTextView(Context context) {
        super(context);
        mOutlineColor = ColorStateList.valueOf(getInverseColor(getResources().getColor(android.R.color.black)));
        mOutlineSize = 2;
        init();
    }

    public OutlinedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray arr = getContext().obtainStyledAttributes(attrs, R.styleable.OutlinedTextView);
        mOutlineColor = arr.getColorStateList(R.styleable.OutlinedTextView_outlineColor);
        mOutlineSize = arr.getInt(R.styleable.OutlinedTextView_outlineSize, 2);
        arr.recycle();
        init();
    }

    public OutlinedTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray arr = getContext().obtainStyledAttributes(attrs, R.styleable.OutlinedTextView);
        mOutlineColor = arr.getColorStateList(R.styleable.OutlinedTextView_outlineColor);
        mOutlineSize = arr.getInt(R.styleable.OutlinedTextView_outlineSize, 2);
        arr.recycle();
        init();
    }

    private int getInverseColor(int color) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        int alpha = Color.alpha(color);
        return Color.argb(alpha, 255 - red, 255 - green, 255 - blue);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void init() {
        setPadding(getPaddingLeft() + mOutlineSize, getPaddingTop(), getPaddingRight() + mOutlineSize,
                getPaddingBottom());
    }

    @Override
    public void draw(Canvas canvas) {
        if (isInEditMode()) {
            super.draw(canvas);
        } else {
            getPaint().setColor(mOutlineColor.getColorForState(getDrawableState(), 0));
            getPaint().setStyle(Style.STROKE);
            getPaint().setStrokeWidth(mOutlineSize);
            canvas.save();
            canvas.translate(getCompoundPaddingLeft() + mOutlineSize, getCompoundPaddingTop());
            getLayout().draw(canvas);
            canvas.restore();
            getPaint().setColor(0xFFFFFFFF);
            getPaint().setStyle(Style.FILL);
            canvas.save();
            canvas.translate(mOutlineSize, 0);
            super.draw(canvas);
            canvas.restore();
        }

    }
}
