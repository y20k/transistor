/*
 * CollectionAdapter.kt
 * Implements the CollectionAdapter class
 * A CollectionAdapter is a custom adapter providing station card views for a RecyclerView
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-22 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.collection

import android.content.Context
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.net.toUri
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import org.y20k.transistor.Keys
import org.y20k.transistor.R
import org.y20k.transistor.core.Collection
import org.y20k.transistor.core.Station
import org.y20k.transistor.dialogs.EditStationDialog
import org.y20k.transistor.dialogs.RenameStationDialog
import org.y20k.transistor.helpers.*


/*
 * CollectionAdapter class
 */
class CollectionAdapter(private val context: Context, private val collectionAdapterListener: CollectionAdapterListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), UpdateHelper.UpdateHelperListener, RenameStationDialog.RenameStationListener, EditStationDialog.EditStationListener {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(CollectionAdapter::class.java)


    /* Main class variables */
    private lateinit var collectionViewModel: CollectionViewModel
    // private lateinit var collectionAdapterListener: CollectionAdapterListener
    private var collection: Collection = Collection()
    private val editStationsEnabled: Boolean = PreferencesHelper.loadEditStationsEnabled()
    private val editStationStreamsEnabled: Boolean = PreferencesHelper.loadEditStationStreamsEnabled()
    private var expandedStationStreamUri: String = PreferencesHelper.loadStationListStreamUriLocation()
    private var expandedStationPosition: Int = -1

    // todo listen for preference changes


    /* Listener Interface */
    interface CollectionAdapterListener {
        fun onPlayButtonTapped(stationUuid: String, playbackState: Int)
        fun onAddNewButtonTapped()
        fun onChangeImageButtonTapped(stationUuid: String)
    }


    /* Overrides onAttachedToRecyclerView */
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        // create view model and observe changes in collection view model
        collectionViewModel = ViewModelProvider(context as AppCompatActivity).get(CollectionViewModel::class.java)
        observeCollectionViewModel(context as LifecycleOwner)

    }


    /* Overrides onCreateViewHolder */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        when (viewType) {
            Keys.VIEW_TYPE_ADD_NEW -> {
                // get view, put view into holder and return
                val v = LayoutInflater.from(parent.context).inflate(R.layout.card_add_new_station, parent, false)
                return AddNewViewHolder(v)
            }
            else -> {
                // get view, put view into holder and return
                val v = LayoutInflater.from(parent.context).inflate(R.layout.card_station, parent, false)
                return StationViewHolder(v)
            }
        }
    }


    /* Overrides onBindViewHolder */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (holder) {

            // CASE ADD NEW CARD
            is AddNewViewHolder -> {
                // get reference to StationViewHolder
                val addNewViewHolder: AddNewViewHolder = holder
                addNewViewHolder.addNewStationView.setOnClickListener {
                    // show the add station dialog
                    collectionAdapterListener.onAddNewButtonTapped()
                }
                addNewViewHolder.settingsButtonView.setOnClickListener {
                    it.findNavController().navigate(R.id.settings_destination)
                }
            }

            // CASE STATION CARD
            is StationViewHolder -> {
                // get station from position
                val station: Station = collection.stations[position]

                // get reference to StationViewHolder
                val stationViewHolder: StationViewHolder = holder

                // set up station views
                setStarredIcon(stationViewHolder, station)
                setStationName(stationViewHolder, station, position)
                setStationImage(stationViewHolder, station, position)
                setStationButtons(stationViewHolder, station, position)
                setEditViews(stationViewHolder, station, position)

                // show / hide edit views
                when (expandedStationPosition) {
                    position -> {
                        stationViewHolder.stationNameView.isVisible = false
                        stationViewHolder.editViews.isVisible = true
                        // todo hide stationViewHolder.stationUriEditView.isGone if option is set accordingly
                    }
                    else -> {
                        stationViewHolder.stationNameView.isVisible = true
                        stationViewHolder.editViews.isGone = true
                    }
                }
            }
        }
    }

    /* Overrides onStationUpdated from UpdateHelperListener */
    override fun onStationUpdated(collection: Collection, positionPriorUpdate: Int, positionAfterUpdate: Int) {
        // check if position has changed after update and move stations around if necessary
        if (positionPriorUpdate != positionAfterUpdate && positionPriorUpdate != -1 && positionAfterUpdate != -1) {
            notifyItemMoved(positionPriorUpdate, positionAfterUpdate)
            notifyItemChanged(positionPriorUpdate)
        }
        // update station (e.g. name)
        notifyItemChanged(positionAfterUpdate)
    }


    /* Overrides onRenameStationDialog from RenameStationListener */
    override fun onRenameStationDialog(textInput: String, stationUuid: String, position: Int) {
        // rename station (and sort collection)
        collection = CollectionHelper.renameStation(context, collection, stationUuid, textInput)
        val newPosition: Int = CollectionHelper.getStationPosition(collection, stationUuid)
        if (position != newPosition && newPosition != -1) {
            notifyItemMoved(position, newPosition)
            notifyItemChanged(position)
        }
        notifyItemChanged(newPosition)
    }


    /* Overrides onEditStationDialog from EditStationListener */
    override fun onEditStationDialog(textInput: String, stationUuid: String, position: Int) {
        // rename station (and sort collection)
        collection = CollectionHelper.renameStation(context, collection, stationUuid, textInput)
        val newPosition: Int = CollectionHelper.getStationPosition(collection, stationUuid)
        if (position != newPosition && newPosition != -1) {
            notifyItemMoved(position, newPosition)
            notifyItemChanged(position)
        }
        notifyItemChanged(newPosition)
    }


    /* Sets the station name view */
    private fun setStationName(stationViewHolder: StationViewHolder, station: Station, position: Int) {
        stationViewHolder.stationNameView.text = station.name
    }


    /* Sets the edit views */
    private fun setEditViews(stationViewHolder: StationViewHolder, station: Station, position: Int) {
        stationViewHolder.stationNameEditView.setText(station.name, TextView.BufferType.EDITABLE)
        stationViewHolder.stationUriEditView.setText(station.getStreamUri(), TextView.BufferType.EDITABLE)
        // todo implement sanity check for station uri
        stationViewHolder.cancelButton.setOnClickListener {
            toggleEditViews(position, station.getStreamUri())
        }
        stationViewHolder.saveButton.setOnClickListener {
            // todo implement save
            Toast.makeText(context, "Saving Changes.", Toast.LENGTH_SHORT).show()
            toggleEditViews(position, station.getStreamUri())
        }
        stationViewHolder.placeOnHomeScreenButton.setOnClickListener {
            ShortcutHelper.placeShortcut(context, station)
            toggleEditViews(position, station.getStreamUri())
        }
        stationViewHolder.stationImageChangeView.setOnClickListener {
            collectionAdapterListener.onChangeImageButtonTapped(station.uuid)
            toggleEditViews(position, station.getStreamUri())
        }
    }


    /* Shows / hides the edit view for a station */
    private fun toggleEditViews(position: Int, stationStreamUri: String) {
        when (expandedStationStreamUri) {
            // CASE: this station's edit view is already expanded
            stationStreamUri -> {
                // reset currently expanded info
                saveStationListExpandedState()
                // update station view
                notifyItemChanged(position)
            }
            // CASE: this station's edit view is not yet expanded
            else -> {
                // remember previously expanded position
                val previousExpandedStationPosition: Int = expandedStationPosition
                // if station was expanded - collapse it
                if (previousExpandedStationPosition > -1 && previousExpandedStationPosition < collection.stations.size) notifyItemChanged(previousExpandedStationPosition)
                // store current station as the expanded one
                saveStationListExpandedState(position, stationStreamUri)
                // update station view
                notifyItemChanged(expandedStationPosition)
            }
        }
    }


    /* Toggles the starred icon */
    private fun setStarredIcon(stationViewHolder: StationViewHolder, station: Station) {
        when (station.starred) {
            true -> {
                if (station.imageColor != -1) {
                    // stationViewHolder.stationCardView.setCardBackgroundColor(station.imageColor)
                    stationViewHolder.stationStarredView.setColorFilter(station.imageColor)
                }
                stationViewHolder.stationStarredView.isVisible = true
            }
            false -> stationViewHolder.stationStarredView.isGone = true
        }
    }


    /* Sets the station image view */
    private fun setStationImage(stationViewHolder: StationViewHolder, station: Station, position: Int) {
        if (station.imageColor != -1) {
            stationViewHolder.stationImageView.setBackgroundColor(station.imageColor)
        }
        stationViewHolder.stationImageView.setImageBitmap(ImageHelper.getStationImage(context, station.smallImage))
        stationViewHolder.stationImageView.contentDescription = "${context.getString(R.string.descr_player_station_image)}: ${station.name}"
    }


    /* Sets up a station's play and edit buttons */
    private fun setStationButtons(stationViewHolder: StationViewHolder, station: Station, position: Int) {
        val playbackState: Int = station.playbackState
        when (playbackState) {
            PlaybackStateCompat.STATE_PLAYING -> stationViewHolder.playButtonView.setImageResource(R.drawable.ic_stop_circle_outline_36dp)
            else -> stationViewHolder.playButtonView.setImageResource(R.drawable.ic_play_circle_outline_36dp)
        }
        stationViewHolder.playButtonView.setOnClickListener {
            collectionAdapterListener.onPlayButtonTapped(station.uuid, playbackState)
        }
        stationViewHolder.stationCardView.setOnLongClickListener {
            if (editStationsEnabled) {
                toggleEditViews(position, station.getStreamUri())
                return@setOnLongClickListener true
            } else {
                return@setOnLongClickListener false
            }
        }
    }


//    /* Displays station popup menu */
//    private fun showStationPopupMenu(view: View, stationUuid: String, position: Int) {
//        val popup = PopupMenu(context, view)
//        popup.inflate(R.menu.station_popup_menu)
//        popup.setOnMenuItemClickListener { item ->
//            when (item.itemId) {
//                R.id.menu_icon -> {
//                    // let fragment get system picker for images
//                    collectionAdapterListener.onChangeImageButtonTapped(stationUuid)
//                    true
//                }
//                R.id.menu_rename -> {
//                    // show rename dialog
//                    val name: String = CollectionHelper.getStationName(collection, stationUuid)
//                    RenameStationDialog(this).show(context, name, stationUuid, position)
//                    true
//                }
////                R.id.menu_delete -> {
////                    // show delete dialog
////                    // DialogDelete.show(activity, station)
////                    true
////                }
//                R.id.menu_update -> {
//                    // update this station
//                    Toast.makeText(context, R.string.toastmessage_updating_station, Toast.LENGTH_SHORT).show()
//                    val updateHelper: UpdateHelper = UpdateHelper(context, this, collection)
//                    updateHelper.updateStation(CollectionHelper.getStation(collection, stationUuid))
//                    true
//                }
//                R.id.menu_shortcut -> {
//                    // create shortcut
//                    val station: Station = CollectionHelper.getStation(collection, stationUuid)
//                    ShortcutHelper.placeShortcut(context, station)
//                    true
//                }
//                else -> false
//            }
//        }
//        popup.show()
//    }


    /* Overrides onBindViewHolder */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>) {

        if (payloads.isEmpty()) {
            // call regular onBindViewHolder method
            onBindViewHolder(holder, position)

        } else if (holder is StationViewHolder) {
            // get station from position
            val station = collection.stations[holder.getAdapterPosition()]

            // get reference to StationViewHolder
            val stationViewHolder = holder

            for (data in payloads) {
                when (data as Int) {
                    Keys.HOLDER_UPDATE_COVER -> {
                        // todo implement
                    }
                    Keys.HOLDER_UPDATE_NAME -> {
                        // todo implement
                    }
                    Keys.HOLDER_UPDATE_PLAYBACK_STATE -> {
                        // todo implement
                    }
                    Keys.HOLDER_UPDATE_PLAYBACK_PROGRESS -> {
                        // todo implement
                    }
                    Keys.HOLDER_UPDATE_DOWNLOAD_STATE -> {
                        // todo implement
                    }
                }
            }
        }
    }


    /* Overrides getItemViewType */
    override fun getItemViewType(position: Int): Int {
        when (isPositionFooter(position)) {
            true -> return Keys.VIEW_TYPE_ADD_NEW
            false -> return Keys.VIEW_TYPE_STATION
        }
    }


    /* Overrides getItemCount */
    override fun getItemCount(): Int {
        // +1 ==> the add station card
        return collection.stations.size + 1
    }


    /* Removes a station from collection */
    fun removeStation(context: Context, position: Int) {
        val newCollection = collection.deepCopy()
        // delete images assets
        CollectionHelper.deleteStationImages(context, newCollection.stations[position])
        // remove station from collection
        newCollection.stations.removeAt(position)
        collection = newCollection
        // update list
        notifyItemRemoved(position)
        // save collection and broadcast changes
        CollectionHelper.saveCollection(context, newCollection)
    }


    /* Toggles starred status of a station */
    fun toggleStarredStation(context: Context, position: Int) {
        // update view
        notifyItemChanged(position)
        // mark starred
        val stationUuid: String = collection.stations[position].uuid
        collection.stations[position].apply { starred = !starred }
        // sort collection
        collection = CollectionHelper.sortCollection(collection)
        // update list view
        notifyItemMoved(position, CollectionHelper.getStationPosition(collection, stationUuid))
        // save collection and broadcast changes
        CollectionHelper.saveCollection(context, collection)
    }


//    /* Initiates update of a station's information */ // todo move to CollectionHelper
//    private fun updateStation(context: Context, station: Station) {
//        if (station.radioBrowserStationUuid.isNotEmpty()) {
//            // get updated station from radio browser - results are handled by onRadioBrowserSearchResults
//            val radioBrowserSearch: RadioBrowserSearch = RadioBrowserSearch(context, this)
//            radioBrowserSearch.searchStation(context, station.radioBrowserStationUuid, Keys.SEARCH_TYPE_BY_UUID)
//        } else if (station.remoteStationLocation.isNotEmpty()) {
//            // download playlist // todo check content type detection is necessary here
//            DownloadHelper.downloadPlaylists(context, arrayOf(station.remoteStationLocation))
//        } else {
//            LogHelper.w(TAG, "Unable to update station: ${station.name}.")
//        }
//    }



    /* Determines if position is last */
    private fun isPositionFooter(position: Int): Boolean {
        return position == collection.stations.size
    }


    /* Updates the station list - redraws the views with changed content */
    private fun updateRecyclerView(oldCollection: Collection, newCollection: Collection) {
        collection = newCollection
        if (oldCollection.stations.size == 0 && newCollection.stations.size > 0) {
            // data set has been initialized - redraw the whole list
            notifyDataSetChanged()
        } else {
            // calculate differences between current collection and new collection - and inform this adapter about the changes
            val diffResult = DiffUtil.calculateDiff(CollectionDiffCallback(oldCollection, newCollection), true)
            diffResult.dispatchUpdatesTo(this@CollectionAdapter)
        }
    }


    /* Updates and saves state of expanded station edit view in list */
    private fun saveStationListExpandedState(position: Int = -1, stationStreamUri: String = String()) {
        expandedStationStreamUri = stationStreamUri
        expandedStationPosition = position
        PreferencesHelper.saveStationListStreamUriLocation(expandedStationStreamUri)
    }


    /* Observe view model of station collection*/
    private fun observeCollectionViewModel(owner: LifecycleOwner) {
        collectionViewModel.collectionLiveData.observe(owner, Observer<Collection> { newCollection ->
            updateRecyclerView(collection, newCollection)
        })
    }


    /*
     * Inner class: ViewHolder for the Add New Station action
     */
    private inner class AddNewViewHolder (listItemAddNewLayout: View) : RecyclerView.ViewHolder(listItemAddNewLayout) {
        val addNewStationView: MaterialButton = listItemAddNewLayout.findViewById(R.id.card_add_new_station)
        val settingsButtonView: MaterialButton = listItemAddNewLayout.findViewById(R.id.card_settings)
    }
    /*
     * End of inner class
     */


    /*
     * Inner class: ViewHolder for a station
     */
    private inner class StationViewHolder (stationCardLayout: View): RecyclerView.ViewHolder(stationCardLayout) {
        val stationCardView: ConstraintLayout = stationCardLayout.findViewById(R.id.station_card)
        val stationImageView: ImageView = stationCardLayout.findViewById(R.id.station_icon)
        val stationNameView: TextView = stationCardLayout.findViewById(R.id.station_name)
        val stationStarredView: ImageView = stationCardLayout.findViewById(R.id.starred_icon)
//        val menuButtonView: ImageView = stationCardLayout.findViewById(R.id.menu_button)
        val playButtonView: ImageView = stationCardLayout.findViewById(R.id.playback_button)
        val editViews: Group = stationCardLayout.findViewById(R.id.edit_views)
        val stationImageChangeView: ImageView = stationCardLayout.findViewById(R.id.change_image_view)
        val stationNameEditView: TextInputEditText = stationCardLayout.findViewById(R.id.edit_station_name)
        val stationUriEditView: TextInputEditText = stationCardLayout.findViewById(R.id.edit_stream_uri)
        val placeOnHomeScreenButton: MaterialButton = stationCardLayout.findViewById(R.id.place_on_home_screen_button)
        val cancelButton: MaterialButton = stationCardLayout.findViewById(R.id.cancel_button)
        val saveButton: MaterialButton = stationCardLayout.findViewById(R.id.save_button)
    }
    /*
     * End of inner class
     */


    /*
     * Inner class: DiffUtil.Callback that determines changes in data - improves list performance
     */
    private inner class CollectionDiffCallback(val oldCollection: Collection, val newCollection: Collection): DiffUtil.Callback() {

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldStation: Station = oldCollection.stations[oldItemPosition]
            val newStation: Station = newCollection.stations[newItemPosition]
            return oldStation.uuid == newStation.uuid
        }

        override fun getOldListSize(): Int {
            return oldCollection.stations.size
        }

        override fun getNewListSize(): Int {
            return newCollection.stations.size
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldStation: Station = oldCollection.stations[oldItemPosition]
            val newStation: Station = newCollection.stations[newItemPosition]

            // compare relevant contents of a station
            if (oldStation.playbackState != newStation.playbackState) return false
            if (oldStation.uuid != newStation.uuid) return false
            if (oldStation.starred != newStation.starred) return false
            if (oldStation.name != newStation.name) return false
            if (oldStation.stream != newStation.stream) return false
            if (oldStation.remoteImageLocation != newStation.remoteImageLocation) return false
            if (oldStation.remoteStationLocation != newStation.remoteStationLocation) return false
            if (!oldStation.streamUris.containsAll(newStation.streamUris)) return false
            if (oldStation.imageColor != newStation.imageColor) return false
            if (FileHelper.getFileSize(context, oldStation.image.toUri()) != FileHelper.getFileSize(context, newStation.image.toUri())) return false
            if (FileHelper.getFileSize(context, oldStation.smallImage.toUri()) != FileHelper.getFileSize(context, newStation.smallImage.toUri())) return false

            // none of the above -> contents are the same
            return true
        }
    }
    /*
     * End of inner class
     */
}
