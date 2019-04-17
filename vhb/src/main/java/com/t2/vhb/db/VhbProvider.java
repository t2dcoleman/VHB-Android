package com.t2.vhb.db;

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

import android.app.backup.BackupManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;

import com.t2.vhb.R;
import com.t2.vhb.db.VhbContract.ActivityIdea;
import com.t2.vhb.db.VhbContract.Media;
import com.t2.vhb.db.VhbContract.Quotes;
import com.t2.vhb.db.VhbContract.SupportContacts;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteQueryBuilder;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import timber.log.Timber;

/**
 * Handles all database operations
 * 
 * @author Steve Ody (stephen.ody@tee2.org)
 **/

public class VhbProvider extends ContentProvider 
{

	private static final String ORIGINALDB_NAME = "vhb.db";
	public static final String DATABASE_NAME = "vhbenc.db";
	private static final int DATABASE_VERSION = 90;

	public static final Object[] DB_LOCK = new Object[0];
	
	private Context context;
	public static SQLiteDatabase db;
	private int currentFips = 0;
	private static String dbPassword = "h0n3yp0t";

	private static final String TAG = "VirtualHopeBoxProvider";

	// URI Pattern Match Sentinels
    private static final int SUPPORT_CONTACTS = 1;
    private static final int SUPPORT_CONTACTS_ID = 2;
    private static final int SUPPORT_CONTACTS_LOOKUP = 7;
    private static final int QUOTES = 8;
    private static final int QUOTES_ID = 9;
    private static final int QUOTES_AUTHOR = 10;
    private static final int MEDIA = 14;
    private static final int MEDIA_FOR_TYPE = 11;
    private static final int MEDIA_FOR_TYPE_AND_ID = 12;
    private static final int MEDIA_FOR_TYPE_AND_EXTERNAL_ID = 13;
    private static final int ACTIVITY_IDEAS = 15;
    private static final int ACTIVITY_IDEAS_AND_ID = 16;
    private static final int RESEARCH_LOG = 17;

    private static final UriMatcher sUriMatcher;
    private BackupManager mBackupManager;
    
    static {
        // Initialize URI Matcher
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        sUriMatcher.addURI(VhbContract.AUTHORITY, SupportContacts.PATH, SUPPORT_CONTACTS);
        sUriMatcher.addURI(VhbContract.AUTHORITY, SupportContacts.PATH_FOR_ID, SUPPORT_CONTACTS_ID);
        sUriMatcher.addURI(VhbContract.AUTHORITY, SupportContacts.PATH_FOR_LOOKUP, SUPPORT_CONTACTS_LOOKUP);

        sUriMatcher.addURI(VhbContract.AUTHORITY, Quotes.PATH, QUOTES);
        sUriMatcher.addURI(VhbContract.AUTHORITY, Quotes.PATH_FOR_ID, QUOTES_ID);
        sUriMatcher.addURI(VhbContract.AUTHORITY, Quotes.PATH_FOR_AUTHOR, QUOTES_AUTHOR);

        sUriMatcher.addURI(VhbContract.AUTHORITY, Media.PATH_FOR_TYPE, MEDIA_FOR_TYPE);
        sUriMatcher.addURI(VhbContract.AUTHORITY, Media.PATH_FOR_TYPE_AND_ID, MEDIA_FOR_TYPE_AND_ID);
        sUriMatcher.addURI(VhbContract.AUTHORITY, Media.PATH_FOR_TYPE_AND_EXTERNAL_ID, MEDIA_FOR_TYPE_AND_EXTERNAL_ID);

        sUriMatcher.addURI(VhbContract.AUTHORITY, Media.PATH, MEDIA);

        sUriMatcher.addURI(VhbContract.AUTHORITY, ActivityIdea.PATH, ACTIVITY_IDEAS);
        sUriMatcher.addURI(VhbContract.AUTHORITY, ActivityIdea.PATH_FOR_ID, ACTIVITY_IDEAS_AND_ID);

    }

	@Override
	public boolean onCreate() {
		this.context = getContext();
		mBackupManager = new BackupManager(getContext());

        SharedPreferences pref = this.context.getSharedPreferences("FIPS MODE", Context.MODE_PRIVATE);
        this.currentFips = pref.getInt(context.getString(R.string.saved_fips_version), 0);
        
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
            edit.putInt(context.getString(R.string.saved_fips_version), 2);
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
            edit.putInt(context.getString(R.string.saved_fips_version), 1);
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


        Timber.v("Current Fips (VHB): %s", currentFips);
        OpenHelper openHelper = new OpenHelper(this.context, currentFips);
		db = openHelper.getWritableDatabase();
		
		if(!getPreference("importPrefs").equalsIgnoreCase("false")) {
			recoverSharedPreferences();
			setPreference("importPrefs", "false");
		}
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
            edit.putInt(context.getString(R.string.saved_fips_version), 1);
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
                edit.putInt(context.getString(R.string.saved_fips_version), 2);
                edit.apply();
            }

        }
    }

	private void recoverSharedPreferences() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Map<String,?> values = prefs.getAll();
		for(Entry<String, ?> entry : values.entrySet()) {
			String key = entry.getKey();
			if(entry.getValue() instanceof Boolean) {
				Boolean bool = (Boolean) entry.getValue();
				setPreference(key, bool ? "true" : "false");
			}
			else {
				setPreference(key, String.valueOf(entry.getValue()));
			}
		}
		
		prefs.edit().clear().apply();
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


        String query = "select PREFSVALUE from SHAREDPREFS where PREFSKEY = '" + inKey + "'";
		try (Cursor cursor = db.rawQuery(query, null))
		{


			if (cursor.moveToFirst()) 
			{
				result = cursor.getString(0);
			}

        }
		catch(Exception ignored){}
		
		return result;
	}

	public void clearPreferences()
	{
		String clearPREFERENCES = "delete from SHAREDPREFS;";
		db.execSQL(clearPREFERENCES);
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
    public Cursor query(@NotNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Timber.d("Query called on URI %s", uri);

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
            case SUPPORT_CONTACTS:
                builder.setTables(SupportContacts.TABLE_NAME);
                break;
            case SUPPORT_CONTACTS_ID:
                builder.setTables(SupportContacts.TABLE_NAME);
                String id = uri.getPathSegments().get(SupportContacts.SUPPORT_CONTACT_ID_POSITION);
                builder.appendWhere(BaseColumns._ID + " = " + id);
                break;
            case SUPPORT_CONTACTS_LOOKUP:
                builder.setTables(SupportContacts.TABLE_NAME);
                String lookupKey = uri.getPathSegments().get(SupportContacts.SUPPORT_CONTACT_LOOKUP_KEY_POSITION);
                builder.appendWhere(SupportContacts.COL_LOOKUP_KEY + " = '" + lookupKey + "'");
                break;
            case QUOTES:
                builder.setTables(Quotes.TABLE_NAME);
                break;
            case QUOTES_ID:
                id = uri.getPathSegments().get(Quotes.QUOTE_ID_POSITION);
                builder.setTables(Quotes.TABLE_NAME);
                builder.appendWhere(BaseColumns._ID + " = " + id);
                break;
            case QUOTES_AUTHOR:
                String author = uri.getPathSegments().get(Quotes.QUOTE_AUTHOR_POSITION);
                builder.setTables(Quotes.TABLE_NAME);
                builder.appendWhere(Quotes.COL_AUTHOR + " LIKE '" + author + "%'");
                break;
            case MEDIA_FOR_TYPE:
                String mediaType = uri.getPathSegments().get(Media.MEDIA_TYPE_POSITION);
                builder.setTables(Media.TABLE_NAME);
                builder.appendWhere(Media.COL_MEDIA_TYPE + " = '" + mediaType + "'");
                break;
            case MEDIA_FOR_TYPE_AND_ID:
                mediaType = uri.getPathSegments().get(Media.MEDIA_TYPE_POSITION);
                id = uri.getPathSegments().get(Media.MEDIA_ID_POSITION);
                builder.setTables(Media.TABLE_NAME);
                builder.appendWhere(Media.COL_MEDIA_TYPE + " = '" + mediaType + "'");
                builder.appendWhere(BaseColumns._ID + " = " + id);
                break;
            case MEDIA_FOR_TYPE_AND_EXTERNAL_ID:
                mediaType = uri.getPathSegments().get(Media.MEDIA_TYPE_POSITION);
                long extId = Long.parseLong(uri.getPathSegments().get(Media.MEDIA_EXTERNAL_ID_POSITION));
                builder.setTables(Media.TABLE_NAME);
                builder.appendWhere(Media.COL_MEDIA_TYPE + " = '" + mediaType + "' AND " + Media.COL_EXTERNAL_ID
                        + " = " + extId);
                break;
            case MEDIA:
                builder.setTables(Media.TABLE_NAME);
                break;
            case ACTIVITY_IDEAS:
                builder.setTables(ActivityIdea.TABLE_NAME);
                break;
            case ACTIVITY_IDEAS_AND_ID:
                id = uri.getPathSegments().get(ActivityIdea.ACTIVITY_IDEA_ID_POSITION);
                builder.setTables(ActivityIdea.TABLE_NAME);
                builder.appendWhere(BaseColumns._ID + " = " + id);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        Cursor result = builder.query(db, projection, selection, selectionArgs, null, null,
                sortOrder);
        result.setNotificationUri(getContext().getContentResolver(), uri);


        return result;
    }

    /*
     * (non-Javadoc)
     * @see android.content.ContentProvider#getType(android.net.Uri)
     */
    @Override
    public String getType(@NotNull Uri uri) {
        int match = sUriMatcher.match(uri);
        switch (match) {
            case SUPPORT_CONTACTS:
                return SupportContacts.CONTENT_MIME_TYPE;
            case SUPPORT_CONTACTS_ID:
            case SUPPORT_CONTACTS_LOOKUP:
                return SupportContacts.CONTENT_ITEM_MIME_TYPE;
            case QUOTES:
            case QUOTES_AUTHOR:
                return Quotes.CONTENT_MIME_TYPE;
            case QUOTES_ID:
                return Quotes.CONTENT_ITEM_MIME_TYPE;
            case MEDIA_FOR_TYPE:
                String mediaType = uri.getPathSegments().get(Media.MEDIA_TYPE_POSITION);
                return Media.getContentMimeType(mediaType);
            case MEDIA_FOR_TYPE_AND_ID:
                mediaType = uri.getPathSegments().get(Media.MEDIA_TYPE_POSITION);
                return Media.getItemContentMimeType(mediaType);
            case MEDIA_FOR_TYPE_AND_EXTERNAL_ID:
                mediaType = uri.getPathSegments().get(Media.MEDIA_TYPE_POSITION);
                return Media.getItemContentMimeType(mediaType);
            case MEDIA:
                return Media.CONTENT_MIME_TYPE;
            case ACTIVITY_IDEAS:
                return ActivityIdea.CONTENT_MIME_TYPE;
            case ACTIVITY_IDEAS_AND_ID:
                return ActivityIdea.CONTENT_ITEM_MIME_TYPE;
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
            case SUPPORT_CONTACTS:
                table = SupportContacts.TABLE_NAME;
                resultUri = SupportContacts.CONTENT_URI;
                break;
            case QUOTES:
                table = Quotes.TABLE_NAME;
                resultUri = Quotes.CONTENT_URI;
                break;
            case MEDIA_FOR_TYPE:
                String mediaType = uri.getPathSegments().get(Media.MEDIA_TYPE_POSITION);
                values.put(Media.COL_MEDIA_TYPE, mediaType);
                table = Media.TABLE_NAME;
                if (!values.containsKey(Media.COL_LOCAL_TITLE)) {
                    values.put(Media.COL_LOCAL_TITLE, "");
                }
                resultUri = Media.getContentUri(mediaType);
                break;
            case ACTIVITY_IDEAS:
                table = ActivityIdea.TABLE_NAME;
                resultUri = ActivityIdea.CONTENT_URI;
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

        String table = null;
        String where = null;

        switch (sUriMatcher.match(uri)) {
            case SUPPORT_CONTACTS:
                table = SupportContacts.TABLE_NAME;
                break;
            case SUPPORT_CONTACTS_ID:
                String id = uri.getPathSegments().get(SupportContacts.SUPPORT_CONTACT_ID_POSITION);

                where = BaseColumns._ID + " = " + id;
                table = SupportContacts.TABLE_NAME;
                break;
            case SUPPORT_CONTACTS_LOOKUP:
                String lookupKey = uri.getPathSegments().get(SupportContacts.SUPPORT_CONTACT_LOOKUP_KEY_POSITION);

                where = SupportContacts.COL_LOOKUP_KEY + " = '" + lookupKey + "' ";
                table = SupportContacts.TABLE_NAME;
                break;
            case QUOTES:
                table = Quotes.TABLE_NAME;
                break;
            case QUOTES_ID:
                id = uri.getPathSegments().get(Quotes.QUOTE_ID_POSITION);
                table = Quotes.TABLE_NAME;
                where = BaseColumns._ID + " = " + id;
                break;
            case QUOTES_AUTHOR:
                String author = uri.getPathSegments().get(Quotes.QUOTE_AUTHOR_POSITION);
                table = Quotes.TABLE_NAME;
                where = Quotes.QUOTE_AUTHOR_POSITION + " LIKE '" + author + "%'";
                break;
            case MEDIA_FOR_TYPE:
                String mediaType = uri.getPathSegments().get(Media.MEDIA_TYPE_POSITION);
                table = Media.TABLE_NAME;
                where = Media.COL_MEDIA_TYPE + " = '" + mediaType + "'";
                break;
            case MEDIA_FOR_TYPE_AND_ID:
                mediaType = uri.getPathSegments().get(Media.MEDIA_TYPE_POSITION);
                id = uri.getPathSegments().get(Media.MEDIA_ID_POSITION);
                table = Media.TABLE_NAME;
                where = Media.COL_MEDIA_TYPE + " = '" + mediaType + "' AND " + BaseColumns._ID + " = " + id;
                break;
            case MEDIA_FOR_TYPE_AND_EXTERNAL_ID:
                mediaType = uri.getPathSegments().get(Media.MEDIA_TYPE_POSITION);
                long extId = Long.parseLong(uri.getPathSegments().get(Media.MEDIA_EXTERNAL_ID_POSITION));
                table = Media.TABLE_NAME;
                where = Media.COL_MEDIA_TYPE + " = '" + mediaType + "' AND " + Media.COL_EXTERNAL_ID + " = " + extId;
                break;
            case ACTIVITY_IDEAS_AND_ID:
                id = uri.getPathSegments().get(ActivityIdea.ACTIVITY_IDEA_ID_POSITION);
                table = ActivityIdea.TABLE_NAME;
                where = BaseColumns._ID + " = " + id;
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
            case SUPPORT_CONTACTS:
                table = SupportContacts.TABLE_NAME;
                break;
            case SUPPORT_CONTACTS_ID:
                String id = uri.getPathSegments().get(SupportContacts.SUPPORT_CONTACT_ID_POSITION);

                where = BaseColumns._ID + " = " + id;
                table = SupportContacts.TABLE_NAME;
                break;
            case SUPPORT_CONTACTS_LOOKUP:
                String lookupKey = uri.getPathSegments().get(SupportContacts.SUPPORT_CONTACT_LOOKUP_KEY_POSITION);

                where = SupportContacts.COL_LOOKUP_KEY + " = '" + lookupKey + "' ";
                table = SupportContacts.TABLE_NAME;
                break;
            case QUOTES:
                table = Quotes.TABLE_NAME;
                break;
            case QUOTES_ID:
                id = uri.getPathSegments().get(Quotes.QUOTE_ID_POSITION);
                table = Quotes.TABLE_NAME;
                where = BaseColumns._ID + " = " + id;
                break;
            case QUOTES_AUTHOR:
                String author = uri.getPathSegments().get(Quotes.QUOTE_AUTHOR_POSITION);
                table = Quotes.TABLE_NAME;
                where = Quotes.QUOTE_AUTHOR_POSITION + " LIKE '" + author + "%'";
                break;
            case MEDIA_FOR_TYPE:
                String mediaType = uri.getPathSegments().get(Media.MEDIA_TYPE_POSITION);
                table = Media.TABLE_NAME;
                where = Media.COL_MEDIA_TYPE + " = '" + mediaType + "'";
                break;
            case MEDIA_FOR_TYPE_AND_ID:
                mediaType = uri.getPathSegments().get(Media.MEDIA_TYPE_POSITION);
                id = uri.getPathSegments().get(Media.MEDIA_ID_POSITION);
                table = Media.TABLE_NAME;
                where = Media.COL_MEDIA_TYPE + " = '" + mediaType + "' AND " + BaseColumns._ID + " = " + id;
                break;
            case MEDIA_FOR_TYPE_AND_EXTERNAL_ID:
                mediaType = uri.getPathSegments().get(Media.MEDIA_TYPE_POSITION);
                long extId = Long.parseLong(uri.getPathSegments().get(Media.MEDIA_EXTERNAL_ID_POSITION));
                table = Media.TABLE_NAME;
                where = Media.COL_MEDIA_TYPE + " = '" + mediaType + "' AND " + Media.COL_EXTERNAL_ID + " = " + extId;
                break;
            case ACTIVITY_IDEAS_AND_ID:
                id = uri.getPathSegments().get(ActivityIdea.ACTIVITY_IDEA_ID_POSITION);

                where = BaseColumns._ID + " = " + id;
                table = ActivityIdea.TABLE_NAME;
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
	
	private static class OpenHelper extends SQLiteOpenHelper 
	{
		private final Context mContext;
	    private final ArrayList<String[]> logs = new ArrayList<>();

	    private static final String CREATE_ACTIVITY_IDEAS = ""
	            + "CREATE TABLE IF NOT EXISTS " + ActivityIdea.TABLE_NAME + " ( "
	            + ActivityIdea._ID + " 			INTEGER PRIMARY KEY AUTOINCREMENT, "
	            + ActivityIdea.COL_NAME + " 		TEXT NOT NULL, "
	            + ActivityIdea.COL_VERB + " 		TEXT, "
	            + ActivityIdea.COL_FAVORITE + " 	INTEGER NOT NULL DEFAULT 0"
	            + " ) ";

	    private static final String CREATE_SUPPORT_CONTACT = ""
	            + "CREATE TABLE IF NOT EXISTS " + SupportContacts.TABLE_NAME + "( "
	            + SupportContacts._ID + "            INTEGER PRIMARY KEY AUTOINCREMENT, "
	            + SupportContacts.COL_LOOKUP_KEY + " TEXT NOT NULL, "
	            + SupportContacts.COL_CONTACT_ID + " INTEGER NOT NULL, "
	            + "UNIQUE (" + SupportContacts.COL_LOOKUP_KEY + ", " + SupportContacts.COL_CONTACT_ID
	            + ") ON CONFLICT REPLACE "
	            + ")";

	    private static final String CREATE_QUOTE = ""
	            + "CREATE TABLE IF NOT EXISTS " + Quotes.TABLE_NAME + "( "
	            + Quotes._ID + "			INTEGER PRIMARY KEY AUTOINCREMENT, "
	            + Quotes.COL_AUTHOR + "		TEXT, "
	            + Quotes.COL_QUOTE + "		TEXT NOT NULL, "
	            + Quotes.COL_CATEGORY + " 	TEXT NOT NULL, "
	            + Quotes.COL_FAVORITE + "	INTEGER NOT NULL DEFAULT 0 "
	            + ")";

	    private static final String CREATE_MEDIA = ""
	            + "CREATE TABLE IF NOT EXISTS " + Media.TABLE_NAME + "( "
	            + Media._ID + "						INTEGER PRIMARY KEY AUTOINCREMENT, "
	            + Media.COL_EXTERNAL_ID + "			INTEGER NOT NULL, "
	            + Media.COL_MEDIA_TYPE + " 			TEXT NOT NULL, "
	            + Media.COL_INACTIVE + " 			INTEGER NOT NULL DEFAULT 0, "
	            + Media.COL_ROTATION + " 			INTEGER NOT NULL DEFAULT 0, "
	            + Media.COL_FILE_PATH + " 			TEXT NOT NULL, "
	            + Media.COL_REMOTE_ONLY + "			INTEGER NOT NULL DEFAULT 0, "
	            + Media.COL_LOCAL_THUMBNAIL_PATH + " TEXT, "
	            + Media.COL_LOCAL_TITLE + " 		 TEXT, "
	            + "UNIQUE (" + Media.COL_EXTERNAL_ID + ", " + Media.COL_MEDIA_TYPE + ", " + Media.COL_LOCAL_THUMBNAIL_PATH
	            + ") ON CONFLICT REPLACE "
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
			
			db.execSQL(CREATE_SUPPORT_CONTACT);
	        db.execSQL(CREATE_QUOTE);
	        db.execSQL(CREATE_MEDIA);
	        db.execSQL(CREATE_ACTIVITY_IDEAS);
	        db.execSQL("PRAGMA foreign_keys=ON;");
	        loadData(db);
	        loadActivityIdeas(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
		{
			if (oldVersion < 90) {
	            db.execSQL("DROP TABLE IF EXISTS " + ActivityIdea.TABLE_NAME);
	            db.execSQL(CREATE_ACTIVITY_IDEAS);
	            loadActivityIdeas(db);
	        }

	        if (oldVersion < 91) {
	            db.execSQL("ALTER TABLE " + Media.TABLE_NAME + " ADD COLUMN " + Media.COL_ROTATION
	                    + " INTEGER NOT NULL DEFAULT 0");
	        }
	        
	        if (oldVersion < 92) {
	            dropAll(db);
	            onCreate(db);
	        }
		}
		
		private void dropAll(SQLiteDatabase db) {
	        db.execSQL("DROP TABLE IF EXISTS " + SupportContacts.TABLE_NAME);
	        db.execSQL("DROP TABLE IF EXISTS " + Quotes.TABLE_NAME);
	        db.execSQL("DROP TABLE IF EXISTS " + Media.TABLE_NAME);
	        db.execSQL("DROP TABLE IF EXISTS " + ActivityIdea.TABLE_NAME);
	    }


	    private void loadData(SQLiteDatabase db) {
	        BufferedReader in = null;
	        try {
	            in = new BufferedReader(new InputStreamReader(mContext.getAssets().open("quotes.txt"), "UTF-8"));
	            loadQuotes(db, in);
	            in.close();
	            // in = new BufferedReader(new
	            // InputStreamReader(mContext.getAssets().open("passages.txt")));
	            // loadPassages(db, in);
	            // in.close();
	            // in = new BufferedReader(new
	            // InputStreamReader(mContext.getAssets().open("jokes.txt")));
	            // loadJokes(db, in);
	            // in.close();
	        } catch (IOException e) {
	            Timber.e(e);
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

	    private void loadActivityIdeas(SQLiteDatabase db) {
	        Map<String, String> ideaVerbMap = new HashMap<>();
	        ideaVerbMap.put("Fishing", "go");
	        ideaVerbMap.put("Running", "go");
	        ideaVerbMap.put("Walking", "go");
	        ideaVerbMap.put("Movie", "watch a");
	        ideaVerbMap.put("Hang Out", "");
	        ideaVerbMap.put("Biking", "go");
	        ideaVerbMap.put("Dinner", "have");
	        ideaVerbMap.put("Lunch", "have");
	        ideaVerbMap.put("Breakfast", "have");
	        ideaVerbMap.put("Basketball", "play");
	        ideaVerbMap.put("Tennis", "play");
	        ideaVerbMap.put("Soccer", "play");
	        ideaVerbMap.put("Golf", "play");
	        ideaVerbMap.put("Gym", "go to the");
	        ideaVerbMap.put("Pool", "play");
	        ideaVerbMap.put("Swimming", "go");
	        ideaVerbMap.put("Hiking", "go");
	        ideaVerbMap.put("Camping", "go");
	        ideaVerbMap.put("Shopping", "go");
	        ideaVerbMap.put("Beach", "go to the");
	        ideaVerbMap.put("Coffee / Tea", "have");
	        ideaVerbMap.put("Study", "");
	        ideaVerbMap.put("Cards", "play");
	        ideaVerbMap.put("Bowling", "go");

	        ContentValues vals = new ContentValues();
	        for (Entry<String, String> entry : ideaVerbMap.entrySet()) {
	            vals = new ContentValues();
	            vals.put(ActivityIdea.COL_NAME, entry.getKey());
	            vals.put(ActivityIdea.COL_VERB, entry.getValue());

	            if(!isRecordExistInDatabase(db, ActivityIdea.TABLE_NAME, ActivityIdea.COL_NAME, entry.getKey()))
	                db.insert(ActivityIdea.TABLE_NAME, null, vals);
	        }
	    }

        private boolean isRecordExistInDatabase(SQLiteDatabase db, String tableName, String field, String value) {
            String query = "SELECT * FROM " + tableName + " WHERE " + field + " = '" + value + "'";
            Cursor c = db.rawQuery(query, null);
            if (c.moveToFirst()) {
                //Record exist
                c.close();
                return true;
            }
            //Record available
            c.close();
            return false;
        }

	    @SuppressWarnings("unused")
	    private void loadJokes(SQLiteDatabase db, BufferedReader in) throws IOException {
	        String line;
	        StringBuilder sb = new StringBuilder();
	        while ((line = in.readLine()) != null) {
	            if (line.trim().equals("--")) {
	                ContentValues vals = new ContentValues();
	                vals.put(Quotes.COL_QUOTE, sb.toString().replaceAll("(\\s|\\n|\\r)+$", ""));
	                vals.put(Quotes.COL_CATEGORY, Quotes.QuoteCategory.JOKE.name());
	                db.insert(Quotes.TABLE_NAME, null, vals);
	                sb = new StringBuilder();
	            } else {
	                sb.append(line).append("\n");
	            }
	        }
	    }

	    /**
	     * @param db
	     * @param in
	     * @throws IOException
	     */
	    @SuppressWarnings("unused")
	    private void loadPassages(SQLiteDatabase db, BufferedReader in) throws IOException {
	        String line;
	        StringBuilder sb = new StringBuilder();
	        while ((line = in.readLine()) != null) {
	            if (line.trim().equals("--")) {
	                ContentValues vals = new ContentValues();
	                String[] data = sb.toString().split("\\|");
	                vals.put(Quotes.COL_QUOTE, data[0].replaceAll("(\\s|\\n|\\r)+$", ""));
	                if (data.length > 1) {
	                    vals.put(Quotes.COL_AUTHOR, data[1]);
	                }
	                vals.put(Quotes.COL_CATEGORY, Quotes.QuoteCategory.RELIGIOUS_TEXT.name());
	                db.insert(Quotes.TABLE_NAME, null, vals);
	                sb = new StringBuilder();
	            } else {
	                sb.append(line).append("\n");
	            }
	        }
	    }

	    /**
	     * @param db
	     * @param in
	     * @return
	     * @throws IOException
	     */
	    private String loadQuotes(SQLiteDatabase db, BufferedReader in) throws IOException {
	        String line;
	        while ((line = in.readLine()) != null) {
	            ContentValues vals = new ContentValues();

	            String[] data = line.split("\\|", 2);
	            String quote = data[0].trim();
	            if (quote.length() == 0) {
	                continue;
	            }
	            vals.put(Quotes.COL_QUOTE, quote.replaceAll("(\\s|\\n|\\r)+$", ""));
	            vals.put(Quotes.COL_CATEGORY, Quotes.QuoteCategory.QUOTE.name());

	            String author = data[1].trim();
	            if (author.length() > 0) {
	                vals.put(Quotes.COL_AUTHOR, author);
	            }

	            db.insert(Quotes.TABLE_NAME, null, vals);
	        }
	        return line;
	    }
	}
}