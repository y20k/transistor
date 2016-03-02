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
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.y20k.transistor.R;
import org.y20k.transistor.core.Collection;

import java.util.LinkedList;


/**
 * CollectionAdapter class
 */
public final class CollectionAdapter extends BaseAdapter {

    /* Define log tag */
    private static final String LOG_TAG = CollectionAdapter.class.getSimpleName();


    /* Keys */
    private static final String STATION_ID_CURRENT = "stationIDCurrent";
    private static final String PLAYBACK = "playback";
    private static final String TIMER_RUNNING = "timerRunning";


    /* Main class variables */
    private final LinkedList<String> mStationNames;
    private final LinkedList<Bitmap> mStationImages;
    private final Activity mActivity;
    private Collection mCollection;
    private CollectionChangedListener mCollectionChangedListener;
    private boolean mPlayback;
    private int mStationIDCurrent;
    private boolean mTimerRunning;


    /* Interface for custom listener */
    public interface CollectionChangedListener {
        void collectionChanged();
    }


    /* Constructor */
    public CollectionAdapter(Activity activity, LinkedList<String> stationNames, LinkedList<Bitmap> stationImage) {
        mActivity = activity;
        mStationNames = stationNames;
        mStationImages = stationImage;
        mCollection = null;
        mCollectionChangedListener = null;

        loadPlaybackState(mActivity);
    }


    @Override
    public int getCount() {
        return mStationNames.size();
    }


    @Override
    public Object getItem(int position) {
        return position;
    }


    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        // create new view if no convertView available
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item_collection, parent, false);

            holder = new ViewHolder();
            holder.stationImageView = (ImageView) convertView.findViewById(R.id.list_item_station_icon);
            holder.stationNameView = (TextView) convertView.findViewById(R.id.list_item_textview);
            holder.timerIndicator = (ImageView) convertView.findViewById(R.id.list_item_timer_indicator);
            holder.playbackIndicator = (ImageView) convertView.findViewById(R.id.list_item_playback_indicator);
            holder.stationMenuView = (ImageView) convertView.findViewById(R.id.list_item_more_button);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }



        // set station image
        holder.stationImageView.setImageBitmap(mStationImages.get(position));

        // set station name
        holder.stationNameView.setText(mStationNames.get(position));

        // set timer indicator
        if (mPlayback && mTimerRunning && mStationIDCurrent == position) {
            holder.timerIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.timerIndicator.setVisibility(View.GONE);
        }

        // set playback indicator
        if (mPlayback && mStationIDCurrent == position) {
            holder.playbackIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.playbackIndicator.setVisibility(View.GONE);
        }

        // attach three dots menu
        holder.stationMenuView.setOnClickListener(new View.OnClickListener() {
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

        return convertView;
    }


    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        loadPlaybackState(mActivity);
    }


    /* Loads playback state from preferences */
    private void loadPlaybackState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        mStationIDCurrent = settings.getInt(STATION_ID_CURRENT, -1);
        mPlayback = settings.getBoolean(PLAYBACK, false);
        mTimerRunning = settings.getBoolean(TIMER_RUNNING, false);
        Log.v(LOG_TAG, "Loading state.");
    }


    /* Setter for collection */
    public void setCollection(Collection collection) {
        mCollection = collection;
    }


    /* Setter for custom listener */
    public void setCollectionChangedListener(CollectionChangedListener mCollectionChangedListener) {
        this.mCollectionChangedListener = mCollectionChangedListener;
    }


    /**
     * Inner class: cache of the children views
     */
    static class ViewHolder {
        ImageView stationImageView;
        TextView stationNameView;
        ImageView timerIndicator;
        ImageView playbackIndicator;
        ImageView stationMenuView;
    }
    /**
     * End of inner class
     */

}