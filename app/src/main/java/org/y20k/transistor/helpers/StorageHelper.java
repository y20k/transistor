/**
 * StorageHelper.java
 * Implements the StorageHelper class
 * A StorageHelper provides reliable access to Androids external storage
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-17 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

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
    private final Context mActivity;
    private File mCollectionDirectory;


    /* Constructor */
    public StorageHelper(Context activity) {
        mActivity = activity;
        mCollectionDirectory = findCollectionDirectory();
    }


    /* Getter for collection directory */
    public File getCollectionDirectory() {
        return mCollectionDirectory;
    }


    /* Checks if given folder holds any m3u files */
    public boolean storageHasStationPlaylistFiles() {
        if (!mCollectionDirectory.isDirectory()) {
            LogHelper.i(LOG_TAG, "Given file object is not a directory.");
            return false;
        }
        File[] listOfFiles = mCollectionDirectory.listFiles();
        for (File file : listOfFiles) {
            if (file.getPath().endsWith(".m3u")) {
                return true;
            }
        }
        LogHelper.i(LOG_TAG, "External storage does not contain any station playlist files.");
        return false;
    }


    /* Return a write-able sub-directory from external storage  */
    private File findCollectionDirectory() {
        String subDirectory = "Collection";
        File[] storage = mActivity.getExternalFilesDirs(subDirectory);
        for (File file : storage) {
            if (file != null) {
                String state = EnvironmentCompat.getStorageState(file);
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    LogHelper.v(LOG_TAG, "External storage: " + file.toString());
                    return file;
                }
            }
        }
        return null;
    }




}
