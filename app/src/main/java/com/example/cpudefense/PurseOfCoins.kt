package com.example.cpudefense

import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson

class PurseOfCoins(val game: Game, val levelMode: Game.LevelMode = Game.LevelMode.BASIC)
/** Auxiliary object that holds the current amount of coins for a level mode
 *
 */
{
    val workInProgress = true

    data class Contents (
        /** total coins gathered in the game mode */
        var totalCoins: Int = 0,
        /** coins spent on heroes */
        var spentCoins: Int = 0,
        /** coins got from level rewards */
        var rewardCoins: Int = 0,
        /** coins caught running in the stages */
        var runningCoins: Int = 0,
    )

    /** whether this purse has already been used. Must be migrated otherwise */
    var initialized: Boolean = false

    var contents = Contents()
    val filename = game.resources.getString(R.string.pref_filename_savegames)

    fun readContentsOfPurse()
    {
        val prefs = game.gameActivity.getSharedPreferences(filename, AppCompatActivity.MODE_PRIVATE)
        val json = prefs.getString(levelMode.toString(), "none")
        if (json == "none" || workInProgress)
            calculateInitialContents()
        else {
            contents = Gson().fromJson(json, PurseOfCoins.Contents::class.java)
            initialized = true
        }
    }

    fun saveContentsOfPurse()
    {
        val editor = game.gameActivity.getSharedPreferences(filename, AppCompatActivity.MODE_PRIVATE).edit()
        editor.putString(levelMode.toString(), Gson().toJson(contents))
        editor.apply()
    }

    fun calculateInitialContents()
    /** method for migrating the "old" style of coin-keeping */
    {
        // get number of reward coins
        val sumRewardCoinsInBasicMode = game.summaryPerNormalLevel.values.sumOf { it.coinsGot } +
                game.summaryPerTurboLevel.values.sumOf { it.coinsGot }
        val sumRewardCoinsInEndlessMode = game.summaryPerEndlessLevel.values.sumOf { it.coinsGot }

        // distribute the coins in the game fairly to the modes
        var coinsSpentOnHeroes = game.heroes.values.sumOf { it.data.coinsSpent }
        var theoreticalAmountOfCoins = game.global.coinsTotal + coinsSpentOnHeroes
        var totalRewardCoins = sumRewardCoinsInBasicMode + sumRewardCoinsInEndlessMode
        // the difference between the (theoretical) sum of all coins and those accounted for is the number of walking coins
        var totalRunningCoins = theoreticalAmountOfCoins - totalRewardCoins
        if (totalRunningCoins<0) totalRunningCoins=0 // safety catch, should not be triggered
        // distribute them into the series according to the general percentage
        when (levelMode)
        {
            Game.LevelMode.BASIC -> {
                contents.rewardCoins = sumRewardCoinsInBasicMode
                contents.runningCoins = totalRunningCoins * sumRewardCoinsInBasicMode / totalRewardCoins
                contents.spentCoins = coinsSpentOnHeroes * sumRewardCoinsInBasicMode / totalRewardCoins
            }
            Game.LevelMode.ENDLESS -> {
                contents.rewardCoins = sumRewardCoinsInEndlessMode
                contents.runningCoins = totalRunningCoins * sumRewardCoinsInEndlessMode / totalRewardCoins
                contents.spentCoins = coinsSpentOnHeroes * sumRewardCoinsInEndlessMode / totalRewardCoins
            }
        }
        contents.totalCoins = contents.rewardCoins + contents.runningCoins
    }

    fun availableCoins(): Int
    {
        return contents.totalCoins - contents.spentCoins
    }




}