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

public class CbtiBeachFragment extends CbtiFragment {
    private final int[] mCaptionIds = {
            R.string.s_Beach01, R.string.s_Beach02, R.string.s_Beach03, R.string.s_Beach04, R.string.s_Beach05,
            R.string.s_Beach06, R.string.s_Beach07, R.string.s_Beach08, R.string.s_Beach09, R.string.s_Beach10,
            R.string.s_Beach11, R.string.s_Beach12, R.string.s_Beach13, R.string.s_Beach14, R.string.s_Beach15,
            R.string.s_Beach16, R.string.s_Beach17, R.string.s_Beach18, R.string.s_Beach19, R.string.s_Beach20,
            R.string.s_Beach21, R.string.s_Beach22, R.string.s_Beach23, R.string.s_Beach24, R.string.s_Beach25,
            R.string.s_Beach26, R.string.s_Beach27, R.string.s_Beach28, R.string.s_Beach29, R.string.s_Beach30,
            R.string.s_Beach31, R.string.s_Beach32, R.string.s_Beach33
    };
    private final int[] mCaptionStarts = {
            1, 9, 14, 20, 24, 30, 41, 43, 46, 51, 62, 66, 74, 90, 96, 106, 117, 126, 132, 140, 150, 162, 173, 178, 188,
            200, 207, 218, 236, 252, 261, 273, 289
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
        return R.raw.mp3_imagerybeach;
    }

    @Override
    protected int getPlaceholderImageId() {
        return R.drawable.guidedimagerybeach;
    }

    @Override
    protected int getWelcomeId() {
        return R.string.s_BeachText;
    }

    @Override
    public CbtiType getType() {
        return CbtiType.BEACH;
    }
}
