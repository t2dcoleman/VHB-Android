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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.widget.CursorAdapter;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;

import com.t2.vhb.ActionBarActivity;
import com.t2.vhb.R;
import com.t2.vhb.db.VhbContract.ActivityIdea;

public class ActivityIdeaEditActivity extends ActionBarActivity {

    private static final int DIALOG_ADD = 1;
    private static final int DIALOG_EDIT = 2;

    private long mEditIdeaId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.activity_ideas_edit);
        setTitle("Activity Ideas");
        setIcon(R.drawable.icon_distract_scheduler);

        registerForContextMenu(findViewById(android.R.id.list));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mnu_edit_activity_ideas, menu);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Cursor idea = (Cursor) getListAdapter().getItem(info.position);
        boolean favorite = idea.getInt(idea.getColumnIndex(ActivityIdea.COL_FAVORITE)) > 0;
        menu.setHeaderTitle(idea.getString(idea.getColumnIndex(ActivityIdea.COL_NAME)));
        getMenuInflater().inflate(R.menu.ctx_edit_activity_ideas, menu);
        menu.getItem(0).setTitle(favorite ? "Remove from Favorites" : "Add to Favorites");
    }

    private CursorAdapter getListAdapter() {
        return (CursorAdapter) ((ListView) findViewById(android.R.id.list)).getAdapter();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Cursor idea = (Cursor) getListAdapter().getItem(info.position);
        Uri ideaUri = ContentUris.withAppendedId(ActivityIdea.CONTENT_URI, getListAdapter().getItemId(info.position));

        switch (item.getItemId()) {
            case R.id.mnu_edit_activity:
                mEditIdeaId = idea.getLong(idea.getColumnIndex(BaseColumns._ID));
                showDialog(DIALOG_EDIT);
                break;
            case R.id.mnu_remove_activity:
                getContentResolver().delete(ideaUri, null, null);
                break;
            case R.id.mnu_favorite:
                boolean favorite = idea.getInt(idea.getColumnIndex(ActivityIdea.COL_FAVORITE)) > 0;
                ContentValues vals = new ContentValues();
                vals.put(ActivityIdea.COL_FAVORITE, !favorite);
                getContentResolver().update(ideaUri, vals, null, null);
                break;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnu_add_activity:
                showDialog(DIALOG_ADD);
                break;
        }
        return true;
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_ADD:
            case DIALOG_EDIT:
                return new AlertDialog.Builder(this)
                        .setView(getLayoutInflater().inflate(R.layout.dialog_activity_idea, null))
                        .setTitle("Add activity idea").setPositiveButton("OK", (dialog, which) -> {
                            ContentValues vals = new ContentValues();
                            String name = ((EditText) ((Dialog) dialog).findViewById(R.id.txt_activity_name))
                                    .getText().toString();
                            vals.put(ActivityIdea.COL_NAME, name);

                            if (mEditIdeaId < 0) {
                                vals.put(ActivityIdea.COL_FAVORITE, true);
                                getContentResolver().insert(ActivityIdea.CONTENT_URI, vals);
                            } else {
                                getContentResolver().update(
                                        ContentUris.withAppendedId(ActivityIdea.CONTENT_URI, mEditIdeaId), vals,
                                        null, null);
                            }
                            getListAdapter().notifyDataSetChanged();
                            mEditIdeaId = -1;
                        }).setNegativeButton("Cancel", (dialog, which) -> mEditIdeaId = -1).create();
        }
        return super.onCreateDialog(id, args);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        super.onPrepareDialog(id, dialog, args);

        EditText nameField = (EditText) dialog.findViewById(R.id.txt_activity_name);
        switch (id) {
            case DIALOG_ADD:
                dialog.setTitle("Add activity idea");
                nameField.setText("");
                break;
            case DIALOG_EDIT:
                dialog.setTitle("Edit activity idea");
                Cursor idea = getContentResolver().query(
                        ContentUris.withAppendedId(ActivityIdea.CONTENT_URI, mEditIdeaId), null, null, null, null);
                if (idea.moveToFirst()) {
                    nameField.setText(idea.getString(idea.getColumnIndex(ActivityIdea.COL_NAME)));
                }
                idea.close();
                break;
        }
    }

}
