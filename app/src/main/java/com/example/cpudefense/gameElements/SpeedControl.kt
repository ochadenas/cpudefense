package com.example.cpudefense.gameElements

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.view.MotionEvent
import com.example.cpudefense.GameMechanics
import com.example.cpudefense.GameView
import com.example.cpudefense.R
import com.example.cpudefense.utils.setCenter
import com.example.cpudefense.utils.setLeft

class SpeedControl(var gameView: GameView)
/** set of buttons that control the game speed, but also provide additional interaction such
 * as "lock scrolling" or "return to main menu". Also shows the level number.
 */
{
    private val gameMechanics = gameView.gameMechanics
    private var button1 = SpeedControlButton(gameView, gameMechanics, SpeedControlButton.Type.FAST, this)
    private var button2 = SpeedControlButton(gameView, gameMechanics, SpeedControlButton.Type.PAUSE, this)
    private var lockButton = SpeedControlButton(gameView, gameMechanics, SpeedControlButton.Type.UNLOCK, this)
    private var returnButton = SpeedControlButton(gameView, gameMechanics, SpeedControlButton.Type.RETURN, this)
    private var buttons = listOf( button1, button2, returnButton, lockButton )
    private var areaRight = Rect(0,0,0,0)
    private var areaLeft = Rect(0,0,0,0)
    private var areaCenter = Rect(0,0,0,0)
    
    private var stageInfoText = ""
    private var statusInfoBitmap: Bitmap? = null
    private var bitmapPaint = Paint()

    fun setSize(parentArea: Rect)
    {
        val actualButtonSize = (GameMechanics.speedControlButtonSize * gameView.resources.displayMetrics.density.toInt() *
            if (gameView.gameActivity.settings.configUseLargeButtons) 1.6f else 1.0f).toInt()
        val margin = actualButtonSize / 5   // space between the buttons
        buttons.forEach {it.setSize(actualButtonSize)}
        areaRight.right = parentArea.right - margin
        areaRight.bottom = parentArea.bottom - margin
        areaRight.left = areaRight.right - 2 * actualButtonSize - margin
        areaRight.top = areaRight.bottom - actualButtonSize
        button1.area.setCenter(areaRight.left + actualButtonSize / 2, areaRight.centerY())
        button2.area.setCenter(areaRight.right - actualButtonSize / 2, areaRight.centerY())
        // put the 'return' button on the other side
        areaLeft = Rect(areaRight)
        areaLeft.setLeft(margin)
        areaCenter = Rect(areaLeft.left, areaLeft.top, areaRight.right, areaRight.bottom)
        returnButton.area.setCenter(areaLeft.left + actualButtonSize / 2, areaLeft.centerY())
        lockButton.area.setCenter(areaLeft.right - actualButtonSize / 2, areaLeft.centerY())
    }
    
    fun setInfoline(newText: String)
    {
        if (newText == stageInfoText)
            return
        else
        {
            stageInfoText = newText
            recreateBitmap()
        }
    }

    @Suppress("DEPRECATION")
    private fun recreateBitmap()
    {
        bitmapPaint.alpha = 255
        val paint = Paint()
        paint.color = gameView.resources.getColor(R.color.connectors)
        paint.typeface = Typeface.SANS_SERIF
        paint.textSize = GameView.scoreHeaderSize * gameView.textScaleFactor
        paint.textAlign = Paint.Align.LEFT
        val bounds = Rect()
        paint.getTextBounds(stageInfoText, 0, stageInfoText.length, bounds)
        statusInfoBitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
        statusInfoBitmap?.let {
            val canvas = Canvas(it)
            canvas.drawText(stageInfoText, 0f, (it.height-bounds.bottom).toFloat(), paint)
        }
    }

    fun resetButtons()
    {
        button1.type = SpeedControlButton.Type.FAST
        button2.type = SpeedControlButton.Type.PAUSE
    }

    fun onDown(p0: MotionEvent): Boolean {
        return button1.onDown(p0) || button2.onDown(p0) || returnButton.onDown(p0) || lockButton.onDown(p0)
    }

    fun display(canvas: Canvas) {
        if (areaRight.left == 0)
            return
        buttons.forEach { it.display(canvas)}
        statusInfoBitmap?.let {
            val statusLineRect = Rect(0, 0, it.width, it.height)
            statusLineRect.setCenter(areaCenter.centerX(), areaCenter.centerY())
            canvas.drawBitmap(it, null, statusLineRect, bitmapPaint)
        }
    }
}