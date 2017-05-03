/**
 * PlayerActivityFragment.java
 * Implements the main fragment of the player activity
 * This fragment is a detail view with the ability to start and stop playback
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
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.core.ImagePipeline;

import org.y20k.transistor.core.Station;
import org.y20k.transistor.helpers.DialogDelete;
import org.y20k.transistor.helpers.DialogRename;
import org.y20k.transistor.helpers.ImageHelper;
import org.y20k.transistor.helpers.LogHelper;
import org.y20k.transistor.helpers.NotificationHelper;
import org.y20k.transistor.helpers.PermissionHelper;
import org.y20k.transistor.helpers.PlaybackStatus;
import org.y20k.transistor.helpers.ShortcutHelper;
import org.y20k.transistor.helpers.SingletonProperties;
import org.y20k.transistor.helpers.StorageHelper;
import org.y20k.transistor.helpers.TransistorKeys;
import org.y20k.transistor.sqlcore.StationsDbHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


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
    private RatingBar mRatingBarView;
    private TextView mStationMetadataView;
    private TextView mtxtDescriptionView;
    private SimpleDraweeView mStationImageView;
    private ImageButton mStationMenuView;
    private ImageView mPlaybackIndicator;
    private ImageButton mPlaybackButton;
    private BroadcastReceiver mPlaybackStateChangedReceiver;
    private BroadcastReceiver mCollectionChangedReceiver;
    private BroadcastReceiver mMetadataChangedReceiver;
    private int mStationID_Position;
    private boolean mTwoPane;
    private boolean mVisibility;
    private Station mStation;
    private int mStationRating;
    private String mStationDescription;
    private String mStationHtmlDesciption;
    private WebView mWbStationWebViewView;
    private CardView mCrdHtmlDescriptionView;
    private RelativeLayout mRelLayLargeButtonPlayView;

    /* Constructor (default) */
    public PlayerActivityFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        // set fragment visibility
        mVisibility = false;

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
                mStationID_Position = arguments.getInt(TransistorKeys.ARG_STATION_ID);
                arguments.remove(TransistorKeys.ARG_STATION_ID);
            } else {
                mStationID_Position = 0;
                LogHelper.e(LOG_TAG, "Error: did not receive id of station. Choosing default ID for station");
            }

            // get station name and Uri
            if (mStation != null) {
                mStationName = mStation.TITLE;
                mStationDescription = mStation.DESCRIPTION;
                mStationHtmlDesciption = mStation.HtmlDescription;
                mStationRating = mStation.RATING;
                mStreamUri = mStation.getStreamUri().toString();
            } else {
                mStationName = mActivity.getString(R.string.descr_station_name_example);
                LogHelper.e(LOG_TAG, "Error: did not receive station. Displaying default station name");
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

    private void findViewsAndAssign() {
        // find views for station name and image and playback indicator
        mStationNameView = (TextView) mRootView.findViewById(R.id.player_textview_stationname);
        mtxtDescriptionView = (TextView) mRootView.findViewById(R.id.txtDescription);
        mRatingBarView = (RatingBar) mRootView.findViewById(R.id.ratingBar);

        mStationMetadataView = (TextView) mRootView.findViewById(R.id.player_textview_station_metadata);
        mPlaybackIndicator = (ImageView) mRootView.findViewById(R.id.player_playback_indicator);
        mPlaybackButton = (ImageButton) mRootView.findViewById(R.id.player_playback_button);

        mStationImageView = (SimpleDraweeView) mRootView.findViewById(R.id.player_imageview_station_icon);
        mStationMenuView = (ImageButton) mRootView.findViewById(R.id.player_item_more_button);
        mWbStationWebViewView = (WebView) mRootView.findViewById(R.id.wbStationWebView);
        mCrdHtmlDescriptionView = (CardView) mRootView.findViewById(R.id.crdHtmlDescription);
        mRelLayLargeButtonPlayView = (RelativeLayout) mRootView.findViewById(R.id.relLayLargeButtonPlay);
    }

    @SuppressWarnings("deprecation")
    public Spanned fromHtml(String source) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(source);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {

        // inflate root view from xml
        mRootView = inflater.inflate(R.layout.fragment_player, container, false);

        findViewsAndAssign();

        // get data from arguments
        Bundle arguments = getArguments();
        if (arguments.containsKey(TransistorKeys.ARG_PLAYBACK)) {
            Boolean mARG_PLAYBACK = arguments.getBoolean(TransistorKeys.ARG_PLAYBACK, false);
            if (mARG_PLAYBACK) {
                // simulate handle tap on big playback button
                handlePlaybackButtonClick();
            }
        }

        //set round icon (special code for Fresco SimpleDraweeView)
        int color = ContextCompat.getColor(mActivity, R.color.colorPrimary);
        RoundingParams roundingParams = RoundingParams.fromCornersRadius(5f);
        roundingParams.setBorder(color, 1.0f);
        roundingParams.setRoundAsCircle(true);
        mStationImageView.getHierarchy().setRoundingParams(roundingParams);


        // set station name
        mStationNameView.setText(mStationName);
        mtxtDescriptionView.setText(mStationDescription);

        //set HTML in webview if mStationHtmlDesciption != ""
        if (mStationHtmlDesciption != null && !mStationHtmlDesciption.isEmpty()) {
            //display HTML inside web view control
            final String mimeType = "text/html";
            final String encoding = "UTF-8";
            //Load the file from assets folder - don't forget to INCLUDE the extension
            String webStyleDefaults = "";
            try {
                webStyleDefaults = LoadFile("webViewStyleDefaults.html");
            } catch (IOException e) {
                e.printStackTrace();
            }
            mWbStationWebViewView.loadDataWithBaseURL("", webStyleDefaults + mStationHtmlDesciption, mimeType, encoding, "");
        } else {
            //hide web view if no HTML description
            mCrdHtmlDescriptionView.setVisibility(View.GONE);
        }
        mRatingBarView.setRating(mStationRating);
        mRatingBarView.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                //change rating in DB
                //update DB
                StationsDbHelper mDbHelper = new StationsDbHelper(mActivity);
                int result = mDbHelper.ChangeRatingOfStation(mStation._ID, Math.round(rating));
                mStation.RATING = Math.round(rating);

                if (result > 0) {
                    Intent i = new Intent();
                    i.setAction(TransistorKeys.ACTION_COLLECTION_CHANGED);
                    i.putExtra(TransistorKeys.EXTRA_COLLECTION_CHANGE, TransistorKeys.STATION_CHANGED_RATING);
                    i.putExtra(TransistorKeys.EXTRA_STATION, mStation);
                    i.putExtra(TransistorKeys.EXTRA_STATION_DB_ID, mStation._ID);
                    LocalBroadcastManager.getInstance(mActivity.getApplicationContext()).sendBroadcast(i);
                }
            }
        });

        // set station image
        setRefreshStationImage();

        // add listener to station info view for clipboard copy
        View stationInfoView = mRootView.findViewById(R.id.player_layout_station_info);
        stationInfoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyStationToClipboard();
            }
        });

        // show metadata
        //remove unneeded mPlayback , and replace it SingletonProperties.getInstance().getIsPlayback()
        if ( SingletonProperties.getInstance().getIsPlayback() && mStationMetadata != null) {
            mStationMetadataView.setText(mStationMetadata);
            mStationMetadataView.setVisibility(View.VISIBLE);
            mStationMetadataView.setSelected(true);
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
            //disable the big play button on tablet view
            mRelLayLargeButtonPlayView.setVisibility(View.VISIBLE);
        } else {
            //disable the big play button on mobile view
            mRelLayLargeButtonPlayView.setVisibility(View.GONE);
        }

        // construct big playback button
        mPlaybackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // handle tap on big playback button
                handlePlaybackButtonClick();
            }
        });

        return mRootView;
    }

    //load file from apps res/raw folder or Assets folder
    public String LoadFile(String fileName) throws IOException {
        //Create a InputStream to read the file into
        InputStream iS;
        Resources resources = getResources();

        //get the file as a stream
        iS = resources.getAssets().open(fileName);

        //create a buffer that has the same size as the InputStream
        byte[] buffer = new byte[iS.available()];
        //read the text file as a stream, into the buffer
        iS.read(buffer);
        //create a output stream to write the buffer into
        ByteArrayOutputStream oS = new ByteArrayOutputStream();
        //write this buffer to the output stream
        oS.write(buffer);
        //Close the Input and Output streams
        oS.close();
        iS.close();

        //return the output stream as a String
        return oS.toString();
    }

    private void setRefreshStationImage() {
        File stationSmallImageFile = mStation.getStationSmallImage(mActivity);
        if (stationSmallImageFile != null && stationSmallImageFile.exists()) {
            mStationImageView.setImageURI(stationSmallImageFile.toURI().toString());//.setImageBitmap(stationImageSmall);
        } else if (mStation.SMALL_IMAGE_PATH != null && !mStation.SMALL_IMAGE_PATH.isEmpty()) {
            mStationImageView.setImageURI(mStation.SMALL_IMAGE_PATH);//.setImageBitmap(stationImageSmall);
        }
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
            if (currentStation != null && currentStation.getStreamUri().equals(mStation.getStreamUri()) && SingletonProperties.getInstance().getIsPlayback() && (SingletonProperties.getInstance().CurrentSelectedStation_Playback_Status != PlaybackStatus.LOADING)) {
                LogHelper.v(LOG_TAG, "Try to start playback from shortcut, but station is already running.");
            } else {
                // hide metadata
                mStationMetadata = null;
                mStationMetadataView.setVisibility(View.GONE);
                // start playback
                startPlayback();
                // clear playback state argument
                getArguments().putBoolean(TransistorKeys.ARG_PLAYBACK, false);
                LogHelper.v(LOG_TAG, "Staring playback from shortcut.");
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
        Uri newImageUri = null;
        if (null != data) {
            newImageUri = data.getData();
        }

        if (requestCode == TransistorKeys.REQUEST_LOAD_IMAGE && resultCode == Activity.RESULT_OK && null != data) {

            ImageHelper imageHelper = new ImageHelper(newImageUri, mActivity, 500, 500);
            Bitmap newImage = imageHelper.getInputImage();

            if (newImage != null) {
                // write image to storage
                StorageHelper storageHelper = new StorageHelper(mActivity);
                File mFolder = storageHelper.getCollectionDirectory();
                File stationImageFile = mStation.getStationImageFileReference(mFolder);//get  station file with correct path according to UniqueID of the station
                boolean changeImageSuccessfully = false;
                try (FileOutputStream out = new FileOutputStream(stationImageFile)) {
                    newImage.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.close();

                    //remve image from fresco cache
                    ImagePipeline imagePipeline = Fresco.getImagePipeline();
                    imagePipeline.evictFromCache(Uri.parse(stationImageFile.toURI().toString()));

                    changeImageSuccessfully = true;
                } catch (IOException e) {
                    LogHelper.e(LOG_TAG, "Unable to save: " + newImage.toString());
                }

                // send local broadcast
                if (changeImageSuccessfully) {
                    Intent i = new Intent();
                    i.setAction(TransistorKeys.ACTION_COLLECTION_CHANGED);
                    i.putExtra(TransistorKeys.EXTRA_COLLECTION_CHANGE, TransistorKeys.STATION_CHANGED_IMAGE);
                    i.putExtra(TransistorKeys.EXTRA_STATION, mStation);
                    i.putExtra(TransistorKeys.EXTRA_STATION_DB_ID, mStation._ID);
                    LocalBroadcastManager.getInstance(mActivity.getApplicationContext()).sendBroadcast(i);
                }

                Toast.makeText(mActivity, "Image Updated", Toast.LENGTH_SHORT).show();
            } else {
                LogHelper.e(LOG_TAG, "Unable to get image from media picker: " + newImageUri.toString());
            }

        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // save current station
        outState.putParcelable(TransistorKeys.INSTANCE_STATION, mStation);
        outState.putInt(TransistorKeys.INSTANCE_STATION_ID, mStationID_Position);
        //remove unneeded mPlayback , and replace it SingletonProperties.getInstance().getIsPlayback()
        outState.putBoolean(TransistorKeys.INSTANCE_PLAYBACK,SingletonProperties.getInstance().getIsPlayback());
    }


    /* Starts player service */
    private void startPlayback() {
        // rotate_infinite playback button
        changeVisualState(mActivity);

        // start player service using intent
        Intent intent = new Intent(mActivity, PlayerService.class);
        intent.setAction(TransistorKeys.ACTION_PLAY);
        intent.putExtra(TransistorKeys.EXTRA_STATION, mStation);
        intent.putExtra(TransistorKeys.EXTRA_STATION_Position_ID, mStationID_Position);
        mActivity.startService(intent);
        LogHelper.v(LOG_TAG, "Starting player service.");
    }


    /* Stops player service */
    private void stopPlayback() {
        // reset metadata
        mStationMetadata = null;

        // rotate_infinite playback button
        changeVisualState(mActivity);

        // stop player service using intent
        Intent intent = new Intent(mActivity, PlayerService.class);
        intent.setAction(TransistorKeys.ACTION_STOP);
        mActivity.startService(intent);
        LogHelper.v(LOG_TAG, "Stopping player service.");
    }


    /* Handles tap on the big playback button */
    public void handlePlaybackButtonClick() {
        // playback stopped or new station - start playback
        if (!SingletonProperties.getInstance().getIsPlayback() || !mStation.getPlaybackState()) {
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
            case R.id.menu_change_photo:
                // get system picker for images
                selectFromImagePicker();
                return true;

            // CASE RENAME
            case R.id.menu_rename:
                // construct and run rename dialog
                final DialogRename dialogRename = new DialogRename(mActivity, mStation, mStationID_Position);
                dialogRename.show();
                return true;

            // CASE DELETE
            case R.id.menu_delete:
                // stop player service using intent
                // construct and run delete dialog
                DialogDelete dialogDelete = new DialogDelete(mActivity, mStation, mStationID_Position);
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

    /* Copy station date to system clipboard */
    private void copyStationToClipboard() {
        if (mStreamUri != null && mStationName != null) {

            // create clip
            String clipboardText;
            ClipData clip;
            if (mStationMetadata != null) {
                clipboardText = mStationName + " - " + mStationMetadata + " (" + mStreamUri + ")";
            } else {
                clipboardText = mStationName + " (" + mStreamUri + ")";
            }
            clip = ClipData.newPlainText("simple text", clipboardText);

            // copy clip to clipboard
            ClipboardManager cm = (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(clip);

            // notify user
            Toast.makeText(mActivity, mActivity.getString(R.string.toastmessage_station_copied), Toast.LENGTH_SHORT).show();
        }
    }


    /* Animate button and then set visual state */
    private void changeVisualState(Context context) {
        setVisualState();
        if (!mTwoPane) {
            return;
        }
        // get rotate_infinite animation from xml
        Animation rotate;
        //remove unneeded mPlayback , and replace it SingletonProperties.getInstance().getIsPlayback()
        if (SingletonProperties.getInstance().getIsPlayback()) {
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
        if (mPlaybackButton != null)
            mPlaybackButton.startAnimation(rotate);

    }


    /* Set button symbol and playback indicator */
    private void setVisualState() {
        // this station is running
        if ( SingletonProperties.getInstance().getIsPlayback()  && mStation != null && mStation.getPlaybackState()) {
            // change playback button image to stop
            mPlaybackButton.setImageResource(R.drawable.smbl_stop);
            // change playback indicator
            if (SingletonProperties.getInstance().CurrentSelectedStation_Playback_Status == PlaybackStatus.LOADING) {
                mPlaybackIndicator.setBackgroundResource(R.drawable.ic_playback_indicator_loading_24dp);
                mStationMetadataView.setText(R.string.descr_station_stream_loading);
            } else {
                mPlaybackIndicator.setBackgroundResource(R.drawable.ic_playback_indicator_started_24dp);
            }
            // show metadata view
            mStationMetadataView.setVisibility(View.VISIBLE);
            mStationMetadataView.setSelected(true);
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

    /* Loads app state from preferences */
    private void loadAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        mStationID_Position = settings.getInt(TransistorKeys.PREF_STATION_ID_SELECTED, 0);
        mStationMetadata = settings.getString(TransistorKeys.PREF_STATION_METADATA, null);
        LogHelper.v(LOG_TAG, "Loading state (" + SingletonProperties.getInstance().CurrentSelectedStation_ID + " / " + SingletonProperties.getInstance().getLastRunningStation_ID() + " / " +SingletonProperties.getInstance().getIsPlayback()  + " / " + (SingletonProperties.getInstance().CurrentSelectedStation_Playback_Status == PlaybackStatus.LOADING) + " / " + mStationMetadata + ")");
    }


    /* Check permissions and start image picker */
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
                //remove unneeded mPlayback , and replace it SingletonProperties.getInstance().getIsPlayback()
                if ( SingletonProperties.getInstance().getIsPlayback() && intent.hasExtra(TransistorKeys.EXTRA_METADATA)) {
                    mStationMetadata = intent.getStringExtra(TransistorKeys.EXTRA_METADATA);
                    mStationMetadataView.setText(mStationMetadata);
                    mStationMetadataView.setSelected(true);
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
                if (mVisibility  && mStation != null && mStation.getStreamUri().equals(station.getStreamUri())) {
                    changeVisualState(mActivity);
                }
                break;

            // CASE: playback has started
            case TransistorKeys.PLAYBACK_STARTED:
                //remove unneeded mPlayback , and replace it SingletonProperties.getInstance().getIsPlayback()
                if (mVisibility && SingletonProperties.getInstance().getIsPlayback() && mStation != null && mStation.getStreamUri().equals(station.getStreamUri())) {
                    loadAppState(mActivity);
                    // update loading status and playback indicator
                    mPlaybackIndicator.setBackgroundResource(R.drawable.ic_playback_indicator_started_24dp);
                    // set metadata
                    if (mStationMetadata != null) {
                        mStationMetadataView.setText(mStationMetadata);
                    } else {
                        mStationMetadataView.setText(mStationName);
                    }
                    mStationMetadataView.setSelected(true);
                }

                if (mVisibility  && mStation != null && mStation.getStreamUri().equals(station.getStreamUri())) {
                    changeVisualState(mActivity);
                }
                break;

            // CASE: playback was stopped
            case TransistorKeys.PLAYBACK_STOPPED:
                // rotate_infinite playback button
                if (mVisibility  && mStation != null && mStation.getStreamUri().equals(station.getStreamUri())) {
                    changeVisualState(mActivity);
                }
                break;
        }
    }


    /* Handles adding, deleting and renaming of station */
    private void handleCollectionChanges(Intent intent) {
        switch (intent.getIntExtra(TransistorKeys.EXTRA_COLLECTION_CHANGE, 1)) {
            // CASE: station was renamed
            case TransistorKeys.STATION_RENAMED:
                if (intent.hasExtra(TransistorKeys.EXTRA_STATION_NEW_NAME) && intent.hasExtra(TransistorKeys.EXTRA_STATION) && intent.hasExtra(TransistorKeys.EXTRA_STATION_Position_ID)) {
                    Station station = intent.getParcelableExtra(TransistorKeys.EXTRA_STATION);
                    int stationID_Position = intent.getIntExtra(TransistorKeys.EXTRA_STATION_Position_ID, 0);
                    mStationNameView.setText(station.TITLE);
                    if (SingletonProperties.getInstance().getIsPlayback()) {
                        NotificationHelper.update(station, stationID_Position, null, null);
                    }
                }
                break;

            // CASE: station was deleted
            case TransistorKeys.STATION_DELETED:
                if (SingletonProperties.getInstance().getIsPlayback()) {
                    // stop player service and notification using intent
                    Intent i = new Intent(mActivity, PlayerService.class);
                    i.setAction(TransistorKeys.ACTION_DISMISS);
                    mActivity.startService(i);
                    LogHelper.v(LOG_TAG, "Stopping player service.");
                }

                if (!mTwoPane && mVisibility) {
                    // start main activity
                    Intent mainActivityStartIntent = new Intent(mActivity, MainActivity.class);
                    startActivity(mainActivityStartIntent);
                }
                // two pane behaviour is handles by the adapter
                break;
            case TransistorKeys.STATION_CHANGED_IMAGE:
                if (intent.hasExtra(TransistorKeys.EXTRA_STATION) && intent.hasExtra(TransistorKeys.EXTRA_STATION_DB_ID)) {
                    //update image
                    // get new name, station and station ID from intent
                    Station station = intent.getParcelableExtra(TransistorKeys.EXTRA_STATION);
                    int stationID = intent.getIntExtra(TransistorKeys.EXTRA_STATION_DB_ID, 0);

                    // set station image
                    setRefreshStationImage();
                }
                break;
        }
    }

}