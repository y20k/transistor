/**
 * PlayerService.java
 * Implements the app's playback background service
 * The player service does xyz
 * <p/>
 * This file is part of
 * TRANSISTOR - Radio App for Android
 * <p/>
 * Copyright (c) 2015 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor;

import android.Manifest;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import org.y20k.transistor.helpers.NotificationHelper;

import java.io.IOException;
import java.util.Random;


/**
 * PlayerService class
 */
public class PlayerService extends Service implements
        AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener {

    /* Define log tag */
    public final String LOG_TAG = PlayerService.class.getSimpleName();

    /* Keys */
    private static final String ACTION_PLAY = "org.y20k.transistor.action.PLAY";
    private static final String ACTION_STOP = "org.y20k.transistor.action.STOP";
    private static final String ACTION_PLAYBACK_STOPPED = "org.y20k.transistor.action.PLAYBACK_STOPPED";
    private static final String EXTRA_STREAM_URL = "STREAM_URL";
    public static final String PLAYBACK = "playback";
    private static final int PLAYER_SERVICE_NOTIFICATION_ID = 1;
    public static final String STATION_ID_CURRENT = "stationIDCurrent";

    /* Main class variables */
    private AudioManager mAudioManager;
    private MediaPlayer mMediaPlayer;
    private String mStreamURL;
    private String mStationName;
    private boolean mPlayback;
    private boolean mPhonePermission;
    private HeadphoneUnplugReceiver mHeadphoneUnplugReceiver;
    private PhoneStateReceiver mPhoneStateReceiver;



    // TODO: Remove?
    private IBinder mBinder;
    private Random mGenerator;


    /* Constructor (default) */
    public PlayerService() {
    }


    @Override
    public void onCreate() {
        super.onCreate();

        // TODO descibe
        checkPermissions();

        // set up variables
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mMediaPlayer = null;

        // Listen for headphone unglug
        IntentFilter headphoneUnplugIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        mHeadphoneUnplugReceiver = new HeadphoneUnplugReceiver();
        registerReceiver(mHeadphoneUnplugReceiver, headphoneUnplugIntentFilter);

        // Listen for phone activity
        IntentFilter phoneStateIntentFilter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        mPhoneStateReceiver = new PhoneStateReceiver();
        registerReceiver(mPhoneStateReceiver, phoneStateIntentFilter);

        // TODO Listen for headphone button
        // Use MediaSession

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // checking for empty intent
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

            // request focus and initialize media player
            if (mStreamURL != null && requestFocus()) {
                initializeMediaPlayer();
            }
        }

        // ACTION STOP
        else if (intent.getAction().equals(ACTION_STOP)) {
            Log.v(LOG_TAG, "Service received command: STOP");
            mPlayback = false;
            stopPlayback();
        }

        // default return value for media playback
        return START_STICKY;
    }


    // @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
                stopPlayback();
                break;
            // transient loss of audio focus
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (!mPlayback && mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    stopPlayback();
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

        // stop playback
        // stopPlayback();

        // save state
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplication());
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(STATION_ID_CURRENT, -1);
        editor.putBoolean(PLAYBACK, false);
        editor.commit();

        // unregister receivers
        this.unregisterReceiver(mPhoneStateReceiver);
        this.unregisterReceiver(mHeadphoneUnplugReceiver);

        // retrieve notification system service and cancel notification
        NotificationManager notificationManager = (NotificationManager) getApplication().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(PLAYER_SERVICE_NOTIFICATION_ID);

    }


    /* Method to start the player */
    public void startActionPlay(Context context, String streamURL, String stationName) {
        mStreamURL = streamURL;
        mStationName = stationName;
        Log.v(LOG_TAG, "starting playback service: " + mStreamURL);

        // start player service using intent
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

        // stop player service using intent
        Intent intent = new Intent(context, PlayerService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
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


    /* Stop playback */
    private void stopPlayback() {
        // release player
        releaseMediaPlayer();

        // store player state in shared preferences
        mPlayback = false;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplication());
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PLAYBACK, mPlayback);
        editor.commit();

        // retrieve notification system service and cancel notification
        NotificationManager notificationManager = (NotificationManager) getApplication().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(PLAYER_SERVICE_NOTIFICATION_ID);

        // notify PlayerActivityFragment
        Intent i = new Intent();
        i.setAction(ACTION_PLAYBACK_STOPPED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);

    }


    /* Request audio manager focus */
    private boolean requestFocus() {
        int result = mAudioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return true;
        } else {
            return false;
        }
    }


//    /* Abandon audio manager focus */
//    private boolean abandonFocus() {
//        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
//                mAudioManager.abandonAudioFocus(this);
//    }


    /* Check permissions and save state of permissions */
    private void checkPermissions() {
        // set default value
        mPhonePermission = true;

        // check for permission to read phone state
        if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // not granted
            mPhonePermission = false;
        }

        if (mPhonePermission) {
            System.out.println("!!! Permission granted ");
        }
        else {
            System.out.println("!!! Permission denied ");
        }

        // save state to settings
        // savePermissionsState(mActivity);
    }






    /**
     * Inner class: Receiver for headphone unplug-signal
     */
    public class HeadphoneUnplugReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mPlayback == true && AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                // stop playback
                stopPlayback();
                // notify user
                Toast.makeText(context, R.string.toastalert_headphones_unplugged, Toast.LENGTH_LONG).show();
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
                // stop playback
                stopPlayback();
                // notify user
                Toast.makeText(getApplication(), R.string.toastalert_phone_active, Toast.LENGTH_LONG).show();
            }
        }
    }
    /**
     * End of inner class
     */


}

/**
 * TODO
 * - try to get metadata from stream MediaPlayer.TrackInfo
 */