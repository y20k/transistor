/*
** AACDecoder - Freeware Advanced Audio (AAC) Decoder for Android
** Copyright (C) 2012 Spolecne s.r.o., http://www.spoledge.com
**  
** This file is a part of AACDecoder.
**
** AACDecoder is free software; you can redistribute it and/or modify
** it under the terms of the GNU Lesser General Public License as published
** by the Free Software Foundation; either version 3 of the License,
** or (at your option) any later version.
** 
** This program is distributed in the hope that it will be useful,
** but WITHOUT ANY WARRANTY; without even the implied warranty of
** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
** GNU Lesser General Public License for more details.
** 
** You should have received a copy of the GNU Lesser General Public License
** along with this program. If not, see <http://www.gnu.org/licenses/>.
*/
package com.spoledge.aacdecoder;

import android.util.Log;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * This is an InputStream which allows to fetch Icecast/Shoutcast metadata from.
 */
public class IcyInputStream extends FilterInputStream {

    private static final String LOG = "IcyInputStream";


    ////////////////////////////////////////////////////////////////////////////
    // Attributes
    ////////////////////////////////////////////////////////////////////////////


    /**
     * The period of metadata frame in bytes.
     */
    protected int period;


    /**
     * The actual number of remaining bytes before the metadata.
     */
    protected int remaining;


    /**
     * This is a temporary buffer used for fetching metadata bytes.
     */
    protected byte[] mbuffer;


    /**
     * The callback - may be null.
     */
    protected PlayerCallback playerCallback;


    /**
     * The character encoding of the metadata.
     */
    protected String characterEncoding;


    ////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new input stream.
     * @param in the underlying input stream
     * @param period the period of metadata frame is repeating (in bytes)
     */
    public IcyInputStream( InputStream in, int period ) {
        this( in, period, null );
    }


    /**
     * Creates a new input stream.
     * @param in the underlying input stream
     * @param period the period of metadata frame is repeating (in bytes)
     * @param playerCallback the callback - may be null
     */
    public IcyInputStream( InputStream in, int period, PlayerCallback playerCallback ) {
        this( in, period, playerCallback, null );
    }


    /**
     * Creates a new input stream.
     * @param in the underlying input stream
     * @param period the period of metadata frame is repeating (in bytes)
     * @param playerCallback the callback - may be null
     * @param characterEncoding the encoding used for metadata strings - may be null = default is UTF-8
     */
    public IcyInputStream( InputStream in, int period, PlayerCallback playerCallback, String characterEncoding ) {
        super( in );
        this.period = period;
        this.playerCallback = playerCallback;
        this.characterEncoding = characterEncoding != null ? characterEncoding : "UTF-8";

        remaining = period;
        mbuffer = new byte[128];
    }


    ////////////////////////////////////////////////////////////////////////////
    // InputStream
    ////////////////////////////////////////////////////////////////////////////

    @Override
    public int read() throws IOException {
        int ret = super.read();

        if (--remaining == 0) fetchMetadata();

        return ret;
    }


    @Override
    public int read( byte[] buffer, int offset, int len ) throws IOException {
        int ret = in.read( buffer, offset, remaining < len ? remaining : len );

        if (remaining == ret) fetchMetadata();
        else remaining -= ret;

        return ret;
    }


    ////////////////////////////////////////////////////////////////////////////
    // Public
    ////////////////////////////////////////////////////////////////////////////

    public String getCharacterEncoding() {
        return characterEncoding;
    }


    /**
     * Sets the character encoding used for the metadata strings.
     * By default it is set to UTF-8.
     */
    public void setCharacterEncoding( String characterEncoding ) {
        this.characterEncoding = characterEncoding;
    }


    ////////////////////////////////////////////////////////////////////////////
    // Protected
    ////////////////////////////////////////////////////////////////////////////

    /**
     * This method reads the metadata string.
     * Actually it calls the method parseMetadata().
     */
    protected void fetchMetadata() throws IOException {
        remaining = period;

        int size = in.read();

        // either no metadata or eof:
        if (size < 1) return;

        // size *= 16:
        size <<= 4;

        if (mbuffer.length < size) {
            mbuffer = null;
            mbuffer = new byte[ size ];
            Log.d( LOG, "Enlarged metadata buffer to " + size + " bytes");
        }

        size = readFully( mbuffer, 0, size );

        // find the string end:
        for (int i=0; i < size; i++) {
            if (mbuffer[i] == 0) {
                size = i;
                break;
            }
        }

        String s;

        try {
            s = new String( mbuffer, 0, size, characterEncoding );
        }
        catch (Exception e) {
            Log.e( LOG, "Cannot convert bytes to String" );
            return;
        }

        Log.d( LOG, "Metadata string: " + s );

        parseMetadata( s );
    }


    /**
     * Parses the metadata and sends them to PlayerCallback.
     * @param s the metadata string like: StreamTitle='...';StreamUrl='...';
     */
    protected void parseMetadata( String s ) {
        String[] kvs = s.split( ";" );

        for (String kv : kvs) {
            int n = kv.indexOf( '=' );
            if (n < 1) continue;

            boolean isString = n + 1 < kv.length()
                                && kv.charAt( kv.length() - 1) == '\''
                                && kv.charAt( n + 1 ) == '\'';

            String key = kv.substring( 0, n );
            String val = isString ?
                            kv.substring( n+2, kv.length()-1) :
                            n + 1 < kv.length() ?
                                kv.substring( n+1 ) : "";

            // yes - we should detect this earlier, but it will not be null in most cases:
            if (playerCallback != null) playerCallback.playerMetadata( key, val );
        }
    }


    /**
     * Tries to read all bytes into the target buffer.
     * @param size the requested size
     * @return the number of really bytes read; if less than requested, then eof detected
     */
    protected final int readFully( byte[] buffer, int offset, int size ) throws IOException {
        int n;
        int oo = offset;

        while (size > 0 && (n = in.read( buffer, offset, size )) != -1) {
            offset += n;
            size -= n;
        }

        return offset - oo;
    }

}

