@file:Suppress("DEPRECATION")

package com.example.cpudefense.gameElements

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
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
    private val lineSpacingY = GameView.computerTextSize * gameView.textScaleFactor * 1.8f
    private var paintLine = Paint()

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

    fun topOfTypewriterArea(): Int
    {
        var y = pos.second  // TODO: define in one place
        textBoxes[0]?.let { y = it.y.toInt() }
        return (y - lineSpacingY).toInt()
    }

    fun display(canvas: Canvas) {
        textBoxes.map { it.display(canvas) }
        paintLine.color = resources.getColor(R.color.text_green)
    }

    inner class TextBox(val gameView: GameView, var text: String, topLeft: Pair<Int, Int>, private var callback: (() -> Unit)?):
        Fadable
    {
        var alpha = 255
        val textSize = GameView.computerTextSize * gameView.textScaleFactor
        private var stringLength = 0 // number of characters to display
        var x = topLeft.first.toFloat()
        var y = topLeft.second.toFloat()
        private val paintText = Paint()

        init {
            Fader(gameView, this, Fader.Type.APPEAR, Fader.Speed.SLOW)
            paintText.color = resources.getColor(R.color.text_green)
            paintText.typeface = gameView.monoTypeface
            paintText.textSize = textSize
        }

        override fun fadeDone(type: Fader.Type) {
            callback?.let { it() }  // call callback function, if defined.
        }

        override fun setOpacity(opacity: Float) {
            stringLength = (text.length * opacity).toInt()
        }

        fun display(canvas: Canvas) {
            val stringToDisplay = if (text.length > stringLength)
                text.substring(0, stringLength) + "█"
            else
                text.substring(0, stringLength)
            paintText.alpha = alpha
            canvas.drawText(stringToDisplay, x, y, paintText)
        }
    }
}