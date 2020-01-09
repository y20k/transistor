/**
 * DialogAddChooseStream.java
 * Implements the DialogAddChooseStream class
 * A DialogAddChooseStream lets the user choose between multiple streams from a fetched playlist
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-20 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.ThemedSpinnerAdapter;

import org.y20k.transistor.R;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * DialogAddChooseStream class
 */
public final class DialogAddChooseStream implements TransistorKeys {

    /* Define log tag */
    private static final String LOG_TAG = DialogAddChooseStream.class.getSimpleName();


    /* Main class variables */
    private static int mStationSelectionId = 0;


    /* Construct and show dialog */
    public static void show(final Activity activity, final ArrayList<String> stationUrls, final ArrayList<String> stationNames) {
        // prepare dialog builder
        LayoutInflater inflater = LayoutInflater.from(activity);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        // set up data adapter for dropdown
        final DropdownAdapter adapter = new DropdownAdapter(activity, stationUrls, stationNames);
        View view = inflater.inflate(R.layout.dialog_add_choose_stream, null);

        // setup dropdown spinner
        final Spinner stationDropdown = (Spinner) view.findViewById(R.id.dialog_add_choose_stream_spinner);
        stationDropdown.setAdapter(adapter);
        stationDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mStationSelectionId = i;
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                mStationSelectionId = 0;
            }
        });

        // set dialog view
        builder.setView(view);

        // add okay button
        builder.setPositiveButton(R.string.dialog_add_station_choose_stream_button, new DialogInterface.OnClickListener() {
            // listen for click on delete button
            public void onClick(DialogInterface arg0, int arg1) {
                // get selection from spinner
                String selectionName = null;
                Uri selectionUri = null;
                if (stationNames.size() > mStationSelectionId) {
                    selectionName = stationNames.get(mStationSelectionId);
                }
                selectionUri = Uri.parse(stationUrls.get(mStationSelectionId));
                // add new station
                StationFetcher stationFetcher = new StationFetcher(activity, StorageHelper.getCollectionDirectory(activity), selectionUri, selectionName);
                stationFetcher.execute();
            }
        });
        // add cancel button
        builder.setNegativeButton(R.string.dialog_generic_button_cancel, new DialogInterface.OnClickListener() {
            // listen for click on cancel button
            public void onClick(DialogInterface arg0, int arg1) {
                // do nothing
            }
        });

        // display rename dialog
        builder.show();
    }


    /**
     * Inner class: Custom adapter for dropdown spinner
     */
    private static final class DropdownAdapter  extends BaseAdapter implements ThemedSpinnerAdapter {

        /* Main class variables */
        private final ThemedSpinnerAdapter.Helper mDropdownAdapterHelper;
        private ArrayList<HashMap<String,String>> mStations;


        public DropdownAdapter(final Activity activity, final ArrayList<String> stationUrls, final ArrayList<String> stationNames) {
            // fill list of stations
            initializeStationList(stationUrls, stationNames);
            // create an adapter helper
            mDropdownAdapterHelper = new ThemedSpinnerAdapter.Helper(activity);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // getView -> collapsed view of dropdown
            View view = convertView;
            if (view == null) {
                // get views
                LayoutInflater inflater = mDropdownAdapterHelper.getDropDownViewInflater();
                view = inflater.inflate(R.layout.list_item_select_station, parent, false);
                TextView stationNameView = view.findViewById(R.id.select_station_name);
                TextView stationUrlView = view.findViewById(R.id.select_station_url);
                // set text views
                stationNameView.setText(getItem(position).get(SELECT_STREAM_KEY_NAME));
                stationUrlView.setText(getItem(position).get(SELECT_STREAM_KEY_URL));
            }
            return view;
        }

        @Override
        public View getDropDownView(int i, View view, ViewGroup viewGroup) {
            // getDropDownView -> expanded view of dropdown
            View dropdownView = view;
            if (dropdownView == null) {
                // get views
                LayoutInflater inflater = mDropdownAdapterHelper.getDropDownViewInflater();
                dropdownView = inflater.inflate(R.layout.list_item_select_station, viewGroup, false);
                TextView stationNameView = dropdownView.findViewById(R.id.select_station_name);
                TextView stationUrlView = dropdownView.findViewById(R.id.select_station_url);
                // set text views and set them selected (to start the marquee effect)
                stationNameView.setText(getItem(i).get(SELECT_STREAM_KEY_NAME));
                stationNameView.setSelected(true);
                stationUrlView.setText(getItem(i).get(SELECT_STREAM_KEY_URL));
                stationUrlView.setSelected(true);
            }
            return dropdownView;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver dataSetObserver) {

        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {

        }

        @Override
        public int getCount() {
            return mStations.size();
        }

        @Override
        public HashMap<String, String> getItem(int i) {
            return mStations.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public int getItemViewType(int i) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return mStations.size() == 0;
        }

        @Override
        public void setDropDownViewTheme(@Nullable Resources.Theme theme) {
            mDropdownAdapterHelper.setDropDownViewTheme(theme);
        }

        @Nullable
        @Override
        public Resources.Theme getDropDownViewTheme() {
            return mDropdownAdapterHelper.getDropDownViewTheme();
        }


        /* Fills adapter list with station names and urls */
        private void initializeStationList(final ArrayList<String> stationUrls, final ArrayList<String> stationNames) {
            mStations = new ArrayList<HashMap<String,String>>();
            for (int i = 0; i < stationUrls.size(); i++) {
                HashMap<String,String> item = new HashMap<String,String>();
                item.put(SELECT_STREAM_KEY_URL, stationUrls.get(i));
                if (stationNames.size() > i) {
                    item.put(SELECT_STREAM_KEY_NAME, stationNames.get(i));
                } else {
                    item.put(SELECT_STREAM_KEY_NAME, "");
                }
                mStations.add(item);
            }
        }
    }
    /**
     * End of inner class
     */

}