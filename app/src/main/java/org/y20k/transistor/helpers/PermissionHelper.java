/**
 * PermissionHelper.java
 * Implements the PermissionHelper class
 * An PermissionHelper asks the user to grant a specific permission
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-19 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import org.y20k.transistor.R;

import androidx.core.app.ActivityCompat;


/**
 * PermissionHelper class
 */
public final class PermissionHelper {

    /* Define log tag */
    private static final String LOG_TAG = PermissionHelper.class.getSimpleName();


    /* Ask user to pick new image */
    public static boolean requestReadExternalStorage(final Activity activity, View rootView, final int requestType) {

        // permission to read external storage granted
        if (ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        // permission to read external storage not granted
        else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // ask for permission and explain why
                Snackbar snackbar = Snackbar.make(rootView, R.string.snackbar_request_storage_access, Snackbar.LENGTH_INDEFINITE);
                snackbar.setAction(R.string.dialog_generic_button_okay, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, requestType);
                    }
                });
                snackbar.show();
                return false;
            } else {
                // ask for permission without explanation
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, requestType);
                return false;
            }
        }
    }
}
