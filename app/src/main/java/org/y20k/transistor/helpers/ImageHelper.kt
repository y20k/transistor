/*
 * ImageHelper.kt
 * Implements the ImageHelper object
 * An ImageHelper provides helper methods for image related operations
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
import android.graphics.*
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.palette.graphics.Palette
import org.y20k.transistor.Keys
import org.y20k.transistor.R
import java.io.IOException
import java.io.InputStream


/*
 * ImageHelper class
 */
object ImageHelper {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(ImageHelper::class.java)

    /* Get scaling factor from display density */
    fun getDensityScalingFactor(context: Context): Float {
        return context.resources.displayMetrics.density
    }


    /* Get a scaled version of the station image */
    fun getScaledStationImage(context: Context, imageUriString: String, imageSize: Int): Bitmap {
        val size: Int = (imageSize * getDensityScalingFactor(context)).toInt()
        return decodeSampledBitmapFromUri(context, imageUriString, size, size)
    }


    /* Get an unscaled version of the station image */
    fun getStationImage(context: Context, imageUriString: String): Bitmap {
        var bitmap: Bitmap? = null

        if (imageUriString != Keys.LOCATION_DEFAULT_STATION_IMAGE) {
            try {
                // just decode the file
                bitmap = BitmapFactory.decodeFile(imageUriString.toUri().path)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // get default image
        if (bitmap == null) {
            bitmap = ContextCompat.getDrawable(context, R.drawable.ic_default_station_image_72dp)!!.toBitmap()
        }

        return bitmap
    }


    /* Composes foreground bitmap onto background bitmap */
    private fun composeImages(foreground: Bitmap, background: Bitmap, size: Int, yOffset: Int): Bitmap {
        val outputImage = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputImage)
        canvas.drawBitmap(background, 0f, 0f, null)
        canvas.drawBitmap(foreground, createTransformationMatrix(size, yOffset, foreground.height.toFloat(), foreground.width.toFloat(), true), null)
        return outputImage
    }


    /* Creates station image on a square background with the main station image color and option padding for adaptive icons */
    fun createSquareImage(context: Context, bitmap: Bitmap, backgroundColor: Int, size: Int, adaptivePadding: Boolean): Bitmap? {

        // create background
        val background = Paint()
        background.style = Paint.Style.FILL
        if (backgroundColor != -1) {
            background.color = backgroundColor
        } else {
            background.color = ContextCompat.getColor(context, R.color.default_neutral_dark)
        }

        // create empty bitmap and canvas
        val outputImage = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val imageCanvas = Canvas(outputImage)

        // draw square background
        val right = size.toFloat()
        val bottom = size.toFloat()
        imageCanvas.drawRect(0f, 0f, right, bottom, background)

        // draw input image onto canvas using transformation matrix
        val paint = Paint()
        paint.isFilterBitmap = true
        imageCanvas.drawBitmap(bitmap, createTransformationMatrix(size, 0, bitmap.height.toFloat(), bitmap.width.toFloat(), adaptivePadding), paint)
        return outputImage
    }


    /* Extracts color from an image */
    fun getMainColor(context: Context, imageUri: String): Int {

        // extract color palette from station image
        val palette: Palette = Palette.from(decodeSampledBitmapFromUri(context, imageUri, 72, 72)).generate()
        // get muted and vibrant swatches
        val vibrantSwatch = palette.vibrantSwatch
        val mutedSwatch = palette.mutedSwatch

        when {
            vibrantSwatch != null -> {
                // return vibrant color
                val rgb = vibrantSwatch.rgb
                return Color.argb(255, Color.red(rgb), Color.green(rgb), Color.blue(rgb))
            }
            mutedSwatch != null -> {
                // return muted color
                val rgb = mutedSwatch.rgb
                return Color.argb(255, Color.red(rgb), Color.green(rgb), Color.blue(rgb))
            }
            else -> {
                // default return
                return context.resources.getColor(R.color.default_neutral_medium_light, null)
            }
        }
    }


    /* Return sampled down image for given Uri */
    private fun decodeSampledBitmapFromUri(context: Context, imageUriString: String, reqWidth: Int, reqHeight: Int): Bitmap {
        var bitmap: Bitmap? = null
        if (imageUriString != Keys.LOCATION_DEFAULT_STATION_IMAGE) {
            try {
                val imageUri: Uri = imageUriString.toUri()

                // first decode with inJustDecodeBounds=true to check dimensions
                var stream: InputStream? = context.contentResolver.openInputStream(imageUri)
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeStream(stream, null, options)
                stream?.close()

                // calculate inSampleSize
                options.inSampleSize = calculateSampleParameter(options, reqWidth, reqHeight)

                // decode bitmap with inSampleSize set
                stream = context.contentResolver.openInputStream(imageUri)
                options.inJustDecodeBounds = false
                bitmap = BitmapFactory.decodeStream(stream, null, options)
                stream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        // get default image
        if (bitmap == null) {
            bitmap = ContextCompat.getDrawable(context, R.drawable.ic_default_station_image_72dp)!!.toBitmap()
        }

        return bitmap
    }


    /* Calculates parameter needed to scale image down */
    private fun calculateSampleParameter(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // get size of original image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight = height / 2
            val halfWidth = width / 2

            // calculates the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width
            while (halfHeight / inSampleSize > reqHeight && halfWidth / inSampleSize > reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }


    /* Creates a transformation matrix with the given size and optional padding  */
    private fun createTransformationMatrix(size: Int, yOffset: Int, inputImageHeight: Float, inputImageWidth: Float, scaled: Boolean): Matrix {
        val matrix = Matrix()

        // calculate padding
        var padding = 0f
        if (scaled) {
            padding = size.toFloat() / 4f
        }

        // define variables needed for transformation matrix
        var aspectRatio = 0.0f
        var xTranslation = 0.0f
        var yTranslation = 0.0f

        // landscape format and square
        if (inputImageWidth >= inputImageHeight) {
            aspectRatio = (size - padding * 2) / inputImageWidth
            xTranslation = 0.0f + padding
            yTranslation = (size - inputImageHeight * aspectRatio) / 2.0f + yOffset
        } else if (inputImageHeight > inputImageWidth) {
            aspectRatio = (size - padding * 2) / inputImageHeight
            yTranslation = 0.0f + padding + yOffset
            xTranslation = (size - inputImageWidth * aspectRatio) / 2.0f
        }

        // construct transformation matrix
        matrix.postTranslate(xTranslation, yTranslation)
        matrix.preScale(aspectRatio, aspectRatio)
        return matrix
    }

}
