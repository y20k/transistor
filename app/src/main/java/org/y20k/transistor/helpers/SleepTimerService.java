/**
 * SleepTimerService.java
 * Implements a sleep timer as a background service
 * The sleep timer stops any running playback after a given time
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-16 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.y20k.transistor.PlayerService;


/**
 * SleepTimerService class
 */
public class SleepTimerService  extends Service {

    /* Define log tag */
    private static final String LOG_TAG = SleepTimerService.class.getSimpleName();


    /* Main class variables */
    private CountDownTimer mSleepTimer;
    private long mTimerRemaining;


    /* Constructor (default) */
    public SleepTimerService() {
    }


    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // checking for empty intent
        if (intent == null) {
            Log.v(LOG_TAG, "Null-Intent received. Stopping self.");
            stopSelf();
        }

        // ACTION TIMER START
        else if (intent.getAction().equals(TransistorKeys.ACTION_TIMER_START)) {
            Log.v(LOG_TAG, "Service received command: START");

            if (intent.hasExtra(TransistorKeys.EXTRA_TIMER_DURATION)) {
                // get duration from intent
                long duration = intent.getLongExtra(TransistorKeys.EXTRA_TIMER_DURATION, 0);

                // set remaining time
                if (mTimerRemaining > 0) {
                    mTimerRemaining = mTimerRemaining + duration;
                } else {
                    mTimerRemaining = duration;
                }

                // set sleep timer
                setSleepTimer(mTimerRemaining);

                // start countdown
                mSleepTimer.start();

                // save timer state to preferences
                saveTimerState(true);
            }

        }

        // ACTION TIMER STOP
        else if (intent.getAction().equals(TransistorKeys.ACTION_TIMER_STOP)) {
            Log.v(LOG_TAG, "Service received command: STOP");

            // set remaining time
            mTimerRemaining = 0;

            // cancel timer
            if (mSleepTimer != null) {
                mSleepTimer.cancel();
            }

            // save timer state to preferences
            saveTimerState(false);

        }

        // START_STICKY is used for services that are explicitly started and stopped as needed
        return START_STICKY;

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    /* Method to start sleep timer */
    public void startActionStart(Context context, long duration) {
        Log.v(LOG_TAG, "Starting sleep timer. Duration: " + duration);

        // start sleep timer service using intent
        Intent intent = new Intent(context, SleepTimerService.class);
        intent.setAction(TransistorKeys.ACTION_TIMER_START);
        intent.putExtra(TransistorKeys.EXTRA_TIMER_DURATION, duration);
        context.startService(intent);
    }


    /* Method to stop sleep timer */
    public void startActionStop(Context context) {
        Log.v(LOG_TAG, "Stopping sleep timer.");

        // stop sleep timer service using intent
        Intent intent = new Intent(context, SleepTimerService.class);
        intent.setAction(TransistorKeys.ACTION_TIMER_STOP);
        context.startService(intent);
    }


    /* Set sleep timer */
    private void setSleepTimer(long duration) {

        if (mTimerRemaining > 0 && mSleepTimer != null) {
            mSleepTimer.cancel();
            mSleepTimer = null;
        }

        // prepare timer
        mSleepTimer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimerRemaining = millisUntilFinished;

                // send local broadcast (needed by PlayerActivityFragment)
                Intent i = new Intent();
                i.setAction(TransistorKeys.ACTION_TIMER_RUNNING);
                i.putExtra(TransistorKeys.EXTRA_TIMER_REMAINING, mTimerRemaining);
                LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(i);

                Log.v(LOG_TAG, "Sleep timer. Remaining time: " + mTimerRemaining);
            }

            @Override
            public void onFinish() {
                mTimerRemaining = 0;

                // stop playback
                Intent intent = new Intent(getApplication(), PlayerService.class);
                intent.setAction(TransistorKeys.ACTION_STOP);
                startService(intent);

                // send local broadcast (needed by PlayerActivityFragment)
                Intent i = new Intent();
                i.setAction(TransistorKeys.ACTION_TIMER_RUNNING);
                i.putExtra(TransistorKeys.EXTRA_TIMER_REMAINING, mTimerRemaining);
                LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(i);

                Log.v(LOG_TAG, "Sleep timer finished. Sweet dreams, dear user.");
            }
        };

    }


    /* save state of timer to shared preferences */
    private void saveTimerState (boolean running) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplication());
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(TransistorKeys.PREF_TIMER_RUNNING, running);
        editor.apply();
        Log.v(LOG_TAG, "Saving state.");
    }




}
