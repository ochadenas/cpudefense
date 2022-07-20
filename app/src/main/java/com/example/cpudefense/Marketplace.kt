package com.example.cpudefense

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import androidx.appcompat.app.AlertDialog
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.gameElements.Button
import com.example.cpudefense.gameElements.GameElement
import com.example.cpudefense.networkmap.Viewport
import java.util.concurrent.CopyOnWriteArrayList

class Marketplace(val game: Game): GameElement()
{
    private var buttonFinish: Button? = null
    private var buttonRefund: Button? = null
    private var myArea = Rect()
    private var cardsArea = Rect()  // area used for cards, without header
    private var viewOffset = 0f  // used for scrolling

    private var upgrades = CopyOnWriteArrayList<Upgrade>()
    private var nextGameLevel = 0

    fun setSize(area: Rect)
    {
        myArea = Rect(area)
        cardsArea = Rect(myArea.top, 64, Game.cardWidth + 20, myArea.bottom)
        createButton()
    }

    fun fillMarket(level: Int)
    {
        upgrades.clear()
        nextGameLevel = level
        for (type in Upgrade.Type.values())
        {
            /* if upgrade already exists (because it has been bought earlier),
            get it from the game data. Otherwise, create an empty card.
            Only add upgrades that are allowed (available) at present.
             */
            var upgrade: Upgrade? = game.gameUpgrades[type]
            if (upgrade == null)
                upgrade = Upgrade.createFromData(game, Upgrade.Data(type))
            if (upgrade.isAvailable())
                upgrades.add(upgrade)
        }
        arrangeCards(viewOffset)
    }

    private fun arrangeCards(dY: Float = 0f)
    /** calculate positions of the cards' rectangles.
     * @param dY Vertical offset used for scrolling */
    {
        val space = 20
        val offset = Game.cardHeight + space
        var pos = offset + dY.toInt()
        for (card in upgrades)
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
        buttonFinish = Button(game.resources.getString(R.string.button_continue))
        buttonFinish?.let {
            Fader(game, it, Fader.Type.APPEAR, Fader.Speed.SLOW)
            it.myArea.setBottomRight(myArea.right-50, myArea.bottom-bottomMargin)
            it.buttonPaint.color = game.resources.getColor(R.color.text_blue)
        }
        buttonRefund = Button(game.resources.getString(R.string.button_refund))
        buttonRefund?.let {
            Fader(game, it, Fader.Type.APPEAR, Fader.Speed.SLOW)
            it.myArea.setBottomRight(myArea.right-50, myArea.bottom - bottomMargin - 200)
            it.buttonPaint.color = game.resources.getColor(R.color.text_blue)
        }
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
            builder.setMessage("Do you want to reset all upgrades?")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, id -> refundAll() }
                .setNegativeButton("No") { dialog, id -> dialog.dismiss() }
            val alert = builder.create()
            alert.show()
            return true
        }
        for (card in upgrades)
        {
            if (card.areaOnScreen.contains(event.x.toInt(), event.y.toInt()))
            {
                var price = card.getPrice(card.data.level)
                if (game.global.coinsTotal >= price) {
                    game.global.coinsTotal -= price
                    card.data.coinsSpent += price
                    card.doUpgrade()
                    fillMarket(nextGameLevel)
                }
                return true
            }
        }
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
            arrangeCards(viewOffset)
        }
        return true
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
        if (game.state.phase != Game.GamePhase.MARKETPLACE)
            return

        // draw empty background
        var paint = Paint()
        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
        paint.alpha = 255
        canvas.drawRect(myArea, paint)

        // draw cards
        for (card in upgrades)
            card.display(canvas)

        // draw 'total coins' line
        val textArea = Rect(0, 0, myArea.right, cardsArea.top)
        canvas.drawRect(textArea, paint)
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        val y = textArea.bottom.toFloat() - 2f
        canvas.drawLine(0f, y, myArea.right.toFloat(), y, paint)
        val textPaint = Paint()
        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 36f
        val text = "Total coins: %d".format(game.global.coinsTotal)
        canvas.drawText(text, 20f, 40f, textPaint)

        // draw button
        buttonFinish?.display(canvas)
        buttonRefund?.display(canvas)
    }
}