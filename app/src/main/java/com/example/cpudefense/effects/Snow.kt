package com.example.cpudefense.effects

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

class Flake(x: Int, y: Int)
{
    var rect = Rect(0, 0, 10, 10)
    var speedY = 1f
    var speedX = 0f
    var posX: Float = x.toFloat()
    var posY: Float = y.toFloat()
    var size = 1

    init {
        size = Random.nextInt(6, 12)
        speedY += Random.nextFloat() * (size-6)
        rect.right = size
        rect.bottom = size
        setCenter()
    }

    fun setCenter()
    {
        var x = posX.toInt()
        var y = posY.toInt()

        rect.set((x-rect.width()/2.0).toInt(), (y-rect.height()/2.0).toInt(),
                 (x+rect.width()/2.0).toInt(), (y+rect.width()/2.0).toInt()
        )
    }

    fun floatDown()
    {
        posY += speedY
        posX += speedX
        setCenter()
        speedX += (Random.nextFloat() - 0.5f) * (size-6) * 0.2f
        if (speedX<-4.0) speedX =  1.0f
        if (speedX> 4.0) speedX = -1.0f
    }


    fun display(canvas: Canvas)
    {
        var paint = Paint()
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        paint.alpha = (255)
        canvas.drawRect(rect, paint)
    }
}

class Snow() {
    var delay = 2  // update only once in <delay> times
    var flakes = CopyOnWriteArrayList<Flake>()
    private var count = delay
    var frequency: Float = 0f // used to set the snow flake amount. 0 = none, 1 = max
    var snowfallArea = Rect()

    fun updateGraphicalEffects()
    {
        if (snowfallArea.width() == 0)
            return // area not defined, do not add snow

        count--
        if (count>=0)
            return
        else
            count = delay

        if (Random.nextFloat() > 0.8 && Random.nextFloat() > (1.0-frequency))
        {
            var flake = Flake(Random.nextInt(0, snowfallArea.width()), 0)
            flakes.add(flake)
        }
        for (f in flakes)
        {
            f.floatDown()
            if (f.rect.bottom > snowfallArea.bottom)
                flakes.remove(f)
        }
    }

    fun display(canvas: Canvas)
    {
        for (f in flakes)
            f.display(canvas)
    }
}