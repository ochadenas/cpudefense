package com.example.cpudefense.networkmap

import android.graphics.Rect
import com.example.cpudefense.GameView
import com.example.cpudefense.gameElements.Chip
import kotlin.collections.component1
import kotlin.collections.component2

class Viewport
/** class that is responsible for mapping internal grid [Coord] to screen coordinates */
{
    data class Data(
        /** size of the complete game board, in grid coordinates */
        var gridSize: Pair<Float, Float> = Pair(0f, 0f)
    )
    var viewportData = Data()

    /** width of the viewport in screen coordinates */
    var viewportWidth: Int = 0
    /** height of the viewport in screen coordinates */
    var viewportHeight: Int = 0
    /** safety margin in order to avoid the grid get shifted out of the screen */
    var viewportSafetyMargin: Int = 0
    /** visible portion of the screen, in screen coordinates */
    var screen = Rect()
    /** width of the whole grid in screen coordinates (can extend the visible viewport) */
    var gridWidth: Int = 0
    /** height of the whole grid in screen coordinates (can extend the visible viewport) */
    var gridHeight: Int = 0
    /** used to keep track whether the network elements must be recalculated after scale changes */
    var scaleHasChanged = true
    /** horizontal scale factor applied */
    private var scaleX = 1.0f
    /** vertical scale factor applied */
    private var scaleY = 1.0f
    /** horizontal offset to the origin when moving the viewport around, in screen coordinates. Positive values mean that the network is shifted towards the right. */
    private var offsetX = 0
    /** vertical offset to the origin when moving the viewport around, in screen coordinates. Positive values mean that the network is shifted towards the bottom. */
    private var offsetY = 0

    /** zoom factor chosen by the player */
    var userScale = 1.0f
    /** general screen scale factor, based on Density */
    var scaleFactor = 1.0f
    /** whether the viewport has been initialized with width and height */
    var isValid = false

    /** default grid size that fits on screen without scrolling */
    private val standardGridSize: Coord = Coord(46, 60)

    /** sets the size in screen coordinates */
    fun determineScreenSize(width: Int, height: Int, scaleFactor: Float)
    {
        if (width == 0 || height == 0)
            isValid = false
        else
        {
            this.scaleFactor = scaleFactor
            screen = Rect(0, 0, width, height)
            viewportWidth = width - (2 * GameView.viewportMargin*scaleFactor).toInt()
            viewportHeight = height - (2 * GameView.viewportMargin*scaleFactor).toInt()
            viewportSafetyMargin = ((width+height) * 0.2f).toInt()
            calculateScale()
        }
    }

    /** resets the viewport zoom and displacement, e.g. at the start of a new stage.
     * Optional offsets can be given in screen coordinates.
     */
    fun reset()
    {
        offsetX = 0
        offsetY = 0
        userScale = 1.0f
        scaleHasChanged = true
    }

    /** sets the size in grid coordinates */
    fun determineGridSize(gridSize: Coord)
    {
        viewportData.gridSize = gridSize.asPair()
        calculateScale()
    }
    /** sets the size in grid coordinates */
    fun determineGridSize(gridSizeX: Int, gridSizeY: Int)
    {
        viewportData.gridSize = Pair(gridSizeX.toFloat(), gridSizeY.toFloat())
        calculateScale()
    }

    /** given the viewport width and height, calculates the x and y scale factors.
     * To be called whenever the size of the screen changes.
     * @param newScale the additional scale factor set by the user
     */
    private fun calculateScale(newScale: Float? = null)
    {
        val width = viewportWidth.toFloat() // screen coords
        val height = viewportHeight.toFloat()
        if (width >= 0f && height >= 0f) {
            scaleX = viewportWidth.toFloat() / standardGridSize.x
            scaleY = viewportHeight.toFloat() / standardGridSize.y
            if (scaleX > scaleY * 1.4f) scaleX = scaleY * 1.2f
            if (scaleY > scaleX * 1.4f) scaleY = scaleX * 1.2f
            newScale?.let { userScale = it }
            scaleX *= userScale
            scaleY *= userScale
            isValid = true
            scaleHasChanged = true
            gridWidth = (viewportData.gridSize.first * scaleX).toInt()
            gridHeight = (viewportData.gridSize.second * scaleY).toInt()
        }
        else
            isValid = false
    }

    /** add an offset to move the viewport around.
     * Positive values move the network to the left and down. */
    fun addOffset(deltaX: Float, deltaY: Float)
    {
        if (deltaX>0 && offsetX<viewportWidth-viewportSafetyMargin)
            offsetX += deltaX.toInt()
        if (deltaX<0 && offsetX>-gridWidth+viewportSafetyMargin)
            offsetX += deltaX.toInt()
        if (deltaY>0 && offsetY<viewportHeight-viewportSafetyMargin)
            offsetY += deltaY.toInt()
        if (deltaY<0 && offsetY>-gridHeight+viewportSafetyMargin)
            offsetY += deltaY.toInt()
    }

    /** set a new scale factor */
    fun scale(param: Float)
    {
        val factor = 1f + (param-1f) * 0.8f
        // avoid scaling too small or too big
        val newScale = userScale * factor
        if (newScale !in 0.25 ..3.2)
            return
        calculateScale(newScale)
        // re-center the viewport
        addOffset(-(screen.width() * (factor-1)).toInt() / 2f,
                  - (screen.height() * (factor-1)).toInt() / 2f)
    }

    /** scale up or down in discrete steps */
    fun scaleByStep(network: Network?, zoomIn: Boolean = true)
    {
        val factor = if (zoomIn) 1.2f else 0.8f
        scale(factor)

        network?.let {
            it.nodes.forEach{ (_, chip) -> (chip as? Chip)?.upgradePossibilities?.clear() }  // clear upgrade boxes, they mess up scaling
            it.applyScale(this)
            it.recreateNetworkImage(false)
        }
    }

    /** converts a point in grid coordinates into screen coordinates */
    fun gridToScreen(gridPos: Coord): Pair<Int, Int>
    {
        val posX = gridPos.x * scaleX + GameView.viewportMargin + offsetX
        val posY = gridPos.y * scaleY + GameView.viewportMargin + offsetY
        return Pair(posX.toInt(), posY.toInt())
    }

    /** converts a rectangle given in grid coordinates */
    fun rectToScreen(rectInGridCoord: Rect): Rect
    {
        val upperLeft = gridToScreen(Coord(rectInGridCoord.left, rectInGridCoord.top))
        val lowerRight = gridToScreen(Coord(rectInGridCoord.right, rectInGridCoord.bottom))
        return Rect(upperLeft.first, upperLeft.second, lowerRight.first, lowerRight.second)
    }

    /** determines which half of the viewport the point is in. */
    fun isInRightHalfOfViewport(posX: Int): Boolean
    {
        return posX > viewportWidth / 2
    }

}