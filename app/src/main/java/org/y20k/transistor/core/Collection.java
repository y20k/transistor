/**
 * Collection.java
 * Implements the Collection class
 * A Collection holds a list of radio stations
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.core;

import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;


/**
 * Collection class
 */
public final class Collection {

    /* Define log tag */
    private static final String LOG_TAG = Collection.class.getSimpleName();


    /* Main class variables */
    private final File mFolder;
    private final LinkedList<Station> mStations;
    private int mStationIndexChanged;


    /* Constructor */
    public Collection(File newFolder) {

        File nomedia = new File(newFolder, ".nomedia");
        mFolder = newFolder;

        // create mFolder
        if (!mFolder.exists()) {
            Log.v(LOG_TAG, "Creating mFolder new folder: " + mFolder.toString());
            mFolder.mkdir();
        }

        // create nomedia file to prevent mediastore scanning
        if (!nomedia.exists()) {
            Log.v(LOG_TAG, "Creating .nomedia file in folder: " + mFolder.toString());

            try (FileOutputStream noMediaOutStream = new FileOutputStream(nomedia)) {
                noMediaOutStream.write(0);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Unable to write .nomedia file in folder: " + mFolder.toString());
            }
        }

        // create new array list of mStations
        mStations = new LinkedList<>();

        // create array of Files from mFolder
        File[] listOfFiles = mFolder.listFiles();

        if (listOfFiles != null) {
            // fill array list of mStations
            for (File listOfFile : listOfFiles) {
                if (listOfFile.isFile()
                        && listOfFile.toString().endsWith(".m3u")) {
                    // create new station from file
                    Station newStation = new Station(listOfFile);
                    if (newStation.getStreamUri() != null) {
                        mStations.add(newStation);
                    }
                }
            }
            // sort mStations
            Collections.sort(mStations);
        }

        // default value for mStationIndexChanged
        mStationIndexChanged = -1;

    }


    /* add station to collection */
    public boolean add(Station station) {
        if (station.getStationName() != null && station.getStreamUri() != null
                && unique(station) && !station.getStationPlaylistFile().exists()) {

            // add station to array list of mStations
            mStations.add(station);

            // save playlist file of station to local storage
            station.writePlaylistFile(mFolder);

            if (station.getStationImage() != null) {
                // save playlist file of station to local storage
                station.writeImageFile();
            }

            // sort mStations
            Collections.sort(mStations);

            return true;
        } else {
            Log.e(LOG_TAG, "Unable to add station to collection: Duplicate name and/or stream URL.");
            return false;
        }
    }


    /* delete station within collection */
    public boolean delete(int stationID) {

        boolean success = false;

        // delete playlist file
        File stationPlaylistFile = mStations.get(stationID).getStationPlaylistFile();
        if (stationPlaylistFile.exists()) {
            stationPlaylistFile.delete();
            success = true;
        }

        // delete station image file
        File stationImageFile = mStations.get(stationID).getStationImageFile();
        if (stationImageFile.exists()) {
            stationImageFile.delete();
            success = true;
        }

        // remove station
        if (success) {
            mStations.remove(stationID);
        }

        return success;
    }


    /* rename station within collection */
    public boolean rename(int stationID, String newStationName) {

        Station station = mStations.get(stationID);
        String oldStationName = station.getStationName();

        // name of station is new
        if (newStationName != null && !(newStationName.equals(oldStationName))) {

            // get reference to old files
            File oldStationPlaylistFile = station.getStationPlaylistFile();
            File oldStationImageFile = station.getStationImageFile();

            // set station name, file and image file for given new station
            station.setStationName(newStationName);
            station.setStationPlaylistFile(mFolder);
            station.setStationImageFile(mFolder);

            // rename playlist file
            oldStationPlaylistFile.delete();
            station.writePlaylistFile(mFolder);

            // rename image file
            File newStationImageFile = station.getStationImageFile();
            oldStationImageFile.renameTo(newStationImageFile);

            // sort mStations
            Collections.sort(mStations);

            // save new index if changed
            int newIndex = mStations.indexOf(station);
            if (newIndex != stationID) {
                mStationIndexChanged = newIndex;
            }

            return true;
        } else {
            // name of station is null or not new
            return false;
        }

    }


    /* Getter for mFolder */
    public File getFolder() {
        return mFolder;
    }


    /* Getter for mStations */
    public LinkedList<Station> getStations() {
        return mStations;
    }


    /* Getter for mStationIndexChanged */
    public int getStationIndexChanged () {
        return mStationIndexChanged;
    }


    /* toString method for collection of mStations */
    @Override
    public String toString() {
        String collectionToString;
        StringBuilder sb = new StringBuilder("");

        for (Station station : mStations) {
            sb.append(station.toString());
            sb.append("\n");
        }

        collectionToString = sb.toString();

        return collectionToString;
    }


    /* Check for duplicate station */
    private boolean unique(Station newStation) {

        // traverse mStations
        for (Station station : mStations) {
            // compare new station with existing mStations
            Uri streamUri = station.getStreamUri();
            Uri newStreamUri = newStation.getStreamUri();

            // compare URL of stream
            if (streamUri.equals(newStreamUri)) {
                Log.e(LOG_TAG, "Stream URL of " + station.getStationName() + " equals stream URL of "
                        + newStation.getStationName() + ": " + streamUri);
                return false;
            }
        }
        Log.v(LOG_TAG, newStation.getStationName() + " has a unique stream URL: " + newStation.getStreamUri());
        return true;
    }
}
