/**
 * DialogDelete.java
 * Implements the DialogDelete class
 * A DialogDelete deletes a station after asking the user for permission
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-16 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.y20k.transistor.R;
import org.y20k.transistor.core.Station;


/**
 * DialogDelete class
 */
public final class DialogDelete {

    /* Define log tag */
    private static final String LOG_TAG = DialogDelete.class.getSimpleName();


    /* Main class variables */
    private final Activity mActivity;
    private final Station mStation;
    private final int mStationID;


    /* Constructor */
    public DialogDelete(Activity activity, Station station, int stationID) {
        mActivity = activity;
        mStation = station;
        mStationID = stationID;
    }


    /* Construct and show dialog */
    public void show() {
        AlertDialog.Builder deleteDialog = new AlertDialog.Builder(mActivity);

        // add message to dialog
        deleteDialog.setMessage(R.string.dialog_delete_station_message);

        // add delete button
        deleteDialog.setPositiveButton(R.string.dialog_button_delete, new DialogInterface.OnClickListener() {
            // listen for click on delete button
            public void onClick(DialogInterface arg0, int arg1) {

                // send local broadcast
                Intent i = new Intent();
                i.setAction(TransistorKeys.ACTION_COLLECTION_CHANGED);
                i.putExtra(TransistorKeys.EXTRA_COLLECTION_CHANGE, TransistorKeys.STATION_DELETED);
                i.putExtra(TransistorKeys.EXTRA_STATION, mStation);
                i.putExtra(TransistorKeys.EXTRA_STATION_ID, mStationID);
                LocalBroadcastManager.getInstance(mActivity.getApplication()).sendBroadcast(i);

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
