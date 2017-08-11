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
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.y20k.transistor.core.Station;
import org.y20k.transistor.helpers.LogHelper;
import org.y20k.transistor.helpers.StationListHelper;
import org.y20k.transistor.helpers.TransistorKeys;

import java.util.ArrayList;

/**
 * CollectionViewModel.class
 */
public class CollectionViewModel extends AndroidViewModel implements TransistorKeys {

    /* Define log tag */
    private static final String LOG_TAG = CollectionViewModel.class.getSimpleName();


    /* Main class variables */
    private MutableLiveData<ArrayList<Station>> mStationListLiveData;
    private MutableLiveData<Station> mPlayerServiceStationLiveData;
    private MutableLiveData<Boolean> mTwoPaneLiveData;

    /* Constructor */
    public CollectionViewModel(Application application) {
        super(application);

        // initialize LiveData
        mStationListLiveData = new MutableLiveData<ArrayList<Station>>();
        mPlayerServiceStationLiveData = new MutableLiveData<Station>();
        mTwoPaneLiveData = new MutableLiveData<Boolean>();

        // load state from shared preferences and set live data values
        loadAppState(application);

        // set station from PlayerService to null
        mPlayerServiceStationLiveData.setValue(null);

        // load station list from storage and set live data
        mStationListLiveData.setValue(StationListHelper.loadStationListFromStorage(application));

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
        mTwoPaneLiveData.setValue(settings.getBoolean(PREF_TWO_PANE, false));
        LogHelper.v(LOG_TAG, "Loading state and updating live data.");
    }


    /* Getter for StationList */
    public MutableLiveData<ArrayList<Station>> getStationList() {
        return mStationListLiveData;
    }


    /* Getter for station from PlayerService */
    public MutableLiveData<Station> getPlayerServiceStation() {
        return mPlayerServiceStationLiveData;
    }


    /* Getter for TwoPane */
    public MutableLiveData<Boolean> getTwoPane() {
        return mTwoPaneLiveData;
    }


    /**
     * Inner class: AsyncTask that loads list in background
     */
    class LoadCollectionAsyncTask extends AsyncTask<Context, Void, ArrayList<Station>> {
        @Override
        protected ArrayList<Station> doInBackground(Context... contexts) {
//            // stress test for DiffResult todo remove
//            try {
//                Thread.sleep(1500);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

            // load stations in background
            return StationListHelper.loadStationListFromStorage(contexts[0]);
        }

        @Override
        protected void onPostExecute(ArrayList<Station> stationList) {
            // set live data
            mStationListLiveData.setValue(stationList);
        }
    }
    /**
     * End of inner class
     */

}
