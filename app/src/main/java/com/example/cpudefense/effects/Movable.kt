package com.example.cpudefense.effects

interface Movable {
    fun moveStart()
    fun moveDone()
    fun setCenter(x: Int, y: Int)
}