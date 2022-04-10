package com.example.cpudefense.networkmap

import android.graphics.Rect
import com.example.cpudefense.Game

class Viewport {
    data class Data(
        var gridSize: Rect = Rect(0, 0, 100, 100)
    )

    var screenWidth: Int = 0
    var screenHeight: Int = 0
    var gridSizeX: Int = 0
    var gridSizeY: Int = 0
    var scaleX = 1.0f
    var scaleY = 1.0f

    fun setSize(width: Int, height: Int)
    {
        this.screenWidth = width - 2 * Game.viewportMargin
        this.screenHeight = height - 2 * Game.viewportMargin
        scaleX = screenWidth.toFloat() / gridSizeX
        scaleY = screenHeight.toFloat() / gridSizeY
    }

    fun setViewportSize(gridSizeX: Int, gridSizeY: Int)
    {
        this.gridSizeX = gridSizeX
        this.gridSizeY = gridSizeY
        scaleX = screenWidth.toFloat() / gridSizeX
        scaleY = screenHeight.toFloat() / gridSizeY
    }

    fun gridToScreen(gridPos: GridCoord): Pair<Int, Int>
    {
        var posX = gridPos.x * scaleX + Game.viewportMargin
        var posY = gridPos.y * scaleY + Game.viewportMargin
        return Pair(posX.toInt(), posY.toInt())
    }

    fun isInRightHalfOfScreen(posX: Int): Boolean
    {
        return posX > screenWidth / 2
    }

    fun getRect(): Rect
    {
        return Rect(0, 0, screenWidth, screenHeight)
    }
}