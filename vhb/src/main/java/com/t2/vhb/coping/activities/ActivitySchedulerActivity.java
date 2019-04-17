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

package com.t2.vhb.coping.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.t2.vhb.ActionBarActivity;
import com.t2.vhb.R;
import com.t2.vhb.db.VhbContract.ActivityIdea;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import timber.log.Timber;

public class ActivitySchedulerActivity extends ActionBarActivity implements OnClickListener, LoaderCallbacks<Cursor> {

    private static final int LOADER_IDEAS = 1;

    private static final int REQUEST_CONTACTS = 1;
    private static final int REQUEST_SMS = 2;
    private static final int REQUEST_EMAIL = 3;
    private static final int REQUEST_CALENDAR = 4;

    private static final int DIALOG_DATE = 1;
    private static final int DIALOG_TIME = 2;
    private static final int DIALOG_NOTIFY = 3;
    private static final int DIALOG_CALENDAR = 4;
    private static final int DIALOG_CONTACTS = 5;

    private ArrayList<Uri> mContacts = new ArrayList<>();
    private ArrayList<String> mContactNames = new ArrayList<>();
    private final ArrayList<String> mContactsMissingInfo = new ArrayList<>();

    private IdeaAdapter mIdeaAdapter;
    private CalendarAdapter mCalendarAdapter;
    private boolean mEmail;

    private long mDate;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.invite:
                Intent intent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
                startActivityForResult(intent, REQUEST_CONTACTS);
                break;
            case R.id.btn_remove:
                ViewGroup parent = (ViewGroup) v.getParent();
                int index = mContacts.indexOf(parent.getTag());
                mContacts.remove(index);
                mContactNames.remove(index);
                ViewGroup grandParent = (ViewGroup) parent.getParent();
                grandParent.removeView(parent);
                findViewById(R.id.btn_send_invites).setEnabled(!mContacts.isEmpty());
                break;
            case R.id.btn_save_to_calendar:
                showDialog(DIALOG_CALENDAR);
                break;
            case R.id.btn_send_invites:
                if (mContacts.isEmpty()) {
                    return;
                }
                showDialog(DIALOG_NOTIFY);
                break;
            case R.id.btn_date:
                showDialog(DIALOG_DATE);
                break;
            case R.id.btn_time:
                showDialog(DIALOG_TIME);
                break;
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return new CursorLoader(this, ActivityIdea.CONTENT_URI, null, null, null, ActivityIdea.COL_FAVORITE + " DESC, "
                + ActivityIdea.COL_NAME + " ASC");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.mnu_activity_ideas, menu);
        return true;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        mIdeaAdapter.swapCursor(null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
        mIdeaAdapter.swapCursor(data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnu_edit_activities:
                Intent intent = new Intent(this, ActivityIdeaEditActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CONTACTS) {
            if (resultCode == RESULT_OK) {
                if (mContacts.contains(data.getData())) {
                    return;
                }
                mContacts.add(data.getData());

                View row = getLayoutInflater().inflate(R.layout.activity_scheduler_contact_row, null);
                String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
                if (name == null) {
                    Cursor cursor = null;
                    try {
                        cursor = getContentResolver().query(data.getData(), new String[]{
                                ContactsContract.Contacts.DISPLAY_NAME
                        }, null, null, null);
                    } catch (SecurityException ex) {
                        Timber.e(ex);
                        Toast.makeText(this, R.string.deniedpermissiontocalendar, Toast.LENGTH_LONG).show();
                    }
                    if (cursor == null || !cursor.moveToFirst()) {
                        return;
                    }
                    name = cursor.getString(0);
                    cursor.close();
                }
                mContactNames.add(name);
                ((TextView) row.findViewById(R.id.lbl_name)).setText(name);
                Bitmap photo = null;
                try {
                    photo = BitmapFactory.decodeStream(Contacts.openContactPhotoInputStream(getContentResolver(),
                            ContentUris.withAppendedId(Contacts.CONTENT_URI, ContentUris.parseId(data.getData()))));
                } catch (SecurityException ex) {
                    Timber.e(ex);
                    Toast.makeText(this, R.string.deniedpermissiontocontacts, Toast.LENGTH_LONG).show();
                }

                createContactRow(name, photo, data.getData());
                findViewById(R.id.btn_send_invites).setEnabled(!mContacts.isEmpty());
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);


        setContentView(R.layout.activity_scheduler);
        setTitle("Activity Planner");
        setIcon(R.drawable.icon_distract_scheduler);

        if (savedInstanceState != null) {
            mContacts = savedInstanceState.getParcelableArrayList("contacts");
            mContactNames = savedInstanceState.getStringArrayList("names");
            mDate = savedInstanceState.getLong("date");
            updateDateDisplay();
            createContactRows();
        } else {
            final Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, 12);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            mDate = c.getTimeInMillis();
            updateDateDisplay();
        }

        mCalendarAdapter = new CalendarAdapter(this);
        if (mCalendarAdapter.isEmpty()) {
            findViewById(R.id.btn_save_to_calendar).setVisibility(View.GONE);
        }

        mIdeaAdapter = new IdeaAdapter(this);
        ((Spinner) findViewById(R.id.spn_activity)).setAdapter(mIdeaAdapter);
        getSupportLoaderManager().initLoader(LOADER_IDEAS, null, this);

        findViewById(R.id.invite).setOnClickListener(this);
        findViewById(R.id.btn_send_invites).setOnClickListener(this);
        findViewById(R.id.btn_save_to_calendar).setOnClickListener(this);
        findViewById(R.id.btn_date).setOnClickListener(this);
        findViewById(R.id.btn_time).setOnClickListener(this);

    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        switch (id) {
            case DIALOG_CONTACTS:
                ((AlertDialog) dialog).setMessage("The following invitees do not have "
                        + (mEmail ? "an email address" : "a phone number")
                        + " and will not be included in this invite " + (mEmail ? "email" : "message") + "\n\n    "
                        + TextUtils.join("\n    ", mContactsMissingInfo) + "\n\nPress OK to continue sending");

        }

    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle bundle) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(mDate);
        switch (id) {
            case DIALOG_DATE:
                return new DatePickerDialog(this, (view, year, monthOfYear, dayOfMonth) -> {
                    final Calendar cal12 = Calendar.getInstance();
                    cal12.setTimeInMillis(mDate);
                    cal12.set(Calendar.YEAR, year);
                    cal12.set(Calendar.MONTH, monthOfYear);
                    cal12.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    mDate = cal12.getTimeInMillis();

                    updateDateDisplay();
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            case DIALOG_TIME:
                return new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                    final Calendar cal1 = Calendar.getInstance();
                    cal1.setTimeInMillis(mDate);
                    cal1.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    cal1.set(Calendar.MINUTE, minute);
                    mDate = cal1.getTimeInMillis();
                    updateDateDisplay();
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false);
            case DIALOG_NOTIFY:
                return new AlertDialog.Builder(this).setTitle("Send invite via").setItems(new String[]{
                        "Email", "Text Messaging"
                }, (dialog, which) -> {
                    mContactsMissingInfo.clear();
                    switch (which) {
                        case 0:
                            mEmail = true;
                            sendEmail(true);
                            break;
                        case 1:
                            mEmail = false;
                            sendMessage(true);
                            break;
                    }
                }).create();
            case DIALOG_CALENDAR:
                return new AlertDialog.Builder(this).setTitle("Choose a calendar")
                        .setAdapter(mCalendarAdapter, (dialog, which) -> saveToCalender(mCalendarAdapter.getItemId(which))).create();
            case DIALOG_CONTACTS:
                return new AlertDialog.Builder(this).setTitle("Invitees unreachable").setMessage("Blah")
                        .setIcon(R.drawable.ic_dialog_alert)
                        .setPositiveButton("OK", (dialog, which) -> {
                            if (mEmail) {
                                sendEmail(false);
                            } else {
                                sendMessage(false);
                            }
                        }).setNegativeButton("Cancel", null).create();
        }

        return null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("contacts", mContacts);
        outState.putStringArrayList("names", mContactNames);
        outState.putLong("date", mDate);
    }

    @SuppressLint({
            "InlinedApi", "NewApi"
    })
    private void addAttendee(long eventId, Uri attendee) {

        final ContentValues attendeesValues = new ContentValues();
        final String email = getContactEmail(ContentUris.parseId(attendee));
        final String name = getContactDisplayName(attendee);

        attendeesValues.put(CalendarContract.Attendees.ATTENDEE_EMAIL, email);
        attendeesValues.put(CalendarContract.Attendees.EVENT_ID, eventId);
        attendeesValues.put(CalendarContract.Attendees.ATTENDEE_NAME, name);
        attendeesValues.put(CalendarContract.Attendees.ATTENDEE_RELATIONSHIP,
                CalendarContract.Attendees.RELATIONSHIP_ATTENDEE);
        attendeesValues.put(CalendarContract.Attendees.ATTENDEE_TYPE, CalendarContract.Attendees.TYPE_NONE);
        attendeesValues
                .put(CalendarContract.Attendees.ATTENDEE_STATUS, CalendarContract.Attendees.STATUS_TENTATIVE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_CALENDAR}, REQUEST_CALENDAR);
        }

        getContentResolver().insert(CalendarContract.Attendees.CONTENT_URI, attendeesValues);

    }

    private String getContactDisplayName(Uri contactUri) {
        Cursor contact = null;
        try {
            contact = getContentResolver().query(contactUri, null, null, null, null);
        } catch (SecurityException ex) {
            Timber.e(ex);
            Toast.makeText(this, R.string.deniedpermissiontocontacts, Toast.LENGTH_LONG).show();
        }
        String name = null;
        if (contact != null && contact.moveToFirst()) {
            name = contact.getString(contact.getColumnIndex(Contacts.DISPLAY_NAME));
            contact.close();
        }

        return name;
    }

    private String getContactEmail(long contactId) {
        Cursor contact = null;
        try {
            contact = getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?", new String[]{
                            Long.toString(contactId)
                    }, null);
        } catch (SecurityException ex) {
            Timber.e(ex);
            Toast.makeText(this, R.string.deniedpermissiontocontacts, Toast.LENGTH_LONG).show();
        }

        String email = null;
        if (contact != null && contact.moveToFirst()) {
            email = contact.getString(contact.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
            contact.close();
        }

        return email;
    }

    private void addAttendeeLegacy(long eventId, Uri attendee) {
        final String attendeuesesUriString = "content://com.android.calendar/attendees";
        final ContentValues attendeesValues = new ContentValues();
        final String email = getContactEmail(ContentUris.parseId(attendee));
        final String name = getContactDisplayName(attendee);

        attendeesValues.put("event_id", eventId);
        attendeesValues.put("attendeeName", name); // Attendees
        attendeesValues.put("attendeeRelationship", 1);
        attendeesValues.put("attendeeType", 0);
        attendeesValues.put("attendeeStatus", 0);
        attendeesValues.put("attendeeEmail", email);

        getContentResolver().insert(Uri.parse(Uri.decode(attendeuesesUriString)), attendeesValues);
    }

    private void createContactRow(String name, Bitmap photo, Uri uri) {
        View row = getLayoutInflater().inflate(R.layout.activity_scheduler_contact_row, null);
        row.findViewById(R.id.btn_remove).setOnClickListener(this);
        if (photo != null) {
            ((ImageView) row.findViewById(R.id.img_photo)).setImageBitmap(photo);
        }
        ((TextView) row.findViewById(R.id.lbl_name)).setText(name);
        row.setTag(uri);

        ViewGroup rows = (ViewGroup) findViewById(R.id.lay_activity);
        rows.addView(row, rows.getChildCount() - 1);
    }

    private void createContactRows() {
        String name = null;
        for (Uri uri : mContacts) {
            name = getContactDisplayName(uri);
            Bitmap photo = BitmapFactory.decodeStream(Contacts.openContactPhotoInputStream(getContentResolver(),
                    ContentUris.withAppendedId(Contacts.CONTENT_URI, ContentUris.parseId(uri))));

            createContactRow(name, photo, uri);
        }

        findViewById(R.id.btn_send_invites).setEnabled(!mContacts.isEmpty());
    }

    private Cursor getActivityIdea() {
        return (Cursor) ((Spinner) findViewById(R.id.spn_activity)).getSelectedItem();
    }

    private String getEventDescription() {
        Cursor idea = getActivityIdea();
        StringBuilder title = new StringBuilder(idea.getString(idea.getColumnIndex(ActivityIdea.COL_NAME)));
        if (!mContactNames.isEmpty()) {
            title.append(" with ").append(TextUtils.join(", ", mContactNames));
        }
        return title.toString();
    }

    private String getEventMessage() {
        StringBuilder sb = new StringBuilder();

        if (mContacts.size() == 1) {
            Cursor person = null;
            try {
                person = getContentResolver().query(ContactsContract.Data.CONTENT_URI, new String[]{
                        ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, Contacts.DISPLAY_NAME
                }, ContactsContract.Data.CONTACT_ID + " = " + ContentUris.parseId(mContacts.get(0)), null, null);
            } catch (SecurityException ex) {
                Timber.e(ex);
                Toast.makeText(this, R.string.deniedpermissiontocontacts, Toast.LENGTH_LONG).show();
            }
            String name = "";
            if (person != null && person.moveToFirst()) {
                name = person.getString(0);
                if (name == null) {
                    name = person.getString(1);
                }
                person.close();
            }
            sb.append("Hey ").append(name.split(" ")[0]).append(", ");
        } else {
            sb.append("Hey all, ");
        }

        Cursor idea = getActivityIdea();

        if (!idea.isNull(idea.getColumnIndex(ActivityIdea.COL_VERB))) {
            String verb = idea.getString(idea.getColumnIndex(ActivityIdea.COL_VERB));
            String name = idea.getString(idea.getColumnIndex(ActivityIdea.COL_NAME));
            sb.append("would you like to ")
                    .append(verb)
                    .append(verb.length() > 0 ? " " : "")
                    .append(name.toLowerCase(Locale.US))
                    .append(" with me on ")
                    .append(DateUtils.formatDateTime(this, getEventStartTime(), DateUtils.FORMAT_SHOW_DATE
                            | DateUtils.FORMAT_SHOW_WEEKDAY))
                    .append(" at ")
                    .append(DateUtils
                            .formatDateTime(
                                    this,
                                    getEventStartTime(),
                                    (DateUtils.FORMAT_12HOUR | DateUtils.FORMAT_CAP_AMPM | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_NO_NOON_MIDNIGHT)
                                            & ~DateUtils.FORMAT_CAP_AMPM)).append("?");
        } else {
            sb.append(getEventSubject());
        }

        return sb.toString();
    }

    private long getEventStartTime() {
        return mDate;
    }

    private String getEventSubject() {
        Cursor idea = getActivityIdea();
        StringBuilder sb = new StringBuilder(idea.getString(idea.getColumnIndex(ActivityIdea.COL_NAME)));
        sb.append(" at ")
                .append(DateUtils.formatDateTime(this, getEventStartTime(), (DateUtils.FORMAT_12HOUR
                        | DateUtils.FORMAT_CAP_AMPM | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_NO_NOON_MIDNIGHT)
                        & ~DateUtils.FORMAT_CAP_AMPM))
                .append(" on ")
                .append(DateUtils.formatDateTime(this, getEventStartTime(), DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_SHOW_WEEKDAY)).append("?");
        return sb.toString();
    }

    @SuppressLint({
            "InlinedApi", "NewApi"
    })
    private long saveToCalender(long calendarId) {
        final Cursor idea = getActivityIdea();

        final Map<String, String> data = new HashMap<>();
        data.put("Activity", idea.getString(idea.getColumnIndex(ActivityIdea.COL_NAME)));

        final ContentValues eventValues = new ContentValues();
        final String title = idea.getString(idea.getColumnIndex(ActivityIdea.COL_NAME));
        final long endDate = getEventStartTime() + (1000 * 60 * 60);

        eventValues.put(CalendarContract.Events.CALENDAR_ID, calendarId);
        eventValues.put(CalendarContract.Events.TITLE, title);
        eventValues.put(CalendarContract.Events.DESCRIPTION, getEventDescription());
        eventValues.put(CalendarContract.Events.EVENT_LOCATION, "");
        eventValues.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
        eventValues.put(CalendarContract.Events.DTSTART, getEventStartTime());
        eventValues.put(CalendarContract.Events.DTEND, endDate);
        eventValues.put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_TENTATIVE);
        eventValues.put(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_PUBLIC);
        eventValues.put(CalendarContract.Events.HAS_ALARM, 1);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_CALENDAR}, REQUEST_CALENDAR);
        }
        final Uri eventUri = getContentResolver().insert(CalendarContract.Events.CONTENT_URI, eventValues);
        final long eventID = Long.parseLong(eventUri.getLastPathSegment());

        final ContentValues reminderValues = new ContentValues();
        reminderValues.put(CalendarContract.Reminders.EVENT_ID, eventID);
        reminderValues.put(CalendarContract.Reminders.MINUTES, 60);
        reminderValues.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_DEFAULT);

        getContentResolver().insert(CalendarContract.Reminders.CONTENT_URI, reminderValues);

        for (Uri contact : mContacts) {
            addAttendee(eventID, contact);
        }

        Toast.makeText(this, "Event saved to calendar", Toast.LENGTH_SHORT).show();
        return eventID;

    }

    private long saveToCalendarLegacy(long calendarId) {
        final String eventUriString = "content://com.android.calendar/events";
        final String reminderUriString = "content://com.android.calendar/reminders";

        final ContentValues eventValues = new ContentValues();
        final Cursor idea = getActivityIdea();
        final String title = idea.getString(idea.getColumnIndex(ActivityIdea.COL_NAME));
        final long endDate = getEventStartTime() + (1000 * 60 * 60);

        eventValues.put("calendar_id", calendarId);
        eventValues.put("title", title);
        eventValues.put("description", getEventDescription());
        eventValues.put("eventLocation", "");
        eventValues.put("dtstart", getEventStartTime());
        eventValues.put("dtend", endDate);
        eventValues.put("eventStatus", 0);
        eventValues.put("visibility", 3);
        eventValues.put("transparency", 0);
        eventValues.put("hasAlarm", 1);

        final Uri eventUri = getContentResolver().insert(Uri.parse(Uri.decode(eventUriString)), eventValues);
        final long eventID = Long.parseLong(eventUri.getLastPathSegment());

        final ContentValues reminderValues = new ContentValues();

        reminderValues.put("event_id", eventID);
        reminderValues.put("minutes", 60);
        reminderValues.put("method", 1);

        getContentResolver().insert(Uri.parse(Uri.decode(reminderUriString)), reminderValues);

        for (Uri contact : mContacts) {
            addAttendee(eventID, contact);
        }

        Toast.makeText(this, "Event saved to calendar", Toast.LENGTH_SHORT).show();
        return eventID;
    }

    private void sendEmail(boolean displayWarnings) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");

        if (getPackageManager().queryIntentActivities(intent, 0).isEmpty()) {
            Toast.makeText(this, "No email client is configured on this device. Unable to send the message.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String> emails = new ArrayList<>();
        for (int i = 0; i < mContacts.size(); i++) {
            Uri uri = mContacts.get(i);
            Cursor email = null;
            try {
                email = getContentResolver().query(Email.CONTENT_URI, new String[]{
                        Email.DATA
                }, Email.CONTACT_ID + " = ?", new String[]{
                        ContentUris.parseId(uri) + ""
                }, null);
            } catch (SecurityException ex) {
                Timber.e(ex);
                Toast.makeText(this, R.string.deniedpermissiontocontacts, Toast.LENGTH_LONG).show();
            }
            if (email != null && email.moveToFirst()) {
                emails.add(mContactNames.get(i) + " <" + email.getString(0) + ">");
                email.close();
            } else {
                mContactsMissingInfo.add(mContactNames.get(i));
            }
        }

        if (displayWarnings && !mContactsMissingInfo.isEmpty()) {
            showDialog(DIALOG_CONTACTS);
            return;
        }

        intent.putExtra(Intent.EXTRA_EMAIL, emails.toArray(new String[] {}));
        intent.putExtra(Intent.EXTRA_SUBJECT, getEventSubject());
        intent.putExtra(Intent.EXTRA_TEXT, getEventMessage());


        startActivityForResult(intent, REQUEST_EMAIL);
    }

    private void sendMessage(boolean displayWarnings) {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            Toast.makeText(this, "This device does not support text messaging", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String> numbers = new ArrayList<>();
        for (int i = 0; i < mContacts.size(); i++) {
            Uri contactUri = mContacts.get(i);
            String name = mContactNames.get(i);
            Cursor phone = null;
            try {
                phone = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{
                        ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.TYPE
                }, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{
                        ContentUris.parseId(contactUri) + ""
                }, null);
            } catch (SecurityException ex) {
                Timber.e(ex);
                Toast.makeText(this, R.string.deniedpermissiontocontacts, Toast.LENGTH_LONG).show();
            }

            String number = null;
            if (phone != null && phone.moveToFirst()) {
                int priority = -1;
                int type = -1;
                do {
                    type = phone.getInt(1);
                    if (type == Phone.TYPE_MOBILE) {
                        number = phone.getString(0);
                        break;
                    } else if (type == Phone.TYPE_HOME && priority < 2) {
                        priority = 2;
                        number = phone.getString(0);
                    } else if (type == Phone.TYPE_WORK && priority < 1) {
                        priority = 1;
                        number = phone.getString(0);
                    }
                } while (phone.moveToNext());
                phone.close();
            } else {
                mContactsMissingInfo.add(name);
            }

            if (number != null) {
                numbers.add(number);
            }
        }

        if (displayWarnings && !mContactsMissingInfo.isEmpty()) {
            showDialog(DIALOG_CONTACTS);
            return;
        }

        String separator = "; ";
        if (android.os.Build.MANUFACTURER.contains("Samsung")) {
            separator = ", ";
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + TextUtils.join(separator, numbers)));
        intent.putExtra("sms_body", getEventMessage());


        startActivityForResult(intent, REQUEST_SMS);
    }

    private void updateDateDisplay() {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(mDate);

        ((Button) findViewById(R.id.btn_date)).setText(DateUtils.formatDateTime(this, cal.getTimeInMillis(),
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY));
        ((Button) findViewById(R.id.btn_time))
                .setText(DateUtils.formatDateTime(this, cal.getTimeInMillis(), (DateUtils.FORMAT_12HOUR
                        | DateUtils.FORMAT_CAP_AMPM | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_CAP_NOON_MIDNIGHT)
                        & ~DateUtils.FORMAT_CAP_AMPM));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALENDAR) {
            if(grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // We can now safely use the API we requested access to
                Timber.v("Permission Granted");
            } else {
                // Permission was denied or request was cancelled
                this.finish();
            }
        }

    }

    private static class CalendarAdapter extends ArrayAdapter<String> implements SpinnerAdapter {

        final ArrayList<Long> ids = new ArrayList<>();

        @SuppressLint("NewApi")
        public CalendarAdapter(Context context) {
            super(context, android.R.layout.select_dialog_item);

            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(Calendars.CONTENT_URI, null,
                        Calendars.CALENDAR_ACCESS_LEVEL + " >= " + Calendars.CAL_ACCESS_CONTRIBUTOR, null, null);
            } catch (SecurityException ex) {
                ex.printStackTrace();
                Toast.makeText(getContext(), R.string.deniedpermissiontocalendar, Toast.LENGTH_LONG).show();
            }

            if (cursor == null) {
                return;
            }

            if (cursor.moveToFirst()) {
                do {
                    ids.add(cursor.getLong(cursor.getColumnIndex("_id")));
                    add(cursor.getString(cursor.getColumnIndex(Calendars.CALENDAR_DISPLAY_NAME)));
                } while (cursor.moveToNext());
                cursor.close();
            }

            notifyDataSetChanged();
        }

        @Override
        public long getItemId(int position) {
            return ids.get(position);
        }
    }

    private static class IdeaAdapter extends SimpleCursorAdapter implements SpinnerAdapter {

        public IdeaAdapter(Context context) {
            super(context, android.R.layout.simple_spinner_item, null, new String[] {
                ActivityIdea.COL_NAME
            }, new int[] {
                android.R.id.text1
            }, 0);
            setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        }

    }
}
