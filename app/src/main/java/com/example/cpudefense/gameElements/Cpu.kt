package com.example.cpudefense.gameElements

import android.graphics.*
import android.view.MotionEvent
import com.example.cpudefense.CpuReached
import com.example.cpudefense.networkmap.Network
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.makeSquare
import com.example.cpudefense.utils.scale
import com.example.cpudefense.utils.setCenter

class Cpu(network: Network, gridX: Int, gridY: Int): Chip(network, gridX, gridY)
{
    data class CpuData
    (
        var hits: Int
        )

    private var cpuData = CpuData(hits = 0)
    override var actualRect: Rect? = null

    override var bitmap: Bitmap? = null
    private val maxAnimationCount: Int = 32
    private var animationCount: Int = 0

    init {
        data.range = 1.0f
        chipData.type = ChipType.CPU
        actualRect = Rect(0, 0, 100, 100)
    }

    /*
    fun attackersInRange(): List<Attacker> {
        return distanceToVehicle.keys.filter { attackerInRange(it as Attacker) }
            .map { it as Attacker }
    }
    */

    override fun update() {
        super.update()
        val attackers = attackersInRange()
        if (attackers.isNotEmpty())
        {
            animationCount = maxAnimationCount
            attackers[0].remove()
            scoreHit()
        }
    }

    private fun createBitmap(): Bitmap?
    {
        var bitmap: Bitmap? = null
        actualRect = calculateActualRect()?.makeSquare()?.scale(2.5f)
        actualRect?.let {bitmap = Bitmap.createScaledBitmap(network.gameView.cpuImage, it.width(), it.height(), true) }
        return bitmap
    }

    override fun display(canvas: Canvas, viewport: Viewport)
    {
        if (bitmap == null)
            bitmap = createBitmap()
        actualRect?.let { rect ->
            rect.setCenter(viewport.gridToViewport(posOnGrid))
            val paint = Paint()
            bitmap?.let()
            { canvas.drawBitmap(it, null, rect, paint) }

            if (animationCount>0)
            {
                val animationRect = Rect(rect)
                animationRect.scale((1.2f - (animationCount/maxAnimationCount.toFloat())))
                paint.color = Color.RED
                paint.alpha = (animationCount * 255)/maxAnimationCount
                animationCount--
                canvas.drawRect(animationRect, paint)
            }
        }
    }

    private fun scoreHit()
    /** function that gets called when an attacker reaches the CPU
     */
    {
        cpuData.hits++
        throw CpuReached()
    }

    override fun onDown(event: MotionEvent): Boolean
    {
        return false
    }
}