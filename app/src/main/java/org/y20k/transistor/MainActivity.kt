package org.y20k.transistor

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController

import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController

import androidx.preference.PreferenceManager
import org.y20k.transistor.helpers.AppThemeHelper
import org.y20k.transistor.helpers.FileHelper
import org.y20k.transistor.helpers.LogHelper
import org.y20k.transistor.helpers.PreferencesHelper

class MainActivity: AppCompatActivity() {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(MainActivity::class.java)


    /* Main class variables */
    private lateinit var appBarConfiguration: AppBarConfiguration


    /* Overrides onCreate from AppCompatActivity */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set up views
        setContentView(R.layout.activity_main)

        // create .nomedia file - if not yet existing
        FileHelper.createNomediaFile(getExternalFilesDir(null))

        // set up action bar
        setSupportActionBar(findViewById(R.id.main_toolbar))
        val navController = findNavController(R.id.main_host_container)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // register listener for changes in shared preferences
        PreferenceManager.getDefaultSharedPreferences(this as Context).registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }


    /* Overrides onSupportNavigateUp from AppCompatActivity */
    override fun onSupportNavigateUp(): Boolean {
        // Taken from: https://developer.android.com/guide/navigation/navigation-ui#action_bar
        val navController = findNavController(R.id.main_host_container)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }


    /* Overrides onDestroy from AppCompatActivity */
    override fun onDestroy() {
        super.onDestroy()
        // unregister listener for changes in shared preferences
        PreferenceManager.getDefaultSharedPreferences(this as Context).unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }


    /*
     * Defines the listener for changes in shared preferences
     */
    private val sharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            Keys.PREF_THEME_SELECTION -> {
                AppThemeHelper.setTheme(PreferencesHelper.loadThemeSelection(this@MainActivity))
            }
        }
    }
    /*
     * End of declaration
     */



}