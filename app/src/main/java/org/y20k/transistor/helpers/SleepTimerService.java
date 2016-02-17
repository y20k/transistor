/**
 * SleepTimerService.java
 * Implements a sleep timer as a background service
 * The sleep timer stops any running playback after a given time
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.y20k.transistor.PlayerService;


/**
 * SleepTimerService class
 */
public class SleepTimerService  extends Service {


    /* Define log tag */
    private static final String LOG_TAG = SleepTimerService.class.getSimpleName();


    /* Keys */
    private static final String ACTION_PLAY = "org.y20k.transistor.action.PLAY";
    private static final String ACTION_STOP = "org.y20k.transistor.action.STOP";
    private static final String ACTION_TIMER_START = "org.y20k.transistor.action.TIMER_START";
    private static final String ACTION_TIMER_STOP = "org.y20k.transistor.action.TIMER_STOP";
    private static final String ACTION_TIMER_RUNNING = "org.y20k.transistor.action.TIMER_RUNNING";
    private static final String EXTRA_TIMER_DURATION = "TIMER_DURATION";
    private static final String EXTRA_TIMER_REMAINING = "TIMER_REMAINING";


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
        else if (intent.getAction().equals(ACTION_TIMER_START)) {
            Log.v(LOG_TAG, "Service received command: START");

            if (intent.hasExtra(EXTRA_TIMER_DURATION)) {
                // get duration from intent
                long duration = intent.getLongExtra(EXTRA_TIMER_DURATION, 0);

                // set remaining time
                if (mTimerRemaining > 0) {
                    mTimerRemaining = mTimerRemaining + duration;
                    Toast.makeText(getApplication(), "Added " + duration + " milliseconds.", Toast.LENGTH_LONG).show();
                } else {
                    mTimerRemaining = duration;
                }

                // set sleep timer
                setSleepTimer(mTimerRemaining);

                // start countdown
                mSleepTimer.start();
            }

        }

        // ACTION TIMER STOP
        else if (intent.getAction().equals(ACTION_TIMER_STOP)) {
            Log.v(LOG_TAG, "Service received command: STOP");

            if (mSleepTimer != null) {
                mSleepTimer.cancel();
            }

        }

        // START_STICKY is used for services that are explicitly started and stopped as needed
        return START_STICKY;

    }
    // TODO remove this
    // For started services, there are two additional major modes of operation they can decide to run in,
    // depending on the value they return from onStartCommand(): START_STICKY is used for services that are explicitly
    // started and stopped as needed, while START_NOT_STICKY or START_REDELIVER_INTENT are used for services that should
    // only remain running while processing any commands sent to them. See the linked documentation for more detail on the semantics.


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
        intent.setAction(ACTION_TIMER_START);
        intent.putExtra(EXTRA_TIMER_DURATION, duration);
        context.startService(intent);
    }


    /* Method to stop sleep timer */
    public void startActionStop(Context context) {
        Log.v(LOG_TAG, "Stopping sleep timer.");

        // stop sleep timer service using intent
        Intent intent = new Intent(context, SleepTimerService.class);
        intent.setAction(ACTION_TIMER_STOP);
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
                i.setAction(ACTION_TIMER_RUNNING);
                i.putExtra(EXTRA_TIMER_REMAINING, mTimerRemaining);
                LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(i);

                Log.v(LOG_TAG, "Sleep timer. Remaining time: " + mTimerRemaining);
            }

            @Override
            public void onFinish() {
                mTimerRemaining = 0;

                // stop playback
                Intent intent = new Intent(getApplication(), PlayerService.class);
                intent.setAction(ACTION_STOP);
                startService(intent);

                // send local broadcast (needed by PlayerActivityFragment)
                Intent i = new Intent();
                i.setAction(ACTION_TIMER_RUNNING);
                i.putExtra(EXTRA_TIMER_REMAINING, mTimerRemaining);
                LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(i);

                Log.v(LOG_TAG, "Sleep timer finished. Sweet dreams, dear user.");
            }
        };

    }




}
