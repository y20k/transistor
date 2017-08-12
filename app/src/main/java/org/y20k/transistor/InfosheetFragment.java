/**
 * InfosheetFragment.java
 * Implements the app's infosheet activity
 * The infosheet activity sets up infosheet screens for "About" and "How to"
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-17 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.y20k.transistor.helpers.TransistorKeys;


/**
 * InfosheetFragment class
 */
public final class InfosheetFragment extends Fragment implements TransistorKeys  {

    /* Define log tag */
    private static final String LOG_TAG = InfosheetFragment.class.getSimpleName();


    /* Main class variables */
    private Activity mActivity;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get activity
        mActivity = getActivity();

        // fragment has options menu
        setHasOptionsMenu(true);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        // inflate root view from xml
        View rootView = inflater.inflate(R.layout.fragment_infosheet, container, false);

        // get data from arguments
        Bundle arguments = getArguments();
        if (arguments != null) {
            // get title from arguments
            if (arguments.containsKey(ARG_INFOSHEET_TITLE)) {
                mActivity.setTitle(arguments.getString(ARG_INFOSHEET_TITLE));
            }
            // set view
            View aboutView = rootView.findViewById(R.id.infosheet_content_about);
            View howtoView = rootView.findViewById(R.id.infosheet_content_howto);
            if (arguments.containsKey(ARG_INFOSHEET_CONTENT) && arguments.getInt(ARG_INFOSHEET_CONTENT) == (INFOSHEET_CONTENT_ABOUT)) {
                aboutView.setVisibility(View.VISIBLE);
                howtoView.setVisibility(View.GONE);
            } else if (arguments.containsKey(ARG_INFOSHEET_CONTENT) && arguments.getInt(ARG_INFOSHEET_CONTENT) == (INFOSHEET_CONTENT_HOWTO)) {
                aboutView.setVisibility(View.GONE);
                howtoView.setVisibility(View.VISIBLE);
            }
        }
        return rootView;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // inflate the menu items for use in the action bar
        inflater.inflate(R.menu.menu_list_actionbar, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // CASE HOME
            case android.R.id.home:
                // initiate back action
                ((AppCompatActivity) mActivity).getSupportFragmentManager().popBackStack();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
