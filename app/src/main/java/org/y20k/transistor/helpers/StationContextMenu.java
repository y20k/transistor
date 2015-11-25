/**
 * StationContextMenu.java
 * Implements the StationContextMenu class
 * The StationContextMenu allows manipulation of station objects, eg. rename or delete
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.app.DialogFragment;
import android.content.Context;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import org.y20k.transistor.R;
import org.y20k.transistor.core.Collection;


/**
 * StationContextMenu class
 */
public class StationContextMenu extends DialogFragment {

    /* Main class variables */
    private View mView;
    private int mStationID;
    private Context mContext;
    private Collection mCollection;
    private StationChangedListener mStationChangedListener;


    /* Interface for custom listener */
    public interface StationChangedListener {
        void stationChanged();
    }


    /* Constructor */
    public StationContextMenu() {
        mStationChangedListener = null;
    }


    /* Setter for custom listener */
    public void setStationChangedListener(StationChangedListener stationChangedListener) {
        mStationChangedListener = stationChangedListener;
    }


    /* Initializer for main class variables */
    public void initialize(Context context, Collection collection, View view, int stationID) {
        mContext = context;
        mCollection = collection;
        mView = view;
        mStationID = stationID;
    }


    /* Displays context menu */
    public void show() {

        PopupMenu popup = new PopupMenu(mContext, mView);
        popup.inflate(R.menu.menu_main_list_item);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()) {

                    // CASE RENAME
                    case R.id.menu_rename:
                        // get name of station
                        String stationName = mCollection.getStations().get(mStationID).getStationName();
                        // construct rename dialog
                        DialogRename dialogRename = new DialogRename(mContext, mCollection, stationName, mStationID);
                        dialogRename.setStationRenamedListener(new DialogRename.StationRenamedListener() {
                            @Override
                            public void stationRenamed() {
                                if (mStationChangedListener != null) {
                                    mStationChangedListener.stationChanged();
                                }
                            }
                        });
                        // run dialog
                        dialogRename.show();
                        return true;

                    // CASE DELETE
                    case R.id.menu_delete:
                        // construct delete dialog
                        DialogDelete dialogDelete = new DialogDelete(mContext, mCollection, mStationID);
                        // run dialog
                        dialogDelete.show();
                        return true;

                    // CASE DEFAULT
                    default:
                        return false;
                }
            }
        });
        popup.show();
    }

}
