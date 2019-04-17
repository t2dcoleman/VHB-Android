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

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.LruCache;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.t2.vhb.R;
import com.t2.vhb.db.VhbContract.SupportContacts;
import com.t2.vhb.util.OnFragmentDataLoadedListener;

import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author wes
 */
public class ContactsListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

    private static final int LOADER_CONTACTS = 1;
    private ContactAdapter mAdapter;
    private boolean mShowEmergencyLines;

    private class SupportContact implements Comparable<SupportContact> {
        private final String mName;
        private final Uri mLocalUri;
        private final Uri mExternalUri;

        public SupportContact(String name, Uri localUri, Uri externalUri) {
            super();
            mName = name;
            mLocalUri = localUri;
            mExternalUri = externalUri;
        }

        @Override
        public int compareTo(SupportContact another) {
            return mName.compareTo(another.mName);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mShowEmergencyLines = savedInstanceState.getBoolean("emergency");
        }

        mAdapter = new ContactAdapter(getActivity(), new ArrayList<>());
        setListAdapter(mAdapter);

        getListView().setCacheColorHint(Color.TRANSPARENT);

        getLoaderManager().initLoader(LOADER_CONTACTS, null, this);

        registerForContextMenu(getListView());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("emergency", mShowEmergencyLines);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnu_remove:
                AdapterView.AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                        .getMenuInfo();

                SupportContact contact = mAdapter.getItem(info.position);
                getActivity().getContentResolver().delete(contact.mLocalUri,
                        null, null);

                Toast.makeText(getActivity(), R.string.contacts_remove_success, Toast.LENGTH_SHORT)
                        .show();
                return true;
        }
        return false;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent intent = new Intent(Intent.ACTION_VIEW, mAdapter.getItem(position).mExternalUri);
        startActivity(intent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        menu.setHeaderTitle(((TextView) info.targetView.findViewById(R.id.lbl_name)).getText());
        getActivity().getMenuInflater().inflate(R.menu.ctx_contacts, menu);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), SupportContacts.CONTENT_URI,
                null, null, null, null);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.clear();
    }

    private List<SupportContact> loadContacts(Cursor localContacts) {
        List<SupportContact> contacts = new ArrayList<>();

        String lookupKey;
        long contactId;
        Uri externalUri = null, localUri, lookupUri;
        Cursor contact = null;
        if (localContacts.moveToFirst()) {
            do {
                lookupKey = localContacts.getString(localContacts.getColumnIndex(SupportContacts.COL_LOOKUP_KEY));
                contactId = localContacts.getLong(localContacts.getColumnIndex(SupportContacts.COL_CONTACT_ID));
                localUri = ContentUris.withAppendedId(SupportContacts.CONTENT_URI,
                        localContacts.getLong(localContacts.getColumnIndex(BaseColumns._ID)));
                lookupUri = Contacts.getLookupUri(contactId, lookupKey);
                try {
                    externalUri = Contacts.lookupContact(getActivity().getContentResolver(), lookupUri);
                } catch (SecurityException ex) {
                    ex.printStackTrace();
                }

                if (externalUri == null) {
                    // Contact doesn't exist anymore
                    getActivity().getContentResolver().delete(localUri, null, null);
                    Toast.makeText(getActivity(), "Unable to locate contact: " + lookupKey + "/" + contactId + " or denied permission to access Contacts",
                            Toast.LENGTH_LONG).show();
                } else {
                    try {
                        contact = getActivity().getContentResolver().query(externalUri, new String[]{
                                Contacts.DISPLAY_NAME
                        }, null, null, null);
                    } catch (SecurityException ex) {
                        ex.printStackTrace();
                        Toast.makeText(getActivity(), R.string.deniedpermissiontocontacts, Toast.LENGTH_LONG).show();
                    }
                    if(contact != null) {
                        if (contact.moveToFirst()) {
                            contacts.add(new SupportContact(contact.getString(0), localUri, externalUri));
                        }
                        contact.close();
                    }
                }
            } while (localContacts.moveToNext());
        }

        Collections.sort(contacts);
        return contacts;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter = new ContactAdapter(getActivity(), loadContacts(data));
        setListAdapter(mAdapter);

        if (getActivity() instanceof OnFragmentDataLoadedListener) {
            int count = data != null ? data.getCount() : 0;
            ((OnFragmentDataLoadedListener) getActivity()).onFragmentDataLoaded(count);
        }

        if (!(getActivity() instanceof ContactsViewActivity)) {
            getListView().requestFocus();
        }
    }

    /**
     * An adapter that takes lookup key/id pair references and uses them to
     * populate from the contact content provider.
     * 
     * @author wes
     */
    private final class ContactAdapter extends ArrayAdapter<SupportContact> {

        private final LruCache<String, SoftReference<Bitmap>> mThumbnailCache;
        private final Drawable mDefaultThumbnail;
        private final LayoutInflater mInf;

        public ContactAdapter(Context context, List<SupportContact> data) {
            super(context, 0, data);
            mDefaultThumbnail = context.getResources().getDrawable(R.drawable.ic_contact_picture);
            mThumbnailCache = new LruCache<>(20);
            mInf = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup group) {
            View v = convertView;
            if (v == null) {
                v = mInf.inflate(R.layout.contacts_list_row, null);
            }

            final TextView name = (TextView) v.findViewById(R.id.lbl_name);
            final ImageView avatar = (ImageView) v.findViewById(R.id.img_avatar);
            final SupportContact contact = getItem(position);

            name.setText(contact.mName);
            final Bitmap thumb = getThumbnail(contact.mExternalUri);
            if (thumb == null) {
                avatar.setImageDrawable(mDefaultThumbnail);
            } else {
                avatar.setImageBitmap(thumb);
            }

            return v;
        }

        /**
         * Attempts to obtain a thumbnail image for the given contact Uri.
         * Maintains an LruCache of soft references to the bitmaps
         * 
         * @param contactUri
         * @return
         */
        private Bitmap getThumbnail(Uri contactUri) {
            String key = contactUri.toString();
            SoftReference<Bitmap> thumbRef = mThumbnailCache.get(key);
            Bitmap thumb = thumbRef != null ? thumbRef.get() : null;

            if (thumb == null) {
                InputStream in = Contacts.openContactPhotoInputStream(
                        getContext().getContentResolver(), contactUri);
                if (in != null) {
                    thumb = BitmapFactory.decodeStream(in);
                    mThumbnailCache.put(key, new SoftReference<>(thumb));
                }
            }

            if (thumb != null) {
                mThumbnailCache.put(key, new SoftReference<>(thumb));
            }

            return thumb;
        }
    }

}
