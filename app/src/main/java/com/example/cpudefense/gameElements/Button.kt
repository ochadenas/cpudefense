package com.example.cpudefense.gameElements

import android.graphics.*
import com.example.cpudefense.GameView
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.utils.*
import java.util.*

class Button(var gameView: GameView, var text: String, val textSize: Float, var color: Int = Color.GREEN, val style: Style = Style.FILLED, val preferredWidth: Int = 0): Fadable
{
    var alpha = 0
    var area = Rect()
    var touchableArea = Rect() // bigger than visible area, making it easier to hit the button
    private var buttonPaint = Paint()
    private var textPaint = Paint()
    var background: Bitmap? = null

    enum class Style { FRAME, FILLED, HP_KEY}

    init {
        when (style)
        {
            Style.FRAME ->
            {
                buttonPaint.style = Paint.Style.STROKE
                buttonPaint.strokeWidth = 2f
                textPaint.typeface = gameView.boldTypeface
                textPaint.color = Color.WHITE
            }
            Style.FILLED -> {
                buttonPaint.style = Paint.Style.FILL
                textPaint.typeface = gameView.boldTypeface
                textPaint.color = Color.BLACK
            }
            Style.HP_KEY ->
            {
                background = gameView.hpBackgroundBitmap
                textPaint.typeface = Typeface.DEFAULT_BOLD
                textPaint.color = Color.WHITE
            }

        }
        textPaint.style = Paint.Style.FILL
        /* calculate the size of the button */
        textPaint.textSize = textSize
        textPaint.getTextBounds("Tg", 0, "Tg".length, area) // sample text with descender
        var height = area.height() * 2
        textPaint.getTextBounds(text, 0, text.length, area)
        var width = area.width() + height
        if (width<preferredWidth)
            width = preferredWidth
        if (style == Style.HP_KEY)  // these buttons need more space
            height = (height*1.5).toInt()
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
        var stringToDisplay = text
        buttonPaint.color = color
        buttonPaint.alpha = alpha
        textPaint.alpha = alpha
        if (style == Style.HP_KEY) {
            stringToDisplay = stringToDisplay.uppercase(Locale.getDefault())
            background?.let { canvas.drawBitmap(it, null, area, buttonPaint) }
        }
        else
            canvas.drawRect(area, buttonPaint)
        area.displayTextCenteredInRect(canvas, stringToDisplay, textPaint)
    }

}