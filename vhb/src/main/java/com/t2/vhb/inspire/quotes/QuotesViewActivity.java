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

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.t2.vhb.ActionBarActivity;
import com.t2.vhb.R;
import com.t2.vhb.db.VhbContract.Quotes;
import com.t2.vhb.util.AccessibilityUtils;

import java.security.SecureRandom;
import java.util.Random;

import timber.log.Timber;

/**
 * @author wes
 */
public class QuotesViewActivity extends ActionBarActivity {

    public static final String EXTRA_INITIAL_QUOTE_ID = "EXTRA_INITIAL_QUOTE_ID";

    private static final int REQUEST_ADD_QUOTE = 1;
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    private static final String TAG = "com.t2.vhb.inspire.quotes.QuotesViewActivity";

    private CountDownTimer mTimer;

    /**
     * Position index for the current quote. Used in conjunction with the mOrder
     * array to determine which quote is displayed
     */
    private int mCurrentIndex;

    /**
     * Used if EXTRA_DEFAULT_QUOTE_ID is set. This will determine the initial
     * quote shown.
     */
    private Long mInitialQuoteId;

    private boolean mReverse;
    private Animation mNextOutAnimation;
    private Animation mNextInAnimation;
    private Animation mPrevOutAnimation;
    private Animation mPrevInAnimation;

    private ScrollView mQuoteScroll;
    private QuoteLayout mQuoteLayout;
    private TextView mQuoteAuthor;

    /**
     * Shuffled array of quote ids used to determine the order quotes are
     * displayed
     */
    private long[] mOrderIds;

    private GestureDetector mGestureDetector;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mTimer != null) {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                stopTimer();
            } else if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
                startTimer();
            }
        }

        return mGestureDetector.onTouchEvent(ev) || super.dispatchTouchEvent(ev);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.mnu_quotes, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final Cursor cursor = getCurrentQuote();
        if (cursor != null) {
            cursor.moveToFirst();
            final boolean favorite = cursor.moveToFirst()
                    && cursor.getInt(cursor.getColumnIndex(Quotes.COL_FAVORITE)) >= 1;
            cursor.close();
            menu.findItem(R.id.mnu_favorite).setTitle(favorite ? "Remove from Favorites" : "Add to Favorites");
        }


        return true;
    }

    private Cursor getCurrentQuote() {
        if(mOrderIds == null) return null;
        if(mCurrentIndex >= mOrderIds.length) { mCurrentIndex = mOrderIds.length - 1; }
        return getContentResolver().query(ContentUris.withAppendedId(Quotes.CONTENT_URI, mOrderIds[mCurrentIndex]),
                null, null, null, null);

    }

    private void shuffleQuotes() {
        final Cursor quotes = getContentResolver().query(Quotes.CONTENT_URI, new String[]{
                Quotes._ID, Quotes.COL_AUTHOR, Quotes.COL_QUOTE, Quotes.COL_FAVORITE
        }, null, null, Quotes.COL_FAVORITE + " DESC");

        final int count = quotes != null ? quotes.getCount() : 0;

        if (count == 0 && quotes != null) {

            quotes.close();
            startActivityForResult(new Intent(this, QuotesEditActivity.class), REQUEST_ADD_QUOTE);
            return;

        }

        mOrderIds = new long[count];

        int favoriteCount = 0;
        int randIndex = 0;
        int leftBoundIndex = mInitialQuoteId != null ? 1 : 0;

        long id;
        int index = leftBoundIndex;
        boolean favorite;
        if (quotes != null) {
            quotes.moveToFirst();
            do {
                id = quotes.getLong(0);
                favorite = quotes.getInt(3) > 0;

                // Skip the initial quote, it is going to be first no matter what.
                if (mInitialQuoteId != null && id == mInitialQuoteId) {
                    continue;
                }

                mOrderIds[index++] = id;
                if (favorite) {
                    favoriteCount++;
                }
            } while (quotes.moveToNext());
            quotes.close();
        }

        if (mInitialQuoteId != null) {
            mOrderIds[0] = mInitialQuoteId;
        }

        final Random rand = new SecureRandom();
        for (int i = favoriteCount - 1; i >= leftBoundIndex + 1; i--) {
            randIndex = rand.nextInt(i) + leftBoundIndex;
            id = mOrderIds[i];
            mOrderIds[i] = mOrderIds[randIndex];
            mOrderIds[randIndex] = id;
        }

        for (int i = mOrderIds.length - 1; i >= favoriteCount + leftBoundIndex; i--) {
            randIndex = rand.nextInt(i - (favoriteCount - 1)) + favoriteCount + leftBoundIndex;
            id = mOrderIds[i];
            mOrderIds[i] = mOrderIds[randIndex];
            mOrderIds[randIndex] = id;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try (final Cursor cursor = getCurrentQuote()) {
            if (cursor != null && cursor.moveToFirst()) {
                final long id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
                final boolean favorite = cursor.getInt(cursor.getColumnIndex(Quotes.COL_FAVORITE)) >= 1;

                switch (item.getItemId()) {
                    case R.id.mnu_quotes_edit:
                        startActivity(new Intent(this, QuotesEditActivity.class));
                        break;
                    case R.id.mnu_favorite:
                        final ContentValues vals = new ContentValues();
                        vals.put(Quotes.COL_FAVORITE, favorite ? 0 : 1);
                        getContentResolver().update(ContentUris.withAppendedId(Quotes.CONTENT_URI, id), vals, null, null);
                        Toast.makeText(this, favorite ? "Quote removed from favorites." : "Quote added to favorites.",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.mnu_settings:
                        startActivity(new Intent(this, QuotesSettingsActivity.class));
                        break;
                    case R.id.mnu_add:
                        Intent addQuoteIntent = new Intent(getApplicationContext(), QuotesAddActivity.class);
                        startActivityForResult(addQuoteIntent, REQUEST_ADD_QUOTE);
                        break;
                    case R.id.mnu_remove:
                        if(mOrderIds.length > 1) {
                            QuoteDeleteDialog frg = QuoteDeleteDialog.createInstance(id);
                            frg.setOnDeleteListener(() -> {
                                shuffleQuotes();
                                loadQuote();
                            });
                            frg.show(getSupportFragmentManager(), "delete");
                        } else {
                            Toast.makeText(this, "You cant delete every quote", Toast.LENGTH_LONG)
                                    .show();
                        }
                        break;
                    default:
                        return super.onOptionsItemSelected(item);
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ADD_QUOTE:
                if (resultCode == RESULT_OK) {
                    shuffleQuotes();
                    loadQuote();
                    mQuoteLayout.startAnimation(mNextInAnimation);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.quotes_view);
        setTitle("Inspire Me");
        setIcon(R.drawable.icon_inspire_me);

        mGestureDetector = new GestureDetector(this, new GestureListener());
        mQuoteScroll = (ScrollView) findViewById(R.id.lay_quote_scroll);
        mQuoteLayout = (QuoteLayout) findViewById(R.id.lay_quote_row);
        mQuoteAuthor = (TextView) findViewById(R.id.lbl_quote_author);

        loadAnimations();

        mQuoteScroll.setOnKeyListener((v, keyCode, event) -> {

            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    mReverse = false;
                    nextQuote();

                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    mReverse = true;
                    nextQuote();

                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                ((ScrollView) v).scrollBy(0, 40);
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                ((ScrollView) v).scrollBy(0, -40);
            }
            return false;
        });

        if (savedInstanceState != null) {
            mCurrentIndex = savedInstanceState.getInt("current_position");
            mOrderIds = savedInstanceState.getLongArray("order");
        } else {
            if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(EXTRA_INITIAL_QUOTE_ID)) {
                mInitialQuoteId = getIntent().getExtras().getLong(EXTRA_INITIAL_QUOTE_ID);
            }

            shuffleQuotes();
        }

        loadQuote();
        mQuoteLayout.startAnimation(mNextInAnimation);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("current_position", mCurrentIndex);
        outState.putLongArray("order", mOrderIds);
    }

    private void loadAnimations() {
        mNextOutAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);
        mNextOutAnimation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationEnd(Animation animation) {
                loadQuote();
                mQuoteLayout.startAnimation(mNextInAnimation);
                mQuoteLayout.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }
        });

        mNextInAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
        mNextInAnimation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mQuoteScroll.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
                supportInvalidateOptionsMenu();
            }
        });

        mPrevOutAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_out_right);
        mPrevOutAnimation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationEnd(Animation animation) {
                loadQuote();
                mQuoteLayout.startAnimation(mPrevInAnimation);
                mQuoteLayout.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }
        });

        mPrevInAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
        mPrevInAnimation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationEnd(Animation animation) {
                mReverse = false;
                mQuoteScroll.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {

            }
        });
    }

    private void loadQuote() {
        if (mOrderIds == null || mOrderIds.length == 0) {
            return;
        }

        Timber.d("Current Quote Index: %s", mCurrentIndex);

        final Cursor cursor = getCurrentQuote();
        if (cursor.moveToFirst() && cursor.getCount() >= 1) {
            String author = cursor.getString(cursor.getColumnIndex(Quotes.COL_AUTHOR));
            String quote = cursor.getString(cursor.getColumnIndex(Quotes.COL_QUOTE));


            if (TextUtils.isEmpty(author)) {
                author = "Anonymous";
            }
            mQuoteScroll.setContentDescription(String.format("%s. %s", quote, author));
            mQuoteAuthor.setText(author);
            mQuoteLayout.setQuote(quote);
            mQuoteLayout.setVisibility(View.VISIBLE);
            mQuoteLayout.invalidate();
        }
        cursor.close();

    }

    private void nextQuote() {
        if (mOrderIds.length == 0) {
            return;
        }

        mCurrentIndex = mReverse ? (mCurrentIndex - 1) : (mCurrentIndex + 1) % mOrderIds.length;
        if (mCurrentIndex < 0) {
            mCurrentIndex = mOrderIds.length + mCurrentIndex;
        }

        mQuoteLayout.startAnimation(mReverse ? mPrevOutAnimation : mNextOutAnimation);
    }

    private void setupTimer() {
        stopTimer();

        int delay = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString(
                getString(R.string.pref_quotes_delay), "10000"));
        if (AccessibilityUtils.isScreenReaderActive(getApplicationContext())) {
            delay = 0;
        }

        if (delay > 0) {

            mTimer = new CountDownTimer(delay, 1000) {
                @Override
                public void onFinish() {
                    nextQuote();
                    start();
                }

                @Override
                public void onTick(long millisUntilFinished) {
                }
            };
            startTimer();
        } else {
            mTimer = null;
        }
    }

    private void startTimer() {
        if (mTimer == null) {
            return;
        }
        mTimer.start();
    }

    private void stopTimer() {
        if (mTimer == null) {
            return;
        }
        mTimer.cancel();
    }

    private class GestureListener extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                nextQuote();
                startTimer();
                return false; // Right to left
            } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                mReverse = true;
                nextQuote();
                startTimer();
                return false; // Left to right
            }
            return false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupTimer();
    }

}
