package com.example.cpudefense.effects

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.example.cpudefense.utils.setCenter
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

class Spark(
    private var posX: Float, private var posY: Float, private var vX: Float, private var vY: Float,
    val color: Int) {

    var size = Random.nextInt(4) + 4
    private var maxAge = 64 + Random.nextInt(128)
    private var age = 0
    val paint = Paint()
    var rect = Rect(0, 0, size, size)

    init { paint.color = color }

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
        paint.alpha = 255 - 255 * age / maxAge
        rect.setCenter(Pair(posX.toInt(), posY.toInt()))
        canvas.drawRect(rect, paint)
    }
}

class Explosion(posOnScreen: Pair<Int, Int>,
                primaryColor: Int, secondaryColor: Int) {
    private var sparks = CopyOnWriteArrayList<Spark>()

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
