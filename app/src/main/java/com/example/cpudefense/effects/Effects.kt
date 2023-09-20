package com.example.cpudefense.effects

import android.graphics.Canvas
import com.example.cpudefense.Game
import com.example.cpudefense.R
import java.util.concurrent.CopyOnWriteArrayList

class Effects(var theGame: Game) {
    var explosions = CopyOnWriteArrayList<Explosion>()
    var faders = CopyOnWriteArrayList<Fader>()

    fun explode(thing: Explodable)
    {
        explosions.add(Explosion(theGame, thing.getPositionOnScreen(),
        theGame.resources.getColor(R.color.attackers_foreground_bin),
        theGame.resources.getColor(R.color.attackers_glow_bin)))
        thing.remove()
    }

    fun fade(thing: Fadable)
    {
        faders.add(Fader(theGame, thing, speed =Fader.Speed.SLOW ))
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