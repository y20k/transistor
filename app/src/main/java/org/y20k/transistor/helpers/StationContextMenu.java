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
import org.y20k.transistor.core.Station;


/**
 * StationContextMenu class
 */
public final class StationContextMenu extends DialogFragment {


    /* Main class variables */
    private View mView;
    private Activity mActivity;
    private Station mStation;
    private int mStationID;


    /* Constructor (default) */
    public StationContextMenu() {
    }


    /* Initializer for main class variables */
    public void initialize(Activity activity, View view, Station station, int stationID) {
        mActivity = activity;
        mView = view;
        mStation = station;
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
                        iconIntent.setAction(TransistorKeys.ACTION_IMAGE_CHANGE_REQUESTED);
                        iconIntent.putExtra(TransistorKeys.EXTRA_STATION, mStation);
                        iconIntent.putExtra(TransistorKeys.EXTRA_STATION_ID, mStationID);
                        LocalBroadcastManager.getInstance(mActivity.getApplication()).sendBroadcast(iconIntent);
                        return true;

                    // CASE RENAME
                    case R.id.menu_rename:
                        // construct and run rename dialog
                        DialogRename dialogRename = new DialogRename(mActivity, mStation, mStationID);
                        dialogRename.show();
                        return true;

                    // CASE DELETE
                    case R.id.menu_delete:
                        // construct and run delete dialog
                        DialogDelete dialogDelete = new DialogDelete(mActivity, mStation, mStationID);
                        dialogDelete.show();
                        return true;

                    // CASE SHORTCUT
                    case R.id.menu_shortcut: {
                        // create shortcut
//                        ShortcutHelper shortcutHelper = new ShortcutHelper(mActivity);
                        ShortcutHelper shortcutHelper = new ShortcutHelper(mActivity.getApplication().getApplicationContext());
                        shortcutHelper.placeShortcut(mStation);
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
