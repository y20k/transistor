/**
 * NotificationHelper.java
 * Implements the NotificationHelper class
 * A NotificationHelper creates and configures a notification
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-16 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import org.y20k.transistor.MainActivity;
import org.y20k.transistor.PlayerService;
import org.y20k.transistor.R;
import org.y20k.transistor.core.Collection;
import org.y20k.transistor.core.Station;


/**
 * NotificationHelper class
 */
public final class NotificationHelper {

    /* Define log tag */
    private static final String LOG_TAG = NotificationHelper.class.getSimpleName();


    /* Keys */
    private static final int PLAYER_SERVICE_NOTIFICATION_ID = 1;
    private static final String ACTION_STOP = "org.y20k.transistor.action.STOP";
    private static final String ACTION_SHOW_PLAYER = "org.y20k.transistor.action.SHOW_PLAYER";
    private static final String EXTRA_STATION_ID = "STATION_ID";


    /* Main class variables */
    private static Collection mCollection;
    private static MediaSessionCompat mSession;
    private static String mStationMetadata;
    private static int mStationID;
    private static String mStationName;


    /* Constructor */
    public NotificationHelper(Collection collection) {
        mCollection = collection;
    }


    /* Create and put up notification */
    public static void createNotification(final Context context) {
        NotificationCompat.Builder builder;
        Notification notification;
        NotificationManager notificationManager;
        String notificationText;
        String notificationTitle;
        int notificationColor;

        // retrieve notification system service
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // create content of notification
        notificationColor = ContextCompat.getColor(context, R.color.transistor_red);
        notificationTitle = context.getString(R.string.notification_playing) + ": " + mStationName;
        if (mStationMetadata != null) {
            notificationText = mStationMetadata;
        } else {
            notificationText = mStationName;
        }

        // explicit intent for notification tap
        Intent tapIntent = new Intent(context, MainActivity.class);
        tapIntent.setAction(ACTION_SHOW_PLAYER);
        tapIntent.putExtra(EXTRA_STATION_ID, mStationID);

        // explicit intent for notification swipe
        Intent stopActionIntent = new Intent(context, PlayerService.class);
        stopActionIntent.setAction(ACTION_STOP);


        // artificial back stack for started Activity.
        // -> navigating backward from the Activity leads to Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // backstack: adds back stack for Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // backstack: add explicit intent for notification tap
        stackBuilder.addNextIntent(tapIntent);


        // pending intent wrapper for notification tap
        PendingIntent tapPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        // pending intent wrapper for notification stop action
        PendingIntent stopActionPendingIntent = PendingIntent.getService(context, 0, stopActionIntent, 0);

        // construct notification in builder
        builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_notification_small_24dp);
        builder.setLargeIcon(getStationIcon(context));
        builder.setContentTitle(notificationTitle);
        builder.setContentText(notificationText);
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(notificationText));
        builder.addAction (R.drawable.ic_stop_black_36dp, context.getString(R.string.notification_stop), stopActionPendingIntent);
        builder.setOngoing(true);
        builder.setColor(notificationColor);
        builder.setContentIntent(tapPendingIntent);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        if (mSession != null) {
            NotificationCompat.MediaStyle style = new NotificationCompat.MediaStyle();
            style.setMediaSession(mSession.getSessionToken());
            style.setShowActionsInCompactView(0);
            builder.setStyle(style);

        } else {
            Log.v(LOG_TAG, "mSession NOT initialized"); // TODO remove this
        }

        // build notification
        notification = builder.build();

        // display notification
        notificationManager.notify(PLAYER_SERVICE_NOTIFICATION_ID, notification);

    }


    /* Get station image for notification's large icon */
    private static Bitmap getStationIcon(Context context) {

        // get station from collection
        Station station = mCollection.getStations().get(mStationID);

        // create station image icon
        ImageHelper imageHelper;
        Bitmap stationImage;
        Bitmap stationIcon;

        if (station.getStationImageFile().exists()) {
            // use station image
            stationImage = BitmapFactory.decodeFile(station.getStationImageFile().toString());
        } else {
            stationImage = null;
        }
        imageHelper = new ImageHelper(stationImage, context);
        stationIcon = imageHelper.createStationIcon(512);

        return stationIcon;

    }


    /* Setter for current media session */
    public static void setMediaSession(MediaSessionCompat session) {
        mSession = session;
    }

    /* Setter for name of station */
    public static void setStationName(String stationName) {
        mStationName = stationName;
    }

    /* Setter for name of station */
    public static void setStationID(int stationID) {
        mStationID = stationID;
    }

    /* Setter for metadata of station */
    public static void setStationMetadata(String stationMetadata) {
        mStationMetadata = stationMetadata;
    }

}
