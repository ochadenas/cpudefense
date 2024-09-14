@file:Suppress("NOTHING_TO_INLINE", "ConstPropertyName")

package com.example.cpudefense

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import com.example.cpudefense.gameElements.Chip
import com.example.cpudefense.gameElements.Wave
import com.example.cpudefense.networkmap.Coord
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Suppress("ReplaceWithEnumMap", "ConstPropertyName")
class GameMechanics(val gameActivity: GameActivity) {
    companion object Params {
        const val maxLevelAvailable = 32

        const val makeAllLevelsAvailable = false  // for debugging purposes only. TODO: SET TO FALSE
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
        const val chipTextSize = 20f
        const val computerTextSize = 26f
        const val notificationTextSize = 22f
        const val instructionTextSize = 20f
        const val biographyTextSize = 20f
        const val heroCardNameSize = 18f
        const val heroCardTextSize = 14f
        const val purchaseButtonTextSize = 20f

        const val coinSizeOnScoreboard = 48
        const val coinSizeOnScreen = 16
        const val cardWidth = 220
        const val cardHeight = cardWidth * 1.41
        const val cardPictureSize = cardWidth * 2 / 3
        const val preferredSizeOfLED = 20  // horizontal size of LEDs, can be smaller if there is too little space


        // some adjustable game playing parameters
        const val minimalAmountOfCash = 8
        const val maxLivesPerStage = 3
        const val maxInternalChipStorage = 4
        const val minAttackerSpeed = 0.02f
        const val numberOfHeroesConsideredForLeave = 5
        const val numberOfHeroesToChooseFrom = 3

        const val levelSnapshotIconSize = 120

        const val SERIES_NORMAL  = 1
        const val SERIES_TURBO   = 2
        const val SERIES_ENDLESS = 3

        val basePrice = mapOf(Chip.ChipUpgrades.REDUCE to 10,
            Chip.ChipUpgrades.SUB to 8, Chip.ChipUpgrades.ACC to 10,
            Chip.ChipUpgrades.SHR to 16, Chip.ChipUpgrades.MEM to 12,
            Chip.ChipUpgrades.CLK to 32, Chip.ChipUpgrades.RES to 20)

        // temperature control:
        /** amount of heat that is generated per shortened tick */
        const val heatAdjustmentFactor = 1.6f
        /** temperature in degrees at the start of each level */
        const val baseTemperature = 17
        const val heatPerDegree = 200
        /** heat cool down rate (multiplied at each tick) */
        const val temperatureCooldownFactor = 0.99995
        /** value in degrees at which the display turns yellow */
        const val temperatureWarnThreshold = 60
        /** temperature degrees above this limit may result in loss of CPU life */
        const val temperatureLimit = 85

        // resistors:
        const val resistorBaseStrength = 10f // ohms
        const val resistorBaseDuration = 160f // ticks
        const val resistorMaxDuration = 1600f // ticks

    }

    var defaultSpeedFactor = 0.512f

    data class StateData(
        /** whether the game is running, paused or between levels */
        var phase: GamePhase,
        /** level to begin the next game with */
        var startingLevel: Stage.Identifier,
        /** maximum number of lives */
        var maxLives: Int,
        /** maximum number of lives, taking into account modifiers */
        var currentMaxLives: Int,
        /** current number of lives */
        var lives: Int,
        /** current amount of 'information' currency in bits */
        var cash: Int,
        /** cryptocoins that can be obtained by completing the current level */
        var coinsInLevel: Int = 0,
        /** cryptocoins that have been acquired by collecting moving coins */
        var coinsExtra: Int = 0,
        /** heat value in internal units */
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
    var stageData: Stage.Data? = null
    var summaryPerNormalLevel  = HashMap<Int, Stage.Summary>()
    var summaryPerTurboLevel  = HashMap<Int, Stage.Summary>()
    var summaryPerEndlessLevel = HashMap<Int, Stage.Summary>()
    var levelThumbnail = HashMap<Int, Bitmap?>()  // level snapshots (common for series 1 and 2)
    var levelThumbnailEndless = HashMap<Int, Bitmap?>()  // level snapshots for series 3
    var heroes = HashMap<Hero.Type, Hero>()
    var heroesByMode = hashMapOf(
        LevelMode.BASIC to HashMap<Hero.Type, Hero>(),
        LevelMode.ENDLESS to HashMap<Hero.Type, Hero>(),
    )
    private var currentlyActiveWave: Wave? = null
    var holidays = HashMap<Int, Hero.Holiday>()

    enum class LevelMode { BASIC, ENDLESS }
    /** coin management */
    var purseOfCoins = hashMapOf(
        LevelMode.BASIC to PurseOfCoins(this, LevelMode.BASIC),
        LevelMode.ENDLESS to PurseOfCoins(this, LevelMode.ENDLESS),
    )
    /** the stage that has been selected in the level selector */
    var currentStage: Stage.Identifier = Stage.Identifier()
    /** current stage when it is actually played and the game is running */
    var currentlyActiveStage: Stage? = null

    // other temporary variables
    private var additionalCashDelay = 0
    private var additionalCashTicks: Float = 0.0f

    /** time between frames in ms. Used by wait cycles in the game */
    var timeBetweenFrames: Double = 20.0
    var ticksCount = 0
    var frameCount = 0

    enum class GamePhase { START, RUNNING, INTERMEZZO, MARKETPLACE, PAUSED }
    enum class GameSpeed { NORMAL, MAX }

    fun beginGame(resetProgress: Boolean = false, resumeGame: Boolean = false)
    {
        /** Begins the current game on a chosen level. Also called when starting a completely
         * new game.
         * @param resetProgress If true, the whole game is started from the first level, and
         * all coins and heroes are cleared. Otherwise, start on the level given in the saved state.
         */
        gameActivity.loadSettings()

        if (resetProgress)
        {
            state.startingLevel = Stage.Identifier(1,1)
            summaryPerNormalLevel = HashMap()
            summaryPerTurboLevel = HashMap()
            setLastPlayedStage(state.startingLevel)
            setMaxPlayedStage(state.startingLevel, resetProgress=true)
            currentStage = state.startingLevel
            gameActivity.prepareLevelAtStartOfGame(state.startingLevel)
        }
        else
        {
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
            if (purseOfCoins[LevelMode.BASIC]?.initialized == false)
                migrateHeroes()
            additionalCashDelay = heroModifier(Hero.Type.GAIN_CASH).toInt()

            if (resumeGame)
                resumeGame()
            else
            {
                currentStage = state.startingLevel
                gameActivity.prepareLevelAtStartOfGame(state.startingLevel)
            }
        }
    }

    private fun resumeGame()
    {
        /** function to resume a running game at exactly the point where the app was left. */
        // persistency.loadState(this)
        stageData?.let {
            currentStage = it.ident
            currentlyActiveStage = Stage.createStageFromData(this, gameActivity.gameView, it)
        }
        currentlyActiveStage?.let {
            it.network.validateViewport()
            gameActivity.gameView.viewport.setGridSize(it.sizeX, it.sizeY)
            gameActivity.gameView.background.prepareAtStartOfStage(it.data.ident)
        }
        when (state.phase)
        {
            GamePhase.MARKETPLACE -> {
                gameActivity.gameView.marketplace.nextGameLevel = state.startingLevel
                gameActivity.setGameActivityStatus(GameActivity.GameActivityStatus.BETWEEN_LEVELS)
            }
            GamePhase.INTERMEZZO -> {
                gameActivity.setGameActivityStatus(GameActivity.GameActivityStatus.BETWEEN_LEVELS)
            }
            GamePhase.START -> {
                gameActivity.setGameActivityStatus(GameActivity.GameActivityStatus.BETWEEN_LEVELS)
            }
            else -> {
                currentlyActiveStage?.let {
                    currentlyActiveWave = if (it.waves.size > 0) it.waves[0]
                    else it.nextWave()
                }
                state.phase = GamePhase.RUNNING
            }
        }
    }

    inline fun globalSpeedFactor(): Float
            /** the global speed factor affects the speed of the attackers. Depending on this.
             * also other delays must be adjusted:
             * - cooldown of the chips
             * - cash gain over time
             * - frequency of attacker generation.
             * Actually, this factor is no longer actively used to handle different speeds,
             * and the function always returns the same value.
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

    fun currentHeroesOnLeave(stage: Stage.Identifier, leaveStartsOnLevel: Boolean = false): HashMap<Hero.Type, Hero>
    /**
     * @param leaveStartsOnLevel whether only include the heroes that are starting their leave, or also those that have started before
     * @return all heroes that are on leave during the given stage */
    {
        return currentHeroes().filterValues { it.isOnLeave(stage, leaveStartsOnLevel) } as HashMap<Hero.Type, Hero>
    }

    fun currentPurse(stage: Stage.Identifier = currentStage): PurseOfCoins
    {
        return purseOfCoins[stage.mode()] ?: PurseOfCoins(this)
    }

    fun update()
    {
        if (state.phase == GamePhase.RUNNING)
        {
            checkTemperature()
            currentlyActiveStage?.network?.update()
            gameActivity.gameView.scoreBoard.update()
            currentlyActiveWave?.update()
            gainAdditionalCash()
        }
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
                gameActivity.setGameActivityStatus(GameActivity.GameActivityStatus.BETWEEN_LEVELS)
            }
        }
    }

    private fun onStageCleared(stage: Stage)
    {
        val intermezzo = gameActivity.gameView.intermezzo
        gameActivity.runOnUiThread {
            val toast: Toast = Toast.makeText(gameActivity, gameActivity.resources.getString(R.string.toast_stage_cleared), Toast.LENGTH_SHORT)
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
        currentStage = level
        val nextStage = Stage(this, gameActivity.gameView)
        StageCatalog.createStage(nextStage, level)
        nextStage.calculateDifficulty()
        if (!nextStage.isInitialized())
            return  // something went wrong, possibly trying to create a level that doesn't exist
        nextStage.network.recreateNetworkImage(true)
        gameActivity.setGameActivityStatus(GameActivity.GameActivityStatus.PLAYING)
        calculateLives()
        calculateStartingCash()
        gameActivity.runOnUiThread {
            val toast: Toast = Toast.makeText(
                gameActivity,
                gameActivity.resources.getString(R.string.toast_next_stage).format(nextStage.getLevel()),
                Toast.LENGTH_SHORT
            )
            toast.show()
        }
        state.coinsInLevel = nextStage.calculateRewardCoins(getSummaryOfStage(level))
        state.coinsExtra = 0
        setSummaryOfStage(level, nextStage.summary)
        state.heat = 0.0
        gameActivity.setGameSpeed(GameSpeed.NORMAL)  // reset speed to normal when starting next stage
        Persistency(gameActivity).saveState(this)
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
            gameActivity.gameView.intermezzo.endOfGame(currentStage, hasWon = false)
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
        val extraLives = heroModifier(Hero.Type.ADDITIONAL_LIVES)
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
            gameActivity.gameView.scoreBoard.addCash(1)
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
                val toast: Toast = Toast.makeText(gameActivity, gameActivity.resources.getString(R.string.overheat), Toast.LENGTH_SHORT)
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
        val hero: Hero? = currentHeroes()[type]
        hero?.let {
            if (!it.isOnLeave)
                return it.getStrength()
        }
        return Hero.getStrengthOfType(type, 0) // hero has no effect, return the "level 0" strength
    }

    fun generateHeat(amount: Float, percent: Int = 0): Int
    /** adds an amount of "heat" to the global game temperature, respecting possible modifiers
     * @param percent the part of the heat (in %) that is effectively not applied
     * @return the part of the generated heat that has not been applied
     */
    {
        val factor = 100f - heroModifier(Hero.Type.REDUCE_HEAT)
        val generatedHeat = amount * (factor * heatAdjustmentFactor / 100f)
        state.heat += (generatedHeat * (100-percent)) / 100
        return percent * generatedHeat.toInt() / 100
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