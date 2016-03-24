/**
 * DialogRename.java
 * Implements the DialogRename class
 * A DialogRename renames a station after asking the user for a new name
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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.y20k.transistor.R;
import org.y20k.transistor.core.Collection;


/**
 * DialogRename class
 */
public final class DialogRename {

    /* Keys */
    private static final String PLAYBACK = "playback";
    private static final String STATION_ID_CURRENT = "stationIDCurrent";
    private static final String ACTION_COLLECTION_CHANGED = "org.y20k.transistor.action.COLLECTION_CHANGED";
    private static final String EXTRA_STATION_NEW_POSITION = "STATION_NEW_POSITION";
    private static final String EXTRA_STATION_DELETED = "STATION_DELETED";


    /* Main class variables */
    private final Activity mActivity;
    private final Collection mCollection;
    private int mStationID;
    private String mStationName;


    /* Constructor */
    public DialogRename(Activity activity, Collection collection, String stationName, int stationID) {
        mActivity = activity;
        mStationName = stationName;
        mStationID = stationID;
        mCollection = collection;
    }


    /* Construct and show dialog */
    public void show() {
        // prepare dialog builder
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);

        // get input field
        View view = inflater.inflate(R.layout.dialog_rename_station, null);
        final EditText inputField = (EditText) view.findViewById(R.id.dialog_rename_station_input);
        inputField.setText(mStationName);

        // set dialog view
        builder.setView(view);

        // add rename button
        builder.setPositiveButton(R.string.dialog_button_rename, new DialogInterface.OnClickListener() {
            // listen for click on delete button
            public void onClick(DialogInterface arg0, int arg1) {
                mStationName = inputField.getText().toString();
                boolean success = mCollection.rename(mStationID, mStationName);
                if (!success) {
                    // notify the user
                    Toast.makeText(mActivity, mActivity.getString(R.string.toastalert_rename_unsuccessful), Toast.LENGTH_LONG).show();
                } else {

                    // check if station index has changed
                    int newStationID = mCollection.getStationIndexChanged();
                    if (newStationID != -1) {
                        // ID of station has changed
                        mStationID = newStationID;
                    }

                    // check for playback
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
                    boolean playback = settings.getBoolean(PLAYBACK, false);
                    if (playback) {
                        // save new station ID if changed
                        int newIndex = mCollection.getStationIndexChanged();
                        if (newIndex != -1) {
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putInt(STATION_ID_CURRENT, newIndex);
                            editor.apply();
                        }

                        // put up changed notification
                        NotificationHelper notificationHelper = new NotificationHelper(mActivity);
                        notificationHelper.setStationName(mStationName);
                        notificationHelper.setStationID(mStationID);
                        notificationHelper.createNotification();
                    }

                    // send local broadcast
                    Intent i = new Intent();
                    i.setAction(ACTION_COLLECTION_CHANGED);
                    i.putExtra(EXTRA_STATION_NEW_POSITION, mStationID);
                    i.putExtra(EXTRA_STATION_DELETED, false);
                    LocalBroadcastManager.getInstance(mActivity.getApplication()).sendBroadcast(i);

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

        // display rename dialog
        builder.show();
    }



    /* Getter for name of station */
    public String getStationName() {
        return mStationName;
    }

}


