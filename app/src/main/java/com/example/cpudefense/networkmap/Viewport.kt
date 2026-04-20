package com.example.cpudefense.networkmap

import android.graphics.Rect
import com.example.cpudefense.GameView

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
    /** vertical size of the complete game board, in grid coordinates */
    private var scaleX = 1.0f
    private var scaleY = 1.0f
    /** offset to the origin when moving the viewport around, in screen coordinates */
    private var offsetX = 0
    private var offsetY = 0

    /** zoom factor chosen by the player */
    var userScale = 1.0f
    /** whether the viewport has been initialized with width and height */
    var isValid = false

    /** default grid size that fits on screen without scrolling */
    private val standardGridSize: Coord = Coord(46, 60)

    fun determineScreenSize(width: Int, height: Int)
    /** sets the size in screen coordinates */
    {
        if (width == 0 || height == 0)
            isValid = false
        else
        {
            screen = Rect(0, 0, width, height)
            viewportWidth = width - 2 * GameView.viewportMargin
            viewportHeight = height - 2 * GameView.viewportMargin
            viewportSafetyMargin = ((width+height) * 0.2f).toInt()
            calculateScale()
        }
    }

    fun reset()
    {
        offsetX = 0
        offsetY = 20
        userScale = 1.0f
        scaleHasChanged = true
    }

    fun determineGridSize(gridSize: Coord)
    /** sets the size in grid coordinates */
    {
        viewportData.gridSize = gridSize.asPair()
        calculateScale()
    }
    fun determineGridSize(gridSizeX: Int, gridSizeY: Int)
            /** sets the size in grid coordinates */
    {
        viewportData.gridSize = Pair(gridSizeX.toFloat(), gridSizeY.toFloat())
        calculateScale()
    }

    private fun calculateScale(newScale: Float? = null)
            /** given the viewport width and height, calculates the x and y scale factors.
             * To be called whenever the size of the screen changes.
             */
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

    fun addOffset(deltaX: Float, deltaY: Float)
    /** add an offset to move the viewport around */
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

    fun gridToScreen(gridPos: Coord): Pair<Int, Int>
    {
        val posX = gridPos.x * scaleX + GameView.viewportMargin + offsetX
        val posY = gridPos.y * scaleY + GameView.viewportMargin + offsetY
        return Pair(posX.toInt(), posY.toInt())
    }

    fun rectToScreen(rectInGridCoord: Rect): Rect
    /** converts a rectangle given in grid coordinates */
    {
        val upperLeft = gridToScreen(Coord(rectInGridCoord.left, rectInGridCoord.top))
        val lowerRight = gridToScreen(Coord(rectInGridCoord.right, rectInGridCoord.bottom))
        return Rect(upperLeft.first, upperLeft.second, lowerRight.first, lowerRight.second)
    }

    fun isInRightHalfOfViewport(posX: Int): Boolean
            /** determines which half of the viewport the point is in. */
    {
        return posX > viewportWidth / 2
    }

}