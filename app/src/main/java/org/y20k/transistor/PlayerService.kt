/*
 * PlayerService.kt
 * Implements the PlayerService class
 * PlayerService is Transistor's foreground service that plays radio station streams and handles playback controls
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-20 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.audiofx.AudioEffect
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.CountDownTimer
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media.MediaBrowserServiceCompat
import androidx.media.MediaBrowserServiceCompat.BrowserRoot.EXTRA_RECENT
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.metadata.icy.IcyHeaders
import com.google.android.exoplayer2.metadata.icy.IcyInfo
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.*
import org.y20k.transistor.collection.CollectionProvider
import org.y20k.transistor.core.Collection
import org.y20k.transistor.core.Station
import org.y20k.transistor.extensions.isActive
import org.y20k.transistor.helpers.*
import org.y20k.transistor.ui.PlayerState
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.min


/*
 * PlayerService class
 */
class PlayerService(): MediaBrowserServiceCompat(), Player.EventListener, MetadataOutput,CoroutineScope {


    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(PlayerService::class.java)


    /* Main class variables */
    private var collection: Collection = Collection()
    private var collectionProvider: CollectionProvider = CollectionProvider()
    private var station: Station = Station()
    private var isForegroundService: Boolean = false
    private lateinit var player: SimpleExoPlayer
    private lateinit var playerState: PlayerState
    private lateinit var metadataHistory: MutableList<String>
    private lateinit var backgroundJob: Job
    private lateinit var packageValidator: PackageValidator
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaController: MediaControllerCompat
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var userAgent: String
    private lateinit var modificationDate: Date
    private lateinit var collectionChangedReceiver: BroadcastReceiver
    private lateinit var sleepTimer: CountDownTimer
    private var sleepTimerTimeRemaining: Long = 0L
    private var playbackRestartCounter: Int = 0


    /* Overrides coroutineContext variable */
    override val coroutineContext: CoroutineContext get() = backgroundJob + Dispatchers.Main


    /* Overrides onCreate from Service */
    override fun onCreate() {
        super.onCreate()

        // initialize background job
        backgroundJob = Job()

        // set user agent
        userAgent = Util.getUserAgent(this, Keys.APPLICATION_NAME)

        // load modification date of collection
        modificationDate = PreferencesHelper.loadCollectionModificationDate(this)

        // get the package validator // todo can be local?
        packageValidator = PackageValidator(this, R.xml.allowed_media_browser_callers)

        // fetch the player state
        playerState = PreferencesHelper.loadPlayerState(this)

        // fetch the metadata history
        metadataHistory = PreferencesHelper.loadMetadataHistory(this)

        // create player
        player = createPlayer()

        // create a new MediaSession
        mediaSession = createMediaSession()
        sessionToken = mediaSession.sessionToken

        // because ExoPlayer will manage the MediaSession, add the service as a callback for state changes
        mediaController = MediaControllerCompat(this, mediaSession).also {
            it.registerCallback(MediaControllerCallback())
        }

        // initialize notification helper and notification manager
        notificationHelper = NotificationHelper(this)
        notificationManager = NotificationManagerCompat.from(this)

        // create and register collection changed receiver
        collectionChangedReceiver = createCollectionChangedReceiver()
        LocalBroadcastManager.getInstance(application).registerReceiver(collectionChangedReceiver, IntentFilter(Keys.ACTION_COLLECTION_CHANGED))

        // load collection
        collection = FileHelper.readCollection(this)
    }


    /* Overrides onStartCommand from Service */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent != null && intent.action == Keys.ACTION_STOP) {
            stopPlayback()
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
                startPlayback()
            }
        }

        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return Service.START_NOT_STICKY
    }


    /* Overrides onTaskRemoved from Service */
    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        // kill service, if MainActivity was canceled through task switcher
        //stopSelf()
    }


    /* Overrides onDestroy from Service */
    override fun onDestroy() {
        // save playback state
        handlePlaybackChange(PlaybackStateCompat.STATE_STOPPED)
        // release media session
        mediaSession.run {
            isActive = false
            release()
        }
        // cancel background job
        backgroundJob.cancel()
        // release player
        player.removeAnalyticsListener(analyticsListener)
        player.removeMetadataOutput(this)
        player.release()
    }


    /* Overrides onMetadata from MetadataOutput */
    override fun onMetadata(metadata: Metadata) {
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
        mediaController.playbackState?.let { updateNotification(it) }
        // update metadata
        mediaSession.setMetadata(CollectionHelper.buildStationMediaMetadata(this@PlayerService, station, metadataHistory.last()))
        // save history to
        PreferencesHelper.saveMetadataHistory(this, metadataHistory)
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
            val isRecentRequest = rootHints?.getBoolean(EXTRA_RECENT) ?: false
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


//    /* Overrides onPlayerError from Player.EventListener  */
//    override fun onPlayerError(error: ExoPlaybackException) {
//        when (error.type) {
//            ExoPlaybackException.TYPE_SOURCE -> LogHelper.e(TAG, "ExoPlaybackException TYPE_SOURCE: " + error.sourceException.message)
//            ExoPlaybackException.TYPE_RENDERER -> ftop(TAG, "ExoPlaybackException TYPE_RENDERER: " + error.rendererException.message)
//            ExoPlaybackException.TYPE_UNEXPECTED -> LogHelper.e(TAG, "ExoPlaybackException TYPE_UNEXPECTED: " + error.unexpectedException.message)
//            else -> LogHelper.e(TAG, "ExoPlaybackException OTHER: " + error.type)
//        }
//    }


    /* Overrides onPlayerStateChanged from Player.EventListener */
    override fun onPlayerStateChanged(playWhenReady: Boolean, playerState: Int) {
        when (playWhenReady) {
            // CASE: playWhenReady = true
            true -> {
                if (playerState == Player.STATE_READY) {
                    // active playback: update media session and save state
                    handlePlaybackChange(PlaybackStateCompat.STATE_PLAYING)
                } else if (playerState == Player.STATE_ENDED) {
                    // playback reached end: stop / end playback
                    handlePlaybackEnded()
                } else {
                    // not playing because the player is buffering, stopped or failed - check playbackState and player.getPlaybackError for details
                    handlePlaybackChange(PlaybackStateCompat.STATE_BUFFERING)
                }
            }
            // CASE: playWhenReady = false
            false -> {
                if (playerState == Player.STATE_READY) {
                    // stopped by app: update media session and save state
                    handlePlaybackChange(PlaybackStateCompat.STATE_PAUSED)
                } else if (playerState == Player.STATE_ENDED) {
                    // ended by app: update media session and save state
                    handlePlaybackChange(PlaybackStateCompat.STATE_STOPPED)
                } else {
                    LogHelper.w(TAG, "Unhandled player state change. Treat state as: playback stopped by app.")
                    handlePlaybackChange(PlaybackStateCompat.STATE_STOPPED)
                }
                // stop sleep timer - if running
                cancelSleepTimer()
            }
        }
    }


    /* Updates media session and save state */
    private fun handlePlaybackChange(playbackState: Int) {
        // reset restart counter
        playbackRestartCounter = 0
        // save collection state and player state
        collection = CollectionHelper.savePlaybackState(this, collection, station, playbackState)
        updatePlayerState(station, playbackState)
        // update media session
        mediaSession.setPlaybackState(createPlaybackState(playbackState, 0))
        mediaSession.isActive = playbackState != PlaybackStateCompat.STATE_STOPPED
    }


    /* Try to restart Playback */
    private fun handlePlaybackEnded() {
        // restart playback for up to five times
        if (playbackRestartCounter < 5) {
            playbackRestartCounter++
            startPlayback()
        } else {
            stopPlayback()
            Toast.makeText(this, this.getString(R.string.toastmessage_error_restart_playback_failed), Toast.LENGTH_LONG).show()
        }
    }


    /* Creates a new MediaSession */
    private fun createMediaSession(): MediaSessionCompat {
        val initialPlaybackState: Int = PreferencesHelper.loadPlayerPlaybackState(this)
        val sessionActivityPendingIntent =
                packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
                    PendingIntent.getActivity(this, 0, sessionIntent, 0)
                }
        return MediaSessionCompat(this, TAG)
                .apply {
                    setSessionActivity(sessionActivityPendingIntent)
                    setCallback(mediaSessionCallback)
                    setPlaybackState(createPlaybackState(initialPlaybackState, 0))
                }
    }


    /* Creates a simple exo player */
    private fun createPlayer(): SimpleExoPlayer {
        if (this::player.isInitialized) {
            player.removeAnalyticsListener(analyticsListener)
            player.release()
        }
        val audioAttributes: AudioAttributes = AudioAttributes.Builder()
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()
        val player = SimpleExoPlayer.Builder(this)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                // player.setMediaSourceFactory() does not make sense here, since Transistor selects MediaSourceFactory based on stream type
                .build()
        player.addListener(this@PlayerService)
        player.addAnalyticsListener(analyticsListener)
        player.addMetadataOutput(this)
        return player
    }


    /* Prepares player with media source created from current station */
    private fun preparePlayer() {
        // todo only prepare if not already prepared

        // build media item.
        val mediaItem: MediaItem = MediaItem.fromUri(station.getStreamUri())

        // create DataSource.Factory - produces DataSource instances through which media data is loaded
        val dataSourceFactory: DataSource.Factory = createDataSourceFactory(this, Util.getUserAgent(this, userAgent), null)

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
    }


    /* Creates a DataSourceFactor that supports http redirects */
    private fun createDataSourceFactory(context: Context, userAgent: String, listener: TransferListener?): DefaultDataSourceFactory {
        // Credit: https://stackoverflow.com/questions/41517440/exoplayer2-how-can-i-make-a-http-301-redirect-work
        // Default parameters, except allowCrossProtocolRedirects is true
        val httpDataSourceFactory = DefaultHttpDataSourceFactory(
                userAgent,
                listener,
                DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                true /* allowCrossProtocolRedirects */
        )
        return DefaultDataSourceFactory(
                context,
                listener,
                httpDataSourceFactory
        )
    }


    /* Start playback with current station */
    private fun startPlayback() {
        LogHelper.d(TAG, "Starting Playback. Station: ${station.name}.")
        // check if station is valid and collection has stations
        if (!station.isValid() && collection.stations.isNullOrEmpty()) {
            LogHelper.e(TAG, "Unable to start playback. Station has no stream addresses.")
            return
        }
        // default to last played station, if no station has been selected
        if (!station.isValid() && collection.stations.isNotEmpty()) {
            LogHelper.w(TAG, "No station has been selected. Starting playback of last played station.")
            station = CollectionHelper.getStation(collection, PreferencesHelper.loadLastPlayedStation(this@PlayerService))
        }
        // update metadata and prepare player
        updateMetadata(station.name)
        preparePlayer()
        // start playback
        player.playWhenReady = true
    }


    /* Stop playback */
    private fun stopPlayback() {
        LogHelper.d(TAG, "Stopping Playback")
        if (!player.isPlaying) {
            handlePlaybackChange(PlaybackStateCompat.STATE_STOPPED)
        }
        // pauses playback
        player.playWhenReady = false
    }


    /* Creates playback state - actions for playback state to be used in media session callback */
    private fun createPlaybackState(state: Int, position: Long): PlaybackStateCompat {
        val skipActions: Long = PlaybackStateCompat.ACTION_FAST_FORWARD or PlaybackStateCompat.ACTION_REWIND or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or  PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        when(state) {
            PlaybackStateCompat.STATE_PLAYING -> {
                return PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING, position, 1f)
                        .setActions(PlaybackStateCompat.ACTION_STOP or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or skipActions)
                        .build()
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                return PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PAUSED, position, 0f)
                        .setActions(PlaybackStateCompat.ACTION_PLAY)
                        .build()
            }
            else -> {
                return PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_STOPPED, position, 0f)
                        .setActions(PlaybackStateCompat.ACTION_PLAY)
                        .build()
            }
        }
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
                stopPlayback()
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


    /* Sets playback speed */
    private fun setPlaybackSpeed(speed: Float = 1f) {
        // update playback parameters - speed up playback
        player.setPlaybackParameters(PlaybackParameters(speed))
        // save speed
        // playerState.playbackSpeed = speed
        PreferencesHelper.savePlayerPlaybackSpeed(this, speed)
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
                LogHelper.w(TAG, "recent station requested.") // todo remove
                val recentStation = collectionProvider.getFirstStation() // todo change
                if (recentStation != null) mediaItems.add(recentStation)
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
        launch {
            // load collection on background thread
            val deferred: Deferred<Collection> = async(Dispatchers.Default) { FileHelper.readCollectionSuspended(context) }
            // wait for result and update collection
            collection = deferred.await()
            // special case: trigger metadata view update for stations that have no metadata
            if (playerState.playbackState == PlaybackState.STATE_PLAYING && station.name == metadataHistory.last()) {
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
        PreferencesHelper.savePlayerState(this, playerState)
    }


    /* Updates notification */
    private fun updateNotification(state: PlaybackStateCompat) {
        // skip building a notification when state is "none" and metadata is null
        // val notification = if (mediaController.metadata != null && state.state != PlaybackStateCompat.STATE_NONE) {
        val notification = if (state.state != PlaybackStateCompat.STATE_NONE) {
            val metadataString: String = if(metadataHistory.isNotEmpty()) metadataHistory.last() else String()
            notificationHelper.buildNotification(mediaSession.sessionToken, station, metadataString)
        } else {
            null
        }

        when (state.isActive) {
            // CASE: Playback has started
            true -> {
                /**
                 * This may look strange, but the documentation for [Service.startForeground]
                 * notes that "calling this method does *not* put the service in the started
                 * state itself, even though the name sounds like it."
                 */
                if (notification != null) {
                    notificationManager.notify(Keys.NOTIFICATION_NOW_PLAYING_ID, notification)
                    if (!isForegroundService) {
                        ContextCompat.startForegroundService(applicationContext, Intent(applicationContext, this@PlayerService.javaClass))
                        startForeground(Keys.NOTIFICATION_NOW_PLAYING_ID, notification)
                        isForegroundService = true
                    }
                }
            }
            // CASE: Playback has stopped
            false -> {
                if (isForegroundService) {
                    stopForeground(false)
                    isForegroundService = false

                    // if playback has ended, also stop the service.
                    if (state.state == PlaybackStateCompat.STATE_NONE) {
                        stopSelf()
                    }

                    if (notification != null && state.state != PlaybackStateCompat.STATE_STOPPED) {
                        notificationManager.notify(Keys.NOTIFICATION_NOW_PLAYING_ID, notification)
                    } else {
                        // remove notification - playback ended (or buildNotification failed)
                        stopForeground(true)
                    }
                }

            }
        }
    }


    /*
     * Custom AnalyticsListener that enables AudioFX equalizer integration
     */
    private var analyticsListener = object: AnalyticsListener {
        override fun onAudioSessionId(eventTime: AnalyticsListener.EventTime, audioSessionId: Int) {
            super.onAudioSessionId(eventTime, audioSessionId)
            // integrate with system equalizer (AudioFX)
            val intent: Intent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            sendBroadcast(intent)
        }
    }


    /*
     * Callback: Defines callbacks for active media session
     */
    private var mediaSessionCallback = object: MediaSessionCompat.Callback() {
        override fun onPlay() {
            // stop current playback, if necessary
            if (playerState.playbackState == PlaybackStateCompat.STATE_PLAYING) {
                stopPlayback()
            }
            // start playback of current station
            startPlayback()
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            // get station, set metadata and start playback
            station = CollectionHelper.getStation(collection, mediaId ?: String())

            startPlayback()
        }

        override fun onPause() {
            stopPlayback()
        }

        override fun onStop() {
            stopPlayback()
        }

        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
            // SPECIAL CASE: Empty query - user provided generic string e.g. 'Play music'
            if (query.isNullOrEmpty()) {
                // try to get last played station
                station = CollectionHelper.getStation(collection, PreferencesHelper.loadLastPlayedStation(this@PlayerService))
                if (station.isValid()) {
                    startPlayback()
                } else {
                    // unable to get the first station - notify user
                    Toast.makeText(this@PlayerService, R.string.toastmessage_error_no_station_found, Toast.LENGTH_LONG).show()
                    LogHelper.e(TAG, "Unable to start playback. Please add a radio station first. (Collection size = ${collection.stations.size} | provider initialized = ${collectionProvider.isInitialized()})")
                }
            }
            // NORMAL CASE: Try to match station name and voice query
            else {
                val queryLowercase: String = query.toLowerCase(Locale.getDefault())
                collectionProvider.stationListByName.forEach { mediaItem ->
                    // get station name (here -> title)
                    val stationName: String = mediaItem.description.title.toString().toLowerCase(Locale.getDefault())
                    // FIRST: try to match the whole query
                    if (stationName == queryLowercase) {
                        // start playback
                        onPlayFromMediaId(mediaItem.description.mediaId, null)
                        return
                    }
                    // SECOND: try to match parts of the query
                    else {
                        val words: List<String> = queryLowercase.split(" ")
                        words.forEach { word ->
                            if (stationName.contains(word)) {
                                // start playback
                                onPlayFromMediaId(mediaItem.description.mediaId, null)
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


        override fun onFastForward() {
            LogHelper.d(TAG, "onFastForward")
        }

        override fun onRewind() {
            LogHelper.d(TAG, "onRewind")
        }

        override fun onSkipToPrevious() {
            LogHelper.d(TAG, "onSkipToPrevious")
            // stop current playback, if necessary
            if (playerState.playbackState == PlaybackStateCompat.STATE_PLAYING) {
                stopPlayback()
            }
            // get station, set metadata and start playback
            station = CollectionHelper.getPreviousStation(collection, station.uuid)

            startPlayback()
        }

        override fun onSkipToNext() {
            LogHelper.d(TAG, "onSkipToNext")
            // stop current playback, if necessary
            if (playerState.playbackState == PlaybackStateCompat.STATE_PLAYING) {
                stopPlayback()
            }
            // get station, set metadata and start playback
            station = CollectionHelper.getNextStation(collection, station.uuid)

            startPlayback()
        }

        override fun onSeekTo(posistion: Long) {
            LogHelper.d(TAG, "onSeekTo")
        }

        override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
            when (command) {
                Keys.CMD_PLAY_STREAM -> {
                    val streamUri: String = extras?.getString(Keys.KEY_STREAM_URI) ?: String()
                    station = CollectionHelper.getStationWithStreamUri(collection, streamUri)
                    startPlayback()
                }
                Keys.CMD_RELOAD_PLAYER_STATE -> {
                    playerState = PreferencesHelper.loadPlayerState(this@PlayerService)
                }
                Keys.CMD_REQUEST_PROGRESS_UPDATE -> {
                    if (cb != null) {
                        val playbackProgressBundle: Bundle = bundleOf(Keys.RESULT_DATA_METADATA to metadataHistory)
                        if (sleepTimerTimeRemaining > 0L) {
                            playbackProgressBundle.putLong(Keys.RESULT_DATA_SLEEP_TIMER_REMAINING, sleepTimerTimeRemaining)
                        }
                        cb.send(Keys.RESULT_CODE_PERIODIC_PROGRESS_UPDATE, playbackProgressBundle)
                    }
                }
                Keys.CMD_START_SLEEP_TIMER -> {
                    startSleepTimer()
                }
                Keys.CMD_CANCEL_SLEEP_TIMER -> {
                    cancelSleepTimer()
                }
            }
        }

    }
    /*
     * End of callback
     */


    /*
     * Inner class: Class to receive callbacks about state changes to the MediaSessionCompat - handles notification
     * Source: https://github.com/googlesamples/android-UniversalMusicPlayer/blob/master/common/src/main/java/com/example/android/uamp/media/MusicService.kt
     */
    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            mediaController.playbackState?.let { updateNotification(it) }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            state?.let { updateNotification(it) }
        }
    }

}