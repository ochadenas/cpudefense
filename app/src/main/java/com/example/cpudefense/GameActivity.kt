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
import androidx.core.app.ActivityCompat
import com.example.cpudefense.GameMechanics.GamePhase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameActivity : Activity() {
    lateinit var gameMechanics: GameMechanics
    lateinit var gameView: GameView
    private var startOnLevel: Stage.Identifier? = null
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

    data class Settings(
        var configDisablePurchaseDialog: Boolean = false,
        var configDisableBackground: Boolean = true,
        var configShowAttackersInRange: Boolean = false,
        var configUseLargeButtons: Boolean = false,
        var showFrameRate: Boolean = false,
        var fastFastForward: Boolean = false,
        var keepLevels: Boolean = true,
    )

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
        Persistency(this).saveState(gameMechanics)
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
        Toast.makeText(this, "Loading ...", Toast.LENGTH_SHORT).show()
        loadSettings()
        setupGameView()

        // determine what to do: resume, restart, or play next level
        if (!intent.getBooleanExtra("RESET_PROGRESS", false)) {
            startOnLevel = Stage.Identifier(
                    series = intent.getIntExtra("START_ON_SERIES", 1),
                    number = intent.getIntExtra("START_ON_STAGE", 1)
            )
        } else
            startOnLevel = null
        if (!resumeGame)
            resumeGame = intent.getBooleanExtra("RESUME_GAME", false)
        when {
            resumeGame -> resumeCurrentGame()
            startOnLevel == null -> startNewGame()
            else -> startGameAtLevel(startOnLevel ?: Stage.Identifier())
        }
        gameIsRunning = true
        resumeGame = true // for the next time we come here
        startGameThreads()
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
        val prefs = getSharedPreferences(Persistency.filename_preferences, Context.MODE_PRIVATE)
        with (prefs.edit())
        {
            putInt("LASTSTAGE", identifier.number)
            putInt("LASTSERIES", identifier.series)
            commit()
        }
    }

    fun setMaxPlayedStage(identifier: Stage.Identifier, resetProgress: Boolean = false)
            /** when completing a level, record the level as highest completed, but only if the old max level is not higher.
             * @param identifier number of the level successfully completed
             * @param resetProgress If true, forces resetting the max stage to the given currentStage
             * */
    {
        val prefs = getSharedPreferences(Persistency.filename_preferences, Context.MODE_PRIVATE)
        val maxStage = Stage.Identifier(prefs.getInt("MAX SERIES", 1), prefs.getInt("MAXSTAGE", 0))
        with (prefs.edit())
        {
            if (resetProgress)
            {
                putInt("MAXSTAGE", 0)
                putInt("MAXSERIES", 1)
                putBoolean("TURBO_AVAILABLE", false)
                putBoolean("ENDLESS_AVAILABLE", false)
                apply()
            }
            if (identifier.isGreaterThan(maxStage))
            {
                putInt("MAXSTAGE", identifier.number)
                putInt("MAXSERIES", identifier.series)
                apply()
            }
            // make advanced series available
            when (identifier.series)
            {
                1 ->
                    if (identifier.number== com.example.cpudefense.GameMechanics.maxLevelAvailable)
                        putBoolean("TURBO_AVAILABLE", true)
                2 -> {
                    putBoolean("TURBO_AVAILABLE", true)
                    if (identifier.number == com.example.cpudefense.GameMechanics.maxLevelAvailable)
                        putBoolean("ENDLESS_AVAILABLE", true)
                }
                3 -> {
                    putBoolean("TURBO_AVAILABLE", true)
                    putBoolean("ENDLESS_AVAILABLE", true)
                }
            }
            commit()
        }
    }

    private fun startNewGame()
            /** starts a new game from level 1, discarding all progress */
    {
        setLastPlayedStage(Stage.Identifier())
        beginGame(resetProgress = true)
    }

    private fun startGameAtLevel(level: Stage.Identifier)
            /** continues a current match at a given level, keeping the progress and upgrades */
    {
        gameMechanics.state.startingLevel = level
        beginGame(resetProgress = false)
    }

    private fun resumeCurrentGame()
            /** continues at exactly the same point within a level, restoring the complete game state.
             */
    {
        Persistency(this).loadState(gameMechanics)
        beginGame(resumeGame = true)
        if (gameMechanics.state.phase == GameMechanics.GamePhase.RUNNING) {
            runOnUiThread {
                val toast: Toast = Toast.makeText(
                        this,
                        "Stage %d".format(gameMechanics.currentStage.number),
                        Toast.LENGTH_SHORT
                )
                toast.show()
            }
        }
    }

    fun beginGame(resetProgress: Boolean = false, resumeGame: Boolean = false)
    {
        /** Begins the current game on a chosen level. Also called when starting a completely
         * new game.
         * @param resetProgress If true, the whole game is started from the first level, and
         * all coins and heroes are cleared. Otherwise, start on the level given in the saved state.
         */
        loadSettings()
        if (resetProgress)
        {
            gameMechanics.beginGameAndResetProgress()
            setLastPlayedStage(gameMechanics.state.startingLevel)
            setMaxPlayedStage(gameMechanics.state.startingLevel, resetProgress=true)
            prepareLevelAtStartOfGame(gameMechanics.state.startingLevel)
        }
        else
        {
            val persistency = Persistency(this)
            gameMechanics.beginGameWithoutResettingProgress(persistency)
            if (resumeGame)
                resumeGame()
            else
            {
                gameMechanics.currentStage = gameMechanics.state.startingLevel
                prepareLevelAtStartOfGame(gameMechanics.state.startingLevel)
            }
        }
    }

    fun resumeGame()
    /** function to resume a running game at exactly the point where the app was left. */
    {
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
                gameView.marketplace.nextGameLevel = gameMechanics.state.startingLevel
                setGameActivityStatus(GameActivityStatus.BETWEEN_LEVELS)
            }
            GamePhase.INTERMEZZO -> {
                setGameActivityStatus(GameActivityStatus.BETWEEN_LEVELS)
            }
            GamePhase.START -> {
                setGameActivityStatus(GameActivityStatus.BETWEEN_LEVELS)
            }
            else -> {
                gameMechanics.currentlyActiveStage?.let {
                    gameMechanics.currentlyActiveWave = if (it.waves.size > 0) it.waves[0]
                    else it.nextWave()
                }
                gameMechanics.state.phase = GamePhase.RUNNING
            }
        }
    }

    private fun startGameThreads() {

        if (updateJob?.isActive != true)  // (!= true) is not the same as (false) here!
            updateJob = GlobalScope.launch { delay(updateDelay); update(); }

        if (effectsJob?.isActive != true)
            effectsJob = GlobalScope.launch { delay(effectsDelay); updateGraphicalEffects(); }
    }

    fun loadSettings()
            /** load global configuration and debug settings from preferences */
    {
        val prefs = getSharedPreferences(Persistency.filename_preferences, MODE_PRIVATE)
        settings.configDisablePurchaseDialog = prefs.getBoolean("DISABLE_PURCHASE_DIALOG", false)
        settings.configDisableBackground = prefs.getBoolean("DISABLE_BACKGROUND", false)
        settings.configShowAttackersInRange = prefs.getBoolean("SHOW_ATTS_IN_RANGE", false)
        settings.configUseLargeButtons = prefs.getBoolean("USE_LARGE_BUTTONS", false)
        settings.showFrameRate = prefs.getBoolean("SHOW_FRAMERATE", false)
        settings.keepLevels = prefs.getBoolean("KEEP_LEVELS", true)
        settings.fastFastForward = prefs.getBoolean("USE_FAST_FAST_FORWARD", false)
    }

    fun setGameSpeed(speed: GameMechanics.GameSpeed) {
        gameMechanics.global.speed = speed
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
        Persistency(this).saveState(gameMechanics)
        finish()
    }

    private fun replayLevel() {
        gameMechanics.currentlyActiveStage?.let { startGameAtLevel(it.data.ident) }
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

    fun prepareLevelAtStartOfGame(ident: Stage.Identifier)
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
        when (livesLeft)
        {
            0-> {
                gameMechanics.takeLevelSnapshot(this)
                gameView.intermezzo.endOfGame(gameMechanics.currentStage, hasWon = false)
            }
            1 -> {
                if (!settings.configDisablePurchaseDialog)
                    runOnUiThread() { showPurchaseLifeDialog() }
            }
            else -> {}
        }
    }

    private fun restoreOneLife()
    {
        gameMechanics.restoreOneLife()
        Persistency(this).saveState(gameMechanics)
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
        // TODO: better move to "state"
        val prefs = getSharedPreferences(Persistency.filename_preferences, MODE_PRIVATE)
        val editor = prefs.edit()
        when (status) {
            GameActivityStatus.PLAYING -> editor.putString("STATUS", "running")
            GameActivityStatus.BETWEEN_LEVELS -> editor.putString("STATUS", "complete")
        }
        editor.apply()
    }
}