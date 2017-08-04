/**
 * StationContextMenu.java
 * Implements the StationContextMenu class
 * The StationContextMenu allows manipulation of station objects, eg. rename or delete
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-17 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.app.Activity;
import android.app.DialogFragment;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import org.y20k.transistor.MainActivity;
import org.y20k.transistor.R;
import org.y20k.transistor.core.Station;


/**
 * StationContextMenu class
 */
public final class StationContextMenu extends DialogFragment implements TransistorKeys {


    /* Main class variables */
    private View mView;
    private Activity mActivity;
    private Station mStation;


    /* Constructor (default) */
    public StationContextMenu() {
    }


    /* Initializer for main class variables */
    public void initialize(Activity activity, View view, Station station) {
        mActivity = activity;
        mView = view;
        mStation = station;
    }


    /* Displays context menu */
    public void show() {

        PopupMenu popup = new PopupMenu(mActivity, mView);
        popup.inflate(R.menu.menu_list_list_item);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()) {

                    // CASE ICON
                    case R.id.menu_icon:
                        // get system picker for images
                        ((MainActivity)mActivity).pickImage(mStation);
                        return true;

                    // CASE RENAME
                    case R.id.menu_rename:
                        // construct and run rename dialog
                        DialogRename dialogRename = new DialogRename(mActivity, mStation);
                        dialogRename.show();
                        return true;

                    // CASE DELETE
                    case R.id.menu_delete:
                        // construct and run delete dialog
                        DialogDelete dialogDelete = new DialogDelete(mActivity, mStation);
                        dialogDelete.show();
                        return true;

                    // CASE SHORTCUT
                    case R.id.menu_shortcut: {
                        // create shortcut
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
