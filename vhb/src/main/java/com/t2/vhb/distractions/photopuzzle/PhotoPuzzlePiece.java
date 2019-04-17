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
package com.t2.vhb.distractions.photopuzzle;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import timber.log.Timber;

/**
 * @author wes
 * 
 */
public class PhotoPuzzlePiece implements Parcelable {

	public static final Parcelable.Creator<PhotoPuzzlePiece> CREATOR = new Parcelable.Creator<PhotoPuzzlePiece>() {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * android.os.Parcelable.Creator#createFromParcel(android.os.Parcel)
		 */
		@Override
        public PhotoPuzzlePiece createFromParcel(Parcel source) {
			return new PhotoPuzzlePiece(source);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.Parcelable.Creator#newArray(int)
		 */
		@Override
        public PhotoPuzzlePiece[] newArray(int size) {
			return new PhotoPuzzlePiece[size];
		}
	};

	private Bitmap mImage;
	private int mCorrectPosition;
	private int mCurrentPosition;

	public PhotoPuzzlePiece() {

	}

	public PhotoPuzzlePiece(Parcel in) {
		mCorrectPosition = in.readInt();
		mCurrentPosition = in.readInt();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.Parcelable#describeContents()
	 */
	@Override
    public int describeContents() {
		return 0;
	}

	/**
	 * @return the correctPosition
	 */
	public int getCorrectPosition() {
		return mCorrectPosition;
	}

	/**
	 * @return the currentPosition
	 */
	public int getCurrentPosition() {
		return mCurrentPosition;
	}

	/**
	 * @return the image
	 */
	public Bitmap getImage() {
		if (mImage == null) {
			mImage = loadFromCache();
		}
		return mImage;
	}

	/**
	 * @param correctPosition
	 *            the correctPosition to set
	 */
	public void setCorrectPosition(int correctPosition) {
		this.mCorrectPosition = correctPosition;
	}

	/**
	 * @param currentPosition
	 *            the currentPosition to set
	 */
	public void setCurrentPosition(int currentPosition) {
		this.mCurrentPosition = currentPosition;
	}

	/**
	 * @param image
	 *            the image to set
	 */
	public void setImage(Bitmap image) {
		this.mImage = image;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
    public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mCorrectPosition);
		dest.writeInt(mCurrentPosition);
	}

	private Bitmap loadFromCache() {
		Bitmap image = null;
		try {
			File cache = new File(Environment.getExternalStorageDirectory(),
					"/Android/data/com.t2.vhb/cache/photopuzzle/" + mCorrectPosition
							+ ".png");
			FileInputStream fin = new FileInputStream(cache);
			image = BitmapFactory.decodeStream(fin);
			Timber.d("Loaded cached slice: " + cache.getName());
		} catch (IOException e) {
			Timber.e(e, "Loading photo slice from cache failed");
		}
		return image;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + mCorrectPosition;
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PhotoPuzzlePiece other = (PhotoPuzzlePiece) obj;
		return mCorrectPosition == other.mCorrectPosition;
	}

}