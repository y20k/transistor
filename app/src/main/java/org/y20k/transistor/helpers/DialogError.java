/**
 * DialogError.java
 * Implements the DialogError class
 * A DialogError shows an error dialog with details
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
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.y20k.transistor.R;


/**
 * DialogError class
 */
public final class DialogError {

    /* Main class variables */
    private final Activity mActivity;
    private final String mErrorTitle;
    private final String mErrorMessage;
    private final String mErrorDetails;


    /* Constructor */
    public DialogError(Activity activity, String errorTitle, String errorMessage, String errorDetails) {
        mActivity = activity;
        mErrorTitle = errorTitle;
        mErrorMessage = errorMessage;
        mErrorDetails = errorDetails;
    }


    /* Construct and show dialog */
    public void show() {
        // prepare dialog builder
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);

        // get views
        View view = inflater.inflate(R.layout.dialog_error, null);
        final TextView errorTitleView = (TextView) view.findViewById(R.id.dialog_error_title);
        final TextView errorMessageView = (TextView) view.findViewById(R.id.dialog_error_message);
        final TextView errorDetailsLinkView = (TextView) view.findViewById(R.id.dialog_error_details_link);
        final TextView errorDetailsView = (TextView) view.findViewById(R.id.dialog_error_details);

        // allow scrolling on details view
        errorDetailsView.setMovementMethod(new ScrollingMovementMethod());

        // show and hide details
        errorDetailsLinkView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (errorDetailsView.getVisibility() == View.GONE) {
                    errorDetailsView.setVisibility(View.VISIBLE);
                } else {
                    errorDetailsView.setVisibility(View.GONE);
                }
            }
        });

        // set text views
        errorTitleView.setText(mErrorTitle);
        errorMessageView.setText(mErrorMessage);
        errorDetailsView.setText(mErrorDetails);

        // set dialog view
        builder.setView(view);

        // add rename button
        builder.setPositiveButton(R.string.dialog_generic_button_okay, new DialogInterface.OnClickListener() {
            // listen for click on okay button
            public void onClick(DialogInterface arg0, int arg1) {
                // do nothing
            }
        });

        // display error dialog
        builder.show();
    }

}