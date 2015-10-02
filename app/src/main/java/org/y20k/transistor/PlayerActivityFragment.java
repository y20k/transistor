/**
 * PlayerActivityFragment.java
 * Implements the main fragment of the player activity
 * This fragment is a detail view with the ability to start and stop playback
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.y20k.transistor.core.Collection;
import org.y20k.transistor.helpers.CollectionAdapter;
import org.y20k.transistor.helpers.ImageHelper;

import java.io.File;
import java.util.LinkedList;


/**
 * PlayerActivityFragment class
 */
public class PlayerActivityFragment extends Fragment {

    /* Define log tag */
    public final String LOG_TAG = PlayerActivityFragment.class.getSimpleName();

    /* Keys */
    public static final String STATION_ID_CURRENT = "stationIDCurrent";
    public static final String STATION_ID_LAST = "stationIDLast";
    public static final String PLAYBACK = "playback";
    private static final String ACTION_PLAYBACK_STOPPED = "org.y20k.transistor.action.PLAYBACK_STOPPED";

    /* Main class variables */
    private Bitmap mStationImage;
    private String mStatiomName;
    private String mStreamURL;
    private Collection mCollection;
    private CollectionAdapter mCollectionAdapter;
    private LinkedList<String> mStationNames;
    private LinkedList<Bitmap> mStationImages;
    private TextView mStationNameView;
    private ImageView mStationeImageView;
    private ImageButton mPlaybackButton;
    private ImageView mPlaybackIndicator;
    private int mStationIDCurrent;
    private int mStationIDLast;
    private boolean mPlayback;
    private PlayerService mPlayerService;
    private ProgressBar bufferProgressBar;


    /* Constructor (default) */
    public PlayerActivityFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set up variables
        mCollectionAdapter = null;
        mStatiomName = null;
        mStreamURL = null;
        mStationIDCurrent = -1;
        mStationIDLast = -1;

        // create default station image
        Bitmap stationImageSmall = BitmapFactory.decodeResource(getResources(), R.drawable.ic_notesymbol);
        ImageHelper imageHelper = new ImageHelper(stationImageSmall, getActivity());
        imageHelper.setBackgroundColor(R.color.transistor_grey_lighter);
        mStationImage = imageHelper.createCircularFramedImage(192);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // restore player state from preferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mStationIDCurrent = settings.getInt(STATION_ID_CURRENT, -1);
        mStationIDLast = settings.getInt(STATION_ID_LAST, -1);
        mPlayback = settings.getBoolean(PLAYBACK, false);

        // initiate playback service
        mPlayerService = new PlayerService();

        // inflate rootview from xml
        View rootView = inflater.inflate(R.layout.fragment_player, container, false);

        // find views for station name and image and playback indicator
        mStationNameView = (TextView) rootView.findViewById(R.id.player_textview_stationname);
        mStationeImageView = (ImageView) rootView.findViewById(R.id.player_imageview_station_icon);
        mPlaybackIndicator = (ImageView) rootView.findViewById(R.id.player_playback_indicator);

        // get intent from context
        Intent intent = getActivity().getIntent();

        // TODO get URL from intent replace EXTRA_TEXT with custom key

        // set station name
        if (mStatiomName != null && intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
            // pull station name string from intent
            mStatiomName = intent.getStringExtra(Intent.EXTRA_TEXT);
            // set text view to station name string
            mStationNameView.setText(mStatiomName);
        }

        // set station image
        if (mStationImage != null) {
            mStationeImageView.setImageBitmap(mStationImage);
        }

        // construct image button
        mPlaybackButton = (ImageButton) rootView.findViewById(R.id.player_playback_button);
        // TODO Description
        setVisualState();

        // set listener to playback button
        mPlaybackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // playback stopped - start playback
                if (!mPlayback) {
                    mPlayback = true;
                    // rotate playback button
                    changeVisualState();
                    // start player
                    mPlayerService.startActionPlay(getActivity(), mStreamURL, mStatiomName);
                }
                // playback active - stop playback
                else {
                    mPlayback = false;
                    // rotate playback button
                    changeVisualState();
                    // stop player
                    mPlayerService.startActionStop(getActivity());
                }
            }
        });

        // broadcast receiver: player service stopped playback
        BroadcastReceiver playbackStoppedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mPlayback = false;
                // rotate playback button
                changeVisualState();

            }
        };
        IntentFilter intentFilter = new IntentFilter(ACTION_PLAYBACK_STOPPED);
        getActivity().registerReceiver(playbackStoppedReceiver, intentFilter);

        // get radio stations and fill collection in background
        GetStations getStations = new GetStations();
        getStations.execute();

        return rootView;
    }


    @Override
    public void onStop() {
        super.onStop();
        saveState();
    }

    private void saveState() {
        // store player state in shared preferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PLAYBACK, mPlayback);
        editor.commit();
    }


    private void changeVisualState() {

        // get rotate animation from xml
        Animation rotate = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate);

        // attach listner for animation end
        rotate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // TODO Description
                setVisualState();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        // start animation of button
        mPlaybackButton.startAnimation(rotate);

    }


    private void setVisualState() {
        // playback running
        if (mPlayback) {
            // change playback button image to stop
            mPlaybackButton.setImageResource(R.drawable.smbl_stop);
            // change playback indicator
            mPlaybackIndicator.setBackgroundResource(R.drawable.ic_playback_indicator_started_24dp);
        }
        // playback stopped
        else {
            // change playback button image to play
            mPlaybackButton.setImageResource(R.drawable.smbl_play);
            // change playback indicator
            mPlaybackIndicator.setBackgroundResource(R.drawable.ic_playback_indicator_stopped_24dp);
        }
    }



    /**
     * Inner class: get radio stations from storage (using background thread)
     */
    public class GetStations extends AsyncTask<Void, Integer, Collection> {

        /* Define log tag */
        public final String LOG_TAG = GetStations.class.getSimpleName();

        /* Main class variables */
        private File folder = null;

        /* Constructor (empty) */
        public GetStations() {
        }

        /* Background thread: reads m3u files from storage */
        @Override
        public Collection doInBackground(Void... params) {
            folder = new File(getActivity().getExternalFilesDir("Collection").toString());
            Log.v(LOG_TAG, "Create mCollection of stations in background (folder:" + folder.toString() + ").");
            publishProgress(folder.listFiles().length);

            mCollection = new Collection(folder);

            return mCollection;
        }

        /* Main thread: Report progress update of background task */
        @Override
        protected void onProgressUpdate(Integer... progress) {
            Log.v(LOG_TAG, "Folder contains " + progress[0].toString() + " files.");
        }

        /* Main thread: Fills array adapter for list view */
        @Override
        protected void onPostExecute(Collection stations) {

            // set image for station
            if (mCollection.getStations().get(mStationIDCurrent).getStationImageFile().exists()) {
                Bitmap stationImageSmall = null;
                ImageHelper imageHelper = null;
                // create image
                stationImageSmall = BitmapFactory.decodeFile(mCollection.getStations().get(mStationIDCurrent).getStationImageFile().toString());
                imageHelper = new ImageHelper(stationImageSmall, getActivity());
                imageHelper.setBackgroundColor(R.color.transistor_grey_lighter);

                mStationImage = imageHelper.createCircularFramedImage(192);
                // set image
                mStationeImageView.setImageBitmap(mStationImage);
            }

            // set name for station
            String newStationName = mCollection.getStations().get(mStationIDCurrent).getStationName();
            if (newStationName != mStatiomName) {
                mStationNameView.setText(newStationName);
            }

            mStreamURL = mCollection.getStations().get(mStationIDCurrent).getStreamURL().toString();
            mStatiomName = mCollection.getStations().get(mStationIDCurrent).getStationName();
        }

    }
    /**
     * End of inner class
     */

}

