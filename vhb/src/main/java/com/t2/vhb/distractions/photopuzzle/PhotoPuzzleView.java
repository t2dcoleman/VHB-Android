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

package com.t2.vhb.distractions.photopuzzle;

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.MediaColumns;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.t2.vhb.R;
import com.t2.vhb.util.MediaUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.SecureRandom;
import java.util.Random;

import timber.log.Timber;

/**
 * @author wes
 */
public class PhotoPuzzleView extends LinearLayout implements OnClickListener, OnFocusChangeListener {
    public static final int MIN_COLUMNS = 2;
    public static final int MIN_ROWS = 3;
    
    private static final int PADDING = 2;

    private static final String TAG = "com.t2.vhb.distractions.photopuzzle.PhotoPuzzleView";

    private boolean mLoaded;

    //
    private PhotoPuzzlePiece mFirstSelection;
    private PhotoPuzzlePiece mSecondSelection;

    private PhotoPuzzleListener mPuzzleListener;

    private GameState mGameState;

    private Animation mPulseAnimation;

    public PhotoPuzzleView(Context context) {
        super(context);
        init();
    }

    public PhotoPuzzleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Bitmap getSolutionImage() {
        if (mGameState.mSolution == null) {
            try {
                File cache = new File(Environment.getExternalStorageDirectory(),
                        "/Android/data/com.t2.vhb/cache/photopuzzle/solution.png");
                FileInputStream fin = new FileInputStream(cache);
                mGameState.mSolution = BitmapFactory.decodeStream(fin);
                fin.close();
            } catch (IOException e) {
                Timber.e(e, "Loading solution from cache failed");
            }
        }
        return mGameState.mSolution;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof PhotoPuzzleSaveState) {
            super.onRestoreInstanceState(((PhotoPuzzleSaveState) state).getSuperState());
            mGameState = ((PhotoPuzzleSaveState) state).mSaveState;
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable state = super.onSaveInstanceState();
        PhotoPuzzleSaveState saveState = new PhotoPuzzleSaveState(state);
        saveState.mSaveState = mGameState;
        return saveState;
    }

    public void reset() {
        mFirstSelection = null;
        mSecondSelection = null;
        mLoaded = false;
        mGameState = new GameState();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        
        mGameState.mCols = prefs.getInt(getContext().getString(R.string.pref_photo_puzzle_columns), MIN_COLUMNS);
        mGameState.mRows = prefs.getInt(getContext().getString(R.string.pref_photo_puzzle_rows), MIN_ROWS);

        init();
        invalidate();
        requestLayout();
    }

    public void setImage(long id, int rotation) {
        mGameState.mImageId = id;
        mGameState.mImageRotation = rotation;
    }

    public void setPhotoPuzzleListener(PhotoPuzzleListener puzzleListener) {
        mPuzzleListener = puzzleListener;
    }

    /*
     * (non-Javadoc)
     * @see android.widget.GridView#onMeasure(int, int)
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (!mLoaded && mGameState != null && mGameState.mImageId >= 0) {
            if (mGameState.mPieces == null) {
                createPuzzle();
            }
            initPuzzle();
            mLoaded = true;
            postDelayed(() -> {
                if (mPuzzleListener != null) {
                    mPuzzleListener.onPuzzleCreated();
                }
            }, 100);

        }
    }

    private int mLastFocusIndex;

    private void initPuzzle() {
        removeAllViews();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT,
                android.view.ViewGroup.LayoutParams.FILL_PARENT);
        lp.weight = 1.0f;
        Resources r = getResources();
        int px = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, PADDING, r.getDisplayMetrics()));
        for (int rowIndex = 0; rowIndex < mGameState.mRows; rowIndex++) {
            LinearLayout row = new LinearLayout(getContext());
            // row.setClipChildren(false);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int colIndex = 0; colIndex < mGameState.mCols; colIndex++) {
                final int index = (rowIndex * mGameState.mCols) + colIndex;
                ImageView cell = new ImageView(getContext());
                cell.setContentDescription("Puzzle Piece");
                cell.setTag(mGameState.mPieces[index]);
                cell.setScaleType(ScaleType.CENTER);
                cell.setPadding(px, px, px, px);
                cell.setBackgroundColor(0x00000000);
                cell.setImageBitmap(mGameState.mPieces[index].getImage());
                cell.setOnClickListener(this);
                cell.setId(mGameState.mPieces[index].getCorrectPosition());
                cell.setEnabled(true);
                cell.setClickable(true);
                row.addView(cell, lp);
                cell.setOnFocusChangeListener(this);
                cell.setFocusable(true);
            }
            addView(row, lp);
        }
        LinearLayout row = (LinearLayout) getChildAt(mLastFocusIndex / mGameState.mCols);
        View v = row.getChildAt(mLastFocusIndex % mGameState.mCols);
        if (v != null) {
            v.requestFocus();
        }
    }

    private void cachePuzzlePiece(PhotoPuzzlePiece piece) {
        FileOutputStream fos = null;
        try {
            final File cache = new File(Environment.getExternalStorageDirectory(),
                    "/Android/data/com.t2.vhb/cache/photopuzzle/" + piece.getCorrectPosition() + ".png");

            if (!cache.exists()) {
                cache.getParentFile().mkdirs();
                cache.createNewFile();
            }
            fos = new FileOutputStream(cache);
            piece.getImage().compress(CompressFormat.PNG, 90, fos);
            Timber.d("Cached slice: %s", cache.getName());
        } catch (IOException e) {
            Timber.e(e, "Caching image slice failed");
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Timber.e(e);
                }
            }
        }
    }

    private void cacheSolution(Bitmap solution) {
        FileOutputStream fos = null;
        try {
            File nomedia = new File(Environment.getExternalStorageDirectory(),
                    "/Android/data/com.t2.vhb/cache/photopuzzle/.nomedia");
            File cache = new File(Environment.getExternalStorageDirectory(),
                    "/Android/data/com.t2.vhb/cache/photopuzzle/solution.png");
            if (!cache.exists()) {
                cache.getParentFile().mkdirs();
                nomedia.createNewFile();
                cache.createNewFile();
            }
            fos = new FileOutputStream(cache);
            solution.compress(CompressFormat.PNG, 90, fos);
            Timber.d("Cached solution: %s", cache.getName());
        } catch (IOException e) {
            Timber.e(e, "Caching solution failed");
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Timber.e(e);
                }
            }
        }
    }

    private void clearCache() {
        File cache = new File(Environment.getExternalStorageDirectory(), "/Android/data/com.t2.vhb/cache/photopuzzle/");

        if (!cache.exists()) {
            return;
        }

        for (File child : cache.listFiles()) {
            child.delete();
        }

    }

    private void createPuzzle() {
        float maxImageWidth = (getMeasuredWidth() - ((mGameState.mCols + 1) * PADDING) - 20);

        float maxImageHeight = (getMeasuredHeight() - ((mGameState.mRows + 1) * PADDING) - 20);

        float columnWidth = maxImageWidth / mGameState.mCols;
        float rowHeight = maxImageHeight / mGameState.mRows;
        float finalRatio = maxImageWidth / maxImageHeight;

        Bitmap photo = null;

        final Cursor media = getContext().getContentResolver().query(
                ContentUris.withAppendedId(Images.Media.EXTERNAL_CONTENT_URI, mGameState.mImageId), new String[] {
                    MediaColumns.DATA
                }, null, null, null);
        String path = null;
        if (media.moveToFirst()) {
            path = media.getString(0);
        }
        media.close();

        try {
            photo = MediaUtils.sampleImage(getContext().getContentResolver(), path, (int) maxImageWidth,
                    (int) maxImageHeight, mGameState.mImageRotation);
        } catch (IOException e) {
            Timber.e(e, "Unable to load image with ID: %s", mGameState.mImageId);
        }

        // decodeUri(ContentUris.withAppendedId(Images.Media.EXTERNAL_CONTENT_URI,
        // mGameState.mImageId), (int) maxImageWidth, (int) maxImageHeight);

        if (photo == null) {
            Toast.makeText(getContext(), "Unable to locate image", Toast.LENGTH_SHORT).show();
            return;
        }

        float imageRatio = (float) photo.getWidth() / (float) photo.getHeight();

        float sourceHeight, sourceWidth;
        if (imageRatio >= finalRatio) {
            // Photo needs to be scaled with regards to height first
            sourceHeight = maxImageHeight;
            sourceWidth = photo.getWidth() * (maxImageHeight / photo.getHeight());
        } else {
            // Photo needs to be scaled with regards to width first
            sourceWidth = maxImageWidth;
            sourceHeight = photo.getHeight() * (maxImageWidth / photo.getWidth());
        }

        float heightDiff = Math.abs(sourceHeight - maxImageHeight);
        float widthDiff = Math.abs(sourceWidth - maxImageWidth);
        int leftOffset = (int) Math.floor(widthDiff / 2.0);
        int topOffset = (int) Math.floor((heightDiff / 2.0));

        Bitmap scaledPhoto = Bitmap.createScaledBitmap(photo, (int) sourceWidth, (int) sourceHeight, false);
        photo.recycle();

        Bitmap solution = Bitmap.createBitmap(scaledPhoto, leftOffset, topOffset,
                (int) (scaledPhoto.getWidth() - widthDiff), (int) (scaledPhoto.getHeight() - heightDiff));
        scaledPhoto.recycle();

        PhotoPuzzlePiece[] pieces = new PhotoPuzzlePiece[mGameState.mRows * mGameState.mCols];
        int index = 0;
        for (int i = 0; i < mGameState.mRows; i++) {
            for (int j = 0; j < mGameState.mCols; j++) {
                int x = (int) Math.floor(j * columnWidth);
                int y = (int) Math.floor(i * rowHeight);
                int width = (int) columnWidth;
                int height = (int) rowHeight;

                if (x + width > solution.getWidth()) {
                    width = solution.getWidth() - x;
                }
                if (y + height > solution.getHeight()) {
                    height = solution.getHeight() - y;
                }

                Bitmap slice = Bitmap.createBitmap(solution, x, y, width, height);
                PhotoPuzzlePiece piece = new PhotoPuzzlePiece();
                piece.setImage(slice);
                piece.setCorrectPosition(index);
                pieces[index] = piece;
                index++;
            }
        }

        Random rand = new SecureRandom();
        for (int i = pieces.length - 1; i >= 1; i--) {
            int randIndex = rand.nextInt(i);
            PhotoPuzzlePiece piece = pieces[i];
            pieces[i] = pieces[randIndex];
            pieces[randIndex] = piece;
        }

        for (int i = 0; i < pieces.length; i++) {
            pieces[i].setCurrentPosition(i);
        }

        mGameState.mPieces = pieces;
        mGameState.mSolution = solution;

        new CachePuzzleTask(this).execute((Void) null);

    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            mLastFocusIndex = ((PhotoPuzzlePiece) v.getTag()).getCurrentPosition();
        }
        v.setBackgroundColor(hasFocus && !isInTouchMode() ? 0xFFCCCCCC : 0x00000000);
    }

    private void init() {
        setOrientation(LinearLayout.VERTICAL);
        mPulseAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.pulse);
        setClipChildren(false);
    }

    @Override
    public void onClick(View v) {
        PhotoPuzzlePiece piece = (PhotoPuzzlePiece) v.getTag();

        if (mFirstSelection != null) {
            if (mFirstSelection.getCorrectPosition() == piece.getCorrectPosition()) {
                return;
            }

            mSecondSelection = piece;
            View firstSelectionView = findViewWithTag(mFirstSelection);
            LinearLayout firstParent = (LinearLayout) firstSelectionView.getParent();
            LinearLayout secondParent = (LinearLayout) v.getParent();
            int deltaLeft = v.getLeft() - firstSelectionView.getLeft();
            int deltaTop = secondParent.getTop() - firstParent.getTop();

            Animation anim = new TranslateAnimation(0, deltaLeft, 0, deltaTop);
            anim.setDuration(300);
            anim.setFillAfter(true);
            firstSelectionView.startAnimation(anim);
            anim = new TranslateAnimation(0, -deltaLeft, 0, -deltaTop);
            anim.setDuration(300);
            anim.setFillAfter(true);
            anim.setAnimationListener(new AnimationListener() {
                private boolean mEnded = false;

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (mEnded) {
                        return;
                    }

                    mEnded = true;
                    swapPieces(mFirstSelection, mSecondSelection);
                    mFirstSelection = null;
                    mSecondSelection = null;
                    postDelayed(() -> {
                        initPuzzle();
                        // forceLayout();
                        // for (int i = 0; i < getChildCount(); i++) {
                        // LinearLayout row = (LinearLayout) getChildAt(i);
                        // row.forceLayout();
                        // }
                        // invalidate();
                        // findViewWithTag(mFirstSelection).clearAnimation();
                        // findViewWithTag(mSecondSelection).clearAnimation();
                        // mFirstSelection = null;
                        // mSecondSelection = null;
                    }, 20);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationStart(Animation animation) {
                }
            });
            // firstSelectionView.getAnimation().reset();
            // v.clearAnimation();
            v.startAnimation(anim);

            // v.invalidate();
            // firstSelectionView.invalidate();
        } else {
            mFirstSelection = piece;
            v.startAnimation(mPulseAnimation);
        }

    }

    private synchronized void swapPieces(PhotoPuzzlePiece first, PhotoPuzzlePiece second) {
        mGameState.mMoveCount++;

        int firstPosition = first.getCurrentPosition();
        first.setCurrentPosition(second.getCurrentPosition());
        second.setCurrentPosition(firstPosition);

        // View firstView = findViewWithTag(first);
        // View secondView = findViewWithTag(second);
        // LinearLayout firstRow = (LinearLayout) firstView.getParent();
        // LinearLayout secondRow = (LinearLayout) secondView.getParent();
        // int firstIndex = firstRow.indexOfChild(firstView);
        // int secondIndex = secondRow.indexOfChild(secondView);
        // firstRow.removeView(firstView);
        // secondRow.removeView(secondView);
        // LinearLayout.LayoutParams lp = new
        // LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
        // LinearLayout.LayoutParams.FILL_PARENT);
        // lp.weight = 1.0f;
        // if (firstIndex > secondIndex) {
        // secondRow.addView(firstView, secondIndex, lp);
        // firstRow.addView(secondView, firstIndex, lp);
        // } else {
        // firstRow.addView(secondView, firstIndex, lp);
        // secondRow.addView(firstView, secondIndex, lp);
        // }

        mGameState.mPieces[first.getCurrentPosition()] = first;
        mGameState.mPieces[second.getCurrentPosition()] = second;

        boolean completed = true;
        for (PhotoPuzzlePiece piece : mGameState.mPieces) {
            if (piece.getCorrectPosition() != piece.getCurrentPosition()) {
                completed = false;
            }
        }

        if (completed) {
            postDelayed(() -> {
                if (mPuzzleListener != null) {
                    mPuzzleListener.onPuzzleComplete(mGameState.mRows, mGameState.mCols, mGameState.mMoveCount);
                }
            }, 100);
        }

        // for (int i = 0; i < mGameState.mRows; i++) {
        // StringBuilder sb = new StringBuilder();
        // for (int j = 0; j < mGameState.mCols; j++) {
        // sb.append(mGameState.mPieces[(i * mGameState.mCols) +
        // j].getCorrectPosition() + "|");
        // }
        // System.out.println(sb.toString());
        // }
        // System.out.println("======GAME STATE=====");
        // for (int i = 0; i < getChildCount(); i++) {
        // StringBuilder sb = new StringBuilder();
        // LinearLayout row = (LinearLayout) getChildAt(i);
        // for (int j = 0; j < row.getChildCount(); j++) {
        // sb.append(((PhotoPuzzlePiece)
        // row.getChildAt(j).getTag()).getCorrectPosition() + "|");
        // }
        // System.out.println(sb.toString());
        // }
        // System.out.println("======VIEW STATE=====");

        // requestLayout();
    }

    public interface PhotoPuzzleListener {

        void onPuzzleComplete(int rows, int cols, int totalMoves);

        void onPuzzleCreated();

    }

    private static class CachePuzzleTask extends AsyncTask<Void, Void, Void> {

        private WeakReference<PhotoPuzzleView> photoPuzzleViewWeakReference;

        CachePuzzleTask(PhotoPuzzleView context) {
            photoPuzzleViewWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... params) {

            PhotoPuzzleView activity = photoPuzzleViewWeakReference.get();
            if(activity == null) return null;

            activity.clearCache();
            final PhotoPuzzlePiece[] pieces = activity.mGameState.mPieces;
            for (PhotoPuzzlePiece piece : pieces) {
                activity.cachePuzzlePiece(piece);
            }
            activity.cacheSolution(activity.mGameState.mSolution);
            return null;
        }
    }

    private static final class GameState {
        private Bitmap mSolution;
        private long mImageId;
        private int mImageRotation;
        private int mCols;
        private int mRows;
        private int mMoveCount;
        private PhotoPuzzlePiece[] mPieces;

        public GameState() {
            mImageId = -1;
        }

        public GameState(Parcel in) {
            mPieces = (PhotoPuzzlePiece[]) in.readParcelableArray(PhotoPuzzlePiece.class.getClassLoader());
            mCols = in.readInt();
            mRows = in.readInt();
            mImageId = in.readLong();
            mMoveCount = in.readInt();
            mImageRotation = in.readInt();
        }

    }

    private static final class PhotoPuzzleSaveState extends BaseSavedState {

        private GameState mSaveState;

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<PhotoPuzzleSaveState> CREATOR = new Parcelable.Creator<PhotoPuzzleView.PhotoPuzzleSaveState>() {

            @Override
            public PhotoPuzzleSaveState createFromParcel(Parcel source) {
                return new PhotoPuzzleSaveState(source);
            }

            @Override
            public PhotoPuzzleSaveState[] newArray(int size) {
                return new PhotoPuzzleSaveState[size];
            }
        };

        public PhotoPuzzleSaveState(Parcel in) {
            super(in);
            mSaveState = new GameState(in);

        }

        public PhotoPuzzleSaveState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            if (mSaveState != null) {
                dest.writeParcelableArray(mSaveState.mPieces, flags);
                dest.writeInt(mSaveState.mCols);
                dest.writeInt(mSaveState.mRows);
                dest.writeLong(mSaveState.mImageId);
                dest.writeInt(mSaveState.mMoveCount);
                dest.writeInt(mSaveState.mImageRotation);
            }
        }

    }
}
