@file:Suppress("DEPRECATION")

package com.example.cpudefense

import android.content.res.Resources
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.MotionEvent
import android.widget.Toast
import com.example.cpudefense.activities.GameActivity
import com.example.cpudefense.effects.Explosion
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.gameElements.Attacker
import com.example.cpudefense.gameElements.Button
import com.example.cpudefense.gameElements.GameElement
import com.example.cpudefense.gameElements.Typewriter
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.shiftBy
import com.example.cpudefense.utils.shrink
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

/* Ideas for "Did you know":
- Horst Feistel's contributions
- IBM drum calculating machine
- Susan Landau
- esoteric programming languages
- six women programmers for ENIAC (cf. Frances Spence)
 */

class Intermezzo(var gameView: GameView): GameElement(), Fadable {
    var level = Stage.Identifier()
    val resources: Resources = gameView.resources
    var alpha = 0
    private var paintLine = Paint()
    private var myArea = Rect()
    private var typewriter: Typewriter? = null
    private var buttonContinue: Button? = null
    private var buttonPurchase: Button? = null
    private var instructions: Instructions? = null
    /** a panel containing several heroes to choose from */
    private var heroSelection: HeroSelection? = null
    /** whether the player must choose a hero to go on leave */
    private var showLeaveDialogue = false

    var coinsGathered = 0
    var durationOfLeave = 2

    /** used for vertical scrolling */
    var vertOffset = 0f
    private val widthOfConsoleLine = 4
    private var textOnContinueButton = ""

    enum class Type {STARTING_LEVEL, NORMAL_LEVEL, GAME_LOST, GAME_WON}
    var type = Type.NORMAL_LEVEL

    fun setSize(area: Rect)
    {
        myArea = Rect(area)
        heroSelection?.adjustArea()
    }

    override fun update() {
        if (type == Type.GAME_WON && gameView.gameMechanics.state.phase == GameMechanics.GamePhase.INTERMEZZO)
            displayFireworks()
    }

    override fun fadeDone(type: Fader.Type) {
        alpha = 255
        instructions = Instructions(gameView, level, showLeaveDialogue) { displayTypewriterText() }
        heroSelection?.prepareScreen()
    }

    private fun displayTypewriterText()
    {
        val lines = CopyOnWriteArrayList<String>()
        when (type)
        {
            Type.GAME_LOST -> {
                lines.add(resources.getString(R.string.failed))
                lines.add(resources.getString(R.string.last_stage).format(level.number))
                textOnContinueButton = resources.getString(R.string.button_retry)
                gameView.gameActivity.setLastPlayedStage(level)
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
                gameView.gameActivity.setLastPlayedStage(level)
            }
            Type.STARTING_LEVEL -> {
                lines.add(resources.getString(R.string.game_start))
                if (gameView.gameMechanics.currentHeroesOnLeave(level).isNotEmpty())
                    lines += heroesOnLeaveText()
                textOnContinueButton = resources.getString(R.string.enter_game)
                gameView.gameActivity.setLastPlayedStage(level)
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
                gameView.gameActivity.setLastPlayedStage(level)
            }
        }
        typewriter = Typewriter(gameView, myArea, lines) { onTypewriterDone() }
    }

    private fun heightOfConsoleLine(): Int
    /** @return the y position of the green line that separates the typewriter text */
    {
        var y = myArea.bottom - Typewriter.heightOfEmptyTypewriterArea
        typewriter?.let { y = it.topOfTypewriterArea() }
        return y
    }

    private fun displayLine(canvas: Canvas, y: Int)
    /** paints the green line that separates the typewriter text */
    {
        paintLine.style = Paint.Style.FILL_AND_STROKE
        paintLine.color = resources.getColor(R.color.text_green)
        canvas.drawRect(Rect(0, y-widthOfConsoleLine, gameView.right, y), paintLine)
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
                                textSize = GameView.computerTextSize * gameView.textScaleFactor,
                                color = resources.getColor(R.color.text_green), style = Button.Style.FILLED)
        val buttonTop = myArea.bottom - (buttonContinue?.area?.height() ?: 20) - bottomMargin
        buttonContinue?.let {
            Fader(gameView, it, Fader.Type.APPEAR, Fader.Speed.SLOW)
            it.alignLeft(bottomMargin, buttonTop)
        }
        // if (game.global.coinsTotal > 0)  // make button always accessible. Issue #20
        if (level.number > 6 || level.series != GameMechanics.SERIES_NORMAL)  // level 6 in series 1 is the first one where coins may be present
        {
            buttonPurchase = Button(gameView, resources.getString(R.string.button_marketplace),
                                    textSize = GameView.computerTextSize * gameView.textScaleFactor,
                                    color = resources.getColor(R.color.text_blue), style = Button.Style.FILLED)
            buttonPurchase?.let {
                Fader(gameView, it, Fader.Type.APPEAR, Fader.Speed.SLOW)
                it.alignRight(myArea.right-bottomMargin, buttonTop)
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
        instructions?.setTextArea(Rect(myArea.left,0,myArea.right,heightOfConsoleLine()))
        instructions?.display(canvas)
        typewriter?.display(canvas)
        buttonContinue?.display(canvas)
        buttonPurchase?.display(canvas)
        heroSelection?.display(canvas)
        displayLine(canvas, heightOfConsoleLine())
    }

    fun onDown(event: MotionEvent): Boolean {
        /** test if a button has been pressed: */
        if (buttonPurchase?.touchableArea?.contains(event.x.toInt(), event.y.toInt()) == true)
            startMarketplace()
        else if (buttonContinue?.touchableArea?.contains(event.x.toInt(), event.y.toInt()) == true) {
            when (type) {
                Type.GAME_WON -> { gameView.gameActivity.finish() }
                Type.GAME_LOST -> { startLevel() }
                else -> { startLevel() }
            }
            return true
        }
        else if (heroSelection?.onDown(event) == true)
            return true
        return false
    }

    @Suppress("UNUSED_PARAMETER")
    fun onScroll(event1: MotionEvent?, event2: MotionEvent?, dX: Float, dY: Float): Boolean {
        val scrollFactor = 1.1f  // higher values make scrolling faster
        if (dY == 0f)
            return false  // only vertical movements are considered here
        event1?.let {
            val max = maxVerticalDisplacement()
            vertOffset += dY * scrollFactor
            if (vertOffset > max)
                vertOffset = max
            if (vertOffset < 0f)
                vertOffset = 0f
            instructions?.vertOffset = vertOffset
        }
        return true
    }

    private fun maxVerticalDisplacement(): Float
    {
        var max = 0
        if (showLeaveDialogue)
            heroSelection?.let { selection ->
                selection.bitmap?.height?.let { max = it - myArea.height()}
            }
        else instructions?.let {
            max = it.bitmap.height - it.myArea.height()
        }
        if (max<0)
            max = 0
        return max.toFloat()
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
        gameView.gameActivity.changeToGamePhase(GameMechanics.GamePhase.INTERMEZZO)

        // determine whether the "hero selection screen" must be displayed
        showLeaveDialogue = false
        if (oneHeroMustGoOnLeave(level)) {
            heroSelection = HeroSelection()
            heroSelection?.let {
                if (it.heroesAskingToTakeLeave.isNotEmpty())
                    showLeaveDialogue = true
            }
        }
        instructions?.showLeaveDialogue = showLeaveDialogue
        gameView.speedControlPanel.setInfoLine(gameView.resources.getString(R.string.stage_number).format(level.numberAsString(Attacker.Representation.HEX)))
        gameView.gameActivity.setGameActivityStatus(GameActivity.GameActivityStatus.BETWEEN_LEVELS)
    }

    private fun startMarketplace()
    {
        if (holidayGranted()) {
            clear()
            gameView.marketplace.fillMarket(level)
            gameView.gameActivity.changeToGamePhase(GameMechanics.GamePhase.MARKETPLACE)
        }
    }

    private fun startLevel()
    {
        if (holidayGranted()) {
            gameView.gameActivity.startNextStage(level)
        }
    }

    private fun holidayGranted(): Boolean
    {
        if (oneHeroMustGoOnLeave(level))
        {
            if (heroSelection?.selectedHero == null) {
                Toast.makeText(gameView.gameActivity, "You must select one hero and give them leave!", Toast.LENGTH_SHORT)
                    .show()
                return false
            }
            heroSelection?.selectedHero?.addLeave(level, durationOfLeave)
            Persistency(gameView.gameActivity).saveHolidays(gameView.gameMechanics)
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
            displayTypewriterText()
        }
        gameView.gameActivity.setGameActivityStatus(GameActivity.GameActivityStatus.BETWEEN_LEVELS)
        gameView.gameActivity.changeToGamePhase(GameMechanics.GamePhase.INTERMEZZO)
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
        var bitmap: Bitmap? = null
        /** outer rectangle with a green border */
        private var heroSelectionArea = Rect()

        private val paint: Paint = Paint()

        init {
            heroesAskingToTakeLeave = choosePossibleHeroes()
            if (heroesAskingToTakeLeave.size > sizeOfHeroPanel)
                heroesAskingToTakeLeave = heroesAskingToTakeLeave.takeLast(sizeOfHeroPanel)
        }

        fun prepareScreen()
        {
            if (heroSelectionArea.height() > 0)
                bitmap = createBitmap(heroSelectionArea)
        }

        fun createBitmap(area: Rect): Bitmap
        /** @param area the rectangle that can be used by the bitmap */
        {
            // create empty bitmap
            val bitmap = Bitmap.createBitmap(area.width(), area.height(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(resources.getColor(R.color.alternate_background))
            // create text for bitmap
            val margin = 20 * gameView.scaleFactor
            canvas.save()
            canvas.translate(margin, margin)
            val text = when (durationOfLeave)
            {
                1 -> resources.getString(R.string.instr_leave_1)
                else -> resources.getString(R.string.instr_leave).format(gameView.intermezzo.durationOfLeave)
            }
            val textPaint = TextPaint()
            textPaint.textSize = GameView.computerTextSize * gameView.textScaleFactor
            textPaint.typeface = Typeface.SANS_SERIF
            textPaint.color = resources.getColor(R.color.text_amber)
            textPaint.alpha = 255
            val textLayout = StaticLayout(text, textPaint, area.width()-2*margin.toInt(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)
            textLayout.draw(canvas)
            canvas.restore()

            // put hero cards
            val viewport = Viewport()
            determineCardPositions(textLayout.height, margin.toInt())
            heroesAskingToTakeLeave.forEach()
            {
                it.card.display(canvas, viewport)
            }
            return bitmap
        }

        fun adjustArea()
        {
            heroSelectionArea = Rect(myArea.left, myArea.top, myArea.right, heightOfConsoleLine())
        }

        private fun determineCardPositions(upperEdge: Int, margin: Int)
        {
            heroesAskingToTakeLeave.forEach {
                it.card.create(showNextUpdate = false, monochrome = true)
            }
            val cardWidth  = heroesAskingToTakeLeave.first().card.cardArea.width()
            val cardHeight = heroesAskingToTakeLeave.first().card.cardArea.height()
            val heroRect = Rect(heroSelectionArea.left, upperEdge, heroSelectionArea.right, heroSelectionArea.bottom).apply { shrink(margin) }
            val xLeft = heroRect.left
            val xRight = heroRect.left + cardWidth + margin
            val yBottom = heroRect.top + cardHeight + 2*margin
            val yTop = heroRect.top + margin
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

        fun display(canvas: Canvas)
        {
            heroSelectionArea.bottom = heightOfConsoleLine()
            paint.color = resources.getColor(R.color.text_amber)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 6f
            paint.alpha = 255
            val sourceRect = Rect(heroSelectionArea).apply { shiftBy(0, vertOffset.toInt()) }
            bitmap?.let {
                canvas.drawBitmap(it, sourceRect, heroSelectionArea, paint)
                // canvas.drawRect(myOuterArea, paint)
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
                val yCoord = event.y + vertOffset
                if (it.card.cardAreaOnScreen.contains(event.x.toInt(), yCoord.toInt())) {
                    selectForLeave(it)
                    prepareScreen()
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