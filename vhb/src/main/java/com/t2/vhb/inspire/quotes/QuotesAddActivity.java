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

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.t2.vhb.ActionBarActivity;
import com.t2.vhb.R;
import com.t2.vhb.db.VhbContract;
import com.t2.vhb.db.VhbContract.Quotes;

/**
 * @author wes
 */
public class QuotesAddActivity extends ActionBarActivity implements OnClickListener, OnEditorActionListener,
        TextWatcher {

    @SuppressWarnings("unused")
    private static final String TAG = "com.t2.vhb.inspire.quotes.AddQuoteActivity";

    private Uri mUri;
    private Long mId;
    private String mAuthor;
    private String mQuote;

    private EditText mQuoteText;

    @Override
    public void onClick(View v) {
        final EditText body = (EditText) findViewById(R.id.txt_quote_body);
        final EditText author = (EditText) findViewById(R.id.txt_quote_author);

        switch (v.getId()) {
            case R.id.btn_done:
                ContentValues quote = new ContentValues();

                quote.put(Quotes.COL_AUTHOR, author.getText().toString());
                quote.put(Quotes.COL_QUOTE, body.getText().toString());

                if (mId == null) {
                    quote.put(Quotes.COL_FAVORITE, true);
                    quote.put(Quotes.COL_CATEGORY, "USER_CREATED");
                    getContentResolver().insert(VhbContract.Quotes.CONTENT_URI, quote);
                } else {
                    getContentResolver().update(mUri, quote, null, null);
                }
                setResult(RESULT_OK);

                Toast.makeText(this, R.string.quotes_save_success, Toast.LENGTH_SHORT).show();
                finish();
                break;
            case R.id.btn_revert:
                author.setText(mAuthor);
                body.setText(mQuote);
        }
    }

    @Override
    public void afterTextChanged(Editable arg0) {
        findViewById(R.id.btn_done).setEnabled(mQuoteText.getText().length() > 0);
    }

    @Override
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
    }

    @Override
    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.quotes_add);

        if (getIntent().getBooleanExtra("setup", false)) {
            setContactsItemEnabled(false);
        }

        EditText mAuthorText = (EditText) findViewById(R.id.txt_quote_author);
        mQuoteText = (EditText) findViewById(R.id.txt_quote_body);

        mQuoteText.addTextChangedListener(this);
        mQuoteText.requestFocus();
        findViewById(R.id.btn_done).setOnClickListener(this);
        findViewById(R.id.btn_revert).setOnClickListener(this);

        mUri = getIntent().getData();
        if (mUri != null) {
            Cursor targetQuote = getContentResolver().query(mUri, null, null, null, null);
            if (targetQuote.moveToFirst()) {
                mId = targetQuote.getLong(targetQuote.getColumnIndex(BaseColumns._ID));
                mAuthor = targetQuote.getString(targetQuote.getColumnIndex(Quotes.COL_AUTHOR));
                mQuote = targetQuote.getString(targetQuote.getColumnIndex(Quotes.COL_QUOTE));
                mAuthorText.setText(mAuthor);
                mQuoteText.setText(mQuote);
                setTitle(R.string.quotes_edit);
            }
        }
        
        findViewById(R.id.btn_done).setEnabled(mQuoteText.getText().length() > 0);

        setTitle((mUri == null ? "Add" : "Edit") + " Quote");

    }

}
