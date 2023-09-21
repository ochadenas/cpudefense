package com.example.cpudefense.effects

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.example.cpudefense.Game
import com.example.cpudefense.utils.clear
import com.example.cpudefense.utils.flipHorizontally
import com.example.cpudefense.utils.flipVertically
import kotlin.math.cos

class Flipper(val theGame: Game, private val thing: Flippable,
              var type: Type = Type.HORIZONTAL, private val speed: Speed = Speed.MEDIUM)
/**
 * Auxiliary object that handles turning (or 'flipping') of game elements,
 * either horizontally or vertically
 */
{
    private var bitmapRecto = thing.provideBitmap()
    private lateinit var bitmapVerso: Bitmap
    private var width = bitmapRecto.width
    private var height = bitmapRecto.height
    private var actualBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    private var paint = Paint()

    enum class Type { HORIZONTAL, VERTICAL, NONE }
    enum class Speed { FAST, MEDIUM, SLOW, VERY_SLOW }

    var angle = 0.0  // turning angle, from 0 to 360

    init {
        when (type) {
            Type.HORIZONTAL -> { bitmapVerso = bitmapRecto.flipHorizontally() }
            Type.VERTICAL -> { bitmapVerso = bitmapRecto.flipVertically() }
            else -> { bitmapVerso = bitmapRecto.copy(bitmapRecto.config, true) }
        }
        theGame.flippers.add(this) // make sure we are in the list so that we can be called during update
    }

    fun update()
    {
        if (type == Type.NONE)
            return
        angle += when (speed)
        {
            Speed.FAST -> 10.0
            Speed.MEDIUM -> 5.0
            Speed.SLOW -> 2.0
            Speed.VERY_SLOW -> 1.0
        }
        if (angle >= 360) {
            flipDone()
            angle = 0.0
        }
        val dimX = (cos(Math.toRadians(angle)) * width).toInt()
        actualBitmap.clear()
        var canvas = Canvas(actualBitmap)
        if (dimX < 0)
        {
            var targetRect = Rect((width + dimX) / 2, 0, (width - dimX) / 2, height)
            canvas.drawBitmap(bitmapVerso, null, targetRect, paint)
        }
        else {
            var targetRect = Rect((width - dimX) / 2, 0, (width + dimX) / 2, height)
            canvas.drawBitmap(bitmapRecto, null, targetRect, paint)
        }
        thing.setBitmap(actualBitmap.copy(actualBitmap.config, true))
        return
    }

    fun flipDone()
    {
        type = Type.NONE
        actualBitmap = bitmapRecto.copy(bitmapRecto.config, true)
        thing.setBitmap(actualBitmap)
    }
}