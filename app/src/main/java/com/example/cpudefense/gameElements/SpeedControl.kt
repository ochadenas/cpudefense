package com.example.cpudefense.gameElements

import android.graphics.Canvas
import android.graphics.Rect
import android.view.MotionEvent
import com.example.cpudefense.Game
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.setCenter

class SpeedControl(var game: Game) {
    var button1 = SpeedControlButton(game, SpeedControlButton.Type.FAST, this)
    var button2 = SpeedControlButton(game, SpeedControlButton.Type.PAUSE, this)
    var area = Rect(0,0,0,0)
    var margin = 20

    fun setSize(parentArea: Rect)
    {
        area.right = parentArea.right - margin
        area.bottom = parentArea.bottom - margin
        area.left = area.right - 2 * Game.speedControlButtonSize - margin
        area.top = area.bottom - Game.speedControlButtonSize
        button1.area.setCenter(area.left + Game.speedControlButtonSize / 2, area.centerY())
        button2.area.setCenter(area.right - Game.speedControlButtonSize / 2, area.centerY())
    }

    fun resetButtons()
    {
        button1.type = SpeedControlButton.Type.FAST
        button2.type = SpeedControlButton.Type.PAUSE
    }

    fun onDown(p0: MotionEvent): Boolean {
        return button1.onDown(p0) || button2.onDown(p0)
    }

    fun display(canvas: Canvas, viewport: Viewport) {
        if (area.left == 0)
            return
        button1.display(canvas)
        button2.display(canvas)
    }
}