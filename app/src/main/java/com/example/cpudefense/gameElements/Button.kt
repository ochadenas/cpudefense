package com.example.cpudefense.gameElements

import android.graphics.*
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.utils.*

class Button(var text: String, val textSize: Float, val color: Int = Color.GREEN, val style: Int = 0, val preferredWidth: Int = 0): Fadable
{
    var alpha = 0
    var area = Rect()
    var touchableArea = Rect() // bigger than visible area, making it easier to hit the button
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
        /* calculate the size of the button */
        textPaint.textSize = textSize
        textPaint.getTextBounds("Tg", 0, "Tg".length, area) // sample text with descender
        val height = area.height() * 2
        textPaint.getTextBounds(text, 0, text.length, area)
        var width = area.width() + height
        if (width<preferredWidth)
            width = preferredWidth
        area = Rect(0,0,width,height)
        touchableArea = Rect(area).inflate(textSize.toInt())
    }

    fun alignRight(right: Int, top: Int)
    {
        area.set(right-area.width(), top, right, top+area.height())
        touchableArea.setCenter(area.center())
    }

    fun alignLeft(left: Int, top: Int)
    {
        area.setTopLeft(left, top)
        touchableArea.setCenter(area.center())
    }

    override fun fadeDone(type: Fader.Type) {
    }

    override fun setOpacity(opacity: Float) {
        alpha = (opacity * 255).toInt()
    }

    fun display(canvas: Canvas) {
        val stringToDisplay = text
        buttonPaint.alpha = alpha
        canvas.drawRect(area, buttonPaint)
        area.displayTextCenteredInRect(canvas, stringToDisplay, textPaint)
    }

}