@file:Suppress("NOTHING_TO_INLINE", "ConstPropertyName")

package com.example.cpudefense

import com.example.cpudefense.gameElements.Chip
import com.example.cpudefense.gameElements.Wave
import com.example.cpudefense.utils.Logger
import java.util.Calendar
import kotlin.random.Random

class TemperatureDamageException: Exception("CPU damage by heat")

class CpuReached: Exception("CPU got hit, removed one life point")

@Suppress("ReplaceWithEnumMap", "ConstPropertyName")
class GameMechanics {
    companion object Params {
        /** how many stages there are in NORMAL and TURBO. Must be changed when adding new stages. */
        const val maxLevelAvailable = 32

        // debug options
        /** for debugging purposes only. MUST BE SET TO FALSE */
        const val makeAllLevelsAvailable = false
        /** for debugging purposes only. MUST BE SET TO FALSE */
        const val resetHeroHolidays = false
        /** for debugging purposes only. MUST BE SET TO FALSE */
        const val forceHeroMigration = false
        /** for debugging purposes only. MUST BE SET TO FALSE */
        const val allowLivesPurchaseInAllStages = false
        /** for debugging purposes only. MUST BE SET TO FALSE */
        const val enableLogging = true
        // end of debug options

        /** minimum log level (if logging is enabled) */
        val logLevel = Logger.Level.MESSAGE

        /** level that shows an Easter egg */
        const val specialLevelNumber = 8

        // some adjustable game playing parameters
        const val minimalAmountOfCash = 8
        const val maxLivesPerStage = 3
        const val maxInternalChipStorage = 4
        const val minAttackerSpeed = 0.02f
        const val numberOfHeroesConsideredForLeave = 5
        const val numberOfHeroesToChooseFrom = 3
        const val defaultRewardCoins = 3

        const val SERIES_NORMAL  = 1
        const val SERIES_TURBO   = 2
        const val SERIES_ENDLESS = 3

        /** price in bits for the purchase of the first level of a chip,
         * may be modified by heroes' effects. */
        val basePrice = mapOf(Chip.ChipUpgrades.REDUCE to 10,
            Chip.ChipUpgrades.SUB to 8, Chip.ChipUpgrades.ACC to 10,
            Chip.ChipUpgrades.SHR to 16, Chip.ChipUpgrades.MEM to 12,
            Chip.ChipUpgrades.CLK to 32, Chip.ChipUpgrades.RES to 8)

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

        enum class Season { DEFAULT, EASTER, CHRISTMAS }

        fun specialSeason(): Season
                /** for "easter egg" purposes. Determine whether - at the time of playing - we are in
                 * a special season.
                 */
        {
            when (Calendar.getInstance().get(Calendar.MONTH))
            {
                0 -> return Season.CHRISTMAS  // January
                2 -> return Season.EASTER
                3 -> return Season.EASTER
                10 -> return Season.DEFAULT
                11 -> return Season.CHRISTMAS
                else -> return Season.DEFAULT
            }
        }

        fun specialLevel(stageIdent: Stage.Identifier): Season
        {
            if (stageIdent.mode() == LevelMode.BASIC && stageIdent.number == specialLevelNumber)
                return specialSeason()
            else
                return Season.DEFAULT
        }

    }

    var defaultSpeedFactor = 0.512f

    data class StateData(
        /** whether the game is running, paused or between levels */
        var phase: GamePhase,
        /** speed setting of the current level */
        var speed: GameSpeed,
        /** maximum number of lives */
        var maxLives: Int,
        /** maximum number of lives, taking into account modifiers */
        var currentMaxLives: Int,
        /** current number of lives */
        var lives: Int,
        /** number of lives that have already been purchased in this stage */
        var livesRestored: Int = 0,
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
        speed = GameSpeed.NORMAL,
        maxLives = maxLivesPerStage,
        currentMaxLives = maxLivesPerStage,
        lives = 0,
        cash = minimalAmountOfCash
    )

    data class GlobalData(
        var coinsTotal: Int = 0  // deprecated. Use PurseOfCoins instead
    )

    var global = GlobalData()
    var stageData: Stage.Data? = null
    var summaryPerNormalLevel  = HashMap<Int, Stage.Summary>()
    var summaryPerTurboLevel  = HashMap<Int, Stage.Summary>()
    var summaryPerEndlessLevel = HashMap<Int, Stage.Summary>()
    var heroes = HashMap<Hero.Type, Hero>()
    var heroesByMode = hashMapOf(
            LevelMode.BASIC to HashMap(),
            LevelMode.ENDLESS to HashMap<Hero.Type, Hero>(),
    )
    var currentlyActiveWave: Wave? = null
    var holidays = HashMap<Int, Hero.Holiday>()

    /** the two different kind of series: BASIC (which means NORMAL and TURBO) and ENDLESS */
    enum class LevelMode { BASIC, ENDLESS }
    /** coin management */
    var purseOfCoins = hashMapOf(
        LevelMode.BASIC to PurseOfCoins(this, LevelMode.BASIC),
        LevelMode.ENDLESS to PurseOfCoins(this, LevelMode.ENDLESS),
    )
    /** the stage that has been selected in the level selector */
    var currentStageIdent: Stage.Identifier = Stage.Identifier()
    /** current stage when it is actually played and the game is running */
    var currentlyActiveStage: Stage? = null

    /** counter for information gain over time */
    private var additionalCashTicks: Float = 0.0f

    /** time between frames in ms. Used by wait cycles in the game */
    var timeBetweenFrames: Double = 20.0
    var ticksCount = 0
    var frameCount = 0

    enum class GamePhase { START, RUNNING, INTERMEZZO, MARKETPLACE, PAUSED }
    enum class GameSpeed { NORMAL, MAX }

    fun deleteProgressOfSeries(mode: LevelMode)
            /** initializes the data structures for level summaries, heroes and coin purse with empty values.
             * @param mode selects whether only the ENDLESS series gets deleted, or all series (including ENDLESS).
             */
    {
        // this gets deleted in any case:
        summaryPerEndlessLevel = HashMap()
        heroesByMode[LevelMode.ENDLESS] = HashMap()
        purseOfCoins[LevelMode.ENDLESS] = PurseOfCoins(this, LevelMode.ENDLESS)
        // additionally, the BASIC series if requested:
        if (mode==LevelMode.BASIC) {
            summaryPerTurboLevel = HashMap()
            summaryPerNormalLevel = HashMap()
            heroesByMode[LevelMode.BASIC] = HashMap()
            purseOfCoins[LevelMode.BASIC] = PurseOfCoins(this, LevelMode.BASIC)
        }
    }

    inline fun globalSpeedFactor(): Float
            /** the global speed factor affects the speed of the attackers. Depending on this.
             * also other delays must be adjusted:
             * - cooldown of the chips
             * - cash gain over time
             * - frequency of attacker generation.
             *
             * Actually, this factor is no longer actively used to handle different speeds,
             * and the function always returns the same value.
             */
    {
        if (state.speed == GameSpeed.MAX)
            return defaultSpeedFactor
        else
            return defaultSpeedFactor
    }

    fun currentHeroes(stage: Stage.Identifier = currentStageIdent): HashMap<Hero.Type, Hero>
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

    fun currentPurse(stage: Stage.Identifier = currentStageIdent): PurseOfCoins
    {
        return purseOfCoins[stage.mode()] ?: PurseOfCoins(this)
    }

    fun update()
    {
        if (state.phase == GamePhase.RUNNING)
        {
            checkTemperature()
            currentlyActiveStage?.network?.update()
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

    fun setSummaryOfStage(stage: Stage.Identifier, summary: Stage.Summary?)
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

    fun removeOneLife(): Int
    /**
     * Remove one of the player's lives.
     * @return the number of lives still left.
     */
    {
        if (state.coinsInLevel > 0)
            state.coinsInLevel--
        state.lives--
        if (state.lives == 0)
            currentlyActiveStage = null
        return state.lives
    }

    fun restoreOneLife(): Int
    /** restores one of the lives, purchasing it with coins.
     * @return the new number of lives
      */
    {
        val price = costOfLife()
        if (currentPurse().canAfford(price) && state.lives<state.currentMaxLives)
        {
            currentPurse().spend(price, PurseOfCoins.ExpenditureType.LIVES)
            state.livesRestored++
            state.lives++
        }
        return state.lives
    }


    fun calculateLives()
    /** calculates the number of lives at the beginning of the stage */
    {
        val extraLives = heroModifier(Hero.Type.ADDITIONAL_LIVES)
        state.currentMaxLives = state.maxLives + extraLives.toInt()
        state.lives = state.currentMaxLives
        state.livesRestored = 0
    }

    fun costOfLife(): Int
            /** Calculates the cost to restore one life lost in the game.
             * @return Number of coins required
             */
    {
        if (currentStageIdent.series != SERIES_ENDLESS && !allowLivesPurchaseInAllStages)
            return 0
        return 1 + state.livesRestored + (currentStageIdent.number / 32)
    }

    fun calculateStartingCash()
    /** calculates the information available at the beginning of a stage */
    {
        state.cash = heroModifier(Hero.Type.INCREASE_STARTING_CASH).toInt()
    }

    fun calculateCoins(nextStage: Stage)
    /** calculates the amount of coins available at the beginning of a stage */
    {
        state.coinsInLevel = nextStage.calculateRewardCoins(getSummaryOfStage(nextStage.data.ident))
        state.coinsExtra = 0
    }

    private fun gainAdditionalCash()
    /** increases the amount of cash in regular intervals */
    {
        val additionalCashDelay = heroModifier(Hero.Type.GAIN_CASH)
        if (additionalCashDelay.toInt() == 0)
            return
        additionalCashTicks -= globalSpeedFactor()
        if (additionalCashTicks<0) {
            state.cash += 1
            additionalCashTicks = additionalCashDelay
        }
    }

    @Throws(TemperatureDamageException::class)
    private fun checkTemperature()
    /** check for exceeding temperature. Create an exception if the heat damage is triggered */
    {
        if (state.heat == 0.0)
            return
        state.heat *= temperatureCooldownFactor  // reduce heat. TODO: consider global speed factor
        val overheat = state.heat - (temperatureLimit- baseTemperature)*heatPerDegree
        if ((overheat > 0) && (Random.nextDouble() < (overheat * 0.00001)))  // chance of damaging the CPU
        {
            state.heat *= 0.6f
            throw TemperatureDamageException()
        }
    }

    fun actualMaxInternalChipStorage(): Int
    /** @return the maximal number of internal slots in MEM chips, taking hero effects into account */
    {
        val maxStorage = heroModifier(Hero.Type.ENABLE_MEM_UPGRADE).toInt()
        return if (maxStorage > maxInternalChipStorage) maxInternalChipStorage else maxStorage
    }

    fun heroModifier(type: Hero.Type): Float
    /** gives the hero modifier value for any given type, even if the hero is not present. */
    {
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
        val heat = amount * heatAdjustmentFactor  // heat in in-game units
        val convertedHeat = heat * percent / 100f // part that is converted into cash
        val factor = 100f - heroModifier(Hero.Type.REDUCE_HEAT) // heat reduction in %
        state.heat += (heat-convertedHeat) * factor / 100f
        return convertedHeat.toInt()
    }

    private fun correctNumberOfCoins()
            /** the purpose of this method is to verify if the total number of coins spent
             * corresponds to the coins got, and to correct this number if coins have been "lost"
             * due to corrupt save files etc.
             *
             * Removed in version 1.44 due to new heroes / purseOfCoins structure.
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

    fun migrateHeroes()
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
                    it.contents.spentCoins = coinsActuallySpentOnHeroes
                }
                else
                {
                    heroesByMode[mode] = hashMapOf() // no heroes
                    it.contents.spentCoins = 0
                }
            }
        }
    }

}