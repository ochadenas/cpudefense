package com.example.cpudefense.effects

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.example.cpudefense.Game
import com.example.cpudefense.setCenter
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

class Spark(var posX: Float, var posY: Float, var vX: Float, var vY: Float,
    val color: Int) {

    var size = Random.nextInt(4) + 4
    var maxAge = 64 + Random.nextInt(128)
    var age = 0

    fun expired(): Boolean
    { return age > maxAge }

    fun update(): Boolean
    {
        posX += vX
        posY += vY
        age += 1
        return (age > maxAge)
    }

    fun display(canvas: Canvas) {
        var paint = Paint()
        paint.color = color
        paint.alpha = 255 - 255 * age / maxAge
        var rect = Rect(0, 0, size, size)
        rect.setCenter(Pair(posX.toInt(), posY.toInt()))
        canvas.drawRect(rect, paint)
    }
}

class Explosion(game: Game, posOnScreen: Pair<Int, Int>,
                var primaryColor: Int, var secondaryColor: Int) {
    var sparks = CopyOnWriteArrayList<Spark>()

    init {
        sparks.clear()
        for (i in 0 .. 16)
        {
            val color = if (Random.nextFloat()>0.2) primaryColor else secondaryColor
            sparks.add(Spark(posOnScreen.first.toFloat(),
                posOnScreen.second.toFloat(),
                Random.nextFloat()-0.5f, Random.nextFloat()-0.4f ,color) )
        }
    }

    fun update() {
        sparks.map { it.update() }
        sparks.removeAll { x: Spark -> x.expired() } // remove sparks after their lifetime
    }

    fun display(canvas: Canvas) {
        sparks.map { it.display(canvas) }
    }

    fun expired(): Boolean
    // explosions without any spark are expired and can be removed
    { return sparks.isEmpty() }
}
