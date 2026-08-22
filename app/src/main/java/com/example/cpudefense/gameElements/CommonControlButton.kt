package com.example.cpudefense.gameElements

import android.graphics.*
import android.view.MotionEvent
import com.example.cpudefense.GameMechanics
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader
import androidx.core.graphics.scale
import com.example.cpudefense.CommonView

open class CommonControlButton(val commonView: CommonView, val gameMechanics: GameMechanics, var type: Type): Fadable
{
    enum class Type { PAUSE, FAST, FASTEST, NORMAL, RETURN, LOCK, UNLOCK, ZOOM_PLUS, ZOOM_MINUS }

    var area = Rect()
    var paint = Paint()
    var alpha = 160
    var bitmapOfType = hashMapOf<Type, Bitmap>()

    open fun setSize(size: Int)
    {
        area = Rect(0, 0, size, size)
        bitmapOfType[Type.PAUSE] = commonView.pauseIcon.scale(size, size)
        bitmapOfType[Type.NORMAL] = commonView.playIcon.scale(size, size)
        bitmapOfType[Type.FAST] = commonView.fastIcon.scale(size, size)
        bitmapOfType[Type.FASTEST] = commonView.fastestIcon.scale(size, size)
        bitmapOfType[Type.RETURN] = commonView.returnIcon.scale(size, size)
        bitmapOfType[Type.LOCK] = commonView.moveLockIcon.scale(size, size)
        bitmapOfType[Type.UNLOCK] = commonView.moveUnlockIcon.scale(size, size)
        bitmapOfType[Type.ZOOM_PLUS] = commonView.zoomPlusIcon.scale(size, size)
        bitmapOfType[Type.ZOOM_MINUS] = commonView.zoomMinusIcon.scale(size, size)
    }

    override fun fadeDone(type: Fader.Type) {
    }

    override fun setOpacity(opacity: Float) {
        alpha = (opacity * 255).toInt()
    }

    open fun onDown(p0: MotionEvent): Boolean {
        if (area.contains(p0.x.toInt(), p0.y.toInt()))
        {
            when (type)
            {
                Type.ZOOM_PLUS -> {
                    commonView.viewport.scaleByStep(gameMechanics.currentlyActiveStage?.network, true)
                }
                Type.ZOOM_MINUS -> {
                    commonView.viewport.scaleByStep(gameMechanics.currentlyActiveStage?.network, false)
                }
                Type.RETURN -> {
                    commonView.showReturnDialog()
                }
                Type.LOCK -> {
                    commonView.scrollAllowed = true
                    type = Type.UNLOCK
                }
                Type.UNLOCK -> {
                    commonView.scrollAllowed = false
                    type = Type.LOCK
                }
                else -> return false
            }
            return true
        }
        else
            return false
    }

    fun display(canvas: Canvas) {
        paint.color = Color.BLACK
        paint.alpha = alpha
        bitmapOfType[type]?.let {canvas.drawBitmap(it, null, area, paint) }
    }

}