package com.example.cpudefense.gameElements

import android.graphics.*
import android.view.MotionEvent
import com.example.cpudefense.*
import com.example.cpudefense.effects.Movable
import com.example.cpudefense.utils.displayTextCenteredInRect
import com.example.cpudefense.utils.setCenter

class ChipUpgrade(val chipToUpgrade: Chip, val type: Chip.ChipUpgrades,
                  var posX: Int, var posY: Int, val color: Int): Movable
{
    val game = chipToUpgrade.network.theGame
    var actualRect = Rect(chipToUpgrade.actualRect)
    var price = calculatePrice()
    val paintBackground = Paint()
    val paintText = Paint()
    val paintFrame = Paint()

    fun calculatePrice(): Int
    {
        when (type){
            Chip.ChipUpgrades.POWERUP -> {
                var upgradePrice = chipToUpgrade.chipData.value * 1.5
                val discount = game.gameUpgrades[Hero.Type.DECREASE_UPGRADE_COST]?.getStrength() ?: 0f
                upgradePrice = upgradePrice * (100f - discount) / 100
                return upgradePrice.toInt()
            }
            Chip.ChipUpgrades.REDUCE -> {
                var upgradePrice = chipToUpgrade.chipData.value
                return upgradePrice.toInt()
            }
            Chip.ChipUpgrades.SELL -> {
                val refund = - chipToUpgrade.chipData.value * (game.gameUpgrades[Hero.Type.INCREASE_REFUND]?.getStrength() ?: 50f) * 0.01f
                return refund.toInt()
            }
            else -> return Game.basePrice.getOrElse(type, { 100 } )
        }
    }

    fun canAfford(): Boolean
    {
        return (price<=game.state.cash)
    }

    override fun moveStart() {

    }

    override fun moveDone() {

    }

    override fun setCenter(x: Int, y: Int) {
        posX = x; posY = y
    }

    fun onDown(event: MotionEvent): Boolean {
        if (actualRect.contains(event.x.toInt(), event.y.toInt()))
        {
            if (canAfford())
                buyUpgrade(type)
            return true
        }
        else
            return false
    }

    fun buyUpgrade(type: Chip.ChipUpgrades)
    {
        when (type)
        {
            Chip.ChipUpgrades.POWERUP ->
            {
                chipToUpgrade.addPower(1)
                chipToUpgrade.chipData.value += price
            }
            Chip.ChipUpgrades.REDUCE ->
            {
                chipToUpgrade.addPower(-1)
                if (chipToUpgrade.chipData.power == 0)
                    chipToUpgrade.resetToEmptyChip()
            }
            Chip.ChipUpgrades.SELL -> chipToUpgrade.resetToEmptyChip()
            Chip.ChipUpgrades.SUB -> chipToUpgrade.setType(Chip.ChipType.SUB)
            Chip.ChipUpgrades.SHR -> chipToUpgrade.setType(Chip.ChipType.SHR)
            Chip.ChipUpgrades.ACC -> chipToUpgrade.setType(Chip.ChipType.ACC)
            Chip.ChipUpgrades.MEM -> chipToUpgrade.setType(Chip.ChipType.MEM)
            else -> {}
        }
        game.state.cash -= price
    }

    fun display(canvas: Canvas)
    {
        if (actualRect.width() == 0 || actualRect.height() == 0)
            return
        actualRect.setCenter(posX, posY)

        val text = when(type)
        {
            Chip.ChipUpgrades.POWERUP -> "+1"
            Chip.ChipUpgrades.REDUCE -> "-1"
            Chip.ChipUpgrades.SELL -> "SELL"
            Chip.ChipUpgrades.SUB -> "SUB"
            Chip.ChipUpgrades.SHR -> "SHR"
            Chip.ChipUpgrades.ACC -> "ACC"
            Chip.ChipUpgrades.MEM -> "MEM"
            else -> "?"
        }
        val bitmap = Bitmap.createBitmap(actualRect.width(), actualRect.height(), Bitmap.Config.ARGB_8888)
        val rect = Rect(0, 0, bitmap.width, bitmap.height)

        paintFrame.color = if (type== Chip.ChipUpgrades.SELL) Color.RED else color

        val newCanvas = Canvas(bitmap)
        paintText.textSize = Game.chipTextSize
        paintText.alpha = 255
        paintText.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD))
        paintText.textAlign = Paint.Align.CENTER
        paintText.color = paintFrame.color
        rect.displayTextCenteredInRect(newCanvas, text, paintText)
        canvas.drawBitmap(bitmap, null, actualRect, paintText)

        paintBackground.color = chipToUpgrade.theNetwork?.theGame?.resources?.getColor(R.color.network_background) ?: Color.BLACK
        paintBackground.alpha = 80
        paintBackground.style = Paint.Style.FILL
        canvas.drawRect(actualRect, paintBackground)

        paintFrame.style = Paint.Style.STROKE
        paintFrame.strokeWidth = 2f
        paintFrame.alpha = 255
        canvas.drawBitmap(bitmap, null, actualRect, paintFrame)
        canvas.drawRect(actualRect, paintFrame)

        /* display the price */
        val priceRect = Rect(actualRect.right - 10, actualRect.top - 20, actualRect.right+50, actualRect.top)
        paintBackground.alpha = 160
        canvas.drawRect(priceRect, paintBackground)
        paintText.typeface = Typeface.create("sans-serif-condensed", Typeface.ITALIC)
        paintText.textSize = Game.chipTextSize - 0
        paintText.color = if (canAfford()) paintFrame.color else Color.YELLOW
        canvas.drawText(game.scoreBoard.informationToString(price), actualRect.right.toFloat()-4, actualRect.top.toFloat()+6, paintText)
    }
}

