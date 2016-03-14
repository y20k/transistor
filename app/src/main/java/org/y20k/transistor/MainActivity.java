/**
 * MainActivity.java
 * Implements the app's main activity
 * The main activity sets up the main view end inflates a menubar menu
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-16 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */

package org.y20k.transistor;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.os.EnvironmentCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Toast;

import org.y20k.transistor.core.Collection;

import java.io.File;


/**
 * MainActivity class
 */
public final class MainActivity extends AppCompatActivity {

    /* Define log tag */
    private static final String LOG_TAG = MainActivity.class.getSimpleName();


    /* Keys */
    private static final String PLAYERFRAGMENT_TAG = "PFTAG";
    private static final String TWOPANE = "twopane";


    /* Main class variables */
    private boolean mTwoPane;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Collection collection = new Collection(getCollectionDirectory("Collection"));

        // if player_container is present two-pane layout has been loaded
        if (findViewById(R.id.player_container) != null) {
            mTwoPane = true;
            if (savedInstanceState == null && !collection.getStations().isEmpty()) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.player_container, new PlayerActivityFragment(), PLAYERFRAGMENT_TAG)
                        .commit();
            } else {
                findViewById(R.id.player_container).setVisibility(View.GONE);
            }
        } else {
            mTwoPane = false;
        }

        saveAppState(this);

        Configuration configuration = this.getResources().getConfiguration();
        int screenWidthDp = configuration.screenWidthDp;
        int smallestScreenWidthDp = configuration.smallestScreenWidthDp;
        Log.d("TAG", "width: " + String.valueOf(screenWidthDp) + " height: " + String.valueOf(smallestScreenWidthDp));



    }


    @Override
    protected void onResume() {
        super.onResume();

        // TODO Replace with collection changed listener
        Collection collection = new Collection(getCollectionDirectory("Collection"));
        View container = findViewById(R.id.player_container);
        if (collection.getStations().isEmpty() && container != null) {
            container.setVisibility(View.GONE);
        } else if (container != null) {
            container.setVisibility(View.VISIBLE);
        }

    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // activity opened for second time set intent to new intent
        setIntent(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main_actionbar, menu);
        return super.onCreateOptionsMenu(menu);
    }


    // Workaround for an IllegalStateException crash (Fragment not attached to Activity)
    // See: https://github.com/y20k/transistor/issues/21
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_main);
        // hand results over to fragment main
        fragment.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_main);
        // hand results over to fragment main
        fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }


    /* Saves app state to SharedPreferences */
    private void saveAppState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(TWOPANE, mTwoPane);
        editor.apply();
        Log.v(LOG_TAG, "Saving state.");
    }


    /* Return a writeable sub-directory from external storage  */
    private File getCollectionDirectory(String subDirectory) {
        File[] storage = this.getExternalFilesDirs(subDirectory);
        for (File file : storage) {
            String state = EnvironmentCompat.getStorageState(file);
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                Log.i(LOG_TAG, "External storage: " + file.toString());
                return file;
            }
        }
        Toast.makeText(this, this.getString(R.string.toastalert_no_external_storage), Toast.LENGTH_LONG).show();
        Log.e(LOG_TAG, "Unable to access external storage.");
        // finish activity
        this.finish();

        return null;
    }

}