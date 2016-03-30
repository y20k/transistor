/**
 * CollectionAdapter.java
 * Implements the CollectionAdapter class
 * A CollectionAdapter is a custom adapter for a listview
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-16 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.y20k.transistor.PlayerActivity;
import org.y20k.transistor.PlayerActivityFragment;
import org.y20k.transistor.PlayerService;
import org.y20k.transistor.R;
import org.y20k.transistor.core.Collection;

import java.util.LinkedList;


/**
 * CollectionAdapter class
 */
public final class CollectionAdapter  extends RecyclerView.Adapter<CollectionAdapterViewHolder> {

    /* Define log tag */
    private static final String LOG_TAG = CollectionAdapter.class.getSimpleName();


    /* Keys */
    private static final String ACTION_PLAYBACK_STOPPED = "org.y20k.transistor.action.PLAYBACK_STOPPED";
    private static final String ACTION_SHOW_PLAYER = "org.y20k.transistor.action.SHOW_PLAYER";
    private static final String EXTRA_STATION_ID = "EXTRA_STATION_ID";
    private static final String ARG_STATION_ID = "ArgStationID";
    private static final String ARG_TWO_PANE = "ArgTwoPane";
    private static final String PREF_TWO_PANE = "prefTwoPane";
    private static final String PREF_STATION_ID_CURRENT = "prefStationIDCurrent";
    private static final String PREF_STATION_ID_LAST = "prefStationIDLast";
    private static final String PREF_PLAYBACK = "prefPlayback";
    private static final String PLAYERFRAGMENT_TAG = "PFTAG";


    /* Main class variables */
    private final LinkedList<String> mStationNames;
    private final LinkedList<Uri> mStationUris;
    private final LinkedList<Bitmap> mStationImages;
    private final Activity mActivity;
    private PlayerService mPlayerService;
    private Collection mCollection;
    private CollectionChangedListener mCollectionChangedListener;
//    private ClickListener mClickListener;
    private View mSelectedView;
    private boolean mPlayback;
    private int mStationIDCurrent;
    private int mStationIDLast;
    private int mStationIDSelected;
    private boolean mTwoPane;


    /* Interface for custom listener */
    public interface CollectionChangedListener {
        void collectionChanged();
    }


    /* Constructor */
    public CollectionAdapter(Activity activity, LinkedList<String> stationNames,  LinkedList<Uri> stationUris, LinkedList<Bitmap> stationImage) {
        // set main variables
        mActivity = activity;
        mStationNames = stationNames;
        mStationUris = stationUris;
        mStationImages = stationImage;
        mCollection = null;
        mCollectionChangedListener = null;
        mSelectedView = null;
        mStationIDSelected = 0;

        // initiate player service
        mPlayerService = new PlayerService();

        // load state
        loadAppState(mActivity);

        // broadcast receiver: player service stopped playback
        BroadcastReceiver playbackStoppedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadAppState(mActivity);
            }
        };
        IntentFilter playbackStoppedIntentFilter = new IntentFilter(ACTION_PLAYBACK_STOPPED);
        LocalBroadcastManager.getInstance(mActivity.getApplication()).registerReceiver(playbackStoppedReceiver, playbackStoppedIntentFilter);

    }


    @Override
    public CollectionAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // get view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_collection, parent, false);

        // put view into holder and return
        CollectionAdapterViewHolder vh = new CollectionAdapterViewHolder(v);
        return vh;
    }


    @Override
    public void onBindViewHolder(CollectionAdapterViewHolder holder, final int position) {

        if (mTwoPane && mSelectedView == null) {
            // markViewSelected(holder.getListItemLayout());
        } else if (mTwoPane) {
            // mSelectedView.setSelected(true);
        }

        // set station image
        holder.getStationImageView().setImageBitmap(mStationImages.get(position));

        // set station name
        holder.getStationNameView().setText(mStationNames.get(position));

        // set playback indicator - in phone view only
        if (!mTwoPane && mPlayback && mStationIDCurrent == position) {
            holder.getPlaybackIndicator().setVisibility(View.VISIBLE);
        } else {
            holder.getPlaybackIndicator().setVisibility(View.GONE);
        }

        // attach three dots menu - in phone view only
        if (!mTwoPane) {
            holder.getStationMenuView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    StationContextMenu menu = new StationContextMenu();
                    menu.initialize(mActivity, mCollection, view, position);
                    menu.show();
                }
            });
        } else {
            holder.getStationMenuView().setVisibility(View.GONE);
        }

        // attach click listener
        holder.setClickListener(new CollectionAdapterViewHolder.ClickListener() {
            @Override
            public void onClick(View view, int pos, boolean isLongClick) {
                mStationIDSelected = pos;
                if (isLongClick && !mTwoPane) {
                    // long click in phone mode
                    handleLongClick(pos);
                } else if (!isLongClick && !mTwoPane) {
                    // click in phone mode
                    handleSingleClick(pos);
                } else {
                    // click in tablet mode
                    handleSingleClick(pos);
                    markViewSelected(view);
                }
            }
        });

    }


    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public int getItemCount() {
        return mStationNames.size();
    }


    /* Refreshes app state info */
    public void refresh() {
        loadAppState(mActivity);
    }


    /* Handles click on list item */
    private void handleSingleClick(int position) {

        if (mTwoPane) {
            Bundle args = new Bundle();
            args.putInt(ARG_STATION_ID, position);
            args.putBoolean(ARG_TWO_PANE, mTwoPane);

            PlayerActivityFragment playerActivityFragment = new PlayerActivityFragment();
            playerActivityFragment.setArguments(args);
            mActivity.getFragmentManager().beginTransaction()
                    .replace(R.id.player_container, playerActivityFragment, PLAYERFRAGMENT_TAG)
                    .commit();
        } else {
            // add id of station to intent and start activity
            Intent intent = new Intent(mActivity, PlayerActivity.class);
            intent.setAction(ACTION_SHOW_PLAYER);
            intent.putExtra(EXTRA_STATION_ID, position);
            mActivity.startActivity(intent);
        }
    }


    /* Handles long click on list item */
    private void handleLongClick(int position) {

        // get current playback state
        loadAppState(mActivity);

        if (mPlayback && position == mStationIDCurrent ) {
            // stop playback service
            mPlayerService.startActionStop(mActivity);

            // set playback state
            mStationIDLast = mStationIDCurrent;
            mPlayback = false;

            // inform user
            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_long_press_playback_stopped), Toast.LENGTH_LONG).show();
        } else {
            // start playback service
            String stationName = mCollection.getStations().get(position).getStationName();
            String streamUri = mCollection.getStations().get(position).getStreamUri().toString();
            mPlayerService.startActionPlay(mActivity, streamUri, stationName, position);

            // set playback state
            mStationIDLast = mStationIDCurrent;
            mStationIDCurrent = position;
            mPlayback = true;

            // inform user
            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_long_press_playback_started), Toast.LENGTH_LONG).show();
        }

        // vibrate 50 milliseconds
        Vibrator v = (Vibrator) mActivity.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(50);

        // notify MainActivityFragment
        if (mCollectionChangedListener != null) {
            mCollectionChangedListener.collectionChanged();
        }

        // Save station name and ID
        saveAppState(mActivity);
    }


    /* Sets given view selected */
    private void markViewSelected(View view) {
        if (mSelectedView != null) {
            // set previously selected false
            mSelectedView.setSelected(false);
        }
        // store selected view
        mSelectedView = view;
        // set selected view true
        mSelectedView.setSelected(true);
    }


    /* Loads app state from preferences */
    private void loadAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        mTwoPane = settings.getBoolean(PREF_TWO_PANE, false);
        mStationIDCurrent = settings.getInt(PREF_STATION_ID_CURRENT, -1);
        mStationIDLast = settings.getInt(PREF_STATION_ID_LAST, -1);
        mPlayback = settings.getBoolean(PREF_PLAYBACK, false);
        Log.v(LOG_TAG, "Loading state.");
    }


    /* Saves app state to SharedPreferences */
    private void saveAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(PREF_STATION_ID_CURRENT, mStationIDCurrent);
        editor.putInt(PREF_STATION_ID_LAST, mStationIDLast);
        editor.putBoolean(PREF_PLAYBACK, mPlayback);
        editor.apply();
        Log.v(LOG_TAG, "Saving state.");
    }


    /* Setter for collection */
    public void setCollection(Collection collection) {
        mCollection = collection;
    }


    /* Resets selection */
    public void resetSelection() {
        if (mSelectedView != null) {
            mSelectedView.setSelected(false);
        }
    }

    /* Setter for ID of currently selected station */
    public void setStationIDSelected(int stationIDSelected) {
        mStationIDSelected = stationIDSelected;
    }


    /* Getter for ID of currently selected station */
    public int getStationIDSelected() {
        return mStationIDSelected;
    }
}