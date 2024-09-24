package com.example.cpudefense

import org.junit.Test

import org.junit.Assert.*

class GameMechanicsTest {
    val gameActivity = GameActivity()
    val gameView = GameView(gameActivity)
    val gameMechanics = GameMechanics()

    @Test
    fun generateHeat() {
        assertEquals(GameMechanics.heatAdjustmentFactor, 1.6f)
        var heat: Int
        heat = gameMechanics.generateHeat(5.0f, 0)
        print(heat)


    }
}