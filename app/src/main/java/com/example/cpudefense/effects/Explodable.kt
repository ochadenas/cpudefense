package com.example.cpudefense.effects
interface Explodable {
    fun remove()
    fun getPositionOnScreen(): Pair<Int, Int>

    val explosionColour: Int?
}