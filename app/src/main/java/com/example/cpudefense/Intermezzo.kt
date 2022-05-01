package com.example.cpudefense

import android.graphics.*
import android.view.MotionEvent
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.gameElements.Button
import com.example.cpudefense.gameElements.GameElement
import com.example.cpudefense.gameElements.Typewriter
import com.example.cpudefense.networkmap.Viewport
import java.util.concurrent.CopyOnWriteArrayList

class Intermezzo(var game: Game): GameElement(), Fadable {
    var level = 0
    var alpha = 0
    var myArea = Rect()
    var typewriter: Typewriter? = null
    var buttonContinue: Button? = null
    var buttonPurchase: Button? = null
    var instructions: Instructions? = null
    var coinsGathered = 0

    enum class Type {STARTING_LEVEL, NORMAL_LEVEL, GAME_LOST, GAME_WON}
    var type = Type.NORMAL_LEVEL

    fun setSize(area: Rect)
    {
        myArea = Rect(area)
    }

    override fun update() {
    }

    override fun fadeDone(faderType: Fader.Type) {
        alpha = 255
        instructions = Instructions(game, level, { displayText() })
    }

    fun displayText()
    {
        val lines = CopyOnWriteArrayList<String>()
        when (type)
        {
            Type.GAME_LOST -> {
                lines.add(game.resources.getString(R.string.failed))
                lines.add(game.resources.getString(R.string.last_stage).format(level))
            }
            Type.GAME_WON  -> {
                lines.add(game.resources.getString(R.string.success))
                if (coinsGathered>0)
                    lines.add(game.resources.getString(R.string.coins_gathered).format(coinsGathered))
                lines.add(game.resources.getString(R.string.win))
            }
            Type.STARTING_LEVEL -> lines.add(game.resources.getString(R.string.game_start))
            Type.NORMAL_LEVEL ->
            {
                lines.add(game.resources.getString(R.string.cleared))
                if (coinsGathered>0)
                    lines.add(game.resources.getString(R.string.coins_gathered).format(coinsGathered))
                lines.add(game.resources.getString(R.string.next_stage).format(level))
            }
        }
        typewriter = Typewriter(game, myArea, lines, { onTypewriterDone() })
    }

    fun onTypewriterDone()
    {
        showButton()
    }

    fun showButton()
    {
        val bottomMargin = 40
        buttonContinue = Button(game.resources.getString(R.string.button_continue))
        buttonContinue?.let {
            Fader(game, it, Fader.Type.APPEAR, Fader.Speed.SLOW)
            it.myArea.set(50, myArea.bottom-it.myArea.height()-bottomMargin, 50+it.myArea.width(), myArea.bottom-bottomMargin)
            it.buttonPaint.color = game.resources.getColor(R.color.text_green)
        }
        buttonPurchase = Button(game.resources.getString(R.string.button_purchase))
        buttonPurchase?.let {
            Fader(game, it, Fader.Type.APPEAR, Fader.Speed.SLOW)
            it.myArea.set(myArea.right-it.myArea.width()-50, myArea.bottom-it.myArea.height()-bottomMargin, myArea.right-50, myArea.bottom-bottomMargin)
            it.buttonPaint.color = game.resources.getColor(R.color.text_blue)
        }
    }

    override fun setOpacity(opacity: Float) {
        alpha = (opacity * 255).toInt()
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
        if (game.data.state != Game.GameState.INTERMEZZO)
            return
        val paint = Paint()
        paint.color = Color.BLACK
        paint.alpha = alpha
        canvas.drawRect(myArea, paint)
        instructions?.display(canvas)
        typewriter?.display(canvas)
        buttonContinue?.display(canvas)
        buttonPurchase?.display(canvas)
    }

    fun onDown(event: MotionEvent): Boolean {
        /** test if a button has been pressed: */
        if (buttonPurchase?.myArea?.contains(event.x.toInt(), event.y.toInt()) == true)
            startMarketplace()
        else if (buttonContinue?.myArea?.contains(event.x.toInt(), event.y.toInt()) == true) {
            when (type) {
                Type.GAME_WON -> {
                    game.quitGame()
                }
                Type.GAME_LOST -> {
                    game.quitGame()
                }
                else -> {
                    game.startNextStage(level)
                }
            }
            return true
        }
        return false
    }

    fun prepareLevel(nextLevel: Int, isStartingLevel: Boolean)
    {
        clear()
        this.level = nextLevel
        if (isStartingLevel) {
            type = Type.STARTING_LEVEL
            Fader(game, this, Fader.Type.APPEAR, Fader.Speed.FAST)
        }
        else
        {
            type = Type.NORMAL_LEVEL
            Fader(game, this, Fader.Type.APPEAR, Fader.Speed.SLOW)
        }
        game.data.state = Game.GameState.INTERMEZZO
    }

    fun startMarketplace()
    {
        clear()
        game.marketplace.fillMarket()
        game.data.state = Game.GameState.MARKETPLACE
    }

    fun endOfGame(lastLevel: Int, hasWon: Boolean)
    {
        clear()
        this.level = lastLevel
        if (hasWon) {
            type = Type.GAME_WON
            Fader(game, this, Fader.Type.APPEAR, Fader.Speed.SLOW)
        }
        else {
            type = Type.GAME_LOST
            alpha = 255
            displayText()
        }
        game.data.state = Game.GameState.INTERMEZZO
    }

    fun clear()
    {
        typewriter = null
        instructions = null
        buttonContinue = null
    }
}