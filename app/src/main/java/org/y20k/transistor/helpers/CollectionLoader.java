/**
 * CollectionLoader.java
 * Implements a loader for radio stations from storage
 * The loader runs as an AsyncTask
 * <p/>
 * This file is part of
 * TRANSISTOR - Radio App for Android
 * <p/>
 * Copyright (c) 2015 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.content.Context;
import android.os.AsyncTask;

import org.y20k.transistor.core.Collection;

import java.io.File;


/**
 * CollectionLoader class
 */
public class CollectionLoader extends AsyncTask<Void, Void, Collection> {

    /* Define log tag */
    public final String LOG_TAG = CollectionLoader.class.getSimpleName();

    /* Keys */

    /* Main class variables */
    private Collection mCollection;
    private File mFolder;
    private CollectionLoadedListener mCollectionLoadedListener;


    /* Interface for custom listener */
    public interface CollectionLoadedListener {
        void collectionLoaded();
    }


    /* Constructor  */
    public CollectionLoader(Context context) {
        mFolder = new File(context.getExternalFilesDir("Collection").toString());
        mCollectionLoadedListener = null;
    }


    /* Background thread: reads m3u files from storage */
    @Override
    public Collection doInBackground(Void... params) {
        mCollection = new Collection(mFolder);
        return mCollection;
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
