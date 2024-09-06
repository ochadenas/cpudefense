@file:Suppress("DEPRECATION")

package com.example.cpudefense

import android.content.res.Resources
import android.graphics.*
import android.view.MotionEvent
import android.widget.Toast
import com.example.cpudefense.effects.Explosion
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.gameElements.Button
import com.example.cpudefense.gameElements.GameElement
import com.example.cpudefense.gameElements.Typewriter
import com.example.cpudefense.networkmap.Viewport
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

/* Ideas for "Did you know":
- Horst Feistel's contributions
- IBM drum calculating machine
- Susan Landau
- esoteric programming languages
 */

class Intermezzo(var gameView: GameView): GameElement(), Fadable {
    var level = Stage.Identifier()
    val resources: Resources = gameView.resources
    val activity = gameView.gameMechanics.gameActivity // TODO: geht das besser?
    var alpha = 0
    private var myArea = Rect()
    private var typewriter: Typewriter? = null
    private var buttonContinue: Button? = null
    private var buttonPurchase: Button? = null
    private var instructions: Instructions? = null
    private var heroSelection: HeroSelection? = null
    var coinsGathered = 0
    var durationOfLeave = 2

    private var textOnContinueButton = ""

    enum class Type {STARTING_LEVEL, NORMAL_LEVEL, GAME_LOST, GAME_WON}
    var type = Type.NORMAL_LEVEL

    fun setSize(area: Rect)
    {
        myArea = Rect(area)
        heroSelection?.setSize(myArea)
    }

    override fun update() {
        if (type == Type.GAME_WON && gameView.gameMechanics.state.phase == GameMechanics.GamePhase.INTERMEZZO)
            displayFireworks()
    }

    override fun fadeDone(type: Fader.Type) {
        alpha = 255
        var showLeaveDialogue = false
        if (oneHeroMustGoOnLeave(level)) {
            heroSelection = HeroSelection()
            heroSelection?.let {
                if (it.heroesAskingToTakeLeave.isNotEmpty())
                    showLeaveDialogue = true
            }
        }
        instructions = Instructions(gameView, level, showLeaveDialogue) { displayText() }
    }

    private fun displayText()
    {
        val lines = CopyOnWriteArrayList<String>()
        when (type)
        {
            Type.GAME_LOST -> {
                lines.add(resources.getString(R.string.failed))
                lines.add(resources.getString(R.string.last_stage).format(level.number))
                textOnContinueButton = resources.getString(R.string.button_retry)
                gameView.gameMechanics.setLastPlayedStage(level) // TODO: sollte woanders erfolgen
            }
            Type.GAME_WON  -> {
                lines.add(resources.getString(R.string.success))
                if (coinsGathered>0)
                    lines.add(resources.getString(R.string.coins_gathered).format(coinsGathered))
                if (level.series == 1)
                {
                    lines.add(resources.getString(R.string.series_completed_message_1))
                    lines.add(resources.getString(R.string.series_completed_message_2))
                    lines.add(resources.getString(R.string.series_completed_message_3))
                    lines.add(resources.getString(R.string.series_completed_message_4))
                }
                else
                    lines.add(resources.getString(R.string.win))
                textOnContinueButton = resources.getString(R.string.button_exit)
                gameView.gameMechanics.setLastPlayedStage(level)
            }
            Type.STARTING_LEVEL -> {
                lines.add(resources.getString(R.string.game_start))
                if (gameView.gameMechanics.currentHeroesOnLeave(level).isNotEmpty())
                    lines += heroesOnLeaveText()
                textOnContinueButton = resources.getString(R.string.enter_game)
                gameView.gameMechanics.setLastPlayedStage(level)
            }
            Type.NORMAL_LEVEL ->
            {
                lines.add(resources.getString(R.string.cleared))
                if (coinsGathered>0)
                    lines.add(resources.getString(R.string.coins_gathered).format(coinsGathered))
                lines.add(resources.getString(R.string.next_stage).format(level.number))
                if (gameView.gameMechanics.currentHeroesOnLeave(level).isNotEmpty())
                    lines += heroesOnLeaveText()
                textOnContinueButton = resources.getString(R.string.enter_game)
                gameView.gameMechanics.setLastPlayedStage(level)
            }
        }
        typewriter = Typewriter(gameView, myArea, lines) { onTypewriterDone() }
    }

    private fun heroesOnLeaveText(): List<String>
    {
        val heroes: List<Hero> = gameView.gameMechanics.currentHeroesOnLeave(level).values.toList()
        when (heroes.size) {
            0 -> return listOf(resources.getString(R.string.heroes_on_leave_0))
            1 -> return listOf(resources.getString(R.string.heroes_on_leave_1),
                               heroes.first().person.fullName)
            else -> return listOf(resources.getString(R.string.heroes_on_leave_1)) +
                    heroes.map { it.person.fullName }
        }
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

        gameView.effects?.explosions?.add(
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
        buttonContinue = Button(gameView, textOnContinueButton,
                                textSize = GameMechanics.computerTextSize * gameView.textScaleFactor,
                                color = resources.getColor(R.color.text_green), style = Button.Style.FILLED)
        val buttonTop = myArea.bottom - (buttonContinue?.area?.height() ?: 20) - bottomMargin
        buttonContinue?.let {
            Fader(gameView, it, Fader.Type.APPEAR, Fader.Speed.SLOW)
            it.alignLeft(50, buttonTop)
        }
        // if (game.global.coinsTotal > 0)  // make button always accessible. Issue #20
        if (level.number > 6 || level.series > 1)  // level 6 in series 1 is the first one where coins may be present
        {
            buttonPurchase = Button(gameView, resources.getString(R.string.button_marketplace),
                                    textSize = GameMechanics.computerTextSize * gameView.textScaleFactor,
                                    color = resources.getColor(R.color.text_blue), style = Button.Style.FILLED)
            buttonPurchase?.let {
                Fader(gameView, it, Fader.Type.APPEAR, Fader.Speed.SLOW)
                it.alignRight(myArea.right, buttonTop)
            }
        }
    }

    override fun setOpacity(opacity: Float) {
        alpha = (opacity * 255).toInt()
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
        if (gameView.gameMechanics.state.phase != GameMechanics.GamePhase.INTERMEZZO)
            return
        val paint = Paint()
        paint.color = Color.BLACK
        paint.alpha = 255 // alpha
        canvas.drawRect(myArea, paint)
        instructions?.display(canvas)
        typewriter?.display(canvas)
        buttonContinue?.display(canvas)
        buttonPurchase?.display(canvas)
        heroSelection?.display(canvas, viewport)
    }

    fun onDown(event: MotionEvent): Boolean {
        /** test if a button has been pressed: */
        if (buttonPurchase?.touchableArea?.contains(event.x.toInt(), event.y.toInt()) == true)
            startMarketplace()
        else if (buttonContinue?.touchableArea?.contains(event.x.toInt(), event.y.toInt()) == true) {
            when (type) {
                Type.GAME_WON -> { gameView.gameMechanics.quitGame() }
                Type.GAME_LOST -> { startLevel() }
                else -> { startLevel() }
            }
            return true
        }
        else if (heroSelection?.onDown(event) == true)
            return true
        return false
    }

    fun prepareLevel(nextLevel: Stage.Identifier, isStartingLevel: Boolean)
    {
        clear()
        this.level = nextLevel
        if (isStartingLevel) {
            type = Type.STARTING_LEVEL
            Fader(gameView, this, Fader.Type.APPEAR, Fader.Speed.FAST)
        }
        else
        {
            type = Type.NORMAL_LEVEL
            Fader(gameView, this, Fader.Type.APPEAR, Fader.Speed.SLOW)
        }
        gameView.background.prepareAtStartOfStage(level)
        gameView.gameMechanics.state.phase = GameMechanics.GamePhase.INTERMEZZO
        activity.setGameActivityStatus(GameActivity.GameActivityStatus.BETWEEN_LEVELS)
    }

    private fun startMarketplace()
    {
        if (holidayGranted()) {
            clear()
            gameView.marketplace.fillMarket(level)
            gameView.gameMechanics.state.phase = GameMechanics.GamePhase.MARKETPLACE
        }
    }

    private fun startLevel()
    {
        if (holidayGranted()) {
            gameView.gameMechanics.startNextStage(level)
        }
    }

    private fun holidayGranted(): Boolean
    {
        if (oneHeroMustGoOnLeave(level))
        {
            if (heroSelection?.selectedHero == null) {
                Toast.makeText(activity, "You must select one hero and give them leave!", Toast.LENGTH_SHORT)
                    .show()
                return false
            }
            heroSelection?.selectedHero?.addLeave(level, durationOfLeave)
            Persistency(activity).saveHolidays(gameView.gameMechanics)
            return true
        }
        else
            return true
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
            Fader(gameView, this, Fader.Type.APPEAR, Fader.Speed.SLOW)
        }
        else {
            type = Type.GAME_LOST
            alpha = 255
            displayText()
        }
        activity.setGameActivityStatus(GameActivity.GameActivityStatus.BETWEEN_LEVELS)
        gameView.gameMechanics.state.phase = GameMechanics.GamePhase.INTERMEZZO
    }

    private fun clear()
    {
        typewriter = null
        instructions = null
        buttonContinue = null
        buttonPurchase = null
        heroSelection = null
    }

    private fun isLevelWhereHeroGoesOnLeave(ident: Stage.Identifier): Boolean
    /** This function also sets the duration of leave (in levels; default is 2) */
    {
        if (ident.series < GameMechanics.SERIES_ENDLESS)
            return false
        durationOfLeave = 1 + ident.number / 100
        return when (ident.number % 4)
        {
            0 -> { ident.number >= 16 }
            1 -> { ident.number >= 256 }
            2 -> { ident.number >= 64 }
            3 -> { ident.number >= 128 }
            else -> false
        }
    }

    private fun oneHeroMustGoOnLeave(ident: Stage.Identifier): Boolean
    {
        return (isLevelWhereHeroGoesOnLeave(ident) && (ident.number !in gameView.gameMechanics.holidays))
    }

    inner class HeroSelection
    {
        private val sizeOfHeroPanel = GameMechanics.numberOfHeroesToChooseFrom
        var heroesAskingToTakeLeave = listOf<Hero>()
        var selectedHero: Hero? = null
        var width: Int = 0
        private var myOuterArea = Rect()
        private var myInnerArea = Rect()
        private var myBackgroundArea = Rect()
        private val backgroundPaint: Paint = Paint()

        init {
            heroesAskingToTakeLeave = choosePossibleHeroes()
            if (heroesAskingToTakeLeave.size > sizeOfHeroPanel)
                heroesAskingToTakeLeave = heroesAskingToTakeLeave.takeLast(sizeOfHeroPanel)
        }

        fun setSize(containingRect: Rect)
        {
            if (heroesAskingToTakeLeave.isEmpty()) return
            heroesAskingToTakeLeave.forEach {
                it.card.create(showNextUpdate = false, monochrome = true)
            }
            val cardWidth  = heroesAskingToTakeLeave.first().card.cardArea.width()
            val cardHeight = heroesAskingToTakeLeave.first().card.cardArea.height()
            val margin = (20 * gameView.scaleFactor).toInt()
            val textLineHeight = GameMechanics.instructionTextSize * gameView.textScaleFactor // approx.
            val top = containingRect.top + (6*textLineHeight).toInt()
            val bottom = top + 2*cardHeight + 3*margin
            myOuterArea = Rect(0, 0, containingRect.right, bottom)
            myBackgroundArea = Rect(myOuterArea.left, top, myOuterArea.right, myOuterArea.bottom)
            myInnerArea = Rect(margin,top+margin,containingRect.right-margin,bottom-margin)

            // if (4*margin+3*cardWidth > containingRect.width())
            //    cardWidth = (containingRect.width()-4*margin) / 3
            val xLeft = myInnerArea.left
            val xRight = myInnerArea.right - cardWidth
            val yBottom = myInnerArea.bottom - cardHeight
            val yTop = myInnerArea.top
            heroesAskingToTakeLeave.forEachIndexed()
            {
                index, hero ->
                when (index)
                {
                    0 -> hero.card.putAt(xLeft, yTop)
                    1 -> hero.card.putAt(xRight, yTop)
                    2 -> hero.card.putAt(xLeft, yBottom)
                    3 -> hero.card.putAt(xRight, yBottom)
                }
            }
        }

        fun display(canvas: Canvas, viewport: Viewport)
        {
            backgroundPaint.color = resources.getColor(R.color.alternate_background)
            backgroundPaint.style = Paint.Style.FILL
            canvas.drawRect(myBackgroundArea, backgroundPaint)
            backgroundPaint.color = resources.getColor(R.color.text_green)
            backgroundPaint.style = Paint.Style.STROKE
            backgroundPaint.strokeWidth = 6f
            canvas.drawRect(myOuterArea, backgroundPaint)
            heroesAskingToTakeLeave.forEach()
            {
                it.card.display(canvas, viewport)
            }

        }

        private fun choosePossibleHeroes(): List<Hero>
                /** returns a list of heroes that may be asking for a leave.
                 * @return list containing the <count> strongest heroes, among those fulfilling certain criteria
                 */
        {
            val count = GameMechanics.numberOfHeroesConsideredForLeave
            val heroesExcluded = listOf(Hero.Type.ENABLE_MEM_UPGRADE, Hero.Type.INCREASE_MAX_HERO_LEVEL )
            var possibleHeroes = gameView.gameMechanics.currentHeroes(level).values.filter {
                it.data.type !in heroesExcluded && !it.isOnLeave(level)
            }.sortedBy { it.data.level }  // list of all heroes that are available, the strongest at the end
            if (possibleHeroes.size > count)
                possibleHeroes = possibleHeroes.takeLast(count)
            return possibleHeroes.shuffled()
        }

        fun onDown(event: MotionEvent): Boolean {
            heroesAskingToTakeLeave.forEach()
            {
                if (it.card.cardAreaOnScreen.contains(event.x.toInt(), event.y.toInt())) {
                    selectForLeave(it)
                    return true
                }
            }
            return false
        }

        private fun selectForLeave(hero: Hero)
        {
            heroesAskingToTakeLeave.filter { it != hero }
                .forEach { it.isOnLeave = false }  // reset the other cards
            hero.isOnLeave = !hero.isOnLeave // toggle this one
            if (hero.isOnLeave)
                selectedHero = hero
            else
                selectedHero = null
        }
    }

}