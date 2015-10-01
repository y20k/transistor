/**
 * DialogAddStationFragment.java
 * Implements the DialogAddStationFragment class
 * A DialogAddStationFragment asks the user for a stream URL of a radio station
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.y20k.transistor.R;
import org.y20k.transistor.core.Collection;
import org.y20k.transistor.core.Station;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * DialogAddStationFragment class
 */
public class DialogAddStationFragment extends DialogFragment {

    /* Main class variables */
    private Collection mCollection;
    private URL mNewStationURL;
    private CollectionChangedListener mCollectionChangedListener;


    /* Interface for custom listener */
    public interface CollectionChangedListener {
        void collectionChanged();
    }


    /* Constructor (default) */
    public DialogAddStationFragment () {
    }


    /* Setter for custom listener */
    public void setCollectionChangedListener(CollectionChangedListener collectionChangedListener) {
        mCollectionChangedListener = collectionChangedListener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set up variables
        mCollection = null;
        mNewStationURL = null;
        mCollectionChangedListener = null;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // prepare dialog builder
        LayoutInflater inflater = getActivity().getLayoutInflater();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // get input field
        View view = inflater.inflate(R.layout.dialog_add_station, null);
        final EditText inputField = (EditText) view.findViewById(R.id.dialog_add_station_input);

        // set dialog view
        builder.setView(view);

        // add "add" button
        builder.setPositiveButton(R.string.dialog_add_station_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                String input = inputField.getText().toString();
                try {
                    mNewStationURL = new URL(input);
                    Toast.makeText(getActivity(), R.string.alertmessage_add_download_started + input, Toast.LENGTH_LONG).show();
                    AddStationFromDialog loadCollection = new AddStationFromDialog();
                    loadCollection.execute();

                    if (mCollectionChangedListener != null) {
                        mCollectionChangedListener.collectionChanged();
                    }

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    Toast.makeText(getActivity(), R.string.alertmessage_add_malformed_url + input, Toast.LENGTH_LONG).show();
                }
            }
        });

        // add cancel button
        builder.setNegativeButton(R.string.dialog_generic_button_cancel, new DialogInterface.OnClickListener() {
            // listen for click on cancel button
            public void onClick(DialogInterface arg0, int arg1) {
                // do nothing
            }
        });

        return builder.create();
    }


    /**
     * Inner class: add new to collection of radio stations using background thread
     */
    public class AddStationFromDialog extends AsyncTask<Void, Void, Collection> {

        /* Main class variables */
        private File folder = null;

        /* Background thread: reads m3u files from storage */
        @Override
        public Collection doInBackground(Void... params) {
            folder = new File(getActivity().getExternalFilesDir("Collection").toString());
            mCollection = new Collection(folder);
            Station newStation = new Station(folder, mNewStationURL);
            mCollection.add(newStation);
            return mCollection;
        }

        /* Main thread: do nothing for now */
        @Override
        protected void onPostExecute(Collection stations) {
            // notify MainActivityFragment
            if (mCollectionChangedListener != null) {
                mCollectionChangedListener.collectionChanged();
            }
        }
    }
    /**
     * End of inner class
     */

}