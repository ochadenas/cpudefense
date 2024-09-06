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

    /** special background on selected levels */
    private var useSpecialBackground = false

    /** standard opacity of the background */
    val backgroundOpacity = 0.6f

    var opacity = backgroundOpacity
    var paint = Paint()

    /** a bitmap that has arbitrary dimensions. Usually bigger than the screen size.
     * Must be scaled to viewport dimensions whenever they change. */
    var wholeBackground: Bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888) // TODO: change the fixed size

    /** bitmap with a background design of the screens's proportions */
    var basicBackground: Bitmap? = null

    /** whether a background picture shall be used (by configuration) */
    var enabled = true

    fun prepareAtStartOfStage(stage: Stage.Identifier)
            /** called before starting a new stage. Gets a new backgriund image
             * and crops or scales it to the required size.
             */
    {
        enabled = !gameView.gameMechanics.gameActivity.settings.configDisableBackground
        if (enabled)
            loadWholeBitmapOfStage(stage)
    }

    fun setSize(width: Int, height: Int)
    {
        if (myArea.width() == width && myArea.height() == height) // size has not changed
            return // no need to recalculate
        if (width>0 && height>0)
        {
            myArea = Rect(0, 0, width, height)
            if (enabled)
                basicBackground = bitmapCroppedToSize(myArea)
            else
                basicBackground = createBlankBackground(myArea)
        }
    }

    fun display(canvas: Canvas)
    {
        paint.alpha = (255 * opacity).toInt()
        basicBackground?.let {
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
        val options = BitmapFactory.Options()
        options.inScaled = false
        return if (useSpecial)   // allows use of special backgrounds, currently disabled
            BitmapFactory.decodeResource(resources, R.drawable.background_flowers)
        else when (number)
        {
            1 -> BitmapFactory.decodeResource(resources, R.drawable.background_1, options)
            2 -> BitmapFactory.decodeResource(resources, R.drawable.background_2, options)
            3 -> BitmapFactory.decodeResource(resources, R.drawable.background_3, options)
            4 -> BitmapFactory.decodeResource(resources, R.drawable.background_4, options)
            5 -> BitmapFactory.decodeResource(resources, R.drawable.background_5, options)
            6 -> BitmapFactory.decodeResource(resources, R.drawable.background_6, options)
            7 -> BitmapFactory.decodeResource(resources, R.drawable.background_7, options)
            8 -> BitmapFactory.decodeResource(resources, R.drawable.background_8, options)
            9 -> BitmapFactory.decodeResource(resources, R.drawable.background_9, options)
            else -> BitmapFactory.decodeResource(resources, R.drawable.background_9, options)
        }
    }

    fun loadWholeBitmapOfStage(stageIdent: Stage.Identifier?)
            /** chooses the background to use,
             * and selects a random part of it
             * @param stageIdent Series and number of the current stage
             */
    {
        if (false && stageIdent?.let {it.series < 3 && it.number == 8 } == true)
            useSpecialBackground = true
        val n = stageIdent?.number ?: 0
        wholeBackground = loadWholeBitmap(n % maxBackgroundNumber + 1, useSpecialBackground)
    }

    private fun createBlankBackground(destRect: Rect): Bitmap
    /** @return an empty bitmap with the dimensions of the given rectangle */
    {
        val bitmap = basicBackground ?: Bitmap.createBitmap(destRect.width(), destRect.height(), Bitmap.Config.ARGB_8888)
        basicBackground = bitmap
        val canvas = Canvas(bitmap)
        canvas.drawColor(gameView.resources.getColor(R.color.network_background))
        return bitmap
    }

    private fun bitmapCroppedToSize(destRect: Rect): Bitmap
            /** returns a portion of wholeBackground in the given size.
             * May be either a cropped part if wholeBackground is bigger than the rectangle,
             * or a scaled image if it is smaller.
             */

    {
        var deltaX = wholeBackground.width - destRect.width()
        var deltaY = wholeBackground.height - destRect.height()

        // if the whole bitmap is smaller than the destination, scale it up, but keep the aspect ratio
        var newSize = Rect(0, 0, destRect.width(), destRect.height())
        val largeBitmap = if (deltaX<0 || deltaY<0)
        {
            val ratio = wholeBackground.height / wholeBackground.width.toFloat()
            if (deltaX<0) // stretch in x direction
                newSize.bottom = (newSize.right * ratio).toInt()
            if (deltaY<0) // stretch in y direction
                newSize.right = (newSize.bottom / ratio).toInt()
            Bitmap.createScaledBitmap(wholeBackground, newSize.width(), newSize.height(), false)
        }
        else wholeBackground

        // here, largeBitmap is at least as big as the destination rectangle (in both dimensions)
        deltaX = largeBitmap.width - destRect.width()
        deltaY = largeBitmap.height - destRect.height()
        val bitmap = createBlankBackground(destRect)
        val canvas = Canvas(bitmap)
        val displacementX = if (deltaX>0) Random.nextInt(deltaX) else 0
        val displacementY = if (deltaY>0) Random.nextInt(deltaY) else 0
        val sourceRect = Rect(destRect)
        paint.alpha = (opacity * 255).toInt()
        sourceRect.setTopLeft(displacementX, displacementY)
        canvas.drawBitmap(largeBitmap, sourceRect, destRect, paint)
        return bitmap
    }

}