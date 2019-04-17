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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.VideoColumns;
import android.text.TextUtils;
import android.util.LongSparseArray;
import android.widget.Toast;

import com.t2.vhb.R;
import com.t2.vhb.db.VhbContract.Media;
import com.t2.vhb.db.VhbContract.Media.Messages;
import com.t2.vhb.db.VhbContract.Media.Music;
import com.t2.vhb.db.VhbContract.Media.Photos;
import com.t2.vhb.db.VhbContract.Media.Videos;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

/**
 * @author wes
 */
public final class MediaUtils {

    private static final String TAG = "com.t2.vhb.util.MediaUtils";

    /**
     * Creates an image placeholder in the android media content provider.
     * Returns a content uri pointing to the image.
     * 
     * @param context
     * @param title Title of the image
     * @param description Description of the image
     * @return A content uri pointing to this image in the MediaStore. This uri
     *         should be provided to the photo capture intent for output.
     */
    public static Uri createPhoto(Context context, String title, String description) throws SecurityException {
        ContentValues values = new ContentValues();
        values.put(MediaColumns.TITLE, title);
        values.put(MediaColumns.MIME_TYPE, "image/jpeg");
        values.put(ImageColumns.DESCRIPTION, description);
        values.put(MediaColumns.DATA,
                getVisualMediaDirectory().getAbsolutePath() + "/vhb_photo_" + System.currentTimeMillis() + ".jpg");
        return context.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    public static Uri getVideoOutputUri() {
        File mediaStorage = MediaUtils.getVisualMediaDirectory();
        if (!mediaStorage.exists() && !mediaStorage.mkdirs()) {
            return null;
        }

        File mediaFile = new File(mediaStorage, "vhb_video_" + System.currentTimeMillis() + ".mp4");
        return Uri.fromFile(mediaFile);
    }

    /**
     * Returns the Android media provider URIs for a given VHB media type.
     * 
     * @param mediaType
     * @return
     */
    public static Uri[] getExternalMediaUris(String mediaType) {
        switch (mediaType) {
            case Videos.MEDIA_TYPE:
                return new Uri[]{
                        Video.Media.EXTERNAL_CONTENT_URI, Video.Media.INTERNAL_CONTENT_URI
                };
            case Messages.MEDIA_TYPE:
                return new Uri[]{
                        Audio.Media.EXTERNAL_CONTENT_URI, Audio.Media.INTERNAL_CONTENT_URI
                };
            case Photos.MEDIA_TYPE:
                return new Uri[]{
                        Images.Media.EXTERNAL_CONTENT_URI, Images.Media.INTERNAL_CONTENT_URI
                };
            case Music.MEDIA_TYPE:
                return new Uri[]{
                        Audio.Media.EXTERNAL_CONTENT_URI, Audio.Media.INTERNAL_CONTENT_URI
                };
            case Media.BreathingMusic.MEDIA_TYPE:
                return new Uri[]{
                        Audio.Media.EXTERNAL_CONTENT_URI, Audio.Media.INTERNAL_CONTENT_URI
                };
            default:
                return null;
        }

    }

    /**
     * Returns the local VHB Uri for a given VHB media type
     * 
     * @param mediaType
     * @return
     */
    public static Uri getLocalMediaUri(String mediaType) {
        switch (mediaType) {
            case Videos.MEDIA_TYPE:
                return Videos.CONTENT_URI;
            case Messages.MEDIA_TYPE:
                return Messages.CONTENT_URI;
            case Photos.MEDIA_TYPE:
                return Photos.CONTENT_URI;
            case Music.MEDIA_TYPE:
                return Music.CONTENT_URI;
            case Media.BreathingMusic.MEDIA_TYPE:
                return Media.BreathingMusic.CONTENT_URI;
            default:
                return null;
        }

    }

    /**
     * Returns if the user has any recorded messages in VHB. This includes media
     * flagged as inactive.
     * 
     * @param ctx
     * @return
     */
    public static boolean hasMessages(Context ctx) {
        Cursor cursor = ctx.getContentResolver().query(getLocalMediaUri(Messages.MEDIA_TYPE), new String[] {
            BaseColumns._ID
        }, null, null, null);
        if (cursor == null) {
            return false;
        }

        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    /**
     * Returns if the user has any Music in VHB. This includes media flagged as
     * inactive.
     * 
     * @param ctx
     * @return
     */
    public static boolean hasMusic(Context ctx) {
        Cursor cursor = ctx.getContentResolver().query(getLocalMediaUri(Music.MEDIA_TYPE), new String[] {
            BaseColumns._ID
        }, null, null, null);
        if (cursor == null) {
            return false;
        }

        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    /**
     * Returns if the user has any photos in VHB. This includes media flagged as
     * inactive.
     * 
     * @param ctx
     * @return
     */
    public static boolean hasPhotos(Context ctx) {
        Cursor cursor = ctx.getContentResolver().query(getLocalMediaUri(Photos.MEDIA_TYPE), new String[] {
            BaseColumns._ID
        }, null, null, null);
        if (cursor == null) {
            return false;
        }

        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    /**
     * Returns if the user has any videos in VHB. This includes media flagged as
     * inactive.
     * 
     * @param ctx
     * @return
     */
    public static boolean hasVideos(Context ctx) {
        Cursor cursor = ctx.getContentResolver().query(getLocalMediaUri(Videos.MEDIA_TYPE), new String[] {
            BaseColumns._ID
        }, null, null, null);
        if (cursor == null) {
            return false;
        }

        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    /**
     * Returns the directory VHB should use for storing captured images so that
     * they properly show up in the Android gallery app as 'Virtual Home Box'
     * images
     * 
     * @return
     */
    private static File getVisualMediaDirectory() {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static void deactivateMediaReferences(ContentResolver contentResolver, String mediaType) {
        Uri vhbUri = getLocalMediaUri(mediaType);
        if (vhbUri == null) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(Media.COL_INACTIVE, true);
        if (mediaType.equals(Videos.MEDIA_TYPE)) {
            // Youtube videos are excluded
            contentResolver.update(vhbUri, values, Media.COL_REMOTE_ONLY + " = 0", null);
        } else {
            contentResolver.update(vhbUri, values, null, null);
        }
    }

    /**
     * Iterates through all the various media items associated with VHB and
     * checks to see if they still exist in the Android content provider. If the
     * item is gone from the system, it is marked as inactive (for potential
     * restoration later)
     * 
     * @param contentResolver
     * @param mediaType
     * @return
     */
    public static int purgeMediaReferences(ContentResolver contentResolver, String mediaType) {
        Uri vhbUri = getLocalMediaUri(mediaType);
        Uri[] sysUris = getExternalMediaUris(mediaType);

        if (vhbUri == null || sysUris == null) {
            return 0;
        }

        int count = 0;

        Cursor vhbRows = contentResolver.query(vhbUri, new String[] {
                BaseColumns._ID, Media.COL_EXTERNAL_ID, Media.COL_INACTIVE, Media.COL_FILE_PATH, Media.COL_REMOTE_ONLY
        }, null, null, null);

        if (vhbRows != null && vhbRows.moveToFirst()) {
            LongSparseArray<String> extIdPathMap = new LongSparseArray<>();
            Map<Long, Long> extIdLocalIdMap = new HashMap<>();

            // Build a map of android system id -> local reference id
            do {
                if (vhbRows.getInt(4) == 1) {
                    continue;
                }
                final long externalId = vhbRows.getLong(1);
                extIdPathMap.put(externalId, vhbRows.getString(3));
                extIdLocalIdMap.put(externalId, vhbRows.getLong(0));
            } while (vhbRows.moveToNext());

            // Query media store for any rows with ids that match those
            // referenced VHB
            for (Uri sysUri : sysUris) {
                Cursor externalMedia = null;
                try {
                    externalMedia = contentResolver.query(sysUri, new String[]{
                            BaseColumns._ID
                    }, BaseColumns._ID + " IN (" + TextUtils.join(",", extIdLocalIdMap.keySet()) + ")", null, null);
                } catch(SecurityException ex) {
                    ex.printStackTrace();
                }

                // Record the ids of media that either needs to become active or
                // inactive.
                if (externalMedia != null && externalMedia.moveToFirst()) {
                    do {
                        final long externalId = externalMedia.getLong(0);
                        extIdLocalIdMap.remove(externalId);
                    } while (externalMedia.moveToNext());
                }
                if(externalMedia != null) {
                    externalMedia.close();
                }
            }

            // Get the current external storage
            String extPath = Environment.getExternalStorageDirectory().toString();

            // For each local entry unable to find its external entry
            Set<Long> extIds = new HashSet<>(extIdLocalIdMap.keySet());
            for (Long extId : extIds) {
                // Get the known file path
                String path = extIdPathMap.get(extId);

                for (Uri sysUri : sysUris) {
                    // Query media store for any references to the known path
                    Cursor externalMedia = null;
                    try {
                        externalMedia = contentResolver.query(sysUri, new String[]{
                                BaseColumns._ID
                        }, MediaColumns.DATA + " = '" + extPath + path + "'", null, null);
                    } catch(SQLiteException | SecurityException ex){
                        Timber.e(ex);
                    }

                    // If a media store reference exists
                    if (externalMedia != null && externalMedia.moveToFirst()) {
                        // Update the local entry with the new id
                        ContentValues values = new ContentValues();
                        values.put(Media.COL_EXTERNAL_ID, externalMedia.getLong(0));
                        contentResolver.update(vhbUri, values, BaseColumns._ID + " = " + extIdLocalIdMap.get(extId), null);
                        extIdLocalIdMap.remove(extId);
                        Timber.d("Local media reference updated to point to new external id");
                    }

                    //This may seem redundant but if externalMedia.moveToFirst() failed then we still need to close the cursor
                    if(externalMedia != null) {
                        externalMedia.close();
                    }
                }
            }

            // If the file is totally gone, mark the referece as inactive for
            // possibly restoration later
            if (!extIdLocalIdMap.isEmpty()) {
                ContentValues values = new ContentValues();
                values.put(Media.COL_INACTIVE, true);
                String selection = BaseColumns._ID + " IN (" + TextUtils.join(",", extIdLocalIdMap.values()) + ")";
                count = contentResolver.delete(vhbUri, selection, null);
                Timber.d(count + " missing references purged for " + vhbUri.toString());
            }
        }
        if(vhbRows != null) {
            vhbRows.close();
        }

        return count;
    }

    /**
     * Goes through all inactive media and checks to see if the associated file
     * exists once again. If it does that media item can be restored. This can
     * be used to restore all the apps media if the path to the files remains
     * unchanged
     * 
     * @param contentResolver
     */
    public static void restoreMediaReferences(ContentResolver contentResolver) {
        Uri[] localUris = {
                Media.Videos.CONTENT_URI, Media.Messages.CONTENT_URI, Media.Photos.CONTENT_URI, Media.Music.CONTENT_URI
        };
        Uri[] externalUris = {
                Video.Media.EXTERNAL_CONTENT_URI, Audio.Media.EXTERNAL_CONTENT_URI, Images.Media.EXTERNAL_CONTENT_URI,
                Audio.Media.EXTERNAL_CONTENT_URI
        };

        String extPath = Environment.getExternalStorageDirectory().toString();

        for (int i = 0; i < localUris.length; i++) {
            Uri localUri = localUris[i];
            Uri externalUri = externalUris[i];

            Cursor localRows = contentResolver.query(localUri, new String[] {
                    BaseColumns._ID, Media.COL_EXTERNAL_ID, Media.COL_INACTIVE, Media.COL_FILE_PATH
            }, Media.COL_INACTIVE + " = 1", null, null);

            if (localRows != null && localRows.moveToFirst()) {
                do {
                    Long localId = localRows.getLong(0);
                    String path = localRows.getString(3);

                    // Query media store for any references to the known path
                    Cursor externalMedia = contentResolver.query(externalUri, new String[] {
                        BaseColumns._ID
                    }, MediaColumns.DATA + " = '" + extPath + path + "'", null, null);
                    // If a media store reference exists
                    if (externalMedia != null && externalMedia.moveToFirst()) {
                        // Update the local entry with the new id
                        ContentValues values = new ContentValues();
                        values.put(Media.COL_EXTERNAL_ID, externalMedia.getLong(0));
                        values.put(Media.COL_INACTIVE, false);
                        contentResolver.update(localUri, values, BaseColumns._ID + " = " + localId, null);
                        Timber.d("Local media reference updated to point to new external id");
                    }

                    if(externalMedia != null)
                        externalMedia.close();
                } while (localRows.moveToNext());
            }
            if(localRows != null) {
                localRows.close();
            }
        }
    }

    public static Bitmap sampleImage(ContentResolver resolver, Uri selectedImage, int targetWidth, int targetHeight,
            int rotation) throws IOException {
        return sampleImage(resolver, selectedImage, targetWidth, targetHeight, rotation, true, true);
    }

    public static Bitmap sampleImage(ContentResolver resolver, String filePath, int targetWidth, int targetHeight,
            int rotation) throws IOException {
        return sampleImage(resolver, filePath, targetWidth, targetHeight, rotation, true, true);
    }

    /**
     * Downsamples and rotates an image to fit within the targeted dimensions
     * 
     * @param resolver
     * @param filePath File path to the image in question
     * @param targetWidth The maximum target width of the resulting sample
     * @param targetHeight The maximum target height of the resulting sample
     * @param rotation The rotation of the resulting sample
     * @param includeWidth Should the sampling process take the target width
     *            into account
     * @param includeHeight Should the sampling process take the target height
     *            into account
     * @return A sampled bitmap or null if process cannot be completed
     * @throws IOException
     */
    public static Bitmap sampleImage(ContentResolver resolver, String filePath, int targetWidth, int targetHeight,
            int rotation, boolean includeWidth, boolean includeHeight) {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        int widthTmp = options.outWidth;
        int heightTmp = options.outHeight;
        int scale = 1;
        while (true) {
            if ((!includeWidth || widthTmp <= targetWidth) && (!includeHeight || heightTmp <= targetHeight)) {
                break;
            }

            widthTmp /= 2;
            heightTmp /= 2;
            scale *= 2;
        }

        options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        Bitmap sampled = null;
        while (sampled == null && scale <= 128) {
            try {
                if (scale > 1) {
                    options.inSampleSize = scale;
                }

                sampled = BitmapFactory.decodeFile(filePath, options);
                break;
            } catch (OutOfMemoryError e) {
                scale *= 2;
            }
        }

        if (sampled != null) {
            Matrix mtx = new Matrix();
            mtx.postRotate(rotation);

            // Likely the same memory source as sampledBmp. Do not recycle
            // sampledBmp.
            sampled = Bitmap.createBitmap(sampled, 0, 0, widthTmp, heightTmp, mtx, true);
        }

        return sampled;
    }

    /**
     * Downsamples and rotates an image to fit within the targeted dimensions
     * 
     * @param resolver
     * @param selectedImage Uri to the image in question
     * @param targetWidth The maximum target width of the resulting sample
     * @param targetHeight The maximum target height of the resulting sample
     * @param rotation The rotation of the resulting sample
     * @param includeWidth Should the sampling process take the target width
     *            into account
     * @param includeHeight Should the sampling process take the target height
     *            into account
     * @return A sampled bitmap or null if process cannot be completed
     * @throws IOException
     */
    private static Bitmap sampleImage(ContentResolver resolver, Uri selectedImage, int targetWidth, int targetHeight,
                                      int rotation, boolean includeWidth, boolean includeHeight) throws IOException {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(resolver.openInputStream(selectedImage), null, options);

        int widthTmp = options.outWidth;
        int heightTmp = options.outHeight;
        int scale = 1;
        while (true) {
            if ((!includeWidth || widthTmp <= targetWidth) && (!includeHeight || heightTmp <= targetHeight)) {
                break;
            }

            widthTmp /= 2;
            heightTmp /= 2;
            scale *= 2;
        }

        options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        Bitmap sampled = null;
        while (sampled == null && scale <= 128) {
            if (scale > 1) {
                options.inSampleSize = scale;
            }

            try (InputStream in = resolver.openInputStream(selectedImage)) {
                sampled = BitmapFactory.decodeStream(in, null, options);
                break;
            } catch (OutOfMemoryError e) {
                scale *= 2;
            }
        }

        if (sampled != null) {
            Matrix mtx = new Matrix();
            mtx.postRotate(rotation);

            // Likely the same memory source as sampledBmp. Do not recycle
            // sampledBmp.
            sampled = Bitmap.createBitmap(sampled, 0, 0, widthTmp, heightTmp, mtx, true);
        }

        return sampled;
    }

    /**
     * Creates a local database reference to an existing MediaStore element
     * 
     * @param context
     * @param externalUri
     * @param mediaType
     * @return A URI pointing to the local entry for this media item.
     */
    public static Uri saveMediaReference(Context context, Uri externalUri, String mediaType) {
        String path = "";
        long externalId = -1;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(externalUri, new String[]{
                    MediaStore.MediaColumns.DATA
            }, null, null, null);
        } catch (SecurityException ex) {
            Timber.e(ex);
            Toast.makeText(context, R.string.deniedpermissiontomedia, Toast.LENGTH_LONG).show();
        }
        if(cursor == null) {
            Toast.makeText(context, "Unable to retrieve " + mediaType, Toast.LENGTH_SHORT).show();
        	return null;
        }
        cursor.moveToFirst();
        path = cursor.getString(0).replace(Environment.getExternalStorageDirectory().toString(), "");
        externalId = ContentUris.parseId(externalUri);
        cursor.close();

        if (externalId == -1) {
            return null;
        }

        final ContentValues media = new ContentValues();
        media.put(Media.COL_FILE_PATH, path);
        media.put(Media.COL_EXTERNAL_ID, externalId);
        return context.getContentResolver().insert(getLocalMediaUri(mediaType), media);
    }

    public static Uri saveYouTubeReference(Context context, String title, Uri videoUri, String thumbPath) {
        final ContentValues video = new ContentValues();
        video.put(Media.COL_REMOTE_ONLY, true);
        video.put(Media.COL_FILE_PATH, videoUri.toString());
        video.put(Media.COL_EXTERNAL_ID, -1);
        video.put(Media.COL_LOCAL_TITLE, title);
        video.put(Media.COL_LOCAL_THUMBNAIL_PATH, thumbPath);

        return context.getContentResolver().insert(getLocalMediaUri(Media.Videos.MEDIA_TYPE), video);
    }

    /**
     * Important: This method will delete the MediaStore entry at the original
     * location provided and move it to the VHB folder. Takes an existing
     * MediaStore video and moves it to the VHB album. Updates title and
     * description.
     * 
     * @param context
     * @param originalLocation
     * @param title
     * @param description
     * @return The URI pointing to the upated MediaStore entry
     */
    public static Uri updateVideoLocation(Context context, Uri originalLocation, String title, String description) {
        Cursor cursor = context.getContentResolver().query(originalLocation, null, null, null, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaColumns.DATA));
        ContentValues vals = new ContentValues();
        DatabaseUtils.cursorRowToContentValues(cursor, vals);
        cursor.close();

        File file = new File(path);
        File newFile = new File(MediaUtils.getVisualMediaDirectory(), file.getName());
        file.renameTo(newFile);

        vals.put(MediaColumns.DATA, newFile.getAbsolutePath());
        vals.put(MediaColumns.TITLE, title != null ? title : "Untitled");
        vals.put(VideoColumns.DESCRIPTION, description != null ? description : "No Description");

        context.getContentResolver().delete(originalLocation, null, null);
        return context.getContentResolver().insert(Video.Media.EXTERNAL_CONTENT_URI, vals);
    }

    private MediaUtils() {
    }
}
