package com.t2.sudoku.db;

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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.t2.sudoku.db.SudokuContract.Sudoku;
import com.t2.sudoku.db.SudokuContract.Sudoku.Difficulty;
import com.t2.vhb.R;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteQueryBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import timber.log.Timber;

/**
 * Handles all database operations
 * 
 * @author Steve Ody (stephen.ody@tee2.org)
 **/

public class SudokuProvider extends ContentProvider 
{

	private static final String ORIGINALDB_NAME = "sudoku.db";
	public static final String DATABASE_NAME = "sudokuenc.db";
	private static final int DATABASE_VERSION = 3;

	public static final Object[] DB_LOCK = new Object[0];
	
	private Context context;
	private static SQLiteDatabase db;
	private int currentFips = 0;
	
	private static String dbPassword = "h0n3yp0t";

	private static final String TAG = "SudokuProvider";

	private static final int SUDOKU_ID = 1;
    private static final int SUDOKU_DIFFICULTY = 2;
    private static final int SUDOKU = 3;

    private static final UriMatcher sUriMatcher;

    static {
        // Initialize URI Matcher
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(SudokuContract.AUTHORITY, Sudoku.PATH, SUDOKU);
        sUriMatcher.addURI(SudokuContract.AUTHORITY, Sudoku.PATH_FOR_ID, SUDOKU_ID);
        sUriMatcher.addURI(SudokuContract.AUTHORITY, Sudoku.PATH_FOR_DIFFICULTY, SUDOKU_DIFFICULTY);
    }

	@Override
	public boolean onCreate() {
		this.context = getContext();


		SharedPreferences pref = this.context.getSharedPreferences("FIPS MODE", Context.MODE_PRIVATE);
		this.currentFips = pref.getInt(context.getString(R.string.saved_fips_version_sudoku), 0);

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
			edit.putInt(context.getString(R.string.saved_fips_version_sudoku), 2);
			edit.apply();
			currentFips = 2;
		}


		/*Reverted back to the original boolean statement */
		boolean updateOldDataToFips1 = ((originalFile.exists()) && (!databaseFile.exists())
				&& currentFips == 0);

		/*Added after our 3/8 meeting, this is what the updateOldDataToFips1 looked like after I changed it -Chris*/
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
			edit.putInt(context.getString(R.string.saved_fips_version_sudoku), 1);
			edit.apply();
			currentFips = 1;
		}

		databaseFile = this.context.getDatabasePath(DATABASE_NAME);
		boolean updateOldDataToFips2 = (currentFips == 1);

		if(updateOldDataToFips2)
		{
			encryptExistingDatabaseWithFips2();
			currentFips = 2;
		}
		//Open Database
		this.context.getDatabasePath(databaseFile.getParent()).mkdirs();


		Timber.tag("CurrentFips").v("Current Fips (Sudoku): %s", currentFips);

		OpenHelper openHelper = new OpenHelper(this.context, currentFips);
		db = openHelper.getWritableDatabase();
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
			edit.putInt(context.getString(R.string.saved_fips_version_sudoku), 1);
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
				e.printStackTrace();
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

                //We successfully updated to fips 2
                if(deleted && renamed) {
                    //Save the current fips to shared preferences
                    SharedPreferences pref = this.context.getSharedPreferences("FIPS MODE", Context.MODE_PRIVATE);
                    SharedPreferences.Editor edit = pref.edit();
                    edit.putInt(context.getString(R.string.saved_fips_version_sudoku), 2);
                    edit.apply();
                }
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
			Cursor cursor = db.rawQuery(query, null);


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
		db = openHelper.getWritableDatabase();
	}
	
	/*
     * (non-Javadoc)
     * @see android.content.ContentProvider#query(android.net.Uri,
     * java.lang.String[], java.lang.String, java.lang.String[],
     * java.lang.String)
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
		Timber.d("Query called on URI %s", uri);

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
            case SUDOKU_DIFFICULTY:
                String difficulty = uri.getPathSegments().get(Sudoku.SUDOKU_PUZZLE_DIFFICULTY_POSITION);
                builder.setTables(Sudoku.TABLE_NAME);
                builder.appendWhere(Sudoku.COL_DIFFICULTY + " = '" + difficulty + "'");
                break;
            case SUDOKU_ID:
                String id = uri.getPathSegments().get(Sudoku.SUDOKU_PUZZLE_ID_POSITION);
                builder.setTables(Sudoku.TABLE_NAME);
                builder.appendWhere(BaseColumns._ID + " = " + id);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        Cursor result = builder.query(db, projection, selection,
                selectionArgs, null, null,
                sortOrder);
        result.setNotificationUri(getContext().getContentResolver(), uri);

        return result;
    }

    /*
     * (non-Javadoc)
     * @see android.content.ContentProvider#getType(android.net.Uri)
     */
    @Override
    public String getType(Uri uri) {
        int match = sUriMatcher.match(uri);
        switch (match) {
            case SUDOKU_ID:
                return Sudoku.CONTENT_ITEM_MIME_TYPE;
            case SUDOKU_DIFFICULTY:
                return Sudoku.CONTENT_MIME_TYPE;
            default:
                return null;
        }
    }

    /*
     * (non-Javadoc)
     * @see android.content.ContentProvider#insert(android.net.Uri,
     * android.content.ContentValues)
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
		Timber.d("Insert called on URI %s", uri);

        Uri resultUri = null;
        String table = null;

        switch (sUriMatcher.match(uri)) {
            case SUDOKU:
                table = Sudoku.TABLE_NAME;
                resultUri = Sudoku.CONTENT_URI;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        long rowId = db.insert(table, null, values);

        if (rowId > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
            return resultUri.buildUpon().appendPath("" + rowId).build();
        }

        throw new android.database.SQLException("Unable to insert row into " + uri);
    }

    /*
     * (non-Javadoc)
     * @see android.content.ContentProvider#delete(android.net.Uri,
     * java.lang.String, java.lang.String[])
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
		Timber.d("Delete called on URI %s", uri);

        switch (sUriMatcher.match(uri)) {
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    /*
     * (non-Javadoc)
     * @see android.content.ContentProvider#update(android.net.Uri,
     * android.content.ContentValues, java.lang.String, java.lang.String[])
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		Timber.d("Update called on URI %s", uri);

        String table = null;
        String where = null;

        switch (sUriMatcher.match(uri)) {
            case SUDOKU_ID:
                String id = uri.getPathSegments().get(Sudoku.SUDOKU_PUZZLE_ID_POSITION);
                table = Sudoku.TABLE_NAME;
                where = BaseColumns._ID + " = " + id;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        if (where != null && selection != null) {
            selection = where + " AND " + selection;
        } else if (where != null) {
            selection = where;
        }

        int count = db.update(table, values, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }
	
	private static class OpenHelper extends SQLiteOpenHelper 
	{
		private final Context mContext;

		private static final String CREATE_SUDOKU = ""
				+ "CREATE TABLE IF NOT EXISTS " + Sudoku.TABLE_NAME + "( "
				+ Sudoku._ID + " 			INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ Sudoku.COL_COMPLETE + " 		INTEGER NOT NULL DEFAULT 0,"
				+ Sudoku.COL_CURRENT + " 		TEXT,"
				+ Sudoku.COL_DIFFICULTY + " 	TEXT NOT NULL,"
				+ Sudoku.COL_PUZZLE + " 		TEXT NOT NULL,"
				+ Sudoku.COL_TITLE + " 			TEXT NOT NULL,"
				+ Sudoku.COL_SOLUTION + " 		TEXT NOT NULL"
				+ ")";

		OpenHelper(Context context, int currentFips)
		{
			super(context, DATABASE_NAME, dbPassword, null, DATABASE_VERSION, currentFips);
			mContext = context;
		}

		@Override
		public void onCreate(SQLiteDatabase db) 
		{	
			//Shared Prefs (required for PreferenceHelper class
			String createPREFERENCES = "CREATE TABLE IF NOT EXISTS SHAREDPREFS (prefID INTEGER PRIMARY KEY AUTOINCREMENT, PREFSKEY TEXT, PREFSVALUE TEXT);";
			db.execSQL(createPREFERENCES);
			
			db.execSQL(CREATE_SUDOKU);
			db.execSQL("PRAGMA foreign_keys=ON;");
			loadData(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
		{
			db.execSQL("DROP TABLE IF EXISTS " + Sudoku.TABLE_NAME);
			onCreate(db);
		}
		
		private void loadData(SQLiteDatabase db) {
			BufferedReader in = null;
			try {
				in = new BufferedReader(new InputStreamReader(mContext.getResources().openRawResource(R.raw.sudoku), "UTF-8"));
				loadSudoku(db, in);
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						// Om nom nom
					}
				}
			}
		}

		private void loadSudoku(SQLiteDatabase db, BufferedReader in) throws IOException {
			String line;
			Sudoku.Difficulty difficulty = null;
			int count = 1;
			while ((line = in.readLine()) != null) {
				if (line.matches("[a-zA-Z].*?")) {
					count = 1;
					difficulty = Difficulty.valueOf(line);
				} else {
					ContentValues vals = new ContentValues();
					vals.put(Sudoku.COL_DIFFICULTY, difficulty.name());
					vals.put(Sudoku.COL_PUZZLE, line);
					vals.put(Sudoku.COL_TITLE, difficulty.toString() + " #" + count);
					line = in.readLine();
					vals.put(Sudoku.COL_SOLUTION, line);
					db.insert(Sudoku.TABLE_NAME, null, vals);
					count++;
				}
			}
		}
	}

}