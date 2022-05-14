package com.example.cpudefense.networkmap

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import com.example.cpudefense.*
import com.example.cpudefense.gameElements.GameElement
import com.example.cpudefense.gameElements.Vehicle

open class Node(val theNetwork: Network?, x: Float, y: Float): GameElement()
{

    data class Data
        (
        var ident: Int,
        var gridX: Float,
        var gridY: Float,
        var range: Float
                )

    var data = Data(ident = -1, gridX = x, gridY = y, range = 0.0f)
    var posOnGrid = GridCoord(Pair(x,y))

    var distanceToVehicle: HashMap<Vehicle, Float> = HashMap()
    open var actualRect = calculateActualRect().makeSquare()

    override fun update() {
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
        actualRect = calculateActualRect().makeSquare()
        actualRect.setCenter(viewport.gridToScreen(posOnGrid))
        val paint = Paint()
        paint.color = theNetwork?.theGame?.resources?.getColor(R.color.network_background) ?: Color.BLACK
        paint.style = Paint.Style.FILL
        canvas.drawRect(actualRect, paint)
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRect(actualRect, paint)

    }

    fun calculateActualRect(): Rect
            /** determines the size of this node on the screen based on the grid points.
             * @return the actual size of a node
             */
    {
        val factor = 3.0f
        val dist = theNetwork?.distanceBetweenGridPoints() ?: Pair(0,0)
        val distX = dist.first * factor
        val distY = dist.second * factor
        return Rect(0, 0, distX.toInt(), distY.toInt())
    }

    fun notify(vehicle: Vehicle, distance: Float)
            /** called to notify this node that a vehicle is near (i.e., on a link from this node).
             * @param vehicle The vehicle approaching
             * @param distance Distance on the link, in grid units. Positive when approaching,
             * negative when leaving
             */
    {
        distanceToVehicle[vehicle] = distance
    }


    fun distanceTo(vehicle: Vehicle): Float?
    {
        val dist = distanceToVehicle[vehicle]
        if (dist != null)
            return if (dist>0) dist else -dist
        else return null
    }

    open fun onDown(event: MotionEvent): Boolean {
        return false
    }

}