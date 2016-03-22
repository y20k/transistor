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
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.y20k.transistor.MainActivity;
import org.y20k.transistor.PlayerActivity;
import org.y20k.transistor.PlayerActivityFragment;
import org.y20k.transistor.PlayerService;
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
    private static final String ACTION_PLAY = "org.y20k.transistor.action.PLAY";
    private static final String STREAM_URI = "streamUri";
    private static final String STATION_ID = "stationID";
    private static final String STATION_ID_CURRENT = "stationIDCurrent";
    private static final String STATION_ID_LAST = "stationIDLast";
    private static final String PLAYBACK = "playback";
    private static final String TWOPANE = "twopane";
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


    /* Handles incoming intent from Home screen shortcut  */
    public void handleShortcutIntent(Intent intent, Bundle savedInstanceState) {
        String streamUri = intent.getStringExtra(STREAM_URI);

        // check if there is a previous saved state to detect if the activity is restored
        // after being destroyed and that playback should not be resumed
        if (ACTION_PLAY.equals(intent.getAction()) && savedInstanceState == null) {

            // find the station corresponding to the stream URI
            int stationID = mCollection.findStationID(streamUri);
            if (stationID != -1) {
                String stationName = mCollection.getStations().get(stationID).getStationName();

                // get current app state
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
                int stationIDCurrent = settings.getInt(STATION_ID_CURRENT, -1);
                boolean playback = settings.getBoolean(PLAYBACK, false);
                boolean twoPane = settings.getBoolean(TWOPANE, false);

                int stationIDLast = stationIDCurrent;

                // check if this station is not already playing
                if (!playback || stationIDCurrent != stationID) {
                    // start playback service
                    PlayerService playerService = new PlayerService();
                    playerService.startActionPlay(mActivity, streamUri, stationName);

                    stationIDLast = stationIDCurrent;
                    stationIDCurrent = stationID;
                    playback = true;
                }

                // save station name and ID
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt(STATION_ID_CURRENT, stationIDCurrent);
                editor.putInt(STATION_ID_LAST, stationIDLast);
                editor.putBoolean(PLAYBACK, playback);
                editor.apply();

                // check for tablet mode
                if (twoPane) {
                    Bundle args = new Bundle();
                    args.putInt(STATION_ID, stationIDCurrent);
                    args.putBoolean(TWOPANE, twoPane);

                    PlayerActivityFragment playerActivityFragment = new PlayerActivityFragment();
                    playerActivityFragment.setArguments(args);
                    mActivity.getFragmentManager().beginTransaction()
                            .replace(R.id.player_container, playerActivityFragment, PLAYERFRAGMENT_TAG)
                            .commit();
                } else {
                    // add if of station to intent and start activity
                    Intent startIntent = new Intent(mActivity, PlayerActivity.class);
                    startIntent.putExtra(STATION_ID, stationID);
                    mActivity.startActivity(startIntent);
                }

            }
            else {
                Toast.makeText(mActivity, mActivity.getString(R.string.toastalert_stream_not_found), Toast.LENGTH_LONG).show();
            }
        }
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
//        Intent shortcutIntent = new Intent();
//        shortcutIntent.setComponent(new ComponentName(mActivity.getPackageName(), "." + mActivity.getLocalClassName()));
        shortcutIntent.putExtra(STREAM_URI, station.getStreamUri().toString());
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        shortcutIntent.setAction(ACTION_PLAY);

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
