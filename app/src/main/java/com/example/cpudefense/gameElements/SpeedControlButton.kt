package com.example.cpudefense.gameElements

import android.graphics.*
import android.view.MotionEvent
import com.example.cpudefense.Game
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader

class SpeedControlButton(val game: Game, var type: Type = Type.PAUSE, val panel: SpeedControl): Fadable
{
    enum class Type { PAUSE, FAST, NORMAL }

    var size = Game.speedControlButtonSize
    var area = Rect(0, 0, size, size)
    var paint = Paint()
    var alpha = 255
    lateinit var bitmap: Bitmap

    override fun fadeDone(type: Fader.Type) {
    }

    override fun setOpacity(opacity: Float) {
        alpha = (opacity * 255).toInt()
    }

    fun onDown(p0: MotionEvent): Boolean {
        if (area.contains(p0.x.toInt(), p0.y.toInt()))
        {
            when (type)
            {
                Type.PAUSE -> {
                    game.gameActivity.setGameSpeed(Game.GameSpeed.NORMAL)
                    game.state.phase = Game.GamePhase.PAUSED
                    panel.resetButtons()
                    type = Type.NORMAL
                }
                Type.NORMAL -> {
                    game.gameActivity.setGameSpeed(Game.GameSpeed.NORMAL)
                    game.state.phase = Game.GamePhase.RUNNING
                    panel.resetButtons()
                }
                Type.FAST -> {
                    game.gameActivity.setGameSpeed(Game.GameSpeed.MAX)
                    panel.resetButtons()
                    type = Type.NORMAL
                }

            }
            return true
        }
        else
            return false
    }

    fun display(canvas: Canvas) {
        paint.color = Color.GREEN
        paint.alpha = alpha
        when (type)
        {
            Type.PAUSE -> bitmap = game.pauseIcon
            Type.NORMAL -> bitmap = game.playIcon
            Type.FAST -> bitmap = game.fastIcon
        }
        canvas.drawRect(area, paint)
        canvas.drawBitmap(bitmap, null, area, paint)
    }

}