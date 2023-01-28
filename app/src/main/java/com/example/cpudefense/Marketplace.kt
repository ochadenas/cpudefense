package com.example.cpudefense

import android.graphics.*
import android.view.MotionEvent
import androidx.appcompat.app.AlertDialog
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.gameElements.Button
import com.example.cpudefense.gameElements.GameElement
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.center
import com.example.cpudefense.utils.setBottomRight
import com.example.cpudefense.utils.setCenter
import com.example.cpudefense.utils.setTopLeft

class Marketplace(val game: Game): GameElement()
{
    private var buttonFinish: Button? = null
    private var buttonRefund: Button? = null
    private var buttonPurchase: Button? = null
    private var myArea = Rect()
    private var cardsArea = Rect()  // area used for cards, without header
    private var biographyArea = Rect()
    private var biographyAreaMargin = 20
    private var clearPaint = Paint()
    private val textPaint = Paint()
    private var paint = Paint()
    private var viewOffset = 0f  // used for scrolling

    private var upgrades = mutableListOf<Hero>()
    private var selected: Hero? = null

    private var nextGameLevel = 0

    init {
        clearPaint.color = Color.BLACK
        clearPaint.style = Paint.Style.FILL
    }

    fun setSize(area: Rect)
    {
        myArea = Rect(area)
        cardsArea = Rect(myArea.top, 64, Game.cardWidth + 20, myArea.bottom)
        biographyArea= Rect(cardsArea.right+biographyAreaMargin, 64+biographyAreaMargin, myArea.right-biographyAreaMargin, myArea.bottom-biographyAreaMargin)
        createButton()
    }

    fun fillMarket(level: Int)
    {
        // upgrades.clear()
        nextGameLevel = level
        var newUpgrades = mutableListOf<Hero>()
        for (type in Hero.Type.values())
        {
            /* if upgrade already exists (because it has been bought earlier),
            get it from the game data. Otherwise, create an empty card.
            Only add upgrades that are allowed (available) at present.
             */
            var upgrade: Hero? = game.gameUpgrades[type]
            if (upgrade == null)
               upgrade = Hero.createFromData(game, Hero.Data(type))
            if (upgrade.isAvailable()) {
                upgrade.createBiography(biographyArea)
                newUpgrades.add(upgrade)
            }
        }
        arrangeCards(newUpgrades, viewOffset)
        upgrades = newUpgrades
    }

    private fun arrangeCards(cards: MutableList<Hero>, dY: Float = 0f)
    /** calculate positions of the cards' rectangles.
     * @param dY Vertical offset used for scrolling */
    {
        val space = 20
        val offset = Game.cardHeight + space
        var pos = offset + dY.toInt()
        for (card in cards)
        {
            card.areaOnScreen.setTopLeft(space, pos)
            card.heroArea.setCenter(card.areaOnScreen.center())
            pos += offset
        }
    }

    override fun update() {
    }

    private fun createButton()
    {
        val bottomMargin = 40
        buttonFinish = Button(game.resources.getString(R.string.button_resume), style = 1)
        buttonFinish?.let {
            Fader(game, it, Fader.Type.APPEAR, Fader.Speed.SLOW)
            it.myArea.setBottomRight(myArea.right-50, myArea.bottom-bottomMargin)
        }
        buttonRefund = Button(game.resources.getString(R.string.button_refund), style = 1)
        buttonRefund?.let {
            Fader(game, it, Fader.Type.APPEAR, Fader.Speed.SLOW)
            it.myArea.setBottomRight(myArea.right-50, myArea.bottom - bottomMargin - 2*(buttonFinish?.myArea?.height() ?: 200))
        }
        buttonPurchase = Button(game.resources.getString(R.string.button_purchase), style = 1)
        buttonPurchase?.let {
            Fader(game, it, Fader.Type.APPEAR, Fader.Speed.SLOW)
            it.myArea.setBottomRight(myArea.right-50, myArea.bottom - bottomMargin - 4*(buttonFinish?.myArea?.height() ?: 200))
        }
        biographyArea.bottom = ((buttonPurchase?.myArea?.top ?: buttonFinish?.myArea?.top) ?: myArea.bottom ) - biographyAreaMargin
    }

    fun onDown(event: MotionEvent): Boolean {
        /** test if a button has been pressed: */
        if (buttonFinish?.myArea?.contains(event.x.toInt(), event.y.toInt()) == true)
        {
            game.startNextStage(nextGameLevel)
            return true
        }
        if (buttonRefund?.myArea?.contains(event.x.toInt(), event.y.toInt()) == true)
        {
            val builder = AlertDialog.Builder(game.gameActivity)
            builder.setMessage(game.resources.getString(R.string.query_reset))
                .setCancelable(false)
                .setPositiveButton(game.resources.getString(R.string.yes)) { dialog, id -> refundAll() }
                .setNegativeButton(game.resources.getString(R.string.no)) { dialog, id -> dialog.dismiss() }
            val alert = builder.create()
            alert.show()
            return true
        }
        if (buttonPurchase?.myArea?.contains(event.x.toInt(), event.y.toInt()) == true)
        {
            selected?.let {
                var price = it.getPrice(it.data.level)
                if (game.global.coinsTotal >= price && it.data.level < it.maxLevel) {
                    game.global.coinsTotal -= price
                    it.data.coinsSpent += price
                    it.doUpgrade()
                    fillMarket(nextGameLevel)
                }
            }
            return true
        }
        for (card in upgrades)
            if (card.areaOnScreen.contains(event.x.toInt(), event.y.toInt())) {
                selected = card
                return true
            }
        selected = null
        return false
    }

    fun refundAll()
    {
        for (card in upgrades.filter { it.data.level > 0} ) {
            var refund =
                if (card.data.coinsSpent > 0) card.data.coinsSpent else 4
            game.global.coinsTotal += refund
            card.resetUpgrade()
        }
        game.gameActivity.saveUpgrades()
        game.gameActivity.saveState()
        fillMarket(nextGameLevel)
    }

    fun onScroll(event1: MotionEvent?, event2: MotionEvent?, dX: Float, dY: Float): Boolean {
        if (dY != 0f) {
            viewOffset -= dY / 2.0f
            arrangeCards(upgrades, viewOffset)
        }
        return true
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
        if (game.state.phase != Game.GamePhase.MARKETPLACE)
            return

        canvas.drawColor(Color.BLACK)

        // draw cards
        selected?.displayHighlightFrame(canvas)
        for (card in upgrades)
            card.display(canvas)

        // draw 'total coins' line
        val textArea = Rect(0, 0, myArea.right, cardsArea.top)
        canvas.drawRect(textArea, clearPaint)
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.alpha = 255
        val y = textArea.bottom.toFloat() - 2f
        canvas.drawLine(0f, y, myArea.right.toFloat(), y, paint)

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 36f
        val text = "Total coins: %d".format(game.global.coinsTotal)
        canvas.drawText(text, 20f, 40f, textPaint)

        // draw buttons
        buttonFinish?.display(canvas)
        buttonRefund?.display(canvas)
        selected?.let {
            buttonPurchase?.display(canvas)
        }

        // draw biography
        selected?.biography?.let {
            canvas.drawBitmap(it.bitmap, null, biographyArea, paint)
        }
    }
}