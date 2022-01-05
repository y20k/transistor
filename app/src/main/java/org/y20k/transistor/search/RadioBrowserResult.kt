/*
 * RadioBrowserResult.kt
 * Implements the RadioBrowserResult class
 * A RadioBrowserResult is the search result of a request to api.radio-browser.info
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-22 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.search

import android.support.v4.media.session.PlaybackStateCompat
import com.google.gson.annotations.Expose
import org.y20k.transistor.Keys
import org.y20k.transistor.core.Station
import java.util.*


/*
 * RadioBrowserResult class
 */
data class RadioBrowserResult (@Expose val changeuuid: String,
                               @Expose val stationuuid: String,
                               @Expose val name: String,
                               @Expose val url: String,
                               @Expose val url_resolved: String,
                               @Expose val homepage: String,
                               @Expose val favicon: String) {

    /* Converts RadioBrowserResult to Station  */
    fun toStation(): Station = Station(
            starred = false,
            name = name,
            nameManuallySet = false,
            streamUris = mutableListOf(url_resolved),
            stream = 0,
            streamContent = Keys.MIME_TYPE_UNSUPPORTED,
            homepage = homepage,
            image = Keys.LOCATION_DEFAULT_STATION_IMAGE,
            smallImage = Keys.LOCATION_DEFAULT_STATION_IMAGE,
            imageColor = -1,
            imageManuallySet = false,
            remoteImageLocation = favicon,
            remoteStationLocation = url,
            modificationDate = GregorianCalendar.getInstance().time,
            playbackState = PlaybackStateCompat.STATE_STOPPED,
            radioBrowserStationUuid = stationuuid,
            radioBrowserChangeUuid = changeuuid)

}


/*
JSON Struct Station
https://de1.api.radio-browser.info/

changeuuid          UUID 	                            A globally unique identifier for the change of the station information
stationuuid 	    UUID 	                            A globally unique identifier for the station
name                string                              The name of the station
url                 string, URL (HTTP/HTTPS)        	The stream URL provided by the user
url_resolved 	    string, URL (HTTP/HTTPS) 	        An automatically "resolved" stream URL. Things resolved are playlists (M3U/PLS/ASX...), HTTP redirects (Code 301/302). This link is especially usefull if you use this API from a platform that is not able to do a resolve on its own (e.g. JavaScript in browser) or you just don't want to invest the time in decoding playlists yourself.
homepage            string, URL (HTTP/HTTPS) 	        URL to the homepage of the stream, so you can direct the user to a page with more information about the stream.
favicon             string, URL (HTTP/HTTPS) 	        URL to an icon or picture that represents the stream. (PNG, JPG)
tags                string, multivalue, split by comma  Tags of the stream with more information about it
country 	        string 	                            DEPRECATED: use countrycode instead, full name of the country
countrycode 	    2 letters, uppercase                Official countrycodes as in ISO 3166-1 alpha-2
state               string                              Full name of the entity where the station is located inside the country
language            string, multivalue, split by comma  Languages that are spoken in this stream.
votes               number, integer                     Number of votes for this station. This number is by server and only ever increases. It will never be reset to 0.
lastchangetime      datetime, YYYY-MM-DD HH:mm:ss       Last time when the stream information was changed in the database
codec               string                              The codec of this stream recorded at the last check.
bitrate          	number, integer, bps             	The bitrate of this stream recorded at the last check.
hls 	            0 or 1                              Mark if this stream is using HLS distribution or non-HLS.
lastcheckok     	0 or 1                              The current online/offline state of this stream. This is a value calculated from multiple measure points in the internet. The test servers are located in different countries. It is a majority vote.
lastchecktime 	    datetime, YYYY-MM-DD HH:mm:ss    	The last time when any radio-browser server checked the online state of this stream
lastcheckoktime     datetime, YYYY-MM-DD HH:mm:ss 	    The last time when the stream was checked for the online status with a positive result
lastlocalchecktime 	datetime, YYYY-MM-DD HH:mm:ss 	    The last time when this server checked the online state and the metadata of this stream
clicktimestamp 	    datetime, YYYY-MM-DD HH:mm:ss 	    The time of the last click recorded for this stream
clickcount 	        number, integer 	                Clicks within the last 24 hours
clicktrend 	        number, integer 	                The difference of the clickcounts within the last 2 days. Posivite values mean an increase, negative a decrease of clicks.
 */
