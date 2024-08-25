package com.example.cpudefense.gameElements

import android.graphics.Canvas
import android.graphics.Rect
import android.view.MotionEvent
import com.example.cpudefense.GameMechanics
import com.example.cpudefense.GameView
import com.example.cpudefense.utils.setCenter
import com.example.cpudefense.utils.setLeft

class SpeedControl(var gameView: GameView)
{
    private val gameMechanics = gameView.gameMechanics
    private var button1 = SpeedControlButton(gameMechanics, SpeedControlButton.Type.FAST, this)
    private var button2 = SpeedControlButton(gameMechanics, SpeedControlButton.Type.PAUSE, this)
    private var lockButton = SpeedControlButton(gameMechanics, SpeedControlButton.Type.UNLOCK, this)
    private var returnButton = SpeedControlButton(gameMechanics, SpeedControlButton.Type.RETURN, this)
    private var buttons = listOf( button1, button2, returnButton, lockButton )
    private var areaRight = Rect(0,0,0,0)
    private var areaLeft = Rect(0,0,0,0)

    fun setSize(parentArea: Rect)
    {
        val actualButtonSize = (GameMechanics.speedControlButtonSize * gameMechanics.resources.displayMetrics.density.toInt() *
            if (gameMechanics.gameActivity.settings.configUseLargeButtons) 1.6f else 1.0f).toInt()
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
        returnButton.area.setCenter(areaLeft.left + actualButtonSize / 2, areaLeft.centerY())
        lockButton.area.setCenter(areaLeft.right - actualButtonSize / 2, areaLeft.centerY())
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
    }
}