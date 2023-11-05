package com.example.cpudefense.gameElements

import android.graphics.Canvas
import android.graphics.Rect
import android.view.MotionEvent
import com.example.cpudefense.Game
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.setCenter
import com.example.cpudefense.utils.setLeft

class SpeedControl(var game: Game) {
    private var button1 = SpeedControlButton(game, SpeedControlButton.Type.FAST, this)
    private var button2 = SpeedControlButton(game, SpeedControlButton.Type.PAUSE, this)
    private var button_lock = SpeedControlButton(game, SpeedControlButton.Type.UNLOCK, this)
    private var button_return = SpeedControlButton(game, SpeedControlButton.Type.RETURN, this)
    private var buttons = listOf<SpeedControlButton>( button1, button2, button_return, button_lock )
    private var area_right = Rect(0,0,0,0)
    private var area_left = Rect(0,0,0,0)

    fun setSize(parentArea: Rect)
    {
        val actualButtonSize = Game.speedControlButtonSize * game.resources.displayMetrics.density.toInt() *
            if (game.gameActivity.settings.configUseLargeButtons) 2 else 1
        val margin = actualButtonSize / 5   // space between the buttons
        buttons.forEach() {it.set_size(actualButtonSize)}
        area_right.right = parentArea.right - margin
        area_right.bottom = parentArea.bottom - margin
        area_right.left = area_right.right - 2 * actualButtonSize - margin
        area_right.top = area_right.bottom - actualButtonSize
        button1.area.setCenter(area_right.left + actualButtonSize / 2, area_right.centerY())
        button2.area.setCenter(area_right.right - actualButtonSize / 2, area_right.centerY())
        // put the 'return' button on the other side
        area_left = Rect(area_right)
        area_left.setLeft(margin)
        button_return.area.setCenter(area_left.left + actualButtonSize / 2, area_left.centerY())
        button_lock.area.setCenter(area_left.right - actualButtonSize / 2, area_left.centerY())
    }

    fun resetButtons()
    {
        button1.type = SpeedControlButton.Type.FAST
        button2.type = SpeedControlButton.Type.PAUSE
    }

    fun onDown(p0: MotionEvent): Boolean {
        return button1.onDown(p0) || button2.onDown(p0) || button_return.onDown(p0) || button_lock.onDown(p0)
    }

    fun display(canvas: Canvas, viewport: Viewport) {
        if (area_right.left == 0)
            return
        buttons.forEach() { it.display(canvas)}
    }
}