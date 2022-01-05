/*
 * Transistor.kt
 * Implements the Transistor class
 * Transistor is the base Application class that sets up day and night theme
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-22 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor

import android.app.Application
import org.y20k.transistor.helpers.AppThemeHelper
import org.y20k.transistor.helpers.LogHelper
import org.y20k.transistor.helpers.PreferencesHelper
import org.y20k.transistor.helpers.PreferencesHelper.initPreferences


/**
 * Transistor.class
 */
class Transistor: Application () {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(Transistor::class.java)


    /* Implements onCreate */
    override fun onCreate() {
        super.onCreate()
        LogHelper.v(TAG, "Transistor application started.")
        initPreferences()
        // set Dark / Light theme state
        AppThemeHelper.setTheme(PreferencesHelper.loadThemeSelection())
    }


    /* Implements onTerminate */
    override fun onTerminate() {
        super.onTerminate()
        LogHelper.v(TAG, "Transistor application terminated.")
    }

}
