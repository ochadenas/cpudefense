package com.example.cpudefense.effects

import android.content.res.Resources
import android.graphics.*
import com.example.cpudefense.*
import com.example.cpudefense.utils.setTopLeft
import kotlin.random.Random

class Background(val gameView: GameView)
/** The background shown during the game, showing a picture of real circuits.
 * This object is created whenever a game is started or resumed.
 * The actual image is only a part of the larger image, cut out at random positions.
 *
 * @property wholeImageOfCurrentStage The chosen background for this level. Might be bigger than the screen (viewport) size
 * @property actualImage A bitmap with the size of the viewport, cut out of the larger bitmap
  */
{
    private var backgroundNumber: Int = 0 // the selected background
    private var useSpecialBackground = false  // special background on selected levels
    private var wholeImageOfCurrentStage: Bitmap? = null
    var opacity = 0.5f
    var paint = Paint()
    var actualImage: Bitmap? = null // this is an image of the proportions of the screen
    var frozen = false
    var enabled = true

    enum class BackgroundState { DISABLED, UNINITIALIZED, BLANK, INITIALIZED }
    var state = BackgroundState.BLANK

    companion object {
        var bitmapsLoaded = false
        var availableBitmaps = hashMapOf<Int, Bitmap>()
        var specialBitmap: Bitmap? = null
    }

    fun initializeAtStartOfGame()
    {
        enabled = gameView.gameMechanics.gameActivity.settings.configDisableBackground
        state = Background.BackgroundState.UNINITIALIZED
    }

    fun prepareAtStartOfStage()
    {

    }

    fun paintNetworkOnBackground(bitmapForeground: Bitmap)
    {
    }

    fun display(canvas: Canvas)
    {

    }


    private fun loadBitmap(number: Int, useSpecial: Boolean = false)
    /** loads the large background images as static objects into memory */
    {
        val resources: Resources = gameView.resources
        if (useSpecial)   // allows use of special backgrounds, currently disabled
            wholeImageOfCurrentStage = BitmapFactory.decodeResource(resources, R.drawable.background_flowers)
        else
            wholeImageOfCurrentStage = when (number)
            {
                1 -> BitmapFactory.decodeResource(resources, R.drawable.background_1)
                2 -> BitmapFactory.decodeResource(resources, R.drawable.background_2)
                3 -> BitmapFactory.decodeResource(resources, R.drawable.background_3)
                4 -> BitmapFactory.decodeResource(resources, R.drawable.background_4)
                5 -> BitmapFactory.decodeResource(resources, R.drawable.background_5)
                6 -> BitmapFactory.decodeResource(resources, R.drawable.background_6)
                7 -> BitmapFactory.decodeResource(resources, R.drawable.background_7)
                8 -> BitmapFactory.decodeResource(resources, R.drawable.background_8)
                9 -> BitmapFactory.decodeResource(resources, R.drawable.background_9)
                else -> BitmapFactory.decodeResource(resources, R.drawable.background_9)
            }
    }

    fun choose(stageIdent: Stage.Identifier?, opacity: Float = 0.6f)
            /** chooses the background to use,
             * and selects a random part of it
             * @param stageIdent Series and number of the current stage
             * @param opacity sets the opacity, from 0.0 to 1.0
             */
    {
        if (false && stageIdent?.let {it.series < 3 && it.number == 8 } == true)
            useSpecialBackground = true
        val n = stageIdent?.number ?: 0
        loadBitmap(n % availableBitmaps.size + 1,  useSpecialBackground)
        this.opacity = opacity
    }

    fun getImage(): Bitmap?
            /** provides the background image. If necessary, recreate the bitmap.
             * If the screen dimensions are unknown, return null.
             * @return the bitmap, or NULL if none can be provided.
             */
    {
        if (actualImage == null || state == BackgroundState.UNINITIALIZED)
        {
            if (enabled)
                createBackgroundImage()
        }


        if (activity.settings.configDisableBackground) {
            actualImage = blankImage()
            actualImage?.let { Canvas(it).drawColor(activity.gameView.backgroundColour) }
            state = BackgroundState.DISABLED
        }
        // draw background on canvas
        actualImage?.let {
            val background = when (state) {
                BackgroundState.DISABLED -> it
                BackgroundState.UNINITIALIZED -> it // should not happen
                BackgroundState.BLANK -> {
                    loadBitmaps()
                    wholeImageOfCurrentStage = if (useSpecialBackground) specialBitmap else availableBitmaps[backgroundNumber]
                    if (bitmapsLoaded)
                        state = BackgroundState.INITIALIZED
                    createBackgroundImage(it)
                }
                BackgroundState.INITIALIZED -> it
            }
            actualImage = background
        }
        return actualImage
    }

    private fun createBlankBackground(): Bitmap
    {
        val bitmap = actualImage ?: Bitmap.createBitmap(gameView.viewport.screen.width(),
                                              gameView.viewport.screen.height(),
                                              Bitmap.Config.ARGB_8888)
        actualImage = bitmap
        val canvas = Canvas(bitmap)
        canvas.drawColor(gameView.resources.getColor(R.color.network_background))
        state = BackgroundState.BLANK
        return bitmap
    }

    private fun createBackgroundImage(): Bitmap
            /** paints a selection out of the whole background image onto the existing bitmap
             * @param bitmap The bitmap where to paint on. Should be a blank canvas.
             * @return the bitmap containing the image */
    {
        createBlankBackground()
        this.wholeImageOfCurrentStage?.let {
            paint.alpha = (255 * opacity).toInt()
            val dest = Rect(0, 0, bitmap.width, bitmap.height)
            val source = Rect(dest)
            val displacementX = Random.nextInt(bitmap.width)
            val displacementY = Random.nextInt(bitmap.height)
            source.setTopLeft(displacementX, displacementY)
            val canvas = Canvas(bitmap)
            canvas.drawBitmap(it, source, dest, paint)
            state = BackgroundState.INITIALIZED
        }
        return bitmap
    }


}