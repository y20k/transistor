/**
 * CollectionAdapter.java
 * Implements the CollectionAdapter class
 * A CollectionAdapter is a custom adapter for a RecyclerView
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-18 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.adapter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.y20k.transistor.R;
import org.y20k.transistor.core.Station;
import org.y20k.transistor.helpers.DialogAdd;
import org.y20k.transistor.helpers.ImageHelper;
import org.y20k.transistor.helpers.LogHelper;
import org.y20k.transistor.helpers.StationListHelper;
import org.y20k.transistor.helpers.StorageHelper;
import org.y20k.transistor.helpers.TransistorKeys;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;


/**
 * CollectionAdapter class
 */
public final class CollectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements TransistorKeys {

    /* Listener Interface */
    public interface CollectionAdapterListener {
        void itemSelected(Station station, boolean isLongPress);
        void jumpToPosition(int position);
    }

    /* Define log tag */
    private static final String LOG_TAG = CollectionAdapter.class.getSimpleName();


    /* Main class variables */
    private final Activity mActivity;
    private final CollectionViewModel mCollectionViewModel;
    private BroadcastReceiver mPlaybackStateChangedReceiver;
    private BroadcastReceiver mMetadataChangedReceiver;
    private CollectionAdapterListener mCollectionAdapterListener;
    private ArrayList<Station> mStationList;
    private int mStationIdSelected;
    private final String mCurrentStationUrl;


    /* Constructor */
    public CollectionAdapter(Activity activity, String currentStationUrl) {
        // set initial values
        mActivity = activity;
        mStationIdSelected = -1;
        mCurrentStationUrl = currentStationUrl;

        // create empty station list
        mStationList = new ArrayList<Station>();

        // initialize listener
        mCollectionAdapterListener = null;

        // initialize BroadcastReceiver that listens for playback changes
        initializeBroadcastReceivers();

        // observe changes in LiveData
        mCollectionViewModel = ViewModelProviders.of((AppCompatActivity)mActivity).get(CollectionViewModel.class);
        mCollectionViewModel.getStationList().observe((LifecycleOwner) mActivity, createStationListObserver());

    }


    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_ADD_NEW: {
                // get view, put view into holder and return
                View v = LayoutInflater.from (parent.getContext ()).inflate (R.layout.list_item_add_new, parent, false);
                return new AddNewViewHolder(v);
            }
            case VIEW_TYPE_STATION: {
                // get view, put view into holder and return
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_station, parent, false);
                return new StationViewHolder(v);
            }
            default: {
                return null;
            }
        }
    }


    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {

        // CASE ADD NEW
        if (holder instanceof AddNewViewHolder) {
            // get reference to AddNewViewHolder and listen for taps
            AddNewViewHolder addNewViewHolder = (AddNewViewHolder) holder;
            addNewViewHolder.getLListItemAddNewLayout().setOnClickListener (new View.OnClickListener () {
                @Override
                public void onClick (View view) {
                    DialogAdd.show(mActivity, StorageHelper.getCollectionDirectory(mActivity));
                }
            });
        }

        // CASE STATION
        else if (holder instanceof StationViewHolder) {
            // get station from position
            final Station station = mStationList.get(position);

            // get reference to StationViewHolder
            StationViewHolder stationViewHolder = (StationViewHolder) holder;

            // set station image
            stationViewHolder.getStationImageView().setImageBitmap(createStationImageBitmap(station));

            // set station name and content description (for accessibility apps like TalkBack)
            stationViewHolder.getStationNameView().setText(station.getStationName());
            stationViewHolder.getStationNameView().setContentDescription(station.getStationName());

            // set playback indicator - in phone view only
            togglePlaybackIndicator(stationViewHolder, station);

            // visually mark holder selected
            stationViewHolder.getListItemLayout().setSelected(position == mStationIdSelected);

            // listen for taps
            stationViewHolder.getListItemLayout().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // notify and update player sheet
                    handleTap(holder.getAdapterPosition(), false);
                }
            });
            stationViewHolder.getListItemLayout().setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    // notify and update player sheet - and start playback
                    handleTap(holder.getAdapterPosition(),  true);
                    return true;
                }
            });
        }
    }


    /* Handles tap on station */
    private void handleTap(int adapterPosition, boolean isLongPress) {

        // notify and update player sheet - and start playback if long press
        mCollectionAdapterListener.itemSelected(mStationList.get(adapterPosition), isLongPress);
        // visually deselect previous station
        notifyItemChanged(mStationIdSelected,HOLDER_UPDATE_SELECTION_STATE);
        // visually select this station
        mStationIdSelected = adapterPosition;
        notifyItemChanged(adapterPosition, HOLDER_UPDATE_SELECTION_STATE);
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, List<Object> payloads) {

        if (payloads.isEmpty()) {
            // call regular onBindViewHolder method
            onBindViewHolder(holder, position);

        } else if (holder instanceof StationViewHolder) {
            // get station from position
            final Station station = mStationList.get(holder.getAdapterPosition());

            // get reference to StationViewHolder
            StationViewHolder stationViewHolder = (StationViewHolder) holder;

            for (Object data : payloads) {
                switch ((int) data) {
                    case HOLDER_UPDATE_NAME:
                        // set station name
                        LogHelper.v(LOG_TAG, "List of station: Partial view update -> station name changed");
                        stationViewHolder.getStationNameView().setText(station.getStationName());
                        break;
                    case HOLDER_UPDATE_PLAYBACK_STATE:
                        // set playback indicator
                        LogHelper.v(LOG_TAG, "List of station: Partial view update -> playback state changed");
                        togglePlaybackIndicator(stationViewHolder, station);
                        break;
                    case HOLDER_UPDATE_SELECTION_STATE:
                        // visually mark holder selected
                        LogHelper.v(LOG_TAG, "List of station: Partial view update -> selection state changed");
                        stationViewHolder.getListItemLayout().setSelected(position == mStationIdSelected);
                        break;
                    case HOLDER_UPDATE_IMAGE:
                        // set station image
                        LogHelper.v(LOG_TAG, "List of station: Partial view update -> station image changed");
                        stationViewHolder.getStationImageView().setImageBitmap(createStationImageBitmap(station));
                        break;
                }
            }
        }
    }


    @Override
    public int getItemViewType (int position) {
        if(isPositionFooter (position)) {
            return VIEW_TYPE_ADD_NEW;
        }
        return VIEW_TYPE_STATION;
    }


    @Override
    public int getItemCount() {
        return mStationList.size() + 1;
    }


    /* Determines if position is last */
    private boolean isPositionFooter (int position) {
        return position == mStationList.size();
    }


    /* Setter for CollectionAdapterListener */
    public void setCollectionAdapterListener(CollectionAdapterListener collectionAdapterListener) {
        mCollectionAdapterListener = collectionAdapterListener;
    }


    /* Create bitmap version of station image */
    private Bitmap createStationImageBitmap(Station station) {
        ImageHelper imageHelper = new ImageHelper(station, mActivity);
        return imageHelper.createCircularFramedImage(192);
    }


    /* Manipulates state of playback indicator */
    private void togglePlaybackIndicator(StationViewHolder holder, Station station) {
        if (station.getPlaybackState() == PLAYBACK_STATE_LOADING_STATION) {
            holder.getPlaybackIndicator().setImageResource(R.drawable.ic_playback_indicator_loading_24dp);
            holder.getPlaybackIndicator().setVisibility(View.VISIBLE);
        } else if (station.getPlaybackState() == PLAYBACK_STATE_STARTED) {
            holder.getPlaybackIndicator().setImageResource(R.drawable.ic_playback_indicator_started_24dp);
            holder.getPlaybackIndicator().setVisibility(View.VISIBLE);
        } else {
            holder.getPlaybackIndicator().setVisibility(View.INVISIBLE);
        }
    }


    /* Setter for image file within station object with given ID */
    public void setNewImageFile(int stationId, File stationImageFile) {
        mStationList.get(stationId).setStationImageFile(stationImageFile);
    }


    /* Getter for list of stations */
    public ArrayList<Station> getStationList() {
        return mStationList;
    }


    /* Initializes and registers broadcast receivers */
    private void initializeBroadcastReceivers() {

        // RECEIVER: state of playback has changed
        mPlaybackStateChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra(EXTRA_STATION)) {
                    handlePlaybackStateChange(intent);
                } else if (intent.hasExtra(EXTRA_ERROR_OCCURRED) && intent.getBooleanExtra(EXTRA_ERROR_OCCURRED, false)) {
                    handlePlaybackStateError(intent);
                }
            }
        };
        IntentFilter playbackStateChangedIntentFilter = new IntentFilter(ACTION_PLAYBACK_STATE_CHANGED);
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(mPlaybackStateChangedReceiver, playbackStateChangedIntentFilter);

        // RECEIVER: station metadata has changed
        mMetadataChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra(EXTRA_STATION)) {
                    handleMetadataChange(intent);
                }
            }
        };
        IntentFilter metadataChangedIntentFilter = new IntentFilter(ACTION_METADATA_CHANGED);
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(mMetadataChangedReceiver, metadataChangedIntentFilter);
    }


    /* Unregisters broadcast receivers */
    public void unregisterBroadcastReceivers(Context context) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(mPlaybackStateChangedReceiver);
        LocalBroadcastManager.getInstance(context).unregisterReceiver(mMetadataChangedReceiver);
        LogHelper.v(LOG_TAG, "Unregistered broadcast receivers in adapter");
    }


    /* handles changes in metadata */
    private void handleMetadataChange(Intent intent) {
        // get new station from intent
        Station station;
        if (intent.hasExtra(EXTRA_STATION)) {
            station = intent.getParcelableExtra(EXTRA_STATION);
        } else {
            return;
        }

        // create copies of station and of main list of stations
        Station newStation = new Station(station);
        ArrayList<Station> newStationList = StationListHelper.copyStationList(mStationList);

        int stationId = StationListHelper.findStationId(newStationList, newStation.getStreamUri());
        if (stationId != -1) {
            newStationList.set(stationId, newStation);
        }

        // update liva data station from PlayerService - used in PlayerFragment
        mCollectionViewModel.getPlayerServiceStation().setValue(newStation);

        // update live data station list - used in CollectionAdapter (this)
        mCollectionViewModel.getStationList().setValue(newStationList);
    }


    /* handles changes in playback state */
    private void handlePlaybackStateChange(Intent intent) {

        // get new station from intent
        Station station;
        if (intent.hasExtra(EXTRA_STATION)) {
            station = intent.getParcelableExtra(EXTRA_STATION);
        } else {
            return;
        }

        // create copies of station and of main list of stations
        Station newStation = new Station(station);
        ArrayList<Station> newStationList = StationListHelper.copyStationList(mStationList);

        // try to set playback state of previous station
        if (intent.hasExtra(EXTRA_PLAYBACK_STATE_PREVIOUS_STATION)) {
            String previousStationUrlString = intent.getStringExtra(EXTRA_PLAYBACK_STATE_PREVIOUS_STATION);
            if (previousStationUrlString != null) {
                int previousStationId = StationListHelper.findStationId(newStationList, Uri.parse(previousStationUrlString));
                if (previousStationId != -1) {
                    newStationList.get(previousStationId).setPlaybackState(PLAYBACK_STATE_STOPPED);
                }
            }
        }

        // set playback state for new station
        int stationId = StationListHelper.findStationId(newStationList, newStation.getStreamUri());
        if (stationId != -1) {
            newStationList.set(stationId, newStation);
        }

        // update liva data station from PlayerService - used in PlayerFragment
        mCollectionViewModel.getPlayerServiceStation().setValue(newStation);

        // update live data station list - used in CollectionAdapter (this)
        mCollectionViewModel.getStationList().setValue(newStationList);
    }


    /* Handles a playback state error that can occur when Transistor crashes during playback */
    private void handlePlaybackStateError(Intent intent) {
        LogHelper.e(LOG_TAG, "Forcing a reload of station list. Did Transistor crash?");
        mCollectionViewModel.getStationList().setValue(StationListHelper.loadStationListFromStorage(mActivity));
    }


    /* Creates an observer for collection of stations stored as LiveData */
    private Observer<ArrayList<Station>> createStationListObserver() {
        return new Observer<ArrayList<Station>>() {
            @Override
            public void onChanged(@Nullable ArrayList<Station> newStationList) {
                LogHelper.v(LOG_TAG, "Observer for list of stations in CollectionAdapter: list has changed.");
                StationListHelper.sortStationList(newStationList);
                // calculate differences between new station list and current station list
                DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new CollectionAdapterDiffUtilCallback(mStationList, newStationList), true);

                // update current station list
                mStationList = StationListHelper.copyStationList(newStationList);

                // get station id selected (once)
                if (mStationIdSelected == -1 && mCurrentStationUrl != null) {
                    mStationIdSelected = StationListHelper.findStationId(mStationList, Uri.parse(mCurrentStationUrl));
                    mCollectionAdapterListener.jumpToPosition(mStationIdSelected);
                }

                // inform this adapter about the changes
                diffResult.dispatchUpdatesTo(CollectionAdapter.this);
            }
        };
    }


    /**
     * Inner class: ViewHolder for the Add New Station action
     */
    private class AddNewViewHolder extends RecyclerView.ViewHolder {
        private final View listItemAddNewLayout;

        /* Constructor */
        AddNewViewHolder(View itemView) {
            super(itemView);
            this.listItemAddNewLayout = itemView;
        }

        /* Getter for parent list item layout */
        View getLListItemAddNewLayout() {
            return this.listItemAddNewLayout;
        }
    }
    /**
     * End of inner class
     */


    /**
     * Inner class: ViewHolder for a station
     */
    private class StationViewHolder extends RecyclerView.ViewHolder {
        private final View listItemLayout;
        private final ImageView stationImageView;
        private final TextView stationNameView;
        private final ImageView playbackIndicator;

        /* Constructor */
        public StationViewHolder(View itemView) {
            super(itemView);
            this.listItemLayout = itemView;
            this.stationImageView = itemView.findViewById(R.id.list_item_station_icon);
            this.stationNameView = itemView.findViewById(R.id.list_item_textview);
            this.playbackIndicator = itemView.findViewById(R.id.list_item_playback_indicator);
        }

        /* Getter for parent list item layout */
        public View getListItemLayout() {
            return this.listItemLayout;
        }

        /* Getter for station image view */
        public ImageView getStationImageView() {
            return this.stationImageView;
        }

        /* Getter for station name view */
        public TextView getStationNameView() {
            return this.stationNameView;
        }

        /* Getter for station playback indicator */
        public ImageView getPlaybackIndicator() {
            return this.playbackIndicator;
        }
    }
    /**
     * End of inner class
     */

}