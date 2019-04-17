/*
 *
 * Created by Wes Turney on 12/17/13.
 *
 * YouTubeLib
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
 * Government Agency Original Software Designation: YouTubeLib001
 * Government Agency Original Software Title: YouTubeLib
 * User Registration Requested. Please send email
 * with your contact information to: robert.a.kayl.civ@mail.mil
 * Government Agency Point of Contact for Original Software: robert.a.kayl.civ@mail.mil
 *
 */
package com.t2.youtube.lazylist;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {

	private final MemoryCache mMemoryCache = new MemoryCache();
	private final FileCache mFileCache;
	private final Map<ImageView, String> mImageViews = Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
	private final ExecutorService mExecutorService;

	public ImageLoader(Context context) {
		mFileCache = new FileCache(context);
		mExecutorService = Executors.newFixedThreadPool(5);
	}

	public void displayImage(String url, ImageView imageView)
	{
		mImageViews.put(imageView, url);
		Bitmap bitmap = mMemoryCache.get(url);
		if (bitmap != null)
			imageView.setImageBitmap(bitmap);
		else
		{
			queuePhoto(url, imageView);
			imageView.setImageBitmap(null);
		}
	}

	private void queuePhoto(String url, ImageView imageView)
	{
		PhotoToLoad p = new PhotoToLoad(url, imageView);
		mExecutorService.submit(new PhotosLoader(p));
	}

	private Bitmap getBitmap(String url)
	{
		File f = mFileCache.getFile(url);

		// from SD cache
		Bitmap b = decodeFile(f);
		if (b != null)
			return b;

		// from web
		try {
			Bitmap bitmap = null;
			URL imageUrl = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
			conn.setConnectTimeout(30000);
			conn.setReadTimeout(30000);
			conn.setInstanceFollowRedirects(true);
			InputStream is = conn.getInputStream();
			OutputStream os = new FileOutputStream(f);
			Utils.CopyStream(is, os);
			os.close();
			bitmap = decodeFile(f);
			return bitmap;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	// decodes image and scales it to reduce memory consumption
	private Bitmap decodeFile(File f) {
		try {
			// decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(f), null, o);

			// Find the correct scale value. It should be the power of 2.
			final int REQUIRED_SIZE = 70;
			int width_tmp = o.outWidth, height_tmp = o.outHeight;
			int scale = 1;
			while (true) {
				if (width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE)
					break;
				width_tmp /= 2;
				height_tmp /= 2;
				scale *= 2;
			}

			// decode with inSampleSize
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
		} catch (FileNotFoundException ignored) {
		}
		return null;
	}

	// Task for the queue
	private class PhotoToLoad
	{
		public final String mUrl;
		public final ImageView mImageView;

		public PhotoToLoad(String u, ImageView i) {
			mUrl = u;
			mImageView = i;
		}
	}

	class PhotosLoader implements Runnable {
		final PhotoToLoad mPhotosToLoad;

		PhotosLoader(PhotoToLoad photoToLoad) {
			this.mPhotosToLoad = photoToLoad;
		}

		@Override
        public void run() {
			if (imageViewReused(mPhotosToLoad))
				return;
			Bitmap bmp = getBitmap(mPhotosToLoad.mUrl);
			mMemoryCache.put(mPhotosToLoad.mUrl, bmp);
			if (imageViewReused(mPhotosToLoad))
				return;
			BitmapDisplayer bd = new BitmapDisplayer(bmp, mPhotosToLoad);
			Activity a = (Activity) mPhotosToLoad.mImageView.getContext();
			a.runOnUiThread(bd);
		}
	}

	private boolean imageViewReused(PhotoToLoad photoToLoad) {
		String tag = mImageViews.get(photoToLoad.mImageView);
		return tag == null || !tag.equals(photoToLoad.mUrl);
	}

	// Used to display bitmap in the UI thread
	class BitmapDisplayer implements Runnable
	{
		final Bitmap mBitmap;
		final PhotoToLoad mPhotosToLoad;

		public BitmapDisplayer(Bitmap b, PhotoToLoad p) {
			mBitmap = b;
			mPhotosToLoad = p;
		}

		@Override
        public void run()
		{
			if (imageViewReused(mPhotosToLoad))
				return;
			if (mBitmap != null)
				mPhotosToLoad.mImageView.setImageBitmap(mBitmap);
			else
				mPhotosToLoad.mImageView.setImageBitmap(null);
		}
	}

	public void clearCache() {
		mMemoryCache.clear();
		mFileCache.clear();
	}

}
