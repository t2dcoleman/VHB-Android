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

import com.t2.vhb.R;

public class Tile {

    private static final int[] TILE_RESOURCE_IDS = new int[] {
            R.drawable.tile_0_0, R.drawable.tile_0_1, R.drawable.tile_0_2, R.drawable.tile_0_3, R.drawable.tile_0_4,
            R.drawable.tile_0_5, R.drawable.tile_0_6, R.drawable.tile_0_7, R.drawable.tile_0_8, R.drawable.tile_0_9,
            R.drawable.tile_0_10, R.drawable.tile_0_11, R.drawable.tile_0_12, R.drawable.tile_0_13,
            R.drawable.tile_0_14, R.drawable.tile_0_15, R.drawable.tile_0_16, R.drawable.tile_0_17,
            R.drawable.tile_0_18, R.drawable.tile_0_19, R.drawable.tile_0_20, R.drawable.tile_0_21,
            R.drawable.tile_0_22, R.drawable.tile_0_23, R.drawable.tile_0_24, R.drawable.tile_0_25,
            R.drawable.tile_0_26, R.drawable.tile_0_27, R.drawable.tile_0_28, R.drawable.tile_0_29,
            R.drawable.tile_0_30, R.drawable.tile_0_31, R.drawable.tile_0_32, R.drawable.tile_0_33,
            R.drawable.tile_0_34, R.drawable.tile_0_35, R.drawable.tile_0_36, R.drawable.tile_0_37,
            R.drawable.tile_0_38, R.drawable.tile_0_39, R.drawable.tile_0_40
    };

    private TileSlot mSlot;

    private int mMatchId;
    private int mSubId; // used for flowers / seasons
    private boolean mVisible;

    public Tile(TileSlot slot) {
        mSlot = slot;
        mVisible = true;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Tile other = (Tile) obj;
        if (mSlot == null) {
            if (other.mSlot != null)
                return false;
        } else if (!mSlot.equals(other.mSlot))
            return false;
        return true;
    }

    /**
     * @return the matchId
     */
    public int getMatchId() {
        return mMatchId;
    }

    public int getTileId() {
        if (mMatchId == 33) {
            return mMatchId + mSubId;
        } else if (mMatchId == 34) {
            return mMatchId + 3 + mSubId;
        } else {
            return mMatchId;
        }
    }

    public int getTileResourceId() {
        return TILE_RESOURCE_IDS[getTileId()];
    }

    /**
     * @return the slot
     */
    public TileSlot getSlot() {
        return mSlot;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mSlot == null) ? 0 : mSlot.hashCode());
        return result;
    }

    /**
     * @return the visible
     */
    public boolean isVisible() {
        return mVisible;
    }

    /**
     * @param matchId the matchId to set
     */
    public void setMatchId(int matchId) {
        mMatchId = matchId;
    }

    /**
     * @param slot the slot to set
     */
    public void setSlot(TileSlot slot) {
        mSlot = slot;
    }

    /**
     * @param visible the visible to set
     */
    public void setVisible(boolean visible) {
        mVisible = visible;
    }

    /**
     * @return the subId
     */
    public int getSubId() {
        return mSubId;
    }

    /**
     * @param subId the subId to set
     */
    public void setSubId(int subId) {
        mSubId = subId;
    }

}
