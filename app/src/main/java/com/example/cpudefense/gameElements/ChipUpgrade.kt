package com.example.cpudefense.gameElements

import android.graphics.*
import android.view.MotionEvent
import com.example.cpudefense.*
import com.example.cpudefense.effects.Movable

class ChipUpgrade(val chipToUpgrade: Chip, val type: Chip.ChipUpgrades,
                  var posX: Int, var posY: Int, val color: Int): Movable
{
    val game = chipToUpgrade.network.theGame
    var actualRect = Rect(chipToUpgrade.actualRect)
    var price = calculatePrice()

    fun calculatePrice(): Int
    {
        if (type == Chip.ChipUpgrades.POWERUP) {
            var upgradePrice = chipToUpgrade.chipData.value * 1.5
            var discount = game.gameUpgrades[Upgrade.Type.DECREASE_UPGRADE_COST]?.getStrength() ?: 0f
            upgradePrice = upgradePrice * (100f - discount) / 100
            return upgradePrice.toInt()
        }
        else
            return Game.basePrice.getOrElse(type, { 100 } )
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
            Chip.ChipUpgrades.SUB -> chipToUpgrade.setType(Chip.ChipType.SUB)
            Chip.ChipUpgrades.SHIFT -> chipToUpgrade.setType(Chip.ChipType.SHIFT)
            Chip.ChipUpgrades.AND -> chipToUpgrade.setType(Chip.ChipType.AND)
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
            Chip.ChipUpgrades.SUB -> "SUB"
            Chip.ChipUpgrades.SHIFT -> "SHR"
            Chip.ChipUpgrades.AND -> "AND"
            else -> "?"
        }

        var paint = Paint()
        val bitmap = Bitmap.createBitmap(actualRect.width(), actualRect.height(), Bitmap.Config.ARGB_8888)
        var rect = Rect(0, 0, bitmap.width, bitmap.height)

        val newCanvas = Canvas(bitmap)
        paint.textSize = 24f
        paint.alpha = 255
        paint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD))
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.WHITE
        rect.displayTextCenteredInRect(newCanvas, text, paint)
        canvas.drawBitmap(bitmap, null, actualRect, paint)
        paint.color = chipToUpgrade.theNetwork?.theGame?.resources?.getColor(R.color.network_background) ?: Color.BLACK
        paint.alpha = 40
        paint.style = Paint.Style.FILL
        canvas.drawRect(actualRect, paint)
        paint.style = Paint.Style.STROKE
        paint.color = color
        paint.alpha = 255
        canvas.drawBitmap(bitmap, null, actualRect, paint)
        canvas.drawRect(actualRect, paint)

        /* display the price */
        // val priceRect = Rect(actualRect.right, actualRect.top, actualRect.right+48, actualRect.bottom)
        paint = Paint()
        paint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.ITALIC))
        paint.textSize = Game.Params.chipTextSize - 2
        paint.color = if (canAfford()) Color.WHITE else Color.YELLOW
        canvas.drawText(game.scoreBoard.informationToString(price), actualRect.right.toFloat()+4, actualRect.top.toFloat()+4, paint)
    }
}

