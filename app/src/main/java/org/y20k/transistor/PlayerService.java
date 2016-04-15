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

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.y20k.transistor.helpers.NotificationHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;


/**
 * PlayerService class
 */
public final class PlayerService extends Service implements
        AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnInfoListener {

    /* Define log tag */
    private static final String LOG_TAG = PlayerService.class.getSimpleName();


    /* Keys */
    private static final String ACTION_PLAY = "org.y20k.transistor.action.PLAY";
    private static final String ACTION_STOP = "org.y20k.transistor.action.STOP";
    private static final String ACTION_PLAYBACK_STARTED = "org.y20k.transistor.action.PLAYBACK_STARTED";
    private static final String ACTION_PLAYBACK_STOPPED = "org.y20k.transistor.action.PLAYBACK_STOPPED";
    private static final String EXTRA_STREAM_URI = "STREAM_URI";
    private static final String PREF_PLAYBACK = "prefPlayback";
    private static final int PLAYER_SERVICE_NOTIFICATION_ID = 1;
    private static final String SHOUTCAST_STREAM_TITLE_HEADER = "StreamTitle='";


    /* Main class variables */
    private AudioManager mAudioManager;
    private MediaPlayer mMediaPlayer;
    private String mStreamUri;
    private int mStationID;
    private boolean mPlayback;
    private int mPlayerInstanceCounter;
    private HeadphoneUnplugReceiver mHeadphoneUnplugReceiver;
    private WifiManager.WifiLock mWifiLock;
    private Socket mProxyConnection = null;
    private boolean mProxyRunning = false;


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

        // Listen for headphone unplug
        IntentFilter headphoneUnplugIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        mHeadphoneUnplugReceiver = new HeadphoneUnplugReceiver();
        registerReceiver(mHeadphoneUnplugReceiver, headphoneUnplugIntentFilter);

        // TODO Listen for headphone button
        // Use MediaSession
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // checking for empty intent
        if (intent == null) {
            Log.v(LOG_TAG, "Null-Intent received. Stopping self.");
            stopSelf();
        }

        // ACTION PLAY
        else if (intent.getAction().equals(ACTION_PLAY)) {
            Log.v(LOG_TAG, "Service received command: PLAY");

            // set mPlayback true
            mPlayback = true;

            // get URL of station from intent
            if (intent.hasExtra(EXTRA_STREAM_URI)) {
                mStreamUri = intent.getStringExtra(EXTRA_STREAM_URI);
            }

            // start playback
            preparePlayback();

            // increase counter
            mPlayerInstanceCounter++;
        }

        // ACTION STOP
        else if (intent.getAction().equals(ACTION_STOP)) {
            Log.v(LOG_TAG, "Service received command: STOP");

            // set mPlayback false
            mPlayback = false;

            // stop playback
            finishPlayback();

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
            Log.v(LOG_TAG, "+++ Preparation finished. Starting playback. Player instance count: " + mPlayerInstanceCounter + " +++");
            Log.v(LOG_TAG, "+++ " + mStreamUri + " +++");

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
        notificationManager.cancel(PLAYER_SERVICE_NOTIFICATION_ID);

    }


    /* Method to start the player */
    public void startActionPlay(Context context, String streamUri, String stationName, int stationID) {
        Log.v(LOG_TAG, "Starting playback service: " + mStreamUri);

        mStreamUri = streamUri;
        mStationID = stationID;
        Log.v(LOG_TAG, "!!! startActionPlay -> mStationID: " + mStationID); // TODO remove

        // acquire WifiLock
//        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
//        mWifiLock.acquire();


        // start player service using intent
        Intent intent = new Intent(context, PlayerService.class);
        intent.setAction(ACTION_PLAY);
        intent.putExtra(EXTRA_STREAM_URI, mStreamUri);
        context.startService(intent);

        // put up notification
        NotificationHelper notificationHelper = new NotificationHelper(context);
        notificationHelper.setStationName(stationName);
        notificationHelper.setStationID(mStationID);
        notificationHelper.createNotification();
    }


    /* Method to stop the player */
    public void startActionStop(Context context) {
        Log.v(LOG_TAG, "Stopping playback service.");

        // release WifiLock
//        mWifiLock.release();

        // stop player service using intent
        Intent intent = new Intent(context, PlayerService.class);
        intent.setAction(ACTION_STOP);
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
            mMediaPlayer.setDataSource(createShoutcastProxyConnection());
            mMediaPlayer.prepareAsync();
            Log.v(LOG_TAG, "setting: " + mStreamUri);
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
            closeShoutcastProxyConnectionAsync();
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
        i.setAction(ACTION_PLAYBACK_STARTED);
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
        i.setAction(ACTION_PLAYBACK_STOPPED);
        LocalBroadcastManager.getInstance(this.getApplication()).sendBroadcast(i);

        // retrieve notification system service and cancel notification
        NotificationManager notificationManager = (NotificationManager) getApplication().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(PLAYER_SERVICE_NOTIFICATION_ID);
    }


    /* Request audio manager focus */
    private boolean requestFocus() {
        int result = mAudioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }


    /* Saves state of playback */
    private void saveAppState () {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplication());
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREF_PLAYBACK, mPlayback);
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

    /* Connect to the server, and create a listening socket on localhost,
       to stream data into the MediaPlayer, and to pull Shoutcast metadata from the stream.
       Returns localhost URL for MediaPlayer to connect to.
       Shoutcast metadata described here: http://www.smackfu.com/stuff/programming/shoutcast.html */
    private String createShoutcastProxyConnection() {
        closeShoutcastProxyConnection();
        mProxyRunning = true;
        final StringBuffer uri = new StringBuffer();

        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Socket proxy = null;
                    URLConnection connection = null;

                    try {
                        final ServerSocket proxyServer = new ServerSocket(0, 1, InetAddress.getLocalHost());
                        uri.append("http://localhost:" + String.valueOf(proxyServer.getLocalPort()) + "/");
                        Log.v(LOG_TAG, "createProxyConnection: " + uri.toString());

                        proxy = proxyServer.accept();
                        mProxyConnection = proxy;
                        proxyServer.close();

                        connection = new URL(mStreamUri).openConnection();

                        shoutcastProxyReaderLoop(proxy, connection);

                    } catch (Exception e) {}

                    mProxyRunning = false;

                    try {
                        if (connection != null) {
                            ((HttpURLConnection)connection).disconnect();
                        }
                    } catch (Exception ee) {}

                    try {
                        if (proxy != null && !proxy.isClosed()) {
                            proxy.close();
                        }
                    } catch (Exception eee) {}
                }
            }).start();

            while (uri.length() == 0) {
                try {
                    Thread.sleep(10);
                } catch (Exception e) {}
            }
            return uri.toString();
        } catch (Exception e) {
            Log.e(LOG_TAG, "createProxyConnection: cannot create new listening socket on localhost: " + e.toString());
            mProxyRunning = false;
            return "";
        }
    }

    private void closeShoutcastProxyConnectionAsync() {
        try {
            if (mProxyConnection != null && !mProxyConnection.isClosed()) {
                mProxyConnection.close(); // Terminate proxy thread loop
            }
        } catch (Exception e) {}
    }

    private void closeShoutcastProxyConnection() {
        try {
            while (mProxyRunning && mProxyConnection == null) {
                Thread.sleep(50); // Wait for proxyServer to initialize
            }
            closeShoutcastProxyConnectionAsync();
            mProxyConnection = null;
            while (mProxyRunning) {
                Thread.sleep(50); // Wait for thread to finish
            }
        } catch(Exception e) {}
    }

    private void shoutcastProxyReaderLoop(Socket proxy, URLConnection connection) throws IOException {

        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("Icy-MetaData", "1");
        connection.connect();

        InputStream in = connection.getInputStream();

        OutputStream out = proxy.getOutputStream();
        out.write( ("HTTP/1.0 200 OK\r\n" +
                "Pragma: no-cache\r\n" +
                "Content-Type: " + connection.getContentType() +
                "\r\n\r\n").getBytes(StandardCharsets.UTF_8));

        byte buf[] = new byte[16384]; // One second of 128kbit stream
        int count = 0;
        int total = 0;
        int metadataSize = 0;
        final int metadataOffset = connection.getHeaderFieldInt("icy-metaint", 0);
        int bitRate = Math.max(connection.getHeaderFieldInt("icy-br", 128), 32);
        Log.v(LOG_TAG, "createProxyConnection: connected, icy-metaint " + metadataOffset + " icy-br " + bitRate);
        while (true) {
            count = Math.min(in.available(), buf.length);
            if (count <= 0) {
                count = Math.min(bitRate * 64, buf.length); // Buffer half-second of stream data
            }
            if (metadataOffset > 0) {
                count = Math.min(count, metadataOffset - total);
            }

            count = in.read(buf, 0, count);
            if (count == 0) {
                continue;
            }
            if (count < 0) {
                break;
            }

            out.write(buf, 0, count);

            total += count;
            if (metadataOffset > 0 && total >= metadataOffset) {
                // Read metadata
                total = 0;
                count = in.read();
                if (count < 0) {
                    break;
                }
                count *= 16;
                metadataSize = count;
                if (metadataSize == 0) {
                    continue;
                }
                // Maximum metadata length is 4080 bytes
                total = 0;
                while (total < metadataSize) {
                    count = in.read(buf, total, count);
                    if (count < 0) {
                        break;
                    }
                    if (count == 0) {
                        continue;
                    }
                    total += count;
                    count = metadataSize - total;
                }
                total = 0;
                String[] metadata = new String(buf, 0, metadataSize, StandardCharsets.UTF_8).split(";");
                for (String s : metadata) {
                    if (s.indexOf(SHOUTCAST_STREAM_TITLE_HEADER) == 0 && s.length() >= SHOUTCAST_STREAM_TITLE_HEADER.length() + 1) {
                        // Update notification
                        NotificationHelper notificationHelper = new NotificationHelper(PlayerService.this);
                        notificationHelper.setStationName(s.substring(SHOUTCAST_STREAM_TITLE_HEADER.length(), s.length() - 1));
                        Log.v(LOG_TAG, "!!! shoutcastProxyReaderLoop -> mStationID: " + mStationID); // TODO remove
                        // notificationHelper.setStationID(mStationId);
                        notificationHelper.createNotification();
                    }
                }
            }
        }
    }

}
