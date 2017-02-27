/**
 * MainActivityFragment.java
 * Implements the main fragment of the main activity
 * This fragment is a list view of radio stations
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
import android.app.Application;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.y20k.transistor.core.Station;
import org.y20k.transistor.helpers.DialogAdd;
import org.y20k.transistor.helpers.ImageHelper;
import org.y20k.transistor.helpers.LogHelper;
import org.y20k.transistor.helpers.NotificationHelper;
import org.y20k.transistor.helpers.PermissionHelper;
import org.y20k.transistor.helpers.SleepTimerService;
import org.y20k.transistor.helpers.StationFetcher;
import org.y20k.transistor.helpers.StorageHelper;
import org.y20k.transistor.helpers.TransistorKeys;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


/**
 * MainActivityFragment class
 */
public final class MainActivityFragment extends Fragment implements TransistorKeys {

    /* Define log tag */
    private static final String LOG_TAG = MainActivityFragment.class.getSimpleName();


    /* Main class variables */
    private Application mApplication;
    private Activity mActivity;
    private CollectionAdapter mCollectionAdapter = null;
    private File mFolder;
    private int mFolderSize;
    private View mRootView;
    private View mActionCallView;
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private Parcelable mListState;
    private BroadcastReceiver mCollectionChangedReceiver;
    private BroadcastReceiver mImageChangeRequestReceiver;
    private BroadcastReceiver mSleepTimerStartedReceiver;
    private BroadcastReceiver mPlaybackStateChangedReceiver;
    private int mStationIDSelected;
    private int mStationIDCurrent;
    private int mStationIDLast;
    private int mTempStationID;
    private Station mTempStation;
    private Uri mNewStationUri;
    private boolean mTwoPane;
    private boolean mPlayback;
    private boolean mSleepTimerRunning;
    private SleepTimerService mSleepTimerService;
    private String mSleepTimerNotificationMessage;
    private Snackbar mSleepTimerNotification;


    /* Constructor (default) */
    public MainActivityFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // fragment has options menu
        setHasOptionsMenu(true);

        // get activity and application contexts
        mActivity = getActivity();
        mApplication = mActivity.getApplication();

        // get notification message
        mSleepTimerNotificationMessage = mActivity.getString(R.string.snackbar_message_timer_set) + " ";

        // initiate sleep timer service
        mSleepTimerService = new SleepTimerService();

        // set list state null
        mListState = null;

        // initialize id of currently selected station
        mStationIDSelected = 0;

        // initialize temporary station image id
        mTempStationID = -1;

        // initialize two pane
        mTwoPane = false;

        // load playback state
        loadAppState(mActivity);

        // get collection folder
        StorageHelper storageHelper = new StorageHelper(mActivity);
        mFolder = storageHelper.getCollectionDirectory();
        if (mFolder == null) {
            Toast.makeText(mActivity, mActivity.getString(R.string.toastalert_no_external_storage), Toast.LENGTH_LONG).show();
            mActivity.finish();
        }
        mFolderSize = mFolder.listFiles().length;

        // create collection adapter
        if (mCollectionAdapter == null) {
            mCollectionAdapter = new CollectionAdapter(mActivity, mFolder);
        }

        // initialize broadcast receivers
        initializeBroadcastReceivers();

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // get list state from saved instance
        if (savedInstanceState != null) {
            mListState = savedInstanceState.getParcelable(INSTANCE_LIST_STATE);
        }

        // inflate root view from xml
        mRootView = inflater.inflate(R.layout.fragment_main, container, false);

        // get reference to action call view from inflated root view
        mActionCallView = mRootView.findViewById(R.id.main_actioncall_layout);

        // get reference to recycler list view from inflated root view
        mRecyclerView = (RecyclerView) mRootView.findViewById(R.id.main_recyclerview_collection);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        // TODO check if necessary here
        mRecyclerView.setHasFixedSize(true);

        // set animator
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(mActivity);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // attach adapter to list view
        mRecyclerView.setAdapter(mCollectionAdapter);


        return mRootView;
    }


    @Override
    public void onResume() {
        super.onResume();

        // refresh app state
        loadAppState(mActivity);

        // update collection adapter
        mCollectionAdapter.setTwoPane(mTwoPane);
        mCollectionAdapter.refresh();
        if (mCollectionAdapter.getItemCount() > 0) {
            mCollectionAdapter.setStationIDSelected(mStationIDSelected, mPlayback, false);
        }

        // handles the activity's intent
        Intent intent = mActivity.getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            handleStreamingLink(intent);
        } else if (ACTION_SHOW_PLAYER.equals(intent.getAction())) {
            handleShowPlayer(intent);
        }

        // check if folder content has been changed
        int folderSize = mFolder.listFiles().length;
        if (mFolderSize != mFolder.listFiles().length) {
            mFolderSize = folderSize;
            mCollectionAdapter = new CollectionAdapter(mActivity, mFolder);
            mRecyclerView.setAdapter(mCollectionAdapter);
        }

        // show call to action, if necessary
        toggleActionCall();

        // show notification bar if timer is running
        if (mSleepTimerRunning) {
            showSleepTimerNotification(-1);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterBroadcastReceivers();
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

                DialogAdd dialog = new DialogAdd(mActivity, mFolder);
                dialog.show();
                return true;

            // CASE ABOUT
            case R.id.menu_about:
                // get title and content
                String aboutTitle = mActivity.getString(R.string.header_about);
                // put title and content into intent and start activity
                Intent aboutIntent = new Intent(mActivity, InfosheetActivity.class);
                aboutIntent.putExtra(EXTRA_INFOSHEET_TITLE, aboutTitle);
                aboutIntent.putExtra(EXTRA_INFOSHEET_CONTENT, INFOSHEET_CONTENT_ABOUT);
                startActivity(aboutIntent);
                return true;

            // CASE HOWTO
            case R.id.menu_howto:
                // get title and content
                String howToTitle = mActivity.getString(R.string.header_howto);
                // put title and content into intent and start activity
                Intent howToIntent = new Intent(mActivity, InfosheetActivity.class);
                howToIntent.putExtra(EXTRA_INFOSHEET_TITLE, howToTitle);
                howToIntent.putExtra(EXTRA_INFOSHEET_CONTENT, INFOSHEET_CONTENT_HOWTO);
                startActivity(howToIntent);
                return true;

            // CASE DEFAULT
            default:
                return super.onOptionsItemSelected(item);
        }

    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        // save list view position
        mListState = mLayoutManager.onSaveInstanceState();
        outState.putParcelable(INSTANCE_LIST_STATE, mListState);
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_REQUEST_IMAGE_PICKER_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted - get system picker for images
                    Intent pickImageIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    mActivity.startActivityForResult(pickImageIntent, REQUEST_LOAD_IMAGE);
                } else {
                    // permission denied
                    Toast.makeText(mActivity, mActivity.getString(R.string.toastalert_permission_denied) + " READ_EXTERNAL_STORAGE", Toast.LENGTH_LONG).show();
                }
                break;
            }

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


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // retrieve selected image Uri from image picker
        Uri newImageUri = null;
        if (null != data) {
            newImageUri = data.getData();
        }

        if (requestCode == REQUEST_LOAD_IMAGE && resultCode == Activity.RESULT_OK && newImageUri != null) {

            ImageHelper imageHelper = new ImageHelper(newImageUri, mActivity);
            Bitmap newImage = imageHelper.getInputImage();

            if (newImage != null && mTempStationID != -1) {
                // write image to storage
                File stationImageFile = mTempStation.getStationImageFile();
                try (FileOutputStream out = new FileOutputStream(stationImageFile)) {
                    newImage.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (IOException e) {
                    LogHelper.e(LOG_TAG, "Unable to save: " + newImage.toString());
                }
                // update adapter
                mCollectionAdapter.notifyItemChanged(mTempStationID);

            } else {
                LogHelper.e(LOG_TAG, "Unable to get image from media picker. Uri was:  " + newImageUri.toString());
            }

        } else {
            LogHelper.e(LOG_TAG, "Unable to get image from media picker. Did not receive an Uri");
        }
    }


    /* Show or hide call to action view if necessary */
    private void toggleActionCall() {
        // show call to action, if necessary
        if (mCollectionAdapter.getItemCount() == 0) {
            mActionCallView.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        } else {
            mActionCallView.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
    }


    /* Check permissions and start image picker */
    private void selectFromImagePicker() {
        // request read permissions
        PermissionHelper permissionHelper = new PermissionHelper(mActivity, mRootView);
        if (permissionHelper.requestReadExternalStorage(PERMISSION_REQUEST_IMAGE_PICKER_READ_EXTERNAL_STORAGE)) {
            // get system picker for images
            Intent pickImageIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickImageIntent, REQUEST_LOAD_IMAGE);
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
            LogHelper.v(LOG_TAG, "Received an empty intent");
        }
    }


    /* Handles intent to show player from notification or from shortcut */
    private void handleShowPlayer(Intent intent) {
        // get station from intent
        Station station = null;
        if (intent.hasExtra(EXTRA_STATION)) {
            // get station from notification
            station = intent.getParcelableExtra(EXTRA_STATION);
        } else if (intent.hasExtra(EXTRA_STREAM_URI)) {
            // get Uri of station from home screen shortcut
            station = mCollectionAdapter.findStation(Uri.parse(intent.getStringExtra(EXTRA_STREAM_URI)));
        } else if (intent.hasExtra(EXTRA_LAST_STATION) && intent.getBooleanExtra(EXTRA_LAST_STATION, false)) {
            // try to get last station
            loadAppState(mActivity);
            if (mStationIDLast > -1 && mStationIDLast < mCollectionAdapter.getItemCount()) {
                station = mCollectionAdapter.getStation(mStationIDLast);
            }
        }

        if (station == null) {
            Toast.makeText(mActivity, getString(R.string.toastalert_station_not_found), Toast.LENGTH_LONG).show();
        }

        // get playback action from intent
        boolean startPlayback;
        if (intent.hasExtra(EXTRA_PLAYBACK_STATE)) {
            startPlayback = intent.getBooleanExtra(EXTRA_PLAYBACK_STATE, false);
        } else {
            startPlayback = false;
        }

        // prepare arguments or intent
        if (mTwoPane && station != null) {
            mStationIDSelected = mCollectionAdapter.getStationID(station);
            mCollectionAdapter.setStationIDSelected(mStationIDSelected, station.getPlaybackState(), startPlayback);
        } else if (station != null) {
            // start player activity - on phone
            Intent playerIntent = new Intent(mActivity, PlayerActivity.class);
            playerIntent.setAction(ACTION_SHOW_PLAYER);
            playerIntent.putExtra(EXTRA_STATION, station);
            playerIntent.putExtra(EXTRA_PLAYBACK_STATE, startPlayback);
            startActivity(playerIntent);
        }
    }


    /* Handles tap timer icon in actionbar */
    private void handleMenuSleepTimerClick() {
        // load app state
        loadAppState(mActivity);

        // set duration
        long duration = 900000; // equals 15 minutes

        // CASE: No station is playing, no timer is running
        if (!mPlayback && !mSleepTimerRunning) {
            // unable to start timer
            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_timer_start_unable), Toast.LENGTH_SHORT).show();
        }
        // CASE: A station is playing, no sleep timer is running
        else if (mPlayback && !mSleepTimerRunning) {
            startSleepTimer(duration);
            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_timer_activated), Toast.LENGTH_SHORT).show();
        }
        // CASE: A station is playing, Sleep timer is running
        else if (mPlayback) {
            startSleepTimer(duration);
            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_timer_duration_increased) + " [+" + getReadableTime(duration) + "]", Toast.LENGTH_SHORT).show();
        }

    }


    /* Starts timer service and notification */
    private void startSleepTimer(long duration) {
        // start timer service
        if (mSleepTimerService == null) {
            mSleepTimerService = new SleepTimerService();
        }
        mSleepTimerService.startActionStart(mActivity, duration);

        // show timer notification
        showSleepTimerNotification(duration);
        mSleepTimerRunning = true;
        LogHelper.v(LOG_TAG, "Starting timer service and notification.");
    }


    /* Stops timer service and notification */
    private void stopSleepTimer() {
        // stop timer service
        if (mSleepTimerService != null) {
            mSleepTimerService.startActionStop(mActivity);
        }
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
                // stop sleep timer service
                mSleepTimerService.startActionStop(mActivity);
                mSleepTimerRunning = false;
                saveAppState(mActivity);
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
        mStationIDSelected = settings.getInt(PREF_STATION_ID_SELECTED, 0);
        mStationIDCurrent = settings.getInt(PREF_STATION_ID_CURRENTLY_PLAYING, -1);
        mStationIDLast = settings.getInt(PREF_STATION_ID_LAST, -1);
        mPlayback = settings.getBoolean(PREF_PLAYBACK, false);
        mTwoPane = settings.getBoolean(PREF_TWO_PANE, false);
        mSleepTimerRunning = settings.getBoolean(PREF_TIMER_RUNNING, false);
        LogHelper.v(LOG_TAG, "Loading state ("+  mStationIDCurrent + " / " + mStationIDLast + " / " + mPlayback + ")");
    }


    /* Saves app state to SharedPreferences */
    private void saveAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(PREF_STATION_ID_CURRENTLY_PLAYING, mStationIDCurrent);
        editor.putInt(PREF_STATION_ID_LAST, mStationIDLast);
        editor.putBoolean(PREF_PLAYBACK, mPlayback);
        editor.putBoolean(PREF_TIMER_RUNNING, mSleepTimerRunning);
        editor.apply();
        LogHelper.v(LOG_TAG, "Saving state ("+  mStationIDCurrent + " / " + mStationIDLast + " / " + mPlayback + ")");
    }


    /* Fetch new station with given Uri */
    private void fetchNewStation(Uri stationUri) {
        // download and add new station
        StationFetcher stationFetcher = new StationFetcher(mActivity, mFolder, stationUri);
        stationFetcher.execute();
    }


    /* Initializes broadcast receivers for onCreate */
    private void initializeBroadcastReceivers() {

        // RECEIVER: state of playback has changed
        mPlaybackStateChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra(EXTRA_PLAYBACK_STATE_CHANGE)) {
                    handlePlaybackStateChanges(intent);
                }
            }
        };
        IntentFilter playbackStateChangedIntentFilter = new IntentFilter(ACTION_PLAYBACK_STATE_CHANGED);
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(mPlaybackStateChangedReceiver, playbackStateChangedIntentFilter);

        // RECEIVER: station added, deleted, or changed
        mCollectionChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && intent.hasExtra(EXTRA_COLLECTION_CHANGE)) {
                    handleCollectionChanges(intent);
                }
            }
        };
        IntentFilter collectionChangedIntentFilter = new IntentFilter(ACTION_COLLECTION_CHANGED);
        LocalBroadcastManager.getInstance(mApplication).registerReceiver(mCollectionChangedReceiver, collectionChangedIntentFilter);

        // RECEIVER: listen for request to change station image
        mImageChangeRequestReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra(EXTRA_STATION) && intent.hasExtra(EXTRA_STATION_ID)) {
                    // get station and id from intent
                    mTempStation = intent.getParcelableExtra(EXTRA_STATION);
                    mTempStationID = intent.getIntExtra(EXTRA_STATION_ID, -1);
                    // start image picker
                    selectFromImagePicker();
                }
            }
        };
        IntentFilter imageChangeRequestIntentFilter = new IntentFilter(ACTION_IMAGE_CHANGE_REQUESTED);
        LocalBroadcastManager.getInstance(mApplication).registerReceiver(mImageChangeRequestReceiver, imageChangeRequestIntentFilter);

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
                    // save state and update user interface
                    mPlayback = false;
                    mSleepTimerRunning = false;
                    saveAppState(mActivity);
                }

            }
        };
        IntentFilter sleepTimerIntentFilter = new IntentFilter(ACTION_TIMER_RUNNING);
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(mSleepTimerStartedReceiver, sleepTimerIntentFilter);

    }


    /* Unregisters broadcast receivers */
    private void unregisterBroadcastReceivers() {
        LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(mPlaybackStateChangedReceiver);
        LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(mCollectionChangedReceiver);
        LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(mImageChangeRequestReceiver);
        LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(mSleepTimerStartedReceiver);
    }

    /* Handles changes in state of playback, eg. start, stop, loading stream */
    private void handlePlaybackStateChanges(Intent intent) {
        switch (intent.getIntExtra(EXTRA_PLAYBACK_STATE_CHANGE, 1)) {
            // CASE: playback was stopped
            case PLAYBACK_STOPPED:
                // load app state
                loadAppState(mActivity);
                // stop sleep timer
                if (mSleepTimerRunning && mSleepTimerService != null) {
                    stopSleepTimer();
                }
                break;
        }
    }


    /* Handles adding, deleting and renaming of station */
    private void handleCollectionChanges(Intent intent) {

        // load app state
        loadAppState(mActivity);

        int newStationPosition;

        switch (intent.getIntExtra(EXTRA_COLLECTION_CHANGE, 1)) {

            // CASE: station was added
            case STATION_ADDED:
                if (intent.hasExtra(EXTRA_STATION)) {

                    // get station from intent
                    Station station = intent.getParcelableExtra(EXTRA_STATION);

                    // add station to adapter, scroll to new position and update adapter
                    newStationPosition = mCollectionAdapter.add(station);

                    if (mCollectionAdapter.getItemCount() > 0) {
                        toggleActionCall();
                    }

                    mLayoutManager.scrollToPosition(newStationPosition);
                    mCollectionAdapter.setStationIDSelected(newStationPosition, mPlayback, false);
                    mCollectionAdapter.notifyDataSetChanged(); // TODO Remove?
                }
                break;

            // CASE: station was renamed
            case STATION_RENAMED:
                if (intent.hasExtra(EXTRA_STATION_NEW_NAME) && intent.hasExtra(EXTRA_STATION) && intent.hasExtra(EXTRA_STATION_ID)) {

                    // get new name, station and station ID from intent
                    String newStationName = intent.getStringExtra(EXTRA_STATION_NEW_NAME);
                    Station station = intent.getParcelableExtra(EXTRA_STATION);
                    int stationID = intent.getIntExtra(EXTRA_STATION_ID, 0);

                    // update notification
                    if (station.getPlaybackState()) {
                        NotificationHelper.update(station, stationID, null, null);
                    }

                    // change station within in adapter, scroll to new position and update adapter
                    newStationPosition = mCollectionAdapter.rename(newStationName, station, stationID);
                    mLayoutManager.scrollToPosition(newStationPosition);
                    mCollectionAdapter.setStationIDSelected(newStationPosition, mPlayback, false);
                    mCollectionAdapter.notifyDataSetChanged(); // TODO Remove?


                }
                break;

            // CASE: station was deleted
            case STATION_DELETED:
                if (intent.hasExtra(EXTRA_STATION) && intent.hasExtra(EXTRA_STATION_ID)) {

                    // get station and station ID from intent
                    Station station = intent.getParcelableExtra(EXTRA_STATION);
                    int stationID = intent.getIntExtra(EXTRA_STATION_ID, 0);

                    // dismiss notification
                    NotificationHelper.stop();

                    if (station.getPlaybackState()) {
                        // stop player service and notification using intent
                        Intent i = new Intent(mActivity, PlayerService.class);
                        i.setAction(ACTION_DISMISS);
                        mActivity.startService(i);
                        LogHelper.v(LOG_TAG, "Stopping player service.");
                    }

                    // remove station from adapter and update
                    newStationPosition = mCollectionAdapter.delete(station, stationID);
                    if (newStationPosition == -1 || mCollectionAdapter.getItemCount() == 0) {
                        // show call to action
                        toggleActionCall();
                    } else {
                        // scroll to new position
                        mCollectionAdapter.setStationIDSelected(newStationPosition, mPlayback, false);
                        mLayoutManager.scrollToPosition(newStationPosition);
                    }
                    mCollectionAdapter.notifyDataSetChanged(); // TODO Remove?
                }
                break;
        }

    }

}
