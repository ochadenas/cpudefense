package com.example.cpudefense

import android.app.Activity
import android.app.Dialog
import android.content.res.Resources.NotFoundException
import android.graphics.Typeface
import android.os.Bundle
import android.os.SystemClock
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainGameActivity : Activity() {
    lateinit var theGame: Game
    lateinit var theGameView: GameView
    private var startOnLevel: Stage.Identifier? = null
    private var resumeGame = true
    private var gameIsRunning =
        true  // flag used to keep the threads running. Set to false when leaving activity

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
    /** how many samples have been taken */
    private var frameCount = 0
    /** cumulated time */
    private var frameTimeSum = 0L

    /** font for displaying "computer messages" */
    lateinit var monoTypeface: Typeface
    lateinit var boldTypeface: Typeface

    enum class GameActivityStatus { PLAYING, BETWEEN_LEVELS }

    data class Settings(
        var configDisableBackground: Boolean = true,
        var configShowAttsInRange: Boolean = false,
        var configUseLargeButtons: Boolean = false,
        var showFramerate: Boolean = false,
        var fastFastForward: Boolean = false,
        var keepLevels: Boolean = true,
    )

    var settings = Settings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /* here, the size of the surfaces might not be known */
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main_game)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        theGame = Game(this)
        setupGameView()
    }

    fun setupGameView()
    {
        setComputerTypeface()
        theGameView = GameView(this, theGame)
        val parentView: FrameLayout? = findViewById(R.id.gameFrameLayout)
        parentView?.addView(theGameView)

        if (intent.getBooleanExtra("RESET_PROGRESS", false) == false) {
            startOnLevel = Stage.Identifier(
                    series = intent.getIntExtra("START_ON_SERIES", 1),
                    number = intent.getIntExtra("START_ON_STAGE", 1)
            )
        } else
            startOnLevel = null
        if (!intent.getBooleanExtra("RESUME_GAME", false))
            resumeGame = false
        theGameView.setup()
    }

    fun setComputerTypeface()
    {
        try
        {
            monoTypeface = ResourcesCompat.getFont(this, R.font.ubuntu_mono) ?: Typeface.MONOSPACE
            boldTypeface = ResourcesCompat.getFont(this, R.font.ubuntu_mono_bold) ?: Typeface.MONOSPACE
        }
        catch (ex: NotFoundException)
        {
            monoTypeface = Typeface.MONOSPACE
            boldTypeface = Typeface.MONOSPACE
        }
    }


    override fun onPause() {
        // this method get executed when the user presses the system's "back" button,
        // but also when she navigates to another app
        Persistency(this).saveState(theGame)
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
        loadSettings()
        when {
            resumeGame -> resumeCurrentGame()
            startOnLevel == null -> startNewGame()
            else -> startGameAtLevel(startOnLevel ?: Stage.Identifier())
        }
        resumeGame = true
        gameIsRunning = true
        startGameThreads()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun startNewGame()
            /** starts a new game from level 1, discarding all progress */
    {
        theGame.setLastPlayedStage(Stage.Identifier())
        theGame.beginGame(resetProgress = true)
    }

    private fun startGameAtLevel(level: Stage.Identifier)
            /** continues a current match at a given level, keeping the progress and upgrades */
    {
        theGame.state.startingLevel = level
        theGame.beginGame(resetProgress = false)
    }

    private fun resumeCurrentGame()
            /** continues at exactly the same point within a level, restoring the complete game state.
             */
    {
        Persistency(this).loadState(theGame)
        theGame.resumeGame()
        if (theGame.state.phase == Game.GamePhase.RUNNING) {
            runOnUiThread {
                val toast: Toast = Toast.makeText(
                    this,
                    "Stage %d".format(theGame.currentStage.number),
                    Toast.LENGTH_SHORT
                )
                toast.show()
            }
        }
    }

    private fun startGameThreads() {

        updateJob = GlobalScope.launch { delay(updateDelay); update(); }

        GlobalScope.launch { delay(effectsDelay); updateGraphicalEffects(); }
    }

    fun loadSettings()
            /** load global configuration and debug settings from preferences */
    {
        val prefs = getSharedPreferences(getString(R.string.pref_filename), MODE_PRIVATE)
        settings.configDisableBackground = prefs.getBoolean("DISABLE_BACKGROUND", false)
        settings.configShowAttsInRange = prefs.getBoolean("SHOW_ATTS_IN_RANGE", false)
        settings.configUseLargeButtons = prefs.getBoolean("USE_LARGE_BUTTONS", false)
        settings.showFramerate = prefs.getBoolean("SHOW_FRAMERATE", false)
        settings.keepLevels = prefs.getBoolean("KEEP_LEVELS", true)
        settings.fastFastForward = prefs.getBoolean("USE_FAST_FAST_FORWARD", false)
    }

    fun setGameSpeed(speed: Game.GameSpeed) {
        theGame.global.speed = speed
        if (speed == Game.GameSpeed.MAX) {
            updateDelay = fastForwardDelay
            if (settings.fastFastForward)
                updateDelay = fastFastForwardDelay
            theGame.background?.frozen = true
        } else {
            updateDelay = defaultDelay
            theGame.background?.frozen = false
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


        dialog.show()
    }

    private fun returnToMainMenu() {
        Persistency(this).saveState(theGame)
        finish()
    }

    private fun replayLevel() {
        theGame.currentlyActiveStage?.let { startGameAtLevel(it.data.ident) }
    }

    private fun update()
    /** Thread for all physical processes on the screen, i.e. movement of attackers, cool-down times, etc.
     * THis thread must run on a fixed pace. When accelerating the game, the delay of this thread is shortened. */
    {
        if (gameIsRunning) {
            val timeAtStartOfCycle = SystemClock.uptimeMillis()
            theGame.ticksCount++
            theGame.update()

            // determine whether to update the display
            if (timeAtStartOfCycle-timeOfLastFrame > 30 && displayJob?.isActive != true)
                displayJob = GlobalScope.launch { display() }
            val elapsed = SystemClock.uptimeMillis() - timeAtStartOfCycle
            val wait: Long =
                if (updateDelay > elapsed) updateDelay - elapsed - 1 else 0  // rest of time in this cycle
            updateJob = GlobalScope.launch { delay(wait); update() }
        }
    }

    private fun display()
    /** Thread for refreshing the display on the screen.
     * The delay between two executions may vary. */
    {
        if (gameIsRunning) {
            theGame.frameCount++
            val timeAtStartOfFrame = SystemClock.uptimeMillis()
            val timeSinceLastFrame = timeAtStartOfFrame - timeOfLastFrame
            timeOfLastFrame = timeAtStartOfFrame
            theGameView.display()
            /* calculate mean time per frame */
            frameTimeSum += timeSinceLastFrame
            frameCount += 1
            if (frameCount >= meanCount) {
                theGame.timeBetweenFrames = (frameTimeSum / frameCount).toDouble()
                frameCount = 0
                frameTimeSum = 0
            }
        }
    }


    private fun updateGraphicalEffects()
    /** do all faders, explosions etc. This thread is independent of the update() cycle. */
    {
        if (gameIsRunning) {
            theGame.updateEffects()
            theGameView.theEffects?.updateGraphicalEffects()
            GlobalScope.launch { delay(effectsDelay); updateGraphicalEffects() }
        }
    }

    fun setGameActivityStatus(status: GameActivityStatus) {
        val prefs = getSharedPreferences(getString(R.string.pref_filename), MODE_PRIVATE)
        val editor = prefs.edit()
        when (status) {
            GameActivityStatus.PLAYING -> editor.putString("STATUS", "running")
            GameActivityStatus.BETWEEN_LEVELS -> editor.putString("STATUS", "complete")
        }
        editor.apply()
    }
}