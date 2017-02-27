/**
 * InfosheetActivity.java
 * Implements the app's infosheet activity
 * The infosheet activity sets up infosheet screens for "About" and "How to"
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-17 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.y20k.transistor.helpers.TransistorKeys;


/**
 * InfosheetActivity class
 */
public final class InfosheetActivity extends AppCompatActivity implements TransistorKeys  {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get activity title from intent
        Intent intent = this.getIntent();

        // set activity title
        if (intent.hasExtra(EXTRA_INFOSHEET_TITLE)) {
            this.setTitle(intent.getStringExtra(EXTRA_INFOSHEET_TITLE));
        }

        // set activity view
        if (intent.hasExtra(EXTRA_INFOSHEET_CONTENT) && intent.getIntExtra(EXTRA_INFOSHEET_CONTENT, -1) == INFOSHEET_CONTENT_ABOUT) {
            setContentView(R.layout.fragment_infosheet_about);
        } else if (intent.hasExtra(EXTRA_INFOSHEET_CONTENT) && intent.getIntExtra(EXTRA_INFOSHEET_CONTENT, -1) == INFOSHEET_CONTENT_HOWTO) {
            setContentView(R.layout.fragment_infosheet_howto);
        }

    }

}
