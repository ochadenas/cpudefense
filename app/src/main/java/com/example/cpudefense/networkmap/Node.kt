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
    val resources: Resources = theNetwork.gameView.resources

    data class Data
        (
        var ident: Int,
        var gridX: Float,
        var gridY: Float,
        var range: Float
                )

    var data = Data(ident = -1, gridX = x, gridY = y, range = 0.0f)
    var posOnGrid = Coord(Pair(x,y))
    var connectedLinks = CopyOnWriteArrayList<Link>() // used during level setup

    open var actualRect: Rect? = null
    /** hack: limit list cleanup to improve performance */
    private var ticks = 100

    /** keep track of the current distance to the vehicles in range */
    enum class VehicleDirection { APPROACHING, LEAVING, GONE }
    data class Distance ( var distance: Float, var direction: VehicleDirection )
    private var distanceToVehicle: ConcurrentHashMap<Vehicle, Distance> = ConcurrentHashMap()
    val vehiclesDefinitelyGone = mutableListOf<Vehicle>()
    val vehiclesInRange = mutableListOf<Vehicle>()

    override fun update() {
        ticks--
        if (ticks<0)
        {
            cleanupVehiclesInRange()
            ticks = 100
        }
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
        actualRect = calculateActualRect()?.makeSquare()
        actualRect?.setCenter(viewport.gridToViewport(posOnGrid))
        actualRect?.let { rect ->
            val paint = Paint()
            paint.color = resources.getColor(R.color.network_background)
            paint.style = Paint.Style.FILL
            canvas.drawRect(rect, paint)
            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRect(rect, paint)
        }
    }

    open fun drawConnectorsOnLinks(): Boolean
            /** whether the ends of connectors are shown.
             * @return false if the node itself supersedes the link ends.
             */
    { return true }

    fun calculateActualRect(): Rect?
            /** determines the size of this node on the screen based on the grid points.
             * @return the actual size of a node, or null if size cannot be determined
             */
    {
        val factor = 3.0f
        val dist = theNetwork.distanceBetweenGridPoints()
        return dist?.let {
            if (it.first>0 && it.second>0) {
                val distX = it.first * factor
                val distY = it.second * factor
                Rect(0, 0, distX.toInt(), distY.toInt())
            }
            else
                null
        }
    }

    fun notify(vehicle: Vehicle, distance: Float = 0f, direction: VehicleDirection)
            /** called to notify this node that a vehicle is near (i.e., on a link from this node).
             * @param vehicle The vehicle approaching
             * @param distance Distance on the link, in grid units. Always positive
             * @param direction Whether the vehicle approaches or leaves. Use "GONE" to de-subscribe.
             */
    {
        // distanceToVehicle[vehicle]?.let { it.distance = distance; it.direction = direction; return }
        distanceToVehicle[vehicle] = Distance(distance, direction)
    }


    fun distanceTo(vehicle: Vehicle): Float?
    /** @return the absolute distance to the vehicle (always positive) or null if out of range */
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
        /*
        // first, clean up our list and remove all vehicles that are no longer considered
        val vehiclesDefinitelyGone = distanceToVehicle.keys.filter { distanceToVehicle[it]?.direction == VehicleDirection.GONE }
        vehiclesDefinitelyGone.forEach { distanceToVehicle.remove(it) }
        // check the distance and return a list of the vehicles in range
        val vehiclesInRange = distanceToVehicle.keys.filter {
            distanceTo(it)?.let { it <= range } ?: false }
        return vehiclesInRange

         */
        /*
        vehiclesDefinitelyGone.clear()
        vehiclesInRange.clear()
        distanceToVehicle.entries.forEach { (vehicle, dist: Distance) ->
            if (dist.direction == VehicleDirection.GONE)
                vehiclesDefinitelyGone.add(vehicle)
            else if (dist.distance <= range )
                vehiclesInRange.add(vehicle)
        }
        vehiclesDefinitelyGone.forEach { distanceToVehicle.remove(it) }
        return vehiclesInRange

         */
        return distanceToVehicle.keys.filter { vehicle ->
            distanceToVehicle[vehicle]?.let {
                it.direction != VehicleDirection.GONE &&
                        it.distance <= range
            } ?: false
        }
    }

    private fun cleanupVehiclesInRange()
    /** remove the vehicles from the list that are already GONE */
    {
        val hashMap: Map<Vehicle, Distance> = distanceToVehicle.filterValues { it.direction != VehicleDirection.GONE }
        if (hashMap.isNotEmpty())
            distanceToVehicle = ConcurrentHashMap(hashMap)
    }

    open fun onDown(event: MotionEvent): Boolean {
        return false
    }

}