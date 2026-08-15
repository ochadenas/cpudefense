package com.example.cpudefense.editorElements

import android.graphics.*
import android.view.MotionEvent
import com.example.cpudefense.GameMechanics
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader
import androidx.core.graphics.scale
import com.example.cpudefense.EditorView

class EditorControlButton(val editorView: EditorView,
                          val gameMechanics: GameMechanics,
                          var type: Type = Type.MENU,
                          private val panel: EditorPanel)
    : Fadable
{
    enum class Type { MENU, NEW_CHIP, MOVE_CHIP }

    var area = Rect()
    var paint = Paint()
    var alpha = 160
    private var bitmapOfType = hashMapOf<Type, Bitmap>()

    fun setSize(size: Int)
    {
        area = Rect(0, 0, size, size)
        bitmapOfType[Type.MENU] = editorView.menuIcon.scale(size, size)
        bitmapOfType[Type.NEW_CHIP] = editorView.chipIcon.scale(size, size)
        bitmapOfType[Type.MOVE_CHIP] = editorView.moveIcon.scale(size, size)
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
                Type.MENU -> { editorView.showMenu() }
                Type.NEW_CHIP -> {  editorView.addChip() }
                Type.MOVE_CHIP -> {}
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