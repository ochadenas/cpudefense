@file:Suppress("DEPRECATION")

package com.example.cpudefense.gameElements

import android.graphics.*
import com.example.cpudefense.GameMechanics
import com.example.cpudefense.GameView
import com.example.cpudefense.R
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader
import java.util.concurrent.CopyOnWriteArrayList

class Typewriter(val gameView: GameView, myArea: Rect, private var lines: CopyOnWriteArrayList<String>, private var callback: (() -> Unit)?)
{
    private var resources = gameView.resources
    private var textBoxes = CopyOnWriteArrayList<TextBox>()
    private val pos = Pair(myArea.left + 50, myArea.bottom - 80)
    private val lineSpacingY = GameMechanics.computerTextSize * gameView.textScaleFactor * 1.8f

    init { showNextLine() }

    @Suppress("MoveLambdaOutsideParentheses")
    private fun showNextLine(): Boolean
    {
        textBoxes.map { it.y -= lineSpacingY }
        if (lines.isEmpty()) {
            callback?.let { it() }  // call callback function, if defined.
            return false
        }
        textBoxes.add(TextBox(gameView, lines.removeFirst(), pos, {  showNextLine() } ))
        return true
    }

    fun display(canvas: Canvas) {
        textBoxes.map { it.display(canvas) }
    }

    inner class TextBox(val gameView: GameView, var text: String, topLeft: Pair<Int, Int>, private var callback: (() -> Unit)?):
        Fadable
    {
        var alpha = 255
        val textSize = GameMechanics.computerTextSize * gameView.textScaleFactor
        private var stringLength = 0 // number of characters to display
        var x = topLeft.first.toFloat()
        var y = topLeft.second.toFloat()
        val paint = Paint()

        init {
            Fader(gameView, this, Fader.Type.APPEAR, Fader.Speed.SLOW)
        }

        override fun fadeDone(type: Fader.Type) {
            callback?.let { it() }  // call callback function, if defined.
        }

        override fun setOpacity(opacity: Float) {
            stringLength = (text.length * opacity).toInt()
        }

        fun display(canvas: Canvas) {
            val stringToDisplay = text.substring(0, stringLength)
            val paint = Paint()
            paint.color = resources.getColor(R.color.text_green)
            paint.typeface = gameView.monoTypeface
            paint.textSize = textSize
            paint.alpha = alpha
            canvas.drawText(stringToDisplay, x, y, paint)
        }

    }
}