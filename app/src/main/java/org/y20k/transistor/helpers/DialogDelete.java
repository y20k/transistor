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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import org.y20k.transistor.R;
import org.y20k.transistor.core.Collection;


/**
 * DialogDelete class
 */
public final class DialogDelete {

    /* Define log tag */
    private static final String LOG_TAG = DialogDelete.class.getSimpleName();


    /* Keys */
    private static final String ACTION_COLLECTION_CHANGED = "org.y20k.transistor.action.COLLECTION_CHANGED";
    private static final String EXTRA_COLLECTION_CHANGE = "COLLECTION_CHANGE";
    private static final String EXTRA_STATION_ID = "STATION_ID";
    private static final String EXTRA_STATION_URI_CURRENT = "STATION_URI_CURRENT";
    private static final String EXTRA_STATION_NEW_NAME = "STATION_NEW_NAME";
    private static final String EXTRA_STATION_NEW_POSITION = "STATION_NEW_POSITION";
    private static final String EXTRA_STATION_OLD_POSITION = "STATION_OLD_POSITION";
    private static final String PREF_STATION_ID_CURRENT = "prefStationIDCurrent";
    private static final int STATION_DELETED = 3;


    /* Main class variables */
    private final Activity mActivity;
    private final Collection mCollection;
    private final int mStationID;


    /* Constructor */
    public DialogDelete(Activity activity, Collection collection, int stationID) {
        mActivity = activity;
        mStationID = stationID;
        mCollection = collection;
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

                // delete station shortcut
                ShortcutHelper shortcutHelper = new ShortcutHelper(mActivity, mCollection);
                shortcutHelper.removeShortcut(mStationID);

                // get currently playing station before deleting
                String stationUriCurrent = null;
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);
                int stationIDCurrent = settings.getInt(PREF_STATION_ID_CURRENT, -1);
                if (stationIDCurrent != -1 && stationIDCurrent > mCollection.getStations().size()) {
                    stationUriCurrent = mCollection.getStations().get(stationIDCurrent).getStreamUri().toString();
                }

                // delete station entry
                boolean success = mCollection.delete(mStationID);
                if (success) {
                    // send local broadcast
                    Intent i = new Intent();
                    i.setAction(ACTION_COLLECTION_CHANGED);
                    i.putExtra(EXTRA_COLLECTION_CHANGE, STATION_DELETED);
                    i.putExtra(EXTRA_STATION_ID, mStationID);
                    if (stationUriCurrent != null) {
                        i.putExtra(EXTRA_STATION_URI_CURRENT, stationUriCurrent);
//                        i.putExtra(EXTRA_STATION_OLD_POSITION, mStationID);
                    }
                    LocalBroadcastManager.getInstance(mActivity.getApplication()).sendBroadcast(i);
                    // notify user
                    Toast.makeText(mActivity, mActivity.getString(R.string.toastalert_delete_successful), Toast.LENGTH_LONG).show();
                }

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
