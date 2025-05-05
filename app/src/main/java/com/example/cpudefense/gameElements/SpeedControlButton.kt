package com.example.cpudefense.gameElements

import android.graphics.*
import android.view.MotionEvent
import com.example.cpudefense.GameMechanics
import com.example.cpudefense.GameView
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader
import androidx.core.graphics.scale

class SpeedControlButton(val gameView: GameView, val gameMechanics: GameMechanics, var type: Type = Type.PAUSE, private val panel: SpeedControl): Fadable
{
    enum class Type { PAUSE, FAST, FASTEST, NORMAL, RETURN, LOCK, UNLOCK }

    var area = Rect()
    var paint = Paint()
    var alpha = 160
    private var bitmapOfType = hashMapOf<Type, Bitmap>()

    fun setSize(size: Int)
    {
        area = Rect(0, 0, size, size)
        bitmapOfType[Type.PAUSE] = gameView.pauseIcon.scale(size, size)
        bitmapOfType[Type.NORMAL] = gameView.playIcon.scale(size, size)
        bitmapOfType[Type.FAST] = gameView.fastIcon.scale(size, size)
        bitmapOfType[Type.FASTEST] = gameView.fastestIcon.scale(size, size)
        bitmapOfType[Type.RETURN] = gameView.returnIcon.scale(size, size)
        bitmapOfType[Type.LOCK] = gameView.moveLockIcon.scale(size, size)
        bitmapOfType[Type.UNLOCK] = gameView.moveUnlockIcon.scale(size, size)
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
                    gameView.gameActivity.setGameSpeed(GameMechanics.GameSpeed.NORMAL)
                    gameView.gameActivity.changeToGamePhase(GameMechanics.GamePhase.PAUSED)
                    panel.resetButtons()
                    type = Type.NORMAL
                }
                Type.NORMAL -> {
                    gameView.gameActivity.setGameSpeed(GameMechanics.GameSpeed.NORMAL)
                    gameView.gameActivity.changeToGamePhase(GameMechanics.GamePhase.RUNNING)
                    panel.resetButtons()
                }
                Type.FAST -> {
                    gameView.gameActivity.setGameSpeed(GameMechanics.GameSpeed.FAST)
                    gameView.gameActivity.changeToGamePhase(GameMechanics.GamePhase.RUNNING)
                    panel.resetButtons()
                    type = Type.NORMAL
                }
                Type.FASTEST -> {
                    gameView.gameActivity.setGameSpeed(GameMechanics.GameSpeed.MAX)
                    gameView.gameActivity.changeToGamePhase(GameMechanics.GamePhase.RUNNING)
                    panel.resetButtons()
                    type = Type.NORMAL
                }
                Type.RETURN -> {
                    gameView.gameActivity.showReturnDialog()
                }
                Type.LOCK -> {
                    gameView.scrollAllowed = true
                    type = Type.UNLOCK
                }
                Type.UNLOCK -> {
                    gameView.scrollAllowed = false
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