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
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
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
import org.y20k.transistor.helpers.DialogDelete;
import org.y20k.transistor.helpers.DialogRename;
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
    public static final String STREAM_URL = "streamURL";
    public static final String STATION_NAME = "stationName";
    public static final String STATION_ID = "stationID";
    public static final String STATION_ID_CURRENT = "stationIDCurrent";
    public static final String STATION_ID_LAST = "stationIDLast";
    public static final String PLAYBACK = "playback";
    private static final String ACTION_PLAYBACK_STOPPED = "org.y20k.transistor.action.PLAYBACK_STOPPED";

    /* Main class variables */
    private Bitmap mStationImage;
    private String mStatiomName;
    private String mStreamURL;
    private TextView mStationNameView;
    private ImageView mStationeImageView;
    private ImageButton mPlaybackButton;
    private ImageView mPlaybackIndicator;
    private int mStationID;
    private int mStationIDCurrent;
    private int mStationIDLast;
    private boolean mPlayback;
    private Collection mCollection;
    private PlayerService mPlayerService;


    /* Constructor (default) */
    public PlayerActivityFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get station name, URL and id from intent
        Intent intent = getActivity().getIntent();
        mStationID = intent.getIntExtra(STATION_ID, -1);
        mStatiomName = intent.getStringExtra(STATION_NAME);
        mStreamURL = intent.getStringExtra(STREAM_URL);

        // load collection
        File folder = new File(getActivity().getExternalFilesDir("Collection").toString());
        mCollection = new Collection(folder);

        // get URL and name for stream
        mStreamURL = mCollection.getStations().get(mStationID).getStreamURL().toString();
        mStatiomName = mCollection.getStations().get(mStationID).getStationName();

        // fragment has options menu
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        // restore player state from preferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mStationIDCurrent = settings.getInt(STATION_ID_CURRENT, -1);
        mStationIDLast = settings.getInt(STATION_ID_LAST, -1);
        mPlayback = settings.getBoolean(PLAYBACK, false);

        // set up button symbol and playback indicator
        setVisualState();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // set image for station
        Bitmap stationImageSmall = null;
        ImageHelper imageHelper = null;
        if (mCollection.getStations().get(mStationID).getStationImageFile().exists()) {
            stationImageSmall = BitmapFactory.decodeFile(mCollection.getStations().get(mStationID).getStationImageFile().toString());
        }
        else {
            stationImageSmall = BitmapFactory.decodeResource(getResources(), R.drawable.ic_notesymbol);
        }
        imageHelper = new ImageHelper(stationImageSmall, getActivity());
        imageHelper.setBackgroundColor(R.color.transistor_grey_lighter);
        mStationImage = imageHelper.createCircularFramedImage(192);

        // initiate playback service
        mPlayerService = new PlayerService();

        // inflate rootview from xml
        View rootView = inflater.inflate(R.layout.fragment_player, container, false);

        // find views for station name and image and playback indicator
        mStationNameView = (TextView) rootView.findViewById(R.id.player_textview_stationname);
        mStationeImageView = (ImageView) rootView.findViewById(R.id.player_imageview_station_icon);
        mPlaybackIndicator = (ImageView) rootView.findViewById(R.id.player_playback_indicator);

         // set station image
        if (mStationImage != null) {
            mStationeImageView.setImageBitmap(mStationImage);
        }

        // set text view to station name
        mStationNameView.setText(mStatiomName);

        // construct image button
        mPlaybackButton = (ImageButton) rootView.findViewById(R.id.player_playback_button);

        // set listener to playback button
        mPlaybackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // playback stopped or new station - start playback
                if (!mPlayback || mStationID != mStationIDCurrent) {
                    // set playback true
                    mPlayback = true;
                    // rotate playback button
                    changeVisualState();
                    // start player
                    mPlayerService.startActionPlay(getActivity(), mStreamURL, mStatiomName);
                }
                // playback active - stop playback
                else {
                    // set playback false
                    mPlayback = false;
                    // rotate playback button
                    changeVisualState();
                    // stop player
                    mPlayerService.startActionStop(getActivity());
                }

                // save state of playback in settings store
                savePlaybackState ();
            }
        });

        // broadcast receiver: player service stopped playback
        BroadcastReceiver playbackStoppedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // set playback false
                mPlayback = false;
                // rotate playback button
                changeVisualState();
                // save state of playback to settings
                savePlaybackState();
            }
        };
        IntentFilter intentFilter = new IntentFilter(ACTION_PLAYBACK_STOPPED);
        getActivity().registerReceiver(playbackStoppedReceiver, intentFilter);

        return rootView;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            // CASE RENAME
            case R.id.menu_rename:
                // construct rename dialog
                final DialogRename dialogRename = new DialogRename(getActivity(), mCollection, mStatiomName, mStationID);
                dialogRename.setStationRenamedListener(new DialogRename.StationRenamedListener() {
                    @Override
                    public void stationRenamed() {
                        mStationNameView.setText(dialogRename.getStationName());
                    }
                });
                // run dialog
                dialogRename.show();
                return true;

            // CASE DELETE
            case R.id.menu_delete:
                // stop playback
                mPlayerService.startActionStop(getActivity());
                // construct delete dialog
                DialogDelete dialogDelete = new DialogDelete(getActivity(), mCollection, mStationID);
                dialogDelete.setStationDeletedListener(new DialogDelete.StationDeletedListener() {
                    @Override
                    public void stationDeleted() {
                        // start main activity
                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        startActivity(intent);
                    }
                });
                // run dialog
                dialogDelete.show();
                return true;

            // CASE DEFAULT
            default:
                return super.onOptionsItemSelected(item);
        }

    }

//    @Override
//    public void onPause() {
//        super.onPause();
//        savePlaybackState();
//    }


    /* Animate button and then set visual state */
    private void changeVisualState() {
        // get rotate animation from xml
        Animation rotate;
        if (mPlayback){
            // if playback has been started get start animation
            rotate = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate_clockwise_slow);
        } else {
            // if playback has been stopped get stop animation
            rotate = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate_counterclockwise_fast);
        }

        // attach listener for animation end
        rotate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // set up button symbol and playback indicator afterwards
                setVisualState();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        // start animation of button
        mPlaybackButton.startAnimation(rotate);

    }


    /* Set button symbol and playback indicator */
    private void setVisualState() {

        // this station is running
        if (mPlayback && mStationID == mStationIDCurrent) {
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


    /* Save station name and ID and playback state to SharedPreferences */
    private void savePlaybackState () {
        // playback started
        if (mPlayback) {
            mStationIDLast = mStationIDCurrent;
            mStationIDCurrent = mStationID;

        }
        // playback stopped
        else {
            mStationIDLast = mStationIDCurrent;
            mStationIDCurrent = -1;
        }

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(STATION_ID_CURRENT, mStationIDCurrent);
        editor.putInt(STATION_ID_LAST, mStationIDLast);
        editor.putBoolean(PLAYBACK, mPlayback);
        editor.commit();

    }

}

