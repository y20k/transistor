/**
 * StationContextMenu.java
 * Implements the StationContextMenu class
 * The StationContextMenu allows manipulation of station objects, eg. rename or delete
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
import android.app.DialogFragment;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import org.y20k.transistor.R;
import org.y20k.transistor.core.Collection;


/**
 * StationContextMenu class
 */
public final class StationContextMenu extends DialogFragment {


    /* Keys */
    private static final String ACTION_IMAGE_CHANGE_REQUESTED = "org.y20k.transistor.action.IMAGE_CHANGE_REQUESTED";
    private static final String ACTION_CREATE_SHORTCUT_REQUESTED = "org.y20k.transistor.action.CREATE_SHORTCUT_REQUESTED";
    private static final String ACTION_COLLECTION_CHANGED = "org.y20k.transistor.action.COLLECTION_CHANGED";
    private static final String EXTRA_STATION_POSITION = "STATION_POSITION";
    private static final String EXTRA_STATION_ID = "STATION_ID";

    /* Main class variables */
    private View mView;
    private int mStationID;
    private Activity mActivity;
    private Collection mCollection;


    /* Constructor (default) */
    public StationContextMenu() {
    }




    /* Initializer for main class variables */
    public void initialize(Activity activity, Collection collection, View view, int stationID) {
        mActivity = activity;
        mCollection = collection;
        mView = view;
        mStationID = stationID;
    }


    /* Displays context menu */
    public void show() {

        PopupMenu popup = new PopupMenu(mActivity, mView);
        popup.inflate(R.menu.menu_main_list_item);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()) {

                    // CASE ICON
                    case R.id.menu_icon:

                        // send local broadcast (needed by MainActivityFragment)
                        Intent iconIntent = new Intent();
                        iconIntent.setAction(ACTION_IMAGE_CHANGE_REQUESTED);
                        iconIntent.putExtra(EXTRA_STATION_ID, mStationID);
                        LocalBroadcastManager.getInstance(mActivity.getApplication()).sendBroadcast(iconIntent);

                        return true;

                    // CASE RENAME
                    case R.id.menu_rename:
                        // get name of station
                        String stationName = mCollection.getStations().get(mStationID).getStationName();
                        // construct and run rename dialog
                        DialogRename dialogRename = new DialogRename(mActivity, mCollection, stationName, mStationID);
                        dialogRename.show();
                        return true;

                    // CASE DELETE
                    case R.id.menu_delete:
                        // construct and run delete dialog
                        DialogDelete dialogDelete = new DialogDelete(mActivity, mCollection, mStationID);
                        dialogDelete.show();
                        return true;

                    // CASE SHORTCUT
                    case R.id.menu_shortcut: {
                        // send local broadcast (needed by MainActivityFragment)
                        Intent shortcutIntent = new Intent();
                        shortcutIntent.setAction(ACTION_CREATE_SHORTCUT_REQUESTED);
                        shortcutIntent.putExtra(EXTRA_STATION_ID, mStationID);
                        LocalBroadcastManager.getInstance(mActivity.getApplication()).sendBroadcast(shortcutIntent);
                        return true;
                    }

                    // CASE DEFAULT
                    default:
                        return false;
                }
            }
        });
        popup.show();
    }

}
