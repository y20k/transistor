/**
 * DialogDelete.java
 * Implements the DialogDelete class
 * A DialogDelete deletes a station after asking the user for permission
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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import org.y20k.transistor.R;
import org.y20k.transistor.core.Collection;


/**
 * DialogDelete class
 */
public class DialogDelete {

    /* Keys */
    private static final String ACTION_COLLECTION_CHANGED = "org.y20k.transistor.action.COLLECTION_CHANGED";


    /* Main class variables */
    private final Context mContext;
    private final Collection mCollection;
    private final int mStationID;
    private StationDeletedListener mStationDeletedListener;


    /* Interface for custom listener */
    public interface StationDeletedListener {
        void stationDeleted();
    }


    /* Constructor */
    public DialogDelete(Context context, Collection collection, int stationID) {
        mContext = context;
        mStationID = stationID;
        mCollection = collection;
        mStationDeletedListener = null;
    }


    /* Construct and show dialog */
    public void show() {
        AlertDialog.Builder deleteDialog = new AlertDialog.Builder(mContext);

        // add message to dialog
        deleteDialog.setMessage(R.string.dialog_delete_station_message);

        // add delete button
        deleteDialog.setPositiveButton(R.string.dialog_button_delete, new DialogInterface.OnClickListener() {
            // listen for click on delete button
            public void onClick(DialogInterface arg0, int arg1) {
                boolean success = mCollection.delete(mStationID);
                if (success) {
                    // notify the user
                    Toast.makeText(mContext, R.string.toastalert_delete_successful, Toast.LENGTH_LONG).show();

                    // send local broadcast
                    System.out.println("!!! Ding (DIALOG DELETE)");
                    Intent i = new Intent();
                    i.setAction(ACTION_COLLECTION_CHANGED);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(i);

                    // notify MainActivityFragment
                    if (mStationDeletedListener != null) {
                        mStationDeletedListener.stationDeleted();
                    }

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

    /* Setter for custom listener */
    public void setStationDeletedListener(StationDeletedListener stationDeletedListener) {
        mStationDeletedListener = stationDeletedListener;
    }


}
