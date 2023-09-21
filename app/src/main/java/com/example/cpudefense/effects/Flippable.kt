package com.example.cpudefense.effects

import android.graphics.Bitmap

interface Flippable {
    fun setBitmap(bitmap: Bitmap): Unit
    fun provideBitmap(): Bitmap
    fun flipStart(): Unit
    fun flipDone(): Unit
}