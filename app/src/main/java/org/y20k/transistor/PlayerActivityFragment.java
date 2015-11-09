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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.y20k.transistor.core.Collection;
import org.y20k.transistor.helpers.DialogDelete;
import org.y20k.transistor.helpers.DialogRename;
import org.y20k.transistor.helpers.ImageHelper;

import java.io.File;


/**
 * PlayerActivityFragment class
 */
public class PlayerActivityFragment extends Fragment {

    /* Define log tag */
    private static final String LOG_TAG = PlayerActivityFragment.class.getSimpleName();

    /* Keys */
    private static final String STREAM_URL = "streamURL";
    private static final String STATION_NAME = "stationName";
    private static final String STATION_ID = "stationID";
    private static final String STATION_ID_CURRENT = "stationIDCurrent";
    private static final String STATION_ID_LAST = "stationIDLast";
    private static final String PLAYBACK = "playback";
    private static final String ACTION_PLAYBACK_STOPPED = "org.y20k.transistor.action.PLAYBACK_STOPPED";


    /* Main class variables */
    private Activity mActivity;
    private String mStationName;
    private String mStreamURL;
    private TextView mStationNameView;
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

        // get activity
        mActivity = getActivity();

        // get station name, URL and id from intent
        Intent intent = mActivity.getIntent();
        mStationID = intent.getIntExtra(STATION_ID, -1);
        mStationName = intent.getStringExtra(STATION_NAME);
        mStreamURL = intent.getStringExtra(STREAM_URL);

        // load playback state from preferences
        loadPlaybackState(mActivity);

        if (mStationID == -1) {
            // set station ID
            mStationID = mStationIDCurrent;
        }

        try {
            // get collection folder from external storage
            File folder = new File(mActivity.getExternalFilesDir("Collection").toString());
            // load collection
            mCollection = new Collection(folder);
        } catch (NullPointerException e) {
            // notify user and log exception
            Toast.makeText(getActivity(), R.string.toastalert_no_external_storage, Toast.LENGTH_LONG).show();
            Log.e(LOG_TAG, "Unable to access external storage.");
            // finish activity
            getActivity().finish();
        }

        // get URL and name for stream
        mStreamURL = mCollection.getStations().get(mStationID).getStreamURL().toString();
        mStationName = mCollection.getStations().get(mStationID).getStationName();

        // fragment has options menu
        setHasOptionsMenu(true);
    }


    @Override
    public void onResume() {
        super.onResume();

        // TODO check connectivity

        // set up button symbol and playback indicator
        setVisualState();

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // set image for station
        Bitmap stationImageSmall;
        ImageHelper imageHelper;
        if (mCollection.getStations().get(mStationID).getStationImageFile().exists()) {
            stationImageSmall = BitmapFactory.decodeFile(mCollection.getStations().get(mStationID).getStationImageFile().toString());
        } else {
            stationImageSmall = BitmapFactory.decodeResource(getResources(), R.drawable.ic_notesymbol);
        }
        imageHelper = new ImageHelper(stationImageSmall, mActivity);
        imageHelper.setBackgroundColor(R.color.transistor_grey_lighter);
        Bitmap stationImage = imageHelper.createCircularFramedImage(192);

        // initiate playback service
        mPlayerService = new PlayerService();

        // inflate rootview from xml
        View rootView = inflater.inflate(R.layout.fragment_player, container, false);

        // find views for station name and image and playback indicator
        mStationNameView = (TextView) rootView.findViewById(R.id.player_textview_stationname);
        ImageView stationeImageView = (ImageView) rootView.findViewById(R.id.player_imageview_station_icon);
        mPlaybackIndicator = (ImageView) rootView.findViewById(R.id.player_playback_indicator);

        // set station image
        if (stationImage != null) {
            stationeImageView.setImageBitmap(stationImage);
        }

        // set text view to station name
        mStationNameView.setText(mStationName);

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
                    changeVisualState(mActivity);
                    // start player
                    mPlayerService.startActionPlay(mActivity, mStreamURL, mStationName);
                    Log.v(LOG_TAG, "Starting player service.");

                }
                // playback active - stop playback
                else {
                    // set playback false
                    mPlayback = false;
                    // rotate playback button
                    changeVisualState(mActivity);
                    // stop player
                    Log.v(LOG_TAG, "Stopping player service.");
                    mPlayerService.startActionStop(mActivity);
                }

                // save state of playback in settings store
                savePlaybackState(mActivity);
            }
        });

        // broadcast receiver: player service stopped playback
        BroadcastReceiver playbackStoppedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // set playback false
                mPlayback = false;
                // rotate playback button
                changeVisualState(context);
                // save state of playback to settings
                savePlaybackState(context);
            }
        };
        IntentFilter intentFilter = new IntentFilter(ACTION_PLAYBACK_STOPPED);
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(playbackStoppedReceiver, intentFilter);

        return rootView;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            // CASE RENAME
            case R.id.menu_rename:
                // construct rename dialog
                final DialogRename dialogRename = new DialogRename(mActivity, mCollection, mStationName, mStationID);
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
                mPlayerService.startActionStop(mActivity);
                // construct delete dialog
                DialogDelete dialogDelete = new DialogDelete(mActivity, mCollection, mStationID);
                dialogDelete.setStationDeletedListener(new DialogDelete.StationDeletedListener() {
                    @Override
                    public void stationDeleted() {
                        // start main activity
                        Intent intent = new Intent(mActivity, MainActivity.class);
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
    private void changeVisualState(Context context) {
        // get rotate animation from xml
        Animation rotate;
        if (mPlayback) {
            // if playback has been started get start animation
            rotate = AnimationUtils.loadAnimation(context, R.anim.rotate_clockwise_slow);
        } else {
            // if playback has been stopped get stop animation
            rotate = AnimationUtils.loadAnimation(context, R.anim.rotate_counterclockwise_fast);
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
    private void savePlaybackState(Context context) {
        // playback started
        if (mPlayback) {
            mStationIDLast = mStationIDCurrent;
            mStationIDCurrent = mStationID;

        }
        // playback stopped
        else {
            mStationIDLast = mStationIDCurrent;
        }

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(STATION_ID_CURRENT, mStationIDCurrent);
        editor.putInt(STATION_ID_LAST, mStationIDLast);
        editor.putBoolean(PLAYBACK, mPlayback);
        editor.apply();

    }


    /* Loads playback state from preferences */
    private void loadPlaybackState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        mStationIDCurrent = settings.getInt(STATION_ID_CURRENT, -1);
        mStationIDLast = settings.getInt(STATION_ID_LAST, -1);
        mPlayback = settings.getBoolean(PLAYBACK, false);
    }

}


/**
 * TODO
 * - Sleep Timer
 */