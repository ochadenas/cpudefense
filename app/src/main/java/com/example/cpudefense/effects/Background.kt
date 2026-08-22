@file:Suppress("DEPRECATION")

package com.example.cpudefense.effects

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.example.cpudefense.GameMechanics
import com.example.cpudefense.R
import com.example.cpudefense.Stage
import com.example.cpudefense.utils.setTopLeft
import kotlin.math.max
import kotlin.random.Random
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.example.cpudefense.CommonView
import com.example.cpudefense.networkmap.Coord
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.setCenter

class Background(val commonView: CommonView)
/** The background shown during the game, showing a picture of real circuits.
 * This object is created whenever a game is started or resumed.
 * The actual image is only a part of the larger image, cut out at random positions.
  */
{
    /** area (in pixels) where to draw the background on */
    private var myArea = Rect()

    /** number of different background pictures available */
    private val maxBackgroundNumber = 9

    /** standard opacity of the background */
    private val backgroundOpacity = 0.6f

    var opacity = backgroundOpacity
    var paint = Paint()

    /** the x coordinate of the "window" on the bigger bitmap that is to be displayed */
    var displacementX = 0
    /** the y coordinate of the "window" on the bigger bitmap that is to be displayed */
    var displacementY = 0


    /** a bitmap that has arbitrary dimensions. Usually bigger than the screen size.
     * Must be scaled to viewport dimensions whenever they change. */
    private var wholeBackground: Bitmap? = null

    /** bitmap with a background design of the screen's proportions */
    var basicBackground: Bitmap? = null

    /** the grid, if any. The bitmap holds the whole grid and may extend the visible part of the screen */
    var wholeGridBackground: Bitmap? = null

    /** visiblie portion of the grid */
    var gridBackground: Bitmap? = null

    /** whether a background picture shall be used (by configuration) */
    private var enabled = true

    /** called before starting a new stage. Gets a new background image
     * and crops or scales it to the required size.
     */
    fun prepareAtStartOfStage(stage: Stage.Identifier)
    {
        enabled = !commonView.settings().configDisableBackground
        if (enabled) {
            loadWholeBitmapOfStage(stage)
            setBackgroundDimensions(commonView.width, commonView.height)
        }
    }

    /** creates the background for the editor view
     */
    fun prepareForEditor()
    {
        setBackgroundDimensions(commonView.width, commonView.height)
        basicBackground = createBlankBackground(myArea)
        createGrid(viewport = commonView.viewport, commonView.resources.getColor(R.color.background_griddots))
    }

    /** Sets the size of the background and re-creates the image.
     * @param forceNewBackground If true, forcibly create a new image. Otherwise, keep the old one
     * if the size has not changed.
     */
    fun setBackgroundDimensions(width: Int, height: Int, forceNewBackground: Boolean = false)
    {
        if (forceNewBackground || width!=myArea.width() || height!=myArea.height())
        {
            myArea = Rect(0, 0, width, height)
            setBasicBackground()
        }
    }

    /** sets the background to the image */
    private fun setBasicBackground()
    {
        if (myArea.width()==0 || myArea.height()==0)
            return
        if (enabled)
            wholeBackground?.let { basicBackground = bitmapCroppedToSize(myArea, it) }
        else
            basicBackground = createBlankBackground(myArea)
    }

    fun display(canvas: Canvas)
    {
        paint.alpha = (255 * opacity).toInt()
        basicBackground?.let {
            canvas.drawBitmap(it, null, myArea, paint)
        }
    }

    fun displayGrid(canvas: Canvas)
    {
        paint.alpha = 255
        wholeGridBackground?.let {
            with (commonView.viewport)
            {
                val viewportRect = Rect(myArea)
                    .setTopLeft(-(offsetX * userScale).toInt(), -(offsetY * userScale).toInt())
                canvas.drawBitmap(it, viewportRect, myArea, paint)
            }
        }
    }

    /** loads a large background image into memory
     * @param number the number of the background chosen. Must be between 1 and maxBackgroundNumber */
    private fun loadWholeBitmap(number: Int, useSpecial: GameMechanics.Params.Season = GameMechanics.Params.Season.DEFAULT): Bitmap
    {
        val resources: Resources = commonView.resources
        // since loading now happens in small chunks, there is no need to display the toast */
        /*
        gameView.gameMechanics.gameActivity.runOnUiThread {
            Toast.makeText(gameView.gameMechanics.gameActivity, resources.getString(R.string.toast_loading), Toast.LENGTH_SHORT).show() }
         */
        val options = BitmapFactory.Options()
        options.inScaled = false
        return when (useSpecial) {
            GameMechanics.Params.Season.EASTER -> BitmapFactory.decodeResource(resources, R.drawable.background_flowers)
            GameMechanics.Params.Season.CHRISTMAS -> BitmapFactory.decodeResource(resources, R.drawable.background_winter)
            else -> when (number) {
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
    }

    /** chooses the background to use,
     * and selects a random part of it as wholeBackground
     * @param stageIdent Series and number of the current stage
     */
    private fun loadWholeBitmapOfStage(stageIdent: Stage.Identifier)
    {
        val useSpecialBackground = GameMechanics.specialLevel(stageIdent)
        if (useSpecialBackground == GameMechanics.Params.Season.CHRISTMAS)
            commonView.effects?.addSnow()
        val n = stageIdent.number
        wholeBackground = loadWholeBitmap(n % maxBackgroundNumber + 1, useSpecialBackground)
    }

    /** @return an empty bitmap with the dimensions of the given rectangle */
    private fun createBlankBackground(destRect: Rect): Bitmap
    {
        val bitmap = basicBackground ?: createBitmap(destRect.width(), destRect.height())
        basicBackground = bitmap
        val canvas = Canvas(bitmap)
        canvas.drawColor(commonView.resources.getColor(R.color.network_background))
        return bitmap
    }

    /** draws a grid (small points on all grid positions) */
    fun createGrid(viewport: Viewport, gridColor: Int)
    {
        if (!viewport.isValid)
            return
        wholeGridBackground = createBitmap(viewport.gridWidth, viewport.gridHeight, Bitmap.Config.ARGB_8888)
        wholeGridBackground?.let { bitmap ->
            val paint = Paint().also {
                it.color = gridColor
                it.style = Paint.Style.STROKE
                it.strokeWidth = CommonView.gridBorderWidth
            }
            val canvas = Canvas(bitmap)
            val rect = Rect(0, 0, bitmap.width, bitmap.height)
            canvas.drawRect(rect, paint)
            val gridSize = viewport.viewportData.gridSize
            repeat(gridSize.first.toInt()) { x ->
                repeat(gridSize.second.toInt()) { y ->
                    val pos = viewport.gridToScreen(Coord(x, y))
                    rect.setCenter(pos)
                    canvas.drawPoint(pos.first.toFloat(), pos.second.toFloat(), paint)
                }
            }
        }
    }

    /** returns a portion of wholeBackground in the given size.
     * May be either a cropped part if wholeBackground is bigger than the rectangle,
     * or a scaled image if it is smaller.
     */
    private fun bitmapCroppedToSize(destRect: Rect, sourceBitmap: Bitmap): Bitmap
    {
        // if the whole bitmap is smaller than the destination, scale it up, but keep the aspect ratio
        val sourceX = sourceBitmap.width
        val sourceY = sourceBitmap.height
        val scaleX: Float = (destRect.width() / sourceX.toFloat())
        val scaleY: Float = (destRect.height() / sourceY.toFloat())
        val scale = max(scaleX, scaleY)
        val largeBitmap = if (scale > 1.0f)
            sourceBitmap.scale((sourceX * scale).toInt(), (sourceY * scale).toInt(), false)
        else sourceBitmap

        // here, largeBitmap is at least as big as the destination rectangle (in both dimensions)
        val deltaX = largeBitmap.width - destRect.width()
        val deltaY = largeBitmap.height - destRect.height()
        displacementX = if (deltaX>0) Random.nextInt(deltaX) else 0
        displacementY = if (deltaY>0) Random.nextInt(deltaY) else 0

        val bitmap = createBlankBackground(destRect)
        val canvas = Canvas(bitmap)
        val sourceRect = Rect(destRect)
        paint.alpha = (opacity * 255).toInt()
        sourceRect.setTopLeft(displacementX, displacementY)
        canvas.drawBitmap(largeBitmap, sourceRect, destRect, paint)
        return bitmap
    }
}