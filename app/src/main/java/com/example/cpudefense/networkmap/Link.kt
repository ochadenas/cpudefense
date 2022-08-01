package com.example.cpudefense.networkmap
import android.graphics.Canvas
import android.graphics.Paint
import com.example.cpudefense.*
import com.example.cpudefense.gameElements.GameElement
import kotlin.math.abs

class Link(val theNetwork: Network, var node1: Node, var node2: Node, var ident: Int, var mask: Int = 0x0F): GameElement() {

    data class Data
        (
        var ident: Int,
        var startId: Int,
        var endId: Int,
        var mask: Int
    )

    var data = Data(
        ident = ident,
        startId = node1.data.ident,
        endId = node2.data.ident,
        mask = mask
    )

    var startPoint: GridCoord = node1.posOnGrid
    var endPoint: GridCoord = node2.posOnGrid
    var interPoint: GridCoord? = null

    var lengthOnGrid: Float = 0f
    var connectorWidth = 4f
    var connectorRadius = 6f
    val paintConnector = Paint()
    val paintBackground = Paint()
    var paintLineBackground = Paint()

    init {
        calculateIntermediatePointPosition()
        paintBackground.color = theNetwork.theGame.resources.getColor(R.color.network_background)
        paintBackground.style = Paint.Style.FILL_AND_STROKE
        paintConnector.color = theNetwork.theGame.resources.getColor(R.color.connectors)
        paintConnector.style = Paint.Style.STROKE
        paintConnector.strokeWidth = connectorWidth
        paintLineBackground = Paint(paintBackground)
    }

    fun calculateIntermediatePointPosition()
    {
        val dist_hori = endPoint.x - startPoint.x
        val dist_vert = endPoint.y - startPoint.y

        if (dist_hori>0 && dist_vert>0)
            calculate1stQuadrant(startPoint, endPoint)
        else if (dist_hori<0 && dist_vert<0)
            calculate1stQuadrant(endPoint, startPoint)
        else if (dist_hori>0 && dist_vert<0)
            calculate4thQuadrant(startPoint, endPoint)
        else if (dist_hori<0 && dist_vert>0)
            calculate4thQuadrant(endPoint, startPoint)
        else
            interPoint = null
        lengthOnGrid = getLength()
    }

    fun calculate1stQuadrant(point1: GridCoord, point2: GridCoord)
    /** determines link coordinates in case that both horizontal and vertical distance
     * from 1st to 2nd point is positive.
     */
    {
        val dist_hori: Float = abs(point2.x - point1.x)
        val dist_vert: Float = abs(point2.y - point1.y)

        if (dist_vert > dist_hori)
            interPoint = GridCoord(point1.x, point2.y - dist_hori)
        else if (dist_vert < dist_hori)
            interPoint = GridCoord(point2.x-dist_vert, point1.y)
    }

    fun calculate4thQuadrant(point1: GridCoord, point2: GridCoord)
    /** determines link coordinates in case that horizontal distance
     * from 1st to 2nd point is positive and vertical is negative
     */
    {
        val dist_hori: Float = abs(point2.x - point1.x)
        val dist_vert: Float = abs(point2.y - point1.y)

        if (dist_vert < dist_hori)
            interPoint = GridCoord(point2.x - dist_vert, point1.y)
        else if (dist_vert > dist_hori)
            interPoint = GridCoord(point1.x, point2.y + dist_hori)
    }

    fun getLength(): Float
    /** returns the length of the link on the grid */
    {
        if (interPoint == null)
            return startPoint.distanceTo(endPoint)
        else
        {
            return startPoint.distanceTo(interPoint) + interPoint!!.distanceTo(endPoint)
            /*
            val lSquared: Float = startPoint.distanceToSquared(interPoint) +
                    interPoint!!.distanceToSquared(endPoint)
            return kotlin.math.sqrt(lSquared) */
        }
    }

    fun getPositionOnGrid(distanceTravelled: Float, start: Node): GridCoord
            /** calculates the position on this link, given a starting point and the distance.
             * @param distanceTravelled The distance from the starting point following the link
             * @param start one of the two nodes of this link
             * @return the coordinates on the grid that correspond to the position
             */
    {
        var end = if (start == node1) node2 else node1
        if (distanceTravelled > this.lengthOnGrid)
            return end.posOnGrid
        if (interPoint != null) // pass by intermediate point
        {
            var delta1 = interPoint!!.minus(start.posOnGrid)
            var delta2 = end.posOnGrid.minus(interPoint!!)
            if (distanceTravelled <= delta1.length())
                return start.posOnGrid.plus(delta1.multiplyBy(distanceTravelled/delta1.length()))
            else
            {
                return interPoint!!.plus(delta2.multiplyBy(
                    (distanceTravelled-delta1.length())/delta2.length()))
            }
        }
        else
        {
            var delta = end.posOnGrid.minus(start.posOnGrid)
            return start.posOnGrid.plus(delta.multiplyBy(distanceTravelled/lengthOnGrid))
        }
    }

    override fun update() {
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
        var point1: GridCoord
        var point2: GridCoord
        val delta = 0.7f // distance between two parallel lines
        var dx: Float
        var dy: Float

        calculateIntermediatePointPosition()

        when (startPoint.direction(endPoint))
        {
            Network.Dir.DIAGONAL -> { dx = delta * 1.2f; dy = delta }
            Network.Dir.REVERSE_DIAGONAL -> { dx = delta * 1.2f; dy = -delta }
            Network.Dir.VERTICAL -> { dx = delta * 1.4f; dy = 0f }
            Network.Dir.HORIZONTAL -> { dx = 0f; dy = delta/1.4f }
            else -> { dx = 0f; dy = 0f }
        }
        for (numLines in 0 .. 3)
        {
            if (mask and maskFilter[numLines] == 0)  // skip masked (invisible) lines
                continue
            val displacementX = (numLines - 1.5f) * dx
            val displacementY = (numLines - 1.5f) * dy
            point1 = GridCoord(startPoint.x + displacementX, startPoint.y + displacementY)
            point2 = GridCoord(endPoint.x + displacementX, endPoint.y + displacementY)
            if (interPoint == null) // direct connection
            {
                displayLine(canvas, viewport, point1, point2)
            }
            else  // connection via intermediate point
            {
                val point0 = GridCoord(interPoint!!.x+displacementX, interPoint!!.y+displacementY)
                displayLine(canvas, viewport, point1, point0)
                displayLine(canvas, viewport, point0, point2)
            }
            displayConnectorCircle(canvas, viewport, point1)
            displayConnectorCircle(canvas, viewport, point2)
        }
    }

    fun displayLine(canvas: Canvas, viewport: Viewport, startGridPoint: GridCoord, endGridPoint: GridCoord)
    /** draws one single line from start point to end point, in grid coords */
    {
        val startPoint = viewport.gridToViewport(startGridPoint)
        val endPoint = viewport.gridToViewport(endGridPoint)
        /*
        paintLineBackground.strokeWidth = 4 * connectorWidth
         canvas.drawLine(startPoint.first.toFloat(), startPoint.second.toFloat(),
            endPoint.first.toFloat(), endPoint.second.toFloat(), paintLineBackground)

         */
        canvas.drawLine(startPoint.first.toFloat(), startPoint.second.toFloat(),
            endPoint.first.toFloat(), endPoint.second.toFloat(), paintConnector)
    }

    fun displayConnectorCircle(canvas: Canvas, viewport: Viewport, gridPoint: GridCoord)
    {
        val point = viewport.gridToViewport(gridPoint)
        val radius = connectorRadius
        canvas.drawCircle(point.first.toFloat(), point.second.toFloat(), radius, paintBackground)
        canvas.drawCircle(point.first.toFloat(), point.second.toFloat(), radius, paintConnector)
    }

    companion object {
        fun createFromData(network: Network, data: Data): Link?
            /** reconstruct a Link object based on the saved data
             * and set all inner proprieties
             */
        {
            var node1 = network.nodes[data.startId] ?: return null
            var node2 = network.nodes[data.endId] ?: return null
            var link = Link(network, node1, node2, data.ident)
            link.data = data
            return link
        }

        var maskFilter: List<Int> = listOf(0x08, 0x04, 0x02, 0x01)
    }
}