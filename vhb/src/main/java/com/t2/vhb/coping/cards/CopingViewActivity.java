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

package com.t2.vhb.coping.cards;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.t2.copingcards.CopingContract.CopingCard;
import com.t2.vhb.ActionBarActivity;
import com.t2.vhb.R;

public class CopingViewActivity extends ActionBarActivity implements OnPageChangeListener, LoaderCallbacks<Cursor> {

    private static final String TAG = "com.t2.vhb.coping.cards.CopingDetailsActivity";

    private static final int REQ_NEW_CARD = 1;

    private ViewPager mPager;
    private View mPrevCardCue, mNextCardCue;
    private TextView mCardCount;
    private CopingCardAdapter mAdapter;

    private boolean mMoveToLast;
    private boolean mShowToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.activity_coping_detail);
        setTitle("Coping Cards");
        setIcon(com.t2.vhb.R.drawable.icon_coping_cards);

        mShowToast = savedInstanceState == null;

        mAdapter = new CopingCardAdapter(getSupportFragmentManager(), null);
        initViews();

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    @Override
    public void onPageSelected(int arg0) {
        initPageCount();
        mPager.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return new CursorLoader(this, CopingCard.CONTENT_URI, new String[] {
            BaseColumns._ID
        }, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
        if (cursor.getCount() == 0) {
            Intent intent = new Intent(this, CopingHelpActivity.class);
            startActivity(intent);
            finish();
        } else {
            mAdapter.swapCursor(cursor);
            if (mMoveToLast) {
                mMoveToLast = false;
                mPager.setCurrentItem(mAdapter.getCount());
            }

            if (mShowToast) {
                Toast.makeText(this, "Add coping cards by pressing the 'Menu' button and selecting 'Add Coping Card'",
                        Toast.LENGTH_LONG).show();
                mShowToast = false;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        mAdapter.swapCursor(null);
    }

    private void initPageCount() {
        mCardCount.setVisibility(View.VISIBLE);
        mCardCount.setText(String.format("%d of %d", mPager.getCurrentItem() + 1, mPager.getAdapter().getCount()));
        mPrevCardCue.setVisibility(mPager.getCurrentItem() == 0 ? View.INVISIBLE : View.VISIBLE);
        mNextCardCue.setVisibility(mPager.getCurrentItem() >= mPager.getAdapter().getCount() - 1 ? View.INVISIBLE
                : View.VISIBLE);
    }

    private void initViews() {
        mPager = (ViewPager) findViewById(R.id.card_pager);
        mPager.setAdapter(mAdapter);

        mPrevCardCue = findViewById(R.id.prev_card_cue);
        mNextCardCue = findViewById(R.id.next_card_cue);
        mCardCount = (TextView) findViewById(R.id.count);

        mPager.setOnPageChangeListener(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_NEW_CARD:
                if (resultCode == Activity.RESULT_OK) {
                    mMoveToLast = true;
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(com.t2.vhb.R.menu.mnu_coping_card, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case com.t2.vhb.R.id.action_add:
                intent = new Intent(this, CopingEditActivity.class);
                startActivityForResult(intent, REQ_NEW_CARD);
                return true;
            case com.t2.vhb.R.id.action_help:
                startActivity(new Intent(this, CopingHelpActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class CopingCardAdapter extends FragmentStatePagerAdapter {

        private Cursor mCursor;

        public CopingCardAdapter(FragmentManager fm, Cursor cursor) {
            super(fm);
            this.mCursor = cursor;
        }

        @Override
        public Fragment getItem(int position) {
            if (mCursor == null) {
                return null;
            }

            mCursor.moveToPosition(position);
            return CopingViewFragment.newInstance(mCursor.getInt(0));
        }

        @Override
        public int getCount() {
            if (mCursor == null) {
                return 0;
            } else {
                return mCursor.getCount();
            }
        }

        @Override
        public int getItemPosition(Object object) {
            return PagerAdapter.POSITION_NONE;
        }

        public void swapCursor(Cursor c) {
            if (mCursor == c) {
                return;
            }

            this.mCursor = c;

            if (!isFinishing()) {
                notifyDataSetChanged();
            }
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            initPageCount();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}
