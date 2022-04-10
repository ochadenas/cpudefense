package com.example.cpudefense.effects

import com.example.cpudefense.effects.Fader

interface Fadable {
    fun fadeDone(type: Fader.Type): Unit
    fun setOpacity(opacity: Float): Unit
}