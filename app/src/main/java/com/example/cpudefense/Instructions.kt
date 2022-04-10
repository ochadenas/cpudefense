package com.example.cpudefense

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader

class Instructions(val game: Game, var stage: Int, var callback: (()->Unit)? ): Fadable {
    var alpha = 0

    companion object {
        fun instructionText(level: Int): String
        {
            return when (level)
            {
                1 ->
                    "Your computer is being invaded by an evil virus. Rogue code fragments - shown as Ones and Zeroes - try to get through to the cpu.\n\n"+
                            "Tap on the empty slot to add a chip as defense against the attack, and then tap again to confirm your choice.\n\n"+
                            "The most basic chip is 'SUB 1' which subtracts 1 from the attacker's value: " +
                            "1's become 0's, and 0's disappear and are no longer a threat."

                2 ->
                            "You may also tap on an attacker. This will invert its value: 1 becomes 0, and 0 becomes 1.\n\nInverting a 0 will actually make the attacker "+
                                    "more dangerous, so be careful not to tap on everything you see.\n\n"+
                                    "Tapping on the cpu pauses the game."
                3 ->
                    "The currency in this game is information. It is displayed on the bottom left.\n\nDestroy the attacking code to gain some information about the virus. "+
                            "Use it to buy additional chips or to upgrade the existing ones.\n\n"
                4 -> "Remember that in binary, 01 equals 2, 11 equals 3 and so on."
                5 -> "In the next level, there is a new chip type 'SHR 1' available, meaning 'shift right by 1'.\n\n"+
                        "All digits are moved one position to the right. The leftmost digit is replaced by 0.\n\n"+
                        "For example, 0110 becomes 0011. This is equivalent to dividing the value by 2.\n\n"
                6 -> "You can also gain cryptocoins by clearing stages. There is a maximum amount of cryptocoins per level shown on the bottom, "+
                        "and each life lost will reduce the reward. "+
                        "If you do not get all the coins, you can always replay the level. However, once you have obtained the maximum, "+
                        "you won't receive any more coins for this stage.\n\n"+
                        "In addition, sometimes you will see a cryptocoin moving along with the attackers. You can collect it by destroying it, "+
                        "even multiple times when you replay the level.\n\n"+
                        "Cryptocoins can be exchanged for global, poweful upgrades. (not implemented yet)"
                else -> ""
            }
        }
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

        var text = instructionText(stage)
        val textPaint = TextPaint()
        textPaint.textSize = Game.instructionTextSize
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