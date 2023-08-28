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
    init {
        this.attackerData.isCoin = true
        this.animationCount = 2 * animationCount
    }
    override fun display(canvas: Canvas, viewport: Viewport) {
        val size =  (Game.coinSizeOnScreen * theNetwork.theGame.resources.displayMetrics.scaledDensity).toInt()
        actualRect = Rect(0, 0, size, size)
        actualRect.setCenter(getPositionOnScreen())
        actualRect.offset(displacement.first, displacement.second)
        if (animationCount>0)
        // animate the coin when shot at by a chip
        {
            val rectHeight = actualRect.height() * (animationCountMax-2*animationCount)/animationCountMax
            val top =  actualRect.centerY()+rectHeight/2
            val bottom =  actualRect.centerY()-rectHeight/2
            if (top<bottom)
                actualRect.set(actualRect.left, top, actualRect.right, bottom)
            else
                actualRect.set(actualRect.left, bottom, actualRect.right, top)
            animationCount--
        }
        var paint = Paint()
        canvas.drawBitmap(theNetwork.theGame.coinIcon, null, actualRect, paint)
    }

    override fun onShot(type: Chip.ChipType, power: Int): Boolean
    {
        when (type)
        {
            Chip.ChipType.ACC -> return false
            Chip.ChipType.MEM -> return false
            else -> {
                if (super.onShot(type, power))
                {
                    theNetwork.theGame.state.coinsExtra++
                    return true
                }
                else {
                    // coin was hit but not destroyed
                    animationCount = animationCountMax
                    return false
                }
            }
        }
    }
}
