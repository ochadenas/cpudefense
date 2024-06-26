package com.example.cpudefense.gameElements

import android.graphics.*
import android.view.MotionEvent
import com.example.cpudefense.Game
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader

class SpeedControlButton(val game: Game, var type: Type = Type.PAUSE, private val panel: SpeedControl): Fadable
{
    enum class Type { PAUSE, FAST, NORMAL, RETURN, LOCK, UNLOCK }

    var area = Rect()
    var paint = Paint()
    var alpha = 160
    private var bitmapOfType = hashMapOf<Type, Bitmap>()

    fun setSize(size: Int)
    {
        area = Rect(0, 0, size, size)
        bitmapOfType[Type.PAUSE] = Bitmap.createScaledBitmap(game.pauseIcon, size, size, true)
        bitmapOfType[Type.NORMAL] = Bitmap.createScaledBitmap(game.playIcon, size, size, true)
        bitmapOfType[Type.FAST] = Bitmap.createScaledBitmap(game.fastIcon, size, size, true)
        bitmapOfType[Type.RETURN] = Bitmap.createScaledBitmap(game.returnIcon, size, size, true)
        bitmapOfType[Type.LOCK] = Bitmap.createScaledBitmap(game.moveLockIcon, size, size, true)
        bitmapOfType[Type.UNLOCK] = Bitmap.createScaledBitmap(game.moveUnlockIcon, size, size, true)
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
                    game.state.phase = Game.GamePhase.RUNNING
                    panel.resetButtons()
                    type = Type.NORMAL
                }
                Type.RETURN -> {
                    game.gameActivity.showReturnDialog()
                }
                Type.LOCK -> {
                    game.gameActivity.theGameView.scrollAllowed = true
                    type = Type.UNLOCK
                }
                Type.UNLOCK -> {
                    game.gameActivity.theGameView.scrollAllowed = false
                    type = Type.LOCK
                }
            }
            return true
        }
        else
            return false
    }

    fun display(canvas: Canvas) {
        paint.color = Color.BLACK
        paint.alpha = alpha
        // canvas.drawRect(area, paint)
        bitmapOfType[type]?.let {canvas.drawBitmap(it, null, area, paint) }
    }

}