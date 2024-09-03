package com.example.cpudefense.effects

import android.graphics.Canvas
import com.example.cpudefense.GameMechanics
import com.example.cpudefense.GameView
import com.example.cpudefense.R
import java.util.concurrent.CopyOnWriteArrayList

class Effects(var gameView: GameView) {
    var explosions = CopyOnWriteArrayList<Explosion>()
    var faders = CopyOnWriteArrayList<Fader>()

    fun explode(thing: Explodable)
    {
        val explosionColour = thing.explosionColour ?: gameView.resources.getColor(R.color.attackers_glow_bin)
        explosions.add(Explosion(thing.getPositionOnScreen(), gameView.resources.getColor(R.color.attackers_foreground_bin), explosionColour ))
        thing.remove()
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

    fun display(canvas: Canvas)
    {
        explosions.map { it.display(canvas) }
    }
}