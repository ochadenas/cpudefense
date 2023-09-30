package com.example.cpudefense.gameElements

import android.graphics.*
import android.view.MotionEvent
import com.example.cpudefense.Game
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader

class SpeedControlButton(val game: Game, var type: Type = Type.PAUSE, val panel: SpeedControl): Fadable
{
    enum class Type { PAUSE, FAST, NORMAL, RETURN }

    var area = Rect()
    var paint = Paint()
    var alpha = 255
    private var bitmapOfType = hashMapOf<Type, Bitmap>()

    fun set_size(size: Int)
    {
        area = Rect(0, 0, size, size)
        bitmapOfType[Type.PAUSE] = Bitmap.createScaledBitmap(game.pauseIcon, size, size, true)
        bitmapOfType[Type.NORMAL] = Bitmap.createScaledBitmap(game.playIcon, size, size, true)
        bitmapOfType[Type.FAST] = Bitmap.createScaledBitmap(game.fastIcon, size, size, true)
        bitmapOfType[Type.RETURN] = Bitmap.createScaledBitmap(game.returnIcon, size, size, true)
    }

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
                Type.RETURN -> {
                    game.gameActivity.showReturnDialog()
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
        canvas.drawRect(area, paint)
        bitmapOfType[type]?.let {canvas.drawBitmap(it, null, area, paint) }
    }

}