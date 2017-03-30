/**
 * IcyDataSource.java
 * Implements a IcyDataSource
 * An IcyDataSource creates a DefaultHttpDataSource that can extract Shoutcast metadata
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-17 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 *
 * Code adapted from
 * https://github.com/Ood-Tsen/ExoPlayer/blob/8ccc99bc5c6428760efd9f1780dd90be9386339e/demo/src/main/java/com/google/android/exoplayer/demo/player/IcyDataSource.java
 */


package org.y20k.transistor.helpers;

import android.media.AudioTrack;

import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.util.Predicate;
import com.spoledge.aacdecoder.IcyInputStream;
import com.spoledge.aacdecoder.PlayerCallback;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * IcyDataSource class
 */
public class IcyDataSource extends DefaultHttpDataSource {

    /* Define log tag */    
    private static final String LOG_TAG = IcyDataSource.class.getSimpleName();


    /* Main class variables */
    boolean metadataEnabled = true;

    
    /* Constructor */
    public IcyDataSource(String userAgent, Predicate<String> contentTypePredicate) {
        super(userAgent, contentTypePredicate);
    }


    @Override
    public long open(DataSpec dataSpec) throws HttpDataSourceException {
        return super.open(dataSpec);
    }

    @Override
    protected HttpURLConnection makeConnection(DataSpec dataSpec) throws IOException {
        // open a http connection for Icy-Metadata (Shoutcast)
        LogHelper.i(LOG_TAG, "makeConnection[" + dataSpec.position + "-" + dataSpec.length);

        URL url = new URL(dataSpec.uri.toString());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Icy-Metadata", "1");
        return connection;
    }


    @Override
    protected InputStream getInputStream(HttpURLConnection conn) throws Exception {
        // Get the input stream from the connection. Actually returns the underlying stream or IcyInputStream
        String smetaint = conn.getHeaderField( "icy-metaint");
        InputStream ret = conn.getInputStream();

        if (!metadataEnabled) {
            LogHelper.v(LOG_TAG, "Metadata not enabled");
        }
        else if (smetaint != null) {
            int period = -1;
            try {
                period = Integer.parseInt( smetaint);
            }
            catch (Exception e) {
                LogHelper.e(LOG_TAG, "The icy-metaint '" + smetaint + "' cannot be parsed: '" + e);
            }

            if (period > 0) {
                LogHelper.v(LOG_TAG, "The dynamic metainfo is sent every " + period + " bytes");

                ret = new IcyInputStream(ret, period, playerCallback, null);
            }
        }
        else LogHelper.v(LOG_TAG, "This stream does not provide dynamic metainfo");

        return ret;
    }


    /* TODO Describe */
    PlayerCallback playerCallback = new PlayerCallback() {

        @Override
        public void playerStarted() {
            LogHelper.v(LOG_TAG, "playerStarted" );
        }

        @Override
        public void playerPCMFeedBuffer(boolean isPlaying, int audioBufferSizeMs, int audioBufferCapacityMs) {
            LogHelper.v(LOG_TAG, "playerPCMFeedBuffer" );
        }

        @Override
        public void playerStopped(int perf) {
            LogHelper.v(LOG_TAG, "playerStopped" );
        }

        @Override
        public void playerException(Throwable t) {
            LogHelper.v(LOG_TAG, "playerException" );
        }

        @Override
        public void playerMetadata(String key, String value) {
            LogHelper.v(LOG_TAG, "playerMetadata " + key + " : " + value);
        }

        @Override
        public void playerAudioTrackCreated(AudioTrack audioTrack) {
            LogHelper.v(LOG_TAG, "playerMetadata" );
        }
    };
}
