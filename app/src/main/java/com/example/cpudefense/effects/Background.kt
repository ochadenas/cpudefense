package com.example.cpudefense.effects

import android.content.res.Resources
import android.graphics.*
import android.widget.Toast
import com.example.cpudefense.*
import com.example.cpudefense.utils.setTopLeft
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlin.random.Random

class Background(val gameView: GameView)
/** The background shown during the game, showing a picture of real circuits.
 * This object is created whenever a game is started or resumed.
 * The actual image is only a part of the larger image, cut out at random positions.
 *
 * @property wholeImageOfCurrentStage The chosen background for this level. Might be bigger than the screen (viewport) size
 * @property basicBackground A bitmap with the size of the viewport, cut out of the larger bitmap
  */
{
    /** area (in pixels) where to draw the background on */
    var myArea = Rect()
    /** number of different background pictures available */
    private val maxBackgroundNumber = 9
    /** number of selected background */
    private var backgroundNumber: Int = 0
    /** special background on selected levels */
    private var useSpecialBackground = false
    var opacity = 0.5f
    var paint = Paint()
    /** bitmap with a background design of the screens's proportions */
    var basicBackground: Bitmap? = null
    /** bitmap with the network picture painted on the background */
    var currentBackground: Bitmap? = null
    /** whether a background picture shall be used (by configuration) */
    var enabled = true
    /** standard opacity of the background */
    val backgroundOpacity = 0.6f

    enum class BackgroundState { DISABLED, UNINITIALIZED, BLANK, INITIALIZED }
    var state = BackgroundState.BLANK

    fun initializeAtStartOfGame()
    {
        enabled = gameView.gameMechanics.gameActivity.settings.configDisableBackground
        state = BackgroundState.UNINITIALIZED
    }

    fun prepareAtStartOfStage(stage: Stage.Identifier)
    {
        // TODO: hier nur das Bild laden, aber noch nicht skalieren oder auschneiden
        createImage(stage, backgroundOpacity)
    }

    fun setSize(area: Rect)
    {
        // TODO: hier das Bild ausschneiden und skalieren, aber nicht mehr laden
        myArea = Rect(area)
    }

    fun paintNetworkOnBackground(bitmapForeground: Bitmap)
    /**
     * paints the given bitmap on the "basic" background picture
     * */
    {
        if (state == BackgroundState.INITIALIZED)
        {
            if (myArea.width() == 0 || myArea.height() == 0)
                return
            paint.alpha = 255
            currentBackground?.let {
                val canvas = Canvas(it)
                canvas.drawBitmap(bitmapForeground, null, myArea, paint)
            }
        }
    }

    fun display(canvas: Canvas)
    {
        if (state == BackgroundState.INITIALIZED)
            currentBackground?.let {
                paint.alpha = 255
                canvas.drawBitmap(it, null, myArea, paint)
            }
    }

    private fun loadWholeBitmap(number: Int, useSpecial: Boolean = false): Bitmap
    /** loads a large background image into memory
     * @param number the number of the background chosen. Must be between 1 and maxBackgroundNumber */
    {
        val resources: Resources = gameView.resources
        gameView.gameMechanics.gameActivity.runOnUiThread() {
            Toast.makeText(gameView.gameMechanics.gameActivity, resources.getString(R.string.toast_loading), Toast.LENGTH_SHORT).show() }
        return if (useSpecial)   // allows use of special backgrounds, currently disabled
            BitmapFactory.decodeResource(resources, R.drawable.background_flowers)
        else when (number)
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

    fun loadWholeBitmapOfStage(stageIdent: Stage.Identifier?): Bitmap
            /** chooses the background to use,
             * and selects a random part of it
             * @param stageIdent Series and number of the current stage
             * @param opacity sets the opacity, from 0.0 to 1.0
             */
    {
        if (false && stageIdent?.let {it.series < 3 && it.number == 8 } == true)
            useSpecialBackground = true
        val n = stageIdent?.number ?: 0
        this.opacity = opacity
        return loadWholeBitmap(n % maxBackgroundNumber + 1, useSpecialBackground)
    }

    fun createImage(stageIdent: Stage.Identifier?, opacity: Float = 0.6f)
            /** recreates the background image as a part of the larger image loaded from disk.
             * @param stageIdent The stage for which the background shall be created
             * @param opacity Alpha of the background, from 0.0 to 1.0. Lower values mean a dimmer picture.
             */
    {
        basicBackground = createBlankBackground()
        if (enabled)
        {
            val largeBitmap = loadWholeBitmapOfStage(stageIdent)
            basicBackground?.let {
                val displacementX = if (largeBitmap.width>it.width)
                    Random.nextInt(largeBitmap.width-it.width) else 0
                val displacementY = if (largeBitmap.height > it.height)
                    Random.nextInt(largeBitmap.height-it.height) else 0
                val destRect = Rect(0,0,it.width,it.height)
                val sourceRect = Rect(destRect)
                sourceRect.setTopLeft(displacementX, displacementY)
                val canvas = Canvas(it)
                paint.alpha = (255 * opacity).toInt()
                canvas.drawBitmap(it, sourceRect, destRect, paint)
                state = BackgroundState.INITIALIZED
            }
        }
    }

    private fun createBlankBackground(): Bitmap
    /** @return an empty bitmap with the dimensions of the screen */
    {
        val bitmap = basicBackground ?: Bitmap.createBitmap(gameView.viewport.screen.width(),
                                                            gameView.viewport.screen.height(),
                                                            Bitmap.Config.ARGB_8888)
        basicBackground = bitmap
        val canvas = Canvas(bitmap)
        canvas.drawColor(gameView.resources.getColor(R.color.network_background))
        state = BackgroundState.BLANK
        return bitmap
    }

    private fun getBasicBackground(stageIdent: Stage.Identifier? = null): Bitmap
    /** @return the background picture without network. Creates the bitmap if necessary. */
    {
        if (basicBackground == null || basicBackground?.width == 0)
            createImage(stageIdent) // this is a fallback in case that the background doesn't exist
        return basicBackground ?: createBlankBackground()
    }

}