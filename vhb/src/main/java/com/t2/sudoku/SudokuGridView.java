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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.LinearLayout;

import com.t2.sudoku.SudokuGridFragment.SudokuCell;

import java.util.List;

import timber.log.Timber;

public class SudokuGridView extends LinearLayout implements OnClickListener, OnFocusChangeListener {

    private Paint mGridPaint;
    private Bitmap mGrid;

    private int mPrevWidth;
    private int mPrevHeight;

    private final Rect mCellRect = new Rect();
    private final Rect mMarkRect = new Rect();

    private OnSudokuCellSelectedListener mOnSudokuCellSelectedListener;

    public void setOnSudokuCellSelectedListener(OnSudokuCellSelectedListener listener) {
        mOnSudokuCellSelectedListener = listener;
    }

    public interface OnSudokuCellSelectedListener {
        void onSudokuCellSelected(SudokuCellView v);
    }

    public SudokuGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SudokuGridView(Context context) {
        super(context);
        init();
    }

    @Override
    public void onClick(View v) {
        SudokuCellView scv = (SudokuCellView) v;
        if (mOnSudokuCellSelectedListener != null && !scv.getCell().isLocked()) {
            mOnSudokuCellSelectedListener.onSudokuCellSelected(scv);
            trySendAccessibilityNotification("Selected.");
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        SudokuCellView scv = (SudokuCellView) v;
        if (mOnSudokuCellSelectedListener != null && !scv.getCell().isLocked()) {
            mOnSudokuCellSelectedListener.onSudokuCellSelected(scv);
        }
    }

    public void clearHighlights() {
        for (int i = 0; i < 81; i++) {
            SudokuCellView scv = getCell(i);
            if (scv.isShowHighlight()) {
                scv.setShowHighlight(false);
                scv.invalidate();
            }
        }
    }

    public void highlightValue(int value) {
        for (int i = 0; i < 81; i++) {
            SudokuCellView scv = getCell(i);
            SudokuCell cell = scv.getCell();
            boolean highlight = cell.getMarks().size() == 1 && cell.getMarks().first() == value;
            if (scv.isShowHighlight() != highlight) {
                scv.setShowHighlight(highlight);
                scv.invalidate();
            }
        }
    }

    public void clearValidation() {
        for (int i = 0; i < 81; i++) {
            SudokuCellView scv = getCell(i);
            if (scv.isShowInvalid()) {
                scv.setShowInvalid(false);
                scv.invalidate();
            }
        }
    }

    public void lockCells() {
        for (int i = 0; i < 81; i++) {
            SudokuCellView scv = getCell(i);
            if (!scv.getCell().isLocked()) {
                scv.getCell().setLocked(true);
                scv.invalidate();
            }
        }
    }

    public boolean isComplete() {
        for (int i = 0; i < 81; i++) {
            SudokuCellView scv = getCell(i);
            SudokuCell cell = scv.getCell();
            if (cell.getMarks().isEmpty() || cell.getMarks().size() > 1
                    || (cell.getMarks().size() == 1 && cell.getMarks().first() != cell.getSolution())) {
                return false;
            }
        }
        return true;
    }

    public void validateBoard() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 81; i++) {
            SudokuCellView scv = getCell(i);
            SudokuCell cell = scv.getCell();
            boolean invalid = cell.getMarks().size() == 1 && cell.getMarks().first() != cell.getSolution();
            if (invalid) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(scv.getCellPosition());
                scv.setShowInvalid(invalid);
                scv.invalidate();
            }
        }

        if (sb.length() == 0) {
            sb.append("All cells are correct.");
        } else {
            sb.insert(0, "Cells ");
            sb.append(", are incorrect.");
        }

        trySendAccessibilityNotification(sb.toString());
    }

    public void trySendAccessibilityNotification(String text) {
        AccessibilityManager accessibilityManager = (AccessibilityManager) getContext().getSystemService(
                Context.ACCESSIBILITY_SERVICE);

        if (!accessibilityManager.isEnabled()) {
            return;
        }

        AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        event.setClassName(getClass().getName());
        event.setPackageName(getContext().getPackageName());
        event.getText().add(text);
        accessibilityManager.sendAccessibilityEvent(event);
    }

    public String getCurrentState() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 81; i++) {
            int row = (int) (i / 9.0f);
            int col = i % 9;
            View v = ((LinearLayout) getChildAt(row)).getChildAt(col);
            sb.append(((SudokuCellView) v).getCell().toString());
        }
        return sb.toString();
    }

    public void setBoard(List<SudokuCell> board) {
        for (int i = 0; i < 81; i++) {
            SudokuCellView scv = getCell(i);
            scv.setCell(board.get(i));
            if (!board.get(i).isLocked()) {
                scv.setOnClickListener(this);
                scv.setOnFocusChangeListener(this);
            }
            scv.postInvalidate();
        }
        postInvalidate();
    }

    public SudokuCellView getCell(int position) {
        return (SudokuCellView) ((LinearLayout) getChildAt((int) (position / 9.0))).getChildAt(position % 9);
    }

    private void init() {
        mGridPaint = new Paint();
        mGridPaint.setStyle(Style.STROKE);
        mGridPaint.setColor(0xFF000000);
        mGridPaint.setStrokeWidth(4);

        setBackgroundColor(0xFFFAFAFA);

        setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT);
        lp.weight = 1.0f;
        for (int i = 0; i < 9; i++) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int j = 0; j < 9; j++) {
                SudokuCellView view = new SudokuCellView(getContext());
                row.addView(view, lp);
            }
            addView(row, lp);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        super.dispatchKeyEvent(event);
        return false;
    }

    @SuppressLint("WrongCall")
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        onDraw(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (mPrevHeight != getMeasuredHeight() || mPrevWidth != getMeasuredWidth()) {
            mCellRect.set(0, 0, getMeasuredWidth() / 9, getMeasuredHeight() / 9);
            mMarkRect.set(0, 0, 0, 0);
            SudokuCellView.sTextPaint.setTextSize(10);
            SudokuCellView.sMarkPaint.setTextSize(80);
            do {
                SudokuCellView.sTextPaint.getTextBounds("9".toCharArray(), 0, 1, mMarkRect);
                mMarkRect.inset(-7, -7);

                if (mCellRect.width() <= mMarkRect.width() || mCellRect.height() <= mMarkRect.height()) {
                    break;
                }

                SudokuCellView.sTextPaint.setTextSize(SudokuCellView.sTextPaint.getTextSize() + 1);
            } while (true);

            do {
                SudokuCellView.sMarkPaint.getTextBounds("9 9 9".toCharArray(), 0, 5, mMarkRect);
                mMarkRect.inset(0, (int) (mMarkRect.height() * -1.5) + 3);

                if (mMarkRect.width() < mCellRect.width() && mMarkRect.height() < mCellRect.height()) {
                    break;
                }

                SudokuCellView.sMarkPaint.setTextSize(SudokuCellView.sMarkPaint.getTextSize() - 2);

            } while (true);
            Timber.d("::onMeasure:PERFORMADIN");
        }

        mPrevWidth = getMeasuredWidth();
        mPrevHeight = getMeasuredHeight();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mGrid == null) {
            mGrid = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas gCanvas = new Canvas(mGrid);
            mGridPaint.setStrokeWidth(2);
            gCanvas.drawRect(new Rect(1, 1, getMeasuredWidth() - 1, getMeasuredHeight() - 1), mGridPaint);

            mGridPaint.setStrokeWidth(0);
            for (int i = 0; i < 9; i++) {
                LinearLayout row = (LinearLayout) getChildAt(i);
                if (i == 3 || i == 6) {
                    mGridPaint.setStrokeWidth(3);
                    gCanvas.drawLine(0, row.getTop(), getMeasuredWidth(), row.getTop(), mGridPaint);
                    mGridPaint.setStrokeWidth(0);

                }
                for (int j = 0; j < 9; j++) {
                    SudokuCellView view = (SudokuCellView) row.getChildAt(j);
                    if (i == 0 && (j == 3 || j == 6)) {
                        mGridPaint.setStrokeWidth(3);
                        gCanvas.drawLine(view.getLeft(), 0, view.getLeft(), getMeasuredHeight(), mGridPaint);
                        mGridPaint.setStrokeWidth(0);
                    }
                    gCanvas.drawRect(new Rect(view.getLeft(), row.getTop(), view.getRight(), row.getBottom()),
                            mGridPaint);
                }
            }
        }

        canvas.drawBitmap(mGrid, 0, 0, mGridPaint);
    }
}
