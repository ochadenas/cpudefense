package com.example.cpudefense.effects

interface Fadable {
    fun fadeDone(type: Fader.Type): Unit
    fun setOpacity(opacity: Float): Unit
}