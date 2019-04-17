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

package com.t2.vhb.widget;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;

import com.t2.vhb.R;
import com.t2.vhb.db.VhbContract;
import com.t2.vhb.db.VhbContract.Quotes;
import com.t2.vhb.db.VhbProvider;
import com.t2.vhb.home.HomeActivity;
import com.t2.vhb.inspire.quotes.QuotesViewActivity;

import java.security.SecureRandom;
import java.util.Random;

public class QuoteWidgetProvider extends AppWidgetProvider {

    @Override
    public void onReceive(Context ctxt, Intent intent) {
        if (intent.getAction() == null) {
            ctxt.startService(new Intent(ctxt, UpdateService.class));
        }
        else {
            super.onReceive(ctxt, intent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        if(context != null) {
            context.startService(new Intent(context, UpdateService.class));
        }
    }

    public static final class UpdateService extends IntentService {

        public UpdateService() {
            super("QuoteWidgetProvider$UpdateService");
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            ComponentName me = new ComponentName(this, QuoteWidgetProvider.class);
            AppWidgetManager mgr = AppWidgetManager.getInstance(this);

            mgr.updateAppWidget(me, buildUpdate(this));
        }

        private RemoteViews buildUpdate(Context context) {
            RemoteViews updateViews = new RemoteViews(context.getPackageName(),
                    R.layout.quote_widget);

            String resName = getSharedPreferences(getPackageName(), 0).getString("widget_background", "widget_grey");
            int resId = context.getResources().getIdentifier(resName, "drawable", getPackageName());
            updateViews.setInt(R.id.background, "setBackgroundResource", resId);

            try(Cursor cursor = VhbProvider.db.query(VhbContract.Quotes.TABLE_NAME, new String[] {
                    Quotes.COL_QUOTE, Quotes.COL_AUTHOR
            }, null, null, null, null, null)) {
                cursor.moveToFirst();

                Random rand = new SecureRandom();
                int index = rand.nextInt(cursor.getCount());
                cursor.moveToPosition(index);

                updateViews.setTextViewText(R.id.lbl_quote_author, cursor.getString(1));
                updateViews.setTextViewText(R.id.lbl_quote_body, "\"" + cursor.getString(0) + "\"");

                Intent i = new Intent(this, QuoteWidgetProvider.class);
                PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
                updateViews.setOnClickPendingIntent(R.id.refresh, pi);

                i = new Intent(this, HomeActivity.class);
                i.putExtra("initial_activity", QuotesViewActivity.class);
                pi = PendingIntent.getActivity(context, 0, i, 0);
                updateViews.setOnClickPendingIntent(R.id.lbl_quote_body, pi);
                updateViews.setOnClickPendingIntent(R.id.lbl_quote_author, pi);

            }

            return updateViews;

        }
    }
}
