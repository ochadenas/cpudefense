@file:Suppress("DEPRECATION")

package com.example.cpudefense.gameElements

import android.content.res.Resources
import android.graphics.*
import com.example.cpudefense.GameView
import com.example.cpudefense.Hero
import com.example.cpudefense.R
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.center
import com.example.cpudefense.utils.displayTextCenteredInRect
import com.example.cpudefense.utils.setCenter
import com.example.cpudefense.utils.setTopLeft

class HeroCard(val gameView: GameView, val hero: Hero): GameElement(), Fadable
/** graphical representation of a hero or a heroine */
{
    val type = hero.data.type
    val resources: Resources = gameView.resources

    /** rectangle with the size of the card, positioned at (0|0) */
    var cardArea = Rect()
    /** rectangle at the actual position on the screen */
    var cardAreaOnScreen = Rect(cardArea)
    /** the area where the hero photo goes */
    private var portraitArea = Rect()
    private var portraitAreaOnScreen = Rect(portraitArea)
    private var myBitmap: Bitmap? = null
    private var effectBitmap = BitmapFactory.decodeResource(resources, R.drawable.glow)
    private var shortDescRect = Rect(cardAreaOnScreen)

    /** state used for various graphical effects */
    enum class GraphicalState { NORMAL, TRANSIENT_LEVEL_0, TRANSIENT }
    private var graphicalState = GraphicalState.NORMAL
    private var transition = 0.0f
    var heroOpacity = 0f

    /** the little boxes that show the current level */
    private var levelIndicator = mutableListOf<Rect>()
    private var indicatorSize = portraitArea.width() / 10

    /** additional flags */
    private var showNextUpdate = true
    private var monochrome = false

    /* different paint objects */
    private var paintRect = Paint()
    private var paintInactive = Paint()
    private var paintIndicator = Paint()
    private var paintText = Paint()
    private val paintHero = Paint()

    var inactiveColor = resources.getColor(R.color.upgrade_inactive)
    private var monochromeColor = inactiveColor
    var activeColor: Int = if (monochrome) monochromeColor
    else when(type)
    {
        Hero.Type.INCREASE_CHIP_SUB_SPEED -> resources.getColor(R.color.upgrade_active_chip_sub)
        Hero.Type.INCREASE_CHIP_SUB_RANGE -> resources.getColor(R.color.upgrade_active_chip_sub)
        Hero.Type.DOUBLE_HIT_SUB -> resources.getColor(R.color.upgrade_active_chip_sub)
        Hero.Type.INCREASE_CHIP_SHR_SPEED -> resources.getColor(R.color.upgrade_active_chip_shr)
        Hero.Type.INCREASE_CHIP_SHR_RANGE -> resources.getColor(R.color.upgrade_active_chip_shr)
        Hero.Type.DOUBLE_HIT_SHR -> resources.getColor(R.color.upgrade_active_chip_shr)
        Hero.Type.INCREASE_CHIP_MEM_SPEED -> resources.getColor(R.color.upgrade_active_chip_mem)
        Hero.Type.INCREASE_CHIP_MEM_RANGE -> resources.getColor(R.color.upgrade_active_chip_mem)
        Hero.Type.ENABLE_MEM_UPGRADE -> resources.getColor(R.color.upgrade_active_chip_mem)
        Hero.Type.REDUCE_HEAT -> resources.getColor(R.color.upgrade_active_chip_clk)
        Hero.Type.INCREASE_CHIP_RES_STRENGTH -> resources.getColor(R.color.upgrade_active_chip_res)
        Hero.Type.INCREASE_CHIP_RES_DURATION -> resources.getColor(R.color.upgrade_active_chip_res)
        Hero.Type.CONVERT_HEAT -> resources.getColor(R.color.upgrade_active_chip_res)
        Hero.Type.DECREASE_ATT_FREQ -> resources.getColor(R.color.upgrade_active_general)
        Hero.Type.DECREASE_ATT_SPEED -> resources.getColor(R.color.upgrade_active_general)
        Hero.Type.DECREASE_ATT_STRENGTH -> resources.getColor(R.color.upgrade_active_general)
        Hero.Type.DECREASE_COIN_STRENGTH -> resources.getColor(R.color.upgrade_active_general)
        Hero.Type.ADDITIONAL_LIVES -> resources.getColor(R.color.upgrade_active_meta)
        Hero.Type.INCREASE_MAX_HERO_LEVEL -> resources.getColor(R.color.upgrade_active_meta)
        Hero.Type.LIMIT_UNWANTED_CHIPS -> resources.getColor(R.color.upgrade_active_meta)
        Hero.Type.CREATE_ADDITIONAL_CHIPS -> resources.getColor(R.color.upgrade_active_meta)
        Hero.Type.INCREASE_STARTING_CASH -> resources.getColor(R.color.upgrade_active_eco)
        Hero.Type.GAIN_CASH -> resources.getColor(R.color.upgrade_active_eco)
        Hero.Type.GAIN_CASH_ON_KILL -> resources.getColor(R.color.upgrade_active_eco)
        Hero.Type.INCREASE_REFUND -> resources.getColor(R.color.upgrade_active_eco)
        Hero.Type.DECREASE_UPGRADE_COST -> resources.getColor(R.color.upgrade_active_eco)
        Hero.Type.DECREASE_REMOVAL_COST -> resources.getColor(R.color.upgrade_active_eco)
    }


    init {
        paintRect.style = Paint.Style.STROKE
        paintInactive = Paint(paintRect)
        paintInactive.color = inactiveColor
        paintText.color = Color.WHITE
        paintText.style = Paint.Style.FILL
        shortDescRect.top = shortDescRect.bottom - (50 * resources.displayMetrics.scaledDensity).toInt()
        heroOpacity = when (hero.data.level) { 0 -> 0f else -> 1f}
    }

    override fun update() {
    }

    override fun display(canvas: Canvas, viewport: Viewport)
    {
        when (hero.data.level) {
            0 -> {
                paintRect.color = inactiveColor
                paintRect.strokeWidth = 2f
            }
            else -> {
                paintRect.color = activeColor
                paintRect.strokeWidth = 2f + hero.data.level / 2
            }
        }
        paintRect.strokeWidth *= resources.displayMetrics.scaledDensity
        myBitmap?.let { canvas.drawBitmap(it, null, cardAreaOnScreen, paintRect) }

        // display hero picture
        // (this is put here because of fading)
        if (hero.isOnLeave)
        {
            portraitAreaOnScreen.displayTextCenteredInRect(canvas, "On leave", paintText)
        }
        else // not on leave, this is the normal case
        {
            paintHero.alpha = (255f * heroOpacity).toInt()
            hero.person.picture?.let { canvas.drawBitmap(it, null, portraitAreaOnScreen, paintHero) }
        }
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
                paintRect.strokeWidth = (2f + 10 * thickness)*resources.displayMetrics.scaledDensity
                canvas.drawRect(cardAreaOnScreen, paintRect)
            }
            GraphicalState.TRANSIENT_LEVEL_0 -> {
                // draw animation for initial activation of the upgrade
                canvas.drawRect(cardAreaOnScreen, paintInactive)
                displayLine(canvas, cardAreaOnScreen.left, cardAreaOnScreen.top, cardAreaOnScreen.left, cardAreaOnScreen.bottom)
                displayLine(canvas, cardAreaOnScreen.right, cardAreaOnScreen.bottom, cardAreaOnScreen.right, cardAreaOnScreen.top)
                displayLine(canvas, cardAreaOnScreen.right, cardAreaOnScreen.top, cardAreaOnScreen.left, cardAreaOnScreen.top)
                displayLine(canvas, cardAreaOnScreen.left, cardAreaOnScreen.bottom, cardAreaOnScreen.right, cardAreaOnScreen.bottom)
                // let hero picture appear
                heroOpacity = transition
            }
            else -> canvas.drawRect(cardAreaOnScreen, paintRect)
        }
    }

    fun displayHighlightFrame(canvas: Canvas)
    {
        with (paintInactive)
        {
            val originalThickness = strokeWidth
            val originalAlpha = alpha
            alpha = 60
            strokeWidth = originalThickness + 12 * resources.displayMetrics.scaledDensity
            canvas.drawRect(cardAreaOnScreen, this)
            alpha = 60
            strokeWidth = originalThickness + 6 * resources.displayMetrics.scaledDensity
            canvas.drawRect(cardAreaOnScreen, this)
            // restore original values
            strokeWidth = originalThickness
            alpha = originalAlpha
        }
        // }
    }

    private fun displayLine(canvas: Canvas, x0: Int, y0: Int, x1: Int, y1: Int)
    /** draws a fraction of the line between x0,y0 and x1,y1 */
    {
        val x: Float = x0 * (1-transition) + x1 * transition
        val y = y0 * (1-transition) + y1 * transition
        canvas.drawLine(x0.toFloat(), y0.toFloat(), x, y, paintRect)
        // draw the glow effect
        val effectRect = Rect(0, 0, effectBitmap.width, effectBitmap.height)
        effectRect.setCenter(x.toInt(), y.toInt())
        canvas.drawBitmap(effectBitmap, null, effectRect, paintText)
    }

    fun putAt(left: Int, top: Int)
    /** sets the top left corner of the card area to the given position */
    {
        cardAreaOnScreen = Rect(cardArea)
        cardAreaOnScreen.setTopLeft(left, top)
        val center = cardAreaOnScreen.center()
        portraitAreaOnScreen = Rect(portraitArea)
        portraitAreaOnScreen.setCenter(center)
        paintText.textSize = GameView.heroCardTextSize * gameView.textScaleFactor
        indicatorSize = portraitArea.width() / 10
    }

    private fun addLevelDecoration(canvas: Canvas)
    {
        paintIndicator.color = if (hero.data.level == 0) inactiveColor else activeColor
        var verticalIndicatorSize = indicatorSize  // squeeze when max level is greater than 8
        if (hero.getMaxUpgradeLevel()>8)
            verticalIndicatorSize = portraitArea.height() / (5+hero.getMaxUpgradeLevel())
        for (i in 1 .. hero.getMaxUpgradeLevel())
        {
            val rect = Rect(0,0, indicatorSize, verticalIndicatorSize)
            rect.setTopLeft(0, (2*i-1)*verticalIndicatorSize)
            levelIndicator.add(rect)
            paintIndicator.style = if (i<=hero.data.level) Paint.Style.FILL else Paint.Style.STROKE
            canvas.drawRect(rect, paintIndicator)
        }
        return
    }

    fun create(showNextUpdate: Boolean = true, monochrome: Boolean = false)
    {
        cardArea = Rect(0, 0, (GameView.cardWidth*gameView.scaleFactor).toInt(), (GameView.cardHeight*gameView.scaleFactor).toInt())
        portraitArea = Rect(0, 0, (GameView.cardPictureSize*gameView.scaleFactor).toInt(), (GameView.cardPictureSize*gameView.scaleFactor).toInt())
        paintText.textSize = GameView.heroCardTextSize * gameView.textScaleFactor
        indicatorSize = portraitArea.width() / 10
        this.showNextUpdate = showNextUpdate
        this.monochrome = monochrome
        createBitmap()
    }

    fun createBitmap()
            /** re-creates the bitmap without border, using a canvas positioned at (0, 0) */
    {
        val bitmap = Bitmap.createBitmap(cardArea.width(), cardArea.height(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // render text used to indicate the effect of the upgrade, and calculate its position
        val marginHorizontal = 10f*resources.displayMetrics.scaledDensity
        val marginVertical = 10f*resources.displayMetrics.scaledDensity
        val baseline = bitmap.height-marginVertical
        val paintUpdate = Paint(paintText)
        canvas.drawText(hero.strengthDesc, marginHorizontal, baseline, paintText)
        val bounds = Rect()
        paintText.getTextBounds(hero.strengthDesc, 0, hero.strengthDesc.length, bounds)
        canvas.drawText(hero.shortDesc, marginHorizontal, baseline - bounds.height() - marginVertical, paintText)
        if (showNextUpdate) {
            paintUpdate.color = resources.getColor(R.color.upgrade_inactive)
            canvas.drawText(hero.upgradeDesc, bounds.right + marginHorizontal, baseline, paintUpdate)
        }
        val margin = (10*resources.displayMetrics.scaledDensity).toInt()
        val heroPaintText = Paint(paintText)
        heroPaintText.textSize = GameView.heroCardNameSize * gameView.textScaleFactor
        heroPaintText.color = if (hero.data.level == 0) inactiveColor else activeColor
        val heroTextRect = Rect(0, margin, cardArea.width(), margin+40)
        heroTextRect.displayTextCenteredInRect(canvas, hero.person.fullName, heroPaintText)

        addLevelDecoration(canvas)
        myBitmap = bitmap
    }

    fun upgradeAnimation()
    /** graphical transition that is called when upgrading the hero in the marketplace */
    {
        if (hero.data.level == 1) {
            Fader(gameView, this, Fader.Type.APPEAR, Fader.Speed.VERY_SLOW)
            graphicalState = GraphicalState.TRANSIENT_LEVEL_0
        }
        else {
            Fader(gameView, this, Fader.Type.APPEAR, Fader.Speed.MEDIUM)
            graphicalState = GraphicalState.TRANSIENT
        }
    }

    fun downgradeAnimation()
            /** graphical transition that is called when downgrading the hero in the marketplace */
    {
        if (hero.data.level == 0) {
            Fader(gameView, this, Fader.Type.DISAPPEAR, Fader.Speed.MEDIUM)
            graphicalState = GraphicalState.TRANSIENT_LEVEL_0
        }
        else {
            Fader(gameView, this, Fader.Type.APPEAR, Fader.Speed.MEDIUM)
            graphicalState = GraphicalState.TRANSIENT
        }
    }

    override fun fadeDone(type: Fader.Type) {
        if (hero.data.level == 0)
            heroOpacity = 0.0f
        else
            heroOpacity = 1.0f
        graphicalState = GraphicalState.NORMAL
    }

    override fun setOpacity(opacity: Float)
    {
        transition = opacity
    }
}