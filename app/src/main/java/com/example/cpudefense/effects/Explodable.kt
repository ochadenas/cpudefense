package com.example.cpudefense.effects

import android.graphics.Color

interface Explodable {
    fun remove()
    fun getPositionOnScreen(): Pair<Int, Int>

    val explosionColour: Int?
}