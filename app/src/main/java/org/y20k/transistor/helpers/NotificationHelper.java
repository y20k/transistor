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
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import org.y20k.transistor.MainActivity;
import org.y20k.transistor.PlayerService;
import org.y20k.transistor.R;
import org.y20k.transistor.core.Station;


/**
 * NotificationHelper class
 */
public final class NotificationHelper {

    /* Define log tag */
    private static final String LOG_TAG = NotificationHelper.class.getSimpleName();


    /* Main class variables */
    private static MediaSessionCompat mSession;
    private static String mStationMetadata;
    private static int mStationID;
    private static String mStationName;
    private static Service mLastUsedService;
    private static Station mStation;


    /* Initializes the NotificationHelper */
    public static void initialize(Station station, int stationID) {
        mStation = station;
        mStationID = stationID;
        mStationName = station.getStationName();
    }


    /* Updates the notification */
    public static void updateNotification() {
        if (mLastUsedService == null) {
            Log.i(LOG_TAG, "PlayerService not started yet, cannot create notification");
            return;
        }
        createNotification(mLastUsedService);
    }


    /* Create and put up notification */
    public static void createNotification(final Service service) {
        NotificationCompat.Builder builder;
        Notification notification;
        // NotificationManager notificationManager;
        String notificationText;
        String notificationTitle;
        // int notificationColor;

        // retrieve notification system service
        // notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

        // create content of notification
        // notificationColor = ContextCompat.getColor(service, R.color.transistor_red_dark);
        notificationTitle = service.getString(R.string.notification_playing) + ": " + mStationName;
        if (mStationMetadata != null) {
            notificationText = mStationMetadata;
        } else {
            notificationText = mStationName;
        }

        // explicit intent for notification tap
        Intent tapIntent = new Intent(service, MainActivity.class);
        tapIntent.setAction(TransistorKeys.ACTION_SHOW_PLAYER);
        tapIntent.putExtra(TransistorKeys.EXTRA_STATION, mStation);
        tapIntent.putExtra(TransistorKeys.EXTRA_STATION_ID, mStationID);

        // explicit intent for notification swipe
        Intent stopActionIntent = new Intent(service, PlayerService.class);
        stopActionIntent.setAction(TransistorKeys.ACTION_STOP);


        // artificial back stack for started Activity.
        // -> navigating backward from the Activity leads to Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(service);
        // backstack: adds back stack for Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // backstack: add explicit intent for notification tap
        stackBuilder.addNextIntent(tapIntent);


        // pending intent wrapper for notification tap
        PendingIntent tapPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        // pending intent wrapper for notification stop action
        PendingIntent stopActionPendingIntent = PendingIntent.getService(service, 0, stopActionIntent, 0);

        // construct notification in builder
        builder = new NotificationCompat.Builder(service);
        builder.setSmallIcon(R.drawable.ic_notification_small_24dp);
        builder.setLargeIcon(getStationIcon(service));
        builder.setContentTitle(notificationTitle);
        builder.setContentText(notificationText);
        builder.setShowWhen(false);
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(notificationText));
        builder.addAction (R.drawable.ic_stop_white_36dp, service.getString(R.string.notification_stop), stopActionPendingIntent);
        builder.setOngoing(true);
        // builder.setColor(notificationColor);
        builder.setContentIntent(tapPendingIntent);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        if (mSession != null) {
            NotificationCompat.MediaStyle style = new NotificationCompat.MediaStyle();
            style.setMediaSession(mSession.getSessionToken());
            style.setShowActionsInCompactView(0);
            builder.setStyle(style);

        } else {
            Log.e(LOG_TAG, "MediaSession not initialized");
        }

        // build notification
        notification = builder.build();

        // display notification
        // System will never kill a service which has a foreground notification,
        // but it will kill a service without notification, so you open few other apps and get a notification and no music
        service.startForeground(TransistorKeys.PLAYER_SERVICE_NOTIFICATION_ID, notification);

        if (mLastUsedService != service) {
            mLastUsedService = service;
        }
    }


    /* Get station image for notification's large icon */
    private static Bitmap getStationIcon(Context context) {
        if (mStation == null) {
            return null;
        }

        // create station image icon
        ImageHelper imageHelper;
        Bitmap stationImage;
        Bitmap stationIcon;

        if (mStation.getStationImageFile().exists()) {
            // use station image
            stationImage = BitmapFactory.decodeFile(mStation.getStationImageFile().toString());
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

    /* Setter for metadata of station */
    public static void setStationMetadata(String stationMetadata) {
        mStationMetadata = stationMetadata;
    }

}
