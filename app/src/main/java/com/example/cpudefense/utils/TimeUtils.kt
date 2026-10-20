package com.example.cpudefense.utils

import android.content.res.Resources
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** class for converting timestamps into a readable form.
 *  Timestamp handling is quite a mess in Java,
 *  and we want to use JDK functions from API 23. May be changed into more modern form in later versions.
 *
 *  @param resources the app's resources, used for determining the desired format
 *  @param timeParam Epoch time in milliseconds. If none is given, use the current time.
 */
class TimeStamp(val resources: Resources, timeParam: Long? = null) {

    var epochTime: Long = timeParam ?: System.currentTimeMillis()
    var timestamp: Date = Date(epochTime)

    /** this is the format used in our save files. We also assume that the time stamp is in UTC */
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).also {
        it.timeZone = TimeZone.getTimeZone("UTC")
    }

    /** output parameter: Date as a string */
    var readableDate: String = ""
    /** output parameter: Tiem as a string */
    var readableTime: String = ""

    constructor(resources: Resources, timeString: String) : this(resources) {
        timestamp = inputFormat.parse(timeString) ?: throw (IllegalArgumentException())
    }

    fun convertToReadable()
    {
        readableDate = DateFormat.getDateInstance(DateFormat.LONG, resources.configuration.locale)
            .format(timestamp)
        readableTime = DateFormat.getTimeInstance(DateFormat.SHORT, resources.configuration.locale)
            .format(timestamp)
    }
}