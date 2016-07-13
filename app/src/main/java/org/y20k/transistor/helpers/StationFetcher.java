/**
 * StationFetcher.java
 * Implements helper for getting radio station metadata from local storage or internet
 * The downloader runs as AsyncTask
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-16 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.y20k.transistor.R;
import org.y20k.transistor.core.Station;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * StationFetcher class
 */
public final class StationFetcher extends AsyncTask<Void, Void, Station> {

    /* Define log tag */
    private static final String LOG_TAG = StationFetcher.class.getSimpleName();


    /* Main class variables */
    private final Activity mActivity;
    private final File mFolder;
    private final Uri mStationUri;
    private final String mStationUriScheme;
    private URL mStationURL;
    private final boolean mFolderExists;


    /* Constructor */
    public StationFetcher(Activity activity, File folder, Uri stationUri) {
        mActivity = activity;
        mStationUri = stationUri;
        mStationUriScheme = stationUri.getScheme();
        mFolder = folder;
        mFolderExists = mFolder.exists();

        if (stationUri != null && mStationUriScheme != null && mStationUriScheme.startsWith("http")) {
            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_add_download_started), Toast.LENGTH_LONG).show();
        } else if (stationUri != null && mStationUriScheme != null && mStationUriScheme.startsWith("file")) {
            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_add_open_file_started), Toast.LENGTH_LONG).show();
        }

    }


    /* Background thread: download station */
    @Override
    public Station doInBackground(Void... params) {

        if (mFolderExists && mStationUriScheme != null && mStationUriScheme.startsWith("http")  && urlCleanup()) {
            // download and return new station
            return new Station(mFolder, mStationURL);

        } else if (mFolderExists && mStationUriScheme != null && mStationUriScheme.startsWith("file")) {
            // read file and return new station
            return new Station(mFolder, mStationUri);

        } else {
            return null;
        }
    }


    /* Main thread: set station and activate listener */
    @Override
    protected void onPostExecute(Station station) {

        Bundle fetchResults = null;
        if (station != null) {
            fetchResults = station.getStationFetchResults();
        }

        // station was successfully fetched
        if (station != null && fetchResults != null && !fetchResults.getBoolean(TransistorKeys.RESULT_FETCH_ERROR) && mFolderExists) {

            // send local broadcast - adapter will save station
            Intent i = new Intent();
            i.setAction(TransistorKeys.ACTION_COLLECTION_CHANGED);
            i.putExtra(TransistorKeys.EXTRA_COLLECTION_CHANGE, TransistorKeys.STATION_ADDED);
            i.putExtra(TransistorKeys.EXTRA_STATION, station);
            LocalBroadcastManager.getInstance(mActivity.getApplication()).sendBroadcast(i);

            // inform user that aac might not work properly
            if (fetchResults.containsKey(TransistorKeys.RESULT_STREAM_TYPE) && fetchResults.getParcelable(TransistorKeys.RESULT_STREAM_TYPE) != null && fetchResults.getParcelable(TransistorKeys.RESULT_STREAM_TYPE).toString().contains("aac")) {
                Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_stream_may_not_work), Toast.LENGTH_LONG).show();
            }

            Log.v(LOG_TAG, "Station was successfully fetched: " + station.getStreamUri().toString());
        }

        // an error occurred
        if (station == null || (fetchResults != null && fetchResults.getBoolean(TransistorKeys.RESULT_FETCH_ERROR)) || !mFolderExists) {

            String errorTitle;
            String errorMessage;
            String errorDetails;

            if (mStationUriScheme != null && mStationUriScheme.startsWith("http")) {
                // construct error message for "http"
                errorTitle = mActivity.getResources().getString(R.string.dialog_error_title_fetch_download);
                errorMessage = mActivity.getResources().getString(R.string.dialog_error_message_fetch_download);
                errorDetails = buildDownloadErrorDetails(fetchResults);
            } else if (mStationUriScheme != null && mStationUriScheme.startsWith("file")) {
                // construct error message for "file"
                errorTitle = mActivity.getResources().getString(R.string.dialog_error_title_fetch_read);
                errorMessage = mActivity.getResources().getString(R.string.dialog_error_message_fetch_read);
                errorDetails = buildReadErrorDetails(fetchResults);
            } else if (!mFolderExists) {
                // construct error message for write error
                errorTitle = mActivity.getResources().getString(R.string.dialog_error_title_fetch_write);
                errorMessage = mActivity.getResources().getString(R.string.dialog_error_message_fetch_write);
                errorDetails = mActivity.getResources().getString(R.string.dialog_error_details_write);
            } else {
                // default values
                errorTitle = mActivity.getResources().getString(R.string.dialog_error_title_default);
                errorMessage = mActivity.getResources().getString(R.string.dialog_error_message_default);
                errorDetails = mActivity.getResources().getString(R.string.dialog_error_details_default);
            }

            // show error dialog
            DialogError dialogError = new DialogError(mActivity, errorTitle, errorMessage, errorDetails);
            dialogError.show();
        }

    }


    /* checks and cleans url string and sets mStationURL */
    private boolean urlCleanup() {
        // remove whitespaces and create url
        try {
            mStationURL = new URL(mStationUri.toString().trim());
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
    }


    /* Builds more detailed download error string */
    private String buildDownloadErrorDetails(Bundle fetchResults) {

        String fileContent = fetchResults.getString(TransistorKeys.RESULT_FILE_CONTENT);
        String playListType;
        String streamType;
        if (fetchResults.containsKey(TransistorKeys.RESULT_PLAYLIST_TYPE) && fetchResults.getParcelable(TransistorKeys.RESULT_PLAYLIST_TYPE) != null) {
            playListType = fetchResults.getParcelable(TransistorKeys.RESULT_PLAYLIST_TYPE).toString();
        } else {
            playListType = "unknown";
        }
        if (fetchResults.containsKey(TransistorKeys.RESULT_STREAM_TYPE) && fetchResults.getParcelable(TransistorKeys.RESULT_STREAM_TYPE) != null) {
            streamType = fetchResults.getParcelable(TransistorKeys.RESULT_STREAM_TYPE).toString();
        } else {
            streamType = "unknown";
        }

        // construct details string
        StringBuilder sb = new StringBuilder("");
        sb.append(mActivity.getResources().getString(R.string.dialog_error_message_fetch_general_external_storage));
        sb.append("\n");
        sb.append(mFolder);
        sb.append("\n\n");
        if (mStationUri.getScheme().startsWith("file")) {
            sb.append(mActivity.getResources().getString(R.string.dialog_error_message_fetch_read_file_location));
        } else {
            sb.append(mActivity.getResources().getString(R.string.dialog_error_message_fetch_download_station_url));
        }
        sb.append("\n");
        sb.append(mStationUri);

        if ((mStationUri.getLastPathSegment() != null && !mStationUri.getLastPathSegment().contains("m3u")) ||
                (mStationUri.getLastPathSegment() != null && !mStationUri.getLastPathSegment().contains("pls")) ) {
            sb.append("\n\n");
            sb.append(mActivity.getResources().getString(R.string.dialog_error_message_fetch_general_hint_m3u));
        }

        if (playListType != null) {
            sb.append("\n\n");
            sb.append(mActivity.getResources().getString(R.string.dialog_error_message_fetch_general_playlist_type));
            sb.append("\n");
            sb.append(playListType);
        }

        if (streamType != null) {
            sb.append("\n\n");
            sb.append(mActivity.getResources().getString(R.string.dialog_error_message_fetch_general_stream_type));
            sb.append("\n");
            sb.append(streamType);
        }

        if (fileContent != null) {
            sb.append("\n\n");
            sb.append(mActivity.getResources().getString(R.string.dialog_error_message_fetch_general_file_content));
            sb.append("\n");
            sb.append(fileContent);
        }

        return sb.toString();

    }


    /* Builds more detailed read error string */
    private String buildReadErrorDetails(Bundle fetchResults) {

        // construct details string
        StringBuilder sb = new StringBuilder("");
        sb.append(mActivity.getResources().getString(R.string.dialog_error_message_fetch_general_external_storage));
        sb.append("\n");
        sb.append(mFolder);
        sb.append("\n\n");
        sb.append(mActivity.getResources().getString(R.string.dialog_error_message_fetch_read));
        sb.append("\n");
        sb.append(mStationUri);
        if (!mStationUri.getLastPathSegment().contains("m3u") || !mStationUri.getLastPathSegment().contains("pls") ) {
            sb.append("\n\n");
            sb.append(mActivity.getResources().getString(R.string.dialog_error_message_fetch_general_hint_m3u));
        }

        if (fetchResults != null && fetchResults.getBoolean(TransistorKeys.RESULT_FETCH_ERROR)) {
            String fileContent = fetchResults.getString(TransistorKeys.RESULT_FILE_CONTENT);
            if (fileContent != null) {
                sb.append("\n\n");
                sb.append(mActivity.getResources().getString(R.string.dialog_error_message_fetch_general_file_content));
                sb.append("\n");
                sb.append(fileContent);
            } else {
                Log.v(LOG_TAG, "no content in local file");
            }

        }
        return sb.toString();

    }

}
