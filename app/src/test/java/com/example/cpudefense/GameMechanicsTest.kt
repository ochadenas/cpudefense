package com.example.cpudefense

import org.junit.Test

import org.junit.Assert.*

class GameMechanicsTest {
    val gameMechanics = GameMechanics()

    @Test
    fun globalSpeedFactor() {
        assertEquals(0.512f, gameMechanics.globalSpeedFactor())
    }
}