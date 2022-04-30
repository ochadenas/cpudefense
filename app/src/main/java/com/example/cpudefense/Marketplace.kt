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

class Marketplace(val game: Game): GameElement()
{
    private var buttonFinish: Button? = null
    private var myArea = Rect()

    fun setSize(area: Rect)
    {
        myArea = Rect(area)
        createButton()
    }

    override fun update() {
    }

    private fun createButton()
    {
        val bottomMargin = 40
        buttonFinish = Button(game.resources.getString(R.string.button_continue))
        buttonFinish?.let {
            Fader(game, it, Fader.Type.APPEAR, Fader.Speed.SLOW)
            it.myArea.set(50, myArea.bottom-it.myArea.height()-bottomMargin, 50+it.myArea.width(), myArea.bottom-bottomMargin)
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
        return false
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
        if (game.data.state != Game.GameState.MARKETPLACE)
            return
        val paint = Paint()
        paint.color = Color.BLACK
        paint.alpha = 255
        canvas.drawRect(myArea, paint)
        buttonFinish?.display(canvas)
    }
}