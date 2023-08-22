package com.example.cpudefense.effects

import android.graphics.*
import com.example.cpudefense.*
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.setCenter
import kotlin.math.sin
import kotlin.math.cos

class Background(val game: Game) {
    var bitmap: Bitmap? = null
    private var angle = 0.0
    var x = 0.0
    var y = 0.0
    private var projX = 0.0
    private var projY = 0.0
    var opacity = 0.5f
    var paint = Paint()
    var actualImage: Bitmap? = null
    private var ticks = 0
    var frozen = false

    // parameters:
    private val deltaAlpha = 0.00008
    private val ticksBeforeUpdate = 12

    companion object {
        var bitmapsLoaded = false
        var availableBitmaps = hashMapOf<Int, Bitmap>()
    }

    private fun loadBitmaps() {
        if (!bitmapsLoaded)
        {
            actualImage = null
            try {
                game.notification.showProgress(0.0f)
                availableBitmaps[1] = BitmapFactory.decodeResource(game.resources, R.drawable.background_1)
                game.notification.showProgress(0.15f)
                availableBitmaps[2] = BitmapFactory.decodeResource(game.resources, R.drawable.background_2)
                game.notification.showProgress(0.30f)
                availableBitmaps[3] = BitmapFactory.decodeResource(game.resources, R.drawable.background_3)
                game.notification.showProgress(0.45f)
                availableBitmaps[4] = BitmapFactory.decodeResource(game.resources, R.drawable.background_4)
                game.notification.showProgress(0.60f)
                availableBitmaps[5] = BitmapFactory.decodeResource(game.resources, R.drawable.background_5)
                game.notification.showProgress(0.75f)
                availableBitmaps[6] = BitmapFactory.decodeResource(game.resources, R.drawable.background_6)
                game.notification.showProgress(0.90f)
                /*
                // code to provoke an Out Of Memory issue
                val myBitmaps = hashMapOf<Int, Bitmap>()
                for (i in 0 until 100)
                {
                    try {
                        // game.notification.showProgress(i * 0.01f)
                        bitmap = BitmapFactory.decodeResource(game.resources, R.drawable.background_6)
                        myBitmaps[i] = bitmap
                    }
                    catch (e: java.lang.Exception)
                    {
                        throw (OutOfMemoryError())
                    }
                }
                 */
                 bitmapsLoaded = true
            }
            catch (e: java.lang.Exception)
            {
                // throw away the bitmaps decoded so far:
                for (bitmap in availableBitmaps.values)
                    bitmap.recycle()
                availableBitmaps.clear()
            }
            finally {
                game.notification.hide()
            }
        }
    }

    @Synchronized
    fun choose(number: Int, opacity: Float = 0.3f)
            /** selects the background to use.
             * @param number selects one of the available backgrounds
             * @param opacity sets the opacity, from 0.0 to 1.0
             */
    {
        loadBitmaps() // if necessary
        val n = number % availableBitmaps.size
        this.bitmap = availableBitmaps[n]
        this.opacity = opacity
    }

    fun update()
    {
        if (actualImage == null)
            initializeImage()
        if (frozen)
            return
        angle += deltaAlpha
        ticks--
        if (ticks < 0)
        {
            ticks = ticksBeforeUpdate
            projX = cos(angle)
            projY = sin(angle)
            recreateBackgroundImage(game.viewport)
        }
    }

    private fun initializeImage()
    {
        if (game.viewport.viewportWidth == 0)
            return   // dimensions are still not known
        actualImage = Bitmap.createBitmap(game.viewport.viewportWidth, game.viewport.viewportHeight, Bitmap.Config.ARGB_8888)
    }

    private fun recreateBackgroundImage(viewport: Viewport)
    {
        val destWidth = viewport.viewportWidth
        val destHeight = viewport.viewportHeight
        val largeImage: Bitmap = this.bitmap ?: return
        actualImage?.let {
            paint.alpha = (255 * opacity).toInt()
            val source = Rect(0,0, destWidth, destHeight)
            val x = largeImage.width/2 + projX * 0.5f * (largeImage.width - destWidth)
            val y = largeImage.height/2 + projY * 0.5f * (largeImage.height - destHeight)
            source.setCenter(x.toInt(),y.toInt())
            val canvas = Canvas(it)
            // canvas.drawColor(Color.BLACK)
            canvas.drawBitmap(largeImage, source, viewport.getRect(), paint)
        }
    }


}