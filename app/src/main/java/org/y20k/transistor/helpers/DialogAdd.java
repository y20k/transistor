/**
 * DialogAdd.java
 * Implements the DialogAdd class
 * A DialogAdd asks the user for a stream URL of a radio station
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
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import org.y20k.transistor.R;

import java.io.File;


/**
 * DialogAdd class
 */
public final class DialogAdd {

    /* Define log tag */
    private static final String LOG_TAG = DialogAdd.class.getSimpleName();


    /* Construct and show dialog */
    public static void show(final Activity activity, final File folder) {
        // prepare dialog builder
        LayoutInflater inflater = LayoutInflater.from(activity);
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        // get input field
        View view = inflater.inflate(R.layout.dialog_add_station, null);

        final EditText inputField = (EditText) view.findViewById(R.id.dialog_add_station_input);

        // set dialog view
        builder.setView(view);

        // add "add" button
        builder.setPositiveButton(R.string.dialog_add_station_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                if (inputField.getText() != null) {
                    final String input = inputField.getText().toString();
                    // download new station
                    StationFetcher stationFetcher = new StationFetcher(activity, folder, Uri.parse(input.trim()), null);
                    stationFetcher.execute();
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

        // display add dialog
        builder.show();
    }

}