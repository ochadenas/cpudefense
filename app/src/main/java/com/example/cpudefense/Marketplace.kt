package com.example.cpudefense

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.gameElements.Button
import com.example.cpudefense.gameElements.GameElement
import com.example.cpudefense.networkmap.Viewport
import java.util.concurrent.CopyOnWriteArrayList

class Marketplace(val game: Game): GameElement()
{
    private var buttonFinish: Button? = null
    private var myArea = Rect()

    var upgrades = CopyOnWriteArrayList<Upgrade>()

    fun setSize(area: Rect)
    {
        myArea = Rect(area)
        createButton()
    }

    fun fillMarket()
    {
        upgrades.clear()
        upgrades.add(Upgrade.createFromData(game, Upgrade.Data(type= Upgrade.Type.INCREASE_CHIP_SPEED)))
        upgrades.add(Upgrade.createFromData(game, Upgrade.Data(type= Upgrade.Type.INCREASE_STARTING_CASH)))
        arrangeCards()
    }

    fun arrangeCards()
    /** calculate positions of the cards' rectangles */
    {
        val space = 20
        val offset = Game.cardHeight + space
        var pos = offset
        for (card in upgrades)
        {
            card.areaOnScreen.setTopLeft(space, pos)
            pos += offset
        }
    }


    override fun update() {
    }

    private fun createButton()
    {
        val bottomMargin = 40
        buttonFinish = Button(game.resources.getString(R.string.button_continue))
        buttonFinish?.let {
            Fader(game, it, Fader.Type.APPEAR, Fader.Speed.SLOW)
            it.myArea.setBottomRight(myArea.right-50, myArea.bottom-bottomMargin)
            it.buttonPaint.color = game.resources.getColor(R.color.text_blue)
        }
    }

    fun onDown(event: MotionEvent): Boolean {
        /** test if a button has been pressed: */
        if (buttonFinish?.myArea?.contains(event.x.toInt(), event.y.toInt()) == true)
        {
            game.startNextStage(game.intermezzo.level)
            return true
        }
        for (card in upgrades)
            card.onDown(event)
        return false
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
        if (game.data.state != Game.GameState.MARKETPLACE)
            return
        val paint = Paint()
        paint.color = Color.BLACK
        paint.alpha = 255
        canvas.drawRect(myArea, paint)
        for (card in upgrades)
            card.display(canvas)
        buttonFinish?.display(canvas)

        val textPaint = Paint()
        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 36f
        var text = "Total coins: %d".format(game.data.coinsTotal)
        canvas.drawText(text, 20f, 40f, textPaint)

    }
}