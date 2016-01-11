/**
 * PlayerActivity.java
 * Implements the app's player activity
 * The player activity sets up the now playing view and inflates a menubar menu
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;


/**
 * PlayerActivity class
 */
public final class PlayerActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_player_actionbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

}
