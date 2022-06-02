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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList


class Game(val gameActivity: MainGameActivity) {
    companion object Params {
        val chipSize = GridCoord(6,3)
        const val viewportMargin = 10
        const val minScoreBoardHeight = 100
        const val maxScoreBoardHeight = 320
        const val speedControlButtonSize = 80
        const val drawLinesFromChip = false

        const val scoreTextSize = 40f
        const val scoreHeaderSize = 20f
        const val chipTextSize = 24f
        const val computerTextSize = 36f
        const val instructionTextSize = computerTextSize

        const val coinSizeOnScoreboard = 40
        const val coinSizeOnScreen = 25
        const val cardHeight = 256
        const val cardWidth = 280

        const val minimalAmountOfCash = 8
        const val maxLivesPerStage = 3

        const val levelSnapshotIconSize = 120

        val basePrice = mapOf(
            Chip.ChipUpgrades.SUB to 8, Chip.ChipUpgrades.AND to 32, Chip.ChipUpgrades.SHIFT to 16)
    }

    data class StateData(
        var phase: GamePhase,       // whether the game is running, paused or between levels
        var startingLevel: Int,     // level to begin the next game with
        var maxLives: Int,          // maximum number of lives
        var currentMaxLives: Int,   // maximum number of lives, taking into account modifiers
        var lives: Int,             // current number of lives
        var cash: Int,              // current amount of 'information' currency in bits
        var coinsInLevel: Int = 0,  // cryptocoins that can be obtained by completing the current level
        var coinsExtra: Int = 0    // cryptocoins that have been acquired by collecting moving coins
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
        var coinsTotal: Int = 0
    )
    var global = GlobalData()

    var stageData: Stage.Data? = null
    var summaryPerLevel = HashMap<Int, Stage.Summary>()
    var levelThumbnail = HashMap<Int, Bitmap?>()  // level snapshots
    var gameUpgrades = HashMap<Upgrade.Type, Upgrade>()

    val viewport = Viewport()
    var background: Background? = null
    var network: Network? = null
    var intermezzo = Intermezzo(this)
    var marketplace = Marketplace(this)
    val scoreBoard = ScoreBoard(this)
    val speedControlPanel = SpeedControl(this)
    var currentStage: Stage? = null
    var currentWave: Wave? = null
    val resources: Resources = (gameActivity as Activity).resources

    var movers = CopyOnWriteArrayList<Mover>() // list of all mover objects that are created for game elements
    var faders = CopyOnWriteArrayList<Fader>() // idem for faders

    enum class GamePhase { START, RUNNING, END, INTERMEZZO, MARKETPLACE, PAUSED }
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
            intermezzo.prepareLevel(state.startingLevel, true)
        }
        else
            intermezzo.prepareLevel(1, true)
    }

    fun continueGame()
    {
        gameActivity.loadState()
        currentStage = Stage.createStageFromData(this, stageData)
        var stage = currentStage ?: return beginGame()

        network = stage.network
        viewport.setViewportSize(stage.sizeX, stage.sizeY)
        currentWave = if (stage.waves.size > 0) stage.waves[0] else stage.nextWave()
        // data.coinsInLevel = it.rewardCoins
        if (state.phase == GamePhase.RUNNING) {
            gameActivity.runOnUiThread {
                val toast: Toast = Toast.makeText(gameActivity, "Stage %d".format(stage.data.level), Toast.LENGTH_SHORT )
                toast.show()
            }
        }
        else if (state.phase == GamePhase.MARKETPLACE)
            marketplace.fillMarket(stage.data.level)
    }

    fun update()
    {
        if (state.phase == GamePhase.RUNNING) {
            network?.update()
            scoreBoard.update()
            currentWave?.update()
        }
    }

    fun updateEffects()
            /**  execute all movers and faders */
    {
        background?.update()
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
            background?.display(canvas, viewport.screen)
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
    }

    fun onDown(p0: MotionEvent): Boolean {
        when (state.phase)
        {
            GamePhase.RUNNING ->
            {
                speedControlPanel.onDown(p0)
                if (network != null) {
                    for (obj in network!!.nodes.values)
                        if (obj.onDown(p0))
                            return true
                    for (obj in network!!.vehicles)
                        if ((obj as Attacker).onDown(p0))
                            return true
                    return false
                }
                else
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
        if (currentStage?.attackerCount()?:0 > 0)  // still attackers left, wait until wave is really over
        {
            GlobalScope.launch { delay(1000L); onEndOfStage() }
        }
        else {
            currentStage?.let { onStageCleared(it) }
        }
        gameActivity.saveState()
    }

    fun onStageCleared(stage: Stage)
    {
        gameActivity.runOnUiThread {
            val toast: Toast = Toast.makeText(gameActivity, "Stage cleared", Toast.LENGTH_SHORT)
            toast.show()
        }
        intermezzo.coinsGathered = state.coinsExtra + state.coinsInLevel
        global.coinsTotal += intermezzo.coinsGathered
        summaryPerLevel[stage.data.level] = Stage.Summary(won = true, coinsGot = stage.summary.coinsGot + state.coinsInLevel)
        if (stage.type == Stage.Type.FINAL)
        {
            intermezzo.endOfGame(stage.data.level, hasWon = true)
        }
        else {
            setMaxStage(stage.data.level+1)
            intermezzo.prepareLevel(stage.data.level + 1, false)
        }
    }

    fun startNextStage(level: Int)
    {
        var extraLives = gameUpgrades[Upgrade.Type.ADDITIONAL_LIVES]?.getStrength()
        state.currentMaxLives = state.maxLives + (extraLives ?: 0f).toInt()
        state.lives = state.currentMaxLives
        calculateStartingCash()
        var nextStage = Stage(this)
        gameActivity.runOnUiThread {
            val toast: Toast = Toast.makeText(gameActivity, "Stage %d".format(nextStage.data.level), Toast.LENGTH_SHORT)
            toast.show() }
        network = nextStage.createNetwork(level)
        state.coinsInLevel = nextStage.calculateRewardCoins(summaryPerLevel[level])
        state.coinsExtra = 0
        summaryPerLevel[level] = nextStage.summary
        gameActivity.setGameSpeed(GameSpeed.NORMAL)
        speedControlPanel.resetButtons()
        gameActivity.saveState()
        if (network == null) // no more levels left
        {
            setMaxStage(level)
            intermezzo.endOfGame(level, hasWon = true)
        }
        else {
            viewport.setViewportSize(network!!.data.gridSizeX, network!!.data.gridSizeY)
            state.phase = GamePhase.RUNNING
            currentWave = nextStage.nextWave()
        }
        currentStage = nextStage
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

    fun setMaxStage(currentStage: Int)
    {
        val prefs = gameActivity.getSharedPreferences(gameActivity.getString(R.string.pref_filename), Context.MODE_PRIVATE)
        val maxStage = prefs.getInt("MAXSTAGE", 1)
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
        val cash = gameUpgrades[Upgrade.Type.INCREASE_STARTING_CASH]?.getStrength()?.toInt()
        state.cash = cash ?: minimalAmountOfCash
    }

    fun takeLevelSnapshot()
    {
        currentStage?.let {
            levelThumbnail[it.data.level] = it.takeSnapshot(levelSnapshotIconSize)
            gameActivity.saveThumbnail(it.data.level)
        }
    }

}