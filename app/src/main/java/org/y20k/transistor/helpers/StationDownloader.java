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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.y20k.transistor.R;
import org.y20k.transistor.core.Collection;
import org.y20k.transistor.core.Station;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * StationDownloader class
 */
public final class StationDownloader extends AsyncTask<Void, Void, Station> {

    /* Define log tag */
    private static final String LOG_TAG = CollectionLoader.class.getSimpleName();


    /* Keys */
    private static final String ACTION_COLLECTION_CHANGED = "org.y20k.transistor.action.COLLECTION_CHANGED";

    /* Main class variables */
    private final Activity mActivity;
    private Collection mCollection;
    private File mFolder;
    private Uri mStationUri;
    private URL mStationURL;
    private final boolean mErrors;


    /* Constructor */
    public StationDownloader(Uri stationUri, Activity activity) {
        mActivity = activity;
        mStationUri = stationUri;

        // get mFolder and mStationURL
        if (getFolder()) {
            mErrors = false;
            // load collection
            mCollection = new Collection(mFolder);

        } else {
            // something is wrong with external storage or url
            mErrors = true;
        }

    }


    /* Background thread: download station */
    @Override
    public Station doInBackground(Void... params) {

        String scheme = mStationUri.getScheme();

        if (!mErrors && scheme.startsWith("http")  && urlCleanup()) {
            // notify user
//            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_add_download_started) + " " + mStationLocationString, Toast.LENGTH_LONG).show();
            // download and return new station
            return new Station(mFolder, mStationURL);

        } else if (!mErrors && scheme.startsWith("file")){
            // notify user
//            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_add_open_file_started) + " " + mStationLocationString, Toast.LENGTH_LONG).show();
            // TODO read file and return new station
            return new Station(mFolder, mStationUri);

        } else {
            return null;
        }
    }


    /* Main thread: set station and activate listener */
    @Override
    protected void onPostExecute(Station station) {

        if (mErrors || station == null) {
            // construct error message
            String errorTitle = mActivity.getResources().getString(R.string.dialog_error_title_download);
            String errorMessage = mActivity.getResources().getString(R.string.dialog_error_message_download);

            // construct details string
            StringBuilder sb = new StringBuilder("");
            sb.append(mActivity.getResources().getString(R.string.dialog_error_message_download_external_storage));
            sb.append("\n");
            sb.append(mFolder);
            sb.append("\n\n");
            sb.append(mActivity.getResources().getString(R.string.dialog_error_message_download_station_url));
            sb.append("\n");
            sb.append(mStationUri);
            if (!mStationUri.getLastPathSegment().contains("m3u") || !mStationUri.getLastPathSegment().contains("pls") ) {
                sb.append("\n\n");
                sb.append(mActivity.getResources().getString(R.string.dialog_error_message_download_hint_m3u));
            }

            if (station != null && station.getDownloadError()) {
                String remoteFileContent = station.getRemoteFileContent();
                if (remoteFileContent != null) {
                    sb.append("\n\n");
                    sb.append(mActivity.getResources().getString(R.string.dialog_error_message_download_file_content));
                    sb.append("\n");
                    sb.append(remoteFileContent);
                } else {
                    Log.v(LOG_TAG, "no remoteFileContent");
                }

            }

            String errorDetails = sb.toString();

            // show error dialog
            DialogError dialogError = new DialogError(mActivity, errorTitle, errorMessage, errorDetails);
            dialogError.show();

        } else {
            // add station to collection
            mCollection.add(station);
            // send local broadcast
            Intent i = new Intent();
            i.setAction(ACTION_COLLECTION_CHANGED);
            LocalBroadcastManager.getInstance(mActivity.getApplication()).sendBroadcast(i);
        }

    }


    /* Get collection folder from external storage */
    private boolean getFolder() {
        try {
            mFolder = mActivity.getExternalFilesDir("Collection");
            return true;
        } catch (NullPointerException e) {
            // notify user and log exception
            Toast.makeText(mActivity, mActivity.getString(R.string.toastalert_no_external_storage), Toast.LENGTH_LONG).show();
            Log.e(LOG_TAG, "Unable to access external storage.");
            return false;
        }
    }


    /* checks and cleans url string and return url */
    private boolean urlCleanup() {
        // remove whitespaces
//        mStationUri = mStationUri.trim();

        // create and check url
        try {
            mStationURL = new URL(mStationUri.toString());
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
