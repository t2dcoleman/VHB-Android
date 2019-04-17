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

package com.t2.vhb.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Video;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.t2.vhb.ActionBarActivity;
import com.t2.vhb.R;
import com.t2.vhb.db.VhbContract.Media;
import com.t2.vhb.inspire.quotes.QuotesViewActivity;
import com.t2.vhb.media.MediaViewActivity;
import com.t2.vhb.media.PhotoLoadOptions;
import com.t2.vhb.tools.CopingTool;
import com.t2.vhb.util.MediaUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.SecureRandom;
import java.util.Random;

import timber.log.Timber;

/**
 * @author wes
 */
public class HomeActivity extends ActionBarActivity implements OnClickListener {

    private static final String TAG = "com.t2.vhb.HomeActivity";

    public static final String EXTRA_QUOTE_PASSTHROUGH = "quote";

    private static final int REQUEST_SETTINGS = 1;
    private static final int DIALOG_BACKUP = 3;
    private static final int SWIPE_MIN_DISTANCE = 100;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    private static final int REQUEST_AUTHORIZATION = 2;


    private CountDownTimer mTimer;
    private CopingTool mParentTool;
    private boolean mFavoritesOnly;
    private long mCurrentReminder;
    private WeakReference<Bitmap> mReminder;
    private boolean mSplashShown;
    private GestureDetector mGestureDetector;

    private View mReminderView;

    private class GestureListener extends SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) {
                return false;
            }

            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                nextReminder();
                setupTimer();
                return true; // Right to left
            } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                nextReminder();
                setupTimer();
                return true; // Left to right
            }
            return false;
        }
    }



    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, CategoryListActivity.class);
        switch (v.getId()) {
            case R.id.img_reminder:
                intent = new Intent(HomeActivity.this, MediaViewActivity.class);
                intent.putExtra(MediaViewActivity.KEY_REMINDER_ID, mCurrentReminder);
                break;
            case R.id.btn_distract:
            case R.id.img_distract:
                intent.putExtra("parent_tool", CopingTool.DISTRACTIONS.name());
                break;
            case R.id.btn_relax:
            case R.id.img_relax:
                intent.putExtra("parent_tool", CopingTool.RELAX.name());
                break;
            case R.id.btn_inspire:
            case R.id.img_inspire:
                startActivity(new Intent(this, QuotesViewActivity.class));
                return;
            case R.id.btn_coping:
            case R.id.img_coping:
                intent.putExtra("parent_tool", CopingTool.COPING.name());
                break;
        }
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mnu_home, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnu_about_us:
                startActivity(new Intent(this, AboutUsActivity.class));
                break;
            case R.id.mnu_settings:
                Intent intent = new Intent(getApplicationContext(), HomeSettingsActivity.class);
                startActivityForResult(intent, REQUEST_SETTINGS);
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    @SuppressWarnings("rawtypes")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.home);
        setTitle("Virtual Hope Box");
        setIcon(R.drawable.icon_vhb_gray);


        mGestureDetector = new GestureDetector(this, new GestureListener());

        if (!isEulaAccepted()) {
            Intent eulaIntent = new Intent(getApplicationContext(), EulaActivity.class);
            startActivity(eulaIntent);
            finish();
            return;
        } else if (!isSetupComplete()) {
            Intent setupIntent = new Intent(getApplicationContext(), WelcomeActivity.class);
            startActivity(setupIntent);
            finish();
            return;
        }

        mReminderView = findViewById(R.id.img_reminder);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (savedInstanceState == null) {
            MediaUtils.restoreMediaReferences(getContentResolver());

            Bundle bundle = getIntent().getExtras();
            Class clazz = null;
            if (bundle != null && bundle.containsKey("initial_activity")) {
                clazz = (Class) bundle.getSerializable("initial_activity");
                Intent intent = new Intent(this, clazz);
                startActivity(intent);
            }
        } else {
            mSplashShown = savedInstanceState.getBoolean("splash_shown");
            mFavoritesOnly = savedInstanceState.getBoolean("only_favorites");
            String parentToolName = savedInstanceState.getString("parent_tool");
            if (parentToolName != null) {
                mParentTool = CopingTool.valueOf(parentToolName);
            }
        }

        if (!prefs.getBoolean(getString(R.string.pref_reminder_toast_shown), false)) {
            Toast toast = Toast
                    .makeText(this, "Tap here to view or manage\nyour personal reminders", Toast.LENGTH_LONG);
            if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 200);
            } else {
                toast.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL, 100, 0);
            }
            toast.show();
            prefs.edit().putBoolean(getString(R.string.pref_reminder_toast_shown), true).apply();
        }

        findViewById(R.id.img_inspire).setOnClickListener(this);
        findViewById(R.id.img_coping).setOnClickListener(this);
        findViewById(R.id.img_distract).setOnClickListener(this);
        findViewById(R.id.img_relax).setOnClickListener(this);
        findViewById(R.id.btn_distract).setOnClickListener(this);
        findViewById(R.id.btn_relax).setOnClickListener(this);
        findViewById(R.id.btn_inspire).setOnClickListener(this);
        findViewById(R.id.btn_coping).setOnClickListener(this);

        mReminderView.setClickable(true);
        mReminderView.setOnFocusChangeListener((v, hasFocus) -> ((ImageButton) v).setColorFilter(hasFocus ? Color.argb(0x60, 0xFF, 0xFF, 0xFF) : Color.argb(0x0, 0x0,
                0x0, 0x0)));
        mReminderView.setOnClickListener(this);
        mReminderView.setOnTouchListener((v, event) -> mGestureDetector.onTouchEvent(event));

        mReminderView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @SuppressLint("NewApi")
            @Override
            public void onGlobalLayout() {
                mReminderView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                setupTimer();
                if (mSplashShown) {
                    nextReminder();
                }
            }
        });

        if (getIntent() != null) {
            handleLaunchIntent(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleLaunchIntent(intent);
    }

    private void handleLaunchIntent(Intent intent) {
        if (intent.hasExtra(EXTRA_QUOTE_PASSTHROUGH)) {
            final Intent quoteIntent = new Intent(this, QuotesViewActivity.class);
            final long quoteId = intent.getLongExtra(EXTRA_QUOTE_PASSTHROUGH, -1);
            quoteIntent.putExtra(QuotesViewActivity.EXTRA_INITIAL_QUOTE_ID, quoteId);
            startActivity(quoteIntent);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("splash_shown", mSplashShown);
        if (mParentTool != null) {
            outState.putString("parent_tool", mParentTool.name());
        }
        outState.putBoolean("only_favorites", mFavoritesOnly);
    }

    private void setupTimer() {
        stopTimer();
        int delay = 6000;
        if (!mSplashShown) {
            delay = 4000;
        }
        mTimer = new CountDownTimer(delay, 1000) {
            @Override
            public void onFinish() {
                if (mSplashShown) {
                    nextReminder();
                    start();
                } else {
                    mSplashShown = true;
                    setupTimer();
                }
            }

            @Override
            public void onTick(long millisUntilFinished) {
            }
        };
        startTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            MediaUtils.purgeMediaReferences(getContentResolver(), Media.Messages.MEDIA_TYPE);
            MediaUtils.purgeMediaReferences(getContentResolver(), Media.Music.MEDIA_TYPE);
            MediaUtils.purgeMediaReferences(getContentResolver(), Media.Photos.MEDIA_TYPE);
            MediaUtils.purgeMediaReferences(getContentResolver(), Media.Videos.MEDIA_TYPE);
            MediaUtils.purgeMediaReferences(getContentResolver(), Media.BreathingMusic.MEDIA_TYPE);
        }

        setupTimer();
    }

    private void nextReminder() {
        Cursor cursor = null;
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            cursor = getContentResolver().query(Media.CONTENT_URI, null,
                    Media.COL_MEDIA_TYPE + " IN ('" + TextUtils.join("','", new String[]{
                            Media.Videos.MEDIA_TYPE, Media.Photos.MEDIA_TYPE
                    }) + "') AND " + Media.COL_INACTIVE + " = 0" + " AND " + BaseColumns._ID + " != " + mCurrentReminder,
                    null, null);
        }
        if (cursor != null && cursor.getCount() != 0) {
            Random rand = new SecureRandom();
            int index = rand.nextInt(cursor.getCount());
            cursor.moveToPosition(index);

            mCurrentReminder = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
            Long extId = cursor.getLong(cursor.getColumnIndex(Media.COL_EXTERNAL_ID));
            int rotation = cursor.getInt(cursor.getColumnIndex(Media.COL_ROTATION));
            String mediaType = cursor.getString(cursor.getColumnIndex(Media.COL_MEDIA_TYPE));

            if (Media.Videos.MEDIA_TYPE.equals(mediaType)) {
                Bitmap swap = null;
                if (extId == -1) {
                    swap = BitmapFactory.decodeFile(new File(Environment.getExternalStorageDirectory(), cursor
                            .getString(cursor.getColumnIndex(Media.COL_LOCAL_THUMBNAIL_PATH))).toString());
                } else {
                    swap = Video.Thumbnails.getThumbnail(this.getContentResolver(), extId, Video.Thumbnails.MINI_KIND,
                            null);
                }

                swapReminders(swap);
            } else {
                ImageView remView = (ImageView) findViewById(R.id.img_reminder);
                new LoadPhotoTask(this).execute(new PhotoLoadOptions(extId, remView.getWidth(), remView.getHeight(), rotation));
            }
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    private void swapReminders(Bitmap newReminder) {
        Animation anim = new AlphaAnimation(1.0f, 0.0f);
        anim.setAnimationListener(new ReminderAnimation(newReminder));
        anim.setDuration(600);
        ((ImageView) findViewById(R.id.img_reminder)).startAnimation(anim);
    }

    private boolean isSetupComplete() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getBoolean(getString(R.string.pref_setup_complete), false);
    }

    private void startTimer() {
        if (mTimer == null) {
            return;
        }
        mTimer.start();
    }

    private void stopTimer() {
        if (mTimer == null) {
            return;
        }
        mTimer.cancel();
    }

    private class ReminderAnimation implements AnimationListener {

        private Bitmap mSwap;

        public ReminderAnimation(Bitmap swap) {
            mSwap = swap;
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            ((ImageView) findViewById(R.id.img_reminder)).setImageBitmap(mSwap);
            if (mReminder != null && mReminder.get() != null) {
                mReminder.get().recycle();
            }
            mReminder = new WeakReference<>(mSwap);
            Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(600);
            fadeIn.setFillAfter(true);
            ((ImageView) findViewById(R.id.img_reminder)).startAnimation(fadeIn);
            mSwap = null;
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }

    }

    private boolean isEulaAccepted() {
        return PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(
                getString(R.string.pref_setup_eula_accepted), false);
    }


    private static class LoadPhotoTask extends AsyncTask<PhotoLoadOptions, Void, Bitmap> {

        private WeakReference<HomeActivity> homeActivityWeakReference;

        LoadPhotoTask(HomeActivity context) {
            homeActivityWeakReference = new WeakReference<>(context);
        }


        @Override
        protected Bitmap doInBackground(PhotoLoadOptions... opts) {

            HomeActivity activity = homeActivityWeakReference.get();
            if (activity == null) return null;

            PhotoLoadOptions opt = opts[0];
            Bitmap bitmap = null;

            try(final Cursor media = activity.getContentResolver().query(
                    ContentUris.withAppendedId(Images.Media.EXTERNAL_CONTENT_URI, opt.getExternalId()),
                    new String[]{
                            MediaColumns.DATA
                    }, null, null, null)) {
                String path = null;
                if (media != null && media.moveToFirst()) {
                    path = media.getString(0);
                }

                try {
                    bitmap = MediaUtils.sampleImage(activity.getContentResolver(), path, opt.getMaxWidth(),
                            opt.getMaxHeight(), opt.getRotation());
                } catch (IOException e) {
                    Timber.e(e, "Unable to load reminder image id: %s", opt.getExternalId());
                }

                if (bitmap == null) {
                    bitmap = Images.Thumbnails.getThumbnail(activity.getContentResolver(), opt.getExternalId(),
                            Images.Thumbnails.MINI_KIND, null);
                }

                return bitmap;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            HomeActivity activity = homeActivityWeakReference.get();
            if(activity == null) return;
            activity.swapReminders(result);
        }
    }
}
