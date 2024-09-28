package com.example.cpudefense

import android.content.res.Resources

class PurseOfCoins(val gameMechanics: GameMechanics, private val levelMode: GameMechanics.LevelMode = GameMechanics.LevelMode.BASIC)
/** Auxiliary object that holds the current amount of coins for a level mode
 *
 */
{
    // DO NOT change the names of the variables!
    data class Contents (
        /** total coins gathered in the game mode */
        var totalCoins: Int = 0,
        /** coins spent on heroes */
        var spentCoins: Int = 0,
        /** coins got from level rewards */
        var rewardCoins: Int = 0,
        /** coins caught running in the stages */
        var runningCoins: Int = 0,
        /** coins used to purchase lost lives */
        var coinsSpentOnPurchases: Int = 0
    )

    enum class ExpenditureType { HEROES, LIVES }

    companion object
    {
        fun coinsAsString(number: Int, resources: Resources): String
        {
            when (number) {
                0 -> return resources.getString(R.string.coins_none)
                1 -> return resources.getString(R.string.coins_singular)
                else -> return resources.getString(R.string.coins_plural).format(number)
            }
        }
    }

    /** whether this purse has already been used. Must be migrated otherwise */
    var initialized: Boolean = false

    var contents = Contents()

    fun addReward(amount: Int)
    {
        contents.totalCoins += amount
        contents.rewardCoins += amount
    }

    fun spend(amount: Int, spendFor: ExpenditureType = ExpenditureType.HEROES)
    {
        when (spendFor)
        {
            ExpenditureType.HEROES -> contents.spentCoins += amount
            ExpenditureType.LIVES -> contents.coinsSpentOnPurchases += amount
        }
    }

    fun availableCoins(): Int
    {
        return contents.totalCoins - contents.spentCoins - contents.coinsSpentOnPurchases
    }

    fun canAfford(price: Int): Boolean
    {
        return availableCoins() >= price
    }

    fun calculateInitialContents()
    /** method for migrating the "old" style of coin-keeping. */
    {
        // get number of reward coins
        val sumRewardCoinsInBasicMode = gameMechanics.summaryPerNormalLevel.values.sumOf { it.coinsGot } +
                gameMechanics.summaryPerTurboLevel.values.sumOf { it.coinsGot }
        val sumRewardCoinsInEndlessMode = gameMechanics.summaryPerEndlessLevel.values.sumOf { it.coinsGot }

        // distribute the coins in the game fairly to the modes
        val coinsSpentOnHeroes = gameMechanics.heroes.values.sumOf { it.data.coinsSpent }
        val theoreticalAmountOfCoins = gameMechanics.global.coinsTotal + coinsSpentOnHeroes
        val totalRewardCoins = sumRewardCoinsInBasicMode + sumRewardCoinsInEndlessMode
        // the difference between the (theoretical) sum of all coins and those accounted for is the number of walking coins
        var totalRunningCoins = theoreticalAmountOfCoins - totalRewardCoins
        if (totalRunningCoins<0) totalRunningCoins=0 // safety catch, should not be triggered
        if (totalRewardCoins == 0)
        // special case: the player has not won any coins yet
        {
            contents.rewardCoins = 0
            contents.runningCoins = 0
            contents.spentCoins = 0
        }
        else when (levelMode)   // distribute the coins into the series according to the general percentage
        {
            GameMechanics.LevelMode.BASIC -> {
                contents.rewardCoins = sumRewardCoinsInBasicMode
                contents.runningCoins = totalRunningCoins * sumRewardCoinsInBasicMode / totalRewardCoins
                contents.spentCoins = 0  // initial value, will be set later accordingly
            }
            GameMechanics.LevelMode.ENDLESS -> {
                contents.rewardCoins = sumRewardCoinsInEndlessMode
                contents.runningCoins = totalRunningCoins * sumRewardCoinsInEndlessMode / totalRewardCoins
                contents.spentCoins = 0
            }
        }
        contents.totalCoins = contents.rewardCoins + contents.runningCoins
    }
}