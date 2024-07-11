package com.example.cpudefense.effects

interface Fadable {
    fun fadeDone(type: Fader.Type)
    fun setOpacity(opacity: Float)
}