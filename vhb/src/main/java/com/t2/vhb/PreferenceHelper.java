package com.t2.vhb;

import android.preference.PreferenceManager;

/*
 * PreferenceHelper.java
 * Interface to simplify migration from SharedPreferences to FIPS DB
 *
 * Created by Steve Ody
 *
 * VHB
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
 * Government Agency Original Software Designation: PECoach
 * Government Agency Original Software Title: PE Coach
 * User Registration Requested. Please send email
 * with your contact information to: robert.a.kayl.civ@mail.mil
 * Government Agency Point of Contact for Original Software: robert.a.kayl.civ@mail.mil
 *
 */

public class PreferenceHelper {

	/**
	 * Universal preference methods
	 */
	
	public static String getStringForKey(String key, String value) {
		String result = Global.databaseHelper.getPreference(key);
		if( (result == null) || (result.trim().equals("")) )
			return value;
		else
			return result;
	}

	private static void setStringForKey(String key, String value) {
		Global.databaseHelper.setPreference(key, value);
		PreferenceManager.getDefaultSharedPreferences(Global.context).edit().putString(key, value).apply();
	}

	public static int getIntForKey(String key, int value) {
		String val = Global.databaseHelper.getPreference(key);
		try{
			return Integer.parseInt(val);
		}
		catch(Exception ex)
		{
			return value;
		}
	}

	public static void setIntForKey(String key, int value) {
		Global.databaseHelper.setPreference(key, ""+value);
		PreferenceManager.getDefaultSharedPreferences(Global.context).edit().putInt(key, value).apply();

	}
	
	public static long getLongForKey(String key, long value) {
		String val = Global.databaseHelper.getPreference(key);
		try{
			return Long.parseLong(val);
		}
		catch(Exception ex)
		{
			return value;
		}
	}

	public static void setLongForKey(String key, long value) {
		Global.databaseHelper.setPreference(key, ""+value);
		PreferenceManager.getDefaultSharedPreferences(Global.context).edit().putLong(key, value).apply();

	}

	public static Boolean getBooleanForKey(String key, Boolean value) {
		try
		{
			String val = Global.databaseHelper.getPreference(key);
			if (val != null && val.equals("true"))
					return true;
			else if (val != null && val.equals("false"))
					return false;
			else
				return value;
		}catch(Exception ex)
		{
			return value;
		}
	}

	public static void setBooleanForKey(String key, Boolean value) {
		
		if(value)
			Global.databaseHelper.setPreference(key, "true");
		else
			Global.databaseHelper.setPreference(key, "false");
		
		PreferenceManager.getDefaultSharedPreferences(Global.context).edit().putBoolean(key, value).apply();

	}


	public static String getQuestion1(){return getStringForKey("question_one", "");}

	public static void setQuestion1(String q){setStringForKey("question_one", q);}

	public static String getQuestion2(){return getStringForKey("question_two", "");}

	public static void setQuestion2(String q){setStringForKey("question_two", q);}

	public static void setQuestion1Answer(String question1Text) {
		setStringForKey("question_one_answer", question1Text);
	}

	public static void setQuestion2Answer(String question2Text) {
		setStringForKey("question_two_answer", question2Text);
	}

	public static String getQuestion1Answer() {
		return getStringForKey("question_one_answer", "");
	}

	public static String getQuestion2Answer() {
		return getStringForKey("question_two_answer", "");
	}
	
	
	
}
