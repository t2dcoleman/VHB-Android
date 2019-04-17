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

package com.t2.vhb.distractions.mahjong;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.view.WindowManager;

import com.t2.mahjong.MahjongPuzzlesFragment;
import com.t2.mahjong.MahjongPuzzlesFragment.OnMahjongPuzzleSelectedListener;
import com.t2.mahjong.db.MahjongContract.Mahjong.Difficulty;
import com.t2.vhb.ActionBarActivity;
import com.t2.vhb.R;

public class MahjongChangePuzzleActivity extends ActionBarActivity implements ActionBar.TabListener,
        OnMahjongPuzzleSelectedListener {

	private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.activity_mahjong_change_puzzle);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

	    DifficultyPagerAdapter mPagerAdapter = new DifficultyPagerAdapter(getSupportFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mPagerAdapter);

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        for (int i = 0; i < mPagerAdapter.getCount(); i++) {
            ActionBar.Tab tab = actionBar.newTab().setText(mPagerAdapter.getPageTitle(i))
                    .setContentDescription(String.format("%s puzzles tab", mPagerAdapter.getPageTitle(i)))
                    .setTabListener(this);
            actionBar.addTab(tab);
        }

        setTitle("Mahjong Puzzles");
    }

    @Override
    public void onMahjongPuzzleSelected(int id) {
        startActivity(new Intent(this, MahjongActivity.class));
        finish();
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction arg1) {

    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction arg1) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction arg1) {

    }

    public class DifficultyPagerAdapter extends FragmentPagerAdapter {

        private final Difficulty[] mDifficulties;

        public DifficultyPagerAdapter(FragmentManager fm) {
            super(fm);
            mDifficulties = Difficulty.values();
        }

        @Override
        public Fragment getItem(int position) {
            return MahjongPuzzlesFragment.createInstance(mDifficulties[position]);
        }

        @Override
        public int getCount() {
            return mDifficulties.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mDifficulties[position].toString();
        }
    }
}
