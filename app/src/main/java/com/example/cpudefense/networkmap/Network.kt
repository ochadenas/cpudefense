package com.example.cpudefense.networkmap

import android.graphics.*
import android.view.MotionEvent
import com.example.cpudefense.Game
import com.example.cpudefense.gameElements.Chip
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

    var backgroundImage: Bitmap? = null
    private lateinit var networkImage: Bitmap
    private var paint = Paint()
    private var gridPointDistance: Pair<Int, Int>? = null

    enum class Dir { HORIZONTAL, VERTICAL, DIAGONAL, REVERSE_DIAGONAL, UNDEFINED }

    fun provideData(): Data
    /** serializes the network into a 'data' structure that can be stored as string. */
    {
        nodes.forEach { (key, value) -> data.nodes[key] = value.data }
        links.forEach { (key, value) -> data.links[key] = value.data }
        return data
    }

    fun loadStateFromData(data: Data)
    {

    }

    companion object {
        fun createNetworkFromData(game: Game, data: Data): Network {

            return Network(game, data.gridSizeX, data.gridSizeY)
        }
    }

    fun distanceBetweenGridPoints(): Pair<Int, Int>?
            /**
             * Calculate the distance on the screen between two grid points. (This is a property of the
             * network under a given viewport and does not depend on the actual network elements.)
             *
             * @return 2-dimensional distance vector (x,y), or null if the distance cannot be determined.
             */
    {
        if (gridPointDistance != null)
            return gridPointDistance // no need to recalculate
        if (validateViewport()) {
            val point0 = theGame.viewport.gridToViewport(GridCoord(0, 0))
            val point1 = theGame.viewport.gridToViewport(GridCoord(1, 1))
            gridPointDistance = Pair(point1.first - point0.first, point1.second - point0.second)
        }
        else
            gridPointDistance = null // screen size not known, can't determine distance
        return gridPointDistance
    }

    override fun update() {
        for (obj in nodes.values)
            obj.update()
        for (obj in links.values)
            obj.update()
        for (obj in vehicles)
            obj.update()
    }

    fun validateViewport(): Boolean
    /** The viewport needs the size of the game surface (GameView) to calculate positions on the screen.
     * However, it is not easy to know when GameView actually receives its dimensions.
     * Therefore we keep track whether the scaling factors are valid, and if not, we recalculate them.
     *
     * @return false if the viewport is not valid, i.e. screen dimensions are not known
     */
    {
        val view = theGame.gameActivity.theGameView
        if (view.width > 0 || view.height > 0) {
            theGame.viewport.setScreenSize(view.width, view.viewportHeight(view.height))
            return true
        }
        else
            return false
    }

    override fun display(canvas: Canvas, viewport: Viewport)
    {
        // if viewport is not valid, try to validate it
        if (viewport.isValid == false && validateViewport() == false)
            return
        displayNetwork(canvas, viewport)
        for (obj in nodes.values)
            obj.display(canvas, viewport)
        for (obj in vehicles)
            obj.display(canvas, viewport)
        for (obj in nodes.values)
            (obj as Chip).displayUpgrades(canvas)
    }

    fun makeSnapshot(canvas: Canvas, viewport: Viewport)
    {
        for (obj in links.values)
            obj.display(canvas, viewport)
        for (obj in nodes.values) {
            obj.display(canvas, viewport)
            (obj as Chip).displayUpgrades(canvas)
        }
    }

    private fun displayNetwork(canvas: Canvas, viewport: Viewport)
    /** draws all 'fixed' elements, i.e. the background image and the links */
    {
        // displayFrame(canvas, viewport) // optional
        if (!this::networkImage.isInitialized)
            recreateNetworkImage(viewport)
        canvas.drawBitmap(this.networkImage, null, viewport.screen, paint)
    }

    private fun displayFrame(canvas: Canvas, viewport: Viewport)
            /** draws an outline around the network area, e.g. for testing purposes.
             */
    {
        val cornerTopLeft = viewport.gridToViewport(GridCoord(0, 0))
        val cornerBottomRight = viewport.gridToViewport(GridCoord(data.gridSizeX, data.gridSizeY))
        val actualRect = Rect(cornerTopLeft.first, cornerTopLeft.second, cornerBottomRight.first, cornerBottomRight.second)
        val paint = Paint()
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRect(actualRect, paint)
    }

    fun recreateNetworkImage(viewport: Viewport)
    /** function that must be called whenever the viewport (or the network configuration) changes.
     * It either takes the provided background image or an empty background
     * and places the network elements on it */
    {
        validateViewport()
        backgroundImage = theGame.background?.getImage()
        backgroundImage?.let {
            if (it.width == viewport.screen.width() && it.height == viewport.screen.height())
            // just use the given bitmap, it has the correct dimensions
                this.networkImage = it.copy(it.config, true)
            else
                this.networkImage = Bitmap.createScaledBitmap(
                    it,
                    viewport.screen.width(),
                    viewport.screen.height(),
                    false
                )
        }
        val canvas = Canvas(this.networkImage)
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
        val nodeId = if (ident >= 0) ident else nodes.size + 1
        node.let { nodes[nodeId] = it }
        return nodeId
    }

    fun createNode(gridX: Int, gridY: Int, ident: Int = -1): Node?
    /** creates a node at the given grid position */
    {
        if (gridX<=0 || gridX>=data.gridSizeX || gridY<=0 || gridY>data.gridSizeY)
            return null
        val node = Node(this, gridX.toFloat(), gridY.toFloat())
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
        linkIdents.forEach { track.links.add(links[it]) }
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