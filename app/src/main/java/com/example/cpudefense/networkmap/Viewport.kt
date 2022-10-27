package com.example.cpudefense.networkmap

import android.graphics.Rect
import com.example.cpudefense.Game

class Viewport
/** class that is responsible for mapping internal grid coordinates to screen coordinates */
{
    data class Data(
        var gridSize: Rect = Rect(0, 0, 100, 100)
    )

    var viewportWidth: Int = 0
    var viewportHeight: Int = 0
    var screen = Rect()
    var gridSizeX: Int = 0
    var gridSizeY: Int = 0
    var scaleX = 1.0f
    var scaleY = 1.0f
    var offsetX = 0
    var offsetY = 0
    var userScale = 1.0f // zoom factor chosen by the player
    var isValid = false

    fun setScreenSize(width: Int, height: Int)
    {
        if (width == 0 || height == 0)
            isValid = false
        else
        {
            screen = Rect(0, 0, width, height)
            this.viewportWidth = width - 2 * Game.viewportMargin
            this.viewportHeight = height - 2 * Game.viewportMargin
            calculateScale()
        }
    }

    fun reset()
    {
        offsetX = 0
        offsetY = 0
        userScale = 1.0f
    }

    fun setGridSize(gridSizeX: Int, gridSizeY: Int)
    {
        this.gridSizeX = gridSizeX
        this.gridSizeY = gridSizeY
        calculateScale()
    }

    private fun calculateScale()
    {
        val width = viewportWidth.toFloat()
        val height = viewportHeight.toFloat()
        if (width == 0f || height == 0f)
            isValid = false
        else if (gridSizeX>0 && gridSizeY>0) {
            scaleX = viewportWidth.toFloat() / gridSizeX
            scaleY = viewportHeight.toFloat() / gridSizeY
            scaleX *= userScale
            scaleY *= userScale
            isValid = true
        }
        else
            isValid = false  // screen size is known, but grid size isn't
    }

    fun addOffset(deltaX: Float, deltaY: Float)
    {
        offsetX += deltaX.toInt()
        offsetY += deltaY.toInt()
    }

    fun gridToViewport(gridPos: GridCoord): Pair<Int, Int>
    {
        var posX = gridPos.x * scaleX + Game.viewportMargin + offsetX
        var posY = gridPos.y * scaleY + Game.viewportMargin + offsetY
        return Pair(posX.toInt(), posY.toInt())
    }

    fun isInRightHalfOfViewport(posX: Int): Boolean
    {
        return posX > viewportWidth / 2
    }

    fun getRect(): Rect
    {
        return Rect(0, 0, viewportWidth, viewportHeight)
    }

}