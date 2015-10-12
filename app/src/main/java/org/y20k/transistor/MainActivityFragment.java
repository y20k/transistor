/**
 * MainActivityFragment.java
 * Implements the main fragment of the main activity
 * This fragment is a list view of radio stations
 * <p/>
 * This file is part of
 * TRANSISTOR - Radio App for Android
 * <p/>
 * Copyright (c) 2015 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

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
    public final String LOG_TAG = MainActivityFragment.class.getSimpleName();

    /* Main class variables */
    private Collection mCollection;
    private CollectionAdapter mCollectionAdapter = null;
    private File mFolder;
    private LinkedList<String> mStationNames;
    private LinkedList<Bitmap> mStationImages;
    private ListView mListView;
    private Parcelable mListState;

    private int mStationIDCurrent;
    private int mStationIDLast;
    private boolean mPlayback;

    /* Keys */
    public static final String LIST_STATE = "ListState";
    public static final String STREAM_URL = "streamURL";
    public static final String STATION_NAME = "stationName";
    public static final String STATION_ID = "stationID";
    public static final String STATION_ID_CURRENT = "stationIDCurrent";
    public static final String STATION_ID_LAST = "stationIDLast";
    public static final String PLAYBACK = "playback";

    public static final String TITLE = "title";
    public static final String CONTENT = "content";


    /* Constructor (default) */
    public MainActivityFragment() {
    }

    @Override
    public void onResume() {
        super.onResume();

        // let Transistor react to external clicks on m3u links
        Intent intent = getActivity().getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            String newStationURL;
            // mime type check
            if (intent.getType() != null && intent.getType().startsWith("audio/")) {
                newStationURL = intent.getDataString();
            }
            // no mime type
            else {
                newStationURL = intent.getDataString();
            }
            // check for null
            if (newStationURL != null) {
                // add new station
                StationHelper stationHelper = new StationHelper(getActivity());
                stationHelper.setStationChangedListener(new StationHelper.StationChangedListener() {
                    @Override
                    public void stationChanged() {
                        // clear and refill mCollection adapter
                        mStationNames.clear();
                        mStationImages.clear();
                        fillCollectionAdapter();
                    }
                });
                stationHelper.add(newStationURL);
            }
            // unsuccessful - notify user
            else {
                System.out.println("!!! Empty Intent");
            }

        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set up variables
        mListState = null;
        mStationIDCurrent = -1;
        mStationIDLast = -1;
        mPlayback = false;
        mFolder = new File(getActivity().getExternalFilesDir("Collection").toString());

        // get list state from saved instance
        if (savedInstanceState != null) {
            mListState = savedInstanceState.getParcelable(MainActivityFragment.LIST_STATE);
        }

        // restore player state from preferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mStationIDCurrent = settings.getInt(STATION_ID_CURRENT, -1);
        mStationIDLast = settings.getInt(STATION_ID_LAST, -1);
        mPlayback = settings.getBoolean(PLAYBACK, false);

        // fragment has options menu
        setHasOptionsMenu(true);


//        // let Transistor react to external clicks on m3u links
//        if (Intent.ACTION_VIEW.equals(action) && type.startsWith("audio/")) {
//            String newStationURL = intent.getDataString();
//            StationHelper stationHelper = new StationHelper(getActivity());
//            stationHelper.setStationChangedListener(new StationHelper.StationChangedListener() {
//                @Override
//                public void stationChanged() {
//                    // clear and refill mCollection adapter
//                    mStationNames.clear();
//                    mStationImages.clear();
//                    fillCollectionAdapter();
//                }
//            });
//            stationHelper.add(newStationURL);
//        }





        // create adapter for collection
        mStationNames = new LinkedList<String>();
        mStationImages = new LinkedList<Bitmap>();
        mCollectionAdapter = new CollectionAdapter(getActivity(), mStationNames, mStationImages);

        // listen for data change in mCollection adapter
        mCollectionAdapter.setCollectionChangedListener(new CollectionAdapter.CollectionChangedListener() {
            @Override
            public void collectionChanged() {
                // clear and refill mCollection adapter
                mStationNames.clear();
                mStationImages.clear();
                fillCollectionAdapter();
            }
        });
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // get list state from saved instance
        if (savedInstanceState != null) {
            mListState = savedInstanceState.getParcelable(MainActivityFragment.LIST_STATE);
        }

        // fetch radio stations and fill mCollection adapter
        fillCollectionAdapter();

        // inflate rootview from xml
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // get reference to list view from inflated rootview
        mListView = (ListView) rootView.findViewById(R.id.listview_collection);

        // attach adapter to list view
        mListView.setAdapter(mCollectionAdapter);

        // attach OnItemClickListener to mListView
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            // inner method override for OnItemClickListener
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // get station name and URL from position
                String stationName = mCollection.getStations().get((Integer) mCollectionAdapter.getItem(position)).getStationName();
                String streamURL = mCollection.getStations().get((Integer) mCollectionAdapter.getItem(position)).getStreamURL().toString();

                // add name, url and id of station to intent
                Intent intent = new Intent(getActivity(), PlayerActivity.class);
                intent.putExtra(STATION_NAME, stationName);
                intent.putExtra(STREAM_URL, streamURL);
                intent.putExtra(STATION_ID, position);

//                // save player state to preferences
//                if (position != mStationIDCurrent && mPlayback == true) {
//                    mStationIDLast = mStationIDCurrent;
//                    mStationIDCurrent = position;
//                    mPlayback = false;
//                    System.out.println("!!! (2) Click & Current & Last: " +  position + " / " + mStationIDCurrent + " / " + mStationIDLast );
//
//                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
//                    SharedPreferences.Editor editor = settings.edit();
//                    editor.putInt(STATION_ID_CURRENT, mStationIDCurrent);
//                    editor.putInt(STATION_ID_LAST, mStationIDLast);
//                    editor.putBoolean(PLAYBACK, mPlayback);
//                    editor.commit();
//                }

                // start activity with intent
                startActivity(intent);

            }
        });

        // return list view
        return rootView;
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
                    // clear and refill mCollection adapter
                    mStationNames.clear();
                    mStationImages.clear();
                    fillCollectionAdapter();
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


    private void fillCollectionAdapter() {

        Bitmap stationImage = null;
        Bitmap stationImageSmall = null;
        String stationName = null;
        ImageHelper stationImageHelper = null;

        // create collection
        Log.v(LOG_TAG, "Create mCollection of stations in background (folder:" + mFolder.toString() + ").");
        mCollection = new Collection(mFolder);


        // put stations into collection adapter
        for (int i = 0; i < mCollection.getStations().size(); i++) {
            // set name of station
            stationName = mCollection.getStations().get(i).getStationName();
            mStationNames.add(stationName);

            // set image for station
            if (mCollection.getStations().get(i).getStationImageFile().exists()) {
                // station image
                stationImageSmall = BitmapFactory.decodeFile(mCollection.getStations().get(i).getStationImageFile().toString());
            } else {
                // default image
                stationImageSmall = BitmapFactory.decodeResource(getResources(), R.drawable.ic_notesymbol);
            }

            stationImageHelper = new ImageHelper(stationImageSmall, getActivity());
            stationImageHelper.setBackgroundColor(R.color.transistor_grey_lighter);
            stationImage = stationImageHelper.createCircularFramedImage(192);
            mStationImages.add(stationImage);
        }
        mCollectionAdapter.setCollection(mCollection);
        mCollectionAdapter.notifyDataSetChanged();

//        // restore scrolling state
//        if (mListState != null) {
//            mListView.onRestoreInstanceState(mListState);
//        }
    }

}
