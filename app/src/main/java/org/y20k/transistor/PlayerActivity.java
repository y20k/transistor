/**
 * PlayerActivity.java
 * Implements the app's player activity
 * The player activity sets up the now playing view and inflates a menubar menu
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-16 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;


/**
 * PlayerActivity class
 */
public final class PlayerActivity extends AppCompatActivity {

    /* Define log tag */
    private static final String LOG_TAG = PlayerActivity.class.getSimpleName();


    /* Keys */
    private static final String ACTION_SHOW_PLAYER = "org.y20k.transistor.action.SHOW_PLAYER";
    private static final String EXTRA_STATION_ID = "EXTRA_STATION_ID";
    private static final String EXTRA_PLAYBACK_STATE = "EXTRA_PLAYBACK_STATE";
    private static final String ARG_STATION_ID = "ArgStationID";
    private static final String ARG_PLAYBACK = "ArgPlayback";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set content view
        setContentView(R.layout.activity_player);

        // get intent
        Intent intent = getIntent();

        // CASE: show player in phone mode
        if (intent != null && ACTION_SHOW_PLAYER.equals(intent.getAction()) && savedInstanceState == null) {

            // get id of station from intent
            int stationID;
            if (intent.hasExtra(EXTRA_STATION_ID)) {
                stationID = intent.getIntExtra(EXTRA_STATION_ID, 0);
            } else {
                stationID = 0;
            }

            // get playback action from intent
            boolean startPlayback;
            if (intent.hasExtra(EXTRA_PLAYBACK_STATE)) {
                startPlayback = intent.getBooleanExtra(EXTRA_PLAYBACK_STATE, false);
            } else {
                startPlayback = false;
            }

            Bundle args = new Bundle();
            args.putInt(ARG_STATION_ID, stationID);
            args.putBoolean(ARG_PLAYBACK, startPlayback);

            PlayerActivityFragment playerActivityFragment = new PlayerActivityFragment();
            playerActivityFragment.setArguments(args);

            getFragmentManager().beginTransaction()
                    .add(R.id.player_container, playerActivityFragment)
                    .commit();

        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_player_actionbar, menu);
        return super.onCreateOptionsMenu(menu);
    }


}
