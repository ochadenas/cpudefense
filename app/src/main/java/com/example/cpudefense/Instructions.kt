package com.example.cpudefense

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader
import kotlin.random.Random

class Instructions(val gameView: GameView, var stage: Stage.Identifier, var showLeaveDialogue: Boolean,
                   var callback: (()->Unit)? ): Fadable
{
    var alpha = 0
    val resources: Resources = gameView.resources

    private var funFact = if (Random.nextFloat() > 0.3)
        resources.getString(R.string.instr_did_you_know) + "\n\n" +
        resources.getStringArray(R.array.fun_fact).random()
    else ""

    private fun instructionText(level: Int): String
    {
        if (gameView.intermezzo.type in setOf(Intermezzo.Type.GAME_LOST, Intermezzo.Type.GAME_WON))
            return ""
        else if (showLeaveDialogue)
            when (gameView.intermezzo.durationOfLeave)
            {
                1 -> return resources.getString(R.string.instr_leave_1)
                else -> return resources.getString(R.string.instr_leave).format(gameView.intermezzo.durationOfLeave)
            }
        else if (stage.series == GameMechanics.SERIES_NORMAL) {
            return when (level) {
                1 -> resources.getString(R.string.instr_1)
                2 -> resources.getString(R.string.instr_2)
                3 -> resources.getString(R.string.instr_3)
                4 -> resources.getString(R.string.instr_4)
                5 -> resources.getString(R.string.instr_5)
                6 -> resources.getString(R.string.instr_6)
                7 -> resources.getString(R.string.instr_7)
                8 -> resources.getString(R.string.instr_7a)
                9 -> resources.getString(R.string.instr_8)
                14 -> resources.getString(R.string.instr_9)
                20 -> resources.getString(R.string.instr_10)
                23 -> resources.getString(R.string.instr_12)
                24 -> resources.getString(R.string.instr_16)
                10 -> resources.getString(R.string.instr_11)
                21 -> resources.getString(R.string.instr_13)
                27 -> resources.getString(R.string.instr_14)
                28 -> resources.getString(R.string.instr_15).format(GameMechanics.temperatureLimit)
                30 -> resources.getString(R.string.instr_17)
                31 -> resources.getString(R.string.instr_18)
                32 -> resources.getString(R.string.instr_23)
                else -> ""
            }
        }
        else if (stage.series == GameMechanics.SERIES_TURBO) {
            return when (level) {
                1 -> resources.getString(R.string.instr_2_1)
                else -> ""
            }
        }
        else if (stage.series == GameMechanics.SERIES_ENDLESS)
        {
            return when (level) {
                1 -> resources.getString(R.string.instr_endless)
                else -> funFact
            }
        }
        else
            return ""
    }

    init { Fader(gameView, this, Fader.Type.APPEAR, Fader.Speed.SLOW) }

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
        textPaint.textSize = GameMechanics.instructionTextSize * resources.displayMetrics.scaledDensity
        textPaint.color =
            if (showLeaveDialogue) resources.getColor(R.color.text_green)
            else Color.WHITE
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