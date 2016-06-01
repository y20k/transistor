/**
 * ImageHelper.java
 * Implements the ImageHelper class
 * An ImageHelper formats icons and symbols for use in the app ui
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-16 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.ContextCompat;

import org.y20k.transistor.R;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;


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
    public ImageHelper(Bitmap inputImage, Context context) {
        mContext = context;

        if (inputImage != null) {
            mInputImage = inputImage;
        } else {
            // set default station image
            mInputImage = getBitmap(R.drawable.ic_notesymbol_36dp);
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
        Bitmap background = getBitmap(R.drawable.ic_notification_large_bg_128dp);
        background = Bitmap.createScaledBitmap(background, size, size, false);

        // compose images
        return composeImages(background, size);
    }



    /* Creates station image on a circular background */
    public Bitmap createCircularFramedImage(int size, int color) {

        Paint background = createBackground(color);

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


    /* Setter for color of background */
    private Paint createBackground(int color) {

        // get background color value in the form 0xAARRGGBB
        int backgroundColor;
        try {
            backgroundColor = ContextCompat.getColor(mContext, color);
        } catch (Exception e) {
            // set default background color white
            backgroundColor = ContextCompat.getColor(mContext, R.color.transistor_white);
            e.printStackTrace();
        }

        // construct circular background
        Paint background = new Paint();
        background.setColor(backgroundColor);
        background.setStyle(Paint.Style.FILL);

        return background;
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


    /* Return sampled down image for given Uri */
    private Bitmap decodeSampledBitmapFromUri(Uri imageUri, int reqWidth, int reqHeight) {

        Bitmap bitmap;
        ParcelFileDescriptor parcelFileDescriptor =  null;

        try {
            parcelFileDescriptor = mContext.getContentResolver().openFileDescriptor(imageUri, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (parcelFileDescriptor != null) {
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

            // decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

            // calculate inSampleSize
            options.inSampleSize = calculateSampleParameter(options, reqWidth, reqHeight);

            // decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

            return bitmap;

        } else {
            return null;
        }

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