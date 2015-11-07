/**
 * DialogAddStation.java
 * Implements the DialogAddStation class
 * A DialogAddStation asks the user for a stream URL of a radio station
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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import org.y20k.transistor.R;


/**
 * DialogAddStation class
 */
public class DialogAddStation extends DialogFragment {

    /* Define log tag */
    public final String LOG_TAG = DialogAddStation.class.getSimpleName();


    /* Main class variables */
    private CollectionChangedListener mCollectionChangedListener;


    /* Interface for custom listener */
    public interface CollectionChangedListener {
        void collectionChanged();
    }


    /* Constructor */
    public DialogAddStation() {
        mCollectionChangedListener = null;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                final String input = inputField.getText().toString();
                StationHelper stationHelper = new StationHelper(getActivity());
                stationHelper.setStationChangedListener(new StationHelper.StationChangedListener() {
                    @Override
                    public void stationChanged() {
                        if (mCollectionChangedListener != null) {
                            mCollectionChangedListener.collectionChanged();
                        }
                    }
                });
                stationHelper.add(input);
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


    /* Setter for custom listener */
    public void setCollectionChangedListener(CollectionChangedListener collectionChangedListener) {
        mCollectionChangedListener = collectionChangedListener;
    }

}