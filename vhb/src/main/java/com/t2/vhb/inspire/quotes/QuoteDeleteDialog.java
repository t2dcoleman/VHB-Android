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

package com.t2.vhb.inspire.quotes;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.widget.Toast;

import com.t2.vhb.db.VhbContract.Quotes;

import java.util.Objects;

public class QuoteDeleteDialog extends DialogFragment {

    @Nullable
    private OnDeleteListener onDeleteListener;

    private static final String KEY_ID = "id";

    private long mId;

    public static QuoteDeleteDialog createInstance(long id) {
        QuoteDeleteDialog dlg = new QuoteDeleteDialog();
        Bundle args = new Bundle();
        args.putLong(KEY_ID, id);
        dlg.setArguments(args);
        return dlg;
    }

    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = savedInstanceState == null ? getArguments() : savedInstanceState;
        mId = args.getLong(KEY_ID);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Confirm Delete").setMessage("Are you sure you want to delete this quote?")
                .setPositiveButton("OK", (dialog, id) -> {
                    Objects.requireNonNull(getActivity()).getContentResolver().delete(Quotes.getContentUri(mId), null, null);
                    if (onDeleteListener != null) onDeleteListener.deleted();
                    Toast.makeText(getActivity(), "Quote Deleted", Toast.LENGTH_SHORT).show();
                }).setNegativeButton("Cancel", null);
        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle args) {
        super.onSaveInstanceState(args);
        args.putLong(KEY_ID, mId);
    }

    public interface OnDeleteListener {
        void deleted();
    }

    public void setOnDeleteListener(@Nullable OnDeleteListener onDeleteListener) {
        this.onDeleteListener = onDeleteListener;
    }
}
