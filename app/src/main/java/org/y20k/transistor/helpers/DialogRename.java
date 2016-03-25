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
    private static final String ACTION_COLLECTION_CHANGED = "org.y20k.transistor.action.COLLECTION_CHANGED";
    private static final String PREF_STATION_ID_CURRENT = "prefStationIDCurrent";
    private static final String EXTRA_STATION_URI_CURRENT = "STATION_URI_CURRENT";
    private static final String EXTRA_STATION_NEW_NAME = "STATION_NEW_NAME";
    private static final String EXTRA_STATION_NEW_POSITION = "STATION_NEW_POSITION";
    private static final String EXTRA_STATION_OLD_POSITION = "STATION_OLD_POSITION";
    private static final String EXTRA_STATION_RENAMED = "STATION_RENAMED";


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
                // get new station name
                mStationName = inputField.getText().toString();

                // get currently playing station
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
                String stationUriCurrent = mCollection.getStations().get(settings.getInt(PREF_STATION_ID_CURRENT, -1)).getStreamUri().toString();

                // rename station
                boolean success = mCollection.rename(mStationID, mStationName);
                if (success) {
                    // send local broadcast
                    Intent i = new Intent();
                    i.setAction(ACTION_COLLECTION_CHANGED);
                    i.putExtra(EXTRA_STATION_URI_CURRENT, stationUriCurrent);
//                    i.putExtra(EXTRA_STATION_NEW_NAME, mStationName);
                    i.putExtra(EXTRA_STATION_NEW_POSITION, mCollection.getStationIndexChanged());
                    i.putExtra(EXTRA_STATION_OLD_POSITION, mStationID);
//                    i.putExtra(EXTRA_STATION_RENAMED, true);
                    LocalBroadcastManager.getInstance(mActivity.getApplication()).sendBroadcast(i);

                } else {
                    // rename operation unsuccessful, notify user
                    Toast.makeText(mActivity, mActivity.getString(R.string.toastalert_rename_unsuccessful), Toast.LENGTH_LONG).show();
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


