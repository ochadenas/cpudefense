package com.example.cpudefense.gameElements

import android.graphics.*
import android.view.MotionEvent
import com.example.cpudefense.*
import com.example.cpudefense.effects.Movable
import com.example.cpudefense.utils.*

class ChipUpgrade(val chipToUpgrade: Chip, val type: Chip.ChipUpgrades,
                  var posX: Int, var posY: Int, val color: Int): Movable
{
    val game = chipToUpgrade.network.theGame
    var actualRect = Rect(chipToUpgrade.actualRect)
    var labelRect = Rect(0,0,0,0)
    private var price = calculatePrice()
    private val paintBackground = Paint()
    private val paintText = Paint()
    private val paintFrame = Paint()

    fun calculatePrice(): Int
    {
        var penalty = 0 // possible price modification
        when (type){
            Chip.ChipUpgrades.POWERUP -> {
                var upgradePrice = chipToUpgrade.chipData.value * 1.5
                val discount = game.heroes[Hero.Type.DECREASE_UPGRADE_COST]?.getStrength() ?: 0f
                upgradePrice = upgradePrice * (100f - discount) / 100
                return upgradePrice.toInt()
            }
            Chip.ChipUpgrades.REDUCE -> {
                return chipToUpgrade.chipData.value
            }
            Chip.ChipUpgrades.SELL -> {
                val refund = - chipToUpgrade.chipData.value * (game.heroes[Hero.Type.INCREASE_REFUND]?.getStrength() ?: 50f) * 0.01f
                return refund.toInt()
            }
            Chip.ChipUpgrades.ACC -> {
                // ACC chips are more expensive if there are already chips of the same type
                game.currentStage?.let {
                    val count = it.chipCount(Chip.ChipType.ACC)
                    penalty = count * count * count
                }
                return Game.basePrice.getOrElse(Chip.ChipUpgrades.ACC) { 20 } + penalty
            }
            else -> return Game.basePrice.getOrElse(type) { 20 }
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
                if (chipToUpgrade.chipData.upgradeLevel == 0)
                    chipToUpgrade.sellChip()
            }
            Chip.ChipUpgrades.SELL -> chipToUpgrade.sellChip()
            Chip.ChipUpgrades.SUB -> chipToUpgrade.setType(Chip.ChipType.SUB)
            Chip.ChipUpgrades.SHR -> chipToUpgrade.setType(Chip.ChipType.SHR)
            Chip.ChipUpgrades.ACC -> chipToUpgrade.setType(Chip.ChipType.ACC)
            Chip.ChipUpgrades.MEM -> chipToUpgrade.setType(Chip.ChipType.MEM)
            Chip.ChipUpgrades.CLK -> chipToUpgrade.setType(Chip.ChipType.CLK)
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
            Chip.ChipUpgrades.CLK -> "CLK"
        }
        val bitmap = Bitmap.createBitmap(actualRect.width(), actualRect.height(), Bitmap.Config.ARGB_8888)
        val rect = Rect(0, 0, bitmap.width, bitmap.height)

        paintFrame.color = if (type== Chip.ChipUpgrades.SELL) Color.RED else color

        val newCanvas = Canvas(bitmap)
        paintText.textSize = Game.chipTextSize * game.resources.displayMetrics.scaledDensity
        paintText.alpha = 255
        paintText.typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        paintText.textAlign = Paint.Align.CENTER
        paintText.color = paintFrame.color
        rect.displayTextCenteredInRect(newCanvas, text, paintText)
        canvas.drawBitmap(bitmap, null, actualRect, paintText)

        paintBackground.color = game.resources.getColor(R.color.network_background)
        paintBackground.alpha = 120
        paintBackground.style = Paint.Style.FILL
        canvas.drawRect(actualRect, paintBackground)

        paintFrame.style = Paint.Style.STROKE
        paintFrame.strokeWidth = 2f
        paintFrame.alpha = 255
        canvas.drawBitmap(bitmap, null, actualRect, paintFrame)
        canvas.drawRect(actualRect, paintFrame)

        /* display the price */
        paintBackground.alpha = 40
        paintBackground.color = Color.BLACK
        paintText.typeface = Typeface.create("sans-serif-condensed", Typeface.ITALIC)
        paintText.textSize = (Game.chipTextSize - 0) * game.resources.displayMetrics.scaledDensity
        paintText.color = if (canAfford()) paintFrame.color else Color.YELLOW
        val priceText = game.scoreBoard.informationToString(price)
        paintText.getTextBounds(priceText, 0, priceText.length, labelRect)
        //
        labelRect.setBottomLeft(actualRect.centerX(), actualRect.top+2)
        labelRect.inflate(1)
        canvas.drawRect(labelRect, paintBackground)
        canvas.drawText(priceText, labelRect.left.toFloat(), labelRect.bottom.toFloat(), paintText)
    }
}

