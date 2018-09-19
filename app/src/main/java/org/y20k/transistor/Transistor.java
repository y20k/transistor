/**
 * Transistor.java
 * Implements the Transistor class
 * Transistor starts up the app and sets up the basic theme (Day / Night)
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-18 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */

package org.y20k.transistor;

import android.app.Application;
import android.os.Build;

import org.y20k.transistor.helpers.LogHelper;
import org.y20k.transistor.helpers.NightModeHelper;

import androidx.appcompat.app.AppCompatDelegate;


/**
 * Transistor.class
 */
public class Transistor extends Application {

    /* Define log tag */
    private static final String LOG_TAG = Transistor.class.getSimpleName();


    @Override
    public void onCreate() {
        super.onCreate();

        // set Day / Night theme state
        if (Build.VERSION.SDK_INT >= 28) {
            // Android P might introduce a system wide theme option - in that case: follow system (28 = Build.VERSION_CODES.P)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else {
            // try t0 get last state the user chose
            NightModeHelper.restoreSavedState(this);
        }

    }


    @Override
    public void onTerminate() {
        super.onTerminate();
        LogHelper.v(LOG_TAG, "Transistor application terminated.");
    }

}
