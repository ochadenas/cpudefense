package com.example.cpudefense.networkmap

import kotlin.math.sqrt

class GridCoord(var x: Float = 0.0f, var y: Float = 0.0f) {
    constructor(xInt: Int = 0, yInt: Int = 0): this (xInt.toFloat(), yInt.toFloat())
    constructor(pos: Pair<Float, Float>): this (pos.first, pos.second)

    fun asPair(): Pair<Float, Float>
    {
        return Pair(x, y)
    }

    fun multiplyBy(f: Float): GridCoord
    {
        x *= f
        y *= f
        return this
    }

    fun plus(other: GridCoord): GridCoord
    {
        val result = GridCoord(0, 0)
        result.x = this.x + other.x
        result.y = this.y + other.y
        return result
    }

    fun minus(other: GridCoord): GridCoord
    {
        val result = GridCoord(0, 0)
        result.x = this.x - other.x
        result.y = this.y - other.y
        return result
    }

    private fun lengthSquared(): Float
    {
        return x*x + y*y
    }

    fun length(): Float
    {
        return sqrt(lengthSquared())
    }

    private fun distanceToSquared(other: GridCoord?): Float
    {
        if (other == null)
            return 0f
        val delta = other.minus(this)
        return delta.lengthSquared()
    }

    fun distanceTo(other: GridCoord?): Float
    { return sqrt(distanceToSquared(other))}

    fun direction(other: GridCoord?): Network.Dir
    {
        if (other == null)
            return Network.Dir.UNDEFINED
        else if (other.x>x && other.y>y)
            return Network.Dir.REVERSE_DIAGONAL
        else if (other.x>x && other.y<y)
            return Network.Dir.DIAGONAL
        else if (other.x<x && other.y>y)
            return Network.Dir.DIAGONAL
        else if (other.x<x && other.y<y)
            return Network.Dir.REVERSE_DIAGONAL
        else if (other.x == x)
            return Network.Dir.VERTICAL
        else if (other.y == y)
            return Network.Dir.HORIZONTAL
        else
            return Network.Dir.UNDEFINED
    }



}