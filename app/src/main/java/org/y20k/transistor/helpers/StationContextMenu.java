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

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewManager;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

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
        final View listItem = (View) mView.getParent();
        final ViewManager rowView = (ViewManager) listItem.getParent();

        PopupMenu popup = new PopupMenu(mContext, mView);
        popup.inflate(R.menu.menu_main_list_item);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {

                    // CASE RENAME
                    case R.id.menu_rename:
                        // prepare dialog builder
                        LayoutInflater inflater = LayoutInflater.from(mContext);
                        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

                        // get input field
                        View view = inflater.inflate(R.layout.dialog_rename_station, null);
                        final EditText inputField = (EditText) view.findViewById(R.id.dialog_rename_station_input);
                        inputField.setText(mCollection.getStations().get(mStationID).getStationName());

                        // set dialog view
                        builder.setView(view);

                        // add rename button
                        builder.setPositiveButton(R.string.dialog_button_rename, new DialogInterface.OnClickListener() {
                            // listen for click on delete button
                            public void onClick(DialogInterface arg0, int arg1) {
                                boolean success = mCollection.rename(mStationID, inputField.getText().toString());

                                if (!success) {
                                    // notify the user
                                    Toast.makeText(mContext, R.string.alertmessage_rename_unsuccessful, Toast.LENGTH_LONG).show();
                                } else {
                                    // notify MainActivityFragment
                                    if (mStationChangedListener != null) {
                                        mStationChangedListener.stationChanged();
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
                        return true;

                    // CASE DELETE
                    case R.id.menu_delete:
                        AlertDialog.Builder deleteDialog = new AlertDialog.Builder(mContext);

                        // add message to dialog
                        deleteDialog.setMessage(R.string.dialog_delete_station_message);

                        // add delete button
                        deleteDialog.setPositiveButton(R.string.dialog_button_delete, new DialogInterface.OnClickListener() {
                            // listen for click on delete button
                            public void onClick(DialogInterface arg0, int arg1) {
                                boolean success = mCollection.delete(mStationID);
                                if (success) {
                                    // notity the user
                                    Toast.makeText(mContext, R.string.alertmessage_delete_successful, Toast.LENGTH_LONG).show();
                                    // notify the collection adapter
                                    if (mStationChangedListener != null) {
                                        mStationChangedListener.stationChanged();
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
