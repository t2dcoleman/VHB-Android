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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Video;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import com.t2.vhb.R;
import com.t2.vhb.db.VhbContract.Media;
import com.t2.vhb.db.VhbContract.Media.Messages;
import com.t2.vhb.db.VhbContract.Media.Music;
import com.t2.vhb.db.VhbContract.Media.Photos;
import com.t2.vhb.db.VhbContract.Media.Videos;
import com.t2.vhb.util.MediaUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wes
 */
public class MediaViewActivity extends MediaActivity implements LoaderCallbacks<Cursor> {

    public static final String KEY_REMINDER_ID = "reminder_id";

    private static final String TAG = "CoverFlowActivity";

    // Async loader of media references. This gets changed by the filter dialog
    // as necessary and force-reloaded
    private static final int LOADER_MEDIA = 1;

    // Dialog for filtering visible media
    private static final int DIALOG_FILTER = 100;

    private static final int SWIPE_MIN_DISTANCE = 100;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    private boolean mShowToast;

    private CoverFlowAdapter mAdapter;

    // Local ID of media we want to default the view to
    private long mDefaultId;

    // Filter checkboxes
    final private boolean[] mFilters = new boolean[4];

    private final GestureDetector mGestureDetector = new GestureDetector(new GestureListener());

    // Context menu popup protection during swipe / fling operations
    private boolean mFlingDetected;

    private boolean mReloadPhoto;

    private AsyncTask<PhotoLoadOptions, Void, Bitmap> mImgLoader;

    /**
     * Used for changing gallery selection when swiping left or right on the
     * large centered image in portrait mode
     * 
     * @author wes
     */
    private class GestureListener extends SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) {
                return false;
            }

            if (mAdapter.getCount() == 0) {
                return false;
            }

            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                getCoverFlow().setSelection(
                        Math.min(mAdapter.getCount() - 1, getCoverFlow().getSelectedItemPosition() + 1));
                return true; // Right to left
            } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                getCoverFlow().setSelection(Math.max(0, getCoverFlow().getSelectedItemPosition() - 1));
                return true; // Left to right
            }
            return false;
        }
    }

    private void loadLargePhoto(PhotoLoadOptions options) {
        mImgLoader = new LoadLargeImageTask(this).execute(options);
    }

    private void loadImage(int position) {
        String title = mAdapter.getTitle(position);
        getSupportActionBar().setSubtitle(title);
        if (findViewById(R.id.img_large) != null) {
            ImageButton image = (ImageButton) findViewById(R.id.img_large);
            image.setImageBitmap(null);
            if (mImgLoader != null) {
                mImgLoader.cancel(true);
            }

            String mediaType = mAdapter.getMediaType(position);

            image.setContentDescription(mediaType + ", " + title + ".");

            if (Media.Photos.MEDIA_TYPE.equals(mediaType)) {
                Cursor photo = (Cursor) mAdapter.getItem(position);
                int rotation = 0;
                if (photo != null) {
                    rotation = photo.getInt(photo.getColumnIndex(Media.COL_ROTATION));
                }
                PhotoLoadOptions options = new PhotoLoadOptions(mAdapter.getExternalItemId(position), image.getWidth(),
                        image.getHeight(), rotation);
                loadLargePhoto(options);
            } else {
                image.setImageBitmap(mAdapter.createBitmap(position));
            }

            if (mediaType.equals(Media.Music.MEDIA_TYPE) || mediaType.equals(Media.Messages.MEDIA_TYPE)) {
                image.setScaleType(ScaleType.CENTER_INSIDE);
            } else {
                image.setScaleType(ScaleType.FIT_CENTER);
            }

            int badgeId = mAdapter.getBadgeResourceId(position);
            if (badgeId > 0) {
                findViewById(R.id.img_large_overlay).setVisibility(View.VISIBLE);
                ((ImageView) findViewById(R.id.img_large_overlay)).setImageResource(badgeId);
            } else {
                findViewById(R.id.img_large_overlay).setVisibility(View.GONE);
            }
            image.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(KEY_REMINDER_ID, mDefaultId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.cover_flow);
        setTitle("Remind Me");
        setIcon(R.drawable.icon_remind_me);

        mShowToast = savedInstanceState == null;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mFilters[0] = prefs.getBoolean(getString(R.string.pref_media_filter_videos), true);
        mFilters[1] = prefs.getBoolean(getString(R.string.pref_media_filter_photos), true);
        mFilters[2] = prefs.getBoolean(getString(R.string.pref_media_filter_music), true);
        mFilters[3] = prefs.getBoolean(getString(R.string.pref_media_filter_messages), true);

        // Get the local ID of whatever reminder image / video was clicked
        // through the home-screen and display it if it isn't filtered out
        if (savedInstanceState != null) {
            mDefaultId = savedInstanceState.getLong(KEY_REMINDER_ID, -1);
        } else {
            mDefaultId = getIntent().getLongExtra(KEY_REMINDER_ID, -1);
        }

        getSupportLoaderManager().initLoader(LOADER_MEDIA, null, this);
        mAdapter = new CoverFlowAdapter(this);
        getCoverFlow().setAdapter(mAdapter);
        getCoverFlow().setCallbackDuringFling(false);
        getCoverFlow().setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                mDefaultId = id;
                loadImage(position);
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                if (findViewById(R.id.img_large) != null) {
                    ((ImageView) findViewById(R.id.img_large)).setImageBitmap(null);
                    supportInvalidateOptionsMenu();
                }
            }
        });
        if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            getCoverFlow().setSpacing(4);
            getCoverFlow().setUnselectedAlpha(0.4f);
            registerForContextMenu(findViewById(R.id.img_large));
            findViewById(R.id.img_large).setOnTouchListener((v, event) -> {
                if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE
                        && mGestureDetector.onTouchEvent(event)) {
                    mFlingDetected = true;
                    return true;
                }
                return false;
            });
        } else {
            getCoverFlow().setOnItemClickListener((arg0, arg1, arg2, arg3) -> {
                if (getCoverFlow().getSelectedItemPosition() == arg2) {
                    coverClicked(arg2);
                }
            });
        }
        registerForContextMenu(getCoverFlow());
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean selected = getCoverFlow().getSelectedItem() != null;
        menu.findItem(R.id.mnu_remove_media).setVisible(selected);
        menu.findItem(R.id.mnu_remove_media).setEnabled(selected);

        boolean photo = false;
        if (selected) {
            String mediaType = mAdapter.getMediaType(getCoverFlow().getSelectedItemPosition());
            photo = selected && Media.Photos.MEDIA_TYPE.equals(mediaType);
        }

        menu.findItem(R.id.mnu_rotate_left).setVisible(photo);
        menu.findItem(R.id.mnu_rotate_left).setEnabled(photo);
        menu.findItem(R.id.mnu_rotate_right).setVisible(photo);
        menu.findItem(R.id.mnu_rotate_right).setEnabled(photo);

        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (mFlingDetected) {
            mFlingDetected = false;
            return;
        }

        getMenuInflater().inflate(R.menu.ctx_media, menu);

        Cursor cursor = null;
        if (v.getId() == R.id.img_large) {
            cursor = (Cursor) getCoverFlow().getSelectedItem();
        } else if (v.getId() == R.id.coverflow) {
            AdapterView.AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
            cursor = (Cursor) mAdapter.getItem(info.position);
        }

        // If no media items are visible just present the user with 'Add Media'
        if (cursor == null || cursor.getCount() == 0) {
            menu.removeItem(R.id.mnu_remove);
            menu.setHeaderTitle("Media");
            return;
        }

        String mediaType = cursor.getString(cursor.getColumnIndex(Media.COL_MEDIA_TYPE));
        Long id = cursor.getLong(cursor.getColumnIndex(Media.COL_EXTERNAL_ID));

        // Different queries are necessary depending on the media type
        switch (mediaType) {
            case Photos.MEDIA_TYPE:
                Cursor image = getContentResolver().query(Images.Media.EXTERNAL_CONTENT_URI, new String[]{
                        MediaColumns.TITLE
                }, BaseColumns._ID + " = " + id, null, null);
                image.moveToFirst();
                menu.setHeaderTitle(image.getString(0));
                image.close();
                break;
            case Messages.MEDIA_TYPE:
                Cursor recording = getContentResolver().query(Audio.Media.EXTERNAL_CONTENT_URI, new String[]{
                        MediaColumns.TITLE
                }, BaseColumns._ID + " = " + id, null, null);
                recording.moveToFirst();
                menu.setHeaderTitle(recording.getString(0));
                recording.close();
                break;
            case Music.MEDIA_TYPE:
                Cursor song = getContentResolver().query(Audio.Media.EXTERNAL_CONTENT_URI, new String[]{
                        MediaColumns.TITLE
                }, BaseColumns._ID + " = " + id, null, null);
                song.moveToFirst();
                menu.setHeaderTitle(song.getString(0));
                song.close();
                break;
            case Videos.MEDIA_TYPE:
                if (id == -1) {
                    menu.setHeaderTitle(cursor.getString(cursor.getColumnIndex(Media.COL_LOCAL_TITLE)));
                } else {
                    Cursor imageMedia = getContentResolver().query(Video.Media.EXTERNAL_CONTENT_URI, new String[]{
                            MediaColumns.TITLE
                    }, BaseColumns._ID + " = " + id, null, null);
                    imageMedia.moveToFirst();
                    menu.setHeaderTitle(imageMedia.getString(0));
                    imageMedia.close();
                }
                break;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnu_remove:
                removeMedia();
                break;
            case R.id.mnu_add_media:
                showAddMediaDialog();
                break;
        }
        return true;
    }

    private void removeMedia() {
        final Cursor cursor = (Cursor) getCoverFlow().getSelectedItem();
        final String mediaType = cursor.getString(cursor.getColumnIndex(Media.COL_MEDIA_TYPE));
        final int externalId = cursor.getInt(cursor.getColumnIndex(Media.COL_EXTERNAL_ID));

        getContentResolver().delete(
                MediaUtils.getLocalMediaUri(mediaType).buildUpon()
                        .appendEncodedPath(cursor.getInt(cursor.getColumnIndex(BaseColumns._ID)) + "").build(), null, null);

        Map<String, String> data = new HashMap<>();
        data.put("Media", getMediaTypeNameForLog(mediaType, externalId));
    }

    @Override
    protected void onMediaAdded(Uri uri) {

    }

    private void rotatePhoto(int degrees) {
        mReloadPhoto = true;
        Cursor photo = (Cursor) getCoverFlow().getSelectedItem();
        int rotation = photo.getInt(photo.getColumnIndex(Media.COL_ROTATION));
        long id = photo.getLong(photo.getColumnIndex(BaseColumns._ID));
        ContentValues vals = new ContentValues();
        vals.put(Media.COL_ROTATION, rotation + degrees);
        getContentResolver().update(ContentUris.withAppendedId(Media.Photos.CONTENT_URI, id), vals, null, null);
        mAdapter.clearCache(id);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnu_help:
                Intent intent = new Intent(this, MediaHelpActivity.class);
                startActivity(intent);
                return true;
            case R.id.mnu_add_media:
                showAddMediaDialog();
                return true;
            case R.id.mnu_rotate_left:
                rotatePhoto(-90);
                return true;
            case R.id.mnu_rotate_right:
                rotatePhoto(90);
                return true;
            case R.id.mnu_filter:
                showDialog(DIALOG_FILTER);
                return true;
            case R.id.mnu_remove_media:
                removeMedia();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String getFilterSelection() {
        List<String> mediaTypes = new ArrayList<>();
        if (mFilters[0]) {
            mediaTypes.add(Media.Videos.MEDIA_TYPE);
        }
        if (mFilters[1]) {
            mediaTypes.add(Media.Photos.MEDIA_TYPE);
        }
        if (mFilters[2]) {
            mediaTypes.add(Media.Music.MEDIA_TYPE);
        }
        if (mFilters[3]) {
            mediaTypes.add(Media.Messages.MEDIA_TYPE);
        }
        return Media.COL_INACTIVE + " = 0 AND " + Media.COL_MEDIA_TYPE + " IN ('" + TextUtils.join("','", mediaTypes)
                + "')";
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_FILTER:
                return new AlertDialog.Builder(this).setTitle("Filter Media").setMultiChoiceItems(new String[] {
                        "Video", "Photo", "Music", "Recorded Message"
                }, mFilters, (dialog, which, isChecked) -> {
                }).setPositiveButton("OK", (dialog, which) -> {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MediaViewActivity.this);
                    prefs.edit().putBoolean(getString(R.string.pref_media_filter_videos), mFilters[0])
                            .putBoolean(getString(R.string.pref_media_filter_photos), mFilters[1])
                            .putBoolean(getString(R.string.pref_media_filter_music), mFilters[2])
                            .putBoolean(getString(R.string.pref_media_filter_messages), mFilters[3]).apply();

                    Loader<Cursor> loader = getSupportLoaderManager().getLoader(LOADER_MEDIA);
                    CursorLoader cLoader = (CursorLoader) loader;
                    cLoader.setSelection(getFilterSelection());
                    cLoader.forceLoad();
                }).setNegativeButton("Cancel", (dialog, which) -> {
                    // Reset checkboxes what they were before
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MediaViewActivity.this);

                    mFilters[0] = prefs.getBoolean(getString(R.string.pref_media_filter_videos), true);
                    mFilters[1] = prefs.getBoolean(getString(R.string.pref_media_filter_photos), true);
                    mFilters[2] = prefs.getBoolean(getString(R.string.pref_media_filter_music), true);
                    mFilters[3] = prefs.getBoolean(getString(R.string.pref_media_filter_messages), true);
                    ListView v = ((AlertDialog) dialog).getListView();
                    int i = 0;
                    while (i < mFilters.length) {
                        v.setItemChecked(i, mFilters[i]);
                        i++;
                    }
                }).create();
        }
        return super.onCreateDialog(id);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mnu_media, menu);
        return true;
    }

    private Gallery getCoverFlow() {
        return (Gallery) findViewById(R.id.coverflow);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, Media.CONTENT_URI, null, getFilterSelection(), null, null);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.setCursor(null);
    }

    public void coverClicked(View v) {
        int position = getCoverFlow().getSelectedItemPosition();
        coverClicked(position);
    }

    private void coverClicked(int position) {
        Cursor cursor = (Cursor) mAdapter.getItem(position);
        if (cursor == null || cursor.getCount() == 0) {
            return;
        }

        String mediaType = mAdapter.getMediaType(position);
        Long id = cursor.getLong(cursor.getColumnIndex(Media.COL_EXTERNAL_ID));

        if (id == null || id == -1) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(cursor.getString(cursor
                    .getColumnIndex(Media.COL_FILE_PATH))));
            startActivity(intent);
        } else {
            if (mediaType.equals(Media.Music.MEDIA_TYPE) || mediaType.equals(Media.Messages.MEDIA_TYPE)) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(
                        Uri.fromFile(new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/"
                                + cursor.getString(cursor.getColumnIndex(Media.COL_FILE_PATH)))), "audio/*");
                startActivity(intent);
            } else {
                Uri[] externalUris = MediaUtils.getExternalMediaUris(mediaType);
                for (Uri externalUri : externalUris) {
                    Uri uri = ContentUris.withAppendedId(externalUri,
                            Long.parseLong(cursor.getString(cursor.getColumnIndex(Media.COL_EXTERNAL_ID))));
                    Cursor mediaItem = null;
                    try {
                        mediaItem = getContentResolver().query(uri, new String[]{
                                BaseColumns._ID
                        }, null, null, null);
                    } catch(SecurityException ex) {
                        ex.printStackTrace();
                        Toast.makeText(this, R.string.deniedpermissiontomedia, Toast.LENGTH_LONG).show();
                    }
                    if (mediaItem != null && mediaItem.moveToFirst()) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                        mediaItem.close();
                        break;
                    } else if(mediaItem != null) {
                        mediaItem.close();
                    }
                }
            }
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.setCursor(data);

        if (data.getCount() == 0) {
            Cursor cursor = getContentResolver().query(
                    Media.CONTENT_URI,
                    null,
                    Media.COL_INACTIVE + " = 0 AND " + Media.COL_MEDIA_TYPE + " != '" + Media.BreathingMusic.MEDIA_TYPE
                            + "'", null, null);
            if (cursor.getCount() == 0) {
                Intent intent = new Intent(this, MediaHelpActivity.class);
                intent.putExtra(MediaHelpActivity.KEY_INITIAL_HELP, true);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(
                        this,
                        "No media current visible. The media filter can be changed by pressing 'Menu' and selecting 'Filters'",
                        Toast.LENGTH_SHORT).show();
            }
            cursor.close();
            // finish();
            return;
        } else {
            if (mShowToast) {
                mShowToast = false;
                Toast.makeText(this, "Add reminders by pressing the 'Menu' button and selecting 'Add Media'",
                        Toast.LENGTH_LONG).show();
            }
        }

        if (mDefaultId >= 0) {
            int pos = mAdapter.getPositionById(mDefaultId);
            if (pos >= 0) {
                getCoverFlow().setSelection(pos);
            }

            if (mReloadPhoto) {
                mReloadPhoto = false;
                loadImage(pos);
            }
        } else {
            if (data.getCount() > 0) {
                getSupportActionBar().setSubtitle(mAdapter.getTitle(0));
            }
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private static final class CoverFlowAdapter extends BaseAdapter implements SpinnerAdapter {

        private final Context mContext;
        private Cursor mCursor;

        private final LruCache<Long, WeakReference<Bitmap>> mCache = new LruCache<>(20);

        public CoverFlowAdapter(Context context) {
            mContext = context;
        }

        public void setCursor(Cursor cursor) {
            mCursor = cursor;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (mCursor == null) {
                return 0;
            }

            return mCursor.getCount();
        }

        @Override
        public long getItemId(int position) {
            if (mCursor == null || mCursor.getCount() == 0) {
                return -1;
            }

            mCursor.moveToPosition(position);
            return mCursor.getLong(mCursor.getColumnIndex(BaseColumns._ID));
        }

        public int getPositionById(long id) {
            if (!mCursor.moveToFirst()) {
                return -1;
            }

            int position = 0;
            do {
                if (mCursor.getLong(mCursor.getColumnIndex(BaseColumns._ID)) == id) {
                    return position;
                }
                position++;
            } while (mCursor.moveToNext());

            return -1;
        }

        @Override
        public Object getItem(int position) {
            if (mCursor == null) {
                return null;
            }

            mCursor.moveToPosition(position);
            return mCursor;
        }

        public String getTitle(int position) {
            mCursor.moveToPosition(position);
            String mediaType = mCursor.getString(mCursor.getColumnIndex(Media.COL_MEDIA_TYPE));
            Long id = mCursor.getLong(mCursor.getColumnIndex(Media.COL_EXTERNAL_ID));
            String title;
            if (id == null || id == -1) {
                title = mCursor.getString(mCursor.getColumnIndex(Media.COL_LOCAL_TITLE));
            } else {
                Uri[] externalUris = MediaUtils.getExternalMediaUris(mediaType);
                title = "No Title";

                for (Uri externalUri : externalUris) {
                    Uri uri = ContentUris.withAppendedId(externalUri,
                            Long.parseLong(mCursor.getString(mCursor.getColumnIndex(Media.COL_EXTERNAL_ID))));
                    Cursor item = null;
                    try {
                        mContext.getContentResolver().query(uri, new String[] {
                            MediaStore.MediaColumns.TITLE
                        }, null, null, null);
                    } catch(SecurityException ex) {
                        ex.printStackTrace();
                        Toast.makeText(mContext, R.string.deniedpermissiontomedia, Toast.LENGTH_LONG).show();
                    }

                    if (item != null && item.moveToFirst()) {
                        title = item.getString(0);
                        item.close();
                        break;
                    } else if (item != null) {
                        item.close();
                    }
                }

            }
            return title;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = LayoutInflater.from(mContext).inflate(R.layout.cover_flow_item, null);
                if (mContext.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                    v.findViewById(R.id.img_cover).setLayoutParams(new RelativeLayout.LayoutParams(100, 100));
                }
            }

            String mediaType = getMediaType(position);

            if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                int badgeId = getBadgeResourceId(position);
                if (badgeId > 0) {
                    v.findViewById(R.id.img_overlay).setVisibility(View.VISIBLE);
                    ((ImageView) v.findViewById(R.id.img_overlay)).setImageResource(badgeId);
                } else {
                    v.findViewById(R.id.img_overlay).setVisibility(View.GONE);
                }
            } else {
                v.findViewById(R.id.img_overlay).setVisibility(View.GONE);
            }

            v.setContentDescription(mediaType + ", " + getTitle(position) + ".");

            ((ImageView) v.findViewById(R.id.img_cover)).setImageBitmap(createBitmap(position));
            return v;
        }

        String getMediaType(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getString(mCursor.getColumnIndex(Media.COL_MEDIA_TYPE));
        }

        int getBadgeResourceId(int position) {
            mCursor.moveToPosition(position);
            String mediaType = getMediaType(position);
            Long id = mCursor.getLong(mCursor.getColumnIndex(Media.COL_EXTERNAL_ID));
            if (mediaType.equals(Media.Videos.MEDIA_TYPE)) {
                return (id == null || id == -1) ? R.drawable.ic_media_youtube : R.drawable.ic_media_play;
            }
            return -1;
        }

        public long getExternalItemId(int position) {
            if (mCursor == null || mCursor.getCount() == 0) {
                return -1;
            }

            mCursor.moveToPosition(position);
            return mCursor.getLong(mCursor.getColumnIndex(Media.COL_EXTERNAL_ID));
        }

        Bitmap createBitmap(int position) {
            mCursor.moveToPosition(position);
            String mediaType = getMediaType(position);
            Long id = mCursor.getLong(mCursor.getColumnIndex(Media.COL_EXTERNAL_ID));
            int rotation = mCursor.getInt(mCursor.getColumnIndex(Media.COL_ROTATION));

            WeakReference<Bitmap> bmpRef = mCache.get(getItemId(position));
            Bitmap bitmap = bmpRef == null ? null : bmpRef.get();

            if (bitmap != null) {
                return bitmap;
            }
            try {
                switch (mediaType) {
                    case Photos.MEDIA_TYPE:
                        bitmap = Images.Thumbnails.getThumbnail(mContext.getContentResolver(), id, Images.Thumbnails.MINI_KIND,
                                null);
                        break;
                    case Messages.MEDIA_TYPE:
                        bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.icon_remind_record);
                        break;
                    case Music.MEDIA_TYPE:
                        bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.icon_remind_music);
                        break;
                    case Videos.MEDIA_TYPE:
                        if (id == null || id == -1) {
                            bitmap = BitmapFactory.decodeFile(new File(Environment.getExternalStorageDirectory(), mCursor
                                    .getString(mCursor.getColumnIndex(Media.COL_LOCAL_THUMBNAIL_PATH))).toString());
                        } else {
                            bitmap = Video.Thumbnails.getThumbnail(mContext.getContentResolver(), id,
                                    Video.Thumbnails.MINI_KIND, null);
                        }
                        break;
                }
            } catch (SecurityException ex) {
                ex.printStackTrace();
                Toast.makeText(mContext, R.string.deniedpermissiontophotos, Toast.LENGTH_LONG).show();
            }

            Matrix mtx = new Matrix();
            mtx.postRotate(rotation);

            if (bitmap == null) {
                return null;
            }

            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mtx, true);

            mCache.put(getItemId(position), new WeakReference<>(bitmap));
            return bitmap;
        }

        public void clearCache(long id) {
            mCache.remove(id);
        }
    }

    private static class LoadLargeImageTask extends AsyncTask<PhotoLoadOptions, Void, Bitmap> {

        private WeakReference<MediaViewActivity> mediaViewActivityWeakReference;

        LoadLargeImageTask(MediaViewActivity context) {
            mediaViewActivityWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Bitmap doInBackground(PhotoLoadOptions... opts) {

            MediaViewActivity activity = mediaViewActivityWeakReference.get();
            if(activity == null) return null;

            PhotoLoadOptions opt = opts[0];
            Bitmap bitmap = null;

            try {
                final Cursor image = activity.getContentResolver().query(
                        ContentUris.withAppendedId(Images.Media.EXTERNAL_CONTENT_URI, opt.getExternalId()),
                        new String[] {
                                MediaColumns.DATA
                        }, null, null, null);
                String path = null;
                if (image.moveToFirst()) {
                    path = image.getString(0);
                }
                image.close();
                bitmap = MediaUtils.sampleImage(activity.getContentResolver(), path, opt.mMaxWidth, opt.mMaxHeight,
                        opt.mRotation);
            } catch (IOException ignored) {
            }

            if (bitmap == null) {
                try {
                    final Cursor image = activity.getContentResolver().query(
                            ContentUris.withAppendedId(Images.Media.INTERNAL_CONTENT_URI, opt.getExternalId()),
                            new String[] {
                                    MediaColumns.DATA
                            }, null, null, null);
                    String path = null;
                    if (image != null && image.moveToFirst()) {
                        path = image.getString(0);
                    }
                    if (image != null) {
                        image.close();
                    }
                    bitmap = MediaUtils.sampleImage(activity.getContentResolver(), path, opt.mMaxWidth, opt.mMaxHeight,
                            opt.mRotation);
                } catch (IOException ignored) {
                }
            }

            if (bitmap == null) {
                bitmap = Images.Thumbnails.getThumbnail(activity.getContentResolver(), opt.mExternalId,
                        Images.Thumbnails.MINI_KIND, null);
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            MediaViewActivity activity = mediaViewActivityWeakReference.get();
            if(activity == null) return;

            ImageView img = (ImageView) activity.findViewById(R.id.img_large);
            if (img != null) {
                img.setImageBitmap(result);
            }
        }

    }

}
