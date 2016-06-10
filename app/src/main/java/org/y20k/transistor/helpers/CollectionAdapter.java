/**
 * CollectionAdapter.java
 * Implements the CollectionAdapter class
 * A CollectionAdapter is a custom adapter for a RecyclerView
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
import org.y20k.transistor.core.Station;

import java.util.ArrayList;


/**
 * CollectionAdapter class
 */
public final class CollectionAdapter  extends RecyclerView.Adapter<CollectionAdapterViewHolder> {

    /* Define log tag */
    private static final String LOG_TAG = CollectionAdapter.class.getSimpleName();


    /* Main class variables */
    private final ArrayList<String> mStationNames;
    private final ArrayList<Bitmap> mStationImages;
    private final Activity mActivity;
    private Collection mCollection;
    private final CollectionChangedListener mCollectionChangedListener;
    private final View mSelectedView;
    private boolean mPlayback;
    private boolean mStationLoading;
    private int mStationIDCurrent;
    private int mStationIDLast;
    private int mStationIDSelected;
    private boolean mTwoPane;


    /* Interface for custom listener */
    public interface CollectionChangedListener {
        void collectionChanged();
    }


    /* Constructor */
    public CollectionAdapter(Activity activity, Collection collection, ArrayList<String> stationNames,  ArrayList<Bitmap> stationImage) {
        // set main variables
        mActivity = activity;
        mCollection = collection;
        mStationNames = stationNames;
        mStationImages = stationImage;
        mCollectionChangedListener = null;
        mSelectedView = null;
        mStationIDSelected = 0;

        // load state
        loadAppState(mActivity); // TODO necessary?

        // broadcast receiver: player service is stopping playback
        BroadcastReceiver playbackStoppingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadAppState(mActivity);
            }
        };
        IntentFilter playbackStoppingIntentFilter = new IntentFilter(TransistorKeys.ACTION_PLAYBACK_STATE_CHANGED);
        LocalBroadcastManager.getInstance(mActivity.getApplication()).registerReceiver(playbackStoppingReceiver, playbackStoppingIntentFilter);

    }


    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        // load state
        loadAppState(mActivity);
    }


    @Override
    public CollectionAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // load state
        // loadAppState(mActivity); // TODO remove ?

        // get view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_collection, parent, false);

        // put view into holder and return
        return new CollectionAdapterViewHolder(v);
    }


    @Override
    public void onBindViewHolder(CollectionAdapterViewHolder holder, final int position) {
        // Problem synopsis: Do not treat position as fixed; only use immediately and call holder.getAdapterPosition() to look it up later

        if (mTwoPane && mStationIDSelected == position) {
            holder.getListItemLayout().setSelected(true);
        } else {
            holder.getListItemLayout().setSelected(false);
        }

        // set station image
        holder.getStationImageView().setImageBitmap(mStationImages.get(position));

        // set station name
        holder.getStationNameView().setText(mStationNames.get(position));

        // set playback indicator - in phone view only
        if (!mTwoPane && mPlayback && mStationIDCurrent == position) {
            if (mStationLoading) {
                holder.getPlaybackIndicator().setBackgroundResource(R.drawable.ic_playback_indicator_small_loading_24dp);
            } else {
                holder.getPlaybackIndicator().setBackgroundResource(R.drawable.ic_playback_indicator_small_started_24dp);
            }
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
                saveAppState(mActivity);
                Log.v(LOG_TAG, "Selected station (ID): " + mStationIDSelected);
                if (isLongClick && !mTwoPane) {
                    // long click in phone mode
                    handleLongClick(pos);
                } else if (!isLongClick && !mTwoPane) {
                    // click in phone mode
                    handleSingleClick(pos);
                } else {
                    // click in tablet mode
                    handleSingleClick(pos);
                    notifyDataSetChanged();
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


    /* Insert new item to RecyclerView on position */
    public void add(int position, Station station) {
        mStationNames.add(position, station.getStationName());
        mStationImages.add(position, station.getStationImage());
        notifyItemInserted(position);
    }

    /* Insert new item to RecyclerView on position */
    public void change(int position, String stationName) {
        mStationNames.set(position, stationName);
        notifyItemChanged(position);
    }

    /* Remove RecyclerView item containing a specified Data object */
    public void remove(int stationID) {
        mStationNames.remove(stationID);
        mStationImages.remove(stationID);
        notifyItemRemoved(stationID);
    }


    /* Handles click on list item */
    private void handleSingleClick(int position) {

        if (mTwoPane) {
            Bundle args = new Bundle();
            args.putInt(TransistorKeys.ARG_STATION_ID, position);
            args.putParcelable(TransistorKeys.ARG_COLLECTION, mCollection);
            args.putBoolean(TransistorKeys.ARG_TWO_PANE, mTwoPane);

            PlayerActivityFragment playerActivityFragment = new PlayerActivityFragment();
            playerActivityFragment.setArguments(args);
            mActivity.getFragmentManager().beginTransaction()
                    .replace(R.id.player_container, playerActivityFragment, TransistorKeys.PLAYERFRAGMENT_TAG)
                    .commit();
        } else {
            // add ID of station to intent and start activity
            Intent intent = new Intent(mActivity, PlayerActivity.class);
            intent.setAction(TransistorKeys.ACTION_SHOW_PLAYER);
            intent.putExtra(TransistorKeys.EXTRA_STATION_ID, position);
            intent.putExtra(TransistorKeys.EXTRA_COLLECTION, mCollection);
            mActivity.startActivity(intent);
        }
    }


    /* Handles long click on list item */
    private void handleLongClick(int position) {

        // get current playback state
        loadAppState(mActivity);

        if (mPlayback && position == mStationIDCurrent ) {
            // stop playback service
            PlayerService.startActionStop(mActivity);

            // set playback state
            mStationIDLast = mStationIDCurrent;
            mStationIDCurrent = -1;
            mPlayback = false;

            // inform user
            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_long_press_playback_stopped), Toast.LENGTH_LONG).show();
        } else {
            // start playback service
            Station station = mCollection.getStations().get(position);
            PlayerService.startActionPlay(mActivity, mCollection, position);

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

        // save app state
        saveAppState(mActivity);

        // notify MainActivityFragment
        if (mCollectionChangedListener != null) {
            mCollectionChangedListener.collectionChanged();
        }

    }


    /* Loads app state from preferences */
    private void loadAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        mTwoPane = settings.getBoolean(TransistorKeys.PREF_TWO_PANE, false);
        mStationIDCurrent = settings.getInt(TransistorKeys.PREF_STATION_ID_CURRENTLY_PLAYING, -1);
        mStationIDLast = settings.getInt(TransistorKeys.PREF_STATION_ID_LAST, -1);
        mStationIDSelected = settings.getInt(TransistorKeys.PREF_STATION_ID_SELECTED, 0);
        mPlayback = settings.getBoolean(TransistorKeys.PREF_PLAYBACK, false);
        mStationLoading = settings.getBoolean(TransistorKeys.PREF_STATION_LOADING, false);
        Log.v(LOG_TAG, "Loading state ("+  mStationIDCurrent + " / " + mStationIDLast + " / " + mPlayback  + " / " + mStationLoading + ")");
    }


    /* Saves app state to SharedPreferences */
    private void saveAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(TransistorKeys.PREF_STATION_ID_CURRENTLY_PLAYING, mStationIDCurrent);
        editor.putInt(TransistorKeys.PREF_STATION_ID_LAST, mStationIDLast);
        editor.putInt(TransistorKeys.PREF_STATION_ID_SELECTED, mStationIDSelected);
        editor.putBoolean(TransistorKeys.PREF_PLAYBACK, mPlayback);
        editor.putBoolean(TransistorKeys.PREF_STATION_LOADING, mStationLoading);
        editor.apply();
        Log.v(LOG_TAG, "Saving state ("+  mStationIDCurrent + " / " + mStationIDLast + " / " + mPlayback  + " / " + mStationLoading + " / " + mStationIDSelected +")");
    }


    /* Setter for collection */
    public void setCollection(Collection collection) {
        mCollection = collection;
    }



    /* Refreshes app state info */
    public void refresh() {
        loadAppState(mActivity);
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


    /* Handles changes in state of playback, eg. start, stop, loading stream */
    private void handlePlaybackStateChanged(Intent intent) {

        // load app state
        loadAppState(mActivity);

        switch (intent.getIntExtra(TransistorKeys.EXTRA_PLAYBACK_STATE_CHANGE, 1)) {

            // CASE: player is preparing stream
            case TransistorKeys.PLAYBACK_LOADING_STATION:
                break;

            // CASE: playback has started
            case TransistorKeys.PLAYBACK_STARTED:
                break;

            // CASE: playback was stopped
            case TransistorKeys.PLAYBACK_STOPPED:
                break;
        }
    }
}