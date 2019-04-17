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

package com.t2.vhb.relax.cbti;

import com.t2.vhb.R;
import com.t2.vhb.relax.cbti.CbtiActivity.CbtiType;

public class CbtiForestFragment extends CbtiFragment {

    private final int[] mCaptionIds = {
            R.string.s_Forest01, R.string.s_Forest02, R.string.s_Forest03, R.string.s_Forest04, R.string.s_Forest05,
            R.string.s_Forest06, R.string.s_Forest07, R.string.s_Forest08, R.string.s_Forest09, R.string.s_Forest10,
            R.string.s_Forest11, R.string.s_Forest12, R.string.s_Forest13, R.string.s_Forest14, R.string.s_Forest15,
            R.string.s_Forest16, R.string.s_Forest17, R.string.s_Forest18, R.string.s_Forest19, R.string.s_Forest20,
            R.string.s_Forest21, R.string.s_Forest22, R.string.s_Forest23, R.string.s_Forest24, R.string.s_Forest25,
            R.string.s_Forest26, R.string.s_Forest27, R.string.s_Forest28, R.string.s_Forest29, R.string.s_Forest30,
            R.string.s_Forest31
    };
    private final int[] mCaptionStarts = {
            1, 8, 14, 20, 24, 
            32, 44, 46, 52, 57, 
            64, 75, 86, 98, 110, 
            120, 136, 147, 151, 158, 
            163, 177, 187, 197, 203, 
            211, 226, 242, 256, 266, 
            278
    };


    @Override
    protected int[] getCaptionIds() {
        return mCaptionIds;
    }

    @Override
    protected int[] getCaptionStarts() {
        return mCaptionStarts;
    }

    @Override
    protected int getMediaId() {
        return R.raw.mp3_imageryforest;
    }

    @Override
    protected int getPlaceholderImageId() {
        return R.drawable.guidedimageryforest;
    }

    @Override
    protected int getWelcomeId() {
        return R.string.s_ForestText;
    }

    @Override
    public CbtiType getType() {
        return CbtiType.FOREST;
    }
}
