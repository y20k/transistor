/**
 * ListFragment.java
 * Implements the main fragment of the main activity
 * This fragment implements a RecyclerView list of radio stations
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-17 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor;

import android.app.Activity;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.y20k.transistor.adapter.CollectionAdapter;
import org.y20k.transistor.adapter.CollectionViewModel;
import org.y20k.transistor.core.Station;
import org.y20k.transistor.helpers.DialogAdd;
import org.y20k.transistor.helpers.LogHelper;
import org.y20k.transistor.helpers.PermissionHelper;
import org.y20k.transistor.helpers.SleepTimerService;
import org.y20k.transistor.helpers.StationFetcher;
import org.y20k.transistor.helpers.StationListHelper;
import org.y20k.transistor.helpers.StorageHelper;
import org.y20k.transistor.helpers.TransistorKeys;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


/**
 * ListFragment class
 */
public final class ListFragment extends Fragment implements TransistorKeys {

    /* Define log tag */
    private static final String LOG_TAG = ListFragment.class.getSimpleName();


    /* Main class variables */
    private Activity mActivity;
    private StorageHelper mStorageHelper;
    private CollectionAdapter mCollectionAdapter = null;
    private View mRootView;
    private View mActionCallView;
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private CollectionViewModel mCollectionViewModel;
    private BroadcastReceiver mSleepTimerStartedReceiver;
    private Station mPlayerServiceStation;
    private Uri mNewStationUri;
    private boolean mTwoPane;
    private boolean mSleepTimerRunning;
    private String mSleepTimerNotificationMessage;
    private Snackbar mSleepTimerNotification;


    /* Constructor (default) */
    public ListFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // fragment has options menu
        setHasOptionsMenu(true);

        // get activity and application contexts
        mActivity = getActivity();

        // get notification message
        mSleepTimerNotificationMessage = mActivity.getString(R.string.snackbar_message_timer_set) + " ";

        // initialize two pane
        Bundle args = getArguments();
        if (args != null && args.containsKey(ARG_TWO_PANE)) {
            mTwoPane = args.getBoolean(ARG_TWO_PANE, false);
        } else {
            mTwoPane = false;
        }

        // initialize StorageHelper
        mStorageHelper = new StorageHelper(mActivity);

        // load playback state
        loadAppState(mActivity);

        // create collection adapter
        mCollectionAdapter = new CollectionAdapter(mActivity, mTwoPane, null);

        // initialize broadcast receivers
        initializeBroadcastReceivers();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // inflate root view from xml
        mRootView = inflater.inflate(R.layout.fragment_list, container, false);

        // get reference to action call view from inflated root view
        mActionCallView = mRootView.findViewById(R.id.collection_actioncall_layout);

        // get reference to recycler list view from inflated root view
        mRecyclerView = (RecyclerView) mRootView.findViewById(R.id.list_recyclerview);

        // set animator
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        // use a linear layout manager - turn PredictiveItemAnimations on
        mLayoutManager = new LinearLayoutManager(mActivity) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return true;
            }
        };
        mRecyclerView.setLayoutManager(mLayoutManager);

        // associate RecyclerView with CollectionAdapter
        mRecyclerView.setAdapter(mCollectionAdapter);

        // observe changes in LiveData
        mCollectionViewModel = ViewModelProviders.of((AppCompatActivity) mActivity).get(CollectionViewModel.class);
        mCollectionViewModel.getStationList().observe((LifecycleOwner) mActivity, createStationListObserver());
        mCollectionViewModel.getPlayerServiceStation().observe((LifecycleOwner) mActivity, createStationObserver());

        // show call to action, if necessary
        toggleActionCall();

        return mRootView;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // inflate the menu items for use in action bar
        inflater.inflate(R.menu.menu_list_actionbar, menu);
    }


    @Override
    public void onResume() {
        super.onResume();

        // set title of activity in ActionBar
        mActivity.setTitle(R.string.app_name);

        // refresh app state
        loadAppState(mActivity);

        // handles the activity's intent
        Intent intent = mActivity.getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            handleStreamingLink(intent);
        } else if (ACTION_SHOW_PLAYER.equals(intent.getAction())) {
            handleShowPlayer(intent);
        }

        // show notification bar if timer is running
        if (mSleepTimerRunning) {
            showSleepTimerNotification(-1);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        // unregister Broadcast Receivers
        unregisterBroadcastReceivers();
        mCollectionAdapter.unregisterBroadcastReceivers(mActivity);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            // CASE TIMER
            case R.id.menu_timer:
                handleMenuSleepTimerClick();
                return true;

            // CASE ADD
            case R.id.menu_add:

                DialogAdd dialog = new DialogAdd(mActivity, mStorageHelper.getCollectionDirectory());
                dialog.show();
                return true;

            // CASE ABOUT
            case R.id.menu_about:
                // put title and content into arguments and start fragment transaction
                String aboutTitle = mActivity.getString(R.string.header_about);
                Bundle aboutArgs = new Bundle();
                aboutArgs.putString(ARG_INFOSHEET_TITLE, aboutTitle);
                aboutArgs.putInt(ARG_INFOSHEET_CONTENT, INFOSHEET_CONTENT_ABOUT);

                InfosheetFragment aboutInfosheetFragment = new InfosheetFragment();
                aboutInfosheetFragment.setArguments(aboutArgs);

                ((AppCompatActivity) mActivity).getSupportFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .replace(R.id.main_container, aboutInfosheetFragment, INFOSHEET_FRAGMENT_TAG)
                        .addToBackStack(null)
                        .commit();
                return true;

            // CASE HOWTO
            case R.id.menu_howto:
                // put title and content into arguments and start fragment transaction
                String howToTitle = mActivity.getString(R.string.header_howto);
                Bundle howtoArgs = new Bundle();
                howtoArgs.putString(ARG_INFOSHEET_TITLE, howToTitle);
                howtoArgs.putInt(ARG_INFOSHEET_CONTENT, INFOSHEET_CONTENT_ABOUT);

                InfosheetFragment howtoInfosheetFragment = new InfosheetFragment();
                howtoInfosheetFragment.setArguments(howtoArgs);

                ((AppCompatActivity) mActivity).getSupportFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .replace(R.id.main_container, howtoInfosheetFragment, INFOSHEET_FRAGMENT_TAG)
                        .addToBackStack(null)
                        .commit();
                return true;

            // CASE REFRESH LIST
            case R.id.menu_refresh:
                // stop player service using intent
                Intent intent = new Intent(mActivity, PlayerService.class);
                intent.setAction(ACTION_DISMISS);
                mActivity.startService(intent);

                // manually refresh list of stations (force reload) - useful when editing playlist files manually outside of Transistor
                ArrayList<Station> newStationList = StationListHelper.loadStationListFromStorage(mActivity);
                mCollectionViewModel.getStationList().setValue(newStationList);

                // update player fragment in tablet mode
                if (mTwoPane && newStationList.size() > 0) {
                    mCollectionAdapter.showPlayerFragment(newStationList.get(0), false);
                }

                // check list if is empty and show action call if so
                ((MainActivity) mActivity).togglePlayerContainerVisibility();
                toggleActionCall();


                // notify user
                Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_list_refreshed), Toast.LENGTH_LONG).show();

                return true;

            // CASE DEFAULT
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_REQUEST_STATION_FETCHER_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted - fetch station from given Uri
                    fetchNewStation(mNewStationUri);
                } else {
                    // permission denied
                    Toast.makeText(mActivity, mActivity.getString(R.string.toastalert_permission_denied) + " READ_EXTERNAL_STORAGE", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }


    /* Updates list state after delete */
    public void updateListAfterDelete(Station newStation, int stationId) {
        mCollectionAdapter.setStationUriSelected(newStation.getStreamUri());
    }


    /* Show or hide call to action view if necessary */
    private void toggleActionCall() {
        if (mStorageHelper.storageHasStationPlaylistFiles()) {
            mActionCallView.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        } else {
            mActionCallView.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        }
    }


    /* Handles tap on streaming link */
    private void handleStreamingLink(Intent intent) {
        mNewStationUri = intent.getData();

        // clear the intent
        intent.setAction("");

        // check for null and type "http"
        if (mNewStationUri != null && mNewStationUri.getScheme().startsWith("http")) {
            // download and add new station
            fetchNewStation(mNewStationUri);
        } else if (mNewStationUri != null && mNewStationUri.getScheme().startsWith("file")) {
            // check for read permission
            PermissionHelper permissionHelper = new PermissionHelper(mActivity, mRootView);
            if (permissionHelper.requestReadExternalStorage(PERMISSION_REQUEST_STATION_FETCHER_READ_EXTERNAL_STORAGE)) {
                // read and add new station
                fetchNewStation(mNewStationUri);
            }
        }
        // unsuccessful - log failure
        else {
            LogHelper.i(LOG_TAG, "Received an empty intent");
        }
    }


    /* Handles intent to show player from notification or from shortcut */
    private void handleShowPlayer(Intent intent) {

        Station station = null;
        boolean startPlayback = false;

        // CASE: user tapped on notification
        if (intent.hasExtra(EXTRA_STATION)) {
            // get station from notification
            station = intent.getParcelableExtra(EXTRA_STATION);
            startPlayback = false;
        }
        // CASE: playback requested via homescreen shortcut
        else if (intent.hasExtra(EXTRA_STREAM_URI)) {
            // get Uri of station from home screen shortcut
            station = StationListHelper.findStation(mCollectionAdapter.getStationList(), Uri.parse(intent.getStringExtra(EXTRA_STREAM_URI)));
            startPlayback = true;
        }
        // CASE: transistor received a last station intent
        else if (intent.hasExtra(EXTRA_LAST_STATION)) {
            // try to get last station from SharedPreferences
            String stationUrlLastString = PreferenceManager.getDefaultSharedPreferences(mActivity).getString(PREF_STATION_URL_LAST, null);
            loadAppState(mActivity);
            if (stationUrlLastString != null) {
                station = StationListHelper.findStation(mCollectionAdapter.getStationList(), Uri.parse(stationUrlLastString));
            }
            startPlayback = true;
        }

        // show player and clear the intent
        if (station != null) {
            mCollectionAdapter.showPlayerFragment(station, startPlayback);
            intent.setAction("");
        } else {
            Toast.makeText(mActivity, getString(R.string.toastalert_station_not_found), Toast.LENGTH_LONG).show();
        }

    }


    /* Handles tap timer icon in actionbar */
    private void handleMenuSleepTimerClick() {
        // set duration
        long duration = FIFTEEN_MINUTES;

        // CASE: No station is playing, no timer is running
        if (mPlayerServiceStation == null || (mPlayerServiceStation.getPlaybackState() == PLAYBACK_STATE_STOPPED && !mSleepTimerRunning)) {
            // unable to start timer
            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_timer_start_unable), Toast.LENGTH_SHORT).show();
        }
        // CASE: A station is playing, no sleep timer is running
        else if (mPlayerServiceStation != null && mPlayerServiceStation.getPlaybackState() != PLAYBACK_STATE_STOPPED && !mSleepTimerRunning) {
            startSleepTimer(duration);
            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_timer_activated), Toast.LENGTH_SHORT).show();
        }
        // CASE: A station is playing, Sleep timer is running
        else if (mPlayerServiceStation != null && mPlayerServiceStation.getPlaybackState() == PLAYBACK_STATE_STARTED) {
            startSleepTimer(duration);
            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_timer_duration_increased) + " [+" + getReadableTime(duration) + "]", Toast.LENGTH_SHORT).show();
        }
    }


    /* Starts timer service and notification */
    private void startSleepTimer(long duration) {
        // start sleep timer service using intent
        Intent intent = new Intent(mActivity, SleepTimerService.class);
        intent.setAction(ACTION_TIMER_START);
        intent.putExtra(EXTRA_TIMER_DURATION, duration);
        mActivity.startService(intent);

        // show timer notification
        showSleepTimerNotification(duration);
        mSleepTimerRunning = true;
        LogHelper.v(LOG_TAG, "Starting timer service and notification.");
    }


    /* Stops timer service and notification */
    private void stopSleepTimer() {
        // stop sleep timer service using intent
        Intent intent = new Intent(mActivity, SleepTimerService.class);
        intent.setAction(ACTION_TIMER_STOP);
        mActivity.startService(intent);

        // cancel notification
        if (mSleepTimerNotification != null && mSleepTimerNotification.isShown()) {
            mSleepTimerNotification.dismiss();
        }
        mSleepTimerRunning = false;
        LogHelper.v(LOG_TAG, "Stopping timer service and notification.");
        Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_timer_cancelled), Toast.LENGTH_SHORT).show();
    }


    /* Shows notification for a running sleep timer */
    private void showSleepTimerNotification(long remainingTime) {

        // set snackbar message
        String message;
        if (remainingTime > 0) {
            message = mSleepTimerNotificationMessage + getReadableTime(remainingTime);
        } else {
            message = mSleepTimerNotificationMessage;
        }

        // show snackbar
        mSleepTimerNotification = Snackbar.make(mRootView, message, Snackbar.LENGTH_INDEFINITE);
        mSleepTimerNotification.setAction(R.string.dialog_generic_button_cancel, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // stop sleep timer service using intent
                Intent intent = new Intent(mActivity, SleepTimerService.class);
                intent.setAction(ACTION_TIMER_STOP);
                mActivity.startService(intent);
                mSleepTimerRunning = false;
                // notify user
                Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_timer_cancelled), Toast.LENGTH_SHORT).show();
                LogHelper.v(LOG_TAG, "Sleep timer cancelled.");
            }
        });
        mSleepTimerNotification.show();

    }


    /* Translates milliseconds into minutes and seconds */
    private String getReadableTime(long remainingTime) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(remainingTime),
                TimeUnit.MILLISECONDS.toSeconds(remainingTime) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(remainingTime)));
    }


    /* Loads app state from preferences */
    private void loadAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        mTwoPane = settings.getBoolean(PREF_TWO_PANE, false);
        mSleepTimerRunning = settings.getBoolean(PREF_TIMER_RUNNING, false);
        LogHelper.v(LOG_TAG, "Loading state.");
    }


    /* Saves app state to SharedPreferences */
    private void saveAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.apply();
        LogHelper.v(LOG_TAG, "Saving state.");
    }


    /* Fetch new station with given Uri */
    private void fetchNewStation(Uri stationUri) {
        // download and add new station
        StationFetcher stationFetcher = new StationFetcher(mActivity, mStorageHelper.getCollectionDirectory(), stationUri);
        stationFetcher.execute();
    }


    /* Creates an observer for collection of stations stored as LiveData */
    private Observer<ArrayList<Station>> createStationListObserver() {
        return new Observer<ArrayList<Station>>() {
            @Override
            public void onChanged(@Nullable ArrayList<Station> newStationList) {
                // toggle action call view if necessary
                toggleActionCall();
            }
        };
    }


    /* Creates an observer for state of two pane layout stored as LiveData */
    private Observer<Boolean> createTwoPaneObserver() {
        return new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean twoPane) {
                LogHelper.v(LOG_TAG, "Observer for two pane layout in ListFragment: layout has changed. ");
                mTwoPane = twoPane;
                // todo change layout
            }
        };
    }


    /* Creates an observer for station from player service stored as LiveData */
    private Observer<Station> createStationObserver() {
        return new Observer<Station>() {
            @Override
            public void onChanged(@Nullable Station newStation) {
                mPlayerServiceStation = newStation;
                // stop sleep timer - if necessary
                if (mSleepTimerRunning && mPlayerServiceStation != null && mPlayerServiceStation.getPlaybackState() == PLAYBACK_STATE_STOPPED) {
                    stopSleepTimer();
                }
            }
        };
    }


    /* Initializes broadcast receivers for onCreate */
    private void initializeBroadcastReceivers() {
        // RECEIVER: sleep timer service sends updates
        mSleepTimerStartedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // get duration from intent
                long remaining = intent.getLongExtra(EXTRA_TIMER_REMAINING, 0);
                if (mSleepTimerNotification != null && remaining > 0) {
                    // update existing notification
                    mSleepTimerNotification.setText(mSleepTimerNotificationMessage + getReadableTime(remaining));
                } else if (mSleepTimerNotification != null) {
                    // cancel notification
                    mSleepTimerNotification.dismiss();
                    mSleepTimerRunning = false;
                }

            }
        };
        IntentFilter sleepTimerIntentFilter = new IntentFilter(ACTION_TIMER_RUNNING);
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(mSleepTimerStartedReceiver, sleepTimerIntentFilter);
    }


    /* Unregisters broadcast receivers */
    private void unregisterBroadcastReceivers() {
        LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(mSleepTimerStartedReceiver);
    }

}

