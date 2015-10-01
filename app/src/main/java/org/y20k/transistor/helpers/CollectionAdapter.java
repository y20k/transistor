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
import android.graphics.Bitmap;
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

    /* Main class variables */
    private LinkedList<String> mStationNames;
    private LinkedList<Bitmap> mStationImages;
    private Collection mCollection;
    private Context mContext;
    private CollectionChangedListener collectionChangedListener;


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
        collectionChangedListener = null;
    }


    /* Setter for collection */
    public void setCollection(Collection collection) {
        mCollection = collection;
    }


    /* Setter for custom listener */
    public void setCollectionChangedListener(CollectionChangedListener mCollectionChangedListener) {
        collectionChangedListener = mCollectionChangedListener;
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
        holder.stationMenuView = (ImageView) rowView.findViewById(R.id.list_item_more_button);

        holder.stationImageView.setImageBitmap(mStationImages.get(position));
        holder.stationNameView.setText(mStationNames.get(position));

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
                        if (collectionChangedListener != null) {
                            collectionChangedListener.collectionChanged();
                        }
                    }
                });

                menu.show();
            }
        });

        return rowView;
    }


    /**
     * Inner class: cache of the children views
     */
    static class ViewHolder {
        ImageView stationImageView;
        TextView stationNameView;
        ImageView stationMenuView;
    }
    /**
     * End of inner class
     */

}