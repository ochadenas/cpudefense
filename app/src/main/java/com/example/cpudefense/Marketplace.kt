package com.example.cpudefense

import android.app.Dialog
import android.content.Intent
import android.content.res.Resources
import android.graphics.*
import android.net.Uri
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.cpudefense.effects.*
import com.example.cpudefense.gameElements.Button
import com.example.cpudefense.gameElements.GameElement
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.setCenter

class Marketplace(val gameView: GameView): GameElement()
{
    private val resources: Resources = gameView.resources
    private val gameMechanics = gameView.gameMechanics
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
    private var purse = gameMechanics.currentPurse()
    private var selected: Hero? = null
    private var coins = mutableListOf<Coin>()
    private var coinSize = (32 * gameView.scaleFactor).toInt()

    private var currentWiki: Hero? = null

    var nextGameLevel = Stage.Identifier()

    init {
        clearPaint.color = Color.BLACK
        clearPaint.style = Paint.Style.FILL
    }

    fun setSize(area: Rect)
    {
        myArea = Rect(area)
        val margin = (120 * gameView.scaleFactor).toInt()
        cardsArea = Rect(margin, margin, ((GameView.cardWidth + 20)*gameView.scaleFactor).toInt(), myArea.bottom)
        coinSize = 80 * margin / 100
        biographyArea= Rect(cardsArea.right+biographyAreaMargin, margin, myArea.right-biographyAreaMargin, myArea.bottom-biographyAreaMargin)
        createButton()
    }

    fun fillMarket(level: Stage.Identifier)
    {
        nextGameLevel = level
        val newUpgrades = mutableListOf<Hero>()
        val heroes = gameMechanics.currentHeroes(level)
        purse = gameMechanics.currentPurse()
        for (type in Hero.Type.values())
        {
            /* if upgrade already exists (because it has been bought earlier),
            get it from the game data. Otherwise, create an empty card.
            Only add upgrades that are allowed (available) at present.
             */
            var hero: Hero? = heroes[type]
            if (hero == null)
               hero = Hero.createFromData(gameView.gameActivity, Hero.Data(type))
            if (hero.isAvailable(level)) {
                hero.createBiography(biographyArea)
                newUpgrades.add(hero)
            }
            hero.setDesc()
            hero.card.create(showNextUpdate = true)
            hero.isOnLeave = hero.isOnLeave(level)
        }
        arrangeCards(newUpgrades, cardViewOffset)
        upgrades = newUpgrades
        coins = MutableList(purse.availableCoins()) { Coin(gameMechanics, coinSize) }
    }

    private fun arrangeCards(heroes: MutableList<Hero>, dY: Float = 0f)
    /** calculate the positions of the cards' rectangles.
     * @param dY Vertical offset used for scrolling */
    {
        val space = 20
        // val offset = (GameMechanics.cardHeight*gameView.scaleFactor).toInt() + space
        var pos = cardsArea.top + space + dY.toInt()
        for (hero in heroes)
        {
            hero.card.putAt(space, pos)
            pos += hero.card.cardArea.height() + space
        }
    }

    override fun update() {
    }

    private fun createButton()
    {
        val bottomMargin = 40
        buttonFinish = Button(gameView, resources.getString(R.string.button_playlevel),
                              textSize = GameView.purchaseButtonTextSize * gameView.textScaleFactor,
                              style = Button.Style.HP_KEY, preferredWidth = biographyArea.width())
        buttonFinish?.let {
            Fader(gameView, it, Fader.Type.APPEAR, Fader.Speed.SLOW)
            it.alignRight(biographyArea.right, myArea.bottom - bottomMargin - it.area.height())
        }
        buttonRefund = Button(gameView, resources.getString(R.string.button_refund_all),
                              textSize = GameView.purchaseButtonTextSize * gameView.textScaleFactor,
                              style = Button.Style.HP_KEY, preferredWidth = biographyArea.width())
        buttonRefund?.let {
            Fader(gameView, it, Fader.Type.APPEAR, Fader.Speed.SLOW)
            it.alignRight(biographyArea.right, myArea.bottom - bottomMargin - 2*it.area.height())
        }
        buttonPurchase = Button(gameView, purchaseButtonText(null),
                                textSize = GameView.purchaseButtonTextSize * gameView.textScaleFactor,
                                style = Button.Style.HP_KEY, preferredWidth = biographyArea.width())
        buttonPurchase?.let {
            Fader(gameView, it, Fader.Type.APPEAR, Fader.Speed.SLOW)
            it.alignRight(biographyArea.right, myArea.bottom - bottomMargin - 3*it.area.height())
        }
        biographyArea.bottom = ((buttonPurchase?.area?.top ?: buttonFinish?.area?.top) ?: myArea.bottom ) - biographyAreaMargin
    }

    fun onDown(event: MotionEvent): Boolean {
        /** test if a button has been pressed: */
        if (buttonFinish?.area?.contains(event.x.toInt(), event.y.toInt()) == true)
        {
            gameView.gameActivity.startNextStage(nextGameLevel)
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
            val dialog = Dialog(gameView.gameActivity)
            dialog.setContentView(R.layout.layout_dialog_heroes)
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.setCancelable(true)
            dialog.findViewById<android.widget.TextView>(R.id.question).text = resources.getText(R.string.query_reset)
            val button1 = dialog.findViewById<android.widget.Button>(R.id.button1)
            val button2 = dialog.findViewById<android.widget.Button>(R.id.button2)
            button2?.text = resources.getText(R.string.yes)
            button1?.text = resources.getText(R.string.no)
            button2?.setOnClickListener { dialog.dismiss(); refundAll() }
            button1?.setOnClickListener { dialog.dismiss() }
            dialog.show()
            return true
        }
        if (buttonPurchase?.area?.contains(event.x.toInt(), event.y.toInt()) == true)
        {
            selected?.let {
                if (heroIsOnLeave(it)) return true
                val price = it.getPrice(it.data.level)
                if (purse.canAfford(price) && it.data.level < it.getMaxUpgradeLevel()) {
                    purse.spend(price)
                    it.data.coinsSpent += price
                    Fader(gameView, coins.last(), Fader.Type.DISAPPEAR)
                    it.doUpgrade()
                    gameMechanics.currentHeroes(nextGameLevel)[it.data.type] = it
                    save()
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
                    Flipper(gameView, coin, Flipper.Type.HORIZONTAL)
                return true
            }
        }
        for (hero in upgrades)
            if (hero.card.cardAreaOnScreen.contains(event.x.toInt(), event.y.toInt())) {
                selected = hero
                biographyViewOffset = 0f
                makeButtonText(hero)
                return true
            }
        if (cardsArea.contains(event.x.toInt(), event.y.toInt())) {
            selected = null
            makeButtonText(null)
        }
        return false
    }

    fun onLongPress(event: MotionEvent): Boolean {
        if (biographyArea.contains(event.x.toInt(), event.y.toInt()))
        {
                // Toast.makeText(gameView.gameActivity, hero.upgradeInfo(), Toast.LENGTH_LONG).show()
                wikipedia()
                return true
            }
        return false
    }

    private fun wikipedia()
    {
        if (currentWiki == null) {
            val browserIntent =
                Intent(Intent.ACTION_VIEW, Uri.parse("https://en.wikipedia.org/wiki/Alan_Turing"))
            try {
                gameView.gameActivity.startActivity(browserIntent)
                currentWiki = selected
            } catch (_: Exception) {
            }  // come here if no external app can handle the request
        }
    }

    private fun refundAll()
            /** resets all heroes to level 0 that meet certain criteria,
             * e.g. that are not on leave.
             * Refunds the coins spent on the hero.
             */
    {
        for (card in upgrades.filter { it.data.level > 0 && !it.isOnLeave} ) {
            val refund =
                if (card.data.coinsSpent > 0) card.data.coinsSpent else 0  // was: 4
            purse.spend(-refund)
            card.resetUpgrade()
        }
        save()
        fillMarket(nextGameLevel)
        makeButtonText(null)
    }

    private fun refundOne(hero: Hero)
    {
        with (hero)
        {
            if (data.type == Hero.Type.INCREASE_MAX_HERO_LEVEL) // heroes that cannot be fired
            {
                val res = resources
                val text = res.getString(R.string.message_cannot_fire).format(res.getString(R.string.button_refund_all))
                Toast.makeText(gameView.gameActivity, text, Toast.LENGTH_SHORT).show()
                return
            }
            if (heroIsOnLeave(hero)) return
            when (data.level)
            {
                0 -> return  // should not happen
                1 -> { // sell hero completely
                    val refund = 1
                    data.coinsSpent = 0
                    purse.spend(-refund)
                    doDowngrade()
                }
                else -> {
                    val refund = data.level-1
                    data.coinsSpent -= refund
                    purse.spend(-refund)
                    doDowngrade()
                }
            }

        }
        save()
        fillMarket(nextGameLevel)
        makeButtonText(hero)
    }

    private fun save() {
        Persistency(gameView.gameActivity).saveHeroes(gameMechanics)
        Persistency(gameView.gameActivity).saveCoins(gameMechanics)
    }


    @Suppress("UNUSED_PARAMETER")
    fun onScroll(event1: MotionEvent?, event2: MotionEvent?, dX: Float, dY: Float): Boolean {
        val scrollFactor = 1.4f  // higher values make scrolling faster
        if (dY == 0f)
            return false  // only vertical movements are considered here
        event1?.let {
            val posX = it.x.toInt()
            val posY = it.y.toInt()
            when {
                cardsArea.contains(posX, posY) -> {
                    cardViewOffset -= dY * scrollFactor
                    if (cardViewOffset>0f)
                        cardViewOffset=0f
                    arrangeCards(upgrades, cardViewOffset)
                }
                biographyArea.contains(posX, posY) -> {
                    biographyViewOffset -= dY * scrollFactor
                    if (biographyViewOffset>0f) // avoid scrolling when already at end of area
                        biographyViewOffset=0f
                }
            }
        }
        return true
    }

    private fun heroIsOnLeave(hero: Hero): Boolean {
        if (hero.isOnLeave) {
            val res = resources
            val text = res.getString(R.string.message_is_on_leave).format(hero.person.name)
            Toast.makeText(gameView.gameActivity, text, Toast.LENGTH_SHORT).show()
            return true
        } else
            return false
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
        if (gameMechanics.state.phase != GameMechanics.GamePhase.MARKETPLACE)
            return
        if (myArea.width() == 0 || myArea.height() == 0)
        {
            val width = gameView.gameActivity.gameView.width
            val height = gameView.gameActivity.gameView.height
            if (width > 0 && height > 0) {
                setSize(Rect(0, 0, width, height))
                fillMarket(nextGameLevel)
            }
            else
                return
        }

        canvas.drawColor(Color.BLACK)
        // draw cards
        selected?.card?.displayHighlightFrame(canvas)
        for (hero in upgrades)
            hero.card.display(canvas, viewport)

        // draw 'total coins' line
        val coinsArea = Rect(0, 0, myArea.right, cardsArea.top)
        canvas.drawRect(coinsArea, clearPaint)
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * gameView.scaleFactor
        paint.alpha = 255
        val y = coinsArea.bottom.toFloat() - paint.strokeWidth
        canvas.drawLine(0f, y, myArea.right.toFloat(), y, paint)

        // determine size and spacing of coins
        val coinLeftMargin = coinSize / 2
        var deltaX = coinSize + 2
        if (coins.size * deltaX + 2*coinLeftMargin > coinsArea.width())  // coins do not fit, must overlap
            deltaX = (myArea.width() - 2*coinLeftMargin) / coins.size
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
        val text: String? = card?.let {
            if (it.data.level <= 1)
                resources.getString(R.string.button_purchase)
            else
                resources.getString(R.string.button_purchase_plural).format(it.getPrice(card.data.level))
            }
        return text ?: resources.getString(R.string.button_purchase)
    }

    private fun refundButtonText(card: Hero?): String
    {
        card?.let {
            if (card.data.level>0)
                return resources.getString(R.string.button_refund_one)
            else
                return resources.getString(R.string.button_refund_all)
        }
        return resources.getString(R.string.button_refund_all)
    }

    inner class Coin(val gameMechanics: GameMechanics, size: Int): GameElement(), Fadable, Flippable
    /** graphical representation of a crypto coin */
    {
        val paint = Paint()
        val myArea = Rect(0,0,size,size)
        private var myBitmap: Bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        private val myCanvas = Canvas(myBitmap)
        var isCurrentlyFlipping = false

        init {
            paint.alpha = 255
            myCanvas.drawBitmap(gameView.currentCoinBitmap(nextGameLevel), null, Rect(0, 0, size, size), paint)
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

