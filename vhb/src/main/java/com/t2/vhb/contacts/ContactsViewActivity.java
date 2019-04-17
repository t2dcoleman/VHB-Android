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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.t2.vhb.R;
import com.t2.vhb.util.OnFragmentDataLoadedListener;

/**
 * @author wes
 */
public class ContactsViewActivity extends ContactsActivity implements OnFragmentDataLoadedListener {

    private boolean mShowToast;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.mnu_contacts, menu);
        return true;
    }

    @Override
    public void onFragmentDataLoaded(int newCount) {
        if (newCount == 0) {
            Intent intent = new Intent(this, ContactsHelpActivity.class);
            intent.putExtra(ContactsHelpActivity.KEY_INITIAL_HELP, true);
            startActivity(intent);
            finish();
        } else if (mShowToast) {
            Toast.makeText(this, "Add support contacts by pressing the 'Menu' button and selecting 'Add Contact'",
                    Toast.LENGTH_LONG).show();
            mShowToast = false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnu_add:
                addContact();
                return true;
            case R.id.mnu_help:
                startActivity(new Intent(this, ContactsHelpActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showHotlines(View v) {
        showHotlinesDialog();
    }

    @Override
    protected void onContactAdded(Uri uri) {
        Toast.makeText(this, R.string.contacts_add_success, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.contacts_view);
        setTitle("Support Contacts");
        setIcon(R.drawable.icon_call_someone);
        setContactsItemEnabled(false);

        mShowToast = savedInstanceState == null;

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

}
