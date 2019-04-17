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

package com.t2.vhb.home;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.t2.vhb.ActionBarActivity;
import com.t2.vhb.R;
import com.t2.vhb.tools.CopingTool;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wes
 */
public class CategoryListActivity extends ActionBarActivity implements LoaderCallbacks<Uri>, OnItemClickListener {

    private static final int DIALOG_STATISTICS = 1;

    private CopingTool mParentTool;
    private boolean mFavoritesOnly;

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_STATISTICS:
                ProgressDialog dlg = new ProgressDialog(this);
                dlg.setIndeterminate(true);
                dlg.setTitle("Send Log");
                dlg.setMessage("Please wait, generating log file...");
                return dlg;
        }
        return super.onCreateDialog(id);
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View v, int arg2, long arg3) {
        CopingTool tool = (CopingTool) v.getTag();
        tool.startActivity(this);
    }

    @Override
    public Loader<Uri> onCreateLoader(int id, Bundle args) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Uri> loader, Uri data) {
        final Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {
                ""
        });
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "VHB Log File");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Attached is an encrypted VHB log file.");
        emailIntent.putExtra(Intent.EXTRA_STREAM, data);
        dismissDialog(DIALOG_STATISTICS);
        startActivity(Intent.createChooser(emailIntent, "Send mail..."));
    }

    @Override
    public void onLoaderReset(Loader<Uri> loader) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.coping_tool_view);

        Bundle bundle = savedInstanceState;
        if (savedInstanceState == null) {
            bundle = getIntent().getExtras();
        }

        if (bundle != null) {
            mFavoritesOnly = bundle.getBoolean("only_favorites");
            String parentToolName = bundle.getString("parent_tool");
            if (parentToolName != null) {
                mParentTool = CopingTool.valueOf(parentToolName);
            }
        }

        if (mParentTool != null) {
            setTitle(mParentTool.getName());
            setIcon(mParentTool.getIcon());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboard();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mParentTool != null) {
            outState.putString("parent_tool", mParentTool.name());
        }
        outState.putBoolean("only_favorites", mFavoritesOnly);
    }

    private ListView getDashboard() {
        return (ListView) findViewById(R.id.lst_dashboard);
    }

    private void loadDashboard() {
        List<CopingTool> tools = new ArrayList<>();
        for (CopingTool tool : mParentTool.getSubTools()) {
            // if (tool.getParent() == null) {
            tools.add(tool);
            // }
        }
        getDashboard().setAdapter(
                new CopingToolAdapter(this, R.layout.coping_tool_row, tools.toArray(new CopingTool[] {})));
        getDashboard().setOnItemClickListener(this);
        getDashboard().requestFocus();
    }

    private static final class CopingToolAdapter extends ArrayAdapter<CopingTool> {

        public CopingToolAdapter(Context context, int textViewResourceId, CopingTool[] tools) {
            super(context, textViewResourceId, tools);

        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = LayoutInflater.from(getContext()).inflate(R.layout.coping_tool_row, null);
            }

            ImageView img = (ImageView) v.findViewById(R.id.btn_coping_tool);

            CopingTool tool = getItem(position);

            StateListDrawable sld = new StateListDrawable();
            sld.addState(new int[] {
                    android.R.attr.state_selected
            }, new ColorDrawable(tool.getFocusColor()));
            sld.addState(new int[] {
                    android.R.attr.state_focused
            }, new ColorDrawable(tool.getFocusColor()));
            sld.addState(new int[] {}, new ColorDrawable(tool.getColor()));
            v.setBackgroundDrawable(sld);

            ((TextView) v.findViewById(R.id.lbl_tool)).setText(tool.getName());
            v.setTag(tool);
            if (tool.getIcon() > 0) {
                img.setImageResource(tool.getIcon());
            }

            return v;

        }
    }

}
