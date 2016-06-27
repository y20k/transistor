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

import org.y20k.transistor.core.Station;
import org.y20k.transistor.helpers.DialogDelete;
import org.y20k.transistor.helpers.DialogRename;
import org.y20k.transistor.helpers.ImageHelper;
import org.y20k.transistor.helpers.PermissionHelper;
import org.y20k.transistor.helpers.ShortcutHelper;
import org.y20k.transistor.helpers.TransistorKeys;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * PlayerActivityFragment class
 */
public final class PlayerActivityFragment extends Fragment {

    /* Define log tag */
    private static final String LOG_TAG = PlayerActivityFragment.class.getSimpleName();


    /* Main class variables */
    private Activity mActivity;
    private View mRootView;
    private String mStationName;
    private String mStationMetadata;
    private String mStreamUri;
    private TextView mStationNameView;
    private TextView mStationMetadataView;
    private ImageView mStationImageView;
    private ImageButton mStationMenuView;
    private ImageView mPlaybackIndicator;
    private ImageButton mPlaybackButton;
    private BroadcastReceiver mPlaybackStateChangedReceiver;
    private BroadcastReceiver mCollectionChangedReceiver;
    private BroadcastReceiver mMetadataChangedReceiver;
    private int mStationID;
    private int mStationIDCurrent;
    private int mStationIDLast;
    private boolean mPlayback;
    private boolean mStationLoading;
    private boolean mTwoPane;
    private boolean mVisibility;
    private Station mStation;


    /* Constructor (default) */
    public PlayerActivityFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        // set fragment visibility
        mVisibility = false;

        // set loading status
        mStationLoading = false;

        // get activity
        mActivity = getActivity();

        // load playback state from preferences
        loadAppState(mActivity);


        // get data from arguments
        Bundle arguments = getArguments();
        if (arguments != null) {

            // get station from arguments
            if (arguments.containsKey(TransistorKeys.ARG_STATION)) {
                mStation = arguments.getParcelable(TransistorKeys.ARG_STATION);
                arguments.remove(TransistorKeys.ARG_STATION);
            }

            // get station ID from arguments
            if (arguments.containsKey(TransistorKeys.ARG_STATION_ID)) {
                mStationID = arguments.getInt(TransistorKeys.ARG_STATION_ID);
                arguments.remove(TransistorKeys.ARG_STATION_ID);
            } else {
                mStationID = 0;
                Log.e(LOG_TAG, "Error: station is null. Choosing default ID for station");
            }

            // get station name and Uri
            if (mStation != null) {
                mStationName = mStation.getStationName();
                mStreamUri = mStation.getStreamUri().toString();
            } else {
                mStationName = mActivity.getString(R.string.descr_station_name_example);
                Log.e(LOG_TAG, "Error: station is null. Displaying default station name");
            }

            // get tablet or phone mode info from arguments
            if (arguments.containsKey(TransistorKeys.ARG_TWO_PANE)) {
                mTwoPane = arguments.getBoolean(TransistorKeys.ARG_TWO_PANE, false);
            } else {
                mTwoPane = false;
            }

        }

        // fragment has options menu
        setHasOptionsMenu(true);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {

        // inflate root view from xml
        mRootView = inflater.inflate(R.layout.fragment_player, container, false);

        // find views for station name and image and playback indicator
        mStationNameView = (TextView) mRootView.findViewById(R.id.player_textview_stationname);
        mStationMetadataView = (TextView) mRootView.findViewById(R.id.player_textview_station_metadata);
        mStationImageView = (ImageView) mRootView.findViewById(R.id.player_imageview_station_icon);
        mPlaybackIndicator = (ImageView) mRootView.findViewById(R.id.player_playback_indicator);
        mStationMenuView = (ImageButton) mRootView.findViewById(R.id.player_item_more_button);

        // set station name
        mStationNameView.setText(mStationName);

        // set station image
        Bitmap stationImage = createStationImage();
        if (stationImage != null) {
            mStationImageView.setImageBitmap(stationImage);
        }

        // add listener to station info view for clipboard copy
        View stationInfoView = mRootView.findViewById(R.id.player_layout_station_info);
        stationInfoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyStationToClipboard();
            }
        });

        // show metadata
        if (mPlayback && mStationMetadata != null) {
            mStationMetadataView.setText(mStationMetadata);
            mStationMetadataView.setVisibility(View.VISIBLE);
        } else {
            mStationMetadataView.setVisibility(View.GONE);
        }

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

        // get arguments
        Bundle args = getArguments();

        // get station from arguments
        if (args != null && args.containsKey(TransistorKeys.ARG_STATION)) {
            mStation = args.getParcelable(TransistorKeys.ARG_STATION);
        }

        // check if activity started from shortcut
        if (args != null && args.getBoolean(TransistorKeys.ARG_PLAYBACK)) {
            // check if this station is not already playing
            Station currentStation = PlayerService.getStation();
            if (currentStation != null && currentStation.getStreamUri().equals(mStation.getStreamUri()) && mPlayback && !mStationLoading) {
                Log.v(LOG_TAG, "Try to start playback from shortcut, but station is already running.");
            } else {
                // hide metadata
                mStationMetadata = null;
                mStationMetadataView.setVisibility(View.GONE);
                // start playback
                mStation.setPlaybackState(true);
                mPlayback = true;
                startPlayback();
                // clear playback state argument
                getArguments().putBoolean(TransistorKeys.ARG_PLAYBACK, false);
                Log.v(LOG_TAG, "Staring playback from shortcut.");
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
    public void onDestroy() {
        super.onDestroy();
        unregisterBroadcastReceivers();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return handleMenuClick(item, super.onOptionsItemSelected(item));
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case TransistorKeys.PERMISSION_REQUEST_IMAGE_PICKER_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted - get system picker for images
                    Intent pickImageIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pickImageIntent, TransistorKeys.REQUEST_LOAD_IMAGE);
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

        // retrieve selected image Uri from image picker
        Uri newImageUri = data.getData();

        if (requestCode == TransistorKeys.REQUEST_LOAD_IMAGE && resultCode == Activity.RESULT_OK && null != data) {

            ImageHelper imageHelper = new ImageHelper(newImageUri, mActivity);
            Bitmap newImage = imageHelper.getInputImage();

            if (newImage != null) {
                // write image to storage
                File stationImageFile = mStation.getStationImageFile();
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
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // save current station
        outState.putParcelable(TransistorKeys.INSTANCE_STATION, mStation);
        outState.putInt(TransistorKeys.INSTANCE_STATION_ID, mStationID);
        outState.putBoolean(TransistorKeys.INSTANCE_PLAYBACK, mPlayback);
    }


    /* Starts player service */
    private void startPlayback() {
        // set playback true
        mPlayback = true;
        mStation.setPlaybackState(true);

        // set station loading status
        mStationLoading = true;

        // rotate playback button
        changeVisualState(mActivity);

        // start player
        Log.v(LOG_TAG, "Starting player service.");
        PlayerService.startActionPlay(mActivity, mStation, mStationID);

    }


    /* Stops player service */
    private void stopPlayback() {
        // set playback false
        mPlayback = false;
        mStation.setPlaybackState(false);

        // reset metadata
        mStationMetadata = null;

        // rotate playback button
        changeVisualState(mActivity);

        // save state of playback to settings
        saveAppState(mActivity);

        // stop player
        Log.v(LOG_TAG, "Stopping player service.");
        PlayerService.startActionStop(mActivity);
    }


    /* Handles tap on the big playback button */
    private void handlePlaybackButtonClick() {
        // playback stopped or new station - start playback
        if (!mPlayback || !mStation.getPlaybackState()) {
            startPlayback();
        }
        // playback active - stop playback
        else {
            stopPlayback();
        }
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
                final DialogRename dialogRename = new DialogRename(mActivity, mStation, mStationID);
                dialogRename.show();
                return true;

            // CASE DELETE
            case R.id.menu_delete:
                // stop playback
                PlayerService.startActionStop(mActivity);
                // construct and run delete dialog
                DialogDelete dialogDelete = new DialogDelete(mActivity, mStation, mStationID);
                dialogDelete.show();
                return true;

            // CASE SHORTCUT
            case R.id.menu_shortcut: {
                // create shortcut
                ShortcutHelper shortcutHelper = new ShortcutHelper(mActivity.getApplication().getApplicationContext());
                shortcutHelper.placeShortcut(mStation);
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
        if (mStation != null &&  mStation.getStationImageFile().exists()) {
            // get image from collection
            stationImageSmall = BitmapFactory.decodeFile(mStation.getStationImageFile().toString());
        } else {
            // get default image
            stationImageSmall = null;
        }
        imageHelper = new ImageHelper(stationImageSmall, mActivity);

        return imageHelper.createCircularFramedImage(192, R.color.transistor_grey_lighter);
    }


    /* Copy station date to system clipboard */
    private void copyStationToClipboard() {
        if (mStreamUri != null && mStationName != null) {

            // create clip
            String clipboardText;
            ClipData clip;
            if (mStationMetadata != null) {
                clipboardText = mStationName +  " - " + mStationMetadata + " (" + mStreamUri + ")";
            } else {
                clipboardText = mStationName + " (" + mStreamUri + ")";
            }
            clip = ClipData.newPlainText("simple text",clipboardText);

            // copy clip to clipboard
            ClipboardManager cm = (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(clip);

            // notify user
            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_station_copied), Toast.LENGTH_SHORT).show();
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
//        if (mPlayback && mStationID == mStationIDCurrent) {
        if (mPlayback && mStation != null && mStation.getPlaybackState()) {
            // change playback button image to stop
            mPlaybackButton.setImageResource(R.drawable.smbl_stop);
            // change playback indicator
            if (mStationLoading) {
                mPlaybackIndicator.setBackgroundResource(R.drawable.ic_playback_indicator_loading_24dp);
                mStationMetadataView.setText(R.string.descr_station_stream_loading);
            } else {
                mPlaybackIndicator.setBackgroundResource(R.drawable.ic_playback_indicator_started_24dp);
            }
            // show metadata view
            mStationMetadataView.setVisibility(View.VISIBLE);
        }
        // playback stopped
        else {
            // change playback button image to play
            mPlaybackButton.setImageResource(R.drawable.smbl_play);
            // change playback indicator
            mPlaybackIndicator.setBackgroundResource(R.drawable.ic_playback_indicator_stopped_24dp);
            // hide metadata view
            mStationMetadataView.setVisibility(View.GONE);
            // mStationMetadata = null;
        }

    }


    /* Saves app state to SharedPreferences */
    private void saveAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
//        editor.putInt(TransistorKeys.PREF_STATION_ID_CURRENTLY_PLAYING, mStationIDCurrent);
//        editor.putInt(TransistorKeys.PREF_STATION_ID_LAST, mStationIDLast);
        editor.putString(TransistorKeys.PREF_STATION_METADATA, mStationMetadata);
//        editor.putBoolean(TransistorKeys.PREF_PLAYBACK, mPlayback);
//        editor.putBoolean(TransistorKeys.PREF_STATION_LOADING, mStationLoading);
        editor.apply();
        Log.v(LOG_TAG, "Saving state ("+ mStationMetadata + ")");
    }


    /* Loads app state from preferences */
    private void loadAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        mStationIDCurrent = settings.getInt(TransistorKeys.PREF_STATION_ID_CURRENTLY_PLAYING, -1);
        mStationIDLast = settings.getInt(TransistorKeys.PREF_STATION_ID_LAST, -1);
        mStationID = settings.getInt(TransistorKeys.PREF_STATION_ID_SELECTED, 0);
        mStationMetadata = settings.getString(TransistorKeys.PREF_STATION_METADATA, null);
        mPlayback = settings.getBoolean(TransistorKeys.PREF_PLAYBACK, false);
        mStationLoading = settings.getBoolean(TransistorKeys.PREF_STATION_LOADING, false);
        Log.v(LOG_TAG, "Loading state ("+  mStationIDCurrent + " / " + mStationIDLast + " / " + mPlayback + " / " + mStationLoading + " / " + mStationMetadata + ")");
    }


    /* Check permissions and start image picker */
    @TargetApi(Build.VERSION_CODES.M)
    private void selectFromImagePicker() {
        // request permissions
        PermissionHelper permissionHelper = new PermissionHelper(mActivity, mRootView);
        if (permissionHelper.requestReadExternalStorage(TransistorKeys.PERMISSION_REQUEST_IMAGE_PICKER_READ_EXTERNAL_STORAGE)) {
            // get system picker for images
            Intent pickImageIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickImageIntent, TransistorKeys.REQUEST_LOAD_IMAGE);
        }
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
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(mCollectionChangedReceiver, collectionChangedIntentFilter);

        // RECEIVER: station metadata has changed
        mMetadataChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mPlayback && intent.hasExtra(TransistorKeys.EXTRA_METADATA)) {
                    mStationMetadata = intent.getStringExtra(TransistorKeys.EXTRA_METADATA);
                    mStationMetadataView.setText(mStationMetadata);
                }
            }
        };
        IntentFilter metadataChangedIntentFilter = new IntentFilter(TransistorKeys.ACTION_METADATA_CHANGED);
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(mMetadataChangedReceiver, metadataChangedIntentFilter);

    }


    /* Unregisters broadcast receivers */
    private void unregisterBroadcastReceivers() {
        LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(mPlaybackStateChangedReceiver);
        LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(mCollectionChangedReceiver);
        LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(mMetadataChangedReceiver);
    }


    /* Handles changes in state of playback, eg. start, stop, loading stream */
    private void handlePlaybackStateChanges(Intent intent) {

        // get station from intent
        Station station = intent.getParcelableExtra(TransistorKeys.EXTRA_STATION);

        switch (intent.getIntExtra(TransistorKeys.EXTRA_PLAYBACK_STATE_CHANGE, 1)) {

            // CASE: player is preparing stream
            case TransistorKeys.PLAYBACK_LOADING_STATION:
                if (mVisibility && !mPlayback && mStation.getStreamUri().equals(station.getStreamUri())) {
                    // set playback true
                    mPlayback = true;
                    mStation.setPlaybackState(true);
                    // rotate playback button
                    changeVisualState(mActivity);
                    // update playback state
                    mStationIDLast = mStationIDCurrent;
                    mStationIDCurrent = mStationID;
                    // save state of playback to settings
                    saveAppState(mActivity);
                }
                break;

            // CASE: playback has started
            case TransistorKeys.PLAYBACK_STARTED:
                if (mVisibility && mPlayback && mStation != null && mStation.getStreamUri().equals(station.getStreamUri())) {
                    // update loading status and playback indicator
                    mStationLoading = false;
                    mPlaybackIndicator.setBackgroundResource(R.drawable.ic_playback_indicator_started_24dp);
                    // set metadata
                    if (mStationMetadata != null) {
                        mStationMetadataView.setText(mStationMetadata);
                    } else {
                        mStationMetadataView.setText(mStationName);
                    }
                    // save state of playback to settings
                    saveAppState(mActivity);
                }
                break;

            // CASE: playback was stopped
            case TransistorKeys.PLAYBACK_STOPPED:
                if (mVisibility && mPlayback && mPlayback && mStation != null && mStation.getStreamUri().equals(station.getStreamUri())) {
                    // set playback false
                    mPlayback = false;
                    // rotate playback button
                    if (intent.hasExtra(TransistorKeys.EXTRA_STATION_ID) && mStationID == intent.getIntExtra(TransistorKeys.EXTRA_STATION_ID, -1)) {
                        mStation.setPlaybackState(false);
                        changeVisualState(mActivity);
                    }
                    // save state of playback to settings
                    saveAppState(mActivity);
                }
                break;
        }
    }


    /* Handles adding, deleting and renaming of station */
    private void handleCollectionChanges(Intent intent) {
        switch (intent.getIntExtra(TransistorKeys.EXTRA_COLLECTION_CHANGE, 1)) {
            // CASE: station was renamed
            case TransistorKeys.STATION_RENAMED:
                if (intent.hasExtra(TransistorKeys.EXTRA_STATION)) {
                    Station station = intent.getParcelableExtra(TransistorKeys.EXTRA_STATION);
                    mStationNameView.setText(station.getStationName());
                }
                break;

            // CASE: station was deleted
            case TransistorKeys.STATION_DELETED:
                if (!mTwoPane && mVisibility) {
                    // start main activity
                    Intent mainActivityStartIntent = new Intent(mActivity, MainActivity.class);
                    startActivity(mainActivityStartIntent);
                    // finish player activity
                    mActivity.finish();
                }
                // two pane behaviour is handles by the adapter
                break;
        }
    }

}