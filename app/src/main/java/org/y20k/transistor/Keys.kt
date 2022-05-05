/*
 * Keys.kt
 * Implements the keys used throughout the app
 * This object hosts all keys used to control Transistor state
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-22 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor

import java.util.*


/*
 * Keys object
 */
object Keys {

    // application name
    const val APPLICATION_NAME: String = "Transistor"

    // version numbers
    const val CURRENT_COLLECTION_CLASS_VERSION_NUMBER: Int = 0

    // time values
    const val UPDATE_REPEAT_INTERVAL: Long = 4L             // every 4 hours
    const val MINIMUM_TIME_BETWEEN_UPDATES: Long = 180000L  // 3 minutes in milliseconds
    const val SLEEP_TIMER_DURATION: Long = 900000L          // 15 minutes in  milliseconds
    const val SLEEP_TIMER_INTERVAL: Long = 1000L            // 1 second in milliseconds

    // ranges
    val PLAYBACK_SPEEDS = arrayOf(1.0f, 1.2f, 1.4f, 1.6f, 1.8f, 2.0f)

    // notification
    const val NOW_PLAYING_NOTIFICATION_ID: Int = 42
    const val NOW_PLAYING_NOTIFICATION_CHANNEL_ID: String = "notificationChannelIdPlaybackChannel"

    // intent actions
    const val ACTION_SHOW_PLAYER: String = "org.y20k.transistor.action.SHOW_PLAYER"
    const val ACTION_START_PLAYER_SERVICE: String = "org.y20k.transistor.action.START_PLAYER_SERVICE"
    const val ACTION_COLLECTION_CHANGED: String = "org.y20k.transistor.action.COLLECTION_CHANGED"
    const val ACTION_START: String = "org.y20k.transistor.action.START"
    const val ACTION_STOP: String = "org.y20k.transistor.action.STOP"

    // intent extras
    const val EXTRA_COLLECTION_MODIFICATION_DATE: String = "COLLECTION_MODIFICATION_DATE"
    const val EXTRA_STATION_UUID: String = "STATION_UUID"
    const val EXTRA_STREAM_URI: String = "STREAM_URI"
    const val EXTRA_START_LAST_PLAYED_STATION: String = "START_LAST_PLAYED_STATION"

    // arguments
    const val ARG_UPDATE_COLLECTION: String = "ArgUpdateCollection"
    const val ARG_UPDATE_IMAGES: String = "ArgUpdateImages"
    const val ARG_RESTORE_COLLECTION: String = "ArgRestoreCollection"

    // keys
    const val KEY_DOWNLOAD_WORK_REQUEST: String = "DOWNLOAD_WORK_REQUEST"
    const val KEY_SAVE_INSTANCE_STATE_STATION_LIST: String = "SAVE_INSTANCE_STATE_STATION_LIST"
    const val KEY_STREAM_URI: String = "STREAM_URI"

    // custom MediaController commands
    const val CMD_RELOAD_PLAYER_STATE: String = "RELOAD_PLAYER_STATE"
    const val CMD_REQUEST_PROGRESS_UPDATE: String = "REQUEST_PROGRESS_UPDATE"
    const val CMD_START_SLEEP_TIMER: String = "START_SLEEP_TIMER"
    const val CMD_CANCEL_SLEEP_TIMER: String = "CANCEL_SLEEP_TIMER"
    const val CMD_PLAY_STREAM: String = "PLAY_STREAM"

    // preferences
    const val PREF_RADIO_BROWSER_API: String = "RADIO_BROWSER_API"
    const val PREF_ONE_TIME_HOUSEKEEPING_NECESSARY: String = "ONE_TIME_HOUSEKEEPING_NECESSARY_VERSIONCODE_72" // increment to current app version code to trigger housekeeping that runs only once
    const val PREF_THEME_SELECTION: String= "THEME_SELECTION"
    const val PREF_LAST_UPDATE_COLLECTION: String = "LAST_UPDATE_COLLECTION"
    const val PREF_COLLECTION_SIZE: String = "COLLECTION_SIZE"
    const val PREF_COLLECTION_MODIFICATION_DATE: String = "COLLECTION_MODIFICATION_DATE"
    const val PREF_CURRENT_PLAYBACK_STATE: String = "CURRENT_PLAYBACK_STATE"
    const val PREF_ACTIVE_DOWNLOADS: String = "ACTIVE_DOWNLOADS"
    const val PREF_DOWNLOAD_OVER_MOBILE: String = "DOWNLOAD_OVER_MOBILE"
    const val PREF_KEEP_DEBUG_LOG: String = "KEEP_DEBUG_LOG"
    const val PREF_STATION_LIST_EXPANDED_UUID = "STATION_LIST_EXPANDED_UUID"
    const val PREF_PLAYER_STATE_STATION_UUID: String = "PLAYER_STATE_STATION_UUID"
    const val PREF_PLAYER_STATE_PLAYBACK_STATE: String = "PLAYER_STATE_PLAYBACK_STATE"
    const val PREF_PLAYER_STATE_PLAYBACK_SPEED: String = "PLAYER_STATE_PLAYBACK_SPEED"
    const val PREF_PLAYER_STATE_BOTTOM_SHEET_STATE: String = "PLAYER_STATE_BOTTOM_SHEET_STATE"
    const val PREF_PLAYER_STATE_SLEEP_TIMER_STATE: String = "PLAYER_STATE_SLEEP_TIMER_STATE"
    const val PREF_PLAYER_METADATA_HISTORY: String = "PLAYER_METADATA_HISTORY"
    const val PREF_EDIT_STATIONS: String = "EDIT_STATIONS"
    const val PREF_EDIT_STREAMS_URIS: String = "EDIT_STREAMS_URIS"


    // states
    const val STATE_SLEEP_TIMER_STOPPED: Int = 0

    // default const values
    const val DEFAULT_SIZE_OF_METADATA_HISTORY: Int = 25
    const val DEFAULT_MAX_LENGTH_OF_METADATA_ENTRY: Int = 127
    const val DEFAULT_DOWNLOAD_OVER_MOBILE: Boolean = false
    const val ACTIVE_DOWNLOADS_EMPTY: String = "zero"

    // media browser
    const val MEDIA_BROWSER_ROOT = "__ROOT__"
    const val MEDIA_BROWSER_ROOT_RECENT = "__RECENT__"
    const val MEDIA_BROWSER_ROOT_EMPTY = "@empty@"

    // view types
    const val VIEW_TYPE_ADD_NEW: Int = 1
    const val VIEW_TYPE_STATION: Int = 2

    // view holder update types
    const val HOLDER_UPDATE_COVER: Int = 0
    const val HOLDER_UPDATE_NAME: Int = 1
    const val HOLDER_UPDATE_PLAYBACK_STATE: Int = 2
    const val HOLDER_UPDATE_DOWNLOAD_STATE: Int = 3
    const val HOLDER_UPDATE_PLAYBACK_PROGRESS: Int = 4

    // dialog types
    const val DIALOG_UPDATE_COLLECTION: Int = 1
    const val DIALOG_REMOVE_STATION: Int = 2
    const val DIALOG_DELETE_DOWNLOADS: Int = 3
    const val DIALOG_UPDATE_STATION_IMAGES: Int = 4
    const val DIALOG_RESTORE_COLLECTION: Int = 5

    // dialog results
    const val DIALOG_RESULT_DEFAULT: Int = -1
    const val DIALOG_EMPTY_PAYLOAD_STRING: String = ""
    const val DIALOG_EMPTY_PAYLOAD_INT: Int = -1

    // search types
    const val SEARCH_TYPE_BY_KEYWORD = 0
    const val SEARCH_TYPE_BY_UUID = 1

    // file types
    const val FILE_TYPE_DEFAULT: Int = 0
    const val FILE_TYPE_PLAYLIST: Int = 10
    const val FILE_TYPE_AUDIO: Int  = 20
    const val FILE_TYPE_IMAGE: Int  = 3

    // mime types and charsets and file extensions
    const val CHARSET_UNDEFINDED = "undefined"
    const val MIME_TYPE_JPG = "image/jpeg"
    const val MIME_TYPE_PNG = "image/png"
    const val MIME_TYPE_MPEG = "audio/mpeg"
    const val MIME_TYPE_HLS = "application/vnd.apple.mpegurl.audio"
    const val MIME_TYPE_M3U = "audio/x-mpegurl"
    const val MIME_TYPE_PLS = "audio/x-scpls"
    const val MIME_TYPE_XML = "text/xml"
    const val MIME_TYPE_ZIP = "application/zip"
    const val MIME_TYPE_OCTET_STREAM = "application/octet-stream"
    const val MIME_TYPE_UNSUPPORTED = "unsupported"
    val MIME_TYPES_M3U = arrayOf("application/mpegurl", "application/x-mpegurl", "audio/mpegurl", "audio/x-mpegurl")
    val MIME_TYPES_PLS = arrayOf("audio/x-scpls", "application/pls+xml")
    val MIME_TYPES_HLS = arrayOf("application/vnd.apple.mpegurl", "application/vnd.apple.mpegurl.audio")
    val MIME_TYPES_MPEG = arrayOf("audio/mpeg")
    val MIME_TYPES_OGG = arrayOf("audio/ogg", "application/ogg", "audio/opus")
    val MIME_TYPES_AAC = arrayOf("audio/aac", "audio/aacp")
    val MIME_TYPES_IMAGE = arrayOf("image/png", "image/jpeg")
    val MIME_TYPES_FAVICON = arrayOf("image/x-icon", "image/vnd.microsoft.icon")
    val MIME_TYPES_ZIP = arrayOf("application/zip", "application/x-zip-compressed", "multipart/x-zip")

    // folder names
    const val FOLDER_COLLECTION: String = "collection"
    const val FOLDER_AUDIO: String = "audio"
    const val FOLDER_IMAGES: String  = "images"
    const val FOLDER_TEMP: String  = "temp"
    const val TRANSISTOR_LEGACY_FOLDER_COLLECTION: String = "Collection"

    // file names and extensions
    const val COLLECTION_FILE: String = "collection.json"
    const val COLLECTION_M3U_FILE: String = "collection.m3u"
    const val COLLECTION_BACKUP_FILE: String = "transistor-backup.zip"
    const val STATION_IMAGE_FILE: String = "station-image.jpg"
    const val STATION_SMALL_IMAGE_FILE: String = "station-image-small.jpg"
    const val DEBUG_LOG_FILE: String = "log-can-be-deleted.txt"
    const val TRANSISTOR_LEGACY_STATION_FILE_EXTENSION: String = ".m3u"

    // server addresses
    const val RADIO_BROWSER_API_BASE: String = "all.api.radio-browser.info"
    const val RADIO_BROWSER_API_DEFAULT: String = "de1.api.radio-browser.info"

    // locations
    const val LOCATION_DEFAULT_STATION_IMAGE: String = "android.resource://org.y20k.transistor/drawable/ic_default_station_image_24dp"

    // sizes (in dp)
    const val SIZE_COVER_NOTIFICATION_LARGE_ICON: Int = 256
    const val SIZE_COVER_LOCK_SCREEN: Int = 320
    const val SIZE_STATION_IMAGE_CARD: Int = 72 // todo adjust according to layout
    const val SIZE_STATION_IMAGE_MAXIMUM: Int = 640
    const val SIZE_STATION_IMAGE_LOCK_SCREEN: Int = 320
    const val BOTTOM_SHEET_PEEK_HEIGHT: Int = 72

    // default values
    val DEFAULT_DATE: Date = Date(0L)
    const val DEFAULT_RFC2822_DATE: String = "Thu, 01 Jan 1970 01:00:00 +0100" // --> Date(0)
    const val EMPTY_STRING_RESOURCE: Int = 0

    // requests
    const val REQUEST_UPDATE_COLLECTION: Int = 2

    // results
    const val RESULT_DATA_SLEEP_TIMER_REMAINING: String = "DATA_SLEEP_TIMER_REMAINING"
    const val RESULT_CODE_PERIODIC_PROGRESS_UPDATE: Int = 1
    const val RESULT_DATA_METADATA: String = "DATA_PLAYBACK_PROGRESS"

    // theme states
    const val STATE_THEME_FOLLOW_SYSTEM: String = "stateFollowSystem"
    const val STATE_THEME_LIGHT_MODE: String = "stateLightMode"
    const val STATE_THEME_DARK_MODE: String = "stateDarkMode"

    // unique names
    const val NAME_PERIODIC_COLLECTION_UPDATE_WORK: String = "PERIODIC_COLLECTION_UPDATE_WORK"

}
