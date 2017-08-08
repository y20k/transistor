/**
 * CollectionAdapterDiffUtilCallback.java
 * Implements a DiffUtil Callback
 * A CollectionAdapterDiffUtilCallback is a DiffUtil.Callback that compares two lists of stations
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-17 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */

package org.y20k.transistor.adapter;

import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import org.y20k.transistor.core.Station;
import org.y20k.transistor.helpers.TransistorKeys;

import java.util.ArrayList;


/**
 * CollectionAdapterDiffUtilCallback class
 */
public class CollectionAdapterDiffUtilCallback extends DiffUtil.Callback implements TransistorKeys {

    /* Define log tag */
    private static final String LOG_TAG = CollectionAdapterDiffUtilCallback.class.getSimpleName();


    /* Main class variables */
    private ArrayList<Station> mOldStations;
    private ArrayList<Station> mNewStations;

    /* Constructor */
    public CollectionAdapterDiffUtilCallback(ArrayList<Station> oldStations, ArrayList<Station> newStations) {
        mOldStations = oldStations;
        mNewStations = newStations;
    }


    @Override
    public int getOldListSize() {
        return mOldStations.size();
    }


    @Override
    public int getNewListSize() {
        return mNewStations.size();
    }


    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        // Called by the DiffUtil to decide whether two objects represent the same Item.
        Station oldStation = mOldStations.get(oldItemPosition);
        Station newStation = mNewStations.get(newItemPosition);
        return oldStation.getStreamUri().equals(newStation.getStreamUri());
    }


    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        // Called by the DiffUtil when it wants to check whether two items have the same data. DiffUtil uses this information to detect if the contents of an item has changed.
        Station oldStation = mOldStations.get(oldItemPosition);
        Station newStation = mNewStations.get(newItemPosition);
        if (oldStation.getStationName().equals(newStation.getStationName()) &&
                oldStation.getPlaybackState() == newStation.getPlaybackState() &&
                oldStation.getStationImageSize() == newStation.getStationImageSize()) {
            return true;
        } else {
            return false;
        }
    }


    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        Station oldStation = mOldStations.get(oldItemPosition);
        Station newStation = mNewStations.get(newItemPosition);
        if (!(oldStation.getStationName().equals(newStation.getStationName()))) {
            return HOLDER_UPDATE_NAME;
        } else if (oldStation.getPlaybackState() != newStation.getPlaybackState()) {
            return HOLDER_UPDATE_PLAYBACK_STATE;
        } else if (oldStation.getStationImageSize() != newStation.getStationImageSize()){
            return HOLDER_UPDATE_IMAGE;
        } else {
            return super.getChangePayload(oldItemPosition, newItemPosition);
        }
    }

}