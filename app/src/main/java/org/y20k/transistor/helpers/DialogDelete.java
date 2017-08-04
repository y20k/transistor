/**
 * DialogDelete.java
 * Implements the DialogDelete class
 * A DialogDelete deletes a station after asking the user for permission
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-17 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import org.y20k.transistor.MainActivity;
import org.y20k.transistor.PlayerService;
import org.y20k.transistor.R;
import org.y20k.transistor.core.Station;


/**
 * DialogDelete class
 */
public final class DialogDelete implements TransistorKeys {

    /* Define log tag */
    private static final String LOG_TAG = DialogDelete.class.getSimpleName();


    /* Main class variables */
    private final Activity mActivity;
    private final Station mStation;


    /* Constructor */
    public DialogDelete(Activity activity, Station station) {
        mActivity = activity;
        mStation = station;
    }


    /* Construct and show dialog */
    public void show() {
        // stop player service using intent
        Intent intent = new Intent(mActivity, PlayerService.class);
        intent.setAction(ACTION_DISMISS);
        mActivity.startService(intent);
        LogHelper.v(LOG_TAG, "Opening delete dialog. Stopping player service.");

        AlertDialog.Builder deleteDialog = new AlertDialog.Builder(mActivity);

        // add message to dialog
        deleteDialog.setMessage(R.string.dialog_delete_station_message);

        // add delete button
        deleteDialog.setPositiveButton(R.string.dialog_button_delete, new DialogInterface.OnClickListener() {
            // listen for click on delete button
            public void onClick(DialogInterface arg0, int arg1) {

                // hand station over to main activity
                ((MainActivity)mActivity).handleStationDelete(mStation);

            }
        });

        // add cancel button
        deleteDialog.setNegativeButton(R.string.dialog_generic_button_cancel, new DialogInterface.OnClickListener() {
            // listen for click on cancel button
            public void onClick(DialogInterface arg0, int arg1) {
                // do nothing
            }
        });

        // display delete dialog
        deleteDialog.show();
    }

}
