package com.example.cpudefense.networkmap
import android.graphics.Canvas
import android.graphics.Paint
import com.example.cpudefense.*
import com.example.cpudefense.gameElements.GameElement
import kotlin.math.abs

class Link(val theNetwork: Network, var node1: Node, var node2: Node, var ident: Int, var mask: Int = 0x0F, var variant: Variant? = Variant.CONVEX): GameElement() {

    data class Data
        (
        var ident: Int,
        var startId: Int,
        var endId: Int,
        var mask: Int,
        var variant: Variant?
    )

    var data = Data(
        ident = ident,
        startId = node1.data.ident,
        endId = node2.data.ident,
        mask = mask,
        variant = variant,
    )

    enum class Variant { CONCAVE, CONVEX }

    var startPointOnGrid: Coord = node1.posOnGrid
    var endPointOnGrid: Coord = node2.posOnGrid
    var interPointOnGrid: Coord? = null

    var lengthOnGrid: Float = 0f
    var usageCount: Int = 0 // number of times this link is in a track. Used during creation.
    var connectorWidth = 6f
    var connectorRadius = 8f
    val paintConnector = Paint()
    val paintEntry = Paint()
    val paintBackground = Paint()
    var paintLineBackground = Paint()

    init {
        calculateIntermediatePointPosition()
        with (paintBackground)
        {
            color = theNetwork.theGame.resources.getColor(R.color.network_background)
            style = Paint.Style.FILL_AND_STROKE
        }
        with (paintConnector)
        {
            color = theNetwork.theGame.resources.getColor(R.color.connectors)
            style = Paint.Style.STROKE
            strokeWidth = connectorWidth
        }
        with (paintEntry)
        {
            color = theNetwork.theGame.resources.getColor(R.color.entrypoints)
            style = Paint.Style.STROKE
            strokeWidth = connectorWidth
        }
        paintLineBackground = Paint(paintBackground)
    }

    private fun calculateIntermediatePointPosition()
    {
        val distHori = endPointOnGrid.x - startPointOnGrid.x
        val distVert = endPointOnGrid.y - startPointOnGrid.y

        if (distHori>0 && distVert>0)
            calculate4thQuadrant(startPointOnGrid, endPointOnGrid)
        else if (distHori<0 && distVert<0)
            calculate4thQuadrant(endPointOnGrid, startPointOnGrid)
        else if (distHori>0 && distVert<0)
            calculate1stQuadrant(startPointOnGrid, endPointOnGrid)
        else if (distHori<0 && distVert>0)
            calculate1stQuadrant(endPointOnGrid, startPointOnGrid)
        else
            interPointOnGrid = null
        lengthOnGrid = getLength()
    }


    private fun calculate1stQuadrant(point1: Coord, point2: Coord)
            /** determines link coordinates in case that horizontal distance
             * from 1st to 2nd point is positive and vertical is negative
             */
    {
        val distHori: Float = abs(point2.x - point1.x)
        val distVert: Float = abs(point2.y - point1.y)

        when (data.variant ?: Variant.CONVEX) {
            Variant.CONCAVE -> {
                if (distVert < distHori)
                    interPointOnGrid = Coord(point1.x+distVert, point2.y)  // done
                else if (distVert > distHori)
                    interPointOnGrid = Coord(point2.x, point1.y-distHori) // done
            }
            Variant.CONVEX -> {
                if (distVert < distHori)
                    interPointOnGrid = Coord(point2.x-distVert, point1.y)
                else if (distVert > distHori)
                    interPointOnGrid = Coord(point1.x, point2.y + distHori)
            }
        }
    }

    private fun calculate4thQuadrant(point1: Coord, point2: Coord)
            /** determines link coordinates in case that both horizontal and vertical distance
             * from 1st to 2nd point is positive.
             */
    {
        val distHori: Float = abs(point2.x - point1.x)
        val distVert: Float = abs(point2.y - point1.y)

        when (data.variant ?: Variant.CONVEX)
        {
            Variant.CONCAVE -> {
                if (distVert > distHori)
                    interPointOnGrid = Coord(point2.x, point1.y+distHori) // done
                else if (distVert < distHori)
                    interPointOnGrid = Coord(point1.x+distVert, point2.y) // done
            }
            Variant.CONVEX -> {
                if (distVert > distHori)
                    interPointOnGrid = Coord(point1.x, point2.y-distHori)
                else if (distVert < distHori)
                    interPointOnGrid = Coord(point2.x-distVert, point1.y)
            }
        }
    }

    private fun getLength(): Float
            /** returns the length of the link on the grid */
    {
        var length = 0.0f
        if (interPointOnGrid == null)
            length = startPointOnGrid.distanceTo(endPointOnGrid)
        else
            interPointOnGrid?.let {
                length = startPointOnGrid.distanceTo(it) + it.distanceTo(endPointOnGrid)
            }
        return length
    }

    fun getPositionOnGrid(distanceTravelled: Float, start: Node): Coord
            /** calculates the position on this link, given a starting point and the distance.
             * @param distanceTravelled The distance from the starting point following the link
             * @param start one of the two nodes of this link
             * @return the coordinates on the grid that correspond to the position
             */
    {
        val end = if (start == node1) node2 else node1
        var positionOnGrid = Coord(0f, 0f)
        if (distanceTravelled > this.lengthOnGrid)
            positionOnGrid = end.posOnGrid
        else if (interPointOnGrid != null) // pass by intermediate point
            interPointOnGrid?.let {
                val delta1 = it.minus(start.posOnGrid)
                val delta2 = end.posOnGrid.minus(it)
                if (distanceTravelled <= delta1.length())
                    positionOnGrid = start.posOnGrid.plus(delta1.multiplyBy(distanceTravelled / delta1.length()))
                else
                    positionOnGrid = it.plus(delta2.multiplyBy((distanceTravelled - delta1.length()) / delta2.length()))
            }
        else // no intermediate point
        {
            val delta = end.posOnGrid.minus(start.posOnGrid)
            positionOnGrid = start.posOnGrid.plus(delta.multiplyBy(distanceTravelled/lengthOnGrid))
        }
        return positionOnGrid
    }

    override fun update() {
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
        var point1: Coord
        var point2: Coord
        val delta = 0.7f // distance between two parallel lines
        val dx: Float
        val dy: Float

        calculateIntermediatePointPosition()

        when (startPointOnGrid.direction(endPointOnGrid))
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
            point1 = Coord(startPointOnGrid.x + displacementX, startPointOnGrid.y + displacementY)
            point2 = Coord(endPointOnGrid.x + displacementX, endPointOnGrid.y + displacementY)
            if (interPointOnGrid == null) // direct connection
            {
                displayLine(canvas, viewport, point1, point2)
            }
            else  // connection via intermediate point
                interPointOnGrid?.let {
                    val point0 = Coord(it.x+displacementX, it.y+displacementY)
                    displayLine(canvas, viewport, point1, point0)
                    displayLine(canvas, viewport, point0, point2)
                }
            if (node1.drawConnectorsOnLinks())
                displayConnectorCircle(canvas, viewport, point1)
            if (node2.drawConnectorsOnLinks())
                displayConnectorCircle(canvas, viewport, point2)
        }
    }

    private fun displayLine(canvas: Canvas, viewport: Viewport, startGridPoint: Coord, endGridPoint: Coord)
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

    private fun displayConnectorCircle(canvas: Canvas, viewport: Viewport, gridPoint: Coord)
    {
        val point = viewport.gridToViewport(gridPoint)
        val radius = connectorRadius
        canvas.drawCircle(point.first.toFloat(), point.second.toFloat(), radius, paintBackground)
        canvas.drawCircle(point.first.toFloat(), point.second.toFloat(), radius, paintEntry)
    }

    companion object {
        fun createFromData(network: Network, data: Data): Link?
                /** reconstruct a Link object based on the saved data
                 * and set all inner proprieties
                 */
        {
            val node1 = network.nodes[data.startId] ?: return null
            val node2 = network.nodes[data.endId] ?: return null
            val link = Link(network, node1, node2, data.ident, data.mask, data.variant)
            link.data = data
            return link
        }

        var maskFilter: List<Int> = listOf(0x08, 0x04, 0x02, 0x01)
    }
}
