/**
 * StationListHelper.java
 * Implements the StationListHelper class
 * A StationListHelper provides methods that can manipulate lists of Station objects
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-17 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;

import org.y20k.transistor.core.Station;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


/**
 * StationListHelper class
 */
public final class StationListHelper implements TransistorKeys {

    /* Define log tag */
    private static final String LOG_TAG = StationListHelper.class.getSimpleName();

    /* Creates a real copy of given station list*/
    public static ArrayList<Station> copyStationList(ArrayList<Station> stationList) {
        ArrayList<Station> newStationList = new ArrayList<Station>();
        for (Station station : stationList) {
            newStationList.add(new Station (station));
        }
        return newStationList;
    }


    /* Finds station when given its Uri */
    public static Station findStation(ArrayList<Station> stationList, Uri streamUri) {

        // make sure list and uri are not null
        if (stationList == null || streamUri == null) {
            return null;
        }

        // traverse list of stations
        for (int i = 0; i < stationList.size(); i++) {
            Station station = stationList.get(i);
            if (station.getStreamUri().equals(streamUri)) {
                return station;
            }
        }

        // return null if nothing was found
        return null;
    }



    /* Finds ID of station when given its Uri */
    public static int findStationId(ArrayList<Station> stationList, Uri streamUri) {

        // make sure list and uri are not null
        if (stationList == null || streamUri == null) {
            return -1;
        }

        // traverse list of stations
        for (int i = 0; i < stationList.size(); i++) {
            Station station = stationList.get(i);
            if (station.getStreamUri().equals(streamUri)) {
                return i;
            }
        }

        // return null if nothing was found
        return -1;
    }


    /* Sorts list of stations */
    public static ArrayList<Station> sortAndReturnStationList(ArrayList<Station> stationList) {
        Collections.sort(stationList, new Comparator<Station>() {
            @Override
            public int compare(Station station1, Station station2) {
                // Compares two stations: returns "1" if name if this station is greater than name of given station
                return station1.getStationName().compareToIgnoreCase(station2.getStationName());
            }
        });
        return stationList;
    }



    /* Sorts list of stations */
    public static void sortStationList(ArrayList<Station> stationList) {
        Collections.sort(stationList, new Comparator<Station>() {
            @Override
            public int compare(Station station1, Station station2) {
                // Compares two stations: returns "1" if name if this station is greater than name of given station
                return station1.getStationName().compareToIgnoreCase(station2.getStationName());
            }
        });
    }


    /* Load list of stations from storage */
    public static ArrayList<Station> loadStationListFromStorage(Context context) {

        StorageHelper storageHelper = new StorageHelper(context);
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
        String urlString = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_STATION_URL, null);
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


}
