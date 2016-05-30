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

    /* Define log tag */
    private static final String LOG_TAG = DialogRename.class.getSimpleName();


    /* Main class variables */
    private final Activity mActivity;
    private final Collection mCollection;
    private final int mStationID;
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

                // TODO rename station shortcut

                // get ID and Uri of currently playing station
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
                int stationIDPlaying = settings.getInt(TransistorKeys.PREF_STATION_ID_CURRENTLY_PLAYING, -1);
                String stationUriPlaying = null;
                if (stationIDPlaying != -1) {
                    stationUriPlaying = mCollection.getStations().get(stationIDPlaying).getStreamUri().toString();
                }

                // set flag if currently playing station is about to be renamed
                boolean stationCurrentlyPlayingRenamed = false;
                if (mStationID == stationIDPlaying) {
                    stationCurrentlyPlayingRenamed = true;
                }

                // get new station name from text input
                mStationName = inputField.getText().toString();

                // rename station
                mStationName = inputField.getText().toString();
                boolean success = mCollection.rename(mStationID, mStationName);
                if (success) {
                    // get updated station id for currently playing station
                    if (stationUriPlaying != null) {
                        stationIDPlaying = mCollection.findStationID(stationUriPlaying);
                    }
                    // send local broadcast
                    Intent i = new Intent();
                    i.setAction(TransistorKeys.ACTION_COLLECTION_CHANGED);
                    i.putExtra(TransistorKeys.EXTRA_COLLECTION_CHANGE, TransistorKeys.STATION_RENAMED);
                    i.putExtra(TransistorKeys.EXTRA_COLLECTION, mCollection);
                    i.putExtra(TransistorKeys.EXTRA_STATION_ID, mStationID);
                    i.putExtra(TransistorKeys.EXTRA_STATION_NEW_NAME, mStationName);
                    i.putExtra(TransistorKeys.EXTRA_STATION_ID_CURRENTLY_PLAYING, stationIDPlaying);
                    i.putExtra(TransistorKeys.EXTRA_STATION_CURRENTLY_PLAYING_RENAMED, stationCurrentlyPlayingRenamed);
                    i.putExtra(TransistorKeys.EXTRA_COLLECTION, mCollection);
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