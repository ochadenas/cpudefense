package com.example.cpudefense

import android.graphics.*
import android.graphics.Bitmap.createBitmap
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.utils.displayTextCenteredInRect
import com.example.cpudefense.utils.setCenter
import com.example.cpudefense.utils.setTopLeft

class Hero(var game: Game, type: Type): Fadable {
    /*
    Ideas:
    - increase radius
    - gain additional coins but disable reset of upgrades

    Heroes:
    - Norbert Wiener
    - John von Neuman
    - Pascal
    - Babbage?
    - Berners-Lee?
    - Mandelbrot
    - Torvalds
     */

    enum class Type { INCREASE_CHIP_SUB_SPEED, INCREASE_CHIP_SHIFT_SPEED, INCREASE_CHIP_MEM_SPEED, INCREASE_CHIP_ACC_SPEED,
        DECREASE_ATT_FREQ, DECREASE_ATT_SPEED,
        INCREASE_STARTING_CASH, GAIN_CASH, DECREASE_UPGRADE_COST, ADDITIONAL_LIVES, INCREASE_REFUND}
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
    var heroArea = Rect(0, 0, heroPictureSize, heroPictureSize)
    private var myBitmap: Bitmap? = null
    private var effectBitmap = BitmapFactory.decodeResource(game.resources, R.drawable.glow)
    private var paintRect = Paint()
    private var paintInactive = Paint()
    private var paintIndicator = Paint()
    private var paintBiography = TextPaint()
    private var shortDescRect = Rect(areaOnScreen)
    private var paintText = Paint()
    private var shortDesc: String = "effect description"
    private var strengthDesc: String = "format string"
    private var upgradeDesc: String = " -> next level"
    private var hero: Hero = Hero(type)
    var heroOpacity = 0f
    private var levelIndicator = mutableListOf<Rect>()
    private val indicatorSize = heroPictureSize / 10

    var inactiveColor = game.resources.getColor(R.color.upgrade_inactive)
    var activeColor: Int = when(type)
    {
        Type.INCREASE_STARTING_CASH -> game.resources.getColor(R.color.upgrade_active_eco)
        Type.DECREASE_UPGRADE_COST -> game.resources.getColor(R.color.upgrade_active_eco)
        Type.INCREASE_CHIP_SUB_SPEED -> game.resources.getColor(R.color.upgrade_active_chip_dec)
        Type.INCREASE_CHIP_SHIFT_SPEED -> game.resources.getColor(R.color.upgrade_active_chip_shr)
        Type.INCREASE_CHIP_MEM_SPEED -> game.resources.getColor(R.color.upgrade_active_chip_mem)
        Type.ADDITIONAL_LIVES -> game.resources.getColor(R.color.upgrade_active_eco)
        Type.INCREASE_CHIP_ACC_SPEED -> game.resources.getColor(R.color.upgrade_active_chip_acc)
        Type.DECREASE_ATT_FREQ -> game.resources.getColor(R.color.upgrade_active_general)
        Type.DECREASE_ATT_SPEED -> game.resources.getColor(R.color.upgrade_active_general)
        Type.GAIN_CASH -> game.resources.getColor(R.color.upgrade_active_eco)
        Type.INCREASE_REFUND -> game.resources.getColor(R.color.upgrade_active_eco)
    }
    var maxLevel = 7   // cannot upgrade beyond this level
    var biography: Biography? = null
    var effect: String = ""
    var vitae: String = ""

    init {
        paintRect.style = Paint.Style.STROKE
        paintRect.strokeWidth = 2f
        paintInactive = Paint(paintRect)
        paintInactive.color = inactiveColor
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
        val paintHero = Paint()
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
                paintRect.strokeWidth = 2f + 10 * thickness
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
                strokeWidth = originalThickness + 12
                canvas.drawRect(areaOnScreen, this)
                alpha = 60
                strokeWidth = originalThickness + 6
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

    private fun createBitmap(): Bitmap
    /** re-creates the bitmap without border, using a canvas positioned at (0, 0) */
    {
        val bitmap = createBitmap( Game.cardWidth, Game.cardHeight, Bitmap.Config.ARGB_8888)
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
        val margin = 10
        val heroPaintText = Paint(paintText)
        heroPaintText.color = if (data.level == 0) inactiveColor else activeColor
        val heroTextRect = Rect(0, margin, Game.cardWidth, margin+40)
        heroTextRect.displayTextCenteredInRect(canvas, hero.fullName, heroPaintText)

        addLevelDecoration(canvas)

        return bitmap
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
        for (i in 1 .. maxLevel)
        {
            val rect = Rect(0,0, indicatorSize, indicatorSize)
            rect.setTopLeft(0, (2*i-1)*indicatorSize)
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
                upgradeDesc = " -> x %.2f".format(next)
            }
            Type.INCREASE_STARTING_CASH ->
            {
                shortDesc = game.resources.getString(R.string.shortdesc_startinfo)
                strengthDesc = "%d bits".format(strength.toInt())
                upgradeDesc = " -> %d bits".format(next.toInt())
            }
            Type.INCREASE_CHIP_SHIFT_SPEED ->             {
                shortDesc = game.resources.getString(R.string.shortdesc_SHR)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " -> x %.2f".format(next)
            }
            Type.INCREASE_CHIP_ACC_SPEED -> {
                shortDesc = game.resources.getString(R.string.shortdesc_ACC)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " -> x %.2f".format(next)
            }
            Type.INCREASE_CHIP_MEM_SPEED -> {
                shortDesc = game.resources.getString(R.string.shortdesc_MEM)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " -> x %.2f".format(next)
            }
            Type.DECREASE_UPGRADE_COST ->             {
                shortDesc = game.resources.getString(R.string.shortdesc_upgrade)
                strengthDesc = "-%d%%".format(strength.toInt())
                upgradeDesc = " -> -%d%%".format(next.toInt())
            }
            Type.ADDITIONAL_LIVES -> {
                shortDesc = game.resources.getString(R.string.shortdesc_lives)
                strengthDesc = "%d".format(strength.toInt())
                upgradeDesc = " -> %d".format(next.toInt())
                maxLevel = 3
            }
            Type.DECREASE_ATT_FREQ -> {
                shortDesc = game.resources.getString(R.string.shortdesc_frequency)
                strengthDesc = "%.2f".format(strength)
                upgradeDesc = " -> %.2f".format(next)
            }
            Type.DECREASE_ATT_SPEED -> {
                shortDesc = game.resources.getString(R.string.shortdesc_att_speed)
                strengthDesc = "%.2f".format(strength)
                upgradeDesc = " -> %.2f".format(next)
            }
            Type.GAIN_CASH ->
            {
                shortDesc = game.resources.getString(R.string.shortdesc_info_gain)
                strengthDesc = "1 bit/%d ticks".format(strength.toInt())
                upgradeDesc = " -> 1/%d ticks".format(next.toInt())
            }
            Type.INCREASE_REFUND ->
            {
                shortDesc = game.resources.getString(R.string.shortdesc_refund)
                strengthDesc = "%d%%".format(strength.toInt())
                upgradeDesc = " -> %d%%".format(next.toInt())
            }
        }
        upgradeDesc = game.resources.getString(R.string.upgrade_format).format(upgradeDesc, getPrice(data.level))
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
            Type.INCREASE_CHIP_MEM_SPEED -> return 1.0f + level / 20f
            Type.DECREASE_UPGRADE_COST -> return level * 5f
            Type.ADDITIONAL_LIVES -> return level.toFloat()
            Type.INCREASE_CHIP_ACC_SPEED -> return 1.0f + level / 20f
            Type.DECREASE_ATT_FREQ -> return 1.0f - level * 0.05f
            Type.DECREASE_ATT_SPEED -> return 1.0f - level * 0.04f
            Type.GAIN_CASH -> return (8f - level) * 20
            Type.INCREASE_REFUND -> return (50f + level * 10)
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
            Type.DECREASE_ATT_SPEED -> return (game.gameUpgrades[Type.DECREASE_ATT_FREQ]?.data?.level?: 0 >= 3)
            Type.GAIN_CASH -> return (game.gameUpgrades[Type.INCREASE_STARTING_CASH]?.data?.level?: 0 >= 4)
            Type.INCREASE_CHIP_MEM_SPEED -> return (game.currentStage?.data?.level?:0 >= 9 )
            Type.INCREASE_CHIP_ACC_SPEED -> return false
            else -> return true
        }
    }

    fun getPrice(level: Int): Int
    {
        // return sqrt(level.toDouble()).toInt()+1
        return (level / 2 ) + 1
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
        fun createFromData(game: Game, data: Data): com.example.cpudefense.Hero
                /** reconstruct a Link object based on the saved data
                 * and set all inner proprieties
                 */
        {
            val newInstance = Hero(game, data.type)
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

        fun setType()
        {
            when (type)
            {
                Type.INCREASE_CHIP_SUB_SPEED ->
                {
                    name = "Turing"
                    fullName = "Alan Turing"
                    effect = "Increases speed of all %s chips".format("SUB")
                    vitae = game.resources.getString(R.string.turing)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.turing)
                }
                Type.INCREASE_CHIP_SHIFT_SPEED ->
                {
                    name = "Lovelace"
                    fullName = "Ada Lovelace"
                    effect = "Increases speed of all %s chips".format("SHR")
                    vitae = game.resources.getString(R.string.lovelace)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.lovelace)
                }
                Type.INCREASE_CHIP_MEM_SPEED ->
                {
                    name = "Knuth"
                    fullName = "Donald E. Knuth"
                    effect = "Increases speed of all %s chips".format("MEM")
                    vitae = game.resources.getString(R.string.knuth)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.knuth)
                }
                Type.INCREASE_STARTING_CASH ->
                {
                    name = "Hollerith"
                    fullName = "Herman Hollerith"
                    effect = "Increases information at start of level"
                    vitae = game.resources.getString(R.string.hollerith)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.hollerith)
                }
                Type.DECREASE_UPGRADE_COST ->
                {
                    name = "Osborne"
                    fullName = "Adam Osborne"
                    effect = "Decreases upgrade cost of all chips"
                    vitae = game.resources.getString(R.string.osborne)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.osborne)
                }
                Type.ADDITIONAL_LIVES ->
                {
                    name = "Zuse"
                    fullName = "Konrad Zuse"
                    effect = "Grants additional lives"
                    vitae = game.resources.getString(R.string.zuse)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.zuse)
                }
                Type.DECREASE_ATT_FREQ ->
                {
                    name = "LHC"
                    fullName = "Les Horribles Cernettes"
                    effect = "Decreases attacker frequency"
                    vitae = game.resources.getString(R.string.cernettes)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.cernettes)
                }
                Type.GAIN_CASH ->
                {
                    name = "Franke"
                    fullName = "Herbert W. Franke"
                    effect = "Gains additional bits over time"
                    vitae = game.resources.getString(R.string.franke)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.franke)
                }
                Type.DECREASE_ATT_SPEED ->
                {
                    name = "Vaughan"
                    fullName = "Dorothy Vaughan"
                    effect = "Decreases attacker speed"
                    vitae = game.resources.getString(R.string.vaughan)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.vaughan)
                }
                Type.INCREASE_REFUND ->
                {
                    name = "Tramiel"
                    fullName = "Jack Tramiel"
                    effect = "Increases refund when selling chips"
                    vitae = game.resources.getString(R.string.tramiel)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.tramiel)
                }
                else ->
                {
                    name = "Schneier"
                    fullName = "Bruce Schneier"
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.schneier)
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
            paintBiography.textSize = 28f
            paintBiography.alpha = 255
            val textLayout = StaticLayout(
                text, paintBiography, myArea.width(),
                Layout.Alignment.ALIGN_NORMAL,
                1.0f,
                0.0f,
                false
            )
            textLayout.draw(canvas)
        }
    }
}