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

public class CbtiRoadFragment extends CbtiFragment {

    private final int[] mCaptionIds = {
            R.string.s_Road01, R.string.s_Road02, R.string.s_Road03, R.string.s_Road04, R.string.s_Road05,
            R.string.s_Road06, R.string.s_Road07, R.string.s_Road08, R.string.s_Road09, R.string.s_Road10,
            R.string.s_Road11, R.string.s_Road12, R.string.s_Road13, R.string.s_Road14, R.string.s_Road15,
            R.string.s_Road16, R.string.s_Road17, R.string.s_Road18, R.string.s_Road19, R.string.s_Road20,
            R.string.s_Road21, R.string.s_Road22, R.string.s_Road23, R.string.s_Road24, R.string.s_Road25,
            R.string.s_Road26, R.string.s_Road27, R.string.s_Road28, R.string.s_Road29, R.string.s_Road30
    };
    private final int[] mCaptionStarts = {
            1, 8, 13, 22, 26, 36, 38, 44, 55, 59, 63, 75, 79, 81, 87, 97, 100, 104, 109, 121, 124, 133, 138, 146, 155,
            167, 180, 184, 206, 214
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
        return R.raw.mp3_imagerycountryroad;
    }

    @Override
    protected int getPlaceholderImageId() {
        return R.drawable.guidedimageryroad;
    }

    @Override
    protected int getWelcomeId() {
        return R.string.s_RoadText;
    }

    @Override
    public CbtiType getType() {
        return CbtiType.ROAD;
    }
}
