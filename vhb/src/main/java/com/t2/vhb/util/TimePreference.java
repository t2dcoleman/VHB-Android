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
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TimePicker;

import com.t2.vhb.R;

import java.util.Calendar;

public class TimePreference extends DialogPreference {
    private TimePicker mTimePicker;
    private CheckBox mEnabled;

    private static final String AM = "AM";
    private static final String PM = "PM";

    public TimePreference(Context context, AttributeSet attributes) {
        super(context, attributes);
    }

    @Override
    public void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mTimePicker = (TimePicker) view.findViewById(R.id.time_picker);
        mEnabled = (CheckBox) view.findViewById(R.id.enabled);

        final String value = getPersistedString(null);
        final boolean valueSet = !TextUtils.isEmpty(value);
        mEnabled.setChecked(valueSet);
        mTimePicker.setEnabled(mEnabled.isChecked());

        int hour;
        int minute;
        if (valueSet) {
            final String[] values = value.split(":");
            hour = Integer.parseInt(values[0]);
            minute = Integer.parseInt(values[1]);
        } else {
            final Calendar cal = Calendar.getInstance();
            hour = cal.get(Calendar.HOUR_OF_DAY);
            minute = cal.get(Calendar.MINUTE);
        }

        mTimePicker.setCurrentHour(hour);
        mTimePicker.setCurrentMinute(minute);

        mEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> mTimePicker.setEnabled(isChecked));

        setSummary(getTimeDescription());
    }

    @Override
    public void onClick(DialogInterface dialog, int button) {
        if (button == DialogInterface.BUTTON_POSITIVE) {
            String result = mEnabled.isChecked() ? mTimePicker.getCurrentHour() + ":"
                    + mTimePicker.getCurrentMinute() : null;

            if (callChangeListener(result)) {
                persistString(result);
                notifyChanged();
                setSummary(getTimeDescription());
            }
        }
    }

    public String getTimeDescription() {
        String value = getPersistedString(null);
        if (value == null) {
            return "Disabled";
        } else {
            String[] values = value.split(":");
            final int hour = Integer.parseInt(values[0]);
            final int minute = Integer.parseInt(values[1]);

            final StringBuilder out = new StringBuilder();

            boolean pm = hour >= 12;

            if (hour > 12) {
                out.append(hour - 12);
            } else if (hour == 0) {
                out.append(hour + 12);
            } else {
                out.append(hour);
            }

            out.append(":");

            if (minute < 10) {
                out.append("0").append(minute);
            } else {
                out.append(minute);
            }

            out.append(" ").append(pm ? PM : AM);
            return out.toString();
        }
    }
}
