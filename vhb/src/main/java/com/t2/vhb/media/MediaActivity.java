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

package com.t2.vhb.media;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Video;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.t2.vhb.ActionBarActivity;
import com.t2.vhb.R;
import com.t2.vhb.db.VhbContract.Media;
import com.t2.vhb.db.VhbContract.Media.Messages;
import com.t2.vhb.db.VhbContract.Media.Music;
import com.t2.vhb.db.VhbContract.Media.Photos;
import com.t2.vhb.db.VhbContract.Media.Videos;
import com.t2.vhb.home.HomeActivity;
import com.t2.vhb.util.LogUtils;
import com.t2.vhb.util.MediaUtils;
import com.t2.youtube.YouTubeSearchActivity;
import com.t2.youtube.lazylist.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import timber.log.Timber;

public abstract class MediaActivity extends ActionBarActivity {

    private static final int REQUEST_CAPTURE_VIDEO = 2;
    private static final int REQUEST_SELECT_PHOTO = 5;
    private static final int REQUEST_SELECT_MUSIC = 6;
    private static final int REQUEST_SELECT_MESSAGES = 7;
    private static final int REQUEST_SELECT_VIDEO = 8;
    private static final int REQUEST_SELECT_VIDEO_YOUTUBE = 9;
    private static final int REQUEST_CAPTURE_PHOTO = 3;
    private static final int REQUEST_RECORD_MESSAGE = 4;

    // Add media dialog for selecting primary type e.g. Videos, Photos
    private static final int DIALOG_TYPE = 1;
    // Dialog for various Add Video options
    private static final int DIALOG_SUBTYPE_VIDEO = 2;
    // Dialog for various Add Photo options
    private static final int DIALOG_SUBTYPE_PHOTO = 3;
    // Dialog for various Add Message options
    private static final int DIALOG_SUBTYPE_MESSAGES = 4;
    // Dialog for filling out Name / Description of new media file
    private static final int DIALOG_NEW_MEDIA = 5;

    // Used when capturing a picture/video directly which has a special
    // workflow. The item in androids content provider is created manually first
    // and then the external activity actually creates the file.
    private Uri mNewMediaUri;

    // Used to differentiate the selected media type when displaying
    // the Name/Desc dialog
    private String mMediaType;

    // Storage for the title / description entered while adding media elements
    private String mDesc;
    private String mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        if (savedInstanceState != null && savedInstanceState.containsKey("media_uri")) {
            mNewMediaUri = savedInstanceState.getParcelable("media_uri");
        }
    }

    void showAddMediaDialog() {
        showDialog(DIALOG_TYPE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mNewMediaUri != null) {
            outState.putParcelable("media_uri", mNewMediaUri);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MediaUtils.purgeMediaReferences(getContentResolver(), Media.Messages.MEDIA_TYPE);
        MediaUtils.purgeMediaReferences(getContentResolver(), Media.Music.MEDIA_TYPE);
        MediaUtils.purgeMediaReferences(getContentResolver(), Media.Photos.MEDIA_TYPE);
        MediaUtils.purgeMediaReferences(getContentResolver(), Media.Videos.MEDIA_TYPE);
    }

    @SuppressLint("InlinedApi")
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_TYPE:
                return new AlertDialog.Builder(this).setTitle("Media Type").setItems(new String[] {
                        "Video", "Photo", "Music", "Recorded Message"
                }, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            mMediaType = Videos.MEDIA_TYPE;
                            showDialog(DIALOG_SUBTYPE_VIDEO);
                            break;
                        case 1:
                            mMediaType = Photos.MEDIA_TYPE;
                            showDialog(DIALOG_SUBTYPE_PHOTO);
                            break;
                        case 2:
                            mMediaType = Music.MEDIA_TYPE;
                            Intent selectIntent = new Intent(Intent.ACTION_PICK, Audio.Media.EXTERNAL_CONTENT_URI);
                            selectIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

                            try {
                                startActivityForResult(selectIntent, REQUEST_SELECT_MUSIC);
                            } catch (ActivityNotFoundException e) {
                                Toast.makeText(MediaActivity.this,
                                        "There are no apps installed that support picking audio files.",
                                        Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case 3:
                            mMediaType = Messages.MEDIA_TYPE;
                            showDialog(DIALOG_SUBTYPE_MESSAGES);
                            break;
                    }
                }).create();
            case DIALOG_SUBTYPE_VIDEO:
                return new AlertDialog.Builder(this).setTitle("Add Video").setItems(new String[] {
                        "Record", "From Storage", "From YouTube"
                }, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            Intent captureVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                            mNewMediaUri = MediaUtils.getVideoOutputUri();
                            captureVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mNewMediaUri);

                            try {
                                startActivityForResult(captureVideoIntent, REQUEST_CAPTURE_VIDEO);
                            } catch (ActivityNotFoundException | SecurityException e) {
                                Toast.makeText(MediaActivity.this,
                                        "There are no apps installed that support capturing videos or denied permission to record video.",
                                        Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case 1:
                            Intent selectVideoIntent = new Intent(Intent.ACTION_PICK);
                            selectVideoIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                            selectVideoIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

                            selectVideoIntent
                                    .setDataAndType(Video.Media.EXTERNAL_CONTENT_URI, "video/*");
                            try {
                                startActivityForResult(selectVideoIntent, REQUEST_SELECT_VIDEO);
                            } catch (ActivityNotFoundException e) {
                                Toast.makeText(MediaActivity.this,
                                        "There are no apps installed that support picking videos.",
                                        Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case 2:
                            Intent youTubeIntent = new Intent(MediaActivity.this, YouTubeSearchActivity.class);
                            startActivityForResult(youTubeIntent, REQUEST_SELECT_VIDEO_YOUTUBE);
                            break;
                    }
                }).create();
            case DIALOG_SUBTYPE_PHOTO:
                return new AlertDialog.Builder(this).setTitle("Add Photo").setItems(new String[] {
                        "Capture", "From Storage"
                }, (dialog, which) -> {
                    mMediaType = Photos.MEDIA_TYPE;
                    switch (which) {
                        case 0:
                            showDialog(DIALOG_NEW_MEDIA);
                            break;
                        case 1:
                            Intent selectPhotoIntent = new Intent(Intent.ACTION_PICK);
                            selectPhotoIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                            selectPhotoIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    "image/*");
                            selectPhotoIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

                            try {
                                startActivityForResult(selectPhotoIntent, REQUEST_SELECT_PHOTO);
                            } catch (ActivityNotFoundException e) {
                                Toast.makeText(MediaActivity.this,
                                        "There are no apps installed that support picking photos.",
                                        Toast.LENGTH_SHORT).show();
                            }
                            break;
                    }
                }).create();
            case DIALOG_SUBTYPE_MESSAGES:
                return new AlertDialog.Builder(this).setTitle("Add Recorded Message").setItems(new String[] {
                        "Record", "From Storage"
                }, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            Intent recordIntent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
                            try {
                                startActivityForResult(recordIntent, REQUEST_RECORD_MESSAGE);
                            } catch (ActivityNotFoundException e) {
                                Toast.makeText(MediaActivity.this,
                                        "There are no apps installed that support recording audio.",
                                        Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case 1:
                            Intent selectIntent = new Intent(Intent.ACTION_PICK, Audio.Media.EXTERNAL_CONTENT_URI);
                            selectIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                            try {
                                startActivityForResult(selectIntent, REQUEST_SELECT_MESSAGES);
                            } catch (ActivityNotFoundException e) {
                                Toast.makeText(MediaActivity.this,
                                        "There are no apps installed that support picking audio files",
                                        Toast.LENGTH_SHORT).show();
                            }
                            break;
                    }
                }).create();

            case DIALOG_NEW_MEDIA:
                View v = getLayoutInflater().inflate(R.layout.dialog_videos_new, null);
                return new AlertDialog.Builder(this).setTitle("Media Details")
                        .setNegativeButton(R.string.cancel, (dialog, which) -> {
                            AlertDialog newMediaDialog = (AlertDialog) dialog;

                            ((EditText) newMediaDialog.findViewById(R.id.txt_video_title)).setText("");
                            ((EditText) newMediaDialog.findViewById(R.id.txt_video_description)).setText("");
                            dialog.dismiss();
                        }).setPositiveButton(R.string.ok, (dialog, which) -> {
                            AlertDialog newMediaDialog = (AlertDialog) dialog;
                            mTitle = ((EditText) newMediaDialog.findViewById(R.id.txt_video_title)).getText()
                                    .toString();
                            mDesc = ((EditText) newMediaDialog.findViewById(R.id.txt_video_description)).getText()
                                    .toString();

                            if (mMediaType.equals(Photos.MEDIA_TYPE)) {
                                try {
                                    mNewMediaUri = MediaUtils.createPhoto(MediaActivity.this, mTitle, mDesc);
                                    Intent capturePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                    capturePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mNewMediaUri);
                                    startActivityForResult(capturePhotoIntent, REQUEST_CAPTURE_PHOTO);
                                } catch (ActivityNotFoundException | SecurityException e) {
                                    Toast.makeText(MediaActivity.this,
                                            "There are no apps installed that support capturing photos or denied permission to access photos.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }

                            ((EditText) newMediaDialog.findViewById(R.id.txt_video_title)).setText("");
                            ((EditText) newMediaDialog.findViewById(R.id.txt_video_description)).setText("");

                            dialog.dismiss();
                        }).setView(v).create();
        }

        return super.onCreateDialog(id);
    }

    /**
     * Gets the log param string for a given media contract type. External id is
     * provided to differentiate between local videos and youtube videos.
     * 
     * @param mediaType
     * @param externalId
     * @return
     */
    String getMediaTypeNameForLog(String mediaType, int externalId) {
        if (Videos.MEDIA_TYPE.equals(mediaType)) {
            if (externalId <= 0) {
                return "YouTube";
            } else {
                return "Video";
            }
        } else if (Photos.MEDIA_TYPE.equals(mediaType)) {
            return "Photo";
        } else if (Music.MEDIA_TYPE.equals(mediaType)) {
            return "Music";
        } else if (Messages.MEDIA_TYPE.equals(mediaType)) {
            return "Voice";
        }

        return null;
    }

    protected abstract void onMediaAdded(Uri uri);

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case REQUEST_CAPTURE_PHOTO:
                if (resultCode == RESULT_OK) {
                    onPhotoCaptured();
                } else {
                    getContentResolver().delete(mNewMediaUri, null, null);
                }
                break;
            case REQUEST_CAPTURE_VIDEO:
                if (resultCode == RESULT_OK) {
                    if (data != null && data.getData() != null) {
                        onVideoCaptured(data.getData());
                    } else {
                        // We got no URI back so resort to drastic measures in
                        // an effort to find the file.
                        final Cursor c = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                new String[] {
                                    BaseColumns._ID
                                }, null, null, MediaStore.Video.VideoColumns.DATE_TAKEN + " DESC");
                        Uri resultUri = null;
                        if (c.moveToFirst()) {
                            resultUri = ContentUris.withAppendedId(Video.Media.EXTERNAL_CONTENT_URI, c.getLong(0));
                        }
                        c.close();

                        if (resultUri != null) {
                            onVideoCaptured(resultUri);
                        } else {
                            Toast.makeText(this, "Unable to add video.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
            case REQUEST_SELECT_MESSAGES:
                if (resultCode == RESULT_OK) {
                    if(data.getClipData() != null) {
                        ClipData mClip = data.getClipData();
                        for(int i = 0; i < mClip.getItemCount(); i++) {
                            onMessageSelected(mClip.getItemAt(i).getUri());
                        }
                    }
                    else if(data.getData() != null) {
                        onMessageSelected(data.getData());
                    }
                }
                break;
            case REQUEST_RECORD_MESSAGE:
                if (resultCode == RESULT_OK) {
                    onMessageRecorded(data.getData());
                }
                MediaUtils.purgeMediaReferences(getContentResolver(), Media.Messages.MEDIA_TYPE);
                break;
            case REQUEST_SELECT_VIDEO_YOUTUBE:
                if (resultCode == RESULT_OK) {
                    HashMap<String, Intent> items = (HashMap<String, Intent>) data.getExtras().get("selections");
                    for(Intent item : items.values()) {
                        onYouTubeSelected(item.getData().toString(), item.getExtras().getString("thumbnail"), item
                                .getExtras().getString("id"), item.getExtras().getString("title"));
                    }
                }
                break;
            case REQUEST_SELECT_MUSIC:
                if (resultCode == RESULT_OK) {
                    if(data.getClipData() != null) {
                        ClipData mClip = data.getClipData();
                        for(int i = 0; i < mClip.getItemCount(); i++) {
                            onMusicSelected(mClip.getItemAt(i).getUri());
                        }
                    }
                    else if(data.getData() != null) {
                        onMusicSelected(data.getData());
                    }
                }
                MediaUtils.purgeMediaReferences(getContentResolver(), Media.Music.MEDIA_TYPE);
                break;
            case REQUEST_SELECT_PHOTO:
                if (resultCode == RESULT_OK) {
                    if(data.getClipData() != null) {
                        ClipData mClip = data.getClipData();
                        for(int i = 0; i < mClip.getItemCount(); i++) {
                            onPhotoSelected(mClip.getItemAt(i).getUri());
                        }
                    }
                    else if(data.getData() != null) {
                        onPhotoSelected(data.getData());
                    }
                }
                break;
            case REQUEST_SELECT_VIDEO:
                if (resultCode == RESULT_OK) {
                    if(data.getClipData() != null) {
                        ClipData mClip = data.getClipData();
                        for(int i = 0; i < mClip.getItemCount(); i++) {
                            onVideoSelected(mClip.getItemAt(i).getUri());
                        }
                    }
                    else if(data.getData() != null) {
                        onVideoSelected(data.getData());
                    }
                }
                break;
        }

        mNewMediaUri = null;
        super.onActivityResult(requestCode, resultCode, data);
    }


    private void onVideoSelected(Uri uri) {
        mNewMediaUri = MediaUtils.saveMediaReference(this, uri, Media.Videos.MEDIA_TYPE);
        Toast.makeText(this, R.string.videos_add_success, Toast.LENGTH_SHORT).show();
        MediaUtils.purgeMediaReferences(getContentResolver(), Media.Videos.MEDIA_TYPE);
        onMediaAdded(mNewMediaUri);
    }

    private void onPhotoSelected(Uri uri) {
        mNewMediaUri = MediaUtils.saveMediaReference(this, uri, Media.Photos.MEDIA_TYPE);
        Toast.makeText(this, R.string.photos_add_success, Toast.LENGTH_SHORT).show();
        MediaUtils.purgeMediaReferences(getContentResolver(), Media.Photos.MEDIA_TYPE);
        onMediaAdded(mNewMediaUri);
    }

    private void onMusicSelected(Uri musicUri) {
        mNewMediaUri = MediaUtils.saveMediaReference(this, musicUri, Media.Music.MEDIA_TYPE);
        Toast.makeText(this, R.string.music_add_success, Toast.LENGTH_SHORT).show();
        onMediaAdded(musicUri);
    }

    private void onMessageSelected(Uri messageUri) {
        mNewMediaUri = MediaUtils.saveMediaReference(this, messageUri, Media.Messages.MEDIA_TYPE);
        Toast.makeText(this, R.string.message_add_success, Toast.LENGTH_SHORT).show();
        onMediaAdded(mNewMediaUri);
    }

    private void onMessageRecorded(Uri messageUri) {
        mNewMediaUri = MediaUtils.saveMediaReference(this, messageUri, Media.Messages.MEDIA_TYPE);
        Toast.makeText(this, R.string.message_add_success, Toast.LENGTH_SHORT).show();
        onMediaAdded(mNewMediaUri);
    }

    private void onYouTubeSelected(String url, String thumbUrl, String id, String title) {
        // Obtain the youtube thumbnail and save a local copy for
        // future display
        new SaveTask(this).execute(url, thumbUrl, id, title);

    }

    private void onVideoCaptured(Uri videoUri) {
        Timber.tag("VideoCap").e(videoUri.toString());

        if (videoUri.toString().startsWith("file://")) {
            mNewMediaUri = null;

            final Handler scanHandler = new Handler();
            MediaScannerConnection.scanFile(this, new String[] {
                videoUri.getPath()
            }, null, (path, uri) -> {
                if (uri != null) {
                    scanHandler.post(() -> {
                        final Uri resultUri = MediaUtils.saveMediaReference(MediaActivity.this, uri,
                                Videos.MEDIA_TYPE);
                        onMediaAdded(resultUri);
                        Toast.makeText(MediaActivity.this, R.string.videos_add_success, Toast.LENGTH_SHORT)
                                .show();
                    });
                }
            });
        } else {
            mNewMediaUri = MediaUtils.saveMediaReference(this, videoUri, Media.Videos.MEDIA_TYPE);
        }

        if (mNewMediaUri == null) {
            return;
        }

        Toast.makeText(this, R.string.videos_add_success, Toast.LENGTH_SHORT).show();
        onMediaAdded(mNewMediaUri);
    }

    private void onPhotoCaptured() {

        mNewMediaUri = MediaUtils.saveMediaReference(this, mNewMediaUri, Media.Photos.MEDIA_TYPE);
        Toast.makeText(this, R.string.photos_add_success, Toast.LENGTH_SHORT).show();
        onMediaAdded(mNewMediaUri);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    private static class SaveTask extends AsyncTask<String, Void, Uri>{
        private WeakReference<MediaActivity> mediaActivityWeakReference;

        SaveTask(MediaActivity context) {
            mediaActivityWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Uri doInBackground(String... params) {

            MediaActivity activity = mediaActivityWeakReference.get();
            if(activity == null) return null;

            String videoUrl = params[0];
            String thumbUrl = params[1];
            String videoId = params[2];
            String title = params[3];

            Uri result = null;

            try {
                File dir = new File(LogUtils.getCacheDirectory(), "/youtube/");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File f = new File(dir, videoId + ".jpg");
                if (!f.exists()) {
                    f.createNewFile();
                }
                URL imageUrl = new URL(thumbUrl);
                HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);
                conn.setInstanceFollowRedirects(true);
                InputStream is = conn.getInputStream();
                OutputStream os = new FileOutputStream(f);
                Utils.CopyStream(is, os);
                os.close();
                result = MediaUtils.saveYouTubeReference(activity.getApplicationContext(), title, Uri.parse(Uri.decode(videoUrl)), f
                        .getAbsolutePath().replace(Environment.getExternalStorageDirectory().toString(), ""));
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(Uri result) {
            super.onPostExecute(result);

            MediaActivity activity = mediaActivityWeakReference.get();
            if(activity == null) return;

            activity.onMediaAdded(result);
        }
    }
}
