/**
 * PermissionHelper.java
 * Implements the PermissionHelper class
 * An PermissionHelper asks the user to grant a specific permission
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-16 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.view.View;

import org.y20k.transistor.R;


/**
 * PermissionHelper class
 */
public class PermissionHelper {

    /* Define log tag */
    private static final String LOG_TAG = PermissionHelper.class.getSimpleName();


    /* Main class variables */
    private final Activity mActivity;
    private final View mRootView;


    /* Constructor */
    public PermissionHelper(Activity activity, View view) {
        mActivity = activity;
        mRootView = view;
    }


    /* Ask user to pick new image */
    public boolean requestReadExternalStorage(final int requestType) {

        // permission to read external storage granted
        if (ActivityCompat.checkSelfPermission(mActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        // permission to read external storage not granted
        else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // ask for permission and explain why
                Snackbar snackbar = Snackbar.make(mRootView, mActivity.getString(R.string.snackbar_request_storage_access), Snackbar.LENGTH_INDEFINITE);
                snackbar.setAction(R.string.dialog_generic_button_okay, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, requestType);
                    }
                });
                snackbar.show();
                return false;
            } else {
                // ask for permission without explanation
                ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, requestType);
                return false;
            }
        }
    }
}
