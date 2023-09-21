package com.example.cpudefense.gameElements

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.example.cpudefense.Game
import com.example.cpudefense.R
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.effects.Flippable
import com.example.cpudefense.effects.Flipper
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.setCenter

class Cryptocoin(network: com.example.cpudefense.networkmap.Network, number: ULong = 1u, speed: Float = 1.0f):
    Attacker(network, Representation.BINARY, number, speed), Flippable
{
    var paint = Paint()
    var myBitmap = theNetwork.theGame.coinIcon.copy(theNetwork.theGame.coinIcon.config, true)
    var isCurrentlyFlipping = false

    init {
        this.attackerData.isCoin = true
        this.animationCount = 2 * animationCount
    }
    override fun display(canvas: Canvas, viewport: Viewport) {
        val size =  (Game.coinSizeOnScreen * theNetwork.theGame.resources.displayMetrics.scaledDensity).toInt()
        actualRect = Rect(0, 0, size, size)
        actualRect.setCenter(getPositionOnScreen())
        actualRect.offset(displacement.first, displacement.second)
        canvas.drawBitmap(myBitmap, null, actualRect, paint)
    }

    override val explosionColour: Int?
        get() = theNetwork.theGame.resources.getColor(R.color.attackers_glow_coin)

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
                    if (!isCurrentlyFlipping)
                        Flipper(theNetwork.theGame, this, Flipper.Type.HORIZONTAL, Flipper.Speed.FAST)
                    return false
                }
            }
        }
    }

    override fun setBitmap(bitmap: Bitmap) {
        myBitmap = bitmap
    }

    override fun provideBitmap(): Bitmap {
        return theNetwork.theGame.coinIcon.copy(theNetwork.theGame.coinIcon.config, true)
    }

    override fun flipStart() {
        isCurrentlyFlipping = true
    }

    override fun flipDone() {
        isCurrentlyFlipping = false
    }
}
