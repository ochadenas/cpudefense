package com.example.cpudefense.networkmap

import android.graphics.Rect
import com.example.cpudefense.GameMechanics

class Viewport
/** class that is responsible for mapping internal grid coordinates to screen coordinates */
{
    data class Data(
        var gridSize: Rect = Rect(0, 0, 100, 100)
    )

    var viewportWidth: Int = 0
    var viewportHeight: Int = 0
    var screen = Rect()
    private var gridSizeX: Int = 0
    private var gridSizeY: Int = 0
    private var scaleX = 1.0f
    private var scaleY = 1.0f
    private var offsetX = 0
    private var offsetY = 0
    private var userScale = 1.0f // zoom factor chosen by the player
    var isValid = false

    /* default grid size that fits on screen without scrolling */
    private val standardGridSizeX = 50
    private val standardGridSizeY = 60

    fun setScreenSize(width: Int, height: Int)
    {
        if (width == 0 || height == 0)
            isValid = false
        else
        {
            screen = Rect(0, 0, width, height)
            this.viewportWidth = width - 2 * GameMechanics.viewportMargin
            this.viewportHeight = height - 2 * GameMechanics.viewportMargin
            calculateScale()
        }
    }

    fun reset()
    {
        offsetX = 0
        offsetY = 20
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
        else if (standardGridSizeX>0 && standardGridSizeY>0) {
            scaleX = viewportWidth.toFloat() / standardGridSizeX
            scaleY = viewportHeight.toFloat() / standardGridSizeY
            scaleX *= userScale
            scaleY *= userScale
            isValid = true
        }
        else
            isValid = false  // screen size is known, but grid size isn't
    }

    fun addOffset(deltaX: Float, deltaY: Float)
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

    fun gridToViewport(gridPos: Coord): Pair<Int, Int>
    {
        val posX = gridPos.x * scaleX + GameMechanics.viewportMargin + offsetX
        val posY = gridPos.y * scaleY + GameMechanics.viewportMargin + offsetY
        return Pair(posX.toInt(), posY.toInt())
    }

    fun rectToViewport(rectInGridCoord: Rect): Rect
    {
        val upperleft = gridToViewport(Coord(rectInGridCoord.left, rectInGridCoord.top))
        val lowerright = gridToViewport(Coord(rectInGridCoord.right, rectInGridCoord.bottom))
        return Rect(upperleft.first, upperleft.second, lowerright.first, lowerright.second)
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