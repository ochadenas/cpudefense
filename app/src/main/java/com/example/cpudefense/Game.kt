package com.example.cpudefense

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.view.MotionEvent
import android.widget.Toast
import com.example.cpudefense.effects.Background
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.effects.Mover
import com.example.cpudefense.gameElements.*
import com.example.cpudefense.networkmap.GridCoord
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.displayTextCenteredInRect
import com.example.cpudefense.utils.flipHorizontally
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList


class Game(val gameActivity: MainGameActivity) {
    companion object Params {
        val maximumStageAvailableInTheGame = 25

        const val defaultMainDelay = 70L
        val chipSize = GridCoord(6,3)
        const val viewportMargin = 10
        const val minScoreBoardHeight = 100
        const val maxScoreBoardHeight = 320
        const val speedControlButtonSize = 48

        // text sizes are given in dp (device independent)
        const val scoreTextSize = 36f
        const val scoreHeaderSize = 18f
        const val chipTextSize = 22f
        const val computerTextSize = 28f
        const val notificationTextSize = 22f
        const val instructionTextSize = 28f
        const val biographyTextSize = 24f
        const val purchaseButtonTextSize = 24f

        const val coinSizeOnScoreboard = 40
        const val coinSizeOnScreen = 25
        const val cardHeight = 256
        const val cardWidth = 220

        const val minimalAmountOfCash = 8
        const val maxLivesPerStage = 3

        const val levelSnapshotIconSize = 120

        val basePrice = mapOf(Chip.ChipUpgrades.REDUCE to 20,
            Chip.ChipUpgrades.SUB to 8, Chip.ChipUpgrades.ACC to 10, Chip.ChipUpgrades.SHR to 16, Chip.ChipUpgrades.MEM to 12 )
    }

    var globalSpeedFactor = 1.0f

    data class StateData(
        var phase: GamePhase,       // whether the game is running, paused or between levels
        var startingLevel: Stage.Identifier,     // level to begin the next game with
        var maxLives: Int,          // maximum number of lives
        var currentMaxLives: Int,   // maximum number of lives, taking into account modifiers
        var lives: Int,             // current number of lives
        var cash: Int,              // current amount of 'information' currency in bits
        var coinsInLevel: Int = 0,  // cryptocoins that can be obtained by completing the current level
        var coinsExtra: Int = 0     // cryptocoins that have been acquired by collecting moving coins
        )
    var state = StateData(
        phase = GamePhase.START,
        startingLevel = Stage.Identifier(),
        maxLives = maxLivesPerStage,
        currentMaxLives = maxLivesPerStage,
        lives = 0,
        cash = minimalAmountOfCash
    )

    data class GlobalData(
        var speed: GameSpeed = GameSpeed.NORMAL,
        var coinsTotal: Int = 0,
        var configDisableBackground: Boolean = true,
        var configShowAttsInRange: Boolean = false
    )
    var global = GlobalData()

    val resources: Resources = (gameActivity as Activity).resources

    var stageData: Stage.Data? = null
    var summaryPerLevelOfSeries1 = HashMap<Int, Stage.Summary>()
    var summaryPerLevelOfSeries2 = HashMap<Int, Stage.Summary>()
    var levelThumbnail = HashMap<Int, Bitmap?>()  // level snapshots (common for series 1 and 2)
    var gameUpgrades = HashMap<Hero.Type, Hero>()

    /* game elements */
    val viewport = Viewport()
    var background: Background? = null
    var intermezzo = Intermezzo(this)
    var marketplace = Marketplace(this)
    val scoreBoard = ScoreBoard(this)
    val speedControlPanel = SpeedControl(this)
    var currentStage: Stage? = null
    private var currentWave: Wave? = null
    var movers = CopyOnWriteArrayList<Mover>() // list of all mover objects that are created for game elements
    var faders = CopyOnWriteArrayList<Fader>() // idem for faders
    val notification = ProgressNotification(this)

    /* other temporary variables */
    private var additionalCashDelay = 0
    private var additionalCashTicks = 0

    enum class GamePhase { START, RUNNING, INTERMEZZO, MARKETPLACE, PAUSED }
    enum class GameSpeed { NORMAL, MAX }

    val coinIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.cryptocoin)
    val coinIconReverse = coinIcon.flipHorizontally()
    val cpuImage: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.cpu)
    val playIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.play_active)
    val pauseIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.pause_active)
    val fastIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.fast_active)

    fun beginGame(resetProgress: Boolean = false)
    {
        if (!resetProgress) {
            global = gameActivity.loadGlobalData()
            summaryPerLevelOfSeries1 = gameActivity.loadLevelData(1)   // get historical data of levels completed so far
            summaryPerLevelOfSeries2 = gameActivity.loadLevelData(2)   // get historical data of levels completed so far
            gameUpgrades = gameActivity.loadUpgrades()       // load the upgrades gained so far
            additionalCashDelay = gameUpgrades[Hero.Type.GAIN_CASH]?.getStrength()?.toInt() ?: 0
            intermezzo.prepareLevel(state.startingLevel, true)
        }
        else {
            state.startingLevel = Stage.Identifier(1,1)
            summaryPerLevelOfSeries1 = HashMap()
            summaryPerLevelOfSeries2 = HashMap()
            setLastPlayedStage(state.startingLevel)
            setMaxPlayedStage(state.startingLevel, resetProgress=true)
            intermezzo.prepareLevel(state.startingLevel, true)
        }
        if (background == null)
            background = Background(this)
    }

    fun resumeGame()
    {
        currentStage = Stage.createStageFromData(this, stageData)
        val stage = currentStage ?: return beginGame()

        stage.network.validateViewport()
        // viewport.setSize(gameActivity.theGameView.width, gameActivity.theGameView.height)
        viewport.setGridSize(stage.sizeX, stage.sizeY)
        if (state.phase == GamePhase.MARKETPLACE)
            marketplace.fillMarket(stage.data.ident)
        else
        {
            state.phase = GamePhase.RUNNING
            currentWave = if (stage.waves.size > 0) stage.waves[0] else stage.nextWave()
        }
        if (background == null)
            background = Background(this)
        if (!global.configDisableBackground)
            background?.choose(stage.getLevel())
        background?.state = Background.BackgroundState.UNINITIALIZED
    }

    fun update()
    {
        if (state.phase == GamePhase.RUNNING)
        {
            currentStage?.network?.let {
                if (background?.update() == true) {
                    it.backgroundImage = background?.actualImage
                    it.recreateNetworkImage(viewport)
                }
                it.update()
            }
            scoreBoard.update()
            currentWave?.update()
            gainAdditionalCash()
        }
    }

    fun updateEffects()
            /**  execute all movers and faders */
    {
        intermezzo.update()
        for (m in movers)
        {
            if (m?.type == Mover.Type.NONE)
                movers.remove(m)
            else
                m?.update()
        }
        for (m in faders)
        {
            if (m?.type == Fader.Type.NONE)
                faders.remove(m)
            else
                m?.update()
        }
    }

    fun display(canvas: Canvas)
    {
        if (state.phase == GamePhase.RUNNING || state.phase == GamePhase.PAUSED)
        {
            /*
            if (! global.configDisableBackground)
                background?.actualImage?.let {
                    canvas.drawBitmap(it, null, viewport.screen, paintBitmap)
                }

             */
            currentStage?.network?.display(canvas, viewport)
            scoreBoard.display(canvas, viewport)
            speedControlPanel.display(canvas, viewport)
        }
        if (state.phase == GamePhase.PAUSED)
        {
            val paint = Paint()
            paint.color = Color.WHITE
            paint.textSize = 72f
            paint.typeface = Typeface.DEFAULT_BOLD
            viewport.let {
                val rect = Rect(0, 0, it.viewportWidth, it.viewportHeight)
                rect.displayTextCenteredInRect(canvas, "GAME PAUSED", paint)
            }
        }
        intermezzo.display(canvas, viewport)
        marketplace.display(canvas, viewport)
        notification.display(canvas)
    }

    fun onDown(p0: MotionEvent): Boolean {
        when (state.phase)
        {
            GamePhase.RUNNING ->
            {
                speedControlPanel.onDown(p0)
                currentStage?.network?.let {
                    for (obj in it.nodes.values)
                        if (obj.onDown(p0))
                            return true
                    for (obj in it.vehicles)
                        if ((obj as Attacker).onDown(p0))
                            return true
                    return false
                }
                return false
            }
            GamePhase.INTERMEZZO ->
                return intermezzo.onDown(p0)
            GamePhase.MARKETPLACE ->
                return marketplace.onDown(p0)
            GamePhase.PAUSED ->
            {
                state.phase = GamePhase.RUNNING
                speedControlPanel.resetButtons()
                return true
            }
            else ->
                return false
        }
    }

    fun getSummaryOfStage(stage: Stage.Identifier): Stage.Summary?
    {
        when (stage.series)
        {
            1 -> return summaryPerLevelOfSeries1[stage.number]
            2 -> return summaryPerLevelOfSeries2[stage.number]
            else -> return null
        }
    }

    fun setSummaryOfStage(stage: Stage.Identifier, summary: Stage.Summary?)
    {
        summary?.let {
            when (stage.series) {
                1 -> summaryPerLevelOfSeries1[stage.number] = it
                2 -> summaryPerLevelOfSeries2[stage.number] = it
                else -> return
            }
        }
    }
    
    private fun startNextWave()
    {
        currentWave = currentStage?.nextWave()
    }

    fun onEndOfWave()
    {
        currentWave = null
        GlobalScope.launch { delay(3000L); startNextWave() }
    }

    fun onEndOfStage()
    {
        if (currentStage == null)
            return // in this case, the stage has already been left
        takeLevelSnapshot()
        currentStage?.let {
            if (it.attackerCount()>0)
                // still attackers left, wait until wave is really over
                GlobalScope.launch { delay(2000L); onEndOfStage() }
            else {
                onStageCleared(it)
                gameActivity.saveState()
                gameActivity.setGameActivityStatus(MainGameActivity.GameActivityStatus.BETWEEN_LEVELS)
            }
        }
    }

    private fun onStageCleared(stage: Stage)
    {
        gameActivity.runOnUiThread {
            val toast: Toast = Toast.makeText(gameActivity, resources.getString(R.string.toast_stage_cleared), Toast.LENGTH_SHORT)
            toast.show()
        }
        intermezzo.coinsGathered = state.coinsExtra + state.coinsInLevel
        global.coinsTotal += intermezzo.coinsGathered
        val summaryOfCompletedStage = Stage.Summary(won = true,
            coinsGot = stage.summary.coinsGot + state.coinsInLevel,
            coinsMaxAvailable = stage.summary.coinsMaxAvailable,
            coinsAvailable = stage.summary.coinsMaxAvailable - state.coinsInLevel
        )
        val currentStage = stage.data.ident
        val nextStage = currentStage.next()
        setSummaryOfStage(currentStage, summaryOfCompletedStage)
        setMaxPlayedStage(currentStage)
        if (stage.type == Stage.Type.FINAL)
        {
            intermezzo.endOfGame(currentStage, hasWon = true)
        }
        else {
            // make next level available. Create an empty one if necessary
            setSummaryOfStage(nextStage, getSummaryOfStage(nextStage) ?: Stage.Summary())
            intermezzo.prepareLevel(nextStage, false)
        }
    }

    fun startNextStage(level: Stage.Identifier)
    {
        val nextStage = Stage(this)
        gameActivity.setGameActivityStatus(MainGameActivity.GameActivityStatus.PLAYING)
        calculateLives()
        calculateStartingCash()
        StageFactory.createStageWithObstacles(nextStage, level)
        if (!nextStage.isInitialized())
            return  // something went wrong, possibly trying to create a level that doesn't exist
        gameActivity.runOnUiThread {
            val toast: Toast = Toast.makeText(
                gameActivity,
                resources.getString(R.string.toast_next_stage).format(nextStage.getLevel()),
                Toast.LENGTH_SHORT
            )
            toast.show()
        }
        state.coinsInLevel = nextStage.calculateRewardCoins(getSummaryOfStage(level))
        state.coinsExtra = 0
        setSummaryOfStage(level, nextStage.summary)
        gameActivity.setGameSpeed(GameSpeed.NORMAL)  // reset speed to normal when starting next stage
        speedControlPanel.resetButtons()
        viewport.reset()
        gameActivity.saveState()

        viewport.setGridSize(nextStage.network.data.gridSizeX, nextStage.network.data.gridSizeY)
        state.phase = GamePhase.RUNNING
        currentWave = nextStage.nextWave()

        currentStage = nextStage
        if (!global.configDisableBackground)
            background?.choose(level.number)
        takeLevelSnapshot()
    }

    fun removeOneLife()
    {
        if (state.coinsInLevel > 0)
            state.coinsInLevel--
        state.lives--
        if (state.lives == 0)
        {
            val lastLevel = currentStage?.data?.ident ?: Stage.Identifier(1,0)
            takeLevelSnapshot()
            currentStage = null
            intermezzo.endOfGame(lastLevel, hasWon = false)
        }
    }

    fun quitGame()
    {
        gameActivity.finish()
    }

    fun setLastPlayedStage(currentStage: Stage.Identifier)
    /** when completing a level, record the current number in the SharedPrefs.
     * @param currentStage number of the level successfully completed */
    {
        val prefs = gameActivity.getSharedPreferences(gameActivity.getString(R.string.pref_filename), Context.MODE_PRIVATE)
        with (prefs.edit())
        {
            putInt("LASTSTAGE", currentStage.number)
            putInt("LASTSERIES", currentStage.series)
            commit()
        }
    }

    fun setMaxPlayedStage(currentStage: Stage.Identifier, resetProgress: Boolean = false)
            /** when completing a level, record the level as highest completed, but only if the old max level is not higher.
             * @param currentStage number of the level successfully completed
             * @param resetProgress If true, forces resetting the max stage to the given currentStage
             * */
    {
        val prefs = gameActivity.getSharedPreferences(gameActivity.getString(R.string.pref_filename), Context.MODE_PRIVATE)
        val maxStage = Stage.Identifier(prefs.getInt("MAXSERIES", 1), prefs.getInt("MAXSTAGE", 0))
        with (prefs.edit())
        {
            if (currentStage.isGreaterThan(maxStage) || resetProgress)
            {
                putInt("MAXSTAGE", currentStage.number)
                putInt("MAXSERIES", currentStage.series)
                commit()
            }
            if (currentStage.series>1 || currentStage.number==Game.maximumStageAvailableInTheGame) {
                putBoolean("TURBO_AVAILABLE", true)
                commit()
            }
        }
    }
    private fun calculateLives()
    {
        val extraLives = gameUpgrades[Hero.Type.ADDITIONAL_LIVES]?.getStrength()
        state.currentMaxLives = state.maxLives + (extraLives ?: 0f).toInt()
        state.lives = state.currentMaxLives
    }
    private fun calculateStartingCash()
    {
        val cash = gameUpgrades[Hero.Type.INCREASE_STARTING_CASH]?.getStrength()?.toInt()
        state.cash = cash ?: minimalAmountOfCash
    }

    private fun gainAdditionalCash()
    /** increases the amount of cash in regular intervals */
    {
        if (additionalCashDelay == 0)
            return
        additionalCashTicks--
        if (additionalCashTicks<0) {
            scoreBoard.addCash(1)
            additionalCashTicks = additionalCashDelay
        }
    }

    private fun takeLevelSnapshot()
    {
        currentStage?.let {
            levelThumbnail[it.getLevel()] = it.takeSnapshot(levelSnapshotIconSize)
            gameActivity.saveThumbnail(it.getLevel())
        }
    }

}