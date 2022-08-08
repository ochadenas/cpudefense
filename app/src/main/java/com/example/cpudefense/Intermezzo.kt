package com.example.cpudefense

import android.graphics.*
import android.view.MotionEvent
import com.example.cpudefense.effects.Explosion
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.gameElements.Button
import com.example.cpudefense.gameElements.GameElement
import com.example.cpudefense.gameElements.Typewriter
import com.example.cpudefense.networkmap.Viewport
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

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
        if (type == Type.GAME_WON && game.state.phase == Game.GamePhase.INTERMEZZO)
            displayFireworks()
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

    fun displayFireworks()
    {
        if (myArea.width()==0 || (Random.nextFloat() < 0.9f))
            return
        // choose random colour
        var colour: Pair<Int, Int>
        when (Random.nextInt(8))
        {
            0 -> colour = Pair(Color.YELLOW, Color.WHITE)
            1 -> colour = Pair(Color.BLUE, Color.YELLOW)
            2 -> colour = Pair(Color.GREEN, Color.WHITE)
            3 -> colour = Pair(Color.BLUE, Color.WHITE)
            4 -> colour = Pair(Color.GREEN, Color.RED)
            else -> colour = Pair(Color.RED, Color.GREEN)
        }

            game.gameActivity.theGameView.theEffects?.explosions?.add(
                Explosion(game, Pair(Random.nextInt(myArea.width()), Random.nextInt(myArea.height()*8/10)),
                    colour.first, colour.second))
    }

    fun onTypewriterDone()
    {
        showButton()
    }

    fun showButton()
    {
        val bottomMargin = 40
        buttonContinue = Button(game.resources.getString(R.string.button_continue), color = game.resources.getColor(R.color.text_green))
        val buttonTop = myArea.bottom - (buttonContinue?.myArea?.height() ?: 20) - bottomMargin
            buttonContinue?.let {
            Fader(game, it, Fader.Type.APPEAR, Fader.Speed.SLOW)
            it.myArea.set(50, buttonTop, 50+it.myArea.width(), myArea.bottom-bottomMargin)
        }
        if (game.global.coinsTotal > 0) {
            buttonPurchase = Button(game.resources.getString(R.string.button_marketplace), color = game.resources.getColor(R.color.text_blue))
            buttonPurchase?.let {
                Fader(game, it, Fader.Type.APPEAR, Fader.Speed.SLOW)
                it.myArea.set(myArea.right - it.myArea.width() - 50, buttonTop, myArea.right - 50, myArea.bottom - bottomMargin)
            }
        }
    }

    override fun setOpacity(opacity: Float) {
        alpha = (opacity * 255).toInt()
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
        if (game.state.phase != Game.GamePhase.INTERMEZZO)
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
                Type.GAME_WON -> { game.quitGame() }
                Type.GAME_LOST -> { game.quitGame() }
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
        game.state.phase = Game.GamePhase.INTERMEZZO
    }

    fun startMarketplace()
    {
        clear()
        game.marketplace.fillMarket(level)
        game.state.phase = Game.GamePhase.MARKETPLACE
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
        game.state.phase = Game.GamePhase.INTERMEZZO
    }

    fun clear()
    {
        typewriter = null
        instructions = null
        buttonContinue = null
        buttonPurchase = null
    }
}