/**
 * ShortcutHelper.java
 * Implements the ShortcutHelper class
 * A ShortcutHelper creates and handles station shortcuts on the Home screen
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-20 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import org.y20k.transistor.MainActivity;
import org.y20k.transistor.R;
import org.y20k.transistor.core.Station;


/**
 * ShortcutHelper class
 */
public final class ShortcutHelper implements TransistorKeys {

    /* Define log tag */
    private static final String LOG_TAG = ShortcutHelper.class.getSimpleName();


    /* Places shortcut on Home screen */
    public static void placeShortcut(Context context, Station station) {

        // credit: https://medium.com/@BladeCoder/using-support-library-26-0-0-you-can-do-bb75911e01e8
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(context, station.getStationName())
                    .setShortLabel(station.getStationName())
                    .setLongLabel(station.getStationName())
                    .setIcon(createShortcutIcon(context, station))
                    .setIntent(createShortcutIntent(context, station))
                    .build();
            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null);
            Toast.makeText(context, R.string.toastmessage_shortcut_created, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, R.string.toastmessage_shortcut_not_created, Toast.LENGTH_LONG).show();
        }

    }


    /* Removes shortcut for given station from Home screen */
    public static void removeShortcut(Context context, Station station) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // from API level 26 ("Android O") on shortcuts are handled by ShortcutManager, which cannot remove shortcuts. The user must remove them manually.
        } else {
            // the pre 26 way: create and launch intent put shortcut on Home screen
            ImageHelper imageHelper = new ImageHelper(station, context);
            Intent removeIntent = new Intent();
            removeIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, station.getStationName());
            removeIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, imageHelper.createShortcutOnRadioShape(192));
            removeIntent.putExtra("duplicate", false);
            removeIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, createShortcutIntent(context, station));
            removeIntent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
            context.getApplicationContext().sendBroadcast(removeIntent);
        }
    }


    /* Creates Intent for a station shortcut */
    private static Intent createShortcutIntent (Context context, Station station) {

        String stationUri = station.getStreamUri().toString();

        // create intent to start MainActivity
        Intent shortcutIntent = new Intent(context, MainActivity.class);
        shortcutIntent.setAction(ACTION_SHOW_PLAYER);
        shortcutIntent.putExtra(EXTRA_STREAM_URI, stationUri);
        shortcutIntent.putExtra(EXTRA_PLAYBACK_STATE, true);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        LogHelper.v(LOG_TAG, "Intent for Home screen shortcut: " + shortcutIntent.toString() + " Activity: " + context);

        return shortcutIntent;
    }


    /* Create shortcut icon */
    private static IconCompat createShortcutIcon(Context context, Station station) {
        ImageHelper imageHelper = new ImageHelper(station, context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // only return the station image - Oreo adds an app icon badge to the shortcut - no need for additional branding
//            return IconCompat.createWithAdaptiveBitmap(imageHelper.createSquareImage(192));
            return IconCompat.createWithAdaptiveBitmap(imageHelper.createSquareImage(192, true));
        } else {
            // return station image in circular frame
            return IconCompat.createWithBitmap(imageHelper.createShortcutOnRadioShape(192));
        }
    }

}
