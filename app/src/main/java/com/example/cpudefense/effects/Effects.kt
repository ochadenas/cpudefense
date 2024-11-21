@file:Suppress("DEPRECATION")

package com.example.cpudefense.effects

import android.graphics.Canvas
import android.graphics.Rect
import com.example.cpudefense.GameView
import com.example.cpudefense.R
import java.util.concurrent.CopyOnWriteArrayList

class Effects(var gameView: GameView) {
    var explosions = CopyOnWriteArrayList<Explosion>()
    private var faders = CopyOnWriteArrayList<Fader>()
    /** snow is used for the "christmas time easter egg" */
    val snow = Snow()
    private var gameArea = Rect()

    fun explode(thing: Explodable)
    {
        val explosionColour = thing.explosionColour ?: gameView.resources.getColor(R.color.attackers_glow_bin)
        explosions.add(Explosion(thing.getPositionOnScreen(), gameView.resources.getColor(R.color.attackers_foreground_bin), explosionColour ))
        thing.remove()
    }

    fun setSize(area: Rect)
    {
        gameArea = Rect(area)
        snow.apply { snowfallArea = Rect(gameArea) }
    }

    fun addSnow()
    {
        snow.frequency = 0.4f
    }

    fun fade(thing: Fadable)
    {
        faders.add(Fader(gameView, thing, speed =Fader.Speed.SLOW ))
    }

    fun updateGraphicalEffects()
    {
        faders.map { it.update() }
        explosions.map { it.update() }
        explosions.removeAll { it.expired() }
    }

    fun displayGraphicalEffects(canvas: Canvas)
    {
        explosions.map { it.display(canvas) }
    }
}