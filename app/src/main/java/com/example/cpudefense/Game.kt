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
import com.example.cpudefense.networkmap.Network
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.displayTextCenteredInRect
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList


class Game(val gameActivity: MainGameActivity) {
    companion object Params {
        const val maxLevelAvailable: Int = 23

        const val defaultMainDelay = 70L
        val chipSize = GridCoord(6,3)
        const val viewportMargin = 10
        const val minScoreBoardHeight = 100
        const val maxScoreBoardHeight = 320
        const val speedControlButtonSize = 80

        const val scoreTextSize = 40f
        const val scoreHeaderSize = 20f
        const val chipTextSize = 28f
        const val computerTextSize = 36f
        const val notificationTextSize = computerTextSize
        const val instructionTextSize = computerTextSize

        const val coinSizeOnScoreboard = 40
        const val coinSizeOnScreen = 25
        const val cardHeight = 256
        const val cardWidth = 280

        const val minimalAmountOfCash = 8
        const val maxLivesPerStage = 3

        const val levelSnapshotIconSize = 120

        val basePrice = mapOf(
            Chip.ChipUpgrades.SUB to 8, Chip.ChipUpgrades.ACC to 10, Chip.ChipUpgrades.SHIFT to 16, Chip.ChipUpgrades.MEM to 12)
    }

    var globalSpeedFactor = 1.0f
    var globalResolutionFactor = 1.0f

    data class StateData(
        var phase: GamePhase,       // whether the game is running, paused or between levels
        var startingLevel: Int,     // level to begin the next game with
        var maxLives: Int,          // maximum number of lives
        var currentMaxLives: Int,   // maximum number of lives, taking into account modifiers
        var lives: Int,             // current number of lives
        var cash: Int,              // current amount of 'information' currency in bits
        var coinsInLevel: Int = 0,  // cryptocoins that can be obtained by completing the current level
        var coinsExtra: Int = 0     // cryptocoins that have been acquired by collecting moving coins
        )
    var state = StateData(
        phase = GamePhase.START,
        startingLevel = 1,
        maxLives = maxLivesPerStage,
        currentMaxLives = maxLivesPerStage,
        lives = 0,
        cash = minimalAmountOfCash
    )

    data class GlobalData(
        var speed: GameSpeed = GameSpeed.NORMAL,
        var coinsTotal: Int = 0,
        var configDisableBackground: Boolean = true
    )
    var global = GlobalData()

    var stageData: Stage.Data? = null
    var summaryPerLevel = HashMap<Int, Stage.Summary>()
    var levelThumbnail = HashMap<Int, Bitmap?>()  // level snapshots
    var gameUpgrades = HashMap<Hero.Type, Hero>()

    /* game elements */
    val viewport = Viewport()
    var background: Background? = null
    var network: Network? = null
    var intermezzo = Intermezzo(this)
    var marketplace = Marketplace(this)
    val scoreBoard = ScoreBoard(this)
    val speedControlPanel = SpeedControl(this)
    var currentStage: Stage? = null
    private var currentWave: Wave? = null
    val resources: Resources = (gameActivity as Activity).resources
    var movers = CopyOnWriteArrayList<Mover>() // list of all mover objects that are created for game elements
    var faders = CopyOnWriteArrayList<Fader>() // idem for faders
    val notification = ProgressNotification(this)
    private val paintBitmap = Paint()

    /* other temporary variables */
    private var additionalCashDelay = 0
    private var additionalCashTicks = 0

    enum class GamePhase { START, RUNNING, INTERMEZZO, MARKETPLACE, PAUSED }
    enum class GameSpeed { NORMAL, MAX }

    val coinIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.cryptocoin)
    val cpuImage: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.cpu)
    val playIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.play_active)
    val pauseIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.pause_active)
    val fastIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.fast_active)

    fun beginGame(resetProgress: Boolean = false)
    {
        if (!resetProgress) {
            global = gameActivity.loadGlobalData()
            summaryPerLevel = gameActivity.loadLevelData()   // get historical data of levels completed so far
            gameUpgrades = gameActivity.loadUpgrades()       // load the upgrades gained so far
            additionalCashDelay = gameUpgrades[Hero.Type.GAIN_CASH]?.getStrength()?.toInt() ?: 0
            intermezzo.prepareLevel(state.startingLevel, true)
        }
        else {
            intermezzo.prepareLevel(1, true)
        }
    }

    fun continueGame()
    {
        currentStage = Stage.createStageFromData(this, stageData)
        val stage = currentStage ?: return beginGame()

        network = stage.network
        network?.validateViewport()
        // viewport.setSize(gameActivity.theGameView.width, gameActivity.theGameView.height)
        viewport.setGridSize(stage.sizeX, stage.sizeY)
        if (state.phase == GamePhase.MARKETPLACE)
            marketplace.fillMarket(stage.data.level)
        else
        {
            state.phase = GamePhase.RUNNING
            currentWave = if (stage.waves.size > 0) stage.waves[0] else stage.nextWave()
        }
    }

    fun update()
    {
        if (state.phase == GamePhase.RUNNING) {
            network?.update()
            scoreBoard.update()
            currentWave?.update()
            gainAdditionalCash()
        }
    }

    fun updateEffects()
            /**  execute all movers and faders */
    {
        background?.update()
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
            if (! global.configDisableBackground)
                background?.actualImage?.let {
                    canvas.drawBitmap(it, null, viewport.screen, paintBitmap)
                }
            network?.display(canvas, viewport)
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
                network?.let {
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
    
    fun startNextWave()
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
            val toast: Toast = Toast.makeText(gameActivity, "Stage cleared", Toast.LENGTH_SHORT)
            toast.show()
        }
        intermezzo.coinsGathered = state.coinsExtra + state.coinsInLevel
        global.coinsTotal += intermezzo.coinsGathered
        summaryPerLevel[stage.data.level] = Stage.Summary(won = true, coinsGot = stage.summary.coinsGot + state.coinsInLevel)
        // make next level available
        val nextLevel = stage.data.level + 1
        if (summaryPerLevel[nextLevel] == null && stage.type != Stage.Type.FINAL)
            summaryPerLevel[nextLevel] = Stage.Summary()
        setLastStage(stage.data.level)
        if (stage.type == Stage.Type.FINAL)
        {
            intermezzo.endOfGame(stage.data.level, hasWon = true)
        }
        else {
            intermezzo.prepareLevel(stage.data.level + 1, false)
        }
    }

    fun startNextStage(level: Int)
    {
        if (level > Game.maxLevelAvailable) {
            quitGame()   // should not happen, but better handle this
            return
        }
        gameActivity.setGameActivityStatus(MainGameActivity.GameActivityStatus.PLAYING)
        val extraLives = gameUpgrades[Hero.Type.ADDITIONAL_LIVES]?.getStrength()
        state.currentMaxLives = state.maxLives + (extraLives ?: 0f).toInt()
        state.lives = state.currentMaxLives
        calculateStartingCash()
        val nextStage = Stage(this)
        gameActivity.runOnUiThread {
            val toast: Toast = Toast.makeText(gameActivity, "Stage %d".format(nextStage.data.level), Toast.LENGTH_SHORT)
            toast.show() }
        network = nextStage.createNetwork(level)
        state.coinsInLevel = nextStage.calculateRewardCoins(summaryPerLevel[level])
        state.coinsExtra = 0
        summaryPerLevel[level] = nextStage.summary
        gameActivity.setGameSpeed(GameSpeed.NORMAL)  // reset speed to normal when starting next stage
        speedControlPanel.resetButtons()
        viewport.reset()
        gameActivity.saveState()

        if (network == null) // no more levels left
        {
            setLastStage(level)
            intermezzo.endOfGame(level, hasWon = true)
        }
        else network?.let {
            viewport.setGridSize(it.data.gridSizeX, it.data.gridSizeY)
            state.phase = GamePhase.RUNNING
            currentWave = nextStage.nextWave()
        }

        currentStage = nextStage
        background?.choose(level, 0.2f)
        takeLevelSnapshot()
    }

    fun removeOneLife()
    {
        if (state.coinsInLevel > 0)
            state.coinsInLevel--
        state.lives--
        if (state.lives == 0)
        {
            val lastLevel = currentStage?.data?.level ?: 1
            takeLevelSnapshot()
            currentStage = null
            intermezzo.endOfGame(lastLevel, hasWon = false)
        }
    }

    fun quitGame()
    {
        gameActivity.finish()
    }

    fun setLastStage(currentStage: Int)
    /** when completing a level, record the current number in the SharedPrefs.
     * If necessary, adjust MAXSTAGE, too.
     * @param currentStage number of the level successfully completed */
    {
        val prefs = gameActivity.getSharedPreferences(gameActivity.getString(R.string.pref_filename), Context.MODE_PRIVATE)
        val maxStage = prefs.getInt("MAXSTAGE", 0)
        with (prefs.edit())
        {
            putInt("LASTSTAGE", currentStage)
            if (currentStage > maxStage)
                putInt("MAXSTAGE", currentStage)
            commit()
        }
    }

    fun calculateStartingCash()
    {
        val cash = gameUpgrades[Hero.Type.INCREASE_STARTING_CASH]?.getStrength()?.toInt()
        state.cash = cash ?: minimalAmountOfCash
    }
    
    fun gainAdditionalCash()
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

    fun takeLevelSnapshot()
    {
        currentStage?.let {
            levelThumbnail[it.data.level] = it.takeSnapshot(levelSnapshotIconSize)
            gameActivity.saveThumbnail(it.data.level)
        }
    }

}