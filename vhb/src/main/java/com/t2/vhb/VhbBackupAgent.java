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
package com.t2.vhb;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.ParcelFileDescriptor;

import com.t2.mahjong.db.MahjongProvider;
import com.t2.sudoku.db.SudokuProvider;
import com.t2.vhb.db.VhbProvider;

import java.io.IOException;


/**
 * @author wes
 * 
 */
public class VhbBackupAgent extends BackupAgentHelper {

	private static final String KEY_DB = VhbProvider.DATABASE_NAME;
	private static final String KEY_SUDOKU_DB = SudokuProvider.DATABASE_NAME;
	private static final String KEY_MAHJONG_DB = MahjongProvider.DATABASE_NAME;
	private static final String KEY_PREFS = "prefs";

	@Override
	public void onCreate() {
		super.onCreate();
		FileBackupHelper dbHelper = new FileBackupHelper(this, "../databases/" + KEY_DB);
		addHelper(KEY_DB, dbHelper);

		dbHelper = new FileBackupHelper(this, "../databases/" + SudokuProvider.DATABASE_NAME);
		addHelper(KEY_SUDOKU_DB, dbHelper);

		dbHelper = new FileBackupHelper(this, "../databases/" + MahjongProvider.DATABASE_NAME);
		addHelper(KEY_MAHJONG_DB, dbHelper);

		SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, getPackageName() + "_preferences");
		addHelper(KEY_PREFS, helper);
	}

	@Override
	public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
			ParcelFileDescriptor newState) throws IOException {
		synchronized (VhbProvider.DB_LOCK) {
			super.onBackup(oldState, data, newState);
		}
	}

	@Override
	public void onRestore(BackupDataInput data, int appVersionCode,
			ParcelFileDescriptor newState) throws IOException {

		synchronized (VhbProvider.DB_LOCK) {
			super.onRestore(data, appVersionCode, newState);
		}
	}

}
