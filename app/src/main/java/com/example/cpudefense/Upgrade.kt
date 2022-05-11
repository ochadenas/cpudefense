package com.example.cpudefense

import android.graphics.*
import android.view.MotionEvent
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader

class Upgrade(var game: Game, type: Type): Fadable {

    enum class Type { INCREASE_CHIP_SPEED, INCREASE_STARTING_CASH }
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
    var shortDesc: String = ""
    var strengthDesc: String = "formatString"
    var upgradeDesc: String = " -> next"

    init {
        paintRect.color = game.resources.getColor(R.color.card_inactive)
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
        myBitmap?.let { canvas.drawBitmap(it, null, areaOnScreen, paintRect) }
        canvas.drawRect(areaOnScreen, paintRect)
    }

    fun createBitmap(): Bitmap
    /** re-creates the bitmap without border, using a canvas positioned at (0, 0) */
    {
        var bitmap = Bitmap.createBitmap( Game.cardWidth, Game.cardHeight, Bitmap.Config.ARGB_8888)
        var canvas = Canvas(bitmap)

        // render text used to indicate the effect of the upgrade, and calculate its position
        val marginHorizontal = 10f
        val marginVertical = 10f
        val baseline = bitmap.height-marginVertical
        val paintUpdate = Paint(paintText)
        canvas.drawText(strengthDesc, marginHorizontal, baseline, paintText)
        var bounds = Rect()
        paintText.getTextBounds(strengthDesc, 0, strengthDesc.length, bounds)
        canvas.drawText(shortDesc, marginHorizontal, baseline - bounds.height() - marginVertical, paintText)
        paintUpdate.color = game.resources.getColor(R.color.card_inactive)
        canvas.drawText(upgradeDesc, bounds.right + marginHorizontal, baseline, paintUpdate)

        addLevelDecoration(canvas)

        return bitmap
    }

    fun addLevelDecoration(canvas: Canvas)
    {
        if (data.level == 0)
            return
        var levelText = "%d".format(data.level)
        var bounds = Rect()
        paintText.getTextBounds(levelText, 0, levelText.length, bounds)
        canvas.drawText(levelText, canvas.width - bounds.width() - 10f, bounds.height() + 10f, paintText)
    }

    fun setDesc()
    {
        val strength = getStrength(data.level)
        val next = getStrength(data.level+1)
        when (data.type)
        {
            Type.INCREASE_CHIP_SPEED -> {
                shortDesc = "Chip speed"
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " -> %.2f".format(next)
            }
            Type.INCREASE_STARTING_CASH ->
            {
                shortDesc = "Info at start"
                strengthDesc = "%d bits".format(strength.toInt())
                upgradeDesc = " -> %d bits".format(next.toInt())
            }
        }
        myBitmap = createBitmap()
    }

    fun getStrength(level: Int): Float
    {
        when (data.type) {
            Type.INCREASE_CHIP_SPEED -> return 1.0f + level / 100f
            Type.INCREASE_STARTING_CASH -> return 8.0f + level * level
        }
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
        if (areaOnScreen.contains(event.x.toInt(), event.y.toInt())) {
            doUpgrade()
            return true
        }
        return false
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
            var newInstance = Upgrade(game, data.type)
            newInstance.data.level = data.level
            newInstance.setDesc()
            return newInstance
        }
    }

}