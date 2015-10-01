/**
 * PlayerService.java
 * Implements the app's playback background service
 * The player service does xyz
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import org.y20k.transistor.helpers.NotificationHelper;

import java.io.IOException;
import java.util.Random;


/**
 * PlayerService class
 */
public class PlayerService extends Service implements AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener {

    /* Define log tag */
    public final String LOG_TAG = PlayerService.class.getSimpleName();

    /* Keys */
    private static final String ACTION_PLAY = "org.y20k.transistor.action.PLAY";
    private static final String ACTION_STOP = "org.y20k.transistor.action.STOP";
    private static final String ACTION_PLAYBACK_STOPPED = "org.y20k.transistor.action.PLAYBACK_STOPPED";
    private static final String EXTRA_STREAM_URL = "STREAM_URL";
//    private static final String EXTRA_STATION_NAME = "STATION_NAME";
    public static final String PLAYBACK = "playback";
    private static final int PLAYER_SERVICE_NOTIFICATION_ID = 1;

    /* Main class variables */
    private AudioManager mAudioManager;
    private MediaPlayer mMediaPlayer;
    private String mStreamURL;
    private String mStationName;
    private boolean mPlayback;


    // TODO: Remove?
    private IBinder mBinder;
    private Random mGenerator;


    /* Constructor (default) */
    public PlayerService() {
    }


    @Override
    public void onCreate() {
        super.onCreate();

        // set up variables
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mMediaPlayer = null;

        // Listen for headphone unglug
        IntentFilter headphoneUnplugIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        HeadphoneUnplugReceiver headphoneUnplugReceiver = new HeadphoneUnplugReceiver();
        registerReceiver(headphoneUnplugReceiver, headphoneUnplugIntentFilter);

        // Listen for phone activity
        IntentFilter phoneStateIntentFilter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        PhoneStateReceiver phoneStateReceiver = new PhoneStateReceiver();
        registerReceiver(phoneStateReceiver, phoneStateIntentFilter);

     }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // checking for null
        if (intent == null) {
            Log.v(LOG_TAG, "Service stopped and restarted by the system. Stopping self now.");
            stopSelf();
        }

        // ACTION PLAY
        else if (intent.getAction().equals(ACTION_PLAY)) {
            Log.v(LOG_TAG, "Service received command: PLAY");

            // stop running player
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                releaseMediaPlayer();
            }

            // set mPlayback true
            mPlayback = true;

            // get URL of station from intent
            mStreamURL = intent.getStringExtra(EXTRA_STREAM_URL);

            // initialize media player
            if (mStreamURL != null) {
                initializeMediaPlayer();
            }
        }

        // ACTION STOP
        else if (intent.getAction().equals(ACTION_STOP)) {
            Log.v(LOG_TAG, "Service received command: STOP");
            mPlayback = false;
            releaseMediaPlayer();

            // notify PlayerActivityFragment
            Intent i = new Intent();
            i.setAction(ACTION_PLAYBACK_STOPPED);
            sendBroadcast(i);

            // TODO CANCEL NOTIFICATION FROM HERE

        }

        // default return value for media playback
        return START_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onAudioFocusChange(int focusChange) {

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mMediaPlayer == null) {
                    initializeMediaPlayer();
                } else if (!mMediaPlayer.isPlaying()) {
                    mMediaPlayer.start();
                }
                mMediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                releaseMediaPlayer();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.setVolume(0.1f, 0.1f);
                }
                break;
        }
    }


    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
    }


    /* Method to start the player */
    public void startActionPlay(Context context, String streamURL, String stationName) {
        mStreamURL = streamURL;
        mStationName = stationName;
        Log.v(LOG_TAG, "starting playback service: " + mStreamURL);
        Intent intent = new Intent(context, PlayerService.class);
        intent.setAction(ACTION_PLAY);
        intent.putExtra(EXTRA_STREAM_URL, mStreamURL);
        context.startService(intent);

        // put up notification
        NotificationHelper notificationHelper = new NotificationHelper(context);
        notificationHelper.setStationName(mStationName);
        notificationHelper.createNotification();
    }


    /* Method to stop the player */
    public void startActionStop(Context context) {
        Log.v(LOG_TAG, "stopping playback service:");
        Intent intent = new Intent(context, PlayerService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);

        // retrieve notification system service and cancel notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(PLAYER_SERVICE_NOTIFICATION_ID);

    }


    /* Set up the media player */
    private void initializeMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                // bufferProgressBar.setSecondaryProgress(percent);
                Log.v(LOG_TAG, "Buffering: " + percent);
            }
        });

        try {
            mMediaPlayer.setDataSource(mStreamURL);
            mMediaPlayer.prepareAsync();
            Log.v(LOG_TAG, "setting: " + mStreamURL);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
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
    }


    /* Request audio manager focus */
    private boolean requestFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                mAudioManager.requestAudioFocus(this,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN);
    }


    /* Abandon audio manager focus */
    private boolean abandonFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                mAudioManager.abandonAudioFocus(this);
    }


    /**
     * Inner class: Receiver for headphone unplug-signal
     */
    public class HeadphoneUnplugReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mPlayback == true && AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                // notify user
                Toast.makeText(getApplication(), R.string.toastalert_headphones_unplugged, Toast.LENGTH_LONG).show();

                // notify PlayerActivityFragment
                Intent i = new Intent();
                i.setAction(ACTION_PLAYBACK_STOPPED);
                sendBroadcast(i);

                // store player state in shared preferences
                mPlayback = false;
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean(PLAYBACK, mPlayback);
                editor.commit();

                // release player
                releaseMediaPlayer();
            }
        }
    }
    /**
     * End of inner class
     */


    /**
     * Inner class: Receiver for phone state signal
     */
    public class PhoneStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mPlayback == true && TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {

                // notify user
                Toast.makeText(getApplication(), R.string.toastalert_phone_active, Toast.LENGTH_LONG).show();

                // notify PlayerActivityFragment
                Intent i = new Intent();
                i.setAction(ACTION_PLAYBACK_STOPPED);
                sendBroadcast(i);

                // store player state in shared preferences
                mPlayback = false;
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean(PLAYBACK, mPlayback);
                editor.commit();

                // release player
                releaseMediaPlayer();
            }
        }
    }
    /**
     * End of inner class
     */

}



// Reconnect on connectivity change
// https://developer.android.com/training/monitoring-device-state/connectivity-monitoring.html
// https://developer.android.com/reference/android/net/ConnectivityManager.html#CONNECTIVITY_ACTION
// You can register a broadcast receiver in your manifest to listen for these changes and resume (or suspend) your background updates accordingly.
// MANIFEST --> <action android:name="android.net.conn.CONNECTIVITY_CHANGE"/>

// TODO consider using an IntentService

//// construct progress bar in case of stream buffering
//bufferProgressBar = (ProgressBar) rootView.findViewById(R.id.progressbar_spinner);
//bufferProgressBar.setMax(100);
//bufferProgressBar.setVisibility(View.INVISIBLE);