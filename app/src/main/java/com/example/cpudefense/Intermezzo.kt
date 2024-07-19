@file:Suppress("DEPRECATION")

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
import com.example.cpudefense.utils.shrink
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

class Intermezzo(var game: Game): GameElement(), Fadable {
    var level = Stage.Identifier()
    var alpha = 0
    private var myArea = Rect()
    private var typewriter: Typewriter? = null
    private var buttonContinue: Button? = null
    private var buttonPurchase: Button? = null
    private var instructions: Instructions? = null
    private var heroSelection: HeroSelection? = null
    var coinsGathered = 0

    private var textOnContinueButton = ""

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

    override fun fadeDone(type: Fader.Type) {
        alpha = 255
        instructions = Instructions(game, level) { displayText() }
        heroSelection = HeroSelection()
    }

    private fun displayText()
    {
        val lines = CopyOnWriteArrayList<String>()
        when (type)
        {
            Type.GAME_LOST -> {
                lines.add(game.resources.getString(R.string.failed))
                lines.add(game.resources.getString(R.string.last_stage).format(level.number))
                textOnContinueButton = game.resources.getString(R.string.button_retry)
                game.setLastPlayedStage(level)
            }
            Type.GAME_WON  -> {
                lines.add(game.resources.getString(R.string.success))
                if (coinsGathered>0)
                    lines.add(game.resources.getString(R.string.coins_gathered).format(coinsGathered))
                if (level.series == 1)
                {
                    lines.add(game.resources.getString(R.string.series_completed_message_1))
                    lines.add(game.resources.getString(R.string.series_completed_message_2))
                    lines.add(game.resources.getString(R.string.series_completed_message_3))
                    lines.add(game.resources.getString(R.string.series_completed_message_4))
                }
                else
                    lines.add(game.resources.getString(R.string.win))
                textOnContinueButton = game.resources.getString(R.string.button_exit)
                game.setLastPlayedStage(level)
            }
            Type.STARTING_LEVEL -> {
                lines.add(game.resources.getString(R.string.game_start))
                textOnContinueButton = game.resources.getString(R.string.enter_game)
                game.setLastPlayedStage(level)
            }
            Type.NORMAL_LEVEL ->
            {
                lines.add(game.resources.getString(R.string.cleared))
                if (coinsGathered>0)
                    lines.add(game.resources.getString(R.string.coins_gathered).format(coinsGathered))
                lines.add(game.resources.getString(R.string.next_stage).format(level.number))
                textOnContinueButton = game.resources.getString(R.string.enter_game)
                game.setLastPlayedStage(level)
            }
        }
        typewriter = Typewriter(game, myArea, lines) { onTypewriterDone() }
    }

    private fun displayFireworks()
    {
        if (myArea.width()==0)
            return
        val frequency = if (level.series == 1) 0.96f else 0.92f
        if (Random.nextFloat() < frequency)  // control amount of fireworks
            return
        // choose random colour
        val colour: Pair<Int, Int>
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
                Explosion(Pair(Random.nextInt(myArea.width()), Random.nextInt(myArea.height()*8/10)),
                    colour.first, colour.second))
    }

    private fun onTypewriterDone()
    {
        showButton()
    }

    private fun showButton()
    {
        val bottomMargin = 40
        buttonContinue = Button(game, textOnContinueButton,
            textSize = Game.computerTextSize * game.resources.displayMetrics.scaledDensity,
            color = game.resources.getColor(R.color.text_green), style = Button.Style.FILLED)
        val buttonTop = myArea.bottom - (buttonContinue?.area?.height() ?: 20) - bottomMargin
        buttonContinue?.let {
            Fader(game, it, Fader.Type.APPEAR, Fader.Speed.SLOW)
            it.alignLeft(50, buttonTop)
        }
        // if (game.global.coinsTotal > 0)  // make button always accessible. Issue #20
        if (level.number > 6 || level.series > 1)  // level 6 in series 1 is the first one where coins may be present
        {
            buttonPurchase = Button(game, game.resources.getString(R.string.button_marketplace),
                textSize = Game.computerTextSize * game.resources.displayMetrics.scaledDensity,
                color = game.resources.getColor(R.color.text_blue), style = Button.Style.FILLED)
            buttonPurchase?.let {
                Fader(game, it, Fader.Type.APPEAR, Fader.Speed.SLOW)
                it.alignRight(myArea.right, buttonTop)
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
        heroSelection?.display(canvas)
    }

    fun onDown(event: MotionEvent): Boolean {
        /** test if a button has been pressed: */
        if (buttonPurchase?.touchableArea?.contains(event.x.toInt(), event.y.toInt()) == true)
            startMarketplace()
        else if (buttonContinue?.touchableArea?.contains(event.x.toInt(), event.y.toInt()) == true) {
            when (type) {
                Type.GAME_WON -> { game.quitGame() }
                Type.GAME_LOST -> { game.startNextStage(level) }
                else -> { game.startNextStage(level) }
            }
            return true
        }
        return false
    }

    fun prepareLevel(nextLevel: Stage.Identifier, isStartingLevel: Boolean)
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
        game.gameActivity.setGameActivityStatus(MainGameActivity.GameActivityStatus.BETWEEN_LEVELS)
    }

    private fun startMarketplace()
    {
        clear()
        game.marketplace.fillMarket(level)
        game.state.phase = Game.GamePhase.MARKETPLACE
    }

    fun endOfGame(lastLevel: Stage.Identifier, hasWon: Boolean)
            /** called when the game is definitely over. Either because the player has completed all levels,
             * or lost all lives.
             * @param lastLevel The level that has been played
             * @param hasWon False if all lives have been lost
             */
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
        game.gameActivity.setGameActivityStatus(MainGameActivity.GameActivityStatus.BETWEEN_LEVELS)
        game.state.phase = Game.GamePhase.INTERMEZZO
    }

    private fun clear()
    {
        typewriter = null
        instructions = null
        buttonContinue = null
        buttonPurchase = null
    }

    inner class HeroSelection
    {
        val sizeOfHeroPanel = 3
        var heroesAskingToTakeLeave = listOf<Hero>()
        var myArea = Rect()
        var width: Int = 0

        init {
            heroesAskingToTakeLeave = choosePossibleHeroes(5)
            if (heroesAskingToTakeLeave.size > sizeOfHeroPanel)
                heroesAskingToTakeLeave = heroesAskingToTakeLeave.takeLast(sizeOfHeroPanel)
            setSize(Intermezzo@myArea)
        }

        fun setSize(containingRect: Rect)
        {
            var margin = 10
            var myArea = Rect(containingRect)
            myArea.shrink(margin)
            var width = if (4*margin+3*Game.cardWidth>containingRect.width())
                (containingRect.width()-4*margin) / 3
            else
                Game.cardWidth



        }

        fun display(canvas: Canvas)
        {
            heroesAskingToTakeLeave.forEachIndexed()
            { index, hero ->
                hero.display(canvas)
            }

        }

        private fun choosePossibleHeroes(count: Int): List<Hero>
                /** returns a list of heroes that may be asking for a leave.
                 * @param count max. number of heroes returned
                 * @return list containing the <count> strongest heroes, among those fulfilling certain criteria
                 */
        {
            val heroesExcluded = listOf<Hero.Type>( Hero.Type.ENABLE_MEM_UPGRADE, Hero.Type.INCREASE_MAX_HERO_LEVEL )
            var possibleHeroes = game.currentHeroes(level).values.filter {
                it.data.type !in heroesExcluded
            }.sortedBy { it.data.level }  // list of all heroes that are available, the strongest at the end
            if (possibleHeroes.size > count)
                possibleHeroes = possibleHeroes.takeLast(count)
            return possibleHeroes.shuffled()
        }


    }

}