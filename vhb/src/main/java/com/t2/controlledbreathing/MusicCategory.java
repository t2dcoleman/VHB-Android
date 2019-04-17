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

import java.security.SecureRandom;
import java.util.Random;

public enum MusicCategory {
	NONE("No Music"),
	PERSONAL_MUSIC("My Music"),
	RANDOM("Random"),
	AMBIENT_EVENINGS("Ambient Evenings", R.raw.ambientevenings),
	EVO_SOLUTION("Evo Solution", R.raw.evosolution),
	OCEAN_MIST("Ocean Mist", R.raw.oceanmist),
	WANING_MOMENTS("Waning Moments", R.raw.waningmoments),
	WATERMARK("Watermark", R.raw.watermark);

	private final String mName;
	private long mResourceId;

	public static long getRandomResource() {
		Random rand = new SecureRandom();
		MusicCategory[] cats = values();
		MusicCategory cat = null;
		do {
			cat = cats[rand.nextInt(cats.length)];
		} while (cat.getResourceId() == 0);
		return cat.getResourceId();
	}

	MusicCategory(String name) {
		mName = name;
	}

	MusicCategory(String name, int resourceId) {
		mName = name;
		mResourceId = resourceId;
	}

	public String getName() {
		return mName;
	}

	public long getResourceId() {
		return mResourceId;
	}

	@Override
	public String toString() {
		return mName;
	}
}