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

    private var upgrades = CopyOnWriteArrayList<Upgrade>()

    fun setSize(area: Rect)
    {
        myArea = Rect(area)
        createButton()
    }

    fun fillMarket()
    {
        upgrades.clear()
        for (type in Upgrade.Type.values())
        {
            /* if upgrade already exists (because it has been bought earlier),
            get it from the game data. Otherwise, create an empty card.
             */
            var upgrade: Upgrade? = game.gameUpgrades[type]
            if (upgrade == null)
                upgrade = Upgrade.createFromData(game, Upgrade.Data(type))
            upgrades.add(upgrade)
        }
        arrangeCards()
    }

    private fun arrangeCards()
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
        {
            if (card.areaOnScreen.contains(event.x.toInt(), event.y.toInt())) {
                if (game.global.coinsTotal >= card.getPrice(card.data.level)) {
                    game.global.coinsTotal -= 1
                    card.doUpgrade()
                }
                return true
            }
        }
        return false
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
        if (game.state.phase != Game.GamePhase.MARKETPLACE)
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
        val text = "Total coins: %d".format(game.global.coinsTotal)
        canvas.drawText(text, 20f, 40f, textPaint)

    }
}