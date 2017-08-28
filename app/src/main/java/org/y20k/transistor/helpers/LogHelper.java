/**
 * LogHelper.java
 * Implements the LogHelper class
 * A LogHelper wraps the logging calls to be able to strip them out of release versions
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-17 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.support.v4.BuildConfig;
import android.util.Log;


/**
 * LogHelper class
 */
public final class LogHelper {

    private final static boolean mTesting = false;

    public static void d(final String tag, String message) {
        // include logging only in debug versions
        if (BuildConfig.DEBUG || mTesting) {
            Log.d(tag, message);
        }
    }


    public static void v(final String tag, String message) {
        // include logging only in debug versions
        if (BuildConfig.DEBUG || mTesting) {
            Log.v(tag, message);
        }
    }


    public static void e(final String tag, String message) {
        Log.e(tag, message);
    }


    public static void i(final String tag, String message) {
        Log.i(tag, message);
    }


    public static void w(final String tag, String message) {
        Log.w(tag, message);
    }

}
