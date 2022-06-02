package com.example.cpudefense.effects

import android.graphics.*
import com.example.cpudefense.*

class Background(val game: Game) {
    var bitmap: Bitmap? = null
    var bitmap1: Bitmap? = null
    var bitmap2: Bitmap? = null
    var dx = 0f
    var dy = 0f
    var vx = 0.1f
    var vy = 0.1f
    var opacity = 0.3f

    init {
        bitmap1 = BitmapFactory.decodeResource(game.resources, R.drawable.background_1)
        bitmap2 = BitmapFactory.decodeResource(game.resources, R.drawable.background_2)
        bitmap = bitmap2
    }

    fun update()
    {
        dx += vx
        dy += vy
    }

    fun display(canvas: Canvas, dest: Rect)
    {
        bitmap?.let {
            var paint = Paint()
            paint.alpha = (255 * opacity).toInt()
            var source =
                Rect(dx.toInt(), dy.toInt(), dest.width() + dx.toInt(), dest.height() + dy.toInt())
            canvas.drawBitmap(it, source, dest, paint)
        }
    }






}