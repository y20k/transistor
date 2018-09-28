/**
 * IcyDataSourceFactory.java
 * Implements a factory for a custom DefaultDataSource
 * A IcyDataSourceFactory produces a DefaultDataSource that can extract Shoutcast metadata - IcyDataSource
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-18 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 *
 * Code adapted from
 * https://github.com/google/ExoPlayer/blob/release-v2/library/core/src/main/java/com/google/android/exoplayer2/upstream/DefaultDataSourceFactory.java
 */

package org.y20k.transistor.helpers;


import android.content.Context;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSource.Factory;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;


/**
 * IcyDataSourceFactory.class
 */
public final class IcyDataSourceFactory implements Factory {

    /* Main class variables */
    private final Context context;
    private final TransferListener listener;
    private final DataSource.Factory baseDataSourceFactory;
    private boolean enableShoutcast = false;
    private PlayerCallback playerCallback;


    /* Constructor */
    public IcyDataSourceFactory(Context context,
                                String userAgent,
                                boolean enableShoutcast,
                                PlayerCallback playerCallback) {
        // use next Constructor
        this(context,
             userAgent,
             null,
             enableShoutcast,
             playerCallback);
    }


    /* Constructor */
    public IcyDataSourceFactory(Context context,
                                String userAgent,
                                TransferListener listener,
                                boolean enableShoutcast,
                                PlayerCallback playerCallback) {
        // use next Constructor
        this(context,
             listener,
             new DefaultHttpDataSourceFactory(userAgent, listener),
             enableShoutcast,
             playerCallback);
    }


    /* Constructor */
    public IcyDataSourceFactory(Context context,
                                TransferListener listener,
                                DataSource.Factory baseDataSourceFactory,
                                boolean enableShoutcast,
                                PlayerCallback playerCallback) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        this.baseDataSourceFactory = baseDataSourceFactory;
        this.enableShoutcast = enableShoutcast;
        this.playerCallback = playerCallback;
    }


    @Override
    public DataSource createDataSource() {
        // toggle Shoutcast extraction
        if (enableShoutcast) {
            return new IcyDataSource(playerCallback);
        } else {
            return new DefaultDataSource(context, listener, baseDataSourceFactory.createDataSource());
        }
    }

}