/**
 * NightModeHelper.java
 * Implements the NightModeHelper class
 * A NightModeHelper can toggle and restore the state of the theme's Night Mode
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-19 - Y20K.org
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
import android.view.View;
import android.widget.Toast;

import org.y20k.transistor.R;

import androidx.appcompat.app.AppCompatDelegate;


/**
 * NightModeHelper class
 */
public final class NightModeHelper implements TransistorKeys {

    /* Define log tag */
    private static final String LOG_TAG = NightModeHelper.class.getSimpleName();


    /* Switches between modes: day, night, undefined */
    public static void switchMode(Activity activity) {
        // SWITCH: undefined -> night / night -> day / day - undefined
        switch (AppCompatDelegate.getDefaultNightMode()) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                // currently: day mode -> switch to: follow system
                displayDefaultStatusBar(activity); // necessary hack :-/
                activateFollowSystemMode(activity, true);
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                // currently: night mode -> switch to: day mode
                displayLightStatusBar(activity); // necessary hack :-/
                activateDayMode(activity, true);
                break;
            default:
                // currently: follow system / undefined -> switch to: day mode
                displayLightStatusBar(activity); // necessary hack :-/
                activateNightMode(activity, true);
                break;
        }
    }


    /* Sets night mode / dark theme */
    public static void restoreSavedState(Context context) {
        int savedNightModeState = loadNightModeState(context);
        int currentNightModeState = AppCompatDelegate.getDefaultNightMode();
        if (savedNightModeState != currentNightModeState) {
            switch (savedNightModeState) {
                case AppCompatDelegate.MODE_NIGHT_NO:
                    // turn on day mode
                    activateDayMode(context, false);
                    break;
                case AppCompatDelegate.MODE_NIGHT_YES:
                    // turn on night mode
                    activateNightMode(context, false);
                    break;
                default:
                    // turn on mode "follow system"
                    activateFollowSystemMode(context, false);
                    break;
            }
        }
    }


    /* Returns state of night mode */
    private static int getCurrentNightModeState(Context context) {
        return context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    }


    /* Activates Night Mode */
    private static void activateNightMode(Context context, Boolean notifyUser) {
        saveNightModeState(context, AppCompatDelegate.MODE_NIGHT_YES);

        // switch to Night Mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        // notify user
        if (notifyUser) {
            Toast.makeText(context, context.getText(R.string.toastmessage_theme_night), Toast.LENGTH_LONG).show();
        }
    }


    /* Activates Day Mode */
    private static void activateDayMode(Context context, Boolean notifyUser) {
        // save the new state
        saveNightModeState(context, AppCompatDelegate.MODE_NIGHT_NO);

        // switch to Day Mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // notify user
        if (notifyUser) {
            Toast.makeText(context, context.getText(R.string.toastmessage_theme_day), Toast.LENGTH_LONG).show();
        }
    }


    /* Activate Mode "Follow System" */
    private static void activateFollowSystemMode(Context context, Boolean notifyUser) {
        // save the new state
        saveNightModeState(context, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // switch to Undefined Mode / Follow System
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // notify user
        if (notifyUser) {
            Toast.makeText(context, context.getText(R.string.toastmessage_theme_follow_system), Toast.LENGTH_LONG).show();
        }
    }


    /* Displays the default status bar */
    private static void displayDefaultStatusBar(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        decorView.setSystemUiVisibility(0);
    }


    /* Displays the light (inverted) status bar - if possible */
    private static void displayLightStatusBar(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            decorView.setSystemUiVisibility(0);
        }
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
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(PREF_NIGHT_MODE_STATE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

}

