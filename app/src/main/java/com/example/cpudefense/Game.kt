package com.example.cpudefense

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.view.MotionEvent
import android.widget.Toast
import com.example.cpudefense.effects.Background
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.effects.Flipper
import com.example.cpudefense.effects.Mover
import com.example.cpudefense.gameElements.*
import com.example.cpudefense.networkmap.Coord
import com.example.cpudefense.networkmap.Network
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.displayTextCenteredInRect
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random


class Game(val gameActivity: MainGameActivity) {
    companion object Params {
        const val maxLevelAvailable = 29

        // feature toggles:
        val enableEndlessMode = true

        val chipSize = Coord(6,3)
        const val viewportMargin = 10
        const val minScoreBoardHeight = 100
        const val maxScoreBoardHeight = 320
        const val speedControlButtonSize = 48

        // text sizes are given in dp (device independent)
        const val scoreTextSize = 36f
        const val scoreHeaderSize = 18f
        const val chipTextSize = 22f
        const val computerTextSize = 28f
        const val notificationTextSize = 22f
        const val instructionTextSize = 28f
        const val biographyTextSize = 24f
        const val purchaseButtonTextSize = 24f

        const val coinSizeOnScoreboard = 40
        const val coinSizeOnScreen = 25
        const val cardHeight = 256
        const val cardWidth = 220

        const val minimalAmountOfCash = 8
        const val maxLivesPerStage = 3

        const val levelSnapshotIconSize = 120

        const val SERIES_NORMAL  = 1
        const val SERIES_TURBO   = 2
        const val SERIES_ENDLESS = 3

        val basePrice = mapOf(Chip.ChipUpgrades.REDUCE to 10,
            Chip.ChipUpgrades.SUB to 8, Chip.ChipUpgrades.ACC to 10,
            Chip.ChipUpgrades.SHR to 16, Chip.ChipUpgrades.MEM to 12,
            Chip.ChipUpgrades.CLK to 32)

        // temperature control:
        val heatAdjustmentFactor = 1.6f // how many heat is generated per shortened tick
        val baseTemperature = 17
        val heatPerDegree = 200
        val temperatureCooldownFactor = 0.99995
        val temperatureWarnThreshold = 60
        val temperatureLimit = 85
    }

    var defaultSpeedFactor = 0.512f

    data class StateData(
        var phase: GamePhase,       // whether the game is running, paused or between levels
        var startingLevel: Stage.Identifier,     // level to begin the next game with
        var maxLives: Int,          // maximum number of lives
        var currentMaxLives: Int,   // maximum number of lives, taking into account modifiers
        var lives: Int,             // current number of lives
        var cash: Int,              // current amount of 'information' currency in bits
        var coinsInLevel: Int = 0,  // cryptocoins that can be obtained by completing the current level
        var coinsExtra: Int = 0,     // cryptocoins that have been acquired by collecting moving coins
        var heat: Double = 0.0
        )
    var state = StateData(
        phase = GamePhase.START,
        startingLevel = Stage.Identifier(),
        maxLives = maxLivesPerStage,
        currentMaxLives = maxLivesPerStage,
        lives = 0,
        cash = minimalAmountOfCash
    )

    data class GlobalData(
        var speed: GameSpeed = GameSpeed.NORMAL,
        var coinsTotal: Int = 0
    )
    var global = GlobalData()

    val resources: Resources = (gameActivity as Activity).resources

    var stageData: Stage.Data? = null
    var summaryPerNormalLevel  = HashMap<Int, Stage.Summary>()
    var summaryPerTurboLevel  = HashMap<Int, Stage.Summary>()
    var summaryPerEndlessLevel = HashMap<Int, Stage.Summary>()
    var levelThumbnail = HashMap<Int, Bitmap?>()  // level snapshots (common for series 1 and 2)
    var levelThumbnailEndless = HashMap<Int, Bitmap?>()  // level snapshots for series 3
    var heroes = HashMap<Hero.Type, Hero>()

    /* game elements */
    val viewport = Viewport()
    var background: Background? = null
    var intermezzo = Intermezzo(this)
    var marketplace = Marketplace(this)
    val scoreBoard = ScoreBoard(this)
    val speedControlPanel = SpeedControl(this)
    var currentStage: Stage? = null
    private var currentWave: Wave? = null
    var movers = CopyOnWriteArrayList<Mover>() // list of all mover objects that are created for game elements
    var faders = CopyOnWriteArrayList<Fader>() // idem for faders
    var flippers = CopyOnWriteArrayList<Flipper>() // idem for flippers
    val notification = ProgressNotification(this)

    /* other temporary variables */
    private var additionalCashDelay = 0
    private var additionalCashTicks: Float = 0.0f

    var timeBetweenFrames: Double = 20.0 /* in ms. Used by wait cycles in the game */
    var ticksCount = 0
    var frameCount = 0

    enum class GamePhase { START, RUNNING, INTERMEZZO, MARKETPLACE, PAUSED }
    enum class GameSpeed { NORMAL, MAX }

    val coinIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.cryptocoin)
    val cpuImage: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.cpu)
    val playIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.play_active)
    val pauseIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.pause_active)
    val fastIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.fast_active)
    val returnIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.cancel_active)
    val moveLockIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.move_lock)
    val moveUnlockIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.move_unlock)
    val hpBackgroundBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.hp_key)

    fun beginGame(resetProgress: Boolean = false)
    {
        gameActivity.loadSettings()
        if (!resetProgress) {
            val persistency = Persistency(gameActivity)
            global = persistency.loadGlobalData()
            summaryPerNormalLevel  = persistency.loadLevelSummaries(Game.SERIES_NORMAL)
            summaryPerTurboLevel   = persistency.loadLevelSummaries(Game.SERIES_TURBO)
            summaryPerEndlessLevel = persistency.loadLevelSummaries(Game.SERIES_ENDLESS)
            heroes = persistency.loadHeroes(this) // load the upgrades gained so far
            correctNumberOfCoins()
            additionalCashDelay = heroes[Hero.Type.GAIN_CASH]?.getStrength()?.toInt() ?: 0
            intermezzo.prepareLevel(state.startingLevel, true)
        }
        else {
            state.startingLevel = Stage.Identifier(1,1)
            summaryPerNormalLevel = HashMap()
            summaryPerTurboLevel = HashMap()
            setLastPlayedStage(state.startingLevel)
            setMaxPlayedStage(state.startingLevel, resetProgress=true)
            intermezzo.prepareLevel(state.startingLevel, true)
        }
        if (background == null)
            background = Background(this)
    }

    fun resumeGame()
    {
        // get historical data of levels completed so far
        val persistency = Persistency(gameActivity)
        summaryPerNormalLevel  = persistency.loadLevelSummaries(Game.SERIES_NORMAL)
        summaryPerTurboLevel   = persistency.loadLevelSummaries(Game.SERIES_TURBO)
        summaryPerEndlessLevel = persistency.loadLevelSummaries(Game.SERIES_ENDLESS)
        // persistency.loadState(this)
        stageData?.let {
            currentStage = Stage.createStageFromData(this, it)
        }
        currentStage?.let {
            it.network.validateViewport()
            viewport.setGridSize(it.sizeX, it.sizeY)
        }
        when (state.phase)
        {
            GamePhase.MARKETPLACE -> {
                marketplace.nextGameLevel = state.startingLevel
                gameActivity.setGameActivityStatus(MainGameActivity.GameActivityStatus.BETWEEN_LEVELS)
            }
            GamePhase.INTERMEZZO -> {
                gameActivity.setGameActivityStatus(MainGameActivity.GameActivityStatus.BETWEEN_LEVELS)
            }
            GamePhase.START -> {
                gameActivity.setGameActivityStatus(MainGameActivity.GameActivityStatus.BETWEEN_LEVELS)
            }
            else -> {
                currentStage?.let {
                    currentWave = if (it.waves.size > 0) it.waves[0]
                    else it.nextWave()
                    speedControlPanel.resetButtons()
                }
                state.phase = GamePhase.RUNNING
            }
        }
        if (background == null)
            background = Background(this)
        if (!gameActivity.settings.configDisableBackground)
            background?.choose(currentStage?.data?.ident)
        background?.state = Background.BackgroundState.UNINITIALIZED
    }

    inline fun globalSpeedFactor(): Float
            /** the global speed factor affects the speed of the attackers. Depending on this.
             * also other delays must be adjusted:
             * - cooldown of the chips
             * - cash gain over time
             * - frequency of attacker generation
             */
    {
        if (global.speed == GameSpeed.MAX)
            return defaultSpeedFactor
        else
            return defaultSpeedFactor
    }

    fun update()
    {
        if (state.phase == GamePhase.RUNNING)
        {
            checkTemperature()
            currentStage?.network?.let {
                if (background?.mustBeChanged() == true) {
                    it.backgroundImage = background?.actualImage
                    it.recreateNetworkImage(viewport)
                }
                it.update()
            }
            scoreBoard.update()
            currentWave?.update()
            gainAdditionalCash()
        }
    }

    fun updateEffects()
            /**  execute all movers and faders */
    {
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
        for (m in flippers)
        {
            if (m?.type == Flipper.Type.NONE)
                flippers.remove(m)
            else
                m?.update()
        }
    }

    fun display(canvas: Canvas)
    {
        if (state.phase == GamePhase.RUNNING || state.phase == GamePhase.PAUSED)
        {
            currentStage?.network?.display(canvas, viewport)
            scoreBoard.display(canvas, viewport)
            speedControlPanel.display(canvas)
        }
        if (state.phase == GamePhase.PAUSED)
        {
            val paint = Paint()
            paint.color = Color.WHITE
            paint.textSize = 72f
            paint.typeface = Typeface.DEFAULT_BOLD
            viewport.let {
                val rect = Rect(0, 0, it.viewportWidth, it.viewportHeight)
                rect.displayTextCenteredInRect(canvas, gameActivity.getString(R.string.game_paused), paint)
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
                if (speedControlPanel.onDown(p0))
                    return true
                currentStage?.network?.let {
                    if (processClickOnNodes(it, p0))
                        return true
                    for (obj in it.vehicles)
                        if ((obj as Attacker).onDown(p0))
                            return true
                }
                return false
            }
            GamePhase.INTERMEZZO ->
                return intermezzo.onDown(p0)
            GamePhase.MARKETPLACE ->
                return marketplace.onDown(p0)
            GamePhase.PAUSED ->
            {
                if (speedControlPanel.onDown(p0))
                    return true
                currentStage?.network?.let {
                    if (processClickOnNodes(it, p0))
                        return true
                }
            }
            else ->
                return false
        }
        return false
    }

    fun processClickOnNodes(network: Network, p0: MotionEvent): Boolean
    {
        /* first, check if the click is inside one of the upgrade boxes
        * of _any_ node */
        for (obj in network.nodes.values) {
            val chip = obj as Chip
            for (upgrade in chip.upgradePossibilities)
                if (upgrade.onDown(p0)) {
                    chip.upgradePossibilities.clear()
                    return true
                }
            /* if we come here, then the click was not on an update. */
            if (chip.actualRect?.contains(p0.x.toInt(), p0.y.toInt()) == false)
                chip.upgradePossibilities.clear()  // clear update boxes of other chips
        }
        /* check the nodes themselves */
        for (obj in network.nodes.values)
            if (obj.onDown(p0))
                return true
        return false
    }

    fun getSummaryOfStage(stage: Stage.Identifier): Stage.Summary?
    {
        when (stage.series)
        {
            Game.SERIES_NORMAL  -> return summaryPerNormalLevel[stage.number]
            Game.SERIES_TURBO   -> return summaryPerTurboLevel[stage.number]
            Game.SERIES_ENDLESS -> return summaryPerEndlessLevel[stage.number]
            else -> return null
        }
    }

    fun setSummaryOfStage(stage: Stage.Identifier, summary: Stage.Summary?)
    {
        summary?.let {
            when (stage.series) {
                Game.SERIES_NORMAL  -> summaryPerNormalLevel[stage.number] = it
                Game.SERIES_TURBO   -> summaryPerTurboLevel[stage.number] = it
                Game.SERIES_ENDLESS -> summaryPerEndlessLevel[stage.number] = it
                else -> return
            }
        }
    }
    
    private fun startNextWave()
    {
        currentWave = currentStage?.nextWave()
    }

    fun onEndOfWave()
    {
        currentWave = null
        // GlobalScope.launch { startNextWave() }
        startNextWave()
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
                Persistency(gameActivity).saveState(this)
                gameActivity.setGameActivityStatus(MainGameActivity.GameActivityStatus.BETWEEN_LEVELS)
            }
        }
    }

    private fun onStageCleared(stage: Stage)
    {
        gameActivity.runOnUiThread {
            val toast: Toast = Toast.makeText(gameActivity, resources.getString(R.string.toast_stage_cleared), Toast.LENGTH_SHORT)
            toast.show()
        }
        intermezzo.coinsGathered = state.coinsExtra + state.coinsInLevel
        global.coinsTotal += intermezzo.coinsGathered
        val summaryOfCompletedStage = Stage.Summary(won = true,
            coinsGot = stage.summary.coinsGot + state.coinsInLevel,
            coinsMaxAvailable = stage.summary.coinsMaxAvailable,
            coinsAvailable = stage.summary.coinsMaxAvailable - state.coinsInLevel
        )
        val currentStage = stage.data.ident
        val nextStage = currentStage.next()
        setSummaryOfStage(currentStage, summaryOfCompletedStage)
        setMaxPlayedStage(currentStage)
        if (stage.data.type == Stage.Type.FINAL)
        {
            intermezzo.endOfGame(currentStage, hasWon = true)
        }
        else {
            // make next level available. Create an empty one if necessary
            setSummaryOfStage(nextStage, getSummaryOfStage(nextStage) ?: Stage.Summary())
            intermezzo.prepareLevel(nextStage, false)
        }
    }

    fun startNextStage(level: Stage.Identifier)
    {
        val nextStage = Stage(this)
        gameActivity.setGameActivityStatus(MainGameActivity.GameActivityStatus.PLAYING)
        calculateLives()
        calculateStartingCash()
        StageCatalog.createStage(nextStage, level)
        nextStage.calculateDifficulty()
        if (!nextStage.isInitialized())
            return  // something went wrong, possibly trying to create a level that doesn't exist
        gameActivity.runOnUiThread {
            val toast: Toast = Toast.makeText(
                gameActivity,
                resources.getString(R.string.toast_next_stage).format(nextStage.getLevel()),
                Toast.LENGTH_SHORT
            )
            toast.show()
        }
        state.coinsInLevel = nextStage.calculateRewardCoins(getSummaryOfStage(level))
        state.coinsExtra = 0
        setSummaryOfStage(level, nextStage.summary)
        state.heat = 0.0
        gameActivity.setGameSpeed(GameSpeed.NORMAL)  // reset speed to normal when starting next stage
        speedControlPanel.resetButtons()
        scoreBoard.recreateBitmap()
        viewport.reset()
        Persistency(gameActivity).saveState(this)

        viewport.setGridSize(nextStage.network.data.gridSizeX, nextStage.network.data.gridSizeY)
        state.phase = GamePhase.RUNNING
        currentWave = nextStage.nextWave()
        currentStage = nextStage
        if (!gameActivity.settings.configDisableBackground)
            background?.choose(level)
        takeLevelSnapshot()
    }

    fun removeOneLife()
    {
        if (state.coinsInLevel > 0)
            state.coinsInLevel--
        state.lives--
        if (state.lives == 0)
        {
            val lastLevel = currentStage?.data?.ident ?: Stage.Identifier(1,0)
            takeLevelSnapshot()
            currentStage = null
            intermezzo.endOfGame(lastLevel, hasWon = false)
        }
    }

    fun quitGame()
    {
        gameActivity.finish()
    }

    fun setLastPlayedStage(currentStage: Stage.Identifier)
    /** when completing a level, record the current number in the SharedPrefs.
     * @param currentStage number of the level successfully completed */
    {
        val prefs = gameActivity.getSharedPreferences(gameActivity.getString(R.string.pref_filename), Context.MODE_PRIVATE)
        with (prefs.edit())
        {
            putInt("LASTSTAGE", currentStage.number)
            putInt("LASTSERIES", currentStage.series)
            commit()
        }
    }

    private fun setMaxPlayedStage(currentStage: Stage.Identifier, resetProgress: Boolean = false)
            /** when completing a level, record the level as highest completed, but only if the old max level is not higher.
             * @param currentStage number of the level successfully completed
             * @param resetProgress If true, forces resetting the max stage to the given currentStage
             * */
    {
        val prefs = gameActivity.getSharedPreferences(gameActivity.getString(R.string.pref_filename), Context.MODE_PRIVATE)
        val maxStage = Stage.Identifier(prefs.getInt("MAXSERIES", 1), prefs.getInt("MAXSTAGE", 0))
        with (prefs.edit())
        {
            if (resetProgress)
            {
                putInt("MAXSTAGE", 0)
                putInt("MAXSERIES", 1)
                putBoolean("TURBO_AVAILABLE", false)
                putBoolean("ENDLESS_AVAILABLE", false)
                commit()
            }
            if (currentStage.isGreaterThan(maxStage))
            {
                putInt("MAXSTAGE", currentStage.number)
                putInt("MAXSERIES", currentStage.series)
                commit()
            }
            // make advanced series available
            when (currentStage.series)
            {
                1 ->
                    if (currentStage.number==maxLevelAvailable)
                        putBoolean("TURBO_AVAILABLE", true)
                2 -> {
                    putBoolean("TURBO_AVAILABLE", true)
                    if (currentStage.number == maxLevelAvailable)
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
    private fun calculateLives()
    {
        val extraLives = heroes[Hero.Type.ADDITIONAL_LIVES]?.getStrength()
        state.currentMaxLives = state.maxLives + (extraLives ?: 0f).toInt()
        state.lives = state.currentMaxLives
    }
    private fun calculateStartingCash()
    {
        val cash = heroes[Hero.Type.INCREASE_STARTING_CASH]?.getStrength()?.toInt()
        state.cash = cash ?: minimalAmountOfCash
    }

    private fun gainAdditionalCash()
    /** increases the amount of cash in regular intervals */
    {
        if (additionalCashDelay == 0)
            return
        additionalCashTicks -= globalSpeedFactor()
        if (additionalCashTicks<0) {
            scoreBoard.addCash(1)
            additionalCashTicks = additionalCashDelay.toFloat()
        }
    }

    private fun checkTemperature()
    {
        if (state.heat == 0.0)
            return
        state.heat *= temperatureCooldownFactor  // reduce heat. TODO: consider global speed factor
        val overheat = state.heat - (temperatureLimit- baseTemperature)*heatPerDegree
        if ((overheat > 0) && (Random.nextDouble() < (overheat * 0.00001)))  // chance of damaging the CPU
        {
            gameActivity.runOnUiThread {
                val toast: Toast = Toast.makeText(gameActivity, resources.getString(R.string.overheat), Toast.LENGTH_SHORT)
                toast.show()
            }
            state.heat *= 0.6f
            removeOneLife()
        }
    }

    private fun takeLevelSnapshot()
    {
        currentStage?.let {
            if (it.getSeries() == Game.SERIES_ENDLESS)
                levelThumbnailEndless[it.getLevel()] = it.takeSnapshot(levelSnapshotIconSize)
            else
                levelThumbnail[it.getLevel()] = it.takeSnapshot(levelSnapshotIconSize)
            Persistency(gameActivity).saveThumbnailOfLevel(this, it)
        }
    }

    fun correctNumberOfCoins()
            /** the purpose of this method is to verify if the total number of coins spent
             * corresponds to the coins got, and to correct this number if coins have been "lost"
             * due to corrupt save files etc.
             */
    {
        var sumCoinsGot = 0
        for (summary in summaryPerNormalLevel.values)
            sumCoinsGot += summary.coinsGot
        for (summary in summaryPerTurboLevel.values)
            sumCoinsGot += summary.coinsGot
        for (summary in summaryPerEndlessLevel.values)
            sumCoinsGot += summary.coinsGot
        var sumCoinsSpent = 0
        for (hero in heroes.values)
            sumCoinsSpent += hero.data.coinsSpent
        val theoreticalAmountOfCoins = sumCoinsGot - sumCoinsSpent
        if (global.coinsTotal < theoreticalAmountOfCoins)
        /** we've got a problem here. Coins are missing.
         * Unfortunately, we're unable to determine the exact number, because
         * the extra coins are not taken into account.
         * Let's assume that for every 4 coins "regularly" got there is one extra coin gahered.
         */
           global.coinsTotal = theoreticalAmountOfCoins + sumCoinsGot / 4
    }

}