/**
 * StorageHelper.java
 * Implements the StorageHelper class
 * A StorageHelper provides reliable access to Androids external storage
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
import android.os.Environment;
import android.support.v4.os.EnvironmentCompat;
import android.util.Log;
import android.widget.Toast;

import org.y20k.transistor.R;

import java.io.File;


/**
 * StorageHelper class
 */
public class StorageHelper {

    /* Define log tag */
    private static final String LOG_TAG = StorageHelper.class.getSimpleName();


    /* Main class variables */
    private final Activity mActivity;


    /* Constructor */
    public StorageHelper(Activity activity) {
        mActivity = activity;
    }


    /* Return a write-able sub-directory from external storage  */
    public File getCollectionDirectory() {
        String subDirectory = "Collection";
        File[] storage = mActivity.getExternalFilesDirs(subDirectory);
        for (File file : storage) {
            if (file != null) {
                String state = EnvironmentCompat.getStorageState(file);
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    Log.i(LOG_TAG, "External storage: " + file.toString());
                    return file;
                }
            }
        }
        Toast.makeText(mActivity, mActivity.getString(R.string.toastalert_no_external_storage), Toast.LENGTH_LONG).show();
        Log.e(LOG_TAG, "Unable to access external storage.");
        // finish activity
        mActivity.finish();

        return null;
    }

}
