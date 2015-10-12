/**
 * StationHelper.java
 * Implements the StationHelper class
 * A StationHelper adds a new station to the collection
 * <p/>
 * This file is part of
 * TRANSISTOR - Radio App for Android
 * <p/>
 * Copyright (c) 2015 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.content.Context;
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

    /* Main class variables */
    private Context mContext;
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
        mFolder = new File(context.getExternalFilesDir("Collection").toString());
    }


    /* Add new station to Collection from Intent */
    public void add(String stationURLString) {
        try {
            URL newStationURL = new URL(stationURLString);
            String toastMessage = mContext.getString(R.string.alertmessage_add_download_started);
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
