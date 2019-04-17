/*
 * DatabaseHelper.java
 * Handles all database operations
 *
 * Created by Steve Ody
 *
 * <APPNAME>
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
package com.t2.copingcards.db;

import android.app.backup.BackupManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.provider.BaseColumns;

import com.t2.copingcards.CopingContract;
import com.t2.copingcards.CopingContract.CopingCard;
import com.t2.copingcards.CopingContract.CopingCard.CopingSkill;
import com.t2.copingcards.CopingContract.CopingCard.Symptom;
import com.t2.vhb.R;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteQueryBuilder;

import java.io.File;
import java.io.IOException;

import timber.log.Timber;

/**
 * Handles all database operations
 * 
 * @author Steve Ody (stephen.ody@tee2.org)
 **/

public class CopingProvider extends ContentProvider 
{

	private static final String ORIGINALDB_NAME = "coping_cards.db";
	private static final String DATABASE_NAME = "coping_cardsenc.db";
	private static final int DATABASE_VERSION = 8;

	private static final Object[] DB_LOCK = new Object[0];
	
	private Context context;
	private static SQLiteDatabase db;

    private int currentFips = 0;
	private static String dbPassword = "h0n3yp0t";

	private static final String TAG = "CopingCardProvider";

    // URI Pattern Match Sentinels
    private static final int COPING_CARDS = 3;
    private static final int COPING_CARDS_ID = 4;
    private static final int COPING_CARDS_COPING_SKILLS = 5;
    private static final int COPING_CARDS_COPING_SKILLS_ID = 6;
    private static final int COPING_CARDS_SYMPTOMS = 7;
    private static final int COPING_CARDS_SYMPTOMS_ID = 8;

    private static final UriMatcher sUriMatcher;

    private BackupManager mBackupManager;
    
    static {
        // Initialize URI Matcher
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        sUriMatcher.addURI(CopingContract.AUTHORITY, CopingCard.PATH,
                COPING_CARDS);
        sUriMatcher.addURI(CopingContract.AUTHORITY, CopingCard.PATH_FOR_ID,
                COPING_CARDS_ID);
        sUriMatcher.addURI(CopingContract.AUTHORITY, CopingSkill.PATH,
                COPING_CARDS_COPING_SKILLS);
        sUriMatcher
                .addURI(CopingContract.AUTHORITY, CopingSkill.PATH_FOR_ID,
                        COPING_CARDS_COPING_SKILLS_ID);
        sUriMatcher.addURI(CopingContract.AUTHORITY, Symptom.PATH,
                COPING_CARDS_SYMPTOMS);
        sUriMatcher
                .addURI(CopingContract.AUTHORITY, Symptom.PATH_FOR_ID,
                        COPING_CARDS_SYMPTOMS_ID);
    }



    public CopingProvider() {
        super();
    }


	@Override
	public boolean onCreate() {
		this.context = getContext();

		mBackupManager = new BackupManager(getContext());

        SharedPreferences pref = this.context.getSharedPreferences("FIPS MODE", Context.MODE_PRIVATE);
        this.currentFips = pref.getInt(context.getString(R.string.saved_fips_version_coping), 0);
		//algo the password to make it slightly more secure than static
		try
		{
			Signature[] sigs = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;
			dbPassword = "T2!"+sigs[0].hashCode();
		}
		catch(Exception ex){
            Timber.v("SIGNATURE ERROR");
		}

		//Encrypt old database if exists
		File originalFile = context.getDatabasePath(ORIGINALDB_NAME);
		File databaseFile = this.context.getDatabasePath(DATABASE_NAME);

        if((!originalFile.exists()) && (!databaseFile.exists())
                && currentFips == 0) {

            //Fresh app Go to fips 2
            SharedPreferences.Editor edit = pref.edit();
            edit.putInt(context.getString(R.string.saved_fips_version_coping), 2);
            edit.apply();
            currentFips = 2;
        }

        boolean updateOldDataToFips1 = ((originalFile.exists()) && (!databaseFile.exists())
                && currentFips == 0);

        boolean alreadyAtFips1 = ((!originalFile.exists()) && (databaseFile.exists())
                && currentFips == 0);

        if(updateOldDataToFips1)
        {
            try {
                encryptExistingDatabaseWithFips1();
                currentFips = 1;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (alreadyAtFips1) /*Added after our 3/8 meeting -Chris*/ {
            SharedPreferences.Editor edit = pref.edit();
            edit.putInt(context.getString(R.string.saved_fips_version_coping), 1);
            edit.apply();
            currentFips = 1;
        }


        databaseFile = this.context.getDatabasePath(DATABASE_NAME);
        boolean updateOldDataToFips2 = (currentFips == 1 && databaseFile.exists());

        if(updateOldDataToFips2)
        {
	        encryptExistingDatabaseWithFips2();
	        currentFips = 2;
        }

		//Open Database
		this.context.getDatabasePath(databaseFile.getParent()).mkdirs();

        Timber.tag("CurrentFips").v("Current Fips (Coping): %s", currentFips);
		OpenHelper openHelper = new OpenHelper(this.context, currentFips);
		CopingProvider.db = openHelper.getWritableDatabase();
		return true;
	}

	private void encryptExistingDatabaseWithFips1() throws IOException
	{
		//Load FIPS libraries
		SQLiteDatabase.loadLibs(context);
        com.t2.fcads.FipsWrapper.getInstance(this.context).doFIPSmode();

		File originalFile = context.getDatabasePath(ORIGINALDB_NAME);

		if (originalFile.exists())
		{
			File newFile = File.createTempFile("sqlcipherutils", "tmp",	context.getCacheDir());

			SQLiteDatabase db =	SQLiteDatabase.openDatabase(originalFile.getAbsolutePath(),"", null, SQLiteDatabase.OPEN_READWRITE);

			db.rawExecSQL(String.format("ATTACH DATABASE '%s' AS encrypted KEY '%s';", newFile, dbPassword));

			db.rawExecSQL("SELECT sqlcipher_export('encrypted')");
			db.rawExecSQL("DETACH DATABASE encrypted;");

			db.close();


			db=SQLiteDatabase.openDatabase(newFile.getAbsolutePath(), dbPassword, null, SQLiteDatabase.OPEN_READWRITE);

			db.setVersion(DATABASE_VERSION);
			db.close();

            originalFile.delete();
            newFile.renameTo(context.getDatabasePath(DATABASE_NAME));

            SharedPreferences pref = this.context.getSharedPreferences("FIPS MODE", Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = pref.edit();
            edit.putInt(context.getString(R.string.saved_fips_version_coping), 1);
            edit.apply();
		}
	}

    private void encryptExistingDatabaseWithFips2() {
        //Load FIPS libraries
        SQLiteDatabase.loadLibs(context);
        com.t2.fcads.FipsWrapper.getInstance(this.context).doFIPSmode();

        File originalFile = context.getDatabasePath(DATABASE_NAME);
        if (originalFile.exists()) {
            File newFile = null;
            try {
                newFile = File.createTempFile("sqlcipherutilsencrypt", "tmp", context.getCacheDir());
            } catch (IOException e) {
                Timber.e(e);
            }
            SQLiteDatabase db = SQLiteDatabase.openDatabase(originalFile.getAbsolutePath(),
                    dbPassword, null, SQLiteDatabase.OPEN_READWRITE);

            db.rawExecSQL(String.format("PRAGMA key = '%s'",dbPassword));
            db.rawExecSQL(String.format("ATTACH DATABASE '%s' AS encrypted KEY '%s';",
                    newFile, dbPassword));
            db.rawExecSQL("PRAGMA encrypted.cipher = 'aes-256-gcm'");
            db.rawExecSQL("SELECT sqlcipher_export('encrypted')");
            db.rawExecSQL("DETACH DATABASE encrypted");
            db.close();

            boolean deleted =originalFile.delete();
            if (newFile != null) {
                boolean renamed = newFile.renameTo(context.getDatabasePath(DATABASE_NAME));

                //Save the current fips to shared preferences
                SharedPreferences pref = this.context.getSharedPreferences("FIPS MODE", Context.MODE_PRIVATE);
                SharedPreferences.Editor edit = pref.edit();
                edit.putInt(context.getString(R.string.saved_fips_version_coping), 2);
                edit.apply();
            }

        }
    }

	/**
	 * Used to clean all user-entered data before being put into database
	 * @param input string to clean
	 * @return cleaned string
	 */
	public static String scrubInput(String input)
	{
		//add more reserved SQL characters to prevent a sql injection attack or just a crash
        return input.replace("'", "''");
	}

	/**
	 * Handles entry of preferences table
	 */
	public boolean setPreference(String inKey, String inValue)
	{
		try
		{
			String DELSQL = "DELETE FROM SHAREDPREFS where PREFSKEY ='" + inKey + "'";
			db.execSQL(DELSQL);

			ContentValues insertValues = new ContentValues();
			insertValues.put("PREFSKEY", inKey);
			insertValues.put("PREFSVALUE", inValue);
			db.insert("SHAREDPREFS", null, insertValues);
			return true;
		}
		catch(Exception ex)
		{
			return false;
		}
	}

	/**
	 * Returns string based preference data
	 */
	public String getPreference(String inKey)
	{
		String result = "";

		try
		{
			String query = "select PREFSVALUE from SHAREDPREFS where PREFSKEY = '" + inKey + "'";
			Cursor cursor = CopingProvider.db.rawQuery(query, null);


			if (cursor.moveToFirst()) 
			{
				result = cursor.getString(0);
			}

			cursor.close();		
		}
		catch(Exception ignored)
		{}
		
		return result;
	}

	public void refreshDatabase() {
		OpenHelper openHelper = new OpenHelper(this.context, currentFips);
		CopingProvider.db = openHelper.getWritableDatabase();
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
        Timber.d("Query called on URI %s", uri);

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        String id = null;

        switch (sUriMatcher.match(uri)) {
            case COPING_CARDS:
                builder.setTables(CopingCard.TABLE_NAME);
                break;
            case COPING_CARDS_ID:
                builder.setTables(CopingCard.TABLE_NAME);
                id = uri.getPathSegments().get(CopingCard.COPING_CARD_ID_POSITION);
                builder.appendWhere(BaseColumns._ID + " = " + id);
                break;
            case COPING_CARDS_COPING_SKILLS:
                builder.setTables(CopingSkill.TABLE_NAME);
                id = uri.getPathSegments().get(CopingCard.COPING_CARD_ID_POSITION);
                builder.appendWhere(CopingSkill.COL_COPING_CARD_ID + " = "
                        + id);
                break;
            case COPING_CARDS_COPING_SKILLS_ID:
                builder.setTables(CopingSkill.TABLE_NAME);
                id = uri.getPathSegments().get(CopingCard.COPING_CARD_ID_POSITION);
                String copingSkillId = uri.getPathSegments().get(
                        CopingSkill.COPING_SKILL_ID_POSITION);
                builder.appendWhere(BaseColumns._ID + " = "
                        + copingSkillId);
                builder.appendWhere(CopingSkill.COL_COPING_CARD_ID + " = "
                        + id);
                break;
            case COPING_CARDS_SYMPTOMS:
                builder.setTables(Symptom.TABLE_NAME);
                id = uri.getPathSegments().get(CopingCard.COPING_CARD_ID_POSITION);
                builder.appendWhere(Symptom.COL_COPING_CARD_ID + " = "
                        + id);
                break;
            case COPING_CARDS_SYMPTOMS_ID:
                builder.setTables(Symptom.TABLE_NAME);
                id = uri.getPathSegments().get(CopingCard.COPING_CARD_ID_POSITION);
                String symptomId = uri.getPathSegments().get(
                        Symptom.COPING_SYMPTOM_POSITION);
                builder.appendWhere(BaseColumns._ID + " = "
                        + symptomId);
                builder.appendWhere(Symptom.COL_COPING_CARD_ID + " = "
                        + id);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        Cursor result = builder.query(db,
                projection, selection, selectionArgs, null, null, sortOrder);
        result.setNotificationUri(getContext().getContentResolver(), uri);

        return result;
	}

	@Override
	public String getType(Uri uri) {
		int match = sUriMatcher.match(uri);
        switch (match) {
            case COPING_CARDS:
                return CopingCard.CONTENT_MIME_TYPE;
            case COPING_CARDS_ID:
                return CopingCard.CONTENT_ITEM_MIME_TYPE;
            case COPING_CARDS_COPING_SKILLS:
                return CopingSkill.CONTENT_MIME_TYPE;
            case COPING_CARDS_COPING_SKILLS_ID:
                return CopingSkill.CONTENT_ITEM_MIME_TYPE;
            case COPING_CARDS_SYMPTOMS:
                return Symptom.CONTENT_MIME_TYPE;
            case COPING_CARDS_SYMPTOMS_ID:
                return Symptom.CONTENT_ITEM_MIME_TYPE;
            default:
                return null;
        }
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
        Timber.d("Insert called on URI %s", uri);

        Uri resultUri = null;
        String table = null;
        String copingCardId = null;

        switch (sUriMatcher.match(uri)) {
            case COPING_CARDS:
                table = CopingCard.TABLE_NAME;
                resultUri = CopingCard.CONTENT_URI;
                break;
            case COPING_CARDS_COPING_SKILLS:
                copingCardId = uri.getPathSegments().get(
                        CopingCard.COPING_CARD_ID_POSITION);
                values.put(CopingSkill.COL_COPING_CARD_ID, copingCardId);

                table = CopingSkill.TABLE_NAME;
                resultUri = CopingSkill.getContentUri(Long
                        .parseLong(copingCardId));
                break;
            case COPING_CARDS_SYMPTOMS:
                copingCardId = uri.getPathSegments().get(
                        CopingCard.COPING_CARD_ID_POSITION);
                values.put(Symptom.COL_COPING_CARD_ID, copingCardId);

                table = Symptom.TABLE_NAME;
                resultUri = Symptom.getContentUri(Long
                        .parseLong(copingCardId));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        long rowId = 0;
        synchronized (DB_LOCK) {
            rowId = db.insert(table, null, values);
        }

        if (rowId > 0) {
            mBackupManager.dataChanged();
            getContext().getContentResolver().notifyChange(uri, null);
            return resultUri.buildUpon().appendPath("" + rowId).build();
        }

        throw new android.database.SQLException("Unable to insert row into "
                + uri);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
        Timber.d("Delete called on URI %s", uri);

        String table = null;
        String where = null;
        String id = null;

        switch (sUriMatcher.match(uri)) {
            case COPING_CARDS:
                table = CopingCard.TABLE_NAME;
                break;
            case COPING_CARDS_ID:
                id = uri.getPathSegments().get(CopingCard.COPING_CARD_ID_POSITION);

                where = BaseColumns._ID + " = " + id;
                table = CopingCard.TABLE_NAME;
                break;
            case COPING_CARDS_COPING_SKILLS:
                id = uri.getPathSegments().get(CopingCard.COPING_CARD_ID_POSITION);

                where = CopingSkill.COL_COPING_CARD_ID + " = " + id;
                table = CopingSkill.TABLE_NAME;
                break;
            case COPING_CARDS_COPING_SKILLS_ID:
                id = uri.getPathSegments().get(CopingCard.COPING_CARD_ID_POSITION);
                String copingSkillId = uri.getPathSegments().get(
                        CopingSkill.COPING_SKILL_ID_POSITION);

                where = CopingSkill.COL_COPING_CARD_ID + " = " + id + " AND "
                        + BaseColumns._ID + " = " + copingSkillId;
                table = CopingSkill.TABLE_NAME;
                break;
            case COPING_CARDS_SYMPTOMS:
                id = uri.getPathSegments().get(CopingCard.COPING_CARD_ID_POSITION);

                where = Symptom.COL_COPING_CARD_ID + " = " + id;
                table = Symptom.TABLE_NAME;
                break;
            case COPING_CARDS_SYMPTOMS_ID:
                id = uri.getPathSegments().get(CopingCard.COPING_CARD_ID_POSITION);
                String symptomId = uri.getPathSegments().get(
                        Symptom.COPING_SYMPTOM_POSITION);

                where = Symptom.COL_COPING_CARD_ID + " = " + id + " AND "
                        + BaseColumns._ID + " = " + symptomId;
                table = Symptom.TABLE_NAME;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        if (where != null && selection != null) {
            where += " AND " + selection;
        } else if (where == null && selection != null) {
            where = selection;
        }

        int count = 0;
        synchronized (DB_LOCK) {
            count = db.delete(table, where, selectionArgs);
        }
        mBackupManager.dataChanged();
        getContext().getContentResolver().notifyChange(uri, null);

        return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
        Timber.d("Update called on URI %s", uri);

        String table = null;
        String where = null;
        String id = null;
        switch (sUriMatcher.match(uri)) {
            case COPING_CARDS:
                table = CopingCard.TABLE_NAME;
                break;
            case COPING_CARDS_ID:
                id = uri.getPathSegments().get(CopingCard.COPING_CARD_ID_POSITION);

                where = BaseColumns._ID + " = " + id;
                table = CopingCard.TABLE_NAME;
                break;
            case COPING_CARDS_COPING_SKILLS:
                id = uri.getPathSegments().get(CopingCard.COPING_CARD_ID_POSITION);

                where = CopingSkill.COL_COPING_CARD_ID + " = " + id;
                table = CopingSkill.TABLE_NAME;
                break;
            case COPING_CARDS_COPING_SKILLS_ID:
                id = uri.getPathSegments().get(CopingCard.COPING_CARD_ID_POSITION);
                String copingSkillId = uri.getPathSegments().get(
                        CopingSkill.COPING_SKILL_ID_POSITION);

                where = CopingSkill.COL_COPING_CARD_ID + " = " + id + " AND "
                        + BaseColumns._ID + " = " + copingSkillId;
                table = CopingSkill.TABLE_NAME;
                break;
            case COPING_CARDS_SYMPTOMS:
                id = uri.getPathSegments().get(CopingCard.COPING_CARD_ID_POSITION);

                where = Symptom.COL_COPING_CARD_ID + " = " + id;
                table = Symptom.TABLE_NAME;
                break;
            case COPING_CARDS_SYMPTOMS_ID:
                id = uri.getPathSegments().get(CopingCard.COPING_CARD_ID_POSITION);
                String symptomId = uri.getPathSegments().get(
                        Symptom.COPING_SYMPTOM_POSITION);

                where = Symptom.COL_COPING_CARD_ID + " = " + id + " AND "
                        + BaseColumns._ID + " = " + symptomId;
                table = Symptom.TABLE_NAME;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        if (where != null && selection != null) {
            selection = where + " AND " + selection;
        } else if (where != null) {
            selection = where;
        }

        int count = 0;
        synchronized (DB_LOCK) {
            count = db.update(table, values, selection, selectionArgs);
        }
        mBackupManager.dataChanged();
        getContext().getContentResolver().notifyChange(uri, null);

        return count;
	}

	@Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        int numInserted = 0;
        String table = null;
        String id = null;

        Timber.d("Bulk Insert called on URI %s", uri);

        int uriType = sUriMatcher.match(uri);

        switch (uriType) {
            case COPING_CARDS_COPING_SKILLS:
                id = uri.getPathSegments().get(CopingCard.COPING_CARD_ID_POSITION);
                for (ContentValues val : values) {
                    val.put(CopingSkill.COL_COPING_CARD_ID, id);
                }
                table = CopingSkill.TABLE_NAME;
                break;
            case COPING_CARDS_SYMPTOMS:
                id = uri.getPathSegments().get(CopingCard.COPING_CARD_ID_POSITION);
                for (ContentValues val : values) {
                    val.put(Symptom.COL_COPING_CARD_ID, id);
                }
                table = Symptom.TABLE_NAME;
                break;
        }
        synchronized (DB_LOCK) {
            db.beginTransaction();
            try {
                for (ContentValues cv : values) {
                    long newID = db.insertOrThrow(table, null, cv);
                    if (newID <= 0) {
                        throw new SQLException("Failed to insert row into " + uri);
                    }
                }
                db.setTransactionSuccessful();
                getContext().getContentResolver().notifyChange(uri, null);
                numInserted = values.length;
            } finally {
                db.endTransaction();
            }

            return numInserted;
        }

    }
	
	private static class OpenHelper extends SQLiteOpenHelper 
	{
		private static final String CREATE_COPING_CARDS = "" + "CREATE TABLE IF NOT EXISTS "
	            + CopingCard.TABLE_NAME + "( " + CopingCard._ID
	            + "                 INTEGER PRIMARY KEY AUTOINCREMENT, "
	            + CopingCard.COL_PROBLEM_AREA + " TEXT NOT NULL " + ")";

	    private static final String CREATE_SYMPTOMS = "" + "CREATE TABLE IF NOT EXISTS "
	            + Symptom.TABLE_NAME + "( " + Symptom._ID
	            + "                     INTEGER PRIMARY KEY AUTOINCREMENT, "
	            + Symptom.COL_SYMPTOM_TEXT + "         TEXT NOT NULL, "
	            + Symptom.COL_COPING_CARD_ID + "  INTEGER NOT NULL, "
	            + "FOREIGN KEY(coping_card_id) REFERENCES "
	            + CopingCard.TABLE_NAME + "(_id) ON DELETE CASCADE" + ")";

	    private static final String CREATE_COPING_SKILLS = "" + "CREATE TABLE IF NOT EXISTS "
	            + CopingSkill.TABLE_NAME + "( " + CopingSkill._ID
	            + "             		INTEGER PRIMARY KEY AUTOINCREMENT, "
	            + CopingSkill.COL_SKILL_TEXT + " 		TEXT NOT NULL, "
	            + CopingSkill.COL_COPING_CARD_ID + "	INTEGER NOT NULL, "
	            + "FOREIGN KEY(coping_card_id) REFERENCES "
	            + CopingCard.TABLE_NAME + "(_id) ON DELETE CASCADE" + ")";

		OpenHelper(Context context, int currentFips)
		{
			super(context, DATABASE_NAME, dbPassword, null, DATABASE_VERSION, currentFips);
		}

		@Override
		public void onCreate(SQLiteDatabase db) 
		{	
			//Shared Prefs (required for PreferenceHelper class
			String createPREFERENCES = "CREATE TABLE IF NOT EXISTS SHAREDPREFS (prefID INTEGER PRIMARY KEY AUTOINCREMENT, PREFSKEY TEXT, PREFSVALUE TEXT);";
			db.execSQL(createPREFERENCES);
			
			db.execSQL(CREATE_COPING_CARDS);
	        db.execSQL(CREATE_COPING_SKILLS);
	        db.execSQL(CREATE_SYMPTOMS);
	        db.execSQL("PRAGMA foreign_keys=ON;");

	        // final ContentValues vals = new ContentValues();
	        // final LoremIpsum ipsum = new LoremIpsum();
	        // final Random rnd = new Random();
	        // long id = 0;
	        // for (int i = 0; i < 20; i++) {
	        // vals.clear();
	        // vals.put(CopingCards.COL_FEELING, ipsum.words(rnd.nextInt(2) +
	        // 1).replace(' ', ','));
	        // vals.put(CopingCards.COL_NEGATIVE_BELIEF, ipsum.words(10));
	        // id = db.insert(CopingCards.TABLE_NAME, null, vals);
	        //
	        // for (int j = 0; j < rnd.nextInt(4) + 1; j++) {
	        // vals.clear();
	        // vals.put(PositiveResponses.COL_COPING_CARD_ID, id);
	        // vals.put(PositiveResponses.COL_BELIEF_TEXT, ipsum.words(10));
	        // db.insert(PositiveResponses.TABLE_NAME, null, vals);
	        // }
	        // }	

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
		{
			 dropAll(db);
		     onCreate(db);
		}
		
		private void dropAll(SQLiteDatabase db) {
	        db.execSQL("DROP TABLE IF EXISTS SHAREDPREFS");
	        db.execSQL("DROP TABLE IF EXISTS " + Symptom.TABLE_NAME);
	        db.execSQL("DROP TABLE IF EXISTS " + CopingSkill.TABLE_NAME);
	        db.execSQL("DROP TABLE IF EXISTS " + CopingCard.TABLE_NAME);
	    }
	}

}