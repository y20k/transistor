/**
 * IcyDataSource.java
 * Implements a IcyDataSource
 * An IcyDataSource creates a DefaultHttpDataSource that can extract Shoutcast metadata
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-18 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 *
 * Code adapted from:
 * https://github.com/google/ExoPlayer/issues/466#issuecomment-361420861
 * Credit: https://github.com/asheeshs (Thanks!)
 */


package org.y20k.transistor.helpers;


import android.net.Uri;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * IcyDataSource class
 */
public class IcyDataSource implements DataSource, TransistorKeys {

    /* Define log tag */
    private static final String LOG_TAG = IcyDataSource.class.getSimpleName();


    /* Main class variables */
    private final PlayerCallback mPlayerCallback;
    private HttpURLConnection mConnection;
    private InputStream mInputStream;
    private boolean mMetadataEnabled = true;


    /* Constructor */
    public IcyDataSource(final PlayerCallback playerCallback) {
        mPlayerCallback = playerCallback;
    }


    @Override
    public long open(final DataSpec dataSpec) throws IOException {
        LogHelper.i(LOG_TAG, "open[" + dataSpec.position + "-" + dataSpec.length);

        URL url = new URL(dataSpec.uri.toString());
        mConnection = (HttpURLConnection) url.openConnection();
        mConnection.setRequestProperty("Icy-Metadata", "1");

        try {
            mInputStream = getInputStream(mConnection);
        } catch (Exception e) {
            closeConnectionQuietly();
            throw new IOException(e.getMessage());
        }

        return dataSpec.length;
    }


    @Override
    public int read(final byte[] buffer, final int offset, final int readLength) throws IOException {
        return mInputStream.read(buffer, offset, readLength);
    }


    @Override
    public Uri getUri() {
        return mConnection == null ? null : Uri.parse(mConnection.getURL().toString());
    }

    @Override
    public void close() throws IOException {
        try {
            if (mInputStream != null) {
                try {
                    mInputStream.close();
                } catch (IOException e) {
                    throw new IOException(e.getMessage());
                }
            }
        } finally {
            mInputStream = null;
            closeConnectionQuietly();
        }
    }


    /* Gets input stream from mConnection. Actually returns underlying stream or IcyInputStream. */
    protected InputStream getInputStream( HttpURLConnection conn ) throws Exception {
        String metaint = conn.getHeaderField( "icy-metaint" );
        InputStream inputStream = conn.getInputStream();

        if (!mMetadataEnabled) {
            LogHelper.i(LOG_TAG, "Metadata not enabled" );
        }
        else if (metaint != null) {
            int period = -1;
            try {
                period = Integer.parseInt( metaint );
            }
            catch (Exception e) {
                LogHelper.e(LOG_TAG, "The icy-metaint '" + metaint + "' cannot be parsed: '" + e );
            }

            if (period > 0) {
                LogHelper.i(LOG_TAG, "The dynamic metainfo is sent every " + period + " bytes" );
                inputStream = new IcyInputStream(inputStream, period, mPlayerCallback, null );
            }
        }
        else {
            LogHelper.i(LOG_TAG, "This stream does not provide dynamic metainfo" );
        }

        return inputStream;
    }


    /* Closes the current mConnection quietly, if there is one. */
    private void closeConnectionQuietly() {
        if (mConnection != null) {
            try {
                mConnection.disconnect();
            } catch (Exception e) {
                LogHelper.e(LOG_TAG, "Unexpected error while disconnecting. " + e.toString());
            }
            mConnection = null;
        }
    }

}
