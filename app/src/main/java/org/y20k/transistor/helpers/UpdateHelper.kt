/*
 * UpdateHelper.kt
 * Implements the UpdateHelper class
 * A UpdateHelper provides methods to update a single station or the whole collection of stations
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-20 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers

import android.content.Context
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


    /* Listener Interface */
    interface UpdateHelperListener {
        fun onStationUpdated(collection: Collection, positionPriorUpdate: Int, positionAfterUpdate: Int)
    }


    /* Overrides onRadioBrowserSearchResults from RadioBrowserSearchListener */
    override fun onRadioBrowserSearchResults(results: Array<RadioBrowserResult>) {
        if (results.isNotEmpty()){
            // get station from results
            val station: Station = results[0].toStation()
            // get position
            val positionPriorUpdate = CollectionHelper.getStationPositionFromRadioBrowserStationUuid(collection, station.radioBrowserStationUuid)
            // update (and sort) collection
            collection = CollectionHelper.updateStation(context, collection, station)
            // get new position
            val positionAfterUpdate: Int = CollectionHelper.getStationPositionFromRadioBrowserStationUuid(collection, station.radioBrowserStationUuid)
            // hand over results
            updateHelperListener.onStationUpdated(collection, positionPriorUpdate, positionAfterUpdate)
        }
    }


    /* Updates the whole collection of stations */
    fun updateCollection() {
        PreferencesHelper.saveLastUpdateCollection(context)
        collection.stations.forEach {station ->
            updateStation(station)
        }
    }


    /* Initiates update of a station's information */
    fun updateStation(station: Station) {
        if (station.radioBrowserStationUuid.isNotEmpty()) {
            // get updated station from radio browser - results are handled by onRadioBrowserSearchResults
            val radioBrowserSearch: RadioBrowserSearch = RadioBrowserSearch(context, this)
            radioBrowserSearch.searchStation(context, station.radioBrowserStationUuid, Keys.SEARCH_TYPE_BY_UUID)
        } else if (station.remoteStationLocation.isNotEmpty()) {
            // download playlist // todo check content type detection is necessary here
            DownloadHelper.downloadPlaylists(context, arrayOf(station.remoteStationLocation))
        } else {
            LogHelper.w(TAG, "Unable to update station: ${station.name}.")
        }
    }


}