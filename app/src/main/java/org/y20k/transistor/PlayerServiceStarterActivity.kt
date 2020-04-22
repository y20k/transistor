/*
 * PlayerServiceStarterActivity.kt
 * Implements the PlayerServiceStarterActivity class
 * A PlayerServiceStarterActivity simply starts the PlayerService
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-20 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor

import android.app.Activity
import android.content.Intent
import android.os.Bundle


/*
 * PlayerServiceStarterActivity class
 */
class PlayerServiceStarterActivity: Activity() {

    /* Overrides onCreate from Activity */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action == Keys.ACTION_START_PLAYER_SERVICE) {
            // start player service and start playback
            val startIntent = Intent(this, PlayerService::class.java)
            startIntent.action = Keys.ACTION_START
            if (intent.hasExtra(Keys.EXTRA_STATION_UUID)) {
                // start station with uuid from intent
                startIntent.putExtra(Keys.EXTRA_STATION_UUID, intent.getStringExtra(Keys.EXTRA_STATION_UUID))
            }
            startService(startIntent)
        }
        finish()
    }

}