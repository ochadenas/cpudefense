package com.example.cpudefense.gameElements

import android.graphics.*
import com.example.cpudefense.*
import com.example.cpudefense.networkmap.Viewport

class ScoreBoard(val game: Game): GameElement() {
    var area = Rect()
    var score = Score()
    var waves = Waves()
    var lives = Lives()
    var coins = Coins()
    var myColor = Color.WHITE
    var divider = 0  // between the display title and the actual display

    fun setSize(area: Rect) {
        this.area = Rect(area)
        divider = /* this.area.top + */ this.area.height() * 32 / 100
        score.setSize(area, divider)
        waves.setSize(area, divider)
        lives.setSize(area, divider)
        coins.setSize(area, divider)
    }

    fun addScore(amount: Int) {
        game.state.cash += amount
    }

    fun informationToString(number: Int): String {
        if (number < 512)
            return "%d bit".format(number)
        var bytes = number / 8
        if (bytes < 10000)
            return "%d B".format(bytes)
        bytes /= 1024
        if (bytes < 10000)
            return "%d KiB".format(bytes)
        bytes /= 1024
        if (bytes < 10000)
            return "%d MiB".format(bytes)
        bytes /= 1024
        if (bytes < 10000)
            return "%d GiB".format(bytes)
        bytes /= 1024
        return "%d TiB".format(bytes)
    }

    override fun update() {
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
        var paint = Paint()
        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
        canvas.drawRect(area, paint)
        paint.color = myColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawRect(area, paint)

        if (game.currentStage?.data?.level ?: 3 > 2)
            score.display(canvas)
        waves.display(canvas)
        lives.display(canvas)
        coins.display(canvas)
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
            var canvas = Canvas(bitmap)
            var rect = Rect(0, divider, area.width(), area.height())
            var text = informationToString(game.state.cash)
            var paint = Paint()
            paint.color = myColor
            paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL))
            paint.textSize = Game.scoreTextSize
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
            var canvas = Canvas(bitmap)
            displayHeader(canvas, Rect(0,0, area.width(), area.height()), game.resources.getString(R.string.scoreboard_waves))
            var rect = Rect(0, divider, area.width(), area.height())
            paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL))
            paint.style = Paint.Style.FILL
            paint.color = myColor
            paint.textSize = Game.scoreTextSize
            game.currentStage?.let {
                rect.displayTextCenteredInRect(canvas, "%d / %d".format(it.data.countOfWaves, it.data.maxWaves ), paint)
            }
        }
    }

    inner class Lives()
    {
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
            var ledArea = Rect(0, 0, ledAreaWidth, ledAreaHeight)
            // var ledArea = Rect(0, divider+(area.height()-ledAreaHeight)/2, ledAreaWidth, ledAreaHeight)
            // determine the exact position of the LEDs. This is a bit frickelig
            ledArea.setCenter(area.width()/2, (area.height()+divider)/2)
            val resources = game.resources
            if (game.state.lives <= 0)
                return
            val paint = Paint()
            paint.style = Paint.Style.FILL
            paint.color = resources.getColor(R.color.led_panel)
            canvas.drawRect(ledArea, paint)
            for (i in 1..game.state.currentMaxLives) {
                var rect = Rect(0, 0, sizeLedX, sizeLedY)
                rect.setCenter(ledArea.right - i * deltaX, ledArea.centerY())
                var glowRect = Rect(rect).inflate(3)
                paint.color =
                    if (i <= game.state.lives) resources.getColor(R.color.led_green_glow)
                    else resources.getColor(R.color.led_red_glow)
                canvas.drawRect(glowRect, paint)
                paint.color =
                    if (i <= game.state.lives) resources.getColor(R.color.led_green)
                    else resources.getColor(R.color.led_red)
                canvas.drawRect(rect, paint)
            }
            displayHeader(canvas, Rect(0,0, area.width(), area.height()), game.resources.getString(R.string.scoreboard_status))
        }
    }

    inner class Coins() {
        var area = Rect()
        var divider = 0

        var lastValue = -1   // used to detect value changes
        lateinit var bitmap: Bitmap
        val paint = Paint()

        fun setSize(area: Rect, divider: Int) {
            this.area = Rect(waves.area.right, area.top, lives.area.left, area.bottom)
            bitmap =
                Bitmap.createBitmap(this.area.width(), this.area.height(), Bitmap.Config.ARGB_8888)
            this.divider = divider
        }

        fun display(canvas: Canvas) {
            var coins = game.state.coinsInLevel + game.state.coinsExtra
            if (coins <= 0)
                return
            if (coins != lastValue) {
                /* only render the display if value has changed, otherwise re-use bitmap */
                lastValue = coins
                recreateBitmap(coins)
            }
            canvas.drawBitmap(bitmap, null, area, paint)
        }

        fun recreateBitmap(value: Int) {
            bitmap = Bitmap.createBitmap(area.width(), area.height(), Bitmap.Config.ARGB_8888)
            var canvas = Canvas(bitmap)

            var y = (divider + area.height()) / 2
            val deltaX = if (value > 1)
                (area.width() - (2 * Game.coinSizeOnScoreboard)) / (value - 1)
            else 0

            var x = area.width() - Game.coinSizeOnScoreboard
            var rect = Rect(0, 0, 50, 50)
            val paint = Paint()
            for (i in 0 until value) {
                rect.setCenter(x - i * deltaX, y)
                canvas.drawBitmap(game.coinIcon, null, rect, paint)
                displayHeader(canvas, Rect(0,0, area.width(), area.height()), game.resources.getString(R.string.scoreboard_coins))
            }
        }
    }

    fun displayHeader(canvas: Canvas, area: Rect, text: String) {
        var rect = Rect(area)
        rect.bottom = divider
        val paint = Paint()
        paint.color = game.resources.getColor(R.color.scoreboard_text)
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL))
        paint.textSize = Game.scoreHeaderSize
        rect.displayTextCenteredInRect(canvas, text, paint)
    }
}