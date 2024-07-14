package com.example.cpudefense.gameElements

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.example.cpudefense.Game
import com.example.cpudefense.R
import com.example.cpudefense.effects.Flippable
import com.example.cpudefense.effects.Flipper
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.setCenter

class Cryptocoin(network: com.example.cpudefense.networkmap.Network, number: ULong = 1u, speed: Float = 1.0f):
    Attacker(network, Representation.BINARY, number, speed), Flippable
{
    var paint = Paint()
    private var isCurrentlyFlipping = false
    private var myBitmap: Bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)

    init {
        this.attackerData.isCoin = true
        this.animationCount = 2 * animationCount
    }
    override fun display(canvas: Canvas, viewport: Viewport) {
        val size =  (Game.coinSizeOnScreen * network.theGame.resources.displayMetrics.scaledDensity).toInt()
        actualRect = Rect(0, 0, size, size)
        actualRect.setCenter(getPositionOnScreen())
        actualRect.offset(displacement.first, displacement.second)
        canvas.drawBitmap(myBitmap, null, actualRect, paint)
    }

    override val explosionColour: Int
        get() = if (network.theGame.currentStage.series == Game.SERIES_ENDLESS) network.theGame.resources.getColor(R.color.attackers_glow_coin_endless)
                else network.theGame.resources.getColor(R.color.attackers_glow_coin)

    override fun onShot(type: Chip.ChipType, power: Int): Boolean
    {
        when (type)
        {
            Chip.ChipType.ACC -> return false
            Chip.ChipType.MEM -> return false
            Chip.ChipType.DUP -> return false
            Chip.ChipType.SPLT -> return false
            else -> {
                if (super.onShot(type, power))
                {
                    network.theGame.state.coinsExtra++
                    return true
                }
                else {
                    // coin was hit but not destroyed
                    if (!isCurrentlyFlipping)
                        Flipper(network.theGame, this, Flipper.Type.HORIZONTAL, Flipper.Speed.FAST)
                    return false
                }
            }
        }
    }

    override fun makeNumber() {
        myBitmap = this.network.theGame.currentCoinBitmap().copy(this.network.theGame.currentCoinBitmap().config, true)
    }

    override fun setBitmap(bitmap: Bitmap) {
        myBitmap = bitmap
    }

    override fun provideBitmap(): Bitmap {
        val bitmap = network.theGame.currentCoinBitmap()
        return bitmap.copy(bitmap.config, true)
    }

    override fun flipStart() {
        isCurrentlyFlipping = true
    }

    override fun flipDone() {
        isCurrentlyFlipping = false
    }
}
