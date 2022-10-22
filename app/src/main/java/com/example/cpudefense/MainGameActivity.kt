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
    var mainDelay: Long = 0
    private val effectsDelay: Long = 15
    lateinit var theGame: Game
    lateinit var theGameView: GameView
    private var startOnLevel = -1
    private var resumeGame = true
    var gameIsRunning = true  // flag used to keep the threads running. Set to false when leaving activity

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

        startOnLevel = intent.getIntExtra("START_ON_STAGE", -1)

        if (intent.getBooleanExtra("RESUME_GAME", false) == false)
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
            startOnLevel == -1 -> startNewGame()
            else -> startGameAtLevel(startOnLevel)
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
        theGame.beginGame(resetProgress = true)
    }

    private fun startGameAtLevel(level: Int)
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
        theGame.continueGame()
        if (theGame.state.phase == Game.GamePhase.RUNNING) {
            runOnUiThread {
                val toast: Toast = Toast.makeText(this, "Stage %d".format(theGame.currentStage?.data?.level), Toast.LENGTH_SHORT )
                toast.show()
            }
        }
    }

    private fun startGameThreads()
    {

        GlobalScope.launch{ delay(mainDelay); update(); }

        GlobalScope.launch{ delay(effectsDelay); updateGraphicalEffects(); }
    }

    fun loadGameSettings()
    /** load global configuration and debug settings from preferences */
    {
        val prefs = getSharedPreferences(getString(R.string.pref_filename), MODE_PRIVATE)
        theGame.global.configDisableBackground = prefs.getBoolean("DISABLE_BACKGROUND", false)
    }

    fun setGameSpeed(speed: Game.GameSpeed)
    {
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

    fun loadState()
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

    fun loadLevelData(): HashMap<Int, Stage.Summary>
    {
        val prefs = getSharedPreferences(getString(R.string.pref_filename), MODE_PRIVATE)
        return Persistency(theGame).loadLevelSummaries(prefs) ?: HashMap()
    }

    fun loadUpgrades(): HashMap<Upgrade.Type, Upgrade>
    {
        val prefs = getSharedPreferences(getString(R.string.pref_filename), MODE_PRIVATE)
        return Persistency(theGame).loadUpgrades(prefs)
    }
}