package com.example.cpudefense

import android.graphics.*
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader
import kotlin.math.sqrt

class Upgrade(var game: Game, type: Type): Fadable {

    enum class Type { INCREASE_CHIP_SUB_SPEED, INCREASE_CHIP_SHIFT_SPEED, INCREASE_STARTING_CASH }
    data class Data (
        val type: Type,
        var level: Int = 0
    )

    var data = Data(type = type)
    var areaOnScreen = Rect(0, 0, Game.cardWidth, Game.cardHeight)
    private var myBitmap: Bitmap? = null
    private var paintRect = Paint()
    private var shortDescRect = Rect(areaOnScreen)
    private var paintText = Paint()
    private var shortDesc: String = "effect description"
    private var strengthDesc: String = "format string"
    private var upgradeDesc: String = " -> next level"
    var inactiveColor = game.resources.getColor(R.color.upgrade_inactive)
    var activeColor: Int = when(type)
    {
        Type.INCREASE_STARTING_CASH -> game.resources.getColor(R.color.upgrade_active_eco)
        Type.INCREASE_CHIP_SUB_SPEED -> game.resources.getColor(R.color.upgrade_active_chip_dec)
        Type.INCREASE_CHIP_SHIFT_SPEED -> game.resources.getColor(R.color.upgrade_active_chip_shr)
    }

    init {
        paintRect.style = Paint.Style.STROKE
        paintRect.strokeWidth = 2f
        paintText.color = Color.WHITE
        paintText.textSize = 24f
        paintText.style = Paint.Style.FILL
        shortDescRect.top = shortDescRect.bottom - 50
        setDesc()
    }


    fun display(canvas: Canvas)
    {
        if (data.level == 0)
        {
            paintRect.color = inactiveColor
            paintRect.strokeWidth = 2f
        }
        else
        {
            paintRect.color = activeColor
            paintRect.strokeWidth = 2f + data.level / 2
        }
        myBitmap?.let { canvas.drawBitmap(it, null, areaOnScreen, paintRect) }
        canvas.drawRect(areaOnScreen, paintRect)
    }

    private fun createBitmap(): Bitmap
    /** re-creates the bitmap without border, using a canvas positioned at (0, 0) */
    {
        val bitmap = Bitmap.createBitmap( Game.cardWidth, Game.cardHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // render text used to indicate the effect of the upgrade, and calculate its position
        val marginHorizontal = 10f
        val marginVertical = 10f
        val baseline = bitmap.height-marginVertical
        val paintUpdate = Paint(paintText)
        canvas.drawText(strengthDesc, marginHorizontal, baseline, paintText)
        val bounds = Rect()
        paintText.getTextBounds(strengthDesc, 0, strengthDesc.length, bounds)
        canvas.drawText(shortDesc, marginHorizontal, baseline - bounds.height() - marginVertical, paintText)
        paintUpdate.color = game.resources.getColor(R.color.upgrade_inactive)
        canvas.drawText(upgradeDesc, bounds.right + marginHorizontal, baseline, paintUpdate)

        addLevelDecoration(canvas)

        return bitmap
    }

    private fun addLevelDecoration(canvas: Canvas)
    {
        if (data.level == 0)
            return
        val levelText = "%d".format(data.level)
        val bounds = Rect()
        paintText.getTextBounds(levelText, 0, levelText.length, bounds)
        canvas.drawText(levelText, canvas.width - bounds.width() - 10f, bounds.height() + 10f, paintText)
    }

    fun setDesc()
    {
        val strength = getStrength(data.level)
        val next = getStrength(data.level+1)
        when (data.type)
        {
            Type.INCREASE_CHIP_SUB_SPEED -> {
                shortDesc = "DEC chip speed"
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " -> %.2f".format(next)
            }
            Type.INCREASE_STARTING_CASH ->
            {
                shortDesc = "Inf at start"
                strengthDesc = "%d bits".format(strength.toInt())
                upgradeDesc = " -> %d bits".format(next.toInt())
            }
            Type.INCREASE_CHIP_SHIFT_SPEED ->             {
                shortDesc = "SHR chip speed"
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " -> %.2f".format(next)
            }
        }
        upgradeDesc = "%s  [cost: %d]".format(upgradeDesc, getPrice(data.level))
        myBitmap = createBitmap()
    }

    fun getStrength(level: Int): Float
    {
        when (data.type) {
            Type.INCREASE_CHIP_SUB_SPEED -> return 5.0f + level / 100f
            Type.INCREASE_STARTING_CASH -> return 8.0f + level * level
            Type.INCREASE_CHIP_SHIFT_SPEED -> return 5.0f + level / 100f
        }
    }

    fun getStrength(): Float
    {
        return getStrength(data.level)
    }

    fun getPrice(level: Int): Int
    {
        return sqrt(level.toDouble()).toInt()+1
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

    fun doUpgrade()
    {
        data.level += 1
        setDesc()
        game.gameUpgrades[this.data.type] = this
        Fader(game, this, Fader.Type.APPEAR, Fader.Speed.MEDIUM)
    }

    companion object {
        fun createFromData(game: Game, data: Data): Upgrade
                /** reconstruct a Link object based on the saved data
                 * and set all inner proprieties
                 */
        {
            val newInstance = Upgrade(game, data.type)
            newInstance.data.level = data.level
            newInstance.setDesc()
            return newInstance
        }
    }

}