@file:Suppress("DEPRECATION")

package com.example.cpudefense.gameElements

import android.graphics.*
import com.example.cpudefense.*
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.*

class ScoreBoard(val gameView: GameView): GameElement() 
{
    private var resources = gameView.resources
    
    // default or min sizes
    var margin = 4   // between LED area and edge
    var preferredSizeOfLED = 10 // horizontal size of LEDs, can be smaller if there is too little space
    
    private var area = Rect()
    private var information = Information()
    private var waves = Waves()
    private var lives = Lives()
    private var coins = Coins()
    private var temperature = Temperature()
    private var debugStatusLine: DebugStatusLine? = null
    private var myColor = Color.WHITE
    private var divider = 0  // between the display title and the actual display

    val fractionOfScoreBoardUsedForInf = 0.3f
    private val scoreboardBorderWidth = 4.0f

    fun setSize(area: Rect)
            /** sets the size of the score board and determines the dimensions of all components.
             * @param area The rectangle that the score board shall occupy
              */
    {
        this.area = Rect(area)
        // divider between title line and actual status indicators
        divider = /* this.area.top + */ this.area.height() * 32 / 100
        var areaRemaining = Rect(area).inflate(-scoreboardBorderWidth.toInt())
        areaRemaining = information.setSize(areaRemaining, divider)
        areaRemaining = waves.setSize(areaRemaining, divider)
        areaRemaining = coins.setSize(areaRemaining, divider)
        areaRemaining = lives.setSize(areaRemaining, divider)
        areaRemaining = temperature.setSize(areaRemaining, divider)
        if (gameView.gameMechanics.gameActivity.settings.showFramerate) {
            debugStatusLine = DebugStatusLine()
            debugStatusLine?.setSize(area, divider)
        }
        recreateBitmap()
    }

    fun addCash(amount: Int) {
        gameView.gameMechanics.state.cash += amount
    }

    fun informationToString(number: Int): String {
        if (number < 512 && number > -512)
            return "%d bit".format(number)
        val bytes: Int = number/8
        if (bytes < 800 && bytes > -800)
            return "%d B".format(bytes)
        val kiB: Float = bytes.toFloat()/1024.0f
        if (kiB < 800 && kiB > -800)
            return "%.1f KiB".format(kiB)
        val mibiBytes: Float = kiB/1024.0f
        if (mibiBytes < 800 && mibiBytes > -800)
            return "%.1f MiB".format(mibiBytes)
        val  gibiBytes: Float = mibiBytes/1024.0f
        return "%.1f GiB".format(gibiBytes)
    }

    override fun update() {
    }

    override fun display(canvas: Canvas, viewport: Viewport)
    {
        val currentStage = gameView.gameMechanics.currentStage
        val paint = Paint()
        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
        canvas.drawRect(area, paint)
        paint.color = myColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scoreboardBorderWidth
        canvas.drawRect(area, paint)
        if (currentStage.series > 1 || currentStage.number > 2)
            information.display(canvas)
        waves.display(canvas)
        lives.display(canvas)
        coins.display(canvas)
        if (currentStage.series > 1 || currentStage.number > 27)
            temperature.display(canvas)
        if (currentStage.series > 1)
            temperature.display(canvas)
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
        // TODO: Problem is that text with letters below the line (such as 'Temp') is slightly off-center vertically
        // due to calculation in rect.displayTextCenteredInRect
        val rect = Rect(area)
        rect.bottom = divider
        val paint = Paint()
        paint.color = resources.getColor(R.color.scoreboard_text)
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        paint.textSize = GameMechanics.scoreHeaderSize * resources.displayMetrics.scaledDensity
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
            information.recreateBitmap()
            waves.recreateBitmap()
            lives.recreateBitmap()
            coins.recreateBitmap()
            temperature.recreateBitmap()
        }
    }

    inner class Information
    /** display of current amount of information ('cash') */
    {
        private var area = Rect()
        private var divider = 0

        private var lastValue = -1   // used to detect value changes
        lateinit var bitmap: Bitmap
        val paint = Paint()

        fun setSize(area: Rect, divider: Int): Rect
                /** sets the area that is taken up by the information count.
                 * @param area The whole area of the score board
                 * @divider height of the line between header and contents
                 * @return The rectangle that remains (original area minus occupied area)
                  */
        {
            this.area = Rect(area.left, area.top, (area.width()*fractionOfScoreBoardUsedForInf).toInt(), area.bottom)
            bitmap = Bitmap.createBitmap(this.area.width(), this.area.height(), Bitmap.Config.ARGB_8888)
            this.divider = divider
            return Rect(this.area.right, area.top, area.right, area.bottom)
        }

        fun display(canvas: Canvas)
        {
            val state = gameView.gameMechanics.state
            if (state.cash != lastValue)
            {
                /* only render the display if value has changed, otherwise re-use bitmap */
                lastValue = state.cash
                recreateBitmap()
            }
            canvas.drawBitmap(bitmap, null, area, paint)
        }

        fun recreateBitmap()
        {
            bitmap = Bitmap.createBitmap(area.width(), area.height(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val rect = Rect(0, divider, area.width(), area.height())
            val text = informationToString(gameView.gameMechanics.state.cash)
            val paint = Paint()
            paint.color = myColor
            paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            paint.textSize = GameMechanics.scoreTextSize * resources.displayMetrics.scaledDensity
            rect.displayTextCenteredInRect(canvas, text, paint)
            displayHeader(canvas, Rect(0,0, area.width(), area.height()), resources.getString(R.string.scoreboard_inf))
        }
    }

    inner class Waves {
        private var area = Rect()
        private var divider = 0

        private var lastValue = -1   // used to detect value changes
        lateinit var bitmap: Bitmap
        val paint = Paint()

        fun setSize(area: Rect, divider: Int): Rect
        {
            this.area = Rect(area.left, area.top, (area.left+area.width()*0.25).toInt(), area.bottom)
            bitmap = Bitmap.createBitmap(this.area.width(), this.area.height(), Bitmap.Config.ARGB_8888)
            this.divider = divider
            return Rect(this.area.right, area.top, area.right, area.bottom)
        }

        fun display(canvas: Canvas)
        {
            val stage: Stage? = gameView.gameMechanics.currentlyActiveStage
            if (stage?.data?.wavesCount != lastValue)
            {
                /* only render the display if value has changed, otherwise re-use bitmap */
                lastValue = stage?.data?.wavesCount ?: -1
                recreateBitmap()
            }
            canvas.drawBitmap(bitmap, null, area, paint)
        }

        fun recreateBitmap() {
            bitmap = Bitmap.createBitmap(area.width(), area.height(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            displayHeader(canvas, Rect(0,0, area.width(), area.height()), resources.getString(R.string.scoreboard_waves), centered = false)
            val rect = Rect(0, divider, area.width(), area.height())
            val bounds = Rect()
            paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            paint.style = Paint.Style.FILL
            paint.color = myColor
            paint.textAlign = Paint.Align.LEFT
            gameView.gameMechanics.currentlyActiveStage?.let {
                val currentWave = "%d".format(it.data.wavesCount)
                paint.textSize = GameMechanics.scoreTextSize * resources.displayMetrics.scaledDensity
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
        private var area = Rect()
        private var divider = 0

        private var lastValue = -1   // used to detect value changes
        lateinit var bitmap: Bitmap
        private val paint = Paint()
        private var ledAreaHeight: Int = 0
        private var ledAreaWidth: Int = 0
        private val preferredSizeLedX = (preferredSizeOfLED * resources.displayMetrics.density).toInt()
        private var sizeLedX = preferredSizeLedX
        private var sizeLedY = 0 // will be calculated in setSize
        private var deltaX = 0


        fun setSize(area: Rect, divider: Int): Rect
        {
            val state = gameView.gameMechanics.state
            this.area = Rect(area.left, area.top, (area.left+area.width()*0.7f).toInt(), area.bottom)
            bitmap = Bitmap.createBitmap(this.area.width(), this.area.height(), Bitmap.Config.ARGB_8888)
            this.divider = divider
            // calculate size and spacing of LEDs
            sizeLedY = (area.height()-divider-2*margin)*74/100
            val maxPossibleDeltaX = area.width()/(state.currentMaxLives + 0.0f)
            deltaX = kotlin.math.min(preferredSizeLedX * 1.2f, maxPossibleDeltaX).toInt()
            ledAreaWidth = (state.currentMaxLives + 1) * deltaX
            sizeLedX = kotlin.math.min(preferredSizeLedX.toFloat(), deltaX / 1.2f).toInt()
            return Rect(this.area.right, area.top, area.right, area.bottom)
        }

        fun display(canvas: Canvas)
        {
            val state = gameView.gameMechanics.state
            if (state.lives != lastValue)
            {
                /* only render the display if value has changed, otherwise re-use bitmap */
                lastValue = state.lives
                recreateBitmap()
            }
            canvas.drawBitmap(bitmap, null, area, paint)
        }

        fun recreateBitmap()
        {
            val state = gameView.gameMechanics.state
            bitmap = Bitmap.createBitmap(area.width(), area.height(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            ledAreaHeight = (area.height()-divider) - 2*margin
            // ledAreaWidth = (game.state.currentMaxLives + 1) * deltaX
            ledAreaWidth = this.area.width()- 2*margin
            val ledArea = Rect(0, 0, ledAreaWidth, ledAreaHeight)
            // var ledArea = Rect(0, divider+(area.height()-ledAreaHeight)/2, ledAreaWidth, ledAreaHeight)
            // determine the exact position of the LEDs. This is a bit frickelig
            ledArea.setCenter(area.width()/2, (area.height()+divider)/2)
            val resources = resources
            if (state.lives <= 0)
                return
            val paint = Paint()
            paint.style = Paint.Style.FILL
            paint.color = resources.getColor(R.color.led_panel)
            val glowPaint = Paint(paint)
            canvas.drawRect(ledArea, paint)
            for (i in 1..state.currentMaxLives) {
                val glowRect = Rect(0, 0, sizeLedX, sizeLedY)
                glowRect.setCenter(ledArea.right - i * deltaX, ledArea.centerY())
                val ledRect = Rect(glowRect).inflate(-4)
                if (i <= state.lives)
                    when (gameView.gameMechanics.currentStage.series)
                    {
                        GameMechanics.SERIES_NORMAL -> {
                            paint.color = resources.getColor(R.color.led_green)
                            glowPaint.color = resources.getColor(R.color.led_green)
                        }
                        GameMechanics.SERIES_TURBO -> {
                            paint.color = resources.getColor(R.color.led_turbo)
                            glowPaint.color = resources.getColor(R.color.led_turbo_glow)
                        }
                        GameMechanics.SERIES_ENDLESS -> {
                            paint.color = resources.getColor(R.color.led_red)
                            glowPaint.color = resources.getColor(R.color.led_red_glow)
                        }
                    }
                else if (i > state.lives)
                {
                    paint.color = resources.getColor(R.color.led_off)
                    glowPaint.color = resources.getColor(R.color.led_off_glow)
                }
                canvas.drawRect(glowRect, glowPaint)
                canvas.drawRect(ledRect, paint)
            }
            displayHeader(canvas, Rect(0,0, area.width(), area.height()), resources.getString(R.string.scoreboard_status))
        }
    }

    inner class Coins {
        private var area = Rect()
        private var divider = 0
        private var coins: Int = 0
        private var actualSize = GameMechanics.coinSizeOnScoreboard

        private var lastValue = -1   // used to detect value changes
        lateinit var bitmap: Bitmap
        private val paint = Paint()

        fun setSize(area: Rect, divider: Int): Rect
        {
            actualSize = (GameMechanics.coinSizeOnScoreboard * resources.displayMetrics.scaledDensity).toInt()
            this.area = Rect(area.left, area.top, (area.left+area.width()*0.36).toInt(), area.bottom)
            bitmap =
                Bitmap.createBitmap(this.area.width(), this.area.height(), Bitmap.Config.ARGB_8888)
            this.divider = divider
            return Rect(this.area.right, area.top, area.right, area.bottom)
        }

        fun display(canvas: Canvas)
        {
            val stage: Stage? = gameView.gameMechanics.currentlyActiveStage
            val state: GameMechanics.StateData = gameView.gameMechanics.state
            if (stage?.summary?.coinsMaxAvailable == 0)
                return  // levels where you can't get coins
            coins = state.coinsInLevel + state.coinsExtra
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
            displayHeader(canvas, Rect(0,0, area.width(), area.height()), resources.getString(R.string.scoreboard_coins))
            for (i in 0 until coins) {
                rect.setCenter(x - i * deltaX, y)
                canvas.drawBitmap(gameView.gameMechanics.currentCoinBitmap(), null, rect, paint)
            }
        }
    }

    inner class Temperature {
        private var area = Rect()
        private var divider = 0
        private var temperature: Int = GameMechanics.baseTemperature
        private var lastValue = -1   // used to detect value changes
        private var actualSize = GameMechanics.coinSizeOnScoreboard
        private var sevenSegmentDisplay: SevenSegmentDisplay? = null

        lateinit var bitmap: Bitmap
        val paint = Paint()

        fun setSize(area: Rect, divider: Int): Rect
        {
            this.divider = divider
            this.area = Rect(area.left, area.top, area.right, area.bottom)
            actualSize = this.area.height() - divider
            sevenSegmentDisplay = SevenSegmentDisplay(2, actualSize, gameView.gameMechanics.gameActivity)
            sevenSegmentDisplay?.let {
                bitmap = it.getDisplayBitmap(0, SevenSegmentDisplay.LedColors.WHITE)
            }
            return Rect(this.area.right, area.top, area.right, area.bottom)
        }

        fun display(canvas: Canvas)
        {
            val state = gameView.gameMechanics.state
            temperature = (state.heat/GameMechanics.heatPerDegree + GameMechanics.baseTemperature).toInt()
            if (temperature != lastValue) {
                lastValue = temperature
                recreateBitmap()
                }
                bitmap.let { canvas.drawBitmap(it, null, area, paint) }
        }

        fun recreateBitmap() {
            bitmap = Bitmap.createBitmap(area.width(), area.height(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            sevenSegmentDisplay?.let {
                val displayRect = Rect(0, divider, area.width(), area.height())
                val headerRect = Rect(0, 0, area.width(), area.height())
                displayRect.shrink(margin)
                when (temperature)
                {
                    in 0 until GameMechanics.temperatureWarnThreshold -> canvas.drawBitmap(it.getDisplayBitmap(temperature, SevenSegmentDisplay.LedColors.WHITE), null, displayRect, paint)
                    in GameMechanics.temperatureWarnThreshold until GameMechanics.temperatureLimit -> canvas.drawBitmap(it.getDisplayBitmap(temperature, SevenSegmentDisplay.LedColors.YELLOW), null, displayRect, paint)
                    else -> canvas.drawBitmap(it.getDisplayBitmap(temperature, SevenSegmentDisplay.LedColors.RED), null, displayRect, paint)
                }
                displayHeader(canvas, headerRect, "Temp")
            }
        }
    }

    inner class DebugStatusLine
    /** this is an additional text displayed at every tick.
     * It is meant to hold additional debug info, e. g. the current frame rate
     */
    {
        private var area = Rect()
        private var divider: Int = 0
        private val paint = Paint()
        private var bitmap: Bitmap? = null
        private var lastValue = 0.0

        fun setSize(area: Rect, divider: Int) {
            this.divider = divider
            this.area = Rect(area.left, 0, area.right, divider)
            paint.color = Color.YELLOW
            paint.style = Paint.Style.FILL
        }

        fun display(canvas: Canvas) {
            if (gameView.gameMechanics.timeBetweenFrames != lastValue || true)
              recreateBitmap()
            bitmap?.let { canvas.drawBitmap(it, null, area, paint) }
        }

        private fun recreateBitmap() {
            if (area.width() >0 && area.height() > 0)
                bitmap = Bitmap.createBitmap(area.width(), area.height(), Bitmap.Config.ARGB_8888)
            // var textToDisplay = "Level %d, difficulty %.2f".format(game.currentStage.number, game.currentlyActiveStage?.data?.difficulty)
            val textToDisplay = "time per frame: %.2f ms.".format(gameView.gameMechanics.timeBetweenFrames)
            // var textToDisplay = "time per frame: %.2f ms. Ticks %d, frames %d.".format(game.timeBetweenFrames, game.ticksCount, game.frameCount)
            bitmap?.let {
                val canvas = Canvas(it)
                displayHeader(canvas, Rect(0, 0, area.width(), area.height()), textToDisplay)
            }
            lastValue = gameView.gameMechanics.timeBetweenFrames
        }
    }
}