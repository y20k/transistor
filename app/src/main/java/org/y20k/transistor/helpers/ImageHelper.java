/**
 * ImageHelper.java
 * Implements the ImageHelper class
 * An ImageHelper formats icons and symbols for use in the app ui
 * <p/>
 * This file is part of
 * TRANSISTOR - Radio App for Android
 * <p/>
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

import org.y20k.transistor.R;


/**
 * ImageHelper class
 */
public class ImageHelper {

    /* Main class variables */
    private Bitmap mInputImage;
    private Bitmap mOutputImage;
    private Paint mBackgroundColor;
    private Context mContext;


    /* Constructor */
    public ImageHelper(Bitmap inputImage, Context context) {
        mInputImage = inputImage;
        mContext = context;

        // set default background color white
        int backgroundColor = mContext.getResources().getColor(R.color.transistor_white);
        mBackgroundColor = new Paint();
        mBackgroundColor.setColor(backgroundColor);
    }


    /* Setter for color of background */
    public void setBackgroundColor(int color) {
        int backgroundColor = mContext.getResources().getColor(color);
        mBackgroundColor.setColor(backgroundColor);
    }


    /* Creates station image on a circular background */
    public Bitmap createCircularFramedImage(int size) {

        // create empty bitmap and canvas
        mOutputImage = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mOutputImage);

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

        return mOutputImage;
    }

}

//    /* Creates circular cropped image of given size */
//    public Bitmap createCircularImage (int size) {
//        mOutputImage = Bitmap.createBitmap(mInputImage.getWidth(), mInputImage.getHeight(), Config.ARGB_8888);
//
//        // define color
//        final int color = 0xffa19774;
//
//        // rect with size of input image
//        final Rect rect = new Rect(0, 0, mInputImage.getWidth(), mInputImage.getHeight());
//        // convert into rect with float values
//        final RectF rectF = new RectF(rect);
//
//        // float version of size
//        final float roundPx = size;
//
//        // create canvas and paint
//        Canvas canvas = new Canvas(mOutputImage);
//        final Paint paint = new Paint();
//
//        // antialias
//        paint.setAntiAlias(true);
//        // set color
//        paint.setColor(color);
//
//        // transparent background
//        canvas.drawARGB(0, 0, 0, 0);
//        // draw circular shape
//        canvas.drawRoundRect(rectF,roundPx,roundPx,paint);
//
//        // cut off a section of a bitmap
//        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
//
//        // draw input image onto canvas
//        canvas.drawBitmap(mInputImage,rect,rect,paint);
//
//        return mOutputImage;
//    }
//    // Thank you: http://stackoverflow.com/questions/2459916/how-to-make-an-imageview-with-rounded-corners