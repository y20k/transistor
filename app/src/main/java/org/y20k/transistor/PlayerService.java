/**
 * PlayerService.java
 * Implements the app's playback background service
 * The service plays streaming audio and handles playback controls
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-20 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor;

import android.annotation.SuppressLint;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media.AudioAttributesCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultLoadControl.Builder;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.metadata.icy.IcyHeaders;
import com.google.android.exoplayer2.metadata.icy.IcyInfo;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.hls.HlsTrackMetadataEntry;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;

import org.y20k.transistor.core.Station;
import org.y20k.transistor.helpers.AudioFocusAwarePlayer;
import org.y20k.transistor.helpers.AudioFocusHelper;
import org.y20k.transistor.helpers.AudioFocusRequestCompat;
import org.y20k.transistor.helpers.LogHelper;
import org.y20k.transistor.helpers.NotificationHelper;
import org.y20k.transistor.helpers.PackageValidator;
import org.y20k.transistor.helpers.StationListProvider;
import org.y20k.transistor.helpers.TransistorKeys;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.android.exoplayer2.ExoPlaybackException.TYPE_RENDERER;
import static com.google.android.exoplayer2.ExoPlaybackException.TYPE_SOURCE;
import static com.google.android.exoplayer2.ExoPlaybackException.TYPE_UNEXPECTED;
import static com.google.android.exoplayer2.Player.STATE_BUFFERING;
import static com.google.android.exoplayer2.Player.STATE_ENDED;
import static com.google.android.exoplayer2.Player.STATE_IDLE;
import static com.google.android.exoplayer2.Player.STATE_READY;
import static org.y20k.transistor.helpers.StationListProvider.MEDIA_ID_EMPTY_ROOT;
import static org.y20k.transistor.helpers.StationListProvider.MEDIA_ID_ROOT;


/**
 * PlayerService class
 */
public final class PlayerService extends MediaBrowserServiceCompat implements TransistorKeys, AudioFocusAwarePlayer, Player.EventListener, MetadataOutput, AnalyticsListener {

    /* Define log tag */
    private static final String LOG_TAG = PlayerService.class.getSimpleName();


    /* Main class variables */
    private static Station mStation;
    private PackageValidator mPackageValidator;
    private StationListProvider mStationListProvider;
    private AudioFocusHelper mAudioFocusHelper;
    private AudioFocusRequestCompat mAudioFocusRequest;
    private static MediaSessionCompat mSession;
    private static MediaControllerCompat mController;
    private boolean mStationMetadataReceived;
    private boolean mPlayerInitLock;
    private HeadphoneUnplugReceiver mHeadphoneUnplugReceiver;
    private WifiManager.WifiLock mWifiLock;
    private PowerManager.WakeLock mWakeLock;
    private static SimpleExoPlayer mPlayer;
    private String mUserAgent;


    /* Constructor (default) */
    public PlayerService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // set up variables
        mStationMetadataReceived = false;
        mPlayerInitLock = false;
        mSession = createMediaSession(this);

        // set user agent
        mUserAgent = Util.getUserAgent(this, APPLICATION_NAME);

        // create Wifi and wake locks
        mWifiLock = ((WifiManager) this.getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "Transistor:wifi_lock");
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Transistor:wake_lock");

        // objects used by the unfinished Android Auto implementation
        mStationListProvider = new StationListProvider();
        mPackageValidator = new PackageValidator(this);

        // create audio focus helper
        mAudioFocusHelper = new AudioFocusHelper(this);

        // create audio focus request
        mAudioFocusRequest = createFocusRequest();

        // create media controller
        try {
            mController = new MediaControllerCompat(getApplicationContext(), mSession.getSessionToken());
        } catch (RemoteException e) {
            LogHelper.e(LOG_TAG, "RemoteException: " + e);
            e.printStackTrace();
        }

        // get instance of mPlayer
        createPlayer();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // checking for empty intent
        if (intent == null) {
            LogHelper.v(LOG_TAG, "Null-Intent received. Stopping self.");
            stopForeground(true); // Remove notification
            // stopSelf();
        }

        // ACTION PLAY
        else if (intent.getAction().equals(ACTION_PLAY)) {
            LogHelper.v(LOG_TAG, "Service received command: PLAY");

            // reset current station if necessary
            if (mStation != null && mStation.getPlaybackState() != PLAYBACK_STATE_STOPPED) {
                mStation.resetState();
                // send local broadcast: stopped
                Intent intentStateBuffering = new Intent();
                intentStateBuffering.setAction(ACTION_PLAYBACK_STATE_CHANGED);
                intentStateBuffering.putExtra(EXTRA_STATION, mStation);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intentStateBuffering);
                LogHelper.v(LOG_TAG, "LocalBroadcast: ACTION_PLAYBACK_STATE_CHANGED -> PLAYBACK_STATE_STOPPED");
            }

            // get URL of station from intent
            if (intent.hasExtra(EXTRA_STATION)) {
                mStation = intent.getParcelableExtra(EXTRA_STATION);
            }

            // update controller - start playback
            mController.getTransportControls().play();
        }

        // ACTION STOP
        else if (intent.getAction().equals(ACTION_STOP)) {
            LogHelper.v(LOG_TAG, "Service received command: STOP");

            // update controller - pause playback
            mController.getTransportControls().pause();
        }

        // ACTION DISMISS
        else if (intent.getAction().equals(ACTION_DISMISS)) {
            LogHelper.v(LOG_TAG, "Service received command: DISMISS");

            // update controller - stop playback
            if (mStation != null && mStation.getPlaybackState() != PLAYBACK_STATE_STOPPED) {
                mController.getTransportControls().stop();
            } else if (mStation != null) {
                // remove the foreground lock (dismisses notification) and don't keep media session active
                stopForeground(true);
                mSession.setActive(false);
            }
        }

        // listen for media button
        MediaButtonReceiver.handleIntent(mSession, intent);

        // default return value for media playback
        return START_STICKY;
    }



    @Override
    public void onMetadata(Metadata metadata) {
        for (int i = 0; i < metadata.length(); i++) {
            final Metadata.Entry entry = metadata.get(i);
            // extract IceCast metadata
            if (entry instanceof IcyInfo) {
                final IcyInfo icyInfo = ((IcyInfo) entry);
                updateMetadata(icyInfo.title);
            } else if (entry instanceof IcyHeaders) {
                final IcyHeaders icyHeaders = ((IcyHeaders) entry);
                LogHelper.i(LOG_TAG, "icyHeaders:" + icyHeaders.name + " - " + icyHeaders.genre);
            } else if (entry instanceof HlsTrackMetadataEntry) {
                final HlsTrackMetadataEntry hlsTrackMetadataEntry = ((HlsTrackMetadataEntry) entry);
            }
            // TODO implement HLS metadata extraction
            // https://exoplayer.dev/doc/reference/com/google/android/exoplayer2/metadata/Metadata.Entry.html
            // https://exoplayer.dev/doc/reference/com/google/android/exoplayer2/source/hls/HlsTrackMetadataEntry.html
        }
    }


    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

        switch (playbackState) {
            case STATE_BUFFERING:
                // player is not able to immediately play from the current position.
                LogHelper.v(LOG_TAG, "State of Player has changed: BUFFERING");

                // set playback state
                mStation.setPlaybackState(PLAYBACK_STATE_LOADING_STATION);
                saveAppState();

                // update notification
                mStation.setMetadata(this.getString(R.string.descr_station_stream_loading));
                NotificationHelper.update(this, mStation, mSession);

                // send local broadcast: buffering
                Intent intentStateBuffering = new Intent();
                intentStateBuffering.setAction(ACTION_PLAYBACK_STATE_CHANGED);
                intentStateBuffering.putExtra(EXTRA_STATION, mStation);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intentStateBuffering);
                LogHelper.v(LOG_TAG, "LocalBroadcast: ACTION_PLAYBACK_STATE_CHANGED -> PLAYBACK_STATE_LOADING_STATION");
                break;

            case STATE_ENDED:
                // player has finished playing the media.
                LogHelper.v(LOG_TAG, "State of Player has changed: ENDED");
                break;

            case STATE_IDLE:
                // player does not have a source to play, so it is neither buffering nor ready to play.
                LogHelper.v(LOG_TAG, "State of Player has changed: IDLE");
                break;

            case STATE_READY:
                // player is able to immediately play from the current position.
                LogHelper.v(LOG_TAG, "State of Player has changed: READY");

                if (mStation.getPlaybackState() == PLAYBACK_STATE_LOADING_STATION) {
                    // update playback state
                    mStation.setPlaybackState(PLAYBACK_STATE_STARTED);
                    saveAppState();
                    // send local broadcast: buffering finished - playback started
                    Intent intentStateReady = new Intent();
                    intentStateReady.setAction(ACTION_PLAYBACK_STATE_CHANGED);
                    intentStateReady.putExtra(EXTRA_STATION, mStation);
                    LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(intentStateReady);
                    LogHelper.v(LOG_TAG, "LocalBroadcast: ACTION_PLAYBACK_STATE_CHANGED -> PLAYBACK_STATE_STARTED");
                }

                // check for race between onPlayerStateChanged and MetadataHelper
                if (!mStationMetadataReceived && mStation.getPlaybackState() != PLAYBACK_STATE_STOPPED) {
                    mStation.setMetadata(mStation.getStationName());
                }

                // update notification
                NotificationHelper.update(this, mStation, mSession);

                break;

            default:
                // default
                break;

        }
    }


    @Override
    public void onPlayerError(ExoPlaybackException error) {
        switch (error.type) {
            case TYPE_RENDERER:
                // error occurred in a Renderer
                LogHelper.e(LOG_TAG, "An error occurred. Type RENDERER: " + error.getRendererException().toString());
                break;
            case TYPE_SOURCE:
                // error occurred loading data from a MediaSource.
                LogHelper.e(LOG_TAG, "An error occurred. Type SOURCE: " + error.getSourceException().toString());
                break;
            case TYPE_UNEXPECTED:
                // error was an unexpected RuntimeException.
                LogHelper.e(LOG_TAG, "An error occurred. Type UNEXPECTED: " + error.getUnexpectedException().toString());
                break;
            default:
                LogHelper.w(LOG_TAG, "An error occurred. Type OTHER ERROR.");
                break;
        }
    }


    @Override
    public void onLoadingChanged(boolean isLoading) {
        String state;
        if (isLoading) {
            state = "Media source is currently being loaded.";
        } else {
            state = "Media source is currently NOT being loaded.";
        }
        LogHelper.v(LOG_TAG, "State of loading has changed: " + state);
    }


    @Override
    public void onRepeatModeChanged(@Player.RepeatMode int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }


    @Override
    public void onPositionDiscontinuity(int reason) {

    }


    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }


    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        for (int i = 0; i < trackGroups.length; i++) {
            if (mPlayer.getRendererType(i) == C.TRACK_TYPE_AUDIO) {
                // update format metadata of station
                Format format = trackGroups.get(i).getFormat(0);
                mStation.setMimeType(format.sampleMimeType);
                mStation.setChannelCount(format.channelCount);
                mStation.setSampleRate(format.sampleRate);
                mStation.setBitrate(format.bitrate);

                // send local broadcast
                Intent intent = new Intent();
                intent.setAction(ACTION_PLAYBACK_STATE_CHANGED);
                intent.putExtra(EXTRA_STATION, mStation);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                LogHelper.v(LOG_TAG, "LocalBroadcast: ACTION_PLAYBACK_STATE_CHANGED -> EXTRA_STATION");
            }
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (SERVICE_INTERFACE.equals(intent.getAction())) {
            return super.onBind(intent);
        } else {
            return null;
        }
    }


    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        // Credit: https://github.com/googlesamples/android-UniversalMusicPlayer (->  MusicService)
        LogHelper.e(LOG_TAG, "OnGetRoot: clientPackageName=" + clientPackageName + "; clientUid=" + clientUid + " ; rootHints=" + rootHints); // todo change
        // to ensure you are not allowing any arbitrary app to browse your app's contents, you need to check the origin:
        if (!mPackageValidator.isCallerAllowed(this, clientPackageName, clientUid)) {
            // request comes from an untrusted package
            LogHelper.i(LOG_TAG, "OnGetRoot: Browsing NOT ALLOWED for unknown caller. "
                    + "Returning empty browser root so all apps can use MediaController."
                    + clientPackageName);
            return new MediaBrowserServiceCompat.BrowserRoot(MEDIA_ID_EMPTY_ROOT, null);
        }
        return new BrowserRoot(MEDIA_ID_ROOT, null);
    }


    @Override
    public void onLoadChildren(@NonNull final String parentMediaId, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        LogHelper.v(LOG_TAG, "OnLoadChildren called.");

        if (!mStationListProvider.isInitialized()) {
            // use result.detach to allow calling result.sendResult from another thread:
            result.detach();

            mStationListProvider.retrieveMediaAsync(this, new StationListProvider.Callback() {
                @Override
                public void onStationListReady(boolean success) {
                    if (success) {
                        loadChildren(parentMediaId, result);
//                    } else {
//                        updatePlaybackState(getString(R.string.error_no_metadata));
//                        result.sendResult(Collections.<MediaBrowserCompat.MediaItem>emptyList());
                    }
                }
            });

        } else {
            // if our music catalog is already loaded/cached, load them into result immediately
            loadChildren(parentMediaId, result);
        }

    }


    @Override
    public boolean isPlaying() {
        // checks if player is playing (method required by AudioFocusAwarePlayer)
        return mPlayer.getPlayWhenReady();
    }


    @Override
    public void play() {
        // start the stream (method required by AudioFocusAwarePlayer)
        mPlayer.setPlayWhenReady(true);
    }


    @Override
    public void pause() {
        // just stop the stream (method required by AudioFocusAwarePlayer)
        mPlayer.setPlayWhenReady(false);
    }


    @Override
    public void stop() {
        // stop the stream (method required by AudioFocusAwarePlayer)
        mController.getTransportControls().pause();
    }


    @Override
    /* Adjust volume - method required by AudioFocusAwarePlayer */
    public void setVolume(float volume) {
        // set volume in ExoPlayer
        mPlayer.setVolume(volume);
    }


    @Override
    public void onAudioSessionId(EventTime eventTime, int audioSessionId) {
        // integrate with system equalizer (AudioFX)
        final Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId);
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
        sendBroadcast(intent);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        LogHelper.v(LOG_TAG, "onDestroy called.");

        // stop playback
        if (mStation != null && mStation.getPlaybackState() != PLAYBACK_STATE_STOPPED) {
            mController.getTransportControls().stop();
        }

        // save state
        saveAppState();

        // release media session
        if (mSession != null) {
            mSession.setActive(false);
            mSession.release();
        }

        // remove analytics listener
        mPlayer.removeAnalyticsListener(this);

        // release player
        if (mPlayer != null) {
            releasePlayer();
        }

        // cancel notification
        stopForeground(true);
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        LogHelper.v(LOG_TAG, "onTaskRemoved called.");
    }


    /* Checks if media session is active */
    public static boolean isMediaSessionActive() {
        if (mSession == null) {
            return false;
        } else {
            return mSession.isActive();
        }
    }


    /* Checks if playback is running */
    public static boolean isPlaybackRunning() {
        if (mPlayer == null) {
            return false;
        } else {
            return mPlayer.getPlayWhenReady();
        }
    }


    /* Getter for current station */
    public static Station getCurrentStation() {
        return mStation;
    }


    /* Starts playback */
    private void startPlayback() {
        // check for null - can happen after a crash during playback
        if (mStation == null || mPlayer == null ||  mSession == null) {
            LogHelper.e(LOG_TAG, "Unable to start playback. An error occurred. Station is probably NULL.");
            saveAppState();
            // send local broadcast: playback stopped
            Intent intent = new Intent();
            intent.setAction(ACTION_PLAYBACK_STATE_CHANGED);
            intent.putExtra(EXTRA_ERROR_OCCURRED, true);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            // stop player service
            stopSelf();
            return;
        }

        // string representation of the stream uri of the previous station
        String previousStationUrlString;

        // stop running mPlayer, if necessary - set type playback change accordingly
        if (mPlayer.getPlayWhenReady()) {
            mPlayer.setPlayWhenReady(false);
            mPlayer.stop();
            previousStationUrlString = PreferenceManager.getDefaultSharedPreferences(getApplication()).getString(PREF_STATION_URL, null);
        } else {
            previousStationUrlString = null;
        }

        // set and save state
        mStationMetadataReceived = false;
        mStation.resetState();
        mStation.setPlaybackState(PLAYBACK_STATE_LOADING_STATION);
        saveAppState();

        // acquire Wifi and wake locks
        if (!mWifiLock.isHeld()) {
            mWifiLock.acquire();
        }
        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire(); // needs android.permission.WAKE_LOCK
        }

        // request audio focus and initialize media mPlayer
        if (mStation.getStreamUri() != null && mAudioFocusHelper.requestAudioFocus(mAudioFocusRequest)) {
            // initialize player and start playback
            initializePlayer();
            mPlayer.setPlayWhenReady(true);
            LogHelper.v(LOG_TAG, "Starting playback. Station name:" + mStation.getStationName());

            // update MediaSession
            updateMediaSession(mStation,true);

            // put up notification
            NotificationHelper.show(this, mSession, mStation);
        }

        // send local broadcast: buffering
        Intent intent = new Intent();
        intent.setAction(ACTION_PLAYBACK_STATE_CHANGED);
        intent.putExtra(EXTRA_STATION, mStation);
        if (previousStationUrlString != null) {
            intent.putExtra(EXTRA_PLAYBACK_STATE_PREVIOUS_STATION, previousStationUrlString);
        }
        LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(intent);
        LogHelper.v(LOG_TAG, "LocalBroadcast: ACTION_PLAYBACK_STATE_CHANGED -> PLAYBACK_STATE_LOADING_STATION");

        // register headphone listener
        IntentFilter headphoneUnplugIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        mHeadphoneUnplugReceiver = new HeadphoneUnplugReceiver();
        registerReceiver(mHeadphoneUnplugReceiver, headphoneUnplugIntentFilter);
    }


    /* Stops playback */
    private void stopPlayback(boolean dismissNotification) {
        // check for null - can happen after a crash during playback
        if (mStation == null || mPlayer == null || mSession == null) {
            LogHelper.e(LOG_TAG, "Stopping playback. An error occurred. Station is probably NULL.");
            saveAppState();
            // send local broadcast: playback stopped
            Intent intent = new Intent();
            intent.setAction(ACTION_PLAYBACK_STATE_CHANGED);
            intent.putExtra(EXTRA_ERROR_OCCURRED, true);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            // unregister headphone listener
            unregisterHeadphoneUnplugReceiver();
            // stop player service
            stopSelf();
            return;
        }

        // reset and save state
        mStation.resetState();
        mStationMetadataReceived = false;
        saveAppState();

        // release Wifi and wake locks
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }

        // stop playback
        mPlayer.setPlayWhenReady(false); // todo empty buffer
        mPlayer.stop();
        LogHelper.v(LOG_TAG, "Stopping playback. Station name:" + mStation.getStationName());

        // give up audio focus
        mAudioFocusHelper.abandonAudioFocus(mAudioFocusRequest);

        if (dismissNotification) {
            // remove the foreground lock (dismisses notification) and don't keep media session active
            stopForeground(true);
            // update media session
            updateMediaSession(mStation, false);
        } else {
            // remove the foreground lock and update notification (make it swipe-able)
            NotificationHelper.update(this, mStation, mSession);
            // update media session
            updateMediaSession(mStation, true);
        }

        // send local broadcast: playback stopped
        Intent intent = new Intent();
        intent.setAction(ACTION_PLAYBACK_STATE_CHANGED);
        intent.putExtra(EXTRA_STATION, mStation);
        LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(intent);
        LogHelper.v(LOG_TAG, "LocalBroadcast: ACTION_PLAYBACK_STATE_CHANGED -> PLAYBACK_STATE_STOPPED");

        // unregister headphone listener
        unregisterHeadphoneUnplugReceiver();
    }


    /* Updates station in MediaSession and state of MediaSession */
    private void updateMediaSession(Station station, boolean activeState) {
        mSession.setPlaybackState(createSessionPlaybackState());
        mSession.setMetadata(getSessionMetadata(getApplicationContext(), station));
        mSession.setActive(activeState);
    }


    /* Creates an instance of SimpleExoPlayer */
    private void createPlayer() {

        if (mPlayer != null) {
            releasePlayer();
        }

        // create default TrackSelector
        TrackSelector trackSelector = new DefaultTrackSelector();

        // create default LoadControl - increase buffer size
        LoadControl loadControl = createDefaultLoadControl(10);

        // create the player
        mPlayer = ExoPlayerFactory.newSimpleInstance(this, new DefaultRenderersFactory(getApplicationContext()), trackSelector, loadControl);

        // start listening for audio session id
        mPlayer.addAnalyticsListener(this);

        // start listening for stream metadata
        mPlayer.addMetadataOutput(this);
    }


    /* Creates a LoadControl - increase buffer size by given factor */
    private DefaultLoadControl createDefaultLoadControl(int factor) {
        Builder builder = new Builder();
        builder.setAllocator(new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE * factor));
        return builder.createDefaultLoadControl();
    }


    /* Add a media source to player */
    private void preparePlayer(int connectionType) {
        // create DataSource.Factory - produces DataSource instances through which media data is loaded
        DataSource.Factory dataSourceFactory;
        dataSourceFactory = createDataSourceFactory(this, Util.getUserAgent(this, mUserAgent), null);
        // dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, mUserAgent));

        // create MediaSource
        MediaSource mediaSource;
        if (connectionType == CONNECTION_TYPE_HLS) {
            // TODO HLS does not work reliable
            Toast.makeText(this, this.getString(R.string.toastmessage_stream_may_not_work), Toast.LENGTH_LONG).show();
            mediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mStation.getStreamUri());
        } else {
            mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).setContinueLoadingCheckIntervalBytes(32).createMediaSource(mStation.getStreamUri());
        }

        // prepare player with source.
        mPlayer.prepare(mediaSource);
    }


    /* Releases player */
    private void releasePlayer() {
        mPlayer.release();
        mPlayer = null;
    }


    /* Set up the media mPlayer */
    private void initializePlayer() {
        if (!mPlayerInitLock) {
            InitializePlayerHelper initializePlayerHelper = new InitializePlayerHelper();
            initializePlayerHelper.execute();
        }
    }


    /* unregister headphone listener */
    private void unregisterHeadphoneUnplugReceiver() {
        try {
            this.unregisterReceiver(mHeadphoneUnplugReceiver);
        } catch (Exception e) {
            LogHelper.v(LOG_TAG, "Unable to unregister HeadphoneUnplugReceiver");
            // e.printStackTrace();
        }
    }


    /* Creates request for AudioFocus */
    private AudioFocusRequestCompat createFocusRequest() {
        // build audio attributes
        @SuppressLint("WrongConstant") AudioAttributesCompat audioAttributes = new AudioAttributesCompat.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .build();

        // built and return focus request
        return new AudioFocusRequestCompat.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(mAudioFocusHelper.getListenerForPlayer(this))
                .setAudioAttributes(audioAttributes)
                .setFocusGain(AudioManager.AUDIOFOCUS_GAIN)
                .setWillPauseWhenDucked(false)
                .setAcceptsDelayedFocusGain(false) // todo check if this flag can be turned on (true)
                .build();
    }


    /* Creates media session */
    private MediaSessionCompat createMediaSession(Context context) {
        // create a media session
        MediaSessionCompat session = new MediaSessionCompat(context, LOG_TAG);
        session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setPlaybackState(createSessionPlaybackState());
        session.setCallback(new MediaSessionCallback());
        setSessionToken(session.getSessionToken());

        return session;
    }


    /* Creates playback state */
    private PlaybackStateCompat createSessionPlaybackState() {

        long skipActions;
        if (isCarUiMode()) {
            skipActions = PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        } else {
            skipActions = 0;
        }

        if (mStation == null || mStation.getPlaybackState() == PLAYBACK_STATE_STOPPED) {
            // define action for playback state to be used in media session callback
            return new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_STOPPED, 0, 0)
                    .setActions(PlaybackStateCompat.ACTION_PLAY | skipActions)
                    .build();
        } else {
            // define action for playback state to be used in media session callback - car mode version
            return new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, 0, 0)
                    .setActions(PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID | skipActions)
                    .build();
        }
    }


    /* Creates the metadata needed for MediaSession */
    private MediaMetadataCompat getSessionMetadata(Context context, Station station) {
        Bitmap stationImage = null;
        // try to get station image
        if (station != null && station.getStationImageFile() != null && station.getStationImageFile().exists()) {
            stationImage = BitmapFactory.decodeFile(station.getStationImageFile().toString());
        }
        // use name of app as album title
        String albumTitle = context.getResources().getString(R.string.app_name);

        // log metadata change
        LogHelper.v(LOG_TAG, "New Metadata available.");

        if (station != null) {
            return new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, station.getStationName())
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, station.getMetadata())
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, albumTitle)
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, stationImage)
                    .build();
        } else {
            return null;
        }
    }


    /* Loads media items into result - assumes that StationListProvider is initialized */
    private void loadChildren(@NonNull final String parentMediaId, final Result<List<MediaBrowserCompat.MediaItem>> result) {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        switch (parentMediaId) {
            case MEDIA_ID_ROOT:
                for (MediaMetadataCompat track : mStationListProvider.getAllStations()) {
                    MediaBrowserCompat.MediaItem item =
                            new MediaBrowserCompat.MediaItem(track.getDescription(),
                                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
                    mediaItems.add(item);
                }

                break;
            case MEDIA_ID_EMPTY_ROOT:
                // since the client provided the empty root we'll just send back an empty list
                break;
            default:
                LogHelper.w(LOG_TAG, "Skipping unmatched parentMediaId: " + parentMediaId);
                break;
        }
        result.sendResult(mediaItems);
    }


    /* Detects car mode */
    private boolean isCarUiMode() {
        UiModeManager uiModeManager = (UiModeManager) this.getSystemService(Context.UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_CAR) {
            LogHelper.v(LOG_TAG, "Running in Car mode");
            return true;
        } else {
            LogHelper.v(LOG_TAG, "Running on a non-Car mode");
            return false;
        }
    }


    /* Updates metadata info and broadcasts the change to the user interface */
    private void updateMetadata(String metadata) {
        if (metadata == null || metadata.length() > 0 ) {
            mStation.setMetadata(metadata);
        } else {
            mStation.setMetadata(mStation.getStationName());
        }
        mStationMetadataReceived = true;

        // send local broadcast
        Intent i = new Intent();
        i.setAction(ACTION_METADATA_CHANGED);
        i.putExtra(EXTRA_STATION, mStation);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
        LogHelper.v(LOG_TAG, "LocalBroadcast: ACTION_METADATA_CHANGED -> EXTRA_STATION");

        // update media session metadata
        mSession.setMetadata(getSessionMetadata(getApplicationContext(), mStation));

        // update notification
        NotificationHelper.update(PlayerService.this, mStation, mSession);
    }


    /* Saves state of playback */
    private void saveAppState() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplication());
        SharedPreferences.Editor editor = settings.edit();
        if (mStation == null) {
            editor.putString(PREF_STATION_URL, null);
        } else if (mStation.getPlaybackState() == PLAYBACK_STATE_STOPPED) {
            editor.putString(PREF_STATION_URL, null);
            editor.putString(PREF_STATION_URL_LAST, mStation.getStreamUri().toString());
        } else {
            editor.putString(PREF_STATION_URL, mStation.getStreamUri().toString());
            editor.putString(PREF_STATION_URL_LAST, mStation.getStreamUri().toString());
        }
        editor.apply();
        LogHelper.v(LOG_TAG, "Saving state.");
    }


    /* Creates a DataSourceFactor that supports http redirects */
    public static DefaultDataSourceFactory createDataSourceFactory(Context context, String userAgent, TransferListener listener) {
        // Credit: https://stackoverflow.com/questions/41517440/exoplayer2-how-can-i-make-a-http-301-redirect-work
        // Default parameters, except allowCrossProtocolRedirects is true
        DefaultHttpDataSourceFactory httpDataSourceFactory = new DefaultHttpDataSourceFactory(
                userAgent,
                listener,
                DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                true /* allowCrossProtocolRedirects */
        );
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(
                context,
                listener,
                httpDataSourceFactory
        );
        return dataSourceFactory;
    }


//    /* Loads app state from preferences */
//    private void loadAppState(Context context) {
//        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
//        mStationIdCurrent = settings.getInt(PREF_STATION_ID_CURRENT, -1);
//        mStationIdLast = settings.getInt(PREF_STATION_ID_LAST, -1);
//        LogHelper.v(LOG_TAG, "Loading state ("+  mStationIdCurrent + " / " + mStationIdLast + ")");
//    }


    /* Creates a http connection from given url */ // todo this method is also in Station.java - move it to a helper
    private HttpURLConnection createConnection(URL fileLocation, int redirectCount) {
        HttpURLConnection connection = null;

        try {
            // try to open connection
            LogHelper.i(LOG_TAG, "Opening http connection.");
            connection = (HttpURLConnection)fileLocation.openConnection();

            // check for redirects
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {

                    // handle redirects
                    LogHelper.i(LOG_TAG, "Following a redirect.");
                    // get redirect url from "location" header field
                    String redirectUrl = connection.getHeaderField("Location");
                    connection.disconnect();
                    if (redirectCount < 5) {
                        // create new connection with redirect url
                        connection = createConnection(new URL(redirectUrl), redirectCount + 1);
                    } else {
                        connection = null;
                        LogHelper.e(LOG_TAG, "Too many redirects.");
                    }

                }
            }

        } catch (IOException e) {
            LogHelper.e(LOG_TAG, "Unable to open http connection.");
            e.printStackTrace();
        }

        return connection;
    }




    /**
     * Inner class: Handles callback from active media session ***
     */
    private final class MediaSessionCallback extends MediaSessionCompat.Callback  {
        @Override
        public void onPlay() {
            // start playback
            if (mStation != null) {
                startPlayback();
            }
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            MediaMetadataCompat stationMediaMetadata = mStationListProvider.getStationMediaMetadata(mediaId);

            // re-construct station from stationMediaMetadata
            mStation = new Station(stationMediaMetadata);
            startPlayback();
        }

        @Override
        public void onPause() {
            // stop playback and keep notification
            stopPlayback(false);
        }

        @Override
        public void onStop() {
            // stop playback and remove notification
            stopPlayback(true);
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            // handle requests to begin playback from a search query (eg. Assistant, Android Auto, etc.)
            LogHelper.i(LOG_TAG, "playFromSearch  query=" + query + " extras="+ extras);

            if (TextUtils.isEmpty(query)) {
                // user provided generic string e.g. 'Play music'
                mStation = new Station(mStationListProvider.getFirstStation());
            } else {
                // try to match station name and voice query
                for (MediaMetadataCompat stationMetadata : mStationListProvider.getAllStations()) {
                    String[] words = query.split(" ");
                    for (String word : words) {
                        if (stationMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE).toLowerCase().contains(word.toLowerCase())) {
                            mStation = new Station(stationMetadata);
                        }
                    }
                }
            }

            // start playback
            startPlayback();
        }

        @Override
        public void onSkipToNext() {
            LogHelper.d(LOG_TAG, "onSkipToNext");
            super.onSkipToNext();
            MediaMetadataCompat station = null;
            if (mStation != null) {
                station = mStationListProvider.getStationAfter(mStation.getStationId());
            }
            if (station == null) {
                station = mStationListProvider.getFirstStation();
            }
            if (station != null && !mStationListProvider.isEmpty()) {
                mStation = new Station(station);
                startPlayback();
            }
        }

        @Override
        public void onSkipToPrevious() {
            LogHelper.d(LOG_TAG, "onSkipToPrevious");
            super.onSkipToPrevious();
            MediaMetadataCompat station = null;
            if (mStation != null) {
                station = mStationListProvider.getStationBefore(mStation.getStationId());
            }
            if (station == null) {
                station = mStationListProvider.getLastStation();
            }
            if (station != null && !mStationListProvider.isEmpty()) {
                mStation = new Station(station);
                startPlayback();
            }
        }

    }
    /**
     * End of inner class
     */


    /**
     * Inner class: Receiver for headphone unplug-signal
     */
    public class HeadphoneUnplugReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mStation.getPlaybackState() != PLAYBACK_STATE_STOPPED && AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                LogHelper.v(LOG_TAG, "Headphones unplugged. Stopping playback.");
                // stop playback
                mController.getTransportControls().pause();
                // notify user
                Toast.makeText(context, context.getString(R.string.toastalert_headphones_unplugged), Toast.LENGTH_LONG).show();
            }
        }
    }
    /**
     * End of inner class
     */


    /**
     * Inner class: Checks for HTTP Live Streaming (HLS) before playing
     */
    private class InitializePlayerHelper extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... voids) {
            mPlayerInitLock = true;

            String contentType = "";

            try {
                HttpURLConnection connection = createConnection(new URL(mStation.getStreamUri().toString()), 0 );
                contentType = connection.getContentType();
                connection.disconnect();
                if (contentType == null) {
                    LogHelper.e(LOG_TAG, "Connection Error. Connection is NULL");
                    return CONNECTION_TYPE_ERROR;
                }

                LogHelper.v(LOG_TAG, "MIME type of stream: " + contentType);
                // strip encoding part after semicolon if necessary
                if (contentType.contains(";")) {
                    contentType = contentType.substring(0, contentType.indexOf(";"));
                }

                if (Arrays.asList(CONTENT_TYPES_HLS).contains(contentType) || Arrays.asList(CONTENT_TYPES_M3U).contains(contentType) ) {
                    LogHelper.v(LOG_TAG, "HTTP Live Streaming detected.");
                    return CONNECTION_TYPE_HLS;
                } else if (Arrays.asList(CONTENT_TYPES_MPEG).contains(contentType) || Arrays.asList(CONTENT_TYPES_AAC).contains(contentType)  || Arrays.asList(CONTENT_TYPES_OGG).contains(contentType) ) {
                    LogHelper.v(LOG_TAG, "Other Streaming protocol detected (MPEG, AAC, OGG).");
                    return CONNECTION_TYPE_OTHER;
                } else {
                    LogHelper.e(LOG_TAG, "Connection Error. Connection is " + contentType);
                    return CONNECTION_TYPE_ERROR;
                }
            } catch (Exception e) {
                LogHelper.e(LOG_TAG, "Connection Error. Details: " + e);
                return CONNECTION_TYPE_ERROR;
            }
        }

        @Override
        protected void onPostExecute(Integer connectionType) {
            if (connectionType == CONNECTION_TYPE_ERROR) {
                Toast.makeText(PlayerService.this, getString(R.string.toastalert_unable_to_connect), Toast.LENGTH_LONG).show();
                stopPlayback(false);
            } else if (mStation.getPlaybackState() != PLAYBACK_STATE_STOPPED) {
                // prepare player
                preparePlayer(connectionType);

                // add listener
                mPlayer.addListener(PlayerService.this);

                // set content type
                mPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.CONTENT_TYPE_MUSIC)
                        .build()
                );
            }

            // release init lock
            mPlayerInitLock = false;

        }

    }
    /**
     * End of inner class
     */

}
