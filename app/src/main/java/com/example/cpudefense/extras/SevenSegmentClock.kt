package com.example.cpudefense.extras

import android.graphics.Bitmap
import com.example.cpudefense.R
import com.example.cpudefense.activities.AboutActivity
import com.example.cpudefense.activities.ExtrasActivity
import com.example.cpudefense.gameElements.SevenSegmentDisplay

class SevenSegmentClock(size: Int, activity: ExtrasActivity): SevenSegmentDisplay(6, size, activity) {
    fun getDisplayBitmap(): Bitmap
    {
        return getDisplayBitmap(123400, SevenSegmentDisplay.LedColors.RED, true, 10)
    }
}