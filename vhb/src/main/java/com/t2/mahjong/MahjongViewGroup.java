/*
 *
 * Created by Wes Turney on 12/17/13.
 *
 * MahjongLib
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
 * Government Agency Original Software Designation: MahjongLib001
 * Government Agency Original Software Title: MahjongLib
 * User Registration Requested. Please send email
 * with your contact information to: robert.a.kayl.civ@mail.mil
 * Government Agency Point of Contact for Original Software: robert.a.kayl.civ@mail.mil
 *
 */

package com.t2.mahjong;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;

import com.t2.vhb.R;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressLint("UseSparseArrays")
public class MahjongViewGroup extends ViewGroup implements OnClickListener {
    private static final Paint PAINT_SELECTION;

    private static final Paint PAINT_DEFAULT;

    private static final Paint PAINT_FREE;

    private static final Paint PAINT_FOCUS;

    private static final float TILE_LEFT_ISO = 40;

    private static final float TILE_TOP_ISO = 40;

    private static final float TILE_FACE_WIDTH = 322;

    private static final float TILE_FACE_HEIGHT = 426;

    private static final float TILE_WIDTH_STEP = 161;

    private static final float TILE_HEIGHT_STEP = 213;

    private final Map<Integer, Bitmap> mIconCache = new HashMap<>();

    private final Rect mViewRect = new Rect();

    private final Matrix mMatrix = new Matrix();

    static {
        PAINT_FREE = new Paint(Paint.ANTI_ALIAS_FLAG);
        ColorFilter cf = new LightingColorFilter(0xFFCCE3BF, 1);
        PAINT_FREE.setColorFilter(cf);

        PAINT_FOCUS = new Paint(Paint.ANTI_ALIAS_FLAG);
        cf = new LightingColorFilter(0xFFe7851c, 1);
        PAINT_FOCUS.setColorFilter(cf);

        PAINT_SELECTION = new Paint(Paint.ANTI_ALIAS_FLAG);
        cf = new LightingColorFilter(0xFFBFDBE3, 1);
        PAINT_SELECTION.setColorFilter(cf);

        PAINT_DEFAULT = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    private Paint mCurrentPaint;

    private String[] mDescriptions;

    private Animation mWobbleAnim;
    private Animation mShrinkAnim, mSelectedShrinkAnim;

    private int mVerticalSteps;
    private int mHorizontalSteps;

    private int mMargin;

    private Animation mHintOne;
    private Animation mHintTwo;

    private MahjongListener mMahjongListener;

    private Mahjong mGame;

    private int mSelectionId = -1;
    private boolean mShowFreeTiles;

    public MahjongViewGroup(Context context) {
        super(context);
        init();
    }

    public MahjongViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MahjongViewGroup(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void clearCache() {
        for (Bitmap bmp : mIconCache.values()) {
            bmp.recycle();
        }
        mIconCache.clear();
    }

    public String getCurrentState() {
        StringBuilder sb = new StringBuilder();
        for (Tile tile : mGame.getTiles()) {
            sb.append(tile.isVisible() ? 1 : 0);
        }
        return sb.toString();
    }

    private boolean isComplete() {
        int size = mGame.getTiles().size();
        for (int i = 0; i < size; i++) {
            if (mGame.getTiles().get(i).isVisible()) {
                return false;
            }
        }
        return true;
    }

    private boolean isIncompletable() {
        List<Tile> freeTiles = mGame.getFreeTiles();
        Set<Integer> matchIds = new HashSet<>();
        for (Tile tile : freeTiles) {
            if (!matchIds.add(tile.getMatchId())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        TileView tv = (TileView) v;
        if (!mGame.isFree(tv.getTile())) {
            return;
        }

        if (mHintOne != null) {
            mHintOne.cancel();
            mHintOne.reset();
        }

        if (mHintTwo != null) {
            mHintTwo.cancel();
            mHintTwo.reset();
        }

        if (mSelectionId >= 0 && mSelectionId != tv.getId()) {
            final TileView selView = (TileView) findViewById(mSelectionId);

            if (selView.getTile().getMatchId() != tv.getTile().getMatchId()) {
                mSelectionId = tv.getId();
                selView.setSelected(false);
                tv.setSelected(true);
                tv.invalidate();
                selView.invalidate();
                trySendAccessibilityNotification("Selected.");
                return;
            }
            mSelectionId = -1;
            tv.setSelected(false);
            selView.setSelected(false);
            selView.startAnimation(mSelectedShrinkAnim);
            mShrinkAnim.setAnimationListener(new RemoveTileAnimationListener(tv, selView));
            tv.startAnimation(mShrinkAnim);

            for (int i = 0; i < getChildCount(); i++) {
                TileView tile = (TileView) getChildAt(i);
                if (tile.isFocusable()) {
                    tile.requestFocus();
                    break;
                }
            }

            trySendAccessibilityNotification("Pair removed from board.");

            // invalidate();
        } else {
            mSelectionId = tv.isSelected() ? -1 : tv.getId();
            trySendAccessibilityNotification(tv.isSelected() ? "Deeselected." : "Selected.");
            tv.setSelected(!tv.isSelected());
            tv.invalidate();
        }
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

    public void restartGame() {
        if (mSelectionId >= 0) {
            final TileView selView = (TileView) findViewById(mSelectionId);
            selView.setSelected(false);
            mSelectionId = -1;
        }

        for (int i = 0; i < getChildCount(); i++) {
            TileView tile = (TileView) getChildAt(i);
            tile.setSelected(false);
            tile.clearAnimation();
            tile.setClickable(true);
            tile.getTile().setVisible(true);
        }
        refreshEnabledState();
        requestLayout();
        invalidate();
    }

    public void setGame(Mahjong game) {
        mGame = game;

        for (int i = 0; i < getChildCount(); i++) {
            TileView tile = (TileView) getChildAt(i);
            tile.clearAnimation();
        }

        removeAllViews();

        int id = 0;
        for (Tile tile : mGame.mTiles) {
            TileView tv = new TileView(getContext());
            tv.setId(id);
            tv.setTag(tile);
            tv.setTile(tile);
            addView(tv);
            id++;
        }

        mVerticalSteps = mGame.getHeight();
        mHorizontalSteps = mGame.getWidth();

        refreshEnabledState();

        requestLayout();
    }

    public void setMahjonggListener(MahjongListener mahjonggListener) {
        mMahjongListener = mahjonggListener;
    }

    public void showHint() {
        List<Tile> freeTiles = mGame.getFreeTiles();
        Collections.shuffle(freeTiles);

        Map<Integer, Tile> tileMap = new HashMap<>();
        for (Tile tile : freeTiles) {
            if (!tileMap.containsKey(tile.getMatchId())) {
                tileMap.put(tile.getMatchId(), tile);
            } else {
                if (mHintOne != null) {
                    mHintOne.cancel();
                    mHintOne.reset();
                }

                if (mHintTwo != null) {
                    mHintTwo.cancel();
                    mHintTwo.reset();
                }

                TileView matchView = (TileView) findViewWithTag(tileMap.get(tile.getMatchId()));
                TileView otherView = (TileView) findViewWithTag(tile);

                TileView tvOne = new TileView(getContext());
                tvOne.setClickable(false);
                tvOne.setSelected(matchView.isSelected());
                // tvOne.setEnabled(false);
                tvOne.setTile(matchView.getTile());
                tvOne.setId(R.id.tileviewone);

                TileView tvTwo = new TileView(getContext());
                tvTwo.setClickable(false);
                tvTwo.setSelected(otherView.isSelected());
                // tvTwo.setEnabled(false);
                tvTwo.setTile(otherView.getTile());
                tvTwo.setId(R.id.tileviewtwo);

                addView(tvOne);
                addView(tvTwo);

                tvOne.bringToFront();
                tvTwo.bringToFront();
                mHintOne = AnimationUtils.loadAnimation(getContext(), R.anim.tile_pulse);
                mHintOne.setAnimationListener(new HintAnimationListener(tvOne));
                mHintTwo = AnimationUtils.loadAnimation(getContext(), R.anim.tile_pulse);
                mHintTwo.setAnimationListener(new HintAnimationListener(tvTwo));
                tvOne.startAnimation(mHintOne);

                tvTwo.startAnimation(mHintTwo);

                return;
            }
        }
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        return super.getChildDrawingOrder(childCount, i);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mGame == null) {
            return;
        }

        mHorizontalSteps = mGame.getWidth();
        mVerticalSteps = mGame.getHeight();

        final int minCol = mGame.getMinCol();
        final int minRow = mGame.getMinRow();

        mViewRect.set(l, t, r, b);
        int mTopPan = 0;
        int mLeftPan = 0;
        mViewRect.offset(mLeftPan, mTopPan);
        float mZoom = 1.0f;
        mViewRect.inset((int) (mViewRect.width() - (mViewRect.width() * mZoom)),
                (int) (mViewRect.height() - (mViewRect.height() * mZoom)));

        final float fitRatio = Math.min(mViewRect.width() / getPuzzleWidth(), mViewRect.height() / getPuzzleHeight());
        final float stepWidth = fitRatio * TILE_WIDTH_STEP;
        final float stepHeight = fitRatio * TILE_HEIGHT_STEP;
        final float tileWidth = stepWidth * 2.0f;
        final float tileHeight = stepHeight * 2.0f;
        final float leftIso = fitRatio * TILE_LEFT_ISO;
        final float topIso = fitRatio * TILE_TOP_ISO;
        final float leftIsoStep = fitRatio * (TILE_LEFT_ISO / 2.0f);
        final float topIsoStep = fitRatio * (TILE_TOP_ISO / 2.0f);
        final float maxRightIso = (mGame.getGreatestRightmostDepth() - 1) * leftIso;
        final float maxTopIso = (mGame.getGreatestTopmostDepth() - 1) * topIso;

        final float finalWidth = (stepWidth * mHorizontalSteps) - (mHorizontalSteps * leftIsoStep) + (maxRightIso);
        final float finalHeight = (stepHeight * mVerticalSteps) - (mVerticalSteps * topIsoStep) + (maxTopIso);
        final int topShift = Math.round((getMeasuredHeight() - finalHeight) / 2.0f);
        final int leftShift = Math.round((getMeasuredWidth() - finalWidth) / 2.0f);

        final int count = getChildCount();
        TileView child;
        TileSlot slot;
        Tile tile;
        for (int i = 0; i < count; i++) {
            child = (TileView) getChildAt(i);
            slot = child.getTile().getSlot();
            tile = child.getTile();
            if (tile.isVisible()) {
                final int layer = slot.getLayer();
                final int col = slot.getCol() - minCol;
                final int row = slot.getRow() - minRow;
                final int left = leftShift + Math.round((col * stepWidth) - (col * leftIsoStep) - maxRightIso);
                final int top = topShift + Math.round((row * stepHeight) - (row * topIsoStep) + maxTopIso);

                final int leftLayerOffset = Math.round(leftIso * layer);
                final int topLayerOffset = Math.round(topIso * layer);

                child.layout(left + leftLayerOffset, top - topLayerOffset,
                        Math.round(left + tileWidth + leftLayerOffset), Math.round(top + tileHeight - topLayerOffset));
            }
        }
    }

    private float getPuzzleHeight() {
        final float tiles = mVerticalSteps / 2.0f;
        return (TILE_FACE_HEIGHT * tiles) + mMargin + (TILE_TOP_ISO * 2.0f)
                + (mGame.getGreatestTopmostDepth() * TILE_TOP_ISO) - (TILE_TOP_ISO * (tiles - 1));
    }

    private float getPuzzleWidth() {
        final float tiles = mHorizontalSteps / 2.0f;
        return (TILE_FACE_WIDTH * tiles) + mMargin + (TILE_LEFT_ISO * 2.0f)
                + (mGame.getGreatestRightmostDepth() * TILE_LEFT_ISO) - (TILE_LEFT_ISO * (tiles - 1));
    }

    private void init() {
        final float scale = getResources().getDisplayMetrics().density;
        mMargin = (int) (10 * scale);
        mDescriptions = getContext().getResources().getStringArray(R.array.tile_descriptions);

        mWobbleAnim = AnimationUtils.loadAnimation(getContext(), R.anim.wobble);
        mShrinkAnim = AnimationUtils.loadAnimation(getContext(), R.anim.shrink);
        mSelectedShrinkAnim = AnimationUtils.loadAnimation(getContext(), R.anim.shrink);
    }

    @SuppressLint("NewApi")
    private void refreshEnabledState() {
        List<Tile> freeTiles = mGame.getFreeTiles();
        for (int i = 0; i < getChildCount(); i++) {
            TileView tile = (TileView) getChildAt(i);
            boolean enabled = freeTiles.contains(tile.getTile());
            tile.setEnabled(enabled);
            tile.setFocusable(enabled);
            tile.setImportantForAccessibility(enabled ? IMPORTANT_FOR_ACCESSIBILITY_YES
                    : IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
    }

    public interface MahjongListener {
        void onPuzzleComplete();

        void onPuzzleIncompletable();
    }

    private class HintAnimationListener implements AnimationListener {
        private final TileView mView;

        public HintAnimationListener(TileView view) {
            mView = view;
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mView.clearAnimation();
            post(() -> MahjongViewGroup.this.removeView(mView));
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }
    }

    private class RemoveTileAnimationListener implements AnimationListener {
        private final TileView mViewOne;
        private final TileView mViewTwo;

        public RemoveTileAnimationListener(TileView viewOne, TileView viewTwo) {
            mViewOne = viewOne;
            mViewTwo = viewTwo;
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mViewOne.hide();
            mViewTwo.hide();

            if (isComplete()) {
                if (mMahjongListener != null) {
                    mMahjongListener.onPuzzleComplete();
                }
                return;
            }

            if (isIncompletable()) {
                if (mMahjongListener != null) {
                    mMahjongListener.onPuzzleIncompletable();
                }
                return;
            }

            refreshEnabledState();
            requestLayout();
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
            mViewOne.setClickable(false);
            mViewTwo.setClickable(false);
        }
    }

    private class TileView extends View {

        private Tile mTile;

        public TileView(Context context) {
            super(context);
            init();
        }

        public TileView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        public TileView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            init();
        }

        public Tile getTile() {
            return mTile;
        }

        public void hide() {
            setSelected(false);
            mTile.setVisible(false);
            setClickable(false);
            invalidate();
        }

        @Override
        public void setSelected(boolean selected) {
            super.setSelected(selected);
            if (selected) {
                startAnimation(mWobbleAnim);
            } else {
                clearAnimation();
            }
        }

        public void setTile(Tile tile) {
            mTile = tile;
        }

        @Override
        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
            if (!isEnabled()) {
                return false;
            }

            if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED
                    || event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                event.getText().add(getTileDescription(mTile) + " Tile.");
                return true;
            }

            return super.dispatchPopulateAccessibilityEvent(event);
        }

        @SuppressLint("DrawAllocation")
        @Override
        protected void onDraw(Canvas canvas) {
            if (!mTile.isVisible()) {
                return;
            }

            getDrawingRect(mViewRect);

            if (mIconCache.get(mTile.getTileId()) == null) {
                mIconCache.put(mTile.getTileId(),
                        BitmapFactory.decodeResource(getResources(), mTile.getTileResourceId()));
            }

            if (mShowFreeTiles && isEnabled() && !isSelected() && !isFocused()) {
                mCurrentPaint = PAINT_FREE;
            } else if (isSelected()) {
                mCurrentPaint = PAINT_SELECTION;
            } else if (isFocused()) {
                mCurrentPaint = PAINT_FOCUS;
            }

            // if (isInEditMode()) {
            // bitmap = BitmapFactory.decodeResource(getResources(),
            // R.drawable.tile_0_0);
            // }

            mMatrix.reset();
            mMatrix.postScale(mViewRect.width() / (float) mIconCache.get(mTile.getTileId()).getWidth(),
                    mViewRect.height() / (float) mIconCache.get(mTile.getTileId()).getHeight());
            canvas.drawBitmap(mIconCache.get(mTile.getTileId()), mMatrix, mCurrentPaint);

            mCurrentPaint = PAINT_DEFAULT;
        }

        private void init() {
            setOnClickListener(MahjongViewGroup.this);
        }

    }

    private String getTileDescription(Tile tile) {
        if (tile.getTileId() < mDescriptions.length) {
            return mDescriptions[tile.getTileId()];
        }
        return "Unknown";
    }

    public boolean isShowFreeTiles() {
        return mShowFreeTiles;
    }

    public void setShowFreeTiles(boolean enabled) {
        trySendAccessibilityNotification(enabled ? "Highlighting enabled." : "Highlighting disabled.");
        mShowFreeTiles = enabled;
        for (int i = 0; i < getChildCount(); i++) {
            TileView tile = (TileView) getChildAt(i);
            tile.invalidate();
        }
    }

}
