package com.example.cpudefense

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.example.cpudefense.utils.displayTextCenteredInRect
import com.example.cpudefense.utils.setCenter
import com.example.cpudefense.utils.shrink

class ProgressNotification (theGame: Game)
{
    var isVisible = false
    var sizeX = 200
    var sizeY = 100
    var baseText = "Loading ..."
    var currentText: String = ""
    var myRect = Rect(0, 0, sizeX, sizeY)
    var value = 0.0f  // ranging from 0 to 1
    var paintRect = Paint()
    var paintText = Paint()

    init {
        paintRect.color = Color.YELLOW
        paintRect.style = Paint.Style.STROKE
        paintRect.strokeWidth = 4.0f
        paintText.color = paintRect.color
        paintText.style = Paint.Style.FILL
        paintText.textSize = Game.notificationTextSize
    }

    fun setPositionOnScreen(x: Int, y: Int)
    /** centers the rectangle on the given position */
    {
        myRect.setCenter(x, y)
    }

    fun showProgress(value: Float)
    {
        this.value = value
        currentText = "%s %d%% done.".format(baseText, (100 * value).toInt())
        isVisible = true
    }

    fun hide()
    {
        isVisible = false
    }

    fun display(canvas: Canvas)
    {
        if (!isVisible)
            return
        canvas.drawRect(myRect, paintRect)

        /* display the text */
        val actualRect: Rect = myRect.displayTextCenteredInRect(canvas, currentText, paintText)
        actualRect.shrink(-20)
        if (actualRect.width() > myRect.width())  // enlarge the rect to make the text fit
        {
            myRect.left = myRect.centerX() - actualRect.width() / 2
            myRect.right = myRect.centerX() + actualRect.width() / 2
        }
        if (actualRect.height() > myRect.height())  // enlarge the rect to make the text fit
        {
            myRect.top = myRect.centerY() - actualRect.height() / 2
            myRect.bottom = myRect.centerY() + actualRect.height() / 2
        }
    }

}