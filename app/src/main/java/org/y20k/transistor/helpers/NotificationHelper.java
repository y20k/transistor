/**
 * NotificationHelper.java
 * Implements the NotificationHelper class
 * A NotificationHelper creates and configures a notification
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;

import org.y20k.transistor.PlayerActivity;
import org.y20k.transistor.PlayerService;
import org.y20k.transistor.R;


/**
 * NotificationHelper class
 */
public final class NotificationHelper {

    /* Keys */
    private static final int PLAYER_SERVICE_NOTIFICATION_ID = 1;
    private static final String ACTION_STOP = "org.y20k.transistor.action.STOP";


    /* Main class variables */
    private final Context mContext;
    private String mStationName;


    /* Constructor */
    public NotificationHelper(Context context) {
        mContext = context;
    }


    /* Setter for name of station */
    public void setStationName(String stationName) {
        mStationName = stationName;
    }


    /* Construct and put up notification */
    public void createNotification() {
        NotificationCompat.Builder builder;
        Notification notification;
        NotificationManager notificationManager;
        String notificationText;
        String notificationTitle;
        int notificationColor;

        // retrieve notification system service
        notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        // create content of notification
        notificationText = mContext.getString(R.string.notification_swipe_to_stop);
        notificationTitle = mContext.getString(R.string.notification_playing) + ": " + mStationName;
        notificationColor = ContextCompat.getColor(mContext, R.color.transistor_red);

        // explicit intent for notification tap
        Intent tapIntent = new Intent(mContext, PlayerActivity.class);

        // explicit intent for notification swipe
        Intent swipeIntent = new Intent(mContext, PlayerService.class);
        swipeIntent.setAction(ACTION_STOP);


        // artificial back stack for started Activity.
        // -> navigating backward from the Activity leads to Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        // backstack: adds back stack for Intent (but not the Intent itself)
        stackBuilder.addParentStack(PlayerActivity.class);
        // backstack: add explicit intent for notification tap
        stackBuilder.addNextIntent(tapIntent);


        // pending intent wrapper for notification tap
        PendingIntent tapPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        // pending intent wrapper for notification swipe
        PendingIntent swipePendingIntent = PendingIntent.getService(mContext, 0, swipeIntent, 0);

        // construct notification in builder
        builder = new NotificationCompat.Builder(mContext);
        builder.setSmallIcon(R.drawable.notification_icon_24dp);
        builder.setContentTitle(notificationTitle);
        builder.setContentText(notificationText);
        builder.setColor(notificationColor);
        // builder.setLargeIcon(largeIcon);
        builder.setContentIntent(tapPendingIntent);
        builder.setDeleteIntent(swipePendingIntent);

        // build notification
        notification = builder.build();

        // display notification
        notificationManager.notify(PLAYER_SERVICE_NOTIFICATION_ID, notification);

    }

}
