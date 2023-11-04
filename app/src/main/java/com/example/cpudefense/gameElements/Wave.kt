package com.example.cpudefense.gameElements

import com.example.cpudefense.Game
import com.example.cpudefense.Hero
import kotlin.random.Random

class Wave(var game: Game, var data: Data)
{
    data class Data (
        var attackerCount: Int,
        var attackerStrength: Int,
        var attackerFrequency: Float,
        var attackerSpeed: Float,
        var coins: Int = 0,
        var representation: Attacker.Representation = Attacker.Representation.UNDEFINED,
        var currentCount: Int,
        var ticksUntilNextAttacker: Double // one tick equals approx. 50 ms.
            )

    fun update() {
        if (data.currentCount == 0)
            game.onEndOfWave()
        else if (data.ticksUntilNextAttacker > 0)
            data.ticksUntilNextAttacker -= game.globalSpeedFactor()/game.defaultSpeedFactor
        else {
            val frequency = data.attackerFrequency * (game.heroes[Hero.Type.DECREASE_ATT_FREQ]?.getStrength() ?: 1f)
            data.ticksUntilNextAttacker = 4.0 / frequency
            if (data.coins>0 && Random.nextFloat() > 0.8)
            {
                data.coins--
                game.currentStage?.createNewAttacker(data.attackerStrength, data.attackerSpeed, isCoin = true)
            }
            else if (data.representation == Attacker.Representation.HEX)
                game.currentStage?.createNewAttacker(data.attackerStrength, data.attackerSpeed, representation = Attacker.Representation.HEX)
            else
                game.currentStage?.createNewAttacker(data.attackerStrength, data.attackerSpeed, representation = Attacker.Representation.UNDEFINED)
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