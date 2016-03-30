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

import android.Manifest;
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
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.os.EnvironmentCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.y20k.transistor.core.Collection;
import org.y20k.transistor.core.Station;
import org.y20k.transistor.helpers.CollectionAdapter;
import org.y20k.transistor.helpers.DialogAddStation;
import org.y20k.transistor.helpers.ImageHelper;
import org.y20k.transistor.helpers.NotificationHelper;
import org.y20k.transistor.helpers.ShortcutHelper;
import org.y20k.transistor.helpers.SleepTimerService;
import org.y20k.transistor.helpers.StationFetcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;


/**
 * MainActivityFragment class
 */
public final class MainActivityFragment extends Fragment {

    /* Define log tag */
    private static final String LOG_TAG = MainActivityFragment.class.getSimpleName();


    /* Keys */
    private static final String ACTION_PLAYBACK_STARTED = "org.y20k.transistor.action.PLAYBACK_STARTED";
    private static final String ACTION_PLAYBACK_STOPPED = "org.y20k.transistor.action.PLAYBACK_STOPPED";
    private static final String ACTION_TIMER_RUNNING = "org.y20k.transistor.action.TIMER_RUNNING";
    private static final String ACTION_IMAGE_CHANGE_REQUESTED = "org.y20k.transistor.action.IMAGE_CHANGE_REQUESTED";
    private static final String ACTION_CREATE_SHORTCUT_REQUESTED = "org.y20k.transistor.action.CREATE_SHORTCUT_REQUESTED";
    private static final String ACTION_CHANGE_VIEW_SELECTION = "org.y20k.transistor.action.CHANGE_VIEW_SELECTION";
    private static final String ACTION_COLLECTION_CHANGED = "org.y20k.transistor.action.COLLECTION_CHANGED";
    private static final String EXTRA_STATION_URI_CURRENT = "STATION_URI_CURRENT";
    private static final String EXTRA_STATION_NEW_NAME = "STATION_NEW_NAME";
    private static final String EXTRA_STATION_NEW_POSITION = "STATION_NEW_POSITION";
    private static final String EXTRA_STATION_OLD_POSITION = "STATION_OLD_POSITION";
    private static final String EXTRA_STATION_ADDED = "STATION_ADDED";
    private static final String EXTRA_STATION_RENAMED = "STATION_RENAMED";
    private static final String EXTRA_STATION_DELETED = "STATION_DELETED";
    private static final String EXTRA_STATION_ID = "STATION_ID";
    private static final String EXTRA_TIMER_REMAINING = "TIMER_REMAINING";
    private static final String EXTRA_INFOSHEET_TITLE = "INFOSHEET_TITLE";
    private static final String EXTRA_INFOSHEET_CONTENT = "INFOSHEET_CONTENT";
    private static final String LIST_STATE = "ListState";
    private static final String PREF_PREF_STATION_ID_CURRENT = "prefStationIDCurrent";
    private static final String PREF_STATION_ID_LAST = "prefStationIDLast";
    private static final String PREF_PLAYBACK = "prefPlayback";
    private static final String TIMER_RUNNING = "timerRunning";
    private static final String CONTENT = "content";
    private static final int INFOSHEET_CONTENT_ABOUT = 1;
    private static final int INFOSHEET_CONTENT_HOWTO = 2;
    private static final int REQUEST_LOAD_IMAGE = 1;
    private static final int PERMISSION_REQUEST_IMAGE_PICKER_READ_EXTERNAL_STORAGE = 1;
    private static final int PERMISSION_REQUEST_STATION_FETCHER_READ_EXTERNAL_STORAGE = 2;


    /* Main class variables */
    private Application mApplication;
    private Activity mActivity;
    private Collection mCollection;
    private CollectionAdapter mCollectionAdapter = null;
    private File mFolder;
    private LinkedList<String> mStationNames;
    private LinkedList<Uri> mStationUris;
    private LinkedList<Bitmap> mStationImages;
    private View mRootView;
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private Parcelable mListState;
    private int mStationIDCurrent;
    private int mStationIDLast;
    private int mTempStationImageID;
    private Uri mNewStationUri;
    private boolean mPlayback;
    private SleepTimerService mSleepTimerService;
    private boolean mSleepTimerRunning;
    private String mSleepTimerNotificationMessage;
    private Snackbar mSleepTimerNotification;


    /* Constructor (default) */
    public MainActivityFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get activity and application contexts
        mActivity = getActivity();
        mApplication = mActivity.getApplication();

        // get notification message
        mSleepTimerNotificationMessage = mActivity.getString(R.string.snackbar_message_timer_set) + " ";

        // initiate sleep timer service
        mSleepTimerService = new SleepTimerService();

        // load playback state
        loadAppState(mActivity);

        // set list state null
        mListState = null;

        // initialize temporary station image id
        mTempStationImageID = -1;

        // get collection folder from external storage
        mFolder = getCollectionDirectory("Collection");

        // fragment has options menu
        setHasOptionsMenu(true);

        // create adapter for collection
        mStationNames = new LinkedList<>();
        mStationUris = new LinkedList<>();
        mStationImages = new LinkedList<>();
        mCollectionAdapter = new CollectionAdapter(mActivity, mStationNames, mStationUris, mStationImages);

        // initialize broadcast receivers
        initializeBroadcastReceivers();

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // get list state from saved instance
        if (savedInstanceState != null) {
            mListState = savedInstanceState.getParcelable(MainActivityFragment.LIST_STATE);
        }

        // inflate rootview from xml
        mRootView = inflater.inflate(R.layout.fragment_main, container, false);

        // get reference to list view from inflated root view
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

        // handle incoming intent
        handleNewStationIntent();

        // refresh playback state
        loadAppState(mActivity);

        // show notification bar if timer is running
        if (mSleepTimerRunning) {
            showSleepTimerNotification(-1);
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        // fill collection adapter with stations
        refreshStationList();

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

                DialogAddStation dialog = new DialogAddStation(mActivity);
                dialog.show();
                return true;

            // CASE ABOUT
            case R.id.menu_about:
                // get title and content
                String aboutTitle = mActivity.getString(R.string.header_about);
                String aboutContent = mActivity.getString(R.string.html_about);
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
                String howToContent = mActivity.getString(R.string.html_howto);
                // put title and content into intent and start activity
                Intent howToIntent = new Intent(mActivity, InfosheetActivity.class);
                howToIntent.putExtra(EXTRA_INFOSHEET_TITLE, howToTitle);
                howToIntent.putExtra(EXTRA_INFOSHEET_CONTENT, INFOSHEET_CONTENT_HOWTO);
                howToIntent.putExtra(CONTENT, howToContent);
                startActivity(howToIntent);
                return true;

            // CASE DEFAULT
            default:
                return super.onOptionsItemSelected(item);
        }

    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // save list view position
        mListState = mLayoutManager.onSaveInstanceState();
        outState.putParcelable(LIST_STATE, mListState);
    }


    /* Fills collection adapter */
    private void fillCollectionAdapter() {

        Bitmap stationImage;
        Bitmap stationImageSmall;
        ImageHelper imageHelper;

        // create collection
        Log.v(LOG_TAG, "Create collection of stations (folder:" + mFolder.toString() + ").");
        mCollection = new Collection(mFolder);

        // put stations into collection adapter
        for (Station station : mCollection.getStations()) {
            // add name to linked list of names
            mStationNames.add(station.getStationName());

            // add Uri to linked list of Uris
            mStationUris.add(station.getStreamUri());

            // set image for station
            if (station.getStationImageFile().exists()) {
                // get image from collection
                stationImageSmall = BitmapFactory.decodeFile(station.getStationImageFile().toString());
            } else {
                // get default image
                stationImageSmall = BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.ic_notesymbol);
            }
            imageHelper = new ImageHelper(stationImageSmall, mActivity);
            stationImage = imageHelper.createCircularFramedImage(192, R.color.transistor_grey_lighter);

            // add image to linked list of images
            mStationImages.add(stationImage);
        }
        mCollectionAdapter.setCollection(mCollection);
        mCollectionAdapter.notifyDataSetChanged();

    }


    /* (Re-)fills collection adapter with stations */
    private void refreshStationList() {

        // clear and refill mCollection adapter
        if (!mStationNames.isEmpty() && !mStationUris.isEmpty() && !mStationImages.isEmpty()) {
            mStationNames.clear();
            mStationUris.clear();
            mStationImages.clear();
        }
        fillCollectionAdapter();

        // show call to action, if necessary
        View actioncall = mRootView.findViewById(R.id.main_actioncall_layout);
        View recyclerview = mRootView.findViewById(R.id.main_recyclerview_collection);
        if (mCollectionAdapter.getItemCount() == 0) {
            actioncall.setVisibility(View.VISIBLE);
            recyclerview.setVisibility(View.GONE);
        } else {
            actioncall.setVisibility(View.GONE);
            recyclerview.setVisibility(View.VISIBLE);
        }

        Log.v(LOG_TAG, "Refreshing list of stations");

    }


    /* handles external taps on streaming links */
    private void handleNewStationIntent() {

        // get intent
        Intent intent = mActivity.getIntent();

        // check for intent of tyoe VIEW
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
                if (requestPermissionReadExternalStorage(PERMISSION_REQUEST_STATION_FETCHER_READ_EXTERNAL_STORAGE)) {
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
            case PERMISSION_REQUEST_IMAGE_PICKER_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted - get system picker for images
                    Intent pickImageIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    mActivity.startActivityForResult(pickImageIntent, REQUEST_LOAD_IMAGE);
                } else {
                    // permission denied
                    Toast.makeText(mActivity, mActivity.getString(R.string.toastalert_permission_denied) + " READ_EXTERNAL_STORAGE", Toast.LENGTH_LONG).show();
                }
            }

            case PERMISSION_REQUEST_STATION_FETCHER_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted - fetch station from given Uri
                    fetchNewStation(mNewStationUri);
                } else {
                    // permission denied
                    Toast.makeText(mActivity, mActivity.getString(R.string.toastalert_permission_denied) + " READ_EXTERNAL_STORAGE", Toast.LENGTH_LONG).show();
                }
            }
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_LOAD_IMAGE && resultCode == Activity.RESULT_OK && null != data) {
            // retrieve selected image from image picker
            processNewImage(data.getData());
        }
    }


    /* Processes new image and saves it to storage */
    private void processNewImage(Uri newImageUri) {

        ImageHelper imageHelper = new ImageHelper(newImageUri, mActivity);
        Bitmap newImage = imageHelper.getInputImage();

        if (newImage != null) {
            // write image to storage
            File stationImageFile = mCollection.getStations().get(mTempStationImageID).getStationImageFile();
            try (FileOutputStream out = new FileOutputStream(stationImageFile)) {
                newImage.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Unable to save: " + newImage.toString());
            }
        } else {
            Log.e(LOG_TAG, "Unable to get image from media picker: " + newImageUri.toString());
        }
    }


    /* Check permissions and start image picker */
    private void selectFromImagePicker() {

        // request read permissions
        if (requestPermissionReadExternalStorage(PERMISSION_REQUEST_IMAGE_PICKER_READ_EXTERNAL_STORAGE)) {
            // get system picker for images
            Intent pickImageIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            mActivity.startActivityForResult(pickImageIntent, REQUEST_LOAD_IMAGE);
        }
    }


    /* Request Read Permissions */
    private boolean requestPermissionReadExternalStorage(final int requestType) {

        // permission to read external storage granted
        if (ActivityCompat.checkSelfPermission(mActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        // permission to read external storage not granted
        else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // ask for permission and explain why
                Snackbar snackbar = Snackbar.make(mRootView, mActivity.getString(R.string.snackbar_request_storage_access), Snackbar.LENGTH_INDEFINITE);
                snackbar.setAction(R.string.dialog_generic_button_okay, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, requestType);
                    }
                });
                snackbar.show();

                return false;

            } else {
                // ask for permission without explanation
                ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, requestType);

                return false;
            }
        }
    }


    /* Handles tap timer icon in actionbar */
    private void handleMenuSleepTimerClick() {
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
            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_timer_duration_increased) + " [+" + getReadableTime(duration) +"]", Toast.LENGTH_SHORT).show();
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
    private String getReadableTime (long remainingTime) {
        String readableTime = String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(remainingTime),
                TimeUnit.MILLISECONDS.toSeconds(remainingTime) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(remainingTime))
        );
        return readableTime;
    }


    /* Loads app state from preferences */
    private void loadAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        mStationIDCurrent = settings.getInt(PREF_PREF_STATION_ID_CURRENT, -1);
        mStationIDLast = settings.getInt(PREF_STATION_ID_LAST, -1);
        mPlayback = settings.getBoolean(PREF_PLAYBACK, false);
        mSleepTimerRunning = settings.getBoolean(TIMER_RUNNING, false);
        Log.v(LOG_TAG, "Loading state.");
    }


    /* Saves app state to SharedPreferences */
    private void saveAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(PREF_PREF_STATION_ID_CURRENT, mStationIDCurrent);
        editor.putInt(PREF_STATION_ID_LAST, mStationIDLast);
        editor.putBoolean(PREF_PLAYBACK, mPlayback);
        editor.putBoolean(TIMER_RUNNING, mSleepTimerRunning);
        editor.apply();
        Log.v(LOG_TAG, "Saving state.");
    }


    /* Fetch new station with given Uri */
    private void fetchNewStation(Uri stationUri) {
        // download and add new station
        StationFetcher stationFetcher = new StationFetcher(stationUri, mActivity);
        stationFetcher.execute();

        // send local broadcast
        Intent i = new Intent();
        i.setAction(ACTION_COLLECTION_CHANGED);
        LocalBroadcastManager.getInstance(mActivity).sendBroadcast(i);
    }


    /* Initializes broadcast receivers for onCreate */
    private void initializeBroadcastReceivers() {
        // RECEIVER: player service stopped playback
        BroadcastReceiver playbackStoppedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mSleepTimerRunning && mSleepTimerService != null) {
                    stopSleepTimer();
                }
                refreshStationList();
                mPlayback = false;
            }
        };
        IntentFilter playbackStoppedIntentFilter = new IntentFilter(ACTION_PLAYBACK_STOPPED);
        LocalBroadcastManager.getInstance(mApplication).registerReceiver(playbackStoppedReceiver, playbackStoppedIntentFilter);

        // RECEIVER: player service started playback
        BroadcastReceiver playbackStartedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCollectionAdapter.refresh();
                refreshStationList();
                mPlayback = true;
            }
        };
        IntentFilter playbackStartedIntentFilter = new IntentFilter(ACTION_PLAYBACK_STARTED);
        LocalBroadcastManager.getInstance(mApplication).registerReceiver(playbackStartedReceiver, playbackStartedIntentFilter);

        // RECEIVER: station added, deleted, or changed
        BroadcastReceiver collectionChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // keep track of playback state
                if (mPlayback && intent.hasExtra(EXTRA_STATION_URI_CURRENT)) {

                    mCollectionAdapter.refresh();

                    mStationIDCurrent = mCollection.findStationID(intent.getStringExtra(EXTRA_STATION_URI_CURRENT));
                    // put up changed notification
                    NotificationHelper notificationHelper = new NotificationHelper(mActivity);
                    notificationHelper.setStationName(mCollection.getStations().get(mStationIDCurrent).getStationName());
                    notificationHelper.setStationID(mStationIDCurrent);
                    notificationHelper.createNotification();

                    Log.v(LOG_TAG, "!!! Debug / Currently Playing: " + mCollection.getStations().get(mStationIDCurrent).getStationName() + " / " + mStationIDCurrent);

                    // save app state
                    saveAppState(mActivity);
                }



                mCollectionAdapter.refresh();
                refreshStationList();
            }
        };
        IntentFilter collectionChangedIntentFilter = new IntentFilter(ACTION_COLLECTION_CHANGED);
        LocalBroadcastManager.getInstance(mApplication).registerReceiver(collectionChangedReceiver, collectionChangedIntentFilter);

        // RECEIVER: listen for request to change station image
        BroadcastReceiver imageChangeRequestReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // get station id and save it
                mTempStationImageID = intent.getIntExtra(EXTRA_STATION_ID, -1);
                // start image picker
                selectFromImagePicker();
            }
        };
        IntentFilter imageChangeRequestIntentFilter = new IntentFilter(ACTION_IMAGE_CHANGE_REQUESTED);
        LocalBroadcastManager.getInstance(mApplication).registerReceiver(imageChangeRequestReceiver, imageChangeRequestIntentFilter);

        // RECEIVER: sleep timer service sends updates
        BroadcastReceiver sleepTimerStartedReceiver = new BroadcastReceiver() {
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
                    refreshStationList();
                }

            }
        };
        IntentFilter sleepTimerIntentFilter = new IntentFilter(ACTION_TIMER_RUNNING);
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(sleepTimerStartedReceiver, sleepTimerIntentFilter);

        // RECEIVER: handles request for shortcut being created
        BroadcastReceiver shortcutCreationRequestReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // get station id and save it
                int stationID = intent.getIntExtra(EXTRA_STATION_ID, -1);

                // create shortcut
                ShortcutHelper shortcutHelper = new ShortcutHelper(mActivity, mCollection);
                shortcutHelper.placeShortcut(stationID);
            }
        };
        IntentFilter shortcutCreationRequestIntentFilter = new IntentFilter(ACTION_CREATE_SHORTCUT_REQUESTED);
        LocalBroadcastManager.getInstance(mApplication).registerReceiver(shortcutCreationRequestReceiver, shortcutCreationRequestIntentFilter);


        // RECEIVER: (re-)sets the selection if needed
        BroadcastReceiver changeViewSelectionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // reset previous selection
                mCollectionAdapter.resetSelection();
                if (intent.hasExtra(EXTRA_STATION_ID)) {
                    // set new id selected
                    mCollectionAdapter.setStationIDSelected(intent.getIntExtra(EXTRA_STATION_ID, 0));
                    mCollectionAdapter.notifyDataSetChanged();
                }
            }
        };
        IntentFilter changeViewSelectionIntentFilter = new IntentFilter(ACTION_CHANGE_VIEW_SELECTION);
        LocalBroadcastManager.getInstance(mApplication).registerReceiver(changeViewSelectionReceiver, changeViewSelectionIntentFilter);

    }


    /* Return a writeable sub-directory from external storage  */
    private File getCollectionDirectory(String subDirectory) {
        File[] storage = mActivity.getExternalFilesDirs(subDirectory);
        for (File file : storage) {
            String state = EnvironmentCompat.getStorageState(file);
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                Log.i(LOG_TAG, "External storage: " + file.toString());
                return file;
            }
        }
        Toast.makeText(mActivity, mActivity.getString(R.string.toastalert_no_external_storage), Toast.LENGTH_LONG).show();
        Log.e(LOG_TAG, "Unable to access external storage.");
        // finish activity
        mActivity.finish();

        return null;
    }


}
