/**
 * MainActivity.java
 * Implements the app's main activity
 * The main activity sets up the main view end inflates a menubar menu
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.widget.Toast;

import org.y20k.transistor.core.Collection;
import org.y20k.transistor.core.Station;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * MainActivity class
 */
public class MainActivity extends ActionBarActivity {

    /* Main class variables */
    private Collection mCollection;
    private URL mNewStationURL;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set up variables
        mCollection = null;
        mNewStationURL = null;

        // handle incoming content
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        // let Transistor react to external clicks on m3u links
        if (Intent.ACTION_VIEW.equals(action) && type.startsWith("audio/")) {
            String intentText = intent.getData().toString();
            try {
                mNewStationURL = new URL(intentText);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            Toast.makeText(getApplication(), R.string.alertmessage_add_download_started + intentText, Toast.LENGTH_LONG).show();

            AddStationFromIntent loadCollection = new AddStationFromIntent();
            loadCollection.execute();
        }

        setContentView(R.layout.activity_main);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_actionbar, menu);
        return true;
    }


    /**
     * Inner class: add new to mCollection of radio stations using background thread
     */
    public class AddStationFromIntent extends AsyncTask<Void, Void, Collection> {

        /* Main class variables */
        private File folder = null;

        /* Background thread: reads m3u files from storage */
        @Override
        public Collection doInBackground(Void... params) {
            folder = new File(getApplication().getExternalFilesDir("Collection").toString());
            mCollection = new Collection(folder);
            Station newStation = new Station(folder, mNewStationURL);
            mCollection.add(newStation);
            return mCollection;
        }

        /* Main thread: do nothing for now */
        @Override
        protected void onPostExecute(Collection stations) {
            // do nothing for now
        }
    }
    /**
     * End of inner class
     */

}
