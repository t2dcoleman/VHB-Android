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
package com.t2.vhb.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.t2.vhb.tools.CopingTool;

import java.util.HashSet;
import java.util.Set;

/**
 * @author wes
 * 
 */
final class CopingToolUtils {

	private static Set<CopingTool> FAVORITES_CACHE = null;

	public synchronized static void addFavorite(Context context, CopingTool tool) {
		if (getCache(context).add(tool)) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			prefs.edit().putString("favorite_tools", TextUtils.join("|", FAVORITES_CACHE)).apply();
		}
	}

	public static Set<CopingTool> getFavorites(Context context) {
		return new HashSet<>(getCache(context));
	}

	public static boolean isFavorite(Context context, CopingTool tool) {
		return getCache(context).contains(tool);
	}

	public synchronized static void removeFavorite(Context context, CopingTool tool) {
		if (getCache(context).remove(tool)) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			prefs.edit().putString("favorite_tools", TextUtils.join("|", FAVORITES_CACHE)).apply();
		}
	}

	private synchronized static Set<CopingTool> getCache(Context context) {
		if (FAVORITES_CACHE == null) {
			FAVORITES_CACHE = new HashSet<>();
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			String favString = prefs.getString("favorite_tools", null);
			if (favString != null) {
				String[] favs = favString.split("\\|");
				for (String fav : favs) {
					try {
						FAVORITES_CACHE.add(CopingTool.valueOf(fav));
					} catch (IllegalArgumentException e) {
						// Coping tool never / no longer exists
					}
				}
			}
		}
		return FAVORITES_CACHE;
	}

	private CopingToolUtils() {

	}
}
