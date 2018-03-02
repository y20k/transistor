/**
 * DialogAddChooseStream.java
 * Implements the DialogAddChooseStream class
 * A DialogAddChooseStream lets the user choose between multiple streams from a fetched playlist
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-18 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Spinner;

import org.y20k.transistor.R;

import java.util.ArrayList;


/**
 * DialogAddChooseStream class
 */
public final class DialogAddChooseStream implements TransistorKeys {

    /* Define log tag */
    private static final String LOG_TAG = DialogAddChooseStream.class.getSimpleName();


    /* Construct and show dialog */
    public static void show(final Activity activity, final ArrayList<String> stationUris, final ArrayList<String> stationNames) {
        // prepare dialog builder
        LayoutInflater inflater = LayoutInflater.from(activity);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        // get input field
        View view = inflater.inflate(R.layout.dialog_add_choose_stream, null);
        final Spinner stationSelection = (Spinner) view.findViewById(R.id.dialog_add_choose_stream_spinner);

        // todo fill spinner
        // ...
//        final AlertDialog.Builder listDialog = new AlertDialog.Builder(myself);
//        listDialog.setIcon(R.mipmap.ic_launcher);
//        listDialog.setTitle(R.string.descr_list_add_new);
//
//        ArrayList<HashMap<String,String>> elements = new ArrayList<HashMap<String,String>>();
//        for (int i = 0; i < stationUris.size(); i++) {
//            HashMap<String,String> item = new HashMap<String,String>();
//            item.put("uri", stationUris.get(i);
//            if (stationNames.size() > i) {
//                item.put("name", stationNames.get(i));
//            } else {
//                item.put("name", "");
//            }
//            elements.add(item);
//        }
//
//        final SimpleAdapter arrayAdapter =
//                new SimpleAdapter(activity, elements, R.layout.listview_select_station,
//                        new String[] {"name", "uri"},
//                        new int[] {R.id.select_station_name, R.id.select_station_uri});
//
//        listDialog.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                HashMap<String,String> selection = (HashMap<String,String>) arrayAdapter.getItem(which);
//                result.uri = uris.get(which);
//                if (names.size() > which) {
//                    result.name = names.get(which);
//                }
//                dialogFinished.release();
//            }
//        });


        // set dialog view
        builder.setView(view);

        // add okay button
        builder.setPositiveButton(R.string.dialog_add_station_choose_stream_button, new DialogInterface.OnClickListener() {
            // listen for click on delete button
            public void onClick(DialogInterface arg0, int arg1) {
                // todo get station from spinner
                String selectionName = stationNames.get(0);
                Uri selectionUri = Uri.parse(stationUris.get(0));

                // add new station
                StationFetcher stationFetcher = new StationFetcher(activity, StorageHelper.getCollectionDirectory(activity), selectionUri, selectionName);
                stationFetcher.execute();
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

}