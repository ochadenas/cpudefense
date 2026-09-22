@file:Suppress("DEPRECATION")

package com.example.cpudefense.networkmap

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import com.example.cpudefense.*
import com.example.cpudefense.gameElements.GameElement
import com.example.cpudefense.gameElements.Vehicle
import com.example.cpudefense.utils.makeSquare
import com.example.cpudefense.utils.setCenter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

open class Node(val theNetwork: Network, x: Float, y: Float): GameElement()
{
    val resources: Resources = theNetwork.commonView.resources

    data class Data
        (
        var ident: Int,
        /** horizontal coordinate on the grid */
        var gridX: Float,
        /** vertical coordinate on the grid */
        var gridY: Float,
        /** the distance (in grid coords) that this node can act, e.g. shoot on attackers */
        var range: Float
                )

    var data = Data(ident = -1, gridX = x, gridY = y, range = 0.0f)
    var posOnGrid = Coord(Pair(x,y))
    /** used during level setup */
    var connectedLinks = CopyOnWriteArrayList<Link>()

    /** enable or disable manual placement of the node */
    var moveEnabled = false

    open var actualRect: Rect? = null

    /** hack: limit list cleanup to improve performance */
    private var ticks = 100

    val paint = Paint()

    /** keep track of the current distance to the vehicles in range */
    enum class VehicleDirection { APPROACHING, LEAVING, GONE }
    data class Distance ( var distance: Float, var direction: VehicleDirection )
    private var distanceToVehicle: ConcurrentHashMap<Vehicle, Distance> = ConcurrentHashMap()
    val vehiclesDefinitelyGone = mutableListOf<Vehicle>()
    val vehiclesInRange = mutableListOf<Vehicle>()

    /** sets the node on the given grid coordinates and re-calculates the rectangle on the screen */
    fun placeOnGrid(viewport: Viewport, x: Float, y: Float)
    {
        posOnGrid = Coord(x, y)
        data.gridX = x
        data.gridY = y
        actualRect = calculateActualRect(viewport)
    }

    override fun update() {
        ticks--
        if (ticks<0)
        {
            cleanupVehiclesInRange()
            ticks = 100
        }
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
        actualRect = calculateActualRect(viewport)?.makeSquare()
        actualRect?.let { rect ->
            paint.color = resources.getColor(R.color.network_background)
            paint.style = Paint.Style.FILL
            canvas.drawRect(rect, paint)
            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRect(rect, paint)
        }
    }

    /** displays the cross symbol on the node */
    fun displayMoveIcon(canvas: Canvas, viewport: Viewport)
    {
        if (!moveEnabled)
            return
        paint.alpha = 255
        calculateActualRect(viewport)?.makeSquare()?.let {
            canvas.drawBitmap(theNetwork.commonView.moveActiveIcon, null, it, paint) }
    }

    /** triggers recalculation of node size */
    open fun applyScale(viewport: Viewport)
    {
        val sizeOnScreen = theNetwork.distanceBetweenGridPoints(viewport)
        sizeOnScreen?.let {
            var widthOnScreen = it.first * CommonView.chipSize.x.toInt()
            var heightOnScreen = it.second * CommonView.chipSize.y.toInt()
            if (widthOnScreen < 2) { widthOnScreen = 2 }  // safety catch
            if (heightOnScreen < 2) { heightOnScreen = 2 }
            actualRect = Rect(0, 0, widthOnScreen, heightOnScreen)
        }
    }

    /** whether the ends of connectors are shown.
     * @return false if the node itself supersedes the link ends.
     */
    open fun drawConnectorsOnLinks(): Boolean
    { return true }

    /** determines the size of this node on the screen based on the grid points.
     * @return the actual rectangle of the node with correct size and position,
     * or null if size cannot be determined
     */
    fun calculateActualRect(viewport: Viewport): Rect?
    {
        val factor = 3.0f
        val dist = theNetwork.distanceBetweenGridPoints(viewport)
        return dist?.let {
            if (it.first>0 && it.second>0) {
                val distX = it.first * factor
                val distY = it.second * factor
                Rect(0, 0, distX.toInt(), distY.toInt())
                    .setCenter(viewport.gridToScreen(posOnGrid))
            }
            else
                null
        }
    }

    /** called to notify this node that a vehicle is near (i.e., on a link from this node).
     * @param vehicle The vehicle approaching
     * @param distance Distance on the link, in grid units. Always positive
     * @param direction Whether the vehicle approaches or leaves. Use "GONE" to de-subscribe.
     */
    fun notify(vehicle: Vehicle, distance: Float = 0f, direction: VehicleDirection)
    {
        // distanceToVehicle[vehicle]?.let { it.distance = distance; it.direction = direction; return }
        distanceToVehicle[vehicle] = Distance(distance, direction)
    }


    /** @return the absolute distance in grid coords to the vehicle (always positive) or null if out of range */
    fun distanceTo(vehicle: Vehicle): Float?
    {
        if (vehicle.startNode != this && vehicle.endNode != this)
        {
            // something went wrong
            return null
        }
        val dist = distanceToVehicle[vehicle]
        if (dist?.direction == VehicleDirection.GONE)
            return null
        else
            return dist?.distance
    }

    fun vehiclesInRange(range: Float): List<Vehicle>
    {
        return distanceToVehicle.keys.filter { vehicle ->
            distanceToVehicle[vehicle]?.let {
                it.direction != VehicleDirection.GONE &&
                        it.distance <= range
            } ?: false
        }
    }

    /** remove the vehicles from the list that are already GONE */
    private fun cleanupVehiclesInRange()
    {
        val hashMap: Map<Vehicle, Distance> = distanceToVehicle.filterValues { it.direction != VehicleDirection.GONE }
        if (hashMap.isNotEmpty())
            distanceToVehicle = ConcurrentHashMap(hashMap)
    }

    open fun onDown(event: MotionEvent): Boolean {
        return false
    }

}