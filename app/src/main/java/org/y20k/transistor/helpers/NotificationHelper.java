/**
 * NotificationHelper.java
 * Implements the NotificationHelper class
 * A NotificationHelper creates and configures a notification
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-17 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.media.session.MediaSessionCompat;

import org.y20k.transistor.MainActivity;
import org.y20k.transistor.PlayerService;
import org.y20k.transistor.R;
import org.y20k.transistor.core.Station;


/**
 * NotificationHelper class
 */
public final class NotificationHelper implements TransistorKeys {

    /* Define log tag */
    private static final String LOG_TAG = NotificationHelper.class.getSimpleName();


    /* Main class variables */
    private static Notification mNotification;
    private static MediaSessionCompat mSession;


    /* Create and put up notification */
    public static void show(Service service, MediaSessionCompat session, Station station) {
        // save service and session
        mSession = session;

        // create notification channel
        createNotificationChannel(service);

        // build notification
        mNotification = getNotificationBuilder(service, station).build();

        // display notification
        service.startForeground(PLAYER_SERVICE_NOTIFICATION_ID, mNotification);
    }


    /* Updates the notification */
    public static void update(final Service service, Station station, MediaSessionCompat session) {

        // session can be null on update - happens when currently playing station was renamed
        if (session != null) {
            // update mSession
            mSession = session;
        }

        // build notification
        mNotification = getNotificationBuilder(service, station).build();

        // display updated notification
        NotificationManager notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(PLAYER_SERVICE_NOTIFICATION_ID, mNotification);

        if (station.getPlaybackState() == PLAYBACK_STATE_STOPPED) {
            // make notification swipe-able
            service.stopForeground(false);
        }

    }


    /* Stop displaying notification */
    public static void stop(Service service) {
        if (service != null) {
            service.stopForeground(true);
        }
    }


    /* Creates a notification builder */
    private static NotificationCompat.Builder getNotificationBuilder(Context context, Station station) {

        // explicit intent for notification tap
        Intent tapActionIntent = new Intent(context, MainActivity.class);
        tapActionIntent.setAction(ACTION_SHOW_PLAYER);
        tapActionIntent.putExtra(EXTRA_STATION, station);

        // explicit intent for stopping playback
        Intent stopActionIntent = new Intent(context, PlayerService.class);
        stopActionIntent.setAction(ACTION_STOP);

        // explicit intent for starting playback
        Intent playActionIntent = new Intent(context, PlayerService.class);
        playActionIntent.setAction(ACTION_PLAY);

        // explicit intent for swiping notification
        Intent swipeActionIntent = new Intent(context, PlayerService.class);
        swipeActionIntent.setAction(ACTION_DISMISS);

        // artificial back stack for started Activity.
        // -> navigating backward from the Activity leads to Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
//        // backstack: adds back stack for Intent (but not the Intent itself)
//        stackBuilder.addParentStack(MainActivity.class);
        // backstack: add explicit intent for notification tap
        stackBuilder.addNextIntent(tapActionIntent);

        // pending intent wrapper for notification tap
        PendingIntent tapActionPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
//        PendingIntent tapActionPendingIntent = PendingIntent.getService(mService, 0, tapActionIntent, 0);
        // pending intent wrapper for notification stop action
        PendingIntent stopActionPendingIntent = PendingIntent.getService(context, 10, stopActionIntent, 0);
        // pending intent wrapper for notification start action
        PendingIntent playActionPendingIntent = PendingIntent.getService(context, 11, playActionIntent, 0);
        // pending intent wrapper for notification swipe action
        PendingIntent swipeActionPendingIntent = PendingIntent.getService(context, 12, swipeActionIntent, 0);

        // create media style
        android.support.v4.media.app.NotificationCompat.MediaStyle style = new android.support.v4.media.app.NotificationCompat.MediaStyle();
        style.setMediaSession(mSession.getSessionToken());
        style.setShowActionsInCompactView(0);
        style.setShowCancelButton(true); // pre-Lollipop workaround
        style.setCancelButtonIntent(swipeActionPendingIntent);

        // construct notification in builder
        NotificationCompat.Builder builder;
        builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANEL_ID_PLAYBACK_CHANNEL);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        builder.setSmallIcon(R.drawable.ic_notification_small_24dp);
        builder.setLargeIcon(getStationIcon(context, station));
        builder.setContentTitle(station.getStationName());
        builder.setContentText(station.getMetadata());
        builder.setShowWhen(false);
        builder.setStyle(style);
        builder.setContentIntent(tapActionPendingIntent);
        builder.setDeleteIntent(swipeActionPendingIntent);

        if (station.getPlaybackState() != PLAYBACK_STATE_STOPPED) {
            builder.addAction(R.drawable.ic_stop_white_36dp, context.getString(R.string.notification_stop), stopActionPendingIntent);
        } else {
            builder.addAction(R.drawable.ic_play_arrow_white_36dp, context.getString(R.string.notification_play), playActionPendingIntent);
        }

        return builder;
    }


    /* Get station image for notification's large icon */
    private static Bitmap getStationIcon(Context context, Station station) {
        if (station == null) {
            return null;
        }

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


    /* Create a notification channel */
    private static boolean createNotificationChannel(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // API level 26 ("Android O") supports notification channels.
            String id = NOTIFICATION_CHANEL_ID_PLAYBACK_CHANNEL;
            CharSequence name = context.getString(R.string.notification_channel_playback_name);
            String description = context.getString(R.string.notification_channel_playback_description);
            int importance = NotificationManager.IMPORTANCE_LOW;

            // create channel
            NotificationChannel channel = new NotificationChannel(id, name, importance);
            channel.setDescription(description);

            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.createNotificationChannel(channel);
            return true;

        } else {
            return false;
        }
    }


}
