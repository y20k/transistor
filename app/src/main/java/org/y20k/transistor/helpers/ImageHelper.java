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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.ContextCompat;

import org.y20k.transistor.R;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;


/**
 * ImageHelper class
 */
public class ImageHelper {

    /* Main class variables */
    private final Bitmap mInputImage;
    private final Paint mBackgroundColor;
    private final Context mContext;


    /* Constructor when given a Bitmap*/
    public ImageHelper(Bitmap inputImage, Context context) {
        mInputImage = inputImage;
        mContext = context;

        // set default background color white
        int backgroundColor = ContextCompat.getColor(mContext, R.color.transistor_white);
        mBackgroundColor = new Paint();
        mBackgroundColor.setColor(backgroundColor);
    }


    /* Constructor when given an Uri */
    public ImageHelper(Uri inputImageUri, Context context) {
        mContext = context;
        mInputImage = decodeSampledBitmapFromUri(inputImageUri, 72, 72);

        // set default background color white
        int backgroundColor = ContextCompat.getColor(mContext, R.color.transistor_white);
        mBackgroundColor = new Paint();
        mBackgroundColor.setColor(backgroundColor);
    }


    /* Setter for color of background */
    public void setBackgroundColor(int color) {
        int backgroundColor = ContextCompat.getColor(mContext, color);
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
        Canvas canvas = new Canvas(outputImage);

        // construct circular background
        mBackgroundColor.setStyle(Paint.Style.FILL);
        float cx = size / 2;
        float cy = size / 2;
        float radius = size / 2;

        // draw circular background
        canvas.drawCircle(cx, cy, radius, mBackgroundColor);

        // construct station image frame
        float inputImageHeight = (float)mInputImage.getHeight();
        float inputImageWidth = (float)mInputImage.getWidth();
        float verticalCorrection;
        float horizontalCorrection;
        float scalingFactor;

        // landscape format
        if (inputImageWidth > inputImageHeight) {
            horizontalCorrection = 0;
            scalingFactor = inputImageHeight / inputImageWidth;
            verticalCorrection = size - (size * scalingFactor);
        }
        // portrait format
        else if (inputImageHeight > inputImageWidth) {
            verticalCorrection = 0;
            scalingFactor = inputImageWidth / inputImageHeight;
            horizontalCorrection = size - (size * scalingFactor);
        }
        // square format
        else {
            verticalCorrection = 0;
            horizontalCorrection = 0;
        }

        int left = (size - (int)verticalCorrection) / 4;
        int top = (size - (int)horizontalCorrection) / 4;
        int right = size - ((size - (int)verticalCorrection) / 4);
        int bottom = size - ((size - (int)horizontalCorrection) / 4);
        Rect frame = new Rect(left, top, right, bottom);

        // overlay station image
        canvas.drawBitmap(mInputImage, null, frame, null);

        return outputImage;
    }


    /* Return sampled down image for given Uri */
    private Bitmap decodeSampledBitmapFromUri(Uri imageUri, int reqWidth, int reqHeight) {

        Bitmap bitmap = null;
        ParcelFileDescriptor parcelFileDescriptor =  null;

        try {
            parcelFileDescriptor = mContext.getContentResolver().openFileDescriptor(imageUri, "r");
        } catch (FileNotFoundException e) {
            // TODO handle error
            e.printStackTrace();
        }

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
    }


    /* Calculates parameter needed to scale image down */
    private static int calculateSampleParameter(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // calculates the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

}