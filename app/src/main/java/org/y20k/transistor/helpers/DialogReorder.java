/**
 * DialogRename.java
 * Implements the DialogRename class
 * A DialogRename renames a station after asking the user for a new name
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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.woxthebox.draglistview.DragItemAdapter;
import com.woxthebox.draglistview.DragListView;

import org.y20k.transistor.MainActivity;
import org.y20k.transistor.R;
import org.y20k.transistor.core.Station;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;


/**
 * DialogRename class
 */
public final class DialogReorder implements TransistorKeys {

    /* Define log tag */
    private static final String LOG_TAG = DialogReorder.class.getSimpleName();

    /* Construct and show dialog */
    public static void show(final Activity activity) {
        final MainActivity mainActivity = (MainActivity) activity;
        // prepare dialog builder
        LayoutInflater inflater = LayoutInflater.from(activity);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        // get input field
        View view = inflater.inflate(R.layout.dialog_reorder_station, null);

        final DragListView dragListView = (DragListView) view.findViewById(R.id.dialog_reorder_listview);
        dragListView.setDragListListener(new DragListView.DragListListenerAdapter() {
            @Override
            public void onItemDragStarted(int position) {
            }

            @Override
            public void onItemDragEnded(int fromPosition, int toPosition) {
            }
        });
        dragListView.setLayoutManager(new LinearLayoutManager(activity));
        final ArrayList<Pair<Integer, String>> itemArray = mainActivity.getStationListItems();
        ItemAdapter listAdapter = new ItemAdapter(itemArray, R.layout.list_item_reorder, R.id.image, false);
        dragListView.setAdapter(listAdapter, true);
        dragListView.setCanDragHorizontally(false);
        dragListView.getRecyclerView().setVerticalScrollBarEnabled(true);

        // set dialog view
        builder.setView(view);

        // add OK button
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            // listen for click on delete button
            public void onClick(DialogInterface arg0, int arg1) {
                mainActivity.handleStationReorder(itemArray);
            }
        });

        // add cancel button
        builder.setNegativeButton(R.string.dialog_generic_button_cancel, new DialogInterface.OnClickListener() {
            // listen for click on cancel button
            public void onClick(DialogInterface arg0, int arg1) {
                // do nothing
            }
        });

        // display rename dialog
        builder.show();
    }

    private static class ItemAdapter extends DragItemAdapter<Pair<Integer, String>, ItemAdapter.ViewHolder> {
        private int mLayoutId;
        private int mGrabHandleId;
        private boolean mDragOnLongPress;

        ItemAdapter(ArrayList<Pair<Integer, String>> list, int layoutId, int grabHandleId, boolean dragOnLongPress) {
            mLayoutId = layoutId;
            mGrabHandleId = grabHandleId;
            mDragOnLongPress = dragOnLongPress;
            setItemList(list);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);
            String text = mItemList.get(position).second;
            holder.mText.setText(text);
            holder.itemView.setTag(mItemList.get(position));
        }

        @Override
        public long getUniqueItemId(int position) {
            return mItemList.get(position).first;
        }

        class ViewHolder extends DragItemAdapter.ViewHolder {
            public TextView mText;

            ViewHolder(final View itemView) {
                super(itemView, mGrabHandleId, mDragOnLongPress);
                mText = (TextView) itemView.findViewById(R.id.text);
            }

            @Override
            public void onItemClicked(View view) {
            }

            @Override
            public boolean onItemLongClicked(View view) {
                return true;
            }
        }
    }
}