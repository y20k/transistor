/**
 * StorageHelper.java
 * Implements the StorageHelper class
 * A StorageHelper provides reliable access to Androids external storage
 * <p>
 * This file is part of
 * TRANSISTOR - Radio App for Android
 * <p>
 * Copyright (c) 2015-17 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.support.v4.os.EnvironmentCompat;

import java.io.File;


/**
 * StorageHelper class
 */
public class StorageHelper {

    /* Define log tag */
    private static final String LOG_TAG = StorageHelper.class.getSimpleName();


    /* Main class variables */
    private final Activity mActivity;
    private final Context mContext;


    /* Constructor */
    public StorageHelper(Activity activity) {
        mActivity = activity;
        mContext=null;
    }

    public StorageHelper(Context context) {
        mContext = context;
        mActivity=null;

    }

    /* Return a write-able sub-directory from external storage  */
    public File getCollectionDirectory() {
        String subDirectory = "Collection";
        File[] storage = (mActivity!=null)? mActivity.getExternalFilesDirs(subDirectory):mContext.getExternalFilesDirs(subDirectory);
        for (File file : storage) {
            if (file != null) {
                String state = EnvironmentCompat.getStorageState(file);
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    LogHelper.i(LOG_TAG, "External storage: " + file.toString());
                    return file;
                }
            }
        }
        return null;
    }

}
