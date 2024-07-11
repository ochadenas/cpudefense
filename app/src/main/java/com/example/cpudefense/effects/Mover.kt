package com.example.cpudefense.effects

import com.example.cpudefense.Game
import java.lang.Math.*

class Mover(
    private val game: Game, private val thing: Movable,
    fromX: Int, fromY: Int, toX: Int, toY: Int,
    var type: Type = Type.STRAIGHT, speed: Speed = Speed.FAST, wait: Int = 0)
/**
 * Auxiliary object that handles moving of game elements
 * by calculating their positions.
 * The idea is to remove complexity from the update routines of the elements.
 *
 * @param wait Cycles to wait before movement starts
 */
{
    var x = 0f // current position
    var y = 0f

    private var startX = fromX // starting point
    private var startY = fromY
    private var endX = toX // target (end) point
    private var endY = toY
    private var waitCycles = wait

    private var dX = 0f // delta per step
    private var dY = 0f
    private var steps = 0
    private var count = steps
    private var angle = 0.0 // for circular movement: angle in radians
    private var radiusX = 0
    private var radiusY = 0
    private val twoPi = 2.0f * PI

    enum class Type { NONE, APPEAR, STRAIGHT, BOUNCE, REPEAT, CIRCLE }

    enum class Speed { FAST, SLOW, MODERATE }

    init {
        var distPerStep = 0
        when (speed)
        {
            Speed.FAST -> { distPerStep = 30 }
            Speed.MODERATE -> { distPerStep = 15 }
            Speed.SLOW -> { distPerStep = 5 }
        }
        val deltaX = (endX - startX).toFloat()
        val deltaY = (endY - startY).toFloat()

        when (type)
        {
            Type.CIRCLE ->   {
                radiusX = endX
                radiusY = endY
                angle = 0.0
                steps = 360 / distPerStep * 2
            }
            Type.STRAIGHT -> {
                // determine dX and dY (movement per step) and number of steps
                val dist = kotlin.math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble())
                steps = (dist / distPerStep).toInt()
                if (steps < 5) {
                    steps = 5
                }
                dX = deltaX / steps
                dY = deltaY / steps
            }
            Type.REPEAT -> {
                val dist = kotlin.math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble())
                steps = (dist / distPerStep).toInt()
                if (steps < 5) {
                    steps = 5
                }
                dX = deltaX / steps
                dY = deltaY / steps
            }
            Type.BOUNCE -> {
                // allows only movement downwards or to the right
                if (deltaX>0)
                    dX = 1f
                else if (deltaY>0)
                    dY = 1f
                steps = 1
            }

            Type.NONE -> {}
            Type.APPEAR -> {}
        }
        reset()
        this.game.movers.add(this) // make sure we are in the list so that we can be called during update
    }

    private fun endMove()
    {
        x = endX.toFloat()
        y = endY.toFloat()
        thing.setCenter(x.toInt(), y.toInt())
        type = Type.NONE
        thing.moveDone()
    }

    private fun reset()
            /**
             * resets the movement to the starting position
             */
    {
        x = startX.toFloat()
        y = startY.toFloat()
        count = steps
    }

    private fun addDelta()
    {
        x += dX
        y += dY
    }

    fun update()
    {
        if (waitCycles>0)
        {
            waitCycles--
            if (waitCycles == 0)
                thing.moveStart()
            return
        }
        count--
        when (type)
        {
            Type.NONE -> { count = 0 }
            Type.STRAIGHT -> {
                addDelta()
                thing.setCenter(x.toInt(), y.toInt())
                if (count<=0) endMove()
            }
            Type.REPEAT -> {
                addDelta()
                thing.setCenter(x.toInt(), y.toInt())
                if (count<=0) reset()
            }
            Type.CIRCLE -> {
                val dW = (twoPi / steps).toFloat()
                angle += dW
                thing.setCenter(startX+(kotlin.math.sin(angle) * radiusX).toInt(), startY+(kotlin.math.cos(angle) * radiusY).toInt())
            }
            Type.BOUNCE -> {
                val gravity = 0.5f // measure for the acceleration to the right
                if (dX>0)
                {
                    addDelta()
                    thing.setCenter(x.toInt(), y.toInt())
                    if (x>endX) // target reached, inverse the movement
                    {
                        dX = -dX
                        dX *= 0.8f // reduce speed
                        if(dX>= -4.0f) {
                            dX = 0f
                            type = Type.NONE // we have come to a halt
                        }
                        addDelta()
                    }
                    else
                        dX += gravity
                }
                else if (dX<0)
                {
                    addDelta()
                    thing.setCenter(x.toInt(), y.toInt())
                    dX += gravity // lower the absolute value of dX, until 0 is reached
                }
                else // dX == 0
                {
                    dX=1f
                }
            }
            Type.APPEAR -> {}
        }
        return
    }
}