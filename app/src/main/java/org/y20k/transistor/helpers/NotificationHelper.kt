/*
 * NotificationHelper.kt
 * Implements the NotificationHelper class
 * A NotificationHelper creates and configures a notification
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-22 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.y20k.transistor.Keys
import org.y20k.transistor.R


/*
 * NotificationHelper class
 * Credit: https://github.com/android/uamp/blob/5bae9316b60ba298b6080de1fcad53f6f74eb0bf/common/src/main/java/com/example/android/uamp/media/UampNotificationManager.kt
 */
class NotificationHelper(private val context: Context, sessionToken: MediaSessionCompat.Token, notificationListener: PlayerNotificationManager.NotificationListener) {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(NotificationHelper::class.java)


    /* Main class variables */
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Main + serviceJob)
    private val notificationManager: PlayerNotificationManager
    private val mediaController: MediaControllerCompat = MediaControllerCompat(context, sessionToken)


    /* Constructor */
    init {
        // create a notification builder
        val notificationBuilder = PlayerNotificationManager.Builder(context, Keys.NOW_PLAYING_NOTIFICATION_ID, Keys.NOW_PLAYING_NOTIFICATION_CHANNEL_ID)
        notificationBuilder.apply {
            setChannelNameResourceId(R.string.notification_now_playing_channel_name)
            setChannelDescriptionResourceId(R.string.notification_now_playing_channel_description)
            setMediaDescriptionAdapter(DescriptionAdapter(mediaController))
            setNotificationListener(notificationListener)
        }
        // create and configure the notification manager
        notificationManager = notificationBuilder.build()
        notificationManager.apply {
            // note: notification icons are customized in values.xml
            setMediaSessionToken(sessionToken)
            setSmallIcon(R.drawable.ic_notification_app_icon_white_24dp)
            setUsePlayPauseActions(true)
            setUseStopAction(true) // set true to display the dismiss button
            setUsePreviousAction(true)
            setUsePreviousActionInCompactView(false)
            setUseNextAction(true) // only visible, if player is set to Player.REPEAT_MODE_ALL
            setUseNextActionInCompactView(false)
            setUseChronometer(true)
        }
    }


    /* Hides notification via notification manager */
    fun hideNotification() {
        notificationManager.setPlayer(null)
    }


    /* Displays notification via notification manager */
    fun showNotificationForPlayer(player: Player) {
        notificationManager.setPlayer(player)
    }


    /* Triggers notification */
    fun updateNotification() {
        notificationManager.invalidate()
    }


//    /*
//     * Inner class: Intercept stop button tap
//     */
//    private inner class Dispatcher: DefaultControlDispatcher(0L, 0L /* hide fast forward and rewind */) {
//        override fun dispatchStop(player: Player, reset: Boolean): Boolean {
//            // Default implementation see: https://github.com/google/ExoPlayer/blob/b1000940eaec9e1202d9abf341a48a58b728053f/library/core/src/main/java/com/google/android/exoplayer2/DefaultControlDispatcher.java#L137
//            mediaController.sendCommand(Keys.CMD_DISMISS_NOTIFICATION, null, null)
//            return true
//        }
//
//        override fun dispatchSetPlayWhenReady(player: Player, playWhenReady: Boolean): Boolean {
//            // changes the default behavior of !playWhenReady from player.pause() to player.stop()
//            when (playWhenReady) {
//                true -> player.play()
//                false -> player.stop()
//            }
//            return true
//        }
//
//        override fun dispatchPrevious(player: Player): Boolean {
//            mediaController.sendCommand(Keys.CMD_PREVIOUS_STATION, null, null)
//            return true
//        }
//
//        override fun dispatchNext(player: Player): Boolean {
//            mediaController.sendCommand(Keys.CMD_NEXT_STATION, null, null)
//            return true
//        }
//    }
//    /*
//     * End of inner class
//     */


    /*
     * Inner class: Create content of notification from metaddata
     */
    private inner class DescriptionAdapter(private val controller: MediaControllerCompat) : PlayerNotificationManager.MediaDescriptionAdapter {

        var currentIconUri: Uri? = null
        var currentBitmap: Bitmap? = null

        override fun createCurrentContentIntent(player: Player): PendingIntent? = controller.sessionActivity

        override fun getCurrentContentText(player: Player) = controller.metadata.description.subtitle.toString()

        override fun getCurrentContentTitle(player: Player) = controller.metadata.description.title.toString()

        override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): Bitmap? {
            val iconUri: Uri? = controller.metadata.description.iconUri
            return if (currentIconUri != iconUri || currentBitmap == null) {
                // Cache the bitmap for the current song so that successive calls to
                // `getCurrentLargeIcon` don't cause the bitmap to be recreated.
                currentIconUri = iconUri
                serviceScope.launch {
                    currentBitmap = iconUri?.let {
                        resolveUriAsBitmap(it)
                    }
                    currentBitmap?.let { callback.onBitmap(it) }
                }
                null
            } else {
                currentBitmap
            }
        }

        private suspend fun resolveUriAsBitmap(currentIconUri: Uri): Bitmap {
            return withContext(IO) {
                // Block on downloading artwork.
                ImageHelper.getStationImage(context, currentIconUri.toString())
            }
        }
    }
    /*
     * End of inner class
     */
}
