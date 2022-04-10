package com.example.cpudefense.effects

import android.graphics.Canvas
import com.example.cpudefense.Game
import com.example.cpudefense.R
import com.example.cpudefense.networkmap.Viewport
import java.util.concurrent.CopyOnWriteArrayList

class Effects(var theGame: Game) {
    var explosions = CopyOnWriteArrayList<Explosion>()

    fun explode(thing: Explodable)
    {
        explosions.add(Explosion(theGame, thing.getPositionOnScreen(),
        theGame.resources.getColor(R.color.attackers_foreground),
        theGame.resources.getColor(R.color.attackers_glow)))
        thing.remove()
    }

    fun updateGraphicalEffects()
    {
        explosions.map { it.update() }
        explosions.removeAll { it.expired() }

    }

    fun display(canvas: Canvas)
    {
        explosions.map { it.display(canvas) }
    }
}