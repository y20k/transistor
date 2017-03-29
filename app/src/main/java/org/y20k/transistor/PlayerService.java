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

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataRenderer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;

import org.y20k.transistor.core.Station;
import org.y20k.transistor.helpers.LogHelper;
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
public final class PlayerService extends MediaBrowserServiceCompat implements TransistorKeys, AudioManager.OnAudioFocusChangeListener, ExoPlayer.EventListener, MetadataRenderer.Output {

    /* Define log tag */
    private static final String LOG_TAG = PlayerService.class.getSimpleName();


    /* Main class variables */
    private static Station mStation;
//    private MetadataHelper mMetadataHelper;
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
    private HeadphoneUnplugReceiver mHeadphoneUnplugReceiver;
    private WifiManager.WifiLock mWifiLock;
    private PowerManager.WakeLock mWakeLock;

    private DataSource.Factory mDataSourceFactory;
    private ExtractorsFactory mExtractorsFactory;
    private SimpleExoPlayer mExoPlayer;
    private String mUserAgent;


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

        mStationMetadataReceived = false;
        mSession = createMediaSession(this);

        mUserAgent = Util.getUserAgent(this, APPLICATION_NAME);

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
        createExoPlayer();

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
            if (mPlayback) {
                mController.getTransportControls().stop();
            }

            // dismiss notification
            NotificationHelper.stop();
            // set media session in-active
            mSession.setActive(false);
        }

        // listen for media button
        MediaButtonReceiver.handleIntent(mSession, intent);

        // default return value for media playback
        return START_STICKY;
    }



    @Override
    public void onMetadata(Metadata metadata) {
        LogHelper.v(LOG_TAG, "Got new metadata: " + metadata.toString());
    }
//
//    @Override
//    public void onMetadata(List<Id3Frame> id3Frames) {
//        for (Id3Frame id3Frame : id3Frames) {
//            if (id3Frame instanceof TxxxFrame) {
//                TxxxFrame txxxFrame = (TxxxFrame) id3Frame;
//                Log.i(LOG_TAG, String.format("ID3 TimedMetadata %s: description=%s, value=%s", txxxFrame.id,
//                        txxxFrame.description, txxxFrame.value));
//            } else if (id3Frame instanceof PrivFrame) {
//                PrivFrame privFrame = (PrivFrame) id3Frame;
//                Log.i(LOG_TAG, String.format("ID3 TimedMetadata %s: owner=%s", privFrame.id, privFrame.owner));
//            } else if (id3Frame instanceof GeobFrame) {
//                GeobFrame geobFrame = (GeobFrame) id3Frame;
//                Log.i(LOG_TAG, String.format("ID3 TimedMetadata %s: mimeType=%s, filename=%s, description=%s",
//                        geobFrame.id, geobFrame.mimeType, geobFrame.filename, geobFrame.description));
//            } else if (id3Frame instanceof ApicFrame) {
//                ApicFrame apicFrame = (ApicFrame) id3Frame;
//                Log.i(LOG_TAG, String.format("ID3 TimedMetadata %s: mimeType=%s, description=%s",
//                        apicFrame.id, apicFrame.mimeType, apicFrame.description));
//            } else if (id3Frame instanceof TextInformationFrame) {
//                TextInformationFrame textInformationFrame = (TextInformationFrame) id3Frame;
//                Log.i(LOG_TAG, String.format("ID3 TimedMetadata %s: description=%s", textInformationFrame.id,
//                        textInformationFrame.description));
//            } else {
//                Log.i(LOG_TAG, String.format("ID3 TimedMetadata %s", id3Frame.id));
//            }
//        }
//    }


    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

        switch (playbackState) {
            case STATE_BUFFERING:
                // The player is not able to immediately play from the current position.
                LogHelper.v(LOG_TAG, "State of ExoPlayer has changed: BUFFERING");
                mStationLoading = true;
                if (mPlayback) {
                    // set loading state
                    mStationLoading = true;
                    // send local broadcast: buffering
                    Intent intent = new Intent();
                    intent.setAction(ACTION_PLAYBACK_STATE_CHANGED);
                    intent.putExtra(EXTRA_PLAYBACK_STATE_CHANGE, PLAYBACK_LOADING_STATION);
                    intent.putExtra(EXTRA_STATION, mStation);
                    intent.putExtra(EXTRA_STATION_ID, mStationID);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                    // update notification
                    NotificationHelper.update(mStation, mStationID, this.getString(R.string.descr_station_stream_loading), mSession);
                }
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

                if (mPlayback && mStationLoading) {
                    // set loading state
                    mStationLoading = false;
                    // send local broadcast: buffering finished - playback started
                    Intent intent = new Intent();
                    intent.setAction(ACTION_PLAYBACK_STATE_CHANGED);
                    intent.putExtra(EXTRA_PLAYBACK_STATE_CHANGE, PLAYBACK_STARTED);
                    intent.putExtra(EXTRA_STATION, mStation);
                    intent.putExtra(EXTRA_STATION_ID, mStationID);
                    LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(intent);
                }

                // check for race between onPlayerStateChanged and MetadataHelper
                if (!mStationMetadataReceived) {
                    // update notification
                    NotificationHelper.update(mStation, mStationID, mStation.getStationName(), mSession);
                }

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
                LogHelper.e(LOG_TAG, "An error occurred. Type RENDERER: " + error.getRendererException().toString());
                break;

            case TYPE_SOURCE:
                // error occurred loading data from a MediaSource. Playback state: ExoPlayer.STATE_IDLE
                LogHelper.e(LOG_TAG, "An error occurred. Type SOURCE: " + error.getSourceException().toString());
//                if (mPlayback) {
//                    Intent intent = new Intent();
//                    intent.setAction(ACTION_PLAYBACK_STATE_CHANGED);
//                    intent.putExtra(EXTRA_PLAYBACK_STATE_CHANGE, PLAYBACK_LOADING_STATION);
//                    intent.putExtra(EXTRA_STATION, mStation);
//                    intent.putExtra(EXTRA_STATION_ID, mStationID);
//                    LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(intent);
//                    mStationLoading = true;
//
//                    // wait a sec - and then try to restart playback
//                    try {
//                        Thread.sleep(1000);
//                        startPlayback();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
                break;

            case TYPE_UNEXPECTED:
                // error was an unexpected RuntimeException. Playback state: ExoPlayer.STATE_IDLE
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
                if (mPlayback && mExoPlayer.getPlayWhenReady()) {
                    stopPlayback();
                }
                break;
            // transient loss of audio focus
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (!mPlayback && mExoPlayer != null && mExoPlayer.getPlayWhenReady()) {
                    stopPlayback();
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
        if (mExoPlayer != null) {
            releaseExoPlayer();
        }

        // cancel notification
        stopForeground(true);

        // todo: unregister broadcast receivers
    }


    /* Getter for current station */
    public static Station getStation() {
        return mStation;
    }


    /* Starts playback */
    private void startPlayback() {
        LogHelper.v(LOG_TAG, "Starting playback.");

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

        // stop running mExoPlayer - request focus and initialize media mExoPlayer
        if (mExoPlayer.getPlayWhenReady()) {
            mExoPlayer.setPlayWhenReady(false);
            mExoPlayer.stop();
            NotificationHelper.stop();
        }

        if (mStreamUri != null && requestFocus()) {
            // initialize player and start playback
            initializeExoPlayer();
//            mExoPlayer.setId3Output(this);
            mExoPlayer.setPlayWhenReady(true);

            // update MediaSession
            mSession.setPlaybackState(getPlaybackState());
            mSession.setMetadata(getMetadata(getApplicationContext(), mStation, mStationMetadata));
            mSession.setActive(true);

            // put up notification
            NotificationHelper.show(this, mSession, mStation, mStationID, this.getString(R.string.descr_station_stream_loading));
        }

        // send local broadcast: buffering
        Intent intent = new Intent();
        intent.setAction(ACTION_PLAYBACK_STATE_CHANGED);
        intent.putExtra(EXTRA_PLAYBACK_STATE_CHANGE, PLAYBACK_LOADING_STATION);
        intent.putExtra(EXTRA_STATION, mStation);
        intent.putExtra(EXTRA_STATION_ID, mStationID);
        LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(intent);

        // register headphone listener
        IntentFilter headphoneUnplugIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        mHeadphoneUnplugReceiver = new HeadphoneUnplugReceiver();
        registerReceiver(mHeadphoneUnplugReceiver, headphoneUnplugIntentFilter);
    }


    /* Stops playback */
    private void stopPlayback() {
        LogHelper.v(LOG_TAG, "Stopping playback.");

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


        // stop playback
        mExoPlayer.setPlayWhenReady(false);
        // mExoPlayer.stop();

        // give up audio focus
        giveUpAudioFocus();

        // update playback state
        mSession.setPlaybackState(getPlaybackState());

        // update notification
        NotificationHelper.update(mStation, mStationID, mStation.getStationName(), mSession);
        // keep media session active
        mSession.setActive(true);

        // send local broadcast: playback stopped
        Intent intent = new Intent();
        intent.setAction(ACTION_PLAYBACK_STATE_CHANGED);
        intent.putExtra(EXTRA_PLAYBACK_STATE_CHANGE, PLAYBACK_STOPPED);
        intent.putExtra(EXTRA_STATION, mStation);
        intent.putExtra(EXTRA_STATION_ID, mStationID);
        LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(intent);

        // unregister headphone listener
        try {
            this.unregisterReceiver(mHeadphoneUnplugReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /* Creates an instance of SimpleExoPlayer */
    private void createExoPlayer() {

        if (mExoPlayer != null) {
            releaseExoPlayer();
        }

        // create default TrackSelector
        TrackSelector trackSelector = new DefaultTrackSelector();

        // create default LoadControl - double the buffer
        LoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE * 2));

        // create the player
        mExoPlayer = ExoPlayerFactory.newSimpleInstance(getApplicationContext(), trackSelector, loadControl);
    }


    private void prepareExoPLayer(boolean sourceIsHLS, String uriString) {
        // create listener for DataSource.Factory
        TransferListener transferListener = new TransferListener() {
            @Override
            public void onTransferStart(Object source, DataSpec dataSpec) {
                LogHelper.v(LOG_TAG, "onTransferStart\nSource: " + source.toString() + "\nDataSpec: " + dataSpec.toString());
            }

            @Override
            public void onBytesTransferred(Object source, int bytesTransferred) {

            }

            @Override
            public void onTransferEnd(Object source) {
                LogHelper.v(LOG_TAG, "onTransferEnd\nSource: " + source.toString());
            }
        };
        // produce DataSource instances through which media data is loaded
        DataSource.Factory dataSourceFactory = new DefaultHttpDataSourceFactory(mUserAgent, transferListener);
        // create MediaSource
        MediaSource mediaSource;
        if (sourceIsHLS) {
            mediaSource = new HlsMediaSource(Uri.parse(uriString),
                    dataSourceFactory, 32, null, null);
        } else {
            ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
            mediaSource = new ExtractorMediaSource(Uri.parse(mStreamUri),
                    dataSourceFactory, extractorsFactory, 32, null, null, null); // todo attach listener here
        }
        // prepare player with source.
        mExoPlayer.prepare(mediaSource);
    }


    /* Releases the ExoPlayer */
    private void releaseExoPlayer() {
        mExoPlayer.release();
        mExoPlayer = null;
        mExtractorsFactory = null;
        mDataSourceFactory = null;

//        // close metadata helper - if it was in use
//        if (mMetadataHelper != null) {
//            mMetadataHelper.closeShoutcastProxyConnection();
//            mMetadataHelper = null;
//        }
    }



    /* Set up the media mExoPlayer */
    private void initializeExoPlayer() {
        InitializeExoPlayerHelper initializeExoPlayerHelper = new InitializeExoPlayerHelper();
        initializeExoPlayerHelper.execute();
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
                stopPlayback();
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
            // stop playback
            stopPlayback();
        }

        @Override
        public void onStop() {
            // stop playback
            stopPlayback();
        }

    }
    /**
     * End of inner class
     */


    /**
     * Inner class: Checks for HTTP Live Streaming (HLS) before playing
     */
    private class InitializeExoPlayerHelper extends AsyncTask<Void, Void, Boolean> {

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
            String uriString = mStreamUri;
//            if (sourceIsHLS) {
//                // stream is HLS - do not extract metadata
//                uriString = mStreamUri;
//            } else {
//                // normal stream - extract metadata
//                mMetadataHelper = new MetadataHelper(getApplicationContext(), mStation);
//                uriString = mMetadataHelper.getShoutcastProxy();
//            }

            // prepare player
            prepareExoPLayer(sourceIsHLS, uriString);
            // add listener
            mExoPlayer.addListener(PlayerService.this);
        }

    }
    /**
     * End of inner class
     */

}
