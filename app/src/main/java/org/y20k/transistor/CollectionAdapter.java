/**
 * CollectionAdapter.java
 * Implements the CollectionAdapter class
 * A CollectionAdapter is a custom adapter for a RecyclerView
 * <p>
 * This file is part of
 * TRANSISTOR - Radio App for Android
 * <p>
 * Copyright (c) 2015-17 - Y20K.org
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
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import org.y20k.transistor.core.Station;
import org.y20k.transistor.helpers.DialogError;
import org.y20k.transistor.helpers.LogHelper;
import org.y20k.transistor.helpers.ShortcutHelper;
import org.y20k.transistor.helpers.SingletonProperties;
import org.y20k.transistor.helpers.StationContextMenu;
import org.y20k.transistor.helpers.TransistorKeys;
import org.y20k.transistor.sqlcore.StationsDbHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;


/**
 * CollectionAdapter class
 */
public final class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapterViewHolder> {

    /* Define log tag */
    private static final String LOG_TAG = CollectionAdapter.class.getSimpleName();
    public static final String LOADING_STATUS = "loading";
    public static final String STOP_STATUS = "stop";
    public static final String PLAY_STATUS = "play";


    /* Main class variables */
    private final Activity mActivity;
    private final File mFolder;
    private BroadcastReceiver mPlaybackStateChangedReceiver;
    private boolean mPlayback;
    private boolean mStationLoading;
    private int mStationIDCurrent;
    private int mStationIDLast;
    private int mStationIDSelected;
    private boolean mTwoPane;
    private final SortedList<Station> mStationList;


    /* Constructor */
    public CollectionAdapter(Activity activity, File folder) {
        // set main variables
        mActivity = activity;
        mFolder = folder;
        mStationIDSelected = 0;
        mStationList = new SortedList<Station>(Station.class, new SortedListAdapterCallback<Station>(this) {

            @Override
            public int compare(Station station1, Station station2) {
                // Compares two stations: returns "1" if name if this station is greater than name of given station
                return station1.TITLE.compareToIgnoreCase(station2.TITLE);
            }

            @Override
            public boolean areContentsTheSame(Station oldStation, Station newStation) {
                return oldStation.TITLE.equals(newStation.TITLE);
            }

            @Override
            public boolean areItemsTheSame(Station station1, Station station2) {
                // return station1.equals(station2);
                return areContentsTheSame(station1, station2);
            }
        });

        // fill station list
        loadCollection();

        // load state
        // loadAppState(mActivity);

    }


    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        // load state
        loadAppState(mActivity);
        // initialize broadcast receivers
        initializeBroadcastReceivers();
    }


    @Override
    public CollectionAdapterViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {

        // get view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_collection_staggered, parent, false);

        // put view into holder and return
        return new CollectionAdapterViewHolder(v);
    }


    @Override
    public void onViewAttachedToWindow(CollectionAdapterViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        Log.v(LOG_TAG + "debugCollectionView","start onViewAttachedToWindow , getFabPlayButton().getTag="+String.valueOf(holder.getFabPlayButton().getTag().toString()));
        //restore animation if loading status
        FloatingActionButton fabPlayButton = holder.getFabPlayButton();
        String tag = fabPlayButton.getTag() == null ? "" : fabPlayButton.getTag().toString();
        if (Objects.equals(tag,LOADING_STATUS)) {
            Animation rotate_infinite = AnimationUtils.loadAnimation(mActivity, R.anim.rotate_infinite);
            rotate_infinite.setFillAfter(true);
            fabPlayButton.startAnimation(rotate_infinite);
        }
    }

    @Override
    public void onBindViewHolder(CollectionAdapterViewHolder holder, final int position) {
        Log.v(LOG_TAG + "debugCollectionView","start onBindViewHolder , position="+String.valueOf(position));
        // final int position --> Do not treat position as fixed; only use immediately and call holder.getAdapterPosition() to look it up later
        // get station from position
        final Station station = mStationList.get(position);

        if (mTwoPane && mStationIDSelected == position) {
            holder.getListItemLayout().setSelected(true);
        } else {
            holder.getListItemLayout().setSelected(false);
        }

        // set station image
        File stationImageFile = station.getStationImage(mActivity);
        if (stationImageFile != null && stationImageFile.exists()) {
            holder.getStationImageView().setImageURI(stationImageFile.toURI().toString());//.setImageBitmap(stationImageSmall);
        } else if (station.IMAGE_PATH != null && station.IMAGE_PATH != "") {
            holder.getStationImageView().setImageURI(station.IMAGE_PATH);//.setImageBitmap(stationImageSmall);
        }
        // set station name
        holder.getStationNameView().setText(station.TITLE);
        holder.getRatingBarView().setRating(station.RATING);

        //station description
        holder.getmStationDesciptionView().setText(station.DESCRIPTION);

        //shadow
        holder.getStationNameView().setShadowLayer(2, 1, 1, Color.BLACK);
        holder.getmStationDesciptionView().setShadowLayer(2, 1, 1, Color.BLACK);

        // set playback indicator - in phone view only
        FloatingActionButton fabPlayButton = holder.getFabPlayButton();
        if (!mTwoPane && mPlayback && (station.getPlaybackState())) { // || getPositionOfCurrentPlayStation() == position
            if (mStationLoading) {
                holder.getPlaybackIndicator().setBackgroundResource(R.drawable.ic_playback_indicator_small_loading_24dp);
                fabPlayButton.setImageResource(R.drawable.progress_loading);
                fabPlayButton.setTag(LOADING_STATUS);
            } else {
                holder.getPlaybackIndicator().setBackgroundResource(R.drawable.ic_playback_indicator_small_started_24dp);
                fabPlayButton.setImageResource(R.drawable.smbl_stop);
                fabPlayButton.setTag(STOP_STATUS);
            }
            holder.getPlaybackIndicator().setVisibility(View.VISIBLE);
        } else {
            fabPlayButton.setImageResource(R.drawable.smbl_play);
            fabPlayButton.setTag(PLAY_STATUS);
            holder.getPlaybackIndicator().setVisibility(View.GONE);
        }

        // attach three dots menu - in phone view only
        if (!mTwoPane) {
            holder.getStationMenuView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    StationContextMenu menu = new StationContextMenu();
                    menu.initialize(mActivity, view, station, position);
                    menu.show();
                }
            });
        } else {
            holder.getStationMenuView().setVisibility(View.GONE);
        }

        fabPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStationIDSelected = position;
                LogHelper.v(LOG_TAG, "Selected station (ID): " + mStationIDSelected);
                if (!mTwoPane) {
                    // long click in phone mode
                    handlePlayStopClick(position);
                } else {
                    // click in tablet mode
                    handleSingleClick(position);
                    notifyDataSetChanged();
                }
            }
        });

        // attach click listener
        holder.setClickListener(new CollectionAdapterViewHolder.ClickListener() {
            @Override
            public void onClick(View view, int pos, boolean isLongClick) {
                mStationIDSelected = pos;
                saveAppState(mActivity);
                LogHelper.v(LOG_TAG, "Selected station (ID): " + mStationIDSelected);
                if (isLongClick && !mTwoPane) {
                    // long click in phone mode
                    handlePlayStopClick(pos);
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

    public int getItemPosition(int _ID) {
        int position = 0;
        for (int i = 0; i < mStationList.size(); i++) {
            if (mStationList.get(i)._ID == _ID) {
                position = i;
                break;
            }
        }
        return position;
    }


    @Override
    public int getItemCount() {
        return mStationList.size();
    }


    /* Fills sorted list of station */
    private void loadCollection() {
        // create folder if necessary
        if (!mFolder.exists()) {
            LogHelper.v(LOG_TAG, "Creating mFolder new folder: " + mFolder.toString());
            mFolder.mkdir();
        }

        //todo:remove that if not required (just backward compatibility)
        // create nomedia file to prevent media scanning
        File nomedia = new File(mFolder, ".nomedia");
        if (!nomedia.exists()) {
            LogHelper.v(LOG_TAG, "Creating .nomedia file in folder: " + mFolder.toString());

            try (FileOutputStream noMediaOutStream = new FileOutputStream(nomedia)) {
                noMediaOutStream.write(0);
            } catch (IOException e) {
                LogHelper.e(LOG_TAG, "Unable to write .nomedia file in folder: " + mFolder.toString());
            }
        }

        //clear mStationList in case of having any record
        mStationList.clear();

        //Get from DB
        StationsDbHelper mDbHelper = new StationsDbHelper(mActivity);
        mDbHelper.FillListOfAllStations(mStationList);
    }


    /* Handles click on list item */
    private void handleSingleClick(int position) {

        Station station = mStationList.get(position);

        if (mTwoPane) {
            Bundle args = new Bundle();
            args.putParcelable(TransistorKeys.ARG_STATION, station);
            args.putInt(TransistorKeys.ARG_STATION_ID, position);
            args.putBoolean(TransistorKeys.ARG_TWO_PANE, mTwoPane);

            PlayerActivityFragment playerActivityFragment = new PlayerActivityFragment();
            playerActivityFragment.setArguments(args);
            mActivity.getFragmentManager().beginTransaction()
                    .replace(R.id.player_container, playerActivityFragment, TransistorKeys.PLAYER_FRAGMENT_TAG)
                    .commit();
        } else {
            // add ID of station to intent and start activity
            Intent intent = new Intent(mActivity, PlayerActivity.class);
            intent.setAction(TransistorKeys.ACTION_SHOW_PLAYER);
            intent.putExtra(TransistorKeys.EXTRA_STATION, station);
            intent.putExtra(TransistorKeys.EXTRA_STATION_ID, position);
            mActivity.startActivity(intent);
        }
    }


    /* Handles long click on list item */
    private void handlePlayStopClick(int position) {

        // get current playback state
        loadAppState(mActivity);

        if (mPlayback && mStationList.get(position).getPlaybackState()) {
            // stop player service using intent
            Intent intent = new Intent(mActivity, PlayerService.class);
            intent.setAction(TransistorKeys.ACTION_STOP);
            mActivity.startService(intent);
            LogHelper.v(LOG_TAG, "Stopping player service.");

            // keep track of playback state
            mStationIDLast = position;
            mStationIDCurrent = -1;

            // remove playback flag from this station
            mPlayback = false;
            mStationList.get(position).setPlaybackState(false);

            // inform user
            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_long_press_playback_stopped), Toast.LENGTH_LONG).show();
        } else {
            // start player service using intent
            Intent intent = new Intent(mActivity, PlayerService.class);
            intent.setAction(TransistorKeys.ACTION_PLAY);
            intent.putExtra(TransistorKeys.EXTRA_STATION, mStationList.get(position));
            intent.putExtra(TransistorKeys.EXTRA_STATION_ID, position);
            mActivity.startService(intent);
            LogHelper.v(LOG_TAG, "Starting player service.");

            // keep track of playback state
            mStationIDLast = getPositionOfCurrentPlayStation();
            mStationIDCurrent = position;

            // remove playback flag from last station
            if (mPlayback && mStationIDLast != -1) {
                mStationList.get(mStationIDLast).setPlaybackState(false);
            }
            // add playback flag to current station
            mPlayback = true;
            mStationList.get(position).setPlaybackState(true);

            // inform user
            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_long_press_playback_started), Toast.LENGTH_LONG).show();
        }

        // vibrate 50 milliseconds
        Vibrator v = (Vibrator) mActivity.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(50);

        // save app state
        saveAppState(mActivity);

    }

    private int getPositionOfCurrentPlayStation() {
        int position=-1;
        for (int i = 0; i < mStationList.size(); i++) {
            String currentRunningStationid = SingletonProperties.getInstance().CurrentStation_ID;
            if(String.valueOf(mStationList.get(i)._ID).equals(currentRunningStationid)){
                position = i;
                break;
            }
        }
        return position;
    }


    /* Loads app state from preferences */
    private void loadAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        mTwoPane = settings.getBoolean(TransistorKeys.PREF_TWO_PANE, false);
        mStationIDCurrent = getPositionOfCurrentPlayStation();// settings.getInt(TransistorKeys.PREF_STATION_ID_CURRENTLY_PLAYING, -1);
        mStationIDLast = settings.getInt(TransistorKeys.PREF_STATION_ID_LAST, -1);
        mStationIDSelected = settings.getInt(TransistorKeys.PREF_STATION_ID_SELECTED, 0);
        mPlayback = settings.getBoolean(TransistorKeys.PREF_PLAYBACK, false);
        mStationLoading = settings.getBoolean(TransistorKeys.PREF_STATION_LOADING, false);
        LogHelper.v(LOG_TAG, "Loading state (" + mStationIDCurrent + " / " + mStationIDLast + " / " + mPlayback + " / " + mStationLoading + ")");
    }


    /* Saves app state to SharedPreferences */
    private void saveAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(TransistorKeys.PREF_STATION_ID_SELECTED, mStationIDSelected);
        editor.apply();
        LogHelper.v(LOG_TAG, "Saving state (" + mStationIDCurrent + " / " + mStationIDLast + " / " + mPlayback + " / " + mStationLoading + " / " + mStationIDSelected + ")");
    }


    /* Setter for ID of currently selected station */
    public void setStationIDSelected(int stationIDSelected, boolean playbackState, boolean startPlayback) {
        mStationIDSelected = stationIDSelected;
        saveAppState(mActivity);
        if (mStationIDSelected >= 0 && mStationIDSelected < mStationList.size()) {
            mStationList.get(stationIDSelected).setPlaybackState(playbackState);
        }
        if (mTwoPane && mStationIDSelected >= 0 && mStationIDSelected < mStationList.size()) {
            handleSingleClick(mStationIDSelected);
        }

        if (startPlayback) {
            // start player service using intent
            Intent intent = new Intent(mActivity, PlayerService.class);
            intent.setAction(TransistorKeys.ACTION_PLAY);
            intent.putExtra(TransistorKeys.EXTRA_STATION, mStationList.get(stationIDSelected));
            intent.putExtra(TransistorKeys.EXTRA_STATION_ID, stationIDSelected);
            mActivity.startService(intent);
            LogHelper.v(LOG_TAG, "Starting player service.");
        }
    }


    /* Setter for two pane flag */
    public void setTwoPane(boolean twoPane) {
        mTwoPane = twoPane;
    }


    /* Reloads app state */
    public void refresh() {
        loadAppState(mActivity);
    }


    /* Finds station when given its Uri */
    public Station findStation(Uri streamUri) {

        // traverse list of stations
        for (int i = 0; i < mStationList.size(); i++) {
            Station station = mStationList.get(i);
            if (station.getStreamUri().equals(streamUri)) {
                return station;
            }
        }

        // return null if nothing was found
        return null;
    }


    /* Getter for ID of given station */
    public int getStationID(Station station) {
        return mStationList.indexOf(station);
    }


    /* Getter for station of given ID */
    public Station getStation(int stationID) {
        return mStationList.get(stationID);
    }


    /* Add station to collection */
    public int add(Station station) {
        if (station.TITLE != null && station.getStreamUri() != null) {
            // add station to list of stations
            mStationList.add(station);

            // return new index
            return mStationList.indexOf(station);
        } else {
            // notify user and log failure to add
            String errorTitle = mActivity.getResources().getString(R.string.dialog_error_title_fetch_write);
            String errorMessage = mActivity.getResources().getString(R.string.dialog_error_message_fetch_write);
            String errorDetails = mActivity.getResources().getString(R.string.dialog_error_details_write);
            DialogError dialogError = new DialogError(mActivity, errorTitle, errorMessage, errorDetails);
            dialogError.show();
            LogHelper.e(LOG_TAG, "Unable to add station to collection: Duplicate name and/or stream URL.");
            return -1;
        }
    }

    /* change Rating station within collection */
    public int updateItemAtPosition(Station station, int position) {
        // name of station is new
        if (station != null) {

            // update station list
            mStationList.updateItemAt(position, station);


            // return changed station
            return mStationList.indexOf(station);

        } else {
            // name of station is null or not new - notify user
            return -1;
        }
    }

    /* Rename station within collection */
    public int rename(String newStationName, Station station, int stationID) {

        // get old station
        Station oldStation = mStationList.get(stationID);

        // name of station is new
        if (station != null && !oldStation.TITLE.equals(newStationName)) {

            //update DB
            StationsDbHelper mDbHelper = new StationsDbHelper(mActivity);
            mDbHelper.RenameStation(oldStation._ID, newStationName);

            //update station object
            station.TITLE = newStationName;

            // update station list
            mStationList.updateItemAt(stationID, station);


            // return changed station
            return mStationList.indexOf(station);

        } else {
            // name of station is null or not new - notify user
            Toast.makeText(mActivity, mActivity.getString(R.string.toastalert_rename_unsuccessful), Toast.LENGTH_LONG).show();
            return -1;
        }

    }


    /* Delete station within collection */
    public int delete(Station station, int stationID) {

        boolean success = false;

        // get old station
        Station oldStation = mStationList.get(stationID);

        //delete from db
        //update DB
        //db test
        StationsDbHelper mDbHelper = new StationsDbHelper(mActivity);
        int resulti = mDbHelper.DeleteStation(oldStation._ID);
        if (resulti > 0) {
            success = true;
        }

        // remove station and notify user
        if (success) {
            mStationList.removeItemAt(stationID);
            Toast.makeText(mActivity, mActivity.getString(R.string.toastalert_delete_successful), Toast.LENGTH_LONG).show();
        }

        // delete station shortcut
        ShortcutHelper shortcutHelper = new ShortcutHelper(mActivity);
        shortcutHelper.removeShortcut(station);

        if (mTwoPane) {
            // determine ID of next station to display in two pane mode
            if (mStationList.size() >= stationID) {
                stationID--;
            }

            if (stationID >= 0) {
                // show next station
                Bundle args = new Bundle();
                args.putParcelable(TransistorKeys.ARG_STATION, mStationList.get(stationID));
                args.putInt(TransistorKeys.ARG_STATION_ID, stationID);
                args.putBoolean(TransistorKeys.ARG_TWO_PANE, mTwoPane);
                PlayerActivityFragment playerActivityFragment = new PlayerActivityFragment();
                playerActivityFragment.setArguments(args);
                mActivity.getFragmentManager().beginTransaction()
                        .replace(R.id.player_container, playerActivityFragment, TransistorKeys.PLAYER_FRAGMENT_TAG)
                        .commit();
            }
        }

        // return ID of next station
        return stationID;
    }


    /* Handles changes in state of playback, eg. start, stop, loading stream */
    private void handlePlaybackStateChanged(Intent intent) {

        // load app state
        loadAppState(mActivity);

        if (intent.hasExtra(TransistorKeys.EXTRA_PLAYBACK_STATE_CHANGE) && intent.hasExtra(TransistorKeys.EXTRA_STATION_ID)) {

            notifyDataSetChanged();

            // get station ID from intent
            int stationID = intent.getIntExtra(TransistorKeys.EXTRA_STATION_ID, 0);
            switch (intent.getIntExtra(TransistorKeys.EXTRA_PLAYBACK_STATE_CHANGE, 1)) {

                // CASE: player is preparing stream
                case TransistorKeys.PLAYBACK_LOADING_STATION:
                    if (mStationIDLast > -1 && mStationIDLast < mStationList.size()) {
                        mStationList.get(mStationIDLast).setPlaybackState(false);
                    }
                    mStationLoading = true;
                    mPlayback = true;
                    if (stationID > -1 && stationID < mStationList.size()) {
                        mStationList.get(stationID).setPlaybackState(true);
                    }
                    notifyDataSetChanged();
                    break;

                // CASE: playback has started
                case TransistorKeys.PLAYBACK_STARTED:
                    mStationLoading = false;
                    if (stationID > -1 && stationID < mStationList.size()) {
                        mStationList.get(stationID).setPlaybackState(true);
                    }
                    notifyDataSetChanged();
                    break;

                // CASE: playback was stopped
                case TransistorKeys.PLAYBACK_STOPPED:
                    mPlayback = false;
                    if (stationID > -1 && stationID < mStationList.size()) {
                        mStationList.get(stationID).setPlaybackState(false);
                    }
                    notifyDataSetChanged();
                    break;
            }
        }

    }


    /* Initializes broadcast receivers for onCreate */
    private void initializeBroadcastReceivers() {

        // RECEIVER: state of playback has changed
        mPlaybackStateChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra(TransistorKeys.EXTRA_PLAYBACK_STATE_CHANGE)) {
                    handlePlaybackStateChanged(intent);
                }
            }
        };
        IntentFilter playbackStateChangedIntentFilter = new IntentFilter(TransistorKeys.ACTION_PLAYBACK_STATE_CHANGED);
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(mPlaybackStateChangedReceiver, playbackStateChangedIntentFilter);
    }

}