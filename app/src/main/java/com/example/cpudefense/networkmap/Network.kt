package com.example.cpudefense.networkmap

import android.graphics.*
import android.view.MotionEvent
import com.example.cpudefense.Game
import com.example.cpudefense.effects.Background
import com.example.cpudefense.gameElements.GameElement
import com.example.cpudefense.gameElements.Vehicle
import java.util.concurrent.CopyOnWriteArrayList

class Network(val theGame: Game, x: Int, y: Int): GameElement() {
    data class Data(
        var gridSizeX: Int = 1,
        var gridSizeY: Int = 1,
        var nodes: HashMap<Int, Node.Data> = hashMapOf(),
        var links: HashMap<Int, Link.Data> = hashMapOf(),
        var tracks: HashMap<Int, Track.Data> = hashMapOf(),
        var vehicles: List<Vehicle.Data> = listOf()
    )

    var data = Data(gridSizeX = x, gridSizeY = y)

    var nodes = hashMapOf<Int, Node>()
    var links = hashMapOf<Int, Link>()
    var vehicles: CopyOnWriteArrayList<Vehicle> = CopyOnWriteArrayList<Vehicle>()

    private lateinit var networkImage: Bitmap
    private var paint = Paint()
    private var gridPointDistance: Pair<Int, Int>? = null

    enum class Dir { HORIZONTAL, VERTICAL, DIAGONAL, REVERSE_DIAGONAL, UNDEFINED }

    fun provideData(): Data
    /** serializes the network into a 'data' structure that can be stored as string. */
    {
        nodes.forEach() { (key, value) -> data.nodes[key] = value.data }
        links.forEach() { (key, value) -> data.links[key] = value.data }
        return data
    }

    fun loadStateFromData(data: Data)
    {

    }

    companion object {
        fun createNetworkFromData(game: Game, data: Data): Network
        {
            var network = Network(game, data.gridSizeX, data.gridSizeY)

            return network
        }
    }

    fun distanceBetweenGridPoints(): Pair<Int, Int>
    {
        if (gridPointDistance == null || gridPointDistance == Pair(0,0))
        {
            val point0 = theGame.viewport.gridToViewport(GridCoord(0,0))
            val point1 = theGame.viewport.gridToViewport(GridCoord(1,1))
            gridPointDistance = Pair(point1.first-point0.first, point1.second-point0.second)
        }
        return gridPointDistance ?: Pair(0,0)
    }

    override fun update() {
        for (obj in nodes.values)
            obj.update()
        for (obj in links.values)
            obj.update()
        for (obj in vehicles)
            obj.update()
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
        displayNetwork(canvas, viewport)
        for (obj in nodes.values)
            obj.display(canvas, viewport)
        for (obj in vehicles)
            obj.display(canvas, viewport)
    }

    private fun displayNetwork(canvas: Canvas, viewport: Viewport)
    {
        // displayFrame(canvas, viewport) // optional
        if (!this::networkImage.isInitialized)
            recreateNetworkImage(viewport)
        canvas.drawBitmap(this.networkImage, null, viewport.getRect(), paint)
    }

    private fun displayFrame(canvas: Canvas, viewport: Viewport)
    {
        val cornerTopleft = viewport.gridToViewport(GridCoord(0, 0))
        val cornerBottomright = viewport.gridToViewport(GridCoord(data.gridSizeX, data.gridSizeY))
        val actualRect = Rect(cornerTopleft.first, cornerTopleft.second, cornerBottomright.first, cornerBottomright.second)
        val paint = Paint()
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRect(actualRect, paint)
    }

    private fun recreateNetworkImage(viewport: Viewport)
    /** function that must be called whenever the viewport (or the network configuration) changes */
    {
        this.networkImage = Bitmap.createBitmap(viewport.viewportWidth, viewport.viewportHeight, Bitmap.Config.ARGB_8888)
        var canvas = Canvas(this.networkImage)
        for (obj in links.values)
            obj.display(canvas, viewport)
    }

    fun addNode(node: Node?, ident: Int = -1): Int
            /**
             * @param ident If no ident is given, a new one is created.
             * @return Ident of the node
             */
    {
        if (node == null)
            return -1
        var nodeId = if (ident >= 0) ident else nodes.size + 1
        node.let { nodes[nodeId] = it }
        return nodeId
    }

    fun createNode(gridX: Int, gridY: Int, ident: Int = -1): Node?
    /** creates a node at the given grid position */
    {
        if (gridX<=0 || gridX>=data.gridSizeX || gridY<=0 || gridY>data.gridSizeY)
            return null
        var node = Node(this, gridX.toFloat(), gridY.toFloat())
        addNode(node, ident)
        return node
    }


    fun addLink(link: Link?, ident: Int = -1): Int
            /**
             * @param ident If no ident is given, a new one is created.
             * @return Ident of the link
             */
    {
        val linkId = if (ident >= 0) ident else links.size + 1
        link?.let { links[linkId] = it }
        return linkId
    }

    fun createTrack(linkIdents: List<Int>, isCircle: Boolean): Track
    {
        var track = Track(this)
        track.data.linkIdents = linkIdents
        track.data.isCircle = isCircle
        linkIdents.forEach() { track.links.add(links[it]) }
        return track
    }

    fun addVehicle(vehicle: Vehicle)
    /** adds an existing vehicle to the network */
    {
        vehicles.add(vehicle)
    }

    fun onLongPress(p0: MotionEvent?) {
    }
}