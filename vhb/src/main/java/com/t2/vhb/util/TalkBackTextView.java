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
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;

import com.t2.vhb.R;

public class TalkBackTextView extends android.support.v7.widget.AppCompatTextView {

	private String mTalkBackPrefix;
	private String mTalkBackSuffix;

	public TalkBackTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray arr = getContext().obtainStyledAttributes(attrs, R.styleable.TalkBackTextView);
		mTalkBackPrefix = arr.getString(R.styleable.TalkBackTextView_talkBackPrefix);
		mTalkBackSuffix = arr.getString(R.styleable.TalkBackTextView_talkBackSuffix);
		arr.recycle();
	}

	public TalkBackTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray arr = getContext().obtainStyledAttributes(attrs, R.styleable.TalkBackTextView);
		mTalkBackPrefix = arr.getString(R.styleable.TalkBackTextView_talkBackPrefix);
		mTalkBackSuffix = arr.getString(R.styleable.TalkBackTextView_talkBackSuffix);
		arr.recycle();
	}

	public TalkBackTextView(Context context) {
		super(context);
	}

	@Override
	public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
		if (TextUtils.isEmpty(getText())) {
			return false;
		}

		if (!TextUtils.isEmpty(mTalkBackPrefix)) {
			event.getText().add(mTalkBackPrefix);
		}
		event.getText().add(getText());
		if (!TextUtils.isEmpty(mTalkBackSuffix)) {
			event.getText().add(mTalkBackSuffix);
		}

		return true;
	}
}
