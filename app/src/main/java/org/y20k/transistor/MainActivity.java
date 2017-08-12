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

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.LifecycleRegistryOwner;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import org.y20k.transistor.adapter.CollectionViewModel;
import org.y20k.transistor.core.Station;
import org.y20k.transistor.helpers.DialogError;
import org.y20k.transistor.helpers.ImageHelper;
import org.y20k.transistor.helpers.LogHelper;
import org.y20k.transistor.helpers.PermissionHelper;
import org.y20k.transistor.helpers.ShortcutHelper;
import org.y20k.transistor.helpers.StationListHelper;
import org.y20k.transistor.helpers.StorageHelper;
import org.y20k.transistor.helpers.TransistorKeys;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


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

        // initialize view model containing live data
        mCollectionViewModel = ViewModelProviders.of(this).get(CollectionViewModel.class);

        // set layout
        setContentView(R.layout.activity_main);

        // check if app is running in two pane mode
        mTwoPane = detectTwoPane();
        mCollectionViewModel.getTwoPane().setValue(mTwoPane);

        // observe changes in LiveData
        mCollectionViewModel.getStationList().observe((LifecycleOwner)this, createStationListObserver());

        Bundle args = new Bundle();
        args.putBoolean(ARG_TWO_PANE, mTwoPane);

        // put collection list in main container
        ListFragment listFragment = new ListFragment();
        listFragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, listFragment, LIST_FRAGMENT_TAG)
                .commit();

        // put player in player container - two pane only
        // -> handled by CollectionAdapter

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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_IMAGE_PICKER_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    selectFromImagePicker();
                } else {
                    // permission denied
                    Toast.makeText(this, getString(R.string.toastalert_permission_denied) + " READ_EXTERNAL_STORAGE", Toast.LENGTH_LONG).show();
                }
                break;
            }
            case PERMISSION_REQUEST_STATION_FETCHER_READ_EXTERNAL_STORAGE: {
                // let list fragment handle the request
                Fragment listFragment = getSupportFragmentManager().findFragmentByTag(LIST_FRAGMENT_TAG);
                if (listFragment != null) {
                    listFragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
                }
                break;
            }
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // make sure that ListFragment's onActivityResult() gets called
        super.onActivityResult(requestCode, resultCode, data);

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            return;
        }

        // check if a station image change request was received
        if (requestCode == REQUEST_LOAD_IMAGE) {
            handleStationImageChange(data);
        }

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

    /* Puts new station in list and updates live data  */
    public int handleStationAdd(Bundle stationDownloadBundle) {

        // get station, station image and station URL from download bundle
        Station station = null;
        Bitmap stationBitmap = null;
        if (stationDownloadBundle.containsKey(KEY_DOWNLOAD_STATION)) {
            station = stationDownloadBundle.getParcelable(KEY_DOWNLOAD_STATION);
        }
        if (stationDownloadBundle.containsKey(KEY_DOWNLOAD_STATION_IMAGE)) {
            stationBitmap = stationDownloadBundle.getParcelable(KEY_DOWNLOAD_STATION_IMAGE);
        }

        // check is station is valid and unique
        if (station != null && StationListHelper.findStationId(mStationList, station.getStreamUri()) == -1) {
            // write playlist file and station image - if available
            station.writePlaylistFile(mStorageHelper.getCollectionDirectory());
            if (stationBitmap != null) {
                station.writeImageFile(stationBitmap);
            }
            // create copy of main list of stations
            ArrayList<Station> newStationList = StationListHelper.copyStationList(mStationList);
            // add station to new list of stations
            newStationList.add(station);
            // update live data list of stations
            mCollectionViewModel.getStationList().setValue(newStationList);
            // return new index
            return StationListHelper.findStationId(newStationList, station.getStreamUri());
        } else {
            // notify user and log failure to add
            String errorTitle = getResources().getString(R.string.dialog_error_title_fetch_write);
            String errorMessage = getResources().getString(R.string.dialog_error_message_fetch_write);
            String errorDetails = getResources().getString(R.string.dialog_error_details_write);
            DialogError dialogError = new DialogError(this, errorTitle, errorMessage, errorDetails);
            dialogError.show();
            LogHelper.e(LOG_TAG, "Unable to add station to collection: Duplicate name and/or stream URL.");

            return -1;
        }

    }


    /* Puts renamed station in list and updates live data */
    public int handleStationRename(Station station, String newStationName) {

        // name of station is new
        if (station != null && newStationName.length() > 0 && !station.getStationName().equals(newStationName)) {

            // initialize StorageHelper
            StorageHelper storageHelper = new StorageHelper(this);

            // create copys of station and main list of stations
            ArrayList<Station> newStationList = StationListHelper.copyStationList(mStationList);
            Station newStation = new Station(station);

            // get position of station in list
            int stationID = StationListHelper.findStationId(newStationList, station.getStreamUri());

            // set new name
            newStation.setStationName(newStationName);

            // delete old playlist file
            File stationPlaylistFile = station.getStationPlaylistFile();
            stationPlaylistFile.delete();
            // set new playlist file - and write file
            newStation.setStationPlaylistFile(storageHelper.getCollectionDirectory());
            newStation.writePlaylistFile(storageHelper.getCollectionDirectory());

            // rename existing image file
            File stationImageFile = station.getStationImageFile();
            newStation.setStationImageFile(storageHelper.getCollectionDirectory());
            stationImageFile.renameTo(newStation.getStationImageFile());

            // update list
            newStationList.set(stationID, newStation);

            // update liva data station from PlayerService - used in PlayerFragment
            mCollectionViewModel.getPlayerServiceStation().setValue(newStation);

            // update live data list of stations - used in CollectionAdapter
            mCollectionViewModel.getStationList().setValue(newStationList);

            // return id of changed station
            return StationListHelper.findStationId(newStationList, newStation.getStreamUri());

        } else {
            // name of station is null or not new - notify user
            Toast.makeText(this, getString(R.string.toastalert_rename_unsuccessful), Toast.LENGTH_LONG).show();
            return -1;
        }

    }


    /* Removes given station from list and updates live data */
    public int handleStationDelete(Station station) {

        // keep track of delete success
        boolean success = false;

        // get position of station
        int stationId = StationListHelper.findStationId(mStationList, station.getStreamUri());

        // delete png image file
        File stationImageFile = station.getStationImageFile();
        if (stationImageFile != null && stationImageFile.exists() && stationImageFile.delete()) {
            success = true;
        }

        // delete m3u playlist file
        File stationPlaylistFile = station.getStationPlaylistFile();
        if (stationPlaylistFile != null && stationPlaylistFile.exists() && stationPlaylistFile.delete()) {
            success = true;
        }

        // remove station and notify user
        if (success) {
            // switch back to station list - if player is visible
            if (!mTwoPane && getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            }

            // create copy of main list of stations
            ArrayList<Station> newStationList = StationListHelper.copyStationList(mStationList);
            // remove station from new station list
            newStationList.remove(stationId);
            // determine ID of next station
            if (newStationList.size() >= stationId && stationId > 0) {
                stationId--;
            } else {
                stationId = 0;
            }

            // show next station in list
            Fragment listFragment = getSupportFragmentManager().findFragmentByTag(LIST_FRAGMENT_TAG);
            if (mTwoPane && listFragment!= null && listFragment.isAdded() && newStationList.size() > 0) {
                ((ListFragment)listFragment).updateListAfterDelete(newStationList.get(stationId), stationId);
            }

            // show next station in player view
            Fragment playerFragment = getSupportFragmentManager().findFragmentByTag(PLAYER_FRAGMENT_TAG);
            if (mTwoPane && playerFragment!= null && playerFragment.isAdded() && newStationList.size() > 0) {
                ((PlayerFragment)playerFragment).updatePlayerAfterDelete(newStationList.get(stationId));
            }

            // update live data list of stations - used in CollectionAdapter
            mCollectionViewModel.getStationList().setValue(newStationList);

            // notify user
            Toast.makeText(this, getString(R.string.toastalert_delete_successful), Toast.LENGTH_LONG).show();
        }

        // delete station shortcut
        ShortcutHelper shortcutHelper = new ShortcutHelper(this);
        shortcutHelper.removeShortcut(station);

        // return ID of station next to the deleted station station
        return stationId;
    }


    /* Check permissions and start image picker */
    public void pickImage(Station station) {
        mTempStation = station;
        View rootView = findViewById(android.R.id.content);
        PermissionHelper permissionHelper = new PermissionHelper(this, rootView);
        if (permissionHelper.requestReadExternalStorage(PERMISSION_REQUEST_IMAGE_PICKER_READ_EXTERNAL_STORAGE)) {
            selectFromImagePicker();
        }
    }


    /* Start image picker */
    private void selectFromImagePicker() {
        // get system picker for images
        Intent pickImageIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickImageIntent, REQUEST_LOAD_IMAGE);
    }


    /* Saves and sets new station image and updates station list and live data */
    private boolean handleStationImageChange(Intent data) {

        // retrieve selected image Uri from image picker
        Bitmap newImage = null;
        if (null != data) {
            ImageHelper imageHelper = new ImageHelper(data.getData(), this);
            newImage = imageHelper.getInputImage();
        }

        if (newImage != null && mTempStation != null) {
            // write image to storage
            try (FileOutputStream out = new FileOutputStream(mTempStation.getStationImageFile())) {
                newImage.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (IOException e) {
                LogHelper.e(LOG_TAG, "Unable to save: " + newImage.toString());
                return false;
            }

            // create copy of main list of stations
            ArrayList<Station> newStationList = StationListHelper.copyStationList(mStationList);

            // create a copy of mTempStation
            Station newStation = new Station(mTempStation);

            // set new station image file object
            newStation.setStationImageFile(mStorageHelper.getCollectionDirectory());

            // update list
            int stationID = StationListHelper.findStationId(mStationList, mTempStation.getStreamUri());
            newStationList.set(stationID, newStation);

            // update liva data station from PlayerService - used in PlayerFragment
            mCollectionViewModel.getPlayerServiceStation().setValue(newStation);

            // update live data
            mCollectionViewModel.getStationList().setValue(newStationList);

            // reset mTemopStation
            mTempStation = null;

            return true;

        } else {
            LogHelper.e(LOG_TAG, "Unable to get image from media picker.");
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

}