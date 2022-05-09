/*
 * UpdateHelper.kt
 * Implements the UpdateHelper class
 * A UpdateHelper provides methods to update a single station or the whole collection of stations
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-22 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import org.y20k.transistor.Keys
import org.y20k.transistor.core.Collection
import org.y20k.transistor.core.Station
import org.y20k.transistor.search.RadioBrowserResult
import org.y20k.transistor.search.RadioBrowserSearch


/*
 * UpdateHelper class
 */
class UpdateHelper(private val context: Context, private val updateHelperListener: UpdateHelperListener, private var collection: Collection): RadioBrowserSearch.RadioBrowserSearchListener {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(UpdateHelper::class.java)


    /* Main class variables */
    private var radioBrowserSearchCounter: Int = 0
    private var remoteStationLocationsList: MutableList<String> = mutableListOf()


    /* Listener Interface */
    interface UpdateHelperListener {
        fun onStationUpdated(collection: Collection, positionPriorUpdate: Int, positionAfterUpdate: Int)
    }


    /* Overrides onRadioBrowserSearchResults from RadioBrowserSearchListener */
    override fun onRadioBrowserSearchResults(results: Array<RadioBrowserResult>) {
        if (results.isNotEmpty()){
            CoroutineScope(IO).launch {
                // get station from results
                val station: Station = results[0].toStation()
                // detect content type
                val deferred: Deferred<NetworkHelper.ContentType> = async(Dispatchers.Default) { NetworkHelper.detectContentTypeSuspended(station.getStreamUri()) }
                // wait for result
                val contentType: NetworkHelper.ContentType = deferred.await()
                // update content type
                station.streamContent = contentType.type
                // get position
                val positionPriorUpdate = CollectionHelper.getStationPositionFromRadioBrowserStationUuid(collection, station.radioBrowserStationUuid)
                // update (and sort) collection
                collection = CollectionHelper.updateStation(context, collection, station)
                // get new position
                val positionAfterUpdate: Int = CollectionHelper.getStationPositionFromRadioBrowserStationUuid(collection, station.radioBrowserStationUuid)
                // hand over results
                withContext(Main) {
                    updateHelperListener.onStationUpdated(collection, positionPriorUpdate, positionAfterUpdate)
                }
                // decrease counter
                radioBrowserSearchCounter--
                // all downloads from radio browser succeeded
                if (radioBrowserSearchCounter == 0 && remoteStationLocationsList.isNotEmpty()) {
                    // direct download of playlists
                    DownloadHelper.downloadPlaylists(context, remoteStationLocationsList.toTypedArray())
                }
            }
        }
    }


    /* Updates the whole collection of stations */
    fun updateCollection() {
        PreferencesHelper.saveLastUpdateCollection()
        var counter: Boolean
        collection.stations.forEach {station ->
            if (station.radioBrowserStationUuid.isNotEmpty()) {
                // increase counter
                radioBrowserSearchCounter++
                // request download from radio browser
                downloadFromRadioBrowser(station.radioBrowserStationUuid)
            } else if (station.remoteStationLocation.isNotEmpty()) {
                // add playlist link to list for later(!) download in onRadioBrowserSearchResults
                remoteStationLocationsList.add(station.remoteStationLocation)
            } else {
                LogHelper.w(TAG, "Unable to update station: ${station.name}.")
            }
        }
        // special case: collection contained only playlist files
        if (radioBrowserSearchCounter == 0) {
            // direct download of playlists
            DownloadHelper.downloadPlaylists(context, remoteStationLocationsList.toTypedArray())
        }
    }


    /* Initiates update of a station's information */
    fun updateStation(station: Station) {
        if (station.radioBrowserStationUuid.isNotEmpty()) {
            // request download from radio browser
            downloadFromRadioBrowser(station.radioBrowserStationUuid)
        } else if (station.remoteStationLocation.isNotEmpty()) {
            // direct playlist download
            DownloadHelper.downloadPlaylists(context, arrayOf(station.remoteStationLocation))
        } else {
            LogHelper.w(TAG, "Unable to update station: ${station.name}.")
        }
    }


    /* Get updated station from radio browser - results are handled by onRadioBrowserSearchResults */
    private fun downloadFromRadioBrowser(radioBrowserStationUuid: String) {
        val radioBrowserSearch: RadioBrowserSearch = RadioBrowserSearch(this)
        radioBrowserSearch.searchStation(context, radioBrowserStationUuid, Keys.SEARCH_TYPE_BY_UUID)
    }

}
