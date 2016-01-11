/**
 * ImageHelper.java
 * Implements the ImageHelper class
 * An ImageHelper formats icons and symbols for use in the app ui
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.ContextCompat;

import org.y20k.transistor.R;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;


/**
 * ImageHelper class
 */
public final class ImageHelper {

    /* Main class variables */
    private final Bitmap mInputImage;
    private final Paint mBackgroundColor;
    private final Activity mActivity;


    /* Constructor when given a Bitmap*/
    public ImageHelper(Bitmap inputImage, Activity activity) {
        mInputImage = inputImage;
        mActivity = activity;

        // set default background color white
        int backgroundColor = ContextCompat.getColor(mActivity, R.color.transistor_white);
        mBackgroundColor = new Paint();
        mBackgroundColor.setColor(backgroundColor);
    }


    /* Constructor when given an Uri */
    public ImageHelper(Uri inputImageUri, Activity activity) {
        mActivity = activity;
        mInputImage = decodeSampledBitmapFromUri(inputImageUri, 72, 72);

        // set default background color white
        int backgroundColor = ContextCompat.getColor(mActivity, R.color.transistor_white);
        mBackgroundColor = new Paint();
        mBackgroundColor.setColor(backgroundColor);
    }


    /* Setter for color of background */
    public void setBackgroundColor(int color) {
        int backgroundColor = ContextCompat.getColor(mActivity, color);
        mBackgroundColor.setColor(backgroundColor);
    }


    /* Getter for input image */
    public Bitmap getInputImage() {
        return mInputImage;
    }


    /* Creates station image on a circular background */
    public Bitmap createCircularFramedImage(int size) {

        // create empty bitmap and canvas
        Bitmap outputImage = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas imageCanvas = new Canvas(outputImage);

        // construct circular background
        mBackgroundColor.setStyle(Paint.Style.FILL);
        float cx = size / 2;
        float cy = size / 2;
        float radius = size / 2;

        // draw circular background
        imageCanvas.drawCircle(cx, cy, radius, mBackgroundColor);

        // get size of original image
        float inputImageHeight = (float)mInputImage.getHeight();
        float inputImageWidth = (float)mInputImage.getWidth();

        // calculate padding
        float padding = (float)size/4;

        // define variables needed for transformation matrix
        Matrix transformationMatrix = new Matrix();
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
        transformationMatrix.postTranslate(xTranslation, yTranslation);
        transformationMatrix.preScale(aspectRatio, aspectRatio);

        // draw input image onto canvas using transformation matrix
        Paint paint = new Paint();
        paint.setFilterBitmap(true);
        imageCanvas.drawBitmap(mInputImage, transformationMatrix, paint);

        return outputImage;
    }


    /* Return sampled down image for given Uri */
    private Bitmap decodeSampledBitmapFromUri(Uri imageUri, int reqWidth, int reqHeight) {

        Bitmap bitmap;
        ParcelFileDescriptor parcelFileDescriptor =  null;

        try {
            parcelFileDescriptor = mActivity.getContentResolver().openFileDescriptor(imageUri, "r");
        } catch (FileNotFoundException e) {
            // TODO handle error
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


    /* Get the dominant color within input image - for testing purposes */
    private int getDominantColor () {
        Bitmap onePixelBitmap = Bitmap.createScaledBitmap(mInputImage, 1, 1, false);
        int pixel = onePixelBitmap.getPixel(0, 0);

        int red = Color.red(pixel);
        int green = Color.green(pixel);
        int blue = Color.blue(pixel);

        return Color.argb(127, red, green, blue);
    }

}