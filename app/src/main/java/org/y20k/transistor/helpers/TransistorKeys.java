/**
 * TransistorKeys.java
 * Implements the keys used throughout the app
 * This interface hosts all keys used to control Transistor's state
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-17 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */

package org.y20k.transistor.helpers;


/**
 * TransistorKeys class
 */
public interface TransistorKeys {

    String APPLICATION_NAME = "Transistor";

    /* ACTIONS */
    String ACTION_PLAY = "org.y20k.transistor.action.PLAY";
    String ACTION_STOP = "org.y20k.transistor.action.STOP";
    String ACTION_DISMISS = "org.y20k.transistor.action.DISMISS";
    String ACTION_PLAYBACK_STATE_CHANGED = "org.y20k.transistor.action.PLAYBACK_STATE_CHANGED";
    String ACTION_METADATA_CHANGED = "org.y20k.transistor.action.METADATA_CHANGED";
    String ACTION_SHOW_PLAYER = "org.y20k.transistor.action.SHOW_PLAYER";
    String ACTION_TIMER_RUNNING = "org.y20k.transistor.action.TIMER_RUNNING";
    String ACTION_TIMER_START = "org.y20k.transistor.action.TIMER_START";
    String ACTION_TIMER_STOP = "org.y20k.transistor.action.TIMER_STOP";

    /* EXTRAS */
    String EXTRA_PLAYBACK_STATE_PREVIOUS_STATION = "PLAYBACK_STATE_PREVIOUS_STATION";
    String EXTRA_PLAYBACK_STATE = "PLAYBACK_STATE";
    String EXTRA_STATION = "STATION";
    String EXTRA_LAST_STATION = "LAST_STATION";
    String EXTRA_STREAM_URI = "STREAM_URI";
    String EXTRA_TIMER_DURATION = "TIMER_DURATION";
    String EXTRA_TIMER_REMAINING = "TIMER_REMAINING";
    String EXTRA_ERROR_OCCURED = "ERROR_OCCURED";

    /* ARGS */
    String ARG_INFOSHEET_TITLE = "INFOSHEET_TITLE";
    String ARG_INFOSHEET_CONTENT = "INFOSHEET_CONTENT";
    String ARG_STATION = "ArgStation";
    String ARG_TWO_PANE = "ArgTwoPane";
    String ARG_PLAYBACK = "ArgPlayback";

    /* PREFS */
    String PREF_STATION_URL = "prefStationUrl";
    String PREF_STATION_URL_LAST = "prefStationUrlLast";
    String PREF_STATION_URI_SELECTED = "prefStationUriSelected";
    String PREF_TIMER_RUNNING = "prefTimerRunning";
    String PREF_TWO_PANE = "prefTwoPane";

    /* RESULTS */
    String RESULT_FETCH_ERROR = "FETCH_ERROR";
    String RESULT_PLAYLIST_TYPE = "PLAYLIST_TYPE";
    String RESULT_STREAM_TYPE = "STREAM_TYPE";
    String RESULT_FILE_CONTENT = "FILE_CONTENT";

    /* DOWNLOAD BUNDLE KEYS */
    String KEY_DOWNLOAD_STATION = "DOWNLOAD_STATION";
    String KEY_DOWNLOAD_STATION_IMAGE = "DOWNLOAD_STATION_IMAGE";

    /* SUPPORTED AUDIO FILE CONTENT TYPES */
    String[] CONTENT_TYPES_MPEG = {"audio/mpeg"};
    String[] CONTENT_TYPES_OGG = {"audio/ogg", "application/ogg", "audio/opus"};
    String[] CONTENT_TYPES_AAC = {"audio/aac", "audio/aacp"};

    /* SUPPORTED PLAYLIST CONTENT TYPES */
    String[] CONTENT_TYPES_PLS = {"audio/x-scpls"};
    String[] CONTENT_TYPES_M3U = {"audio/mpegurl", "application/x-mpegurl", "application/x-mpegURL", "audio/x-mpegurl", "application/x-mpegurl"};
    String[] CONTENT_TYPES_HLS = {"application/vnd.apple.mpegurl", "application/vnd.apple.mpegurl.audio"};

    /* PERMISSION REQUESTS */
    int PERMISSION_REQUEST_IMAGE_PICKER_READ_EXTERNAL_STORAGE = 1;
    int PERMISSION_REQUEST_STATION_FETCHER_READ_EXTERNAL_STORAGE = 2;

    /* INFOSHEET CONTENTS */
    int INFOSHEET_CONTENT_ABOUT = 1;
    int INFOSHEET_CONTENT_HOWTO = 2;

    /* FRAGMENT TAGS */
    String LIST_FRAGMENT_TAG = "CFTAG";
    String PLAYER_FRAGMENT_TAG = "PFTAG";
    String INFOSHEET_FRAGMENT_TAG = "PFTAG";

    /* PLAYBACK STATES */
    int PLAYBACK_STATE_LOADING_STATION = 1;
    int PLAYBACK_STATE_STARTED = 2;
    int PLAYBACK_STATE_STOPPED = 3;

    /* VIEW HOLDER UPDATE TYPES  */
    int HOLDER_UPDATE_NAME = 1;
    int HOLDER_UPDATE_PLAYBACK_STATE = 2;
    int HOLDER_UPDATE_IMAGE = 3;

    /* SAVED INSTANCE STATE */
    String INSTANCE_STATION = "instanceStation";

    /* CLIPBOARD COPY TYPES */
    int COPY_STATION_ALL = 1;
    int COPY_STATION_METADATA = 2;
    int COPY_STREAM_URL = 3;

    /* CONNECTION TYPES */
    int CONNECTION_TYPE_HLS = 1;
    int CONNECTION_TYPE_OTHER = 2;
    int CONNECTION_TYPE_ERROR = 3;

    /* MISC */
    long FIFTEEN_MINUTES = 900000; // equals 15 minutes
    int PLAYER_SERVICE_NOTIFICATION_ID = 1;
    int REQUEST_LOAD_IMAGE = 1;
    String NOTIFICATION_CHANEL_ID_PLAYBACK_CHANNEL ="notificationChannelIdPlaybackChannel";
    String SHOUTCAST_STREAM_TITLE_HEADER = "StreamTitle";

}
