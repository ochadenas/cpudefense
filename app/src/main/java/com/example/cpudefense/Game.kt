package com.example.cpudefense

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.widget.Toast
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.effects.Mover
import com.example.cpudefense.gameElements.Attacker
import com.example.cpudefense.gameElements.ScoreBoard
import com.example.cpudefense.gameElements.SpeedControl
import com.example.cpudefense.gameElements.Wave
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
        val viewportMargin = 32
        val minScoreBoardHeight = 100
        val maxScoreBoardHeight = 320
        val speedControlButtonSize = 80
        val speedControlButtonMargin = 16
        val drawLinesFromChip = false


        val scoreTextSize = 40f
        val scoreHeaderSize = 20f
        val chipTextSize = 24f
        val computerTextSize = 36f
        val instructionTextSize = computerTextSize

        val coinSizeOnScoreboard = 40
        val coinSizeOnScreen = 25

        val minimalAmountOfCash = 8
        val maxLivesPerStage = 4

        val basePrice = mapOf(
            Chip.ChipUpgrades.SUB to 8, Chip.ChipUpgrades.AND to 32, Chip.ChipUpgrades.SHIFT to 16)
    }

    data class Data(
        var state: GameState,       // whether the game is running, paused or between levels
        var startingLevel: Int,     // level to begin the next game with
        var maxLives: Int,          // maximum number of lives
        var lives: Int,             // current number of lives
        var cash: Int,              // current amount of 'information' currency in bits
        var coinsInLevel: Int = 0,  // cryptocoins that can be obtained by completing the current level
        var coinsExtra: Int = 0,    // cryptocoins that have been acquired by collecting moving coins
        var coinsTotal: Int = 0,    // total number of cryptocoins that can be acquired in the level
        var coinsPerLevel: HashMap<Int, Int>  // keeping track of coins gathered in previous levels
        )

    var data = Data(
        state = GameState.START,
        startingLevel = 1,
        maxLives = maxLivesPerStage,
        lives = 0,
        cash = minimalAmountOfCash,
        coinsPerLevel = HashMap()
    )

    var stageData: Stage.Data? = null

    val viewport = Viewport()
    var network: Network? = null
    var intermezzo = Intermezzo(this)
    val scoreBoard = ScoreBoard(this)
    val speedControlPanel = SpeedControl(this)
    var currentStage: Stage? = null
    var currentWave: Wave? = null
    val resources = (gameActivity as Activity).resources

    var movers = CopyOnWriteArrayList<Mover>() // list of all mover objects that are created for game elements
    var faders = CopyOnWriteArrayList<Fader>() // idem for faders

    enum class GameState { START, RUNNING, END, INTERMEZZO, PAUSED }
    enum class GameSpeed { NORMAL, MAX }

    val coinIcon = BitmapFactory.decodeResource(resources, R.drawable.cryptocoin)
    val cpuImage = BitmapFactory.decodeResource(resources, R.drawable.cpu)
    val playIcon = BitmapFactory.decodeResource(resources, R.drawable.play_active)
    val pauseIcon = BitmapFactory.decodeResource(resources, R.drawable.pause_active)
    val fastIcon = BitmapFactory.decodeResource(resources, R.drawable.fast_active)

    fun startGame()
    {
        intermezzo.prepareLevel(data.startingLevel, true)
    }

    fun continueGame()
    {
        gameActivity.loadState()
        currentStage = Stage.createStageFromData(this, stageData)
        currentStage?.let {
            // network = it.createNetwork(it.data.level)
            // data.coinsInLevel = it.rewardCoins
            network = it.network
            currentWave = if (it.waves.size > 0) it.waves[0] else it.nextWave()
            viewport.setViewportSize(it.sizeX, it.sizeY)
            gameActivity.runOnUiThread {
                val toast: Toast = Toast.makeText(
                    gameActivity,
                    "Stage %d".format(it.data.level),
                    Toast.LENGTH_SHORT
                )
                toast.show() }
        }
        if (currentStage == null)
            startGame()
    }

    fun update()
    {
        if (data.state == GameState.RUNNING) {
            network?.update()
            scoreBoard.update()
            currentWave?.update()
        }
    }

    fun updateAndMoveEverything()
            /**  execute all movers and faders */
    {
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
        if (data.state == GameState.RUNNING || data.state == GameState.PAUSED)
        {
            network?.display(canvas, viewport)
            scoreBoard.display(canvas, viewport)
            speedControlPanel.display(canvas, viewport)
        }
        if (data.state == GameState.PAUSED)
        {
            var paint = Paint()
            paint.color = Color.WHITE
            paint.textSize = 72f
            paint.typeface = Typeface.DEFAULT_BOLD
            viewport?.let {
                var rect = Rect(0, 0, it.screenWidth, it.screenHeight)
                rect.displayTextCenteredInRect(canvas, "GAME PAUSED", paint)
            }
        }

        intermezzo.display(canvas, viewport)
    }

    fun onDown(p0: MotionEvent): Boolean {
        when (data.state)
        {
            GameState.RUNNING ->
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
            GameState.INTERMEZZO ->
                return intermezzo.onDown(p0)
            GameState.PAUSED ->
            {
                data.state = GameState.RUNNING
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
        GlobalScope.launch { delay(5000L); startNextWave() }
    }

    fun onEndOfStage()
    {
        if (currentStage == null)
            return // in this case, the stage has already been left
        if (currentStage?.attackerCount()?:0 > 0)
        {
            GlobalScope.launch { delay(1000L); onEndOfStage() }
            return
        }
        else {
            gameActivity.saveState()
            currentStage?.let { onStageCleared(it) }
        }
    }

    fun onStageCleared(stage: Stage)
    {
        gameActivity.runOnUiThread {
            val toast: Toast = Toast.makeText(gameActivity, "Stage cleared", Toast.LENGTH_SHORT)
            toast.show()
        }
        intermezzo.coinsGathered = data.coinsExtra + data.coinsInLevel
        data.coinsPerLevel[stage.data.level] = stage.coinsPreviouslyGot + intermezzo.coinsGathered

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
        data.lives = data.maxLives

        calculateStartingCash()
        currentStage = Stage(this)
        gameActivity.runOnUiThread {
            val toast: Toast = Toast.makeText(gameActivity, "Stage %d".format(currentStage?.data?.level), Toast.LENGTH_SHORT)
            toast.show() }
        if (currentStage != null) {
            network = currentStage!!.createNetwork(level)
            currentStage?.let {
                data.coinsInLevel = it.rewardCoins - it.coinsPreviouslyGot
            }
        }
        if (network == null) // no more levels left
        {
            setMaxStage(level)
            intermezzo.endOfGame(level, hasWon = true)
        }
        else {
            viewport.setViewportSize(network!!.data.gridSizeX, network!!.data.gridSizeY)
            data.state = GameState.RUNNING
            currentWave = currentStage?.nextWave()
        }
    }

    fun removeOneLife()
    {
        if (data.coinsInLevel > 0)
            data.coinsInLevel--
        data.lives--
        if (data.lives == 0)
        {
            val lastLevel = currentStage?.data?.level ?: 1
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
        var prefs = gameActivity.getSharedPreferences(gameActivity.getString(R.string.pref_filename), Context.MODE_PRIVATE)
        var maxStage = prefs.getInt("MAXSTAGE", 1)
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
        data.cash = minimalAmountOfCash
    }

}