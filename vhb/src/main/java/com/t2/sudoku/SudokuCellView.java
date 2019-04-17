/*
 *
 * Created by Wes Turney on 12/17/13.
 *
 * SudokuLib
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
 * Government Agency Original Software Designation: SudokuLib001
 * Government Agency Original Software Title: SudokuLib
 * User Registration Requested. Please send email
 * with your contact information to: robert.a.kayl.civ@mail.mil
 * Government Agency Point of Contact for Original Software: robert.a.kayl.civ@mail.mil
 *
 */

package com.t2.sudoku;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import com.t2.sudoku.SudokuGridFragment.SudokuCell;

import java.util.Iterator;

public class SudokuCellView extends View {

    //private static final String TAG = "SudokuCellView";

    public static final Paint sTextPaint;
    public static final Paint sMarkPaint;

    private static final Paint sFillPaint;

    private SudokuCell mCell;
    private boolean mShowHighlight;
    private boolean mShowInvalid;

    private final Rect mMarkRect = new Rect();
    private final Rect mCellRect = new Rect();

    static {
        sTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sTextPaint.setColor(Color.BLACK);
        sTextPaint.setTextSize(40);
        sTextPaint.setStyle(Style.FILL);
        sTextPaint.setTextAlign(Align.CENTER);

        sMarkPaint = new Paint(sTextPaint);
        sMarkPaint.setTextSize(40);

        sFillPaint = new Paint();
        sFillPaint.setColor(0xFFa5bccd);
    }

    public SudokuCellView(Context context) {
        super(context);
        init();
    }

    public boolean isShowHighlight() {
        return mShowHighlight;
    }

    public boolean isShowInvalid() {
        return mShowInvalid;
    }

    public void setShowInvalid(boolean showInvalid) {
        mShowInvalid = showInvalid;
    }

    public void setShowHighlight(boolean showHighlight) {
        mShowHighlight = showHighlight;
    }

    public SudokuCellView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public SudokuCellView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public String getCellPosition() {
        final int row = ((int) (mCell.getPosition() / 9.0f)) + 65;
        final int col = (mCell.getPosition() % 9) + 1;

        return "" + ((char) row) + ";" + col;
    }

    private void init() {
        setEnabled(true);
        setFocusable(true);
        if (isInEditMode()) {
            mCell = new SudokuCell();
            for (int i = 1; i <= 9; i++) {
                mCell.getMarks().add(i);
            }
        }

        ViewCompat.setAccessibilityDelegate(this, new AccessibilityDelegateCompat() {
            @Override
            public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
                super.onPopulateAccessibilityEvent(host, event);
                if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
                        || event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
                    StringBuilder tts = new StringBuilder();

                    tts.append("Cell ").append(getCellPosition()).append(", ");
                    tts.append(mCell.isLocked() ? "Locked, " : "");

                    if (mCell.getMarks().size() == 1) {
                        tts.append(mCell.getMarks().first());
                    } else if (mCell.getMarks().size() > 1) {
                        tts.append("Marked, ");
                        tts.append(TextUtils.join(" ", mCell.getMarks()));
                    } else {
                        tts.append("Empty");
                    }

                    if (isShowInvalid()) {
                        tts.append(", Incorrect");
                    }

                    if (isSelected()) {
                        tts.append(", Selected");
                    } else if (!mCell.isLocked()) {
                        tts.append(", Double tap to select");
                    }

                    tts.append(".");

                    event.getText().add(tts.toString());
                } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
                    StringBuilder tts = new StringBuilder();
                    if (mCell.getMarks().size() == 1) {
                        tts.append(mCell.getMarks().first());
                    } else if (mCell.getMarks().size() > 1) {
                        tts.append(TextUtils.join(" ", mCell.getMarks()));
                    } else {
                        tts.append("Empty");
                    }
                    tts.append(".");
                    event.getText().add(tts.toString());
                    event.setBeforeText("");
                    event.setAddedCount(tts.length());
                } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                    event.getText().add("Cell selected");
                }
            }

            @Override
            public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
                super.onInitializeAccessibilityEvent(host, event);

            }

            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);

            }
        });
    }

    public SudokuCell getCell() {
        return mCell;
    }

    public void setCell(SudokuCell cell) {
        mCell = cell;
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        getDrawingRect(mCellRect);
        if (mCell != null) {
            if (mShowInvalid) {
                sFillPaint.setColor(0xFFbc8585);
                canvas.drawRect(mCellRect, sFillPaint);
            } else if (mCell.isLocked() && isFocused()) {
                sFillPaint.setColor(0xFFE0E0E0);
                canvas.drawRect(mCellRect, sFillPaint);
            } else if (!mCell.isLocked() && isSelected()) {
                canvas.drawRect(mCellRect, sFillPaint);
            } else if (!mCell.isLocked() && isFocused()) {
                sFillPaint.setColor(0xFFbcAAAA);
                canvas.drawRect(mCellRect, sFillPaint);
            } else if (mShowHighlight) {
                sFillPaint.setColor(0xFFa5bc6e);
                canvas.drawRect(mCellRect, sFillPaint);
            } else if (mCell.isLocked()) {
                sFillPaint.setColor(0xFFC0C0C0);
                canvas.drawRect(mCellRect, sFillPaint);
            }
        }
        sFillPaint.setColor(0xFFa5bccd);

        if (mCell != null && mCell.getMarks().size() == 1) {
            String value = mCell.getMarks().first() + "";
            canvas.drawText(value, mCellRect.centerX(), mCellRect.centerY() + (sTextPaint.measureText(value) / 1.6f),
                    sTextPaint);
        } else if (mCell != null && mCell.getMarks().size() > 1) {
            sMarkPaint.getTextBounds("9 9 9", 0, 5, mMarkRect);
            // mMarkRect.inset(0, -1);

            final Iterator<Integer> itr = mCell.getMarks().iterator();
            int index = 0;
            StringBuilder value = new StringBuilder(6);
            while (itr.hasNext()) {
                final int row = (int) Math.ceil(index / 3.0);
                if (index > 0 && index % 3 == 0) {
                    canvas.drawText(value.substring(0, value.length() - 1), mCellRect.centerX(),
                            (mCellRect.top + mMarkRect.height() * row) + (2 * row) + 1, sMarkPaint);
                    value = new StringBuilder(6);
                }
                value.append(itr.next()).append(" ");
                index++;
            }

            if (value.length() > 0) {
                final int row = (int) Math.ceil(index / 3.0);
                canvas.drawText(value.substring(0, value.length() - 1), mCellRect.centerX(),
                        (float) (mCellRect.top + mMarkRect.height() * row) + (2 * row) + 1, sMarkPaint);
            }

        }
    }
}
