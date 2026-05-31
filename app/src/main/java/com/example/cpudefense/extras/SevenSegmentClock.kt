package com.example.cpudefense.extras

import android.graphics.Bitmap
import com.example.cpudefense.activities.ExtrasActivity
import com.example.cpudefense.gameElements.SevenSegmentDisplay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SevenSegmentClock(size: Int, activity: ExtrasActivity): SevenSegmentDisplay(6, size, activity) {
    fun getDisplayBitmap(): Bitmap
    {
        return getDisplayBitmap(currentTimeString().toInt(), LedColors.RED, true, 10, dotPosition = listOf(1, 3))
    }

    fun currentTimeString(): String
    /** current time (as shown by the clock) */
    {
        val clockTimeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
        return clockTimeFormat.format(Date())
    }

}