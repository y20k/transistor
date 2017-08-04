/**
 * CollectionViewModel.java
 * Implements the CollectionViewModel class
 * A CollectionViewModel stores the Collection of stations in a ViewModel
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-17 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */

package org.y20k.transistor.adapter;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.y20k.transistor.core.Station;
import org.y20k.transistor.helpers.LogHelper;
import org.y20k.transistor.helpers.StorageHelper;
import org.y20k.transistor.helpers.TransistorKeys;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * CollectionViewModel.class
 */
public class CollectionViewModel extends AndroidViewModel implements TransistorKeys {

    /* Define log tag */
    private static final String LOG_TAG = CollectionViewModel.class.getSimpleName();


    /* Main class variables */
    private MutableLiveData<ArrayList<Station>> mLiveStationList;
    private MutableLiveData<Boolean> mTwoPane;

    /* Constructor */
    public CollectionViewModel(Application application) {
        super(application);

        // initialize LiveData
        mLiveStationList = new MutableLiveData<ArrayList<Station>>();
        mTwoPane = new MutableLiveData<Boolean>();

        // load state from shared preferences and set live data values
        loadAppState(application);

        // load station list from storage and set live data
        mLiveStationList.setValue(loadStationList(application));

        // load station list and set live data in background -> not used because DiffResult.dispatchUpdatesTo is causing problems in Adapter
        // new LoadCollectionAsyncTask().execute(application);
    }


    @Override
    protected void onCleared() {
        super.onCleared();
    }


    /* Loads app state from preferences and updates live data */
    private void loadAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        mTwoPane.setValue(settings.getBoolean(PREF_TWO_PANE, false));
        LogHelper.v(LOG_TAG, "Loading state and updating live data.");
    }


    /* Getter for StationList */
    public MutableLiveData<ArrayList<Station>> getStationList() {
        return mLiveStationList;
    }


    /* Getter for TwoPane */
    public MutableLiveData<Boolean> getTwoPane() {
        return mTwoPane;
    }


    /* Load list of stations from storage */
    private ArrayList<Station> loadStationList(Application application) {

        StorageHelper storageHelper = new StorageHelper(application);
        File folder = storageHelper.getCollectionDirectory();

        // create folder if necessary
        if (!folder.exists()) {
            LogHelper.v(LOG_TAG, "Creating mFolder new folder: " + folder.toString());
            folder.mkdir();
        }

        // create nomedia file to prevent media scanning
        File nomedia = new File(folder, ".nomedia");
        if (!nomedia.exists()) {
            LogHelper.v(LOG_TAG, "Creating .nomedia file in folder: " + folder.toString());
            try (FileOutputStream noMediaOutStream = new FileOutputStream(nomedia)) {
                noMediaOutStream.write(0);
            } catch (IOException e) {
                LogHelper.e(LOG_TAG, "Unable to write .nomedia file in folder: " + folder.toString());
            }
        }

        // create array of Files from folder
        File[] listOfFiles = folder.listFiles();

        // initialize list
        ArrayList<Station> stationList = new ArrayList<Station>();

        // get Uri of currently playing station - CASE: PlayerService is active, but Activity has been killed
        String urlString = PreferenceManager.getDefaultSharedPreferences(application).getString(PREF_STATION_URL, null);
        Uri uri = null;
        if (urlString != null) {
            uri = Uri.parse(urlString);
        }

        // fill list of stations
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile() && file.toString().endsWith(".m3u")) {
                    // create new station from file
                    Station newStation = new Station(file);
                    if (newStation.getStreamUri() != null) {
                        // recreate playback state - if Activity was killed
                        if (newStation.getStreamUri().equals(uri)) {
                            // set playback state and set mStation value
                            LogHelper.v(LOG_TAG, "Shared preferences has playback information for " + newStation.getStationName() + ": Playback running.");
                            newStation.setPlaybackState(PLAYBACK_STATE_STARTED);
                        }
                        // add new station to list
                        stationList.add(newStation);
                    }
                }
            }
        }

        LogHelper.v(LOG_TAG, "Finished initial read operation from storage. Stations found: " + stationList.size());
        return stationList;
    }


    /**
     * Inner class: AsyncTask that loads list in background
     */
    class LoadCollectionAsyncTask extends AsyncTask<Application, Void, ArrayList<Station>> {
        @Override
        protected ArrayList<Station> doInBackground(Application... applications) {
//            // stress test for DiffResult todo remove
//            try {
//                Thread.sleep(1500);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

            // load stations in background
            return loadStationList(applications[0]);
        }

        @Override
        protected void onPostExecute(ArrayList<Station> stationList) {
            // set live data
            mLiveStationList.setValue(stationList);
        }
    }
    /**
     * End of inner class
     */

}
