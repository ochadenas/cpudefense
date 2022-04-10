package com.example.cpudefense.gameElements

import android.graphics.*
import com.example.cpudefense.displayTextCenteredInRect
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.inflate

class Button(val text: String, val textsize: Float = 36f): Fadable
{
    var alpha = 0
    var myArea = Rect()
    var paint = Paint()

    init {
        /* determine size of area */
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.MONOSPACE
        paint.textSize = textsize
        paint.getTextBounds(text, 0, text.length, myArea)
        myArea.inflate(10)
    }

    override fun fadeDone(type: Fader.Type) {
    }

    override fun setOpacity(opacity: Float) {
        alpha = (opacity * 255).toInt()
    }

    fun display(canvas: Canvas) {
        val stringToDisplay = text
        val paint = Paint()
        paint.color = Color.GREEN
        paint.alpha = alpha
        paint.style = Paint.Style.FILL
        canvas.drawRect(myArea, paint)

        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.MONOSPACE
        paint.textSize = textsize
        myArea.displayTextCenteredInRect(canvas, stringToDisplay, paint)
    }

}