/**
 * RecyclerViewHolder.java
 * Implements the a custom view holder
 * A RecyclerViewHolder is an implementation of the Android "view holder" design pattern
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-16 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;


import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.y20k.transistor.R;


/**
 * RecyclerViewHolder.class
 */
public class RecyclerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

    /* Define log tag */
    private static final String LOG_TAG = RecyclerViewHolder.class.getSimpleName();


    /* Main class variables */
    private View mListItemLayout;
    private ImageView mStationImageView;
    private TextView mStationNameView;
    private ImageView mPlaybackIndicator;
    private ImageView mStationMenuView;
    private ClickListener mClickListener;


    /* Interface for handling clicks - both normal and long ones. */
    public interface ClickListener {
        void onClick(View v, int position, boolean isLongClick);
    }


    /* Constructor */
    public RecyclerViewHolder (View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);
        itemView.setClickable(true);
        mListItemLayout = itemView;
        mStationImageView = (ImageView) itemView.findViewById(R.id.list_item_station_icon);
        mStationNameView = (TextView) itemView.findViewById(R.id.list_item_textview);
        mPlaybackIndicator = (ImageView) itemView.findViewById(R.id.list_item_playback_indicator);
        mStationMenuView = (ImageView) itemView.findViewById(R.id.list_item_more_button);
    }


    @Override
    public void onClick(View v) {
        // if not long clicked, pass last variable as false.
        // TODO change getPosition
        mClickListener.onClick(v, getPosition(), false);
    }


    @Override
    public boolean onLongClick(View v) {
        // if long clicked, passed last variable as true.
        // TODO change getPosition
        mClickListener.onClick(v, getPosition(), true);
        return true;
    }


    /* Getter for parent list item layout */
    public View getListItemLayout() {
        return mListItemLayout;
    }

    /* Getter for station image view */
    public ImageView getStationImageView() {
        return mStationImageView;
    }


    /* Getter for station name view */
    public TextView getStationNameView() {
        return mStationNameView;
    }


    /* Getter for station playback indicator */
    public ImageView getPlaybackIndicator() {
        return mPlaybackIndicator;
    }


    /* Getter for station menu view */
    public ImageView getStationMenuView() {
        return mStationMenuView;
    }


    /* Setter for listener. */
    public void setClickListener(ClickListener clickListener) {
        mClickListener = clickListener;
    }

}