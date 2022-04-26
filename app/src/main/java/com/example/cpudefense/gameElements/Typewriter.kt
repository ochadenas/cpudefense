package com.example.cpudefense.gameElements

import android.graphics.*
import com.example.cpudefense.Game
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader
import java.util.concurrent.CopyOnWriteArrayList

class Typewriter(val game: Game, myArea: Rect, private var lines: CopyOnWriteArrayList<String>, private var callback: (() -> Unit)?) {
    var textBoxes = CopyOnWriteArrayList<TextBox>()
    val pos = Pair(myArea.left + 50, myArea.bottom - 50)
    val lineSpacingY = 70

    init { showNextLine() }

    fun showNextLine(): Boolean
    {
        textBoxes.map { it.y -= lineSpacingY }
        if (lines.isEmpty()) {
            callback?.let { it() }  // call callback function, if defined.
            return false
        }
        textBoxes.add(TextBox(game, lines.removeFirst(), pos, {  showNextLine() } ))
        return true
    }

    fun display(canvas: Canvas) {
        textBoxes.map { it.display(canvas) }
    }

    inner class TextBox(val game: Game, var text: String, topLeft: Pair<Int, Int>, var callback: (() -> Unit)?):
        Fadable
    {
        var alpha = 255
        val textSize = Game.computerTextSize
        var stringLength = 0 // number of characters to display
        var x = topLeft.first.toFloat()
        var y = topLeft.second.toFloat()

        init { Fader(game, this, Fader.Type.APPEAR, Fader.Speed.SLOW) }

        override fun fadeDone(type: Fader.Type) {
            callback?.let { it() }  // call callback function, if defined.
        }

        override fun setOpacity(opacity: Float) {
            stringLength = (text.length * opacity).toInt()
        }

        fun display(canvas: Canvas) {
            val stringToDisplay = text.substring(0, stringLength)
            val paint = Paint()
            paint.color = Color.GREEN
            paint.typeface = Typeface.MONOSPACE
            paint.textSize = textSize
            paint.alpha = alpha
            canvas.drawText(stringToDisplay, x, y, paint)
        }

    }
}