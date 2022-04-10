package com.example.cpudefense.effects

interface Movable {
    fun moveStart(): Unit
    fun moveDone(): Unit
    fun setCenter(x: Int, y: Int): Unit
}