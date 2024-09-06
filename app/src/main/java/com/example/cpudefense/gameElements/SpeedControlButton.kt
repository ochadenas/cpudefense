package com.example.cpudefense.gameElements

import android.graphics.*
import android.view.MotionEvent
import com.example.cpudefense.GameMechanics
import com.example.cpudefense.GameView
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader

class SpeedControlButton(val gameView: GameView, val gameMechanics: GameMechanics, var type: Type = Type.PAUSE, private val panel: SpeedControl): Fadable
{
    enum class Type { PAUSE, FAST, NORMAL, RETURN, LOCK, UNLOCK }

    var area = Rect()
    var paint = Paint()
    var alpha = 160
    private var bitmapOfType = hashMapOf<Type, Bitmap>()

    fun setSize(size: Int)
    {
        area = Rect(0, 0, size, size)
        bitmapOfType[Type.PAUSE] = Bitmap.createScaledBitmap(gameView.pauseIcon, size, size, true)
        bitmapOfType[Type.NORMAL] = Bitmap.createScaledBitmap(gameView.playIcon, size, size, true)
        bitmapOfType[Type.FAST] = Bitmap.createScaledBitmap(gameView.fastIcon, size, size, true)
        bitmapOfType[Type.RETURN] = Bitmap.createScaledBitmap(gameView.returnIcon, size, size, true)
        bitmapOfType[Type.LOCK] = Bitmap.createScaledBitmap(gameView.moveLockIcon, size, size, true)
        bitmapOfType[Type.UNLOCK] = Bitmap.createScaledBitmap(gameView.moveUnlockIcon, size, size, true)
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
                    gameMechanics.gameActivity.setGameSpeed(GameMechanics.GameSpeed.NORMAL)
                    gameMechanics.state.phase = GameMechanics.GamePhase.PAUSED
                    panel.resetButtons()
                    type = Type.NORMAL
                }
                Type.NORMAL -> {
                    gameMechanics.gameActivity.setGameSpeed(GameMechanics.GameSpeed.NORMAL)
                    gameMechanics.state.phase = GameMechanics.GamePhase.RUNNING
                    panel.resetButtons()
                }
                Type.FAST -> {
                    gameMechanics.gameActivity.setGameSpeed(GameMechanics.GameSpeed.MAX)
                    gameMechanics.state.phase = GameMechanics.GamePhase.RUNNING
                    panel.resetButtons()
                    type = Type.NORMAL
                }
                Type.RETURN -> {
                    gameMechanics.gameActivity.showReturnDialog()
                }
                Type.LOCK -> {
                    gameMechanics.gameActivity.gameView.scrollAllowed = true
                    type = Type.UNLOCK
                }
                Type.UNLOCK -> {
                    gameMechanics.gameActivity.gameView.scrollAllowed = false
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