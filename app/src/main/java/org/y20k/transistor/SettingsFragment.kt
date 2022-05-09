/*
 * SettingsFragment.kt
 * Implements the SettingsFragment fragment
 * A SettingsFragment displays the user accessible settings of the app
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-22 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.preference.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.y20k.transistor.dialogs.ErrorDialog
import org.y20k.transistor.dialogs.YesNoDialog
import org.y20k.transistor.helpers.*


/*
 * SettingsFragment class
 */
class SettingsFragment: PreferenceFragmentCompat(), YesNoDialog.YesNoDialogListener {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(SettingsFragment::class.java)


    /* Overrides onViewCreated from PreferenceFragmentCompat */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // set the background color
        view.setBackgroundColor(resources.getColor(R.color.app_window_background, null))
        // show action bar
        (activity as AppCompatActivity).supportActionBar?.show()
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.fragment_settings_title)
    }


    /* Overrides onCreatePreferences from PreferenceFragmentCompat */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)


        // set up "App Theme" preference
        val preferenceThemeSelection: ListPreference = ListPreference(activity as Context)
        preferenceThemeSelection.title = getString(R.string.pref_theme_selection_title)
        preferenceThemeSelection.setIcon(R.drawable.ic_smartphone_24dp)
        preferenceThemeSelection.key = Keys.PREF_THEME_SELECTION
        preferenceThemeSelection.summary = "${getString(R.string.pref_theme_selection_summary)} ${AppThemeHelper.getCurrentTheme(activity as Context)}"
        preferenceThemeSelection.entries = arrayOf(getString(R.string.pref_theme_selection_mode_device_default), getString(R.string.pref_theme_selection_mode_light), getString(R.string.pref_theme_selection_mode_dark))
        preferenceThemeSelection.entryValues = arrayOf(Keys.STATE_THEME_FOLLOW_SYSTEM, Keys.STATE_THEME_LIGHT_MODE, Keys.STATE_THEME_DARK_MODE)
        preferenceThemeSelection.setDefaultValue(Keys.STATE_THEME_FOLLOW_SYSTEM)
        preferenceThemeSelection.setOnPreferenceChangeListener { preference, newValue ->
            if (preference is ListPreference) {
                val index: Int = preference.entryValues.indexOf(newValue)
                preferenceThemeSelection.summary = "${getString(R.string.pref_theme_selection_summary)} ${preference.entries[index]}"
                return@setOnPreferenceChangeListener true
            } else {
                return@setOnPreferenceChangeListener false
            }
        }

        // set up "Update Station Images" preference
        val preferenceUpdateStationImages: Preference = Preference(activity as Context)
        preferenceUpdateStationImages.title = getString(R.string.pref_update_station_images_title)
        preferenceUpdateStationImages.setIcon(R.drawable.ic_image_24dp)
        preferenceUpdateStationImages.summary = getString(R.string.pref_update_station_images_summary)
        preferenceUpdateStationImages.setOnPreferenceClickListener {
            // show dialog
            YesNoDialog(this).show(context = activity as Context, type = Keys.DIALOG_UPDATE_STATION_IMAGES, message = R.string.dialog_yes_no_message_update_station_images, yesButton = R.string.dialog_yes_no_positive_button_update_covers)
            return@setOnPreferenceClickListener true
        }


//        // set up "Update Stations" preference
//        val preferenceUpdateCollection: Preference = Preference(activity as Context)
//        preferenceUpdateCollection.title = getString(R.string.pref_update_collection_title)
//        preferenceUpdateCollection.setIcon(R.drawable.ic_refresh_24dp)
//        preferenceUpdateCollection.summary = getString(R.string.pref_update_collection_summary)
//        preferenceUpdateCollection.setOnPreferenceClickListener {
//            // show dialog
//            YesNoDialog(this).show(context = activity as Context, type = Keys.DIALOG_UPDATE_COLLECTION, message = R.string.dialog_yes_no_message_update_collection, yesButton = R.string.dialog_yes_no_positive_button_update_collection)
//            return@setOnPreferenceClickListener true
//        }


        // set up "M3U Export" preference
        val preferenceM3uExport: Preference = Preference(activity as Context)
        preferenceM3uExport.title = getString(R.string.pref_m3u_export_title)
        preferenceM3uExport.setIcon(R.drawable.ic_playlist_24dp)
        preferenceM3uExport.summary = getString(R.string.pref_m3u_export_summary)
        preferenceM3uExport.setOnPreferenceClickListener {
            openSaveM3uDialog()
            return@setOnPreferenceClickListener true
        }


        // set up "Backup Stations" preference
        val preferenceBackupCollection: Preference = Preference(activity as Context)
        preferenceBackupCollection.title = getString(R.string.pref_backup_title)
        preferenceBackupCollection.setIcon(R.drawable.ic_save_24dp)
        preferenceBackupCollection.summary = getString(R.string.pref_backup_summary)
        preferenceBackupCollection.setOnPreferenceClickListener {
            openBackupCollectionDialog()
            return@setOnPreferenceClickListener true
        }


        // set up "Restore Stations" preference
        val preferenceRestoreCollection: Preference = Preference(activity as Context)
        preferenceRestoreCollection.title = getString(R.string.pref_restore_title)
        preferenceRestoreCollection.setIcon(R.drawable.ic_restore_24dp)
        preferenceRestoreCollection.summary = getString(R.string.pref_restore_summary)
        preferenceRestoreCollection.setOnPreferenceClickListener {
            openRestoreCollecionDialog()
            return@setOnPreferenceClickListener true
        }


        // set up "Edit Stream Address" preference
        val preferenceEnableEditingStreamUri: SwitchPreferenceCompat = SwitchPreferenceCompat(activity as Context)
        preferenceEnableEditingStreamUri.title = getString(R.string.pref_edit_station_stream_title)
        preferenceEnableEditingStreamUri.setIcon(R.drawable.ic_music_note_24dp)
        preferenceEnableEditingStreamUri.key = Keys.PREF_EDIT_STREAMS_URIS
        preferenceEnableEditingStreamUri.summaryOn = getString(R.string.pref_edit_station_stream_summary_enabled)
        preferenceEnableEditingStreamUri.summaryOff = getString(R.string.pref_edit_station_stream_summary_disabled)
        preferenceEnableEditingStreamUri.setDefaultValue(PreferencesHelper.loadEditStreamUrisEnabled())


        // set up "Edit Stations" preference
        val preferenceEnableEditingGeneral: SwitchPreferenceCompat = SwitchPreferenceCompat(activity as Context)
        preferenceEnableEditingGeneral.title = getString(R.string.pref_edit_station_title)
        preferenceEnableEditingGeneral.setIcon(R.drawable.ic_edit_24dp)
        preferenceEnableEditingGeneral.key = Keys.PREF_EDIT_STATIONS
        preferenceEnableEditingGeneral.summaryOn = getString(R.string.pref_edit_station_summary_enabled)
        preferenceEnableEditingGeneral.summaryOff = getString(R.string.pref_edit_station_summary_disabled)
        preferenceEnableEditingGeneral.setDefaultValue(PreferencesHelper.loadEditStationsEnabled())
        preferenceEnableEditingGeneral.setOnPreferenceChangeListener { preference, newValue ->
            when (newValue) {
                true -> {
                    preferenceEnableEditingStreamUri.isEnabled = true
                }
                false -> {
                    preferenceEnableEditingStreamUri.isEnabled = false
                    preferenceEnableEditingStreamUri.isChecked = false
                }
            }
            return@setOnPreferenceChangeListener true
        }


        // set up "App Version" preference
        val preferenceAppVersion: Preference = Preference(context)
        preferenceAppVersion.title = getString(R.string.pref_app_version_title)
        preferenceAppVersion.setIcon(R.drawable.ic_info_24dp)
        preferenceAppVersion.summary = "${getString(R.string.pref_app_version_summary)} ${BuildConfig.VERSION_NAME} (${getString(R.string.app_version_name)})"
        preferenceAppVersion.setOnPreferenceClickListener {
            // copy to clipboard
            val clip: ClipData = ClipData.newPlainText("simple text", preferenceAppVersion.summary)
            val cm: ClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(clip)
            Toast.makeText(activity as Context, R.string.toastmessage_copied_to_clipboard, Toast.LENGTH_LONG).show()
            return@setOnPreferenceClickListener true
        }


        // set up "Report Issue" preference
        val preferenceReportIssue: Preference = Preference(context)
        preferenceReportIssue.title = getString(R.string.pref_report_issue_title)
        preferenceReportIssue.setIcon(R.drawable.ic_bug_report_24dp)
        preferenceReportIssue.summary = getString(R.string.pref_report_issue_summary)
        preferenceReportIssue.setOnPreferenceClickListener {
            // open web browser
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                data = "https://github.com/y20k/transistor/issues".toUri()
            }
            startActivity(intent)
            return@setOnPreferenceClickListener true
        }


        // set preference categories
        val preferenceCategoryGeneral: PreferenceCategory = PreferenceCategory(activity as Context)
        preferenceCategoryGeneral.title = getString(R.string.pref_general_title)
        preferenceCategoryGeneral.contains(preferenceThemeSelection)

        val preferenceCategoryMaintenance: PreferenceCategory = PreferenceCategory(activity as Context)
        preferenceCategoryMaintenance.title = getString(R.string.pref_maintenance_title)
        preferenceCategoryMaintenance.contains(preferenceUpdateStationImages)
//        preferenceCategoryMaintenance.contains(preferenceUpdateCollection)
        preferenceCategoryMaintenance.contains(preferenceM3uExport)
        preferenceCategoryMaintenance.contains(preferenceBackupCollection)
        preferenceCategoryMaintenance.contains(preferenceRestoreCollection)

        val preferenceCategoryAdvanced: PreferenceCategory = PreferenceCategory(activity as Context)
        preferenceCategoryAdvanced.title = getString(R.string.pref_advanced_title)
        preferenceCategoryAdvanced.contains(preferenceEnableEditingGeneral)
        preferenceCategoryAdvanced.contains(preferenceEnableEditingStreamUri)

        val preferenceCategoryAbout: PreferenceCategory = PreferenceCategory(context)
        preferenceCategoryAbout.title = getString(R.string.pref_about_title)
        preferenceCategoryAbout.contains(preferenceAppVersion)
        preferenceCategoryAbout.contains(preferenceReportIssue)


        // setup preference screen
        screen.addPreference(preferenceCategoryGeneral)
        screen.addPreference(preferenceThemeSelection)
        screen.addPreference(preferenceCategoryMaintenance)
        screen.addPreference(preferenceUpdateStationImages)
//        screen.addPreference(preferenceUpdateCollection)
        screen.addPreference(preferenceM3uExport)
        screen.addPreference(preferenceBackupCollection)
        screen.addPreference(preferenceRestoreCollection)
        screen.addPreference(preferenceCategoryAdvanced)
        screen.addPreference(preferenceEnableEditingGeneral)
        screen.addPreference(preferenceEnableEditingStreamUri)

        screen.addPreference(preferenceCategoryAbout)
        screen.addPreference(preferenceAppVersion)
        screen.addPreference(preferenceReportIssue)
        preferenceScreen = screen
    }



    /* Overrides onYesNoDialog from YesNoDialogListener */
    override fun onYesNoDialog(type: Int, dialogResult: Boolean, payload: Int, payloadString: String) {
        super.onYesNoDialog(type, dialogResult, payload, payloadString)

        when (type) {

            Keys.DIALOG_UPDATE_STATION_IMAGES -> {
                if (dialogResult) {
                    // user tapped: refresh station images
                    updateStationImages()
                }
            }

            Keys.DIALOG_UPDATE_COLLECTION -> {
                if (dialogResult) {
                    // user tapped update collection
                    updateCollection()
                }
            }

        }

    }


    /* Register the ActivityResultLauncher for the save m3u dialog */
    private val requestSaveM3uLauncher = registerForActivityResult(StartActivityForResult(), this::requestSaveM3uResult)


    /* Register the ActivityResultLauncher for the backup dialog */
    private val requestBackupCollectionLauncher = registerForActivityResult(StartActivityForResult(), this::requestBackupCollecionResult)


    /* Register the ActivityResultLauncher for the restore dialog */
    private val requestRestoreCollectionLauncher = registerForActivityResult(StartActivityForResult(), this::requestRestoreCollectionResult)


    /* Pass the activity result for the save m3u dialog */
    private fun requestSaveM3uResult(result: ActivityResult) {
        // save M3U file to result file location
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val sourceUri: Uri? = FileHelper.getM3ulUri(activity as Activity)
            val targetUri: Uri? = result.data?.data
            if (targetUri != null && sourceUri != null) {
                // copy file async (= fire & forget - no return value needed)
                CoroutineScope(IO).launch {
                    FileHelper.saveCopyOfFileSuspended(activity as Context, sourceUri, targetUri)
                }
                Toast.makeText(activity as Context, R.string.toastmessage_save_m3u, Toast.LENGTH_LONG).show()
            } else {
                LogHelper.w(TAG, "M3U export failed.")
            }
        }
    }


    /* Pass the activity result for the backup collecion dialog */
    private fun requestBackupCollecionResult(result: ActivityResult) {
        // save station backup file to result file location
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val targetUri: Uri? = result.data?.data
            if (targetUri != null) {
                BackupHelper.backup(activity as Context, targetUri)
            } else {
                LogHelper.w(TAG, "Station backup failed.")
            }
        }
    }


    /* Pass the activity result for the restore collection dialog */
    private fun requestRestoreCollectionResult(result: ActivityResult) {
        // save station backup file to result file location
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val sourceUri: Uri? = result.data?.data
            if (sourceUri != null) {
                // open and import OPML in player fragment
                val bundle: Bundle = bundleOf(
                    Keys.ARG_RESTORE_COLLECTION to "$sourceUri"
                )
                this.findNavController().navigate(R.id.player_destination, bundle)
            }
        }
    }


    /* Updates collection */
    private fun updateCollection() {
        if (NetworkHelper.isConnectedToNetwork(activity as Context)) {
            Toast.makeText(activity as Context, R.string.toastmessage_updating_collection, Toast.LENGTH_LONG).show()
            // update collection in player screen
            val bundle: Bundle = bundleOf(Keys.ARG_UPDATE_COLLECTION to true)
            this.findNavController().navigate(R.id.player_destination, bundle)
        } else {
            ErrorDialog().show(activity as Context, R.string.dialog_error_title_no_network, R.string.dialog_error_message_no_network)
        }
    }


    /* Updates station images */
    private fun updateStationImages() {
        if (NetworkHelper.isConnectedToNetwork(activity as Context)) {
            Toast.makeText(activity as Context, R.string.toastmessage_updating_station_images, Toast.LENGTH_LONG).show()
            // update collection in player screen
            val bundle: Bundle = bundleOf(
                Keys.ARG_UPDATE_IMAGES to true
            )
            this.findNavController().navigate(R.id.player_destination, bundle)
        } else {
            ErrorDialog().show(activity as Context, R.string.dialog_error_title_no_network, R.string.dialog_error_message_no_network)
        }
    }


    /* Opens up a file picker to select the save location */
    private fun openSaveM3uDialog() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = Keys.MIME_TYPE_M3U
            putExtra(Intent.EXTRA_TITLE, Keys.COLLECTION_M3U_FILE)
        }
        // file gets saved in the ActivityResult
        try {
            requestSaveM3uLauncher.launch(intent)
        } catch (exception: Exception) {
            LogHelper.e(TAG, "Unable to save M3U.\n$exception")
            Toast.makeText(activity as Context, R.string.toastmessage_install_file_helper, Toast.LENGTH_LONG).show()
        }
    }




    /* Opens up a file picker to select the backup location */
    private fun openBackupCollectionDialog() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = Keys.MIME_TYPE_ZIP
            putExtra(Intent.EXTRA_TITLE, Keys.COLLECTION_BACKUP_FILE)
        }
        // file gets saved in the ActivityResult
        try {
            requestBackupCollectionLauncher.launch(intent)
        } catch (exception: Exception) {
            LogHelper.e(TAG, "Unable to save M3U.\n$exception")
            Toast.makeText(activity as Context, R.string.toastmessage_install_file_helper, Toast.LENGTH_LONG).show()
        }
    }




    /* Opens up a file picker to select the file containing the collection to be restored */
    private fun openRestoreCollecionDialog() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, Keys.MIME_TYPES_ZIP)
        }
        // file gets saved in the ActivityResult
        try {
            requestRestoreCollectionLauncher.launch(intent)
        } catch (exception: Exception) {
            LogHelper.e(TAG, "Unable to open file picker for ZIP.\n$exception")
            // Toast.makeText(activity as Context, R.string.toast_message_install_file_helper, Toast.LENGTH_LONG).show()
        }
    }


}
