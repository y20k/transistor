/*
 * DateTimeHelper.kt
 * Implements the DateTimeHelper object
 * A DateTimeHelper provides helper methods for converting Date and Time objects
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-22 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers

import org.y20k.transistor.Keys
import java.text.SimpleDateFormat
import java.util.*


/*
 * DateTimeHelper object
 */
object DateTimeHelper {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(DateTimeHelper::class.java)

    /* Main class variables */
    private const val pattern: String = "EEE, dd MMM yyyy HH:mm:ss Z"
    private val dateFormat: SimpleDateFormat = SimpleDateFormat(pattern, Locale.ENGLISH)


    /* Converts RFC 2822 string representation of a date to DATE */
    fun convertFromRfc2822(dateString: String): Date {
        var date: Date = Keys.DEFAULT_DATE
        try {
            // parse date string using standard pattern
            date = dateFormat.parse((dateString)) ?: Keys.DEFAULT_DATE
        } catch (e: Exception) {
            LogHelper.w(TAG, "Unable to parse. Trying an alternative Date format. $e")
            // try alternative parsing patterns
            date = tryAlternativeRfc2822Parsing(dateString)
        }
        return date
    }


    /* Converts a DATE to its RFC 2822 string representation */
    fun convertToRfc2822(date: Date): String {
        val dateFormat: SimpleDateFormat = SimpleDateFormat(pattern, Locale.ENGLISH)
        return dateFormat.format(date)
    }


    /* Converts a milliseconds in to a readable format */
    fun convertToMinutesAndSeconds(milliseconds: Long, negativeValue: Boolean = false): String {
        // convert milliseconds to minutes and seconds
        val minutes: Long = milliseconds / 1000 / 60
        val seconds: Long = milliseconds / 1000 % 60
        var timeString: String = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        if (negativeValue) {
            // add a minus sign if a negative values was requested
            timeString = "-$timeString"
        }
        return timeString
    }


    /* Converts RFC 2822 string representation of a date to DATE - using alternative patterns */
    private fun tryAlternativeRfc2822Parsing(dateString: String): Date {
        var date: Date = Keys.DEFAULT_DATE
        try {
            // try to parse without seconds
            date = SimpleDateFormat("EEE, dd MMM yyyy HH:mm Z", Locale.ENGLISH).parse((dateString)) ?: Keys.DEFAULT_DATE
        } catch (e: Exception) {
            try {
                LogHelper.w(TAG, "Unable to parse. Trying an alternative Date format. $e")
                // try to parse without time zone
                date = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH).parse((dateString)) ?: Keys.DEFAULT_DATE
            } catch (e: Exception) {
                LogHelper.e(TAG, "Unable to parse. Returning a default date. $e")
            }
        }
        return date
    }

}
