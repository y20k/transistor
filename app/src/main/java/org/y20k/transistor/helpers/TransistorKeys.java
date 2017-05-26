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
    String ACTION_CHANGE_VIEW_SELECTION = "org.y20k.transistor.action.CHANGE_VIEW_SELECTION";
    String ACTION_COLLECTION_CHANGED = "org.y20k.transistor.action.COLLECTION_CHANGED";
    String ACTION_IMAGE_CHANGE_REQUESTED = "org.y20k.transistor.action.IMAGE_CHANGE_REQUESTED";
    String ACTION_METADATA_CHANGED = "org.y20k.transistor.action.METADATA_CHANGED";
    String ACTION_SHOW_PLAYER = "org.y20k.transistor.action.SHOW_PLAYER";
    String ACTION_TIMER_RUNNING = "org.y20k.transistor.action.TIMER_RUNNING";
    String ACTION_TIMER_START = "org.y20k.transistor.action.TIMER_START";
    String ACTION_TIMER_STOP = "org.y20k.transistor.action.TIMER_STOP";

    /* EXTRAS */
    String EXTRA_COLLECTION_CHANGE = "COLLECTION_CHANGE";
    String EXTRA_INFOSHEET_TITLE = "INFOSHEET_TITLE";
    String EXTRA_INFOSHEET_CONTENT = "INFOSHEET_CONTENT";
    String EXTRA_METADATA = "METADATA";
    String EXTRA_PLAYBACK_STATE_CHANGE = "PLAYBACK_STATE_CHANGE";
    String EXTRA_PLAYBACK_STATE = "PLAYBACK_STATE";
    String EXTRA_STATION = "STATION";
    String EXTRA_STATION_ID = "STATION_ID";
    String EXTRA_LAST_STATION = "LAST_STATION";
    String EXTRA_STATION_NEW_NAME = "STATION_NEW_NAME";
    String EXTRA_STREAM_URI = "STREAM_URI";
    String EXTRA_TIMER_DURATION = "TIMER_DURATION";
    String EXTRA_TIMER_REMAINING = "TIMER_REMAINING";

    /* ARGS */
    String ARG_STATION = "ArgStation";
    String ARG_STATION_ID = "ArgStationID";
    String ARG_STREAM_URI = "ArgStreamUri";
    String ARG_TWO_PANE = "ArgTwoPane";
    String ARG_PLAYBACK = "ArgPlayback";

    /* PREFS */
    String PREF_PLAYBACK = "prefPlayback";
    String PREF_STATION_LOADING = "prefStationLoading";
    String PREF_STATION_ID_CURRENTLY_PLAYING = "prefStationIDCurrentlyPlaying";
    String PREF_STATION_ID_LAST = "prefStationIDLast";
    String PREF_STATION_ID_SELECTED = "prefStationIDSelected";
    String PREF_STATION_METADATA = "prefStationMetadata";
    String PREF_STATION_MIME_TYPE = "prefStationMimeType";
    String PREF_STATION_CHANNEL_COUNT = "prefStationChannelCount";
    String PREF_STATION_SAMPLE_RATE = "prefStationSampleRate";
    String PREF_STATION_BIT_RATE = "prefStationBitRate";
    String PREF_TIMER_RUNNING = "prefTimerRunning";
    String PREF_TWO_PANE = "prefTwoPane";

    /* RESULTS */
    String RESULT_FETCH_ERROR = "FETCH_ERROR";
    String RESULT_PLAYLIST_TYPE = "PLAYLIST_TYPE";
    String RESULT_STREAM_TYPE = "STREAM_TYPE";
    String RESULT_FILE_CONTENT = "FILE_CONTENT";

    /* MISC */
    int INFOSHEET_CONTENT_ABOUT = 1;
    int INFOSHEET_CONTENT_HOWTO = 2;
    int PERMISSION_REQUEST_IMAGE_PICKER_READ_EXTERNAL_STORAGE = 1;
    int PERMISSION_REQUEST_STATION_FETCHER_READ_EXTERNAL_STORAGE = 2;

    int PLAYER_SERVICE_NOTIFICATION_ID = 1;
    int REQUEST_LOAD_IMAGE = 1;
    int STATION_ADDED = 1;
    int STATION_RENAMED = 2;
    int STATION_DELETED = 3;
    int PLAYBACK_LOADING_STATION = 1;
    int PLAYBACK_STARTED = 2;
    int PLAYBACK_STOPPED = 3;
    String INSTANCE_LIST_STATE = "instanceListState";
    String INSTANCE_STATION = "instanceStation";
    String INSTANCE_STATION_ID = "instanceStationID";
    String INSTANCE_PLAYBACK = "instancePlayback";
    String PLAYER_FRAGMENT_TAG = "PFTAG";
    String SHOUTCAST_STREAM_TITLE_HEADER = "StreamTitle";

    int COPY_STATION_ALL = 1;
    int COPY_STATION_METADATA = 2;
    int COPY_STREAM_URL = 3;

}
