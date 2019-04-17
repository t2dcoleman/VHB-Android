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

package com.t2.vhb.contacts;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract.Contacts;
import android.widget.Toast;

import com.t2.vhb.ActionBarActivity;
import com.t2.vhb.db.VhbContract;

import java.util.List;

public abstract class ContactsActivity extends ActionBarActivity {

    private static final int REQUEST_CONTACT = 1;
    private static final int DIALOG_HOTLINES = 1;

    void showHotlinesDialog() {
        ContactsHotlineDialog dlg = ContactsHotlineDialog.newInstance();
        dlg.show(getSupportFragmentManager(), "hotlines");
    }

    void addContact() {
        Intent selectContactIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
        selectContactIntent.setType(Contacts.CONTENT_TYPE);
        startActivityForResult(selectContactIntent, REQUEST_CONTACT);
    }

    protected abstract void onContactAdded(Uri uri);

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_CONTACT:
                // A contact was picked, add a local app reference to it.

                if (resultCode == RESULT_OK) {
                    Uri lookupUri = data.getData();
                    if (!data.getDataString().contains("lookup")) {
                        lookupUri = Contacts.getLookupUri(getContentResolver(), data.getData());
                    }

                    if (lookupUri == null) {
                        Toast.makeText(this, "Unable to save contact.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Uri saveUri = VhbContract.SupportContacts.CONTENT_URI;

                    // Contact content provider returns a URI of the form
                    // */lookupkey/lastknownid
                    List<String> segments = lookupUri.getPathSegments();
                    String key = segments.get(segments.size() - 2);
                    String id = segments.get(segments.size() - 1);

                    Cursor lookupCursor = getContentResolver().query(
                            saveUri.buildUpon().appendEncodedPath(key).build(), new String[] {
                                BaseColumns._ID
                            }, null, null, null);
                    int count = lookupCursor.getCount();
                    lookupCursor.close();

                    if (count == 0) {
                        // Save the reference
                        ContentValues supportContact = new ContentValues();
                        supportContact.put(VhbContract.SupportContacts.COL_LOOKUP_KEY, key);
                        supportContact.put(VhbContract.SupportContacts.COL_CONTACT_ID, id);

                        Uri result = getContentResolver().insert(saveUri, supportContact);
                        onContactAdded(result);

                    } else {
                        Toast.makeText(this, "Support contact already exists.", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
