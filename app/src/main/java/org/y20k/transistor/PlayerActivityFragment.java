/**
 * PlayerActivityFragment.java
 * Implements the main fragment of the player activity
 * This fragment is a detail view with the ability to start and stop playback
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
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
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
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import org.y20k.transistor.core.Collection;
import org.y20k.transistor.helpers.DialogDelete;
import org.y20k.transistor.helpers.DialogRename;
import org.y20k.transistor.helpers.ImageHelper;
import org.y20k.transistor.helpers.StorageHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * PlayerActivityFragment class
 */
public final class PlayerActivityFragment extends Fragment {

    /* Define log tag */
    private static final String LOG_TAG = PlayerActivityFragment.class.getSimpleName();


    /* Keys */
    private static final String ACTION_COLLECTION_CHANGED = "org.y20k.transistor.action.COLLECTION_CHANGED";
    private static final String ACTION_PLAYBACK_STOPPED = "org.y20k.transistor.action.PLAYBACK_STOPPED";
    private static final String ACTION_CREATE_SHORTCUT_REQUESTED = "org.y20k.transistor.action.CREATE_SHORTCUT_REQUESTED";
    private static final String ACTION_METADATA_CHANGED = "org.y20k.transistor.action.METADATA_CHANGED";
    private static final String EXTRA_METADATA = "METADATA";
    private static final String EXTRA_COLLECTION_CHANGE = "COLLECTION_CHANGE";
    private static final String EXTRA_STATION_NEW_NAME = "STATION_NEW_NAME";
    private static final String EXTRA_STATION_ID = "STATION_ID";
    private static final String ARG_STATION_ID = "ArgStationID";
    private static final String ARG_TWO_PANE = "ArgTwoPane";
    private static final String ARG_PLAYBACK = "ArgPlayback";
    private static final String PREF_STATION_ID_CURRENT = "prefStationIDCurrent";
    private static final String PREF_STATION_ID_LAST = "prefStationIDLast";
    private static final String PREF_STATION_ID_SELECTED = "prefStationIDSelected";
    private static final String PREF_PLAYBACK = "prefPlayback";
    private static final int STATION_RENAMED = 2;
    private static final int STATION_DELETED = 3;
    private static final int REQUEST_LOAD_IMAGE = 1;
    private static final int PERMISSION_REQUEST_IMAGE_PICKER_READ_EXTERNAL_STORAGE = 1;


    /* Main class variables */
    private Activity mActivity;
    private View mRootView;
    private String mStationName;
    private String mStreamUri;
    private TextView mStationNameView;
    private TextView mStationMetadataView;
    private ImageView mStationImageView;
    private ImageButton mStationMenuView;
    private ImageView mPlaybackIndicator;
    private ImageButton mPlaybackButton;
    private int mStationID;
    private int mStationIDCurrent;
    private int mStationIDLast;
    private boolean mPlayback;
    private boolean mTwoPane;
    private boolean mVisibility;
    private Collection mCollection;
    private PlayerService mPlayerService;


    /* Constructor (default) */
    public PlayerActivityFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get activity
        mActivity = getActivity();

        // initiate playback service
        mPlayerService = new PlayerService();

        // load playback state from preferences
        loadAppState(mActivity);

        // get collection from external storage
        StorageHelper storageHelper = new StorageHelper(mActivity);
        mCollection = new Collection(storageHelper.getCollectionDirectory());

        Bundle arguments = getArguments();
        // get station id from arguments
        if (arguments != null && arguments.containsKey(ARG_STATION_ID)) {
            mStationID = arguments.getInt(ARG_STATION_ID, 0);
        }

        // get tablet or phone mode info from arguments
        if (arguments != null && arguments.containsKey(ARG_TWO_PANE)) {
            mTwoPane = arguments.getBoolean(ARG_TWO_PANE, false);
        } else {
            mTwoPane = false;
        }

        // get URL and name for stream
        mStreamUri = mCollection.getStations().get(mStationID).getStreamUri().toString();
        mStationName = mCollection.getStations().get(mStationID).getStationName();

        // fragment has options menu
        setHasOptionsMenu(true);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // inflate rootview from xml
        mRootView = inflater.inflate(R.layout.fragment_player, container, false);

        // find views for station name and image and playback indicator
        mStationNameView = (TextView) mRootView.findViewById(R.id.player_textview_stationname);
        mStationMetadataView = (TextView) mRootView.findViewById(R.id.player_textview_station_metadata);
        mStationImageView = (ImageView) mRootView.findViewById(R.id.player_imageview_station_icon);
        mPlaybackIndicator = (ImageView) mRootView.findViewById(R.id.player_playback_indicator);
        mStationMenuView = (ImageButton) mRootView.findViewById(R.id.player_item_more_button);

        // set station image
        Bitmap stationImage = createStationImage();
        if (stationImage != null) {
            mStationImageView.setImageBitmap(stationImage);
        }

        // set text view to station name and add listener for clipboard copy
        mStationNameView.setText(mStationName);
        mStationNameView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyStationToClipboard();
            }
        });

        // show three dots menu in tablet mode
        if (mTwoPane) {
            mStationMenuView.setVisibility(View.VISIBLE);
            // attach three dots menu
            mStationMenuView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PopupMenu popup = new PopupMenu(mActivity, view);
                    popup.inflate(R.menu.menu_main_list_item);
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            return handleMenuClick(item, false);
                        }
                    });
                    popup.show();
                }
            });
        }

        // construct big playback button
        mPlaybackButton = (ImageButton) mRootView.findViewById(R.id.player_playback_button);
        mPlaybackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // handle tap on big playback button
                handlePlaybackButtonClick();
            }
        });

        return mRootView;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // setVisualState();

        // initialize broadcast receivers
        initializeBroadcastReceivers();

    }


    @Override
    public void onResume() {
        super.onResume();
        // set fragment visibility
        mVisibility = true;

        // refresh playback state
        loadAppState(mActivity);

        // check if activity started from shortcut
        Bundle arguments = getArguments();
        if (arguments != null && arguments.getBoolean(ARG_PLAYBACK)) {
            // check if this station is not already playing
            if (!mPlayback || mStationIDCurrent != mStationID) {
                // start playback
                startPlayback();
                // save state
                mStationIDLast = mStationIDCurrent;
                mStationIDCurrent = mStationID;
                saveAppState(mActivity);
                // clear playback state argument
                getArguments().putBoolean(ARG_PLAYBACK, false);
            }

        }

        // set up button symbol and playback indicator
        setVisualState();

    }


    @Override
    public void onPause() {
        super.onPause();
        // set fragment visibility
        mVisibility = false;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return handleMenuClick(item, super.onOptionsItemSelected(item));
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_IMAGE_PICKER_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted - get system picker for images
                    Intent pickImageIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pickImageIntent, REQUEST_LOAD_IMAGE);
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


    /* Starts player service */
    private void startPlayback() {
        // set playback true
        mPlayback = true;
        // rotate playback button
        changeVisualState(mActivity);
        // start player
        Log.v(LOG_TAG, "Starting player service.");
        mPlayerService.startActionPlay(mActivity, mStreamUri, mStationName, mStationID);
    }


    /* Stops player service */
    private void stopPlayback() {
        // set playback false
        mPlayback = false;
        // rotate playback button
        changeVisualState(mActivity);
        // stop player
        Log.v(LOG_TAG, "Stopping player service.");
        mPlayerService.startActionStop(mActivity);
    }


    /* Handles tap on the big playback button */
    private void handlePlaybackButtonClick() {
        // playback stopped or new station - start playback
        if (!mPlayback || mStationID != mStationIDCurrent) {
            startPlayback();
        }
        // playback active - stop playback
        else {
            stopPlayback();
        }
        // update and save currently and last played station
        setStationState();
        saveAppState(mActivity);
    }


    /* Handles menu selection */
    private boolean handleMenuClick(MenuItem item, boolean defaultReturn) {

        switch (item.getItemId()) {

            // CASE ICON
            case R.id.menu_icon:
                // get system picker for images
                selectFromImagePicker();
                return true;

            // CASE RENAME
            case R.id.menu_rename:
                // construct and run rename dialog
                final DialogRename dialogRename = new DialogRename(mActivity, mCollection, mStationName, mStationID);
                dialogRename.show();
                return true;

            // CASE DELETE
            case R.id.menu_delete:
                // stop playback
                mPlayerService.startActionStop(mActivity);
                // construct and run delete dialog
                DialogDelete dialogDelete = new DialogDelete(mActivity, mCollection, mStationID);
                dialogDelete.show();
                return true;

            // CASE SHORTCUT
            case R.id.menu_shortcut: {
                // send local broadcast (needed by MainActivityFragment)
                Intent shortcutIntent = new Intent();
                shortcutIntent.setAction(ACTION_CREATE_SHORTCUT_REQUESTED);
                shortcutIntent.putExtra(EXTRA_STATION_ID, mStationID);
                LocalBroadcastManager.getInstance(mActivity.getApplication()).sendBroadcast(shortcutIntent);
                return true;
            }

            // CASE DEFAULT
            default:
                return defaultReturn;
        }

    }


    /* Create Bitmap image for station */
    private Bitmap createStationImage () {
        Bitmap stationImageSmall;
        ImageHelper imageHelper;
        if (!mCollection.getStations().isEmpty() && mCollection.getStations().get(mStationID).getStationImageFile().exists()) {
            // get image from collection
            stationImageSmall = BitmapFactory.decodeFile(mCollection.getStations().get(mStationID).getStationImageFile().toString());
        } else {
            // get default image
            stationImageSmall = BitmapFactory.decodeResource(getResources(), R.drawable.ic_notesymbol);
        }
        imageHelper = new ImageHelper(stationImageSmall, mActivity);

        return imageHelper.createCircularFramedImage(192, R.color.transistor_grey_lighter);
    }


    /* Copy station date to system clipboard */
    private void copyStationToClipboard() {
        if (mStreamUri != null && mStationName != null) {
            // prepare clip
            String stationData = mStationName + " - " + mStreamUri;
            ClipData clip = ClipData.newPlainText("simple text",stationData);

            // copy clip to clipboard
            ClipboardManager cm = (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(clip);

            // notify user
            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_station_copied), Toast.LENGTH_SHORT).show();
        }
    }


    /* Processes new image and saves it to storage */
    private void processNewImage(Uri newImageUri) {

        ImageHelper imageHelper = new ImageHelper(newImageUri, mActivity);
        Bitmap newImage = imageHelper.getInputImage();

        if (newImage != null) {
            // write image to storage
            File stationImageFile = mCollection.getStations().get(mStationID).getStationImageFile();
            try (FileOutputStream out = new FileOutputStream(stationImageFile)) {
                newImage.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Unable to save: " + newImage.toString());
            }
            // change mStationImageView
            Bitmap stationImage = imageHelper.createCircularFramedImage(192, R.color.transistor_grey_lighter);
            mStationImageView.setImageBitmap(stationImage);
        } else {
            Log.e(LOG_TAG, "Unable to get image from media picker: " + newImageUri.toString());
        }
    }


    /* Animate button and then set visual state */
    private void changeVisualState(Context context) {
        // get rotate animation from xml
        Animation rotate;
        if (mPlayback) {
            // if playback has been started get start animation
            rotate = AnimationUtils.loadAnimation(context, R.anim.rotate_clockwise_slow);
        } else {
            // if playback has been stopped get stop animation
            rotate = AnimationUtils.loadAnimation(context, R.anim.rotate_counterclockwise_fast);
        }

        // attach listener for animation end
        rotate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // set up button symbol and playback indicator afterwards
                setVisualState();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        // start animation of button
        mPlaybackButton.startAnimation(rotate);

    }


    /* Set button symbol and playback indicator */
    private void setVisualState() {

        // this station is running
        if (mPlayback && mStationID == mStationIDCurrent) {
            // change playback button image to stop
            mPlaybackButton.setImageResource(R.drawable.smbl_stop);
            // change playback indicator
            mPlaybackIndicator.setBackgroundResource(R.drawable.ic_playback_indicator_started_24dp);
        }
        // playback stopped
        else {
            // change playback button image to play
            mPlaybackButton.setImageResource(R.drawable.smbl_play);
            // change playback indicator
            mPlaybackIndicator.setBackgroundResource(R.drawable.ic_playback_indicator_stopped_24dp);
        }
    }


    /* set state of currently playing station */
    private void setStationState() {
        // playback started
        if (mPlayback) {
            mStationIDLast = mStationIDCurrent;
            mStationIDCurrent = mStationID;
        }
        // playback stopped
        else {
            mStationIDLast = mStationIDCurrent;
            mStationIDCurrent = -1;
        }
    }


    /* Saves app state to SharedPreferences */
    private void saveAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(PREF_STATION_ID_CURRENT, mStationIDCurrent);
        editor.putInt(PREF_STATION_ID_LAST, mStationIDLast);
        editor.putBoolean(PREF_PLAYBACK, mPlayback);
        editor.apply();
        Log.v(LOG_TAG, "Saving state ("+  mStationIDCurrent + " / " + mStationIDLast + " / " + mPlayback + ")");
    }


    /* Loads app state from preferences */
    private void loadAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        mStationIDCurrent = settings.getInt(PREF_STATION_ID_CURRENT, -1);
        mStationIDLast = settings.getInt(PREF_STATION_ID_LAST, -1);
        mStationID = settings.getInt(PREF_STATION_ID_SELECTED, 0);
        mPlayback = settings.getBoolean(PREF_PLAYBACK, false);
        Log.v(LOG_TAG, "Loading state ("+  mStationIDCurrent + " / " + mStationIDLast + " / " + mPlayback + " / " + mStationID + ")");
    }


    /* Check permissions and start image picker */
    @TargetApi(Build.VERSION_CODES.M)
    private void selectFromImagePicker() {
        // permission to read external storage granted
        if (ActivityCompat.checkSelfPermission(mActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {

            // get system picker for images
            Intent pickImageIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickImageIntent, REQUEST_LOAD_IMAGE);
        }
        // permission to read external storage granted
        else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // ask for permission and explain why
                Snackbar snackbar = Snackbar.make(mRootView, mActivity.getString(R.string.snackbar_request_storage_access), Snackbar.LENGTH_INDEFINITE);
                snackbar.setAction(R.string.dialog_generic_button_okay, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                PERMISSION_REQUEST_IMAGE_PICKER_READ_EXTERNAL_STORAGE);
                    }
                });
                snackbar.show();

            } else {
                // ask for permission without explanation
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_IMAGE_PICKER_READ_EXTERNAL_STORAGE);
            }
        }
    }


    /* Handles adding, deleting and renaming of station */
    private void handleCollectionChanges(Intent intent) {
        switch (intent.getIntExtra(EXTRA_COLLECTION_CHANGE, 1)) {
            // CASE: station was renamed
            case STATION_RENAMED:
                if (intent.hasExtra(EXTRA_STATION_NEW_NAME)) {
                    mStationName = intent.getStringExtra(EXTRA_STATION_NEW_NAME);
                    mStationNameView.setText(mStationName);
                }
                break;

            // CASE: station was deleted
            case STATION_DELETED:
                if (!mTwoPane && mVisibility) {
                    // start main activity
                    Intent mainActivityStartIntent = new Intent(mActivity, MainActivity.class);
                    startActivity(mainActivityStartIntent);
                    // finish player activity
                    mActivity.finish();
                } else {
                    // get collection from external storage
                    StorageHelper storageHelper = new StorageHelper(mActivity);
                    mCollection = new Collection(storageHelper.getCollectionDirectory());

                    int collectionSize = mCollection.getStations().size();

                    if (mStationID > collectionSize) {
                        // station at the end of the list was deleted
                        mStationID = collectionSize - 1;
                    }

                    if (collectionSize != 0 && mStationID >= 0) {
                        // get URL and name for stream
                        mStreamUri = mCollection.getStations().get(mStationID).getStreamUri().toString();
                        mStationName = mCollection.getStations().get(mStationID).getStationName();

                        mStationNameView.setText(mStationName);

                        // set station image
                        Bitmap stationImage = createStationImage();
                        if (stationImage != null) {
                            mStationImageView.setImageBitmap(stationImage);
                        }
                    }

                }
                break;
        }
    }


    /* Initializes broadcast receivers for onCreate */
    private void initializeBroadcastReceivers() {

        // RECEIVER: player service stopped playback
        BroadcastReceiver playbackStoppedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mPlayback) {
                    // set playback false
                    mPlayback = false;
                    // rotate playback button
                    changeVisualState(context);
                }
                // save state of playback to settings
                saveAppState(context);
            }
        };
        IntentFilter playbackIntentFilter = new IntentFilter(ACTION_PLAYBACK_STOPPED);
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(playbackStoppedReceiver, playbackIntentFilter);

        // RECEIVER: station added, deleted, or changed
        BroadcastReceiver collectionChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && intent.hasExtra(EXTRA_COLLECTION_CHANGE)) {
                    handleCollectionChanges(intent);
                }
            }
        };
        IntentFilter collectionChangedIntentFilter = new IntentFilter(ACTION_COLLECTION_CHANGED);
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(collectionChangedReceiver, collectionChangedIntentFilter);

        // RECEIVER: station metadata has changed
        BroadcastReceiver metadataChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra(EXTRA_METADATA)) {
                    mStationMetadataView.setText(intent.getStringExtra(EXTRA_METADATA));
                    mStationMetadataView.setVisibility(View.VISIBLE);
                }
            }
        };
        IntentFilter metadataChangedIntentFilter = new IntentFilter(ACTION_METADATA_CHANGED);
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(metadataChangedReceiver, metadataChangedIntentFilter);

    }


}