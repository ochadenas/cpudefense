package com.example.cpudefense.effects

import android.graphics.Bitmap

interface Flippable {
    fun setBitmap(bitmap: Bitmap)
    fun provideBitmap(): Bitmap
    fun flipStart()
    fun flipDone()
}