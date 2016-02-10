/**
 * DialogRename.java
 * Implements the DialogRename class
 * A DialogRename renames a station after asking the user for a new name
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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


    /* Main class variables */
    private final Activity mActivity;
    private final Collection mCollection;
    private final int mStationID;
    private String mStationName;
    private StationRenamedListener mStationRenamedListener;


    /* Interface for custom listener */
    public interface StationRenamedListener {
        void stationRenamed();
    }


    /* Constructor */
    public DialogRename(Activity activity, Collection collection, String stationName, int stationID) {
        mActivity = activity;
        mStationName = stationName;
        mStationID = stationID;
        mCollection = collection;
        mStationRenamedListener = null;
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
                    // check for playback
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
                    boolean playback = settings.getBoolean(PLAYBACK, false);

                    if (playback) {
                        // put up changed notification
                        NotificationHelper notificationHelper = new NotificationHelper(mActivity);
                        notificationHelper.setStationName(mStationName);
                        notificationHelper.createNotification();

                        // save new station ID if changed
                        int newIndex = mCollection.getStationIndexChanged();
                        if (newIndex != -1) {
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putInt(STATION_ID_CURRENT, newIndex);
                            editor.apply();
                        }

                    }

                    // notify MainActivityFragment
                    if (mStationRenamedListener != null) {
                        mStationRenamedListener.stationRenamed();
                    }

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


    /* Setter for custom listener */
    public void setStationRenamedListener(StationRenamedListener stationRenamedListener) {
        mStationRenamedListener = stationRenamedListener;
    }


    /* Getter for name of station */
    public String getStationName() {
        return mStationName;
    }

}


