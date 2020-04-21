package org.y20k.transistor.helpers

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import org.y20k.transistor.Keys
import org.y20k.transistor.core.Station
import java.io.File
import java.util.*

object ImportHelper {


    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(ImportHelper::class.java)


    /* Converts older station of type .m3u  */
    fun convertOldStations(context: Context) {
        val oldStations: ArrayList<Station> = arrayListOf()
        val collectionFolder: File? = context.getExternalFilesDir(Keys.FOLDER_COLLECTION)
        if (collectionFolder != null && collectionFolder.exists() && collectionFolder.isDirectory) {
            collectionFolder.listFiles()?.forEach { file ->
                if (file.name.endsWith(Keys.TRANSISTOR_LEGACY_STATION_FILE_EXTENSION)) {
                    // read stream uri and name
                    val station: Station = FileHelper.readStationPlaylist(context, file.inputStream())
                    station.nameManuallySet = true
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
                }
            }
        }
    }


    /* Gets Uri for station images created by older Transistor versions */
    private fun getLegacyStationImageFileUri(context: Context, station: Station): Uri {
        val collectionFolder: File? = context.getExternalFilesDir(Keys.FOLDER_COLLECTION)
        if (collectionFolder != null && collectionFolder.exists() && collectionFolder.isDirectory) {
            val stationNameCleaned: String = station.name.replace(Regex("[:/]"), "_")
            return File("$collectionFolder/$stationNameCleaned.png").toUri()
        } else {
            return Keys.LOCATION_DEFAULT_STATION_IMAGE.toUri()
        }
    }


}