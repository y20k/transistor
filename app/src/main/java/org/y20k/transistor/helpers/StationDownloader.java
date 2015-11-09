/**
 * StationDownloader.java
 * Implements a downloader for radio station metadata from the internet
 * The downloader runs as AsyncTask
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.os.AsyncTask;

import org.y20k.transistor.core.Station;

import java.io.File;
import java.net.URL;


/**
 * StationDownloader class
 */
class StationDownloader extends AsyncTask<Void, Void, Station> {

    /* Define log tag */
    private static final String LOG_TAG = CollectionLoader.class.getSimpleName();


    /* Main class variables */
    private final File mFolder;
    private Station mStation;
    private final URL mStationURL;
    private StationDownloadListener mStationDownloadListener;


    /* Interface for custom listener */
    public interface StationDownloadListener {
        void stationDownloaded();
    }


    /* Constructor */
    public StationDownloader(File folder, URL stationURL) {
        mFolder = folder;
        mStationURL = stationURL;
    }


    /* Background thread: download station */
    @Override
    public Station doInBackground(Void... params) {
        Station station = new Station(mFolder, mStationURL);
        return station;
    }


    /* Main thread: set station and activate listener */
    @Override
    protected void onPostExecute(Station station) {
        if (mStationDownloadListener != null) {
            mStation = station;
            mStationDownloadListener.stationDownloaded();
        }
    }


    /* Getter for station */
    public Station getStation() {
        return mStation;
    }


    /* Setter fpr StationDownloadListener */
    public void setStationDownloadListener(StationDownloadListener stationDownloadListener) {
        mStationDownloadListener = stationDownloadListener;
    }

}