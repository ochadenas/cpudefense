package com.example.cpudefense.networkmap

import android.graphics.Rect
import com.example.cpudefense.GameView

class Viewport(var number: Int)
/** class that is responsible for mapping internal grid [Coord] to screen coordinates */
{
    data class Data(
        var gridSize: Rect = Rect(0, 0, 100, 100)
    )

    /** width of the viewport in screen coordinates */
    var viewportWidth: Int = 0
    /** height of the viewport in screen coordinates */
    var viewportHeight: Int = 0
    /** visible portion of the screen, in screen coordinates */
    var screen = Rect()
    /** used to keep track whether the network elements must be recalculated after scale changes */
    var scaleHasChanged = true
    /** size of the complete game board, in grid coordinates */
    private var gridSize = Coord(0, 0)
    /** vertical size of the complete game board, in grid coordinates */
    private var scaleX = 1.0f
    private var scaleY = 1.0f
    /** offset to the origin when moving the viewport around, in screen coordinates */
    private var offsetX = 0
    private var offsetY = 0
    /** zoom factor chosen by the player */
    private var userScale = 1.0f
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
            this.viewportWidth = width - 2 * GameView.viewportMargin
            this.viewportHeight = height - 2 * GameView.viewportMargin
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
        this.gridSize = gridSize
        calculateScale()
    }

    private fun calculateScale()
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
            scaleX *= userScale
            scaleY *= userScale
            isValid = true
            scaleHasChanged = true
        }
        else
            isValid = false
    }

    fun addOffset(deltaX: Float, deltaY: Float)
    /** add an offset to move the viewport around */
    {
        val maxX = viewportWidth / 2
        offsetX += deltaX.toInt()
        if (offsetX > maxX)
            offsetX = maxX
        else if (offsetX < -maxX)
            offsetX = -maxX
        val maxY = viewportHeight / 2
        offsetY += deltaY.toInt()
        if (offsetY > maxY)
            offsetY = maxY
        else if (offsetY < -maxY)
            offsetY = - maxY
    }

    fun scale(factor: Float)
    {
        // avoid scaling too small or too big
        val newScale = userScale * factor
        if (newScale !in 0.25 ..3.2)
            return
        userScale = newScale
        calculateScale()
        // re-center the viewport
        offsetX -= (screen.width() * (factor-1)).toInt() / 2
        offsetY -= (screen.height() * (factor-1)).toInt() / 2
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