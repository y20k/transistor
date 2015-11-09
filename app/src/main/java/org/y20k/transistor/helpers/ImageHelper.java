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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.content.ContextCompat;

import org.y20k.transistor.R;


/**
 * ImageHelper class
 */
public class ImageHelper {

    /* Main class variables */
    private final Bitmap mInputImage;
    private final Paint mBackgroundColor;
    private final Context mContext;


    /* Constructor */
    public ImageHelper(Bitmap inputImage, Context context) {
        mInputImage = inputImage;
        mContext = context;

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
        int left = size / 4;
        int top = size / 4;
        int right = size - (size / 4);
        int bottom = size - (size / 4);
        Rect frame = new Rect(left, top, right, bottom);

        // overlay station image
        canvas.drawBitmap(mInputImage, null, frame, null);

        return outputImage;
    }

}