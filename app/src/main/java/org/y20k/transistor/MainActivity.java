/**
 * MainActivity.java
 * Implements the app's main activity
 * The main activity sets up the main view end inflates a menubar menu
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-16 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */

package org.y20k.transistor;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import org.y20k.transistor.core.Collection;
import org.y20k.transistor.helpers.StorageHelper;


/**
 * MainActivity class
 */
public final class MainActivity extends AppCompatActivity {

    /* Define log tag */
    private static final String LOG_TAG = MainActivity.class.getSimpleName();


    /* Keys */
    private static final String ACTION_SHOW_PLAYER = "org.y20k.transistor.action.SHOW_PLAYER";
    private static final String ACTION_CHANGE_VIEW_SELECTION = "org.y20k.transistor.action.CHANGE_VIEW_SELECTION";
    private static final String ACTION_COLLECTION_CHANGED = "org.y20k.transistor.action.COLLECTION_CHANGED";
    private static final String EXTRA_STATION_ID = "EXTRA_STATION_ID";
    private static final String EXTRA_PLAYBACK_STATE = "EXTRA_PLAYBACK_STATE";
    private static final String ARG_STATION_ID = "ArgStationID";
    private static final String ARG_TWO_PANE = "ArgTwoPane";
    private static final String ARG_PLAYBACK = "ArgPlayback";
    private static final String PREF_TWO_PANE = "prefTwoPane";
    private static final String PLAYERFRAGMENT_TAG = "PFTAG";


    /* Main class variables */
    private boolean mTwoPane;
    private Collection mCollection;
    private View mContainer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set layout
        setContentView(R.layout.activity_main);

        // if player_container is present two-pane layout has been loaded
        mContainer = findViewById(R.id.player_container);
        if (mContainer != null) {
            mTwoPane = true;
        } else {
            mTwoPane = false;
        }


        // load collection
        StorageHelper storageHelper = new StorageHelper(this);
        mCollection = new Collection(storageHelper.getCollectionDirectory());

        // get intent
        Intent intent = getIntent();

        // prepare bundle
        Bundle playerArgs = new Bundle();
        int stationID;
        boolean startPlayback;

        // CASE: player should be launched (e.g. from shortcut or notification)
        if (intent != null && ACTION_SHOW_PLAYER.equals(intent.getAction())) {

            // get id of station from intent
            if (intent.hasExtra(EXTRA_STATION_ID)) {
                stationID = intent.getIntExtra(EXTRA_STATION_ID, 0);
            } else {
                stationID = 0;
            }

            // get playback action from intent
            if (intent.hasExtra(EXTRA_PLAYBACK_STATE)) {
                startPlayback = intent.getBooleanExtra(EXTRA_PLAYBACK_STATE, false);
            } else {
                startPlayback = false;
            }

            // prepare arguments and intent
            if (mTwoPane) {
                // prepare args for player fragment
                playerArgs.putInt(ARG_STATION_ID, stationID);
                playerArgs.putBoolean(ARG_PLAYBACK, startPlayback);
                // notify main activity fragment
                Intent changeSelectionIntent = new Intent();
                changeSelectionIntent.setAction(ACTION_CHANGE_VIEW_SELECTION);
                changeSelectionIntent.putExtra(EXTRA_STATION_ID, stationID);
                LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(changeSelectionIntent);
            } else {
                // start player activity - on phone
                Intent playerIntent = new Intent(this, PlayerActivity.class);
                playerIntent.setAction(ACTION_SHOW_PLAYER);
                playerIntent.putExtra(EXTRA_STATION_ID, stationID);
                playerIntent.putExtra(EXTRA_PLAYBACK_STATE, startPlayback);
                startActivity(playerIntent);
            }
        }

        // tablet mode: show player fragment in player container
        if (mTwoPane && savedInstanceState == null && !mCollection.getStations().isEmpty()) {
            playerArgs.putBoolean(ARG_TWO_PANE, mTwoPane);
            PlayerActivityFragment playerActivityFragment = new PlayerActivityFragment();
            playerActivityFragment.setArguments(playerArgs);
            getFragmentManager().beginTransaction()
                    .replace(R.id.player_container, playerActivityFragment, PLAYERFRAGMENT_TAG)
                    .commit();
        } else if (mTwoPane) {
            // make room for action call
            mContainer.setVisibility(View.GONE);
        }

        saveAppState(this);

        // initialize broadcast receivers
        initializeBroadcastReceivers();

    }


    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // activity opened for second time set intent to new intent
        setIntent(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main_actionbar, menu);
        return super.onCreateOptionsMenu(menu);
    }


    // Workaround for an IllegalStateException crash (Fragment not attached to Activity)
    // See: https://github.com/y20k/transistor/issues/21
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.fragment_main);
        // hand results over to fragment main
        fragment.onActivityResult(requestCode, resultCode, data);
    }


    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.fragment_main);
        // hand results over to fragment main
        fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    /* Saves app state to SharedPreferences */
    private void saveAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREF_TWO_PANE, mTwoPane);
        editor.apply();
        Log.v(LOG_TAG, "Saving state.");
    }


    /* Initializes broadcast receivers for onCreate */
    private void initializeBroadcastReceivers() {
        // RECEIVER: station added, deleted, or changed
        BroadcastReceiver collectionChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mTwoPane && mCollection.getStations().isEmpty()) {
                    // make room for action call
                    mContainer.setVisibility(View.GONE);
                } else if (mTwoPane) {
                    mContainer.setVisibility(View.VISIBLE);
                }
            }
        };
        IntentFilter collectionChangedIntentFilter = new IntentFilter(ACTION_COLLECTION_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(collectionChangedReceiver, collectionChangedIntentFilter);
    }

}