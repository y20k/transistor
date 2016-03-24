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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.os.EnvironmentCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Toast;

import org.y20k.transistor.core.Collection;

import java.io.File;


/**
 * MainActivity class
 */
public final class MainActivity extends AppCompatActivity {

    /* Define log tag */
    private static final String LOG_TAG = MainActivity.class.getSimpleName();


    /* Keys */
    private static final String ACTION_SHOW_PLAYER = "org.y20k.transistor.action.PLAY";
    private static final String EXTRA_STATION_ID = "EXTRA_STATION_ID";
    private static final String EXTRA_PLAYBACK_STATE = "EXTRA_PLAYBACK_STATE";
    private static final String EXTRA_TWOPANE = "EXTRA_TWOPANE";
    private static final String TWOPANE = "twopane";
    private static final String PLAYERFRAGMENT_TAG = "PFTAG";


    /* Main class variables */
    private boolean mTwoPane;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // if player_container is present two-pane layout has been loaded
        if (findViewById(R.id.player_container) != null) {
            mTwoPane = true;
        } else {
            mTwoPane = false;
        }

        // set layout
        setContentView(R.layout.activity_main);

        // get intent
        Intent intent = getIntent();

        // special case: player activity should be launched
        if (intent != null && ACTION_SHOW_PLAYER.equals(intent.getAction())) {

            // load collection
            Collection collection = new Collection(getCollectionDirectory("Collection"));

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

            if (!mTwoPane) {
                // start player activity - on phone
                Intent i = new Intent(this, PlayerActivity.class);
                i.setAction(ACTION_SHOW_PLAYER);
                i.putExtra(EXTRA_STATION_ID, stationID);
                i.putExtra(EXTRA_PLAYBACK_STATE, startPlayback);
                startActivity(i);
            } else if (mTwoPane && savedInstanceState == null && !collection.getStations().isEmpty()) {
                Bundle args = new Bundle();
                args.putInt(EXTRA_STATION_ID, stationID);
                args.putBoolean(EXTRA_TWOPANE, mTwoPane);
                args.putBoolean(EXTRA_PLAYBACK_STATE, startPlayback);

                PlayerActivityFragment playerActivityFragment = new PlayerActivityFragment();
                playerActivityFragment.setArguments(args);
                getFragmentManager().beginTransaction()
                        .replace(R.id.player_container, playerActivityFragment, PLAYERFRAGMENT_TAG)
                        .commit();
            } else if (mTwoPane) {
                // make room for action call
                findViewById(R.id.player_container).setVisibility(View.GONE);
            }

        }

        saveAppState(this);

    }


    @Override
    protected void onResume() {
        super.onResume();

        // TODO Replace with collection changed listener?
        Collection collection = new Collection(getCollectionDirectory("Collection"));
        View container = findViewById(R.id.player_container);
        if (collection.getStations().isEmpty() && container != null) {
            // make room for action call
            container.setVisibility(View.GONE);
        } else if (container != null) {
            container.setVisibility(View.VISIBLE);
        }

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
        editor.putBoolean(TWOPANE, mTwoPane);
        editor.apply();
        Log.v(LOG_TAG, "Saving state.");
    }


    /* Return a writeable sub-directory from external storage  */
    private File getCollectionDirectory(String subDirectory) {
        File[] storage = this.getExternalFilesDirs(subDirectory);
        for (File file : storage) {
            String state = EnvironmentCompat.getStorageState(file);
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                Log.i(LOG_TAG, "External storage: " + file.toString());
                return file;
            }
        }
        Toast.makeText(this, this.getString(R.string.toastalert_no_external_storage), Toast.LENGTH_LONG).show();
        Log.e(LOG_TAG, "Unable to access external storage.");
        // finish activity
        this.finish();

        return null;
    }

}