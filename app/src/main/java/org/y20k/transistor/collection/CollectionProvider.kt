/*
 * CollectionProvider.kt
 * Implements the CollectionProvider class
 * A CollectionProvider provides a list of stations as MediaMetadata items
 * Credit: https://github.com/googlesamples/android-MediaBrowserService/ (-> MusicProvider)
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-22 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.collection

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import org.y20k.transistor.core.Collection
import org.y20k.transistor.core.Station
import org.y20k.transistor.helpers.CollectionHelper


/**
 * CollectionProvider.class
 */
class CollectionProvider {

    /* Define log tag */
    private val TAG = CollectionProvider::class.java.simpleName


    /* Main class variables */
    private enum class State { NON_INITIALIZED, INITIALIZING, INITIALIZED }
    private var currentState = State.NON_INITIALIZED
    val stationListByName: MutableList<MediaBrowserCompat.MediaItem> = mutableListOf()


    /* Callback used by PlayerService */
    interface CollectionProviderCallback {
        fun onStationListReady(success: Boolean)
    }


    /* Return current state */
    fun isInitialized(): Boolean {
        return currentState == State.INITIALIZED
    }


    /* Gets list of stations and caches meta information in list of MediaMetaItems */
    fun retrieveMedia(context: Context, collection: Collection, stationListProviderCallback: CollectionProviderCallback) {
        if (currentState == State.INITIALIZED) {
            // already initialized, set callback immediately
            stationListProviderCallback.onStationListReady(true)
        } else {
            // sort station list by name
            val stationListSorted: List<Station> = collection.stations.sortedBy { it.name }
            stationListSorted.forEach { station ->
                stationListByName.add(CollectionHelper.buildStationMediaMetaItem(context, station))
            }
            // afterwards: update state and set callback
            currentState = State.INITIALIZED
            stationListProviderCallback.onStationListReady(true)
        }
    }


    /* Get first station as media item */
    fun getFirstStation(): MediaBrowserCompat.MediaItem? {
        when (isInitialized() && stationListByName.isNotEmpty()) {
            true -> return stationListByName.first()
            false -> return null
        }
    }


    /* Get last station as media item */
    fun getLastStation(): MediaBrowserCompat.MediaItem? {
        when (isInitialized() && stationListByName.isNotEmpty()) {
            true -> return stationListByName.last()
            false -> return null
        }
    }


    /* Get next station as media item */
    fun getNextStation(stationUuid: String): MediaBrowserCompat.MediaItem? {
        stationListByName.forEachIndexed { index, mediaItem ->
            if (mediaItem.description.mediaId == stationUuid) {
                if (index + 1 > stationListByName.size) {
                    // return next station
                    return stationListByName[index + 1]
                }
            }
        }
        // default: return newest (cycle through from oldest)
        return getFirstStation()
    }


    /* Get previous station as media item */
    fun getPreviousStation(stationUuid: String): MediaBrowserCompat.MediaItem? {
        stationListByName.forEachIndexed { index, mediaItem ->
            if (mediaItem.description.mediaId == stationUuid) {
                if (index - 1 >= 0) {
                    // return previous station
                    return stationListByName[index - 1]
                }
            }
        }
        // default: return oldest (cycle through from newest)
        return getLastStation()
    }

}
