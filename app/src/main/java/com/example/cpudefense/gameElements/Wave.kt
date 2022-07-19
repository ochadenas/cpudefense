package com.example.cpudefense.gameElements

import com.example.cpudefense.Game
import kotlin.random.Random

class Wave(var game: Game, var data: Data)
{
    data class Data (
        var attackerCount: Int,
        var attackerStrength: Int,
        var attackerFreqency: Float,
        var attackerSpeed: Float,
        var coins: Int = 0,
        var representation: Attacker.Representation = Attacker.Representation.UNDEFINED,
        var currentCount: Int
            )

    var ticks = 0

    fun update() {
        if (data.currentCount == 0)
            game.onEndOfWave()
        else if (ticks > 0)
            ticks--
        else {
            ticks = ((2.0f / data.attackerFreqency + Random.nextInt(10))/game.globalSpeedFactor).toInt()
            if (data.coins>0 && Random.nextFloat() > 0.8)
            {
                data.coins--
                game.currentStage?.createNewAttacker(data.attackerStrength, data.attackerSpeed, isCoin = true)
            }
            else if (data.representation == Attacker.Representation.HEX)
                game.currentStage?.createNewAttacker(data.attackerStrength, data.attackerSpeed, representation = Attacker.Representation.HEX)
            else
                game.currentStage?.createNewAttacker(data.attackerStrength, data.attackerSpeed)
            data.currentCount--
        }
    }

    companion object {
        fun createFromData(game: Game, data: Data): Wave
        {
            val wave = Wave(game, data)
            return wave
        }
    }
}