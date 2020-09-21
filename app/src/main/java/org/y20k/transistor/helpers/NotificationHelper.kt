/*
 * NotificationHelper.kt
 * Implements the NotificationHelper class
 * A NotificationHelper creates and configures a notification
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-20 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.media.session.MediaControllerCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import org.y20k.transistor.Keys
import org.y20k.transistor.R


/*
 * NotificationHelper class
 */
class NotificationHelper(val context: Context, val mediaController: MediaControllerCompat) {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(NotificationHelper::class.java)


    fun createPlayerNotificationManager(playerNotificationListener: PlayerNotificationManager.NotificationListener): PlayerNotificationManager {
        return PlayerNotificationManager.createWithNotificationChannel(
                context,
                    Keys.NOTIFICATION_NOW_PLAYING_CHANNEL,
                    R.string.notification_now_playing_channel_name,
                    R.string.notification_now_playing_channel_description,
                    Keys.NOTIFICATION_NOW_PLAYING_ID,
                    descriptionAdapter,
                    playerNotificationListener
            )
    }


    /* Set up notification properties via a MediaDescriptionAdapter */
    val descriptionAdapter = object : PlayerNotificationManager.MediaDescriptionAdapter {
        var currentIconUri: Uri? = null
        var currentBitmap: Bitmap? = null

        override fun createCurrentContentIntent(player: Player): PendingIntent? = mediaController.sessionActivity

        override fun getCurrentContentText(player: Player) = mediaController.metadata.description.subtitle.toString()

        override fun getCurrentContentTitle(player: Player) = mediaController.metadata.description.title.toString()

        override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): Bitmap? {
            val iconUri = mediaController.metadata.description.iconUri
            return if (currentIconUri != iconUri || currentBitmap == null) {
                // cache the bitmap for the current song so that successive calls to `getCurrentLargeIcon` don't cause the bitmap to be recreated
                currentIconUri = iconUri
                currentBitmap = ImageHelper.getScaledStationImage( context, iconUri.toString(), Keys.SIZE_COVER_NOTIFICATION_LARGE_ICON)
                currentBitmap
            } else {
                currentBitmap
            }
        }
    }

}