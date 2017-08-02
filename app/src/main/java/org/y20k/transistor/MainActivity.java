/**
 * MainActivity.java
 * Implements the app's main activity
 * The main activity sets up the main view end inflates a menu bar menu
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-17 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */

package org.y20k.transistor;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.LifecycleRegistryOwner;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import org.y20k.transistor.adapter.CollectionViewModel;
import org.y20k.transistor.core.Station;
import org.y20k.transistor.helpers.ImageHelper;
import org.y20k.transistor.helpers.LogHelper;
import org.y20k.transistor.helpers.ShortcutHelper;
import org.y20k.transistor.helpers.StorageHelper;
import org.y20k.transistor.helpers.TransistorKeys;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


/**
 * MainActivity class
 */
public final class MainActivity extends AppCompatActivity implements LifecycleRegistryOwner, FragmentManager.OnBackStackChangedListener, TransistorKeys {

    /* Define log tag */
    private static final String LOG_TAG = MainActivity.class.getSimpleName();


    /* Main class variables */
    private boolean mTwoPane;
    private StorageHelper mStorageHelper;
    private View mContainer;
    private CollectionViewModel mCollectionViewModel;
    private final LifecycleRegistry mRegistry = new LifecycleRegistry(this);
    private ArrayList<Station> mStationList;
    private Station mTempStation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // initialize list of stations
        mStationList = new ArrayList<Station>();

        // initialize storage helper
        mStorageHelper = new StorageHelper(this);

        // initialize temp station (used by image change requests)
        mTempStation = null;

        // check if system/app has external storage access
        checkExternalStorageState();

        // observe changes in LiveData
        mCollectionViewModel = ViewModelProviders.of(this).get(CollectionViewModel.class);
        mCollectionViewModel.getStationList().observe((LifecycleOwner)this, createStationListObserver());

        // set layout
        setContentView(R.layout.activity_main);

        // check if app is running in two pane mode
        mTwoPane = detectTwoPane();
        mCollectionViewModel.getTwoPane().setValue(mTwoPane);

        Bundle args = new Bundle();
        args.putBoolean(ARG_TWO_PANE, mTwoPane);

        // put collection list in main container
        ListFragment listFragment = new ListFragment();
        listFragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, listFragment, COLLECTION_FRAGMENT_TAG)
                .commit();

        // put player in player container - two pane only
        if (mTwoPane) {
            PlayerFragment playerFragment = new PlayerFragment();
            playerFragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.player_container, playerFragment, PLAYER_FRAGMENT_TAG)
                    .commit();
        }

        // observe changes in backstack initiated by fragment transactions
        getSupportFragmentManager().addOnBackStackChangedListener(this);
    }


    @Override
    protected void onResume() {
        super.onResume();

        // check state of External Storage
        checkExternalStorageState();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        // check if two pane mode can be used
        mTwoPane = detectTwoPane();
        // update live data
        mCollectionViewModel.getTwoPane().setValue(mTwoPane);
        // save change
        saveAppState(this);
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // activity opened for second time set intent to new intent
        setIntent(intent);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // make sure that ListFragment's onActivityResult() gets called
        super.onActivityResult(requestCode, resultCode, data);

        // check if a station image change request was received
        if (requestCode == REQUEST_LOAD_IMAGE && resultCode == Activity.RESULT_OK) {
            handleStationImageChange(data);
        }

    }


    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.main_container);
        // hand results over to fragment main
        fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    public LifecycleRegistry getLifecycle() {
        return mRegistry;
    }


    @Override
    public void onBackStackChanged() {
        toggleDisplayHomeUp();
    }


    /* Hides / shows the Home/Up navigation in ActionBar */
    public void toggleDisplayHomeUp() {
        // toggle "back" icon
        boolean back = getSupportFragmentManager().getBackStackEntryCount() > 0;
        getSupportActionBar().setDisplayHomeAsUpEnabled(back);
        // reset title of activity in ActionBar, if backstack is empty
        if (!back) {
            setTitle(R.string.app_name);
        }
    }


    /* Show/hide player layout container - two pane layout only */
    public void togglePlayerContainerVisibility() {
        if (mTwoPane && mContainer != null && !mStorageHelper.storageHasStationPlaylistFiles()) {
            // make room for action call - hide player container
            mContainer.setVisibility(View.GONE);
        } else if (mTwoPane && mContainer != null) {
            // show player container
            mContainer.setVisibility(View.VISIBLE);
        }
    }

    /* Setter for temp station */
    public void setTempStation(Station tempStation) {
        mTempStation = tempStation;
    }


    /* Puts new station in list and updates live data  */
    public int handleStationAdd(Station station) {

        if (station != null) {
            // add station to list
            mStationList.add(station);

            // sort list
            sortStationList(mStationList);

            // update live data list of stations
            mCollectionViewModel.getStationList().setValue(mStationList);

            // return id of changed station
            return findStationId(station.getStreamUri());
        } else {
            return -1;
        }

    }


    /* Puts renamed station in list and updates live data */
    public int handleStationRename (Station station) {

        // get hold of previous state of this station
        Station oldStation = findStation(station.getStreamUri());

        // name of station is new
        if (station != null && oldStation!= null && !station.getStationName().equals(oldStation.getStationName())) {

            // todo check if a REAL copy of the list is needed

            // get new station
            int newStationId = findStationId(station.getStreamUri());

            // rename playlist file
            File oldStationPlaylistFile = oldStation.getStationPlaylistFile();
            oldStationPlaylistFile.delete();
            StorageHelper storageHelper = new StorageHelper(this);
            mStationList.get(newStationId).writePlaylistFile(storageHelper.getCollectionDirectory());

            // rename image file
            File oldStationImageFile = oldStation.getStationImageFile();
            oldStationImageFile.renameTo(station.getStationImageFile());

            // update list
            int stationID = findStationId(oldStation.getStreamUri());
            mStationList.set(stationID, station);

            // sort list
            sortStationList(mStationList);

            // update live data list of stations
            mCollectionViewModel.getStationList().setValue(mStationList);

            // return id of changed station
            return findStationId(station.getStreamUri());

        } else {
            // name of station is null or not new - notify user
            Toast.makeText(this, getString(R.string.toastalert_rename_unsuccessful), Toast.LENGTH_LONG).show();
            return -1;
        }

    }


    /* Removes given station from list and updates live data */
    public boolean handleStationDelete (Station station) {

        // todo check if a REAL copy of the list is needed

        // remove station from list
        int stationId = findStationId(station.getStreamUri());
        mStationList.remove(stationId);

        // delete m3u playlist file
        File stationPlaylistFile = station.getStationPlaylistFile();
        if (stationPlaylistFile != null) {
            stationPlaylistFile.delete();
        }

        // delete png image file
        File stationImageFile = station.getStationImageFile();
        if (stationImageFile != null) {
            stationImageFile.delete();
        }

        // delete station shortcut
        ShortcutHelper shortcutHelper = new ShortcutHelper(this);
        shortcutHelper.removeShortcut(station);

        // update live data
        mCollectionViewModel.getStationList().setValue(mStationList);

        // notify user
        Toast.makeText(this, getString(R.string.toastalert_delete_successful), Toast.LENGTH_LONG).show();

        return true;
    }


    /* Saves and sets new station image and updates station list and live data */
    private boolean handleStationImageChange (Intent data) {

        // retrieve selected image Uri from image picker
        Uri newImageUri = null;
        Bitmap newImage = null;
        if (null != data) {
            newImageUri = data.getData();
            ImageHelper imageHelper = new ImageHelper(newImageUri, this);
            newImage = imageHelper.getInputImage();
        }

        if (newImage != null && mTempStation != null) {
            // write image to storage
            File stationImageFile = mTempStation.getStationImageFile();
            try (FileOutputStream out = new FileOutputStream(stationImageFile)) {
                newImage.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (IOException e) {
                LogHelper.e(LOG_TAG, "Unable to save: " + newImage.toString());
                return false;
            }

            // update list
            int stationID = findStationId(mTempStation.getStreamUri());
            mStationList.set(stationID, mTempStation);

            // update live data
            mCollectionViewModel.getStationList().setValue(mStationList);

            return true;

        } else {
            LogHelper.e(LOG_TAG, "Unable to get image from media picker. Uri was:  " + newImageUri.toString());
            return false;
        }
    }


    /* Checks if two-pane mode can be used */
    private boolean detectTwoPane() {
        mContainer = findViewById(R.id.player_container);
        // if player_container is present two-pane layout can be used
        if (mContainer != null) {
            LogHelper.v(LOG_TAG, "Large screen detected. Choosing two pane layout.");
            return true;
        } else {
            LogHelper.v(LOG_TAG, "Small screen detected. Choosing single pane layout.");
            return false;
        }
    }


    /* Saves app state to SharedPreferences */
    private void saveAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREF_TWO_PANE, mTwoPane);
        editor.apply();
        LogHelper.v(LOG_TAG, "Saving state. Two Pane = " + mTwoPane);
    }


    /* Creates an observer for collection of stations stored as LiveData */
    private Observer<ArrayList<Station>> createStationListObserver() {
        return new Observer<ArrayList<Station>>() {
            @Override
            public void onChanged(@Nullable ArrayList<Station> newStationList) {
                // update station list
                mStationList = newStationList;

                // show/hide player layout container
                togglePlayerContainerVisibility();
            }
        };
    }


    /* Checks state of External Storage */
    private void checkExternalStorageState() {

        String state = Environment.getExternalStorageState();
        if (!state.equals(Environment.MEDIA_MOUNTED) || mStorageHelper.getCollectionDirectory() == null) {
            Toast.makeText(this, getString(R.string.toastalert_no_external_storage), Toast.LENGTH_LONG).show();
            LogHelper.e(LOG_TAG, "Error: Unable to mount External Storage. Current state: " + state);

            // move MainActivity to back
            moveTaskToBack(true);

            // shutting down app
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }


//    /* Initializes broadcast receivers for onCreate */
//    private void initializeBroadcastReceivers() {
//
//        // RECEIVER: state of playback has changed
//        mPlaybackStateChangedReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                if (intent.hasExtra(EXTRA_STATION)) {
//                    handlePlaybackStateChange(intent);
//                }
//            }
//        };
//        IntentFilter playbackStateChangedIntentFilter = new IntentFilter(ACTION_PLAYBACK_STATE_CHANGED);
//        LocalBroadcastManager.getInstance(this).registerReceiver(mPlaybackStateChangedReceiver, playbackStateChangedIntentFilter);
//
//        // RECEIVER: station metadata has changed
//        mMetadataChangedReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                if (intent.hasExtra(EXTRA_STATION)) {
//                    handleMetadataChange(intent);
//                }
//            }
//        };
//        IntentFilter metadataChangedIntentFilter = new IntentFilter(ACTION_METADATA_CHANGED);
//        LocalBroadcastManager.getInstance(this).registerReceiver(mMetadataChangedReceiver, metadataChangedIntentFilter);
//    }
//
//
//
//
//
//    /* Unregisters broadcast receivers */
//    private void unregisterBroadcastReceivers() {
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(mPlaybackStateChangedReceiver);
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMetadataChangedReceiver);
//    }


    /* Sorts list of stations */
    private void sortStationList(ArrayList<Station> stationList) {
        Collections.sort(stationList, new Comparator<Station>() {
            @Override
            public int compare(Station station1, Station station2) {
                // Compares two stations: returns "1" if name if this station is greater than name of given station
                return station1.getStationName().compareToIgnoreCase(station2.getStationName());
            }
        });
    }


    /* Finds ID of station when given its Uri */
    private int findStationId(Uri streamUri) {

        // make sure list and uri are not null
        if (mStationList == null || streamUri == null) {
            return -1;
        }

        // traverse list of stations
        for (int i = 0; i < mStationList.size(); i++) {
            Station station = mStationList.get(i);
            if (station.getStreamUri().equals(streamUri)) {
                return i;
            }
        }

        // return null if nothing was found
        return -1;
    }



    /* Finds station when given its Uri */
    private Station findStation(Uri streamUri) {

        // traverse list of stations
        for (int i = 0; i < mStationList.size(); i++) {
            Station station = mStationList.get(i);
            if (station.getStreamUri().equals(streamUri)) {
                return station;
            }
        }

        // return null if nothing was found
        return null;
    }


}