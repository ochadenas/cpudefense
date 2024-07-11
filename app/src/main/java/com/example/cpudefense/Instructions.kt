package com.example.cpudefense

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader
import kotlin.random.Random

class Instructions(val game: Game, var stage: Stage.Identifier, var callback: (()->Unit)? ): Fadable {
    var alpha = 0

    private var funFact = if (Random.nextFloat() > 0.3)
        "%s\n\n%s".format(
        game.resources.getString(R.string.instr_did_you_know),
        game.resources.getStringArray(R.array.fun_fact).random())
    else ""

    private fun instructionText(level: Int): String
    {
        if (game.intermezzo.type in setOf(Intermezzo.Type.GAME_LOST, Intermezzo.Type.GAME_WON))
            return ""
        if (stage.series == Game.SERIES_NORMAL) {
            return when (level) {
                1 -> game.resources.getString(R.string.instr_1)
                2 -> game.resources.getString(R.string.instr_2)
                3 -> game.resources.getString(R.string.instr_3)
                4 -> game.resources.getString(R.string.instr_4)
                5 -> game.resources.getString(R.string.instr_5)
                6 -> game.resources.getString(R.string.instr_6)
                7 -> game.resources.getString(R.string.instr_7)
                8 -> game.resources.getString(R.string.instr_7a)
                9 -> game.resources.getString(R.string.instr_8)
                14 -> game.resources.getString(R.string.instr_9)
                20 -> game.resources.getString(R.string.instr_10)
                23 -> game.resources.getString(R.string.instr_12)
                24 -> game.resources.getString(R.string.instr_16)
                10 -> game.resources.getString(R.string.instr_11)
                21 -> game.resources.getString(R.string.instr_13)
                27 -> game.resources.getString(R.string.instr_14)
                28 -> game.resources.getString(R.string.instr_15).format(Game.temperatureLimit)
                30 -> game.resources.getString(R.string.instr_17)
                31 -> game.resources.getString(R.string.instr_18)
                else -> ""
            }
        }
        else if (stage.series == Game.SERIES_TURBO) {
            return when (level) {
                1 -> game.resources.getString(R.string.instr_2_1)
                else -> ""
            }
        }
        else if (stage.series == Game.SERIES_ENDLESS)
        {
            return when (level) {
                1 -> game.resources.getString(R.string.instr_endless)
                else -> funFact
            }

        }
        else
            return ""
    }

    init { Fader(game, this, Fader.Type.APPEAR, Fader.Speed.SLOW) }

    override fun fadeDone(type: Fader.Type) {
        callback?.let { it() }  // call callback function, if defined.
    }

    override fun setOpacity(opacity: Float) {
        alpha = (opacity*255).toInt()
    }


    fun display(canvas: Canvas) {
        val margin = 20
        val textArea = Rect(0, 0, canvas.width - 2 * margin, canvas.height - 200)
        canvas.save()

        canvas.translate(2*margin.toFloat(), margin.toFloat())
        val text = instructionText(stage.number)
        val textPaint = TextPaint()
        textPaint.textSize = Game.instructionTextSize * game.resources.displayMetrics.scaledDensity
        textPaint.color = Color.WHITE
        textPaint.alpha = alpha
        val textLayout = StaticLayout(
            text,
            textPaint,
            textArea.width(),
            Layout.Alignment.ALIGN_NORMAL,
            1.0f,
            0.0f,
            false
        )
        textLayout.draw(canvas)
        canvas.restore()
    }
}