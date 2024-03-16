package com.example.cpudefense

import android.app.Dialog
import android.graphics.*
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.Toast
import com.example.cpudefense.effects.*
import com.example.cpudefense.gameElements.Button
import com.example.cpudefense.gameElements.GameElement
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.center
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
    private var paint = Paint()
    private var cardViewOffset = 0f  // used for scrolling
    private var biographyViewOffset = 0f  // used for scrolling

    private var upgrades = mutableListOf<Hero>()
    private var selected: Hero? = null
    private var coins = mutableListOf<Coin>()
    private var coinSize = (32 * game.resources.displayMetrics.scaledDensity).toInt()

    var nextGameLevel = Stage.Identifier()

    init {
        clearPaint.color = Color.BLACK
        clearPaint.style = Paint.Style.FILL
    }

    fun setSize(area: Rect)
    {
        myArea = Rect(area)
        val topMargin = (80 * game.resources.displayMetrics.scaledDensity).toInt()
        cardsArea = Rect(64, topMargin, ((Game.cardWidth + 20)*game.resources.displayMetrics.scaledDensity).toInt(), myArea.bottom)
        coinSize = 80 * topMargin / 100
        biographyArea= Rect(cardsArea.right+biographyAreaMargin, topMargin, myArea.right-biographyAreaMargin, myArea.bottom-biographyAreaMargin)
        createButton()
    }

    fun fillMarket(level: Stage.Identifier)
    {
        nextGameLevel = level
        val newUpgrades = mutableListOf<Hero>()
        for (type in Hero.Type.values())
        {
            /* if upgrade already exists (because it has been bought earlier),
            get it from the game data. Otherwise, create an empty card.
            Only add upgrades that are allowed (available) at present.
             */
            var card: Hero? = game.heroes[type]
            if (card == null)
               card = Hero.createFromData(game, Hero.Data(type))
            if (card.isAvailable(level)) {
                card.createBiography(biographyArea)
                newUpgrades.add(card)
            }
            card.setDesc()
            card.setSize()
            card.createBitmap()
        }
        arrangeCards(newUpgrades, cardViewOffset)
        upgrades = newUpgrades
        coins = MutableList(game.global.coinsTotal) { Coin(game, coinSize) }
    }

    private fun arrangeCards(cards: MutableList<Hero>, dY: Float = 0f)
    /** calculate positions of the cards' rectangles.
     * @param dY Vertical offset used for scrolling */
    {
        val space = 20
        val offset = (Game.cardHeight*game.resources.displayMetrics.scaledDensity).toInt() + space
        var pos = cardsArea.top + space + dY.toInt()
        for (card in cards)
        {
            card.setSize()
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
        buttonFinish = Button(game, game.resources.getString(R.string.button_playlevel),
            textSize = Game.purchaseButtonTextSize * game.resources.displayMetrics.scaledDensity,
            style = Button.Style.HPKEY, preferredWidth = biographyArea.width())
        buttonFinish?.let {
            Fader(game, it, Fader.Type.APPEAR, Fader.Speed.SLOW)
            it.alignRight(myArea.right, myArea.bottom - bottomMargin - it.area.height())
        }
        buttonRefund = Button(game, game.resources.getString(R.string.button_refund_all),
            textSize = Game.purchaseButtonTextSize * game.resources.displayMetrics.scaledDensity,
            style = Button.Style.HPKEY, preferredWidth = biographyArea.width())
        buttonRefund?.let {
            Fader(game, it, Fader.Type.APPEAR, Fader.Speed.SLOW)
            it.alignRight(myArea.right, myArea.bottom - bottomMargin - 2*it.area.height())
        }
        buttonPurchase = Button(game, purchaseButtonText(null),
            textSize = Game.purchaseButtonTextSize * game.resources.displayMetrics.scaledDensity,
            style = Button.Style.HPKEY, preferredWidth = biographyArea.width())
        buttonPurchase?.let {
            Fader(game, it, Fader.Type.APPEAR, Fader.Speed.SLOW)
            it.alignRight(myArea.right, myArea.bottom - bottomMargin - 3*it.area.height())
        }
        biographyArea.bottom = ((buttonPurchase?.area?.top ?: buttonFinish?.area?.top) ?: myArea.bottom ) - biographyAreaMargin
    }

    fun onDown(event: MotionEvent): Boolean {
        /** test if a button has been pressed: */
        if (buttonFinish?.area?.contains(event.x.toInt(), event.y.toInt()) == true)
        {
            game.startNextStage(nextGameLevel)
            return true
        }
        if (buttonRefund?.area?.contains(event.x.toInt(), event.y.toInt()) == true)
        {
            // if a hero card is selected, then only sell this one; otherwise, go into the "reset all" dialog
            selected?.let {
                if (it.data.level > 0)
                {
                    refundOne(it)
                    return true
                }
            }
            val dialog = Dialog(game.gameActivity)
            dialog.setContentView(R.layout.layout_dialog_heroes)
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.setCancelable(true)
            dialog.findViewById<android.widget.TextView>(R.id.question).text = game.resources.getText(R.string.query_reset)
            val button1 = dialog.findViewById<android.widget.Button>(R.id.button1)
            val button2 = dialog.findViewById<android.widget.Button>(R.id.button2)
            button2?.text = game.resources.getText(R.string.yes)
            button1?.text = game.resources.getText(R.string.no)
            button2?.setOnClickListener { dialog.dismiss(); refundAll() }
            button1?.setOnClickListener { dialog.dismiss() }
            dialog.show()
            return true
        }
        if (buttonPurchase?.area?.contains(event.x.toInt(), event.y.toInt()) == true)
        {
            selected?.let {
                val price = it.getPrice(it.data.level)
                if (game.global.coinsTotal >= price && it.data.level < it.getMaxUpgradeLevel()) {
                    game.global.coinsTotal -= price
                    it.data.coinsSpent += price
                    Fader(game, coins.last(), Fader.Type.DISAPPEAR)
                    it.doUpgrade()
                    fillMarket(nextGameLevel)
                    makeButtonText(it)
                }
            }
            return true
        }
        for (coin in coins)
        {
            if (coin.myArea.contains(event.x.toInt(), event.y.toInt())) {
                if (!coin.isCurrentlyFlipping)
                    Flipper(game, coin, Flipper.Type.HORIZONTAL)
                return true
            }
        }
        for (card in upgrades)
            if (card.areaOnScreen.contains(event.x.toInt(), event.y.toInt())) {
                selected = card
                biographyViewOffset = 0f
                makeButtonText(card)
                return true
            }
        if (cardsArea.contains(event.x.toInt(), event.y.toInt())) {
            selected = null
            makeButtonText(null)
        }
        return false
    }

    fun onLongPress(event: MotionEvent): Boolean {
        for (card in upgrades)
            if (card.areaOnScreen.contains(event.x.toInt(), event.y.toInt())) {
                Toast.makeText(game.gameActivity, card.upgradeInfo(), Toast.LENGTH_LONG).show()
                return true
            }
        return false
    }

    private fun refundAll()
    {
        for (card in upgrades.filter { it.data.level > 0} ) {
            val refund =
                if (card.data.coinsSpent > 0) card.data.coinsSpent else 0  // was: 4
            game.global.coinsTotal += refund
            card.resetUpgrade()
        }
        Persistency(game.gameActivity).saveHeroes(game)
        Persistency(game.gameActivity).saveState(game)
        fillMarket(nextGameLevel)
        makeButtonText(null)
    }

    private fun refundOne(hero: Hero)
    {
        with (hero)
        {
            if (data.type == Hero.Type.INCREASE_MAX_HERO_LEVEL) // heroes that cannot be fired
            {
                val res = game.resources
                val text = res.getString(R.string.message_cannot_fire).format(res.getString(R.string.button_refund_all))
                Toast.makeText(game.gameActivity, text, Toast.LENGTH_SHORT).show()
                return
            }
            when (data.level)
            {
                0 -> return  // should not happen
                1 -> { // sell hero completely
                    val refund = 1
                    data.coinsSpent = 0
                    game.global.coinsTotal += refund
                    doDowngrade()
                }
                else -> {
                    val refund = data.level-1
                    data.coinsSpent -= refund
                    game.global.coinsTotal += refund
                    doDowngrade()
                }
            }

        }
        Persistency(game.gameActivity).saveState(game)
        fillMarket(nextGameLevel)
        makeButtonText(hero)
    }
    fun onScroll(event1: MotionEvent?, event2: MotionEvent?, dX: Float, dY: Float): Boolean {
        val scrollfactor = 1.1f  // higher values make scrolling faster
        if (dY == 0f)
            return false  // only vertical movements are considered here
        event1?.let {
            val posX = it.x.toInt()
            val posY = it.y.toInt()
            when {
                cardsArea.contains(posX, posY) -> {
                    cardViewOffset -= dY * scrollfactor
                    if (cardViewOffset>0f)
                        cardViewOffset=0f
                    arrangeCards(upgrades, cardViewOffset)
                }
                biographyArea.contains(posX, posY) -> {
                    biographyViewOffset -= dY * scrollfactor
                    if (biographyViewOffset>0f) // avoid scrolling when already at end of area
                        biographyViewOffset=0f
                }
            }
        }
        return true
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
        if (game.state.phase != Game.GamePhase.MARKETPLACE)
            return
        if (myArea.width() == 0 || myArea.height() == 0)
        {
            val width = game.gameActivity.theGameView.width
            val height = game.gameActivity.theGameView.height
            if (width > 0 && height > 0) {
                setSize(Rect(0, 0, width, height))
                fillMarket(nextGameLevel)
            }
            else
                return
        }

        canvas.drawColor(Color.BLACK)

        // draw cards
        selected?.displayHighlightFrame(canvas)
        for (card in upgrades)
            card.display(canvas)

        // draw 'total coins' line
        val coinsArea = Rect(0, 0, myArea.right, cardsArea.top)
        canvas.drawRect(coinsArea, clearPaint)
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * game.resources.displayMetrics.scaledDensity
        paint.alpha = 255
        val y = coinsArea.bottom.toFloat() - paint.strokeWidth
        canvas.drawLine(0f, y, myArea.right.toFloat(), y, paint)

        // determine size and spacing of coins
        var coinLeftMargin = coinSize / 2
        var deltaX = coinSize + 2
        if (game.global.coinsTotal * deltaX + 2*coinLeftMargin > coinsArea.width())  // coins do not fit, must overlap
            deltaX = (myArea.width() - 2*coinLeftMargin) / game.global.coinsTotal
        val coinPosY = coinsArea.centerY()
        var coinPosX = coinLeftMargin
        for (c in coins)
        {
            c.setCenter(coinPosX, coinPosY)
            c.display(canvas, viewport)
            coinPosX += deltaX
        }
        // draw buttons
        buttonFinish?.display(canvas)
        buttonRefund?.display(canvas)
        selected?.let {
            buttonPurchase?.display(canvas)
        }
        // draw biography
        selected?.biography?.let {
            val sourceRect = Rect(0, -biographyViewOffset.toInt(), it.bitmap.width, it.myArea.height()-biographyViewOffset.toInt())
            canvas.drawBitmap(it.bitmap, sourceRect, biographyArea, paint)
        }
    }

    private fun makeButtonText(card: Hero?)
    {
        buttonPurchase?.text = purchaseButtonText(card)
        buttonRefund?.text = refundButtonText(card)
    }
    private fun purchaseButtonText(card: Hero?): String
    {
        var text: String? = card?.let {
            if (it.data.level <= 1)
                game.resources.getString(R.string.button_purchase)
            else
                game.resources.getString(R.string.button_purchase_plural).format(it.getPrice(card.data.level))
            }
        return text ?: game.resources.getString(R.string.button_purchase)
    }

    private fun refundButtonText(card: Hero?): String
    {
        card?.let {
            if (card.data.level>0)
                return game.resources.getString(R.string.button_refund_one)
            else
                return game.resources.getString(R.string.button_refund_all)
        }
        return game.resources.getString(R.string.button_refund_all)
    }

    class Coin(val game: Game, size: Int): GameElement(), Fadable, Flippable
    {
        val paint = Paint()
        val myArea = Rect(0,0,size,size)
        private var myBitmap: Bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        private val myCanvas = Canvas(myBitmap)
        var isCurrentlyFlipping = false

        init {
            paint.alpha = 255
            myCanvas.drawBitmap(game.coinIcon, null, Rect(0,0,size,size), paint)
        }
        override fun update() {
        }

        override fun display(canvas: Canvas, viewport: Viewport) {
            canvas.drawBitmap(myBitmap, null, myArea, paint)
        }

        override fun fadeDone(type: Fader.Type) {
        }

        override fun setOpacity(opacity: Float) {
            paint.alpha = (opacity * 255f).toInt()
        }

        override fun setBitmap(bitmap: Bitmap) {
            myBitmap = bitmap
        }

        override fun provideBitmap(): Bitmap {
            return myBitmap
        }

        override fun flipStart() {
            isCurrentlyFlipping = true
        }

        override fun flipDone() {
            isCurrentlyFlipping = false
        }

        fun setCenter(x: Int, y: Int) {
            myArea.setCenter(x, y)
        }


    }
}

