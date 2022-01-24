/*
 * UiHelper.kt
 * Implements the UiHelper object
 * A UiHelper provides helper methods for User Interface related tasks
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-22 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import org.y20k.transistor.Keys
import org.y20k.transistor.R


/*
 * UiHelper object
 */
object UiHelper {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(UiHelper::class.java)


    /* Sets layout margins for given view in DP */
    fun setViewMargins(context: Context, view: View, left: Int = 0, right: Int = 0, top: Int= 0, bottom: Int = 0) {
        val l: Int = (left * ImageHelper.getDensityScalingFactor(context)).toInt()
        val r: Int = (right * ImageHelper.getDensityScalingFactor(context)).toInt()
        val t: Int = (top * ImageHelper.getDensityScalingFactor(context)).toInt()
        val b: Int = (bottom * ImageHelper.getDensityScalingFactor(context)).toInt()
        if (view.layoutParams is ViewGroup.MarginLayoutParams) {
            val p = view.layoutParams as ViewGroup.MarginLayoutParams
            p.setMargins(l, t, r, b)
            view.requestLayout()
        }
    }


    /* Sets layout margins for given view in percent */
    fun setViewMarginsPercentage(context: Context, view: View, height: Int, width: Int, left: Int = 0, right: Int = 0, top: Int= 0, bottom: Int = 0) {
        val l: Int = ((width / 100.0f) * left).toInt()
        val r: Int = ((width / 100.0f) * right).toInt()
        val t: Int = ((height / 100.0f) * top).toInt()
        val b: Int = ((height / 100.0f) * bottom).toInt()
        setViewMargins(context, view, l, r, t, b)
    }


    /* Displays a simple Snackbar message and anchors it to given view */
    fun displaySnackbar(contextView: View, anchorView: View, text: Int, requireConfirmation: Boolean) {
        if (requireConfirmation) {
            Snackbar.make(contextView, text, Snackbar.LENGTH_INDEFINITE)
                    .setAnchorView(anchorView)
                    .setAction(R.string.dialog_generic_button_okay) {
                        // snackbar ok button has clicked - just dismiss / do nothing
                    }
                    .show()
        } else {
            Snackbar.make(contextView, text, Snackbar.LENGTH_SHORT)
                    .setAnchorView(anchorView)
                    .show()
        }
    }


    /* Get the height of the system's top status bar */
    fun getStatusBarHeight(context: Context): Int {
        var result: Int = 0
        val resourceId: Int = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }


    /* Get scaling factor from display density */
    fun getDensityScalingFactor(context: Context): Float {
        return context.resources.displayMetrics.density
    }


    /* Hide keyboard */
    fun hideSoftKeyboard(context: Context, view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }


    /*
     * Inner class: Callback that detects a swipe to left
     * Credit: https://github.com/kitek/android-rv-swipe-delete/blob/master/app/src/main/java/pl/kitek/rvswipetodelete/SwipeToDeleteCallback.kt
     */
    abstract class SwipeToDeleteCallback(context: Context): ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

        private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_remove_circle_24dp)
        private val intrinsicWidth: Int = deleteIcon?.intrinsicWidth ?: 0
        private val intrinsicHeight: Int = deleteIcon?.intrinsicHeight ?: 0
        private val background: ColorDrawable = ColorDrawable()
        private val backgroundColor = context.resources.getColor(R.color.list_card_delete_background, null)
        private val clearPaint: Paint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            // disable swipe for the add new card
            if (viewHolder.itemViewType == Keys.VIEW_TYPE_ADD_NEW) {
                return 0
            }
            return super.getMovementFlags(recyclerView, viewHolder)
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            // do nothing
            return false
        }

        override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
            val itemView = viewHolder.itemView
            val itemHeight = itemView.bottom - itemView.top
            val isCanceled = dX == 0f && !isCurrentlyActive

            if (isCanceled) {
                clearCanvas(c, itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                return
            }

            // draw red delete background
            background.color = backgroundColor
            background.setBounds(
                    itemView.right + dX.toInt(),
                    itemView.top,
                    itemView.right,
                    itemView.bottom
            ) // left - top - right - bottom
            background.draw(c)

            // calculate position of delete icon
            val deleteIconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
            val deleteIconMargin = (itemHeight - intrinsicHeight) / 2
            val deleteIconLeft = itemView.right - deleteIconMargin - intrinsicWidth
            val deleteIconRight = itemView.right - deleteIconMargin
            val deleteIconBottom = deleteIconTop + intrinsicHeight

            // draw delete icon
            deleteIcon?.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
            deleteIcon?.draw(c)

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }

        private fun clearCanvas(c: Canvas?, left: Float, top: Float, right: Float, bottom: Float) {
            c?.drawRect(left, top, right, bottom, clearPaint)
        }
    }
    /*
     * End of inner class
     */


    /*
     * Inner class: Callback that detects a swipe to left
     * Credit: https://github.com/kitek/android-rv-swipe-delete/blob/master/app/src/main/java/pl/kitek/rvswipetodelete/SwipeToDeleteCallback.kt
     */
    abstract class SwipeToMarkStarredCallback(context: Context): ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

        private val starIcon = ContextCompat.getDrawable(context, R.drawable.ic_marked_starred_star_24dp)
        private val intrinsicWidth: Int = starIcon?.intrinsicWidth ?: 0
        private val intrinsicHeight: Int = starIcon?.intrinsicHeight ?: 0
        private val background: ColorDrawable = ColorDrawable()
        private val backgroundColor = context.resources.getColor(R.color.list_card_mark_starred_background, null)
        private val clearPaint: Paint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            // disable swipe for the add new card
            if (viewHolder.itemViewType == Keys.VIEW_TYPE_ADD_NEW) {
                return 0
            }
            return super.getMovementFlags(recyclerView, viewHolder)
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            // do nothing
            return false
        }

        override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
            val itemView = viewHolder.itemView
            val itemHeight = itemView.bottom - itemView.top
            val isCanceled = dX == 0f && !isCurrentlyActive

            if (isCanceled) {
                clearCanvas(c, itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                return
            }

            // draw red background
            background.color = backgroundColor
            background.setBounds(
                    itemView.left,
                    itemView.top,
                    itemView.left + dX.toInt(),
                    itemView.bottom
            ) // left - top - right - bottom
            background.draw(c)

            // calculate position of delete icon
            val deleteIconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
            val deleteIconMargin = (itemHeight - intrinsicHeight) / 2
            val deleteIconLeft = itemView.left + deleteIconMargin
            val deleteIconRight = itemView.left + deleteIconMargin + intrinsicWidth
            val deleteIconBottom = deleteIconTop + intrinsicHeight

            // draw delete icon
            starIcon?.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
            starIcon?.draw(c)

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }

        private fun clearCanvas(c: Canvas?, left: Float, top: Float, right: Float, bottom: Float) {
            c?.drawRect(left, top, right, bottom, clearPaint)
        }
    }
    /*
     * End of inner class
     */




}
