package com.example.cpudefense.upgrades

import android.graphics.*
import android.view.MotionEvent
import com.example.cpudefense.Game
import com.example.cpudefense.R
import com.example.cpudefense.displayTextCenteredInRect
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader

open class Upgrade(var game: Game, type: Type): Fadable {

    enum class Type { INCREASE_CHIP_SPEED, INCREASE_STARTING_CASH }
    data class Data (
        val type: Upgrade.Type,
        val level: Int = 0
    )

    var data = Data(type = type)
    var areaOnScreen = Rect(0, 0, Game.cardWidth, Game.cardHeight)
    private var myBitmap: Bitmap? = null
    private var paintRect = Paint()
    private var shortDescRect = Rect(areaOnScreen)
    private var paintText = Paint()
    open var shortDesc: String = ""

    init {
        paintRect.color = game.resources.getColor(R.color.card_inactive)
        paintRect.style = Paint.Style.STROKE
        paintRect.strokeWidth = 2f
        paintText.color = Color.WHITE
        paintText.textSize = 24f
        paintText.style = Paint.Style.FILL
        shortDescRect.top = shortDescRect.bottom - 50
    }


    fun display(canvas: Canvas)
    {
        myBitmap?.let { canvas.drawBitmap(it, null, areaOnScreen, paintRect) }
        canvas.drawRect(areaOnScreen, paintRect)
    }

    fun createBitmap(): Bitmap
    /** re-creates the bitmap without border, using a canvas positioned at (0, 0) */
    {
        var bitmap = Bitmap.createBitmap( Game.cardWidth, Game.cardHeight, Bitmap.Config.ARGB_8888)
        var canvas = Canvas(bitmap)
        shortDescRect.displayTextCenteredInRect(canvas, shortDesc, paintText)
        return bitmap
    }

    fun setDesc(short: String)
    {
        shortDesc = short
        myBitmap = createBitmap()
    }

    override fun fadeDone(type: Fader.Type) {

    }

    override fun setOpacity(opacity: Float)
    {
        var thickness = opacity
        if (opacity > 0.5)
            thickness = 1.0f - opacity
        paintRect.strokeWidth = 2f + 10 * thickness
    }

    fun onDown(event: MotionEvent): Boolean
    {
        if (areaOnScreen.contains(event.x.toInt(), event.y.toInt()))
        {
            Fader(game, this, Fader.Type.APPEAR, Fader.Speed.MEDIUM)
            return true
        }
        return false
    }

    

    companion object {
        fun createFromData(game: Game, data: Upgrade.Data): Upgrade?
                /** reconstruct a Link object based on the saved data
                 * and set all inner proprieties
                 */
        {
            lateinit var newInstance: Upgrade
            when (data.type)
            {
                Type.INCREASE_STARTING_CASH -> newInstance = IncreaseStartingCash(game)
                // Type.INCREASE_CHIP_SPEED -> newInstance =
                else -> newInstance = Upgrade(game, data.type)
            }
            return newInstance
        }
    }

}