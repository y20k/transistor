/*
 * PlayerFragment.kt
 * Implements the PlayerFragment class
 * PlayerFragment is the fragment that hosts Transistor's list of stations and a player sheet
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-22 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.os.ResultReceiver
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import org.y20k.transistor.collection.CollectionAdapter
import org.y20k.transistor.collection.CollectionViewModel
import org.y20k.transistor.core.Collection
import org.y20k.transistor.core.Station
import org.y20k.transistor.dialogs.FindStationDialog
import org.y20k.transistor.dialogs.YesNoDialog
import org.y20k.transistor.extensions.isActive
import org.y20k.transistor.helpers.*
import org.y20k.transistor.playback.PlayerController
import org.y20k.transistor.playback.PlayerService
import org.y20k.transistor.ui.LayoutHolder
import org.y20k.transistor.ui.PlayerState
import java.util.*
import kotlin.coroutines.CoroutineContext


/*
 * PlayerFragment class
 */
class PlayerFragment: Fragment(), CoroutineScope,
        SharedPreferences.OnSharedPreferenceChangeListener,
        FindStationDialog.FindFindStationDialogListener,
        CollectionAdapter.CollectionAdapterListener,
        YesNoDialog.YesNoDialogListener {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(PlayerFragment::class.java)


    /* Main class variables */
    private lateinit var backgroundJob: Job
    private lateinit var mediaBrowser: MediaBrowserCompat
    private lateinit var collectionViewModel: CollectionViewModel
    private lateinit var layout: LayoutHolder
    private lateinit var collectionAdapter: CollectionAdapter
    private lateinit var playerController: PlayerController
    private var collection: Collection = Collection()
    private var playerServiceConnected: Boolean = false
    private var onboarding: Boolean = false
    private var station: Station = Station()
    private var playerState: PlayerState = PlayerState()
    private var listLayoutState: Parcelable? = null
    private var tempStationUuid: String = String()
    private val handler: Handler = Handler()


    /* Overrides coroutineContext variable */
    override val coroutineContext: CoroutineContext get() = backgroundJob + Dispatchers.Main


    /* Overrides onCreate from Fragment */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // initialize background job
        backgroundJob = Job()

        // handle back press
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // minimize player sheet - or if already minimized let activity handle back press
                if (isEnabled && !layout.minimizePlayerIfExpanded(activity as Context)) {
                    isEnabled = false
                    activity?.onBackPressed()
                }
            }
        })

        // create view model and observe changes in collection view model
        collectionViewModel = ViewModelProvider(this).get(CollectionViewModel::class.java)

        // create collection adapter
        collectionAdapter = CollectionAdapter(activity as Context, this as CollectionAdapter.CollectionAdapterListener)

        // Create MediaBrowserCompat
        mediaBrowser = MediaBrowserCompat(activity as Context, ComponentName(activity as Context, PlayerService::class.java), mediaBrowserConnectionCallback, null)

    }


    /* Overrides onCreate from Fragment*/
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        // find views and set them up
        val rootView: View = inflater.inflate(R.layout.fragment_player, container, false);
        layout = LayoutHolder(rootView)
        initializeViews()

        // convert old stations (one-time import)
        if (PreferencesHelper.isHouseKeepingNecessary()) {
            if (ImportHelper.convertOldStations(activity as Context)) layout.toggleImportingStationViews()
            PreferencesHelper.saveHouseKeepingNecessaryState()
        }

        // hide action bar
        (activity as AppCompatActivity).supportActionBar?.hide()

        return rootView
    }


    /* Overrides onResume from Fragment */
    override fun onStart() {
        super.onStart()
        // connect to PlayerService
        mediaBrowser.connect()
    }


    /* Overrides onSaveInstanceState from Fragment */
    override fun onSaveInstanceState(outState: Bundle) {
        if (this::layout.isInitialized) {
            // save current state of station list
            listLayoutState = layout.layoutManager.onSaveInstanceState()
            outState.putParcelable(Keys.KEY_SAVE_INSTANCE_STATE_STATION_LIST, listLayoutState)
        }
        // always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(outState)
    }


    /* Overrides onRestoreInstanceState from Activity */
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        // always call the superclass so it can restore the view hierarchy
        super.onActivityCreated(savedInstanceState)
        // restore state of station list
        listLayoutState = savedInstanceState?.getParcelable(Keys.KEY_SAVE_INSTANCE_STATE_STATION_LIST)
    }


    /* Overrides onResume from Fragment */
    override fun onResume() {
        super.onResume()
        // assign volume buttons to music volume
        activity?.volumeControlStream = AudioManager.STREAM_MUSIC
        // try to recreate player state
        playerState = PreferencesHelper.loadPlayerState()
        // setup ui
        setupPlayer()
        setupList()
        // start watching for changes in shared preferences
        PreferencesHelper.registerPreferenceChangeListener(this as SharedPreferences.OnSharedPreferenceChangeListener)
        // toggle download progress indicator
        layout.toggleDownloadProgressIndicator()
    }


    /* Overrides onPause from Fragment */
    override fun onPause() {
        super.onPause()
        // save player state
        PreferencesHelper.savePlayerState(playerState)
        // stop receiving playback progress updates
        handler.removeCallbacks(periodicProgressUpdateRequestRunnable)
        // stop watching for changes in shared preferences
        PreferencesHelper.unregisterPreferenceChangeListener(this as SharedPreferences.OnSharedPreferenceChangeListener)

    }


    /* Overrides onStop from Fragment */
    override fun onStop() {
        super.onStop()
        // (see "stay in sync with the MediaSession")
        if (this::playerController.isInitialized) playerController.unregisterCallback(mediaControllerCallback)
        mediaBrowser.disconnect()
        playerServiceConnected = false
    }


    /* Register the ActivityResultLauncher */
    private val requestLoadImageLauncher =
        registerForActivityResult(StartActivityForResult(), this::requestLoadImageResult)

    /* Pass the activity result */
    private fun requestLoadImageResult(result: ActivityResult) {
        if (result.resultCode == RESULT_OK && result.data != null) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) {
                collection = CollectionHelper.setStationImageWithStationUuid(activity as Context, collection, imageUri.toString(), tempStationUuid, imageManuallySet = true)
                tempStationUuid = String()
            }
        }
    }

    /* Register permission launcher */
    private val requestPermissionLauncher = registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            // permission granted
            pickImage()
        } else {
            // permission denied
            Toast.makeText(activity as Context, R.string.toastmessage_error_missing_storage_permission, Toast.LENGTH_LONG).show()
        }
    }


    /* Overrides onSharedPreferenceChanged from OnSharedPreferenceChangeListener */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == Keys.PREF_ACTIVE_DOWNLOADS) {
            layout.toggleDownloadProgressIndicator()
        }
    }


    /* Overrides onFindStationDialog from FindStationDialog */
    override fun onFindStationDialog(remoteStationLocation: String, station: Station) {
        super.onFindStationDialog(remoteStationLocation, station)
        if (remoteStationLocation.isNotEmpty()) {
            // detect content type on background thread
            launch {
                val deferred: Deferred<NetworkHelper.ContentType> = async(Dispatchers.Default) { NetworkHelper.detectContentTypeSuspended(remoteStationLocation) }
                // wait for result
                val contentType: String = deferred.await().type.lowercase(Locale.getDefault())
                // CASE: playlist detected
                if (Keys.MIME_TYPES_M3U.contains(contentType) or
                    Keys.MIME_TYPES_PLS.contains(contentType)) {
                    // download playlist
                    DownloadHelper.downloadPlaylists(activity as Context, arrayOf(remoteStationLocation))
                }
                // CASE: stream address detected
                else if (Keys.MIME_TYPES_MPEG.contains(contentType) or
                         Keys.MIME_TYPES_OGG.contains(contentType) or
                         Keys.MIME_TYPES_AAC.contains(contentType) or
                         Keys.MIME_TYPES_HLS.contains(contentType)) {
                    // create station and add to collection
                    val newStation: Station = Station(name = remoteStationLocation, streamUris = mutableListOf(remoteStationLocation), streamContent = contentType, modificationDate = GregorianCalendar.getInstance().time)
                    collection = CollectionHelper.addStation(activity as Context, collection, newStation)
                }
                // CASE: invalid address
                else {
                    Toast.makeText(activity as Context, R.string.toastmessage_station_not_valid, Toast.LENGTH_LONG).show()
                }
            }
        }

        if (station.radioBrowserStationUuid.isNotEmpty()) {
            // detect content type on background thread
            launch {
                val deferred: Deferred<NetworkHelper.ContentType> = async(Dispatchers.Default) { NetworkHelper.detectContentTypeSuspended(station.getStreamUri()) }
                // wait for result
                val contentType: NetworkHelper.ContentType = deferred.await()
                // set content type
                station.streamContent = contentType.type
                // add station and save collection
                collection = CollectionHelper.addStation(activity as Context, collection, station)
            }
        }
    }


    /* Overrides onPlayButtonTapped from CollectionAdapterListener */
    override fun onPlayButtonTapped(stationUuid: String, playbackState: Int) {
        when (playbackState) {
            // PLAYER STATE: PLAYING or BUFFERING
            PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.STATE_BUFFERING-> {
                // stop playback
                togglePlayback(false, stationUuid, playbackState)
            }
            // PLAYER STATE: NOT PLAYING
            else -> {
                // start playback
                togglePlayback(true, stationUuid, playbackState)
            }
        }
    }


    /* Overrides onAddNewButtonTapped from CollectionAdapterListener */
    override fun onAddNewButtonTapped() {
        FindStationDialog(activity as Activity, this as FindStationDialog.FindFindStationDialogListener).show()
    }


    /* Overrides onChangeImageButtonTapped from CollectionAdapterListener */
    override fun onChangeImageButtonTapped(stationUuid: String) {
        tempStationUuid = stationUuid
        pickImage()
    }


    /* Overrides onYesNoDialog from YesNoDialogListener */
    override fun onYesNoDialog(type: Int, dialogResult: Boolean, payload: Int, payloadString: String) {
        super.onYesNoDialog(type, dialogResult, payload, payloadString)
        when (type) {
            // handle result of remove dialog
            Keys.DIALOG_REMOVE_STATION -> {
                when (dialogResult) {
                    // user tapped remove station
                    true -> collectionAdapter.removeStation(activity as Context, payload)
                    // user tapped cancel
                    false -> collectionAdapter.notifyItemChanged(payload)
                }
            }
            // handle result from the restore collection dialog
            Keys.DIALOG_RESTORE_COLLECTION -> {
                when (dialogResult) {
                    // user tapped restore
                    true -> BackupHelper.restore(activity as Context, payloadString.toUri())
                    // user tapped cancel
                    false -> { /* do nothing */ }
                }
            }
        }
    }


    /* Sets up views and connects tap listeners - first run */
    private fun initializeViews() {
        // set adapter data source
        layout.recyclerView.adapter = collectionAdapter

        // enable swipe to delete
        val swipeToDeleteHandler = object : UiHelper.SwipeToDeleteCallback(activity as Context) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // ask user
                val adapterPosition: Int = viewHolder.adapterPosition
                val dialogMessage: String = "${getString(R.string.dialog_yes_no_message_remove_station)}\n\n- ${collection.stations[adapterPosition].name}"
                YesNoDialog(this@PlayerFragment as YesNoDialog.YesNoDialogListener).show(context = activity as Context, type = Keys.DIALOG_REMOVE_STATION, messageString = dialogMessage, yesButton = R.string.dialog_yes_no_positive_button_remove_station, payload = adapterPosition)
            }
        }
        val swipeToDeleteItemTouchHelper = ItemTouchHelper(swipeToDeleteHandler)
        swipeToDeleteItemTouchHelper.attachToRecyclerView(layout.recyclerView)

        // enable swipe to mark starred
        val swipeToMarkStarredHandler = object : UiHelper.SwipeToMarkStarredCallback(activity as Context) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // mark card starred
                val adapterPosition: Int = viewHolder.bindingAdapterPosition
                collectionAdapter.toggleStarredStation(activity as Context, adapterPosition)
            }
        }
        val swipeToMarkStarredItemTouchHelper = ItemTouchHelper(swipeToMarkStarredHandler)
        swipeToMarkStarredItemTouchHelper.attachToRecyclerView(layout.recyclerView)

        // set up sleep timer start button
        layout.sheetSleepTimerStartButtonView.setOnClickListener {
            val playbackState: PlaybackStateCompat = playerController.getPlaybackState()
            when (playbackState.isActive) {
                true -> playerController.startSleepTimer()
                false -> Toast.makeText(activity as Context, R.string.toastmessage_sleep_timer_unable_to_start, Toast.LENGTH_LONG).show()
            }
        }

        // set up sleep timer cancel button
        layout.sheetSleepTimerCancelButtonView.setOnClickListener {
            playerController.cancelSleepTimer()
            layout.sleepTimerRunningViews.isGone = true
        }

    }


    /* Builds playback controls - used after connected to player service */
    @SuppressLint("ClickableViewAccessibility") // it is probably okay to suppress this warning - the OnTouchListener on the time played view does only toggle the time duration / remaining display
    private fun buildPlaybackControls() {

        // get player state
        playerState = PreferencesHelper.loadPlayerState()

        // main play/pause button
        layout.playButtonView.setOnClickListener {
            onPlayButtonTapped(playerState.stationUuid, playerState.playbackState)
            //onPlayButtonTapped(playerState.stationUuid, playerController.getPlaybackState().state) // todo remove
        }

        // register a callback to stay in sync
        playerController.registerCallback(mediaControllerCallback)
    }


    /* Sets up the player */
    private fun setupPlayer() {
        layout.togglePlayButton(playerState.playbackState)
        var station: Station = Station()
        if (playerState.stationUuid.isNotEmpty()) {
            // get station from player state
            station = CollectionHelper.getStation(collection, playerState.stationUuid)
        } else if (collection.stations.isNotEmpty()) {
            // fallback: get first station
            station = collection.stations[0]
            playerState.stationUuid = station.uuid
        }
        layout.updatePlayerViews(activity as Context, station, playerState.playbackState)
    }


    /* Sets up state of list station list */
    private fun setupList() {
        if (listLayoutState != null) {
            layout.layoutManager.onRestoreInstanceState(listLayoutState)
        }
    }


    /* Starts / pauses playback */
    private fun togglePlayback(startPlayback: Boolean, stationUuid: String, playbackState: Int) {
        playerState.stationUuid = stationUuid
        playerState.playbackState = playbackState // = current state BEFORE desired startPlayback action
        // setup ui
        var station: Station = CollectionHelper.getStation(collection, playerState.stationUuid)
        if (!station.isValid() && collection.stations.isNotEmpty()) station = collection.stations[0]
        layout.updatePlayerViews(activity as Context, station, playerState.playbackState)
        // start / stop playback
        when (startPlayback) {
            true -> playerController.play(station.uuid)
            false -> playerController.stop()
        }
    }


    /* Check permissions and start image picker */
    private fun pickImage() {
        if (ContextCompat.checkSelfPermission(activity as Context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // permission READ_EXTERNAL_STORAGE not granted - request permission
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            // permission READ_EXTERNAL_STORAGE granted - get system picker for images
            val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            try {
                requestLoadImageLauncher.launch(pickImageIntent)
            } catch (e: Exception) {
                LogHelper.e(TAG, "Unable to select image. Probably no image picker available.")
                Toast.makeText(context, R.string.toastalert_no_image_picker, Toast.LENGTH_LONG).show()
            }
        }
    }


    /* Handles this activity's start intent */
    private fun handleStartIntent() {
        if ((activity as Activity).intent.action != null) {
            when ((activity as Activity).intent.action) {
                Keys.ACTION_SHOW_PLAYER -> handleShowPlayer()
                Intent.ACTION_VIEW -> handleViewIntent()
                Keys.ACTION_START_PLAYER_SERVICE -> handleStartPlayer()
            }
        }
        // clear intent action to prevent double calls
        (activity as Activity).intent.action = ""
    }


    /* Handles ACTION_SHOW_PLAYER request from notification */
    private fun handleShowPlayer() {
        LogHelper.i(TAG, "Tap on notification registered.")
        // todo implement
    }


    /* Handles ACTION_VIEW request to add Station */
    private fun handleViewIntent() {
        val contentUri: Uri? = (activity as Activity).intent.data
        if (contentUri != null) {
            val scheme: String = contentUri.scheme ?: String()
            if (scheme.startsWith("http")) DownloadHelper.downloadPlaylists(activity as Context, arrayOf(contentUri.toString()))
        }
    }

    /* Handles START_PLAYER_SERVICE request from App Shortcut */
    private fun handleStartPlayer() {
        val intent: Intent = (activity as Activity).intent
        if (intent.hasExtra(Keys.EXTRA_START_LAST_PLAYED_STATION)) {
            playerController.play(playerState.stationUuid)
        } else if (intent.hasExtra(Keys.EXTRA_STATION_UUID)) {
            val uuid: String = intent.getStringExtra(Keys.EXTRA_STATION_UUID) ?: String()
            playerController.play(uuid)
        } else if (intent.hasExtra(Keys.EXTRA_STREAM_URI)) {
            val streamUri: String = intent.getStringExtra(Keys.EXTRA_STREAM_URI) ?: String()
            playerController.playStreamDirectly(streamUri)
        }
    }


    /* Toggle periodic request of playback position from player service */
    private fun togglePeriodicProgressUpdateRequest(playbackState: PlaybackStateCompat) {
        when (playbackState.isActive) {
            true -> {
                handler.removeCallbacks(periodicProgressUpdateRequestRunnable)
                handler.postDelayed(periodicProgressUpdateRequestRunnable, 0)
            }
            false -> {
                handler.removeCallbacks(periodicProgressUpdateRequestRunnable)
                layout.sleepTimerRunningViews.isGone = true
            }
        }
    }


    /* Observe view model of collection of stations */
    private fun observeCollectionViewModel() {
        collectionViewModel.collectionLiveData.observe(this, Observer<Collection> { it ->
            // update collection
            collection = it
            // updates current station in player views
            playerState = PreferencesHelper.loadPlayerState()
            // toggle onboarding layout
            onboarding = layout.toggleOnboarding(activity as Context, collection.stations.size)
            // get station
            station = CollectionHelper.getStation(collection, playerState.stationUuid)
            // update player views
            layout.updatePlayerViews(activity as Context, station, playerState.playbackState)
            // handle start intent
            handleStartIntent()
            // handle navigation arguments
            handleNavigationArguments()
        })
        collectionViewModel.collectionSizeLiveData.observe(this, Observer<Int> { it ->
            // size of collection changed
            CollectionHelper.exportCollectionM3u(activity as Context, collection)
        })
    }


    /* Handles arguments handed over by navigation (from SettingsFragment) */
    private fun handleNavigationArguments() {
        // get arguments
        val updateCollection: Boolean = arguments?.getBoolean(Keys.ARG_UPDATE_COLLECTION, false) ?: false
        val updateStationImages: Boolean = arguments?.getBoolean(Keys.ARG_UPDATE_IMAGES, false) ?: false
        val restoreCollectionFileString: String? = arguments?.getString(Keys.ARG_RESTORE_COLLECTION)

        if (updateCollection) {
            arguments?.putBoolean(Keys.ARG_UPDATE_COLLECTION, false)
            val updateHelper: UpdateHelper = UpdateHelper(activity as Context, collectionAdapter, collection)
            updateHelper.updateCollection()
        }
        if (updateStationImages) {
            arguments?.putBoolean(Keys.ARG_UPDATE_IMAGES, false)
            DownloadHelper.updateStationImages(activity as Context)
        }
        if (!restoreCollectionFileString.isNullOrEmpty()) {
            arguments?.putString(Keys.ARG_RESTORE_COLLECTION, null)
            when (collection.stations.isNotEmpty()) {
                true -> {
                    YesNoDialog(this as YesNoDialog.YesNoDialogListener).show(
                        context = activity as Context,
                        type = Keys.DIALOG_RESTORE_COLLECTION,
                        messageString = "Replace current collection radio stations with the radio station from backup?",
                        payloadString = restoreCollectionFileString
                    )
                }
                false -> {
                    BackupHelper.restore(activity as Context, restoreCollectionFileString.toUri())
                }
            }
        }
    }


    /*
     * Defines callbacks for media browser service connection
     */
    private val mediaBrowserConnectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            // get the token for the MediaSession
            mediaBrowser.sessionToken.also { token ->
                // create a MediaControllerCompat
                val mediaController = MediaControllerCompat(activity as Context, token)
                // save the controller
                MediaControllerCompat.setMediaController(activity as Activity, mediaController)
                // initialize playerController
                playerController = PlayerController(mediaController)
            }
            playerServiceConnected = true

            mediaBrowser.subscribe(Keys.MEDIA_BROWSER_ROOT, mediaBrowserSubscriptionCallback)

            // finish building the UI
            buildPlaybackControls()

            if (playerState.playbackState == PlaybackStateCompat.STATE_PLAYING) {
                // start requesting continuous progess updates
                handler.removeCallbacks(periodicProgressUpdateRequestRunnable)
                handler.postDelayed(periodicProgressUpdateRequestRunnable, 0)
            }

            // begin looking for changes in collection
            observeCollectionViewModel()
        }

        override fun onConnectionSuspended() {
            playerServiceConnected = false
            // service has crashed. Disable transport controls until it automatically reconnects
        }

        override fun onConnectionFailed() {
            playerServiceConnected = false
            // service has refused our connection
        }
    }
    /*
     * End of callback
     */


    /*
     * Defines callbacks for media browser service subscription
     */
    private val mediaBrowserSubscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
            super.onChildrenLoaded(parentId, children)
        }

        override fun onError(parentId: String) {
            super.onError(parentId)
        }
    }
    /*
     * End of callback
     */


    /*
     * Defines callbacks for state changes of player service
     */
    private var mediaControllerCallback = object : MediaControllerCompat.Callback() {

        override fun onSessionReady() {
            LogHelper.d(TAG, "Session ready. Update UI.")
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            LogHelper.d(TAG, "Metadata changed. Update UI.")
        }

        override fun onPlaybackStateChanged(playbackState: PlaybackStateCompat) {
            LogHelper.d(TAG, "Playback State changed. Update UI.")
            playerState.playbackState = playbackState.state
            layout.animatePlaybackButtonStateTransition(activity as Context, playbackState.state)
            togglePeriodicProgressUpdateRequest(playbackState)
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }
    /*
     * End of callback
     */


    /*
     * Runnable: Periodically requests playback position (and sleep timer if running)
     */
    private val periodicProgressUpdateRequestRunnable: Runnable = object : Runnable {
        override fun run() {
            // request current playback position
            if (this@PlayerFragment::playerController.isInitialized) playerController.requestProgressUpdate(resultReceiver)
            // use the handler to start runnable again after specified delay
            handler.postDelayed(this, 500)
        }
    }
    /*
     * End of declaration
     */


    /*
     * ResultReceiver: Handles results from commands send to player
     */
    var resultReceiver: ResultReceiver = object: ResultReceiver(Handler()) {
        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
            when (resultCode) {
                Keys.RESULT_CODE_PERIODIC_PROGRESS_UPDATE -> {
                    if (resultData != null && resultData.containsKey(Keys.RESULT_DATA_METADATA)) {
                        layout.updateMetadata(resultData.getStringArrayList(Keys.RESULT_DATA_METADATA) ?: mutableListOf(), station.name, playerState.playbackState)
                    }
                    if (resultData != null && resultData.containsKey(Keys.RESULT_DATA_SLEEP_TIMER_REMAINING)) {
                        layout.updateSleepTimer(activity as Context, resultData.getLong(Keys.RESULT_DATA_SLEEP_TIMER_REMAINING, 0L))
                    }
                }
            }
        }
    }
    /*
     * End of ResultReceiver
     */

}
