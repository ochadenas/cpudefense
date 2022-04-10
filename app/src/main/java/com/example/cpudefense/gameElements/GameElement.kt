package com.example.cpudefense.gameElements

import android.graphics.Canvas
import android.graphics.Rect
import com.example.cpudefense.networkmap.Viewport

abstract class GameElement {
    abstract fun update()

    abstract fun display(canvas: Canvas, viewport: Viewport)
}