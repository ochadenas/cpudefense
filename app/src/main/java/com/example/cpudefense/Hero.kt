package com.example.cpudefense

import android.graphics.*
import android.graphics.Bitmap.createBitmap
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.utils.center
import com.example.cpudefense.utils.displayTextCenteredInRect
import com.example.cpudefense.utils.setCenter
import com.example.cpudefense.utils.setTopLeft

class Hero(var game: Game, type: Type): Fadable {
    /*
    potential Heroes for versions to come:
    - John von Neuman?
    - Babbage?
    - Berners-Lee?
    - Torvalds
    - Sid Meier
    - Leibniz
    - Baudot
    - Chappe (?)
    - Jack St. Clair Kilby
    - John Conway
     */

    enum class Type { INCREASE_CHIP_SUB_SPEED, INCREASE_CHIP_SUB_RANGE,
        INCREASE_CHIP_SHR_SPEED,  INCREASE_CHIP_SHR_RANGE,
        INCREASE_CHIP_MEM_SPEED,  INCREASE_CHIP_MEM_RANGE,
        DECREASE_ATT_FREQ, DECREASE_ATT_SPEED, DECREASE_ATT_STRENGTH,
        ADDITIONAL_LIVES, INCREASE_MAX_HERO_LEVEL,
        INCREASE_STARTING_CASH, GAIN_CASH,
        DECREASE_UPGRADE_COST, INCREASE_REFUND, GAIN_CASH_ON_KILL}
    data class Data (
        val type: Type,
        var level: Int = 0,
        var coinsSpent: Int = 0
    )
    var data = Data(type = type)

    enum class GraphicalState { NORMAL, TRANSIENT_LEVEL_0, TRANSIENT }  // used for different graphical effects
    private var graphicalState = GraphicalState.NORMAL
    private var transition = 0.0f

    var areaOnScreen = Rect(0, 0, (Game.cardWidth*game.resources.displayMetrics.scaledDensity).toInt(), (Game.cardHeight*game.resources.displayMetrics.scaledDensity).toInt())
    var heroArea = Rect(0, 0, (heroPictureSize*game.resources.displayMetrics.scaledDensity).toInt(), (heroPictureSize*game.resources.displayMetrics.scaledDensity).toInt())
    private var myBitmap: Bitmap? = null
    private var effectBitmap = BitmapFactory.decodeResource(game.resources, R.drawable.glow)
    private var paintRect = Paint()
    private var paintInactive = Paint()
    private var paintIndicator = Paint()
    private var paintBiography = TextPaint()
    private var shortDescRect = Rect(areaOnScreen)
    private var paintText = Paint()
    private val paintHero = Paint()
    private var shortDesc: String = "effect description"
    private var strengthDesc: String = "format string"
    private var upgradeDesc: String = " → next level"
    private var costDesc: String = "[cost: ]"
    private var hero: Hero = Hero(type)
    var heroOpacity = 0f
    private var levelIndicator = mutableListOf<Rect>()
    private var indicatorSize = heroArea.width() / 10

    var inactiveColor = game.resources.getColor(R.color.upgrade_inactive)
    var activeColor: Int = when(type)
    {
        Type.INCREASE_CHIP_SUB_SPEED -> game.resources.getColor(R.color.upgrade_active_chip_sub)
        Type.INCREASE_CHIP_SUB_RANGE -> game.resources.getColor(R.color.upgrade_active_chip_sub)
        Type.INCREASE_CHIP_SHR_SPEED -> game.resources.getColor(R.color.upgrade_active_chip_shr)
        Type.INCREASE_CHIP_SHR_RANGE -> game.resources.getColor(R.color.upgrade_active_chip_shr)
        Type.INCREASE_CHIP_MEM_SPEED -> game.resources.getColor(R.color.upgrade_active_chip_mem)
        Type.INCREASE_CHIP_MEM_RANGE -> game.resources.getColor(R.color.upgrade_active_chip_mem)
        Type.DECREASE_ATT_FREQ -> game.resources.getColor(R.color.upgrade_active_general)
        Type.DECREASE_ATT_SPEED -> game.resources.getColor(R.color.upgrade_active_general)
        Type.DECREASE_ATT_STRENGTH -> game.resources.getColor(R.color.upgrade_active_general)
        Type.ADDITIONAL_LIVES -> game.resources.getColor(R.color.upgrade_active_meta)
        Type.INCREASE_MAX_HERO_LEVEL -> game.resources.getColor(R.color.upgrade_active_meta)
        Type.INCREASE_STARTING_CASH -> game.resources.getColor(R.color.upgrade_active_eco)
        Type.GAIN_CASH -> game.resources.getColor(R.color.upgrade_active_eco)
        Type.GAIN_CASH_ON_KILL -> game.resources.getColor(R.color.upgrade_active_eco)
        Type.INCREASE_REFUND -> game.resources.getColor(R.color.upgrade_active_eco)
        Type.DECREASE_UPGRADE_COST -> game.resources.getColor(R.color.upgrade_active_eco)
    }

    var biography: Biography? = null
    var effect: String = ""
    var vitae: String = ""
    /** cannot upgrade beyond this level. This value can be modified for certain heroes,
     * or by the effect of Sid Meier
      */
    private var maxLevel = 7


    init {
        paintRect.style = Paint.Style.STROKE
        paintInactive = Paint(paintRect)
        paintInactive.color = inactiveColor
        paintText.color = Color.WHITE
        paintText.style = Paint.Style.FILL
        shortDescRect.top = shortDescRect.bottom - (50 * game.resources.displayMetrics.scaledDensity).toInt()
        heroOpacity = when (data.level) { 0 -> 0f else -> 1f}
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
        paintRect.strokeWidth *= game.resources.displayMetrics.scaledDensity
        myBitmap?.let { canvas.drawBitmap(it, null, areaOnScreen, paintRect) }

        // display hero picture
        // (this is put here because of fading)
        paintHero.alpha = (255f * heroOpacity).toInt()
        hero.picture?.let { canvas.drawBitmap(it, null, heroArea, paintHero) }

        displayFrame(canvas)
    }

    private fun displayFrame(canvas: Canvas)
    {
        when (graphicalState) {
            GraphicalState.TRANSIENT -> {
                // draw animation
                var thickness = transition
                if (transition > 0.5)
                    thickness = 1.0f - transition
                paintRect.strokeWidth = (2f + 10 * thickness)*game.resources.displayMetrics.scaledDensity
                canvas.drawRect(areaOnScreen, paintRect)
            }
            GraphicalState.TRANSIENT_LEVEL_0 -> {
                // draw animation for initial activation of the upgrade
                canvas.drawRect(areaOnScreen, paintInactive)
                displayLine(canvas, areaOnScreen.left, areaOnScreen.top, areaOnScreen.left, areaOnScreen.bottom)
                displayLine(canvas, areaOnScreen.right, areaOnScreen.bottom, areaOnScreen.right, areaOnScreen.top)
                displayLine(canvas, areaOnScreen.right, areaOnScreen.top, areaOnScreen.left, areaOnScreen.top)
                displayLine(canvas, areaOnScreen.left, areaOnScreen.bottom, areaOnScreen.right, areaOnScreen.bottom)
                // let hero picture appear
                heroOpacity = transition
            }
            else -> canvas.drawRect(areaOnScreen, paintRect)
        }
    }

    fun displayHighlightFrame(canvas: Canvas)
    {
        // if (graphicalState != GraphicalState.TRANSIENT_LEVEL_0) {
            with (paintInactive)
            {
                val originalThickness = strokeWidth
                val originalAlpha = alpha
                alpha = 60
                strokeWidth = originalThickness + 12 * game.resources.displayMetrics.scaledDensity
                canvas.drawRect(areaOnScreen, this)
                alpha = 60
                strokeWidth = originalThickness + 6 * game.resources.displayMetrics.scaledDensity
                canvas.drawRect(areaOnScreen, this)
                // restore original values
                strokeWidth = originalThickness
                alpha = originalAlpha
            }
        // }
    }

    private fun displayLine(canvas: Canvas, x0: Int, y0: Int, x1: Int, y1: Int)
    // draws a fraction of the line between x0,y0 and x1,y1
    {
        val x: Float = x0 * (1-transition) + x1 * transition
        val y = y0 * (1-transition) + y1 * transition
        canvas.drawLine(x0.toFloat(), y0.toFloat(), x, y, paintRect)
        // draw the glow effect
        val effectRect = Rect(0, 0, effectBitmap.width, effectBitmap.height)
        effectRect.setCenter(x.toInt(), y.toInt())
        canvas.drawBitmap(effectBitmap, null, effectRect, paintText)
    }

    fun setSize()
    {
        var centre = areaOnScreen.center() // remember the former screen position, if given
        areaOnScreen = Rect(0, 0, (Game.cardWidth*game.resources.displayMetrics.scaledDensity).toInt(), (Game.cardHeight*game.resources.displayMetrics.scaledDensity).toInt())
        areaOnScreen.setCenter(centre)
        centre = heroArea.center()
        heroArea = Rect(0, 0, (heroPictureSize*game.resources.displayMetrics.scaledDensity).toInt(), (heroPictureSize*game.resources.displayMetrics.scaledDensity).toInt())
        heroArea.setCenter(centre)
        paintText.textSize = (Game.biographyTextSize - 2) * game.resources.displayMetrics.scaledDensity
        indicatorSize = heroArea.width() / 10
    }
    fun createBitmap()
    /** re-creates the bitmap without border, using a canvas positioned at (0, 0) */
    {
        val bitmap = createBitmap( areaOnScreen.width(), areaOnScreen.height(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // render text used to indicate the effect of the upgrade, and calculate its position
        val marginHorizontal = 10f*game.resources.displayMetrics.scaledDensity
        val marginVertical = 10f*game.resources.displayMetrics.scaledDensity
        val baseline = bitmap.height-marginVertical
        val paintUpdate = Paint(paintText)
        canvas.drawText(strengthDesc, marginHorizontal, baseline, paintText)
        val bounds = Rect()
        paintText.getTextBounds(strengthDesc, 0, strengthDesc.length, bounds)
        canvas.drawText(shortDesc, marginHorizontal, baseline - bounds.height() - marginVertical, paintText)
        paintUpdate.color = game.resources.getColor(R.color.upgrade_inactive)
        canvas.drawText(upgradeDesc, bounds.right + marginHorizontal, baseline, paintUpdate)

        // draw hero
        val margin = (10*game.resources.displayMetrics.scaledDensity).toInt()
        val heroPaintText = Paint(paintText)
        heroPaintText.color = if (data.level == 0) inactiveColor else activeColor
        val heroTextRect = Rect(0, margin, areaOnScreen.width(), margin+40)
        heroTextRect.displayTextCenteredInRect(canvas, hero.fullName, heroPaintText)

        addLevelDecoration(canvas)
        myBitmap = bitmap
    }

    fun createBiography(area: Rect)
    {
        if (biography == null)
            biography = Biography(Rect(0,0,area.width(), area.height()))
        biography?.createBiography(this)

    }

    private fun addLevelDecoration(canvas: Canvas)
    {
        paintIndicator.color = if (data.level == 0) inactiveColor else activeColor
        var verticalIndicatorSize = indicatorSize  // squeeze when max level is greater than 8
        if (getMaxUpgradeLevel()>8)
            verticalIndicatorSize = heroArea.height() / (5+getMaxUpgradeLevel())
        for (i in 1 .. getMaxUpgradeLevel())
        {
            val rect = Rect(0,0, indicatorSize, verticalIndicatorSize)
            rect.setTopLeft(0, (2*i-1)*verticalIndicatorSize)
            levelIndicator.add(rect)
            paintIndicator.style = if (i<=data.level) Paint.Style.FILL else Paint.Style.STROKE
            canvas.drawRect(rect, paintIndicator)
        }
        return
    }

    fun setDesc()
    {
        val strength = getStrength(data.level)
        val next = getStrength(data.level+1)
        when (data.type)
        {
            Type.INCREASE_CHIP_SUB_SPEED -> {
                shortDesc = game.resources.getString(R.string.shortdesc_SUB)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → x %.2f".format(next)
            }
            Type.INCREASE_STARTING_CASH ->
            {
                shortDesc = game.resources.getString(R.string.shortdesc_startinfo)
                strengthDesc = "%d bits".format(strength.toInt())
                upgradeDesc = " → %d bits".format(next.toInt())
            }
            Type.INCREASE_CHIP_SHR_SPEED ->             {
                shortDesc = game.resources.getString(R.string.shortdesc_SHR)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.INCREASE_CHIP_MEM_SPEED -> {
                shortDesc = game.resources.getString(R.string.shortdesc_MEM)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.DECREASE_UPGRADE_COST ->             {
                shortDesc = game.resources.getString(R.string.shortdesc_upgrade)
                strengthDesc = "-%d%%".format(strength.toInt())
                upgradeDesc = " → -%d%%".format(next.toInt())
            }
            Type.ADDITIONAL_LIVES -> {
                shortDesc = game.resources.getString(R.string.shortdesc_lives)
                strengthDesc = "%d".format(strength.toInt())
                upgradeDesc = " → %d".format(next.toInt())
                maxLevel = 3
            }
            Type.DECREASE_ATT_FREQ -> {
                shortDesc = game.resources.getString(R.string.shortdesc_frequency)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.DECREASE_ATT_SPEED -> {
                shortDesc = game.resources.getString(R.string.shortdesc_att_speed)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.INCREASE_MAX_HERO_LEVEL ->
            {
                shortDesc = game.resources.getString(R.string.shortdesc_max_hero_upgrade)
                strengthDesc = "+%d".format(strength.toInt())
                upgradeDesc = " → +%d".format(next.toInt())
                maxLevel = 3
            }
            Type.GAIN_CASH ->
            {
                shortDesc = game.resources.getString(R.string.shortdesc_info_gain)
                strengthDesc = "1 bit/%d ticks".format(strength.toInt())
                upgradeDesc = " → 1/%d ticks".format(next.toInt())
            }
            Type.GAIN_CASH_ON_KILL ->
            {
                shortDesc = game.resources.getString(R.string.shortdesc_info_on_kill)
                strengthDesc = "%d bit/kill".format(strength.toInt())
                upgradeDesc = " → %d bit/kill".format(next.toInt())
            }
            Type.INCREASE_REFUND ->
            {
                shortDesc = game.resources.getString(R.string.shortdesc_refund)
                strengthDesc = "%d%%".format(strength.toInt())
                upgradeDesc = " → %d%%".format(next.toInt())
                maxLevel = 6  // even at level 6, refund is more than 100%
            }
            Type.INCREASE_CHIP_SUB_RANGE ->
            {
                shortDesc = game.resources.getString(R.string.shortdesc_range)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }  
            Type.INCREASE_CHIP_SHR_RANGE ->
            {
                shortDesc = game.resources.getString(R.string.shortdesc_range)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.INCREASE_CHIP_MEM_RANGE ->
            {
                shortDesc = game.resources.getString(R.string.shortdesc_range)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.DECREASE_ATT_STRENGTH -> {
                shortDesc = game.resources.getString(R.string.shortdesc_att_strength)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
        }
        val cost = getPrice(data.level)
        costDesc = game.resources.getString(R.string.cost_desc).format(cost)
        if (data.level >= getMaxUpgradeLevel()) {
            upgradeDesc = ""
            costDesc = ""
        }
    }

    fun getStrength(level: Int = data.level): Float
            /** determines the numerical effect ("strength") of the upgrade,
             * depending on its level
             */
    {
        when (data.type) {
            Type.INCREASE_CHIP_SUB_SPEED -> return 1.0f + level / 20f
            Type.INCREASE_CHIP_SHR_SPEED -> return 1.0f + level / 20f
            Type.INCREASE_CHIP_MEM_SPEED -> return 1.0f + level / 20f
            Type.INCREASE_STARTING_CASH -> return 8.0f + level * level
            Type.DECREASE_UPGRADE_COST -> return level * 5f
            Type.ADDITIONAL_LIVES -> return level.toFloat()
            Type.DECREASE_ATT_FREQ -> return 1.0f - level * 0.05f
            Type.DECREASE_ATT_SPEED -> return 1.0f - level * 0.04f
            Type.DECREASE_ATT_STRENGTH -> return 1.0f - level * 0.1f
            Type.INCREASE_MAX_HERO_LEVEL -> return level.toFloat()
            Type.GAIN_CASH -> return (8f - level) * 9
            Type.GAIN_CASH_ON_KILL -> return level * 0.5f
            Type.INCREASE_REFUND -> return (50f + level * 10)
            Type.INCREASE_CHIP_SUB_RANGE -> return 1.0f + level / 10f
            Type.INCREASE_CHIP_SHR_RANGE -> return 1.0f + level / 10f
            Type.INCREASE_CHIP_MEM_RANGE -> return 1.0f + level / 10f
        }
    }

    fun getMaxUpgradeLevel(): Int
            /** @return The maximal allowed upgrade level for this hero,
             * taking into account the type of the card and
             * the possible effect of Sid Meier
             */
    {
        val additionalUpgradePossibility = game.heroes[Type.INCREASE_MAX_HERO_LEVEL]?.getStrength()?.toInt()
        when (data.type)
        {
            Type.ADDITIONAL_LIVES -> return maxLevel
            Type.INCREASE_MAX_HERO_LEVEL -> return maxLevel
            Type.GAIN_CASH -> return maxLevel
            else -> return maxLevel + (additionalUpgradePossibility ?: 0)
        }
    }

    private fun upgradeLevel(type: Type): Int
    {
        val level = game.heroes[type]?.data?.level
        return level ?: 0
    }

    fun isAvailable(stageIdentifier: Stage.Identifier): Boolean
            /** function that evaluates certain restrictions on upgrades.
             * Some upgrades require others to reach a certain level, etc.
             * @param stageIdentifier cards may depend on the stage and/or series
             */
    {
        if (stageIdentifier.series > 1)  // restrictions only apply for series 1
            return true
        return when (data.type) {
            Type.INCREASE_MAX_HERO_LEVEL -> upgradeLevel(Type.ADDITIONAL_LIVES) >= 3
            Type.DECREASE_ATT_STRENGTH ->   upgradeLevel(Type.DECREASE_ATT_SPEED) >= 3
            Type.DECREASE_ATT_SPEED ->      upgradeLevel(Type.DECREASE_ATT_FREQ) >= 3
            Type.ADDITIONAL_LIVES ->        upgradeLevel(Type.DECREASE_ATT_SPEED) >= 5
            Type.DECREASE_ATT_FREQ ->       upgradeLevel(Type.INCREASE_CHIP_SHR_SPEED) >= 3
            Type.GAIN_CASH_ON_KILL ->       upgradeLevel(Type.INCREASE_REFUND) >= 3
            Type.INCREASE_REFUND ->         upgradeLevel(Type.DECREASE_UPGRADE_COST) >= 3
            Type.DECREASE_UPGRADE_COST ->   upgradeLevel(Type.GAIN_CASH) >= 3
            Type.GAIN_CASH ->               upgradeLevel(Type.INCREASE_STARTING_CASH) >= 3
            Type.INCREASE_CHIP_MEM_SPEED -> stageIdentifier.number >= 14
            Type.INCREASE_CHIP_SUB_RANGE -> upgradeLevel(Type.INCREASE_CHIP_SUB_SPEED) >= 5
            Type.INCREASE_CHIP_SHR_RANGE -> upgradeLevel(Type.INCREASE_CHIP_SHR_SPEED) >= 5
            Type.INCREASE_CHIP_MEM_RANGE -> upgradeLevel(Type.INCREASE_CHIP_MEM_SPEED) >= 5
            else -> true
        }
    }

    fun getPrice(level: Int): Int
    /**
     * Cost of next hero upgrade.
     * @param level The current level of the hero
     * @return the cost (in coins) for reaching the next level
     */
    {
        return if (level == 0) 1 else level
    }

    fun upgradeInfo(): String
    /** displays a text with info on the next available upgrade */
    {
        return "%s %s\n%s %s".format(shortDesc, strengthDesc, upgradeDesc, costDesc)
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
        if (data.level >= getMaxUpgradeLevel())
            return
        data.level += 1
        setDesc()
        game.heroes[this.data.type] = this
        game.gameActivity.saveUpgrades()
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
        fun createFromData(game: Game, data: Data): com.example.cpudefense.Hero
                /** reconstruct a Hero object based on the saved data
                 * and set all inner proprieties
                 */
        {
            val newInstance = Hero(game, data.type)
            newInstance.data.level = data.level
            newInstance.data.coinsSpent = data.coinsSpent
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

        fun setType()
        {
            when (type)
            {
                Type.INCREASE_CHIP_SUB_SPEED ->
                {
                    name = "Turing"
                    fullName = "Alan Turing"
                    effect = game.resources.getString(R.string.HERO_EFFECT_CHIPSPEED).format("SUB")
                    vitae = game.resources.getString(R.string.turing)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.turing)
                }
                Type.INCREASE_CHIP_SHR_SPEED ->
                {
                    name = "Lovelace"
                    fullName = "Ada Lovelace"
                    effect = game.resources.getString(R.string.HERO_EFFECT_CHIPSPEED).format("SHR")
                    vitae = game.resources.getString(R.string.lovelace)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.lovelace)
                }
                Type.INCREASE_CHIP_MEM_SPEED ->
                {
                    name = "Knuth"
                    fullName = "Donald E. Knuth"
                    effect = game.resources.getString(R.string.HERO_EFFECT_CHIPSPEED).format("MEM")
                    vitae = game.resources.getString(R.string.knuth)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.knuth)
                }
                Type.INCREASE_STARTING_CASH ->
                {
                    name = "Hollerith"
                    fullName = "Herman Hollerith"
                    effect = game.resources.getString(R.string.HERO_EFFECT_STARTINFO)
                    vitae = game.resources.getString(R.string.hollerith)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.hollerith)
                }
                Type.DECREASE_UPGRADE_COST ->
                {
                    name = "Osborne"
                    fullName = "Adam Osborne"
                    effect = game.resources.getString(R.string.HERO_EFFECT_UPGRADECOST)
                    vitae = game.resources.getString(R.string.osborne)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.osborne)
                }
                Type.ADDITIONAL_LIVES ->
                {
                    name = "Zuse"
                    fullName = "Konrad Zuse"
                    effect = game.resources.getString(R.string.HERO_EFFECT_LIVES)
                    vitae = game.resources.getString(R.string.zuse)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.zuse)
                }
                Type.DECREASE_ATT_FREQ ->
                {
                    name = "LHC"
                    fullName = "Les Horribles Cernettes"
                    effect = game.resources.getString(R.string.HERO_EFFECT_FREQUENCY)
                    vitae = game.resources.getString(R.string.cernettes)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.cernettes)
                }
                Type.GAIN_CASH ->
                {
                    name = "Franke"
                    fullName = "Herbert W. Franke"
                    effect = game.resources.getString(R.string.HERO_EFFECT_INFOOVERTIME)
                    vitae = game.resources.getString(R.string.franke)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.franke)
                }
                Type.GAIN_CASH_ON_KILL ->
                {
                    name = "Mandelbrot"
                    fullName = "Benoît B. Mandelbrot"
                    effect = game.resources.getString(R.string.HERO_EFFECT_GAININFO)
                    vitae = game.resources.getString(R.string.mandelbrot)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.mandelbrot)
                }
                Type.DECREASE_ATT_SPEED ->
                {
                    name = "Vaughan"
                    fullName = "Dorothy Vaughan"
                    effect = game.resources.getString(R.string.HERO_EFFECT_ATTSPEED)
                    vitae = game.resources.getString(R.string.vaughan)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.vaughan)
                }
                Type.DECREASE_ATT_STRENGTH ->
                {
                    name = "Schneier"
                    fullName = "Bruce Schneier"
                    effect = game.resources.getString(R.string.HERO_EFFECT_ATTSTRENGTH)
                    vitae = game.resources.getString(R.string.schneier)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.schneier)
                }
                Type.INCREASE_REFUND ->
                {
                    name = "Tramiel"
                    fullName = "Jack Tramiel"
                    effect = game.resources.getString(R.string.HERO_EFFECT_REFUNDPRICE)
                    vitae = game.resources.getString(R.string.tramiel)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.tramiel)
                }
                Type.INCREASE_CHIP_SUB_RANGE ->
                {
                    name = "Wiener"
                    fullName = "Norbert Wiener"
                    effect = game.resources.getString(R.string.HERO_EFFECT_RANGE).format("SUB")
                    vitae = game.resources.getString(R.string.wiener)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.wiener)
                }
                Type.INCREASE_CHIP_SHR_RANGE ->
                {
                    name = "Pascal"
                    fullName = "Blaise Pascal"
                    effect = game.resources.getString(R.string.HERO_EFFECT_RANGE).format("SHR")
                    vitae = game.resources.getString(R.string.pascal)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.pascal)
                }
                Type.INCREASE_CHIP_MEM_RANGE ->
                {
                    name = "Hopper"
                    fullName = "Grace Hopper"
                    effect = game.resources.getString(R.string.HERO_EFFECT_RANGE).format("MEM")
                    vitae = game.resources.getString(R.string.hopper)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.hopper)
                }
                Type.INCREASE_MAX_HERO_LEVEL ->
                {
                    name = "Meier"
                    fullName = "Sid Meier"
                    effect = game.resources.getString(R.string.HERO_EFFECT_MAXHEROUPGRADE)
                    vitae = game.resources.getString(R.string.meier)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.meier)
                }
            }
        }

    }

    inner class Biography(var myArea: Rect)
    {
        var bitmap: Bitmap = createBitmap(myArea.width(), myArea.height(), Bitmap.Config.ARGB_8888)
        private var canvas = Canvas(bitmap)

        fun createBiography(selected: com.example.cpudefense.Hero?)
        {
            val text: String
            if (data.level>0)
            {
                text = vitae
                paintBiography.color = selected?.activeColor ?: Color.WHITE
            }
            else
            {
                text = "%s\n\n%s".format(hero.fullName, effect)
                paintBiography.color = selected?.inactiveColor ?: Color.WHITE
            }



            canvas.drawColor(Color.BLACK)
            paintBiography.textSize = Game.biographyTextSize*game.resources.displayMetrics.scaledDensity
            paintBiography.alpha = 255
            val textLayout = StaticLayout(
                text, paintBiography, myArea.width(),
                Layout.Alignment.ALIGN_NORMAL,
                1.0f,
                0.0f,
                false
            )
            // if the text exceeds the area provided, enlarge the bitmap
            if (textLayout.height>this.bitmap.height)
            {
                this.bitmap = createBitmap(myArea.width(), textLayout.height, Bitmap.Config.ARGB_8888)
                canvas = Canvas(bitmap)
            }
            textLayout.draw(canvas)
        }
    }
}