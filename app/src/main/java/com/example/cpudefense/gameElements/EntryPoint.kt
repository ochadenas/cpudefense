package com.example.cpudefense.gameElements

import android.graphics.Canvas
import android.graphics.Rect
import com.example.cpudefense.networkmap.Network
import com.example.cpudefense.networkmap.Viewport

class EntryPoint(network: Network, gridX: Int, gridY: Int): Chip(network, gridX, gridY)
{
    init {
        chipData.type = ChipType.ENTRY
        actualRect = Rect(0,0,0,0)
    }

    override fun update() {
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
        // super.display(canvas, viewport)
    }
}