package com.example.cpudefense.gameElements

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.view.MotionEvent
import com.example.cpudefense.CommonView
import com.example.cpudefense.R
import com.example.cpudefense.utils.setCenter
import com.example.cpudefense.utils.setLeft
import androidx.core.graphics.createBitmap
import com.example.cpudefense.GameView
import com.example.cpudefense.Stage
import com.example.cpudefense.utils.setTop

class CommonButtonPanel(var commonView: CommonView)
/** set of buttons that control the game speed, but also provide additional interaction such
 * as "lock scrolling" or "return to main menu". Also shows the level number.
 */
{
    private val gameMechanics = commonView.gameMechanics
    private var zoomPlusButton = CommonControlButton(commonView, gameMechanics, CommonControlButton.Type.ZOOM_PLUS)
    private var zoomMinusButton = CommonControlButton(commonView, gameMechanics, CommonControlButton.Type.ZOOM_MINUS)
    private var buttons = mutableListOf( zoomPlusButton, zoomMinusButton)
    /** area that contains "zoom in" and "zoom out" buttons */
    private var areaTop = Rect()
    /** the size of the control buttons in pixels, with density factor applied */
    var actualButtonSize: Int = 0

    /** the stage number (minus one) where the zoom buttons are shown for the first time */
    private val showZoomOnStage = Stage.Identifier(1, 14)

    private var stageInfoText = ""
    private var statusInfoBitmap: Bitmap? = null
    private var bitmapPaint = Paint()

    fun setSize(parentArea: Rect)
    {
        actualButtonSize = (CommonView.speedControlButtonSize * commonView.resources.displayMetrics.density.toInt() *
            if (commonView.settings().configUseLargeButtons) 1.6f else 1.0f).toInt()
        val margin = actualButtonSize / 5   // space between the buttons
        if (commonView.settings().zoom && commonView.gameMechanics.currentStageIdent.isGreaterThan(showZoomOnStage))
        {
            buttons.add(zoomPlusButton)
            buttons.add(zoomMinusButton)
        }
        buttons.forEach {it.setSize(actualButtonSize)}
        areaTop = Rect(parentArea.left+margin, parentArea.top, parentArea.left+2*(actualButtonSize+margin),
                       parentArea.top+actualButtonSize)
        zoomPlusButton.area.setCenter(areaTop.left + actualButtonSize / 2, areaTop.centerY())
        zoomMinusButton.area.setCenter(areaTop.right - actualButtonSize / 2, areaTop.centerY())
    }

    @Suppress("DEPRECATION")
    private fun recreateBitmap()
    {
        bitmapPaint.alpha = 255
        val paint = Paint()
        paint.color = commonView.resources.getColor(R.color.connectors)
        paint.typeface = Typeface.SANS_SERIF
        paint.textSize = CommonView.scoreHeaderSize * commonView.textScaleFactor
        paint.textAlign = Paint.Align.LEFT
        val bounds = Rect()
        paint.getTextBounds(stageInfoText, 0, stageInfoText.length, bounds)
        statusInfoBitmap = createBitmap(bounds.width(), bounds.height())
        statusInfoBitmap?.let {
            val canvas = Canvas(it)
            canvas.drawText(stageInfoText, 0f, (it.height-bounds.bottom).toFloat(), paint)
        }
    }

    fun onDown(p0: MotionEvent): Boolean {
        buttons.forEach { if (it.onDown(p0)) return true }
        return false
    }

    fun display(canvas: Canvas) {
        buttons.forEach { it.display(canvas)}
    }
}