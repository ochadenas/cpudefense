package com.example.cpudefense.gameElements

import com.example.cpudefense.GameMechanics
import com.example.cpudefense.Hero
import kotlin.random.Random

class Wave(var gameMechanics: GameMechanics, var data: Data)
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
            gameMechanics.onEndOfWave()
        else if (data.ticksUntilNextAttacker > 0)
            data.ticksUntilNextAttacker -= gameMechanics.globalSpeedFactor()/gameMechanics.defaultSpeedFactor
        else {
            val frequency = data.attackerFrequency * gameMechanics.heroModifier(Hero.Type.DECREASE_ATT_FREQ)
            data.ticksUntilNextAttacker = 6.0 / frequency
            if (data.coins>0 && Random.nextFloat() > 0.8)
            {
                data.coins--
                gameMechanics.currentlyActiveStage?.createNewAttacker(data.attackerStrength, data.attackerSpeed, isCoin = true)
            }
            else if (data.representation == Attacker.Representation.HEX)
                gameMechanics.currentlyActiveStage?.createNewAttacker(data.attackerStrength, data.attackerSpeed, representation = Attacker.Representation.HEX)
            else
                gameMechanics.currentlyActiveStage?.createNewAttacker(data.attackerStrength, data.attackerSpeed, representation = Attacker.Representation.UNDEFINED)
            data.currentCount--
        }
    }

    companion object {
        fun createFromData(gameMechanics: GameMechanics, data: Data): Wave
        {
            val wave = Wave(gameMechanics, data)
            return wave
        }
    }
}