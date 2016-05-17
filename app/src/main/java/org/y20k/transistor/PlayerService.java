/**
 * PlayerService.java
 * Implements the app's playback background service
 * The player service plays streaming audio
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-16 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
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
import android.util.Log;
import android.widget.Toast;

import org.y20k.transistor.core.Collection;
import org.y20k.transistor.helpers.MetadataHelper;
import org.y20k.transistor.helpers.NotificationHelper;
import org.y20k.transistor.helpers.StorageHelper;
import org.y20k.transistor.helpers.TransistorKeys;

import java.io.IOException;
import java.util.List;


/**
 * PlayerService class
 */
public final class PlayerService extends MediaBrowserServiceCompat implements
        AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnInfoListener {

    /* Define log tag */
    private static final String LOG_TAG = PlayerService.class.getSimpleName();


    /* Main class variables */
    private MetadataHelper mMetadataHelper;
    private AudioManager mAudioManager;
    private MediaPlayer mMediaPlayer;
    private MediaSessionCompat mSession;
    private MediaControllerCompat mController;
    private String mStreamUri;
    private String mStationName;
    private int mStationID;
    private boolean mPlayback;
    private int mPlayerInstanceCounter;
    private HeadphoneUnplugReceiver mHeadphoneUnplugReceiver;
//    private WifiManager.WifiLock mWifiLock;


    /* Constructor (default) */
    public PlayerService() {
    }


    @Override
    public void onCreate() {
        super.onCreate();

        // set up variables
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mMediaPlayer = null;
        mPlayerInstanceCounter = 0;
        if (mSession == null) {
            mSession = createMediaSession(this, null);
        }

        try {
            mController = new MediaControllerCompat(getApplicationContext(), mSession.getSessionToken());
        } catch (RemoteException e) {
            Log.v(LOG_TAG, "RemoteException: " + e);
            e.printStackTrace();
        }

        // Listen for headphone unplug
        IntentFilter headphoneUnplugIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        mHeadphoneUnplugReceiver = new HeadphoneUnplugReceiver();
        registerReceiver(mHeadphoneUnplugReceiver, headphoneUnplugIntentFilter);

        // RECEIVER: station metadata has changed
        BroadcastReceiver metadataChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra(TransistorKeys.EXTRA_METADATA)) {

                    // TODO update media session metadata
                    mSession.setMetadata(getMetadata(context, intent.getStringExtra(TransistorKeys.EXTRA_METADATA)));

                    NotificationHelper.setMediaSession(mSession);
                    NotificationHelper.setStationMetadata(intent.getStringExtra(TransistorKeys.EXTRA_METADATA));
                    NotificationHelper.createNotification(PlayerService.this);


                }
            }
        };
        IntentFilter metadataChangedIntentFilter = new IntentFilter(TransistorKeys.ACTION_METADATA_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(metadataChangedReceiver, metadataChangedIntentFilter);

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // listen for media button
        MediaButtonReceiver.handleIntent(mSession, intent);

        // checking for empty intent
        if (intent == null) {
            Log.v(LOG_TAG, "Null-Intent received. Stopping self.");
            stopSelf();
        }

        // ACTION PLAY
        else if (intent.getAction().equals(TransistorKeys.ACTION_PLAY)) {
            Log.v(LOG_TAG, "Service received command: PLAY");

            // set mPlayback true
            mPlayback = true;

            // get URL of station from intent
            if (intent.hasExtra(TransistorKeys.EXTRA_STREAM_URI)) {
                mStreamUri = intent.getStringExtra(TransistorKeys.EXTRA_STREAM_URI);
            }

            // set media session active and set playback state
            mSession.setPlaybackState(getPlaybackState());
            mSession.setActive(true);

            // update controller
            mController.getTransportControls().play();

            // increase counter
            mPlayerInstanceCounter++;
        }

        // ACTION STOP
        else if (intent.getAction().equals(TransistorKeys.ACTION_STOP)) {
            Log.v(LOG_TAG, "Service received command: STOP");

            // set mPlayback false
            mPlayback = false;

            // set media session in-active and set playback state
            mSession.setPlaybackState(getPlaybackState());
            mSession.setActive(false);

            // update controller
            mController.getTransportControls().stop();

            // reset counter
            mPlayerInstanceCounter = 0;
        }

        // default return value for media playback
        return START_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return null;
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {

    }


    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            // gain of audio focus of unknown duration
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mPlayback) {
                    if (mMediaPlayer == null) {
                        initializeMediaPlayer();
                    } else if (!mMediaPlayer.isPlaying()) {
                        mMediaPlayer.start();
                    }
                    mMediaPlayer.setVolume(1.0f, 1.0f);
                }
                break;
            // loss of audio focus of unknown duration
            case AudioManager.AUDIOFOCUS_LOSS:
                finishPlayback();
                break;
            // transient loss of audio focus
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (!mPlayback && mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    finishPlayback();
                }
                else if (mPlayback && mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                }
                break;
            // temporary external request of audio focus
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    mMediaPlayer.setVolume(0.1f, 0.1f);
                }
                break;
        }
    }


    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.v(LOG_TAG, "Resuming playback after completion / signal loss. Player instance count: " + mPlayerInstanceCounter);
        mMediaPlayer.reset();
        mPlayerInstanceCounter++;
        initializeMediaPlayer();
    }


    @Override
    public void onPrepared(MediaPlayer mp) {

        if (mPlayerInstanceCounter == 1) {
            Log.v(LOG_TAG, "Preparation finished. Starting playback. Player instance count: " + mPlayerInstanceCounter);
            Log.v(LOG_TAG, "Playback: " + mStreamUri);

            // starting media player
            mp.start();

            // decrease counter
            mPlayerInstanceCounter--;

        } else {
            Log.v(LOG_TAG, "Stopping and re-initializing media player. Player instance count: " + mPlayerInstanceCounter);

            // release media player
            releaseMediaPlayer();

            // decrease counter
            mPlayerInstanceCounter--;

            // re-initializing media player
            if (mPlayerInstanceCounter >= 0) {
                initializeMediaPlayer();
            }
        }

    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {

        switch (what) {
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.e(LOG_TAG, "Unknown media playback error");
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.e(LOG_TAG, "Connection to server lost");
                break;
            default:
                Log.e(LOG_TAG, "Generic audio playback error");
                break;
        }

        switch (extra) {
            case MediaPlayer.MEDIA_ERROR_IO:
                Log.e(LOG_TAG, "IO media error.");
                break;
            case MediaPlayer.MEDIA_ERROR_MALFORMED:
                Log.e(LOG_TAG, "Malformed media.");
                break;
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                Log.e(LOG_TAG, "Unsupported content type");
                break;
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                Log.e(LOG_TAG, "Media timeout");
                break;
            default:
                Log.e(LOG_TAG, "Other case of media playback error");
                break;
        }

        mp.reset();

        return true;
    }


    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {

        switch (what){
            case MediaPlayer.MEDIA_INFO_UNKNOWN:
                Log.i(LOG_TAG, "Unknown media info");
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                Log.i(LOG_TAG, "Buffering started");
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                Log.i(LOG_TAG, "Buffering finished");
                break;
            case MediaPlayer.MEDIA_INFO_METADATA_UPDATE: // case never selected
                Log.i(LOG_TAG, "New metadata available");
                break;
            default:
                Log.i(LOG_TAG, "other case of media info");
                break;
        }

        return true;
    }


    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Log.v(LOG_TAG, "Buffering: " + percent);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.v(LOG_TAG, "onDestroy called.");

        // save state
        mPlayback = false;
        saveAppState();

        // unregister receivers
        this.unregisterReceiver(mHeadphoneUnplugReceiver);

        // retrieve notification system service and cancel notification
        NotificationManager notificationManager = (NotificationManager) getApplication().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(TransistorKeys.PLAYER_SERVICE_NOTIFICATION_ID);

    }


    /* Method to start the player */
    public void startActionPlay(Context context, String streamUri, String stationName, int stationID) {
        Log.v(LOG_TAG, "Starting playback service: " + mStreamUri);

        Collection collection = new Collection(new StorageHelper((Activity)context).getCollectionDirectory());

        mStreamUri = streamUri;
        mStationName = stationName;
        mStationID = stationID;

        // acquire WifiLock
//        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
//        mWifiLock.acquire();

        if (mSession == null) {
            mSession = createMediaSession(context, stationName);
        }

        // put up notification
        new NotificationHelper(collection);
        NotificationHelper.setStationName(stationName);
        NotificationHelper.setStationID(stationID);
        NotificationHelper.setStationMetadata(null);
        NotificationHelper.setMediaSession(mSession); // mSession is null here !
        NotificationHelper.createNotification(context);

        // start player service using intent
        Intent intent = new Intent(context, PlayerService.class);
        intent.setAction(TransistorKeys.ACTION_PLAY);
        intent.putExtra(TransistorKeys.EXTRA_STREAM_URI, mStreamUri);
        context.startService(intent);

    }


    /* Method to stop the player */
    public void startActionStop(Context context) {
        Log.v(LOG_TAG, "Stopping playback service.");

        // release WifiLock
//        mWifiLock.release();

        // stop player service using intent
        Intent intent = new Intent(context, PlayerService.class);
        intent.setAction(TransistorKeys.ACTION_STOP);
        context.startService(intent);
    }


    /* Set up the media player */
    private void initializeMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnInfoListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK); // needs android.permission.WAKE_LOCK

        try {
            mMetadataHelper = new MetadataHelper(getApplicationContext(), mStreamUri);
            mMediaPlayer.setDataSource(mMetadataHelper.getShoutcastProxy());
            // mMediaPlayer.setDataSource(mStreamUri);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /* Release the media player */
    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        if (mMetadataHelper != null) {
            mMetadataHelper.closeShoutcastProxyConnection();
            mMetadataHelper = null;
        }

        if (mSession != null) {
            mSession.setActive(false);
            // mSession.release();
            // mSession = null;
        }

    }


    /* Prepare playback */
    private void preparePlayback() {
        // stop running player
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            releaseMediaPlayer();
        }

        // request focus and initialize media player
        if (mStreamUri != null && requestFocus()) {
            initializeMediaPlayer();
        }

        // save state
        mPlayback = true;
        saveAppState();

        // send local broadcast (needed by MainActivityFragment)
        Intent i = new Intent();
        i.setAction(TransistorKeys.ACTION_PLAYBACK_STARTED);
        LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(i);
    }


    /* Finish playback */
    private void finishPlayback() {
        // release player
        releaseMediaPlayer();

        // save state
        mPlayback = false;
        saveAppState();

        // send local broadcast (needed by PlayerActivityFragment and MainActivityFragment)
        Intent i = new Intent();
        i.setAction(TransistorKeys.ACTION_PLAYBACK_STOPPED);
        LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(i);

        // retrieve notification system service and cancel notification
        NotificationManager notificationManager = (NotificationManager) getApplication().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(TransistorKeys.PLAYER_SERVICE_NOTIFICATION_ID);
    }


    /* Request audio manager focus */
    private boolean requestFocus() {
        int result = mAudioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }


    /* Creates media session */
    private MediaSessionCompat createMediaSession(Context context, String stationNAme) {
        // start a new MediaSession
        // https://www.youtube.com/watch?v=XQwe30cZffg&feature=youtu.be&t=883
        // https://www.youtube.com/watch?v=G6pFai3ll9E
        // https://www.youtube.com/watch?v=FBC1FgWe5X4
        // https://gist.github.com/ianhanniballake/15dce0b233b4f4b23ef8

        // create a media session
        MediaSessionCompat session = new MediaSessionCompat(context, LOG_TAG);
        session.setPlaybackState(getPlaybackState());
        session.setCallback(new MediaSessionCallback());
        session.setMetadata(getMetadata(context, null));
        session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        setSessionToken(session.getSessionToken());

        return session;
    }


    /* Creates playback state depending on mPlaback */
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


    /* TODO finish this method*/
    private MediaMetadataCompat getMetadata(Context context, String metaData) {

        Log.v(LOG_TAG, "!!! Name of Station (getMetadata): " + mStationName); // TODO: remove this

        MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mStationName)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, metaData)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_notification_large_bg_128dp))
                .build();

        return metadata;
    }




    /* Saves state of playback */
    private void saveAppState () {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplication());
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(TransistorKeys.PREF_PLAYBACK, mPlayback);
        editor.apply();
        Log.v(LOG_TAG, "Saving state.");
    }


    /**
     * Inner class: Receiver for headphone unplug-signal
     */
    public class HeadphoneUnplugReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mPlayback && AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                Log.v(LOG_TAG, "Headphones unplugged. Stopping playback.");
                // stop playback
                finishPlayback();
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
            preparePlayback();
        }

        @Override
        public void onPause() {
            // stop playback on pause signal from Android Wear or headphone button
            finishPlayback();
        }

        @Override
        public void onStop() {
            // stop playback on stop signal from notification button
            finishPlayback();
        }

    }
    /**
     * End of inner class
     */



}
