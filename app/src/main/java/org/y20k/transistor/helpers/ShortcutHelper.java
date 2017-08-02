/**
 * ShortcutHelper.java
 * Implements the ShortcutHelper class
 * A ShortcutHelper creates and handles station shortcuts on the Home screen
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-17 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.widget.Toast;

import org.y20k.transistor.MainActivity;
import org.y20k.transistor.R;
import org.y20k.transistor.core.Station;


/**
 * ShortcutHelper class
 */
public class ShortcutHelper implements TransistorKeys {

    /* Define log tag */
    private static final String LOG_TAG = ShortcutHelper.class.getSimpleName();


    /* Main class variables */
    private final Context mContext;


    /* Constructor */
    public ShortcutHelper(Context context) {
        mContext = context;
    }


    /* Places shortcut on Home screen */
    public void placeShortcut(Station station) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // from API level 26 ("Android O") on shortcuts are handled by ShortcutManager
            ShortcutManager shortcutManager = mContext.getSystemService(ShortcutManager.class);
            if (shortcutManager.isRequestPinShortcutSupported()) {
                ShortcutInfo shortcut = new ShortcutInfo.Builder(mContext, station.getStationName())
                        .setShortLabel(station.getStationName())
                        .setLongLabel(station.getStationName())
                        .setIcon(Icon.createWithBitmap(createShortcutIcon(station)))
                        .setIntent(createShortcutIntent(station))
                        .build();
                shortcutManager.requestPinShortcut(shortcut, null);
                Toast.makeText(mContext, mContext.getString(R.string.toastmessage_shortcut_created), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(mContext, mContext.getString(R.string.toastmessage_shortcut_not_created), Toast.LENGTH_LONG).show();
            }

         } else {
            // the pre 26 way: create and launch intent put shortcut on Home screen
            Intent addIntent = new Intent();
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, station.getStationName());
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, createShortcutIcon(station));
            addIntent.putExtra("duplicate", false);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, createShortcutIntent(station));
            addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            mContext.getApplicationContext().sendBroadcast(addIntent);
            Toast.makeText(mContext, mContext.getString(R.string.toastmessage_shortcut_created), Toast.LENGTH_LONG).show();
        }


    }


    /* Removes shortcut for given station from Home screen */
    public void removeShortcut(Station station) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // from API level 26 ("Android O") on shortcuts are handled by ShortcutManager, which cannot remove shortcuts. The user must remove them manually.
        } else {
            // the pre 26 way: create and launch intent put shortcut on Home screen
            Intent removeIntent = new Intent();
            removeIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, station.getStationName());
            removeIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, createShortcutIcon(station));
            removeIntent.putExtra("duplicate", false);
            removeIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, createShortcutIntent(station));
            removeIntent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
            mContext.getApplicationContext().sendBroadcast(removeIntent);
        }
    }


    /* Creates Intent for a station shortcut */
    private Intent createShortcutIntent (Station station) {

        String stationUri = station.getStreamUri().toString();

        // create intent to start MainActivity
        Intent shortcutIntent = new Intent(mContext, MainActivity.class);
        shortcutIntent.setAction(ACTION_SHOW_PLAYER);
        shortcutIntent.putExtra(EXTRA_STREAM_URI, stationUri);
        shortcutIntent.putExtra(EXTRA_PLAYBACK_STATE, true);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        LogHelper.v(LOG_TAG, "Intent for Home screen shortcut: " + shortcutIntent.toString() + " Activity: " + mContext);

        return shortcutIntent;
    }


    /* Create shortcut icon */
    private Bitmap createShortcutIcon(Station station) {
        ImageHelper imageHelper;
        Bitmap stationImage;

        if (station.getStationImageFile().exists()) {
            // use station image
            stationImage = BitmapFactory.decodeFile(station.getStationImageFile().toString());
        } else {
            stationImage = null;
        }
        imageHelper = new ImageHelper(stationImage, mContext);

        return imageHelper.createShortcut(192);
    }

}
