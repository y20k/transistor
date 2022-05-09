/*
 * PlayerService.kt
 * Implements the PlayerService class
 * PlayerService is Transistor's foreground service that plays radio station audio
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-22 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.playback

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.audiofx.AudioEffect
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.icy.IcyHeaders
import com.google.android.exoplayer2.metadata.icy.IcyInfo
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import org.y20k.transistor.Keys
import org.y20k.transistor.R
import org.y20k.transistor.collection.CollectionProvider
import org.y20k.transistor.core.Collection
import org.y20k.transistor.core.Station
import org.y20k.transistor.helpers.*
import org.y20k.transistor.ui.PlayerState
import java.util.*
import kotlin.math.min


/*
 * PlayerService class
 */
class PlayerService(): MediaBrowserServiceCompat() {


    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(PlayerService::class.java)


    /* Main class variables */
    private var collection: Collection = Collection()
    private var collectionProvider: CollectionProvider = CollectionProvider()
    private var station: Station = Station()
    private var isForegroundService: Boolean = false
    private lateinit var forwardingPlayer: ForwardingPlayer
    private lateinit var playerState: PlayerState
    private lateinit var metadataHistory: MutableList<String>
    private lateinit var packageValidator: PackageValidator
    protected lateinit var mediaSession: MediaSessionCompat
    protected lateinit var mediaSessionConnector: MediaSessionConnector
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var userAgent: String
    private lateinit var modificationDate: Date
    private lateinit var collectionChangedReceiver: BroadcastReceiver
    private lateinit var sleepTimer: CountDownTimer
    private var sleepTimerTimeRemaining: Long = 0L
    private var playbackRestartCounter: Int = 0

    private val attributes = AudioAttributes.Builder()
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

    private val player: ExoPlayer by lazy {
        ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(attributes, true)
            setHandleAudioBecomingNoisy(true)
            repeatMode = Player.REPEAT_MODE_ALL
            addListener(playerListener)
            addAnalyticsListener(analyticsListener)
        }
    }


    /* Overrides onCreate from Service */
    override fun onCreate() {
        super.onCreate()
        // set user agent
        userAgent = Util.getUserAgent(this, Keys.APPLICATION_NAME)

        // load modification date of collection
        modificationDate = PreferencesHelper.loadCollectionModificationDate()

        // get the package validator // todo can be local?
        packageValidator = PackageValidator(this, R.xml.allowed_media_browser_callers)

        // fetch the player state
        playerState = PreferencesHelper.loadPlayerState()

        // fetch the metadata history
        metadataHistory = PreferencesHelper.loadMetadataHistory()

        // create a new MediaSession
        createMediaSession()

        // create custom ForwardingPlayer used in Notification and playback control
        forwardingPlayer = createForwardingPlayer()

        // ExoPlayer manages MediaSession
        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(preparer)
        mediaSessionConnector.setQueueNavigator(object : TimelineQueueNavigator(mediaSession) {
            override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
                // create media description - used in notification
                 return CollectionHelper.buildStationMediaDescription(this@PlayerService, station, getCurrentMetadata())
            }
        })

        // initialize notification helper
        notificationHelper = NotificationHelper(this, mediaSession.sessionToken, notificationListener)
//        notificationHelper.showNotificationForPlayer(forwardingPlayer)

        // create and register collection changed receiver
        collectionChangedReceiver = createCollectionChangedReceiver()
        LocalBroadcastManager.getInstance(application).registerReceiver(collectionChangedReceiver, IntentFilter(Keys.ACTION_COLLECTION_CHANGED))

        // load collection
        collection = FileHelper.readCollection(this)
    }


    /* Overrides onStartCommand from Service */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // handle start/stop requests issued via Intent - used for example by the home screen shortcuts
        if (intent != null && intent.action == Keys.ACTION_STOP) {
            player.stop()
        }
        if (intent != null && intent.action == Keys.ACTION_START) {
            if (intent.hasExtra(Keys.EXTRA_STATION_UUID)) {
                val stationUuid: String = intent.getStringExtra(Keys.EXTRA_STATION_UUID) ?: String()
                station = CollectionHelper.getStation(collection, stationUuid)
            } else if(intent.hasExtra(Keys.EXTRA_STREAM_URI)) {
                val streamUri: String = intent.getStringExtra(Keys.EXTRA_STREAM_URI) ?: String()
                station = CollectionHelper.getStationWithStreamUri(collection, streamUri)
            } else {
                station = CollectionHelper.getStation(collection, playerState.stationUuid)
            }
            if (station.isValid()) {
                preparePlayer(true)
            }
        }
        return Service.START_STICKY_COMPATIBILITY
    }


    /* Overrides onTaskRemoved from Service */
    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        // kill service, if MainActivity was canceled through task switcher
        //stopSelf()
    }


    /* Overrides onDestroy from Service */
    override fun onDestroy() {
        // set playback state if necessary
        if (player.isPlaying) {
            handlePlaybackChange(PlaybackStateCompat.STATE_STOPPED)
        }
        // release media session
        mediaSession.run {
            isActive = false
            release()
        }
        // release player
        player.removeAnalyticsListener(analyticsListener)
        player.removeListener(playerListener)
        player.release()
    }


    /* Updates metadata */
    private fun updateMetadata(metadata: String?) {
        // get metadata string
        val metadataString: String
        if (metadata != null && metadata.isNotEmpty()) {
            metadataString = metadata.substring(0, min(metadata.length, Keys.DEFAULT_MAX_LENGTH_OF_METADATA_ENTRY))
        } else {
            metadataString = station.name
        }
        // append metadata to metadata history
        if (metadataHistory.contains(metadataString)) {
            metadataHistory.removeIf { it == metadataString }
        }
        metadataHistory.add(metadataString)
        // trim metadata list
        if (metadataHistory.size > Keys.DEFAULT_SIZE_OF_METADATA_HISTORY) {
            metadataHistory.removeAt(0)
        }
        // update notification
        // see: https://github.com/google/ExoPlayer/issues/5494#issuecomment-462476576
        mediaSessionConnector.invalidateMediaSessionQueue()
        mediaSessionConnector.invalidateMediaSessionMetadata()
        notificationHelper.updateNotification()
        // save history
        PreferencesHelper.saveMetadataHistory(metadataHistory)
    }


    /* Overrides onGetRoot from MediaBrowserService */ // todo: implement a hierarchical structure -> https://github.com/googlesamples/android-UniversalMusicPlayer/blob/47da058112cee0b70442bcd0370c1e46e830c66b/media/src/main/java/com/example/android/uamp/media/library/BrowseTree.kt
    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        // Credit: https://github.com/googlesamples/android-UniversalMusicPlayer (->  MusicService)
        // LogHelper.d(TAG, "OnGetRoot: clientPackageName=$clientPackageName; clientUid=$clientUid ; rootHints=$rootHints")
        // to ensure you are not allowing any arbitrary app to browse your app's contents, you need to check the origin
        if (!packageValidator.isKnownCaller(clientPackageName, clientUid)) {
            // request comes from an untrusted package
            LogHelper.i(TAG, "OnGetRoot: Browsing NOT ALLOWED for unknown caller. "
                    + "Returning empty browser root so all apps can use MediaController."
                    + clientPackageName)
            return BrowserRoot(Keys.MEDIA_BROWSER_ROOT_EMPTY, null)
        } else {
            // content style extras: see https://developer.android.com/training/cars/media#apply_content_style
            val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
            val CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
            val CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
            val CONTENT_STYLE_LIST_ITEM_HINT_VALUE = 1
            val CONTENT_STYLE_GRID_ITEM_HINT_VALUE = 2
            val rootExtras = bundleOf(
                    CONTENT_STYLE_SUPPORTED to true,
                    CONTENT_STYLE_BROWSABLE_HINT to CONTENT_STYLE_GRID_ITEM_HINT_VALUE,
                    CONTENT_STYLE_PLAYABLE_HINT to CONTENT_STYLE_LIST_ITEM_HINT_VALUE
            )
            // check if rootHints contained EXTRA_RECENT - return BrowserRoot with MEDIA_BROWSER_ROOT_RECENT in that case
            val isRecentRequest = rootHints?.getBoolean(BrowserRoot.EXTRA_RECENT) ?: false
            val browserRootPath: String = if (isRecentRequest) Keys.MEDIA_BROWSER_ROOT_RECENT else Keys.MEDIA_BROWSER_ROOT
            return BrowserRoot(browserRootPath, rootExtras)
        }
    }


    /* Overrides onLoadChildren from MediaBrowserService */
    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        if (!collectionProvider.isInitialized()) {
            // use result.detach to allow calling result.sendResult from another thread:
            result.detach()
            collectionProvider.retrieveMedia(this, collection, object: CollectionProvider.CollectionProviderCallback {
                override fun onStationListReady(success: Boolean) {
                    if (success) {
                        loadChildren(parentId, result)
                    }
                }
            })
        } else {
            // if music catalog is already loaded/cached, load them into result immediately
            loadChildren(parentId, result)
        }
    }


    /* Updates media session and save state */
    private fun handlePlaybackChange(playbackState: Int) {
        // reset restart counter
        playbackRestartCounter = 0
        // save collection state and player state
        collection = CollectionHelper.savePlaybackState(this, collection, station, playbackState)
        updatePlayerState(station, playbackState)
        if (player.isPlaying) {
            notificationHelper.showNotificationForPlayer(forwardingPlayer)
        } else {
            updateMetadata(null)
        }
    }


    /* Try to restart Playback */
    private fun handlePlaybackEnded() {
        // restart playback for up to five times
        if (playbackRestartCounter < 5) {
            playbackRestartCounter++
            player.stop()
            player.play()
        } else {
            player.stop()
            Toast.makeText(this, this.getString(R.string.toastmessage_error_restart_playback_failed), Toast.LENGTH_LONG).show()
        }
    }


    /* Creates a forwardingPlayer that overrides default exoplayer behavior */
    private fun createForwardingPlayer() : ForwardingPlayer {
        return object : ForwardingPlayer(player) {
            // emulate headphone buttons
            // start/pause: adb shell input keyevent 85
            // next: adb shell input keyevent 87
            // prev: adb shell input keyevent 88
            override fun stop(reset: Boolean) {
                stop()
            }
            override fun stop() {
                player.stop()
                notificationHelper.hideNotification()
            }
            override fun pause() {
                player.stop() /* pause command translates internally to stop() */
            }
            override fun seekForward() {
                seekToNext()
            }
            override fun seekBack() {
                seekToPrevious()
            }
            override fun seekToNext() {
                // stop current playback, if necessary
                if (player.isPlaying) player.stop()
                // get station start playback
                station = CollectionHelper.getNextStation(collection, station.uuid)
                preparer.onPrepare(true)
            }
            override fun seekToPrevious() {
                // stop current playback, if necessary
                if (player.isPlaying) player.stop()
                // get station start playback
                station = CollectionHelper.getPreviousStation(collection, station.uuid)
                preparer.onPrepare(true)
            }
        }
    }


    /* Creates a new MediaSession */
    private fun createMediaSession() {
        val sessionActivityPendingIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
            PendingIntent.getActivity(this, 0, sessionIntent, PendingIntent.FLAG_IMMUTABLE)
        }
        mediaSession = MediaSessionCompat(this, TAG).apply {
            setSessionActivity(sessionActivityPendingIntent)
            isActive = true
//            setCallback(object : MediaSessionCompat.Callback() { // todo remove
//                override fun onPause() {
//                    LogHelper.e(TAG, "Ding")
//                    super.onPause()
//                }
//            })
        }
        sessionToken = mediaSession.sessionToken
    }


    /* Prepares player with media source created from current station */
    private fun preparePlayer(playWhenReady: Boolean) {
        // sanity check
        if (!station.isValid()) {
            LogHelper.e(TAG, "Unable to start playback. No radio station has been loaded.")
            return
        }

        // stop playback if necessary
        if (player.isPlaying) { player.stop() }

        // build media item.
        val mediaItem: MediaItem = MediaItem.fromUri(station.getStreamUri())

        // create DataSource.Factory - produces DataSource instances through which media data is loaded
        val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory().apply {
            setUserAgent(userAgent)
            // follow http redirects
            setAllowCrossProtocolRedirects(true)
        }

        // create MediaSource
        val mediaSource: MediaSource
        if (station.streamContent in Keys.MIME_TYPE_HLS || station.streamContent in Keys.MIME_TYPES_M3U) {
            // HLS media source
            //Toast.makeText(this, this.getString(R.string.toastmessage_stream_may_not_work), Toast.LENGTH_LONG).show()
            mediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        } else {
            // MPEG or OGG media source
            mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory).setContinueLoadingCheckIntervalBytes(32).createMediaSource(mediaItem)
        }

        // set source and prepare player
        player.setMediaSource(mediaSource)
        // player.setMediaItem() - unable to use here, because different media items may need different MediaSourceFactories to work properly
        player.prepare()

        // update media session connector using custom player
        mediaSessionConnector.setPlayer(forwardingPlayer)

        // reset metadata to station name
        updateMetadata(station.name)

        // set playWhenReady state
        player.playWhenReady = playWhenReady
    }


    /* Starts sleep timer / adds default duration to running sleeptimer */
    private fun startSleepTimer() {
        // stop running timer
        if (sleepTimerTimeRemaining > 0L && this::sleepTimer.isInitialized) {
            sleepTimer.cancel()
        }
        // initialize timer
        sleepTimer = object:CountDownTimer(Keys.SLEEP_TIMER_DURATION + sleepTimerTimeRemaining, Keys.SLEEP_TIMER_INTERVAL) {
            override fun onFinish() {
                LogHelper.v(TAG, "Sleep timer finished. Sweet dreams.")
                // reset time remaining
                sleepTimerTimeRemaining = 0L
                // stop playback
                player.stop()
            }
            override fun onTick(millisUntilFinished: Long) {
                sleepTimerTimeRemaining = millisUntilFinished
            }
        }
        // start timer
        sleepTimer.start()
    }


    /* Cancels sleep timer */
    private fun cancelSleepTimer() {
        if (this::sleepTimer.isInitialized) {
            sleepTimerTimeRemaining = 0L
            sleepTimer.cancel()
        }
    }


    /* Loads media items into result - assumes that collectionProvider is initialized */
    private fun loadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        val mediaItems = ArrayList<MediaBrowserCompat.MediaItem>()
        when (parentId) {
            Keys.MEDIA_BROWSER_ROOT -> {
                collectionProvider.stationListByName.forEach { item ->
                    mediaItems.add(item)
                }
            }
            Keys.MEDIA_BROWSER_ROOT_RECENT -> {
//                // un-comment (and implement ;-) ), if you want the media resumption notification to be shown
//                val recentStation = collectionProvider.getFirstStation() // todo get last played station
//                if (recentStation != null) mediaItems.add(recentStation)
            }
            Keys.MEDIA_BROWSER_ROOT_EMPTY -> {
                // do nothing
            }
            else -> {
                // log error
                LogHelper.w(TAG, "Skipping unmatched parentId: $parentId")
            }
        }
        result.sendResult(mediaItems)
    }


    /* Creates the collectionChangedReceiver - handles Keys.ACTION_COLLECTION_CHANGED */
    private fun createCollectionChangedReceiver(): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.hasExtra(Keys.EXTRA_COLLECTION_MODIFICATION_DATE)) {
                    val date: Date = Date(intent.getLongExtra(Keys.EXTRA_COLLECTION_MODIFICATION_DATE, 0L))

                    if (date.after(collection.modificationDate)) {
                        LogHelper.v(TAG, "PlayerService - reload collection after broadcast received.")
                        loadCollection(context)
                    }
                }
            }
        }
    }


    /* Reads collection of stations from storage using GSON */
    private fun loadCollection(context: Context) {
        LogHelper.v(TAG, "Loading collection of stations from storage")
        CoroutineScope(Main).launch {
            // load collection on background thread
            val deferred: Deferred<Collection> = async(Dispatchers.Default) { FileHelper.readCollectionSuspended(context) }
            // wait for result and update collection
            collection = deferred.await()
            // special case: trigger metadata view update for stations that have no metadata
            if (playerState.playbackState == PlaybackState.STATE_PLAYING && station.name == getCurrentMetadata()) {
                station = CollectionHelper.getStation(collection, station.uuid)
                updateMetadata(null)
            }
        }
    }


    /* Updates and saves the state of the player ui */
    private fun updatePlayerState(station: Station, playbackState: Int) {
        if (station.isValid()) {
            playerState.stationUuid = station.uuid
        }
        playerState.playbackState = playbackState
        PreferencesHelper.savePlayerState(playerState)
    }


    /* Gets the most current metadata string */
    private fun getCurrentMetadata(): String {
        val metadataString: String
        if (metadataHistory.isEmpty()) {
            metadataString = station.name
        } else {
            metadataString = metadataHistory.last()
        }
        return metadataString
    }


    /*
     * EventListener: Listener for ExoPlayer Events
     */
    private val playerListener = object : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean){
            if (isPlaying) {
                // active playback
                handlePlaybackChange(PlaybackStateCompat.STATE_PLAYING)
            } else {
                // playback stopped by user
                handlePlaybackChange(PlaybackStateCompat.STATE_STOPPED)
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            super.onPlayWhenReadyChanged(playWhenReady, reason)
            if (!playWhenReady) {
                // detect dismiss action
                if (player.mediaItemCount == 0) {
                    stopSelf()
                }
                when (reason) {
                    Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM -> {
                        // playback reached end: try to resume
                        handlePlaybackEnded()
                    }
                    else -> {
                        // playback has been paused by OS
                        // PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST or
                        // PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS or
                        // PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY or
                        // PLAY_WHEN_READY_CHANGE_REASON_REMOTE
                        handlePlaybackChange(PlaybackStateCompat.STATE_STOPPED)
                    }
                }
            } else if (playWhenReady && player.playbackState == Player.STATE_BUFFERING) {
                handlePlaybackChange(PlaybackStateCompat.STATE_BUFFERING)
            }
        }

        override fun onMetadata(metadata: Metadata) {
            super.onMetadata(metadata)
            for (i in 0 until metadata.length()) {
                val entry = metadata[i]
                // extract IceCast metadata
                if (entry is IcyInfo) {
                    val icyInfo: IcyInfo = entry as IcyInfo
                    updateMetadata(icyInfo.title)
                } else if (entry is IcyHeaders) {
                    val icyHeaders = entry as IcyHeaders
                    LogHelper.i(TAG, "icyHeaders:" + icyHeaders.name + " - " + icyHeaders.genre)
                } else {
                    LogHelper.w(TAG, "Unsupported metadata received (type = ${entry.javaClass.simpleName})")
                    updateMetadata(null)
                }
                // TODO implement HLS metadata extraction (Id3Frame / PrivFrame)
                // https://exoplayer.dev/doc/reference/com/google/android/exoplayer2/metadata/Metadata.Entry.html
            }
        }
    }

    /*
     * End of declaration
     */



    /*
     * NotificationListener: handles foreground state of service
     */
    private val notificationListener = object : PlayerNotificationManager.NotificationListener {
        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            super.onNotificationCancelled(notificationId, dismissedByUser)
            stopForeground(true)
            isForegroundService = false
            stopSelf()
        }

        override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
            super.onNotificationPosted(notificationId, notification, ongoing)
            if (ongoing && !isForegroundService) {
                ContextCompat.startForegroundService(applicationContext, Intent(applicationContext, this@PlayerService.javaClass))
                startForeground(Keys.NOW_PLAYING_NOTIFICATION_ID, notification)
                isForegroundService = true
            }
        }
    }
    /*
     * End of declaration
     */


    /*
     * PlaybackPreparer: Handles prepare and play requests - as well as custom commands like sleep timer control
     */
    private val preparer = object : MediaSessionConnector.PlaybackPreparer {

        override fun getSupportedPrepareActions(): Long =
                PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                        PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH or
                        PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH

        override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) = Unit

        override fun onPrepare(playWhenReady: Boolean) {
            if (station.isValid()) {
                preparePlayer(playWhenReady)
            } else {
                val currentStationUuid: String = PreferencesHelper.loadLastPlayedStationUuid()
                onPrepareFromMediaId(currentStationUuid, playWhenReady, null)
            }
        }

        override fun onPrepareFromMediaId(mediaId: String, playWhenReady: Boolean, extras: Bundle?) {
            // get station and start playback
            station = CollectionHelper.getStation(collection, mediaId ?: String())
            preparePlayer(playWhenReady)
        }

        override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) {

            // SPECIAL CASE: Empty query - user provided generic string e.g. 'Play music'
            if (query.isEmpty()) {
                // try to get first station
                val stationMediaItem: MediaBrowserCompat.MediaItem? = collectionProvider.getFirstStation()
                if (stationMediaItem != null) {
                    onPrepareFromMediaId(stationMediaItem.mediaId!!, playWhenReady = true, extras = null)
                } else {
                    // unable to get the first station - notify user
                    Toast.makeText(this@PlayerService, R.string.toastmessage_error_no_station_found, Toast.LENGTH_LONG).show()
                    LogHelper.e(TAG, "Unable to start playback. Please add a radio station first. (Collection size = ${collection.stations.size} | provider initialized = ${collectionProvider.isInitialized()})")
                }
            }
            // NORMAL CASE: Try to match station name and voice query
            else {
                val queryLowercase: String = query.lowercase(Locale.getDefault())
                collectionProvider.stationListByName.forEach { mediaItem ->
                    // get station name (here -> title)
                    val stationName: String = mediaItem.description.title.toString().lowercase(Locale.getDefault())
                    // FIRST: try to match the whole query
                    if (stationName == queryLowercase) {
                        // start playback
                        onPrepareFromMediaId(mediaItem.description.mediaId!!, playWhenReady = true, extras = null)
                        return
                    }
                    // SECOND: try to match parts of the query
                    else {
                        val words: List<String> = queryLowercase.split(" ")
                        words.forEach { word ->
                            if (stationName.contains(word)) {
                                // start playback
                                onPrepareFromMediaId(mediaItem.description.mediaId!!, playWhenReady = true, extras = null)
                                return
                            }
                        }
                    }
                }
                // NO MATCH: unable to match query - notify user
                Toast.makeText(this@PlayerService, R.string.toastmessage_error_no_station_matches_search, Toast.LENGTH_LONG).show()
                LogHelper.e(TAG, "Unable to find a station that matches your search query: $query")
            }
        }

        override fun onCommand(player: Player,  command: String, extras: Bundle?, cb: ResultReceiver?): Boolean {
            when (command) {
                Keys.CMD_RELOAD_PLAYER_STATE -> {
                    playerState = PreferencesHelper.loadPlayerState()
                    return true
                }
                Keys.CMD_REQUEST_PROGRESS_UPDATE -> {
                    if (cb != null) {
                        // check if station is valid - assumes that then the player has been prepared as well
                        if (station.isValid()) {
                            val playbackProgressBundle: Bundle = bundleOf(Keys.RESULT_DATA_METADATA to metadataHistory)
                            if (sleepTimerTimeRemaining > 0L) {
                                playbackProgressBundle.putLong(Keys.RESULT_DATA_SLEEP_TIMER_REMAINING, sleepTimerTimeRemaining)
                            }
                            cb.send(Keys.RESULT_CODE_PERIODIC_PROGRESS_UPDATE, playbackProgressBundle)
                            return true
                        } else {
                            return false
                        }
                    } else {
                        return false
                    }
                }
                Keys.CMD_START_SLEEP_TIMER -> {
                    startSleepTimer()
                    return true
                }
                Keys.CMD_CANCEL_SLEEP_TIMER -> {
                    cancelSleepTimer()
                    return true
                }
                Keys.CMD_PLAY_STREAM -> {
                    // get station and start playback
                    val streamUri: String = extras?.getString(Keys.KEY_STREAM_URI) ?: String()
                    station = CollectionHelper.getStationWithStreamUri(collection, streamUri)
                    preparePlayer(true)
                    return true
                }
                else -> {
                    return false
                }
            }
        }
    }
    /*
     * End of declaration
     */


    /*
     * Custom AnalyticsListener that enables AudioFX equalizer integration
     */
    private var analyticsListener = object: AnalyticsListener {
        override fun onAudioSessionIdChanged(eventTime: AnalyticsListener.EventTime, audioSessionId: Int) {
            super.onAudioSessionIdChanged(eventTime, audioSessionId)
            // integrate with system equalizer (AudioFX)
            val intent: Intent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            sendBroadcast(intent)
        }
    }
    /*
     * End of declaration
     */

}
