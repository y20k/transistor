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
import android.app.NotificationManager;
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
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
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

import org.y20k.transistor.core.Collection;
import org.y20k.transistor.core.Station;
import org.y20k.transistor.helpers.CollectionAdapter;
import org.y20k.transistor.helpers.DialogAddStation;
import org.y20k.transistor.helpers.ImageHelper;
import org.y20k.transistor.helpers.NotificationHelper;
import org.y20k.transistor.helpers.ShortcutHelper;
import org.y20k.transistor.helpers.SleepTimerService;
import org.y20k.transistor.helpers.StationFetcher;
import org.y20k.transistor.helpers.TransistorKeys;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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
    private Collection mCollection;
    private CollectionAdapter mCollectionAdapter = null;
    private File mFolder;
    private ArrayList<String> mStationNames;
    private ArrayList<Bitmap> mStationImages;
    private View mRootView;
    private View mActionCallView;
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

        // fragment has options menu
        setHasOptionsMenu(true);

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

        // get collection
        Intent intent = mActivity.getIntent();
        if (intent.hasExtra(TransistorKeys.EXTRA_COLLECTION)) {
            mCollection = intent.getParcelableExtra(TransistorKeys.EXTRA_COLLECTION);
        }

        // create adapter for collection
        mStationNames = new ArrayList<>();
        mStationImages = new ArrayList<>();
        mCollectionAdapter = new CollectionAdapter(mActivity, mCollection, mStationNames, mStationImages);

        // initialize broadcast receivers
        initializeBroadcastReceivers();

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // get list state from saved instance
        if (savedInstanceState != null) {
            mListState = savedInstanceState.getParcelable(TransistorKeys.INSTANCE_LIST_STATE);
        }

        // inflate rootview from xml
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

        // handle incoming intent
        handleIncomingIntent();

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

                DialogAddStation dialog = new DialogAddStation(mActivity, mCollection);
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
        super.onSaveInstanceState(outState);
        // save list view position
        mListState = mLayoutManager.onSaveInstanceState();
        outState.putParcelable(TransistorKeys.INSTANCE_LIST_STATE, mListState);
    }


    /* Fills collection adapter */
    private void fillCollectionAdapter() {

        Bitmap stationImage;
        Bitmap stationImageSmall;
        ImageHelper imageHelper;

        // put stations into collection adapter
        for (Station station : mCollection.getStations()) {
            // add name to linked list of names
            mStationNames.add(station.getStationName());

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
        if (!mStationNames.isEmpty() && !mStationImages.isEmpty()) {
            mStationNames.clear();
            mStationImages.clear();
        }
        fillCollectionAdapter();

        // show call to action, if necessary
        if (mCollectionAdapter.getItemCount() == 0) {
            mActionCallView.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        } else {
            mActionCallView.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }

        Log.v(LOG_TAG, "Refreshing list of stations");

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
                if (requestPermissionReadExternalStorage(TransistorKeys.PERMISSION_REQUEST_STATION_FETCHER_READ_EXTERNAL_STORAGE)) {
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
        if (requestCode == TransistorKeys.REQUEST_LOAD_IMAGE && resultCode == Activity.RESULT_OK && null != data) {
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
        if (requestPermissionReadExternalStorage(TransistorKeys.PERMISSION_REQUEST_IMAGE_PICKER_READ_EXTERNAL_STORAGE)) {
            // get system picker for images
            Intent pickImageIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            mActivity.startActivityForResult(pickImageIntent, TransistorKeys.REQUEST_LOAD_IMAGE);
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
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(remainingTime),
                TimeUnit.MILLISECONDS.toSeconds(remainingTime) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(remainingTime)));
    }


    /* Loads app state from preferences */
    private void loadAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        mStationIDCurrent = settings.getInt(TransistorKeys.PREF_STATION_ID_CURRENT, -1);
        mStationIDLast = settings.getInt(TransistorKeys.PREF_STATION_ID_LAST, -1);
        mPlayback = settings.getBoolean(TransistorKeys.PREF_PLAYBACK, false);
        mSleepTimerRunning = settings.getBoolean(TransistorKeys.PREF_TIMER_RUNNING, false);
        Log.v(LOG_TAG, "Loading state.");
    }


    /* Saves app state to SharedPreferences */
    private void saveAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(TransistorKeys.PREF_STATION_ID_CURRENT, mStationIDCurrent);
        editor.putInt(TransistorKeys.PREF_STATION_ID_LAST, mStationIDLast);
        editor.putBoolean(TransistorKeys.PREF_PLAYBACK, mPlayback);
        editor.putBoolean(TransistorKeys.PREF_TIMER_RUNNING, mSleepTimerRunning);
        editor.apply();
        Log.v(LOG_TAG, "Saving state.");
    }


    /* Fetch new station with given Uri */
    private void fetchNewStation(Uri stationUri) {
        // download and add new station
        StationFetcher stationFetcher = new StationFetcher(mActivity, mCollection, stationUri);
        stationFetcher.execute();

//        // send local broadcast
//        Intent i = new Intent();
//        i.setAction(TransistorKeys.ACTION_COLLECTION_CHANGED);
//        LocalBroadcastManager.getInstance(mActivity).sendBroadcast(i);
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
                mCollectionAdapter.refresh();
                refreshStationList();
                mPlayback = false;
            }
        };
        IntentFilter playbackStoppedIntentFilter = new IntentFilter(TransistorKeys.ACTION_PLAYBACK_STOPPED);
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
        IntentFilter playbackStartedIntentFilter = new IntentFilter(TransistorKeys.ACTION_PLAYBACK_STARTED);
        LocalBroadcastManager.getInstance(mApplication).registerReceiver(playbackStartedReceiver, playbackStartedIntentFilter);

        // RECEIVER: station added, deleted, or changed
        BroadcastReceiver collectionChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && intent.hasExtra(TransistorKeys.EXTRA_COLLECTION_CHANGE)) {
                    handleCollectionChanges(intent);
                }
            }
        };
        IntentFilter collectionChangedIntentFilter = new IntentFilter(TransistorKeys.ACTION_COLLECTION_CHANGED);
        LocalBroadcastManager.getInstance(mApplication).registerReceiver(collectionChangedReceiver, collectionChangedIntentFilter);

        // RECEIVER: listen for request to change station image
        BroadcastReceiver imageChangeRequestReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // get station id and save it
                mTempStationImageID = intent.getIntExtra(TransistorKeys.EXTRA_STATION_ID, -1);
                // start image picker
                selectFromImagePicker();
            }
        };
        IntentFilter imageChangeRequestIntentFilter = new IntentFilter(TransistorKeys.ACTION_IMAGE_CHANGE_REQUESTED);
        LocalBroadcastManager.getInstance(mApplication).registerReceiver(imageChangeRequestReceiver, imageChangeRequestIntentFilter);

        // RECEIVER: sleep timer service sends updates
        BroadcastReceiver sleepTimerStartedReceiver = new BroadcastReceiver() {
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
                    refreshStationList();
                }

            }
        };
        IntentFilter sleepTimerIntentFilter = new IntentFilter(TransistorKeys.ACTION_TIMER_RUNNING);
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(sleepTimerStartedReceiver, sleepTimerIntentFilter);

        // RECEIVER: handles request for shortcut being created
        BroadcastReceiver shortcutCreationRequestReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // get station id and save it
                int stationID = intent.getIntExtra(TransistorKeys.EXTRA_STATION_ID, -1);

                // create shortcut
                ShortcutHelper shortcutHelper = new ShortcutHelper(mActivity, mCollection);
                shortcutHelper.placeShortcut(stationID);
            }
        };
        IntentFilter shortcutCreationRequestIntentFilter = new IntentFilter(TransistorKeys.ACTION_CREATE_SHORTCUT_REQUESTED);
        LocalBroadcastManager.getInstance(mApplication).registerReceiver(shortcutCreationRequestReceiver, shortcutCreationRequestIntentFilter);


        // RECEIVER: (re-)sets the selection if needed
        BroadcastReceiver changeViewSelectionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // reset previous selection
                mCollectionAdapter.resetSelection();
                if (intent.hasExtra(TransistorKeys.EXTRA_STATION_ID)) {
                    // set new id selected
                    mCollectionAdapter.setStationIDSelected(intent.getIntExtra(TransistorKeys.EXTRA_STATION_ID, 0));
                    mCollectionAdapter.notifyDataSetChanged();
                }
            }
        };
        IntentFilter changeViewSelectionIntentFilter = new IntentFilter(TransistorKeys.ACTION_CHANGE_VIEW_SELECTION);
        LocalBroadcastManager.getInstance(mApplication).registerReceiver(changeViewSelectionReceiver, changeViewSelectionIntentFilter);

    }


    /* Handles adding, deleting and renaming of station */
    private void handleCollectionChanges(Intent intent) {
        // load app state
        loadAppState(mActivity);

        switch (intent.getIntExtra(TransistorKeys.EXTRA_COLLECTION_CHANGE, 1)) {

            // CASE: station was added
            case TransistorKeys.STATION_ADDED:
                if (intent.hasExtra(TransistorKeys.EXTRA_COLLECTION)) {
                    mCollection = intent.getParcelableExtra(TransistorKeys.EXTRA_COLLECTION);
                }
                // refresh collection adapter and station list
                mCollectionAdapter.refresh();
                refreshStationList();
                break;

            // CASE: station was renamed
            case TransistorKeys.STATION_RENAMED:
                // keep track of playback state
                if (mPlayback && intent.hasExtra(TransistorKeys.EXTRA_STATION_ID) && intent.hasExtra(TransistorKeys.EXTRA_STATION_URI_CURRENT)) {

                    // retrieve and save ID of currently playing station from Uri
                    mStationIDCurrent = mCollection.findStationID(intent.getStringExtra(TransistorKeys.EXTRA_STATION_URI_CURRENT));
                    saveAppState(mActivity);

                    // put up notification
                    NotificationHelper notificationHelper = new NotificationHelper(mCollection);
                    notificationHelper.setStationName(mCollection.getStations().get(mStationIDCurrent).getStationName());
                    notificationHelper.setStationID(mStationIDCurrent);
                    notificationHelper.createNotification(mActivity);
                }

                // refresh collection adapter and station list
                mCollectionAdapter.refresh();
                refreshStationList();
                break;

            // CASE: station was deleted
            case TransistorKeys.STATION_DELETED:
                // keep track of playback state
                if (mPlayback && intent.hasExtra(TransistorKeys.EXTRA_STATION_ID) && mStationIDCurrent == intent.getIntExtra(TransistorKeys.EXTRA_STATION_ID, -1)) {
                    mPlayback = false;
                    saveAppState(mActivity);

                    PlayerService playerService = new PlayerService();
                    playerService.startActionStop(mActivity);

                    NotificationManager notificationManager = (NotificationManager) mActivity.getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancel(TransistorKeys.PLAYER_SERVICE_NOTIFICATION_ID);

                } else if (mPlayback && intent.hasExtra(TransistorKeys.EXTRA_STATION_URI_CURRENT)) {
                    mStationIDCurrent = mCollection.findStationID(intent.getStringExtra(TransistorKeys.EXTRA_STATION_URI_CURRENT));
                    saveAppState(mActivity);
                }

                // refresh collection adapter and station list
                mCollectionAdapter.refresh();
                refreshStationList();
                break;

            // TODO station was renamed in PlayerActivityFragment (playback indicator in list)

        }

    }

}
