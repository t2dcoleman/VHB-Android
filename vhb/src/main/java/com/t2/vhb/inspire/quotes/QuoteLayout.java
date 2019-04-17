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
package com.t2.vhb.inspire.quotes;

import android.content.Context;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.t2.vhb.R;

/**
 * @author wes
 * 
 */
public class QuoteLayout extends RelativeLayout {

	private String mQuote;
	private boolean mCleanSplit;
	private boolean mPreSplit;

	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public QuoteLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	/**
	 * @param context
	 * @param attrs
	 */
	public QuoteLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	/**
	 * @param context
	 */
	public QuoteLayout(Context context) {
		super(context);
		init();
	}

	public void setQuote(String quote) {
		mQuote = quote;
		mCleanSplit = false;
		updateQuote();
	}

	private void init() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.RelativeLayout#onMeasure(int, int)
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		if (mQuote != null) {
			updateQuote();
		}

	}

	private void updateQuote() {
		if (getMeasuredWidth() == 0) {
			return;
		}

		TextView firstLineView = (TextView) findViewById(R.id.lbl_quote_line_one);
		TextView bodyView = (TextView) findViewById(R.id.lbl_quote_body);

		mCleanSplit = false;
		mPreSplit = false;
		String firstLine = TextUtils.ellipsize(mQuote, firstLineView.getPaint(), firstLineView.getMeasuredWidth(), TruncateAt.END,
				false,
				(start, end) -> {
					if (mQuote.charAt(start) == ' ' || mQuote.charAt(start) == '\n') {
						mCleanSplit = true;
					}

					int wordStart = mQuote.lastIndexOf(' ', start);
					if (wordStart == -1) {
						wordStart = 0;
					}

					String split = mQuote;
					if (wordStart > 0) {
						split = split.substring(0, wordStart);
					}
					int lineIndex = split.indexOf('\n');
					if (lineIndex > 0) {
						wordStart = lineIndex;
						mPreSplit = true;
					}

					TextView bodyView1 = (TextView) findViewById(R.id.lbl_quote_body);
					bodyView1.setVisibility(View.VISIBLE);
					bodyView1.setText(mQuote.substring(wordStart).trim());
				}).toString().trim();
		if (firstLine.endsWith("\u2026") || firstLine.endsWith("...")) {
			if (mCleanSplit) {
				if (firstLine.endsWith("\u2026")) {
					firstLine = firstLine.substring(0, firstLine.length() - 1);
				} else {
					firstLine = firstLine.substring(0, firstLine.length() - 3);
				}
			} else {
				int wordEnd = firstLine.lastIndexOf(' ');
				if (wordEnd != -1) {
					firstLine = firstLine.substring(0, wordEnd);
				}
			}
		} else if (!mPreSplit) {
			bodyView.setVisibility(View.GONE);
		}

		if (mPreSplit) {
			firstLine = firstLine.substring(0, firstLine.indexOf('\n'));
		}

		firstLineView.setText(firstLine);
	}
}
