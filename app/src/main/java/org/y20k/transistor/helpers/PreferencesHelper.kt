/*
 * PreferencesHelper.kt
 * Implements the PreferencesHelper object
 * A PreferencesHelper provides helper methods for the saving and loading values from shared preferences
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
import android.content.SharedPreferences
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.Gson
import org.y20k.transistor.Keys
import org.y20k.transistor.ui.PlayerState
import java.util.*


/*
 * PreferencesHelper object
 */
object PreferencesHelper {

    /* The sharedPreferences object to be initialized */
    private lateinit var sharedPreferences: SharedPreferences

    /* Initialize a single sharedPreferences object when the app is launched */
    fun Context.initPreferences() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    }

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(PreferencesHelper::class.java)


    /* Loads address of radio-browser.info API from shared preferences */
    fun loadRadioBrowserApiAddress(): String {
        return sharedPreferences.getString(Keys.PREF_RADIO_BROWSER_API, Keys.RADIO_BROWSER_API_DEFAULT) ?: Keys.RADIO_BROWSER_API_DEFAULT
    }


    /* Saves address of radio-browser.info API to shared preferences */
    fun saveRadioBrowserApiAddress(radioBrowserApi: String) {
        sharedPreferences.edit {
            putString(Keys.PREF_RADIO_BROWSER_API, radioBrowserApi)
        }
    }


    /* Loads keepDebugLog true or false */
    fun loadKeepDebugLog(): Boolean {
        return sharedPreferences.getBoolean(Keys.PREF_KEEP_DEBUG_LOG, false)
    }


    /* Saves keepDebugLog true or false */
    fun saveKeepDebugLog(keepDebugLog: Boolean = false) {
        sharedPreferences.edit {
            putBoolean(Keys.PREF_KEEP_DEBUG_LOG, keepDebugLog)
        }
    }


    /* Loads state of playback for player / PlayerService from shared preferences */
    fun loadPlayerPlaybackState(): Int {
        return sharedPreferences.getInt(Keys.PREF_CURRENT_PLAYBACK_STATE, PlaybackStateCompat.STATE_STOPPED)
    }


    /* Saves state of playback for player / PlayerService to shared preferences */
    fun savePlayerPlaybackState(playbackState: Int) {
        sharedPreferences.edit {
            putInt(Keys.PREF_CURRENT_PLAYBACK_STATE, playbackState)
        }
    }


    /* Loads state of playback for player / PlayerService from shared preferences */
    fun loadPlayerPlaybackSpeed(): Float {
        return sharedPreferences.getFloat(Keys.PREF_PLAYER_STATE_PLAYBACK_SPEED, 1f)
    }


    /* Saves state of playback for player / PlayerService to shared preferences */
    fun savePlayerPlaybackSpeed(playbackSpeed: Float) {
        sharedPreferences.edit {
            putFloat(Keys.PREF_PLAYER_STATE_PLAYBACK_SPEED, playbackSpeed)
        }
    }


    /* Load uuid of the station in the station list which is currently expanded */
    fun loadStationListStreamUuid(): String {
        return sharedPreferences.getString(Keys.PREF_STATION_LIST_EXPANDED_UUID, String()) ?: String()
    }


    /* Save uuid of the station in the station list which is currently expanded  */
    fun saveStationListStreamUuid(stationUuid: String = String()) {
        sharedPreferences.edit {
            putString(Keys.PREF_STATION_LIST_EXPANDED_UUID, stationUuid)
        }
    }


    /* Loads last update from shared preferences */
    fun loadLastUpdateCollection(): Date {
        val lastSaveString: String = sharedPreferences.getString(Keys.PREF_LAST_UPDATE_COLLECTION, "") ?: String()
        return DateTimeHelper.convertFromRfc2822(lastSaveString)
    }


    /* Saves last update to shared preferences */
    fun saveLastUpdateCollection(lastUpdate: Date = Calendar.getInstance().time) {
        sharedPreferences.edit {
            putString(Keys.PREF_LAST_UPDATE_COLLECTION, DateTimeHelper.convertToRfc2822(lastUpdate))
        }
    }


    /* Loads size of collection from shared preferences */
    fun loadCollectionSize(): Int {
        return sharedPreferences.getInt(Keys.PREF_COLLECTION_SIZE, -1)
    }


    /* Saves site of collection to shared preferences */
    fun saveCollectionSize(size: Int) {
        sharedPreferences.edit {
            putInt(Keys.PREF_COLLECTION_SIZE, size)
        }
    }


    /* Loads date of last save operation from shared preferences */
    fun loadCollectionModificationDate(): Date {
        val modificationDateString: String = sharedPreferences.getString(Keys.PREF_COLLECTION_MODIFICATION_DATE, "") ?: String()
        return DateTimeHelper.convertFromRfc2822(modificationDateString)
    }


    /* Saves date of last save operation to shared preferences */
    fun saveCollectionModificationDate(lastSave: Date = Calendar.getInstance().time) {
        sharedPreferences.edit {
            putString(Keys.PREF_COLLECTION_MODIFICATION_DATE, DateTimeHelper.convertToRfc2822(lastSave))
        }
    }


    /* Loads active downloads from shared preferences */
    fun loadActiveDownloads(): String {
        val activeDownloadsString: String = sharedPreferences.getString(Keys.PREF_ACTIVE_DOWNLOADS, Keys.ACTIVE_DOWNLOADS_EMPTY) ?: Keys.ACTIVE_DOWNLOADS_EMPTY
        LogHelper.v(TAG, "IDs of active downloads: $activeDownloadsString")
        return activeDownloadsString
    }


    /* Saves active downloads to shared preferences */
    fun saveActiveDownloads(activeDownloadsString: String = String()) {
        sharedPreferences.edit {
            putString(Keys.PREF_ACTIVE_DOWNLOADS, activeDownloadsString)
        }
    }


    /* Loads state of player user interface from shared preferences */
    fun loadPlayerState(): PlayerState {
        return PlayerState().apply {
            stationUuid = sharedPreferences.getString(Keys.PREF_PLAYER_STATE_STATION_UUID, String()) ?: String()
            playbackState = sharedPreferences.getInt(Keys.PREF_PLAYER_STATE_PLAYBACK_STATE, PlaybackStateCompat.STATE_STOPPED)
            bottomSheetState = sharedPreferences.getInt(Keys.PREF_PLAYER_STATE_BOTTOM_SHEET_STATE, BottomSheetBehavior.STATE_HIDDEN)
            sleepTimerState = sharedPreferences.getInt(Keys.PREF_PLAYER_STATE_SLEEP_TIMER_STATE, Keys.STATE_SLEEP_TIMER_STOPPED)
        }
    }


    /* Saves state of player user interface to shared preferences */
    fun savePlayerState(playerState: PlayerState) {
        sharedPreferences.edit {
            putString(Keys.PREF_PLAYER_STATE_STATION_UUID, playerState.stationUuid)
            putInt(Keys.PREF_PLAYER_STATE_PLAYBACK_STATE, playerState.playbackState)
            putInt(Keys.PREF_PLAYER_STATE_BOTTOM_SHEET_STATE, playerState.bottomSheetState)
            putInt(Keys.PREF_PLAYER_STATE_SLEEP_TIMER_STATE, playerState.sleepTimerState)
        }
    }


    /* Loads uuid of last played station from shared preferences */
    fun loadLastPlayedStationUuid(): String {
        return sharedPreferences.getString(Keys.PREF_PLAYER_STATE_STATION_UUID, String()) ?: String()
    }


    /* Saves history of metadata in shared preferences */
    fun saveMetadataHistory(metadataHistory: MutableList<String>) {
        val gson = Gson()
        val json = gson.toJson(metadataHistory)
        sharedPreferences.edit {
            putString(Keys.PREF_PLAYER_METADATA_HISTORY, json)
        }
    }


    /* Loads history of metadata from shared preferences */
    fun loadMetadataHistory(): MutableList<String> {
        var metadataHistory: MutableList<String> = mutableListOf()
        val json: String = sharedPreferences.getString(Keys.PREF_PLAYER_METADATA_HISTORY, String()) ?: String()
        if (json.isNotEmpty()) {
            val gson = Gson()
            metadataHistory = gson.fromJson(json, metadataHistory::class.java)
        }
        return metadataHistory
    }


    /* Start watching for changes in shared preferences - context must implement OnSharedPreferenceChangeListener */
    fun registerPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }


    /* Stop watching for changes in shared preferences - context must implement OnSharedPreferenceChangeListener */
    fun unregisterPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }


    /* Checks if housekeeping work needs to be done - used usually in DownloadWorker "REQUEST_UPDATE_COLLECTION" */
    fun isHouseKeepingNecessary(): Boolean {
        return sharedPreferences.getBoolean(Keys.PREF_ONE_TIME_HOUSEKEEPING_NECESSARY, true)
    }


    /* Saves state of housekeeping */
    fun saveHouseKeepingNecessaryState(state: Boolean = false) {
        sharedPreferences.edit {
            putBoolean(Keys.PREF_ONE_TIME_HOUSEKEEPING_NECESSARY, state)
        }
    }


    /* Load currently selected app theme */
    fun loadThemeSelection(): String {
        return sharedPreferences.getString(Keys.PREF_THEME_SELECTION, Keys.STATE_THEME_FOLLOW_SYSTEM) ?: Keys.STATE_THEME_FOLLOW_SYSTEM
    }


    /* Loads value of the option: Edit Stations */
    fun loadEditStationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(Keys.PREF_EDIT_STATIONS, false)
    }


    /* Saves value of the option: Edit Stations (only needed for migration) */
    fun saveEditStationsEnabled(enabled: Boolean = false) {
        sharedPreferences.edit {
            putBoolean(Keys.PREF_EDIT_STATIONS, enabled)
        }
    }


    /* Loads value of the option: Edit Station Streams */
    fun loadEditStreamUrisEnabled(): Boolean {
        return sharedPreferences.getBoolean(Keys.PREF_EDIT_STREAMS_URIS, false)
    }



    /* Return whether to download over mobile */
    fun downloadOverMobile(): Boolean {
        return sharedPreferences.getBoolean(Keys.PREF_DOWNLOAD_OVER_MOBILE, Keys.DEFAULT_DOWNLOAD_OVER_MOBILE)
    }

}
