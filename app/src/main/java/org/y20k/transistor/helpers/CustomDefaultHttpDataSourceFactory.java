/**
 * CustomDefaultHttpDataSourceFactory.java
 * Implements a factory for a custom DefaultHttpDataSource
 * A CustomDefaultHttpDataSourceFactory produces a DefaultHttpDataSource that can extract Shoutcast metadata - IcyDataSource
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-17 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 *
 * Code adapted from
 * https://github.com/google/ExoPlayer/blob/release-v2/library/src/main/java/com/google/android/exoplayer2/upstream/DefaultHttpDataSourceFactory.java
 */

package org.y20k.transistor.helpers;


import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.spoledge.aacdecoder.PlayerCallback;


/**
 * CustomDefaultHttpDataSourceFactory.class
 */
public class CustomDefaultHttpDataSourceFactory implements DataSource.Factory {

    /* Main class variables */
    private final String userAgent;
    private final TransferListener<? super DataSource> listener;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final boolean allowCrossProtocolRedirects;
    private boolean enableShoutcast = false;
    private PlayerCallback playerCallback;


    /* Constructor */
    public CustomDefaultHttpDataSourceFactory(String userAgent,
                                              TransferListener<? super DataSource> listener,
                                              boolean enableShoutcast,
                                              PlayerCallback playerCallback) {
        this(userAgent,
             listener,
             DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
             DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
             false,
             enableShoutcast,
             playerCallback);
    }


    /* Constructor */
    public CustomDefaultHttpDataSourceFactory(String userAgent,
                                              TransferListener<? super DataSource> listener,
                                              int connectTimeoutMillis,
                                              int readTimeoutMillis,
                                              boolean allowCrossProtocolRedirects,
                                              boolean enableShoutcast,
                                              PlayerCallback playerCallback) {
        this.userAgent = userAgent;
        this.listener = listener;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.allowCrossProtocolRedirects = allowCrossProtocolRedirects;
        this.enableShoutcast = enableShoutcast;
        this.playerCallback = playerCallback;
    }


    @Override
    public DefaultHttpDataSource createDataSource() {
        // toggle Shoutcast extraction
        if (enableShoutcast) {
            return new IcyDataSource(userAgent, null, playerCallback);
        } else {
            return new DefaultHttpDataSource(userAgent, null, listener, connectTimeoutMillis,
                    readTimeoutMillis, allowCrossProtocolRedirects, null);
        }
    }
}