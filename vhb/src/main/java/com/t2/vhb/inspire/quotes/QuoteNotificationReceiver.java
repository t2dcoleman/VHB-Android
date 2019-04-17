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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.provider.BaseColumns;
import android.support.v4.app.NotificationCompat;

import com.t2.vhb.R;
import com.t2.vhb.db.VhbContract.Quotes;
import com.t2.vhb.home.HomeActivity;

import java.security.SecureRandom;
import java.util.Random;

public class QuoteNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final Cursor quotes = context.getContentResolver().query(Quotes.CONTENT_URI, new String[] {
                BaseColumns._ID, Quotes.COL_QUOTE, "IFNULL(" + Quotes.COL_AUTHOR + ", 'Unknown') AS " + Quotes.COL_AUTHOR
        }, null, null, null);

        if (quotes != null && quotes.moveToFirst()) {
            final int count = quotes.getCount();
            final Random rnd = new SecureRandom();
            quotes.moveToPosition(rnd.nextInt(count));
            final long id = quotes.getLong(0);
            String body = quotes.getString(1);
            String author = quotes.getString(2);
            final boolean bodyFits = body.length() < 120;
            if (!bodyFits) {
                body = body.substring(0, 117) + "...";
            }

            String channelId = "quote_reminder";
            CharSequence channelName = "Quote Reminder";

            Intent notificationIntent = new Intent(context, HomeActivity.class);
            notificationIntent.putExtra(HomeActivity.EXTRA_QUOTE_PASSTHROUGH, id);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            Notification not = new NotificationCompat.Builder(context, channelId)
                    .setAutoCancel(true)
                    .setContentTitle("VHB Inspiring Quote")
                    .setTicker("VHB Quote Reminder")
                    .setWhen(System.currentTimeMillis())
                    .setContentIntent(contentIntent)
                    .setSmallIcon(R.drawable.ic_notification_quote)
                    .setContentText(body)
                    .build();
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel = new NotificationChannel(channelId,
                        channelName,
                        NotificationManager.IMPORTANCE_HIGH);
                notificationChannel.enableLights(true);
                notificationChannel.setLightColor(Color.YELLOW);
                notificationChannel.enableVibration(true);
                notificationChannel.setVibrationPattern(new long[]{0, 150});
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(notificationChannel);
                }
            }

            if (notificationManager != null) {
                notificationManager.notify(1, not);
            }
        }

        if (quotes != null) {
            quotes.close();
        }
    }
}
