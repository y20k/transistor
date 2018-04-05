/**
 * ImageHelper.java
 * Implements the ImageHelper class
 * An ImageHelper formats icons and symbols for use in the app ui
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-18 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;

import org.y20k.transistor.R;
import org.y20k.transistor.core.Station;

import java.io.IOException;
import java.io.InputStream;


/**
 * ImageHelper class
 */
public final class ImageHelper {

    /* Define log tag */
    private static final String LOG_TAG = ImageHelper.class.getSimpleName();


    /* Main class variables */
    private static Bitmap mInputImage;
    private final Context mContext;


    /* Constructor when given a Bitmap */
    public ImageHelper(Station station, Context context) {
        mContext = context;
        if (station != null && station.getStationImageFile() != null && station.getStationImageFile().exists()) {
            // get station image
            mInputImage = decodeSampledBitmapFromFile(station.getStationImageFile().toString(), 72, 72);
        } else {
            // set default station image
            mInputImage = getBitmap(R.drawable.ic_music_note_black_36dp);
        }
    }


    /* Constructor when given an Uri */
    public ImageHelper(Uri inputImageUri, Context context) {
        mContext = context;
        mInputImage = decodeSampledBitmapFromUri(inputImageUri, 72, 72);
    }


    /* Creates shortcut icon for Home screen */
    public Bitmap createShortcut(int size) {

        // get scaled background bitmap
        Bitmap background = getBitmap(R.drawable.ic_shortcut_bg_48dp);
        background = Bitmap.createScaledBitmap(background, size, size, false);

        // compose images
        return composeImages(background, size);
    }


    /* Creates station icon for notification */
    public Bitmap createStationIcon(int size) {

        // get scaled background bitmap
        Bitmap background = getBitmap(R.drawable.ic_notification_large_bg_grey_128dp);
        background = Bitmap.createScaledBitmap(background, size, size, false);

        // compose images
        return composeImages(background, size);
    }


    /* Extracts color from station icon */
    public int getStationImageColor() {

        // extract color palette from station image
        Palette palette = Palette.from(mInputImage).generate();
        // get muted and vibrant swatches
        Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();
        Palette.Swatch mutedSwatch = palette.getMutedSwatch();

        if (vibrantSwatch != null) {
            // return vibrant color
            int rgb = vibrantSwatch.getRgb();
            return Color.argb(255, Color.red(rgb), Color.green(rgb), Color.blue(rgb));
        } else if (mutedSwatch != null) {
            // return muted color
            int rgb = mutedSwatch.getRgb();
            return Color.argb(255, Color.red(rgb), Color.green(rgb), Color.blue(rgb));
        } else {
            // default return
            return mContext.getResources().getColor(R.color.transistor_grey_lighter);
        }
    }



    /* Creates station image on a circular background with default color */
    public Bitmap createCircularFramedImage(int size) {
        // get default color
        int color = ContextCompat.getColor(mContext, R.color.station_image_background);
        return createCircularFramedImage(size, color);
    }


    /* Creates station image on a circular background with given color */
    public Bitmap createCircularFramedImage(int size, int color) {

        // create background
        Paint background = new Paint();
        background.setColor(color);
        background.setStyle(Paint.Style.FILL);


        // create empty bitmap and canvas
        Bitmap outputImage = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas imageCanvas = new Canvas(outputImage);

        // draw circular background
        float cx = size / 2;
        float cy = size / 2;
        float radius = size / 2;
        imageCanvas.drawCircle(cx, cy, radius, background);

        // draw input image onto canvas using transformation matrix
        Paint paint = new Paint();
        paint.setFilterBitmap(true);
        imageCanvas.drawBitmap(mInputImage, createTransformationMatrix(size), paint);

        return outputImage;
    }


    /* Composes foreground bitmap onto background bitmap */
    private Bitmap composeImages(Bitmap background, int size) {

        // compose output image
        Bitmap outputImage = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(outputImage);
        canvas.drawBitmap(background, 0, 0, null);
        canvas.drawBitmap(mInputImage, createTransformationMatrix(size), null);

        return outputImage;
    }


    /* Creates a transformation matrix for given */
    private Matrix createTransformationMatrix (int size) {
        Matrix matrix = new Matrix();

        // get size of original image and calculate padding
        float inputImageHeight = (float)mInputImage.getHeight();
        float inputImageWidth = (float)mInputImage.getWidth();
        float padding = (float)size/4;

        // define variables needed for transformation matrix
        float aspectRatio = 0.0f;
        float xTranslation = 0.0f;
        float yTranslation = 0.0f;

        // landscape format and square
        if (inputImageWidth >= inputImageHeight) {
            aspectRatio = (size - padding*2) / inputImageWidth;
            xTranslation = 0.0f + padding;
            yTranslation = (size - inputImageHeight * aspectRatio)/2.0f;
        }
        // portrait format
        else if (inputImageHeight > inputImageWidth) {
            aspectRatio = (size - padding*2) / inputImageHeight;
            yTranslation = 0.0f + padding;
            xTranslation = (size - inputImageWidth * aspectRatio)/2.0f;
        }

        // construct transformation matrix
        matrix.postTranslate(xTranslation, yTranslation);
        matrix.preScale(aspectRatio, aspectRatio);

        return matrix;
    }



    /* Return sampled down image for given image file path */
    private Bitmap decodeSampledBitmapFromFile(String imageFilePath, int reqWidth, int reqHeight) {

        // first decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFilePath, options);

        // calculate inSampleSize
        options.inSampleSize = calculateSampleParameter(options, reqWidth, reqHeight);

        // decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(imageFilePath, options);
    }


    /* Return sampled down image for given Uri */
    private Bitmap decodeSampledBitmapFromUri(Uri imageUri, int reqWidth, int reqHeight) {

        InputStream stream = null;
        Bitmap bitmap = null;

        try {
            // first decode with inJustDecodeBounds=true to check dimensions
            stream = mContext.getContentResolver().openInputStream(imageUri);
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(stream, null, options);
            stream.close();

            // calculate inSampleSize
            options.inSampleSize = calculateSampleParameter(options, reqWidth, reqHeight);

            // decode bitmap with inSampleSize set
            stream = mContext.getContentResolver().openInputStream(imageUri);
            options.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeStream(stream, null, options);
            stream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }


    /* Calculates parameter needed to scale image down */
    private static int calculateSampleParameter(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // get size of original image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // calculates the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }


    /* Return a bitmap for a given resource id of a vector drawable */
    private Bitmap getBitmap(int resource) {
        VectorDrawableCompat drawable = VectorDrawableCompat.create(mContext.getResources(), resource, null);
        if (drawable != null) {
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } else {
            return null;
        }
    }


    /* Getter for input image */
    public Bitmap getInputImage() {
        return mInputImage;
    }

}