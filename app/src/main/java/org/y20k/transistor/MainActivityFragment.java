/**
 * MainActivityFragment.java
 * Implements the main fragment of the main activity
 * This fragment is a list view of radio stations
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.y20k.transistor.core.Collection;
import org.y20k.transistor.helpers.CollectionAdapter;
import org.y20k.transistor.helpers.DialogAddStation;
import org.y20k.transistor.helpers.ImageHelper;
import org.y20k.transistor.helpers.StationHelper;

import java.io.File;
import java.util.LinkedList;


/**
 * MainActivityFragment class
 */
public class MainActivityFragment extends Fragment {

    /* Define log tag */
    private static final String LOG_TAG = MainActivityFragment.class.getSimpleName();


    /* Keys */
    private static final String ACTION_PLAYBACK_STARTED = "org.y20k.transistor.action.PLAYBACK_STARTED";
    private static final String ACTION_PLAYBACK_STOPPED = "org.y20k.transistor.action.PLAYBACK_STOPPED";
    private static final String LIST_STATE = "ListState";
    private static final String STREAM_URL = "streamURL";
    private static final String STATION_NAME = "stationName";
    private static final String STATION_ID = "stationID";
    private static final String TITLE = "title";
    private static final String CONTENT = "content";


    /* Main class variables */
    private Collection mCollection;
    private CollectionAdapter mCollectionAdapter = null;
    private File mFolder;
    private LinkedList<String> mStationNames;
    private LinkedList<Bitmap> mStationImages;
    private View mRootView;
    private ListView mListView;
    private Parcelable mListState;


    /* Constructor (default) */
    public MainActivityFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set list state null
        mListState = null;

        try {
            // get collection folder from external storage
            mFolder = new File(getActivity().getExternalFilesDir("Collection").toString());
        } catch (NullPointerException e) {
            // notify user and log exception
            Toast.makeText(getActivity(), R.string.toastalert_no_external_storage, Toast.LENGTH_LONG).show();
            Log.e(LOG_TAG, "Unable to access external storage.");
            // finish activity
            getActivity().finish();
        }

        // fragment has options menu
        setHasOptionsMenu(true);

        // create adapter for collection
        mStationNames = new LinkedList<>();
        mStationImages = new LinkedList<>();
        mCollectionAdapter = new CollectionAdapter(getActivity(), mStationNames, mStationImages);

        // listen for data change in mCollection adapter
        mCollectionAdapter.setCollectionChangedListener(new CollectionAdapter.CollectionChangedListener() {
            @Override
            public void collectionChanged() {
                refreshStationList(getActivity());
            }
        });

        // broadcast receiver: player service stopped playback
        BroadcastReceiver playbackStoppedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                refreshStationList(context);
            }
        };
        IntentFilter playbackStoppedIntentFilter = new IntentFilter(ACTION_PLAYBACK_STOPPED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(playbackStoppedReceiver, playbackStoppedIntentFilter);

        // broadcast receiver: player service stopped playback
        BroadcastReceiver playbackStartedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                refreshStationList(context);
            }
        };
        IntentFilter playbackStartedIntentFilter = new IntentFilter(ACTION_PLAYBACK_STARTED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(playbackStartedReceiver, playbackStartedIntentFilter);
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
        mListView = (ListView) mRootView.findViewById(R.id.main_listview_collection);

        // attach adapter to list view
        mListView.setAdapter(mCollectionAdapter);

        // attach OnItemClickListener to mListView
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            // inner method override for OnItemClickListener
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mCollection != null) {
                    // get station name and URL from position
                    String stationName = mCollection.getStations().get((Integer) mCollectionAdapter.getItem(position)).getStationName();
                    String streamURL = mCollection.getStations().get((Integer) mCollectionAdapter.getItem(position)).getStreamURL().toString();

                    // add name, url and id of station to intent
                    Intent intent = new Intent(getActivity(), PlayerActivity.class);
                    intent.putExtra(STATION_NAME, stationName);
                    intent.putExtra(STREAM_URL, streamURL);
                    intent.putExtra(STATION_ID, position);

                    // start activity with intent
                    startActivity(intent);
                }
            }
        });

        // return list view
        return mRootView;
    }


    @Override
    public void onResume() {
        super.onResume();

        // handle incoming intent
        handleNewStationIntent();
    }


    @Override
    public void onStart() {
        super.onStart();

        // fill collection adapter with stations
        refreshStationList(getActivity());
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // action bar - add
        if (id == R.id.menu_add) {
            DialogAddStation dialog = new DialogAddStation();
            dialog.show(getFragmentManager(), "addstation");
            dialog.setCollectionChangedListener(new DialogAddStation.CollectionChangedListener() {
                @Override
                public void collectionChanged() {
                    refreshStationList(getActivity());
                }
            });
            return true;
        }

        // action bar menu - about
        else if (id == R.id.menu_about) {
            // get title and content
            String title = getActivity().getString(R.string.header_about);
            String content = getActivity().getString(R.string.html_about);

            // create intent
            Intent intent = new Intent(getActivity(), InfosheetActivity.class);

            // put title and content to intent
            intent.putExtra(TITLE, title);
            intent.putExtra(CONTENT, content);

            // start activity
            startActivity(intent);
            return true;
        }

        // action bar menu - how to
        else if (id == R.id.menu_howto) {
            // get title and content
            String title = getActivity().getString(R.string.header_howto);
            String content = getActivity().getString(R.string.html_howto);

            // create intent
            Intent intent = new Intent(getActivity(), InfosheetActivity.class);

            // put title and content to intent
            intent.putExtra(TITLE, title);
            intent.putExtra(CONTENT, content);

            // start activity
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // save list view position
        mListState = mListView.onSaveInstanceState();
        outState.putParcelable(LIST_STATE, mListState);
    }


    /* Fills collection adapter */
    private void fillCollectionAdapter(Context context) {

        Bitmap stationImage;
        Bitmap stationImageSmall;
        String stationName;
        ImageHelper stationImageHelper;

        // create collection
        Log.v(LOG_TAG, "Create collection of stations (folder:" + mFolder.toString() + ").");
        mCollection = new Collection(mFolder);

        // put stations into collection adapter
        for (int i = 0; i < mCollection.getStations().size(); i++) {
            // set name of station
            stationName = mCollection.getStations().get(i).getStationName();
            // add name to linked list of names
            mStationNames.add(stationName);

            // set image for station
            if (mCollection.getStations().get(i).getStationImageFile().exists()) {
                // station image
                stationImageSmall = BitmapFactory.decodeFile(mCollection.getStations().get(i).getStationImageFile().toString());
            } else {
                // default image
                stationImageSmall = BitmapFactory.decodeResource(getResources(), R.drawable.ic_notesymbol);
            }
            stationImageHelper = new ImageHelper(stationImageSmall, context);
            stationImageHelper.setBackgroundColor(R.color.transistor_grey_lighter);
            stationImage = stationImageHelper.createCircularFramedImage(192);
            // add image to linked list of images
            mStationImages.add(stationImage);
        }
        mCollectionAdapter.setCollection(mCollection);
        mCollectionAdapter.notifyDataSetChanged();

    }


    /* (Re-)fills collection adapter with stations */
    private void refreshStationList(Context context) {

        // clear and refill mCollection adapter
        if (!mStationNames.isEmpty() && !mStationImages.isEmpty()) {
            mStationNames.clear();
            mStationImages.clear();
        }
        fillCollectionAdapter(context);

        // show call to action, if necessary
        View actioncall = mRootView.findViewById(R.id.main_actioncall_layout);
        if (mCollectionAdapter.isEmpty()) {
            actioncall.setVisibility(View.VISIBLE);
        } else {
            actioncall.setVisibility(View.GONE);
        }

    }


    /* handles external taps on streaming links */
    private void handleNewStationIntent() {

        // get intent
        Intent intent = getActivity().getIntent();

        // check for intent of tyoe VIEW
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {

            // set new station URL
            String newStationURL;
            // mime type check
            if (intent.getType() != null && intent.getType().startsWith("audio/")) {
                newStationURL = intent.getDataString();
            }
            // no mime type
            else {
                newStationURL = intent.getDataString();
            }

            // clear the intent
            intent.setAction("");

            // check for null
            if (newStationURL != null) {
                // add new station
                StationHelper stationHelper = new StationHelper(getActivity());
                stationHelper.setStationChangedListener(new StationHelper.StationChangedListener() {
                    @Override
                    public void stationChanged() {
                        refreshStationList(getActivity());
                    }
                });
                stationHelper.add(newStationURL);
            }
            // unsuccessful - log failure
            else {
                Log.v(LOG_TAG, "Received an empty intent");
            }

        }
    }

}
