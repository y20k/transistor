/**
 * MainActivityFragment.java
 * Implements the main fragment of the main activity
 * This fragment is a list view of radio stations
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-16 - Y20K.org
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.y20k.transistor.core.Station;
import org.y20k.transistor.helpers.DialogAdd;
import org.y20k.transistor.helpers.ImageHelper;
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
public final class MainActivityFragment extends Fragment {

    /* Define log tag */
    private static final String LOG_TAG = MainActivityFragment.class.getSimpleName();


    /* Main class variables */
    private Application mApplication;
    private Activity mActivity;
    private CollectionAdapter mCollectionAdapter = null;
    private File mFolder;
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

        // create collection adapter
        mCollectionAdapter = new CollectionAdapter(mActivity, mFolder);

        // initialize broadcast receivers
        initializeBroadcastReceivers();

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // get list state from saved instance
        if (savedInstanceState != null) {
            mListState = savedInstanceState.getParcelable(TransistorKeys.INSTANCE_LIST_STATE);
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
            mCollectionAdapter.setStationIDSelected(mStationIDSelected);
        }

        // handle incoming intent
        handleIncomingIntent();

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
                aboutIntent.putExtra(TransistorKeys.EXTRA_INFOSHEET_TITLE, aboutTitle);
                aboutIntent.putExtra(TransistorKeys.EXTRA_INFOSHEET_CONTENT, TransistorKeys.INFOSHEET_CONTENT_ABOUT);
                startActivity(aboutIntent);
                return true;

            // CASE HOWTO
            case R.id.menu_howto:
                // get title and content
                String howToTitle = mActivity.getString(R.string.header_howto);
                // put title and content into intent and start activity
                Intent howToIntent = new Intent(mActivity, InfosheetActivity.class);
                howToIntent.putExtra(TransistorKeys.EXTRA_INFOSHEET_TITLE, howToTitle);
                howToIntent.putExtra(TransistorKeys.EXTRA_INFOSHEET_CONTENT, TransistorKeys.INFOSHEET_CONTENT_HOWTO);
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
        outState.putParcelable(TransistorKeys.INSTANCE_LIST_STATE, mListState);
        super.onSaveInstanceState(outState);
    }


    /* handles incoming intent */
    private void handleIncomingIntent() {

        // get intent
        Intent intent = mActivity.getIntent();

        // handles external taps on streaming links
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {

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
                if (permissionHelper.requestReadExternalStorage(TransistorKeys.PERMISSION_REQUEST_STATION_FETCHER_READ_EXTERNAL_STORAGE)) {
                    // read and add new station
                    fetchNewStation(mNewStationUri);
                }
            }

            // unsuccessful - log failure
            else {
                Log.v(LOG_TAG, "Received an empty intent");
            }

        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case TransistorKeys.PERMISSION_REQUEST_IMAGE_PICKER_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted - get system picker for images
                    Intent pickImageIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    mActivity.startActivityForResult(pickImageIntent, TransistorKeys.REQUEST_LOAD_IMAGE);
                } else {
                    // permission denied
                    Toast.makeText(mActivity, mActivity.getString(R.string.toastalert_permission_denied) + " READ_EXTERNAL_STORAGE", Toast.LENGTH_LONG).show();
                }
                break;
            }

            case TransistorKeys.PERMISSION_REQUEST_STATION_FETCHER_READ_EXTERNAL_STORAGE: {
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

        if (requestCode == TransistorKeys.REQUEST_LOAD_IMAGE && resultCode == Activity.RESULT_OK && newImageUri != null) {

            ImageHelper imageHelper = new ImageHelper(newImageUri, mActivity);
            Bitmap newImage = imageHelper.getInputImage();

            if (newImage != null && mTempStationID != -1) {
                // write image to storage
                File stationImageFile = mTempStation.getStationImageFile();
                try (FileOutputStream out = new FileOutputStream(stationImageFile)) {
                    newImage.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Unable to save: " + newImage.toString());
                }
                // update adapter
                mCollectionAdapter.notifyItemChanged(mTempStationID);

            } else {
                Log.e(LOG_TAG, "Unable to get image from media picker. Uri was:  " + newImageUri.toString());
            }

        } else {
            Log.e(LOG_TAG, "Unable to get image from media picker. Did not receive an Uri");
        }
    }


    /* Show or hide call to action view if necessary */
    public void toggleActionCall() {
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
    public void selectFromImagePicker() {
        // request read permissions
        PermissionHelper permissionHelper = new PermissionHelper(mActivity, mRootView);
        if (permissionHelper.requestReadExternalStorage(TransistorKeys.PERMISSION_REQUEST_IMAGE_PICKER_READ_EXTERNAL_STORAGE)) {
            // get system picker for images
            Intent pickImageIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            mActivity.startActivityForResult(pickImageIntent, TransistorKeys.REQUEST_LOAD_IMAGE);
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
        Log.v(LOG_TAG, "Starting timer service and notification.");
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
        Log.v(LOG_TAG, "Stopping timer service and notification.");
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
                Log.v(LOG_TAG, "Sleep timer cancelled.");
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
        mStationIDSelected = settings.getInt(TransistorKeys.PREF_STATION_ID_SELECTED, 0);
        mStationIDCurrent = settings.getInt(TransistorKeys.PREF_STATION_ID_CURRENTLY_PLAYING, -1);
        mStationIDLast = settings.getInt(TransistorKeys.PREF_STATION_ID_LAST, -1);
        mPlayback = settings.getBoolean(TransistorKeys.PREF_PLAYBACK, false);
        mTwoPane = settings.getBoolean(TransistorKeys.PREF_TWO_PANE, false);
        mSleepTimerRunning = settings.getBoolean(TransistorKeys.PREF_TIMER_RUNNING, false);
        Log.v(LOG_TAG, "Loading state ("+  mStationIDCurrent + " / " + mStationIDLast + " / " + mPlayback + ")");
    }


    /* Saves app state to SharedPreferences */
    private void saveAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(TransistorKeys.PREF_STATION_ID_CURRENTLY_PLAYING, mStationIDCurrent);
        editor.putInt(TransistorKeys.PREF_STATION_ID_LAST, mStationIDLast);
        editor.putBoolean(TransistorKeys.PREF_PLAYBACK, mPlayback);
        editor.putBoolean(TransistorKeys.PREF_TIMER_RUNNING, mSleepTimerRunning);
        editor.apply();
        Log.v(LOG_TAG, "Saving state ("+  mStationIDCurrent + " / " + mStationIDLast + " / " + mPlayback + ")");
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
                if (intent.hasExtra(TransistorKeys.EXTRA_PLAYBACK_STATE_CHANGE)) {
                    handlePlaybackStateChanges(intent);
                }
            }
        };
        IntentFilter playbackStateChangedIntentFilter = new IntentFilter(TransistorKeys.ACTION_PLAYBACK_STATE_CHANGED);
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(mPlaybackStateChangedReceiver, playbackStateChangedIntentFilter);

        // RECEIVER: station added, deleted, or changed
        mCollectionChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && intent.hasExtra(TransistorKeys.EXTRA_COLLECTION_CHANGE)) {
                    handleCollectionChanges(intent);
                }
            }
        };
        IntentFilter collectionChangedIntentFilter = new IntentFilter(TransistorKeys.ACTION_COLLECTION_CHANGED);
        LocalBroadcastManager.getInstance(mApplication).registerReceiver(mCollectionChangedReceiver, collectionChangedIntentFilter);

        // RECEIVER: listen for request to change station image
        mImageChangeRequestReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra(TransistorKeys.EXTRA_STATION) && intent.hasExtra(TransistorKeys.EXTRA_STATION_ID)) {
                    // get station and id from intent
                    mTempStation = intent.getParcelableExtra(TransistorKeys.EXTRA_STATION);
                    mTempStationID = intent.getIntExtra(TransistorKeys.EXTRA_STATION_ID, -1);
                    // start image picker
                    selectFromImagePicker();
                }
            }
        };
        IntentFilter imageChangeRequestIntentFilter = new IntentFilter(TransistorKeys.ACTION_IMAGE_CHANGE_REQUESTED);
        LocalBroadcastManager.getInstance(mApplication).registerReceiver(mImageChangeRequestReceiver, imageChangeRequestIntentFilter);

        // RECEIVER: sleep timer service sends updates
        mSleepTimerStartedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // get duration from intent
                long remaining = intent.getLongExtra(TransistorKeys.EXTRA_TIMER_REMAINING, 0);
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
        IntentFilter sleepTimerIntentFilter = new IntentFilter(TransistorKeys.ACTION_TIMER_RUNNING);
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
        switch (intent.getIntExtra(TransistorKeys.EXTRA_PLAYBACK_STATE_CHANGE, 1)) {
            // CASE: playback was stopped
            case TransistorKeys.PLAYBACK_STOPPED:
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

        switch (intent.getIntExtra(TransistorKeys.EXTRA_COLLECTION_CHANGE, 1)) {

            // CASE: station was added
            case TransistorKeys.STATION_ADDED:
                if (intent.hasExtra(TransistorKeys.EXTRA_STATION)) {

                    // get station from intent
                    Station station = intent.getParcelableExtra(TransistorKeys.EXTRA_STATION);

                    // add station to adapter, scroll to new position and update adapter
                    newStationPosition = mCollectionAdapter.add(station);

                    if (mCollectionAdapter.getItemCount() > 0) {
                        toggleActionCall();
                    }

                    mLayoutManager.scrollToPosition(newStationPosition);
                    mCollectionAdapter.setStationIDSelected(newStationPosition);
                    mCollectionAdapter.notifyDataSetChanged(); // TODO Remove?
                }
                break;

            // CASE: station was renamed
            case TransistorKeys.STATION_RENAMED:
                if (intent.hasExtra(TransistorKeys.EXTRA_STATION_NEW_NAME) && intent.hasExtra(TransistorKeys.EXTRA_STATION) && intent.hasExtra(TransistorKeys.EXTRA_STATION_ID)) {                    // get station and station ID from intent

                    // get new name, station and station ID from intent
                    String newStationName = intent.getStringExtra(TransistorKeys.EXTRA_STATION_NEW_NAME);
                    Station station = intent.getParcelableExtra(TransistorKeys.EXTRA_STATION);
                    int stationID = intent.getIntExtra(TransistorKeys.EXTRA_STATION_ID, 0);

                    // update view
                    // mRecyclerView

                    // change station within in adapter, scroll to new position and update adapter
                    newStationPosition = mCollectionAdapter.rename(newStationName, station, stationID);
                    mLayoutManager.scrollToPosition(newStationPosition);
                    mCollectionAdapter.setStationIDSelected(newStationPosition);
                    mCollectionAdapter.notifyDataSetChanged(); // TODO Remove?

                    // update notification
                    if (station.getPlaybackState()) {
                        NotificationHelper.initialize(station, stationID);
                        NotificationHelper.updateNotification();
                    }
                }
                break;

            // CASE: station was deleted
            case TransistorKeys.STATION_DELETED:
                if (intent.hasExtra(TransistorKeys.EXTRA_STATION) && intent.hasExtra(TransistorKeys.EXTRA_STATION_ID)) {

                    // get station and station ID from intent
                    Station station = intent.getParcelableExtra(TransistorKeys.EXTRA_STATION);
                    int stationID = intent.getIntExtra(TransistorKeys.EXTRA_STATION_ID, 0);

                    // remove station from adapter and update
                    newStationPosition = mCollectionAdapter.delete(station, stationID);
                    if (newStationPosition == -1 || mCollectionAdapter.getItemCount() == 0) {
                        // show call to action
                        toggleActionCall();
                    } else {
                        // scroll to new position
                        mCollectionAdapter.setStationIDSelected(newStationPosition);
                        mLayoutManager.scrollToPosition(newStationPosition);
                    }
                    mCollectionAdapter.notifyDataSetChanged(); // TODO Remove?
                }
                break;
        }

    }




}
