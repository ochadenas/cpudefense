package com.example.cpudefense.effects

import android.graphics.*
import android.widget.Toast
import com.example.cpudefense.*
import com.example.cpudefense.utils.setCenter
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class Background(val game: Game)
/** The background shown during the game, showing a picture of real circuits.
 * This object is created whenever a game is started or resumed.
 * The actual image is only a part of the larger image, cut out at random positions.
 *
 * @property wholeImageOfCurrentStage The chosen background for this level. Might be bigger than the screen (viewport) size
 * @property actualImage A bitmap with the size of the viewport, cut out of the larger bitmap
  */
{
    private var backgroundNumber: Int = 0 // the selected background
    private var wholeImageOfCurrentStage: Bitmap? = null
    private var angle = 0.0
    var x = 0.0
    var y = 0.0
    private var projX = 0.0
    private var projY = 0.0
    var opacity = 0.5f
    var paint = Paint()
    var actualImage: Bitmap? = null // this is an image of the proportions of the screen
    var frozen = false

    enum class BackgroundState { DISABLED, UNINITIALIZED, BLANK, INITIALIZED }
    var state = BackgroundState.BLANK

    companion object {
        var bitmapsLoaded = false
        var availableBitmaps = hashMapOf<Int, Bitmap>()
    }
    // TODO: this should be moved to the application

    private fun loadBitmaps()
    /** loads the large background images as static objects into memory */
    {
        if (!bitmapsLoaded)
        {
            game.gameActivity.runOnUiThread {
                val toast: Toast = Toast.makeText(game.gameActivity, game.resources.getString(R.string.toast_loading), Toast.LENGTH_LONG)
                toast.show()
            }
            actualImage = null
            try {
                game.notification.showProgress(0.0f)
                availableBitmaps[1] = BitmapFactory.decodeResource(game.resources, R.drawable.background_1)
                game.notification.showProgress(0.10f)
                availableBitmaps[2] = BitmapFactory.decodeResource(game.resources, R.drawable.background_2)
                game.notification.showProgress(0.20f)
                availableBitmaps[3] = BitmapFactory.decodeResource(game.resources, R.drawable.background_3)
                game.notification.showProgress(0.30f)
                availableBitmaps[4] = BitmapFactory.decodeResource(game.resources, R.drawable.background_4)
                game.notification.showProgress(0.40f)
                availableBitmaps[5] = BitmapFactory.decodeResource(game.resources, R.drawable.background_5)
                game.notification.showProgress(0.50f)
                availableBitmaps[6] = BitmapFactory.decodeResource(game.resources, R.drawable.background_6)
                game.notification.showProgress(0.60f)
                availableBitmaps[7] = BitmapFactory.decodeResource(game.resources, R.drawable.background_7)
                game.notification.showProgress(0.70f)
                availableBitmaps[8] = BitmapFactory.decodeResource(game.resources, R.drawable.background_8)
                game.notification.showProgress(0.80f)
                availableBitmaps[9] = BitmapFactory.decodeResource(game.resources, R.drawable.background_9)
                game.notification.showProgress(0.90f)
                /*
                // code to provoke an Out Of Memory issue
                val myBitmaps = hashMapOf<Int, Bitmap>()
                for (i in 0 until 100)
                {
                    try {
                        myBitmaps[i] = BitmapFactory.decodeResource(game.resources, R.drawable.background_6)
                    }
                    catch (e: java.lang.Exception)
                    {
                    }
                }
                 */
                 bitmapsLoaded = true
            }
            catch (e: java.lang.Exception)
            {
                bitmapsLoaded = true
            // keep the bitmaps loaded so far,
            // avoid further attempts to load more bitmaps
            }
            finally {
                game.notification.hide()
            }
        }
    }

    fun choose(number: Int, opacity: Float = 0.6f)
            /** chooses the background to use,
             * and selects a random part of it
             * @param number selects one of the available backgrounds
             * @param opacity sets the opacity, from 0.0 to 1.0
             */
    {
        loadBitmaps()
        backgroundNumber = number % availableBitmaps.size + 1
        this.opacity = opacity
        angle = Random.nextDouble() * 2 * Math.PI
        projX = cos(angle)
        projY = sin(angle)
        state = BackgroundState.UNINITIALIZED
    }

    fun update(): Boolean
            /** called in regular time intervals.
             * @return true if the background image must be changed, false otherwise
             */
    {
        return (this.state == BackgroundState.BLANK)
    }

    private fun blankImage(): Bitmap?
    {
        if (game.viewport.screen.width() == 0 || game.viewport.screen.height() == 0)
            return null  // can't determine screen dimensions
        else
            return Bitmap.createBitmap(game.viewport.screen.width(), game.viewport.screen.height(), Bitmap.Config.ARGB_8888)
    }
    fun getImage(): Bitmap?
            /** provides the background image. If necessary, recreate the bitmap.
             * If the screen dimensions are unknown, return null.
             * @return the bitmap, or NULL if none can be provided.
             */
    {
        if (actualImage == null || state == BackgroundState.UNINITIALIZED) {
            actualImage = blankImage()
            state = BackgroundState.BLANK
        }
        if (game.gameActivity.settings.configDisableBackground) {
            actualImage = blankImage()
            actualImage?.let { Canvas(it).drawColor(game.gameActivity.theGameView.backgroundColour) }
            state = BackgroundState.DISABLED
        }
        // draw background on canvas
        actualImage?.let {
            val background = when (state) {
                BackgroundState.DISABLED -> it
                BackgroundState.UNINITIALIZED -> it // should not happen
                BackgroundState.BLANK -> {
                    loadBitmaps()
                    wholeImageOfCurrentStage = availableBitmaps[backgroundNumber]
                    if (bitmapsLoaded)
                        state = BackgroundState.INITIALIZED
                    paintOnBackgroundImage(it)
                }
                BackgroundState.INITIALIZED -> it
            }
            actualImage = background
        }
        return actualImage
    }

    private fun paintOnBackgroundImage(bitmap: Bitmap): Bitmap
            /** paints a selection out of the whole background image onto the existing bitmap
             * @param bitmap The bitmap where to paint on. Should be a blank canvas.
             * @return the bitmap containing the image */
    {
        this.wholeImageOfCurrentStage?.let {
            paint.alpha = (255 * opacity).toInt()
            val dest = Rect(0, 0, bitmap.width, bitmap.height)
            val source = Rect(dest)
            val x = it.width / 2 + projX * 0.5f * (it.width - dest.width())
            val y = it.height / 2 + projY * 0.5f * (it.height - dest.height())
            source.setCenter(x.toInt(), y.toInt())
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.BLACK)
            canvas.drawBitmap(it, source, dest, paint)
        }
        return bitmap
    }


}