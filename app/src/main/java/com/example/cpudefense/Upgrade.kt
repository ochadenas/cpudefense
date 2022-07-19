package com.example.cpudefense

import android.graphics.*
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader
import kotlin.math.sqrt

class Upgrade(var game: Game, type: Type): Fadable {

    enum class Type { INCREASE_CHIP_SUB_SPEED, INCREASE_CHIP_SHIFT_SPEED, INCREASE_CHIP_ACC_SPEED,
        DECREASE_ATT_FREQ ,
        INCREASE_STARTING_CASH, GAIN_CASH, DECREASE_UPGRADE_COST, ADDITIONAL_LIVES}
    data class Data (
        val type: Type,
        var level: Int = 0,
        var coinsSpent: Int = 0
    )
    var data = Data(type = type)

    enum class GraphicalState { NORMAL, TRANSIENT_LEVEL_0, TRANSIENT }  // used for different graphical effects
    private var graphicalState = GraphicalState.NORMAL
    private var transition = 0.0f

    var areaOnScreen = Rect(0, 0, Game.cardWidth, Game.cardHeight)
    private var myBitmap: Bitmap? = null
    private var effectBitmap = BitmapFactory.decodeResource(game.resources, R.drawable.glow)
    private var paintRect = Paint()
    private var shortDescRect = Rect(areaOnScreen)
    private var paintText = Paint()
    private var shortDesc: String = "effect description"
    private var strengthDesc: String = "format string"
    private var upgradeDesc: String = " -> next level"
    private var hero: Hero = Hero(type)
    private var heroOpacity = 0f

    var inactiveColor = game.resources.getColor(R.color.upgrade_inactive)
    var activeColor: Int = when(type)
    {
        Type.INCREASE_STARTING_CASH -> game.resources.getColor(R.color.upgrade_active_eco)
        Type.DECREASE_UPGRADE_COST -> game.resources.getColor(R.color.upgrade_active_eco)
        Type.INCREASE_CHIP_SUB_SPEED -> game.resources.getColor(R.color.upgrade_active_chip_dec)
        Type.INCREASE_CHIP_SHIFT_SPEED -> game.resources.getColor(R.color.upgrade_active_chip_shr)
        Type.ADDITIONAL_LIVES -> game.resources.getColor(R.color.upgrade_active_eco)
        Type.INCREASE_CHIP_ACC_SPEED -> game.resources.getColor(R.color.upgrade_active_chip_acc)
        Type.DECREASE_ATT_FREQ -> game.resources.getColor(R.color.upgrade_active_general)
        Type.GAIN_CASH -> game.resources.getColor(R.color.upgrade_active_eco)
    }
    var maxLevel = 7   // cannot upgrade beyond this level

    init {
        paintRect.style = Paint.Style.STROKE
        paintRect.strokeWidth = 2f
        paintText.color = Color.WHITE
        paintText.textSize = 24f
        paintText.style = Paint.Style.FILL
        shortDescRect.top = shortDescRect.bottom - 50
        heroOpacity = when (data.level) { 0 -> 0f else -> 1f}
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

        // display hero picture
        var paintHero = Paint()
        paintHero.alpha = (255f * heroOpacity).toInt()
        var heroArea = Rect(0, 0, heroPictureSize, heroPictureSize)
        heroArea.setCenter(areaOnScreen.center())
        hero.picture?.let { canvas.drawBitmap(it, null, heroArea, paintHero) }

        displayFrame(canvas)
    }

    fun displayFrame(canvas: Canvas)
    {
        if (graphicalState == GraphicalState.TRANSIENT)
        {
            // draw animation
            var thickness = transition
            if (transition > 0.5)
                thickness = 1.0f - transition
            paintRect.strokeWidth = 2f + 10 * thickness
            canvas.drawRect(areaOnScreen, paintRect)
        }
        else if (graphicalState == GraphicalState.TRANSIENT_LEVEL_0)
        {
            // draw animation for initial activation of the upgrade
            var paintInactive = Paint(paintRect)
            paintInactive.color = inactiveColor
            canvas.drawRect(areaOnScreen, paintInactive)
            displayLine(canvas, areaOnScreen.left, areaOnScreen.top, areaOnScreen.left, areaOnScreen.bottom)
            displayLine(canvas, areaOnScreen.right, areaOnScreen.bottom, areaOnScreen.right, areaOnScreen.top)
            displayLine(canvas, areaOnScreen.right, areaOnScreen.top, areaOnScreen.left, areaOnScreen.top)
            displayLine(canvas, areaOnScreen.left, areaOnScreen.bottom, areaOnScreen.right, areaOnScreen.bottom)
            // let hero picture appear
            heroOpacity = transition
        }
        else
            canvas.drawRect(areaOnScreen, paintRect)
    }

    fun displayLine(canvas: Canvas, x0: Int, y0: Int, x1: Int, y1: Int)
    // draws a fraction of the line between x0,y0 and x1,y1
    {
        var x: Float = x0 * (1-transition) + x1 * transition
        var y = y0 * (1-transition) + y1 * transition
        canvas.drawLine(x0.toFloat(), y0.toFloat(), x, y, paintRect)
        // draw the glow effect
        var effectRect = Rect(0, 0, effectBitmap.width, effectBitmap.height)
        effectRect.setCenter(x.toInt(), y.toInt())
        canvas.drawBitmap(effectBitmap, null, effectRect, paintText)
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

        // draw hero
        var margin = 10
        var heroPaintText = Paint(paintText)
        heroPaintText.color = if (data.level == 0) inactiveColor else activeColor
        var heroTextRect = Rect(0, margin, Game.cardWidth, margin+40)
        heroTextRect.displayTextCenteredInRect(canvas, hero.fullName, heroPaintText)

        addLevelDecoration(canvas)

        return bitmap
    }

    private fun addLevelDecoration(canvas: Canvas)
    {
        if (data.level == 0)
            return
        val levelText = "%d".format(data.level)
        val bounds = Rect()
        var paintDeco = Paint(paintText)
        paintDeco.color = activeColor
        paintText.getTextBounds(levelText, 0, levelText.length, bounds)
        canvas.drawText(levelText, canvas.width - bounds.width() - 10f, bounds.height() + 10f, paintDeco)
    }

    fun setDesc()
    {
        val strength = getStrength(data.level)
        val next = getStrength(data.level+1)
        when (data.type)
        {
            Type.INCREASE_CHIP_SUB_SPEED -> {
                shortDesc = "SUB chip speed"
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " -> x %.2f".format(next)
            }
            Type.INCREASE_STARTING_CASH ->
            {
                shortDesc = "Starting information"
                strengthDesc = "%d bits".format(strength.toInt())
                upgradeDesc = " -> %d bits".format(next.toInt())
            }
            Type.INCREASE_CHIP_SHIFT_SPEED ->             {
                shortDesc = "SHR chip speed"
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " -> x %.2f".format(next)
            }
            Type.INCREASE_CHIP_ACC_SPEED -> {
                shortDesc = "ACC chip speed"
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " -> x %.2f".format(next)
            }
            Type.DECREASE_UPGRADE_COST ->             {
                shortDesc = "Chip upgrade cost"
                strengthDesc = "-%d%%".format(strength.toInt())
                upgradeDesc = " -> -%d%%".format(next.toInt())
            }
            Type.ADDITIONAL_LIVES -> {
                shortDesc = "Additional lives"
                strengthDesc = "%d".format(strength.toInt())
                upgradeDesc = " -> %d".format(next.toInt())
                maxLevel = 3
            }
            Type.DECREASE_ATT_FREQ -> {
                shortDesc = "Attacker frequency"
                strengthDesc = "%.2f".format(strength)
                upgradeDesc = " -> %.2f".format(next)
            }
            Type.GAIN_CASH ->
            {
                shortDesc = "Information gain"
                strengthDesc = "%d bits/sec".format(strength.toInt())
                upgradeDesc = " -> %d bits/sec".format(next.toInt())
            }
        }
        upgradeDesc = "%s  [cost: %d]".format(upgradeDesc, getPrice(data.level))
        if (data.level >= maxLevel)
            upgradeDesc = ""
        myBitmap = createBitmap()
    }

    fun getStrength(level: Int = data.level): Float
            /** determines the numerical effect ("strength") of the upgrade,
             * depending on its level
             */
    {
        when (data.type) {
            Type.INCREASE_CHIP_SUB_SPEED -> return 1.0f + level / 20f
            Type.INCREASE_STARTING_CASH -> return 8.0f + level * level
            Type.INCREASE_CHIP_SHIFT_SPEED -> return 1.0f + level / 20f
            Type.DECREASE_UPGRADE_COST -> return level * 5f
            Type.ADDITIONAL_LIVES -> return level.toFloat()
            Type.INCREASE_CHIP_ACC_SPEED -> return 1.0f + level / 20f
            Type.DECREASE_ATT_FREQ -> return 1.0f - level / 20f
            Type.GAIN_CASH -> return (10f - level)
        }
    }

    fun isAvailable(): Boolean
            /** function that evaluates certain restrictions on upgrades.
             * Some upgrades require others to reach a certain level, etc.
             */
    {
        when (data.type) {
            Type.DECREASE_UPGRADE_COST -> return (game.gameUpgrades[Type.INCREASE_STARTING_CASH]?.data?.level?: 0 >= 3)
            Type.ADDITIONAL_LIVES -> return (game.gameUpgrades[Type.DECREASE_UPGRADE_COST]?.data?.level?: 0 >= 3)
            Type.DECREASE_ATT_FREQ -> return (game.gameUpgrades[Type.INCREASE_CHIP_SHIFT_SPEED]?.data?.level?: 0 >= 3)
            Type.GAIN_CASH -> return (game.gameUpgrades[Type.INCREASE_STARTING_CASH]?.data?.level?: 0 >= 4)
            Type.INCREASE_CHIP_ACC_SPEED -> return false
            else -> return true
        }
    }

    fun getPrice(level: Int): Int
    {
        return sqrt(level.toDouble()).toInt()+1
    }

    override fun fadeDone(type: Fader.Type) {
        graphicalState = GraphicalState.NORMAL
        heroOpacity = 1.0f
    }

    override fun setOpacity(opacity: Float)
    {
        transition = opacity
    }

    fun doUpgrade()
    {
        if (data.level >= maxLevel)
            return
        data.level += 1
        setDesc()
        game.gameActivity.saveUpgrades()
        game.gameUpgrades[this.data.type] = this
        // start graphical transition */
        if (data.level == 1) {
            Fader(game, this, Fader.Type.APPEAR, Fader.Speed.VERY_SLOW)
            graphicalState = GraphicalState.TRANSIENT_LEVEL_0
        }
        else {
            Fader(game, this, Fader.Type.APPEAR, Fader.Speed.MEDIUM)
            graphicalState = GraphicalState.TRANSIENT
        }
    }

    fun resetUpgrade()
    {
        data.level = 0
        data.coinsSpent = 0
        heroOpacity = 0f
        setDesc()
    }

    companion object {
        fun createFromData(game: Game, data: Data): Upgrade
                /** reconstruct a Link object based on the saved data
                 * and set all inner proprieties
                 */
        {
            val newInstance = Upgrade(game, data.type)
            newInstance.data.level = data.level
            newInstance.hero.setType()
            newInstance.heroOpacity = when (data.level) { 0 -> 0f else -> 1f}
            newInstance.setDesc()
            return newInstance
        }

        const val heroPictureSize = 120
    }

    inner class Hero(var type: Type)
    {
        var name = ""
        var fullName = ""
        var picture: Bitmap? = null
        var vitae = ""

        init { }

        fun setType()
        {
            when (type)
            {
                Type.INCREASE_CHIP_SUB_SPEED ->
                {
                    name = "Turing"
                    fullName = "Alan Turing"
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.turing)
                }
                Type.INCREASE_CHIP_SHIFT_SPEED ->
                {
                    name = "Lovelace"
                    fullName = "Ada Lovelace"
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.lovelace)
                }
                Type.INCREASE_CHIP_ACC_SPEED ->
                {
                    name = "Knuth"
                    fullName = "Donald E. Knuth"
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.knuth)
                }
                Type.INCREASE_STARTING_CASH ->
                {
                    name = "Hollerith"
                    fullName = "Herman Hollerith"
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.hollerith)
                }
                Type.DECREASE_UPGRADE_COST ->
                {
                    name = "Osborne"
                    fullName = "Adam Osborne"
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.osborne)
                }
                Type.ADDITIONAL_LIVES ->
                {
                    name = "Zuse"
                    fullName = "Konrad Zuse"
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.zuse)
                }
                Type.DECREASE_ATT_FREQ ->
                {
                    name = "LHC"
                    fullName = "Les Horribles Cernettes"
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.cernettes)
                }
                Type.GAIN_CASH ->
                {
                    name = "Franke"
                    fullName = "Herbert W. Franke"
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.cernettes)
                }
                else ->
                {
                    name = "Vaughan"
                    fullName = "Dorothy Vaughan"
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.vaughan)
                }
            }
        }

    }

}