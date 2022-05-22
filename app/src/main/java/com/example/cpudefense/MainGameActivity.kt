package com.example.cpudefense

import android.app.Activity
import android.os.Bundle
import android.view.Window
import android.widget.FrameLayout
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainGameActivity : Activity() {
    private var mainDelay: Long = 50
    private val effectsDelay: Long = 15
    lateinit var theGame: Game
    lateinit var theGameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /* here, the size of the surfaces might not be known */
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main_game)
        theGame = Game(this)
        theGameView = GameView(this, theGame)

        val parentView: FrameLayout? = findViewById(R.id.gameFrameLayout)
        parentView?.addView(theGameView)

        theGameView.setup()
        startGameThreads()

        if (intent.getBooleanExtra("CONTINUE_GAME", true))
        {
            theGame.continueGame()
        }
        else
        {
            theGame.state.startingLevel = intent.getIntExtra("START_ON_STAGE", 1)
            val resetProgress = intent.getBooleanExtra("RESET_PROGRESS", false)
            theGame.beginGame(resetProgress)
        }
    }

    override fun onPause() {
        // this method get executed when the user presses the system's "back" button,
        // but also when she navigates to another app
        saveState()
        theGame.state.phase = Game.GamePhase.END
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        // loadState()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun startGameThreads()
    {

        GlobalScope.launch{ delay(mainDelay); update(); }

        GlobalScope.launch{ delay(effectsDelay); updateGraphicalEffects(); }
    }


    fun setGameSpeed(speed: Game.GameSpeed)
    {
        if (speed == Game.GameSpeed.MAX)
            mainDelay = 1
        else
            mainDelay = 50
    }


    private fun update()
    {
        if (theGame.state.phase == Game.GamePhase.END)
            return
        theGame.update()
        theGameView.display()
        GlobalScope.launch{ delay(mainDelay); update() }
    }

    private fun updateGraphicalEffects()
    {
        if (theGame.state.phase == Game.GamePhase.END)
            return
        theGame.updateEffects()
        theGameView.theEffects?.updateGraphicalEffects()
        GlobalScope.launch{ delay(effectsDelay); updateGraphicalEffects() }
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