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
import org.y20k.transistor.helpers.TransistorKeys;

import java.io.File;


/**
 * MainActivity class
 */
public final class MainActivity extends AppCompatActivity {

    /* Define log tag */
    private static final String LOG_TAG = MainActivity.class.getSimpleName();


    /* Main class variables */
    private boolean mTwoPane;
    private Intent mIntent;
    private File mFolder;
    private Collection mCollection;
    private View mContainer;
    private BroadcastReceiver mCollectionChangedReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get collection folder from external storage and load collection
        StorageHelper storageHelper = new StorageHelper(this);
        mFolder = storageHelper.getCollectionDirectory();
        mCollection = new Collection(mFolder);

        // get intent add collection
        mIntent = getIntent();

        if (mIntent != null) {
            mIntent.putExtra(TransistorKeys.EXTRA_COLLECTION, mCollection);
        }

        // set layout
        setContentView(R.layout.activity_main);

        // initialize broadcast receivers
        initializeBroadcastReceivers();

    }


    @Override
    protected void onResume() {
        super.onResume();


        // if player_container is present two-pane layout has been loaded
        mContainer = findViewById(R.id.player_container);

        if (mContainer != null) {
            mTwoPane = true;
            Log.v(LOG_TAG, "Large screen detected. Choosing two pane layout.");
        } else {
            Log.v(LOG_TAG, "Small screen detected. Choosing single pane layout.");
        }

        // special case: player should be launched (e.g. from shortcut or notification)
        if (mIntent != null && TransistorKeys.ACTION_SHOW_PLAYER.equals(mIntent.getAction())) {
            launchPlayer();
        }

        // tablet mode: show player fragment in player container
        if (mTwoPane && !mCollection.getStations().isEmpty()) {
            // prepare bundle
            Bundle playerArgs = new Bundle();
            playerArgs.putParcelable(TransistorKeys.ARG_COLLECTION, mCollection);
            playerArgs.putBoolean(TransistorKeys.ARG_TWO_PANE, mTwoPane);

            // show fragment
            PlayerActivityFragment playerActivityFragment = new PlayerActivityFragment();
            playerActivityFragment.setArguments(playerArgs);
            getFragmentManager().beginTransaction()
                    .replace(R.id.player_container, playerActivityFragment, TransistorKeys.PLAYERFRAGMENT_TAG)
                    .commit();
        } else if (mTwoPane) {
            // make room for action call
            mContainer.setVisibility(View.GONE);
        }

        saveAppState(this);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterBroadcastReceivers();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // activity opened for second time set intent to new intent
        setIntent(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // inflate the menu items for use in the action bar
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


    /* Directly launch the player */
    private void launchPlayer() {

        // prepare bundle
        Bundle playerArgs = new Bundle();
        int stationID;
        boolean startPlayback;

        // get id of station from intent
        if (mIntent.hasExtra(TransistorKeys.EXTRA_STATION_ID)) {
            // get station from notification
            stationID = mIntent.getIntExtra(TransistorKeys.EXTRA_STATION_ID, 0);
        } else if (mIntent.hasExtra(TransistorKeys.EXTRA_STREAM_URI)) {
            // get station from home screen shortcut
            stationID = mCollection.findStationID(mIntent.getStringExtra(TransistorKeys.EXTRA_STREAM_URI));
        }
        else {
            // default station
            stationID = 0;
        }

        // save station id as selected station (TODO: put into saveAppState)
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(TransistorKeys.PREF_STATION_ID_SELECTED, stationID);
        editor.apply();

        // get playback action from intent
        if (mIntent.hasExtra(TransistorKeys.EXTRA_PLAYBACK_STATE)) {
            startPlayback = mIntent.getBooleanExtra(TransistorKeys.EXTRA_PLAYBACK_STATE, false);
        } else {
            startPlayback = false;
        }

        // prepare arguments and intent
        if (mTwoPane) {
            // prepare args for player fragment
            playerArgs.putInt(TransistorKeys.ARG_STATION_ID, stationID);
            playerArgs.putParcelable(TransistorKeys.ARG_COLLECTION, mCollection);
            playerArgs.putBoolean(TransistorKeys.ARG_PLAYBACK, startPlayback);
            // show player fragment
            PlayerActivityFragment playerActivityFragment = new PlayerActivityFragment();
            playerActivityFragment.setArguments(playerArgs);
            getFragmentManager().beginTransaction()
                    .replace(R.id.player_container, playerActivityFragment, TransistorKeys.PLAYERFRAGMENT_TAG)
                    .commit();
            // notify main activity fragment
            Intent changeSelectionIntent = new Intent();
            changeSelectionIntent.setAction(TransistorKeys.ACTION_CHANGE_VIEW_SELECTION);
            changeSelectionIntent.putExtra(TransistorKeys.EXTRA_STATION_ID, stationID);
            LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(changeSelectionIntent);
        } else {
            // start player activity - on phone
            Intent playerIntent = new Intent(this, PlayerActivity.class);
            playerIntent.setAction(TransistorKeys.ACTION_SHOW_PLAYER);
            playerIntent.putExtra(TransistorKeys.EXTRA_STATION_ID, stationID);
            playerIntent.putExtra(TransistorKeys.EXTRA_COLLECTION, mCollection);
            playerIntent.putExtra(TransistorKeys.EXTRA_PLAYBACK_STATE, startPlayback);
            startActivity(playerIntent);
        }

        // remove ACTION_SHOW_PLAYER action from intent
        getIntent().setAction("");
    }


    /* Saves app state to SharedPreferences */
    private void saveAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        // editor.putInt(PREF_STATION_ID_SELECTED, mStationID);
        editor.putBoolean(TransistorKeys.PREF_TWO_PANE, mTwoPane);
        editor.apply();
        Log.v(LOG_TAG, "Saving state.");
    }


    /* Unregisters broadcast receivers */
    private void unregisterBroadcastReceivers() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mCollectionChangedReceiver);
    }


    /* Initializes broadcast receivers for onCreate */
    private void initializeBroadcastReceivers() {
        // RECEIVER: station added, deleted, or changed
        mCollectionChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                // get collection from intent
                if (intent.hasExtra(TransistorKeys.EXTRA_COLLECTION)) {
                    mCollection = intent.getParcelableExtra(TransistorKeys.EXTRA_COLLECTION);
                }

                // show/hide player layout container
                if (mTwoPane && mCollection.getStations().isEmpty()) {
                    // make room for action call
                    mContainer.setVisibility(View.GONE);
                } else if (mTwoPane && mCollection.getStations().size() == 1) {
                    mContainer.setVisibility(View.VISIBLE);
                    Bundle playerArgs = new Bundle();
                    playerArgs.putBoolean(TransistorKeys.ARG_TWO_PANE, mTwoPane);
                    PlayerActivityFragment playerActivityFragment = new PlayerActivityFragment();
                    playerActivityFragment.setArguments(playerArgs);
                    getFragmentManager().beginTransaction()
                            .replace(R.id.player_container, playerActivityFragment, TransistorKeys.PLAYERFRAGMENT_TAG)
                            .commit();
                }
            }
        };
        IntentFilter collectionChangedIntentFilter = new IntentFilter(TransistorKeys.ACTION_COLLECTION_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mCollectionChangedReceiver, collectionChangedIntentFilter);
    }

}