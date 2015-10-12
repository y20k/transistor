/**
 * Collection.java
 * Implements the Collection class
 * A Collection holds a list of radio stations
 * <p/>
 * This file is part of
 * TRANSISTOR - Radio App for Android
 * <p/>
 * Copyright (c) 2015 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.core;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;


/**
 * Collection class
 */
public class Collection {

    /* Define log tag */
    public final String LOG_TAG = Collection.class.getSimpleName();

    /* Main class variables */
    private File mFolder;
    private LinkedList<Station> mStations;
    private Station mNowPlaying;
    private Station mLastPlayed;


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
            Log.v(LOG_TAG, "Creating .nomdeia file in folder: " + mFolder.toString());

            try (FileOutputStream noMediaOutStream = new FileOutputStream(nomedia)) {
                noMediaOutStream.write(0);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Unable to write .nomdeia file in folder: " + mFolder.toString());
            }
        }

        // create new array list of mStations
        mStations = new LinkedList<Station>();

        // create array of Files from mFolder
        File[] listOfFiles = mFolder.listFiles();

        // fill array list of mStations
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()
                    && listOfFiles[i].toString().endsWith(".m3u")) {
                // create new station from file
                Station newStation = new Station(listOfFiles[i]);
                if (newStation.getStreamURL() != null) {
                    mStations.add(newStation);
                }
            }
        }
        // sort mStations
        Collections.sort(mStations);
    }

    /* add station to collection */
    public boolean add(Station station) {
        if (station.getStationName() != null && station.getStreamURL() != null
                && unique(station) && !station.getStationPlaylistFile().exists()) {

            // add station to array list of mStations
            mStations.add(station);

            // save playlist file of station to local storage
            station.writePlaylistFile(mFolder);

            if (station.getStationImage() != null) {
                // save playlist file of station to local storage
                station.writeImageFile(mFolder);
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

        // delete playlist file
        File stationPlaylistFile = mStations.get(stationID).getStationPlaylistFile();
        if (stationPlaylistFile.exists()) {
            stationPlaylistFile.delete();
        }

        // delete station image file
        File stationImageFile = mStations.get(stationID).getStationImageFile();
        if (stationImageFile.exists()) {
            stationImageFile.delete();
        }

        // remove station
        mStations.remove(stationID);

        return true;
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
            URL streamURL = station.getStreamURL();
            URL newStreamURL = newStation.getStreamURL();

            // compare URL of stream
            if (streamURL.equals(newStreamURL)) {
                Log.e(LOG_TAG, "Stream URL of " + station.getStationName() + " equals stream URL of "
                        + newStation.getStationName() + ": " + streamURL);
                return false;
            }
        }
        Log.v(LOG_TAG, newStation.getStationName() + " has a unique stream URL: " + newStation.getStreamURL());
        return true;
    }
}
