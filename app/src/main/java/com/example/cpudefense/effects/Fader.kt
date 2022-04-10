package com.example.cpudefense.effects

import com.example.cpudefense.Game

class Fader(game: Game, thing: Fadable, type: Type = Type.DISAPPEAR, speed: Speed = Speed.FAST, wait: Int = 0)
/**
 * Auxiliary object that handles appearing and disappearing of game elements.
 */
{
    var opacity = 0f

    private val theGame = game
    private val thing = thing

    private var waitCycles = wait
    private var dAlpha = 0f

    enum class Type { NONE, APPEAR, DISAPPEAR }
    var type = type

    enum class Speed { FAST, MEDIUM, SLOW, VERY_SLOW }

    init {
        when (type) {
            Type.APPEAR -> {
                opacity = 0f
            }
            Type.DISAPPEAR -> {
                opacity = 1.0f
            }
        }
        thing.setOpacity(opacity)
        when (speed) {
            Speed.FAST      -> { dAlpha = 0.05f }
            Speed.MEDIUM    -> { dAlpha = 0.03f }
            Speed.SLOW      -> { dAlpha = 0.02f }
            Speed.VERY_SLOW -> { dAlpha = 0.005f }
        }
        theGame.faders.add(this) // make sure we are in the list so that we can be called during update
    }

    fun endFade() {
        thing.fadeDone(type)
        this.type = Type.NONE
    }

    fun update()
    {
        if (waitCycles>0)
        {
            waitCycles--
            return
        }
        when (type)
        {
            Type.NONE -> { }
            Type.DISAPPEAR ->
            {
                opacity -= dAlpha
                thing.setOpacity(opacity)
                if (opacity <= 0)
                {
                    thing.setOpacity(0.0f)
                    endFade()
                }
            }
            Type.APPEAR -> {
                opacity += dAlpha
                thing.setOpacity(opacity)
                if (opacity >= 1.0f) {
                    thing.setOpacity(1.0f)
                    endFade()
                }
            }
        }
        return
    }
}