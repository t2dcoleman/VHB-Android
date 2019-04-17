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

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.CursorLoader;

import java.util.Arrays;

/**
 * @author wes
 */
class ProxyCursorLoader extends CursorLoader {

    private final ContentObserver mObserver = new ForceLoadContentObserver();
    private final Uri mLocalUri;
    private final String[] mLocalProjection;
    private final SelectionGenerator mSelectionGenerator;
    private final String mLocalSelection;

    public ProxyCursorLoader(Context context, Uri localUri, String[] localProjection,
            String localSelection,
            Uri remoteUri, String[] remoteProjection, SelectionGenerator selectionGenerator,
            String sortOrder) {
        super(context);
        setUri(remoteUri);
        setProjection(remoteProjection);
        setSortOrder(sortOrder);

        //Changed to conform with the following find bugs error
        // Malicious code vulnerability - May expose internal representation by returning reference to mutable object
        mLocalProjection = Arrays.copyOf(localProjection, localProjection.length);

        mLocalUri = localUri;
        mSelectionGenerator = selectionGenerator;
        mLocalSelection = localSelection;
    }

    /*
     * (non-Javadoc)
     * @see android.support.v4.content.CursorLoader#loadInBackground()
     */
    @Override
    public Cursor loadInBackground() {
        Cursor localResults = doLocalQuery();

        if (localResults == null) {
            return null;
        }

        String selection = mSelectionGenerator.generateSelection(localResults);
        if (selection == null) {
            selection = "0";
        }
        setSelection(selection);
        localResults.close();
        Cursor proxiedResults = super.loadInBackground();
        proxiedResults.registerContentObserver(mObserver);
        proxiedResults.setNotificationUri(getContext().getContentResolver(), mLocalUri);

        return proxiedResults;
    }

    /**
     * @return
     */
    private Cursor doLocalQuery() {
        return getContext().getContentResolver().query(mLocalUri, mLocalProjection,
                mLocalSelection, null, null);
    }

    public interface SelectionGenerator {
        String generateSelection(Cursor localCursor);
    }

}
