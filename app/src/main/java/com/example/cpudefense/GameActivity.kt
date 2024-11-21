@file:Suppress("SpellCheckingInspection")

package com.example.cpudefense

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import com.example.cpudefense.GameMechanics.GamePhase
import com.example.cpudefense.GameMechanics.LevelMode
import com.example.cpudefense.GameMechanics.Params.forceHeroMigration
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameActivity : Activity() {
    lateinit var gameMechanics: GameMechanics
    lateinit var gameView: GameView
    /** flag used to keep the threads running. Set to false when leaving activity */
    private var gameIsRunning = true

    companion object;

    enum class GameActivityStatus { PLAYING, BETWEEN_LEVELS }

    /* properties used for assuring a constant frame rate */
    /** delta T in normal operation */
    private val defaultDelay = 40L
    /** delta T when accelerated */
    private val fastForwardDelay = 7L
    private val fastFastForwardDelay = 3L
    private var updateDelay: Long = defaultDelay
    private val effectsDelay: Long = 15

    /** additional properties for displaying an average frame rate */
    private var timeOfLastFrame = SystemClock.uptimeMillis()
    /** how many samples in one count */
    private val meanCount = 10
    private var updateJob: Job? = null
    private var displayJob: Job? = null
    private var effectsJob: Job? = null
    /** how many samples have been taken */
    private var frameCount = 0
    /** cumulated time */
    private var frameTimeSum = 0L

    var settings = Settings()

    /** if onCreate is _not_ called, this stays "true".
     * The other possibility to make it "true" is to use the button "Resume Game". */
    private var resumeGame = true

    override fun onCreate(savedInstanceState: Bundle?)
            /** this function gets called when the app was started, but not when the user returns
             * here from another app.
             */
    {
        super.onCreate(savedInstanceState)
        /* here, the size of the surfaces might not be known */
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main_game)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        resumeGame = false
        gameMechanics = GameMechanics()
        gameView = GameView(this)
    }

    override fun onPause() {
        // this method get executed when the user presses the system's "back" button,
        // but also when she navigates to another app
        Persistency(this).saveGeneralState(gameMechanics)
        Persistency(this).saveCurrentLevelState(gameMechanics)
        gameIsRunning = false
        super.onPause()
    }

    override fun onResume()
            /** this function gets called in any case, regardless of whether
             * a new game is started or the user just navigates back to the app.
             * theGame already exists when we come here.
             */
    {
        super.onResume()
        Toast.makeText(this, resources.getString(R.string.toast_loading), Toast.LENGTH_SHORT).show()
        loadSettings()
        setupGameView()

        // determine what to do: resume, restart, or play next level
        val restartGame = intent.getBooleanExtra("RESET_PROGRESS", false)
        val startOnLevel =
            if (restartGame)
                Stage.Identifier.startOfNewGame
            else
                Stage.Identifier(
                    series = intent.getIntExtra("START_ON_SERIES", 1),
                    number = intent.getIntExtra("START_ON_STAGE", 1))
        if (!resumeGame)
            resumeGame = intent.getBooleanExtra("RESUME_GAME", false)

        beginGame(resumeGame = resumeGame, resetProgress = restartGame, startingLevel = startOnLevel)

        if (resumeGame && gameMechanics.state.phase == GamePhase.RUNNING)
            showStageMessage(gameMechanics.currentStage)

        gameIsRunning = true
        resumeGame = true // for the next time we come here
        setGameSpeed(GameMechanics.GameSpeed.NORMAL) // always start with normal speed
        startGameThreads()
    }

    fun showStageMessage(ident: Stage.Identifier)
    {
        runOnUiThread { Toast.makeText(this, resources.getString(R.string.toast_enter_stage).format(ident.number),
                    Toast.LENGTH_SHORT).show() }
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun setupGameView()
    /** creates the game view including all game components */
    {
        if (gameView.parent == null)
        {
            val parentView: FrameLayout? = findViewById(R.id.gameFrameLayout)
            parentView?.addView(gameView)
        }
        gameView.setupView()
    }


    fun setLastPlayedStage(identifier: Stage.Identifier)
            /** when completing a level, record the current number in the SharedPrefs.
             * @param identifier number of the level successfully completed */
    {
        val prefs = getSharedPreferences(Persistency.filename_state, Context.MODE_PRIVATE)
        with (prefs.edit())
        {
            putInt("LASTSTAGE", identifier.number)
            putInt("LASTSERIES", identifier.series)
            commit()
        }
    }

    fun setMaxPlayedStage(identifier: Stage.Identifier, forceReset: Boolean = false)
            /** when completing a level, record the level as highest completed, but only if the old max level is not higher.
             * @param identifier number of the level successfully completed
             * @param forceReset If true, forces resetting the max stage to the given currentStage
             * */
    {
        val prefs = getSharedPreferences(Persistency.filename_state, Context.MODE_PRIVATE)
        val previousMaxStage = Stage.Identifier(prefs.getInt("MAXSERIES", 1), prefs.getInt("MAXSTAGE", 0))
        val newMaxStage = if (identifier.isGreaterThan(previousMaxStage) || forceReset) identifier else previousMaxStage
        with (prefs.edit())
        {
            putInt("MAXSTAGE", newMaxStage.number)
            putInt("MAXSERIES", newMaxStage.series)
            // make next series available if last level is completed, otherwise remove access
            val completedLastStageOfSeries: Boolean = (identifier.number == GameMechanics.maxLevelAvailable)
            when (newMaxStage.series) {
                GameMechanics.SERIES_NORMAL -> {
                    putBoolean("TURBO_AVAILABLE", completedLastStageOfSeries)
                    putBoolean("ENDLESS_AVAILABLE", false)
                }
                GameMechanics.SERIES_TURBO -> {
                    putBoolean("TURBO_AVAILABLE", true)
                    putBoolean("ENDLESS_AVAILABLE", completedLastStageOfSeries)
                }
                GameMechanics.SERIES_ENDLESS -> {
                    putBoolean("TURBO_AVAILABLE", true)
                    putBoolean("ENDLESS_AVAILABLE", true)
                }
            }
            apply()
        }
    }

    private fun beginGame(resetProgress: Boolean = false,
                          resetEndless: Boolean = false,
                          resumeGame: Boolean = false,
                          startingLevel: Stage.Identifier = Stage.Identifier())
    /** Begins the current game on a chosen level. Also called when starting a completely
     * new game.
     * @param resetProgress If true, the whole game is started from the first level, and
     * all coins and heroes are cleared. Otherwise, start on the level given in the saved state.
     * @param resetEndless same as resetProgress, but only for the 'endless' series.
     * @param resumeGame Continue the game within a level, at exactly the point where it has been left.
     */
    {
        loadSettings()
        when {
            resetProgress -> {
                gameMechanics.deleteProgressOfSeries(LevelMode.BASIC)
                gameMechanics.currentStage = Stage.Identifier.startOfNewGame
                setLastPlayedStage(Stage.Identifier.startOfNewGame)
                setMaxPlayedStage(Stage.Identifier.startOfNewGame, forceReset=true)
                prepareLevelAtStartOfGame(Stage.Identifier.startOfNewGame)
            }
            resetEndless -> {
                gameMechanics.deleteProgressOfSeries(LevelMode.ENDLESS)
                gameMechanics.currentStage = Stage.Identifier.startOfEndless
                setLastPlayedStage(Stage.Identifier.startOfEndless)
                setMaxPlayedStage(Stage.Identifier.startOfEndless, forceReset=true)
                prepareLevelAtStartOfGame(Stage.Identifier.startOfEndless)
            }
            else -> {
                Persistency(this).let {
                    it.loadAllStageSummaries(gameMechanics)
                    it.loadAllHeroes(gameMechanics)
                    it.loadGeneralState(gameMechanics)
                    it.loadCoins(gameMechanics)
                }
                if (gameMechanics.purseOfCoins[LevelMode.BASIC]?.initialized == false || forceHeroMigration)
                    gameMechanics.migrateHeroes()
                if (resumeGame)
                    resumeGame()
                else {
                    gameMechanics.currentStage = startingLevel
                    prepareLevelAtStartOfGame(startingLevel)
                }
            }
        }
    }

    private fun resumeGame()
    /** function to resume a running game at exactly the point where the app was left. */
    {
        Persistency(this).loadCurrentLevelState(gameMechanics)
        gameMechanics.stageData?.let {
            gameMechanics.currentStage = it.ident
            gameMechanics.currentlyActiveStage = Stage.createStageFromData(gameMechanics, gameView, it)
        }
        gameMechanics.currentlyActiveStage?.let {
            it.network.validateViewport()
            gameView.viewport.setGridSize(it.sizeX, it.sizeY)
            gameView.background.prepareAtStartOfStage(it.data.ident)
        }
        when (gameMechanics.state.phase)
        {
            GamePhase.MARKETPLACE -> {
                gameView.marketplace.nextGameLevel = gameMechanics.currentStage
                setGameActivityStatus(GameActivityStatus.BETWEEN_LEVELS)
            }
            GamePhase.INTERMEZZO -> {
                setGameActivityStatus(GameActivityStatus.BETWEEN_LEVELS)
            }
            GamePhase.START -> {
                setGameActivityStatus(GameActivityStatus.BETWEEN_LEVELS)
                gameMechanics.currentlyActiveStage?.let {
                    gameMechanics.currentlyActiveWave = if (it.waves.size > 0) it.waves[0]
                    else it.nextWave() }
            }
            else -> {
                gameMechanics.currentlyActiveStage?.let {
                    gameMechanics.currentlyActiveWave = if (it.waves.size > 0) it.waves[0]
                    else it.nextWave()
                }
            }
        }
        gameMechanics.state.phase = GamePhase.RUNNING
    }

    private fun startGameThreads() {

        if (updateJob?.isActive != true)  // (!= true) is not the same as (false) here!
            updateJob = GlobalScope.launch { delay(updateDelay); update(); }

        if (effectsJob?.isActive != true)
            effectsJob = GlobalScope.launch { delay(effectsDelay); updateGraphicalEffects(); }
    }

    private fun loadSettings()
            /** load global configuration and debug settings from preferences */
    {
        val prefs = getSharedPreferences(Persistency.filename_settings, MODE_PRIVATE)
        settings.loadFromFile(prefs)
    }

    fun setGameSpeed(speed: GameMechanics.GameSpeed) {
        gameMechanics.state.speed = speed
        Persistency(this).saveGeneralState(gameMechanics)
        if (speed == GameMechanics.GameSpeed.MAX) {
            updateDelay = fastForwardDelay
            if (settings.fastFastForward)
                updateDelay = fastFastForwardDelay
        } else {
            updateDelay = defaultDelay
        }
    }

    fun showReturnDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.layout_dialog_replay)
        dialog.window?.setLayout(MATCH_PARENT, WRAP_CONTENT)
        dialog.setCancelable(true)
        dialog.findViewById<Button>(R.id.button_return)
            ?.setOnClickListener { dialog.dismiss(); returnToMainMenu() }
        dialog.findViewById<Button>(R.id.button_replay)
            ?.setOnClickListener { dialog.dismiss(); replayLevel() }
        dialog.findViewById<Button>(R.id.button_cancel)
            ?.setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener { gameIsRunning = true; startGameThreads() }
        gameIsRunning = false
        dialog.show()
    }

    private fun returnToMainMenu() {
        Persistency(this).saveGeneralState(gameMechanics)
        Persistency(this).saveCurrentLevelState(gameMechanics)
        finish()
    }

    private fun replayLevel() {
        gameMechanics.currentlyActiveStage?.let { beginGame(startingLevel = it.data.ident) }
    }

    fun showPurchaseLifeDialog(showHint: Boolean = true)
            /** @param showHint Whether to display the text how to disable this dialog
             */
    {
        val price = gameMechanics.costOfLife()
        if (price>0 && gameMechanics.currentPurse().canAfford(price)
            && gameMechanics.state.lives<gameMechanics.state.currentMaxLives)
        {
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.layout_dialog_purchaselife)
            dialog.window?.setLayout(MATCH_PARENT, WRAP_CONTENT)
            dialog.setCancelable(true)
            dialog.findViewById<TextView>(R.id.textView)?.text =
                resources.getString(R.string.query_purchase_life)
                    .format(PurseOfCoins.coinsAsString(gameMechanics.costOfLife(), resources))
            if (!showHint)
                dialog.findViewById<TextView>(R.id.textViewAnnotation)?.text = ""
            dialog.findViewById<Button>(R.id.button_yes)
                ?.setOnClickListener { restoreOneLife(); dialog.dismiss(); }
            dialog.findViewById<Button>(R.id.button_no)
                ?.setOnClickListener { dialog.dismiss(); }
            dialog.setOnDismissListener { gameIsRunning = true; startGameThreads() }
            gameIsRunning = false
            dialog.show()
        }
    }

    private fun prepareLevelAtStartOfGame(ident: Stage.Identifier)
            /** function that is called when starting a new level from the main menu.
             * Does not get called when resuming a running game.
              */
    {
        gameView.resetAtStartOfStage()
        gameView.intermezzo.prepareLevel(ident, true)
    }

    private fun update()
    /** Thread for all physical processes on the screen, i.e. movement of attackers, cool-down times, etc.
     * This thread must run on a fixed pace. When accelerating the game, the delay of this thread is shortened. */
    {
        if (gameIsRunning) {
            val timeAtStartOfCycle = SystemClock.uptimeMillis()
            gameMechanics.ticksCount++
            try {
                gameMechanics.update(this)
            }
            catch (ex: TemperatureDamageException)
            {
                removeOneLife()
                runOnUiThread {
                    val toast: Toast = Toast.makeText(this, resources.getString(R.string.overheat), Toast.LENGTH_SHORT)
                    toast.show()
                }
            }
            catch (ex: CpuReached)
            {
                removeOneLife()
            }
            gameView.scoreBoard.update()
            // determine whether to update the display
            if (timeAtStartOfCycle-timeOfLastFrame > 30 && displayJob?.isActive != true)
                displayJob = GlobalScope.launch { display() }
            val elapsed = SystemClock.uptimeMillis() - timeAtStartOfCycle
            val wait: Long =
                if (updateDelay > elapsed) updateDelay - elapsed - 1 else 0  // rest of time in this cycle
            updateJob = GlobalScope.launch { delay(wait); update() }
        }
    }

    private fun removeOneLife()
    {
        val livesLeft = gameMechanics.removeOneLife()
        Persistency(this).saveGeneralState(gameMechanics)
        when (livesLeft)
        {
            0-> {
                gameMechanics.takeLevelSnapshot(this)
                gameView.intermezzo.endOfGame(gameMechanics.currentStage, hasWon = false)
            }
            1 -> {
                if (!settings.configDisablePurchaseDialog)
                    runOnUiThread { showPurchaseLifeDialog() }
            }
            else -> {}
        }
    }

    private fun restoreOneLife()
    {
        gameMechanics.restoreOneLife()
        Persistency(this).saveGeneralState(gameMechanics)
        Persistency(this).saveCoins(gameMechanics)
    }

    private fun display()
    /** Thread for refreshing the display on the screen.
     * The delay between two executions may vary. */
    {
        if (gameIsRunning) {
            gameMechanics.frameCount++
            val timeAtStartOfFrame = SystemClock.uptimeMillis()
            val timeSinceLastFrame = timeAtStartOfFrame - timeOfLastFrame
            timeOfLastFrame = timeAtStartOfFrame
            gameView.display()
            /* calculate mean time per frame */
            frameTimeSum += timeSinceLastFrame
            frameCount += 1
            if (frameCount >= meanCount) {
                gameMechanics.timeBetweenFrames = (frameTimeSum / frameCount).toDouble()
                frameCount = 0
                frameTimeSum = 0
            }
        }
    }

    private fun updateGraphicalEffects()
    /** do all faders, explosions etc. This thread is independent of the update() cycle. */
    {
        if (gameIsRunning) {
            gameView.updateEffects()
            gameView.effects?.updateGraphicalEffects()
            GlobalScope.launch { delay(effectsDelay); updateGraphicalEffects() }
        }
    }

    fun setGameActivityStatus(status: GameActivityStatus) {
        val prefs = getSharedPreferences(Persistency.filename_state, MODE_PRIVATE)
        val editor = prefs.edit()
        when (status) {
            GameActivityStatus.PLAYING -> editor.putString("STATUS", "running")
            GameActivityStatus.BETWEEN_LEVELS -> editor.putString("STATUS", "complete")
        }
        editor.apply()
    }
}