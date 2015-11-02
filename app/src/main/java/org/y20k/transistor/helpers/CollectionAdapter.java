/**
 * CollectionAdapter.java
 * Implements the CollectionAdapter class
 * A CollectionAdapter is a custom adapter for a listview
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
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
public class CollectionAdapter extends BaseAdapter {

    /* Keys */
    public static final String STATION_ID_CURRENT = "stationIDCurrent";
    public static final String STATION_ID_LAST = "stationIDLast";
    public static final String PLAYBACK = "playback";


    /* Main class variables */
    private LinkedList<String> mStationNames;
    private LinkedList<Bitmap> mStationImages;
    private Collection mCollection;
    private Context mContext;
    private CollectionChangedListener mCollectionChangedListener;
    private boolean mPlayback;
    private int mStationIDCurrent;
    private int mStationIDLast;


    /* Interface for custom listener */
    public interface CollectionChangedListener {
        void collectionChanged();
    }


    /* Constructor */
    public CollectionAdapter(Context context, LinkedList<String> stationNames, LinkedList<Bitmap> stationImage) {
        mContext = context;
        mStationNames = stationNames;
        mStationImages = stationImage;
        mCollection = null;
        mCollectionChangedListener = null;

        System.out.println("!!! @CollectionAdapter.Constructor | LOADING");
        loadPlaybackState(context);
    }


    /* Setter for collection */
    public void setCollection(Collection collection) {
        mCollection = collection;
    }


    /* Setter for custom listener */
    public void setCollectionChangedListener(CollectionChangedListener mCollectionChangedListener) {
        this.mCollectionChangedListener = mCollectionChangedListener;
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

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        ViewHolder holder = new ViewHolder();
        View rowView;

        rowView = inflater.inflate(R.layout.list_item_collection, null);
        holder.stationImageView = (ImageView) rowView.findViewById(R.id.list_item_station_icon);
        holder.stationNameView = (TextView) rowView.findViewById(R.id.list_item_textview);
        holder.playbackIndicator = (ImageView) rowView.findViewById(R.id.list_item_playback_indicator);
        holder.stationMenuView = (ImageView) rowView.findViewById(R.id.list_item_more_button);

        holder.stationImageView.setImageBitmap(mStationImages.get(position));
        holder.stationNameView.setText(mStationNames.get(position));

        if (mPlayback && mStationIDCurrent == position) {
            holder.playbackIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.playbackIndicator.setVisibility(View.GONE);
        }

        holder.stationMenuView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StationContextMenu menu = new StationContextMenu();
                menu.initialize(mContext, mCollection, view, position);

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

        return rowView;
    }

    private void loadPlaybackState(Context context) {

        // restore player state from preferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        mStationIDCurrent = settings.getInt(STATION_ID_CURRENT, -1);
        mStationIDLast = settings.getInt(STATION_ID_LAST, -1);
        mPlayback = settings.getBoolean(PLAYBACK, false);

        System.out.println("!!! @CollectionAdapter | Current: " + mStationIDCurrent);
        System.out.println("!!! @CollectionAdapter | Last: " + mStationIDLast);
        System.out.println("!!! @CollectionAdapter | Playback" + mPlayback);
    }

//    Parent:
//    MainActivityFragment
//    + create object of type child
//    + attach listener to object using child's setter
//
//    Child:
//    DialogAddStationFragment
//    - define interface (1)
//            + define instance variable of type interface
//    + set instance variable to null in constructor
//    + create setter for instance variable
//    - fire listener using "if (listener != null) {listener.dosomething}"






    /**
     * Inner class: cache of the children views
     */
    static class ViewHolder {
        ImageView stationImageView;
        TextView stationNameView;
        ImageView playbackIndicator;
        ImageView stationMenuView;
    }
    /**
     * End of inner class
     */

}