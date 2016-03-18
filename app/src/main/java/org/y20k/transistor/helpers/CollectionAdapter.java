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
    private static final String TWOPANE = "twopane";
    private static final String PLAYERFRAGMENT_TAG = "PFTAG";
    private static final String STATION_ID_CURRENT = "stationIDCurrent";
    private static final String STATION_ID_LAST = "stationIDLast";
    private static final String PLAYBACK = "playback";
    private static final String STATION_ID = "stationID";
    private static final String ACTION_PLAYBACK_STOPPED = "org.y20k.transistor.action.PLAYBACK_STOPPED";



    /* Main class variables */
    private final LinkedList<String> mStationNames;
    private final LinkedList<Uri> mStationUris;
    private final LinkedList<Bitmap> mStationImages;
    private final Activity mActivity;
    private PlayerService mPlayerService;
    private Collection mCollection;
    private CollectionChangedListener mCollectionChangedListener;
    private ClickListener mClickListener;
    private View mSelectedView;
    private boolean mPlayback;
    private int mStationIDCurrent;
    private int mStationIDLast;
    private boolean mTwoPane;


    /* Interface for custom listener */
    public interface CollectionChangedListener {
        void collectionChanged();
    }


    /* Interface for handling clicks - both normal and long ones. */
    public interface ClickListener {
        public void onClick(View v, int position, boolean isLongClick);
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

//        if (mSelectedView == null && position == mStationIDCurrent) {
//            setSelectedView(holder.getListItemLayout());
//        }

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

                    // listen for changes invoked by StationContextMenu
                    menu.setStationChangedListener(new StationContextMenu.StationChangedListener() {
                        @Override
                        public void stationChanged() {
                            // notify MainActivityFragment
                            if (mCollectionChangedListener != null) {
                                mCollectionChangedListener.collectionChanged();
                            }
                        }
                    });

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
                // long click is only available in phone mode
                if (isLongClick && !mTwoPane) {
                    handleLongClick(pos);
                } else if (!isLongClick && !mTwoPane) {
                    handleSingleClick(pos);
                } else {
                    handleSingleClick(pos);
                    setSelectedView(view);
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


    /* Handles click on list item */
    private void handleSingleClick(int position) {

        if (mTwoPane) {
            Bundle args = new Bundle();
            args.putInt(STATION_ID, position);
            args.putBoolean(TWOPANE, mTwoPane);

            PlayerActivityFragment playerActivityFragment = new PlayerActivityFragment();
            playerActivityFragment.setArguments(args);
            mActivity.getFragmentManager().beginTransaction()
                    .replace(R.id.player_container, playerActivityFragment, PLAYERFRAGMENT_TAG)
                    .commit();

            } else {
            // add id of station to intent
            Intent intent = new Intent(mActivity, PlayerActivity.class);
            intent.putExtra(STATION_ID, position);

            // start activity with intent
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
            mPlayerService.startActionPlay(mActivity, streamUri, stationName);

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
    private void setSelectedView(View view) {
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
        mTwoPane = settings.getBoolean(TWOPANE, false);
        mStationIDCurrent = settings.getInt(STATION_ID_CURRENT, -1);
        mStationIDLast = settings.getInt(STATION_ID_LAST, -1);
        mPlayback = settings.getBoolean(PLAYBACK, false);
        Log.v(LOG_TAG, "Loading state.");
    }


    /* Saves app state to SharedPreferences */
    private void saveAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(STATION_ID_CURRENT, mStationIDCurrent);
        editor.putInt(STATION_ID_LAST, mStationIDLast);
        editor.putBoolean(PLAYBACK, mPlayback);
        editor.apply();
        Log.v(LOG_TAG, "Saving state.");
    }


    /* Setter for collection */
    public void setCollection(Collection collection) {
        mCollection = collection;
    }


    /* Setter for listener. */
    public void setClickListener(ClickListener clickListener) {
        mClickListener = clickListener;
    }


    /* Setter for custom listener */
    public void setCollectionChangedListener(CollectionChangedListener collectionChangedListener) {
        mCollectionChangedListener = collectionChangedListener;
    }

}