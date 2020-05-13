/*
 * ImportHelper.kt
 * Implements the ImportHelper object
 * A ImportHelper provides methods for integrating station files from Transistor v3
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
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.y20k.transistor.Keys
import org.y20k.transistor.core.Collection
import org.y20k.transistor.core.Station
import java.io.File
import java.util.*


/*
 * ImportHelper object
 */
object ImportHelper {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(ImportHelper::class.java)


    /* Converts older station of type .m3u  */
    fun convertOldStations(context: Context): Boolean {
        val oldStations: ArrayList<Station> = arrayListOf()
        val oldCollectionFolder: File? = context.getExternalFilesDir(Keys.TRANSISTOR_LEGACY_FOLDER_COLLECTION)
        if (oldCollectionFolder != null && oldCollectionFolder.exists() && oldCollectionFolder.isDirectory) {
            val oldCollectionFiles = oldCollectionFolder.listFiles()
            // check if folder contains more than one file (more than e.g. collection.json)
            if (oldCollectionFiles != null && oldCollectionFiles.size > 1 ) {
                GlobalScope.launch {
                    // start import
                    var success: Boolean = false
                    oldCollectionFiles?.forEach { file ->
                        if (file.name.endsWith(Keys.TRANSISTOR_LEGACY_STATION_FILE_EXTENSION)) {
                            // read stream uri and name
                            val station: Station = FileHelper.readStationPlaylist(file.inputStream())
                            station.nameManuallySet = true
                            // detect stream content
                            station.streamContent = NetworkHelper.detectContentType(station.getStreamUri()).type
                            // try to also import station image
                            val sourceImageUri: Uri = getLegacyStationImageFileUri(context, station)
                            if (sourceImageUri != Keys.LOCATION_DEFAULT_STATION_IMAGE.toUri()) {
                                // create and add image and small image + get main color
                                station.image = FileHelper.saveStationImage(context, station.uuid, sourceImageUri, Keys.SIZE_STATION_IMAGE_CARD, Keys.STATION_SMALL_IMAGE_FILE).toString()
                                station.smallImage = FileHelper.saveStationImage(context, station.uuid, sourceImageUri, Keys.SIZE_STATION_IMAGE_MAXIMUM, Keys.STATION_IMAGE_FILE).toString()
                                station.imageColor = ImageHelper.getMainColor(context, sourceImageUri)
                                station.imageManuallySet = true
                            }
                            if (station.name.isEmpty()) {
                                station.name = file.name.substring(0, file.name.lastIndexOf("."))
                                station.nameManuallySet = false
                            }
                            station.modificationDate = GregorianCalendar.getInstance().time
                            // add station
                            oldStations.add(station)
                            success = true
                        }
                    }
                    // check for success (= at least one station was found)
                    if (success) {
                        // delete files from Transistor v3
                        oldCollectionFolder.deleteRecursively()
                        // save collection
                        val newCollection: Collection = Collection(stations = oldStations)
                        CollectionHelper.saveCollection(context, newCollection)
                    }
                }
            }
            // import has been started
            return true
        } else {
            // import has NOT been started
            return false
        }
    }


    /* Gets Uri for station images created by older Transistor versions */
    private fun getLegacyStationImageFileUri(context: Context, station: Station): Uri {
        val collectionFolder: File? = context.getExternalFilesDir(Keys.TRANSISTOR_LEGACY_FOLDER_COLLECTION)
        if (collectionFolder != null && collectionFolder.exists() && collectionFolder.isDirectory) {
            val stationNameCleaned: String = station.name.replace(Regex("[:/]"), "_")
            val legacyStationImage = File("$collectionFolder/$stationNameCleaned.png")
            if (legacyStationImage.exists()) {
                return legacyStationImage.toUri()
            } else {
                return Keys.LOCATION_DEFAULT_STATION_IMAGE.toUri()
            }
        } else {
            return Keys.LOCATION_DEFAULT_STATION_IMAGE.toUri()
        }
    }


}