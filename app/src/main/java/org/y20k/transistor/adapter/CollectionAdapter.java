/**
 * CollectionAdapter.java
 * Implements the CollectionAdapter class
 * A CollectionAdapter is a custom adapter for a RecyclerView
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-17 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.adapter;

import android.app.Activity;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.y20k.transistor.PlayerFragment;
import org.y20k.transistor.PlayerService;
import org.y20k.transistor.R;
import org.y20k.transistor.core.Station;
import org.y20k.transistor.helpers.DialogError;
import org.y20k.transistor.helpers.ImageHelper;
import org.y20k.transistor.helpers.LogHelper;
import org.y20k.transistor.helpers.ShortcutHelper;
import org.y20k.transistor.helpers.StationContextMenu;
import org.y20k.transistor.helpers.TransistorKeys;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


/**
 * CollectionAdapter class
 */
public final class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapterViewHolder> implements TransistorKeys {

    /* Define log tag */
    private static final String LOG_TAG = CollectionAdapter.class.getSimpleName();


    /* Main class variables */
    private Activity mActivity;
    private final File mFolder;
    private CollectionViewModel mCollectionViewModel;
    private BroadcastReceiver mPlaybackStateChangedReceiver;
    private BroadcastReceiver mMetadataChangedReceiver;
    private Station mStation;
    private Uri mStationUriSelected;
    private String mStationUrlLast;
    private boolean mTwoPane;
    private ArrayList<Station> mStationList;


    /* Constructor */
    public CollectionAdapter(Activity activity, File folder, boolean twoPane, Uri stationUriSelected) {
        // set initial values
        mActivity = activity;
        mFolder = folder;
        mTwoPane = twoPane;
        mStationUriSelected = stationUriSelected;
        mStation = null;

        // create empty station list
        mStationList = new ArrayList<Station>();

        // load state
        loadAppState(mActivity);

        // initialize BroadcastReceiver that listens for playback changes
        initializeBroadcastReceivers();

        // observe changes in LiveData
        mCollectionViewModel = ViewModelProviders.of((AppCompatActivity)mActivity).get(CollectionViewModel.class);
        mCollectionViewModel.getStationList().observe((LifecycleOwner) mActivity, createStationListObserver());
        mCollectionViewModel.getTwoPane().observe((LifecycleOwner)mActivity, createTwoPaneObserver());
//        mCollectionViewModel.getStation().observe((LifecycleOwner)mActivity, createStationObserver());
    }


    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }


    @Override
    public CollectionAdapterViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {

        // get view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_collection, parent, false);

        // put view into holder and return
        return new CollectionAdapterViewHolder(v);
    }


    @Override
    public void onBindViewHolder(CollectionAdapterViewHolder holder, final int position) {

        // get station from position
        final Station station = mStationList.get(holder.getAdapterPosition());

        if (mTwoPane && station.getStreamUri().equals(mStationUriSelected)) {
            holder.getListItemLayout().setSelected(true);
        } else {
            holder.getListItemLayout().setSelected(false);
        }

        // create station image
        Bitmap stationImage;
        Bitmap stationImageSmall = null;
        if (station.getStationImageFile().exists()) {
            stationImageSmall = BitmapFactory.decodeFile(station.getStationImageFile().toString());
        }
        ImageHelper imageHelper = new ImageHelper(stationImageSmall, mActivity);
        stationImage = imageHelper.createCircularFramedImage(192, R.color.transistor_grey_lighter);

        // set station image
        holder.getStationImageView().setImageBitmap(stationImage);

        // set station name
        holder.getStationNameView().setText(station.getStationName());

        // set playback indicator - in phone view only
        if (!mTwoPane && station.getPlaybackState() == PLAYBACK_STATE_LOADING_STATION) {
            holder.getPlaybackIndicator().setBackgroundResource(R.drawable.ic_playback_indicator_small_loading_24dp);
            holder.getPlaybackIndicator().setVisibility(View.VISIBLE);
        } else if (!mTwoPane  && station.getPlaybackState() == PLAYBACK_STATE_STARTED) {
            holder.getPlaybackIndicator().setBackgroundResource(R.drawable.ic_playback_indicator_small_started_24dp);
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
                    menu.initialize(mActivity, view, station);
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
                if (isLongClick && !mTwoPane) {
                    // LONG PRESS in phone mode
                    startPlayback(pos);
                } else if (!isLongClick && !mTwoPane) {
                    // SINGLE TAP in phone mode
                    showPlayerFragment(mStationList.get(pos), false);
                } else {
                    //  SINGLE TAP in tablet mode
                    showPlayerFragment(mStationList.get(pos), false);
                }
                mStationUriSelected = mStationList.get(pos).getStreamUri();
            }
        });

    }


    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public int getItemCount() {
        return mStationList.size();
    }


    /* Shows player fragment with given station */
    public void showPlayerFragment(Station station, boolean startPlayback) {

        Bundle args = new Bundle();
        args.putParcelable(ARG_STATION, station);
        args.putBoolean(ARG_TWO_PANE, mTwoPane);
        args.putBoolean(ARG_PLAYBACK, startPlayback);

        PlayerFragment playerFragment = new PlayerFragment();
        playerFragment.setArguments(args);

        if (mTwoPane) {
            notifyItemChanged(findStationId(mStationUriSelected, mStationList));
            notifyItemChanged(findStationId(station.getStreamUri(), mStationList));
            ((AppCompatActivity)mActivity).getSupportFragmentManager().beginTransaction()
                    .replace(R.id.player_container, playerFragment, PLAYER_FRAGMENT_TAG)
                    .commit();
        } else {
            ((AppCompatActivity)mActivity).getSupportFragmentManager().beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .replace(R.id.main_container, playerFragment, PLAYER_FRAGMENT_TAG)
                    .addToBackStack(null)
                    .commit();
        }

        // save app state
        saveAppState(mActivity);
    }


    /* Handles long click on list item */
    private void startPlayback(int position) {

        if (mStationList.get(position).getPlaybackState() != PLAYBACK_STATE_STOPPED) {
            // stop player service using intent
            Intent intent = new Intent(mActivity, PlayerService.class);
            intent.setAction(ACTION_STOP);
            mActivity.startService(intent);
            LogHelper.v(LOG_TAG, "Stopping player service.");

            // inform user
            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_long_press_playback_stopped), Toast.LENGTH_LONG).show();
        } else {
            // start player service using intent
            Intent intent = new Intent(mActivity, PlayerService.class);
            intent.setAction(ACTION_PLAY);
            intent.putExtra(EXTRA_STATION, mStationList.get(position));
            mActivity.startService(intent);
            LogHelper.v(LOG_TAG, "Starting player service.");

            // inform user
            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_long_press_playback_started), Toast.LENGTH_LONG).show();
        }

        // vibrate 50 milliseconds
        Vibrator v = (Vibrator) mActivity.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(50);
//        v.vibrate(VibrationEffect.createOneShot(50, DEFAULT_AMPLITUDE)); // todo check if there is a support library vibrator

        // save app state
        saveAppState(mActivity);

    }


    /* Loads app state from preferences */
    private void loadAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
//        mTwoPane = settings.getBoolean(PREF_TWO_PANE, false);
//        mStationIdSelected = settings.getInt(PREF_STATION_ID_SELECTED, 0);
        mStationUrlLast = settings.getString(PREF_STATION_URL_LAST, null);
        LogHelper.v(LOG_TAG, "Loading state.");
    }


    /* Saves app state to SharedPreferences */
    private void saveAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
//        editor.putInt(PREF_STATION_ID_SELECTED, mStationIdSelected);
        editor.apply();
        LogHelper.v(LOG_TAG, "Saving state.");
    }


    /* Setter for image file within station object with given ID */
    public void setNewImageFile(int stationId, File stationImageFile) {
        mStationList.get(stationId).setStationImageFile(stationImageFile);
    }


    /* Setter for ID of currently selected station */
    public void setStationSelected(Station station, boolean startPlayback) {
        mStationUriSelected = station.getStreamUri();


        // update playback state
        mStationList.get(findStationId(station.getStreamUri(), mStationList)).setPlaybackState(station.getPlaybackState());

        if (mTwoPane) {
            showPlayerFragment(station, true);
        }

        // startPlayback is set, if started from shortcut
        if (startPlayback) {
            // start player service using intent
            Intent intent = new Intent(mActivity, PlayerService.class);
            intent.setAction(ACTION_PLAY);
            intent.putExtra(EXTRA_STATION, station);
            mActivity.startService(intent);
            LogHelper.v(LOG_TAG, "Starting player service. Special case: Transistor was started by an Intent");
        }
    }


    /* Updates list of stations */
    public void updateStationList(ArrayList<Station> stationList) {
        mStationList.clear();
        mStationList.addAll(stationList);
    }


    /* Getter for list of stations */
    public ArrayList<Station> getStationList() {
        return mStationList;
    }


    /* Getter for Uri of selected station */
    public Uri getStationUriSelected() {
        return mStationUriSelected;
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
        LogHelper.v(LOG_TAG, "No station for given Uri found.");
        return null;
    }



    /* Finds ID of station within list of stations when given its Uri */
    public int findStationId(Uri streamUri, ArrayList<Station> stationList) {

        if (stationList == null) {
            stationList = mStationList;
        }

        // traverse list of stations
        for (int i = 0; i < stationList.size(); i++) {
            Station station = stationList.get(i);
            if (station.getStreamUri().equals(streamUri)) {
                return i;
            }
        }

        // return -1 if nothing was found
        LogHelper.v(LOG_TAG, "No station ID for given Uri found.");
        return -1;
    }


    /* Getter for station of given ID */
//    public Station getStation (int stationId) {
//        return mStationList.get(stationId);
//    }


    /* Add station to collection */
    public int add(Station station) {
        if (station.getStationName() != null && station.getStreamUri() != null) {
            // create copy of main list of stations
            ArrayList<Station> newStationList = copyStationList(mStationList);
            // add station to new list of stations
            newStationList.add(station);
            // sort list
            sortStationList(mStationList);
            // update live data list of stations
            mCollectionViewModel.getStationList().setValue(newStationList);
            // return new index
            return findStationId(station.getStreamUri(), newStationList);
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


    /* Rename station within collection */
    public int rename(String newStationName, Station station) {

        // name of station is new
        if (station != null && !station.getStationName().equals(newStationName)) {

            // create copy of main list of stations
            ArrayList<Station> newStationList = copyStationList(mStationList);

            // get new station
            int newStationId = findStationId(station.getStreamUri(), newStationList);

            // get reference to old files
            File oldStationPlaylistFile = mStationList.get(newStationId).getStationPlaylistFile();
            File oldStationImageFile = mStationList.get(newStationId).getStationImageFile();

            // update new station
            mStationList.get(newStationId).setStationName(newStationName);
            mStationList.get(newStationId).setStationPlaylistFile(mFolder);
            mStationList.get(newStationId).setStationImageFile(mFolder);

            // rename playlist file
            oldStationPlaylistFile.delete();
            mStationList.get(newStationId).writePlaylistFile(mFolder);

            // rename image file
            oldStationImageFile.renameTo(mStationList.get(newStationId).getStationImageFile());

            // sort list
            sortStationList(mStationList);

            // update live data list of stations
            mCollectionViewModel.getStationList().setValue(newStationList);

            // return id of changed station
            return findStationId(station.getStreamUri(), newStationList);

        } else {
            // name of station is null or not new - notify user
            Toast.makeText(mActivity, mActivity.getString(R.string.toastalert_rename_unsuccessful), Toast.LENGTH_LONG).show();
            return -1;
        }

    }


    /* Delete station within collection */
    public int delete(Station station) {

        boolean success = false;
        int stationId = findStationId(station.getStreamUri(), mStationList);

        // delete playlist file
        File stationPlaylistFile = station.getStationPlaylistFile();
        if (stationPlaylistFile.exists() && stationPlaylistFile.delete()) {
            success = true;
        }

        // delete station image file
        File stationImageFile = station.getStationImageFile();
        if (stationImageFile.exists() && stationImageFile.delete()) {
            success = true;
        }

        // remove station and notify user
        if (success) {
            // create copy of main list of stations
            ArrayList<Station> newStationList = copyStationList(mStationList);
            // remove station from new station list
            newStationList.remove(stationId);
            // update live data list of stations
            mCollectionViewModel.getStationList().setValue(newStationList);
            // notify user
            Toast.makeText(mActivity, mActivity.getString(R.string.toastalert_delete_successful), Toast.LENGTH_LONG).show();
        }

        // delete station shortcut
        ShortcutHelper shortcutHelper = new ShortcutHelper(mActivity);
        shortcutHelper.removeShortcut(station);

        if (mTwoPane) {
            // determine ID of next station to display in two pane mode
            if (mStationList.size() >= stationId) {
                stationId--;
            }

            if (stationId >= 0) {
                // show next station
                Bundle args = new Bundle();
                args.putParcelable(ARG_STATION, mStationList.get(stationId));
                args.putInt(ARG_STATION_ID, stationId);
                args.putBoolean(ARG_TWO_PANE, mTwoPane);
                PlayerFragment playerFragment = new PlayerFragment();
                playerFragment.setArguments(args);
                ((AppCompatActivity)mActivity).getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_container, playerFragment, PLAYER_FRAGMENT_TAG)
                        .commit();
            }
        }

        // return ID of next station
        return stationId;
    }


    /* Sorts the list of stations */
    private void sortStationList(ArrayList<Station> stationList) {
        Collections.sort(stationList, new Comparator<Station>() {
            @Override
            public int compare(Station station1, Station station2) {
                // Compares two stations: returns "1" if name if this station is greater than name of given station
                return station1.getStationName().compareToIgnoreCase(station2.getStationName());
            }
        });
    }


    /* Initializes and registers broadcast receivers */
    private void initializeBroadcastReceivers() {

        // RECEIVER: state of playback has changed
        mPlaybackStateChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra(EXTRA_STATION)) {
                    handlePlaybackStateChange(intent);
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


    /* Creates a real copy of given station list*/
    private ArrayList<Station> copyStationList(ArrayList<Station> stationList) {
        ArrayList<Station> newStationList = new ArrayList<Station>();
        for (Station station : stationList) {
            newStationList.add(new Station (station));
        }
        return newStationList;
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

        // update station in live data
        mCollectionViewModel.getStation().setValue(station);
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

        // create copy of main list of stations
        ArrayList<Station> newStationList = copyStationList(mStationList);

        // try to set playback state of previous station
        if (intent.hasExtra(EXTRA_PLAYBACK_STATE_PREVIOUS_STATION)) {
            String previousStationUrlString = intent.getStringExtra(EXTRA_PLAYBACK_STATE_PREVIOUS_STATION);
            if (previousStationUrlString != null) {
                int previousStationId = findStationId(Uri.parse(previousStationUrlString), newStationList);
                if (previousStationId != -1) {
                    newStationList.get(previousStationId).setPlaybackState(PLAYBACK_STATE_STOPPED);
                }
            }
        }

        // set playback state for new station
        int stationId = findStationId(station.getStreamUri(), newStationList);
        if (stationId != -1) {
            newStationList.get(stationId).setPlaybackState(station.getPlaybackState());
        }

        // update live data station list
        mCollectionViewModel.getStationList().setValue(newStationList);

        // update live data station list
        mCollectionViewModel.getStation().setValue(station);
    }


    /* Creates an observer for collection of stations stored as LiveData */
    private Observer<ArrayList<Station>> createStationListObserver() {
        return new Observer<ArrayList<Station>>() {
            @Override
            public void onChanged(@Nullable ArrayList<Station> newStationList) {
                LogHelper.v(LOG_TAG, "Observer for list of stations in CollectionAdapter: list has changed.");
                sortStationList(newStationList);
                // calculate differences between new station list and current station list
                DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new CollectionAdapterDiffUtilCallback(mStationList, newStationList), true);
                // update current station list
                mStationList = newStationList;
                // inform this adapter about the changes
                diffResult.dispatchUpdatesTo(CollectionAdapter.this);

                // first run: get mStationUrlSelected
                if (mStationUriSelected == null && newStationList.size() != 0) {
                    mStationUriSelected = newStationList.get(0).getStreamUri();
                    // todo set recyclerview selected
                }
           }
        };
    }


    /* Creates an observer for state of two pane layout stored as LiveData */
    private Observer<Boolean> createTwoPaneObserver() {
        return new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean twoPane) {
                LogHelper.v(LOG_TAG, "Observer for two pane layout in CollectionAdapter: layout has changed. State mTwoPane:" + twoPane);
                mTwoPane = twoPane;
                // todo change layout
            }
        };
    }

}