@file:Suppress("DEPRECATION")

package com.example.cpudefense

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.utils.setTopLeft
import kotlin.random.Random

class Instructions(val gameView: GameView, var stage: Stage.Identifier, var showLeaveDialogue: Boolean,
                   private var callback: (()->Unit)? ): Fadable
{
    val margin = 32
    var myArea = Rect()
    var vertOffset = 0f

    var alpha = 0
    val resources: Resources = gameView.resources
    var paint = Paint()
    private var funFact = if (Random.nextFloat() > 0.3)
        resources.getString(R.string.instr_did_you_know) + "\n\n" +
        resources.getStringArray(R.array.fun_fact).random()
    else ""
    var bitmap: Bitmap = createBitmap(instructionText(stage.number), gameView.width-2*margin)

    fun setTextArea(rect: Rect)
    {
        myArea = Rect(rect.left+margin, 0+margin, rect.right-margin, rect.bottom-margin)
    }

    private fun instructionText(level: Int): String
    {
        if (gameView.intermezzo.type in setOf(Intermezzo.Type.GAME_LOST, Intermezzo.Type.GAME_WON))
            return ""
        else if (showLeaveDialogue)
            return ""
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
        paint.alpha = alpha
        val sourceRect = Rect(myArea).setTopLeft(0, vertOffset.toInt())
        canvas.drawBitmap(bitmap, sourceRect, myArea, paint)
    }

    private fun createBitmap(text: String, width: Int): Bitmap
    {
        val textPaint = TextPaint()
        textPaint.textSize = GameView.computerTextSize * gameView.textScaleFactor
        textPaint.typeface = Typeface.SANS_SERIF
        textPaint.color = Color.WHITE
        textPaint.alpha = 255
        val textLayout = StaticLayout(text, textPaint, width,
                                      Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)
        val bitmap = Bitmap.createBitmap(textLayout.width, textLayout.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        textLayout.draw(canvas)
        return bitmap
    }
}