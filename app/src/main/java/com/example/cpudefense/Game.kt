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
    val workInProgress = false  // REMOVE THIS

    companion object Params {
        const val maxLevelAvailable = 32

        // feature toggles:
        const val enableEndlessMode = true

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
        const val maxInternalChipStorage = 4
        const val minAttackerSpeed = 0.01f

        const val levelSnapshotIconSize = 120

        const val SERIES_NORMAL  = 1
        const val SERIES_TURBO   = 2
        const val SERIES_ENDLESS = 3

        val basePrice = mapOf(Chip.ChipUpgrades.REDUCE to 10,
            Chip.ChipUpgrades.SUB to 8, Chip.ChipUpgrades.ACC to 10,
            Chip.ChipUpgrades.SHR to 16, Chip.ChipUpgrades.MEM to 12,
            Chip.ChipUpgrades.CLK to 32, Chip.ChipUpgrades.RES to 20)

        // temperature control:
        const val heatAdjustmentFactor = 1.6f // how many heat is generated per shortened tick
        const val baseTemperature = 17
        const val heatPerDegree = 200
        const val temperatureCooldownFactor = 0.99995
        const val temperatureWarnThreshold = 60
        const val temperatureLimit = 85

        // resistors:
        const val resistorBaseStrength = 10f // ohms
        const val resistorBaseDuration = 160f // ticks

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
    var heroesByMode = hashMapOf<LevelMode, HashMap<Hero.Type, Hero>>(
        LevelMode.BASIC to HashMap<Hero.Type, Hero>(),
        LevelMode.ENDLESS to HashMap<Hero.Type, Hero>(),
    )

    /* coin management */
    enum class LevelMode { BASIC, ENDLESS }
    var purseOfCoins = hashMapOf(
        LevelMode.BASIC to PurseOfCoins(this, LevelMode.BASIC),
        LevelMode.ENDLESS to PurseOfCoins(this, LevelMode.ENDLESS),
    )
    /** the stage that has been selected in the level selector */
    var currentStage: Stage.Identifier = Stage.Identifier()
    /** current stage when it is actually played and the game is running */
    var currentlyActiveStage: Stage? = null

    /* game elements */
    val viewport = Viewport()
    var background: Background? = null
    var intermezzo = Intermezzo(this)
    var marketplace = Marketplace(this)
    val scoreBoard = ScoreBoard(this)
    val speedControlPanel = SpeedControl(this)
    private var currentlyActiveWave: Wave? = null
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

    val coinIconBlue: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.cryptocoin)
    val coinIconRed: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.cryptocoin_red)
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
        /** Begin the current game on a chosen level. Also called when starting a completely
         * new game.
         * @param resetProgress If true, the whole game is started from the first level, and
         * all coins and heroes are cleared. Otherwise, start on the level given in the saved state.
         */
        gameActivity.loadSettings()
        if (!resetProgress) {
            val persistency = Persistency(gameActivity)
            global = persistency.loadGlobalData()
            summaryPerNormalLevel  = persistency.loadLevelSummaries(SERIES_NORMAL)
            summaryPerTurboLevel   = persistency.loadLevelSummaries(SERIES_TURBO)
            summaryPerEndlessLevel = persistency.loadLevelSummaries(SERIES_ENDLESS)
            heroes = persistency.loadHeroes(this, null) // load the upgrades gained so far
            heroesByMode[LevelMode.BASIC] = persistency.loadHeroes(this, LevelMode.BASIC)
            heroesByMode[LevelMode.ENDLESS] = persistency.loadHeroes(this, LevelMode.ENDLESS)
            correctNumberOfCoins()

            // calculate coins
            persistency.loadCoins(this)
            if (purseOfCoins[LevelMode.BASIC]?.initialized == false || workInProgress)
                migrateHeroes()

            additionalCashDelay = heroModifier(Hero.Type.GAIN_CASH).toInt()
            currentStage = state.startingLevel
            intermezzo.prepareLevel(state.startingLevel, true)
        }
        else {
            state.startingLevel = Stage.Identifier(1,1)
            summaryPerNormalLevel = HashMap()
            summaryPerTurboLevel = HashMap()
            setLastPlayedStage(state.startingLevel)
            setMaxPlayedStage(state.startingLevel, resetProgress=true)
            currentStage = state.startingLevel
            intermezzo.prepareLevel(state.startingLevel, true)
        }
        if (background == null)
            background = Background(this)
    }

    fun resumeGame()
    {
        /** function to resume a running game at exactly the point where the app was left. */
        // get historical data of levels completed so far
        val persistency = Persistency(gameActivity)
        summaryPerNormalLevel  = persistency.loadLevelSummaries(SERIES_NORMAL)
        summaryPerTurboLevel   = persistency.loadLevelSummaries(SERIES_TURBO)
        summaryPerEndlessLevel = persistency.loadLevelSummaries(SERIES_ENDLESS)
        // persistency.loadState(this)
        stageData?.let {
            currentStage = it.ident
            currentlyActiveStage = Stage.createStageFromData(this, it)
        }
        currentlyActiveStage?.let {
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
                currentlyActiveStage?.let {
                    currentlyActiveWave = if (it.waves.size > 0) it.waves[0]
                    else it.nextWave()
                    speedControlPanel.resetButtons()
                }
                state.phase = GamePhase.RUNNING
            }
        }
        if (background == null)
            background = Background(this)
        if (!gameActivity.settings.configDisableBackground)
            background?.choose(currentStage)
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

    fun currentHeroes(stage: Stage.Identifier = currentStage): HashMap<Hero.Type, Hero>
    /** @return the set of heroes for the current level, depending on the mode (BASIC or ENDLESS) */
    {
        val heroes = heroesByMode[stage.mode()]
        return heroes ?: HashMap()
    }

    fun currentPurse(stage: Stage.Identifier = currentStage): PurseOfCoins
    {
        return purseOfCoins[stage.mode()] ?: PurseOfCoins(this)
    }

    fun currentCoinBitmap(stage: Stage.Identifier = currentStage): Bitmap
    {
        return when (stage.mode())
        {
            LevelMode.BASIC -> coinIconBlue
            LevelMode.ENDLESS -> coinIconRed
        }
    }


    fun update()
    {
        if (state.phase == GamePhase.RUNNING)
        {
            checkTemperature()
            currentlyActiveStage?.network?.let {
                if (background?.mustBeChanged() == true) {
                    it.backgroundImage = background?.actualImage
                    it.recreateNetworkImage(viewport)
                }
                it.update()
            }
            scoreBoard.update()
            currentlyActiveWave?.update()
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
            currentlyActiveStage?.network?.display(canvas, viewport)
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
                currentlyActiveStage?.network?.let {
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
                currentlyActiveStage?.network?.let {
                    if (processClickOnNodes(it, p0))
                        return true
                }
            }
            else ->
                return false
        }
        return false
    }

    private fun processClickOnNodes(network: Network, p0: MotionEvent): Boolean
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
            SERIES_NORMAL  -> return summaryPerNormalLevel[stage.number]
            SERIES_TURBO   -> return summaryPerTurboLevel[stage.number]
            SERIES_ENDLESS -> return summaryPerEndlessLevel[stage.number]
            else -> return null
        }
    }

    private fun setSummaryOfStage(stage: Stage.Identifier, summary: Stage.Summary?)
    {
        summary?.let {
            when (stage.series) {
                SERIES_NORMAL  -> summaryPerNormalLevel[stage.number] = it
                SERIES_TURBO   -> summaryPerTurboLevel[stage.number] = it
                SERIES_ENDLESS -> summaryPerEndlessLevel[stage.number] = it
                else -> return
            }
        }
    }
    
    private fun startNextWave()
    {
        currentlyActiveWave = currentlyActiveStage?.nextWave()
    }

    fun onEndOfWave()
    {
        currentlyActiveWave = null
        // GlobalScope.launch { startNextWave() }
        startNextWave()
    }

    fun onEndOfStage()
    {
        if (currentlyActiveStage == null)
            return // in this case, the stage has already been left
        takeLevelSnapshot()
        currentlyActiveStage?.let {
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
        currentPurse().addReward(intermezzo.coinsGathered)
        val summaryOfCompletedStage = Stage.Summary(won = true,
            coinsGot = stage.summary.coinsGot + state.coinsInLevel,
            coinsMaxAvailable = stage.summary.coinsMaxAvailable,
            coinsAvailable = stage.summary.coinsMaxAvailable - state.coinsInLevel
        )
        currentStage = stage.data.ident
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
        if (!gameActivity.settings.configDisableBackground)
            background?.choose(level)
        Persistency(gameActivity).saveState(this)

        viewport.setGridSize(nextStage.network.data.gridSizeX, nextStage.network.data.gridSizeY)
        state.phase = GamePhase.RUNNING
        currentlyActiveWave = nextStage.nextWave()
        currentlyActiveStage = nextStage
        takeLevelSnapshot()
    }

    fun removeOneLife()
    {
        if (state.coinsInLevel > 0)
            state.coinsInLevel--
        state.lives--
        if (state.lives == 0)
        {
            takeLevelSnapshot()
            currentlyActiveStage = null
            intermezzo.endOfGame(currentStage, hasWon = false)
        }
    }

    fun quitGame()
    {
        gameActivity.finish()
    }

    fun setLastPlayedStage(identifier: Stage.Identifier)
    /** when completing a level, record the current number in the SharedPrefs.
     * @param identifier number of the level successfully completed */
    {
        val prefs = gameActivity.getSharedPreferences(gameActivity.getString(R.string.pref_filename), Context.MODE_PRIVATE)
        with (prefs.edit())
        {
            putInt("LASTSTAGE", identifier.number)
            putInt("LASTSERIES", identifier.series)
            commit()
        }
    }

    private fun setMaxPlayedStage(identifier: Stage.Identifier, resetProgress: Boolean = false)
            /** when completing a level, record the level as highest completed, but only if the old max level is not higher.
             * @param identifier number of the level successfully completed
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
            if (identifier.isGreaterThan(maxStage))
            {
                putInt("MAXSTAGE", identifier.number)
                putInt("MAXSERIES", identifier.series)
                commit()
            }
            // make advanced series available
            when (identifier.series)
            {
                1 ->
                    if (identifier.number==maxLevelAvailable)
                        putBoolean("TURBO_AVAILABLE", true)
                2 -> {
                    putBoolean("TURBO_AVAILABLE", true)
                    if (identifier.number == maxLevelAvailable)
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
        var extraLives = heroModifier(Hero.Type.ADDITIONAL_LIVES)
        state.currentMaxLives = state.maxLives + extraLives.toInt()
        state.lives = state.currentMaxLives
    }
    private fun calculateStartingCash()
    {
        state.cash = heroModifier(Hero.Type.INCREASE_STARTING_CASH).toInt()
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

    fun actualMaxInternalChipStorage(): Int
    {
        val maxStorage = heroModifier(Hero.Type.ENABLE_MEM_UPGRADE).toInt()
        return if (maxStorage > maxInternalChipStorage) maxInternalChipStorage else maxStorage
    }

    fun heroModifier(type: Hero.Type): Float {
        val hero: Hero? = currentHeroes().get(type)
        return hero?.getStrength() ?: Hero.getStrengthOfType(type, 0)
    }

    private fun takeLevelSnapshot()
    {
        currentlyActiveStage?.let {
            if (it.getSeries() == SERIES_ENDLESS)
                levelThumbnailEndless[it.getLevel()] = it.takeSnapshot(levelSnapshotIconSize)
            else
                levelThumbnail[it.getLevel()] = it.takeSnapshot(levelSnapshotIconSize)
            Persistency(gameActivity).saveThumbnailOfLevel(this, it)
        }
    }

    private fun correctNumberOfCoins()
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
        for (hero in heroes.values)  // these are the 'old' heroes
            sumCoinsSpent += hero.data.coinsSpent
        var theoreticalAmountOfCoins = sumCoinsGot - sumCoinsSpent
        if (theoreticalAmountOfCoins < 0)
            theoreticalAmountOfCoins = 0  // more coins have been spent than earned, that is clearly a bug
        if (global.coinsTotal < theoreticalAmountOfCoins)
        /** we've got a problem here. Coins are missing.
         * Unfortunately, we're unable to determine the exact number, because
         * the extra coins are not taken into account.
         * Let's assume that for every 4 coins got from stage rewards there is one extra coin gathered.
         */
         global.coinsTotal = theoreticalAmountOfCoins + sumCoinsGot / 4
    }
    private fun migrateHeroes()
            /** method to separate heroes and coins between the modes of playing (basic/endless).
             * Called when migrating from 1.33 to 1.34
             */
    {
        /* check whether the sum of coins actually spent on heroes exceeds the theoretically
         available amount. In this case, reset the heroes and refund the coins.
         */
        val coinsActuallySpentOnHeroes = heroes.values.sumOf { it.data.coinsSpent }
        for (mode in LevelMode.values()) // do this for basic and endless
        {
            purseOfCoins[mode]?.let {
                it.calculateInitialContents()
                if (it.contents.totalCoins >= coinsActuallySpentOnHeroes) {
                    heroesByMode[mode] = HashMap(heroes)
                    it.contents.spentCoins= coinsActuallySpentOnHeroes
                }
                else
                {
                    heroesByMode[mode] = hashMapOf() // no heroes
                    it.contents.spentCoins= 0
                }
            }
        }
    }

}