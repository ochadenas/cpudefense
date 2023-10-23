package com.example.cpudefense.gameElements

import android.graphics.*
import com.example.cpudefense.*
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.*

class ScoreBoard(val game: Game): GameElement() {
    var area = Rect()
    var score = Score()
    var waves = Waves()
    var lives = Lives()
    var coins = Coins()
    var debugStatusLine: DebugStatusLine? = null
    var myColor = Color.WHITE
    var divider = 0  // between the display title and the actual display

    fun setSize(area: Rect) {
        this.area = Rect(area)
        // divider between title line and actual status indicators
        divider = /* this.area.top + */ this.area.height() * 32 / 100
        score.setSize(area, divider)
        waves.setSize(area, divider)
        lives.setSize(area, divider)
        coins.setSize(area, divider)
        if (game.gameActivity.settings.showFramerate) {
            debugStatusLine = DebugStatusLine()
            debugStatusLine?.setSize(area, divider)
        }
        recreateBitmap()
    }

    fun addCash(amount: Int) {
        game.state.cash += amount
    }

    fun informationToString(number: Int): String {
        if (number < 512)
            return "%d bit".format(number)
        var bytes = number / 8
        if (bytes < 1000)
            return "%d B".format(bytes)
        bytes /= 1024
        if (bytes < 1000)
            return "%d KiB".format(bytes)
        bytes /= 1024
        if (bytes < 1000)
            return "%d MiB".format(bytes)
        bytes /= 1024
        if (bytes < 1000)
            return "%d GiB".format(bytes)
        bytes /= 1024
        return "%d TiB".format(bytes)
    }

    override fun update() {
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
        val paint = Paint()
        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
        canvas.drawRect(area, paint)
        paint.color = myColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawRect(area, paint)

        if (game.currentStage?.getSeries() ?: 1 > 1 || game.currentStage?.getLevel() ?: 3 > 2)
            score.display(canvas)
        waves.display(canvas)
        lives.display(canvas)
        coins.display(canvas)
        debugStatusLine?.display(canvas)
    }

    fun displayHeader(canvas: Canvas, area: Rect, text: String, centered: Boolean = true)
            /**
             * Display text in 'header' text size
             *
             * @param canvas Where to paint on
             * @param area The rectangle where the header text should be placed
             * @param text The actual string to be displayed
             */
    {
        var rect = Rect(area)
        rect.bottom = divider
        val paint = Paint()
        paint.color = game.resources.getColor(R.color.scoreboard_text)
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        paint.textSize = Game.scoreHeaderSize * game.resources.displayMetrics.scaledDensity
        if (centered)
            rect.displayTextCenteredInRect(canvas, text, paint)
        else
            rect.displayTextLeftAlignedInRect(canvas, text, paint)
    }

    fun recreateBitmap()
            /**
             * Recreate all parts of the score board. Called when resuming the game.
             */
    {
        if (area.width()>0 && area.height()>0) {
            score.recreateBitmap()
            waves.recreateBitmap()
            lives.recreateBitmap()
            coins.recreateBitmap()
        }
    }

    inner class Score()
    /** display of current amount of information ('cash') */
    {
        var area = Rect()
        var divider = 0

        var lastValue = -1   // used to detect value changes
        lateinit var bitmap: Bitmap
        val paint = Paint()

        fun setSize(area: Rect, divider: Int) {
            this.area = Rect(area.left, area.top, 200, area.bottom)
            bitmap = Bitmap.createBitmap(this.area.width(), this.area.height(), Bitmap.Config.ARGB_8888)
            this.divider = divider
        }

        fun display(canvas: Canvas)
        {
            if (game.state.cash != lastValue)
            {
                /* only render the display if value has changed, otherwise re-use bitmap */
                lastValue = game.state.cash
                recreateBitmap()
            }
            canvas.drawBitmap(bitmap, null, area, paint)
        }

        fun recreateBitmap()
        {
            bitmap = Bitmap.createBitmap(area.width(), area.height(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val rect = Rect(0, divider, area.width(), area.height())
            val text = informationToString(game.state.cash)
            val paint = Paint()
            paint.color = myColor
            paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            paint.textSize = Game.scoreTextSize * game.resources.displayMetrics.scaledDensity
            rect.displayTextCenteredInRect(canvas, text, paint)
            displayHeader(canvas, Rect(0,0, area.width(), area.height()), game.resources.getString(R.string.scoreboard_inf))
        }
    }

    inner class Waves()
    {
        var area = Rect()
        var divider = 0

        var lastValue = -1   // used to detect value changes
        lateinit var bitmap: Bitmap
        val paint = Paint()

        fun setSize(area: Rect, divider: Int) {
            this.area = Rect(240, area.top, area.centerX(), area.bottom)
            bitmap = Bitmap.createBitmap(this.area.width(), this.area.height(), Bitmap.Config.ARGB_8888)
            this.divider = divider
        }

        fun display(canvas: Canvas)
        {
            if (game.currentStage?.data?.countOfWaves != lastValue)
            {
                /* only render the display if value has changed, otherwise re-use bitmap */
                lastValue = game.currentStage?.data?.countOfWaves ?: -1
                recreateBitmap()
            }
            canvas.drawBitmap(bitmap, null, area, paint)
        }

        fun recreateBitmap() {
            bitmap = Bitmap.createBitmap(area.width(), area.height(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            displayHeader(canvas, Rect(0,0, area.width(), area.height()), game.resources.getString(R.string.scoreboard_waves), centered = false)
            val rect = Rect(0, divider, area.width(), area.height())
            val bounds = Rect()
            paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            paint.style = Paint.Style.FILL
            paint.color = myColor
            paint.textAlign = Paint.Align.LEFT
            game.currentStage?.let {
                val currentWave = "%d".format(it.data.countOfWaves)
                paint.textSize = Game.scoreTextSize * game.resources.displayMetrics.scaledDensity
                paint.getTextBounds(currentWave, 0, currentWave.length, bounds)
                val verticalMargin = (rect.height()-bounds.height())/2
                val rectLeft = Rect(0, rect.top+verticalMargin, bounds.width(), rect.bottom-verticalMargin)
                val rectRight = Rect(rectLeft.right, rectLeft.top, rect.right, rectLeft.bottom)
                canvas.drawText(currentWave, rectLeft.left.toFloat(), rectLeft.bottom.toFloat(), paint)
                paint.textSize *= 0.6f
                canvas.drawText("  / %d".format(it.data.maxWaves), rectRight.left.toFloat(), rectRight.bottom.toFloat(), paint)
            }
        }
    }

    inner class Lives {
        var area = Rect()
        var divider = 0

        var lastValue = -1   // used to detect value changes
        lateinit var bitmap: Bitmap
        val paint = Paint()

        fun setSize(area: Rect, divider: Int) {
            this.area = Rect((area.width() * 0.7f).toInt(), area.top, area.right, area.bottom)
            bitmap = Bitmap.createBitmap(this.area.width(), this.area.height(), Bitmap.Config.ARGB_8888)
            this.divider = divider
        }

        fun display(canvas: Canvas)
        {
            if (game.state.lives != lastValue)
            {
                /* only render the display if value has changed, otherwise re-use bitmap */
                lastValue = game.state.lives
                recreateBitmap()
            }
            canvas.drawBitmap(bitmap, null, area, paint)
        }

        fun recreateBitmap()
        {
            bitmap = Bitmap.createBitmap(area.width(), area.height(), Bitmap.Config.ARGB_8888)
            var canvas = Canvas(bitmap)
            val sizeLedX = 12
            val sizeLedY = (area.height() * 0.3).toInt()
            val deltaX = 14 + sizeLedX
            val ledAreaHeight = sizeLedY + deltaX
            val ledAreaWidth = (game.state.currentMaxLives + 1) * deltaX
            val ledArea = Rect(0, 0, ledAreaWidth, ledAreaHeight)
            // var ledArea = Rect(0, divider+(area.height()-ledAreaHeight)/2, ledAreaWidth, ledAreaHeight)
            // determine the exact position of the LEDs. This is a bit frickelig
            ledArea.setCenter(area.width()/2, (area.height()+divider)/2)
            val resources = game.resources
            if (game.state.lives <= 0)
                return
            val paint = Paint()
            paint.style = Paint.Style.FILL
            paint.color = resources.getColor(R.color.led_panel)
            val glowPaint = Paint(paint)
            canvas.drawRect(ledArea, paint)
            for (i in 1..game.state.currentMaxLives) {
                val rect = Rect(0, 0, sizeLedX, sizeLedY)
                rect.setCenter(ledArea.right - i * deltaX, ledArea.centerY())
                val glowRect = Rect(rect).inflate(3)
                when (game.currentStage?.getSeries())
                {
                    1 -> {
                        paint.color = resources.getColor(R.color.led_green)
                        glowPaint.color = resources.getColor(R.color.led_green)
                    }
                    2 -> {
                        paint.color = resources.getColor(R.color.led_turbo)
                        glowPaint.color = resources.getColor(R.color.led_turbo_glow)
                    }
                    else -> {
                        paint.color = resources.getColor(R.color.led_green)
                        glowPaint.color = resources.getColor(R.color.led_green)
                    }
                }
                if (i > game.state.lives)
                {
                    paint.color = resources.getColor(R.color.led_red)
                    glowPaint.color = resources.getColor(R.color.led_red_glow)
                }
                canvas.drawRect(glowRect, glowPaint)
                canvas.drawRect(rect, paint)
            }
            displayHeader(canvas, Rect(0,0, area.width(), area.height()), game.resources.getString(R.string.scoreboard_status))
        }
    }

    inner class Coins {
        var area = Rect()
        var divider = 0
        var coins: Int = 0
        var actualSize = Game.coinSizeOnScoreboard

        var lastValue = -1   // used to detect value changes
        lateinit var bitmap: Bitmap
        val paint = Paint()

        fun setSize(area: Rect, divider: Int) {
            actualSize = (Game.coinSizeOnScoreboard * game.resources.displayMetrics.scaledDensity).toInt()
            this.area = Rect(waves.area.right, area.top, lives.area.left, area.bottom)
            bitmap =
                Bitmap.createBitmap(this.area.width(), this.area.height(), Bitmap.Config.ARGB_8888)
            this.divider = divider
        }

        fun display(canvas: Canvas) {
            if (game.currentStage?.summary?.coinsMaxAvailable == 0)
                return  // levels where you can't get coins
            coins = game.state.coinsInLevel + game.state.coinsExtra
            if (coins<0)
                return  // something went wrong, shouldn't happen
            if (coins != lastValue) {
                /* only render the display if value has changed, otherwise re-use bitmap */
                lastValue = coins
                recreateBitmap()
            }
            canvas.drawBitmap(bitmap, null, area, paint)
        }

        fun recreateBitmap() {
            bitmap = Bitmap.createBitmap(area.width(), area.height(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val y = (divider + area.height()) / 2
            val deltaX = if (coins > 1)
                (area.width() - (2 * actualSize)) / (coins - 1)
            else 0

            val x = area.width() - actualSize
            val rect = Rect(0, 0, actualSize, actualSize)
            val paint = Paint()
            displayHeader(canvas, Rect(0,0, area.width(), area.height()), game.resources.getString(R.string.scoreboard_coins))
            for (i in 0 until coins) {
                rect.setCenter(x - i * deltaX, y)
                canvas.drawBitmap(game.coinIcon, null, rect, paint)
            }
        }
    }

    inner class DebugStatusLine()
    /** this is an additional text displayed at every tick.
     * It is meant to hold additional debug info, e. g. the current frame rate
     */
    {
        var area = Rect()
        var divider: Int = 0
        val paint = Paint()
        var bitmap: Bitmap? = null
        var lastValue = 0.0

        fun setSize(area: Rect, divider: Int) {
            this.divider = divider
            this.area = Rect(area.left, 0, area.right, divider)
            paint.color = Color.YELLOW
            paint.style = Paint.Style.FILL
        }

        fun display(canvas: Canvas) {
            if (game.frameRate != lastValue)
              recreateBitmap()
            bitmap?.let { canvas.drawBitmap(it, null, area, paint) }
        }

        fun recreateBitmap() {
            if (area.width() >0 && area.height() > 0)
                bitmap = Bitmap.createBitmap(area.width(), area.height(), Bitmap.Config.ARGB_8888)
            bitmap?.let {
                val canvas = Canvas(it)
                displayHeader(canvas, Rect(0, 0, area.width(), area.height()), "time per frame: %.2f ms".format(game.frameRate))
            }
            lastValue = game.frameRate
        }
    }
}