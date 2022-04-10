package com.example.cpudefense.gameElements

import android.graphics.Canvas
import com.example.cpudefense.Chip
import com.example.cpudefense.networkmap.Network
import com.example.cpudefense.networkmap.Viewport

class EntryPoint(network: Network, gridX: Int, gridY: Int): Chip(network, gridX, gridY)
{
    init {
        chipData.type = ChipType.ENTRY
    }

    override fun update() {
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
        super.display(canvas, viewport)
    }
}