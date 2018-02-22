/**
 * NightModeHelper.java
 * Implements the NightModeHelper class
 * A NightModeHelper can toggle and restore the state of the theme's Night Mode
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-18 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */

package org.y20k.transistor.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDelegate;
import android.view.View;


/**
 * NightModeHelper class
 */
public final class NightModeHelper implements TransistorKeys {


    /* Define log tag */
    private static final String LOG_TAG = NightModeHelper.class.getSimpleName();


    /* Switches to opposite theme */
    public static void switchToOpposite(Activity activity) {
        switch (getCurrentNightModeState(activity)) {
            case Configuration.UI_MODE_NIGHT_NO:
                // night mode is currently not active - turn on night mode
                activateNightMode(activity);
                break;
            case Configuration.UI_MODE_NIGHT_YES:
                // night mode is currently active - turn off night mode
                deactivateNightMode(activity);
                break;
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                // don't know what mode is active - turn off night mode
                deactivateNightMode(activity);
                break;
        }
        activity.recreate(); // todo check if necessary
    }


    /* Sets night mode / dark theme */
    public static void restoreSavedState(Activity activity) {
        int savedNightModeState = loadNightModeState(activity);
        int currentNightModeState = getCurrentNightModeState(activity);
        if (savedNightModeState != -1 && savedNightModeState != currentNightModeState) {
            switch (savedNightModeState) {
                case Configuration.UI_MODE_NIGHT_NO:
                     // turn off night mode
                    deactivateNightMode(activity);
                    break;
                case Configuration.UI_MODE_NIGHT_YES:
                    // turn on night mode
                    activateNightMode(activity);
                    break;
                case Configuration.UI_MODE_NIGHT_UNDEFINED:
                    // turn off night mode
                    deactivateNightMode(activity);
                    break;
            }
            activity.recreate(); // todo check if necessary
        }
    }


    /* Returns state of night mode */
    private static int getCurrentNightModeState(Context context) {
        return context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    }


    /* Activates Night Mode */
    private static void activateNightMode(Activity activity) {
        saveNightModeState(activity, Configuration.UI_MODE_NIGHT_YES);

        // revert to normal status bar
        View decorView = activity.getWindow().getDecorView();
        decorView.setSystemUiVisibility(0);

        // switch to Nighh Mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }


    /* Deactivates Night Mode */
    private static void deactivateNightMode(Activity activity) {
        // save the new state
        saveNightModeState(activity, Configuration.UI_MODE_NIGHT_NO);

        // switch to white status bar - if possible
        View decorView = activity.getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            decorView.setSystemUiVisibility(0);
        }

        // switch to Day Mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }


    /* Save state of night mode */
    private static void saveNightModeState(Context context, int currentState) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(PREF_NIGHT_MODE_STATE, currentState);
        editor.apply();
    }


    /* Load state of Night Mode */
    private static int loadNightModeState(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(PREF_NIGHT_MODE_STATE, -1);
    }

}

