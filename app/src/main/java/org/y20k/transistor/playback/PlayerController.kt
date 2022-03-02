/*
 * PlayerController.kt
 * Implements the PlayerController class
 * PlayerController is provides playback controls for PlayerService
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-22 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.playback

import android.os.ResultReceiver
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.os.bundleOf
import org.y20k.transistor.Keys
import org.y20k.transistor.helpers.LogHelper


/*
 * PlayerController class
 */
class PlayerController (private val mediaController: MediaControllerCompat) {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(PlayerController::class.java)


    /* Main class variables */
    private val transportControls: MediaControllerCompat.TransportControls = mediaController.transportControls


    /* Start playback for given media id */
    fun play(mediaId: String = String()) {
        if (mediaId.isNotEmpty()) {
            transportControls.playFromMediaId(mediaId, null)
        }
    }


    /* Stop playback - translates internally to pause */
    fun stop() {
        transportControls.pause()
    }


    /* Send command to start sleep timer */
    fun startSleepTimer() {
        mediaController.sendCommand(Keys.CMD_START_SLEEP_TIMER, null, null)
    }


    /* Send command to cancel sleep timer */
    fun cancelSleepTimer() {
        mediaController.sendCommand(Keys.CMD_CANCEL_SLEEP_TIMER, null, null)
    }


    /* Send command to request updates - used to build the ui */
    fun requestProgressUpdate(resultReceiver: ResultReceiver) {
        mediaController.sendCommand(Keys.CMD_REQUEST_PROGRESS_UPDATE, null, resultReceiver)
    }


    fun playStreamDirectly(streamUri: String) {
        mediaController.sendCommand(Keys.CMD_PLAY_STREAM, bundleOf(Pair(Keys.KEY_STREAM_URI, streamUri)), null)
    }


    /* Register MediaController callback to get notified about player state changes */
    fun registerCallback(callback: MediaControllerCompat.Callback) {
        mediaController.registerCallback(callback)
    }


    /* Unregister MediaController callback */
    fun unregisterCallback(callback: MediaControllerCompat.Callback) {
        mediaController.unregisterCallback(callback)
    }


    /* Get the current playback state */
    fun getPlaybackState(): PlaybackStateCompat = mediaController.playbackState

}
