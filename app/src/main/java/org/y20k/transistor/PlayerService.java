/**
 * PlayerService.java
 * Implements the app's playback background service
 * The mExoPlayer service plays streaming audio
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-17 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;

import org.y20k.transistor.core.Station;
import org.y20k.transistor.helpers.LogHelper;
import org.y20k.transistor.helpers.MetadataHelper;
import org.y20k.transistor.helpers.NotificationHelper;
import org.y20k.transistor.helpers.TransistorKeys;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import static com.google.android.exoplayer2.ExoPlaybackException.TYPE_RENDERER;
import static com.google.android.exoplayer2.ExoPlaybackException.TYPE_SOURCE;
import static com.google.android.exoplayer2.ExoPlaybackException.TYPE_UNEXPECTED;
import static com.google.android.exoplayer2.ExoPlayer.STATE_BUFFERING;
import static com.google.android.exoplayer2.ExoPlayer.STATE_ENDED;
import static com.google.android.exoplayer2.ExoPlayer.STATE_IDLE;
import static com.google.android.exoplayer2.ExoPlayer.STATE_READY;


/**
 * PlayerService class
 */
public final class PlayerService extends MediaBrowserServiceCompat implements TransistorKeys, AudioManager.OnAudioFocusChangeListener, ExoPlayer.EventListener {

    /* Define log tag */
    private static final String LOG_TAG = PlayerService.class.getSimpleName();


    /* Main class variables */
    private static Station mStation;
    private MetadataHelper mMetadataHelper;
    private AudioManager mAudioManager;
    private static MediaSessionCompat mSession;
    private static MediaControllerCompat mController;
    private int mStationID;
    private int mStationIDCurrent;
    private int mStationIDLast;
    private String mStationMetadata;
    private String mStreamUri;
    private boolean mPlayback;
    private boolean mStationLoading;
    private boolean mStationMetadataReceived;
    private int mPlayerInstanceCounter;
    private HeadphoneUnplugReceiver mHeadphoneUnplugReceiver;
    private int mReconnectCounter;
    private WifiManager.WifiLock mWifiLock;
    private PowerManager.WakeLock mWakeLock;

    private DataSource.Factory mDataSourceFactory;
    private ExtractorsFactory mExtractorsFactory;
    private SimpleExoPlayer mExoPlayer;


    /* Constructor (default) */
    public PlayerService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // load app state
        loadAppState(getApplication());

        // set up variables
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mPlayerInstanceCounter = 0;
        mReconnectCounter = 0;
        mStationMetadataReceived = false;
        mSession = createMediaSession(this);

        // create Wifi and wake locks
        mWifiLock = ((WifiManager) this.getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "Transistor_wifi_lock");
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Transistor_wake_lock");

        // create media controller
        try {
            mController = new MediaControllerCompat(getApplicationContext(), mSession.getSessionToken());
        } catch (RemoteException e) {
            LogHelper.e(LOG_TAG, "RemoteException: " + e);
            e.printStackTrace();
        }

        // get instance of mExoPlayer
        mExoPlayer = createExoPlayer();

        // RECEIVER: station metadata has changed
        BroadcastReceiver metadataChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra(EXTRA_METADATA) && intent.hasExtra(EXTRA_STATION)) {

                    Station station = intent.getParcelableExtra(EXTRA_STATION);
                    mStationMetadata = intent.getStringExtra(EXTRA_METADATA);
                    saveAppState();

                    if (!mStationMetadataReceived && station.equals(mStation)) {
                        // race between onPrepared and MetadataHelper has been won by the latter
                        mStationMetadataReceived = true;
                    }

                    // update media session metadata
                    mSession.setMetadata(getMetadata(context, station, mStationMetadata));

                    // update notification
                    NotificationHelper.update(mStation, mStationID, mStationMetadata, mSession);
                }
            }
        };
        IntentFilter metadataChangedIntentFilter = new IntentFilter(ACTION_METADATA_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(metadataChangedReceiver, metadataChangedIntentFilter);

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // checking for empty intent
        if (intent == null) {
            LogHelper.v(LOG_TAG, "Null-Intent received. Stopping self.");
            stopForeground(true); // Remove notification
            stopSelf();
        }

        // ACTION PLAY
        else if (intent.getAction().equals(ACTION_PLAY)) {
            LogHelper.v(LOG_TAG, "Service received command: PLAY");

            // get URL of station from intent
            if (intent.hasExtra(EXTRA_STATION)) {
                mStation = intent.getParcelableExtra(EXTRA_STATION);
                mStationID = intent.getIntExtra(EXTRA_STATION_ID, 0);
                mStreamUri = mStation.getStreamUri().toString();
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
            mController.getTransportControls().stop();
        }

        // listen for media button
        MediaButtonReceiver.handleIntent(mSession, intent);

        // default return value for media playback
        return START_STICKY;
    }



    /* NEW EXO PLAYER EVENT LISTENER METHODS*/

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

        switch (playbackState) {
            case STATE_BUFFERING:
                // The player is not able to immediately play from the current position.
                LogHelper.v(LOG_TAG, "State of ExoPlayer has changed: BUFFERING");
                break;

            case STATE_ENDED:
                // The player has finished playing the media.
                LogHelper.v(LOG_TAG, "State of ExoPlayer has changed: ENDED");
                break;

            case STATE_IDLE:
                // The player does not have a source to play, so it is neither buffering nor ready to play.
                LogHelper.v(LOG_TAG, "State of ExoPlayer has changed: IDLE");
                break;

            case STATE_READY:
                // The player is able to immediately play from the current position.
                LogHelper.v(LOG_TAG, "State of ExoPlayer has changed: READY");

                // send local broadcast: buffering finished - playback started
                Intent intent = new Intent();
                intent.setAction(ACTION_PLAYBACK_STATE_CHANGED);
                intent.putExtra(EXTRA_PLAYBACK_STATE_CHANGE, PLAYBACK_STARTED);
                intent.putExtra(EXTRA_STATION, mStation);
                intent.putExtra(EXTRA_STATION_ID, mStationID);
                LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(intent);
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
                // error occurred in a Renderer. Playback state: ExoPlayer.STATE_IDLE
                mExoPlayer.release();
                mExoPlayer = createExoPlayer();
                stopPlayback(true);
                LogHelper.w(LOG_TAG, "An error occurred. Type RENDERER: " + error.getRendererException().toString());
                break;
            case TYPE_SOURCE:
                // error occurred loading data from a MediaSource. Playback state: ExoPlayer.STATE_IDLE
                if (mPlayback) {
                    stopPlayback(true);
                }
                LogHelper.w(LOG_TAG, "An error occurred. Type SOURCE: " + error.getSourceException().toString());
                break;
            case TYPE_UNEXPECTED:
                // error was an unexpected RuntimeException. Playback state: ExoPlayer.STATE_IDLE
                mExoPlayer.release();
                mExoPlayer = createExoPlayer();
                stopPlayback(true);
                LogHelper.w(LOG_TAG, "An error occurred. Type UNEXPECTED: " + error.getUnexpectedException().toString());
                break;
            default:
                mExoPlayer.release();
                mExoPlayer = createExoPlayer();
                stopPlayback(true);
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
            state = "Media source is currently not being loaded.";
        }
        LogHelper.v(LOG_TAG, "State of loading has changed: " + state);
    }


    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }


    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }


    @Override
    public void onPositionDiscontinuity() {

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot(getString(R.string.app_name), null);

    }

    @Override
    public void onLoadChildren(@NonNull String rootId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(null);
    }


    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            // gain of audio focus of unknown duration
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mPlayback) {
                    if (mExoPlayer == null) {
                        initializeExoPlayer();
                        mExoPlayer.setPlayWhenReady(true);
                    } else if (mExoPlayer.getPlayWhenReady()) {
                        mExoPlayer.setPlayWhenReady(true);
                    }
                    mExoPlayer.setVolume(1.0f);
                }
                break;
            // loss of audio focus of unknown duration
            case AudioManager.AUDIOFOCUS_LOSS:
                if (mPlayback) {
                    stopPlayback(false);
                }
                break;
            // transient loss of audio focus
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (!mPlayback && mExoPlayer != null && mExoPlayer.getPlayWhenReady()) {
                    stopPlayback(false);
                }
                else if (mPlayback && mExoPlayer != null && mExoPlayer.getPlayWhenReady()) {
                    mExoPlayer.setPlayWhenReady(false);
                }
                break;
            // temporary external request of audio focus
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mExoPlayer != null && mExoPlayer.getPlayWhenReady()){
                    mExoPlayer.setVolume(0.1f);
                }
                break;
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        LogHelper.v(LOG_TAG, "onDestroy called.");

        // save state
        mPlayback = false;
        saveAppState();

        // release media session
        if (mSession != null) {
            mSession.setActive(false);
            mSession.release();
        }

        // release ExoPlayer
        mExoPlayer.release(); // release only when service gets destroyed

        // cancel notification
        stopForeground(true);
    }


    /* Getter for current station */
    public static Station getStation() {
        return mStation;
    }


    /* Starts playback */
    private void startPlayback() {

        // set and save state
        mStationMetadata = mStation.getStationName();
        mStationMetadataReceived = false;
        mStation.setPlaybackState(true);
        mPlayback = true;
        mStationLoading = true;
        mStationIDLast = mStationIDCurrent;
        mStationIDCurrent = mStationID;
        saveAppState();

        // acquire Wifi and wake locks
        if (!mWifiLock.isHeld()) {
            mWifiLock.acquire();
        }
        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire(); // needs android.permission.WAKE_LOCK
        }

        // send local broadcast: buffering
        Intent intent = new Intent();
        intent.setAction(ACTION_PLAYBACK_STATE_CHANGED);
        intent.putExtra(EXTRA_PLAYBACK_STATE_CHANGE, PLAYBACK_LOADING_STATION);
        intent.putExtra(EXTRA_STATION, mStation);
        intent.putExtra(EXTRA_STATION_ID, mStationID);
        LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(intent);

        // increase counter
        mPlayerInstanceCounter++;

        // stop running mExoPlayer - request focus and initialize media mExoPlayer
        if (mExoPlayer.getPlayWhenReady()) {
            mExoPlayer.setPlayWhenReady(false);
            mExoPlayer.stop();
            NotificationHelper.stop();
        }

        if (mStreamUri != null && requestFocus()) {
            // initialize player and start playback
            initializeExoPlayer();
            mExoPlayer.setPlayWhenReady(true);

            // update MediaSession
            mSession.setPlaybackState(getPlaybackState());
            mSession.setMetadata(getMetadata(getApplicationContext(), mStation, mStationMetadata));
            mSession.setActive(true);

            // put up notification
            NotificationHelper.show(this, mSession, mStation, mStationID, this.getString(R.string.descr_station_stream_loading));
        }

        // register headphone listener
        IntentFilter headphoneUnplugIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        mHeadphoneUnplugReceiver = new HeadphoneUnplugReceiver();
        registerReceiver(mHeadphoneUnplugReceiver, headphoneUnplugIntentFilter);
    }


    /* Stops playback */
    private void stopPlayback(boolean dismissNotification) {

        // set and save state
        mStationMetadata = mStation.getStationName();
        mStationMetadataReceived = false;
        mStation.setPlaybackState(false);
        mPlayback = false;
        mStationLoading = false;
        mStationIDLast = mStationID;
        mStationIDCurrent = -1;
        saveAppState();

        // release Wifi and wake locks
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }

        // send local broadcast: playback stopped
        Intent intent = new Intent();
        intent.setAction(ACTION_PLAYBACK_STATE_CHANGED);
        intent.putExtra(EXTRA_PLAYBACK_STATE_CHANGE, PLAYBACK_STOPPED);
        intent.putExtra(EXTRA_STATION, mStation);
        intent.putExtra(EXTRA_STATION_ID, mStationID);
        LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(intent);

        // reset counter
        mPlayerInstanceCounter = 0;

        // stop playback
        mExoPlayer.setPlayWhenReady(false);
        mExoPlayer.stop();

        // give up audio focus
        giveUpAudioFocus();

        // update playback state
        mSession.setPlaybackState(getPlaybackState());

        if (dismissNotification) {
            // dismiss notification
            NotificationHelper.stop();
            // set media session in-active
            mSession.setActive(false);
        } else {
            // update notification
            NotificationHelper.update(mStation, mStationID, mStation.getStationName(), mSession);
            // keep media session active
            mSession.setActive(true);
        }

        // close metadata helper
        if (mMetadataHelper != null) {
            mMetadataHelper.closeShoutcastProxyConnection();
            mMetadataHelper = null;
        }

        // unregister headphone listener
        try {
            this.unregisterReceiver(mHeadphoneUnplugReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /* Creates an instance of SimpleExoPlayer */
    private SimpleExoPlayer createExoPlayer() {

        // https://github.com/yusufcakmak/ExoPlayerSample/blob/master/app/src/main/java/com/yusufcakmak/exoplayersample/RadioPlayerActivity.java

        // create default ExoPlayer modules
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelector trackSelector = new DefaultTrackSelector();
        LoadControl loadControl = new DefaultLoadControl();
        mExtractorsFactory = new DefaultExtractorsFactory();
        mDataSourceFactory = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, APPLICATION_NAME),
                (TransferListener<? super DataSource>) bandwidthMeter);

        // return instance of SimpleExoPlayer
        return ExoPlayerFactory.newSimpleInstance(getApplicationContext(), trackSelector, loadControl);
    }



    /* Set up the media mExoPlayer */
    private void initializeExoPlayer() {
        InitializeMediaPlayerHelper initializeMediaPlayerHelper = new InitializeMediaPlayerHelper();
        initializeMediaPlayerHelper.execute();
    }


    /* Request audio manager focus */
    private boolean requestFocus() {
        int result = mAudioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }


    /* Give up audio focus */
    private boolean giveUpAudioFocus() {
        int result = mAudioManager.abandonAudioFocus(this);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }


    /* Creates media session */
    private MediaSessionCompat createMediaSession(Context context) {
        // create a media session
        MediaSessionCompat session = new MediaSessionCompat(context, LOG_TAG);
        session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setPlaybackState(getPlaybackState());
        session.setCallback(new MediaSessionCallback());
        if (mStation != null) {
            session.setMetadata(getMetadata(context, mStation, null));
        }
        setSessionToken(session.getSessionToken());

        return session;
    }


    /* Creates playback state depending on mPlayback */
    private PlaybackStateCompat getPlaybackState() {

        if (mPlayback) {
            // define action for playback state to be used in media session callback
            return new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, 0, 0)
                    .setActions(PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_PAUSE)
                    .build();
        } else {
            // define action for playback state to be used in media session callback
            return new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_STOPPED, 0, 0)
                    .setActions(PlaybackStateCompat.ACTION_PLAY)
                    .build();
        }
    }


    /* Creates the metadata needed for MediaSession */
    private MediaMetadataCompat getMetadata(Context context, Station station, String metaData) {
        Bitmap stationImage;
        if (station.getStationImageFile() != null && station.getStationImageFile().exists()) {
            // use station image
            stationImage = BitmapFactory.decodeFile(station.getStationImageFile().toString());
        } else {
            stationImage = null;
        }
        // use name of app as album title
        String albumTitle = context.getResources().getString(R.string.app_name);

        // log metadata change
        LogHelper.i(LOG_TAG, "New Metadata available. Artist: " + station.getStationName() + ", Title: " +  metaData + ", Album: " +  albumTitle);

        return new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, station.getStationName())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, metaData)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, albumTitle)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, stationImage)
                .build();
    }


    /* Saves state of playback */
    private void saveAppState() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplication());
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(PREF_STATION_ID_CURRENTLY_PLAYING, mStationIDCurrent);
        editor.putInt(PREF_STATION_ID_LAST, mStationIDLast);
        editor.putBoolean(PREF_PLAYBACK, mPlayback);
        editor.putBoolean(PREF_STATION_LOADING, mStationLoading);
        editor.putString(PREF_STATION_METADATA, mStationMetadata);
        editor.apply();
        LogHelper.v(LOG_TAG, "Saving state ("+  mStationIDCurrent + " / " + mStationIDLast + " / " + mPlayback + " / " + mStationLoading + " / " + ")");
    }


    /* Loads app state from preferences */
    private void loadAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        mStationIDCurrent = settings.getInt(PREF_STATION_ID_CURRENTLY_PLAYING, -1);
        mStationIDLast = settings.getInt(PREF_STATION_ID_LAST, -1);
        LogHelper.v(LOG_TAG, "Loading state ("+  mStationIDCurrent + " / " + mStationIDLast + ")");
    }

    /**
     * Inner class: Receiver for headphone unplug-signal
     */
    public class HeadphoneUnplugReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mPlayback && AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                LogHelper.v(LOG_TAG, "Headphones unplugged. Stopping playback.");
                // stop playback
                stopPlayback(false);
                // notify user
                Toast.makeText(context, context.getString(R.string.toastalert_headphones_unplugged), Toast.LENGTH_LONG).show();
            }
        }
    }
    /**
     * End of inner class
     */


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
        public void onPause() {
            // stop playback on pause signal from Android Wear or headphone button
            stopPlayback(false);
        }

        @Override
        public void onStop() {
            // stop playback and dismiss notification on stop signal
            stopPlayback(true);
        }

    }
    /**
     * End of inner class
     */


    /**
     * Inner class: Checks for HTTP Live Streaming (HLS) before playing
     */
    private class InitializeMediaPlayerHelper extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            String contentType = "";
            URLConnection connection = null;
            try {
                connection = new URL(mStreamUri).openConnection();
                connection.connect();
                contentType = connection.getContentType();
                LogHelper.v(LOG_TAG, "MIME type of stream: " + contentType);
                if (contentType.contains("application/vnd.apple.mpegurl") || contentType.contains("application/x-mpegurl")) {
                    LogHelper.v(LOG_TAG, "HTTP Live Streaming detected.");
                    return true;
                } else {
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean sourceIsHLS) {

            // get a stream uri string
            String uriString;
            if (sourceIsHLS) {
                // stream is HLS - do not extract metadata
                uriString = mStreamUri;
            } else {
                // normal stream - extract metadata
                mMetadataHelper = new MetadataHelper(getApplicationContext(), mStation);
                uriString = mMetadataHelper.getShoutcastProxy();
            }

            // create media source using stream uri string
            MediaSource mediaSource = new ExtractorMediaSource(Uri.parse(uriString),
                    mDataSourceFactory, mExtractorsFactory, null, null);

            // prepare mExoPlayer
            if (mExoPlayer == null) {
                mExoPlayer = createExoPlayer();
            }
            mExoPlayer.prepare(mediaSource);
            mExoPlayer.addListener(PlayerService.this);
        }

    }
    /**
     * End of inner class
     */


//    @Override
//    public void onCompletion(MediaPlayer mp) {
//        LogHelper.w(LOG_TAG, "Resuming playback after completion / signal loss. Player instance count: " + mPlayerInstanceCounter);
//        mp.reset();
//        mPlayerInstanceCounter++;
//        initializeMediaPlayer();
//    }


//    @Override
//    public void onPrepared(MediaPlayer mp) {
//
//        if (mPlayerInstanceCounter == 1) {
//            LogHelper.v(LOG_TAG, "Preparation finished. Starting playback. Player instance count: " + mPlayerInstanceCounter);
//            LogHelper.v(LOG_TAG, "Playback: " + mStreamUri);
//
//            // check for race between onPrepared ans MetadataHelper
//            if (!mStationMetadataReceived) {
//                // update notification
//                NotificationHelper.update(mStation, mStationID, mStation.getStationName(), mSession);
//            }
//
//            // start media mExoPlayer
//            mp.start();
//
//            // send local broadcast: buffering finished
//            Intent i = new Intent();
//            i.setAction(ACTION_PLAYBACK_STATE_CHANGED);
//            i.putExtra(EXTRA_PLAYBACK_STATE_CHANGE, PLAYBACK_STARTED);
//            i.putExtra(EXTRA_STATION, mStation);
//            i.putExtra(EXTRA_STATION_ID, mStationID);
//            LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(i);
//
//            // save state
//            mStationLoading = false;
//            saveAppState();
//
//            // decrease counter
//            mPlayerInstanceCounter--;
//
//            // reset reconnect counter
//            mReconnectCounter = 0;
//
//        } else {
//            LogHelper.v(LOG_TAG, "Stopping and re-initializing media mExoPlayer. Player instance count: " + mPlayerInstanceCounter);
//
//            // release media mExoPlayer
//            releaseMediaPlayer();
//
//            // decrease counter
//            mPlayerInstanceCounter--;
//
//            // re-initializing media mExoPlayer
//            if (mPlayerInstanceCounter >= 0) {
//                initializeMediaPlayer();
//            }
//        }
//
//    }
//
//
//    @Override
//    public boolean onError(MediaPlayer mp, int what, int extra) {
//        // try to reconnect to stream - limited to ten attempts
//        if (mReconnectCounter < 10) {
//            mReconnectCounter++;
//            LogHelper.e(LOG_TAG, "Trying to reconnect after media playback error - attempt #" + mReconnectCounter + ".");
//            startPlayback();
//        }
//
//        return true;
//    }



}
