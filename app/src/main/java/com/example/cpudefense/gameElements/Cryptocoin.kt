@file:Suppress("DEPRECATION")

package com.example.cpudefense.gameElements

import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.example.cpudefense.GameMechanics
import com.example.cpudefense.GameView
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
        this.animationCount *= 2
    }
    override fun display(canvas: Canvas, viewport: Viewport) {
        val size =  (GameView.coinSizeOnScreen * network.gameView.resources.displayMetrics.scaledDensity).toInt()
        actualRect = Rect(0, 0, size, size)
        actualRect.setCenter(getPositionOnScreen())
        actualRect.offset(displacement.first, displacement.second)
        canvas.drawBitmap(myBitmap, null, actualRect, paint)
    }

    override val explosionColour: Int
        get() = if (network.gameMechanics.currentStageIdent.series == GameMechanics.SERIES_ENDLESS) network.gameView.resources.getColor(R.color.attackers_glow_coin_endless)
                else network.gameView.resources.getColor(R.color.attackers_glow_coin)

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
                    network.gameMechanics.state.coinsExtra++
                    return true
                }
                else {
                    // coin was hit but not destroyed
                    if (!isCurrentlyFlipping)
                        Flipper(network.gameView, this, Flipper.Type.HORIZONTAL, Flipper.Speed.FAST)
                    return false
                }
            }
        }
    }

    override fun makeNumber() {
        myBitmap = this.network.gameView.currentCoinBitmap().copy(this.network.gameView.currentCoinBitmap().config ?: ARGB_8888, true)
    }

    override fun setBitmap(bitmap: Bitmap) {
        myBitmap = bitmap
    }

    override fun provideBitmap(): Bitmap {
        val bitmap = network.gameView.currentCoinBitmap()
        return bitmap.copy(bitmap.config ?: ARGB_8888, true)
    }

    override fun flipStart() {
        isCurrentlyFlipping = true
    }

    override fun flipDone() {
        isCurrentlyFlipping = false
    }
}
