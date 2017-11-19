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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.Group;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.y20k.transistor.adapter.CollectionAdapter;
import org.y20k.transistor.adapter.CollectionViewModel;
import org.y20k.transistor.core.Station;
import org.y20k.transistor.helpers.DialogAdd;
import org.y20k.transistor.helpers.ImageHelper;
import org.y20k.transistor.helpers.LogHelper;
import org.y20k.transistor.helpers.PermissionHelper;
import org.y20k.transistor.helpers.SleepTimerService;
import org.y20k.transistor.helpers.StationContextMenu;
import org.y20k.transistor.helpers.StationFetcher;
import org.y20k.transistor.helpers.StationListHelper;
import org.y20k.transistor.helpers.StorageHelper;
import org.y20k.transistor.helpers.TransistorKeys;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import be.rijckaert.tim.animatedvector.FloatingMusicActionButton;


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
    private View mPlayerBottomSheet;
    private ImageView mPlayerStationImage;
    private TextView mPlayerStationName;
    private TextView mPlayerStationMetadata;
    private FloatingMusicActionButton mPlayerPlaybackButton;
    private ImageButton mPlayerExpandButton;
    private Group mPlaybackActiveViews;
    private Group mPlayerSheetMetadataViews;
    private Group mPlayerSheetStreamUrlViews;
    private ImageButton mPlayerSheetSleepTimerButton;
    private ImageButton mPlayerSheetStationOptionsButton;
    private ImageButton mPlayerSheetMetadataCopyButton;
    private ImageButton mPlayerSheetStreamUrlCopyButton;
    private TextView mPlayerSessionValue;
    private TextView mPlayerSheetMetadataValue;
    private TextView mPlayerSheetStreamUrlValue;
    private TextView mPlayerSheetChannelCountValue;
    private TextView mPlayerSheetSamplerateValue;
    private TextView mPlayerSheetBitrateValue;
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private CollectionViewModel mCollectionViewModel;
    private BottomSheetBehavior mPlayerBottomSheetBehavior;
    private BroadcastReceiver mSleepTimerStartedReceiver;
    private String mCurrentStationUrl;
    private Station mCurrentStation = null;
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
//        setHasOptionsMenu(true); // TODO just a test
//        setHasOptionsMenu(false);

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

        // get needed references to views
        mActionCallView = mRootView.findViewById(R.id.collection_actioncall_layout);
        mRecyclerView = mRootView.findViewById(R.id.list_recyclerview);
        mPlayerExpandButton = mRootView.findViewById(R.id.player_button_expand);
        mPlayerStationImage = mRootView.findViewById(R.id.player_station_image);
        mPlayerStationName = mRootView.findViewById(R.id.player_station_name);
        mPlayerStationMetadata = mRootView.findViewById(R.id.player_station_metadata);
        mPlayerPlaybackButton = mRootView.findViewById(R.id.player_button_playback);
        mPlaybackActiveViews = mRootView.findViewById(R.id.playback_active_views);
        mPlayerBottomSheet = mRootView.findViewById(R.id.player_sheet);
        mPlayerSheetMetadataViews = mRootView.findViewById(R.id.player_sheet_metadata_views);
        mPlayerSheetStreamUrlViews = mRootView.findViewById(R.id.player_sheet_stream_url_views);
        mPlayerSheetSleepTimerButton = mRootView.findViewById(R.id.player_sheet_timer_button);
        mPlayerSheetStationOptionsButton = mRootView.findViewById(R.id.player_sheet_station_options_button);
        mPlayerSheetMetadataCopyButton = mRootView.findViewById(R.id.player_sheet_metadata_copy_button);
        mPlayerSheetStreamUrlCopyButton = mRootView.findViewById(R.id.player_sheet_stream_url_copy_button);
        mPlayerSessionValue = mRootView.findViewById(R.id.player_sheet_p_session);
        mPlayerSheetMetadataValue = mRootView.findViewById(R.id.player_sheet_p_metadata);
        mPlayerSheetStreamUrlValue = mRootView.findViewById(R.id.player_sheet_p_stream_url);
        mPlayerSheetChannelCountValue = mRootView.findViewById(R.id.player_sheet_p_channels);
        mPlayerSheetSamplerateValue = mRootView.findViewById(R.id.player_sheet_p_samplerate);
        mPlayerSheetBitrateValue = mRootView.findViewById(R.id.player_sheet_p_bitrate);

        // setuo RecyclerView
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mLayoutManager = new LinearLayoutManager(mActivity) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return true;
            }
        };
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mCollectionAdapter);


        // listen for taps on list
        mCollectionAdapter.setCollectionAdapterListener(new CollectionAdapter.CollectionAdapterListener() {
            @Override
            public void itemSelected(Station station) {
                mCurrentStation = station;
                setupPlayer(station);
            }
        });


        // observe changes in LiveData
        mCollectionViewModel = ViewModelProviders.of((AppCompatActivity) mActivity).get(CollectionViewModel.class);
        mCollectionViewModel.getStationList().observe((LifecycleOwner) mActivity, createStationListObserver());
        mCollectionViewModel.getPlayerServiceStation().observe((LifecycleOwner) mActivity, createStationObserver());

        // show call to action, if necessary
        toggleActionCall(); // todo remove

        // set up and show station data sheet
        mPlayerBottomSheetBehavior = BottomSheetBehavior.from(mPlayerBottomSheet);
        mPlayerBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        mPlayerBottomSheetBehavior.setBottomSheetCallback(getPlayerBottomSheetCallback());
        mPlayerExpandButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPlayerBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    mPlayerBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                } else {
                    mPlayerBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        });

        // attach listeners (for clipboard copy)
        mPlayerSheetMetadataCopyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(COPY_STATION_METADATA);
                mPlayerBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
        mPlayerSheetStreamUrlCopyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(COPY_STREAM_URL);
                mPlayerBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        // attach listener for sleep timer tap
        mPlayerSheetSleepTimerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleSleepTimerTap();
                mPlayerBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        // attach listener for option menu
        mPlayerSheetStationOptionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StationContextMenu menu = new StationContextMenu();
                menu.initialize(mActivity, view, mCurrentStation);
                menu.show();
            }
        });

        // attach listener to playback button
        mPlayerPlaybackButton.setOnMusicFabClickListener(new FloatingMusicActionButton.OnMusicFabClickListener() {
            @Override
            public void onClick(@NotNull View view) {
                handlePlaybackButtonTap();
            }
        });

        // initial set-up of player
//        setupPlayer(mCurrentStation, mCurrentStation.getPlaybackState());

        return mRootView;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // inflate the menu items for use in action bar
        // inflater.inflate(R.menu.menu_list_actionbar, menu);
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
                handleSleepTimerTap();
                return true;

            // CASE ADD
            case R.id.menu_add:

                DialogAdd dialog = new DialogAdd(mActivity, mStorageHelper.getCollectionDirectory());
                dialog.show();
                return true;

//            // CASE ABOUT
//            case R.id.menu_about:
//                // put title and content into arguments and start fragment transaction
//                String aboutTitle = mActivity.getString(R.string.header_about);
//                Bundle aboutArgs = new Bundle();
//                aboutArgs.putString(ARG_INFOSHEET_TITLE, aboutTitle);
//                aboutArgs.putInt(ARG_INFOSHEET_CONTENT, INFOSHEET_CONTENT_ABOUT);
//
//                InfosheetFragment aboutInfosheetFragment = new InfosheetFragment();
//                aboutInfosheetFragment.setArguments(aboutArgs);
//
//                ((AppCompatActivity) mActivity).getSupportFragmentManager().beginTransaction()
//                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
//                        .replace(R.id.main_container, aboutInfosheetFragment, INFOSHEET_FRAGMENT_TAG)
//                        .addToBackStack(null)
//                        .commit();
//                return true;
//
//            // CASE HOWTO
//            case R.id.menu_howto:
//                // put title and content into arguments and start fragment transaction
//                String howToTitle = mActivity.getString(R.string.header_howto);
//                Bundle howtoArgs = new Bundle();
//                howtoArgs.putString(ARG_INFOSHEET_TITLE, howToTitle);
//                howtoArgs.putInt(ARG_INFOSHEET_CONTENT, INFOSHEET_CONTENT_HOWTO);
//
//                InfosheetFragment howtoInfosheetFragment = new InfosheetFragment();
//                howtoInfosheetFragment.setArguments(howtoArgs);
//
//                ((AppCompatActivity) mActivity).getSupportFragmentManager().beginTransaction()
//                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
//                        .replace(R.id.main_container, howtoInfosheetFragment, INFOSHEET_FRAGMENT_TAG)
//                        .addToBackStack(null)
//                        .commit();
//                return true;

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


    /* start player service using intent */
    private void startPlayback(Station station) {
        Intent intent = new Intent(mActivity, PlayerService.class);
        intent.setAction(ACTION_PLAY);
        intent.putExtra(EXTRA_STATION, station);
        mActivity.startService(intent);
        LogHelper.v(LOG_TAG, "Starting player service.");
    }


    /* Stop player service using intent */
    private void stopPlayback() {
        Intent intent = new Intent(mActivity, PlayerService.class);
        intent.setAction(ACTION_STOP);
        mActivity.startService(intent);
        LogHelper.v(LOG_TAG, "Stopping player service.");
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
            // todo implement
//            mCollectionAdapter.showPlayerFragment(station, startPlayback);
            intent.setAction("");
        } else {
            Toast.makeText(mActivity, getString(R.string.toastalert_station_not_found), Toast.LENGTH_LONG).show();
        }

    }



    /* Handles playback button taps */
    private void handlePlaybackButtonTap() {
        switch (mPlayerPlaybackButton.getCurrentMode()) {
            case PLAY_TO_STOP: {
                LogHelper.w(LOG_TAG, "!!! tapped on start"); // todo remove
                startPlayback(mCurrentStation);
                mPlayerPlaybackButton.changeMode(FloatingMusicActionButton.Mode.STOP_TO_PLAY);
                break;
            }
            case STOP_TO_PLAY: {
                LogHelper.w(LOG_TAG, "!!! tapped on stop"); // todo remove
                // stop playback and collapse bottom sheet
                stopPlayback();
                mPlayerPlaybackButton.changeMode(FloatingMusicActionButton.Mode.PLAY_TO_STOP);
                mPlayerBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                break;
            }
        }
    }


    /* Setup player visually */
    private void setupPlayer(Station station) {

        if (station != null) {
            // show player
            // todo implement

            LogHelper.w(LOG_TAG, "!!! setting up player. state = " + station.getPlaybackState()); // todo remove

            // set station name, image and stream url
            mPlayerStationName.setText(station.getStationName());
            mPlayerStationImage.setImageBitmap(createStationImage(station));
            mPlayerSheetStreamUrlValue.setText(station.getStreamUri().toString());

            // toggle views depending on playback state
            updateStationPlaybackState(station);

        } else {
            // hide player
            // todo implement
        }
    }


    /* Update the playback state name */
    private void updateStationPlaybackState(Station station) {
        if (isAdded()) {
            LogHelper.w(LOG_TAG, "!!! updating playbackstate = " + station.getPlaybackState()); // todo remove

            // toggle views needed for active playback
            switch (station.getPlaybackState()) {
                case PLAYBACK_STATE_STOPPED: {
                    mPlaybackActiveViews.setVisibility(View.GONE);
                    if (mPlayerPlaybackButton.getCurrentMode() == FloatingMusicActionButton.Mode.STOP_TO_PLAY) {
                        LogHelper.w(LOG_TAG, "!!! PLAYBACK_STATE_STOPPED & changing button mode"); // todo remove
                        mPlayerPlaybackButton.changeMode(FloatingMusicActionButton.Mode.PLAY_TO_STOP);
                    }
                    break;
                }
                case PLAYBACK_STATE_LOADING_STATION: {
                    mPlaybackActiveViews.setVisibility(View.VISIBLE);
                    if (mPlayerPlaybackButton.getCurrentMode() == FloatingMusicActionButton.Mode.PLAY_TO_STOP) {
                        LogHelper.w(LOG_TAG, "!!! PLAYBACK_STATE_LOADING_STATION & changing button mode"); // todo remove
                        mPlayerPlaybackButton.changeMode(FloatingMusicActionButton.Mode.STOP_TO_PLAY);
                    }
                    break;
                }
                case PLAYBACK_STATE_STARTED: {
                    mPlaybackActiveViews.setVisibility(View.VISIBLE);
                    if (mPlayerPlaybackButton.getCurrentMode() == FloatingMusicActionButton.Mode.PLAY_TO_STOP) {
                        LogHelper.w(LOG_TAG, "!!! PLAYBACK_STATE_STARTED & changing button mode"); // todo remove
                        mPlayerPlaybackButton.changeMode(FloatingMusicActionButton.Mode.STOP_TO_PLAY);
                    }
                    break;
                }
            }

        }
    }


    /* Update station name */
    private void updateStationNameView(Station station) {
        if (isAdded()) {
            mPlayerStationName.setText(station.getStationName());
            // mStationDataSheetName.setText(station.getStationName()); // todo remove
        }
    }


    /* Update station image */
    private void updateStationImageView(Station station) {
        if (isAdded()) {
            Bitmap stationImage = createStationImage(station);
            if (stationImage != null) {
                mPlayerStationImage.setImageBitmap(stationImage);
            }
        }
    }


    /* Update station metadata */
    private void updateStationMetadataView(Station station) {
        if (isAdded()) {
            mPlayerStationMetadata.setText(station.getMetadata());
            mPlayerSheetMetadataValue.setText(station.getMetadata());
        }
    }


    /* Create Bitmap image for station */
    private Bitmap createStationImage(Station station) {
        Bitmap stationImageSmall;
        ImageHelper imageHelper;
        if (station != null &&  station.getStationImageFile().exists()) {
            // get image from collection
            stationImageSmall = BitmapFactory.decodeFile(station.getStationImageFile().toString());
        } else {
            // get default image
            stationImageSmall = null;
        }
        imageHelper = new ImageHelper(stationImageSmall, mActivity);

        return imageHelper.createCircularFramedImage(192, R.color.transistor_grey_lighter);
    }


    /* Handles tap timer icon in actionbar */
    private void handleSleepTimerTap() {
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
        mCurrentStationUrl = settings.getString(PREF_STATION_URI_SELECTED, null);
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


    /* Creates BottomSheetCallback for the player sheet - needed in onCreateView */
    private BottomSheetBehavior.BottomSheetCallback getPlayerBottomSheetCallback() {
        return new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                // react to state change
                switch (newState) {
                    case BottomSheetBehavior.STATE_EXPANDED:
                        // details sheet expanded
                        mPlayerExpandButton.setImageResource(R.drawable.ic_minimize_white_24dp);
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        // details sheet collapsed
                        mPlayerExpandButton.setImageResource(R.drawable.ic_expand_white_24dp);
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        // statistics sheet hidden
                        mPlayerBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        break;
                    default:
                        break;
                }
            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // react to dragging events
                if (slideOffset < 0.125f) {
                    // change expand button
                    mPlayerExpandButton.setImageResource(R.drawable.ic_expand_white_24dp);
                } else {
                    // change expand button
                    mPlayerExpandButton.setImageResource(R.drawable.ic_minimize_white_24dp);
                }

            }
        };
    }


    /* Todo describe */
    private Station findStationByUri(Uri streamUri, ArrayList<Station> stationList) {
        for (Station station :  stationList) {
            if (station.getStreamUri().equals(streamUri)) {
                // found matching station
                return station;
            }
        }
        // default return
        return null;
    }


    /* Copy station data to system clipboard */
    private void copyToClipboard(int contentType) {

        String clipboardText = null;
        ClipData clip;

        switch (contentType) {
            case COPY_STATION_ALL:
                // set clip text // todo implement
//                if (mCurrentStation.getMetadata() != null) {
//                    clipboardText = mCurrentStation.getStationName() +  " - " +  mCurrentStation.getMetadata() + " (" +  mCurrentStation.getStreamUri().toString() + ")";
//                } else {
//                    clipboardText = mCurrentStation.getStationName() + " (" + mCurrentStation.getStreamUri().toString() + ")";
//                }
                // notify user
                Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_station_copied), Toast.LENGTH_SHORT).show();
                break;

            case COPY_STATION_METADATA:
                // set clip text and notify user
                clipboardText = mPlayerSheetMetadataValue.getText().toString();
                Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_copied_to_clipboard_metadata), Toast.LENGTH_SHORT).show();
                break;

            case COPY_STREAM_URL:
                // set clip text and notify user
                clipboardText = mPlayerSheetStreamUrlValue.getText().toString();
                Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_copied_to_clipboard_url), Toast.LENGTH_SHORT).show();
                break;

        }

        // create clip and to clipboard
        if (clipboardText != null) {
            clip = ClipData.newPlainText("simple text", clipboardText);
            ClipboardManager cm = (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(clip);
        }

    }


    /* Creates an observer for collection of stations stored as LiveData */
    private Observer<ArrayList<Station>> createStationListObserver() {
        return new Observer<ArrayList<Station>>() {
            @Override
            public void onChanged(@Nullable ArrayList<Station> newStationList) {
                if (newStationList.size() == 0) {
                    // hide player
                    setupPlayer(null);
                } else if (mCurrentStation == null) {
                    // restore last station
                    if (mCurrentStationUrl != null) {
                        mCurrentStation = findStationByUri(Uri.parse(mCurrentStationUrl), newStationList);
                    } else {
                        mCurrentStation = newStationList.get(0);
                    }
                    // setup and show player
                    setupPlayer(mCurrentStation);
                }
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
                // check if this station parameters have changed
                if (mCurrentStation != null && newStation!= null &&
                        mCurrentStation.getStreamUri().equals(newStation.getStreamUri())) {

                    String newName = newStation.getStationName();
                    long newImageSize = newStation.getStationImageSize();
                    String newMetaData = newStation.getMetadata();

                    String oldName = mCurrentStation.getStationName();
                    long oldImageSize = mCurrentStation.getStationImageSize();
                    String oldMetaData = mCurrentStation.getMetadata();

                    // CASE: NAME
                    if (!(newName.equals(oldName))) {
                        updateStationNameView(newStation);
                    }
                    // CASE: IMAGE
                    else if (newImageSize != oldImageSize) {
                        updateStationImageView(newStation);
                    }
                    // CASE: METADATA
                    else if (!(newMetaData.equals(oldMetaData))) {
                        updateStationMetadataView(newStation);
                    }
                    // CASE: PLAYBACK STATE
                    if (mCurrentStation.getPlaybackState() != newStation.getPlaybackState()) {
                        updateStationPlaybackState(newStation);
                    }
//                    if (mPlaybackState != newStation.getPlaybackState()) { // todo remove
//                        mPlaybackState = newStation.getPlaybackState();
//                        changeVisualState(newStation);
//                    }

                    // update this station
                    mCurrentStation = newStation;

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

