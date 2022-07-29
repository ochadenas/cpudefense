package com.example.cpudefense.gameElements

import android.graphics.*
import android.hardware.camera2.params.BlackLevelPattern
import com.example.cpudefense.displayTextCenteredInRect
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.inflate

class Button(val text: String, val textsize: Float = 36f, val color: Int = Color.GREEN, val style: Int = 0): Fadable
{
    var alpha = 0
    var myArea = Rect()
    var buttonPaint = Paint()
    var textPaint = Paint()

    init {
        when (style)
        {
            1 ->
            {
                buttonPaint.color = Color.WHITE  // default, should be overridden
                buttonPaint.style = Paint.Style.STROKE
                buttonPaint.strokeWidth = 2f
                textPaint.color = Color.WHITE
            }
            else -> {
                buttonPaint.color = color
                buttonPaint.style = Paint.Style.FILL
                textPaint.color = Color.BLACK
            }

        }
        textPaint.style = Paint.Style.FILL
        textPaint.typeface = Typeface.MONOSPACE
        textPaint.textSize = textsize
        textPaint.getTextBounds(text, 0, text.length, myArea)
        myArea.inflate(12)
    }

    override fun fadeDone(type: Fader.Type) {
    }

    override fun setOpacity(opacity: Float) {
        alpha = (opacity * 255).toInt()
    }

    fun display(canvas: Canvas) {
        val stringToDisplay = text
        buttonPaint.alpha = alpha
        canvas.drawRect(myArea, buttonPaint)
        myArea.displayTextCenteredInRect(canvas, stringToDisplay, textPaint)
    }

}