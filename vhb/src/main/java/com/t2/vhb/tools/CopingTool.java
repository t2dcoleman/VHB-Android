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

package com.t2.vhb.tools;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;

import com.t2.copingcards.CopingContract.CopingCard;
import com.t2.vhb.R;
import com.t2.vhb.coping.activities.ActivitySchedulerActivity;
import com.t2.vhb.coping.cards.CopingHelpActivity;
import com.t2.vhb.coping.cards.CopingViewActivity;
import com.t2.vhb.distractions.mahjong.MahjongActivity;
import com.t2.vhb.distractions.mahjong.MahjongChangePuzzleActivity;
import com.t2.vhb.distractions.photopuzzle.PhotoPuzzleActivity;
import com.t2.vhb.distractions.sudoku.SudokuActivity;
import com.t2.vhb.distractions.sudoku.SudokuChangePuzzleActivity;
import com.t2.vhb.distractions.wordsearch.WordsearchActivity;
import com.t2.vhb.relax.breathing.ControlledBreathingActivity;
import com.t2.vhb.relax.cbti.CbtiActivity;
import com.t2.vhb.relax.cbti.CbtiActivity.CbtiType;
import com.t2.vhb.relax.pmr.PmrActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wes
 */
public enum CopingTool {

    REMIND("Remind Me", R.drawable.icon_sm_remind),
    DISTRACTIONS("Distract Me", R.drawable.icon_sm_distract),
    COPING("Coping Tools", R.drawable.icon_sm_coping_tools),
    RELAX("Relax Me", R.drawable.icon_sm_relax),
    INSPIRE("Inspire Me", R.drawable.icon_sm_inspire),

    COPING_CARDS("Coping Cards", R.drawable.icon_coping_cards, 0xFF993399, 0xFFC259C2, COPING, CopingViewActivity.class),
    ACTIVITY_PLANNER("Activity Planner", R.drawable.icon_distract_scheduler, 0xFF820D82, 0xFFC259C2, COPING,
            ActivitySchedulerActivity.class),

    SUDOKU("Sudoku Puzzle", R.drawable.icon_distract_soduku, 0xFF339933, 0xFF9bdc9b, DISTRACTIONS, SudokuActivity.class),
    PHOTO_PUZZLE("Photo Puzzle", R.drawable.icon_distract_picture, 0xFF297b29, 0xFF9bdc9b, DISTRACTIONS,
            PhotoPuzzleActivity.class),
    WORDSEARCH_PUZZLE("Word Search", R.drawable.icon_distract_word, 0xFF339933, 0xFF9bdc9b, DISTRACTIONS,
            WordsearchActivity.class),
    MAHJONG("Mahjong Solitaire", R.drawable.icon_distract_mahjong, 0xFF297b29, 0xFF9bdc9b, DISTRACTIONS,
            MahjongActivity.class),

    CONTROLLED_BREATHING("Controlled Breathing", R.drawable.icon_relax_breath, 0xFF0167b1, 0xFF64afe3, RELAX,
            ControlledBreathingActivity.class),
    PMR("Muscle Relaxation", R.drawable.icon_relax_muscle, 0xFF01518b, 0xFF64afe3, RELAX, PmrActivity.class),
    CBTI_BEACH("Guided Meditation - Beach", R.drawable.icon_beach, 0xFF0167b1, 0xFF64afe3, RELAX, CbtiActivity.class),
    CBTI_FOREST("Guided Meditation - Forest", R.drawable.icon_forest, 0xFF01518b, 0xFF64afe3, RELAX, CbtiActivity.class),
    CBTI_ROAD("Guided Meditation - Country Road", R.drawable.icon_road, 0xFF0167b1, 0xFF64afe3, RELAX,
            CbtiActivity.class);

    private static final Map<CopingTool, List<CopingTool>> COPING_TOOL_MAP = new HashMap<>();

    static {
        for (CopingTool tool : CopingTool.values()) {
            CopingTool parent = tool.getParent();
            if (tool.getParent() == null) {
                continue;
            }

            List<CopingTool> subTools = COPING_TOOL_MAP.get(parent);
            if (subTools == null) {
                subTools = new ArrayList<>();
            }

            subTools.add(tool);
            COPING_TOOL_MAP.put(parent, subTools);
        }
    }

    private int mColor, mFocusColor;
    private CopingTool mParent;
    private String mName;
    private int mIcon;
    private Class<?> mActivity;

    CopingTool(String name) {
        mName = name;
    }

    CopingTool(String name, CopingTool parent, Class<?> activity) {
        mName = name;
        mParent = parent;
        mActivity = activity;
    }

    CopingTool(String name, int icon) {
        mName = name;
        mIcon = icon;
    }

    CopingTool(String name, int icon, int color, int focusColor, Class<?> activity) {
        mName = name;
        mIcon = icon;
        mFocusColor = focusColor;
        mActivity = activity;
        mColor = color;
    }

    CopingTool(String name, int icon, int color, int focusColor, CopingTool parent, Class<?> activity) {
        mName = name;
        mParent = parent;
        mIcon = icon;
        mFocusColor = focusColor;
        mActivity = activity;
        mColor = color;
    }

    public void startActivity(Context ctx) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        int puzzleId;
        CbtiType cbtiType;
        Intent intent = new Intent(ctx, getActivity());

        switch (this) {
            case CBTI_BEACH:
                intent.putExtra(CbtiActivity.EXTRA_CBTI_TYPE, CbtiType.BEACH);
                break;
            case CBTI_FOREST:
                intent.putExtra(CbtiActivity.EXTRA_CBTI_TYPE, CbtiType.FOREST);
                break;
            case CBTI_ROAD:
                intent.putExtra(CbtiActivity.EXTRA_CBTI_TYPE, CbtiType.ROAD);
                break;
            case MAHJONG:
                puzzleId = prefs.getInt(ctx.getString(R.string.pref_mahjong_puzzle), -1);
                if (puzzleId <= 0) {
                    intent = new Intent(ctx, MahjongChangePuzzleActivity.class);
                }
                break;
            case SUDOKU:
                puzzleId = prefs.getInt(ctx.getString(R.string.pref_sudoku_puzzle), -1);
                if (puzzleId <= 0) {
                    intent = new Intent(ctx, SudokuChangePuzzleActivity.class);
                }
                break;
            case COPING_CARDS:
                final Cursor c = ctx.getContentResolver().query(CopingCard.CONTENT_URI, new String[] {
                    BaseColumns._ID
                }, null, null, null);
                if (c.getCount() > 0) {
                    intent = new Intent(ctx, CopingViewActivity.class);
                } else {
                    intent = new Intent(ctx, CopingHelpActivity.class);
                }
                c.close();
            default:
                break;
        }
        ctx.startActivity(intent);
    }

    /**
     * @return the activity
     */
    private Class<?> getActivity() {
        return mActivity;
    }

    /**
     * @return the icon
     */
    public int getIcon() {
        return mIcon;
    }

    /**
     * @return the name
     */
    public String getName() {
        return mName;
    }

    /**
     * @return the parent
     */
    private CopingTool getParent() {
        return mParent;
    }

    /**
     * @return the color
     */
    public int getColor() {
        return mColor;
    }

    /**
     * @return the focusColor
     */
    public int getFocusColor() {
        return mFocusColor;
    }

    public List<CopingTool> getSubTools() {
        if (hasSubTools()) {
            return new ArrayList<>(COPING_TOOL_MAP.get(this));
        }

        return null;
    }

    private boolean hasSubTools() {
        return COPING_TOOL_MAP.containsKey(this);
    }

}
