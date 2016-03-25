/**
 * ShortcutHelper.java
 * Implements the ShortcutHelper class
 * A ShortcutHelper creates and handles station shortcuts on the Home screen
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-16 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;

import org.y20k.transistor.MainActivity;
import org.y20k.transistor.R;
import org.y20k.transistor.core.Collection;
import org.y20k.transistor.core.Station;


/**
 * ShortcutHelper class
 */
public class ShortcutHelper {

    /* Define log tag */
    private static final String LOG_TAG = ShortcutHelper.class.getSimpleName();


    /* Keys */
    private static final String ACTION_SHOW_PLAYER = "org.y20k.transistor.action.SHOW_PLAYER";
    private static final String EXTRA_STATION_ID = "EXTRA_STATION_ID";
    private static final String EXTRA_PLAYBACK_STATE = "EXTRA_PLAYBACK_STATE";
    private static final String EXTRA_STREAM_URI = "EXTRA_STREAM_URI";
    private static final String EXTRA_TWOPANE = "EXTRA_TWOPANE";

    private static final String STREAM_URI = "streamUri";
    private static final String STATION_ID = "stationID";
    private static final String PREF_STATION_ID_CURRENT = "prefStationIDCurrent";
    private static final String PREF_STATION_ID_LAST = "prefStationIDLast";
    private static final String PREF_PLAYBACK = "prefPlayback";
    private static final String PREF_TWO_PANE = "prefTwoPane";
    private static final String PLAYERFRAGMENT_TAG = "PFTAG";


    /* Main class variables */
    private Activity mActivity;
    private Collection mCollection;


    /* Constructor */
    public ShortcutHelper(Activity activity, Collection collection) {
        mActivity = activity;
        mCollection = collection;
    }


    /* Places shortcut on Home screen */
    public void placeShortcut(int stationID) {
        // create and launch intent to put shortcut on Home screen
        Intent addIntent = createShortcutIntent(stationID);
        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        mActivity.getApplicationContext().sendBroadcast(addIntent);

        // notify user
        Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_shortcut_created), Toast.LENGTH_LONG).show();
    }


    /* Removes shortcut for given station from Home screen */
    public void removeShortcut(int stationID) {
        // create and launch intent to remove shortcut on Home screen
        Intent removeIntent = createShortcutIntent(stationID);
        removeIntent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
        mActivity.getApplicationContext().sendBroadcast(removeIntent);
    }


    /* Creates Intent for a station shortcut */
    private Intent createShortcutIntent (int stationID) {

        // get station
        Station station = mCollection.getStations().get(stationID);

        // create shortcut icon
        ImageHelper imageHelper;
        Bitmap stationImage;
        Bitmap shortcutIcon;
        if (station.getStationImageFile().exists()) {
            // use station image
            stationImage = BitmapFactory.decodeFile(station.getStationImageFile().toString());
            imageHelper = new ImageHelper(stationImage, mActivity);
            shortcutIcon = imageHelper.createShortcut(192);
        } else {
            // use default station image
            stationImage = BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.ic_notesymbol);
            imageHelper = new ImageHelper(stationImage, mActivity);
            shortcutIcon = imageHelper.createShortcut(192);
        }


        // create intent to start MainActivity
        Intent shortcutIntent = new Intent(mActivity, MainActivity.class);
        shortcutIntent.setAction(ACTION_SHOW_PLAYER);
        shortcutIntent.putExtra(EXTRA_STATION_ID, stationID);
        shortcutIntent.putExtra(EXTRA_PLAYBACK_STATE, true);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

        Log.v(LOG_TAG, "Intent for Home screen shortcut: " + shortcutIntent.toString() + " Activity: " + mActivity);

        // create and launch intent put shortcut on Home screen
        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, station.getStationName());
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, shortcutIcon);
        addIntent.putExtra("duplicate", false);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);

        return addIntent;
    }

}
