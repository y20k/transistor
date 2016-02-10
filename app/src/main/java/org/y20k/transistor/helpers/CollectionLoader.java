/**
 * CollectionLoader.java
 * Implements a loader for radio stations from storage
 * The loader runs as an AsyncTask
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.y20k.transistor.R;
import org.y20k.transistor.core.Collection;

import java.io.File;


/**
 * CollectionLoader class
 */
final class CollectionLoader extends AsyncTask<Void, Void, Collection> {

    /* Define log tag */
    private static final String LOG_TAG = CollectionLoader.class.getSimpleName();


    /* Main class variables */
    private Collection mCollection;
    private File mFolder;
    private CollectionLoadedListener mCollectionLoadedListener;
    private boolean externalFilesDirDenied;


    /* Interface for custom listener */
    public interface CollectionLoadedListener {
        void collectionLoaded();
    }


    /* Constructor  */
    public CollectionLoader(Context context) {
        try {
            // get collection folder from external storage
            mFolder = new File(context.getExternalFilesDir("Collection").toString());
            externalFilesDirDenied = false;
        } catch (NullPointerException e) {
            // notify user and log exception
            Toast.makeText(context, context.getString(R.string.toastalert_no_external_storage), Toast.LENGTH_LONG).show();
            Log.e(LOG_TAG, "Unable to access external storage.");
            externalFilesDirDenied = true;
        }
        mCollectionLoadedListener = null;
    }


    /* Background thread: reads m3u files from storage */
    @Override
    public Collection doInBackground(Void... params) {
        if (externalFilesDirDenied) {
            return null;
        } else {
            mCollection = new Collection(mFolder);
            return mCollection;
        }
    }


    /* Main thread: set collection and activate listener */
    @Override
    protected void onPostExecute(Collection collection) {
        if (mCollectionLoadedListener != null) {
            mCollection = collection;
            mCollectionLoadedListener.collectionLoaded();
        }
    }


    /* Getter for collection */
    public Collection getCollection() {
        return mCollection;
    }


    /* Setter fpr CollectionLoadedListener */
    public void setCollectionLoadedListener(CollectionLoadedListener collectionLoadedListener) {
        mCollectionLoadedListener = collectionLoadedListener;
    }

}
