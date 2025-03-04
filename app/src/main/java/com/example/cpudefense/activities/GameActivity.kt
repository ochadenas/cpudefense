@file:Suppress("SpellCheckingInspection")

package com.example.cpudefense.activities

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
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
import com.example.cpudefense.CpuReached
import com.example.cpudefense.GameMechanics
import com.example.cpudefense.GameMechanics.GamePhase
import com.example.cpudefense.GameMechanics.GameSpeed
import com.example.cpudefense.GameMechanics.LevelMode
import com.example.cpudefense.GameMechanics.Params.SERIES_ENDLESS
import com.example.cpudefense.GameMechanics.Params.SERIES_NORMAL
import com.example.cpudefense.GameMechanics.Params.SERIES_TURBO
import com.example.cpudefense.GameMechanics.Params.forceHeroMigration
import com.example.cpudefense.GameView
import com.example.cpudefense.Persistency
import com.example.cpudefense.PurseOfCoins
import com.example.cpudefense.R
import com.example.cpudefense.Settings
import com.example.cpudefense.Stage
import com.example.cpudefense.Stage.Identifier
import com.example.cpudefense.StageCatalog
import com.example.cpudefense.TemperatureDamageException
import com.example.cpudefense.utils.Logger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameActivity : Activity() {
    var logger: Logger? = null
    lateinit var gameMechanics: GameMechanics
    lateinit var gameView: GameView
    /** flag used to keep the threads running. Set to false when leaving activity */
    private var gameThreadsRunning = true

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

    // additional properties for displaying an average frame rate
    /** the moment when the last frame was displayed (in ms since last device reboot) */
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
    /** level snapshots (common for series 1 and 2) */
    var levelThumbnail = HashMap<Int, Bitmap?>()
    /** level snapshots for series 3 */
    var levelThumbnailEndless = HashMap<Int, Bitmap?>()

    val settings = Settings()

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
        requestWindowFeature(Window.FEATURE_NO_TITLE) // method of Activity
        setContentView(R.layout.activity_main_game)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (intent.getBooleanExtra("ACTIVATE_LOGGING", false) && GameMechanics.enableLogging)
            logger = Logger(this, GameMechanics.logLevel)
        logger?.start()
        resumeGame = false
        gameMechanics = GameMechanics()
        gameView = GameView(this)
    }

    override fun onPause()
    /** this method get executed when the user presses the system's "back" button,
     *  but also when she navigates to another app
     */
    {
        logger?.log("Pausing Game Activity")
        Persistency(this).saveGeneralState(gameMechanics)
        Persistency(this).saveCurrentLevelState(gameMechanics)
        gameThreadsRunning = false
        super.onPause()
    }

    override fun onResume()
    /** this function gets called in any case, regardless of whether
     * a new game is started or the user just navigates back to the app.
     */
    {
        super.onResume()
        Toast.makeText(this, resources.getString(R.string.toast_loading), Toast.LENGTH_SHORT).show()
        loadSettings()
        setupGameView()
        logger?.log("Entering game activity, game state is %s".format(gameMechanics.state.toString()))

        // determine what to do: resume, restart, or play next level
        val restartGame = intent.getBooleanExtra("RESET_PROGRESS", false)
        val restartEndless = intent.getBooleanExtra("RESET_ENDLESS", false)
        val startOnLevel = when
        {
            restartGame -> Stage.Identifier.startOfNewGame
            restartEndless -> Stage.Identifier.startOfEndless
            else -> Identifier(
                    series = intent.getIntExtra("START_ON_SERIES", SERIES_NORMAL),
                    number = intent.getIntExtra("START_ON_STAGE", 1)
            )
        }
        if (!resumeGame)
            resumeGame = intent.getBooleanExtra("RESUME_GAME", false)

        when (gameMechanics.state.phase)
        {
            GamePhase.START -> {
                beginGame(resumeGame = resumeGame, resetProgress = restartGame, resetEndless = restartEndless, startingLevel = startOnLevel)
            }
            GamePhase.RUNNING -> {
                beginGame(resumeGame = resumeGame, resetProgress = restartGame, resetEndless = restartEndless, startingLevel = startOnLevel)
                if (resumeGame)
                    showStageMessage(gameMechanics.currentlyActiveStage)
            }
            GamePhase.INTERMEZZO -> {}
            GamePhase.MARKETPLACE -> {}
            GamePhase.PAUSED -> {}
        }
        gameThreadsRunning = true
        resumeGame = true // for the next time we come here
        setGameSpeed(GameSpeed.NORMAL) // always start with normal speed
        startGameThreads()
    }

    private fun showStageMessage(stage: Stage?)
    {
        runOnUiThread { stage?.let {Toast.makeText(this, resources.getString(R.string.toast_enter_stage).format(it.numberAsString()),
                                       Toast.LENGTH_SHORT).show() }}
    }

    override fun onStop() {
        logger?.log("Stopping Game Activity")
        super.onStop()
    }

    override fun onDestroy() {
        logger?.log("Ending Game Activity")
        logger?.stop()
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

    fun changeToGamePhase(phase: GamePhase)
    {
        gameMechanics.state.phase = phase
        logger?.log("Switching to game phase %s".format(phase.toString()))
    }


    fun setLastPlayedStage(identifier: Identifier)
    /** when completing a level, record the current number in the SharedPrefs.
     * @param identifier number of the level successfully completed */
    {
        logger?.log("Setting last played stage to series %d / level %d.".format(identifier.series, identifier.number))
        val prefs = getSharedPreferences(Persistency.filename_state, Context.MODE_PRIVATE)
        with (prefs.edit())
        {
            putInt("LASTSTAGE", identifier.number)
            putInt("LASTSERIES", identifier.series)
            commit()
        }
    }

    fun setMaxPlayedStage(identifier: Identifier, forceReset: Boolean = false)
            /** when completing a level, record the level as highest completed, but only if the old max level is not higher.
             * @param identifier number of the level successfully completed
             * @param forceReset If true, forces resetting the max stage to the given currentStage
             * */
    {
        val prefs = getSharedPreferences(Persistency.filename_state, Context.MODE_PRIVATE)
        val previousMaxStage =
            Identifier(prefs.getInt("MAXSERIES", 1), prefs.getInt("MAXSTAGE", 0))
        val newMaxStage = if (identifier.isGreaterThan(previousMaxStage) || forceReset) identifier else previousMaxStage
        logger?.log("Setting max stage to series %d / level %d.".format(newMaxStage.series, newMaxStage.number))
        with (prefs.edit())
        {
            putInt("MAXSTAGE", newMaxStage.number)
            putInt("MAXSERIES", newMaxStage.series)
            // make next series available if last level is completed, otherwise remove access
            val completedLastStageOfSeries: Boolean = (identifier.number == GameMechanics.maxLevelAvailable)
            when (newMaxStage.series) {
                SERIES_NORMAL -> {
                    putBoolean("TURBO_AVAILABLE", completedLastStageOfSeries)
                    putBoolean("ENDLESS_AVAILABLE", false)
                }
                SERIES_TURBO -> {
                    putBoolean("TURBO_AVAILABLE", true)
                    putBoolean("ENDLESS_AVAILABLE", completedLastStageOfSeries)
                }
                SERIES_ENDLESS -> {
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
                          startingLevel: Identifier = Identifier()
    )
    /** Begins the current game on a chosen level. Also called when starting a completely
     * new game.
     * @param resetProgress If true, the whole game is started from the first level, and
     * all coins and heroes are cleared. Otherwise, start on the level given in the saved state.
     * @param resetEndless same as resetProgress, but only for the 'endless' series.
     * @param resumeGame Continue the game within a level, at exactly the point where it has been left.
     */
    {
        loadSettings()
        Persistency(this).let {
            it.loadAllStageSummaries(gameMechanics)
            it.loadAllHeroes(gameMechanics)
            it.loadGeneralState(gameMechanics)
            it.loadCoins(gameMechanics)
        }

        var level = startingLevel
        val resetRequested = (resetEndless || resetProgress)

        if (resetRequested)
        {
            level = Identifier.startOfEndless
            gameMechanics.deleteProgressOfSeries(LevelMode.ENDLESS)
            if (resetProgress)
            // in addition: if a complete reset is requested, also clear the BASIC series
            {
                level = Identifier.startOfNewGame
                gameMechanics.deleteProgressOfSeries(LevelMode.BASIC)
            }
            gameMechanics.currentStageIdent = level
            setLastPlayedStage(level)
            setMaxPlayedStage(level, forceReset=true)
            prepareLevelAtStartOfGame(level)
            Persistency(this).apply {
                saveHeroes(gameMechanics)
                saveCoins(gameMechanics)
                saveStageSummaries(gameMechanics, SERIES_NORMAL)
                saveStageSummaries(gameMechanics, SERIES_TURBO)
                saveStageSummaries(gameMechanics, SERIES_ENDLESS)
            }
        }
        // final actions, to be executed in any case
        if (gameMechanics.purseOfCoins[LevelMode.BASIC]?.initialized == false || forceHeroMigration)
            gameMechanics.migrateHeroes()

        if (resumeGame)
            resumeGame()
        else {
            gameMechanics.currentStageIdent = level
            prepareLevelAtStartOfGame(level)
        }
    }

    private fun resumeGame()
    /** function to resume a running game at exactly the point where the app was left. */
    {
        Persistency(this).loadCurrentLevelState(gameMechanics)
        gameMechanics.stageData?.let {
            gameMechanics.currentStageIdent = it.ident
            gameMechanics.currentlyActiveStage =
                Stage.createStageFromData(gameMechanics, gameView, it)
        }
        gameMechanics.currentlyActiveStage?.let {
            it.network.validateViewport()
            gameView.viewport.setGridSize(it.sizeX, it.sizeY)
            gameView.background.prepareAtStartOfStage(it.data.ident)
            gameView.speedControlPanel.setInfoLine(gameView.resources.getString(R.string.stage_number)
                                                       .format(it.numberAsString()))
        }
        when (gameMechanics.state.phase)
        {
            GamePhase.MARKETPLACE -> {
                gameView.marketplace.nextGameLevel = gameMechanics.currentStageIdent
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
        changeToGamePhase(GamePhase.RUNNING)
    }

    fun startNextStage(ident: Identifier)
    {
        val nextStage = Stage(gameMechanics, gameView)
        logger?.log("Starting level %d of series %d".format(ident.number, ident.series))
        StageCatalog.createStage(nextStage, ident)
        nextStage.calculateDifficulty()
        if (!nextStage.isInitialized())
            return  // something went wrong, possibly trying to create a level that doesn't exist
        nextStage.network.recreateNetworkImage(true)
        setGameActivityStatus(GameActivityStatus.PLAYING)
        with (gameMechanics) {
            currentStageIdent = ident
            calculateLives()
            calculateStartingCash()
            calculateCoins(nextStage)
            setSummaryOfStage(ident, nextStage.summary)
            state.heat = 0.0
            currentlyActiveStage = nextStage
        }
        Persistency(this).saveStageSummaries(gameMechanics, ident.series)
        logger?.log("Saving summary of series %d".format(ident.series))
        gameMechanics.currentlyActiveStage?.let {
            gameView.speedControlPanel.setInfoLine(gameView.resources.getString(R.string.stage_number).format(it.numberAsString()))
            showStageMessage(it)
        }
        setGameSpeed(GameSpeed.NORMAL)  // reset speed to normal when starting next stage
        changeToGamePhase(GamePhase.RUNNING)
        nextStage.gameView.resetAtStartOfStage()
        gameMechanics.currentlyActiveWave = nextStage.nextWave()
        Persistency(this).saveCurrentLevelState(gameMechanics)
        Persistency(this).saveGeneralState(gameMechanics)
        takeLevelSnapshot()
    }

    fun onEndOfStage()
    {
        if (gameMechanics.currentlyActiveStage == null)
            return // in this case, the stage has already been left
        gameMechanics.currentlyActiveStage?.let {
            if (it.attackerCount()>0)
            // still attackers left, wait until wave is really over
                GlobalScope.launch { delay(2000L); onEndOfStage() }
            else {
                onStageCleared(it)
                Persistency(this).saveGeneralState(gameMechanics)
                Persistency(this).saveStageSummaries(gameMechanics, gameMechanics.currentStageIdent.series)
                setGameActivityStatus(GameActivityStatus.BETWEEN_LEVELS)
                takeLevelSnapshot()
            }
        }
    }

    fun onStageCleared(stage: Stage)
    {
        runOnUiThread { Toast.makeText(this, resources.getString(R.string.toast_stage_cleared), Toast.LENGTH_SHORT).show() }
        gameView.intermezzo.coinsGathered = gameMechanics.state.coinsExtra + gameMechanics.state.coinsInLevel
        gameMechanics.currentPurse().addReward(gameView.intermezzo.coinsGathered)
        val summaryOfCompletedStage = Stage.Summary(won = true,
                                                    coinsGot = stage.summary.coinsGot + gameMechanics.state.coinsInLevel,
                                                    coinsMaxAvailable = stage.summary.coinsMaxAvailable,
                                                    coinsAvailable = stage.summary.coinsMaxAvailable - gameMechanics.state.coinsInLevel
        )
        val currentStageIdent = stage.data.ident
        gameMechanics.currentStageIdent = currentStageIdent
        val nextStage = currentStageIdent.next()
        gameMechanics.setSummaryOfStage(currentStageIdent, summaryOfCompletedStage)
        setMaxPlayedStage(currentStageIdent)
        if (stage.data.type == Stage.Type.FINAL)
        {
            gameView.intermezzo.endOfGame(currentStageIdent, hasWon = true)
        }
        else {
            // make next level available. Create an empty one if necessary
            gameMechanics.setSummaryOfStage(nextStage, gameMechanics.getSummaryOfStage(nextStage) ?: Stage.Summary())
            gameView.intermezzo.prepareLevel(nextStage, false)
        }
        Persistency(this).let {
            it.saveCoins(gameMechanics)
            it.saveStageSummaries(gameMechanics, currentStageIdent.series)
            it.saveGeneralState(gameMechanics)
        }
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

    fun setGameSpeed(speed: GameSpeed) {
        gameMechanics.state.speed = speed
        Persistency(this).saveGeneralState(gameMechanics)
        updateDelay = when (speed)
        {
            GameSpeed.NORMAL -> defaultDelay
            GameSpeed.FAST -> fastForwardDelay
            GameSpeed.MAX -> fastFastForwardDelay
        }
        logger?.log("Game speed set to %s. Update delay is %s".format(speed.toString(), updateDelay.toString()))
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
        dialog.setOnDismissListener { gameThreadsRunning = true; startGameThreads() }
        gameThreadsRunning = false
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
            dialog.setOnDismissListener { gameThreadsRunning = true; startGameThreads() }
            gameThreadsRunning = false
            dialog.show()
        }
    }

    private fun prepareLevelAtStartOfGame(ident: Identifier)
            /** function that is called when starting a new level from the main menu.
             * Does not get called when resuming a running game.
              */
    {
        logger?.log("Preparing level %s".format(ident.toString()))
        gameView.resetAtStartOfStage()
        gameView.intermezzo.prepareLevel(ident, true)
    }

    private fun update()
    /** Thread for all physical processes on the screen, i.e. movement of attackers, cool-down times, etc.
     * This thread must run on a fixed pace. When accelerating the game, the delay of this thread is shortened. */
    {
        if (gameThreadsRunning) {
            val timeAtStartOfCycle = SystemClock.uptimeMillis()
            gameMechanics.ticksCount++
            try {
                gameMechanics.update()
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
                takeLevelSnapshot()
                gameView.intermezzo.endOfGame(gameMechanics.currentStageIdent, hasWon = false)
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
        if (gameThreadsRunning) {
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
        if (gameThreadsRunning) {
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
        logger?.log("Activity status set to %s".format(status.toString()))
    }

    private fun takeLevelSnapshot()
    {
        gameMechanics.currentlyActiveStage?.let {
            if (it.getSeries() == SERIES_ENDLESS)
                levelThumbnailEndless[it.getLevel()] = it.takeSnapshot(GameView.levelSnapshotIconSize)
            else
                levelThumbnail[it.getLevel()] = it.takeSnapshot(GameView.levelSnapshotIconSize)
            Persistency(this).saveThumbnailOfLevel(this, it)
            logger?.log("Level snapshot taken.")
        }
    }

}