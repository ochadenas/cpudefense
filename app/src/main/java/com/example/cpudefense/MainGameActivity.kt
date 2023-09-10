package com.example.cpudefense

import android.app.Activity
import android.os.Bundle
import android.view.Window
import android.widget.FrameLayout
import android.widget.Toast
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainGameActivity : Activity() {
    private var mainDelay: Long = 0
    private val effectsDelay: Long = 15
    lateinit var theGame: Game
    lateinit var theGameView: GameView
    private var startOnLevel: Stage.Identifier? = null
    private var resumeGame = true
    private var gameIsRunning = true  // flag used to keep the threads running. Set to false when leaving activity

    enum class GameActivityStatus { PLAYING, BETWEEN_LEVELS, UNDETERMINED }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        /* here, the size of the surfaces might not be known */
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main_game)
        theGame = Game(this)
        theGameView = GameView(this, theGame)

        val parentView: FrameLayout? = findViewById(R.id.gameFrameLayout)
        parentView?.addView(theGameView)

        if (intent.getBooleanExtra("RESET_PROGRESS", false) == false)
        {
            startOnLevel = Stage.Identifier(
                series = intent.getIntExtra("START_ON_SERIES", 1),
                number = intent.getIntExtra("START_ON_STAGE", 1)
            )
        }
        else
            startOnLevel = null
        if (!intent.getBooleanExtra("RESUME_GAME", false))
            resumeGame = false
        theGameView.setup()
    }

    override fun onPause() {
        // this method get executed when the user presses the system's "back" button,
        // but also when she navigates to another app
        saveState()
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
        when
        {
            resumeGame -> resumeCurrentGame()
            startOnLevel == null -> startNewGame()
            else -> startGameAtLevel(startOnLevel ?: Stage.Identifier())
        }
        resumeGame = true
        gameIsRunning = true
        loadGameSettings()
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
        loadState()
        theGame.resumeGame()
        if (theGame.state.phase == Game.GamePhase.RUNNING) {
            runOnUiThread {
                val toast: Toast = Toast.makeText(this, "Stage %d".format(theGame.currentStage?.getLevel()), Toast.LENGTH_SHORT )
                toast.show()
            }
        }
    }

    private fun startGameThreads()
    {

        GlobalScope.launch{ delay(mainDelay); update(); }

        GlobalScope.launch{ delay(effectsDelay); updateGraphicalEffects(); }
    }

    private fun loadGameSettings()
    /** load global configuration and debug settings from preferences */
    {
        val prefs = getSharedPreferences(getString(R.string.pref_filename), MODE_PRIVATE)
        theGame.global.configDisableBackground = prefs.getBoolean("DISABLE_BACKGROUND", false)
        theGame.global.configShowAttsInRange = prefs.getBoolean("SHOW_ATTS_IN_RANGE", false)
    }

    fun setGameSpeed(speed: Game.GameSpeed)
    {
        theGame.global.speed = speed
        if (speed == Game.GameSpeed.MAX) {
            mainDelay = 0
            theGame.background?.frozen = true
        }
        else {
            mainDelay = Game.defaultMainDelay
            theGame.background?.frozen = false
        }
    }

    private fun update()
    {
        if (gameIsRunning) {
            theGame.update()
            theGameView.display()
            GlobalScope.launch { delay(mainDelay); update() }
        }
    }

    private fun updateGraphicalEffects()
    {
        if (gameIsRunning) {
            theGame.updateEffects()
            theGameView.theEffects?.updateGraphicalEffects()
            GlobalScope.launch { delay(effectsDelay); updateGraphicalEffects() }
        }
    }

    fun setGameActivityStatus(status: GameActivityStatus)
    {
        val prefs = getSharedPreferences(getString(R.string.pref_filename), MODE_PRIVATE)
        val editor = prefs.edit()
        when (status)
        {
            GameActivityStatus.PLAYING -> editor.putString("STATUS", "running")
            GameActivityStatus.BETWEEN_LEVELS -> editor.putString("STATUS", "complete")
            else -> editor.putString("STATUS", "complete")
        }
        editor.apply()
    }

    fun saveState()
    {
        val prefs = getSharedPreferences(getString(R.string.pref_filename), MODE_PRIVATE)
        val editor = prefs.edit()
        Persistency(theGame).saveState(editor)
        editor.apply()
    }

    fun saveUpgrades()
    {
        val prefs = getSharedPreferences(getString(R.string.pref_filename), MODE_PRIVATE)
        val editor = prefs.edit()
        Persistency(theGame).saveUpgrades(editor)
        editor.apply()
    }

    fun saveThumbnail(level: Int)
    {
        val prefs = getSharedPreferences(getString(R.string.pref_filename), MODE_PRIVATE)
        val editor = prefs.edit()
        Persistency(theGame).saveThumbnailOfLevel(editor, level)
        editor.apply()
    }

    private fun loadState()
    {
        val prefs = getSharedPreferences(getString(R.string.pref_filename), MODE_PRIVATE)
        Persistency(theGame).loadState(prefs)
    }

    fun loadGlobalData(): Game.GlobalData
    /* retrieve some global game data, such as total number of coins.
    Saving is done in saveState().
     */
    {
        val prefs = getSharedPreferences(getString(R.string.pref_filename), MODE_PRIVATE)
        return Persistency(theGame).loadGlobalData(prefs)
    }

    fun loadLevelData(series: Int): HashMap<Int, Stage.Summary>
    {
        val prefs = getSharedPreferences(getString(R.string.pref_filename), MODE_PRIVATE)
        return Persistency(theGame).loadLevelSummaries(prefs, series) ?: HashMap()
    }

    fun loadUpgrades(): HashMap<Hero.Type, Hero>
    {
        val prefs = getSharedPreferences(getString(R.string.pref_filename), MODE_PRIVATE)
        return Persistency(theGame).loadUpgrades(prefs)
    }
}