/**
 * StationHelper.java
 * Implements the StationHelper class
 * A StationHelper adds a new station to the collection
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.y20k.transistor.R;
import org.y20k.transistor.core.Collection;
import org.y20k.transistor.core.Station;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * StationHelper class
 */
public class StationHelper {

    /* Define log tag */
    private static final String LOG_TAG = StationHelper.class.getSimpleName();


    /* Main class variables */
    private final Context mContext;
    private Collection mCollection;
    private File mFolder;
    private StationChangedListener mStationChangedListener;


    /* Interface for custom listener */
    public interface StationChangedListener {
        void stationChanged();
    }


    /* Constructor */
    public StationHelper(Context context) {
        mContext = context;
        mStationChangedListener = null;

        try {
            // get collection folder from external storage
            mFolder = new File(context.getExternalFilesDir("Collection").toString());
        } catch (NullPointerException e) {
            // notify user and log exception
            Toast.makeText(context, R.string.toastalert_no_external_storage, Toast.LENGTH_LONG).show();
            Log.e(LOG_TAG, "Unable to access external storage.");
        }

    }


    /* Add new station to Collection from Intent */
    public void add(String stationURLString) {
        try {
            URL newStationURL = new URL(stationURLString);
            String toastMessage = mContext.getString(R.string.toastmessage_add_download_started);
            Toast.makeText(mContext, toastMessage + stationURLString, Toast.LENGTH_LONG).show();

            mCollection = new Collection(mFolder);

            final StationDownloader stationDownloader = new StationDownloader(mFolder, newStationURL);
            stationDownloader.setStationDownloadListener(new StationDownloader.StationDownloadListener() {
                @Override
                public void stationDownloaded() {
                    Station newStation = stationDownloader.getStation();
                    mCollection.add(newStation);
                    if (mStationChangedListener != null) {
                        mStationChangedListener.stationChanged();
                    }
                }
            });
            stationDownloader.execute();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }


    /* Setter for custom listener */
    public void setStationChangedListener(StationChangedListener stationChangedListener) {
        mStationChangedListener = stationChangedListener;
    }

}
