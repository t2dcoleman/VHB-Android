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

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AlphabetIndexer;
import android.widget.CheckBox;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

import com.t2.vhb.R;
import com.t2.vhb.db.VhbContract.Quotes;
import com.t2.vhb.util.OnFragmentDataLoadedListener;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author wes
 */
public class QuotesListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

    private static final int REQUEST_EDIT_QUOTE = 1;
    private static final int LOADER_QUOTES = 1;

    private SimpleCursorAdapter mAdapter;

    public void loadQuotes() {
        getLoaderManager().restartLoader(LOADER_QUOTES, null, this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(LOADER_QUOTES, null, this);

        mAdapter = new QuoteAdapter(getActivity());

        setListAdapter(mAdapter);
        getListView().setCacheColorHint(Color.TRANSPARENT);

        registerForContextMenu(getListView());
        getListView().setFastScrollEnabled(true);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        long id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
        Uri quoteUri = Quotes.getContentUri(id);

        switch (item.getItemId()) {
            case R.id.mnu_remove:
                if(mAdapter.getCount() > 1) {
                    QuoteDeleteDialog frg = QuoteDeleteDialog.createInstance(id);
                    if (this.getFragmentManager() != null) {
                        frg.show(this.getFragmentManager(), "delete");
                    }
                } else {
                    Toast.makeText(this.getActivity(), "You cant delete every quote", Toast.LENGTH_LONG)
                            .show();
                }
                break;
            case R.id.mnu_edit:
                Intent intent = new Intent(getActivity(), QuotesAddActivity.class);
                intent.setData(quoteUri);
                startActivityForResult(intent, REQUEST_EDIT_QUOTE);
                break;
            case R.id.mnu_favorite:
                ContentValues vals = new ContentValues();
                boolean favorite = cursor.getInt(cursor.getColumnIndex(Quotes.COL_FAVORITE)) == 1;
                vals.put(Quotes.COL_FAVORITE, favorite ? 0 : 1);
                getActivity().getContentResolver().update(quoteUri, vals, null, null);
                Toast.makeText(getActivity(), favorite ? "Quote removed from favorites." : "Quote added to favorites.",
                        Toast.LENGTH_SHORT).show();
                break;
            default:
                return super.onContextItemSelected(item);
        }
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

        getActivity().getMenuInflater().inflate(R.menu.ctx_quotes, menu);
        AdapterView.AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Cursor quote = (Cursor) mAdapter.getItem(info.position);
        menu.setHeaderTitle(quote.getString(quote.getColumnIndex(Quotes.COL_QUOTE)));

        boolean favorite = quote.getInt(quote.getColumnIndex(Quotes.COL_FAVORITE)) > 0;
        menu.getItem(0).setTitle(favorite ? "Remove from Favorites" : "Add to Favorites");
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Set<String> cats = new HashSet<>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String catNames = prefs.getString("hidden_quote_categories", null);
        if (catNames != null) {
            String[] split = catNames.split("\\|");
            cats.addAll(Arrays.asList(split));
        }

        String select = null;
        if (!cats.isEmpty()) {
            select = Quotes.COL_CATEGORY + " IS NULL OR " + Quotes.COL_CATEGORY + " NOT IN ('"
                    + TextUtils.join("','", cats) + "')";
        }

        return new CursorLoader(getActivity(), Quotes.CONTENT_URI, new String[] {
                BaseColumns._ID, "IFNULL(" + Quotes.COL_AUTHOR + ", 'Unknown') AS " + Quotes.COL_AUTHOR,
                Quotes.COL_QUOTE, Quotes.COL_FAVORITE
        }, select, null, Quotes.COL_AUTHOR + " ASC, " + Quotes.COL_QUOTE + " ASC");
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        if (getActivity() instanceof OnFragmentDataLoadedListener) {
            int count = data != null ? data.getCount() : 0;
            ((OnFragmentDataLoadedListener) getActivity()).onFragmentDataLoaded(count);
        }
        getListView().requestFocus();
    }

    public final class QuoteAdapter extends SimpleCursorAdapter implements SectionIndexer {

        private AlphabetIndexer mIndexer;

        public QuoteAdapter(Context context) {
            super(context, R.layout.quotes_list_row, null, new String[] {
                    Quotes.COL_AUTHOR, Quotes.COL_QUOTE
            }, new int[] {
                    R.id.lbl_quote_author, R.id.lbl_quote_body
            }, 0);
            mIndexer = new AlphabetIndexer(null, 0, "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        }

        @Override
        public int getPositionForSection(int section) {
            return mIndexer.getPositionForSection(section);
        }

        @Override
        public int getSectionForPosition(int position) {
            return mIndexer.getSectionForPosition(position);
        }

        @Override
        public Object[] getSections() {
            return mIndexer.getSections();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View v = super.getView(position, convertView, parent);
            final Cursor cursor = (Cursor) getItem(position);
            final CheckBox cb = (CheckBox) v.findViewById(R.id.chk_favorite);

            final boolean favorite = cursor.getInt(cursor.getColumnIndex(Quotes.COL_FAVORITE)) > 0;
            cb.setTag(position);
            cb.setChecked(favorite);
            cb.setOnClickListener(v1 -> {
                CheckBox cb1 = (CheckBox) v1;
                ContentValues vals = new ContentValues();
                vals.put(Quotes.COL_FAVORITE, cb1.isChecked());
                mContext.getContentResolver().update(
                        ContentUris.withAppendedId(Quotes.CONTENT_URI, getItemId((Integer) cb1.getTag())), vals,
                        null, null);
                Toast.makeText(getActivity(),
                        cb1.isChecked() ? "Quote added to favorites." : "Quote removed from favorites.",
                        Toast.LENGTH_SHORT).show();
                logFavorite(cb1);
            });

            final TextView body = (TextView) v.findViewById(R.id.lbl_quote_body);
            final TextView author = (TextView) v.findViewById(R.id.lbl_quote_author);
            v.setContentDescription(String.format("%s. %s. %s.", body.getText().toString(),
                    author.getText().toString(), (favorite ? "Favorite quote." : "")));
            return v;
        }

        @Override
        public void setViewText(TextView v, String text) {
            String finalText = text;
            if (v.getId() == R.id.lbl_quote_author && (text == null || text.trim().length() == 0)) {
                finalText = "Unknown";
            }

            super.setViewText(v, finalText);
        }

        @Override
        public Cursor swapCursor(Cursor c) {
            if (c != null) {
                mIndexer = new AlphabetIndexer(c, c.getColumnIndex(Quotes.COL_AUTHOR), "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
            }
            return super.swapCursor(c);
        }

        private void logFavorite(CheckBox cb) {
            ViewGroup vg = (ViewGroup) cb.getParent();
            TextView tv = (TextView) vg.findViewById(R.id.lbl_quote_body);
            String quote = TextUtils.substring(tv.getText(), 0, Math.min(50, tv.getText().length())).replace("\"",
                    "\"\"");
        }

    }
}
