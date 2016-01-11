/**
 * InfosheetActivity.java
 * Implements the app's infosheet activity
 * The infosheet activity sets up the infosheet html textview
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
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


/**
 * InfosheetActivity class
 */
public final class InfosheetActivity extends AppCompatActivity {

    /* Keys */
    private static final String TITLE = "title";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get activity title from intent
        Intent intent = this.getIntent();
        String title = intent.getExtras().getString(TITLE);

        // set activity title
        if (title != null) {
            this.setTitle(title);
        }

        // set view
        setContentView(R.layout.activity_infosheet);
    }

}
