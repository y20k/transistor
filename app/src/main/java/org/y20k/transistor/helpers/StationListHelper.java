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

import android.net.Uri;

import org.y20k.transistor.core.Station;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


/**
 * StationListHelper class
 */
public final class StationListHelper {


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




}
