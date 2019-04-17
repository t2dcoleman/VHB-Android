/*
 *
 * Created by Wes Turney on 12/17/13.
 *
 * ControlledBreathingLib
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
 * Government Agency Original Software Designation: ControlledBreathingLib001
 * Government Agency Original Software Title: ControlledBreathingLib
 * User Registration Requested. Please send email
 * with your contact information to: robert.a.kayl.civ@mail.mil
 * Government Agency Point of Contact for Original Software: robert.a.kayl.civ@mail.mil
 *
 */

package com.t2.controlledbreathing;

import com.t2.vhb.R;

public enum DurationType {
    INHALE(R.string.pref_inhale_duration, R.string.inhale_instructions, R.string.inhale, 7000, 3000, false),
    EXHALE(R.string.pref_exhale_duration, R.string.exhale_instructions, R.string.exhale, 7000, 3000, false),
    HOLD(R.string.pref_hold_duration, R.string.hold_instructions, R.string.hold, 0, 1000, true),
    REST(R.string.pref_rest_duration, R.string.rest_instructions, R.string.rest, 0, 1000, true);

    final int mPrefKeyId;
    private final int mInstructionsId;
    private final int mNameId;
    private final long mDefaultDuration;
    private final long mMinDuration;
    private final boolean mShowDisable;

    DurationType(int prefKeyId, int instructionsId, int nameId, long defaultDuration, long minDuration,
                 boolean showDisable) {
        mPrefKeyId = prefKeyId;
        mInstructionsId = instructionsId;
        mNameId = nameId;
        mDefaultDuration = defaultDuration;
        mMinDuration = minDuration;
        mShowDisable = showDisable;
    }

    public int getPrefKeyId() {
        return mPrefKeyId;
    }

    public int getInstructionsId() {
        return mInstructionsId;
    }

    public int getNameId() {
        return mNameId;
    }

    public long getDefaultDuration() {
        return mDefaultDuration;
    }

    public long getMinDuration() {
        return mMinDuration;
    }

    public boolean isShowDisable() {
        return mShowDisable;
    }

}
