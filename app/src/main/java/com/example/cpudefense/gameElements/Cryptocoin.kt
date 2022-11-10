package com.example.cpudefense.gameElements

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.example.cpudefense.Game
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.setCenter

class Cryptocoin(network: com.example.cpudefense.networkmap.Network, number: ULong = 1u, speed: Float = 1.0f):
    Attacker(network, Representation.BINARY, number, speed)
{
    override fun display(canvas: Canvas, viewport: Viewport) {
        actualRect = Rect(0,0, Game.coinSizeOnScreen, Game.coinSizeOnScreen)
        actualRect.setCenter(getPositionOnScreen())
        actualRect.offset(displacement.first, displacement.second)
        var paint = Paint()
        canvas.drawBitmap(theNetwork.theGame.coinIcon, null, actualRect, paint)
    }

    override fun onShot(type: Chip.ChipType, power: Int): Boolean {
        if (super.onShot(type, power))
        {
            theNetwork.theGame.state.coinsExtra++
            return true
        }
        return false
    }
}
