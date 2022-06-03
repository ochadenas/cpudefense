package com.example.cpudefense.effects

import android.graphics.*
import com.example.cpudefense.*
import kotlin.math.sin
import kotlin.math.cos

class Background(val game: Game) {
    var bitmap: Bitmap? = null
    var bitmap1: Bitmap? = null
    var bitmap2: Bitmap? = null
    var dx = 0f
    var dy = 0f
    var angle = 0f
    var vx = 0.1f
    var vy = 0.1f
    var x = 0f
    var y = 0f
    var projX = 0f
    var projY = 0f
    var opacity = 0.3f

    init {
        bitmap2 = BitmapFactory.decodeResource(game.resources, R.drawable.background_2)
        choose(1, 0.2f)
    }

    fun choose(number: Int, opacity: Float = 0.3f)
            /** selects the background to use.
             * @param number selects one of the available backgrounds
             * @param opacity sets the opacity, from 0.0 to 1.0
             */
    {
        when (number)
        {
            2 -> bitmap = bitmap2
            else -> bitmap = bitmap2
        }
        this.opacity = opacity
    }

    fun update()
    {
        angle += 0.0002f
        projX = cos(angle)
        projY = sin(angle)
    }

    fun display(canvas: Canvas, dest: Rect)
    {
        bitmap?.let {
            var paint = Paint()
            paint.alpha = (255 * opacity).toInt()
            var source = Rect(0,0,dest.width(),dest.height())
            var x = it.width/2 + projX * 0.5f * (it.width - dest.width())
            var y = it.height/2 + projY * 0.5f * (it.height - dest.height())
            source.setCenter(x.toInt(),y.toInt())

            /*
            var source =
                Rect(dx.toInt(), dy.toInt(), dest.width() + dx.toInt(), dest.height() + dy.toInt())

             */
            canvas.drawBitmap(it, source, dest, paint)
        }
    }






}